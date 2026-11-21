import {DocumentReference, Firestore, WriteBatch} from "firebase-admin/firestore";
import {ChainId, COLLECTIONS, PriceSnapshotEntry} from "../models";

/**
 * Taille maximale (octets) d'une partie du priceMap. Un document Firestore est
 * limité à 1 MiB : on reste bien en dessous (docs/data-model.md).
 */
export const MAX_PART_BYTES = 700_000;

/** Photo des prix de la collecte précédente, relue pour ne réécrire que ce qui a changé. */
export interface LoadedSnapshot {
  prices: Map<string, number>;
  /** Nombre de documents (latest, latest_1, ...) qui la composent. */
  parts: number;
}

/** latest, puis latest_1, latest_2, ... si la photo est découpée. */
export function snapshotRef(db: Firestore, chainId: ChainId, part: number): DocumentReference {
  return db
    .collection(COLLECTIONS.chains)
    .doc(chainId)
    .collection(COLLECTIONS.snapshot)
    .doc(part === 0 ? "latest" : `latest_${part}`);
}

/** Découpe les prix en textes JSON `{ [barcode]: { price } }` de taille bornée. */
export function splitPriceMap(
  prices: Map<string, number>,
  maxBytes: number = MAX_PART_BYTES,
): string[] {
  const parts: string[] = [];
  let entries: string[] = [];
  let size = 2; // les accolades

  for (const [barcode, price] of prices) {
    const entry = `${JSON.stringify(barcode)}:${JSON.stringify({price})}`;
    const entrySize = Buffer.byteLength(entry, "utf8") + 1; // + la virgule
    if (entries.length > 0 && size + entrySize > maxBytes) {
      parts.push(`{${entries.join(",")}}`);
      entries = [];
      size = 2;
    }
    entries.push(entry);
    size += entrySize;
  }
  parts.push(`{${entries.join(",")}}`);
  return parts;
}

/**
 * Relit la photo de la collecte précédente. Renvoie null s'il n'y en a pas, ou
 * si elle est illisible : on réécrit alors tout (plus coûteux mais correct, et
 * sans suppression, puisqu'on ne sait pas ce qui existait avant).
 */
export async function loadSnapshot(db: Firestore, chainId: ChainId): Promise<LoadedSnapshot | null> {
  const first = await snapshotRef(db, chainId, 0).get();
  if (!first.exists) return null;

  const parts: number = first.get("parts") ?? 1;
  const docs = [first];
  for (let i = 1; i < parts; i++) docs.push(await snapshotRef(db, chainId, i).get());

  try {
    const prices = new Map<string, number>();
    for (const doc of docs) {
      if (!doc.exists) return null;
      const obj = JSON.parse(doc.get("priceMap")) as Record<string, PriceSnapshotEntry>;
      for (const [barcode, entry] of Object.entries(obj)) prices.set(barcode, entry.price);
    }
    return {prices, parts};
  } catch {
    return null;
  }
}

/**
 * Ajoute au batch l'écriture de la nouvelle photo (et la suppression des parties
 * en trop de l'ancienne). Renvoie le nombre de parties écrites.
 */
export function writeSnapshot(
  db: Firestore,
  batch: WriteBatch,
  chainId: ChainId,
  runId: string,
  prices: Map<string, number>,
  previousParts: number,
  maxBytes: number = MAX_PART_BYTES,
): number {
  const parts = splitPriceMap(prices, maxBytes);
  parts.forEach((priceMap, i) => {
    batch.set(
      snapshotRef(db, chainId, i),
      i === 0 ? {runId, priceMap, parts: parts.length} : {runId, priceMap},
    );
  });
  for (let i = parts.length; i < previousParts; i++) batch.delete(snapshotRef(db, chainId, i));
  return parts.length;
}
