// Páginas acessíveis sem sessão autenticada. Usado tanto por AuthGuard (bloqueio de
// rota) quanto por useAuth (decidir se tenta refresh de token) — mantido em um único
// lugar para as duas checagens nunca ficarem dessincronizadas entre si.
export const publicPages = ["/login"];
