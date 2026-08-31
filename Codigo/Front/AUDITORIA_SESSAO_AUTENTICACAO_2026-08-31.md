# Auditoria — Sessão, Autenticação, Expiração de Token e Hibernação do Railway (Frontend)

**Escopo desta rodada:** apenas o **frontend** (`Codigo/Front/`). Continuação da auditoria equivalente já feita no backend (`Codigo/Back/AUDITORIA_SESSAO_AUTENTICACAO_2026-08-31.md`) — leia aquele documento primeiro; este aqui assume o comportamento do backend já descrito lá (JWT stateless de 60 min via cookie `auth_token`, refresh token opaco de 30 dias via cookie `refresh_token`, `SecurityFilter` devolvendo 403 para token inválido/expirado e **503** — de propósito — para instabilidade de infraestrutura).

**Método:** leitura completa da cadeia de sessão no cliente (interceptor global de `fetch`, `AuthContext`, `AuthGuard`, `authService`, `RoleGuard`, layouts que os montam) e simulação mental dos mesmos cenários da rodada anterior: backend hibernado/reiniciando, token expirado, refresh token expirado, timeout, erro 5xx, erro 401/403, perda momentânea de conexão, múltiplas abas/requisições simultâneas, e permanência prolongada com o app aberto. Nenhuma alteração de código foi feita — auditoria somente leitura.

Este repositório já tem um `AUDITORIA.md` anterior (foco em RBAC/estrutura de páginas) com um item ainda aberto e diretamente relevante aqui — **`AuthGuard` não bloqueia render corretamente (item B-V1)** — referenciado na seção 1 abaixo em vez de duplicado.

---

## Resumo executivo

O achado mais importante desta rodada explica exatamente o sintoma que motivou a auditoria: **`authService.me()` e `authService.refresh()` tratam *qualquer* resposta não-2xx (401, 403, **429, 500, 503**) e *qualquer* falha de rede (timeout, conexão recusada, DNS) da mesma forma — como "usuário não autenticado".** O backend foi deliberadamente projetado para devolver **503** (não 403) quando o problema é de infraestrutura, exatamente para o frontend não confundir indisponibilidade com sessão inválida (ver `SecurityFilter.java:118-134` na auditoria do backend) — mas o frontend nunca olha o código de status além de `response.ok`, então esse cuidado do backend é descartado. Na prática, isso significa: **se o backend do Railway estiver hibernando, reiniciando, ou só lento nesse exato milissegundo em que o usuário navega de página**, o frontend conclui "sessão inválida" e redireciona para `/login` — mesmo com cookies 100% válidos.

Essa lógica existe **duplicada** em dois componentes independentes (`AuthContext.checkAuth` e `AuthGuard`), que rodam a cada troca de rota dentro do `(shell)` — ou seja, a superfície de exposição a esse bug é "toda navegação da aplicação autenticada", não um caso raro.

Em segundo lugar, o interceptor global de `fetch` reage a **qualquer** 403 (não só token expirado) tentando renovar a sessão — inclusive em respostas de "acesso negado por role" ou bloqueio de origem, que também usam 403 no backend. Isso soma tentativas de refresh desnecessárias, que aumentam a exposição à corrida de rotação de refresh token já documentada na auditoria do backend (P1).

---

## 1) Problemas encontrados, por gravidade

### 🔴 CRÍTICO
**F1 — `authService.me()`/`authService.refresh()` tratam indisponibilidade (5xx, timeout, erro de rede) como "não autenticado" e derrubam a sessão na tela**
Arquivos: `src/services/authService.ts:67-109`, `src/contexts/AuthContext.tsx:43-68`, `src/components/auth/AuthGuard.tsx:18-51`

### 🟠 ALTO
**F2 — O interceptor global trata qualquer 403 como "token expirado", inclusive 403 de autorização (role) ou de origem bloqueada, ampliando a exposição à corrida de refresh do backend**
Arquivo: `src/utils/fetchInterceptor.ts:39-65`

### 🟡 MÉDIO
**F3 — Duas implementações paralelas e não sincronizadas da mesma checagem de sessão (`AuthContext` e `AuthGuard`)**
Arquivos: `src/contexts/AuthContext.tsx:43-68`, `src/components/auth/AuthGuard.tsx:18-51`

**F4 (já conhecido, referenciado aqui por relevância direta) — `AuthGuard` não bloqueia a renderização do conteúdo protegido corretamente**
Já documentado como item aberto `[ ] B-V1` em `Codigo/Front/AUDITORIA.md:51,100-111`. Relevante para esta auditoria porque descreve exatamente o tipo de janela de estado inconsistente (`isAuthChecked` não resetado por navegação) que piora a percepção de "sessão se comportando de forma aleatória".

### 🟢 BAIXO / INFORMATIVO
**F5 — Reconexão do WebSocket de tempo real é um loop fixo de 3s sem backoff nem limite**
Arquivo: `src/hooks/useRealtimeSocket.ts:7,71-75`
Não derruba a sessão HTTP (é um canal separado, autenticado por ticket de uso único — ver auditoria do backend, seção sobre `RealtimeTicketService`), mas numa hibernação prolongada do Railway gera uma tentativa de reconexão a cada 3s indefinidamente.

**F6 — Nenhuma chamada HTTP no frontend define timeout explícito (`AbortController`) nem retry com backoff para requisições de dados comuns**
Arquivo: `src/utils/httpUtils.ts:7-12` (wrapper fino, sem timeout/retry) — confirmado como característica intencional já registrada no `AUDITORIA.md` anterior (linha 122), mas citado aqui porque é exatamente a peça que falta para o cliente "aguardar/repetir a requisição" durante um cold start do Railway em vez de só mostrar erro.

---

## 2) Causa provável de cada problema

### F1 — Indisponibilidade tratada como logout

```ts
// authService.ts:67-88
async me(): Promise<AuthUser | null> {
  ...
  const response = await fetch(`${API_BASE_URL}/auth/me`, { method: "GET", credentials: "include" });
  if (!response.ok) return null;      // 401, 403, 429, 500, 503 -> tudo vira "não logado"
  return await response.json();
  ...
  } catch { return null; }            // erro de rede/timeout -> também "não logado"
}

// authService.ts:90-109
async refresh(): Promise<boolean> {
  ...
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, { method: "POST", credentials: "include" });
  return response.ok;                 // mesmo problema: 503 vira "refresh falhou", igual a token realmente inválido
  ...
  } catch { return false; }
}
```

Essas duas funções são o único ponto de contato do frontend com "a sessão ainda é válida?". Nenhuma delas distingue:
- **401/403 de verdade** (token ausente, expirado, revogado) → correto tratar como não autenticado.
- **503** (o próprio backend devolve isso de propósito para não confundir indisponibilidade com sessão expirada — ver `SecurityFilter.java:118-134` na auditoria do backend) → deveria ser tratado como "tente de novo", nunca como logout.
- **429** (rate limit, inclusive no próprio `/auth/refresh` — ver P3 na auditoria do backend) → deveria ser tratado como "espere e tente de novo", não como logout.
- **Falha de rede/timeout** (Railway hibernando, TCP recusado, DNS, conexão caiu) → deveria ser tratado como "backend indisponível agora", nunca como logout.

Esse resultado (`null`/`false`) alimenta diretamente duas consumidoras:

```ts
// AuthContext.tsx:43-56
const checkAuth = useCallback(async () => {
  let user = await authService.me();
  if (!user && !publicPages.includes(pathname)) {
    const refreshed = await authService.refresh();
    if (refreshed) user = await authService.me();
  }
  setIsAuthenticated(!!user);   // indisponibilidade transitória vira isAuthenticated=false
  ...
```

```ts
// AuthGuard.tsx:21-38
let user = await authService.me();
if (!user && !isPublicPage) {
  const refreshed = await authService.refresh();
  if (refreshed) user = await authService.me();
}
const authenticated = !!user;
...
if (!authenticated && !isPublicPage) {
  router.push("/login");        // <- redireciona pro login por causa de um 503/timeout
}
```

`AuthGuard` roda esse efeito a **cada troca de `pathname`** (dependência do `useEffect`, linha 51) — ou seja, isso não é um caso extremo de "só na inicialização do app": é reavaliado em toda navegação dentro do `(shell)`. Qualquer soluço de rede, qualquer 503 do backend, qualquer cold start do Railway que aconteça bem no momento em que o `/auth/me` (e depois o `/auth/refresh`, também sujeito ao mesmo problema) é chamado durante uma navegação **força o usuário para a tela de login**, mesmo com o cookie de sessão perfeitamente válido no navegador.

Isso é, por evidência direta de código, a explicação mais provável para "uso normal da aplicação e, depois de um tempo aleatório, a sessão para de funcionar" — o "aleatório" é, na verdade, "toda vez que uma navegação coincidir com uma resposta não-2xx ou uma falha de rede do backend", o que inclui exatamente o cenário de hibernação do Railway descrito pelo usuário, mas **não se limita a ele** — qualquer 503/429/erro de rede eventual (rede do próprio usuário, timeout do proxy do Next em `next.config.ts`, etc.) dispara o mesmo efeito.

### F2 — Interceptor reage a qualquer 403

```ts
// fetchInterceptor.ts:45-64
window.fetch = async (input, init) => {
  const response = await originalFetch(input, init);
  const url = resolveUrl(input);
  if (!isApiRequest(url) || isAuthBypassRequest(url) || response.status !== 403) {
    return response;
  }
  const refreshed = await authService.refresh();
  if (!refreshed) { redirectToLogin(); return response; }
  return originalFetch(input, init);
};
```

O comentário do próprio arquivo (linha 35-37) já assume "403 = token expirado", mas no backend 403 também é devolvido para: acesso negado por role (`GlobalExceptionHandler.handleAccessDeniedException`), bloqueio de origem forjada (`SecurityFilter.isForgedCrossOriginRequest`), e reuso de refresh token detectado (P1 da auditoria do backend) — nenhum desses é "token expirado, renove por favor". O interceptor não tem como diferenciar esses casos só pelo status (o backend também não emite um contrato diferente para eles — ver P2 na auditoria do backend), então **toda vez que um usuário clica em algo que sua role não permite, o frontend dispara silenciosamente uma rotação de refresh token** — desperdício isolado é pequeno, mas cada rotação extra é mais uma chance de coincidir com outra rotação concorrente (outra aba, outro clique de 403) e acionar a corrida documentada como P1 no backend, que pode revogar todas as sessões do usuário.

Vale notar que o *dedup* dentro da mesma aba (`pendingRefreshRequest` em `authService.ts:35,91-108`) está correto e evita chamadas concorrentes de refresh **dentro do mesmo contexto JS** — mas duas abas do navegador são dois contextos JS separados, cada uma com seu próprio `pendingRefreshRequest`, e ambas compartilham os mesmos cookies. O dedup não protege contra esse caso, que é justamente o cenário descrito na auditoria do backend (P1).

### F3 — Duas implementações paralelas de checagem de sessão

`AuthContext.checkAuth` (`AuthContext.tsx:43-68`, montado uma vez no `layout.tsx` raiz) e `AuthGuard` (`AuthGuard.tsx:18-51`, montado no `layout.tsx` do `(shell)`) implementam **a mesma lógica** (`me()` → se falhar e não for página pública, `refresh()` → `me()` de novo) de forma independente, com estados (`isAuthenticated`) separados que nunca se sincronizam entre si. Quem efetivamente redireciona para `/login` é o `AuthGuard`; o `isAuthenticated` do `AuthContext` é consumido por `RoleGuard`/`hasRole` em outros lugares da árvore. Isso significa:
- O mesmo bug (F1) precisa ser corrigido **nos dois lugares** — corrigir só um e esquecer o outro deixa metade do sintoma vivo.
- É possível (embora não determinístico) haver uma janela em que `AuthContext.isAuthenticated` e o estado interno de `AuthGuard` divirjam brevemente durante uma navegação, já que cada um dispara sua própria chamada assíncrona a `/auth/me` de forma independente (mesmo com o dedup de promise reduzindo, mas não eliminando, essa janela).

### F4 — (referência) `AuthGuard` não bloqueia render corretamente

Já auditado e documentado em `Codigo/Front/AUDITORIA.md:100-111` como item `[ ] B-V1`, ainda não corrigido. Resumo: o gate de render depende só de `isAuthChecked` (nunca de `isAuthenticated`), e `isAuthChecked` não é resetado a `false` no início do efeito a cada troca de `pathname` — então, ao navegar dentro do `(shell)`, a página de destino pode renderizar por um instante com o estado de autenticação da rota anterior, antes da nova checagem terminar. Combinado com F1 (que pode virar `isAuthenticated=false` por causa de um 503 passageiro), esse é mais um motivo para a sessão "parecer" instável de forma intermitente e difícil de reproduzir manualmente.

### F5 e F6

Já descritos acima — não são causa do sintoma relatado (a sessão HTTP não depende do WebSocket, e a ausência de timeout/retry genérico é uma lacuna de robustez, não de autenticação), documentados por completude do "varredura completa" pedida.

---

## 3) Fluxo atual de autenticação e onde ele falha

```
1. Carregamento inicial / troca de rota
   RootLayout (layout.tsx) monta AuthProvider -> AuthContext.checkAuth() roda a cada `pathname`
   (shell)/layout.tsx monta AuthGuard -> efeito próprio roda a cada `pathname` (duplicado, F3)

   Ambos fazem: GET /auth/me
     -> !response.ok (qualquer não-2xx: 401/403/429/500/503) OU erro de rede/timeout
        => tratado como "user = null"                                [F1]
     -> se null e rota não é pública: POST /auth/refresh
          -> mesmo problema de !response.ok/erro de rede => "refreshed = false"   [F1]
     -> AuthGuard: se ainda não autenticado -> router.push("/login")  <- ponto de falha real

2. Requisição autenticada comum (qualquer service em src/services/*)
   window.fetch (patched por fetchInterceptor.ts) -> resposta 403?
     -> SIM (token expirado OU role negada OU origem bloqueada, backend não diferencia) [F2]
        -> authService.refresh() (dedup só dentro da mesma aba)
        -> sucesso: repete a requisição original uma vez
        -> falha: redirectToLogin()
     -> NÃO -> devolve a resposta normalmente (503/500/429/erro de rede seguem para
        o código de chamada tratar individualmente, sem lógica central)

3. Enquanto autenticado
   AuthGuard dispara authService.refresh() a cada 15 min (silent refresh) — ignora o
   resultado, só ajuda a manter o access token fresco durante uso contínuo.
```

**Onde ele falha, na ordem de probabilidade de causar o sintoma relatado:**

1. Passo 1 (F1): qualquer navegação que coincida com um `/auth/me` ou `/auth/refresh` respondendo não-2xx por motivo de indisponibilidade (não de autenticação) — incluindo o cold start do Railway — derruba a sessão na tela, mesmo com cookies válidos. **Este é o achado central desta auditoria de frontend.**
2. Passo 2 (F2): qualquer 403 de autorização (não de token) aciona refresh desnecessário, aumentando a chance de coincidir com outro refresh concorrente (de outra aba) e acionar a corrida P1 do backend.
3. F3/F4: não causam o problema sozinhos, mas tornam mais difícil confiar no `isAuthenticated` em qualquer momento específico, e distribuem a correção de F1 em dois lugares.

---

## 4) Como deveria funcionar o fluxo correto de recuperação automática

1. **`authService.me()` e `authService.refresh()` devem parar de colapsar tudo em `null`/`false`.** Precisam devolver informação suficiente para quem chama decidir corretamente:
   - `response.status === 401 || response.status === 403` → sessão realmente inválida (após o backend adotar 401 para esse caso especificamente — ver P2 na auditoria do backend, hoje ambos ainda usam 403 pra token expirado e pra autorização).
   - `response.status >= 500 || response.status === 429` → indisponibilidade/limite temporário → **não** tratar como logout; manter o estado de sessão anterior e sinalizar "tentar de novo em breve" para a UI, sem navegar para `/login`.
   - Exceção lançada pelo `fetch` (rede/timeout/DNS/Railway acordando) → mesmo tratamento do item acima: indisponibilidade, não logout.
2. **`AuthGuard` só deveria redirecionar para `/login` no primeiro caso** (401/403 real, ou refresh que devolveu 401/403 real). Nos demais casos, deveria manter a última tela conhecida, opcionalmente com um indicador de "reconectando..." e reintentar automaticamente com backoff, até obter uma resposta conclusiva (autenticado, não-autenticado, ou desistir após um número razoável de tentativas).
3. **Unificar a checagem de sessão num único lugar** (eliminar a duplicação `AuthContext`/`AuthGuard` — ver F3), para que a correção do item 1 acima não precise ser replicada e para que exista uma única fonte de verdade de `isAuthenticated` na árvore de componentes.
4. **Cold start do Railway**, do ponto de vista do frontend, deveria ser invisível ao usuário sempre que a sessão continuar válida: a requisição (de `/auth/me`, `/auth/refresh`, ou de qualquer chamada de API) deveria ser reintentada automaticamente (com backoff) enquanto a resposta for um erro de rede/timeout/5xx, e só desistir e mandar para o login diante de um 401/403 real. Hoje nada no frontend faz esse retry — nem para as chamadas de auth (F1), nem para chamadas de dados comuns (F6).

---

## 5) O que é Railway/hibernação vs. o que é bug da própria aplicação

| Item | Classificação | Evidência |
|---|---|---|
| F1 — indisponibilidade tratada como logout | **Bug da aplicação (frontend)** | Reproduz com qualquer 503/500/429/erro de rede simulado localmente (ex.: derrubando o backend por 1s durante uma navegação); nada específico do Railway, só mais fácil de notar lá por causa do cold start. |
| F2 — refresh disparado por 403 de autorização | **Bug/decisão de design da aplicação (frontend + contrato com o backend)** | Reproduz clicando em algo fora da role do usuário; sem relação com Railway. |
| F3 — duplicação de checagem de sessão | **Decisão de arquitetura da aplicação** | Está nos dois componentes React, nada de infraestrutura envolvido. |
| F4 (referência) — `AuthGuard` não bloqueia render | **Bug já documentado da aplicação** | Ver `AUDITORIA.md` anterior; reproduz em qualquer navegação interna do `(shell)`, sem depender de Railway. |
| F5 — reconexão do WS sem backoff | **Comportamento aceitável de resiliência**, agravado (não causado) por uma hibernação longa | Só gera mais tentativas de reconexão durante o período em que o backend está mesmo fora do ar — não é um bug de sessão. |
| F6 — sem timeout/retry genérico | **Lacuna de robustez da aplicação**, mais visível justamente durante cold start do Railway | Ausência confirmada em `httpUtils.ts`; é a peça que faltaria para o cliente "esperar e repetir" em vez de só falhar, exatamente como o usuário descreveu esperar. |
| "App desloga durante hibernação do Railway" | **Sintoma real, mas causa é o frontend (F1), não o Railway** | O Railway apenas cria a janela de tempo (resposta lenta/5xx/timeout) em que F1 se manifesta; o mesmo sintoma ocorre com qualquer instabilidade de rede, com ou sem Railway. |

**Conclusão desta seção:** assim como na auditoria do backend, **nenhum dos problemas encontrados é causado pelo Railway em si**. O Railway hibernando apenas fornece o gatilho mais fácil de reproduzir (uma janela garantida de latência/erro temporário) para um bug que já existe no frontend independentemente disso: tratar qualquer resposta não-2xx ou falha de rede como "sessão inválida".

---

## 6) Estratégia de correção priorizada

1. **[x] F1 (crítico, primeiro):** reescrever `authService.me()` e `authService.refresh()` para devolver um resultado que distinga "não autenticado" de "indisponível/erro de rede" (ex.: um tipo de retorno com três estados, ou lançar/propagar o erro de rede e o status HTTP em vez de engolir tudo em `null`/`false`). Atualizar `AuthContext.checkAuth` e `AuthGuard` para só redirecionar a `/login` no caso "não autenticado" de verdade, e implementar retry com backoff (poucas tentativas, ex.: 2-3, com espera crescente) para o caso "indisponível" antes de desistir. **Implementado em 2026-08-31**, junto com a mudança de contrato 401/403 do backend (P2): `authService.ts` ganhou o tipo `SessionCheckResult` (`"authenticated" | "unauthenticated" | "unavailable"`) — `/auth/me`/`/auth/refresh` só viram "unauthenticated" em 401 real ou 200-com-corpo-`null`; qualquer outro não-2xx ou exceção de rede vira "unavailable". `AuthContext.checkAuth` faz até 3 tentativas com backoff (2s/5s/10s) quando o resultado é "unavailable", sem nunca zerar `isAuthenticated` nesse caminho — só um "unauthenticated" de verdade limpa a sessão.
2. **[x] F3 (médio, junto com o item 1):** ao mexer no item 1, aproveitar para eliminar a duplicação entre `AuthContext` e `AuthGuard` — mover toda a lógica de checagem/redirecionamento para um único lugar (ex.: o próprio `AuthContext`, com `AuthGuard` apenas consumindo `isAuthenticated`/`isLoading` dele em vez de fazer sua própria chamada a `/auth/me`). **Implementado em 2026-08-31**: `AuthGuard.tsx` não chama mais `authService.me()`/`refresh()` — só lê `isAuthenticated`/`isLoading` de `useAuth()`. `AuthContext` é agora a única fonte de verdade e a única a chamar `/auth/me`/`/auth/refresh` (fora do refresh reativo do `fetchInterceptor`); o refresh silencioso de 15 min também migrou para lá. `src/app/page.tsx`, que também chamava `authService.me()` direto (formato antigo, quebrou o build ao mudar o tipo de retorno), foi ajustado para consumir `useAuth()` pelo mesmo motivo.
3. **[x] F4 (médio, aproveitar a mesma mexida):** corrigir o gate de render do `AuthGuard` (já especificado em `AUDITORIA.md:111`: condicionar o render também a `isAuthenticated`, e resetar o estado de "checagem concluída" a cada troca de `pathname`) — natural de resolver junto do item 2, já que ambos mexem no mesmo componente. **Implementado em 2026-08-31**: o gate agora é `if (isLoading ...) return null` seguido de `if (!isAuthenticated ...) return null`, e `AuthContext.checkAuth` chama `setIsLoading(true)` no início de toda checagem (inclusive a disparada por troca de `pathname`) — um contador de geração (`checkGeneration`) garante que uma checagem antiga, ainda em retry quando a rota já mudou, nunca aplique estado por cima do resultado da checagem mais nova.
4. **[x] F2 (alto, mas depende de uma mudança coordenada com o backend):** depois que o backend separar 401 (token) de 403 (autorização) — ver P2/item 2 da estratégia do backend —, ajustar `fetchInterceptor.ts` para só disparar refresh em 401, nunca em 403. Até essa mudança de contrato acontecer, considerar paliativamente inspecionar o corpo da resposta 403 antes de decidir se vale tentar refresh (mais frágil, mas reduz o dano enquanto o contrato de status não muda). **Implementado em 2026-08-31, junto com a mudança correspondente no backend** (`TokenException` agora devolve 401 — ver `Codigo/Back/AUDITORIA_SESSAO_AUTENTICACAO_2026-08-31.md`, item P2): `fetchInterceptor.ts` agora reage a `response.status === 401`, não mais a 403 — um 403 de role negada não dispara mais refresh desnecessário. Também passou a diferenciar o resultado do refresh: só redireciona pro login em "unauthenticated", nunca em "unavailable".
5. **[x] F6 (baixo, oportunista):** adicionar timeout (`AbortController`) e uma política simples de retry/backoff num wrapper central de fetch para chamadas de API comuns (não só auth), para que uma lentidão pontual do Railway não vire imediatamente um erro visível ao usuário. **Implementado parcialmente em 2026-08-31**: `fetchInterceptor.ts` (que já intercepta `window.fetch` globalmente) ganhou `fetchWithNetworkRetry`, que repete automaticamente qualquer requisição **GET** que falhe por exceção de rede (2 tentativas, 500ms/1500ms de backoff) antes de desistir. Deliberadamente **não** se aplica a POST/PUT/PATCH/DELETE (retry automático de uma escrita que já pode ter chegado ao servidor arriscaria duplicar o efeito colateral) e **não** adiciona `AbortController`/timeout — havia risco real de cortar endpoints legitimamente lentos já documentados no backend (ex.: emissão de NF-e aguardando ~2 min). Timeout explícito por endpoint fica como possível trabalho futuro, não feito aqui.
6. **[x] F5 (baixo, oportunista):** adicionar backoff crescente (com teto) na reconexão do `useRealtimeSocket`, para não martelar `/realtime/ws-ticket` a cada 3s indefinidamente durante uma indisponibilidade prolongada. **Implementado em 2026-08-31**: backoff exponencial (`RECONECTAR_BASE_MS=3000`, teto `RECONECTAR_MAX_MS=30000`), resetado a cada `onopen` bem-sucedido.

---

## 7) Testes para confirmar que a sessão sobrevive a inatividade, restart e falha temporária

**Testes de indisponibilidade vs. autenticação (validam a correção de F1 — os mais importantes):**
- Com um usuário autenticado (cookies válidos), simular o backend devolvendo 503 em `/auth/me` (ex.: interceptando a rede no DevTools ou derrubando o backend por alguns segundos) durante uma navegação dentro do `(shell)` → confirmar que a aplicação **não** redireciona para `/login` e, assim que o backend voltar a responder, a navegação segue normalmente sem exigir novo login.
- Repetir o teste acima simulando timeout/conexão recusada (em vez de 503) — mesmo critério de sucesso.
- Repetir o teste simulando 429 em `/auth/refresh` (ex.: disparando várias chamadas de refresh de propósito para estourar o rate limit do backend) → confirmar que a aplicação trata como "tente de novo em instantes", não como logout.
- Confirmar que um 401/403 **real** (cookie de sessão deliberadamente inválido/ausente) continua corretamente redirecionando para `/login` — a correção de F1 não pode enfraquecer esse caminho.

**Testes de concorrência entre abas (relacionados a F2 + à corrida P1 do backend):**
- Abrir a aplicação em duas abas com a mesma sessão, provocar em ambas, ao mesmo tempo, uma ação que dispare refresh (ex.: dois cliques quase simultâneos em algo que retorne 403, ou aguardar o token expirar com as duas abas ativas) → confirmar que nenhuma das duas sessões é derrubada como efeito colateral.
- Clicar em uma ação negada por role (403 de autorização, não de token) e confirmar, via Network do DevTools, que isso **não** dispara uma chamada a `/auth/refresh` (após a correção de F2) — hoje dispara.

**Testes do fluxo normal (garantir que nada quebrou):**
- Login → deixar o access token expirar (esperar ~60 min ou usar um valor baixo de `jwt.expiration-minutes` em ambiente de teste) → navegar para outra página → confirmar refresh automático e permanência logado, sem piscar para `/login`.
- Deixar a aba aberta e inativa por mais de 15 minutos (silent refresh) e depois interagir novamente → confirmar que nenhuma ação exige novo login enquanto o refresh token (30 dias) ainda for válido.
- Deixar o refresh token expirar de verdade (ou simular no backend) → confirmar que **aí sim** a aplicação redireciona para `/login` — esse é o único caso em que perder a sessão é o comportamento correto.

**Teste de cold start real do Railway (fim a fim, já com as correções acima aplicadas):**
- Provocar a hibernação do serviço no Railway (ou aguardar o ciclo natural de inatividade, se configurado) e então usar a aplicação já logada (com cookies válidos de uma sessão anterior) → confirmar que a primeira requisição após o cold start pode demorar (esperado), mas **não** desloga o usuário — a tela deve aguardar/reintentar até o backend responder, e só então prosseguir normalmente.

---

## Observação final

Com as duas rodadas (backend e frontend), o quadro completo é: **o backend tem uma corrida real de concorrência no rotation do refresh token (P1)**, e **o frontend tem um bug real de tratar indisponibilidade como logout (F1)** — os dois são candidatos concretos e independentes para o sintoma relatado, e ambos podem estar contribuindo simultaneamente (por exemplo: um 503 momentâneo do backend, causado por uma instabilidade qualquer de banco, já é suficiente para o frontend deslogar via F1, sem nem precisar da corrida P1 acontecer). Nenhum dos dois é causado pelo Railway "hibernar" em si — o Railway só é o cenário mais fácil de reproduzir os dois, porque garante uma janela de latência/erro temporário. Recomenda-se priorizar F1 (frontend) e P1 (backend) juntos, já que são independentes e qualquer um dos dois sozinho já explica o sintoma.
