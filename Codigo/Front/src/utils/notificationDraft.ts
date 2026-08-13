import type { NotificacoesDraft } from "@/types/notificacoesTypes";

const DRAFT_STORAGE_KEY = "notificacoes:rascunho";
const DRAFT_TTL_MS = 30 * 60 * 1000;
const DRAFT_KEY_MATERIAL = "hortifruti-sl-notificacoes-draft-v1";
const DRAFT_SALT = new TextEncoder().encode("hortifruti-sl-notificacoes-salt");

async function getDraftCryptoKey(): Promise<CryptoKey> {
  const baseKey = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(DRAFT_KEY_MATERIAL),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt: DRAFT_SALT, iterations: 100_000, hash: "SHA-256" },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

function bytesToBase64(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes));
}

function base64ToBytes(base64: string): Uint8Array<ArrayBuffer> {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

export async function loadDraft(): Promise<NotificacoesDraft | null> {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) return null;

    const { iv, data } = JSON.parse(raw);
    const key = await getDraftCryptoKey();
    const plaintext = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: base64ToBytes(iv) },
      key,
      base64ToBytes(data),
    );
    const parsed = JSON.parse(new TextDecoder().decode(plaintext));

    if (Date.now() - parsed.savedAt > DRAFT_TTL_MS) {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      return null;
    }

    return parsed;
  } catch {
    return null;
  }
}

export async function saveDraft(draft: NotificacoesDraft) {
  try {
    const key = await getDraftCryptoKey();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const plaintext = new TextEncoder().encode(
      JSON.stringify({ ...draft, savedAt: Date.now() }),
    );
    const ciphertext = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      plaintext,
    );

    sessionStorage.setItem(
      DRAFT_STORAGE_KEY,
      JSON.stringify({
        iv: bytesToBase64(iv),
        data: bytesToBase64(new Uint8Array(ciphertext)),
      }),
    );
  } catch {
    // sessionStorage/WebCrypto indisponível (ex.: modo privado) — rascunho não é essencial, ignora
  }
}

export function clearDraft() {
  try {
    sessionStorage.removeItem(DRAFT_STORAGE_KEY);
  } catch {}
}
