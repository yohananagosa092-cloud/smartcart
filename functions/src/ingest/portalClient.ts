/**
 * Client du portail partagé https://url.publishedprices.co.il
 * (Rami Levy, Yochananof, Osher Ad). Voir docs/price-files.md.
 *
 * Le nom d'utilisateur est le nom de l'enseigne et il n'y a pas de mot de passe.
 */
import {pickLatestForStore} from "./priceFilename";

const BASE = "https://url.publishedprices.co.il";

export class PortalClient {
  private cookies = new Map<string, string>();

  constructor(private readonly username: string) {}

  private cookieHeader(): string {
    return [...this.cookies].map(([k, v]) => `${k}=${v}`).join("; ");
  }

  private storeCookies(res: Response): void {
    for (const line of res.headers.getSetCookie()) {
      const [pair] = line.split(";");
      const eq = pair.indexOf("=");
      if (eq > 0) this.cookies.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
    }
  }

  private async request(path: string, init: RequestInit = {}): Promise<Response> {
    const res = await fetch(BASE + path, {
      ...init,
      redirect: "manual",
      headers: {...(init.headers as Record<string, string>), Cookie: this.cookieHeader()},
    });
    this.storeCookies(res);
    return res;
  }

  private static csrfFrom(html: string): string {
    const m = html.match(/<meta name="csrftoken" content="([^"]+)"/);
    if (!m) throw new Error("PORTAL_CSRF_NOT_FOUND");
    return m[1];
  }

  async login(): Promise<void> {
    const page = await this.request("/login");
    const csrf = PortalClient.csrfFrom(await page.text());
    const res = await this.request("/login/user", {
      method: "POST",
      headers: {"Content-Type": "application/x-www-form-urlencoded"},
      body: new URLSearchParams({username: this.username, password: "", csrftoken: csrf}),
    });
    const location = res.headers.get("location") ?? "";
    if (res.status !== 302 || !location.includes("/file")) throw new Error("PORTAL_LOGIN_FAILED");
  }

  /** Liste les fichiers dont le nom contient `search` (ex. "PriceFull7290058140886-001-044"). */
  async listFiles(search: string): Promise<string[]> {
    const page = await this.request("/file");
    const csrf = PortalClient.csrfFrom(await page.text());
    const res = await this.request("/file/json/dir", {
      method: "POST",
      headers: {"Content-Type": "application/x-www-form-urlencoded"},
      body: new URLSearchParams({
        sEcho: "1", iDisplayStart: "0", iDisplayLength: "100000", sSearch: search, csrftoken: csrf,
      }),
    });
    if (!res.ok) throw new Error(`PORTAL_LIST_FAILED_${res.status}`);
    const json = (await res.json()) as {aaData?: {name: string}[]};
    return (json.aaData ?? []).map((f) => f.name);
  }

  async download(name: string): Promise<Buffer> {
    const res = await this.request(`/file/d/${encodeURIComponent(name)}`);
    if (!res.ok) throw new Error(`PORTAL_DOWNLOAD_FAILED_${res.status}`);
    return Buffer.from(await res.arrayBuffer());
  }
}

/**
 * Source de fichier pour une enseigne du portail : connexion, liste des PriceFull
 * de l'enseigne, puis téléchargement du plus récent du magasin de référence.
 */
export function portalSource(
  portalUser: string,
  chainCode: string,
  referenceStoreId: string,
): () => Promise<Buffer> {
  return async () => {
    const client = new PortalClient(portalUser);
    await client.login();
    const names = await client.listFiles(`PriceFull${chainCode}`);
    const latest = pickLatestForStore(names, chainCode, referenceStoreId);
    if (!latest) throw new Error("NO_PRICEFULL_FOR_REFERENCE_STORE");
    return client.download(latest);
  };
}
