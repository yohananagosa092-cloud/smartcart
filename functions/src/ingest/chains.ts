import {ChainConfig} from "./ingestChain";
import {portalSource} from "./portalClient";

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
