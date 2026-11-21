import {setGlobalOptions} from "firebase-functions";
import {onRequest, onCall} from "firebase-functions/https";
import {onSchedule} from "firebase-functions/scheduler";
import * as logger from "firebase-functions/logger";
import {adminDb} from "./admin";
import {runIngestion} from "./ingest/ingestPrices";

// Même région que ta base Firestore
setGlobalOptions({maxInstances: 10, region: "europe-west1"});

// Tâche 28 : déclencheurs Firestore.
//  - onListCreated : crée l'entrée OWNER de la propriétaire dans members.
//  - onItemWritten : tient à jour itemCount et updatedAt de la liste.
export {onListCreated} from "./sharing/onListCreated";
export {onItemWritten} from "./lists/onItemWritten";

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

// Tâche 25 : collecte quotidienne des prix des cinq enseignes (06:00, heure de Jérusalem).
// Remplace ingestRamiLevyDaily (tâche 16). Les enseignes sont traitées l'une après l'autre ;
// une enseigne qui échoue n'arrête pas les autres. Le détail de chaque exécution est
// dans la collection ingestionRuns (visible dans la console Firebase).
export const ingestPrices = onSchedule(
  {
    schedule: "every day 06:00",
    timeZone: "Asia/Jerusalem",
    timeoutSeconds: 900,
    memory: "1GiB",
  },
  async () => {
    const outcomes = await runIngestion(adminDb());
    for (const o of outcomes) {
      if (o.ok) logger.info(`${o.chainId} : collecte terminée`, o.result);
      else logger.error(`${o.chainId} : collecte échouée`, {error: o.error});
    }
  }
);

// 3) Fonction planifiée d'apprentissage (tâche 1), sans effet sur les données
export const dailyCheck = onSchedule(
  {schedule: "every day 06:00", timeZone: "Asia/Jerusalem"},
  async () => {
    logger.info("Il est 06:00 à Jérusalem.");
  }
);