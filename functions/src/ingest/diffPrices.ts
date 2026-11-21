import {ParsedItem} from "./parsePriceFull";

/** Différences entre le nouveau fichier d'une enseigne et la collecte précédente. */
export interface PriceDiff {
  /** Produits absents de la collecte précédente : écriture complète du produit. */
  added: ParsedItem[];
  /** Produits dont le prix a changé : on ne met à jour que le prix. */
  changed: ParsedItem[];
  /** Codes-barres présents dans la collecte précédente et absents du nouveau fichier. */
  removed: string[];
  /** Produits dont le prix n'a pas bougé : aucune écriture. */
  unchanged: number;
}

/**
 * Compare les produits du fichier aux prix de la collecte précédente
 * (chains/{chainId}/snapshot). Sans collecte précédente, tout est « ajouté ».
 */
export function diffPrices(
  items: ParsedItem[],
  previous: Map<string, number> | null,
): PriceDiff {
  if (!previous) return {added: items, changed: [], removed: [], unchanged: 0};

  const added: ParsedItem[] = [];
  const changed: ParsedItem[] = [];
  const seen = new Set<string>();
  let unchanged = 0;

  for (const item of items) {
    seen.add(item.barcode);
    const before = previous.get(item.barcode);
    if (before === undefined) added.push(item);
    else if (before !== item.price) changed.push(item);
    else unchanged++;
  }

  const removed = [...previous.keys()].filter((barcode) => !seen.has(barcode));
  return {added, changed, removed, unchanged};
}
