import {gunzipSync} from "node:zlib";

/** Un produit tel qu'il sort du fichier PriceFull, après filtrage. */
export interface ParsedItem {
  barcode: string;
  name: string;
  brand: string;
  unitDescription: string;
  price: number;
}

export interface ParseResult {
  storeId: string;
  items: ParsedItem[];
  /** Compteurs pour les logs / ingestionRuns. */
  stats: {total: number; kept: number; dropped: number};
}

const ENTITIES: Record<string, string> = {
  "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": "\"", "&apos;": "'",
};

function decode(text: string): string {
  return text
    .replace(/&(amp|lt|gt|quot|apos);/g, (m) => ENTITIES[m])
    .replace(/&#(\d+);/g, (_m, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_m, n) => String.fromCodePoint(parseInt(n, 16)))
    .trim();
}

function field(block: string, tag: string): string {
  const m = block.match(new RegExp("<" + tag + ">([^]*?)</" + tag + ">"));
  return m ? decode(m[1]) : "";
}

/**
 * Valeurs de remplissage au lieu d'un vrai fabricant : « לא ידוע » (Rami Levy),
 * « Unknown » (Yochananof), « --- » et « כללי » (Shufersal).
 */
const PLACEHOLDERS = new Set(["", "לא ידוע", "---", "כללי", "unknown"]);

function isUnknown(text: string): boolean {
  return PLACEHOLDERS.has(text.toLowerCase());
}

/** Accepte un Buffer .gz ou du XML déjà décompressé (string / Buffer). */
export function toXmlText(input: Buffer | string): string {
  if (typeof input === "string") return input;
  const raw = input.length > 2 && input[0] === 0x1f && input[1] === 0x8b ?
    gunzipSync(input) : input;
  let text = raw.toString("utf8");
  if (text.charCodeAt(0) === 0xfeff) text = text.slice(1); // BOM
  return text;
}

/**
 * Règles de filtrage (docs/price-files.md, tâche 13) :
 * ItemType = 1, bIsWeighted = 0, ItemCode composé uniquement de chiffres,
 * ItemPrice strictement positif. Les doublons de code-barres gardent la
 * première occurrence.
 */
export function parsePriceFull(input: Buffer | string): ParseResult {
  const xml = toXmlText(input);
  const storeId = field(xml, "StoreID");
  const blocks = xml.match(/<Item>[\s\S]*?<\/Item>/g) ?? [];
  const seen = new Set<string>();
  const items: ParsedItem[] = [];

  for (const block of blocks) {
    const barcode = field(block, "ItemCode");
    const price = Number(field(block, "ItemPrice"));
    if (field(block, "ItemType") !== "1") continue;
    if (field(block, "bIsWeighted") !== "0") continue;
    if (!/^\d+$/.test(barcode)) continue;
    if (!Number.isFinite(price) || price <= 0) continue;
    if (seen.has(barcode)) continue;
    seen.add(barcode);

    const qty = Number(field(block, "Quantity"));
    // Rami Levy écrit « לא ידוע » (inconnu) dans UnitQty : on prend alors UnitOfMeasure.
    const unitQty = field(block, "UnitQty");
    const unit = isUnknown(unitQty) ? field(block, "UnitOfMeasure") : unitQty;
    const unitDescription = qty > 0 && !isUnknown(unit) ? `${qty} ${unit}` : "";

    items.push({
      barcode,
      name: field(block, "ItemName"),
      brand: isUnknown(field(block, "ManufactureName")) ? "" : field(block, "ManufactureName"),
      unitDescription,
      price,
    });
  }

  return {
    storeId,
    items,
    stats: {total: blocks.length, kept: items.length, dropped: blocks.length - items.length},
  };
}
