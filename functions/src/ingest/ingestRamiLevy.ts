import {Firestore} from "firebase-admin/firestore";
import {RAMI_LEVY} from "./chains";
import {FileSource, ingestChain} from "./ingestChain";
import {WriteResult} from "./writeChain";

export {RAMI_LEVY};
export type {FileSource};

/** Collecte complète pour Rami Levy (voir ingestChain). */
export function ingestRamiLevy(db: Firestore, source?: FileSource): Promise<WriteResult> {
  return ingestChain(db, RAMI_LEVY, source);
}
