# Auditoria de Qualidade e Segurança — Frontend Hortifruti SL

**Data:** 2026-08-06 (última limpeza: 2026-08-13)
**Escopo:** `Codigo/Front/src/**` (140 arquivos TS/TSX), `next.config.ts`, `package.json`, `biome.json`, `tsconfig.json`, `scripts/check-env.mjs`.
**Metodologia:** leitura completa (não apenas grep) de todo o código-fonte, dividida em 3 frentes paralelas: rotas/páginas (`app/`), componentes (`components/`), e a camada de dados (`services`, `hooks`, `types`, `utils`, `config`).
**Não avaliado:** backend (ver `Codigo/Back/AUDITORIA.md`), testes automatizados (o projeto não tem suíte de testes no escopo revisado), pipeline de CI (não há `.github/workflows` no escopo).

> **Convenção:** cada achado tem checkbox `- [ ]`, severidade, localização exata (`arquivo:linha`) e o impacto real. Achados já corrigidos foram removidos deste documento — ele lista só o que ainda falta.

---

## Sumário

1. [Resumo executivo](#1-resumo-executivo)
2. [Plano de ataque recomendado](#2-plano-de-ataque-recomendado)
3. [Área A — Rotas e páginas (`src/app`, `proxy.ts`)](#área-a--rotas-e-páginas)
4. [Área B — Componentes (`src/components`)](#área-b--componentes)
5. [Área C — Services, Hooks, Types, Utils, Config](#área-c--services-hooks-types-utils-config)
6. [Apêndice — cobertura de guarda de papel por rota](#6-apêndice--cobertura-de-guarda-de-papel-por-rota)

---

## 1. Resumo executivo

O frontend segue uma arquitetura documentada e coerente (App Router + route group `(shell)` com `AuthGuard`/`RoleGuard` + camada `services`/`hooks`), e a promessa mais crítica de segurança — **o JWT de sessão nunca é lido/decodificado/armazenado no cliente, só cookie `httpOnly`** — está genuinamente respeitada em todo o código auditado.

O problema estrutural mais sério que resta: **o controle de acesso por papel foi implementado no menu (`Sidebar.tsx`), mas não foi replicado de forma consistente dentro das próprias páginas.** Isso significa que, para boa parte das telas restritas a Gestor, um usuário Funcionário ou Contador que souber (ou adivinhar) a URL acessa o conteúdo completo — inclusive ações de escrita e cancelamento financeiro — mesmo não vendo o link no menu. Some-se a isso um bug real no `AuthGuard` (o componente que decide "há sessão válida?") que pode deixar conteúdo protegido renderizar antes/sem confirmar a autenticação, e uma rota de debug esquecida em produção sem proteção nenhuma.

**Contagem aproximada de achados restantes: ~77**, distribuídos como:

| Severidade | Qtde. aprox. | Onde estão os mais graves |
|---|---|---|
| 🔴 Crítico | **2** | Guarda de rota ausente (`/dev/teste-nota`), `AuthGuard` não bloqueia render |
| 🟠 Alto | **~4** | Componentes com `fetch` cru embutido, `comercio/boletos` monolítico |
| 🟡 Médio | **~33** | Padrão sistêmico de guarda só no menu, componentes pulando a camada de hook |
| 🔵 Baixo | **~38** | Código morto, nomenclatura, ausência de CI rodando lint/typecheck |

### Os 2 achados críticos, em uma frase cada

1. **`/dev/teste-nota` está acessível em produção sem login algum** — fica fora do route group protegido, e o próprio comentário no topo do arquivo diz "REMOVER ANTES DO MERGE FINAL". ([Área A, item A-V1](#a-v1))
2. **`AuthGuard` decide se renderiza o conteúdo protegido olhando só `isAuthChecked`, nunca `isAuthenticated`** — e como o componente vive no layout persistente do `(shell)` sem resetar esse estado a cada navegação, existe uma janela real em que a tela renderiza com o estado de sessão da rota anterior. ([Área B, item B-V1](#b-v1))

O problema real está concentrado em **guarda de acesso inconsistente entre menu e rota**.

---

## 2. Plano de ataque recomendado

### Onda 1 — Correções pontuais, baixo risco, alto impacto (1 a 3 dias)
- [ ] **A-V1** — Remover (ou proteger com `AuthGuard` + flag de ambiente) a rota `src/app/dev/teste-nota`.
- [x] **A-V2** — Envolver o conteúdo principal de `src/app/(shell)/lancamentos/page.tsx` com `<RoleGuard roles="MANAGER">`.
- [ ] **B-V1** — Corrigir `AuthGuard.tsx`: condicionar `return <>{children}</>` também a `isAuthenticated`, e resetar `isAuthChecked` para `false` no início do `useEffect` a cada troca de `pathname`.
- [x] **C-V1** — Remover os `catch` que retornam sucesso fictício em `userAdminService.createUser`/`deleteUser`; propagar o erro real para a UI.
- [x] **C-V2** — Remover o fallback "Backup simulado realizado com sucesso!" em `userAdminService.performBackup`/`restoreBackup`; propagar falha real.
- [x] **A-V3** — Adicionar `RoleGuard` faltante em `/comercio/recomendacoes`.

### Onda 2 — Fechar o padrão sistêmico de guarda (3 a 5 dias)
- [ ] Aplicar `RoleGuard` de forma consistente em todas as rotas marcadas ❌ na [tabela do apêndice](#6-apêndice--cobertura-de-guarda-de-papel-por-rota) — idealmente extraindo um wrapper único (`<RestrictedPage roles={...}>`) usado no `layout.tsx` de cada seção, para eliminar a chance desse padrão se repetir no futuro.

### Onda 3 — Estrutural, por módulo (1 a 2 semanas)
- [ ] Quebrar `comercio/boletos/page.tsx` (1959 linhas) em 3 componentes de página (uma por aba) + hook/service genérico parametrizado por tipo de cobrança.

### Onda 4 — Débito técnico contínuo (backlog, sem urgência)
Tudo marcado 🟡/🔵 nas seções abaixo: CSP `unsafe-inline` em `style-src`, "criptografia" client-side teatral do rascunho de notificações.

---

## Área A — Rotas e páginas

**Escopo analisado:** todas as rotas de `src/app` (públicas e o route group `(shell)`), `src/proxy.ts` (middleware de CSP/segurança), `next.config.ts`.

> Confirmado: nenhum `dangerouslySetInnerHTML`, `eval`, segredo sem prefixo `NEXT_PUBLIC_` exposto ao cliente, ou wildcard em CSP em todo o escopo `app/`.

### A · Vulnerabilidades

<a id="a-v1"></a>
- [ ] 🔴 **[A-V1] `/dev/teste-nota` acessível em produção sem nenhuma autenticação**
  **Local:** `src/app/dev/teste-nota/page.tsx:1-48`
  ```
  // ⚠️ PÁGINA TEMPORÁRIA DE DEV — REMOVER ANTES DO MERGE FINAL (ver Etapa 7 da spec de...)
  ```
  A rota vive fora do route group `(shell)` (não existe `src/app/dev/layout.tsx`), então nunca passa pelo `AuthGuard`. Nada em `next.config.ts` ou no build exclui essa pasta de produção; não há checagem de `NODE_ENV`. Qualquer pessoa não autenticada abre a página e vê a fila de notas pendentes em tempo real de **todos os usuários** (`NotasPendentesFila`, WebSocket + fetch para `/api/compras/notas/pendentes`) — exposição de rota de debug e de dados operacionais/de clientes em produção.

### A · Acoplamento e baixa coesão

- [ ] 🟡 **[A-A1] `fetch()` direto em páginas, ignorando a camada de `services`**
  **Locais:** `src/app/dev/teste-nota/page.tsx:100-107,208-215`, `src/app/(shell)/comercio/capturar-nota/page.tsx:25-29`, `src/app/dispositivo/vincular/page.tsx` — montam `FormData`, chamam `fetch` com URL manual e duplicam a mesma lógica de "extrair mensagem de erro" (`extrairMensagemErro`). Mudança de contrato de API ainda exige tocar em múltiplos arquivos de página em vez de 1.

- [ ] 🟠 **[A-A2] `/comercio/boletos/page.tsx` concentra estado e lógica de 3 telas completamente diferentes em um único componente de 1959 linhas**
  **Local:** `src/app/(shell)/comercio/boletos/page.tsx` — mistura aba "Boletos em Aberto" (seleção em massa, pagar/baixar/cancelar), "Consultar por Cliente" e "NF sem Boleto". Funções como `executeMarkAsPaid`/`executeConfirmInvoicePayment` e `executeCancel`/`executeCancelInvoices` são pares quase idênticos duplicados para boleto vs. nota fiscal (ex.: linhas 723-773 vs. 437-487). Qualquer bug fix precisa ser replicado manualmente nos dois fluxos.

- [ ] 🟡 **[A-A3] Lógica de negócio pesada dentro da página em vez de hooks/services**
  **Local:** `src/app/(shell)/lancamentos/page.tsx:263-356` (`handleGenerateExtratos`) — orquestra diretamente 2 chamadas de API em paralelo (`Promise.allSettled`), monta resultados por banco e decide side-effects, tudo fora de `useTransaction` (que já existe e é usado no resto do arquivo).

- [ ] 🔵 **[A-A4] Guardas de papel ad-hoc repetidas em vez de um wrapper de seção reaproveitável**
  **Local:** `src/app/(shell)/dashboard/page.tsx:53,62,78,115,124` — 5 usos de `<RoleGuard roles={["MANAGER"]}>` no mesmo arquivo. Um componente `GestorOnly` reduziria a chance de alguém esquecer de aplicá-lo.

---

## Área B — Componentes

**Escopo analisado:** `src/components/auth`, `layout`, `ui`, `forms`, `modals`, `modules` (incl. `modules/tables`), `landing`, `img`.

### B · Vulnerabilidades

<a id="b-v1"></a>
- [ ] 🔴 **[B-V1] `AuthGuard` não bloqueia a renderização de conteúdo protegido quando a checagem falha**
  **Local:** `src/components/auth/AuthGuard.tsx:44-67`
  ```tsx
  setIsAuthenticated(authenticated);
  setIsAuthChecked(true);
  ...
  if (!isAuthChecked && !publicPages.includes(pathname)) { return null; }
  return <>{children}</>;
  ```
  O `return` só depende de `isAuthChecked` — `isAuthenticated` nunca é checado. Quando a sessão é inválida, `router.push("/login")` é chamado, mas como a navegação do Next.js é assíncrona, `children` já renderiza no mesmo ciclo. Pior: `AuthGuard` envolve o `layout.tsx` do `(shell)` (`src/app/(shell)/layout.tsx:16-43`), que não é remontado em navegações internas (SPA) — e `isAuthChecked` não é resetado para `false` no início do `useEffect` a cada troca de `pathname`. Ou seja, ao trocar de rota dentro do shell, o valor **anterior** de `isAuthChecked` permanece `true` durante toda a janela em que a nova checagem de `/auth/me` está em voo, e a página de destino renderiza imediatamente com o estado de autenticação desatualizado.
  **Mitigante:** a proteção de dados de fato depende do backend (cookies `httpOnly` + 401/403 tratados por `fetchInterceptor.ts:39-64`) — não há vazamento de dados de API neste cenário, mas o chrome da UI (menus, nomes de rotas, estrutura de páginas) pode aparecer para um usuário não autenticado, contrariando o propósito documentado do componente.
  **Correção esperada:** condicionar `return <>{children}</>` também a `isAuthenticated || isPublicPage`, e resetar `isAuthChecked` para `false` a cada mudança de `pathname`.

- [ ] 🔵 **[B-V4] Validação de upload é responsabilidade só do client** *(informativo)*
  **Local:** `src/components/modules/EnhancedUploadNotes.tsx:25-41`, `src/hooks/useUpload.ts` — arquitetura correta (componente→hook→service), mas a tela assume implicitamente que tipo/tamanho também são reforçados no backend. Checagem client-side é trivial de contornar.

---

## Área C — Services, Hooks, Types, Utils, Config

**Escopo analisado:** `src/services/**`, `src/hooks/**`, `src/types/**`, `src/utils/**`, `src/config/api.ts`, `scripts/check-env.mjs`, `package.json`, `biome.json`, `tsconfig.json`.

> **Confirmação positiva mais importante desta auditoria:** nenhum resquício de JWT/token de sessão lido, decodificado ou armazenado manualmente pelo frontend. `httpUtils.ts` é hoje exatamente o "wrapper fino" descrito no README, e todas as ~40 chamadas `fetch` em `services/**` usam `credentials: "include"`. A promessa arquitetural central está sendo respeitada.

### C · Configuração de build/qualidade

- [ ] 🔵 **[C-CFG1] Nenhum workflow de CI encontrado no escopo rodando `check-types`/`lint`/`npm audit` em PRs** *(informativo — fora do escopo estrito de arquivos revisados, sinalizado para confirmação externa)*

---

## 6. Apêndice — cobertura de guarda de papel por rota

Comparação entre a tabela de papéis do `README.md` / declaração em `Sidebar.tsx` e a proteção real encontrada dentro de cada página:

| Rota | Papel esperado | Guarda aplicada na página? |
|---|---|---|
| `/dashboard` | Gestor | ✅ `RoleGuard roles={["MANAGER"]}` |
| `/lancamentos` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/comercio/clientes` (+novo, +editar/[id]) | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` em todas |
| `/comercio/compras` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/comercio/boletos` | Gestor, Funcionário | ❌ **sem guarda** — [A-V4](#a-v4) |
| `/comercio/frete` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/comercio/recomendacoes` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/comercio/nota-fiscal-xml` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/comercio/capturar-nota` | Gestor, Funcionário (agora documentada no README) | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/notificacoes` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/acesso` (+novo, +editar/[id]) | Gestor | ✅ `RoleGuard roles="MANAGER"` em todas |
| `/admin` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/backup` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/perfil` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/dev/teste-nota` | (não deveria existir) | ❌ **fora do `AuthGuard`, sem guarda nenhuma** — [A-V1](#a-v1) |
