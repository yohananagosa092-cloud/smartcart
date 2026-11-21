import {Auth} from "firebase-admin/auth";
import {Firestore, Timestamp} from "firebase-admin/firestore";
import {onDocumentCreated} from "firebase-functions/firestore";
import * as logger from "firebase-functions/logger";
import {adminAuth, adminDb} from "../admin";
import {COLLECTIONS, MemberDoc} from "../models";

/** Code gRPC « le document existe déjà ». */
const ALREADY_EXISTS = 6;

/**
 * Crée l'entrée de la propriétaire dans shoppingLists/{listId}/members/{ownerUid}
 * (rôle OWNER). L'application n'a pas le droit d'écrire dans members : c'est cette
 * fonction qui le fait (règles de sécurité, tâche 28).
 *
 * - Le nom vient du profil users/{uid}, sinon d'Authentication.
 * - L'adresse e-mail vient d'Authentication (la source sûre), sinon du profil.
 * - Rejouer la fonction ne change rien : si l'entrée existe, on ne la touche pas
 *   (Firebase peut livrer un même événement plusieurs fois).
 *
 * Renvoie true si l'entrée a été créée, false si elle existait déjà.
 */
export async function createOwnerMember(
  db: Firestore,
  auth: Auth,
  listId: string,
  ownerUid: string,
): Promise<boolean> {
  const profile = await db.collection(COLLECTIONS.users).doc(ownerUid).get();
  let displayName: string = profile.get("displayName") ?? "";
  let email = "";
  try {
    const account = await auth.getUser(ownerUid);
    email = (account.email ?? "").toLowerCase();
    if (!displayName) displayName = account.displayName ?? "";
  } catch {
    // Compte introuvable dans Authentication : on se rabat sur le profil.
  }
  if (!email) email = profile.get("email") ?? "";

  const member: MemberDoc = {role: "OWNER", displayName, email, joinedAt: Timestamp.now()};
  try {
    await db
      .collection(COLLECTIONS.shoppingLists)
      .doc(listId)
      .collection(COLLECTIONS.members)
      .doc(ownerUid)
      .create(member);
    return true;
  } catch (err) {
    if ((err as {code?: number}).code === ALREADY_EXISTS) return false;
    throw err;
  }
}

/** Déclencheur : une liste vient d'être créée. */
export const onListCreated = onDocumentCreated("shoppingLists/{listId}", async (event) => {
  const ownerUid = event.data?.get("ownerUid");
  if (typeof ownerUid !== "string" || ownerUid === "") {
    logger.warn("onListCreated : liste sans ownerUid", {listId: event.params.listId});
    return;
  }
  const created = await createOwnerMember(adminDb(), adminAuth(), event.params.listId, ownerUid);
  logger.info("onListCreated", {listId: event.params.listId, ownerUid, created});
});
