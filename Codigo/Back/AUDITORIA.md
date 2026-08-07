# Auditoria de Qualidade e Segurança — Backend Hortifruti SL

**Data:** 2026-08-06
**Escopo:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/**` (381 arquivos Java), `application*.properties`, `products.yml`, `pom.xml`.
**Metodologia:** leitura completa (não apenas grep) de todo o código-fonte, dividida em 5 frentes paralelas por área do sistema. Nenhuma correção foi aplicada — este documento é só diagnóstico, para orientar um trabalho de refatoração subsequente.
**Não avaliado:** frontend, testes automatizados (o projeto não tem pasta `src/test` com cobertura relevante — ver seção 7), infraestrutura de deploy além do que está em `Dockerfile`/`pom.xml`.

> **Convenção:** cada achado tem uma checkbox `- [ ]` (para marcar como resolvido), severidade, localização exata (`arquivo:linha`) e uma explicação do impacto real — não é opinião de estilo, é "isso pode doer assim".

---

## Sumário

1. [Como usar este documento](#1-como-usar-este-documento)
2. [Resumo executivo](#2-resumo-executivo)
3. [Plano de ataque recomendado](#3-plano-de-ataque-recomendado)
4. [Área A — Autenticação, Segurança e mTLS](#área-a--autenticação-segurança-e-mtls)
5. [Área B — Invoice (NF-e) / Billet (Boleto Sicoob) / Finance (Extratos BB/Sicoob)](#área-b--invoice-nf-e--billet-boleto-sicoob--finance-extratos-bbsicoob)
6. [Área C — Demais Services (purchase, notification, backup, climate, freight, storage, dashboard, scheduler...)](#área-c--demais-services)
7. [Área D — Controllers, DTOs e Mappers](#área-d--controllers-dtos-e-mappers)
8. [Área E — Models, Repositories e Configuração/Build](#área-e--models-repositories-e-configuraçãobuild)
9. [Achados duplicados entre áreas (reforço de sinal)](#9-achados-duplicados-entre-áreas)
10. [O que já está bom (não mexer)](#10-o-que-já-está-bom-não-mexer)

---

## 1. Como usar este documento

- Cada achado é uma **checkbox independente** — marque `[x]` conforme for corrigindo, e este arquivo vira o tracker da refatoração.
- Severidade: 🔴 **Crítico** (bug real / risco de perda de dado ou segurança ativa) · 🟠 **Alto** (defeito funcional/de design com impacto real) · 🟡 **Médio** (dívida técnica que vai doer em manutenção) · 🔵 **Baixo** (estilo/limpeza, baixo risco).
- Dentro de cada área, os achados estão nas 5 categorias que vocês pediram: **Vulnerabilidades**, **Acoplamento excessivo**, **Baixa coesão**, **Clareza/código confuso**, **Comentários desnecessários** — mais uma seção "Outros" quando o achado é um bug funcional puro (não se encaixa nas 5 categorias, mas é grave demais pra omitir).
- Comece pela seção 3 (plano de ataque) — ela já ordena os itens mais importantes por impacto real, não por ordem de leitura do código.

---

## 2. Resumo executivo

O código está organizado em uma arquitetura em camadas coerente (controller → service → repository) e, na maior parte do domínio fiscal/financeiro, usa `BigDecimal` corretamente e tem comentários que explicam o *porquê* das decisões — acima da média para um projeto deste tamanho. O problema não é falta de estrutura; é que **381 arquivos e ~15 integrações externas cresceram sem um segundo revisor consistente**, e isso deixou rachaduras específicas e localizadas, não uma bagunça generalizada.

**Contagem total de achados: ~140**, sendo:

| Severidade | Qtde. total | Onde estão os mais graves |
|---|---|---|
| 🔴 Crítico | **8** | Segurança (bypass de rate limit), Fiscal (ICMS zerado), Backup (limpeza de token é no-op), Controllers (rota sem proteção, controller acessando repository), Modelo de dados (cascade delete, dinheiro em `double`, schema sem migração versionada) |
| 🟠 Alto | **24** | Validação de entrada ausente em dados financeiros, N+1 em entidades JPA, acoplamento cruzado entre domínios, perda silenciosa de dados em exports, ausência de idempotência na emissão fiscal |
| 🟡 Médio | **45** | Duplicação de lógica entre integrações parecidas, god classes, tratamento de erro genérico, documentação desatualizada |
| 🔵 Baixo | **43** | Nomenclatura inconsistente, código morto isolado, metadados de build |

### Os 8 achados críticos, em uma frase cada

1. **Segurança:** o IP do cliente é lido de um header (`X-Forwarded-For`) que o próprio atacante controla — rate limiting e bloqueio de brute-force por IP são hoje contornáveis trivialmente. ([Área A, item A-V1](#a-v1))
2. **Fiscal:** o relatório de apuração de ICMS calcula os totais e os descarta por um bug clássico de pass-by-value em Java — os valores retornados são sempre zero. ([Área B, item B-C1](#b-c1))
3. **Backup:** a rotina que deveria limpar tokens OAuth corrompidos do Google percorre os arquivos mas nunca chama `.delete()` — é um no-op disfarçado de correção de erro. ([Área C, item C-O1](#c-o1))
4. **Controller:** `DELETE /clients/{id}` — documentado no próprio README como restrito a Gestor — está acessível a qualquer usuário autenticado, sem `@PreAuthorize` nenhum. ([Área D, item D-V1](#d-v1))
5. **Controller:** `AuthController` é o único controller do projeto que acessa `UserRepository` diretamente, violando a regra de arquitetura documentada. ([Área D, item D-V2](#d-v2))
6. **Modelo de dados:** apagar um cliente apaga em cascata (`CascadeType.ALL`) todo o histórico de compras dele — dado financeiro que deveria sobreviver ao cliente. ([Área E, item E-M1](#e-m1))
7. **Modelo de dados:** `FreightConfig` — a entidade que decide o custo/preço de frete — usa `double` em vez de `BigDecimal` para todos os seus 12 campos monetários. ([Área E, item E-M2](#e-m2))
8. **Config/Build:** `ddl-auto=update` está ativo em produção, sem Flyway/Liquibase — o schema do banco é alterado automaticamente a cada deploy, sem histórico de migração nem rollback. ([Área E, item E-C1](#e-c1))

Nenhuma das 5 frentes de análise encontrou blocos relevantes de **código morto comentado** ou `TODO`/`FIXME` esquecidos — ao contrário do que a preocupação inicial sugeria, "comentários desnecessários" é a categoria com **menos** achados no projeto inteiro (a maioria dos comentários existentes explica *por quê*, não *o quê*). O problema real de qualidade está concentrado em **acoplamento entre domínios** (services de um módulo mexendo direto no repository de outro) e em **bugs silenciosos** que não geram exceção — só dado errado.

---

## 3. Plano de ataque recomendado

Ordem sugerida, misturando "baixo custo/alto impacto" primeiro com os itens que bloqueiam outros:

### Onda 1 — Correções pontuais, baixo risco, alto impacto (1 a 3 dias)
- [x] **A-V1** — Corrigir `resolveClientIp` para não confiar cegamente no primeiro valor de `X-Forwarded-For`.
- [x] **D-V1** — Adicionar `@PreAuthorize("hasRole('MANAGER')")` no `DELETE /clients/{id}` (e reavaliar os demais endpoints de `ClientController`).
- [x] **D-V2** — Mover a busca de usuário do `AuthController` para dentro de `RefreshTokenService`/`Auth`.
- [x] **C-O1** — Corrigir `TokenCleaner.deleteDirectoryRecursively` para de fato chamar `.delete()`.
- [x] **C-O2** — Corrigir separador de path Windows (`\\`) em `BackupService.performBackupForPeriod` (usar `Paths.get(...).getFileName()`).
- [x] **B-C1** — Corrigir o pass-by-value em `ImcsReportCalculator.updateTotals` (retornar os totais ou usar `AtomicReference`/objeto acumulador mutável).
- [x] **B-C4** — Corrigir a paginação fixa de 10 registros em `TransactionExportService.exportTransactionsAsZip` que descarta extratos do relatório mensal.
- [x] **E-M1** — Trocar `CascadeType.ALL` de `Client.purchases` por algo sem `REMOVE` (ex.: `PERSIST, MERGE`), ou implementar soft-delete de cliente.

### Onda 2 — Estrutural, mas isolado por módulo (1 a 2 semanas)
- [x] **E-M2** — Migrar `FreightConfig` de `double` para `BigDecimal`.
- [ ] **E-C1** — Introduzir Flyway (ou Liquibase), congelar `ddl-auto=validate` em produção, versionar os `.sql` avulsos que já existem em `static/`. *(Não aplicado nesta rodada — exige acesso ao schema real de hml/prod para gerar uma baseline confiável; ver observação abaixo.)*
- [x] **D-A1**/**D-A2** — Adicionar Bean Validation (`@Valid`, `@NotNull`, `@Positive`) em `ManualPurchaseRequest`, `UpdateInvoiceProduct`, `UserUpdateRequest`.
- [x] **C-A1**/**C-A2** — Tirar `service/backup` (`CsvGeneratorService`, `EntityCleanupService`) e `DashboardService` do acesso direto a repositories de outros domínios; passar a usar os services donos.
- [x] **D-C1** — Consolidar o tratamento de exceção do `BilletController` (e os outros 5 controllers com try/catch manual) para usar o `GlobalExceptionHandler` já existente. *(`InvoiceController.cancelInvoice` ficou de fora deliberadamente — o front lê essa resposta como texto puro, não JSON; consolidar mudaria a mensagem exibida ao usuário. Ver nota no commit/PR.)*
- [x] **B-A2** — Unificar os 2 clientes HTTP (RestTemplate/WebClient) da Focus NFe em um só, com timeout único.
- [ ] **E-J1** — Declarar `fetch = FetchType.LAZY` nos 4 `@ManyToOne` de negócio (`Transaction.statement`, `Purchase.client`, `InvoiceProduct.purchase`, `GroupedProduct.combinedScore`).

### Onda 3 — Decisões de produto/negócio antes de mexer no código
- [ ] **B-C2** — Decidir se o relatório "Resumo de Vendas por Forma de Pagamento" deve de fato discriminar por forma de pagamento (hoje não discrimina — tudo cai como "Liquidação Bancária"/"DINHEIRO" fixo) ou deve ser removido/renomeado.
- [ ] **B-C3** — Avaliar idempotência na emissão de NF-e (`IssueInvoice.issueInvoice` usa UUID aleatório em vez de derivar de `combinedScoreId`).
- [ ] **E-C2** — Decidir fonte única de verdade para o catálogo fiscal (`products.yml` vs. tabela `fiscal_products` — hoje o fluxo de emissão de NF-e ignora o banco).
- [ ] **D-A4** — Decidir se `BilletController`/`CombinedScoreController` (hoje sem `@PreAuthorize` nenhum) devem ser restritos a Gestor.

### Onda 4 — Débito técnico contínuo (backlog, sem urgência)
Tudo marcado 🟡/🔵 nas seções abaixo: duplicação entre provedores de e-mail, parsing de endereço duplicado (NF-e vs. boleto), DTOs/mappers mortos, READMEs desatualizados, nomenclatura inconsistente.

---

## Área A — Autenticação, Segurança e mTLS

**Escopo analisado:** `config/auth/**`, `config/bb/**`, `config/billet/**`, `config/ssl/**`, `config/storage/**`, inicializadores (`UserInitializer`, `FiscalProductInitializer`, `Base64FileDecoder`, `WebClientConfig`, `SwaggerConfig`), `exception/**`, `service/user/**`, `controller/user/**`, `application*.properties`, `.env.example`, `pom.xml`.

> Observação geral desta frente: o padrão de comentários deste módulo é notavelmente bom — explica *por que* uma decisão foi tomada, não *o que* o código já diz. Nenhum código morto comentado ou `TODO` foi encontrado.

### A · Vulnerabilidades

<a id="a-v1"></a>
- [x] 🔴 **[A-V1] Rate limiting e bloqueio de brute-force são inteiramente contornáveis via spoofing de `X-Forwarded-For`**
  **Local:** `util/HttpRequestUtils.java:16-22`; consumido por `config/auth/RateLimitingFilter.java:41` e (via `Auth.java:33`) `LoginProtectionService`.
  ```java
  public static String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
  ```
  O header `X-Forwarded-For` é enviado pelo cliente e não é validado contra proxies confiáveis. A implementação pega o **primeiro** valor da lista, mas proxies reversos (Railway, rewrite do Next.js) **acrescentam** o IP real ao final da cadeia, não escrevem no início. Um atacante que envie `X-Forwarded-For: 1.2.3.4` controla 100% o "IP do cliente" percebido pelo backend. Variando esse header a cada request:
  - `RateLimitingFilter` (10 req/min padrão, 5 req/min no pareamento de dispositivo) deixa de limitar qualquer coisa.
  - O lockout por IP do `LoginProtectionService` nunca é acionado (o bloqueio por **conta**, chaveado por username, continua funcionando).
  - O código de pareamento de dispositivo de 6 dígitos (1.000.000 de combinações, TTL 5 min) fica exposto a brute-force distribuído, pois depende só do rate limiter.
  **Correção sugerida:** usar `ForwardedHeaderFilter`/`forward-headers-strategy` do Spring Boot (que sabe quantos hops confiar) ou pegar o **último** valor da lista assumindo que só o proxy de borda acrescenta.

- [ ] 🟠 **[A-V2] Mapa de buckets do rate limiter cresce sem limite (TTL/eviction ausente) — DoS de memória**
  **Local:** `config/auth/RateLimitingFilter.java:35,45`
  ```java
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(endpoint));
  ```
  Cada combinação `clientIp:endpoint` cria uma entrada permanente, nunca removida. Combinado com A-V1 (IP forjável), um atacante cresce esse mapa indefinidamente e esgota a heap — sem nem precisar estar autenticado, já que o filtro roda antes da autenticação (`SecurityConfig.java:111`).

- [ ] 🟠 **[A-V3] Política de senha fraca: mínimo de 4 caracteres, teto artificial de 20**
  **Local:** `dto/user/UserRequest.java:11` e reimplementado manualmente em `service/user/UserService.java:39,67`
  ```java
  @Size(min = 4, max = 20, message = "A senha deve ter entre 4 e 20 caracteres")
  ```
  4 caracteres está muito abaixo de qualquer diretriz atual (NIST 800-63B recomenda mínimo 8, priorizando senhas longas). O teto de 20 impede passphrases fortes sem justificativa técnica (BCrypt suporta até 72 bytes). *(Ver também duplicação em [seção 9](#9-achados-duplicados-entre-áreas).)*

- [ ] 🟡 **[A-V4] Mensagens de exceção internas (paths, detalhes de SSL) expostas cruas ao cliente da API**
  **Local:** `exception/GlobalExceptionHandler.java` (`handleBBApiException`, `handleBilletException`, `handleStorageException`, `handleBackupException`, entre outros) devolvem `ex.getMessage()` direto; origem em `config/ssl/MtlsRestTemplateFactory.java:100-104`.
  Dependendo da exceção original, isso pode vazar caminhos de arquivo do servidor (`temp/cert/...`) ou detalhes de biblioteca TLS — informação útil para reconhecimento por um atacante.

- [ ] 🟡 **[A-V5] Senha de bootstrap do admin logada em texto plano**
  **Local:** `config/UserInitializer.java:346-353`
  ```java
  log.warn("Nenhum usuário encontrado. Conta administrativa inicial criada — usuário: 'admin',"
      + " senha temporária: '{}'. Faça login e troque a senha imediatamente.", password);
  ```
  Logs são frequentemente enviados a agregadores de terceiros e retidos por tempo indeterminado. Nada força a troca da senha após o primeiro login. Recomenda-se não persistir em log permanente e/ou forçar troca no primeiro acesso.

- [ ] 🟡 **[A-V6] Swagger/OpenAPI não é desabilitado no profile `hml`**
  **Local:** `application-hml.properties` (ausência de `springdoc.*.enabled=false`, presente só em `application-prod.properties:30-31`); `SecurityConfig.java:87-88` libera `/swagger-ui/**` em todos os profiles.
  Em homologação — que roda em infraestrutura pública (Railway) — o schema completo da API fica navegável sem autenticação, facilitando reconhecimento de superfície de ataque.

- [ ] 🔵 **[A-V7] Estado de segurança em memória, single-instance apenas**
  **Local:** `config/auth/TokenBlocklist.java:9-11`, `RateLimitingFilter.java:35`, `DeviceTokenAuthFilter.java:58`
  Os três usam `ConcurrentHashMap` local ao processo. Já documentado como limitação aceita, mas fica silenciosamente quebrado (logout não invalida em todas as instâncias, limites de rate viram por-instância) se o serviço for escalado horizontalmente no futuro.

- [ ] 🔵 **[A-V8] `bucket4j-core` usa coordenadas Maven descontinuadas** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `pom.xml` (`com.github.vladimir-bukhtoyarov:bucket4j-core`)

- [ ] 🔵 **[A-V9] `DataIntegrityViolationException` mapeado por substring de mensagem de driver MySQL**
  **Local:** `exception/GlobalExceptionHandler.java:143-166` (`errorMessage.contains("Duplicate entry")` + `errorMessage.contains("users")`)
  Dependência frágil de string específica do driver/idioma. Troca de driver ou versão do MySQL quebra silenciosamente esse tratamento. Prefira checar `ex.getCause()` por `SQLIntegrityConstraintViolationException` + nome da constraint.

### A · Acoplamento e baixa coesão

- [ ] 🟡 **[A-A1] Lógica de hashing de token duplicada entre `RefreshTokenService` e `DispositivoVinculadoService`**
  **Local:** `config/auth/RefreshTokenService.java:83-107` vs. `config/auth/DispositivoVinculadoService.java:87-89,180-188`
  Ambas implementam, de forma idêntica e independente: gerar 32 bytes via `SecureRandom` + Base64 URL-safe, e SHA-256 formatado em hex. Se o algoritmo precisar mudar (ex.: migrar para Argon2/PBKDF2), é preciso lembrar de atualizar dois lugares. Extrair para um `TokenHasher` compartilhado.

- [ ] 🔵 **[A-A2] `GlobalExceptionHandler` concentra ~25 handlers de domínios não relacionados**
  **Local:** `exception/GlobalExceptionHandler.java` (369 linhas)
  Padrão aceitável para `@RestControllerAdvice` central, mas a maioria dos handlers é boilerplate quase idêntico. Poderia usar uma interface `DomainException` carregando seu próprio `HttpStatus` + um handler genérico.

- [ ] 🔵 **[A-A3] Certificado mTLS compartilhado entre BB e Sicoob via propriedade de nome genérico**
  **Local:** `config/ssl/MtlsRestTemplateFactory.java:39-40` (`@Value("${password.pfx}")`)
  Intencional (mesmo certificado e-CNPJ), mas acopla duas integrações de negócio distintas a uma única propriedade sem namespace — rotação de certificado de uma impacta a outra silenciosamente.

### A · Clareza / código confuso

- [ ] 🔵 **[A-C1] Offset de fuso horário como string mágica (`"-03:00"`)**
  **Local:** `config/auth/TokenConfiguration.java:75` — usa `ZoneOffset.of("-03:00")` em vez de `ZoneId.of("America/Sao_Paulo")` (já usado em `application.properties`). Armadilha silenciosa se o horário de verão for reinstituído no Brasil.

- [ ] 🔵 **[A-C2] Nome de arquivo do certificado PFX duplicado como literal em 2 métodos**
  **Local:** `config/Base64FileDecoder.java:50,97` — mesmo literal `"HORTIFRUTISANTALUZIALTDA275409060001552025.pfx"` copiado. Extrair para constante.

- [ ] 🔵 **[A-C3] Validação de senha duplicada quase verbatim entre `updateUser` e `updateUserById`** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `service/user/UserService.java:37-43` vs. `:64-72`

- [ ] 🔵 **[A-C4] Variável local com nome enganoso (`user` para um DTO de contagem)**
  **Local:** `service/user/UserService.java:96-103` — `UsersCountResponse user = ...`. Cosmético.

### A · Comentários desnecessários

Nenhum achado. Busca por `TODO|FIXME|XXX` e por blocos de código comentado não retornou ocorrências no escopo.

---

## Área B — Invoice (NF-e) / Billet (Boleto Sicoob) / Finance (Extratos BB/Sicoob)

**Escopo analisado:** `service/invoice/**` (incl. `factory/`, `tax/`), `service/billet/**`, `service/finance/**` (incl. `bb/`, `sicoob/`, `transaction/`), `config/FocusNfeApiClient.java`, `config/bb/**`, `config/billet/**`, `model/billet|invoice|finance/**` (~65 arquivos, ~9.200 linhas).

> A maior parte do código lido usa `BigDecimal` corretamente para dinheiro e tem comentários explicando regras da Sefaz/decisões de concorrência — acima da média para este domínio. O risco real está em bugs de cálculo silenciosos e duplicação de parsing frágil.

### B · Clareza / código confuso (contém os achados mais graves desta área)

<a id="b-c1"></a>
- [x] 🔴 **[B-C1] Bug de pass-by-value: totais do relatório de apuração de ICMS nunca são acumulados — sempre zero**
  **Local:** `service/invoice/tax/icms/ImcsReportCalculator.java:36-45,82-94`
  ```java
  BigDecimal totalContabil = BigDecimal.ZERO;
  for (CombinedScore cs : combinedScores) {
    processCombinedScore(cs, valoresPorCfop, totalContabil, totalBaseCalculo, ...); // reatribuição LOCAL
  }
  ...
  private void updateTotals(InvoiceTaxDetails taxDetails, BigDecimal totalContabil, ...) {
    totalContabil = totalContabil.add(taxDetails.valorTotal()); // não propaga pro chamador
  }
  ```
  `BigDecimal` é imutável e os parâmetros são passados por valor — a reatribuição dentro de `updateTotals` nunca chega de volta em `generateIcmsSalesReport`. Os 5 totais retornados em `IcmsSalesReport` são **sempre `BigDecimal.ZERO`**. Mascarado hoje porque `IcmsPdfGenerator.java:117-123,136-138,148-150` já ignora esses campos e imprime literais `"0"` fixos no PDF — ou seja, o "Registro de Apuração de ICMS" mostra zero fixo nessas colunas independentemente dos dados reais. É um relatório fiscal sendo gerado com dado numérico estruturalmente quebrado.

<a id="b-c2"></a>
- [ ] 🟠 **[B-C2] Relatório "Resumo de Vendas por Forma de Pagamento" não distingue formas de pagamento**
  **Local:** `service/invoice/tax/payment/PaymentCalculator.java:23-32`
  ```java
  String bankSettlement = "Liquidação Bancária";
  .collect(Collectors.toMap(key -> bankSettlement, value -> value, BigDecimal::add));
  ```
  Toda venda vira a chave fixa `"Liquidação Bancária"` — o mapa nunca tem mais de 1 entrada. `PaymentPdfGenerator.java:159,169` ainda imprime esse total sob o rótulo fixo `"DINHEIRO"`. O relatório afirma que 100% das vendas foram em dinheiro, quase certamente falso.

<a id="b-c3"></a>
- [ ] 🟠 **[B-C3] Ausência de idempotência determinística na emissão de NF-e — retry pode duplicar a nota fiscal real**
  **Local:** `service/invoice/IssueInvoice.java:69` — `String ref = UUID.randomUUID().toString();`
  A referência enviada à Focus NFe (chave de idempotência da API externa) é aleatória a cada chamada, não derivada de `combinedScoreId`. Se a chamada HTTP for bem-sucedida mas uma etapa posterior falhar antes do commit local, um retry gera um **novo UUID** e emite uma **segunda NF-e real**. Recomenda-se derivar `ref` deterministicamente (hash estável) do `combinedScoreId`.

<a id="b-c4"></a>
- [x] 🟠 **[B-C4] Export mensal "Relatório Macro" perde silenciosamente extratos quando há mais de 10 transações no período**
  **Local:** `service/finance/transaction/TransactionExportService.java:41-48`
  ```java
  Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionDate").descending());
  ```
  Esse `10` é usado para descobrir quais `Statement`s referenciar no ZIP — qualquer mês com mais de 10 transações por banco perde extratos silenciosamente do `Relatorio-Macro-*.zip`, sem log de aviso. Um mês comercial normal facilmente ultrapassa 10 lançamentos — bug provavelmente já ativo em produção.

- [ ] 🟡 **[B-C5] Alíquota de ICMS por CFOP hardcoded, só 2 casos cobertos, fallback silencioso para zero**
  **Local:** `service/invoice/tax/registerReport/RegisterCalculator.java:70-76`
  ```java
  case "5102" -> BigDecimal.valueOf(18.00);
  case "5405" -> BigDecimal.ZERO;
  default -> BigDecimal.ZERO;
  ```
  Sem constante nomeada nem comentário sobre a base legal. Qualquer CFOP fora desses dois cai silenciosamente em alíquota zero, sem log de CFOP não mapeado.

- [ ] 🟡 **[B-C6] Espera síncrona longa (`Thread.sleep`) dentro de requisição HTTP, fora do padrão assíncrono já usado no mesmo domínio**
  **Local:** `service/invoice/IssueInvoiceWithBilletService.waitForInvoiceNumber:88-106` — até 12× `Thread.sleep(10_000)` (2 minutos), somado aos retries de `DanfeXmlService.downloadWithRetry:120-161` (~16s extras). Uma única requisição pode prender uma thread do servlet por minutos. O próprio módulo já resolve um problema parecido com `@Async` em `FiscalNoteXmlStorageService.triggerSaveAfterIssuance:95` — o padrão certo existe no código, só não foi reaplicado aqui.

- [ ] 🔵 **[B-C7] Tratamento de erro genérico (`catch Exception`) em orquestradores de alto nível**
  **Local:** `service/invoice/tax/ReportTaxService.generateMonthly:34-61`, `service/finance/MacroExportService.exportMacroReports:28-68`

### B · Baixa coesão / duplicação de lógica

- [ ] 🟠 **[B-B1] Parsing de endereço em texto livre implementado de duas formas diferentes e frágeis, para o mesmo campo**
  **Local:** `service/invoice/factory/Recipient.parseAddress:50-135` (regex + split por vírgula) vs. `service/billet/BilletFactory.createPagadorFromClient:65-159` (split posicional)
  ```java
  // BilletFactory.java:80-84
  if (addressParts.length == 6) { complemento = addressParts[2].trim(); }
  String bairro = addressParts[addressParts.length - 3].trim();
  ```
  Um mesmo cadastro de cliente pode aparecer com endereço correto na NF-e e errado no boleto (ou vice-versa) — as regras de corte diferem entre os dois parsers. Candidato a virar um `AddressParser` compartilhado, ou melhor, migrar o cadastro para campos estruturados.

- [ ] 🟡 **[B-B2] `ReportTaxService` duplica quase por completo a geração dos 4 relatórios fiscais entre o caminho "ZIP" e "mapa de arquivos"**
  **Local:** `service/invoice/tax/ReportTaxService.java:68-128` (`generateMonthlyFiles`) vs. `:174-212` (`generateAndSaveReports`) — mesmos 4 relatórios, mesmo try/catch copiado 8 vezes ao todo.

- [ ] 🟡 **[B-B3] Normalização inconsistente do campo `codigoHistorico` da API do BB, em duas classes**
  **Local:** `service/finance/bb/TransactionBBApiService.isMarcadorDeSaldo:60-63` remove zeros à esquerda antes de comparar; `service/finance/bb/BBSaldoService.consultarSaldo:75-76` compara **sem** essa normalização. Se a API realmente retorna `"000"` (como a primeira classe documenta), o `if` da segunda nunca casa — o endpoint de saldo bancário quebraria sempre. Extrair a normalização para `BBExtratoParsingUtil` e usar nos dois lugares.

- [ ] 🟡 **[B-B4] Representação de valores monetários inconsistente entre geradores de Excel do mesmo módulo**
  **Local:** `service/finance/transaction/TransactionReportExcelGenerator.setAmountCell:94-104` grava número real; `BBExtratoExcelGenerator.java:88-97`/`SicoobExtratoExcelGenerator.java:74-85` gravam como string já formatada. Usuário não consegue somar/filtrar a coluna nas planilhas de extrato.

- [ ] 🔵 **[B-B5] `Category` (enum financeiro) mistura categorias de negócio com uma categoria pessoal, sem explicação** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `model/finance/Category.java:3-15` — `FAMÍLIA` é a única entrada acentuada, sem comentário sobre por que uma transação bancária da empresa cairia nessa categoria.

### B · Acoplamento excessivo

<a id="b-a2"></a>
- [x] 🟠 **[B-A2] Dois clientes HTTP diferentes (RestTemplate + WebClient) para a mesma API Focus NFe, com timeouts divergentes**
  **Local:** `service/invoice/FiscalNoteXmlStorageService.downloadFileBytes:443-459` (WebClient, timeout 60s) e `service/invoice/DanfeXmlService.downloadFileStream:69-98` (WebClient, timeout 100s), enquanto `config/FocusNfeApiClient.java` usa `RestTemplate` para as chamadas JSON. Qualquer ajuste de timeout/header/erro precisa ser replicado em 3 lugares — já divergiram sem motivo documentado.

- [ ] 🟡 **[B-A3] `FiscalNoteXmlStorageService` é uma classe com responsabilidades demais (candidato a "God class")**
  **Local:** `service/invoice/FiscalNoteXmlStorageService.java` (474 linhas, 7 dependências injetadas). Acumula: locking em memória por ref, job assíncrono de polling, download HTTP direto, orquestração de upload/rollback no R2, callback pós-commit, e regra de negócio de reversão de `hasInvoice`. Mistura infra + regra de negócio fiscal no mesmo arquivo.

- [ ] 🟡 **[B-A4] `BilletService` como fachada com 10 dependências injetadas e guard repetido 8 vezes**
  **Local:** `service/billet/BilletService.java:36-45`. O padrão `if (sicoobEnvironmentGuard.isBlocked()) {...}` se repete quase idêntico em 8 métodos públicos. Decisão de design documentada e aceita (fachada única para `BilletController`), mas o guard duplicado é candidato barato para extrair.

- [ ] 🔵 **[B-A5] Exports concorrentes batendo no mesmo diretório temporário fixo, sem lock**
  **Local:** `service/invoice/tax/ReportTaxService.createMonthlyFolder:162-172`, `service/finance/MacroExportService.createMacroFolder:92-103` — nomes de pasta determinísticos sem UUID. Dois usuários exportando o mesmo mês simultaneamente podem corromper o resultado um do outro. O padrão para resolver isso (`ConcurrentHashMap<String, ReentrantLock>`) já existe em `FiscalNoteXmlStorageService.refLocks:68-78` — só não foi reaplicado.

### B · Outros (moeda, idempotência, retry/timeout)

- [ ] 🟡 **[B-O1] `double` usado para dinheiro na geração de Excel de transações**
  **Local:** `service/finance/transaction/TransactionReportExcelGenerator.java:96` — limitação conhecida do Apache POI (sem overload `BigDecimal` para `Cell.setCellValue`), severidade moderada, mas é exatamente o padrão que gera diferenças de centavos em somas de planilha.

- [ ] 🔵 **[B-O2] Retry ausente na Focus NFe, inconsistente com o padrão já adotado para Sicoob/BB**
  **Local:** `config/FocusNfeApiClient.java:39-53,55-69` sem retry, enquanto `config/billet/BilletHttpClient.executeWithRetry:85-111` e `config/bb/BBExtratoClient.getExtratoPage:55-62` já implementam retry de 401 com invalidação de token. A emissão de NF-e — operação mais sensível do sistema — não tem nenhuma segunda tentativa em falha de rede transitória.

### B · Comentários desnecessários

Nenhum bloco relevante de código morto/comentado encontrado. Comentários de legenda em `IcmsPdfGenerator.java:156-206` repetem o rótulo da coluna em prosa — baixo custo, artefato de UX do PDF mais do que ruído.

---

## Área C — Demais Services

**Escopo analisado:** `service/purchase/**`, `service/notification/**` (email/whatsapp), `service/backup/**` (auth/folders/oauth), `service/climate/**`, `service/freight/**`, `service/chatbot/**`, `service/realtime/**`, `service/scheduler/**`, `service/storage/**`, `service/user/**`, `service/product/**`, `service/dashboard/**`, configs correspondentes, `tools/`, `util/` (~80 arquivos, ~8.900 linhas).

### C · Outros (bugs funcionais graves, fora das 5 categorias mas críticos demais para omitir)

<a id="c-o1"></a>
- [x] 🔴 **[C-O1] `TokenCleaner.deleteDirectoryRecursively` nunca deleta nenhum arquivo**
  **Local:** `service/backup/auth/TokenCleaner.java:16-25`
  ```java
  private static void deleteDirectoryRecursively(File directory) {
    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectoryRecursively(file); // nunca chama file.delete()
        }
      }
    }
  }
  ```
  Invocado por `TokenExceptionHandler.handleInvalidGrantError:47` exatamente quando o Google retorna `invalid_grant` e o fluxo tenta limpar tokens corrompidos antes de reautenticar. Hoje é um no-op — a recuperação de erro que a classe existe para viabilizar não funciona.

<a id="c-o2"></a>
- [x] 🟠 **[C-O2] `BackupService` extrai nome de arquivo com separador Windows (`\\`) em path gerado no Linux**
  **Local:** `service/backup/BackupService.java:34` — `String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);`. Em produção (Linux) isso retorna `-1`, então `fileName` vira o path absoluto inteiro em vez do nome do arquivo — enviado assim ao Google Drive.

- [ ] 🟠 **[C-O3] Dois mecanismos divergentes de diretório de tokens OAuth do Google**
  **Local:** `service/backup/oauth/AuthorizationFlowFactory.java:25` hardcoda `"temp/google/tokens"`, enquanto `CredentialManager`/`GoogleAuthService` (`service/backup/auth/GoogleAuthService.java:25-26`) usam a property `google.tokens.directory`. Se divergirem, token gravado por um fluxo não é encontrado pelo outro.

### C · Acoplamento excessivo

- [x] 🟠 **[C-A1] `service/backup` acessa e deleta entidades de `purchase`/`finance` diretamente, sem passar pelos services de domínio**
  **Local:** `service/backup/CsvGeneratorService.java:28-31` e `service/backup/EntityCleanupService.java:20-23` injetam `PurchaseRepository`, `InvoiceProductRepository`, `TransactionRepository`, `StatementRepository` diretamente e chamam `deleteAll`/`deleteByCreatedAtBetween` sem validação de regra de negócio do domínio dono. Backup deveria depender de `PurchaseService`/`ClientService`, não do repository cru.

- [x] 🟠 **[C-A2] `DashboardService` acessa `TransactionRepository` (finance) e `CombinedScoreRepository` (purchase) diretamente**
  **Local:** `service/dashboard/DashboardService.java:25-26`. Reimplementa filtros/agregações que deveriam vir dos services desses domínios.

- [ ] 🟡 **[C-A3] `GmailApiEmailSender` (notification/email) depende de `CredentialManager` (backup/auth)**
  **Local:** `service/notification/email/GmailApiEmailSender.java:13-14,45`. Decisão de design razoável (reaproveitar OAuth do backup), mas cria dependência cruzada entre domínios sem outra relação — mudança no fluxo OAuth do backup quebra silenciosamente o envio de e-mail.

- [ ] 🟡 **[C-A4] `CombinedScoreCancellationService` depende de `InvoiceService` e `BilletService` além do próprio `CombinedScoreService`**
  **Local:** `service/purchase/CombinedScoreCancellationService.java:41-47`. Bem documentado (evita dependência circular), mas o cancelamento de um agrupamento já conhece 3 domínios — cresce a cada novo domínio que precisar ser cancelado em cascata.

- [ ] 🟡 **[C-A5] `StatementSelectionService` (lógica de extrato bancário/finance) vive no pacote `service.notification`**
  **Local:** `service/notification/StatementSelectionService.java` — não manipula nada de notificação, está no pacote errado por conveniência histórica.

- [ ] 🔵 **[C-A6] `DatabaseStorageService` (pacote scheduler) depende de `NotificationCoordinator`/`EmailTemplateService`**
  **Local:** `service/scheduler/DatabaseStorageService.java:22-23`. Mistura monitorar tamanho do banco com montar/disparar e-mail.

### C · Baixa coesão / pacotes "gaveta"

- [ ] 🟡 **[C-B1] `util/TransactionUtil.java` mistura utilitário genérico com regra de negócio hardcoded (nomes de funcionários)**
  **Local:** `util/TransactionUtil.java:26-78`
  ```java
  map.put("marlucia natania vieira", Category.FUNCIONARIO);
  map.put("marcos", Category.FAMÍLIA);
  ```
  Nomes próprios de funcionários e do dono da empresa hardcoded como strings mapeadas para categoria contábil. Troca de funcionário exige código + deploy; dado de RH vaza para o código-fonte. Também recria o `HashMap` a cada chamada em vez de ser campo estático.

- [ ] 🔵 **[C-B2] `TransactionUtil` é `@Component` com construtor privado e só métodos estáticos** — contradição de design (bean Spring vs. classe utilitária estática).

- [ ] 🔵 **[C-B3] Pacote `util/` mistura domínios sem relação temática**
  **Local:** `PdfUtil` (purchase), `HttpRequestUtils` (segurança/infra), `FileZipUtils` (export genérico), `SicoobExtratoFormatUtil` (finance), `TransactionUtil` (finance) — só compartilham o rótulo "utilitário".

- [ ] 🔵 **[C-B4] `FileMetadataFactory.createFolderMetadata` é código morto/duplicado**
  **Local:** `service/backup/folders/FileMetadataFactory.java:21-31` duplica exatamente `FolderManager.createFolderMetadata:75-85`, mas nunca é chamado — `FolderManager` usa sua própria cópia privada.

### C · Clareza / código confuso

- [ ] 🟠 **[C-C1] Parsing de PDF de fornecedor por regex/split posicional sem tolerância a formato alternativo**
  **Local:** `service/purchase/PurchaseProcessingService.extractProducts/parseProductLine:123-234`. Assume layout fixo de colunas; qualquer mudança no PDF do fornecedor quebra a extração de forma imprevisível. `catch (Exception e)` em `:97-98` embrulha qualquer erro (incluindo bugs do próprio parser) na mesma mensagem genérica.

- [ ] 🟡 **[C-C2] `ClientService.findMatchingClient` carrega todos os clientes em memória e usa heurística de nome frágil**
  **Local:** `service/purchase/ClientService.java:143-174`. `findAll()` sem paginação; heurística remove a letra "L" de ambos os lados para comparar (`clientFirstName.replace("L", "")`) sem explicação — pode casar nomes que não deveriam colidir.

- [ ] 🟡 **[C-C3] `WhatsAppService.formatPhoneNumber` assume DDD 31 (BH) como fallback, magic prefix sem constante**
  **Local:** `service/notification/whatsapp/WhatsAppService.java:65-73` — `return "+5531" + cleanNumber;`

- [ ] 🟡 **[C-C4] `WhatsAppService.sendMultipleDocuments` bloqueia a thread da requisição com `Thread.sleep(2000)` por documento**
  **Local:** `service/notification/whatsapp/WhatsAppService.java:171-177`. Em envio em massa (10 clientes × 3 documentos × 2s = 60s de thread bloqueada por request), risco real de esgotamento do pool.

- [ ] 🟡 **[C-C5] `WeatherForecastService` faz parsing manual de `Map<String,Object>` com casts em cascata não verificados**
  **Local:** `service/climate/WeatherForecastService.java:28-154` — `@SuppressWarnings("unchecked")` espalhado em 3 métodos; campo ausente na resposta da OpenWeather gera `NullPointerException`/`ClassCastException` não tratado → 500 genérico.

- [ ] 🟡 **[C-C6] `DashboardService.getDashboardData` retorna `Map<String,Object>` com chaves inconsistentes (`"TotalReceita"` vs. `"Fluxo de Vendas"` com espaço)**
  **Local:** `service/dashboard/DashboardService.java:29-57`. Sem contrato tipado — erro de digitação no frontend não é pego em compilação.

- [ ] 🟡 **[C-C7] `DashboardService` recarrega toda a tabela de `CombinedScore` e repete a mesma query de `Transaction` 6 vezes por chamada**
  **Local:** `service/dashboard/DashboardService.java:325-334` — `findAllByOrderByIdDesc(Pageable.unpaged())` sem filtro de data, filtra em memória; chamado por 3 métodos diferentes. `findTransactionsByDateRange` repetido 6× para o mesmo intervalo sem cache/reuso. Piora proporcionalmente conforme a base cresce.

- [ ] 🔵 **[C-C8] `new RestTemplate()` sem timeout em `DistanceMatrixService.fetchApiResponse`, inconsistente com o resto do projeto**
  **Local:** `service/freight/DistanceMatrixService.java:80` — único ponto que cria `RestTemplate` "cru" a cada chamada, risco de travar a thread indefinidamente.

- [ ] 🔵 **[C-C9] Validação de senha duplicada (4-20 caracteres, magic numbers)** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `service/user/UserService.java:39,67`

- [ ] 🔵 **[C-C10] `InvoiceProductService.deleteInvoiceProduct` faz `existsById` seguido de `findById(...).get()`**
  **Local:** `service/purchase/InvoiceProductService.java:37-43` — race condition teórica, uso desnecessário de `.get()`. Poderia ser um único `findById(...).orElseThrow(...)`.

- [ ] 🔵 **[C-C11] `GroupedProductService` usa `catch (Exception e)` genérico ao redor de agrupamento em memória de listas já validadas**
  **Local:** `service/purchase/GroupedProductService.java:33-45,53-65` — embolsa qualquer `RuntimeException` (incluindo bugs) e reembrulha como `PurchaseException`, perdendo o stacktrace real.

### C · Duplicação de código

- [ ] 🟡 **[C-D1] Três provedores de e-mail duplicam o método `addInlineLogo` quase identicamente**
  **Local:** `service/notification/email/SendGridEmailSender.java:98-119`, `GmailSmtpEmailSender.java:143-152`, `GmailApiEmailSender.java:171-180`. Correção de bug nessa lógica precisa ser replicada em 3 lugares.

- [ ] 🔵 **[C-D2] Montagem de contexto de mensagem (`Map<String,String> variables`) repetida quase palavra-por-palavra em 3 services**
  **Local:** `service/notification/NotificationService.java:222-237` vs. `BulkNotificationService.java:255-272` vs. `DatabaseStorageService`.

### C · Tratamento de erro genérico / catch silencioso

- [ ] 🟠 **[C-E1] `BulkNotificationService.sendBulkNotifications` engole exceção sem logar**
  **Local:** `service/notification/BulkNotificationService.java:83-86`
  ```java
  } catch (Exception e) {
    return BulkNotificationResponse.failure("Erro ao enviar notificações: " + e.getMessage(), List.of());
  }
  ```
  Uma falha inesperada (ex.: `NullPointerException` por bug) desaparece sem rastro nos logs em produção. Mesmo padrão em `sendToClients:219-221`.

- [ ] 🟡 **[C-E2] `WhatsAppService.sendTextMessage`/`sendDocument` capturam `Exception` amplo sem log antes de relançar**
  **Local:** `service/notification/whatsapp/WhatsAppService.java:98-101,135-138` — perde o tipo original do erro (timeout de rede vs. telefone inválido).

- [ ] 🔵 **[C-E3] `EmailTemplateService.processTemplate` engole `IOException` e retorna HTML de fallback fixo, sem log**
  **Local:** `service/notification/email/EmailTemplateService.java:19-26` — template renomeado/ausente vira silenciosamente e-mail com conteúdo errado, sem alarme.

### C · Comentários / documentação

- [ ] 🔵 **[C-F1] Comentário informal misturando explicação de regra de negócio sem estrutura de Javadoc**
  **Local:** `service/climate/ClimateProductRecommendationService.java:35-38`

- [ ] 🔵 **[C-F2] `FiscalProductService`/`UserService` sem comentário de classe, inconsistente com o padrão do resto do domínio `purchase`/`storage`**

Nenhum bloco relevante de código morto comentado foi encontrado.

### C · Outros (infra/operação)

- [ ] 🟠 **[C-G1] Tokens OAuth do Google Drive/Gmail persistidos em disco local, não em banco/secret store**
  **Local:** `service/backup/auth/CredentialManager.java:44-45`, `service/backup/oauth/AuthorizationFlowFactory.java:55`. Em ambientes com filesystem efêmero (containers, redeploys), tokens são perdidos a cada reinício, forçando reautenticação manual — sem criptografia em repouso.

- [ ] 🟡 **[C-G2] `AuthorizationHandler` usa fluxo OAuth desenhado para app desktop (`LocalServerReceiver` na porta 8888) dentro de um backend servidor**
  **Local:** `service/backup/auth/AuthorizationHandler.java:15-30`. Possível código legado morto — o fluxo real em produção parece ser via `oauth/GoogleOAuthService`. Vale confirmar se ainda é chamado; se morto, a porta 8888 hardcoded é superfície de risco/confusão desnecessária.

- [ ] 🔵 **[C-G3] `GoogleAuthService.getDriveService()` reconstrói o cliente Drive (com handshake OAuth completo) a cada chamada, sem cache**
  **Local:** `service/backup/auth/GoogleAuthService.java:28-59` — chamado N vezes na mesma operação lógica de backup.

---

## Área D — Controllers, DTOs e Mappers

**Escopo analisado:** `controller/**` (23 controllers, todas as subpastas), `dto/**` (~90 DTOs), `mapper/**` (9 mappers).

### D · Violações diretas de regras documentadas no README (categoria "Vulnerabilidades"/arquitetura)

O README documenta 3 regras: *"Controllers nunca acessam repository diretamente"*, *"Endpoints sensíveis exigem `@PreAuthorize(\"hasRole('MANAGER')\")`"*, *"DTOs via MapStruct — entidades JPA não são expostas diretamente"*.

<a id="d-v1"></a>
- [x] 🔴 **[D-V1] `ClientController` sem `@PreAuthorize` em nenhum endpoint — `DELETE /clients/{id}` deveria ser restrito a Gestor**
  **Local:** `controller/purchase/ClientController.java` (arquivo inteiro, zero ocorrências de `@PreAuthorize`)
  ```java
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    clientService.deleteClient(id);
    return ResponseEntity.noContent().build();
  }
  ```
  O README (linha 172) documenta explicitamente essa rota como restrita a Gestor. No código, é acessível a qualquer usuário autenticado (`EMPLOYEE` incluído) — qualquer funcionário pode apagar cadastros de clientes vinculados a histórico de compras/boletos/notas fiscais. Divergência clara entre documentação e código — prioridade máxima.

<a id="d-v2"></a>
- [x] 🔴 **[D-V2] `AuthController` injeta e usa `UserRepository` diretamente**
  **Local:** `controller/user/AuthController.java:12,41,92`
  ```java
  private final UserRepository userRepository;
  ...
  User user = userRepository.findById(rotation.userId()).orElse(null);
  ```
  Único caso no projeto (confirmado via grep em toda a árvore `controller/`) — todos os outros 22 controllers respeitam a regra. Correção: mover a busca para dentro de `RefreshTokenService`/`Auth`.

- [ ] 🟠 **[D-V3] `PurchaseMapper` nunca é usado — `Purchase` (entidade JPA) atravessa service→controller e é mapeada manualmente em 5 lugares**
  **Local:** `mapper/PurchaseMapper.java` (morto, zero referências); repetido manualmente em `controller/purchase/PurchaseController.java:44-52`, `service/purchase/PurchaseService.java:~193,~263,~284`, `service/purchase/CapturaNotaPendenteService.java:124`.
  ```java
  Purchase purchase = purchaseService.createManualPurchase(request);
  return ResponseEntity.ok(new PurchaseResponse(purchase.getId(), purchase.getPurchaseDate(), purchase.getTotal(), purchase.getUpdatedAt()));
  ```
  Mudança futura em `PurchaseResponse` exige tocar em 5 arquivos manualmente. Usar o mapper já existente e hoje inerte.

### D · Qualidade da camada de Controller

**Tratamento de exceção duplicado** (ignora o `GlobalExceptionHandler` já existente, 369 linhas, cobre praticamente toda exceção de domínio):

<a id="d-c1"></a>
- [x] 🟠 **[D-C1] `BilletController` — todos os 8 endpoints reimplementam tratamento de erro manual**
  **Local:** `controller/billet/BilletController.java:34-43,66-69,77-80,87-90,104-123,133-155,163-166,178-181`
  ```java
  } catch (BilletException e) {
    if (errorMessage.contains("Título em processo de baixa/liquidação")) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("...");
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(...);
  }
  ```
  `GlobalExceptionHandler.handleBilletException` já devolve `400` com JSON `{error, message}` padronizado. Este controller devolve ora `String` crua, ora `byte[]`, ora `409`, ora `500` — inconsistente entre si e com o resto da API.

- [ ] 🟡 **[D-C2] `NotificationController` — 3 métodos com try/catch manual duplicando exceções já cobertas pelo handler central**
  **Local:** `controller/notification/NotificationController.java:69-84,101-115,171-190`

- [ ] 🟡 **[D-C3] `WeatherForecastController` — dois blocos `catch` idênticos, devolvem 500 vazio (mascara a causa real)**
  **Local:** `controller/climate/WeatherForecastController.java:31-38`

- [ ] 🟡 **[D-C4] `ReportTaxController` — captura `Exception` genérica e devolve a mensagem crua no corpo, potencial vazamento de detalhe interno**
  **Local:** `controller/invoice/ReportTaxController.java:25-41`

- [ ] 🟡 **[D-C5] Mesmo padrão de try/catch manual redundante em outros 3 pontos**
  **Local:** `controller/invoice/InvoiceController.java:111-120`; `controller/purchase/PurchaseController.java:32-39`; `controller/purchase/CombinedScoreController.java:101-108` (retorna 500 com corpo `null` em vez de deixar a exceção subir).

**Falta de validação de entrada (Bean Validation):**

<a id="d-a1"></a>
- [x] 🟠 **[D-A1] `ManualPurchaseRequest`/`ManualPurchaseItemRequest` sem nenhuma anotação de validação, sem `@Valid` no controller**
  **Local:** `dto/purchase/ManualPurchaseRequest.java:6-7`, `ManualPurchaseItemRequest.java:5`; `controller/purchase/PurchaseController.java:42-44` e `controller/purchase/NotaController.java:89-91` sem `@Valid`.
  Alimenta diretamente registros financeiros — permite preço/quantidade negativos ou nulos. Compare com `WildcardBilletRequest.java:7-11`, que já tem `@NotNull`/`@Positive` — o padrão existe no projeto, só não foi aplicado aqui.

<a id="d-a2"></a>
- [x] 🟠 **[D-A2] `UpdateInvoiceProduct`/`UpdateGroupedProduct` sem validação — permitem preço/quantidade negativos via PUT**
  **Local:** `dto/purchase/UpdateInvoiceProduct.java:5-6`; `controller/purchase/InvoiceProductController.java:19-21` sem `@Valid`. (`UpdateGroupedProduct` também está morto — ver seção D-morto.)

- [ ] 🟡 **[D-A3] `ClientRequest.variablePrice` é `@NotNull` sobre um `boolean` primitivo — validação inócua, nunca dispara**
  **Local:** `dto/purchase/client/ClientRequest.java:9` — Jackson sempre desserializa `boolean` ausente como `false`; a anotação nunca barra nada.

- [ ] 🟡 **[D-A4] `UserUpdateRequest` sem nenhuma anotação de validação, apesar do controller usar `@Valid`**
  **Local:** `dto/user/UserUpdateRequest.java:6-7`; `controller/user/UserController.java:43-44,50-51`. `PUT /users/update` com `username`/`password` vazios passa sem erro.

- [ ] 🔵 **[D-A5] `DashboardController.getDashboardData` faz `LocalDate.parse`/`Month.of` sem tratamento — request malformada estoura exceção não controlada**
  **Local:** `controller/dashboard/DashboardController.java:27-29`

**Falta de `@PreAuthorize` em endpoints sensíveis** (além do D-V1 crítico):

- [ ] 🟡 **[D-A6] `BilletController` — zero `@PreAuthorize`, incluindo `mark-paid` e `cancel`/`cancel-by-number`**
  **Local:** `controller/billet/BilletController.java` (arquivo inteiro). Qualquer `EMPLOYEE` pode marcar boleto como pago ou cancelá-lo.

- [ ] 🟡 **[D-A7] `CombinedScoreController` — zero `@PreAuthorize`, incluindo `DELETE`/`confirm-payment`/`cancel-payment`**
  **Local:** `controller/purchase/CombinedScoreController.java` (arquivo inteiro)

**Inconsistência de padrão REST:**

- [ ] 🟡 **[D-R1] Nomenclatura de path variable inconsistente dentro do mesmo controller (`combinedScoreId` vs. `idCombinedScore`)**
  **Local:** `controller/billet/BilletController.java` — linhas 28,98,158,173 vs. 83,102 (mesmo `Long`, dois nomes diferentes no mesmo arquivo).

- [ ] 🔵 **[D-R2] Verbo no path (`/users/update`, `/users/delete/{username}`) destoa do resto da API (REST puro)**
  **Local:** `controller/user/UserController.java:42,49,62`

- [ ] 🔵 **[D-R3] Rota do `ReportTaxController` não bate com a documentada no README**
  **Local:** `controller/invoice/ReportTaxController.java:14-16,21` — sem `@RequestMapping` de classe, rota real é `/icms-report/monthly/{start}/{end}`; README (linha 241) documenta `/dashboard/icms-report/monthly/{start}/{end}`.

- [ ] 🔵 **[D-R4] `ResponseEntity<?>` (wildcard) em múltiplos endpoints, com `String` hardcoded em vez de DTO tipado**
  **Local:** `controller/purchase/CombinedScoreController.java:37,70,76,82`; `controller/purchase/PurchaseController.java:31,56`

**Falta de paginação:**

- [ ] 🟡 **[D-P1] Listagens sem paginação apesar do padrão `Page`/`Pageable` já existir em outros endpoints do projeto**
  **Local:** `controller/billet/BilletController.java:74-81` (todos os boletos em aberto); `controller/purchase/ClientController.java:30-33,57-61,68-72`; `controller/invoice/InvoiceController.java:63-66`; `controller/finance/StatementController.java:30-34` (cresce indefinidamente); `controller/purchase/CombinedScoreController.java:64-67`

- [ ] 🔵 **[D-P2] `GET /products` sem paginação e `GET /products/paginated` com paginação coexistindo**
  **Local:** `controller/climate/ProductController.java:44-47` vs. `49-82`

**Lógica de parsing/mapeamento manual dentro do controller:**

- [ ] 🟡 **[D-M1] `NotificationController` monta DTOs manualmente de `@RequestParam` soltos, incl. `new BigDecimal(String)` sem tratamento de erro**
  **Local:** `controller/notification/NotificationController.java:55-68,70-72,90-104` — `NumberFormatException` não mapeada cai no handler genérico (500 em vez de 400); mesmo para `NotificationChannel.valueOf` com string inválida.

- [ ] 🟡 **[D-M2] Magic strings decidindo código HTTP por conteúdo de mensagem de exceção**
  **Local:** `controller/billet/BilletController.java:39,111,140,144`
  ```java
  if (errorMessage.contains("já foi gerado")) { ... }
  if (errorMessage.contains("Título em processo de baixa/liquidação")) { ... }
  ```
  Lógica de negócio (decidir código HTTP) codificada como comparação de substring de log — frágil, quebra se o texto da exceção mudar. Deveria ser um tipo de exceção dedicado tratado no `GlobalExceptionHandler`.

- [ ] 🔵 **[D-M3] Validação manual de ID repetida 3× no mesmo controller em vez de `@Positive` no `@PathVariable`**
  **Local:** `controller/climate/ProductController.java:96-98,158-160,178-180`

### D · Acoplamento e baixa coesão em DTOs/Mappers

- [ ] 🔵 **[D-morto] DTOs mortos — declarados mas nunca referenciados (confirmado via busca no projeto), sobrevivem só na documentação**
  **Local:** `dto/notification/BulkNotificationRequest.java` (endpoint real usa `@RequestParam` soltos), `AccountingNotificationRequest.java`, `ClientNotificationRequest.java`, `MonthlyStatementsRequest.java`, `dto/purchase/UpdateGroupedProduct.java`. Indica refactor anterior que não removeu os DTOs nem atualizou `dto/notification/README.md`.

- [ ] 🟡 **[D-D1] `TransactionMapper.toTransaction` recebe 9 parâmetros posicionais, 6 do mesmo tipo `String` intercalados**
  **Local:** `mapper/TransactionMapper.java:34-44` — risco real de troca de argumentos que o compilador não pega. Usado em `TransactionBBApiService.java:89` e `TransactionSicoobApiService.java:59`. Melhor receber um DTO/record de entrada.

- [ ] 🔵 **[D-D2] `@Mapping(source="x", target="x")` redundante em várias interfaces MapStruct (ruído sem benefício)**
  **Local:** `mapper/ClientMapper.java:14-18,22-33`, `CombinedScoreMapper.java:19-27`, `GroupedProductMapper.java:12-16`, `InvoiceProductMapper.java:11-16`, `PurchaseMapper.java:12-15`, `UserMapper.java:16-19`

- [ ] 🔵 **[D-D3] Lógica de formatação de apresentação embutida no DTO de resposta em vez do mapper**
  **Local:** `dto/climate/ProductResponse.java:34-42` — `formatMonthsList` calcula string de exibição dentro do próprio DTO.

- [ ] 🔵 **[D-D4] `BulkNotificationRequest.dueDate` é `BigDecimal`, não `LocalDate` — nome sugere data, tipo sugere valor monetário**
  **Local:** `dto/notification/BulkNotificationRequest.java:12` (DTO morto, mas risco se reaproveitado)

### D · Clareza / código confuso

- [ ] 🔵 **[D-CL1] Javadoc incompleto — documenta 1 de 3 parâmetros, descrição não bate com o nome do parâmetro**
  **Local:** `controller/billet/BilletController.java:25-33`

- [ ] 🔵 **[D-CL2] `throws IOException` na assinatura é morto/enganoso — o corpo inteiro já está em `try/catch(Exception e)`**
  **Local:** `controller/billet/BilletController.java:29-33`

### D · Comentários desnecessários / código morto

Poucos achados — nenhum bloco de código comentado ou `TODO`/`FIXME` encontrado. A maioria dos Javadocs lidos explica decisões não óbvias.

### D · Documentação desatualizada (fora do escopo estrito, mas relevante)

- [ ] 🟡 **[D-DOC1] READMEs de pacote descrevem controllers que não existem mais no código**
  **Local:** `controller/chatbot/README.md` e `controller/scheduler/README.md` documentam `ChatbotController.java`/`SchedulerController.java` com endpoints detalhados — nenhum dos dois existe (deletados no commit `e36ea1a`, confirmado via `git log --diff-filter=D`). Pode levar alguém a acreditar que os endpoints ainda existem.

---

## Área E — Models, Repositories e Configuração/Build

**Escopo analisado:** `model/**` (todas subpastas), `repository/**` (todas subpastas), `HortifrutiSlApplication.java`, `application*.properties`, `products.yml`, `pom.xml`.

### E · Design de Entidades JPA

<a id="e-m1"></a>
- [x] 🔴 **[E-M1] `Client.purchases` com `cascade = CascadeType.ALL` — deleção em cascata de histórico de compras**
  **Local:** `model/purchase/Client.java:81-83`
  ```java
  @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
  @JsonIgnore
  private List<Purchase> purchases;
  ```
  `CascadeType.ALL` inclui `REMOVE` — apagar um cliente apaga todas as suas compras junto, registros financeiros/fiscais que normalmente precisam ser preservados para auditoria/contabilidade. Diferente de itens de linha sem valor fora do agregado (`Purchase.invoiceProducts`), `Purchase` é um registro de venda com valor próprio.

<a id="e-m2"></a>
- [x] 🔴 **[E-M2] Campos monetários como `double` em `FreightConfig`**
  **Local:** `model/FreightConfig.java:23-36` — os 12 campos (`kmPerLiterConsumption`, `fuelPrice`, `maintenanceCostPerKm`, `tireCostPerKm`, `depreciationCostPerKm`, `insuranceCostPerKm`, `baseSalary`, `chargesPercentage`, `monthlyHoursWorked`, `administrativeCostsPercentage`, `marginPercentage`, `fixedFee`) são todos `double`. Ponto flutuante binário não representa decimais exatamente — inaceitável na entidade que alimenta o cálculo de custo/preço de frete. Contrasta com o resto do domínio financeiro, que já usa `BigDecimal`.

- [ ] 🟠 **[E-J1] Todos os `@ManyToOne` de negócio sem `fetch = FetchType.LAZY` (EAGER implícito)**
  **Local:** `model/finance/Transaction.java:26-28` (`statement`), `model/purchase/Purchase.java:27-29` (`client`), `model/purchase/InvoiceProduct.java:47-50` (`purchase`), `model/purchase/GroupedProduct.java:35-37` (`combinedScore`)
  Especialmente grave em `Transaction.statement`/`Purchase.client`: repositórios com dezenas de métodos que retornam listas grandes (relatórios, exportações, dashboards) — cada linha dispara query adicional (N+1) ou força JOIN implícito mesmo quando só se precisa do valor/data. `Purchase.client` não tem `@JsonIgnore`, então serializar uma lista de `Purchase` expõe o `Client` completo por item.

- [ ] 🟠 **[E-J2] Colunas `@Lob` legadas sem `@Basic(fetch = FetchType.LAZY)`**
  **Local:** `model/finance/Statement.java:38-40` (`filePath`, LONGBLOB), `model/invoice/FiscalNoteXmlStorage.java:39-41` (`xmlContent`, LONGTEXT). O próprio Javadoc diz que são campos "legados" (registros novos usam `objectKey`/R2) — para linhas antigas, um blob de vários MB é carregado inteiro sempre que a entidade é buscada, mesmo em telas de listagem que só precisam de metadados.

- [ ] 🟡 **[E-J3] `@Data` do Lombok em entidade JPA, inconsistente com o resto do projeto**
  **Local:** `model/climate/ClimateProduct.java:11` — todas as outras ~20 entidades usam `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` explícitos. `@Data` gera `equals`/`hashCode` sobre todos os campos mutáveis, quebrando identidade estável em `Set`/`HashMap` se a entidade for mutada após inserida — padrão desaconselhado para entidades JPA.

- [ ] 🟡 **[E-J4] `precision`/`scale` ausentes em colunas monetárias, inconsistente com o padrão do domínio**
  **Local:** `model/purchase/CombinedScore.java:31-32` (`totalValue`), `model/purchase/Purchase.java:38-39` (`total`) — sem `precision`/`scale` explícitos, diferente de `GroupedProduct` (`precision=10/12, scale=4`), `Transaction.amount` (`precision=15, scale=2`). Risco de truncamento/arredondamento inconsistente entre `Purchase.total` somado e `CombinedScore.totalValue`.

- [ ] 🟡 **[E-J5] Ausência quase total de Bean Validation nas entidades**
  **Local:** todo `model/**` — único uso é `Client.java:39` (`@Email`). O projeto declara `spring-boot-starter-validation` mas campos obrigatórios só têm `@Column(nullable=false)`, verificado apenas no INSERT/UPDATE (exceção de banco pouco amigável), não na camada de aplicação.

- [ ] 🔵 **[E-J6] Nome de campo misturando `camelCase` e `snake_case`**
  **Local:** `model/purchase/CombinedScore.java:45` — `private String ourNumber_sicoob;` (único campo com underscore no meio do identificador).

- [ ] 🔵 **[E-J7] `GroupedProduct` e `CombinedScore` sem timestamps de auditoria**
  **Local:** arquivos inteiros — diferente de praticamente todas as outras entidades. `CombinedScore` representa um agrupamento de cobrança (boleto/NF); a ausência de `createdAt`/`updatedAt` dificulta auditoria.

- [ ] 🔵 **[E-J8] Enum `Category` com valor acentuado inconsistente e semântica questionável** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `model/finance/Category.java:8` — `FAMÍLIA` é o único valor acentuado.

### E · Acoplamento excessivo

- [ ] 🟠 **[E-R1] Nome de enum totalmente qualificado repetido como literal de string dentro de `@Query`, 6 vezes**
  **Local:** `repository/purchase/CombinedScoreRepository.java` linhas 28,33,41,46,76,89-90
  ```java
  @Query("SELECT cs FROM CombinedScore cs WHERE cs.clientId = :clientId AND cs.status = com.hortifruti.sl.hortifruti.model.purchase.Status.PENDENTE AND cs.hasBillet = true")
  ```
  JPQL não é verificado em tempo de compilação — mover/renomear `Status` quebra essas 6 queries silenciosamente, só detectável em runtime.

- [ ] 🟡 **[E-R2] `TransactionRepository` com 6+ métodos de query redundantes apesar de já ter `JpaSpecificationExecutor`**
  **Local:** `repository/finance/TransactionRepository.java:24-90` — deveria compor `Specification`s combináveis (mecanismo já disponível na interface) em vez de multiplicar métodos manuais.

- [ ] 🔵 **[E-R3] Query retorna enum como `List<String>` em vez de `List<Category>`**
  **Local:** `repository/finance/TransactionRepository.java:41-42`

- [ ] 🔵 **[E-R4] Projeção não tipada (`Object[]`) em vez de DTO/interface**
  **Local:** `repository/purchase/PurchaseRepository.java:22-23` — `List<Object[]> sumTotalGroupedByClientId()`, exige cast manual no chamador.

### E · Baixa coesão

- [ ] 🔵 **[E-B1] Nome de repositório genérico demais, ambíguo entre domínios**
  **Local:** `repository/climate/ProductRepository.java` — entidade é `ClimateProduct`, mas o repositório se chama só `ProductRepository`, colidindo conceitualmente com `FiscalProductRepository`, `GroupedProductRepository`, `InvoiceProductRepository`.
  *(De resto, os repositórios do projeto são coesos — nenhum outro método fora do propósito da entidade principal foi encontrado.)*

### E · Clareza / código confuso

- [ ] 🟡 **[E-CL1] README descreve campo (`totalPurchaseValue`) que não existe mais na entidade `Client`**
  **Local:** `model/purchase/README.md:7` — campo removido intencionalmente (ver `repository/purchase/PurchaseRepository.java:18`, `mapper/ClientMapper.java:34-36`), README não atualizado.

- [ ] 🟡 **[E-CL2] Documentação descreve entidades e repositório de chatbot que não existem no código**
  **Local:** `model/chatbot/README.md`, `repository/chatbot/README.md` — descrevem `ChatSession.java`, `SessionStatus.java`, `SessionContext.java`, `ChatSessionRepository.java` em detalhe, mas nenhum arquivo existe (as pastas só têm o `README.md`).

- [ ] 🔵 **[E-CL3] Typo recorrente `update_at` (faltando o "d") em 4 entidades diferentes**
  **Local:** `model/finance/Statement.java:70`, `model/product/FiscalProduct.java:45`, `model/purchase/InvoiceProduct.java:41`, `model/purchase/Purchase.java:58` — vs. `updated_at` correto no resto do sistema (`User`, `Client`, `LoginLockout`, etc.). Exige migração se corrigido depois de dados em produção.

- [ ] 🔵 **[E-CL4] Dois enums quase idênticos representando "canal de notificação"**
  **Local:** `model/notification/NotificationChannel.java` (`EMAIL, WHATSAPP, BOTH`) e `model/notification/NotificationType.java` (`EMAIL_ONLY, WHATSAPP_ONLY, BOTH`) — ambos espalhados por DTOs/services distintos, sem clareza de quando usar um ou outro.

### E · Comentários desnecessários / código morto

- [ ] 🔵 **[E-CM1] Linha de configuração comentada residual**
  **Local:** `application.properties:270` — `#debug=true`. Sem efeito, ruído a remover.

*(Nenhum outro bloco de código morto comentado encontrado em `model/**`/`repository/**`.)*

### E · Configuração / Build

<a id="e-c1"></a>
- [ ] 🔴 **[E-C1] `ddl-auto=update` em produção, sem Flyway/Liquibase**
  **Local:** `application-local.properties:9`, `application-hml.properties:11`, `application-prod.properties:12` (os 3 ambientes) — `spring.jpa.hibernate.ddl-auto=update`. `pom.xml` não tem Flyway nem Liquibase. O Hibernate altera o schema de produção automaticamente a cada deploy, por inferência das entidades — sem histórico de migração versionado, sem plano de rollback. Reforçado por scripts SQL manuais em `static/*.sql` (`billet_files_migration.sql`, `fiscal_note_xml_storage_migration.sql`, etc.) que existem no repositório mas não são aplicados automaticamente por nenhuma ferramenta — dependem de alguém lembrar de rodá-los manualmente, facilmente divergindo entre `hml` e `prod`.

<a id="e-c2"></a>
- [ ] 🟡 **[E-C2] `products.yml` como fonte paralela à tabela `fiscal_products`, com dois parsers independentes**
  **Local:** `products.yml` (~80 produtos), `config/FiscalProductInitializer.java:19-24`, `service/invoice/ProductNFService.java:9-28`. O próprio Javadoc do initializer admite: sincroniza `products.yml` → tabela na subida da app, **mas** `ProductNFService` (usado na emissão real de NF-e) lê o YAML direto do classpath, ignorando a tabela. Correção cadastral de um produto exige rebuild+redeploy no fluxo mais crítico fiscalmente, apesar da tabela já ser editável em runtime.

- [ ] 🟡 **[E-C3] `spring-boot-starter-web` e `spring-boot-starter-webflux` juntos no mesmo projeto**
  **Local:** `pom.xml:67-78` — duas pilhas HTTP concorrentes (Tomcat + Reactor Netty). Se o motivo é só `WebClient` reativo para chamar APIs externas, dá para obter isso com dependência mais enxuta, ou migrar para `RestClient` síncrono (Spring Framework 6.1+) e remover o starter reativo.

- [ ] 🔵 **[E-C4] Coordenada Maven desatualizada do Bucket4j** *(ver [seção 9](#9-achados-duplicados-entre-áreas))*
  **Local:** `pom.xml:174-178` — `com.github.vladimir-bukhtoyarov:bucket4j-core`

- [ ] 🔵 **[E-C5] Metadados de projeto vazios (boilerplate do Spring Initializr nunca preenchido)**
  **Local:** `pom.xml:18-31` — `<url/>`, `<licenses><license/></licenses>`, `<developers><developer/></developers>`, `<scm>...</scm>` vazios. Não bloqueia build, é ruído.

---

## 9. Achados duplicados entre áreas

Sinalizados **independentemente** por duas frentes de análise diferentes — reforça que são pontos reais, não ruído de uma leitura isolada:

- [ ] **Validação de senha (4-20 caracteres, magic numbers, duplicada em 2 métodos)** — reportado em [A-V3](#a-v1)/[A-C3] e [C-C9](#área-c--demais-services), todos apontando para `service/user/UserService.java:39,67` e `dto/user/UserRequest.java:11`. Correção única resolve os dois achados: extrair `validateAndEncodePassword(String)` e subir o mínimo para 8+ caracteres.
- [ ] **`bucket4j-core` com `groupId` descontinuado** — reportado em [A-V8](#a-v1) e [E-C4](#e-c1), ambos em `pom.xml`.
- [ ] **`Category.FAMÍLIA` — enum financeiro com valor acentuado/semântica questionável** — reportado em [B-B5](#b-c1) e [E-J8](#e-m1), ambos observando o vínculo com `util/TransactionUtil.java:61` (nome pessoal "marcos" hardcoded → `FAMÍLIA`), que a frente de "Demais Services" também aponta em [C-B1](#c-o1) como problema de dado de RH hardcoded no código-fonte.

---

## 10. O que já está bom (não mexer)

Para não perder de vista em meio a ~140 achados — pontos que as 5 frentes de análise destacaram positivamente e que **não devem ser "refatorados" por engano**:

- Cookies de autenticação corretamente configurados por ambiente (`Secure=true`/`SameSite=None` em prod/hml, `false`/`Lax` em local).
- Nenhum segredo hardcoded no código — tudo via `.env`/variáveis de ambiente.
- CORS com origens explícitas (sem wildcard), JWT com algoritmo/issuer fixos e verificação de assinatura correta.
- `BigDecimal` usado corretamente para dinheiro na maior parte do domínio fiscal/financeiro (exceções pontuais já listadas acima).
- Detecção de reuso de refresh token revogado (deteção de vazamento) já implementada corretamente em `RefreshTokenService`.
- Lockout progressivo por conta com auditoria de tentativas (`LoginAuditLog`) — a lógica em si está correta, só o lockout por IP é que fica neutralizado pelo achado A-V1.
- Retry com invalidação de token em `BilletHttpClient`/`BBExtratoClient` — padrão bem implementado, só não replicado para a Focus NFe (B-O2).
- Padrão de comentários explicando "por quê" (não "o quê") é consistente na maior parte do código — a categoria "comentários desnecessários" foi, de longe, a com menos achados nas 5 frentes de análise.
