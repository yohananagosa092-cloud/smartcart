import {FieldValue, Firestore} from "firebase-admin/firestore";
import {onDocumentWritten} from "firebase-functions/firestore";
import * as logger from "firebase-functions/logger";
import {adminDb} from "../admin";
import {COLLECTIONS} from "../models";

/** Code gRPC « document introuvable ». */
const NOT_FOUND = 5;

/**
 * Recalcule itemCount et updatedAt de la liste après une écriture sur un de ses
 * produits (ajout, changement de quantité ou de note, suppression).
 *
 * - On COMPTE les produits au lieu de faire +1 / -1 : Firebase peut livrer un même
 *   événement deux fois, et un compteur incrémenté se tromperait. Un recomptage donne
 *   toujours le bon résultat, même si un événement est rejoué ou arrive dans le désordre.
 * - On utilise update() et non set() : si la liste n'existe plus (suppression de la
 *   liste par deleteList, qui supprime aussi ses produits), on ne la recrée pas et on
 *   s'arrête sans erreur.
 *
 * Renvoie true si la liste a été mise à jour, false si elle n'existe plus.
 */
export async function refreshListCounters(db: Firestore, listId: string): Promise<boolean> {
  const listRef = db.collection(COLLECTIONS.shoppingLists).doc(listId);
  const total = (await listRef.collection(COLLECTIONS.items).count().get()).data().count;
  try {
    await listRef.update({itemCount: total, updatedAt: FieldValue.serverTimestamp()});
    return true;
  } catch (err) {
    if ((err as {code?: number}).code === NOT_FOUND) return false;
    throw err;
  }
}

/** Déclencheur : un produit de liste a été créé, modifié ou supprimé. */
export const onItemWritten = onDocumentWritten(
  "shoppingLists/{listId}/items/{itemId}",
  async (event) => {
    const updated = await refreshListCounters(adminDb(), event.params.listId);
    if (!updated) {
      logger.info("onItemWritten : liste introuvable, rien à faire", {listId: event.params.listId});
    }
  },
);
