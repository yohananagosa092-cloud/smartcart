import {setGlobalOptions} from "firebase-functions";
import {onRequest, onCall} from "firebase-functions/https";
import {onSchedule} from "firebase-functions/scheduler";
import * as logger from "firebase-functions/logger";
import {getApps, initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {ingestRamiLevy} from "./ingest/ingestRamiLevy";

// Même région que ta base Firestore
setGlobalOptions({maxInstances: 10, region: "europe-west1"});

// 1) Fonction visible dans le navigateur (pour tester facilement)
export const hello = onRequest((request, response) => {
  logger.info("La fonction hello a été appelée");
  response.send("Bonjour Hodaia ! Ta première fonction marche.");
});

// 2) Fonction « à la demande » (callable) : comme compareBasket plus tard
export const greet = onCall((request) => {
  const name = request.data?.name ?? "toi";
  return {message: `Bonjour ${name} !`};
});

// Tâche 16 : collecte quotidienne des prix de Rami Levy (06:00, heure de Jérusalem)
export const ingestRamiLevyDaily = onSchedule(
  {
    schedule: "every day 06:00",
    timeZone: "Asia/Jerusalem",
    timeoutSeconds: 540,
    memory: "1GiB",
  },
  async () => {
    if (getApps().length === 0) initializeApp();
    const result = await ingestRamiLevy(getFirestore());
    logger.info("Rami Levy : collecte terminée", result);
  }
);

// 3) Fonction planifiée : comme ingestPrices plus tard (tous les jours à 06:00)
export const dailyCheck = onSchedule(
  {schedule: "every day 06:00", timeZone: "Asia/Jerusalem"},
  async () => {
    logger.info("Il est 06:00 à Jérusalem.");
  }
);