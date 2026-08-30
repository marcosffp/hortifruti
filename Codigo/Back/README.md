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
![Sicoob](https://img.shields.io/badge/Sicoob-Boletos_%26_Extratos-00A651?style=for-the-badge&labelColor=00A651)
![Banco do Brasil](https://img.shields.io/badge/Banco_do_Brasil-Saldo_%26_Extratos-F9DD16?style=for-the-badge&labelColor=0038A8)
![Focus NFe](https://img.shields.io/badge/Focus_NFe-NF--e-FF7A00?style=for-the-badge)
![Cloudflare R2](https://img.shields.io/badge/Cloudflare_R2-Storage-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)
![Google Drive](https://img.shields.io/badge/Google_Drive-Backup-4285F4?style=for-the-badge&logo=googledrive&logoColor=white)
![E-mail](https://img.shields.io/badge/E--mail-SendGrid_%7C_Gmail-1A82E2?style=for-the-badge)
![OpenWeather](https://img.shields.io/badge/OpenWeather-Forecast-EB6E4B?style=for-the-badge)
![Railway](https://img.shields.io/badge/Railway-Deploy-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini-Extração_de_notas-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-Tempo_real-010101?style=for-the-badge&logo=socketdotio&logoColor=white)

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

O backend do **Hortifruti SL** é uma API REST que sustenta toda a operação administrativa e financeira do Hortifruti Santa Luzia LTDA. Ele recebe extratos bancários e notas de compra em PDF/planilha/foto, extrai e concilia as transações automaticamente, agrupa vendas por cliente em "scores combinados" e gera boletos (Sicoob) e notas fiscais eletrônicas (Focus NFe) a partir desses agrupamentos — inclusive em um fluxo combinado que emite a NF-e e o boleto vinculado em uma única chamada, com rollback automático da NF-e se o boleto falhar, e um fluxo de reconciliação que libera o agrupamento para reemissão quando uma NF-e ficou em erro/denegada/cancelada, evitando duplicidade. A captura de notas de compra por foto conta com extração via Gemini (com retry e backoff exponencial em erros 503), fila de revisão para capturas com falha, reprocessamento reaproveitando a imagem original sem exigir nova foto, e pareamento de dispositivo móvel (código de 6 dígitos + `device_token` em cookie httpOnly) para que o celular envie fotos direto para a fila, com notificação em tempo real via WebSocket quando a extração termina. Também consulta o saldo em tempo real e importa extratos (BB e Sicoob), calcula frete (Google Maps), gera recomendações de compra baseadas em previsão do tempo (OpenWeather), envia notificações por e-mail (SendGrid, Gmail SMTP ou Gmail API — provedor plugável) e WhatsApp (Ultramsg, incluindo um atendente automatizado/chatbot), faz backup do banco de dados no Google Drive, armazena boletos/XMLs/extratos no Cloudflare R2 e alimenta o dashboard consolidado consumido pelo frontend. A autenticação usa JWT de curta duração com refresh token rotativo, e o login é protegido contra brute-force com lockout progressivo e auditoria de tentativas.

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
| Access token + refresh token rotativo, ambos em cookie `httpOnly` | `AuthController` emite `auth_token` (curta duração) e `refresh_token` no login; `POST /auth/refresh` rotaciona o refresh token a cada uso — reuso de um token já revogado é tratado como vazamento e derruba todas as sessões ativas do usuário (`RefreshTokenService`) |
| Proteção de login contra brute-force | `LoginProtectionService` aplica lockout progressivo (15min → 1h → 24h) por conta **e** por IP, com e-mail de alerta (`SECURITY_ALERT_EMAILS`) e auditoria de toda tentativa (`LoginAuditLog`/`LoginLockout`), sem depender de Redis |
| Controle de acesso por papel (`@PreAuthorize`) | Endpoints sensíveis restritos a `MANAGER` |
| Rate limiting | Bucket4J (`RateLimitingFilter`) protege endpoints por IP + rota; o IP real do cliente é resolvido via `X-Forwarded-For`, já que a app roda atrás dos proxies do Railway e do *rewrite* same-origin do Next.js |
| Agendamento (`@Scheduled`) | Limpeza diária de refresh tokens expirados |
| Processamento assíncrono (`@Async`) | Notificações em massa e operações de backup não bloqueiam a requisição |
| mTLS (certificados `.pfx` + `.pem`) | Autenticação mútua compartilhada entre Sicoob (boletos e extrato) e Banco do Brasil (saldo e extrato) |
| Provedor de e-mail plugável (`EmailSender`) | `EMAIL_PROVIDER` escolhe em runtime entre SendGrid, Gmail SMTP e Gmail API (reaproveitando a autorização OAuth do backup no Drive), sem trocar código |
| Armazenamento de objetos (S3-compatible) | `R2StorageService` grava/lê/move boletos, XMLs de NF-e e extratos no Cloudflare R2 |
| Fila de captura de notas + reprocessamento | Fotos de notas de compra (upload direto ou via dispositivo pareado) caem numa fila de revisão (`CapturaNotaPendenteService`); capturas com erro de extração podem ser reprocessadas reaproveitando a imagem já salva no R2, sem pedir nova foto |
| Retry com backoff exponencial | `GeminiExtractionService` reexecuta a extração em erro `503` da API do Gemini, com nº de tentativas e espera inicial configuráveis (`GEMINI_RETRY_MAX_TENTATIVAS`, `GEMINI_RETRY_ESPERA_INICIAL_MS`) |
| Reconciliação de emissões fiscais | `POST /invoices/{combinedScoreId}/reconciliar` consulta o status real na Focus NFe e libera o agrupamento para nova emissão quando a NF-e ficou em erro/denegada/cancelada, evitando NF-e ou boleto duplicados |
| Pareamento de dispositivo móvel | Código de uso único de 6 dígitos (TTL configurável) autentica o celular sem login completo; o `device_token` resultante trafega só em cookie `httpOnly` (nunca no corpo da resposta) e expira por inatividade prolongada |
| Notificação em tempo real (WebSocket) | Ticket de curta duração (`POST /realtime/ws-ticket`) autentica a conexão WebSocket que avisa a fila de capturas pendentes assim que uma extração termina |
| Migrações de schema versionadas (Flyway) | `spring-boot-starter-flyway` aplica os scripts de `resources/db/migration` (`V1`…`V15`) na subida da aplicação; convive, por ora, com `ddl-auto=update` em todos os perfis |

---

## 🧩 Estrutura de módulos

| Módulo (`service/…`) | Responsabilidade | Integração externa |
|---|---|---|
| `user` / `auth` (`config/auth`) | Cadastro e gestão de usuários (papéis: Manager, Employee); login com JWT + refresh token rotativo, lockout progressivo por conta/IP e auditoria de tentativas | — |
| `purchase` | Clientes, compras, agrupamento de vendas (`CombinedScore`, inclusive agrupamento avulso "somente boleto"), produtos por nota, captura de notas por foto (upload direto ou via dispositivo pareado) com fila de revisão e reprocessamento de capturas com erro | Gemini (extração) |
| `finance` | Importação de extratos bancários (PDF, BB e Sicoob), transações, categorização, exportação (Excel/ZIP) e consulta de saldo/extrato em tempo real (`BBSaldoService`, `finance/bb`, `finance/sicoob`) | Apache PDFBox · Apache POI · Banco do Brasil (mTLS) · Sicoob (mTLS) |
| `billet` | Geração, consulta, listagem de boletos em aberto, baixa manual (`mark-paid`), 2ª via, cancelamento por agrupamento ou por "nosso número" | Sicoob (mTLS) |
| `invoice` | Emissão, consulta, cancelamento e armazenamento de notas fiscais eletrônicas (XML/DANFE), emissão combinada de NF-e + boleto (`IssueInvoiceWithBilletService`), reconciliação de emissões com falha parcial (evita duplicidade) e relatórios fiscais (ICMS, vendas, pagamentos) | Focus NFe |
| `product` (`service/product`) | Catálogo de produtos fiscais (`FiscalProduct`) usado na composição de notas fiscais | — |
| `storage` | Geração de chaves de objeto e upload/download/move de arquivos (boletos, XMLs, extratos, fotos de notas) | Cloudflare R2 (S3-compatible) |
| `freight` | Cálculo de distância e frete entre endereços | Google Maps Distance Matrix |
| `climate` | Previsão do tempo e recomendações de compra de produtos sazonais | OpenWeather |
| `notification` | Envio de e-mails (provedor plugável), mensagens de WhatsApp e notificações em massa para clientes e contabilidade | SendGrid · Gmail (SMTP/API) · Ultramsg |
| `chatbot` | Atendente automatizado via WhatsApp, com limpeza periódica de sessões inativas (`ChatSessionCleanupService`) | Ultramsg |
| `realtime` (`service/realtime`, `config/realtime`) | Notificação em tempo real via WebSocket para a fila de capturas de notas pendentes, autenticada por ticket de curta duração | — |
| `device` (pareamento — `config/auth`) | Pareamento de dispositivo móvel via código de 6 dígitos, permitindo que o celular envie fotos de notas sem sessão de usuário completa | — |
| `backup` | Autenticação OAuth2, geração de CSV e upload periódico (ou por período) de backups do banco | Google Drive |
| `scheduler` | Monitoramento de armazenamento do banco (`DatabaseStorageService`), acionado sob demanda via `/api/notifications/test/database-storage-alert` | — |

---

## 📁 Estrutura de pastas

> 📄 **Cada pasta de código-fonte tem seu próprio `README.md`** com a lista de arquivos daquela pasta, o tipo de cada um (`@Service`, `@Entity`, `record`…) e sua responsabilidade específica — inclusive rotas expostas (controllers), entidades/relacionamentos (model) e quando cada exceção é lançada (exception). Para entender um pacote específico, leia o README dele em vez de abrir arquivo por arquivo; este README raiz cobre só a visão geral.

```
Back/
├── src/
│   └── main/
│       ├── java/com/hortifruti/sl/hortifruti/
│       │   ├── HortifrutiSlApplication.java
│       │   ├── controller/          # Controllers REST — cada subpasta = 1 domínio (README.md em cada uma)
│       │   │   ├── backup/ · billet/ · climate/ · dashboard/ · device/ · finance/
│       │   │   ├── freight/ · invoice/ · notification/ · product/ · purchase/
│       │   │   ├── realtime/ · user/
│       │   ├── service/             # Services de domínio e integração
│       │   │   ├── backup/ (auth/ · folders/ · oauth/) · billet/ · chatbot/ · climate/
│       │   │   ├── dashboard/ · finance/ (bb/ · sicoob/ · transaction/) · freight/
│       │   │   ├── invoice/ (factory/ · tax/…) · notification/ (email/ · whatsapp/)
│       │   │   ├── product/ · purchase/ · realtime/ · scheduler/ · storage/ · user/
│       │   ├── repository/          # Repositórios Spring Data JPA
│       │   ├── model/               # Entidades JPA + enums (Role, Status, FileStatus…)
│       │   │   └── product/         # FiscalProduct
│       │   ├── dto/                 # DTOs de request/response, organizados por domínio (device/ · realtime/ · product/…)
│       │   ├── mapper/              # Mappers MapStruct (entidade ⇄ DTO)
│       │   ├── config/              # Beans gerais, segurança JWT, rate limiting, clientes HTTP, Swagger
│       │   │   ├── auth/ · bb/ · billet/ · climate/ · email/ · freight/ · gemini/
│       │   │   ├── realtime/ · sicoob/ · ssl/ · storage/
│       │   ├── exception/           # Exceções de domínio + tratamento global (GlobalExceptionHandler)
│       │   ├── util/                # Utilitários (Base64, datas, arquivos)
│       │   └── tools/               # Ferramentas auxiliares de linha de comando/scripts internos
│       └── resources/
│           ├── application.properties (+ application-{local,hml,prod}.properties)
│           ├── db/migration/        # Scripts versionados do Flyway (V1…V15)
│           ├── products.yml         # Catálogo de produtos para recomendação climática
│           ├── static/              # Scripts SQL auxiliares e imagens
│           └── templates/email/     # Templates HTML dos e-mails enviados a clientes/contabilidade
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
| `POST` | `/auth` | Login — valida credenciais (com proteção de lockout progressivo) e emite `auth_token` + `refresh_token` em cookies `httpOnly` |
| `GET` | `/auth/me` | Retorna o usuário autenticado (lido do cookie/sessão) |
| `POST` | `/auth/refresh` | Rotaciona o refresh token (revoga o atual e emite um novo) e emite novo access token |
| `POST` | `/auth/logout` | Revoga access token e refresh token e limpa os cookies |
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
| `POST` | `/purchases/manual` | Criar compra manualmente, sem upload de nota |
| `GET` | `/purchases/client/{clientId}/ordered` | Compras paginadas de um cliente |
| `GET` | `/purchases/{id}/products` · `/purchases/date-range` | Produtos da compra e busca por período |
| `GET` | `/purchases/{id}/imagem` | Imagem da nota associada à compra |
| `POST` | `/purchases/{id}/products` | Adicionar produtos a uma compra existente |
| `DELETE` | `/purchases/{id}` | Remover compra |
| `POST` | `/combined-scores/create` | Criar agrupamento de vendas (score combinado) |
| `POST` | `/combined-scores/create-wildcard-billet` | Criar agrupamento avulso com produto coringa (R$1/kg), para clientes "somente boleto" |
| `GET` | `/combined-scores` · `/combined-scores/last-per-client` | Listar agrupamentos paginados e o último agrupamento de cada cliente |
| `GET` | `/combined-scores/{id}/grouped-products` | Listar produtos agrupados do agrupamento |
| `GET` | `/combined-scores/{id}/imagens` · `/combined-scores/{id}/fotos/pdf` | Imagens das notas do agrupamento e PDF consolidado das fotos |
| `PATCH` | `/combined-scores/confirm-payment/{id}` · `/cancel-payment/{id}` | Confirmar ou cancelar pagamento do agrupamento |
| `DELETE` | `/combined-scores/{id}` | Cancelar agrupamento |
| `PUT` / `DELETE` | `/invoice-products/{id}` | Editar ou remover produto de uma nota |

### Captura de notas por foto (`/api/compras/notas`)

Fluxo de extração automática de notas de compra a partir de uma foto (via Gemini), com fila de revisão para capturas que falharam na extração.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/compras/notas/upload` | Upload simples da foto da nota |
| `POST` | `/api/compras/notas/extrair` | Extração síncrona dos dados da nota via Gemini |
| `POST` | `/api/compras/notas/capturas` | Recebe foto de um dispositivo pareado (`DEVICE_CAPTURE`) ou de um usuário autenticado; responde `202` e processa a extração em segundo plano |
| `GET` | `/api/compras/notas/pendentes` | Listar a fila de capturas pendentes do usuário (tela do PC) |
| `GET` | `/api/compras/notas/pendentes/{id}/imagem` | Baixar a foto original da captura pendente, para comparação lado a lado na revisão |
| `POST` | `/api/compras/notas/pendentes/{id}/reprocessar` | Reprocessar uma captura com erro de extração, reaproveitando a imagem original (sem exigir nova foto); resultado chega por notificação em tempo real |
| `POST` | `/api/compras/notas/pendentes/{id}/confirmar` | Confirmar os dados revisados da captura como uma compra real |
| `POST` | `/api/compras/notas/pendentes/{id}/descartar` | Descartar uma captura pendente |

### Dispositivos pareados (`/api/dispositivos`)

Permite que um celular capture fotos de notas diretamente para a fila de revisão, sem uma sessão de usuário completa — autenticado por um `device_token` próprio, em cookie `httpOnly`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/dispositivos/pareamento/iniciar` | Gerar código de pareamento de 6 dígitos (usuário autenticado no PC) |
| `POST` | `/api/dispositivos/pareamento/confirmar` | Público — celular confirma o código e recebe o `device_token` em cookie `httpOnly` |
| `GET` | `/api/dispositivos/pareamento/status` | Público — verificar se o celular já possui um `device_token` válido |
| `POST` | `/api/dispositivos/pareamento/desvincular` | Público — limpar o `device_token` local do celular |
| `GET` | `/api/dispositivos/pareamento` | Listar dispositivos vinculados ao usuário |
| `DELETE` | `/api/dispositivos/pareamento/{id}` | Revogar um dispositivo vinculado |

### Tempo real e catálogo fiscal

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/realtime/ws-ticket` | Emitir ticket de curta duração para autenticar a conexão WebSocket da fila de capturas pendentes |
| `GET` | `/fiscal-products` | Listar o catálogo de produtos fiscais usado na composição de notas fiscais |

### Boletos — Sicoob (`/billet`)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/billet/generate/{combinedScoreId}` | Gerar boleto em PDF (com vencimento opcional) |
| `GET` | `/billet/client/{clientId}` | Listar boletos de um cliente (pagador), com filtros opcionais |
| `GET` | `/billet/open` | Listar todos os boletos em aberto de todos os clientes |
| `GET` | `/billet/issue-copy/{idCombinedScore}` | Emitir 2ª via |
| `GET` | `/billet/{combinedScoreId}/file` | Baixar o PDF armazenado no R2 (sem emitir nova via no Sicoob) |
| `POST` | `/billet/cancel/{idCombinedScore}` | Cancelar (baixar) boleto pelo agrupamento |
| `POST` | `/billet/cancel-by-number` | Cancelar (baixar) boleto avulso pelo "nosso número" |
| `PATCH` | `/billet/mark-paid/{combinedScoreId}` | Marcar boleto como pago manualmente (pagamento recebido fora do Sicoob) |
| `GET` | `/billet/{combinedScoreId}` | Consultar boleto pelo agrupamento |

### Notas fiscais — Focus NFe (`/invoices`)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/invoices/issue/{combinedScoreId}` | Emitir NF-e |
| `POST` | `/invoices/issue-with-billet/{combinedScoreId}` | Emitir NF-e e o boleto vinculado em um único fluxo (com rollback automático da NF-e em caso de falha) |
| `POST` | `/invoices/{combinedScoreId}/reconciliar` | Consultar o status real da NF-e na Focus NFe e liberar o agrupamento para nova emissão quando ela ficou em erro/denegada/cancelada, evitando duplicidade |
| `GET` | `/invoices/open` | Listar agrupamentos com NF-e emitida mas sem boleto vinculado, pendentes de confirmação manual |
| `GET` | `/invoices/consulta/{ref}` | Consultar status da nota |
| `GET` | `/invoices/{ref}/danfe` · `/invoices/{ref}/xml/download` | Baixar DANFE e XML |
| `DELETE` | `/invoices/{ref}/cancel` | Cancelar nota (`justificativa` obrigatória; `extemporaneo` opcional para cancelamento fora do prazo) |
| `GET` | `/invoices/xml-storage` · `/invoices/xml-storage/{ref}/download` | Consultar e baixar XMLs armazenados por período *(Gestor)* |
| `GET` | `/api/tax-reports/...` | Relatórios fiscais (ICMS, vendas, pagamentos, cadastro) |

### Financeiro (`/statements`, `/transactions`, `/finance/bank-balance`) *(Gestor)*

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/finance/bank-balance` | Saldo disponível em conta corrente, via API do Banco do Brasil |
| `GET` | `/statements` | Listar todos os extratos importados |
| `POST` | `/statements/sicoob-api/import` | Importar extrato do Sicoob (mês/ano, dia inicial/final opcionais) |
| `GET` | `/statements/sicoob-api/export/pdf` · `/export/excel` | Baixar o extrato do Sicoob no layout original |
| `POST` | `/statements/bb-api/import` | Importar extrato do Banco do Brasil (`dataInicio`/`dataFim`, máx. 31 dias) |
| `GET` | `/statements/bb-api/export/pdf` · `/export/excel` | Baixar o extrato do BB no layout original |
| `GET` | `/transactions` | Transações filtradas e paginadas (busca/tipo/categoria) |
| `GET` | `/transactions/revenue` · `/expenses` · `/balance` | Receita, despesas e saldo por período |
| `GET` | `/transactions/categories` | Listar categorias de transação (sem restrição de papel) |
| `PUT` / `DELETE` | `/transactions/{id}` | Editar ou remover transação |
| `POST` | `/transactions/export` · `/export-complete` | Exportar transações como Excel ou ZIP com relatórios |
| `GET` | `/transactions/report/pdf` · `/report/excel` | Relatório consolidado (BB + Sicoob) por período, com padrão de mês anterior quando datas não informadas |

### Dashboard, produtos e clima

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/dashboard` | Visão consolidada do negócio (período, mês, ano) *(Gestor)* |
| `GET` | `/dashboard/icms-report/monthly/{start}/{end}` | Relatório mensal de ICMS *(Gestor)* |
| `GET` / `POST` / `PUT` / `DELETE` | `/products`, `/products/{id}`, `/products/search`, `/products/paginated`, `/products/count` | CRUD, busca e paginação de produtos *(Gestor)* |
| `GET` | `/api/weather/forecast/5days` | Previsão do tempo de 5 dias (Santa Luzia/MG) |
| `GET` | `/api/recommendations/by-temperature/{category}` · `/by-date` | Recomendações de compra por clima |

### Notificações, frete e backup

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/notifications/client/documents` | Enviar documentos ao cliente (e-mail/WhatsApp/ambos) |
| `POST` | `/api/notifications/accounting/generic-files` | Enviar arquivos à contabilidade |
| `POST` | `/api/notifications/overdue/check` · `/send-bulk` | Verificar vencimentos e enviar notificações em massa |
| `POST` | `/api/notifications/test/database-storage-alert` | Disparar e-mail de teste com o tamanho atual do banco |
| `POST` | `/distance` · `GET /distance/freight-config` · `PATCH /distance/freight-config` | Calcular frete e consultar/atualizar configuração *(Gestor)* |
| `POST` | `/backup` | Disparar backup para o Google Drive (`startDate`/`endDate` opcionais) *(Gestor)* |
| `GET` | `/backup/storage` | Consultar tamanho atual do banco e limite máximo *(Gestor)* |
| `GET` | `/backup/oauth2callback` | Callback OAuth2 do Google Drive |

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

# Proteção contra brute-force no login: e-mails avisados quando um lockout é
# ativado (não a cada tentativa falha, há throttle de 30min).
SECURITY_ALERT_EMAILS=email1@empresa.com,email2@empresa.com

# ── Google ─────────────────────────────────────
# Não há GOOGLE_REDIRECT_URI: a URI de callback é derivada de
# LOCAL_BACKEND_URL/HML_BACKEND_URL/PROD_BACKEND_URL + /backup/oauth2callback (fixo).
CREDENTIALS_GOOGLE=sua_api_key_google_maps
GOOGLE_DRIVE_CREDENTIALS=suas_credenciais_google_drive_em_base64
# GMAIL / GMAIL_PASSWORD: reaproveitadas pelo provedor de e-mail "gmail" (SMTP) —
# ver bloco Notificações abaixo.
GMAIL=seu_email@gmail.com
GMAIL_PASSWORD=app_password_gerada_em_myaccount.google.com/apppasswords

# ── Sicoob (boletos e extrato de conta corrente) ─
SICOOB_CLIENT_ID=seu_client_id_sicoob
SICOOB_API_URL=https://api.sicoob.com.br
SICOOB_AUTH_URL=https://auth.sicoob.com.br
SICOOB_SCOPE=cobranca_boletos
SICOOB_NUM_CLIENTE=numero_cliente_sicoob
SICOOB_NUM_CONTA_CORRENTE=numero_conta_corrente

# ── Certificado .pfx e .pem (mTLS — compartilhado entre Sicoob e Banco do Brasil) ─
DOCUMENT_PFX=certificado_digital_em_base64
PASSWORD_PFX=senha_do_certificado_pfx
DOCUMENT_PEM=chave_pem_em_base64

# ── Banco do Brasil (consulta de saldo/extrato) ─
BB_AMBIENT=hml_ou_prod
BB_APP_KEY=app_key_bb
BB_BASIC=credencial_basic_auth_bb
BB_AGENCIA=numero_agencia
BB_CONTA=numero_conta
BB_SCOPE=extrato-info
# BB_MCITESTE: opcional, só usado quando BB_AMBIENT=hml (ambiente de testes do BB).

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

# Provedor de e-mail ativo — trocar aqui não exige mudança de código, os três
# provedores já estão implementados (EmailSender):
#   sendgrid  = API do SendGrid (padrão)
#   gmail     = SMTP direto (bloqueado em hosts que fecham a porta 587/465, ex: Railway)
#   gmail-api = Gmail via API HTTPS, reaproveita a autorização OAuth do backup (GOOGLE_DRIVE_CREDENTIALS)
EMAIL_PROVIDER=sendgrid
SENDGRID_API_KEY=sua_api_key_sendgrid
SENDGRID_FROM_EMAIL=noreply@seudominio.com
# Config opcional do Gmail SMTP (defaults já cobrem o Gmail padrão)
GMAIL_SMTP_HOST=smtp.gmail.com
GMAIL_SMTP_PORT=587
NOTIFICATION_SENDER_NAME=Nome exibido na assinatura dos e-mails
ACCOUNTING_EMAIL=contabilidade@empresa.com,contabilidade2@empresa.com
ACCOUNTING_WHATSAPP=5531999999999
OVERDUE_NOTIFICATION_EMAILS=email1@empresa.com,email2@empresa.com

# ── Cloudflare R2 (storage de boletos, XMLs e extratos) ─
# Um bucket por ambiente — local reaproveita o bucket de HML (mesmo padrão da Focus NFe).
R2_ACCOUNT_ID=id_da_conta_cloudflare
R2_ACCESS_KEY_ID=access_key_id_r2
R2_SECRET_ACCESS_KEY=secret_access_key_r2
R2_ENDPOINT=https://<account_id>.r2.cloudflarestorage.com
HML_R2_BUCKET_NAME=nome_do_bucket_hml
PROD_R2_BUCKET_NAME=nome_do_bucket_prod

# ── Gemini (extração de notas de compra via foto) ─
# Mesma chave free tier em todos os ambientes — não varia por profile.
GEMINI_API_KEY=sua_api_key_gemini
# gemini-2.5-flash (default original) não é mais oferecido pra chaves novas — use o alias
# estável abaixo, que a Google mantém apontando pro Flash mais recente disponível.
GEMINI_MODEL=gemini-flash-latest
# Latência real do free tier pra extração com response_schema variou entre ~17s e ~30s nos
# testes (mesma imagem, chamadas consecutivas) — 15000 (default original da spec) estoura
# quase sempre. 60000 dá folga de verdade.
GEMINI_TIMEOUT_MS=60000
# Retry com backoff exponencial em erro 503 da API do Gemini.
GEMINI_RETRY_MAX_TENTATIVAS=3
GEMINI_RETRY_ESPERA_INICIAL_MS=2000

# ── Pareamento de dispositivo (captura de notas via celular) ─
# TTL do código de 6 dígitos usado para vincular o celular; dias de inatividade
# até o device_token do dispositivo ser revogado automaticamente.
DISPOSITIVO_PAREAMENTO_TTL_MINUTOS=5
DISPOSITIVO_INATIVIDADE_MAX_DIAS=90
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
- ⚠️ **Certificados Sicoob/BB (`DOCUMENT_PFX` e `DOCUMENT_PEM`)** devem estar em Base64 — usados para autenticação mútua (mTLS) na emissão de boletos e na consulta de saldo/extrato do Banco do Brasil e do Sicoob
- ⚠️ **Swagger UI** fica desabilitado em produção (`springdoc.swagger-ui.enabled=false`) — disponível apenas em `local`/`hml`
- 📁 Diretórios temporários (`temp/cert`, `temp/cert/bb_upload`, `temp/google`, `temp/notifications`) são criados automaticamente
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
| Diretórios voláteis | `/app/temp/{cert,cert/bb_upload,google,notifications}` |
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
| `spring.jpa.hibernate.ddl-auto` definido por perfil (`application-{local,hml,prod}.properties`) | Cada ambiente controla sua própria estratégia de schema — hoje `update` em todos os perfis (convivendo, por ora, com as migrations do Flyway) |
| Migrations do Flyway (`resources/db/migration`, `baseline-on-migrate=true`) | Evolução do schema versionada e auditável; requer `spring-boot-starter-flyway` (o `flyway-core` puro não integra sozinho com `spring.flyway.*`) |
| `device_token` de dispositivo pareado nunca trafega no corpo da resposta, só em cookie `httpOnly` | Mesma proteção contra XSS aplicada aos tokens de usuário; a authority `DEVICE_CAPTURE` não é um valor do enum `Role` — é resolvida por essa autenticação separada |
| `open-in-view=false` | Previne sessões Hibernate abertas durante a renderização da resposta (N+1 fora da camada de serviço) |
| Controllers nunca acessam `repository` diretamente | Toda regra de negócio passa pela camada `service` |
| Endpoints sensíveis exigem `@PreAuthorize("hasRole('MANAGER')")` | Controle de acesso por papel centralizado na camada de segurança |
| `SecurityFilter`/`JwtAuthenticationFilter` nunca são *bypassados* | Endpoints públicos são declarados explicitamente na configuração de segurança |
| Access e refresh token trafegam em cookies `httpOnly` | Reduz exposição a XSS em relação a manter o token em `localStorage` |
| Reuso de refresh token já revogado derruba todas as sessões do usuário | Detecção de possível vazamento/roubo de token (`RefreshTokenService`) |
| Lockout progressivo por conta e por IP no login, com mensagem de erro genérica | Mitiga brute-force e enumeration attack; toda tentativa é auditada em `LoginAuditLog` |
| Integração com Sicoob e Banco do Brasil via certificados `.pfx`/`.pem` compartilhados (mTLS) | Exigência das próprias instituições financeiras para boletos, saldo e extratos |
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
| Migrations de schema | Flyway (spring-boot-starter-flyway + flyway-mysql) | — |
| Extração de notas por foto | Google Gemini (REST, `gemini-flash-latest`) | — |
| Rate limiting | Bucket4J (core + jcache) | 8.0.1 |
| PDF | Apache PDFBox | 3.0.7 |
| Planilhas | Apache POI OOXML | 5.5.1 |
| Compressão | Zip4j | 2.11.5 |
| HTTP Client | Apache HttpComponents Client5 | 5.6.1 |
| CSV | Apache Commons CSV | 1.14.1 |
| Google APIs (Drive/OAuth/Maps/Gmail) | google-api-client / google-oauth-client-jetty / google-api-services-drive / google-api-services-gmail | 2.8.1 / 1.39.0 / v3-rev20220815-2.0.0 / v1-rev20220404-2.0.0 |
| Storage (S3-compatible) | AWS SDK for Java (S3) — usado com Cloudflare R2 | 2.29.52 |
| E-mail | SendGrid Java · Spring Boot Starter Mail (Gmail SMTP) · Gmail API | 4.10.0 / — / — |
| WhatsApp | Ultramsg (REST) | — |
| NF-e | Focus NFe (REST) | — |
| Boletos | Sicoob (REST + mTLS) | — |
| Extratos bancários | Banco do Brasil — API Extratos e Sicoob — API Conta Corrente (REST + mTLS) | — |
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
