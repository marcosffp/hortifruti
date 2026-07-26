<img width="1600" style="height:auto; border-radius: 12px;" alt="banner" src="Documentacao/images/banner.png" />

# Hortifruti SL

> Sistema de gestão para o Hortifruti Santa Luzia LTDA, focado em automatizar processos manuais críticos: conciliação bancária via extração de dados de PDF e agrupamento de vendas por cliente. O software visa eliminar tarefas repetitivas, centralizar informações e fornecer controle operacional, modernizando a gestão do negócio e promovendo eficiência.

---

## 🛠️ Stack Principal

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-16.2-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React-19.1-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth0-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Sicoob](https://img.shields.io/badge/Sicoob-Boletos-00A651?style=for-the-badge&labelColor=00A651)
![Banco do Brasil](https://img.shields.io/badge/Banco_do_Brasil-Extratos-F9DD16?style=for-the-badge&labelColor=0038A8)
![Focus NFe](https://img.shields.io/badge/Focus_NFe-NF--e-FF7A00?style=for-the-badge)
![Google Drive](https://img.shields.io/badge/Google_Drive-Backup-4285F4?style=for-the-badge&logo=googledrive&logoColor=white)
![SendGrid](https://img.shields.io/badge/SendGrid-E--mail-1A82E2?style=for-the-badge)
![OpenWeather](https://img.shields.io/badge/OpenWeather-Forecast-EB6E4B?style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Cloudflare R2](https://img.shields.io/badge/Cloudflare_R2-Storage-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-Backend-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-Frontend-000000?style=for-the-badge&logo=vercel&logoColor=white)


---

## 📑 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [Acesso ao sistema](#-acesso-ao-sistema)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Equipe](#-equipe)
- [Sprints do projeto](#-sprints-do-projeto)
- [Instalação e execução](#-instalação-e-execução)
- [Funcionalidades e integrações](#-funcionalidades-e-integrações)
- [Tecnologias e dependências](#-tecnologias-e-dependências)

---

## 📖 Sobre o projeto

O **Hortifruti SL** é um sistema de gestão desenvolvido para o Hortifruti Santa Luzia LTDA, com o objetivo de digitalizar e automatizar processos administrativos e financeiros que hoje são feitos manualmente. Entre os principais focos estão a **conciliação bancária** (extração e categorização automática de transações a partir de extratos em PDF, com consulta de saldo em tempo real via API do Banco do Brasil), o **agrupamento de vendas por cliente** para geração de cobranças consolidadas, a **emissão de boletos** (Sicoob) e **notas fiscais eletrônicas** (Focus NFe) — inclusive de forma combinada (NF-e + boleto vinculado em um único fluxo) —, o **cálculo de frete**, **recomendações de compra** baseadas em previsão do tempo e um **dashboard** com visão consolidada do negócio — tudo isso com notificações automáticas por e-mail e WhatsApp e armazenamento de arquivos (boletos, XMLs, extratos) na nuvem via Cloudflare R2.

O sistema é dividido em duas aplicações independentes que se comunicam via API REST:

| Aplicação | Stack | Documentação |
|---|---|---|
| **Backend** | Java 25 + Spring Boot 4.1 + MySQL | [`Codigo/Back/README.md`](Codigo/Back/README.md) |
| **Frontend** | Next.js 16 + React 19 + TypeScript + Tailwind CSS | [`Codigo/Front/README.md`](Codigo/Front/README.md) |

---

## 🌐 Acesso ao Sistema

**Site em Produção:** https://www.hortifrutisl.zone.id

### 👤 Credenciais de Acesso

| Papel | Usuário | Senha |
|---|---|---|
| Gestor | `root` | `root` |
| Administrador | `admin` | `admin` |

---

## 🗂️ Estrutura do repositório

```
plf-es-2025-2-ti4-1247100-hortifruti-sl/
├── Artefatos/        # Atas, modelagem (ER, casos de uso, classes), planejamento de sprints, protótipos
├── Codigo/
│   ├── Back/         # API REST — Java + Spring Boot
│   └── Front/        # Aplicação web — Next.js + TypeScript
├── Divulgacao/
│   ├── Apresentacao/ # Slides e materiais de apresentação
│   └── Video/        # Vídeos demonstrativos e da Mostra
├── Documentacao/     # Relatório técnico completo (PDF) e imagens institucionais
└── README.md         # Este arquivo
```

Cada diretório possui seu próprio `README.md` detalhando seu conteúdo — veja [`Artefatos`](Artefatos/README.md), [`Codigo`](Codigo/README.md), [`Divulgacao/Apresentacao`](Divulgacao/Apresentacao/README.md), [`Divulgacao/Video`](Divulgacao/Video/README.md) e [`Documentacao`](Documentacao/README.md).

---

## 👥 Equipe

### Alunos integrantes

* Bernado Souza Alvim
* Carlos José Gomes Batista Figueiredo
* Gabriela Alvarenga Cardoso
* Marcos Alberto Ferreira Pinto
* Mateus Araujo Santos
* Rafael Ganascini de Moura

### Professores responsáveis

* Soraia Lúcia da Silva
* Lucila Ishitani

---

## 🚀 Sprints do projeto

### Sprint 1 — Planejamento e Definições Iniciais

Nesta sprint inicial, a equipe realizou o Kick Off com o cliente Vlanney Gualberto para compreender as necessidades do Hortifruti Santa Luzia LTDA. Foram definidos os requisitos funcionais e não funcionais do projeto, além da escolha das tecnologias a serem utilizadas: Java com Spring Boot para o backend e Next.js com TypeScript para o frontend. Também foram elaborados documentos essenciais como a Ata de Acordo, Termo de Sigilo e Confidencialidade, e a Procuração NIT.

A organização inicial do repositório GitHub foi estabelecida, criando a estrutura de pastas para Artefatos, Código, Divulgação e Documentação. A equipe preparou os slides da primeira apresentação e iniciou a documentação do projeto no Overleaf, estabelecendo as bases para o desenvolvimento nas sprints seguintes.

### Sprint 2 — Fundamentos do Sistema

Durante a Sprint 2, foram criados os diagramas fundamentais do sistema: Diagrama de Entidade-Relacionamento (ER) e Diagrama de Caso de Uso. Os protótipos de telas foram desenvolvidos para validação com o cliente. A implementação iniciou com funcionalidades essenciais como o cadastro e gerenciamento de usuários (RF001), cadastro e edição de clientes (RF002-RF004), e a tela de login junto com a home page.

Também foram implementadas funcionalidades críticas para o negócio: upload e visualização de extratos bancários (RF005-RF006), cálculo e visualização de frete (RF007-RF008), cadastro de produtos (RF009) e o sistema de recomendação de compras (RF010). Esta sprint estabeleceu a base operacional do sistema com os módulos de usuários, clientes e a estrutura inicial de lançamentos financeiros.

### Sprint 3 — Gestão de Compras e Dashboard

A Sprint 3 focou na expansão das funcionalidades de gestão comercial. Foi implementado o upload de notas de compra (RF014), listagem de arquivos (RF015) e a seleção de clientes com configuração de período (RF016-RF017). O módulo de visualização de informações do cliente (RF018) permitiu centralizar dados importantes para análise.

O dashboard do Hortifruti (RF024) foi desenvolvido, oferecendo uma visão consolidada das operações do negócio. A sprint também incluiu a conclusão do cadastro de produtos e visualização de recomendações de compra. Os diagramas de Caso de Uso, ER e Lógico foram atualizados para refletir as novas funcionalidades implementadas.

### Sprint 4 — Boletos e Notificações

Esta sprint concentrou-se no sistema de boletos e comunicação com clientes. Foram implementados filtros por tipo/categoria (RF011), busca de lançamentos (RF012), envio de arquivos (RF019) e personalização de mensagens (RF020). O sistema de notificações ganhou canal de envio configurável (RF021) e alertas automáticos de vencimento (RF022).

O módulo de agrupamento de vendas foi finalizado com confirmação e cancelamento de agrupamentos (RF023, RF025). A geração de boletos (RF026) com download em PDF (RF027), baixa de boleto (RF028) e consulta de boletos pendentes via WhatsApp (RF029-RF030) completaram o ciclo financeiro. Requisitos não funcionais como verificação automatizada de vencimentos, backup automatizado e monitoramento de capacidade também foram implementados.

### Sprint 5 — Documentação e Entrega Final

A Sprint 5 foi dedicada à finalização do projeto e preparação para entrega. A documentação completa foi atualizada no Overleaf, incluindo metodologia, resultados obtidos, conclusão e referências bibliográficas. A ata da reunião final com o cliente foi preparada para formalizar a entrega do sistema.

A equipe elaborou os slides da apresentação final e conduziu avaliação pelos usuários através de questionário para validar a aceitação do sistema. O resumo para a Mostra foi preparado, o vídeo demonstrativo foi criado, e a organização final do GitHub Classroom foi realizada para garantir a entrega adequada de todos os artefatos do projeto.

---

## 🚀 Instalação e execução

### Pré-requisitos

- **Java 25** (JDK)
- **Maven 3.9+**
- **Node.js 20+** e **npm**
- **MySQL 8.0+**

### Visão geral

```bash
# 1. Clone o repositório
git clone https://github.com/ICEI-PUC-Minas-PPLES-TI/plf-es-2025-2-ti4-1247100-hortifruti-sl.git
cd plf-es-2025-2-ti4-1247100-hortifruti-sl

# 2. Backend — configure o .env e suba a API (porta 8080)
cd Codigo/Back
# crie e preencha o .env — ver Codigo/Back/README.md
./mvnw spring-boot:run

# 3. Frontend — em outro terminal, configure o .env e suba a aplicação (porta 3000)
cd Codigo/Front
# crie e preencha o .env — ver Codigo/Front/README.md
npm install
npm run dev
```

As instruções completas de configuração — incluindo todas as variáveis de ambiente, integrações externas, scripts de build, padrões de código e deploy — estão documentadas em cada subprojeto:

- 🔧 **Backend**: [`Codigo/Back/README.md`](Codigo/Back/README.md) — endpoints REST, módulos, variáveis de ambiente, Docker e deploy no Railway
- 🎨 **Frontend**: [`Codigo/Front/README.md`](Codigo/Front/README.md) — rotas, papéis de acesso, componentes, hooks/serviços e deploy na Vercel

### Banco de Dados

1. Instale e configure o **MySQL 8.0+**
2. O banco (`hortifruti_sl`) é criado automaticamente na primeira execução (`createDatabaseIfNotExist=true`), com schema gerenciado manualmente (`ddl-auto=none`)
3. **Timezone**: `America/Sao_Paulo` · **Formato de data**: `dd/MM/yyyy`

### Troubleshooting

| Sintoma | Verificação |
|---|---|
| Erro de conexão com banco | MySQL está rodando e as credenciais do prefixo correspondente ao ambiente ativo (`LOCAL_`/`HML_`/`PROD_MYSQLUSER`/`MYSQLPASSWORD`) estão corretas |
| Erro de JWT | `JWT_SECRET` tem pelo menos 32 caracteres |
| Erro de certificado Sicoob/BB | Arquivo `.pfx` está corretamente codificado em Base64 em `DOCUMENT_PFX` (certificado mTLS compartilhado entre Sicoob e Banco do Brasil) |
| Falha em integrações externas | Chaves de API (Sicoob, Banco do Brasil, Google, Focus NFe, SendGrid, Ultramsg, OpenWeather, Cloudflare R2) válidas e com permissões necessárias |

---

## 🔧 Funcionalidades e integrações

#### Módulos do sistema

- **Gestão de Usuários**: cadastro e autenticação JWT, controle por papéis (Gestor, Funcionário, Contador)
- **Gestão de Clientes**: CRUD completo com informações de contato e histórico de compras
- **Conciliação Bancária**: upload e processamento automático de extratos em PDF
- **Gestão de Compras**: upload de notas, agrupamento de vendas por cliente (*combined scores*)
- **Sistema de Boletos**: integração com Sicoob para geração, consulta, baixa manual/automática e cancelamento de cobranças
- **Nota Fiscal Eletrônica**: integração com Focus NFe para emissão, DANFE e XML — inclusive emissão combinada de NF-e + boleto vinculado
- **Saldo Bancário**: consulta em tempo real do saldo em conta via API do Banco do Brasil
- **Notificações**: envio por e-mail (SendGrid) e WhatsApp (Ultramsg), avulso e em massa
- **Gestão de Produtos**: cadastro e recomendações de compra baseadas em clima
- **Cálculo de Frete**: rotas e distância via Google Maps, visualização em mapa interativo
- **Dashboard**: visão consolidada das operações com gráficos financeiros
- **Backup**: armazenamento automatizado no Google Drive

#### Integrações externas

| Integração | Uso |
|---|---|
| **Sicoob** | Geração e consulta de boletos bancários (mTLS com certificado digital) |
| **Banco do Brasil** | Consulta de saldo e extrato da conta corrente (API Extratos, mTLS com o mesmo certificado do Sicoob) |
| **Focus NFe** | Emissão e gestão de notas fiscais eletrônicas |
| **Google Maps** | Cálculo de frete, rotas e autocomplete de endereços |
| **Google Drive** | Armazenamento de backups do banco de dados |
| **Cloudflare R2** | Armazenamento de boletos, XMLs de NF-e e extratos bancários (S3-compatible) |
| **SendGrid** | Envio de e-mails transacionais |
| **Ultramsg** | Envio de mensagens via WhatsApp |
| **OpenWeather** | Previsão do tempo para recomendações de compra (Santa Luzia/MG) |

#### Segurança

- Autenticação via **JWT** entregue em cookie `httpOnly` (não em `localStorage`), com o frontend chamando a API em same-origin (`/api/*`) via *rewrite* do Next.js — evita bloqueio de cookies cross-site em navegadores como Safari/iOS
- Controle de acesso por papel (`@PreAuthorize` no backend, `RoleGuard` no frontend)
- Cabeçalhos de segurança (CSP com nonce, HSTS, `X-Frame-Options`) aplicados via *middleware* do Next.js
- Rate limiting por IP real do cliente (resolvido via `X-Forwarded-For`, considerando os proxies do Railway e do Next.js)
- Certificados digitais (`.pfx`) para integração bancária (Sicoob e Banco do Brasil)
- Segredos e credenciais sempre via variáveis de ambiente — nunca versionados

---

## 📦 Tecnologias e dependências

| Camada | Tecnologias principais |
|---|---|
| **Backend** | Java 25 · Spring Boot 4.1 (Web, WebFlux, Security, Data JPA) · MySQL + HikariCP · JWT (Auth0) · Springdoc OpenAPI (Swagger UI) · MapStruct · Bucket4J (rate limiting) · Apache PDFBox/POI · Apache HttpComponents Client5 · AWS SDK S3 (Cloudflare R2) |
| **Frontend** | Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS 4 · Material UI · Chart.js · Leaflet/OSRM · Biome |
| **Integrações bancárias/fiscais** | Sicoob (boletos, mTLS) · Banco do Brasil (extratos, mTLS) · Focus NFe (NF-e) |
| **Integrações de suporte** | Google Drive (backup) · Google Maps (frete) · SendGrid (e-mail) · Ultramsg (WhatsApp) · OpenWeather (clima) |
| **Infraestrutura** | MySQL 8.0 · Cloudflare R2 (storage) · Docker (multi-stage) · Railway (backend) · Vercel (frontend) |
| **Qualidade** | Spotless + Google Java Format (backend) · Biome (frontend) |

> A lista completa de dependências e versões está nas tabelas "Tecnologias e dependências" de cada subprojeto: [Backend](Codigo/Back/README.md#-tecnologias-e-dependências) · [Frontend](Codigo/Front/README.md#-tecnologias-e-dependências)

---

<div align="center">
  <img width="70%" alt="pucminas" src="Documentacao/images/banner-institucional.svg"/>
</div>
<p align="center">Fonte do banner: <a href="https://github.com/joaopauloaramuni">João Paulo Carneiro Aramuni</a></p>

---
