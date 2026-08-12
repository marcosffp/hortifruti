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

**Contagem de achados em aberto: ~52** (itens já resolvidos foram removidos deste documento), sendo:

| Severidade | Qtde. em aberto | Onde estão os mais graves |
|---|---|---|
| 🔴 Crítico | **1** | Config/Build (schema sem migração versionada) |
| 🟠 Alto | **7** | Mapper morto, parsing de endereço duplicado, acoplamento cruzado remanescente |
| 🟡 Médio | **21** | Duplicação de lógica entre integrações parecidas, god classes, paginação de telas com UX dependente da lista completa |
| 🔵 Baixo | **23** | Nomenclatura inconsistente, código morto isolado, metadados de build |

### O achado crítico remanescente

Dos 8 achados críticos originais, 7 já foram corrigidos e removidos deste documento (bypass de rate limit por IP forjável, bug de zeragem no relatório de apuração de ICMS, no-op na limpeza de tokens OAuth, `DELETE /clients/{id}` sem proteção, `AuthController` acessando repository diretamente, cascade delete de histórico de compras, `FreightConfig` em `double`). Resta em aberto:

1. **Config/Build:** `ddl-auto=update` está ativo em produção, sem Flyway/Liquibase — o schema do banco é alterado automaticamente a cada deploy, sem histórico de migração nem rollback. ([Área E, item E-C1](#e-c1))

Nenhuma das 5 frentes de análise encontrou blocos relevantes de **código morto comentado** ou `TODO`/`FIXME` esquecidos — ao contrário do que a preocupação inicial sugeria, "comentários desnecessários" é a categoria com **menos** achados no projeto inteiro (a maioria dos comentários existentes explica *por quê*, não *o quê*). O problema real de qualidade está concentrado em **acoplamento entre domínios** (services de um módulo mexendo direto no repository de outro) e em **bugs silenciosos** que não geram exceção — só dado errado.

---

## 3. Plano de ataque recomendado

Ordem sugerida, misturando "baixo custo/alto impacto" primeiro com os itens que bloqueiam outros. Itens já resolvidos (Onda 1 inteira, a maior parte da Onda 2 e a Onda 3 inteira) foram removidos deste documento — o que resta:

### Onda 2 — Estrutural, mas isolado por módulo (1 a 2 semanas)
- [ ] **E-C1** — Introduzir Flyway (ou Liquibase), congelar `ddl-auto=validate` em produção, versionar os `.sql` avulsos que já existem em `static/`. *(Não aplicado nesta rodada — exige acesso ao schema real de hml/prod para gerar uma baseline confiável; ver observação abaixo.)*

### Onda 4 — Débito técnico contínuo (backlog, sem urgência)
Tudo marcado 🟡/🔵 nas seções abaixo: duplicação entre provedores de e-mail, god classes, nomenclatura inconsistente, paginação das telas com "selecionar todos" (D-P1 restante).

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

- [ ] 🔵 **[C-C10] `InvoiceProductService.deleteInvoiceProduct` faz `existsById` seguido de `findById(...).get()`**
  **Local:** `service/purchase/InvoiceProductService.java:37-43` — race condition teórica, uso desnecessário de `.get()`. Poderia ser um único `findById(...).orElseThrow(...)`.

- [ ] 🔵 **[C-C11] `GroupedProductService` usa `catch (Exception e)` genérico ao redor de agrupamento em memória de listas já validadas**
  **Local:** `service/purchase/GroupedProductService.java:33-45,53-65` — embolsa qualquer `RuntimeException` (incluindo bugs) e reembrulha como `PurchaseException`, perdendo o stacktrace real.

### C · Duplicação de código

- [ ] 🟡 **[C-D1] Três provedores de e-mail duplicam o método `addInlineLogo` quase identicamente**
  **Local:** `service/notification/email/SendGridEmailSender.java:98-119`, `GmailSmtpEmailSender.java:143-152`, `GmailApiEmailSender.java:171-180`. Correção de bug nessa lógica precisa ser replicada em 3 lugares.

- [ ] 🔵 **[C-D2] Montagem de contexto de mensagem (`Map<String,String> variables`) repetida quase palavra-por-palavra em 3 services**
  **Local:** `service/notification/NotificationService.java:222-237` vs. `BulkNotificationService.java:255-272` vs. `service/scheduler/DatabaseStorageAlertService.java`.

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

- [ ] 🟠 **[D-V3] `PurchaseMapper` nunca é usado — `Purchase` (entidade JPA) atravessa service→controller e é mapeada manualmente em 5 lugares**
  **Local:** `mapper/PurchaseMapper.java` (morto, zero referências); repetido manualmente em `controller/purchase/PurchaseController.java:44-52`, `service/purchase/PurchaseService.java:~193,~263,~284`, `service/purchase/CapturaNotaPendenteService.java:124`.
  ```java
  Purchase purchase = purchaseService.createManualPurchase(request);
  return ResponseEntity.ok(new PurchaseResponse(purchase.getId(), purchase.getPurchaseDate(), purchase.getTotal(), purchase.getUpdatedAt()));
  ```
  Mudança futura em `PurchaseResponse` exige tocar em 5 arquivos manualmente. Usar o mapper já existente e hoje inerte.

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

Nenhum achado em aberto — todos os itens desta categoria já foram corrigidos (`model/purchase/README.md` não menciona mais `totalPurchaseValue`; `model/chatbot/README.md` e `repository/chatbot/README.md` removidos, já que descreviam código que nunca existia nas pastas; typo `update_at`→`updated_at` corrigido nas 4 entidades, com migração de dados renomeando a coluna sem perder os valores já gravados; `NotificationType`, que se confirmou 100% morto — zero referências fora do próprio arquivo — foi removido, restando só `NotificationChannel`, que já era o único de fato usado).

### E · Comentários desnecessários / código morto

- [ ] 🔵 **[E-CM1] Linha de configuração comentada residual**
  **Local:** `application.properties:270` — `#debug=true`. Sem efeito, ruído a remover.

*(Nenhum outro bloco de código morto comentado encontrado em `model/**`/`repository/**`.)*

### E · Configuração / Build

<a id="e-c1"></a>
- [ ] 🔴 **[E-C1] `ddl-auto=update` em produção, sem Flyway/Liquibase**
  **Local:** `application-local.properties:9`, `application-hml.properties:11`, `application-prod.properties:12` (os 3 ambientes) — `spring.jpa.hibernate.ddl-auto=update`. `pom.xml` não tem Flyway nem Liquibase. O Hibernate altera o schema de produção automaticamente a cada deploy, por inferência das entidades — sem histórico de migração versionado, sem plano de rollback. Reforçado por scripts SQL manuais em `static/*.sql` (`billet_files_migration.sql`, `fiscal_note_xml_storage_migration.sql`, etc.) que existem no repositório mas não são aplicados automaticamente por nenhuma ferramenta — dependem de alguém lembrar de rodá-los manualmente, facilmente divergindo entre `hml` e `prod`.

- [ ] 🟡 **[E-C3] `spring-boot-starter-web` e `spring-boot-starter-webflux` juntos no mesmo projeto**
  **Local:** `pom.xml:67-78` — duas pilhas HTTP concorrentes (Tomcat + Reactor Netty). Se o motivo é só `WebClient` reativo para chamar APIs externas, dá para obter isso com dependência mais enxuta, ou migrar para `RestClient` síncrono (Spring Framework 6.1+) e remover o starter reativo.

- [ ] 🔵 **[E-C5] Metadados de projeto vazios (boilerplate do Spring Initializr nunca preenchido)**
  **Local:** `pom.xml:18-31` — `<url/>`, `<licenses><license/></licenses>`, `<developers><developer/></developers>`, `<scm>...</scm>` vazios. Não bloqueia build, é ruído.

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
