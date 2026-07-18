<img width="1600" style="height:auto; border-radius: 12px;" alt="banner" src="../../Documentacao/images/banner.png" />

# Backend

> Sistema de gestão para o Hortifruti Santa Luzia LTDA — automatiza a conciliação bancária (extração de dados de extratos em PDF), o agrupamento de vendas por cliente, a emissão de boletos e notas fiscais, e centraliza informações operacionais do negócio em um dashboard único.

---

## 🛠️ Stack Principal

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth0-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Sicoob](https://img.shields.io/badge/Sicoob-Boletos-00A651?style=for-the-badge)
![Banco do Brasil](https://img.shields.io/badge/Banco_do_Brasil-Extratos-FDC300?style=for-the-badge)
![Focus NFe](https://img.shields.io/badge/Focus_NFe-NF--e-orange?style=for-the-badge)
![Cloudflare R2](https://img.shields.io/badge/Cloudflare_R2-Storage-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)
![Google Drive](https://img.shields.io/badge/Google_Drive-Backup-4285F4?style=for-the-badge&logo=googledrive&logoColor=white)
![SendGrid](https://img.shields.io/badge/SendGrid-E--mail-1A82E2?style=for-the-badge&logo=twilio&logoColor=white)
![OpenWeather](https://img.shields.io/badge/OpenWeather-Forecast-EB6E4B?style=for-the-badge&logo=openweathermap&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-Deploy-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)

---

## 📑 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [Arquitetura](#-arquitetura)
- [Estrutura de módulos](#-estrutura-de-módulos)
- [Estrutura de pastas](#-estrutura-de-pastas)
- [APIs e endpoints](#-apis-e-endpoints)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Instalação e execução](#-instalação-e-execução)
- [Deploy em nuvem](#-deploy-em-nuvem)
- [Padrão de código](#-padrão-de-código)
- [Regras de arquitetura](#-regras-de-arquitetura)
- [Tecnologias e dependências](#-tecnologias-e-dependências)

---

## 📖 Sobre o projeto

O backend do **Hortifruti SL** é uma API REST que sustenta toda a operação administrativa e financeira do Hortifruti Santa Luzia LTDA. Ele recebe extratos bancários e notas de compra em PDF/planilha, extrai e concilia as transações automaticamente, agrupa vendas por cliente em "scores combinados" e gera boletos (Sicoob) e notas fiscais eletrônicas (Focus NFe) a partir desses agrupamentos — inclusive em um fluxo combinado que emite a NF-e e o boleto vinculado em uma única chamada, com rollback automático da NF-e se o boleto falhar. Também consulta o saldo e o extrato da conta corrente em tempo real via API do Banco do Brasil, calcula frete (Google Maps), gera recomendações de compra baseadas em previsão do tempo (OpenWeather), envia notificações por e-mail (SendGrid) e WhatsApp (Ultramsg), faz backup do banco de dados no Google Drive, armazena boletos/XMLs/extratos no Cloudflare R2 e alimenta o dashboard consolidado consumido pelo frontend.

---

## 🏛️ Arquitetura

A aplicação segue uma **arquitetura em camadas** (*layered architecture*), com separação clara entre apresentação, regras de negócio e persistência — favorecendo testabilidade e organização por domínio.

```
┌──────────────────────────────────────────────────────────────┐
│                        Controller                            │
│         REST Controllers · Validação · Segurança             │
└────────────────────────┬─────────────────────────────────────┘
                         │ DTOs (request/response)
┌────────────────────────▼─────────────────────────────────────┐
│                         Service                              │
│   Regras de negócio · Orquestração · Integrações externas    │
└──────┬──────────────┬──────────────┬─────────────────┬───────┘
       │              │              │                 │
  Repository      Mapper         Exception          Config
  (Spring Data   (MapStruct/    (Tratamento de    (Segurança JWT,
   JPA / MySQL)   Lombok)        domínio)          rate limit,
                                                    integrações)
```

**Padrões centrais:**

| Padrão | Onde se aplica |
|---|---|
| DTO + Mapper (MapStruct) | Conversão entre entidades JPA e objetos de request/response |
| Autenticação via JWT em cookie `httpOnly` | `AuthController` emite o token em um cookie `auth_token` (`httpOnly`, `secure`/`SameSite` por ambiente); `JwtAuthenticationFilter` valida o token (cookie ou header `Authorization`) em toda requisição protegida |
| Controle de acesso por papel (`@PreAuthorize`) | Endpoints sensíveis restritos a `MANAGER` |
| Rate limiting | Bucket4J (`RateLimitingFilter`) protege endpoints por IP + rota; o IP real do cliente é resolvido via `X-Forwarded-For`, já que a app roda atrás dos proxies do Railway e do *rewrite* same-origin do Next.js |
| Token de serviço dedicado | Endpoints do `scheduler` exigem `API_SCHEDULER_TOKEN`, isolados do fluxo de usuário |
| Agendamento (`@Scheduled`) | Verificação automática de boletos vencidos e monitoramento de armazenamento do banco |
| Processamento assíncrono (`@Async`) | Notificações em massa e operações de backup não bloqueiam a requisição |
| mTLS (certificado `.pfx`) | Autenticação mútua compartilhada entre Sicoob (boletos) e Banco do Brasil (extratos) |
| Armazenamento de objetos (S3-compatible) | `R2StorageService` grava/lê/move boletos, XMLs de NF-e e extratos no Cloudflare R2 |

---

## 🧩 Estrutura de módulos

| Módulo (`service/…`) | Responsabilidade | Integração externa |
|---|---|---|
| `user` / `auth` | Cadastro, autenticação via JWT em cookie `httpOnly` e gestão de usuários (papéis: Gestor, Administrador) | — |
| `purchase` | Clientes, compras, agrupamento de vendas (`CombinedScore`) e produtos por nota | — |
| `finance` | Importação de extratos bancários (PDF), transações, categorização, exportação (Excel/ZIP) e consulta de saldo/extrato em tempo real (`BBSaldoService`, `TransactionBBService`) | Apache PDFBox · Apache POI · Banco do Brasil (mTLS) |
| `billet` | Geração, consulta, listagem de boletos em aberto, baixa manual (`mark-paid`), 2ª via e cancelamento de boletos | Sicoob (mTLS) |
| `invoice` | Emissão, consulta, cancelamento e armazenamento de notas fiscais eletrônicas (XML/DANFE), emissão combinada de NF-e + boleto (`IssueInvoiceWithBilletService`) e relatórios fiscais (ICMS, vendas, pagamentos) | Focus NFe |
| `storage` | Geração de chaves de objeto e upload/download/move de arquivos (boletos, XMLs, extratos) | Cloudflare R2 (S3-compatible) |
| `freight` | Cálculo de distância e frete entre endereços | Google Maps Distance Matrix |
| `climate` | Previsão do tempo e recomendações de compra de produtos sazonais | OpenWeather |
| `notification` | Envio de e-mails, mensagens de WhatsApp e notificações em massa para clientes e contabilidade | SendGrid · Ultramsg |
| `backup` | Autenticação OAuth2, geração de CSV e upload periódico de backups do banco | Google Drive |
| `chatbot` | Sessões e respostas automatizadas de atendimento via webhook | — |
| `scheduler` | Tarefas agendadas: verificação de vencimentos e monitoramento de armazenamento, protegidas por token de serviço | — |

---

## 📁 Estrutura de pastas

```
Back/
├── src/
│   └── main/
│       ├── java/com/hortifruti/sl/hortifruti/
│       │   ├── HortifrutiSlApplication.java
│       │   ├── controller/          # Controllers REST (user, purchase, finance, climate, notification…)
│       │   │   ├── climate/ · finance/ · notification/ · purchase/ · user/
│       │   ├── service/             # Services de domínio e integração
│       │   │   ├── backup/ · billet/ · chatbot/ · climate/ · finance/
│       │   │   ├── freight/ · invoice/ · notification/ · purchase/ · scheduler/ · storage/
│       │   ├── repository/          # Repositórios Spring Data JPA
│       │   ├── model/               # Entidades JPA + enumerations (Role, Status, Bank…)
│       │   ├── dto/                 # DTOs de request/response, organizados por domínio
│       │   ├── mapper/              # Mappers MapStruct (entidade ⇄ DTO)
│       │   ├── config/              # Segurança JWT, rate limiting, clientes HTTP, Swagger
│       │   │   ├── auth/ · bb/ · billet/ · climate/ · email/ · storage/
│       │   ├── exception/           # Exceções de domínio + tratamento global
│       │   └── util/                # Utilitários (Base64, datas, arquivos)
│       └── resources/
│           ├── application.properties
│           └── products.yml         # Catálogo de produtos para recomendação climática
├── Dockerfile                       # Build multi-stage (Maven → Eclipse Temurin JRE 25)
├── format.sh / format.bat           # Atalhos para formatação via Spotless
├── pom.xml
└── .env                             # Nunca versionar em produção
```

---

## 🌐 APIs e endpoints

Documentação interativa disponível em `http://localhost:8080/swagger-ui.html` após subir a aplicação.

### Autenticação e usuários

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth` | Login — emite o JWT em cookie `httpOnly` (`auth_token`) |
| `GET` | `/auth/me` | Retorna o usuário autenticado (lido do cookie/sessão) |
| `POST` | `/auth/logout` | Revoga o token e limpa o cookie de sessão |
| `POST` | `/users/register` | Cadastrar novo usuário |
| `GET` | `/users/all` | Listar usuários |
| `PUT` | `/users/update` · `/users/update/{id}` | Atualizar usuário (próprio ou por ID) |
| `GET` | `/users/count` | Contar usuários cadastrados |
| `DELETE` | `/users/delete/{username}` | Remover usuário |

### Clientes (`/clients`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/clients/register` | Cadastrar cliente |
| `GET` | `/clients` · `/clients/{id}` · `/clients/name/{name}` | Listar e buscar clientes |
| `PUT` | `/clients/{id}` | Atualizar cliente |
| `DELETE` | `/clients/{id}` | Remover cliente *(Gestor)* |
| `GET` | `/clients/with-last-purchase` | Clientes com dados da última compra |
| `GET` | `/clients/{id}/summary` · `/clients/for-selection` | Resumo do cliente e listagem para seleção |

### Compras e agrupamento de vendas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/purchases/process` | Upload e processamento de notas de compra (multipart) |
| `GET` | `/purchases/client/{clientId}/ordered` | Compras paginadas de um cliente |
| `GET` | `/purchases/{id}/products` · `/purchases/date-range` | Produtos da compra e busca por período |
| `DELETE` | `/purchases/{id}` | Remover compra |
| `POST` | `/combined-scores/create` | Criar agrupamento de vendas (score combinado) |
| `GET` | `/combined-scores` · `/combined-scores/{id}/grouped-products` | Listar agrupamentos e produtos agrupados |
| `DELETE` | `/combined-scores/{id}` | Cancelar agrupamento |
| `PUT` / `DELETE` | `/invoice-products/{id}` | Editar ou remover produto de uma nota |

### Boletos — Sicoob (`/billet`)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/billet/generate/{combinedScoreId}` | Gerar boleto em PDF (com vencimento opcional) |
| `GET` | `/billet/client/{clientId}` | Listar boletos de um cliente (pagador), com filtros opcionais |
| `GET` | `/billet/open` | Listar todos os boletos em aberto de todos os clientes |
| `GET` | `/billet/issue-copy/{idCombinedScore}` | Emitir 2ª via |
| `GET` | `/billet/{combinedScoreId}/file` | Baixar o PDF armazenado no R2 (sem emitir nova via no Sicoob) |
| `POST` | `/billet/cancel/{idCombinedScore}` | Cancelar (baixar) boleto |
| `PATCH` | `/billet/mark-paid/{combinedScoreId}` | Marcar boleto como pago manualmente (pagamento recebido fora do Sicoob) |
| `GET` | `/billet/{combinedScoreId}` | Consultar boleto pelo agrupamento |

### Notas fiscais — Focus NFe (`/invoices`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/invoices/issue/{combinedScoreId}` | Emitir NF-e |
| `POST` | `/invoices/issue-with-billet/{combinedScoreId}` | Emitir NF-e e o boleto vinculado em um único fluxo (com rollback automático da NF-e em caso de falha) |
| `GET` | `/invoices/consulta/{ref}` | Consultar status da nota |
| `GET` | `/invoices/{ref}/danfe` · `/invoices/{ref}/xml/download` | Baixar DANFE e XML |
| `DELETE` | `/invoices/{ref}/cancel` | Cancelar nota (com justificativa) |
| `GET` | `/invoices/xml-storage` · `/invoices/xml-storage/{ref}/download` | Consultar e baixar XMLs armazenados por período |
| `GET` | `/api/tax-reports/...` | Relatórios fiscais (ICMS, vendas, pagamentos, cadastro) |

### Financeiro (`/statements`, `/transactions`, `/finance/bank-balance`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/statements/import` | Importar extrato bancário (PDF, multipart) |
| `GET` | `/statements` | Listar extratos importados |
| `GET` | `/finance/bank-balance` | Saldo disponível em conta corrente, via API do Banco do Brasil *(Gestor)* |
| `GET` | `/transactions` | Transações filtradas e paginadas *(Gestor)* |
| `GET` | `/transactions/revenue` · `/expenses` · `/balance` | Receita, despesas e saldo por período *(Gestor)* |
| `GET` | `/transactions/categories` | Listar categorias de transação |
| `PUT` / `DELETE` | `/transactions/{id}` | Editar ou remover transação *(Gestor)* |
| `POST` | `/transactions/export` · `/export-complete` | Exportar como Excel ou ZIP com relatórios *(Gestor)* |

### Dashboard, produtos e clima

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/dashboard` | Visão consolidada do negócio (período, mês, ano) *(Gestor)* |
| `GET` | `/dashboard/icms-report/monthly/{start}/{end}` | Relatório mensal de ICMS |
| `GET` / `POST` / `PUT` / `DELETE` | `/products`, `/products/{id}`, `/products/search`, `/products/paginated`, `/products/count` | CRUD, busca e paginação de produtos *(Gestor)* |
| `GET` | `/api/weather/forecast/5days` | Previsão do tempo de 5 dias (Santa Luzia/MG) |
| `GET` | `/api/recommendations/by-temperature/{category}` · `/by-date` | Recomendações de compra por clima |

### Notificações, frete, backup e chatbot

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/notifications/client/documents` | Enviar documentos ao cliente (e-mail/WhatsApp) |
| `POST` | `/api/notifications/accounting/generic-files` | Enviar arquivos à contabilidade |
| `POST` | `/api/notifications/overdue/check` · `/send-bulk` | Verificar vencimentos e enviar notificações em massa |
| `POST` | `/distance` · `GET /distance/freight-config` | Calcular frete e consultar configuração |
| `POST` | `/backup` · `GET /backup/storage` | Disparar backup e consultar uso de armazenamento |
| `GET` | `/backup/oauth2callback` | Callback OAuth2 do Google Drive |
| `POST` / `GET` | `/chatbot/webhook` | Webhook de mensagens do chatbot |
| `GET` | `/scheduler/health` · `POST /scheduler/check-overdue` · `/check-database-storage` | Operações do scheduler — exigem `API_SCHEDULER_TOKEN` |

---

## 🔑 Variáveis de ambiente

Crie um arquivo `.env` na raiz de `Codigo/Back/` com as variáveis abaixo. **Nunca versionar em produção.**

Cada variável tem uma versão por ambiente (`LOCAL_`, `HML_`, `PROD_`), selecionada em runtime
pelo profile ativo (`SPRING_PROFILES_ACTIVE`). Veja `.env.example` para a lista completa —
resumo do essencial:

```dotenv
# ── Ambiente ativo ─────────────────────────────
SPRING_PROFILES_ACTIVE=local

# ── URLs (uma por ambiente) ────────────────────
LOCAL_FRONTEND_URL=http://localhost:3000
LOCAL_BACKEND_URL=http://localhost:8080
HML_FRONTEND_URL=
HML_BACKEND_URL=
PROD_FRONTEND_URL=
PROD_BACKEND_URL=

# ── MySQL (uma por ambiente) ────────────────────
LOCAL_MYSQLHOST=localhost
LOCAL_MYSQLPORT=3306
LOCAL_MYSQLDATABASE=hortifruti_sl
LOCAL_MYSQLUSER=root
LOCAL_MYSQLPASSWORD=root
HML_MYSQLHOST=
HML_MYSQLPORT=
HML_MYSQLDATABASE=
HML_MYSQLUSER=
HML_MYSQLPASSWORD=
PROD_MYSQLHOST=
PROD_MYSQLPORT=
PROD_MYSQLDATABASE=
PROD_MYSQLUSER=
PROD_MYSQLPASSWORD=

# ── Autenticação ───────────────────────────────
JWT_SECRET=sua_chave_secreta_jwt_com_pelo_menos_32_caracteres
API_SCHEDULER_TOKEN=token_seguro_para_endpoints_scheduler

# ── Google ─────────────────────────────────────
# Não há GOOGLE_REDIRECT_URI: a URI de callback é derivada de
# LOCAL_BACKEND_URL/HML_BACKEND_URL/PROD_BACKEND_URL + /backup/oauth2callback (fixo).
CREDENTIALS_GOOGLE=sua_api_key_google_maps
GOOGLE_DRIVE_CREDENTIALS=suas_credenciais_google_drive_em_base64

# ── Sicoob (boletos) ───────────────────────────
SICOOB_CLIENT_ID=seu_client_id_sicoob
SICOOB_API_URL=https://api.sicoob.com.br
SICOOB_AUTH_URL=https://auth.sicoob.com.br
SICOOB_SCOPE=cobranca_boletos
SICOOB_NUM_CLIENTE=numero_cliente_sicoob
SICOOB_NUM_CONTA_CORRENTE=numero_conta_corrente

# ── Certificado .pfx (mTLS — compartilhado entre Sicoob e Banco do Brasil) ─
DOCUMENT_PFX=certificado_digital_em_base64
PASSWORD_PFX=senha_do_certificado_pfx

# ── Banco do Brasil (consulta de saldo/extrato) ─
BB_APP_KEY=app_key_bb
BB_CLIENT_ID=client_id_bb
BB_CLIENT_SECRET=client_secret_bb
BB_REGISTRATION_ACCESS_TOKEN=registration_access_token_bb
BB_BASIC=credencial_basic_auth_bb
BB_AGENCIA=numero_agencia
BB_CONTA=numero_conta
BB_SCOPE=extrato-info

# ── OpenWeather ────────────────────────────────
API_TOKEN=sua_api_key_openweather
API_URL=https://api.openweathermap.org/data/2.5/forecast

# ── Focus NFe (uma por ambiente, exceto local que reaproveita a de HML) ─
HML_FOCUS_NFE_TOKEN=seu_token_focus_nfe_homologacao
HML_FOCUS_NFE_API_URL=https://homologacao.focusnfe.com.br
PROD_FOCUS_NFE_TOKEN=seu_token_focus_nfe_producao
PROD_FOCUS_NFE_API_URL=https://api.focusnfe.com.br
FOCUS_NFE_CNPJ_EMITENTE=cnpj_da_empresa_emitente
COMPANY_NAME=Nome da Empresa LTDA
COMPANY_STATE_REGISTRATION=inscricao_estadual
COMPANY_CNPJ=cnpj_da_empresa

# ── Notificações ───────────────────────────────
ULTRAMSG_TOKEN=token_ultramsg_whatsapp
ULTRAMSG_INSTANCE_ID=instance_id_ultramsg
CHATBOT_WEBHOOK_SECRET=segredo_estatico_do_webhook_do_chatbot
SENDGRID_API_KEY=sua_api_key_sendgrid
SENDGRID_FROM_EMAIL=noreply@seudominio.com
ACCOUNTING_EMAIL=contabilidade@empresa.com
ACCOUNTING_WHATSAPP=5531999999999
OVERDUE_NOTIFICATION_EMAILS=email1@empresa.com,email2@empresa.com

# ── Cloudflare R2 (storage de boletos, XMLs e extratos) ─
R2_ACCOUNT_ID=id_da_conta_cloudflare
R2_ACCESS_KEY_ID=access_key_id_r2
R2_SECRET_ACCESS_KEY=secret_access_key_r2
R2_BUCKET_NAME=nome_do_bucket
R2_ENDPOINT=https://<account_id>.r2.cloudflarestorage.com
```

---

## 🚀 Instalação e execução

### Pré-requisitos

- Java 25+
- Maven 3.9+
- MySQL 8.0+
- Docker (opcional, para build containerizado)

### Passo a passo

```bash
# 1. Navegue até a pasta do backend
cd Codigo/Back

# 2. Crie e preencha o arquivo .env (ver seção acima)

# 3. Formate o código (recomendado antes de qualquer commit)
./format.sh        # ou format.bat no Windows

# 4. Suba a aplicação
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080` e cria/atualiza o banco `hortifruti_sl` automaticamente (`createDatabaseIfNotExist=true`). A documentação interativa da API fica disponível em `http://localhost:8080/swagger-ui.html`.

### Build e empacotamento

```bash
# Gerar o JAR
./mvnw clean package -DskipTests

# Build da imagem Docker (multi-stage: Maven builder + Eclipse Temurin 25 JRE)
docker build -t hortifruti-backend .
```

### Observações importantes

- ⚠️ **MySQL** precisa estar acessível antes de iniciar a aplicação (`ddl-auto=update` em todos os perfis — o schema é atualizado automaticamente)
- ⚠️ **`JWT_SECRET`** deve ter pelo menos 32 caracteres
- ⚠️ **Certificado Sicoob/BB (`DOCUMENT_PFX`)** deve estar em Base64 — usado para autenticação mútua (mTLS) na emissão de boletos e na consulta de extratos do Banco do Brasil
- 📁 Diretórios temporários (`temp/cert`, `temp/google`, `temp/notifications`) são criados automaticamente
- 🕐 **Timezone**: `America/Sao_Paulo` · **Formato de data**: `dd/MM/yyyy`

---

## 🌐 Deploy em nuvem

A aplicação é empacotada via **Docker multi-stage** (build com Maven + runtime em Eclipse Temurin 25 JRE, executando como usuário não-root) e publicada no **Railway**, que injeta a variável `PORT` dinamicamente.

```
Build (Maven 3.9 + Temurin 25)
    │  mvn clean package -DskipTests
    ▼
Runtime (Eclipse Temurin 25 JRE · usuário não-root)
    │  JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC
    │  SPRING_PROFILES_ACTIVE=prod
    ▼
java -Dserver.port=$PORT -jar app.jar     ← Railway injeta $PORT
```

| Item | Configuração |
|---|---|
| Imagem base (build) | `maven:3.9.16-eclipse-temurin-25` |
| Imagem base (runtime) | `eclipse-temurin:25-jre-jammy` |
| Usuário | `appuser` (não-root, uid/gid 1000) |
| Diretórios voláteis | `/app/temp/{cert,google,notifications}` |
| Variável injetada pela plataforma | `PORT` |

---

## 🎨 Padrão de código

O projeto usa **Google Java Format** via **Spotless**, com atalhos prontos:

```bash
./format.sh             # ou format.bat — formata o código (mvnw spotless:apply)
./mvnw spotless:check   # valida a formatação sem alterar arquivos
```

**Regras gerais:**
- Código em inglês
- Comentários, logs e mensagens de exceção em pt-BR
- DTOs e mapeamentos via MapStruct — entidades JPA não são expostas diretamente nos controllers

---

## 🔒 Regras de arquitetura

| Regra | Motivação |
|---|---|
| `spring.jpa.hibernate.ddl-auto` definido por perfil (`application-{local,hml,prod}.properties`) | Cada ambiente controla sua própria estratégia de schema — hoje `update` em todos os perfis |
| `open-in-view=false` | Previne sessões Hibernate abertas durante a renderização da resposta (N+1 fora da camada de serviço) |
| Controllers nunca acessam `repository` diretamente | Toda regra de negócio passa pela camada `service` |
| Endpoints sensíveis exigem `@PreAuthorize("hasRole('MANAGER')")` | Controle de acesso por papel centralizado na camada de segurança |
| `JwtAuthenticationFilter` nunca é *bypassado* | Endpoints públicos são declarados explicitamente na configuração de segurança |
| JWT trafega em cookie `httpOnly` | Reduz exposição a XSS em relação a manter o token em `localStorage` |
| Endpoints do `scheduler` exigem `API_SCHEDULER_TOKEN` | Isola tarefas automatizadas do fluxo de autenticação de usuários |
| Integração com Sicoob e Banco do Brasil via certificado `.pfx` compartilhado (mTLS) | Exigência das próprias instituições financeiras para boletos e extratos |
| Operações de notificação e backup executam via `@Async` | Evita bloquear a thread da requisição HTTP em chamadas a serviços externos |
| Variáveis sensíveis somente via `.env` / variáveis de ambiente | Nunca *hardcoded* — chaves, tokens e certificados nunca expostos no código |

---

## 📦 Tecnologias e dependências

| Categoria | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 25 |
| Framework | Spring Boot (Web, WebFlux, WebSocket, Validation, Security, Data JPA) | 4.1.0 |
| Banco relacional | MySQL + HikariCP | 8.0 |
| Segurança JWT | java-jwt (Auth0) | 4.5.2 |
| Documentação | Springdoc OpenAPI (Swagger UI) | 3.0.3 |
| Mapeamento DTO | MapStruct + Lombok | 1.6.3 / 1.18.46 |
| Variáveis de ambiente | spring-dotenv | 5.1.0 |
| Rate limiting | Bucket4J (core + jcache) | 8.0.1 |
| PDF | Apache PDFBox | 3.0.7 |
| Planilhas | Apache POI OOXML | 5.5.1 |
| Compressão | Zip4j | 2.11.5 |
| HTTP Client | Apache HttpComponents Client5 | 5.6.1 |
| CSV | Apache Commons CSV | 1.14.1 |
| Google APIs (Drive/OAuth/Maps) | google-api-client / google-oauth-client-jetty / google-api-services-drive | 2.8.1 / 1.39.0 / v3-rev20220815-2.0.0 |
| Storage (S3-compatible) | AWS SDK for Java (S3) — usado com Cloudflare R2 | 2.29.52 |
| E-mail | SendGrid Java | 4.10.0 |
| WhatsApp | Ultramsg (REST) | — |
| NF-e | Focus NFe (REST) | — |
| Boletos | Sicoob (REST + mTLS) | — |
| Extratos bancários | Banco do Brasil — API Extratos (REST + mTLS) | — |
| Clima | OpenWeather (REST) | — |
| JSON | Jackson + org.json | 20260522 |
| Formatação | Spotless + Google Java Format | 3.8.0 / 1.28.0 |
| Build | Maven | 3.9+ |
| Containers | Docker (multi-stage) | — |
| Deploy | Railway | — |

---

<div align="center">
  <img width="70%" alt="pucminas" src="../../Documentacao/images/banner-institucional.svg"/>
</div>
<p align="center">Fonte do banner: <a href="https://github.com/joaopauloaramuni">João Paulo Carneiro Aramuni</a></p>

---
