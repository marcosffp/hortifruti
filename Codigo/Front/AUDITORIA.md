# Auditoria de Qualidade e Segurança — Frontend Hortifruti SL

**Data:** 2026-08-06
**Escopo:** `Codigo/Front/src/**` (140 arquivos TS/TSX), `next.config.ts`, `package.json`, `biome.json`, `tsconfig.json`, `scripts/check-env.mjs`.
**Metodologia:** leitura completa (não apenas grep) de todo o código-fonte, dividida em 3 frentes paralelas: rotas/páginas (`app/`), componentes (`components/`), e a camada de dados (`services`, `hooks`, `types`, `utils`, `config`). Nenhuma correção foi aplicada — este documento é só diagnóstico.
**Não avaliado:** backend (ver `Codigo/Back/AUDITORIA.md`), testes automatizados (o projeto não tem suíte de testes no escopo revisado), pipeline de CI (não há `.github/workflows` no escopo).

> **Convenção:** cada achado tem checkbox `- [ ]`, severidade, localização exata (`arquivo:linha`) e o impacto real.

---

## Sumário

1. [Resumo executivo](#1-resumo-executivo)
2. [Plano de ataque recomendado](#2-plano-de-ataque-recomendado)
3. [Área A — Rotas e páginas (`src/app`, `proxy.ts`)](#área-a--rotas-e-páginas)
4. [Área B — Componentes (`src/components`)](#área-b--componentes)
5. [Área C — Services, Hooks, Types, Utils, Config](#área-c--services-hooks-types-utils-config)
6. [Apêndice — cobertura de guarda de papel por rota](#6-apêndice--cobertura-de-guarda-de-papel-por-rota)
7. [O que já está bom (não mexer)](#7-o-que-já-está-bom-não-mexer)

---

## 1. Resumo executivo

O frontend segue uma arquitetura documentada e coerente (App Router + route group `(shell)` com `AuthGuard`/`RoleGuard` + camada `services`/`hooks`), e a promessa mais crítica de segurança — **o JWT de sessão nunca é lido/decodificado/armazenado no cliente, só cookie `httpOnly`** — está genuinamente respeitada em todo o código auditado. `tsconfig.json` está em modo `strict`, não há `any`/`as any` no escopo revisado, e `npm audit` não acusa vulnerabilidade conhecida.

Dito isso, a auditoria encontrou um problema estrutural sério: **o controle de acesso por papel foi implementado no menu (`Sidebar.tsx`), mas não foi replicado de forma consistente dentro das próprias páginas.** Isso significa que, para boa parte das telas restritas a Gestor, um usuário Funcionário ou Contador que souber (ou adivinhar) a URL acessa o conteúdo completo — inclusive ações de escrita e cancelamento financeiro — mesmo não vendo o link no menu. Some-se a isso um bug real no `AuthGuard` (o componente que decide "há sessão válida?") que pode deixar conteúdo protegido renderizar antes/sem confirmar a autenticação, e uma rota de debug esquecida em produção sem proteção nenhuma.

**Contagem aproximada de achados: ~95**, distribuídos como:

| Severidade | Qtde. aprox. | Onde estão os mais graves |
|---|---|---|
| 🔴 Crítico | **4** | Guarda de rota ausente (`/lancamentos`, `/dev/teste-nota`), `AuthGuard` não bloqueia render, `userAdminService` mascara falha como sucesso |
| 🟠 Alto | **~6** | Páginas sem `RoleGuard` (recomendações, boletos), backup "simulado" retorna sucesso em falha, componentes com `fetch` cru embutido |
| 🟡 Médio | **~35** | Padrão sistêmico de guarda só no menu, componentes pulando a camada de hook, duplicação de formatação em 9+ arquivos, CSP com `unsafe-inline` em estilos |
| 🔵 Baixo | **~40** | Código morto (tabelas mockadas, componente duplicado), nomenclatura, comentários residuais |

### Os 4 achados críticos, em uma frase cada

1. **`/dev/teste-nota` está acessível em produção sem login algum** — fica fora do route group protegido, e o próprio comentário no topo do arquivo diz "REMOVER ANTES DO MERGE FINAL". ([Área A, item A-V1](#a-v1))
2. **`/lancamentos` — tela de dados financeiros/bancários documentada como exclusiva do Gestor — não tem `RoleGuard` no conteúdo principal**, só em um botão isolado; qualquer Funcionário ou Contador autenticado edita/exclui lançamentos via URL direta. ([Área A, item A-V2](#a-v2))
3. **`AuthGuard` decide se renderiza o conteúdo protegido olhando só `isAuthChecked`, nunca `isAuthenticated`** — e como o componente vive no layout persistente do `(shell)` sem resetar esse estado a cada navegação, existe uma janela real em que a tela renderiza com o estado de sessão da rota anterior. ([Área B, item B-V1](#b-v1))
4. **`userAdminService.createUser`/`deleteUser` retornam sucesso mesmo quando a chamada ao backend falha** (inclusive fabricando um usuário fake com `id: Date.now()`) — um admin pode achar que revogou o acesso de um funcionário desligado quando, na verdade, a exclusão nunca aconteceu. ([Área C, item C-V1](#c-v1))

Curiosamente, assim como no backend, "comentários desnecessários" foi a categoria com menos achados — o problema real está concentrado em **guarda de acesso inconsistente entre menu e rota** e em **services que escondem falhas reais atrás de mensagens de sucesso**.

---

## 2. Plano de ataque recomendado

### Onda 1 — Correções pontuais, baixo risco, alto impacto (1 a 3 dias)
- [ ] **A-V1** — Remover (ou proteger com `AuthGuard` + flag de ambiente) a rota `src/app/dev/teste-nota`.
- [ ] **A-V2** — Envolver o conteúdo principal de `src/app/(shell)/lancamentos/page.tsx` com `<RoleGuard roles="MANAGER">`.
- [ ] **B-V1** — Corrigir `AuthGuard.tsx`: condicionar `return <>{children}</>` também a `isAuthenticated`, e resetar `isAuthChecked` para `false` no início do `useEffect` a cada troca de `pathname`.
- [ ] **C-V1** — Remover os `catch` que retornam sucesso fictício em `userAdminService.createUser`/`deleteUser`; propagar o erro real para a UI.
- [ ] **C-V2** — Remover o fallback "Backup simulado realizado com sucesso!" em `userAdminService.performBackup`/`restoreBackup`; propagar falha real.
- [ ] **A-V3**/**A-V4** — Adicionar `RoleGuard` faltante em `/comercio/recomendacoes` e `/comercio/boletos`.

### Onda 2 — Fechar o padrão sistêmico de guarda (3 a 5 dias)
- [ ] Aplicar `RoleGuard` de forma consistente em todas as rotas marcadas ❌/⚠️ na [tabela do apêndice](#6-apêndice--cobertura-de-guarda-de-papel-por-rota) — idealmente extraindo um wrapper único (`<RestrictedPage roles={...}>`) usado no `layout.tsx` de cada seção, para eliminar a chance desse padrão se repetir no futuro.
- [ ] **B-A1** — Extrair a lógica de `fetch` cru de `NotaRevisaoModal.tsx`/`NotasPendentesFila.tsx` para um `capturaNotaService`.

### Onda 3 — Estrutural, por módulo (1 a 2 semanas)
- [ ] Quebrar `comercio/boletos/page.tsx` (1959 linhas) em 3 componentes de página (uma por aba) + hook/service genérico parametrizado por tipo de cobrança.
- [ ] Extrair `formatCurrency`/`getStatusColor`/intervalos de data para `src/utils`, hoje duplicados em 9+ arquivos.
- [ ] Proxiar a chamada de roteamento de `Map.tsx` (hoje vai direto para o servidor de demonstração público do OSRM) pelo backend, como já é feito para o Google Places.

### Onda 4 — Débito técnico contínuo (backlog, sem urgência)
Tudo marcado 🟡/🔵 nas seções abaixo: componentes/tabelas mortas (`BilletsTable.tsx`, `NotesTable.tsx`), CSP `unsafe-inline` em `style-src`, "criptografia" client-side teatral do rascunho de notificações.

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

<a id="a-v2"></a>
- [ ] 🔴 **[A-V2] `/lancamentos` (dados financeiros — Gestor apenas conforme README) sem guarda de papel no conteúdo principal**
  **Local:** `src/app/(shell)/lancamentos/page.tsx:83`
  `FinancialLaunchesPage` não é envolvido por `<RoleGuard roles="MANAGER">`; só o botão "Gerar Extratos" (linha 484) está protegido. `Sidebar.tsx:43` declara `roles: ["MANAGER"]` — o menu esconde o link, mas a URL continua totalmente funcional para EMPLOYEE/ACCOUNTANT: listagem de todos os lançamentos bancários, edição, exclusão, exportação Excel/relatório completo e download de extratos Sicoob/BB em PDF.

<a id="a-v3"></a>
- [ ] 🟠 **[A-V3] `/comercio/recomendacoes` (Gestor apenas) sem nenhuma guarda de papel**
  **Local:** `src/app/(shell)/comercio/recomendacoes/page.tsx:30` — `RecommendationPage` (881 linhas) não importa `RoleGuard` em lugar nenhum (confirmado via grep). Qualquer EMPLOYEE/ACCOUNTANT cria, edita e exclui produtos de recomendação diretamente pela URL.

<a id="a-v4"></a>
- [ ] 🟠 **[A-V4] `/comercio/boletos` (Gestor + Funcionário) sem nenhuma guarda de papel — 1959 linhas com ações financeiras de escrita**
  **Local:** `src/app/(shell)/comercio/boletos/page.tsx:331` — zero `RoleGuard` no arquivo inteiro. O papel ACCOUNTANT (que segundo o README não deveria acessar este módulo) consegue marcar boleto como pago, baixar PDFs, cancelar boletos e **cancelar notas fiscais na SEFAZ** apenas navegando para a URL — ações financeiras irreversíveis fora do papel previsto.

- [ ] 🟡 **[A-V5] Padrão sistêmico: proteção de papel implementada só no menu (`Sidebar.tsx`), não replicada na rota**
  **Locais:** `comercio/compras/page.tsx:15`, `comercio/clientes/page.tsx:42` (+`novo`, +`editar/[id]`, CRUD completo de PII — telefone/e-mail/endereço/documento), `comercio/frete/page.tsx:84` (só um card interno protegido), `comercio/capturar-nota/page.tsx:20`, `notificacoes/page.tsx:143` (só a aba Contabilidade protegida). Ver [tabela completa no apêndice](#6-apêndice--cobertura-de-guarda-de-papel-por-rota). O `Sidebar.tsx` prova que a intenção de restringir por papel existe e foi corretamente modelada — só não foi replicada nas páginas, o tipo de inconsistência que passa despercebida em code review porque a tela "parece" protegida (o link não aparece no menu para quem não deveria acessar).

- [ ] 🔵 **[A-V6] `/perfil` (Gestor + Funcionário) sem `RoleGuard` no nível da página**
  **Local:** `src/app/(shell)/perfil/page.tsx:7` — só o bloco "Acesso de Gerente" (linha 53) está protegido. Impacto baixo (exibe só dados do próprio usuário logado), mas ainda diverge da tabela de papéis documentada.

- [ ] 🔵 **[A-V7] `/comercio/capturar-nota` não está documentada na tabela de papéis do README**
  Dificulta auditar se o acesso atual (sem guarda nenhuma, ver A-V5) é intencional ou omissão.

- [ ] 🟡 **[A-V8] CSP com `'unsafe-inline'` em `style-src`**
  **Local:** `src/proxy.ts:23` — `style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com`. Diferente de `script-src` (nonce + `strict-dynamic`, corretamente configurado), `style-src` libera qualquer `<style>`/`style=""` inline — usado extensivamente no código. Neutraliza boa parte da proteção de CSP contra ataques baseados em CSS.

- [ ] 🟡 **[A-V9] "Criptografia" client-side do rascunho de notificações é teatro de segurança**
  **Local:** `src/app/(shell)/notificacoes/page.tsx:45-141` — o rascunho em `sessionStorage` é "criptografado" com AES-GCM, mas a chave é derivada via PBKDF2 de uma string hardcoded no bundle JS (`DRAFT_KEY_MATERIAL`, linha 45) com salt também hardcoded. Qualquer visitante do site pode derivar a mesma chave — a "criptografia" não protege contra nada que o `sessionStorage` em texto puro já não protegesse. ~90 linhas de complexidade (PBKDF2, AES-GCM, base64) para proteção que não existe de fato.

- [ ] 🔵 **[A-V10] Validação de senha (mínimo 4 caracteres) só client-side ao criar usuário**
  **Local:** `src/app/(shell)/acesso/novo/page.tsx:40-43` — política já fraca (4 caracteres) checada só no cliente + `minLength` HTML; fora do escopo confirmar se o backend também valida, mas é dado sensível o suficiente (senha de login) para merecer nota. *(Ver também [Área E do backend, A-V3](../Back/AUDITORIA.md) — mesma política de senha fraca do lado servidor.)*

### A · Acoplamento e baixa coesão

- [ ] 🟡 **[A-A1] `fetch()` direto em páginas, ignorando a camada de `services`**
  **Locais:** `src/app/dev/teste-nota/page.tsx:100-107,208-215`, `src/app/(shell)/comercio/capturar-nota/page.tsx:25-29`, `src/app/dispositivo/vincular/page.tsx:194-198` — as três montam `FormData`, chamam `fetch` com URL manual e duplicam a mesma lógica de "extrair mensagem de erro" (`extrairMensagemErro`) em pelo menos 4 lugares (também em `NotasPendentesFila.tsx`, ver B-A1). Mudança de contrato de API exige tocar em 4 arquivos em vez de 1.

- [ ] 🟠 **[A-A2] `/comercio/boletos/page.tsx` concentra estado e lógica de 3 telas completamente diferentes em um único componente de 1959 linhas**
  **Local:** `src/app/(shell)/comercio/boletos/page.tsx` — mistura aba "Boletos em Aberto" (seleção em massa, pagar/baixar/cancelar), "Consultar por Cliente" e "NF sem Boleto". Funções como `executeMarkAsPaid`/`executeConfirmInvoicePayment` e `executeCancel`/`executeCancelInvoices` são pares quase idênticos duplicados para boleto vs. nota fiscal (ex.: linhas 723-773 vs. 437-487). Qualquer bug fix precisa ser replicado manualmente nos dois fluxos.

- [ ] 🟡 **[A-A3] Lógica de negócio pesada dentro da página em vez de hooks/services**
  **Local:** `src/app/(shell)/lancamentos/page.tsx:263-356` (`handleGenerateExtratos`) — orquestra diretamente 2 chamadas de API em paralelo (`Promise.allSettled`), monta resultados por banco e decide side-effects, tudo fora de `useTransaction` (que já existe e é usado no resto do arquivo).

- [ ] 🔵 **[A-A4] Guardas de papel ad-hoc repetidas em vez de um wrapper de seção reaproveitável**
  **Local:** `src/app/(shell)/dashboard/page.tsx:53,62,78,115,124` — 5 usos de `<RoleGuard roles={["MANAGER"]}>` no mesmo arquivo. Um componente `GestorOnly` reduziria a chance de alguém esquecer de aplicá-lo (como aconteceu nas rotas de A-V5).

### A · Clareza / código confuso

- [ ] 🟠 **[A-C1] `comercio/boletos/page.tsx` — 1959 linhas** *(ver também A-A2)* — mistura data-fetching, 12+ `useState`, tabela responsiva duplicada (desktop/mobile) e modais de confirmação.
- [ ] 🟠 **[A-C2] `lancamentos/page.tsx` — 1343 linhas**, incluindo um formulário de edição inteiro definido inline (linhas 1064-1299) em vez de extraído — compare com `ClientForm`, usado corretamente em `clientes/novo`/`editar`.
- [ ] 🟡 **[A-C3] `comercio/recomendacoes/page.tsx` — 881 linhas**, modal de edição inline (732-846) e a mesma lógica de "recarregar recomendações" repetida em 3 lugares (`handleAddProduct`, `handleSaveEdit`, `handleConfirmDelete`).
- [ ] 🟡 **[A-C4] `notificacoes/page.tsx` — 846 linhas**, com criptografia de rascunho, validações e envio todos dentro do componente de página em vez de hooks/utils dedicados.
- [ ] 🔵 **[A-C5] Estados booleanos/união paralelos que poderiam ser uma máquina de estados**
  **Local:** `src/app/(shell)/lancamentos/page.tsx:114-121` — `isGeneratingExtratos` (boolean) + `sicoobResult`/`bbResult` (union) + `exportKind` como três fontes de verdade paralelas para "qual operação assíncrona está em andamento".
- [ ] 🔵 **[A-C6] Estado morto mantido com prefixo `_` para silenciar o linter**
  **Local:** `notificacoes/page.tsx:157-158` (`_dataVencimento`, `_valorBoleto`) — setados mas nunca lidos; `recomendacoes/page.tsx:261-272` (`_getTagColor`) nunca chamada.
- [ ] 🔵 **[A-C7] `"use client"` em páginas majoritariamente estáticas**
  **Locais:** `src/app/acesso-negado/page.tsx:1`, `src/app/(shell)/admin/page.tsx:1` — poderiam ser Server Components com só o `onClick`/`RoleGuard` extraído para um Client Component filho pequeno. Padrão repetido em quase toda página do projeto.

### A · Comentários desnecessários / código morto

- [ ] 🔵 **[A-CM1] Bloco de dark mode inteiro comentado (34 linhas)**
  **Local:** `src/app/globals.css:57-91` — ou implementa ou remove; deixado comentado sugere trabalho inacabado.
- [ ] 🔵 **[A-CM2] Decorações de fundo com `opacity-0 pointer-events-none` em vez de removidas**
  **Local:** `src/app/login/layout.tsx:22-41` — elementos continuam no DOM e o CSS de animação continua no bundle, só invisíveis.
- [ ] 🔵 **[A-CM3] Comentário "REMOVER ANTES DO MERGE FINAL" não removido** *(mesmo achado que A-V1, sinal de processo — recomenda-se lint/CI que falhe o build se `src/app/dev/**` existir fora de ambiente de desenvolvimento)*

---

## Área B — Componentes

**Escopo analisado:** `src/components/auth`, `layout`, `ui`, `forms`, `modals`, `modules` (incl. `modules/tables`), `landing`, `img` (37 arquivos).

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

- [ ] 🟡 **[B-V2] `RoleGuard` implementado corretamente, mas sem fonte única de verdade de autenticação**
  **Local:** `src/components/auth/RoleGuard.tsx:31` — cada instância chama seu próprio `useAuth()` (que dispara seu próprio `checkAuth()`). `Sidebar.tsx` usa `RoleGuard` 9 vezes dentro de um `.map()` (`Sidebar.tsx:173-260`) — dezenas de hooks de auth independentes montados simultaneamente, cada um com seu próprio estado local. Só não gera tempestade de requisições por uma deduplicação incidental (`pendingMeRequest` em `authService.ts:33,68-88`), não por decisão de arquitetura. Ausência de um `AuthContext`/Provider central.

- [ ] 🟡 **[B-V3] Endereços de clientes enviados a um servidor OSRM público de demonstração, sem proxy do backend**
  **Local:** `src/components/modules/Map.tsx:32-34` — HTTPS ok, mas coordenadas de origem/destino de entregas (potencialmente endereços de clientes reais) vão direto do navegador para `router.project-osrm.org` (servidor demo, sem SLA, sem relação contratual). Inconsistente com `useAutocomplete.ts`, que corretamente protege a chave do Google Maps via Server Action. Sem `AbortController` — trocas rápidas de origem/destino podem gerar corrida entre respostas.

- [ ] 🔵 **[B-V4] Validação de upload é responsabilidade só do client** *(informativo)*
  **Local:** `src/components/modules/EnhancedUploadNotes.tsx:25-41`, `src/hooks/useUpload.ts` — arquitetura correta (componente→hook→service), mas a tela assume implicitamente que tipo/tamanho também são reforçados no backend. Checagem client-side é trivial de contornar.

- [ ] 🔵 **[B-V5] Token de dispositivo (`X-Device-Token`) em `localStorage`** *(informativo, fora do escopo direto)*
  Citado em `src/components/modules/CapturaNotaCamera.tsx:16` — não é o JWT de sessão (arquitetura de cookie httpOnly continua intacta), mas é um bearer token exposto a XSS. Ver detalhamento em [Área C, item C-V5](#c-v5).

### B · Acoplamento e baixa coesão

Arquitetura documentada prevê Componente → Hook → Service. Na prática, boa parte dos modais e algumas tabelas pulam a camada de hook:

- [ ] 🟡 **[B-A1] Componentes que chamam `service` diretamente, sem hook intermediário** *(11 ocorrências)*
  `ClientForm.tsx:7,131` (cepService), `GroupedProductsModal.tsx:6,26-27`, `CombinedScoreImagesModal.tsx:6,40,54-61` (e ainda faz `fetch` cru), `CreateManualPurchaseModal.tsx:7,9,80-88,134`, `FreightConfigsModal.tsx:3,82-83`, `ClientDetailModal.tsx:16,107`, `InvoiceProductsModal.tsx:8,9,46-165`, `ClientProductsTable.tsx:3,5,90-127`, `PurchaseFilesTable.tsx:8,10,91-183`, `FreightConfigInfo.tsx:5,35-41`, `CombinedScoresCards.tsx:12-14,112,127,130,188` (mistura hooks **e** chamada direta no mesmo arquivo).

- [ ] 🟠 **[B-A2] Componentes que fazem `fetch` cru, sem passar nem por hook nem por service**
  **Locais:** `NotaRevisaoModal.tsx:226-248` (monta URL/headers/body/erro completo dentro do modal — caso mais grave, nem hook nem service), `NotasPendentesFila.tsx:64-143` (3 `fetch` diretos, cada um com sua própria extração de erro duplicada), `ClientSummaryCards.tsx:27-51`.

- [ ] 🟡 **[B-A3] Duplicação de formatação/lógica entre arquivos, sem utilitário compartilhado** *(grep confirmou zero `export function formatCurrency` em `src/utils`)*
  - `formatCurrency` reimplementado de forma idêntica em **9 arquivos**: `GroupedProductsModal.tsx:41-46`, `ShowBilletDataModal.tsx:117-122`, `InvoiceProductsModal.tsx:167-172`, `CreateManualPurchaseModal.tsx:114-118`, `ShowInvoiceDataModal.tsx:90-95`, `CombinedScoreImagesModal.tsx:17-22`, `NotaRevisaoModal.tsx:134-139`, `combined-scores/formatters.ts:14-18`, `PurchaseFilesTable.tsx:199-204`.
  - Mesmo padrão de `.toLocaleString` repetido dezenas de vezes em `CashFlow.tsx` e `FreightConfigInfo.tsx:21-24,197-200`.
  - `getStatusColor` duplicado em `ShowBilletDataModal.tsx:124-139`, `ShowInvoiceDataModal.tsx:107-121`, `combined-scores/formatters.ts:21-33`.
  - `getWeekInterval`/`getLastMonthInterval` **copiadas literalmente** entre `ClientProductsTable.tsx:13-42` e `PurchaseFilesTable.tsx:20-49`.
  - Padrão de "recalcular o 3º campo numérico a partir dos outros dois" (`recalcRow`/`round`/`NumericField`) duplicado quase byte-a-byte entre `CreateManualPurchaseModal.tsx:19-66` e `NotaRevisaoModal.tsx:60-118` — candidato a hook compartilhado.
  - Normalização de texto para busca (`normalize()` + regex de diacríticos) duplicada 3× com o mesmo comentário: `ClientAutocompleteField.tsx:6-19`, `ProductAutocompleteField.tsx:6-19`, `ClientSelector.tsx:10-21`.

- [ ] 🟡 **[B-A4] Regra fiscal/comercial hardcoded como string mágica em componente de UI**
  **Local:** `src/components/modules/CombinedScoresCards.tsx:301,372` — decide se exige "dados adicionais" na nota fiscal comparando o nome do cliente contra o literal `"LLINEA"`, duplicado em 2 handlers.

### B · Baixa coesão (arquivos "kitchen sink" / código morto)

- [ ] 🔵 **[B-B2] Tabelas inteiramente mockadas, nunca importadas em lugar nenhum**
  **Locais:** `src/components/modules/tables/BilletsTable.tsx`, `NotesTable.tsx` — dados hardcoded (`"BOL123"`, `"R$ 500,00"`), scaffolding esquecido de fase inicial.
- [ ] 🔵 **[B-B3] Componente de exemplo estático deixado como se fosse reutilizável**
  **Local:** `src/components/ui/Alerts.tsx` — texto fixo em português sem props, usado só uma vez.

### B · Clareza / código confuso

- [ ] 🟡 **[B-C4] Componentes muito longos (>250 linhas)**
  `ClientForm.tsx` (919 — deveria dividir em subcomponentes de seção), `CashFlow.tsx` (854 — extrair cada par Card+gráfico), `PurchaseFilesTable.tsx` (568, com o modal de "Criar Agrupamento" inteiro embutido nas linhas 390-537), `NotaRevisaoModal.tsx` (547), `InvoiceProductsModal.tsx` (459), `FavoritesModal.tsx` (422, duas abas inteiras no mesmo arquivo).

### B · Comentários desnecessários / código morto

- [ ] 🟡 **[B-CM1] Comentário mascarando funcionalidade sabidamente incompleta em produção**
  **Local:** `src/components/modals/FavoritesModal.tsx:70-85` — `lat: 0, lng: 0` hardcoded ao adicionar local favorito manualmente, com o comentário `// Seria obtido do autocomplete` como único registro do problema (não um TODO rastreável). Favoritos manuais ficam com coordenadas no meio do oceano, quebrando cálculo de frete/rota — bug funcional, não só comentário morto.
- [ ] 🔵 **[B-CM2] Arquivos mortos que deveriam ser removidos, não deixados "por via das dúvidas"** — `BilletsTable.tsx`, `NotesTable.tsx` (ver seção Baixa coesão).

---

## Área C — Services, Hooks, Types, Utils, Config

**Escopo analisado:** `src/services/**`, `src/hooks/**`, `src/types/**`, `src/utils/**`, `src/config/api.ts`, `scripts/check-env.mjs`, `package.json`, `biome.json`, `tsconfig.json`.

> **Confirmação positiva mais importante desta auditoria:** nenhum resquício de JWT/token de sessão lido, decodificado ou armazenado manualmente pelo frontend. `httpUtils.ts` é hoje exatamente o "wrapper fino" descrito no README, e todas as ~40 chamadas `fetch` em `services/**` usam `credentials: "include"`. A promessa arquitetural central está sendo respeitada.

### C · Vulnerabilidades / Segurança

<a id="c-v1"></a>
- [ ] 🔴 **[C-V1] Falso sucesso mascarando falhas reais de escrita/exclusão de usuário**
  **Local:** `src/services/userAdminService.ts:67-98` (`createUser`), `:163-172` (`deleteUser`)
  ```ts
  async deleteUser(id: number): Promise<boolean> {
    try {
      const usuario = await this.getUserById(id);
      await userService.deleteUser(usuario.nome);
      return true;
    } catch (error) {
      console.warn("Erro ao excluir usuário no backend:", error);
      return true; // retorna sucesso mesmo em erro
    }
  }
  ```
  `createUser`, em erro, devolve um usuário "fake" com `id: Date.now()`. A UI de gestão de acesso consome este serviço — se a exclusão falhar (permissão, backend fora do ar), o app informa "sucesso" ao admin. Um funcionário desligado pode continuar com acesso ativo enquanto o admin acredita ter revogado. Falha de integridade com implicação direta de segurança.

<a id="c-v2"></a>
- [ ] 🟠 **[C-V2] Backup "simulado" retorna sucesso quando a chamada real falha**
  **Local:** `src/services/userAdminService.ts:174-193` (`performBackup`), `:195-220` (`restoreBackup`)
  ```ts
  } catch (error) {
    console.warn("Backend não disponível para backup:", error);
    return { success: true, message: "Backup simulado realizado com sucesso! (Modo offline)" };
  }
  ```
  Se o backend estiver fora do ar — exatamente o cenário em que um backup é mais necessário — o usuário recebe mensagem de conclusão. Risco real de perda de dados por falsa sensação de segurança.

- [ ] 🟡 **[C-V3] `getStats()` retorna dados mockados silenciosamente em caso de erro**
  **Local:** `src/services/userAdminService.ts:31-50` — `{ totalUsers: 2, totalManagers: 1, ... }` fixo no `catch`, sem sinalização de erro na tela. Decisões administrativas podem ser tomadas sobre dado fictício.

- [ ] 🟡 **[C-V4] Interpolação direta de datas/strings em URL sem `encodeURIComponent`/`URLSearchParams`**
  **Locais:** `reportService.ts:8-9` (datas viram **segmentos de path** — `/` ou `..` alteram o caminho requisitado), `dashboardService.ts:58-59`, `transactionService.ts:64,90,118`, `fiscalNoteXmlStorageService.ts:11`, `groupedProductsService.ts:10` (`clientId` pode virar literalmente `"undefined"` na URL). O resto do código-base já usa `encodeURIComponent` consistentemente em outros pontos — desvio pontual, fácil de padronizar.

<a id="c-v5"></a>
- [ ] 🔵 **[C-V5] Token de dispositivo (`X-Device-Token`) em `localStorage`, acessível via XSS** *(informativo)*
  **Local:** `src/services/dispositivoService.ts:11`, uso em `src/app/dispositivo/vincular/page.tsx:89,196`. Não é o JWT de sessão (comentário em `dispositivoService.ts:6-10` explica o motivo, uso legítimo de pareamento de dispositivo móvel) — mas qualquer XSS teria acesso, permitindo chamadas autenticadas como aquele dispositivo. Melhoria possível: cookie `httpOnly` de curta duração em vez de `localStorage`.

- [ ] 🔵 **[C-V6] Mensagens de erro do backend repassadas ao usuário sem filtragem**
  **Locais:** `authService.ts:50-57`, `billetService.ts:29-37` (`response.text()` cru), `groupedProductsService.ts` e outros — `throw new Error(errorData.message || ...)` propaga texto do backend direto para toasts. Risco baixo hoje (backend confiável/mesmo time), mas sem allowlist/normalização.

### C · Comentários desnecessários / código morto

- Nenhum `TODO`/`FIXME`/código comentado morto encontrado além disso. Os `catch` "vazios" encontrados (`billetService.ts:33`, `invoiceService.ts:43,70,154`, `authService.ts:80,101`, `statementApiService.ts:37`, `useRealtimeSocket.ts:50,66`) **têm comentário explicando a decisão de ignorar o erro** — padrão consciente, não descuido.

### C · Configuração de build/qualidade

- [ ] ✅ `tsconfig.json` em `strict: true`, sem flags enfraquecidas — nenhum achado.
- [ ] ✅ `biome.json` sem regras relevantes desabilitadas sem justificativa (única regra off, `suspicious.noUnknownAtRules`, tem efeito prático questionável já que `**/*.css` está excluído da varredura do Biome — vale confirmar se ainda é necessária).
- [ ] ✅ `npm audit --production` — 0 vulnerabilidades conhecidas no momento da varredura (2026-08-06).
- [ ] 🔵 **[C-CFG1] Nenhum workflow de CI encontrado no escopo rodando `check-types`/`lint`/`npm audit` em PRs** *(informativo — fora do escopo estrito de arquivos revisados, sinalizado para confirmação externa)*

---

## 6. Apêndice — cobertura de guarda de papel por rota

Comparação entre a tabela de papéis do `README.md` / declaração em `Sidebar.tsx` e a proteção real encontrada dentro de cada página:

| Rota | Papel esperado | Guarda aplicada na página? |
|---|---|---|
| `/dashboard` | Gestor | ✅ `RoleGuard roles={["MANAGER"]}` |
| `/lancamentos` | Gestor | ❌ **sem guarda no conteúdo principal** (só 1 botão) — [A-V2](#a-v2) |
| `/comercio/clientes` (+novo, +editar/[id]) | Gestor, Funcionário | ❌ **sem guarda** — [A-V5](#a-v5) |
| `/comercio/compras` | Gestor, Funcionário | ❌ **sem guarda** |
| `/comercio/boletos` | Gestor, Funcionário | ❌ **sem guarda** — [A-V4](#a-v4) |
| `/comercio/frete` | Gestor, Funcionário | ⚠️ parcial (só um card interno) |
| `/comercio/recomendacoes` | Gestor | ❌ **sem guarda** — [A-V3](#a-v3) |
| `/comercio/nota-fiscal-xml` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/comercio/capturar-nota` | não documentada (Gestor+Funcionário no menu) | ❌ **sem guarda** |
| `/notificacoes` | Gestor, Funcionário | ⚠️ parcial (só aba Contabilidade) |
| `/acesso` (+novo, +editar/[id]) | Gestor | ✅ `RoleGuard roles="MANAGER"` em todas |
| `/admin` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/backup` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/perfil` | Gestor, Funcionário | ❌ **sem guarda** (baixo impacto — só dados próprios) |
| `/dev/teste-nota` | (não deveria existir) | ❌ **fora do `AuthGuard`, sem guarda nenhuma** — [A-V1](#a-v1) |

---

## 7. O que já está bom (não mexer)

- **JWT de sessão nunca é lido/decodificado/armazenado manualmente no frontend** — a arquitetura de cookie `httpOnly` documentada no README está sendo respeitada em 100% do código auditado.
- `httpUtils.ts` confirmado como wrapper fino, sem lógica de `Authorization: Bearer`.
- Todas as chamadas `fetch` em `services/**` usam `credentials: "include"`.
- Nenhum uso de `any`/`as any` em todo o escopo auditado; `tsconfig.json` em modo `strict`.
- `RoleGuard` (diferente do `AuthGuard`) está implementado corretamente — só renderiza `children` quando `hasVerified && hasPermission`; nenhum bypass identificado nele.
- CSP com nonce + `strict-dynamic` corretamente configurada para `script-src` em produção (a lacuna está só em `style-src`, ver A-V8).
- `npm audit` limpo; nenhum `dangerouslySetInnerHTML`/`eval` em todo o código.
- Os `catch` "silenciosos" existentes no projeto são todos comentados/intencionais — não descuido, e sim uma decisão documentada caso a caso.
- Upload de arquivos delega corretamente a validação para um hook dedicado (`useUpload`), seguindo o padrão componente→hook→service.
