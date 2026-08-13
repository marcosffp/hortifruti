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
- [x] **B-A1** — Extrair a lógica de `fetch` cru de `NotaRevisaoModal.tsx`/`NotasPendentesFila.tsx` para um `capturaNotaService`.

### Onda 3 — Estrutural, por módulo (1 a 2 semanas)
- [ ] Quebrar `comercio/boletos/page.tsx` (1959 linhas) em 3 componentes de página (uma por aba) + hook/service genérico parametrizado por tipo de cobrança.
- [x] Extrair `formatCurrency`/`getStatusColor`/intervalos de data para `src/utils`, hoje duplicados em 9+ arquivos. *(`formatCurrency`, intervalos de data, `normalize()` e o padrão `recalcRow` foram extraídos para `src/utils`; `getStatusColor` foi mantido por arquivo — os três eram domínios distintos com mapeamentos de status diferentes, não duplicação real.)*

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

### A · Acoplamento e baixa coesão

- [ ] 🟡 **[A-A1] `fetch()` direto em páginas, ignorando a camada de `services`**
  **Locais:** `src/app/dev/teste-nota/page.tsx:100-107,208-215`, `src/app/(shell)/comercio/capturar-nota/page.tsx:25-29`, `src/app/dispositivo/vincular/page.tsx:194-198` — as três montam `FormData`, chamam `fetch` com URL manual e duplicam a mesma lógica de "extrair mensagem de erro" (`extrairMensagemErro`) que também existia em `NotasPendentesFila.tsx` (já corrigido — ver B-A2 no changelog). Mudança de contrato de API ainda exige tocar em 3 arquivos de página em vez de 1.

- [ ] 🟠 **[A-A2] `/comercio/boletos/page.tsx` concentra estado e lógica de 3 telas completamente diferentes em um único componente de 1959 linhas**
  **Local:** `src/app/(shell)/comercio/boletos/page.tsx` — mistura aba "Boletos em Aberto" (seleção em massa, pagar/baixar/cancelar), "Consultar por Cliente" e "NF sem Boleto". Funções como `executeMarkAsPaid`/`executeConfirmInvoicePayment` e `executeCancel`/`executeCancelInvoices` são pares quase idênticos duplicados para boleto vs. nota fiscal (ex.: linhas 723-773 vs. 437-487). Qualquer bug fix precisa ser replicado manualmente nos dois fluxos.

- [ ] 🟡 **[A-A3] Lógica de negócio pesada dentro da página em vez de hooks/services**
  **Local:** `src/app/(shell)/lancamentos/page.tsx:263-356` (`handleGenerateExtratos`) — orquestra diretamente 2 chamadas de API em paralelo (`Promise.allSettled`), monta resultados por banco e decide side-effects, tudo fora de `useTransaction` (que já existe e é usado no resto do arquivo).

- [ ] 🔵 **[A-A4] Guardas de papel ad-hoc repetidas em vez de um wrapper de seção reaproveitável**
  **Local:** `src/app/(shell)/dashboard/page.tsx:53,62,78,115,124` — 5 usos de `<RoleGuard roles={["MANAGER"]}>` no mesmo arquivo. Um componente `GestorOnly` reduziria a chance de alguém esquecer de aplicá-lo.

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

- [ ] 🔵 **[B-V4] Validação de upload é responsabilidade só do client** *(informativo)*
  **Local:** `src/components/modules/EnhancedUploadNotes.tsx:25-41`, `src/hooks/useUpload.ts` — arquitetura correta (componente→hook→service), mas a tela assume implicitamente que tipo/tamanho também são reforçados no backend. Checagem client-side é trivial de contornar.

- [ ] 🔵 **[B-V5] Token de dispositivo (`X-Device-Token`) em `localStorage`** *(informativo, fora do escopo direto)*
  Citado em `src/components/modules/CapturaNotaCamera.tsx:16` — não é o JWT de sessão (arquitetura de cookie httpOnly continua intacta), mas é um bearer token exposto a XSS. Ver detalhamento em [Área C, item C-V5](#c-v5).

### B · Acoplamento e baixa coesão

Arquitetura documentada prevê Componente → Hook → Service. Na prática, boa parte dos modais e algumas tabelas pulam a camada de hook:

- [ ] 🟡 **[B-A3] Duplicação de formatação/lógica entre arquivos, sem utilitário compartilhado** — resolvido: `formatCurrency` (`src/utils/formatCurrency.ts`), `getWeekInterval`/`getLastMonthInterval` (`src/utils/dateUtils.ts`), `normalize()` (`src/utils/textSearch.ts`) e o padrão `recalcRow`/`round`/`NumericField` (`src/utils/numericRow.ts`). Restam, fora do escopo desta rodada:
  - Mesmo padrão de `.toLocaleString` repetido dezenas de vezes em `CashFlow.tsx` e `FreightConfigInfo.tsx:21-24,197-200`.
  - `getStatusColor` duplicado em `ShowBilletDataModal.tsx:124-139`, `ShowInvoiceDataModal.tsx:107-121`, `combined-scores/formatters.ts:21-33` — mantido por arquivo de propósito: cada um usa um vocabulário de status e mapeamento de cor diferente (billet/invoice/combined-score), não é a mesma lógica duplicada.

### B · Clareza / código confuso

- [ ] 🟡 **[B-C4] Componentes muito longos (>250 linhas)**
  `ClientForm.tsx` (921), `CashFlow.tsx` (854 — extrair cada par Card+gráfico), `PurchaseFilesTable.tsx` (537, com o modal de "Criar Agrupamento" embutido), `NotaRevisaoModal.tsx` (473), `InvoiceProductsModal.tsx` (456), `FavoritesModal.tsx` (450, duas abas inteiras no mesmo arquivo). *(Avaliado nesta rodada e adiado a pedido — mexe em telas grandes de produção sem suíte de testes para validar visualmente.)*

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

<a id="c-v5"></a>
- [ ] 🔵 **[C-V5] Token de dispositivo (`X-Device-Token`) em `localStorage`, acessível via XSS** *(informativo — não corrigido nesta rodada: a melhoria sugerida (cookie `httpOnly`) exige mudança de contrato no backend, fora do escopo de `Codigo/Front`)*
  **Local:** `src/services/dispositivoService.ts:11`, uso em `src/app/dispositivo/vincular/page.tsx:89,196`. Não é o JWT de sessão (comentário em `dispositivoService.ts:6-10` explica o motivo, uso legítimo de pareamento de dispositivo móvel) — mas qualquer XSS teria acesso, permitindo chamadas autenticadas como aquele dispositivo. Melhoria possível: cookie `httpOnly` de curta duração em vez de `localStorage`.

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
| `/comercio/clientes` (+novo, +editar/[id]) | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` em todas |
| `/comercio/compras` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/comercio/boletos` | Gestor, Funcionário | ❌ **sem guarda** — [A-V4](#a-v4) |
| `/comercio/frete` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/comercio/recomendacoes` | Gestor | ❌ **sem guarda** — [A-V3](#a-v3) |
| `/comercio/nota-fiscal-xml` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/comercio/capturar-nota` | Gestor, Funcionário (agora documentada no README) | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/notificacoes` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/acesso` (+novo, +editar/[id]) | Gestor | ✅ `RoleGuard roles="MANAGER"` em todas |
| `/admin` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/backup` | Gestor | ✅ `RoleGuard roles="MANAGER"` |
| `/perfil` | Gestor, Funcionário | ✅ `RoleGuard roles={["MANAGER","EMPLOYEE"]}` |
| `/dev/teste-nota` | (não deveria existir) | ❌ **fora do `AuthGuard`, sem guarda nenhuma** — [A-V1](#a-v1) |

---

## 7. O que já está bom (não mexer)

- **JWT de sessão nunca é lido/decodificado/armazenado manualmente no frontend** — a arquitetura de cookie `httpOnly` documentada no README está sendo respeitada em 100% do código auditado.
- `httpUtils.ts` confirmado como wrapper fino, sem lógica de `Authorization: Bearer`.
- Todas as chamadas `fetch` em `services/**` usam `credentials: "include"`.
- Nenhum uso de `any`/`as any` em todo o escopo auditado; `tsconfig.json` em modo `strict`.
- `RoleGuard` (diferente do `AuthGuard`) está implementado corretamente — só renderiza `children` quando `hasVerified && hasPermission`; nenhum bypass identificado nele.
- CSP com nonce + `strict-dynamic` corretamente configurada para `script-src`; `style-src` foi dividida em `style-src-elem` (sem `unsafe-inline`, bloqueia injeção de `<style>` via XSS) e `style-src-attr` (`unsafe-inline`, necessário pro `style` inline dinâmico do React/Leaflet).
- `npm audit` limpo; nenhum `dangerouslySetInnerHTML`/`eval` em todo o código.
- Os `catch` "silenciosos" existentes no projeto são todos comentados/intencionais — não descuido, e sim uma decisão documentada caso a caso.
- Upload de arquivos delega corretamente a validação para um hook dedicado (`useUpload`), seguindo o padrão componente→hook→service.
