/**
 * Client du site public https://shop.hazi-hinam.co.il/Prices (sans connexion).
 *
 * Comportement constaté le 22.11.2026 (différent de ce que supposait l'analyse du 17.11) :
 *  - les paramètres `d` (date) et `s` (recherche) sont ignorés : la page affiche
 *    toujours les fichiers du dernier jour publié, pour tous les magasins ;
 *  - la liste est paginée (`p`) : on lit les pages jusqu'à une page vide ;
 *  - un même magasin peut publier plusieurs PriceFull le même jour (ex. 103 et 219) ;
 *  - les liens de téléchargement sont directs, sans signature.
 *
 * Le fichier du magasin de référence peut manquer certains jours : dans ce cas
 * on échoue (NO_PRICEFULL_FOR_REFERENCE_STORE) et les prix existants sont conservés.
 */
import {pickLatestForStore} from "./priceFilename";

const BASE = "https://shop.hazi-hinam.co.il";
const TIMEOUT_MS = 60_000;
/** Garde-fou : la liste d'un jour tient aujourd'hui sur une seule page. */
const MAX_PAGES = 10;

export interface HatziHinamFile {
  /** Nom du fichier, ex. PriceFull7290700100008-000-204-20261121-002428.gz */
  name: string;
  url: string;
}

/** Extrait le nom et le lien de chaque fichier PriceFull de la page HTML. */
export function parseHatziHinamListing(html: string): HatziHinamFile[] {
  const files = new Map<string, HatziHinamFile>();
  for (const m of html.matchAll(/<a href="(https:\/\/[^"]+\/(PriceFull[^/"?]+\.gz))"/g)) {
    files.set(m[2], {name: m[2], url: m[1]});
  }
  return [...files.values()];
}

/** Source de fichier pour Hatzi Hinam : dernier PriceFull du magasin de référence. */
export function hatziHinamSource(
  chainCode: string,
  referenceStoreId: string,
): () => Promise<Buffer> {
  return async () => {
    const all: HatziHinamFile[] = [];
    for (let page = 1; page <= MAX_PAGES; page++) {
      // f=1 : fichiers complets ; t=1 : fichiers de prix.
      const res = await fetch(`${BASE}/Prices?p=${page}&f=1&t=1`, {
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) throw new Error(`HATZI_HINAM_LIST_FAILED_${res.status}`);
      const files = parseHatziHinamListing(await res.text());
      if (files.length === 0) break;
      all.push(...files);
    }

    const latest = pickLatestForStore(all.map((f) => f.name), chainCode, referenceStoreId);
    const file = all.find((f) => f.name === latest);
    if (!file) throw new Error("NO_PRICEFULL_FOR_REFERENCE_STORE");

    const res = await fetch(file.url, {signal: AbortSignal.timeout(TIMEOUT_MS)});
    if (!res.ok) throw new Error(`HATZI_HINAM_DOWNLOAD_FAILED_${res.status}`);
    return Buffer.from(await res.arrayBuffer());
  };
}
