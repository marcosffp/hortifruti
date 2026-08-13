import type { NotificacoesDraft } from "@/types/notificacoesTypes";

const DRAFT_STORAGE_KEY = "notificacoes:rascunho";
const DRAFT_TTL_MS = 30 * 60 * 1000;

export function loadDraft(): NotificacoesDraft | null {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) return null;

    const parsed = JSON.parse(raw);

    if (Date.now() - parsed.savedAt > DRAFT_TTL_MS) {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      return null;
    }

    return parsed;
  } catch {
    return null;
  }
}

export function saveDraft(draft: NotificacoesDraft) {
  try {
    sessionStorage.setItem(
      DRAFT_STORAGE_KEY,
      JSON.stringify({ ...draft, savedAt: Date.now() }),
    );
  } catch {
    // sessionStorage indisponível (ex.: modo privado) — rascunho não é essencial, ignora
  }
}

export function clearDraft() {
  try {
    sessionStorage.removeItem(DRAFT_STORAGE_KEY);
  } catch {}
}
