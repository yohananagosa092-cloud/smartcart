/**
 * Construit les préfixes de recherche d'un nom de produit (voir docs/data-model.md).
 * "חלב תנובה" -> חל, חלב, תנ, תנו, תנוב, תנובה  (minuscules, longueur >= 2).
 */
export function buildSearchPrefixes(name: string): string[] {
  const out = new Set<string>();
  const words = name
    .toLowerCase()
    .split(/[^\p{L}\p{N}]+/u)
    .filter((w) => w.length >= 2);
  for (const word of words) {
    for (let len = 2; len <= word.length; len++) {
      out.add(word.slice(0, len));
    }
  }
  return [...out];
}
