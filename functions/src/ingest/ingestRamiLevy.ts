import {Firestore, Timestamp} from "firebase-admin/firestore";
import {ChainId} from "../models";
import {PortalClient} from "./portalClient";
import {parsePriceFull} from "./parsePriceFull";
import {pickLatestForStore} from "./priceFilename";
import {recordFailure, writeChain, WriteResult} from "./writeChain";

export const RAMI_LEVY = {
  chainId: "ramiLevy" as ChainId,
  /** Identifiant de l'enseigne dans les noms de fichiers. */
  chainCode: "7290058140886",
  /** Magasin de référence (docs/price-files.md, tâche 13). */
  referenceStoreId: "044",
  /** Nom d'utilisateur du portail = nom de l'enseigne, sans mot de passe. */
  portalUser: "RamiLevi",
};

/** Source des octets du fichier : le portail en production, un fichier local en test. */
export type FileSource = () => Promise<Buffer>;

export async function downloadFromPortal(): Promise<Buffer> {
  const client = new PortalClient(RAMI_LEVY.portalUser);
  await client.login();
  const names = await client.listFiles(`PriceFull${RAMI_LEVY.chainCode}`);
  const latest = pickLatestForStore(names, RAMI_LEVY.chainCode, RAMI_LEVY.referenceStoreId);
  if (!latest) throw new Error("NO_PRICEFULL_FOR_REFERENCE_STORE");
  return client.download(latest);
}

/**
 * Collecte complète pour Rami Levy. En cas d'erreur, les prix existants ne sont
 * pas touchés : on journalise l'échec dans ingestionRuns puis on relance l'erreur.
 */
export async function ingestRamiLevy(
  db: Firestore,
  source: FileSource = downloadFromPortal,
): Promise<WriteResult> {
  const startedAt = Timestamp.now();
  try {
    const parsed = parsePriceFull(await source());
    if (parsed.items.length === 0) throw new Error("EMPTY_PRICE_FILE");
    return await writeChain(db, RAMI_LEVY.chainId, parsed.items, startedAt, {
      displayName: "רמי לוי",
      referenceStoreId: RAMI_LEVY.referenceStoreId,
    });
  } catch (err) {
    const code = err instanceof Error ? err.message : String(err);
    await recordFailure(db, RAMI_LEVY.chainId, startedAt, code);
    throw err;
  }
}
