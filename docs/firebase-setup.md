# הגדרת פרויקט Firebase (משימה 4)

## סביבות

| סביבה | כינוי ב-`.firebaserc` | מזהה פרויקט | מצב |
|---|---|---|---|
| dev | `dev` (וגם `default`) | `smartcart-3410a` | פעיל |
| prod | `prod` | `smartcart-prod-4b382` | נוצר (Spark); מעבר ל-Blaze לפני הפריסה ל-prod (משימה 73) |

מעבר בין סביבות: `firebase use dev` או `firebase use prod`.

## שירותים בפרויקט dev

| שירות | מצב |
|---|---|
| Cloud Firestore | מסד `(default)`, מהדורה STANDARD, מיקום `europe-west1` |
| Cloud Functions | אזור `europe-west1` (זהה למיקום Firestore), Node.js 24, TypeScript |
| Authentication | יש להפעיל את ספק Email/Password במסוף |
| Hosting | מוגדר ב-`firebase.json` (תיקייה `hosting/`); משמש ל-App Links של קישורי ההזמנה `/join/{token}` דרך `/.well-known/assetlinks.json`. **נשאר להשלים:** טביעת SHA-256 של האפליקציה בקובץ `assetlinks.json` |
| תוכנית Blaze | נדרשת לפריסת פונקציות ולפונקציות מתוזמנות |
| התראת תקציב | יש להגדיר ב-Google Cloud Billing |

## אפליקציית האנדרואיד

יש לרשום את אפליקציית האנדרואיד בפרויקט (Project settings → Add app → Android) ולהוריד את `google-services.json`.
הקובץ **אינו** מועלה למאגר (מופיע ב-`.gitignore`); כל חברת צוות שומרת עותק מקומי ב-`app/`.

## הרצה מקומית (אמולטור)

```bash
cd functions
npm install
npm run build
cd ..
firebase emulators:start
```

| אמולטור | פורט |
|---|---|
| Auth | 9099 |
| Functions | 5001 |
| Firestore | 8080 |
| Emulator UI | ברירת מחדל (4000) |
