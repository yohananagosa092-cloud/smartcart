import {Firestore} from "firebase-admin/firestore";
import {ChainId} from "../models";
import {ALL_CHAINS} from "./chains";
import {ChainConfig, ingestChain} from "./ingestChain";
import {WriteResult} from "./writeChain";

export interface ChainOutcome {
  chainId: ChainId;
  ok: boolean;
  /** Présent si la collecte a réussi. */
  result?: WriteResult;
  /** Présent si elle a échoué : le code d'erreur (aussi noté dans ingestionRuns). */
  error?: string;
}

/**
 * Collecte quotidienne : lance les enseignes l'une après l'autre. Une enseigne qui
 * échoue n'arrête pas les autres (exigence du cahier des charges, FR-14) ; son
 * échec est déjà journalisé dans ingestionRuns par ingestChain.
 *
 * L'enchaînement est volontairement séquentiel : plusieurs enseignes écrivent dans
 * les mêmes documents products/{barcode}, et la suppression d'un produit retiré
 * (voir writeChain.ts) ne doit pas croiser l'écriture d'une autre enseigne.
 */
export async function runIngestion(
  db: Firestore,
  chains: ChainConfig[] = ALL_CHAINS,
  ingest: typeof ingestChain = ingestChain,
): Promise<ChainOutcome[]> {
  const outcomes: ChainOutcome[] = [];
  for (const chain of chains) {
    try {
      outcomes.push({chainId: chain.chainId, ok: true, result: await ingest(db, chain)});
    } catch (err) {
      outcomes.push({
        chainId: chain.chainId,
        ok: false,
        error: err instanceof Error ? err.message : String(err),
      });
    }
  }
  return outcomes;
}
