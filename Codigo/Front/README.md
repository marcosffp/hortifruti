<img width="1600" style="height:auto; border-radius: 12px;" alt="banner" src="../../Documentacao/images/banner.png" />

# Frontend

> Interface web do sistema de gestão do Hortifruti Santa Luzia LTDA — dashboard financeiro, gestão de clientes e compras, conciliação bancária, cálculo de frete com mapas, geração de boletos e notas fiscais, notificações e controle de acesso por papéis, tudo em uma SPA construída com Next.js e TypeScript.

---

## 🛠️ Stack Principal

![Next.js](https://img.shields.io/badge/Next.js-16.2-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React-19.1-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![MUI](https://img.shields.io/badge/MUI-7.3-007FFF?style=for-the-badge&logo=mui&logoColor=white)
![Chart.js](https://img.shields.io/badge/Chart.js-4.5-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white)
![Leaflet](https://img.shields.io/badge/Leaflet-1.9-199900?style=for-the-badge&logo=leaflet&logoColor=white)
![Biome](https://img.shields.io/badge/Biome-2.5-60A5FA?style=for-the-badge&logo=biome&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-Deploy-000000?style=for-the-badge&logo=vercel&logoColor=white)

---

## 📑 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [Arquitetura](#-arquitetura)
- [Estrutura de pastas](#-estrutura-de-pastas)
- [Rotas e papéis de acesso](#-rotas-e-papéis-de-acesso)
- [Módulos e funcionalidades](#-módulos-e-funcionalidades)
- [Camada de serviços e hooks](#-camada-de-serviços-e-hooks)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Instalação e execução](#-instalação-e-execução)
- [Deploy em nuvem](#-deploy-em-nuvem)
- [Padrão de código](#-padrão-de-código)
- [Tecnologias e dependências](#-tecnologias-e-dependências)

---

## 📖 Sobre o projeto

O frontend do **Hortifruti SL** é uma SPA em **Next.js (App Router) + TypeScript** que dá acesso visual a toda a operação gerenciada pelo backend: dashboard financeiro com gráficos de fluxo de caixa e saldo bancário em tempo real, cadastro e acompanhamento de clientes e compras, conciliação de extratos bancários, cálculo de frete com mapas interativos (Leaflet/OSRM e Google Places), geração e consulta de boletos (incluindo listagem de boletos em aberto e baixa manual) e notas fiscais eletrônicas, central de notificações (e-mail/WhatsApp), backup do banco de dados e administração de usuários — tudo protegido por autenticação JWT (em cookie `httpOnly`, via proxy same-origin para a API) e controle de acesso por papéis (**Gestor**, **Funcionário**, **Contador**).

---

## 🏛️ Arquitetura

A aplicação segue o modelo de rotas do **App Router** do Next.js, separando páginas públicas (login, landing, acesso negado) de um *layout* autenticado (*route group* `(shell)`) que aplica guarda de sessão a todas as telas internas; o controle por papel é aplicado ponto a ponto (itens do menu, seções de página) via `RoleGuard`.

A autenticação é feita por **cookie `httpOnly`**, não por token em `localStorage`. O navegador chama a API sempre em same-origin (`NEXT_PUBLIC_API_URL=/api`), e o próprio Next.js faz o *proxy* servidor-a-servidor para o backend real (`rewrites` em `next.config.ts`, usando `BACKEND_URL`). Isso faz o cookie de sessão ser first-party do ponto de vista do navegador — sem isso, Safari/iOS bloqueiam cookies em chamadas cross-site e o login nunca "gruda".

```
┌────────────────────────────────────────────────────────────────┐
│                        app/ (rotas)                            │
│   /login · /landing · /acesso-negado   (públicas)              │
│   (shell)/...                          (autenticadas)          │
│       └─ AuthGuard (sessão) → Sidebar + Header + página        │
│            └─ RoleGuard (papel), aplicado por item/seção       │
└───────────────────────────┬────────────────────────────────────┘
                            │ hooks (estado, loading, erro)
┌───────────────────────────▼────────────────────────────────────┐
│                          services/                              │
│   fetch(..., { credentials: "include" }) → /api/* (same-origin) │
└───────────────────────────┬─────────────────────────────────────┘
                            │ rewrite server-to-server (next.config.ts)
                            ▼
                   Backend real (BACKEND_URL)
```

**Padrões centrais:**

| Padrão | Onde se aplica |
|---|---|
| Guarda de rota (`AuthGuard`) | Chama `GET /auth/me` (cookie); redireciona para `/login` quando não há sessão válida |
| Guarda de papel (`RoleGuard`) | Redireciona para `/acesso-negado` (ou oculta o conteúdo) quando o usuário não tem o papel exigido |
| *Route group* `(shell)` | Agrupa todas as telas internas sob um layout único com `Sidebar` + `Header` |
| Hooks por domínio (`use*`) | Encapsulam estado de *loading*/erro e chamam os `services` correspondentes |
| Proxy same-origin (`next.config.ts` + `src/proxy.ts`) | *Rewrite* de `/api/*` para `BACKEND_URL` (server-to-server) e injeção de cabeçalhos de segurança (CSP com nonce, HSTS, `X-Frame-Options`) em toda resposta |
| `services` + `credentials: "include"` | Cada chamada `fetch` inclui o cookie de sessão automaticamente — não há token para anexar manualmente |
| Validação de ambiente no build (`scripts/check-env.mjs`) | Falha o build se `NEXT_PUBLIC_API_URL`/`BACKEND_URL` não estiverem definidas ou não seguirem o formato esperado em produção (caminho relativo / `https://`) |
| Notificações via *toast* | `notificationService` envolve o `react-toastify` para feedback consistente de sucesso/erro |
| Tokens de design via CSS variables | Cores e papéis (verde = Gestor, laranja = Funcionário, amarelo = Contador) definidos em `globals.css` |

---

## 📁 Estrutura de pastas

```
Front/
├── public/
│   └── leaflet/                      # Ícones de marcador do Leaflet (marker-icon, shadow…)
├── scripts/
│   └── check-env.mjs                 # Valida variáveis de ambiente obrigatórias antes do build
├── src/
│   ├── proxy.ts                      # Middleware: CSP com nonce e cabeçalhos de segurança
│   ├── config/
│   │   └── api.ts                    # API_BASE_URL (lido de NEXT_PUBLIC_API_URL)
│   ├── app/
│   │   ├── layout.tsx                # Layout raiz (ToastContainer, fontes)
│   │   ├── page.tsx                  # Redireciona para /landing ou /login
│   │   ├── globals.css               # Tailwind + tokens de cor/tema por papel
│   │   ├── login/                    # Tela de autenticação
│   │   ├── landing/                  # Página de apresentação (marketing)
│   │   ├── acesso-negado/            # Página 403
│   │   └── (shell)/                  # Grupo de rotas autenticadas (AuthGuard + RoleGuard)
│   │       ├── layout.tsx            #   Sidebar + Header + guardas de papel
│   │       ├── dashboard/            #   Visão financeira consolidada (gráficos)
│   │       ├── lancamentos/          #   Transações bancárias
│   │       ├── comercio/
│   │       │   ├── clientes/         #     Listagem, criação e edição de clientes
│   │       │   ├── compras/          #     Upload e gestão de notas de compra
│   │       │   ├── frete/            #     Cálculo de frete com mapa
│   │       │   ├── recomendacoes/    #     Recomendações de compra por clima
│   │       │   └── nota-fiscal-xml/  #     Consulta e download de XMLs de NF-e
│   │       ├── notificacoes/         #   Central de notificações (e-mail/WhatsApp)
│   │       ├── acesso/               #   Gestão de usuários e permissões
│   │       ├── admin/                #   Painel administrativo
│   │       ├── backup/               #   Disparo e acompanhamento de backups
│   │       └── perfil/               #   Perfil e preferências do usuário
│   ├── components/
│   │   ├── auth/                     # AuthGuard, RoleGuard
│   │   ├── layout/                   # Sidebar, Header
│   │   ├── ui/                       # Button, Card, Alerts, Loading, SkeletonTableLoading
│   │   ├── forms/                    # ClientForm
│   │   ├── modals/                   # Modais de confirmação, boletos, notas, frete…
│   │   ├── modules/                  # CashFlow, Map, ClientCard, BankBalanceCard, UploadExtract, tables/…
│   │   └── landing/                  # Componentes da página de apresentação
│   ├── hooks/                        # useAuth, useClient, useDashboard, useBankBalance, useTransaction…
│   ├── services/                     # Camada de comunicação com a API (fetch + JWT)
│   ├── types/                        # Tipos e interfaces TypeScript por domínio
│   └── utils/                        # httpUtils, addressUtils, validationUtils
├── biome.json                        # Configuração do linter/formatter Biome
├── next.config.ts
├── package.json
└── .env                              # Nunca versionar em produção
```

---

## 🔐 Rotas e papéis de acesso

Todas as rotas internas ficam sob o *route group* `(shell)` e são protegidas por `AuthGuard` (sessão) e `RoleGuard` (papel). Os três papéis do sistema são **MANAGER** (Gestor), **EMPLOYEE** (Funcionário) e **ACCOUNTANT** (Contador).

| Rota | Tela | Papéis com acesso |
|---|---|---|
| `/login`, `/landing`, `/acesso-negado` | Autenticação, apresentação e página 403 | Pública |
| `/dashboard` | Visão financeira (fluxo de caixa, receitas, top produtos) | Gestor |
| `/lancamentos` | Transações bancárias importadas | Gestor |
| `/comercio/clientes` (+ `/novo`, `/editar/[id]`) | Cadastro e edição de clientes | Gestor, Funcionário |
| `/comercio/compras` | Upload e gestão de notas de compra | Gestor, Funcionário |
| `/comercio/boletos` | Geração, listagem (inclusive boletos em aberto), baixa manual e cancelamento de boletos | Gestor, Funcionário |
| `/comercio/frete` | Cálculo de frete com mapa interativo | Gestor, Funcionário |
| `/comercio/recomendacoes` | Recomendações de compra por clima | Gestor |
| `/comercio/nota-fiscal-xml` | Armazenamento e download de XMLs de NF-e | Gestor |
| `/notificacoes` | Central de notificações (e-mail/WhatsApp) | Gestor, Funcionário |
| `/acesso` (+ `/novo`, `/editar/[id]`) | Gestão de usuários e permissões | Gestor |
| `/admin` | Painel administrativo | Gestor |
| `/backup` | Disparo e monitoramento de backups | Gestor |
| `/perfil` | Perfil e preferências do usuário autenticado | Gestor, Funcionário |

---

## 🧩 Módulos e funcionalidades

| Módulo | Componentes-chave | Destaques |
|---|---|---|
| **Dashboard** | `CashFlow` (gráficos Chart.js: linha, barra, pizza), `BankBalanceCard` | Fluxo de caixa mensal, receitas por tipo, categorias de gasto, top produtos, saldo bancário em tempo real (BB) |
| **Clientes** | `ClientCard`, `ClientForm`, `ClientSelector`, `ClientSummaryCards` | CRUD completo, resumo de compras e seleção rápida para agrupamentos |
| **Compras** | `EnhancedUploadNotes`, `PurchaseFilesTable`, `InvoiceProductsModal` | Upload de notas, listagem paginada e edição de produtos por nota |
| **Conciliação bancária** | `UploadExtract`, `EnhancedUploadExtract` | Upload de extratos em PDF/CSV com validação |
| **Frete** | `Map` (Leaflet + OSRM), `AddressAutocomplete` (Google Places), `FreightConfigInfo` | Roteamento visual origem → destino e cálculo de custo |
| **Boletos** | `BilletsTable`, `ShowBilletModal`, `ShowBilletDataModal`, `WildcardBilletModal` | Geração, listagem de boletos em aberto, 2ª via, baixa manual (marcar como pago) e cancelamento de boletos Sicoob |
| **Notas fiscais** | `NotesTable`, `ShowInvoiceModal`, `ShowInvoiceDataModal` | Emissão (inclusive combinada com boleto vinculado), DANFE, XML e relatórios fiscais |
| **Agrupamento de vendas** | `GroupedProductsModal`, `CombinedScoresCards` | Visualização e gestão dos *scores combinados* por cliente |
| **Notificações** | `FavoritesModal`, `bulkNotificationService` | Disparo de e-mails/WhatsApp avulsos e em massa |
| **Backup** | tela `/backup` + `useBackup` | Disparo manual e consulta do uso de armazenamento do banco |
| **Acesso** | `UserCard`, `RoleGuard` | Cadastro, edição e controle de papéis dos usuários |

---

## 🔌 Camada de serviços e hooks

A comunicação com a API é centralizada em `src/services`, que chama `fetch` sempre com `credentials: "include"` contra `API_BASE_URL` (`src/config/api.ts`, lido de `NEXT_PUBLIC_API_URL`) — o cookie de sessão `httpOnly` é enviado automaticamente pelo navegador, então não há token para anexar manualmente em headers (`src/utils/httpUtils.ts` hoje só existe como um *wrapper* fino, sem lógica de `Authorization: Bearer`). Cada domínio tem um hook (`src/hooks`) que encapsula estado de carregamento/erro sobre o respectivo `service`:

| Hook | Service | Responsabilidade |
|---|---|---|
| `useAuth` | `authService` | Login, logout, verificação de sessão (`/auth/me`) e de papéis (`hasRole`) |
| `useClient` | `clientService` | CRUD de clientes, clientes com última compra, resumo |
| `useDashboard` | `dashboardService` | Dados agregados do dashboard (fluxo de caixa, rankings, totais) |
| `useBankBalance` | — | Saldo bancário em tempo real (`/finance/bank-balance`, via Banco do Brasil) |
| `useTransaction` | `transactionService` | Listagem paginada, filtros, totais, exportação (Excel/ZIP) |
| `useStatement` | `statementService` | Importação e listagem de extratos bancários |
| `useBillet` | `billetService` | Geração, consulta, listagem de boletos em aberto, baixa manual, 2ª via e cancelamento de boletos |
| `useInvoice` | `invoiceService` | Emissão (inclusive combinada com boleto), consulta, DANFE, XML e cancelamento de NF-e |
| `useGroupedProducts` | `groupedProductsService` | Agrupamento de vendas (*combined scores*) |
| `useReport` | `reportService` | Geração e exportação de relatórios |
| `useBackup` | `backupService` | Disparo e acompanhamento de backups |
| `useUpload` | — | Upload de arquivos com progresso |
| `useAutocomplete` | — | Autocomplete de endereços (Google Places) |

---

## 🔑 Variáveis de ambiente

Crie um arquivo `.env` na raiz de `Codigo/Front/` com as variáveis abaixo. **Nunca versionar em produção.**

O build falha (`scripts/check-env.mjs`, executado no `prebuild`) se `NEXT_PUBLIC_API_URL` ou `BACKEND_URL` não estiverem definidas — ou, em produção, se `NEXT_PUBLIC_API_URL` não for um caminho relativo (ex.: `/api`) ou `BACKEND_URL` não começar com `https://`.

```dotenv
# URL real do backend (Spring Boot) — usada só no servidor Next.js para o rewrite
# de /api/* em next.config.ts. Nunca é exposta ao navegador. Em produção precisa
# começar com https://.
BACKEND_URL=http://localhost:8080

# Caminho usado pelo navegador para chamar a API. Em desenvolvimento local aponta
# direto para o backend; em produção deve ser um caminho relativo (ex.: "/api")
# para que as chamadas sejam same-origin e o cookie httpOnly de login seja
# first-party (evita o bloqueio de cookies cross-site em Safari/iOS).
NEXT_PUBLIC_API_URL=/api

# Chave da API do Google Maps (autocomplete de endereços / cálculo de frete)
GOOGLE_MAPS_KEY=sua_api_key_google_maps

# E-mail da contabilidade exibido na interface
NEXT_PUBLIC_CONTABILIDADE_EMAIL=contabilidade@empresa.com

# Link da planilha de controle de notas (Google Sheets)
NEXT_PUBLIC_LINK_PLANILHA_NOTINHAS=https://docs.google.com/spreadsheets/d/SEU_ID/edit

# Link da planilha de listas maiores (Google Sheets)
NEXT_PUBLIC_LINK_PLANILHA_LISTAS_MAIORES=https://docs.google.com/spreadsheets/d/SEU_ID/edit
```

---

## 🚀 Instalação e execução

### Pré-requisitos

- Node.js 20+
- npm
- Backend do Hortifruti SL em execução (ver [README do backend](../Back/README.md))

### Passo a passo

```bash
# 1. Navegue até a pasta do frontend
cd Codigo/Front

# 2. Crie e preencha o arquivo .env (ver seção acima)

# 3. Instale as dependências
npm install

# 4. Suba o servidor de desenvolvimento
npm run dev
```

A aplicação fica disponível em `http://localhost:3000`.

### Scripts disponíveis

```bash
npm run dev           # Servidor de desenvolvimento (Next.js + Turbopack)
npm run build         # Build de produção
npm run start         # Executa o build de produção
npm run lint          # Verifica o código com Biome
npm run format        # Formata o código com Biome
npm run check-types   # Checagem de tipos TypeScript (tsc --noEmit)
```

### Observações importantes

- ⚠️ **`BACKEND_URL`** deve apontar para o backend correspondente (local ou produção) — é o destino do *rewrite* de `/api/*`
- ⚠️ Em produção, **`NEXT_PUBLIC_API_URL`** precisa ser um caminho relativo (ex.: `/api`) e **`BACKEND_URL`** precisa ser `https://` — o build falha caso contrário (`scripts/check-env.mjs`)
- ⚠️ Variáveis com prefixo **`NEXT_PUBLIC_`** ficam expostas no bundle do cliente — nunca usar para segredos
- 🗺️ O módulo de frete depende de `GOOGLE_MAPS_KEY` (autocomplete) e do serviço público de roteamento OSRM (Leaflet)
- 🔑 A sessão é mantida em um cookie `httpOnly` emitido pelo backend — o frontend nunca lê nem decodifica o JWT, apenas chama `GET /auth/me` para saber quem está autenticado e com quais papéis

---

## 🌐 Deploy em nuvem

A aplicação está publicada na **Vercel**, com build e deploy automáticos a partir do repositório.

**Site em produção:** https://www.hortifrutisl.zone.id

| Item | Configuração |
|---|---|
| Plataforma | Vercel |
| Framework preset | Next.js (App Router) |
| Build | `npm run build` |
| Variáveis de ambiente | Configuradas no painel da Vercel (mesmas do `.env` local — `NEXT_PUBLIC_API_URL=/api` e `BACKEND_URL=https://...` são obrigatórias nesse formato em produção) |

---

## 🎨 Padrão de código

O projeto usa **Biome** como linter e formatter único (substitui ESLint + Prettier), configurado em `biome.json` com 2 espaços de indentação, organização automática de imports e regras recomendadas para React e Next.js.

```bash
npm run lint      # biome check — analisa o código
npm run format    # biome format --write — formata o código
```

**Regras gerais:**
- Componentes e código em inglês/TypeScript idiomático; textos de interface em pt-BR
- Estilização via Tailwind CSS, com tokens de cor centralizados em `globals.css`
- Tipagem explícita via `src/types` — `services` e `hooks` não trafegam `any`

---

## 📦 Tecnologias e dependências

| Categoria | Tecnologia | Versão |
|---|---|---|
| Framework | Next.js (App Router) | 16.2 |
| Biblioteca de UI | React | 19.1 |
| Linguagem | TypeScript | 5 |
| Estilização | Tailwind CSS | 4 |
| Componentes de UI | Material UI (MUI) + Emotion | 7.3 |
| Ícones | Lucide React + React Icons | — |
| Gráficos | Chart.js + react-chartjs-2 | 4.5 / 5.3 |
| Mapas | Leaflet + react-leaflet (roteamento via OSRM público) | 1.9 / 5.0 |
| HTTP Client | `fetch` nativo (`credentials: "include"`) | — |
| Notificações (toast) | React Toastify | 11.0 |
| Lint & Format | Biome | 2.5 |
| Build | npm + Turbopack (via Next.js) | — |
| Deploy | Vercel | — |

---

<div align="center">
  <img width="70%" alt="pucminas" src="../../Documentacao/images/banner-institucional.svg"/>
</div>
<p align="center">Fonte do banner: <a href="https://github.com/joaopauloaramuni">João Paulo Carneiro Aramuni</a></p>

---
