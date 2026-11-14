/**
 * Firestore data model (task 7).
 * Source of truth for field names and types: docs/data-model.md.
 */
import type {Timestamp} from "firebase-admin/firestore";

// ---------- Constants ----------

export const CHAIN_IDS = [
  "ramiLevy",
  "shufersal",
  "yochananof",
  "chatziHinam",
  "osherAd",
] as const;
export type ChainId = (typeof CHAIN_IDS)[number];

export const COLLECTIONS = {
  users: "users",
  shoppingLists: "shoppingLists",
  items: "items",
  members: "members",
  invitations: "invitations",
  products: "products",
  chains: "chains",
  snapshot: "snapshot",
  comparisons: "comparisons",
  ingestionRuns: "ingestionRuns",
  mail: "mail",
} as const;

export const LIMITS = {
  displayNameMin: 2,
  displayNameMax: 40,
  listNameMin: 1,
  listNameMax: 40,
  quantityMin: 1,
  quantityMax: 999,
  budgetMin: 1,
  budgetMax: 10000,
  invitationTtlDays: 7,
  searchMinChars: 2,
  searchMaxResults: 20,
} as const;

/** Split-basket recommendation thresholds in ₪ (spec §3.4). */
export const RECOMMENDATION_THRESHOLDS = {
  recommendedMin: 15,
  neutralMin: 5,
} as const;

// ---------- users/{uid} ----------

export interface UserDoc {
  email: string; // lower-case
  displayName: string;
  createdAt: Timestamp;
  fcmTokens: string[];
  comparisonCount: number; // written by compareBasket only
}

// ---------- shoppingLists/{listId} ----------

export interface ShoppingListDoc {
  name: string;
  ownerUid: string;
  memberUids: string[]; // includes ownerUid
  budget: number | null; // whole ₪, 1–10,000
  itemCount: number; // written by onItemWritten
  createdAt: Timestamp;
  updatedAt: Timestamp;
  lastComparisonId: string | null;
  lastCheapestChain: ChainId | null;
  lastCheapestTotal: number | null;
}

// ---------- shoppingLists/{listId}/items/{itemId} ----------

export interface ItemDoc {
  barcode: string | null; // null for a free-text item
  name: string;
  quantity: number; // integer 1–999
  note: string; // "" when empty
  isFree: boolean;
  addedBy: string; // uid
  addedAt: Timestamp;
  updatedAt: Timestamp;
}

// ---------- shoppingLists/{listId}/members/{uid} ----------

export type MemberRole = "OWNER" | "EDITOR";

export interface MemberDoc {
  role: MemberRole;
  displayName: string;
  email: string;
  joinedAt: Timestamp;
}

// ---------- invitations/{id} ----------

export type InvitationStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED";

export interface InvitationDoc {
  listId: string;
  listName: string;
  itemCount: number;
  memberNames: string[];
  inviterUid: string;
  inviterName: string;
  inviteeEmail: string; // lower-case
  status: InvitationStatus;
  createdAt: Timestamp;
  expiresAt: Timestamp; // createdAt (or last resend) + 7 days
}

// ---------- products/{barcode} ----------

export interface ChainPrice {
  price: number; // regular unit price, ₪
  promoPrice: number | null;
  promoUntil: Timestamp | null;
  updatedAt: Timestamp;
}

export interface ProductDoc {
  name: string;
  brand: string;
  unitDescription: string; // e.g. "1 ליטר"
  searchPrefixes: string[];
  prices: Partial<Record<ChainId, ChainPrice>>;
}

// ---------- chains/{chainId} ----------

export type RunStatus = "SUCCESS" | "FAILED";

export interface ChainDoc {
  displayName: string;
  referenceStoreId: string;
  lastSuccessAt: Timestamp | null;
  lastStatus: RunStatus | null;
  itemCount: number;
}

/** chains/{chainId}/snapshot/latest */
export interface ChainSnapshotDoc {
  runId: string;
  /** JSON string of Record<barcode, PriceSnapshotEntry>. */
  priceMap: string;
}

export interface PriceSnapshotEntry {
  price: number;
  promoPrice: number | null;
  promoUntil: string | null; // ISO date
}

// ---------- comparisons/{id} ----------

export type ComparisonStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED";

/**
 * Progress step shown on screen 13:
 * 0 created, 1 list read, 2 prices fetched, 3 chain totals,
 * 4 split basket, 5 result saved.
 */
export type ComparisonStep = 0 | 1 | 2 | 3 | 4 | 5;

export type Recommendation = "RECOMMENDED" | "NEUTRAL" | "SINGLE_CHAIN";

export interface MissingItem {
  barcode: string;
  name: string;
  quantity: number;
  foundIn: ChainId[];
}

export interface PricedItem {
  barcode: string;
  name: string;
  quantity: number;
  unitPrice: number; // effective price (promo if valid)
  regularPrice: number;
  cost: number; // unitPrice × quantity
}

export interface SplitGroup {
  chain: ChainId;
  itemCount: number;
  subtotal: number;
  items: PricedItem[];
}

export interface ChainBreakdown {
  items: PricedItem[];
  subtotalRegular: number;
  promoDiscount: number;
  total: number;
}

export interface ComparisonResult {
  commonItems: number;
  missingItems: MissingItem[];
  totals: Partial<Record<ChainId, number>>;
  cheapestChain: ChainId;
  split: {
    total: number;
    diffFromCheapest: number;
    recommendation: Recommendation;
    groups: SplitGroup[];
  };
  breakdown: Partial<Record<ChainId, ChainBreakdown>>;
  pricesUpdatedAt: Partial<Record<ChainId, Timestamp>>;
}

export interface ComparisonDoc {
  listId: string;
  requestedBy: string;
  chains: ChainId[];
  status: ComparisonStatus;
  step: ComparisonStep;
  result: ComparisonResult | null;
  error: string | null; // error code when FAILED
  computeTimeMs: number | null;
  createdAt: Timestamp;
}

// ---------- ingestionRuns/{id} ----------

export interface IngestionRunDoc {
  chainId: ChainId;
  startedAt: Timestamp;
  finishedAt: Timestamp | null;
  status: RunStatus;
  itemCount: number;
  error: string | null;
}

// ---------- mail/{id} (Trigger Email extension) ----------

export interface MailDoc {
  to: string;
  message: {
    subject: string;
    html: string;
  };
}
