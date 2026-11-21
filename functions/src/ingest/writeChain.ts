import {Firestore, FieldValue, Timestamp} from "firebase-admin/firestore";
import {COLLECTIONS, ChainId, ChainPrice, PriceSnapshotEntry} from "../models";
import {ParsedItem} from "./parsePriceFull";
import {buildSearchPrefixes} from "./searchPrefixes";

// Firestore accepte 500 opérations par batch ; on garde une marge.
const BATCH_SIZE = 400;

export interface WriteResult {
  runId: string;
  itemCount: number;
}

/**
 * Écrit les produits d'une enseigne :
 *  - products/{barcode} : name, brand, unitDescription, searchPrefixes, prices.{chainId}
 *  - chains/{chainId}/snapshot/latest : priceMap (JSON barcode -> {price})
 *  - chains/{chainId} : lastSuccessAt, lastStatus, itemCount
 *  - ingestionRuns/{id} : journal de la collecte
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
): Promise<WriteResult> {
  const now = Timestamp.now();
  const runRef = db.collection(COLLECTIONS.ingestionRuns).doc();

  for (let i = 0; i < items.length; i += BATCH_SIZE) {
    const batch = db.batch();
    for (const item of items.slice(i, i + BATCH_SIZE)) {
      const price: ChainPrice = {price: item.price, updatedAt: now};
      batch.set(
        db.collection(COLLECTIONS.products).doc(item.barcode),
        {
          name: item.name,
          brand: item.brand,
          unitDescription: item.unitDescription,
          searchPrefixes: buildSearchPrefixes(item.name),
          prices: {[chainId]: price},
        },
        {merge: true},
      );
    }
    await batch.commit();
  }

  const priceMap: Record<string, PriceSnapshotEntry> = {};
  for (const item of items) priceMap[item.barcode] = {price: item.price};

  const chainRef = db.collection(COLLECTIONS.chains).doc(chainId);
  const end = db.batch();
  end.set(chainRef.collection("snapshot").doc("latest"), {
    runId: runRef.id,
    priceMap: JSON.stringify(priceMap),
  });
  end.set(
    chainRef,
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

  return {runId: runRef.id, itemCount: items.length};
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
