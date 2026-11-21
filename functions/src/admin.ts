import {getApp, initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {getFirestore} from "firebase-admin/firestore";

/**
 * Initialise l'application Admin par défaut, une seule fois.
 * On teste l'application PAR DÉFAUT (getApp lève une erreur si elle n'existe pas) et
 * non getApps().length : l'émulateur de fonctions crée lui-même une application
 * interne, et getApps() serait alors non vide sans que l'application par défaut existe.
 */
function ensureApp(): void {
  try {
    getApp();
  } catch {
    initializeApp();
  }
}

export function adminDb() {
  ensureApp();
  return getFirestore();
}

export function adminAuth() {
  ensureApp();
  return getAuth();
}
