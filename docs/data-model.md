# מודל הנתונים (משימה 7)

מסמך זה מפרט את אוספי Firestore, השדות, הטיפוסים, מי כותבת כל שדה והאינדקסים.
הוא מבוסס על פרקים 5.2 ו-14 (כולל 14.3) בגרסה הסופית של מסמך האפיון. הטיפוסים המקבילים ב-TypeScript נמצאים ב-[`functions/src/models.ts`](../functions/src/models.ts).

## מוסכמות

| נושא | מוסכמה |
|---|---|
| תאריכים | `Timestamp` של Firestore (בצד השרת `FieldValue.serverTimestamp()`) |
| סכומי כסף | `number` בשקלים, מעוגל לשתי ספרות. בתוך `compareBasket` החישוב נעשה באגורות (מספרים שלמים) כדי למנוע שגיאות עיגול |
| כתובות דוא״ל | נשמרות באותיות קטנות (`toLowerCase()`) |
| ערך חסר | `null` (לא שדה חסר), כדי שהאפליקציה תמיד תקבל את אותו מבנה |
| מזהי רשתות | `ramiLevy`, `shufersal`, `yochananof`, `chatziHinam`, `osherAd` |

## מבנה האוספים

```
users/{uid}
shoppingLists/{listId}
  ├── items/{itemId}
  └── members/{uid}
inviteLinks/{token}
products/{barcode}
chains/{chainId}
  └── snapshot/latest
comparisons/{comparisonId}
ingestionRuns/{runId}
```

---

## `users/{uid}`

מזהה המסמך = `uid` של Firebase Authentication. נוצר על ידי האפליקציה לאחר אימות כתובת הדוא״ל (תהליך 6.1, שלב 5).

| שדה | טיפוס | כותבת | הערות |
|---|---|---|---|
| `email` | string | אפליקציה | באותיות קטנות |
| `displayName` | string | אפליקציה | 2–40 תווים |
| `createdAt` | Timestamp | אפליקציה | |
| `comparisonCount` | number | `compareBasket` בלבד | מתחיל ב-0; `FieldValue.increment(1)` |

## `shoppingLists/{listId}`

מזהה אוטומטי. נוצר על ידי האפליקציה (מסך 06).

| שדה | טיפוס | כותבת | הערות |
|---|---|---|---|
| `name` | string | אפליקציה (בעלת הרשימה) | 1–40 תווים |
| `ownerUid` | string | אפליקציה ביצירה בלבד | אינו ניתן לשינוי |
| `memberUids` | string[] | אפליקציה ביצירה (`[ownerUid]`); אחר כך פונקציות בלבד | כולל את בעלת הרשימה |
| `lastBudget` | number \| null | אפליקציה (בעלת הרשימה) | מספר שלם 1–10,000; התקציב האחרון שנעשה בו שימוש בהשוואה; `null` = אין |
| `inviteToken` | string \| null | אפליקציה / פונקציות | האסימון של מסמך `inviteLinks` הפעיל של הרשימה |
| `itemCount` | number | `onItemWritten` | 0 ביצירה |
| `createdAt` | Timestamp | אפליקציה | |
| `updatedAt` | Timestamp | אפליקציה, `onItemWritten` | משמש למיון (אינדקסים) |
| `lastComparisonId` | string \| null | `compareBasket` | |
| `lastCheapestChain` | ChainId \| null | `compareBasket` | מוצג במסכים 05, 09 |
| `lastCheapestTotal` | number \| null | `compareBasket` | |

**נתונים נגזרים במסך הבית (מסך 05):** "שיצרתי" = רשימות עם `ownerUid == uid`; "שיתפתי" = מתוכן, `memberUids.length > 1`; "פרטיות" = מתוכן, `memberUids.length == 1`.

## `shoppingLists/{listId}/items/{itemId}`

מזהה אוטומטי. נכתב על ידי כל חברה ברשימה.

| שדה | טיפוס | הערות |
|---|---|---|
| `barcode` | string | תמיד קיים |
| `name` | string | מועתק מ-`products` בהוספה מהקטלוג |
| `quantity` | number | מספר שלם 1–999 (נאכף גם בכללי האבטחה) |
| `note` | string | `""` כשאין הערה |
| `addedBy` | string | `uid` של המוסיפה |
| `addedAt` | Timestamp | |
| `updatedAt` | Timestamp | |

הוספת מוצר שכבר ברשימה (מסך 07) מגדילה את `quantity` של הפריט הקיים במקום ליצור פריט חדש. האפליקציה מחפשת פריט עם אותו `barcode`.

## `shoppingLists/{listId}/members/{uid}`

מזהה המסמך = `uid` של המשתתפת. **נכתב בפונקציות בלבד**: `onListCreated` (OWNER), פונקציית הצטרפות דרך קישור (EDITOR), `removeMember` (מחיקה).

| שדה | טיפוס | הערות |
|---|---|---|
| `role` | `"OWNER"` \| `"EDITOR"` | |
| `displayName` | string | מועתק מ-`users` |
| `email` | string | |
| `joinedAt` | Timestamp | |

> `memberUids` ברשימה משמש לכללי האבטחה ולשאילתות; `members` משמש להצגת שמות ותפקידים (מסכים 08, 10, 19). שתי הפונקציות מעדכנות את שניהם ב-batch אחד.

## `inviteLinks/{token}`

מזהה המסמך = אסימון הקישור. **נכתב בפונקציות בלבד.** מחליף את ההזמנות בדוא״ל.

| שדה | טיפוס | הערות |
|---|---|---|
| `listId` | string | |
| `listName` | string | עותק: המצטרפת אינה יכולה לקרוא את הרשימה לפני ההצטרפות |
| `ownerName` | string | |
| `itemCount` | number | עותק |
| `memberNames` | string[] | עותק |
| `createdAt` | Timestamp | |
| `active` | boolean | `false` = הקישור בוטל |

## `products/{barcode}`

מזהה המסמך = הברקוד. **נכתב על ידי `ingestPrices` בלבד.**

| שדה | טיפוס | הערות |
|---|---|---|
| `name` | string | |
| `brand` | string | יצרן |
| `unitDescription` | string | לדוגמה "1 ליטר" |
| `searchPrefixes` | string[] | תחיליות של כל מילה בשם, מאורך 2 ועד אורך המילה, באותיות קטנות. לדוגמה "חלב תנובה" → `חל`, `חלב`, `תנ`, `תנו`, `תנוב`, `תנובה` |
| `prices` | map | מפתח = `chainId` |
| `prices.{chainId}.price` | number | מחיר ליחידה |
| `prices.{chainId}.updatedAt` | Timestamp | |

מוצר שהוסר מקובץ הרשת: נמחק השדה `prices.{chainId}` (`FieldValue.delete()`).

## `chains/{chainId}`

**נכתב על ידי `ingestPrices` בלבד.** חמישה מסמכים קבועים.

| שדה | טיפוס | הערות |
|---|---|---|
| `displayName` | string | "רמי לוי" וכו׳ |
| `referenceStoreId` | string | סניף הייחוס (ייקבע במשימה 13) |
| `lastSuccessAt` | Timestamp \| null | מוצג במסכים 12, 16; אזהרה אם עברו יותר מ-48 שעות |
| `lastStatus` | `SUCCESS` \| `FAILED` \| null | |
| `itemCount` | number | מספר המוצרים בקליטה האחרונה |

### `chains/{chainId}/snapshot/latest`

תמונת המחירים מהריצה הקודמת, כדי לכתוב רק מוצרים שהשתנו.

| שדה | טיפוס | הערות |
|---|---|---|
| `runId` | string | |
| `priceMap` | string | JSON של `{ [barcode]: { price } }`. אם יתקרב ל-1 MiB, יפוצל ל-`latest_1`, `latest_2`... |

## `comparisons/{comparisonId}`

מזהה אוטומטי. האפליקציה יוצרת בסטטוס `PENDING`; `compareBasket` מעדכנת את השאר.

| שדה | טיפוס | הערות |
|---|---|---|
| `listId` | string | |
| `requestedBy` | string | `uid` |
| `mode` | `LIST` \| `BUDGET` | |
| `budget` | number \| null | חובה כאשר `mode = BUDGET` |
| `status` | `PENDING` \| `RUNNING` \| `DONE` \| `FAILED` | |
| `step` | number 0–4 | 0 נוצר · 1 קריאת הרשימה · 2 שליפת המחירים · 3 עלות בכל רשת · 4 שמירת התוצאה (מסך 13) |
| `result` | map \| null | ראו למטה |
| `error` | string \| null | קוד שגיאה כאשר `FAILED` (*תוספת למסמך האפיון*) |
| `computeTimeMs` | number \| null | מדד הצלחה: עד 10 שניות ל-50 פריטים |
| `createdAt` | Timestamp | |

### מבנה `result`

```jsonc
{
  "commonItems": 14,
  "missingItems": [
    { "barcode": "729...", "name": "...", "quantity": 1, "foundIn": ["ramiLevy", "shufersal"] }
  ],
  "totals": { "ramiLevy": 234.50, "chatziHinam": 251.70 },
  "cheapestChain": "ramiLevy",
  "budgetLeft": 15.50,                          // רק כאשר mode = BUDGET, אחרת null
  "breakdown": {                                // תוספת למסמך האפיון, עבור מסכים 11 ו-16
    "ramiLevy": { "items": [ /* PricedItem */ ], "total": 234.50 }
  },
  "pricesUpdatedAt": { "ramiLevy": "<Timestamp>" }
}
```

`PricedItem` = `{ barcode, name, quantity, unitPrice, cost }`, כאשר `cost = unitPrice × quantity`.

> **למה `breakdown`?** מסך 16 מציג לכל רשת את מחיר היחידה והסכום, ומסך 11 מציג את שלושת הפריטים היקרים. הוספת הפירוט לתוצאה חוסכת לאפליקציה קריאה של כל מסמכי המוצרים (ל-50 פריטים ו-5 רשתות זה כ-250 שורות, הרבה מתחת למגבלת 1 MiB).

## `ingestionRuns/{runId}`

**נכתב על ידי `ingestPrices` בלבד.** מסמך לכל רשת בכל ריצה.

| שדה | טיפוס |
|---|---|
| `chainId` | ChainId |
| `startedAt` | Timestamp |
| `finishedAt` | Timestamp \| null |
| `status` | `SUCCESS` \| `FAILED` |
| `itemCount` | number |
| `error` | string \| null |

---

## הרשאות כתיבה (סיכום)

| אוסף | קריאה מהאפליקציה | כתיבה מהאפליקציה |
|---|---|---|
| `users/{uid}` | המשתמשת עצמה | המשתמשת עצמה, למעט `comparisonCount` |
| `shoppingLists` | חברות ב-`memberUids` | יצירה; עדכון `name`, `budget` על ידי הבעלים בלבד |
| `…/items` | חברות | חברות; `quantity` 1–999 |
| `…/members` | חברות | אסורה |
| `inviteLinks` | אסורה (נקרא דרך פונקציה) | אסורה |
| `products`, `chains` | כל משתמשת מחוברת | אסורה |
| `comparisons` | `requestedBy` | יצירה בלבד (`PENDING`, חברה ברשימה) |
| `ingestionRuns` | אסורה | אסורה |

הכללים עצמם ייכתבו במשימות 28 ו-60.

## אינדקסים

מוגדרים ב-[`firestore.indexes.json`](../firestore.indexes.json).

| אוסף | שדות | שאילתה | מסכים |
|---|---|---|---|
| `shoppingLists` | `memberUids` (array-contains), `updatedAt` ↓ | רשימות שהמשתמשת חברה בהן | 05, 18 |
| `shoppingLists` | `ownerUid`, `updatedAt` ↓ | הרשימות שהמשתמשת יצרה | 09 |
| `products` | `searchPrefixes` (array-contains) | חיפוש מוצרים | 07; אינדקס שדה בודד, נוצר אוטומטית |

## שאילתות לדוגמה (Kotlin)

```kotlin
// רשימות שהמשתמשת חברה בהן (מסכים 05, 18)
db.collection("shoppingLists")
  .whereArrayContains("memberUids", uid)
  .orderBy("updatedAt", Query.Direction.DESCENDING)

// הרשימות שהמשתמשת יצרה (מסך 09)
db.collection("shoppingLists")
  .whereEqualTo("ownerUid", uid)
  .orderBy("updatedAt", Query.Direction.DESCENDING)

// חיפוש מוצרים (מסך 07)
db.collection("products")
  .whereArrayContains("searchPrefixes", query.lowercase())
  .limit(20)
```
