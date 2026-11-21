/** PriceFull<chain>-<subchain>-<store>-<YYYYMMDD>-<HHMMSS>.gz (docs/price-files.md) */
const PRICE_FULL = /^PriceFull(\d+)-(\d+)-(\d+)-(\d{8})-(\d+)\.gz$/;

export interface PriceFileName {
  name: string;
  chain: string;
  subChain: string;
  store: string;
  /** YYYYMMDD + HHMMSS, comparable comme chaîne. */
  stamp: string;
}

export function parsePriceFileName(name: string): PriceFileName | null {
  const m = name.match(PRICE_FULL);
  if (!m) return null;
  return {name, chain: m[1], subChain: m[2], store: m[3], stamp: m[4] + m[5].padStart(6, "0")};
}

/** Dernier PriceFull d'un magasin donné (ex. "044"), ou null. */
export function pickLatestForStore(names: string[], chain: string, storeId: string): string | null {
  const wanted = storeId.replace(/^0+/, "");
  let best: PriceFileName | null = null;
  for (const n of names) {
    const p = parsePriceFileName(n);
    if (!p || p.chain !== chain || p.store.replace(/^0+/, "") !== wanted) continue;
    if (!best || p.stamp > best.stamp) best = p;
  }
  return best ? best.name : null;
}
