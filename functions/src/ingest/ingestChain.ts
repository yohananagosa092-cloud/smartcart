import {Firestore, Timestamp} from "firebase-admin/firestore";
import {ChainId} from "../models";
import {ParseResult, parsePriceFull} from "./parsePriceFull";
import {recordFailure, writeChain, WriteResult} from "./writeChain";

/** Source des octets du fichier : le site de l'enseigne en production, un fichier local en test. */
export type FileSource = () => Promise<Buffer>;

/** Tout ce qui est propre à une enseigne. Le reste de la collecte est commun. */
export interface ChainConfig {
  chainId: ChainId;
  /** Nom affiché de l'enseigne (hébreu). */
  displayName: string;
  /** Identifiant de l'enseigne dans les noms de fichiers. */
  chainCode: string;
  /** Magasin de référence (docs/price-files.md, tâche 13). */
  referenceStoreId: string;
  /** Télécharge le dernier PriceFull du magasin de référence. */
  download: FileSource;
  /**
   * Parseur propre à l'enseigne. Par défaut parsePriceFull (le format est le même
   * dans les cinq enseignes) ; une enseigne dont le format change peut fournir le sien
   * sans toucher aux autres (exigence de maintenance du cahier des charges, 3.8).
   */
  parse?: (input: Buffer | string) => ParseResult;
}

/**
 * Collecte complète d'une enseigne. En cas d'erreur, les prix existants ne sont
 * pas touchés : on journalise l'échec dans ingestionRuns puis on relance l'erreur.
 * Une enseigne qui échoue n'empêche pas les autres (l'appelant les lance séparément).
 */
export async function ingestChain(
  db: Firestore,
  chain: ChainConfig,
  source: FileSource = chain.download,
): Promise<WriteResult> {
  const startedAt = Timestamp.now();
  try {
    const parse = chain.parse ?? parsePriceFull;
    const parsed = parse(await source());
    if (parsed.items.length === 0) throw new Error("EMPTY_PRICE_FILE");
    return await writeChain(db, chain.chainId, parsed.items, startedAt, {
      displayName: chain.displayName,
      referenceStoreId: chain.referenceStoreId,
    });
  } catch (err) {
    const code = err instanceof Error ? err.message : String(err);
    await recordFailure(db, chain.chainId, startedAt, code);
    throw err;
  }
}
