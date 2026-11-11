import {setGlobalOptions} from "firebase-functions";
import {onRequest, onCall} from "firebase-functions/https";
import {onSchedule} from "firebase-functions/scheduler";
import * as logger from "firebase-functions/logger";

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

// 3) Fonction planifiée : comme ingestPrices plus tard (tous les jours à 06:00)
export const dailyCheck = onSchedule(
  {schedule: "every day 06:00", timeZone: "Asia/Jerusalem"},
  async () => {
    logger.info("Il est 06:00 à Jérusalem.");
  }
);