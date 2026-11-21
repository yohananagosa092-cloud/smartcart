import {hatziHinamSource} from "./hatziHinamClient";
import {ChainConfig} from "./ingestChain";
import {portalSource} from "./portalClient";
import {shufersalSource} from "./shufersalClient";

/**
 * Configuration de chaque enseigne : identifiants et magasin de référence
 * (docs/price-files.md, tâche 13) et manière de télécharger le fichier.
 * Ajouter une enseigne = ajouter une entrée ici.
 */

/** Portail partagé url.publishedprices.co.il ; l'identifiant est le nom de l'enseigne, sans mot de passe. */
export const RAMI_LEVY: ChainConfig = {
  chainId: "ramiLevy",
  displayName: "רמי לוי",
  chainCode: "7290058140886",
  referenceStoreId: "044",
  download: portalSource("RamiLevi", "7290058140886", "044"),
};

export const YOCHANANOF: ChainConfig = {
  chainId: "yochananof",
  displayName: "יוחננוף",
  chainCode: "7290803800003",
  referenceStoreId: "017",
  download: portalSource("yohananof", "7290803800003", "017"),
};

export const OSHER_AD: ChainConfig = {
  chainId: "osherAd",
  displayName: "אושר עד",
  chainCode: "7290103152017",
  referenceStoreId: "026",
  download: portalSource("osherad", "7290103152017", "026"),
};

/** Site public shop.hazi-hinam.co.il, sans connexion. */
export const HATZI_HINAM: ChainConfig = {
  chainId: "chatziHinam",
  displayName: "חצי חינם",
  chainCode: "7290700100008",
  referenceStoreId: "204",
  download: hatziHinamSource("7290700100008", "204"),
};

/** Site public prices.shufersal.co.il, sans connexion. */
export const SHUFERSAL: ChainConfig = {
  chainId: "shufersal",
  displayName: "שופרסל",
  chainCode: "7290027600007",
  referenceStoreId: "413",
  download: shufersalSource("7290027600007", "413"),
};
