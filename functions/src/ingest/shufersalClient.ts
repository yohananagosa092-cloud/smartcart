/**
 * Client du site public https://prices.shufersal.co.il (sans connexion).
 * Voir docs/price-files.md.
 *
 * Les liens de téléchargement sont des URL Azure signées qui expirent au bout
 * de quelques heures : on relit la liste à chaque exécution et on ne garde jamais un lien.
 */
import {pickLatestForStore} from "./priceFilename";

const BASE = "https://prices.shufersal.co.il";
const TIMEOUT_MS = 60_000;

export interface ShufersalFile {
  /** Nom du fichier, ex. PriceFull7290027600007-002-413-20261121-034000.gz */
  name: string;
  /** Lien de téléchargement signé (valable quelques heures). */
  url: string;
}

/** Extrait le nom et le lien signé de chaque ligne du tableau HTML de la liste. */
export function parseShufersalListing(html: string): ShufersalFile[] {
  const files: ShufersalFile[] = [];
  for (const m of html.matchAll(/<a href="([^"]+\.gz[^"]*)"/g)) {
    // Dans le HTML, les « & » du lien sont écrits « &amp; ».
    const url = m[1].replace(/&amp;/g, "&");
    const name = url.match(/\/(PriceFull[^/?]+\.gz)\?/);
    if (name) files.push({name: name[1], url});
  }
  return files;
}

/**
 * Source de fichier pour Shufersal : liste des PriceFull du magasin de référence,
 * puis téléchargement du plus récent.
 *
 * Le site accepte le numéro de magasin (sans zéros) en filtre : une seule requête
 * suffit, au lieu de parcourir les 22 pages de la liste complète. Le tri du site
 * (`sort=Size`) est alphabétique et non numérique, on ne s'y fie donc pas.
 */
export function shufersalSource(
  chainCode: string,
  referenceStoreId: string,
): () => Promise<Buffer> {
  return async () => {
    const storeNumber = referenceStoreId.replace(/^0+/, "");
    const listRes = await fetch(
      `${BASE}/FileObject/UpdateCategory?catID=2&storeId=${storeNumber}&sort=Time&sortdir=DESC&page=1`,
      {signal: AbortSignal.timeout(TIMEOUT_MS)},
    );
    if (!listRes.ok) throw new Error(`SHUFERSAL_LIST_FAILED_${listRes.status}`);

    const files = parseShufersalListing(await listRes.text());
    const latest = pickLatestForStore(files.map((f) => f.name), chainCode, referenceStoreId);
    const file = files.find((f) => f.name === latest);
    if (!file) throw new Error("NO_PRICEFULL_FOR_REFERENCE_STORE");

    const res = await fetch(file.url, {signal: AbortSignal.timeout(TIMEOUT_MS)});
    if (!res.ok) throw new Error(`SHUFERSAL_DOWNLOAD_FAILED_${res.status}`);
    return Buffer.from(await res.arrayBuffer());
  };
}
