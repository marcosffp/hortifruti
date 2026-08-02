const LAST_REFRESH_KEY = "auth:lastRefreshAt";
const REFRESH_LOCK_KEY = "auth:refreshLock";
const LOCK_TTL_MS = 10_000;

/**
 * Coordena a renovação silenciosa do token entre abas via localStorage: como o refresh token é de
 * uso único no backend, duas abas chamando /auth/refresh quase ao mesmo tempo fariam uma delas
 * reapresentar um token já rotacionado, o que o backend trata como reuso/vazamento e revoga todas
 * as sessões do usuário.
 */

export function markRefreshed(timestamp = Date.now()) {
  try {
    localStorage.setItem(LAST_REFRESH_KEY, String(timestamp));
  } catch {
    // localStorage indisponível (modo privado, etc.) — segue sem coordenação entre abas
  }
}

export function getLastRefreshedAt(): number {
  try {
    return Number(localStorage.getItem(LAST_REFRESH_KEY)) || 0;
  } catch {
    return 0;
  }
}

export function tryAcquireRefreshLock(): boolean {
  try {
    const now = Date.now();
    const lockedAt = Number(localStorage.getItem(REFRESH_LOCK_KEY)) || 0;
    if (now - lockedAt < LOCK_TTL_MS) return false;
    localStorage.setItem(REFRESH_LOCK_KEY, String(now));
    return true;
  } catch {
    return true;
  }
}
