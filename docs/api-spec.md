# מפרט ממשק התכנות (API) · משימה 10

מסמך זה מפרט לכל פונקציית ענן את הקלט, הפלט, השגיאות ותשובת דמה לדוגמה. הוא מבוסס על סעיף 14.1 במסמך האפיון, ועל [`data-model.md`](data-model.md) לשמות השדות. תשובות הדמה מיועדות למקור הנתונים המדומה באפליקציה (משימה 15), עד שהפונקציות יהיו מוכנות.

## מוסכמות

| נושא | מוסכמה |
|---|---|
| אזור | `europe-west1` (כמו מסד הנתונים) |
| פונקציות callable | נקראות מהאפליקציה דרך ה-SDK של Firebase, שמצרף את זהות המשתמשת. קריאה בלי משתמשת מחוברת נדחית בשגיאה `unauthenticated` |
| שגיאות | הפונקציה זורקת `HttpsError(code, message, details)`. קוד הסיבה מהטבלאות שלהלן מועבר ב-`details.reason` |
| סכומי כסף | `number` בשקלים, מעוגל לשתי ספרות; החישוב הפנימי באגורות (מספרים שלמים) |
| תאריכים בתשובה | מחרוזת ISO 8601 ב-UTC |
| בדיקת הרשאה | כל פונקציה בודקת בעצמה את ההרשאה (פונקציות הענן אינן כפופות לכללי האבטחה) |

### קודי שגיאה כלליים (`HttpsError.code`)

| קוד | מתי |
|---|---|
| `unauthenticated` | אין משתמשת מחוברת |
| `invalid-argument` | קלט חסר או שגוי |
| `permission-denied` | המשתמשת אינה בעלת הרשאה לפעולה |
| `not-found` | הרשימה, הקישור או המשתתפת לא נמצאו |
| `failed-precondition` | המצב הנוכחי אינו מאפשר את הפעולה |
| `already-exists` | הפעולה כבר בוצעה |

## סיכום הפונקציות

| פונקציה | סוג | קלט | פלט | שגיאות (`details.reason`) |
|---|---|---|---|---|
| `ingestPrices` | מתוזמנת, כל יום ב-06:00 (`Asia/Jerusalem`) | — | מעדכנת `products` ו-`chains`, רושמת ריצה ב-`ingestionRuns` | כישלון ברשת אחת אינו עוצר את האחרות |
| `onListCreated` | טריגר: יצירת רשימה | — | יוצרת `members/{ownerUid}` בתפקיד OWNER | — |
| `onItemWritten` | טריגר: שינוי פריט | — | מעדכנת `itemCount` ו-`updatedAt` ברשימה | — |
| `compareBasket` | callable | `listId`, `comparisonId` | `comparisonId`, `status` | `NOT_MEMBER`, `LIST_EMPTY`, `INVALID_BUDGET` |
| `createInviteLink` | callable | `listId` | `url` | `NOT_OWNER` |
| `joinList` | callable | `token` | `listId` | `LINK_NOT_FOUND`, `LINK_CANCELLED`, `ALREADY_MEMBER` |
| `cancelInviteLink` | callable | `listId` | `cancelled` | `NOT_OWNER` |
| `removeMember` | callable | `listId`, `memberUid` | `removed` | `NOT_OWNER`, `MEMBER_NOT_FOUND`, `CANNOT_REMOVE_OWNER` |
| `deleteList` | callable | `listId` | `deleted` | `NOT_OWNER` |
| `deleteAccount` | callable | — | `deleted` | `RECENT_LOGIN_REQUIRED` |

---

## `compareBasket`

מסכים 11–13. האפליקציה יוצרת קודם מסמך ב-`comparisons` בסטטוס `PENDING` (עם `mode` ו-`budget`), מתחילה להאזין לו, ואז קוראת לפונקציה.

**קלט**

```json
{ "listId": "L8f2k", "comparisonId": "C91xa" }
```

**בדיקות לפי הסדר**

| בדיקה | שגיאה | `HttpsError.code` |
|---|---|---|
| המסמך `comparisons/{comparisonId}` קיים, `requestedBy` הוא המשתמשת, `listId` זהה והסטטוס `PENDING` | `invalid-argument` | `invalid-argument` |
| המשתמשת ב-`memberUids` של הרשימה | `NOT_MEMBER` | `permission-denied` |
| יש ברשימה פריט אחד לפחות | `LIST_EMPTY` | `failed-precondition` |
| `mode` הוא `LIST` או `BUDGET`; בהשוואה לפי תקציב, `budget` מספר שלם 1–10,000 | `INVALID_BUDGET` | `invalid-argument` |

**התנהגות.** הפונקציה מעבירה את המסמך ל-`RUNNING` ומעדכנת את `step` במהלך החישוב: 1 קריאת הרשימה, 2 שליפת המחירים, 3 חישוב עלות הסל בכל רשת, 4 שמירת התוצאה. בסיום: `status = DONE`, `step = 5`, `result` ו-`computeTimeMs`. בנוסף היא מעדכנת ברשימה את `lastComparisonId`, `lastCheapestChain`, `lastCheapestTotal` ואת `lastBudget` (כאשר `mode = BUDGET`), ומגדילה ב-1 את `comparisonCount` של המשתמשת. בשגיאה בתוך החישוב: `status = FAILED` וקוד הסיבה בשדה `error` (שדה שנוסף למודל).

**אלגוריתם** (סעיף 3.4 באפיון)
1. הסל המשותף: הפריטים שנמצאו בקטלוג של כל חמש הרשתות. פריט החסר בלפחות רשת אחת נכנס ל-`missingItems` ואינו נספר בסכומים. רשת שאין בה מחירים כלל (כישלון קליטה) מסומנת כלא זמינה ואינה נספרת בסל המשותף.
2. `T(r) = Σ qᵢ · pᵢ,ᵣ` לכל רשת, באגורות.
3. הרשתות ממוינות מהזולה ליקרה. אם כמה רשתות בעלות אותו סכום מינימלי, כולן זולות ביותר.
4. בהשוואה לפי תקציב: `budgetLeft[r] = budget − T(r)`; ערך שלילי הוא חריגה.

**פלט** (חוזר מיד; התוצאה עצמה נכתבת במסמך)

```json
{ "comparisonId": "C91xa", "status": "DONE" }
```

**תשובת דמה: מסמך `comparisons/{id}` שהושלם** (השוואה לפי תקציב של 250 ₪; תואם מסכים 11 ו-13)

```json
{
  "listId": "L8f2k",
  "requestedBy": "uid_yiska",
  "mode": "BUDGET",
  "budget": 250,
  "status": "DONE",
  "step": 5,
  "result": {
    "commonItems": 14,
    "missingItems": [],
    "totals": { "ramiLevy": 234.50, "chatziHinam": 251.70, "shufersal": 254.90,
                "yochananof": 256.10, "osherAd": 258.30 },
    "cheapestChain": "ramiLevy",
    "budgetLeft": { "ramiLevy": 15.50, "chatziHinam": -1.70, "shufersal": -4.90,
                    "yochananof": -6.10, "osherAd": -8.30 },
    "breakdown": {
      "ramiLevy": {
        "items": [
          { "barcode": "7290000012345", "name": "תנובה חלב 3%", "quantity": 2, "unitPrice": 6.90, "cost": 13.80 }
        ],
        "total": 234.50
      }
    },
    "pricesUpdatedAt": { "ramiLevy": "2027-01-10T04:02:00Z" }
  },
  "error": null,
  "computeTimeMs": 1200,
  "createdAt": "2027-01-10T16:22:05Z"
}
```

בהשוואה לפי רשימת קניות (`mode = "LIST"`): `budget` ו-`result.budgetLeft` הם `null`.

**תשובת דמה: שגיאה**

```json
{ "code": "failed-precondition", "message": "The list has no items", "details": { "reason": "LIST_EMPTY" } }
```

---

## `createInviteLink`

מסך 10. נקראת כשבעלת הרשימה פותחת את מסך השיתוף או לוחצת "קישור חדש" (אחרי `cancelInviteLink`).

**קלט:** `{ "listId": "L8f2k" }`

**התנהגות**
- בודקת שהמשתמשת היא `ownerUid` של הרשימה, אחרת `NOT_OWNER` (`permission-denied`).
- אם לרשימה כבר יש קישור פעיל (`inviteToken` שאינו `null` והמסמך `inviteLinks/{token}` עם `active = true`), היא מחזירה אותו ואינה יוצרת חדש.
- אחרת היא יוצרת `token` אקראי (לפחות 12 תווים, מחולל קריפטוגרפי), כותבת `inviteLinks/{token}` עם `listId`, `listName`, `ownerName`, `itemCount`, `memberNames`, `createdAt`, `active = true`, ומעדכנת `shoppingLists/{listId}.inviteToken`.

**פלט / תשובת דמה**

```json
{ "url": "https://smartcart-app.web.app/join/8kP2vX" }
```

---

## `joinList`

מסך 15. נקראת כשהמשתמשת לוחצת "הצטרפי לרשימה". קריאת פרטי הרשימה להצגה במסך נעשית ישירות ממסמך `inviteLinks/{token}`, לפי כללי האבטחה.

**קלט:** `{ "token": "8kP2vX" }`

**בדיקות**

| מצב | שגיאה | `HttpsError.code` |
|---|---|---|
| אין מסמך `inviteLinks/{token}` | `LINK_NOT_FOUND` | `not-found` |
| `active = false` | `LINK_CANCELLED` | `failed-precondition` |
| המשתמשת כבר ב-`memberUids` | `ALREADY_MEMBER` | `already-exists` |

**התנהגות.** ב-batch אחד: הוספת ה-`uid` ל-`memberUids` של הרשימה, יצירת `members/{uid}` בתפקיד `EDITOR` (`displayName` ו-`email` מ-`users`, `joinedAt` כעת), ועדכון `memberNames` במסמך הקישור. באפליקציה, `ALREADY_MEMBER` פותחת את הרשימה ישירות במסך העריכה (סעיף 18.2).

**פלט / תשובת דמה:** `{ "listId": "L8f2k" }`

---

## `cancelInviteLink`

מסך 10. מבטלת את הקישור הקיים. משתתפות שכבר הצטרפו נשארות ברשימה.

**קלט:** `{ "listId": "L8f2k" }`

**התנהגות.** בודקת בעלות (`NOT_OWNER`). מעדכנת `inviteLinks/{token}.active = false` ו-`shoppingLists/{listId}.inviteToken = null`. אם אין קישור פעיל, מחזירה `cancelled: false` בלי שגיאה.

**פלט / תשובת דמה:** `{ "cancelled": true }`

---

## `removeMember`

מסך 17 (וגם מסך 10, ✕ ליד משתתפת).

**קלט:** `{ "listId": "L8f2k", "memberUid": "uid_ornelle" }`

**בדיקות**

| מצב | שגיאה | `HttpsError.code` |
|---|---|---|
| המשתמשת אינה בעלת הרשימה | `NOT_OWNER` | `permission-denied` |
| `memberUid` הוא הבעלת | `CANNOT_REMOVE_OWNER` | `failed-precondition` |
| `memberUid` אינו ב-`memberUids` | `MEMBER_NOT_FOUND` | `not-found` |

**התנהגות.** ב-batch אחד: הסרת ה-`uid` מ-`memberUids`, מחיקת `members/{memberUid}`, ועדכון `memberNames` במסמך הקישור הפעיל. משתתפת שצופה ברשימה באותו זמן מזהה זאת במאזין, והאפליקציה סוגרת את הרשימה (משימה 59).

**פלט / תשובת דמה:** `{ "removed": true }`

---

## `deleteList`

מסך 08, תפריט ⋮.

**קלט:** `{ "listId": "L8f2k" }`

**התנהגות.** בודקת בעלות (`NOT_OWNER`). מוחקת את תת-האוספים `items` ו-`members` (מחיקת מסמך ב-Firestore אינה מוחקת אותם), את מסמכי `comparisons` של הרשימה, מבטלת את הקישור הפעיל, ובסוף מוחקת את `shoppingLists/{listId}`. הרשימה נעלמת מהמסכים של כל המשתתפות.

**פלט / תשובת דמה:** `{ "deleted": true }`

---

## `deleteAccount`

מסך 18. האפליקציה מבקשת מהמשתמשת להזין שוב את הסיסמה (אימות מחדש) לפני הקריאה.

**קלט:** אין.

**בדיקות.** `auth_time` של האסימון בחמש הדקות האחרונות, אחרת `RECENT_LOGIN_REQUIRED` (`failed-precondition`).

**התנהגות.** מוחקת את כל הרשימות שבבעלות המשתמשת (כמו `deleteList`), מסירה אותה מרשימות משותפות (`memberUids` ו-`members`), מוחקת את מסמכי ה-`comparisons` שלה, את `users/{uid}`, ובסוף את החשבון ב-Firebase Authentication.

**פלט / תשובת דמה:** `{ "deleted": true }`

---

## טריגרים ופונקציה מתוזמנת

### `onListCreated`

מופעל ביצירת מסמך ב-`shoppingLists/{listId}`. יוצר את `members/{ownerUid}`: `role = OWNER`, `displayName` ו-`email` מ-`users/{ownerUid}`, `joinedAt` כעת.

### `onItemWritten`

מופעל בכל יצירה, שינוי או מחיקה של `shoppingLists/{listId}/items/{itemId}`. מחשב מחדש את `itemCount` ומעדכן את `updatedAt` ברשימה. העדכון הוא `update` ולא `set`: אם הרשימה אינה קיימת (נמחקה), הטריגר מסתיים בלי פעולה ואינו יוצר אותה מחדש.

### `ingestPrices`

פועלת כל יום ב-06:00 לפי `Asia/Jerusalem`. לכל אחת מחמש הרשתות: מורידה את קובץ המחירים של סניף הייחוס, מפענחת, משווה ל-`chains/{chainId}/snapshot/latest` וכותבת ל-`products` רק מוצרים שמחירם השתנה. מוצר שנעלם מהקובץ: נמחק ממנו `prices.{chainId}` (`FieldValue.delete()`). רושמת ריצה ב-`ingestionRuns` ומעדכנת `chains/{chainId}` (`lastSuccessAt`, `lastStatus`, `itemCount`). אם הקליטה נכשלה (למשל קובץ פגום): הסטטוס `FAILED`, המחירים הקיימים נשמרים, והרשתות האחרות ממשיכות.

---

## שאלות פתוחות לצוות

1. **הרשאת הקישור.** מסך 10 מאפשר לבחור בין "צפייה" ל"שינוי ועריכה", ו"לכל הרשאה נוצר קישור משלה", אבל במודל הנתונים (סעיף 14.3) אין שדה הרשאה במסמך הקישור, וסעיף 3.1 מגדיר רק את התפקידים OWNER ו-EDITOR. עד להחלטה, `joinList` מוסיפה תמיד בתפקיד `EDITOR`.
2. **התקדמות ההשוואה.** באפיון מוצגים ארבעה שלבים (מסך 12), ובדוגמה בסעיף 14.2 מופיע `step: 5`. המפרט מגדיר `0` נוצר, `1–4` השלבים, `5` הושלם.
3. **שדה `error` ו-`breakdown`.** שניהם אינם מופיעים בסעיף 14.3, והוספו: `error` לקוד השגיאה כאשר `FAILED`, ו-`breakdown` לפירוט הפריטים במסך 14.
