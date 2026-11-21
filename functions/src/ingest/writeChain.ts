import {Firestore, FieldValue, Timestamp, WriteBatch} from "firebase-admin/firestore";
import {COLLECTIONS, ChainId, ChainPrice} from "../models";
import {diffPrices} from "./diffPrices";
import {ParsedItem} from "./parsePriceFull";
import {buildSearchPrefixes} from "./searchPrefixes";
import {LoadedSnapshot, writeSnapshot} from "./snapshot";

// Firestore accepte 500 opérations par batch ; on garde une marge.
const BATCH_SIZE = 400;
// Nombre de documents lus d'un coup pour vérifier les produits retirés.
const READ_CHUNK = 300;

type Op = (batch: WriteBatch) => void;

export interface WriteResult {
  runId: string;
  itemCount: number;
  /** Produits nouveaux pour cette enseigne (écriture complète). */
  added: number;
  /** Produits dont le prix a changé (mise à jour du prix seul). */
  changed: number;
  /** Produits retirés du fichier (prix de l'enseigne supprimé). */
  removed: number;
  /** Produits dont le prix n'a pas bougé (aucune écriture). */
  unchanged: number;
}

async function commitInBatches(db: Firestore, ops: Op[]): Promise<void> {
  for (let i = 0; i < ops.length; i += BATCH_SIZE) {
    const batch = db.batch();
    for (const op of ops.slice(i, i + BATCH_SIZE)) op(batch);
    await batch.commit();
  }
}

/**
 * Prépare la suppression du prix de l'enseigne pour les produits retirés du fichier.
 * Un produit qui n'a plus de prix dans aucune enseigne est supprimé en entier :
 * il ne servirait plus à rien dans la recherche ni dans la comparaison.
 * (On lit les documents pour le savoir ; l'enchaînement des enseignes est séquentiel,
 * voir ingestPrices.ts, pour qu'une autre enseigne n'écrive pas en même temps.)
 */
async function removalOps(db: Firestore, chainId: ChainId, barcodes: string[]): Promise<Op[]> {
  const ops: Op[] = [];
  const products = db.collection(COLLECTIONS.products);
  for (let i = 0; i < barcodes.length; i += READ_CHUNK) {
    const refs = barcodes.slice(i, i + READ_CHUNK).map((b) => products.doc(b));
    for (const snap of await db.getAll(...refs)) {
      if (!snap.exists) continue;
      const otherChains = Object.keys(snap.get("prices") ?? {}).filter((id) => id !== chainId);
      if (otherChains.length === 0) ops.push((b) => b.delete(snap.ref));
      else ops.push((b) => b.update(snap.ref, {[`prices.${chainId}`]: FieldValue.delete()}));
    }
  }
  return ops;
}

/**
 * Écrit les changements d'une enseigne par rapport à la collecte précédente :
 *  - produit nouveau : products/{barcode} complet (name, brand, unitDescription,
 *    searchPrefixes, prices.{chainId}) ;
 *  - prix modifié : seulement prices.{chainId} ;
 *  - produit retiré : le champ prices.{chainId} est supprimé ;
 *  - prix identique : rien n'est écrit.
 * Puis, en un seul batch : chains/{chainId}/snapshot (nouvelle photo), chains/{chainId}
 * (lastSuccessAt, lastStatus, itemCount) et ingestionRuns/{id} (journal).
 *
 * Les produits sont écrits avant la photo : si la collecte s'interrompt en route,
 * l'ancienne photo reste en place et la collecte suivante refait les mêmes écritures.
 *
 * `updatedAt` est la date de collecte, pas PriceUpdateTime du fichier
 * (voir docs/price-files.md).
 */
export async function writeChain(
  db: Firestore,
  chainId: ChainId,
  items: ParsedItem[],
  startedAt: Timestamp,
  meta: {displayName: string; referenceStoreId: string},
  previous: LoadedSnapshot | null = null,
): Promise<WriteResult> {
  const now = Timestamp.now();
  const runRef = db.collection(COLLECTIONS.ingestionRuns).doc();
  const products = db.collection(COLLECTIONS.products);
  const diff = diffPrices(items, previous?.prices ?? null);

  const ops: Op[] = [];
  for (const item of diff.added) {
    const price: ChainPrice = {price: item.price, updatedAt: now};
    ops.push((b) => b.set(
      products.doc(item.barcode),
      {
        name: item.name,
        brand: item.brand,
        unitDescription: item.unitDescription,
        searchPrefixes: buildSearchPrefixes(item.name),
        prices: {[chainId]: price},
      },
      {merge: true},
    ));
  }
  for (const item of diff.changed) {
    const price: ChainPrice = {price: item.price, updatedAt: now};
    ops.push((b) => b.set(products.doc(item.barcode), {prices: {[chainId]: price}}, {merge: true}));
  }
  ops.push(...(await removalOps(db, chainId, diff.removed)));
  await commitInBatches(db, ops);

  const prices = new Map(items.map((i) => [i.barcode, i.price] as [string, number]));
  const end = db.batch();
  writeSnapshot(db, end, chainId, runRef.id, prices, previous?.parts ?? 0);
  end.set(
    db.collection(COLLECTIONS.chains).doc(chainId),
    {
      displayName: meta.displayName,
      referenceStoreId: meta.referenceStoreId,
      lastSuccessAt: now,
      lastStatus: "SUCCESS",
      itemCount: items.length,
    },
    {merge: true},
  );
  end.set(runRef, {
    chainId,
    startedAt,
    finishedAt: Timestamp.now(),
    status: "SUCCESS",
    itemCount: items.length,
    error: null,
  });
  await end.commit();

  return {
    runId: runRef.id,
    itemCount: items.length,
    added: diff.added.length,
    changed: diff.changed.length,
    removed: diff.removed.length,
    unchanged: diff.unchanged,
  };
}

/** Journalise un échec sans toucher aux prix existants. */
export async function recordFailure(
  db: Firestore,
  chainId: ChainId,
  startedAt: Timestamp,
  error: string,
): Promise<void> {
  const batch = db.batch();
  batch.set(db.collection(COLLECTIONS.ingestionRuns).doc(), {
    chainId, startedAt, finishedAt: Timestamp.now(), status: "FAILED", itemCount: 0, error,
  });
  batch.set(
    db.collection(COLLECTIONS.chains).doc(chainId),
    {lastStatus: "FAILED", lastFailureAt: FieldValue.serverTimestamp()},
    {merge: true},
  );
  await batch.commit();
}
