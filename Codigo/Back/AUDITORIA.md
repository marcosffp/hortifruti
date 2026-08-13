# Auditoria de Qualidade e Segurança — Backend Hortifruti SL

**Data:** 2026-08-06
**Escopo:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/**` (381 arquivos Java), `application*.properties`, `products.yml`, `pom.xml`.
**Metodologia:** leitura completa (não apenas grep) de todo o código-fonte, dividida em 5 frentes paralelas por área do sistema. Nenhuma correção foi aplicada — este documento é só diagnóstico, para orientar um trabalho de refatoração subsequente.
**Não avaliado:** frontend, testes automatizados (o projeto não tem pasta `src/test` com cobertura relevante — ver seção 7), infraestrutura de deploy além do que está em `Dockerfile`/`pom.xml`.

> **Convenção:** cada achado tem uma checkbox `- [ ]` (para marcar como resolvido), severidade, localização exata (`arquivo:linha`) e uma explicação do impacto real — não é opinião de estilo, é "isso pode doer assim". Itens resolvidos são removidos deste documento (não apenas marcados), para o arquivo continuar refletindo só o trabalho pendente.

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

- Cada achado é uma **checkbox independente** — marque `[x]` e remova o item conforme for corrigindo, e este arquivo vira o tracker da refatoração.
- Severidade: 🔴 **Crítico** (bug real / risco de perda de dado ou segurança ativa) · 🟠 **Alto** (defeito funcional/de design com impacto real) · 🟡 **Médio** (dívida técnica que vai doer em manutenção) · 🔵 **Baixo** (estilo/limpeza, baixo risco).
- Dentro de cada área, os achados estão nas 5 categorias que vocês pediram: **Vulnerabilidades**, **Acoplamento excessivo**, **Baixa coesão**, **Clareza/código confuso**, **Comentários desnecessários** — mais uma seção "Outros" quando o achado é um bug funcional puro (não se encaixa nas 5 categorias, mas é grave demais pra omitir).
- Comece pela seção 3 (plano de ataque) — ela já ordena os itens mais importantes por impacto real, não por ordem de leitura do código.

---

## 2. Resumo executivo

O código está organizado em uma arquitetura em camadas coerente (controller → service → repository) e, na maior parte do domínio fiscal/financeiro, usa `BigDecimal` corretamente e tem comentários que explicam o *porquê* das decisões — acima da média para um projeto deste tamanho. O problema não é falta de estrutura; é que **381 arquivos e ~15 integrações externas cresceram sem um segundo revisor consistente**, e isso deixou rachaduras específicas e localizadas, não uma bagunça generalizada.

**Contagem de achados em aberto: 4** (itens já resolvidos foram removidos deste documento), sendo:

| Severidade | Qtde. em aberto | Onde estão |
|---|---|---|
| 🔴 Crítico | **0** | — |
| 🟠 Alto | **1** | Diretório de tokens OAuth do Google divergente entre dois fluxos — C-O3 |
| 🟡 Médio | **3** | Acoplamento aceito por design em cancelamento de agrupamento (C-A4); paginação de 4 telas que dependem da lista completa no cliente (D-P1); `ddl-auto` ainda não travado em `validate` (E-C1) |
| 🔵 Baixo | **0** | — |

### Achados críticos

Nenhum em aberto. Dos 8 achados críticos originais, todos os 8 já foram corrigidos e removidos deste documento (bypass de rate limit por IP forjável, bug de zeragem no relatório de apuração de ICMS, no-op na limpeza de tokens OAuth, `DELETE /clients/{id}` sem proteção, `AuthController` acessando repository diretamente, cascade delete de histórico de compras, `FreightConfig` em `double`, e por último `ddl-auto=update` sem nenhuma ferramenta de migração — Flyway foi introduzido e rebaixou o item para 🟡, ver [E-C1](#e-c1)).

Nenhuma das 5 frentes de análise encontrou blocos relevantes de **código morto comentado** ou `TODO`/`FIXME` esquecidos — ao contrário do que a preocupação inicial sugeria, "comentários desnecessários" é a categoria com **menos** achados no projeto inteiro (a maioria dos comentários existentes explica *por quê*, não *o quê*). O problema real de qualidade está concentrado em **acoplamento entre domínios** (services de um módulo mexendo direto no repository de outro) e em **bugs silenciosos** que não geram exceção — só dado errado.

---

## 3. Plano de ataque recomendado

Ordem sugerida, misturando "baixo custo/alto impacto" primeiro com os itens que bloqueiam outros. Itens já resolvidos (Onda 1 inteira, a Onda 2 inteira e a Onda 3 inteira) foram removidos deste documento — o que resta:

### Onda 2 — concluída
Flyway introduzido, os 11 scripts de `static/*.sql` versionados em `db/migration/V1..V11` com baseline seguro para `hml`/`prod`. Falta só travar `ddl-auto=validate` (ver [E-C1](#e-c1)) — não feito nesta rodada por decisão explícita: exige confirmar com acesso real ao banco de `hml` (depois `prod`) que o schema bate com as entidades antes de travar, senão o app pode não subir no próximo deploy.

### Onda 4 — Débito técnico contínuo (backlog, sem urgência)
Os itens 🟡 restantes: acoplamento aceito por design em `CombinedScoreCancellationService` (C-A4) e paginação das telas com "selecionar todos os filtrados" (D-P1) — ambos exigem redesenho de fluxo, não troca mecânica.

---

## Área A — Autenticação, Segurança e mTLS

**Escopo analisado:** `config/auth/**`, `config/bb/**`, `config/billet/**`, `config/ssl/**`, `config/storage/**`, inicializadores (`UserInitializer`, `FiscalProductInitializer`, `Base64FileDecoder`, `WebClientConfig`, `SwaggerConfig`), `exception/**`, `service/user/**`, `controller/user/**`, `application*.properties`, `.env.example`, `pom.xml`.

> Observação geral desta frente: o padrão de comentários deste módulo é notavelmente bom — explica *por que* uma decisão foi tomada, não *o que* o código já diz. Nenhum código morto comentado ou `TODO` foi encontrado.

### A · Vulnerabilidades

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### A · Acoplamento e baixa coesão

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### A · Clareza / código confuso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### A · Comentários desnecessários

Nenhum achado. Busca por `TODO|FIXME|XXX` e por blocos de código comentado não retornou ocorrências no escopo.

---

## Área B — Invoice (NF-e) / Billet (Boleto Sicoob) / Finance (Extratos BB/Sicoob)

**Escopo analisado:** `service/invoice/**` (incl. `factory/`, `tax/`), `service/billet/**`, `service/finance/**` (incl. `bb/`, `sicoob/`, `transaction/`), `config/FocusNfeApiClient.java`, `config/bb/**`, `config/billet/**`, `model/billet|invoice|finance/**` (~65 arquivos, ~9.200 linhas).

> A maior parte do código lido usa `BigDecimal` corretamente para dinheiro e tem comentários explicando regras da Sefaz/decisões de concorrência — acima da média para este domínio. O risco real está em bugs de cálculo silenciosos e duplicação de parsing frágil.

### B · Clareza / código confuso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### B · Baixa coesão / duplicação de lógica

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### B · Acoplamento excessivo

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### B · Outros (moeda, idempotência, retry/timeout)

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### B · Comentários desnecessários

Nenhum bloco relevante de código morto/comentado encontrado. Comentários de legenda em `IcmsPdfGenerator.java:156-206` repetem o rótulo da coluna em prosa — baixo custo, artefato de UX do PDF mais do que ruído.

---

## Área C — Demais Services

**Escopo analisado:** `service/purchase/**`, `service/notification/**` (email/whatsapp), `service/backup/**` (auth/folders/oauth), `service/climate/**`, `service/freight/**`, `service/chatbot/**`, `service/realtime/**`, `service/scheduler/**`, `service/storage/**`, `service/user/**`, `service/product/**`, `service/dashboard/**`, configs correspondentes, `tools/`, `util/` (~80 arquivos, ~8.900 linhas).

### C · Outros (bugs funcionais graves, fora das 5 categorias mas críticos demais para omitir)

- [ ] 🟠 **[C-O3] Dois mecanismos divergentes de diretório de tokens OAuth do Google**
  **Local:** `service/backup/oauth/AuthorizationFlowFactory.java:25` hardcoda `"temp/google/tokens"`, enquanto `CredentialManager`/`GoogleAuthService` (`service/backup/auth/GoogleAuthService.java:25-26`) usam a property `google.tokens.directory`. Se divergirem, token gravado por um fluxo não é encontrado pelo outro.

### C · Acoplamento excessivo

- [ ] 🟡 **[C-A4] `CombinedScoreCancellationService` depende de `InvoiceService` e `BilletService` além do próprio `CombinedScoreService`**
  **Local:** `service/purchase/CombinedScoreCancellationService.java:41-47`. Bem documentado (evita dependência circular), mas o cancelamento de um agrupamento já conhece 3 domínios — cresce a cada novo domínio que precisar ser cancelado em cascata. *Revisado: mantido como está — o Javadoc da classe já explica que ela existe fora de `CombinedScoreService` justamente para evitar a dependência circular (`InvoiceService`/`BilletService` já dependem dele); não há correção de baixo risco disponível sem reintroduzir esse ciclo.*

### C · Clareza / código confuso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos (`[C-C1]`: detecção do cabeçalho do PDF agora tolera variação de maiúsculas/minúsculas, e o `catch (Exception e)` genérico de `PurchaseProcessingService.processPurchaseFile` agora usa `PurchaseException(message, cause, unexpected=true)`, que loga com stacktrace completo — diferenciando, no log, um bug real do parser de uma falha de validação esperada, como layout de PDF não reconhecido. O parsing continua assumindo colunas fixas por natureza do problema — não há como tolerar layout arbitrário sem amostras reais de formatos alternativos do fornecedor).

### C · Duplicação de código

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### C · Tratamento de erro genérico / catch silencioso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### C · Comentários / documentação

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos. Nenhum bloco relevante de código morto comentado foi encontrado.

### C · Outros (infra/operação)

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

---

## Área D — Controllers, DTOs e Mappers

**Escopo analisado:** `controller/**` (23 controllers, todas as subpastas), `dto/**` (~90 DTOs), `mapper/**` (9 mappers).

### D · Violações diretas de regras documentadas no README (categoria "Vulnerabilidades"/arquitetura)

O README documenta 3 regras: *"Controllers nunca acessam repository diretamente"*, *"Endpoints sensíveis exigem `@PreAuthorize(\"hasRole('MANAGER')\")`"*, *"DTOs via MapStruct — entidades JPA não são expostas diretamente"*.

Nenhum achado em aberto — `[D-V3]` resolvido: `PurchaseMapper` (que já existia, mas nunca era injetado) agora é usado nos 5 pontos que antes montavam `PurchaseResponse` manualmente (`PurchaseController.createManualPurchase`, `PurchaseService.getPurchasesByClientOrdered`/`getPurchasesByDateRange` (2 sobrecargas) e `CapturaNotaPendenteService.confirmarComoCompra`) — mudança futura em `PurchaseResponse` agora só precisa tocar o mapper.

### D · Qualidade da camada de Controller

Tratamento de exceção duplicado, falta de validação de entrada (Bean Validation) e inconsistência de padrão REST — todos os achados dessas 3 subcategorias já foram corrigidos.

**Falta de paginação:**

- [ ] 🟡 **[D-P1] Listagens sem paginação apesar do padrão `Page`/`Pageable` já existir em outros endpoints do projeto** *(parcialmente resolvido)*
  **Restam em aberto:** `controller/billet/BilletController.java` (`GET /billet/open`), `controller/invoice/InvoiceController.java` (`GET /invoices/open`), `controller/purchase/ClientController.java` (`GET /clients`), `controller/purchase/CombinedScoreController.java` (`GET /combined-scores/last-per-client`) — confirmado via mapeamento do `Codigo/Front`: todos alimentam telas que dependem da lista completa no cliente (busca + "selecionar todos os filtrados" para ações em massa nas abas de Boletos/NF sem boleto; join em memória entre `/clients` e `/combined-scores/last-per-client` na tela de Clientes). Paginar de verdade exige redesenhar esses fluxos (busca/seleção assíncrona por página, ou embutir a "última compra" na própria resposta paginada de `/clients` em vez de dois fetches separados) — não é troca mecânica de tipo de retorno, por isso ficou fora desta rodada.
  **Resolvido:** `GET /clients/with-last-purchase` (`ClientController`) e `GET /statements` (`StatementController`) agora retornam `Page<T>` — confirmado sem nenhum consumidor no `Codigo/Front` hoje, conversão sem risco de quebrar tela nenhuma.

Nenhum achado em aberto para **[D-P2]** — `GET /products` (sem paginação) e `GET /products/paginated` coexistiam sem necessidade; unificados em um único `GET /products` paginado (nenhum dos dois tinha consumidor no frontend, então a fusão não quebra nada).

Lógica de parsing/mapeamento manual dentro do controller — todos os achados desta subcategoria já foram corrigidos (o suposto bug de `NumberFormatException`/`NotificationChannel.valueOf` caindo em 500 já não existia — o handler genérico de `IllegalArgumentException` já cobria os dois casos com 400; só a mensagem ficou mais clara. O padrão de magic string decidindo código HTTP por conteúdo de exceção também não existe mais no controller — o código mudou desde a auditoria original).

### D · Acoplamento e baixa coesão em DTOs/Mappers

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos.

### D · Clareza / código confuso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos (o Javadoc incompleto de `BilletController.generateBillet` foi completado; o suposto `throws IOException` morto não existia mais — o método não tem try/catch, o `throws` é legítimo porque propaga uma exceção checked real do service).

### D · Comentários desnecessários / código morto

Poucos achados — nenhum bloco de código comentado ou `TODO`/`FIXME` encontrado. A maioria dos Javadocs lidos explica decisões não óbvias.

### D · Documentação desatualizada (fora do escopo estrito, mas relevante)

Nenhum achado em aberto — `controller/chatbot/README.md` e `controller/scheduler/README.md` (que descreviam controllers deletados) foram removidos.

---

## Área E — Models, Repositories e Configuração/Build

**Escopo analisado:** `model/**` (todas subpastas), `repository/**` (todas subpastas), `HortifrutiSlApplication.java`, `application*.properties`, `products.yml`, `pom.xml`.

### E · Design de Entidades JPA

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos (`@Basic(LAZY)` nos `@Lob` legados, `@Data`→`@Getter/@Setter/@Builder` em `ClimateProduct`, `precision`/`scale` em `CombinedScore.totalValue`/`Purchase.total`, Bean Validation adicionada em todo `model/**`, `ourNumber_sicoob`→`ourNumberSicoob`, timestamps de auditoria em `GroupedProduct`/`CombinedScore`, `Category.FAMÍLIA`→`FAMILIA`).

### E · Acoplamento excessivo

Nenhum achado em aberto — `[E-R1]` resolvido: as 6 queries de `CombinedScoreRepository` que embutiam `com.hortifruti.sl.hortifruti.model.purchase.Status.PENDENTE` como literal de string (`findAllPendingWithBilletByClient`, `findAllPendingByClient`, `findOverdueUnpaidScoresByClient`, `findAllOpenBillets`, `findOverduePendingScores`, `findAllOpenInvoiceOnly`) agora recebem `Status` como parâmetro `@Param` (JPQL passa a usar `:status`); os 4 call sites usados (`CombinedScoreService`) passam `Status.PENDENTE` como referência Java normal, checada em tempo de compilação — renomear/mover `Status` agora quebra o build em vez de falhar silenciosamente em runtime.

Demais achados desta categoria já foram corrigidos: `TransactionRepository` (E-R2) agora compõe `Specification`s combináveis via `TransactionSpecifications` em vez de multiplicar métodos `@Query` — os 3 que não tinham nenhum caller (`...AndStatementOrigin`, `...AndTransactionType`, `...AndCategory`) e o `existsByHash` morto foram removidos; `findAllCategories` (E-R3) agora retorna `List<Category>` (a conversão pra `List<String>` do contrato da API fica no service, não no repositório); `PurchaseRepository.sumTotalGroupedByClientId` (E-R4) retorna `List<ClientPurchaseTotal>` (record via constructor expression) em vez de `List<Object[]>`.

### E · Baixa coesão

Nenhum achado em aberto — `repository/climate/ProductRepository` (E-B1) foi renomeado para `ClimateProductRepository`, removendo a colisão conceitual com `FiscalProductRepository`/`GroupedProductRepository`/`InvoiceProductRepository`.

### E · Clareza / código confuso

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos (`model/purchase/README.md` não menciona mais `totalPurchaseValue`; `model/chatbot/README.md` e `repository/chatbot/README.md` removidos, já que descreviam código que nunca existia nas pastas; typo `update_at`→`updated_at` corrigido nas 4 entidades, com migração de dados renomeando a coluna sem perder os valores já gravados; `NotificationType`, que se confirmou 100% morto — zero referências fora do próprio arquivo — foi removido, restando só `NotificationChannel`, que já era o único de fato usado).

### E · Comentários desnecessários / código morto

Nenhum achado em aberto — `[E-CM1]` resolvido: a linha `#debug=true` comentada e sem efeito foi removida de `application.properties`. Nenhum outro bloco de código morto comentado encontrado em `model/**`/`repository/**`.

### E · Configuração / Build

<a id="e-c1"></a>
- [ ] 🟡 **[E-C1] `ddl-auto=update` continua ativo (não `validate`) — Flyway já introduzido** *(parcialmente resolvido, rebaixado de 🔴 para 🟡)*
  Flyway foi adicionado (`org.flywaydb:flyway-core`/`flyway-mysql` no `pom.xml`) e os 11 scripts de `static/*.sql` foram versionados em `src/main/resources/db/migration/V1__...` a `V11__...` (mesmo conteúdo, com guarda adicional por `INFORMATION_SCHEMA.TABLES` para funcionar também num banco novo — Flyway roda antes do Hibernate criar as tabelas das entidades). Configurado com `spring.flyway.baseline-on-migrate=true`/`baseline-version=11`: em `hml`/`prod` (bancos já existentes) o Flyway só marca a versão 11 como baseline, sem reexecutar nada — testado com um MySQL descartável simulando os dois cenários (banco vazio e banco com schema legado) antes de aplicar; ambos passaram sem erro. Mudanças de schema futuras agora entram como `V12__...` em diante, versionadas e com histórico (`flyway_schema_history`).
  **Resta em aberto:** `spring.jpa.hibernate.ddl-auto` continua `update` nos 3 ambientes — não foi travado para `validate`. Travar exige confirmar, com acesso real ao banco de `hml`/`prod`, que o schema batia exatamente com as entidades no momento da troca; sem isso o risco é a aplicação não subir no próximo deploy. Ficou de fora desta rodada por decisão explícita (ver discussão no PR) — plano: trocar `hml` para `validate` primeiro, confirmar boot limpo, só então `prod`.

---

## 9. Achados duplicados entre áreas

Nenhum achado em aberto — o único item duplicado (`Category.FAMÍLIA`, reportado em B-B5 e E-J8) foi resolvido: o valor do enum foi renomeado para `FAMILIA` (o rótulo de exibição acentuado "Família" continua em `TransactionCategoryClassifier#categoryLabel`).

---

## 10. O que já está bom (não mexer)

Para não perder de vista em meio aos achados — pontos que as 5 frentes de análise destacaram positivamente e que **não devem ser "refatorados" por engano**:

- Cookies de autenticação corretamente configurados por ambiente (`Secure=true`/`SameSite=None` em prod/hml, `false`/`Lax` em local).
- Nenhum segredo hardcoded no código — tudo via `.env`/variáveis de ambiente.
- CORS com origens explícitas (sem wildcard), JWT com algoritmo/issuer fixos e verificação de assinatura correta.
- `BigDecimal` usado corretamente para dinheiro na maior parte do domínio fiscal/financeiro (exceções pontuais já listadas acima).
- Detecção de reuso de refresh token revogado (deteção de vazamento) já implementada corretamente em `RefreshTokenService`.
- Lockout progressivo por conta com auditoria de tentativas (`LoginAuditLog`) — a lógica em si está correta (o problema do lockout por IP ser contornável via spoofing de `X-Forwarded-For` já foi corrigido).
- Retry com invalidação de token em `BilletHttpClient`/`BBExtratoClient` — padrão bem implementado, só não replicado para a Focus NFe (B-O2).
- Padrão de comentários explicando "por quê" (não "o quê") é consistente na maior parte do código — a categoria "comentários desnecessários" foi, de longe, a com menos achados nas 5 frentes de análise.
