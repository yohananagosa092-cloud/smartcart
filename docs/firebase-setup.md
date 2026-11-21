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
| Authentication | ספק Email/Password; הפעלה: Authentication ← Sign-in method (ראו הסעיף "התחברות" למטה) |
| Hosting | מוגדר ב-`firebase.json` (תיקייה `hosting/`); משמש ל-App Links של קישורי ההזמנה `/join/{token}` דרך `/.well-known/assetlinks.json`. **נשאר להשלים:** טביעת SHA-256 של האפליקציה בקובץ `assetlinks.json` |
| תוכנית Blaze | נדרשת לפריסת פונקציות ולפונקציות מתוזמנות |
| התראת תקציב | יש להגדיר ב-Google Cloud Billing |

## התחברות (Authentication)

כדי ליצור רשימה צריך לדעת מי המשתמשת, כדי לשייך אליה את הרשימה (`ownerUid`, `memberUids`) ולהגביל גישה בכללי האבטחה. לכן האפליקציה דורשת התחברות עם **דוא״ל וסיסמה** (מסכי הרשמה והתחברות, תהליך 6.1 באפיון), וה-`uid` שנוצר הוא מזהה המסמך ב-`users/{uid}` וב-`ownerUid` של הרשימות.

**הפעלה בפרויקט Firebase (נדרש בכל סביבה: dev ו-prod):**

1. במסוף Firebase: **Authentication** ← **Get started** (אם עוד לא הופעל).
2. **Sign-in method** ← **Email/Password** ← להפעיל (**Enable**) ← **Save**.
3. פונקציות callable דורשות משתמשת מחוברת; בלי התחברות הן מחזירות `unauthenticated` (ראו `docs/api-spec.md`).

לבדיקה מקומית האמולטור של Auth רץ על פורט 9099.

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
