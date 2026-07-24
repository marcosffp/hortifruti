# Correções recomendadas — Backend (Hortifruti SL)

> Documento gerado a partir de uma auditoria completa de `Codigo/Back/src/main/java/...`.
> **Nenhum código foi alterado.** Isto é um mapa de problemas + plano de correção, para ser
> executado aos poucos, sem quebrar o que já funciona. Cada item indica arquivo:linha, o
> problema e a correção sugerida.

## Como usar este documento

- Trate cada seção como um cartão de tarefa independente. Dá pra corrigir um item por vez,
  rodar os testes/subir local e seguir pro próximo — não precisa (nem deve) fazer tudo de uma vez.
- A ordem das seções já é a ordem de prioridade sugerida: segurança primeiro, depois as coisas
  que mais custam tempo/tokens no dia a dia (arquivo morto, duplicação, god classes), e por
  último inconsistências de estilo que são baratas de ignorar por enquanto.
- "Correção sugerida" descreve a direção, não um passo a passo rígido — adapte durante a implementação.

---

## 0. Resumo executivo

O ponto central da "bagunça" não é volume de código (só ~23.6k linhas, 313 arquivos — é um
projeto de porte médio) e sim **inconsistência**: cada integração/domínio (Sicoob, BB, Focus NFe,
Google Drive, Chatbot) foi implementada com um padrão diferente de tratamento de erro, logging,
injeção de dependência e nomenclatura. Isso obriga quem for mexer numa feature nova a reler o
arquivo inteiro para descobrir "qual é o padrão aqui", em vez de reconhecer um padrão já visto.

Os achados mais graves (ver detalhes abaixo):

1. **Segredo de webhook do chatbot hardcoded no `application.properties`** (não é variável de ambiente) — item de segurança real, não só estilo.
2. **Credenciais fracas (`root`/`root`, `admin`/`admin`) para bootstrap de usuários** — risco se o profile `local` rodar por engano contra um banco real.
3. **`GoogleDriveService.java` (445 linhas) é código morto** — já foi substituído por `service/backup/auth/*` e `service/backup/folders/*`, mas ninguém apagou o arquivo antigo. Isso é o exemplo perfeito de "gastar tempo entendendo um arquivo que nem é usado".
4. **Um DTO duplicado e morto** (`exception/CombinedScoreRequest.java`) com o mesmo nome de outro DTO real (`dto/purchase/CombinedScoreRequest.java`), só que no pacote errado.
5. Vários "PDF generators" e configs de SSL/HTTP client **copiados e colados** entre bancos/integrações em vez de terem uma base compartilhada.
6. `GlobalExceptionHandler` **não loga nada** em nenhum dos 19 handlers — erros em produção desaparecem silenciosamente.

Corrigir os itens 1–6 já reduz boa parte da confusão sem exigir refatoração grande.

---

## 1. Segurança (prioridade máxima — revisar antes de qualquer outra coisa)

### 1.1 Segredo do webhook do chatbot commitado em texto puro
- **Onde:** `src/main/resources/application.properties:168`
- **Problema:** `chatbot.webhook.secret=CHATBOT_WEBHOOK_SECRET` — compare com o resto do arquivo,
  que usa `${VAR_NAME}` para puxar de variável de ambiente. Aqui o valor literal
  `"CHATBOT_WEBHOOK_SECRET"` é o segredo de verdade usado em runtime, e está no Git.
- **Consumido em:** `controller/notification/ChatbotController.java:38` via `@Value("${chatbot.webhook.secret}")`.
- **Por que importa:** o endpoint `/chatbot/webhook` é `permitAll` em `config/auth/SecurityConfig.java:54`. Qualquer pessoa com acesso ao repositório (ou só ao nome da env var no `.env.example`) pode forjar chamadas autenticadas ao webhook.
- **Correção sugerida:** trocar para `chatbot.webhook.secret=${CHATBOT_WEBHOOK_SECRET}`, gerar um valor novo (aleatório) para a env var em todos os ambientes, e revogar/rotacionar o valor atual já vazado no histórico do Git.

### 1.2 Usuários de bootstrap com senha previsível
- **Onde:** `config/UserInitializer.java:80-82`
- **Problema:** no profile `local`, o initializer cria `root/root` (role `MANAGER`) e `admin/admin` (role `EMPLOYEE`).
- **Risco:** se `SPRING_PROFILES_ACTIVE` for configurado errado (ex.: `local` indo parar num ambiente com banco real), isso cria uma conta `MANAGER` com senha `root`.
- **Reforço do mesmo padrão:** `.env.example:34-35` também usa `LOCAL_MYSQLUSER=root` / `LOCAL_MYSQLPASSWORD=root`.
- **Correção sugerida:** gerar senha aleatória no primeiro boot e logar/gravar em local seguro (ou exigir env var própria só para o profile local), e adicionar uma guarda explícita que recuse rodar esse initializer fora do profile `local`/`test` (ex.: `@Profile("local")` já deve existir — confirmar que cobre também `hml`/`prod` corretamente).

### 1.3 Swagger/OpenAPI público em produção
- **Onde:** `config/auth/SecurityConfig.java:50-51`, sem nenhuma restrição em `application-prod.properties`.
- **Problema:** `/swagger-ui/**` e `/v3/api-docs/**` são `permitAll` incondicionalmente, inclusive no profile `prod`. O schema completo da API (endpoints internos, shape de payloads) fica navegável publicamente.
- **Correção sugerida:** desabilitar springdoc no profile `prod` (`springdoc.api-docs.enabled=false` / `springdoc.swagger-ui.enabled=false`) ou colocar atrás de autenticação.

### 1.4 Validação de token do scheduler duplicada e frágil
- **Onde:** `config/auth/SecurityFilter.java:37-38,66` (lê `${api.token.scheduler}` e faz um "fast path" sem branch de rejeição) e, de forma independente, `service/scheduler/ApiTokenService.java` / `controller/SchedulerController.java` (revalida o mesmo token).
- **Problema:** a real proteção está só no controller, porque `/scheduler/**` é `permitAll` e o filtro não rejeita. Se algum dia alguém assumir que o filtro já protege e remover a checagem do controller, os endpoints `/scheduler/check-overdue` e `/scheduler/check-database-storage` ficam abertos.
- **Correção sugerida:** consolidar a validação em um único lugar (idealmente o filtro, com branch explícito de rejeição), documentando com um comentário curto por que a rota é `permitAll` (a proteção acontece em outra camada).

### 1.5 Regras de autorização como lista manual gigante e implícita
- **Onde:** `config/auth/SecurityConfig.java:46-67`.
- **Problema:** cadeia de `requestMatchers(...).hasRole(...)` misturando rotas públicas, por papel e o catch-all `anyRequest().authenticated()`. Endpoint novo, sem matcher explícito, cai silenciosamente em "qualquer usuário autenticado" — inclusive quando deveria ser restrito a `MANAGER`. Já existe inconsistência real: `/products/**` exige `MANAGER`, mas `/clients/**` GET só exige `EMPLOYEE`/`MANAGER`, sem documentação do critério.
- **Correção sugerida:** documentar (comentário curto) a regra de decisão por domínio, e considerar mover a decisão de papel para `@PreAuthorize` nos controllers (mais perto do código que ela protege) em vez de só na lista central — ou pelo menos garantir que todo controller novo tenha teste de segurança cobrindo o cenário "sem papel adequado".

### 1.6 API key do OpenWeather pode vazar em log
- **Onde:** `config/climate/OpenWeatherClient.java:42-51` (key concatenada na URL) e `:69-77` (`logger.error("Erro detalhado: {}", e.getMessage())`).
- **Problema:** se a exceção do `RestTemplate` incluir a URL na mensagem (comum em `RestClientException`), a API key vaza para os logs da aplicação.
- **Correção sugerida:** passar a API key como parâmetro/header separado quando o provider permitir, e sanitizar a mensagem de erro antes de logar (remover query string).

### 1.7 Controllers sem `@PreAuthorize` onde deveriam ter
- **Onde:** `PurchaseController` e `InvoiceProductController` não têm `@PreAuthorize` em endpoints de mutação, enquanto domínios equivalentes (`ProductController`, `TransactionController`, `ClientController`) restringem a `MANAGER`.
- **Correção sugerida:** revisar caso a caso se a ausência é intencional; se não for, alinhar com o padrão dos domínios irmãos.

---

## 2. Código morto — a maior fonte de confusão "custa tempo/tokens"

### 2.1 `GoogleDriveService.java` inteiro é código morto
- **Onde:** `service/backup/GoogleDriveService.java` (445 linhas).
- **Problema:** é a versão pré-refatoração da integração com Google Drive. Já foi dividida em
  `service/backup/auth/*` (`GoogleAuthService`, `CredentialManager`, `TokenValidator`, `TokenExceptionHandler`)
  e `service/backup/folders/*` (`GoogleFolderService`, `FolderManager`, `FileUploader`).
  `BackupService.java` só depende das classes novas — nada no projeto referencia mais `GoogleDriveService`.
- **Por que importa:** é o segundo maior arquivo do backend e ninguém deveria gastar tempo lendo-o. É uma armadilha: alguém pode editar o arquivo errado achando que está corrigindo a integração ativa.
- **Correção sugerida:** confirmar (grep + build) que não há nenhuma referência e apagar o arquivo.

### 2.2 DTO duplicado no pacote errado
- **Onde:** `exception/CombinedScoreRequest.java` — mesmo nome de classe que `dto/purchase/CombinedScoreRequest.java`, mas dentro do pacote `exception/`, sem uso.
- **Correção sugerida:** confirmar que não é referenciado e apagar. Se algum import hoje resolve para o arquivo errado por acidente, corrigir o import antes de remover.

### 2.3 Outros trechos mortos/placeholder confirmados
- `service/invoice/IssueInvoice.java:33-37` — campos `@Value` `focusNfeToken`/`focusNfeApiUrl` injetados e nunca usados na classe (aparenta copy-paste de `InvoicePayload`).
- `config/auth/TokenConfiguration.java:82-97` — `getRoleFromToken(String token)` sem nenhum chamador (confirmado via grep no repositório inteiro); o papel do usuário na prática vem de `UserDetails`/banco via `SecurityFilter.loadByUserName`, não do claim do JWT. Remover para não confundir sobre qual mecanismo autoriza de fato.
- `service/scheduler/CombinedScoreSchedulerService.java` — nome sugere agendamento (`@Scheduled`), mas não há nenhum `@Scheduled` na classe (confirmado via grep). Renomear ou implementar o agendamento que falta, o que fizer sentido.
- Vários `System.out.println`/debug prints deixados em código de produção (lista completa na seção 4.3).
- Várias funcionalidades de `FileGenerationService` geram dados de exemplo obviamente fake e não têm nenhum chamador real — candidatas a remoção após confirmação.

**Ação recomendada:** antes de tocar em qualquer refatoração maior, faça uma passada só de remoção de morto (itens 2.1–2.3). É baixo risco (não muda comportamento) e já corta uma fatia real da confusão.

---

## 3. Duplicação de código (God classes e boilerplate copiado)

### 3.1 Geradores de PDF — duas famílias de copy-paste
**Família A** (desenho low-level via PDFBox, padrão `PageWriter`):
`service/finance/SicoobExtratoPdfGenerator.java:252-282`, `BBExtratoPdfGenerator.java:232-262`,
`TransactionReportPdfGenerator.java:247-272` — os métodos `text()`, `textRightAligned()` (2 overloads)
e `truncate()` são quase idênticos byte a byte nos três arquivos, assim como a lógica de `newPage()`/`ensureSpace()`.

**Família B** (relatórios em tabela):
`service/invoice/tax/icms/IcmsPdfGenerator.java:209-264`, `payment/PaymentPdfGenerator.java:177-235`,
`registerReport/RegisterPdfGenerator.java:167-222` — `addText()`, `drawTableHeader()`, `drawTableRow()`
copiados literalmente (mesma assinatura e corpo) nos três; `sales/SalesPdfGenerator.java:117-179` é uma
variante próxima com colunas diferentes. Constantes de layout (`leftMargin=50`, `tableWidth=500`,
`cellHeight=25`, `yPosition=750`, `bottomMargin=100`) reaparecem redeclaradas em cada gerador.

- **Correção sugerida:** extrair uma classe base (ou utilitário) `PdfReportSupport`/`AbstractPdfGenerator`
  por família, com os métodos de desenho e as constantes de layout compartilhados. Isso também facilita
  criar um novo relatório no futuro (hoje exige copiar um arquivo de 200+ linhas).

### 3.2 Clientes HTTP com SSL/mTLS quase idênticos
- **Onde:** `config/bb/BBSSLConfig.java:46-106` e `config/billet/BilletSSLConfig.java:42-108`.
- **Problema:** mesmo carregamento de `KeyStore`/`SSLContext`/`PoolingHttpClientConnectionManager`
  (mesmo PFX, mesma config TLS 1.2/1.3, mesmo padrão de timeout), duplicado para dois bancos diferentes.
- **Correção sugerida:** extrair uma factory/base comum parametrizada por propriedades (path do PFX, senha),
  usada pelos dois configs.

### 3.3 `BilletHttpClient` com retry copiado 6 vezes
- **Onde:** `config/billet/BilletHttpClient.java:39-183`.
- **Problema:** os 6 métodos públicos (`get`, `post`, `postCancel`, `put`, `delete`, `getWithResponse`)
  repetem quase o mesmo bloco de ~20 linhas (invalidar token → retry → catch `HttpClientErrorException|HttpServerErrorException`
  → embrulhar em `BilletException` → catch genérico). Isso é mais da metade do arquivo.
- **Correção sugerida:** extrair um helper genérico `executeWithRetry(Supplier<T> call)` e reduzir cada
  método público a uma chamada dele.

### 3.4 Lógica de negócio duplicada entre serviços
- Caso especial do cliente **"LLINEA"** implementado de forma independente em dois lugares com
  regras diferentes: `service/purchase/DueDateCalculator.java:58` (regra de vencimento) e
  `service/invoice/IssueInvoice.java:102-105` (texto da nota). Não existe um cadastro único de
  configuração por cliente — cada dev que mexeu nisso recriou a regra do zero.
- `service/DashboardService.java` — o filtro "`CombinedScore` por `confirmedAt` dentro de
  `[startDate, endDate]`" está copiado quase igual três vezes: `getCombinedScoreData` (212-222),
  `getTopSellingProducts` (246-256), `getTopProductsByQuantity` (307-317).
- `zipFolder`/`saveFile`/`compressFolder`/`capitalizeFirstLetter` duplicados quase iguais entre
  `service/finance/MacroExportService.java:158-209` e `service/invoice/tax/ReportTaxService.java:156-189`.
- **Correção sugerida:** criar um cadastro de "regras por cliente" (tabela ou config), um método
  utilitário compartilhado para o filtro de data do dashboard, e uma classe utilitária única de
  zip/arquivo usada pelos dois serviços de export.

### 3.5 God classes / responsabilidades misturadas
- `service/billet/BilletService.java` (378 linhas) — orquestra 6 colaboradores (`BilletFactory`,
  `BilletIssue`, `BilletQuery`, `BilletCancel`, `BilletInfoCombinedAndClient`, `BilletFileStorageService`)
  **e também** injeta `CombinedScoreRepository`/`ClientRepository` direto, além de implementar
  regra de conciliação Sicoob nela mesma (`reconcileClientOpenScores` 131-169, `tryUpdateClosedStatus` 177-207).
- `service/finance/StatementService.java` (349 linhas) — mistura clientes HTTP de bancos, geração
  de PDF/Excel, upload pro storage e dedup de transações tudo na mesma classe.
- `service/invoice/IssueInvoice.java:53-136` — trata casos especiais de cliente, monta payload HTTP,
  chama Focus NFe, atualiza banco e dispara job assíncrono, tudo inline.
- `service/notification/ChatbotService.java` (665 linhas, **o maior arquivo do projeto**) — mistura
  parsing de webhook, máquina de estado da conversa, templating de mensagens e persistência.
  Métodos longos: `handleBilletRequestByDocument` (128 linhas), `handleInvoiceQuery` (100 linhas).
- **Correção sugerida (progressiva, não precisa ser tudo de uma vez):** dividir por responsabilidade
  única — ex. separar "orquestração HTTP externa" de "regra de negócio" de "persistência" — seguindo
  o mesmo padrão que já foi aplicado com sucesso em `service/backup/auth` e `service/backup/folders`
  (prova de que o time já sabe fazer essa divisão bem, só não fez ainda nesses módulos).

### 3.6 Acesso a repositório atravessando domínios
- `BilletService` (pacote billet) injeta `CombinedScoreRepository`/`ClientRepository` direto em vez
  de passar por `CombinedScoreService`/`ClientService`.
- `FiscalNoteXmlStorageService` (pacote invoice) faz o mesmo.
- `SalesCalculator` mistura acesso direto ao repo com uso do service no mesmo método.
- **Correção sugerida:** regra simples para adotar daqui pra frente — um serviço de domínio X só
  acessa o repositório de X; para ler dados de outro domínio, chama o service daquele domínio.

---

## 4. Inconsistências de padrão (tratamento de erro, logging, DI)

### 4.1 `GlobalExceptionHandler` sem logging
- **Onde:** `exception/GlobalExceptionHandler.java` — 19 handlers, nenhum loga nada.
- **Problema:** erros de produção somem sem deixar rastro — quando um usuário reclama de erro 500, não tem log pra investigar.
- **Correção sugerida:** adicionar `log.warn`/`log.error` em cada handler (nível de acordo com a gravidade), incluindo contexto útil (path, tipo de exceção).

### 4.2 Handling de erro duplicado/inconsistente entre controllers
- `BilletController`, `NotificationController` e `BackupController` capturam exceções localmente e
  devolvem `ResponseEntity` de erro ad-hoc, mesmo já existindo handler equivalente no
  `GlobalExceptionHandler` — ou seja, parte do handler global está morta pra esses casos, e o
  formato de erro varia por endpoint.
- Pelo menos 3 formatos diferentes de resposta de sucesso: `String` puro, `Map.of("message", ...)`,
  e DTO tipado.
- **Correção sugerida:** escolher um único formato de erro (ideal: um `ErrorResponse` DTO já usado
  pelo `GlobalExceptionHandler`) e remover os catches locais que reimplementam isso; padronizar
  resposta de sucesso (DTO tipado sempre, sem `Map.of`/`String` solto).

### 4.3 Logging inconsistente (`Slf4j` vs `System.out`/`System.err`)
- Usam `@Slf4j` corretamente: `BilletService`, `BilletCancel`, `IssueInvoiceWithBilletService`, `FiscalNoteXmlStorageService`, `NfSalesCalculator`.
- Usam `System.out.println`/`System.err.println`/`printStackTrace` (ou não logam nada):
  `service/finance/MacroExportService.java:52-204`, `service/invoice/tax/ReportTaxService.java:43-56,113-138`
  (4 catches **vazios**), `service/invoice/tax/sales/SalesCalculator.java:76-77`,
  `service/invoice/InvoiceQuery.java:89,95-96,214-215,221-226`, `service/invoice/DanfeXmlService.java:185-189,204-205,218-219`,
  `service/invoice/IssueInvoice.java:93,98,104,113` (prints de debug tipo `[buildInvoiceRequest] ...`),
  `config/auth/SecurityFilter.java:84` (sem logger nenhum na classe, print em toda falha de autenticação),
  `config/UserInitializer.java:380`, `service/notification/ChatbotService.java` (catch vazio em `findInvoiceRefByNumber`).
- **Correção sugerida:** padronizar em `@Slf4j` + `log.error/warn/info` em todo o projeto; eliminar
  todos os `System.out`/`System.err`/`printStackTrace`; **nunca** deixar catch vazio — no mínimo logar.

### 4.4 Injeção de dependência inconsistente
- Maioria usa `@RequiredArgsConstructor` (construtor com campos `final`).
- `config/Base64FileDecoder.java`, `config/FocusNfeApiClient.java`, `config/climate/OpenWeatherClient.java`
  usam injeção por campo (`@Value` direto, sem construtor, campos mutáveis).
- `config/auth/Auth.java` usa `@AllArgsConstructor` em vez do `@RequiredArgsConstructor` padrão do resto do projeto.
- `FocusNfeApiClient`/`OpenWeatherClient` instanciam `new RestTemplate()` local em vez de injetar os
  beans já configurados (`genericRestTemplate`, `bbRestTemplate`, `billetRestTemplate`), perdendo o
  tuning de timeout/pool feito nesses beans.
- `BBToken`/`SicoobToken` instanciam `new ObjectMapper()` local em vez do bean `@Primary` de `JacksonConfig`.
- **Correção sugerida:** padronizar em construtor (`@RequiredArgsConstructor`, campos `final`) em todo
  o projeto; sempre injetar os beans de `RestTemplate`/`ObjectMapper` já existentes.

### 4.5 `protected` sem motivo (nenhuma classe tem subclasse)
- Confirmado via grep em `service/billet`, `service/invoice`, `service/purchase`, `service/freight`,
  `service/backup`: métodos como `BilletIssue.issueBillet`, `BilletValidation.validateHasBillet`,
  `BilletInfoCombinedAndClient.findClientById`, `FreightService.calculateFreight` são `protected`
  mas nenhuma dessas classes é estendida.
- Isso também tem efeito real, não só estético: **`@Transactional` em métodos `protected`
  chamados dentro da própria classe não é interceptado pelo proxy do Spring** — pode estar
  silenciosamente virando no-op em `PurchaseProcessingService`, `DanfeXmlService`, `InvoiceQuery`, `FreightService`.
- **Correção sugerida:** trocar para `public` (ou `package-private` se for só uso interno ao pacote)
  e revisar caso a caso se o `@Transactional` está de fato sendo aplicado (testar com `TransactionSynchronizationManager` ou logs de commit/rollback).

### 4.6 Nomenclatura de DTO/rota e validação de entrada
- Nem todo request DTO tem `@NotNull`/`@NotBlank` etc.: `dto/user/UserRequest` e a maioria de
  `dto/notification/*` não têm nenhuma anotação de Bean Validation, enquanto `dto/invoice/*` e
  `dto/purchase/client/ClientRequest` estão bem anotados.
- **Correção sugerida:** auditar todo DTO usado como `@RequestBody` e garantir validação mínima de
  campo obrigatório; ativar `@Valid` no controller correspondente se ainda não estiver.

---

## 5. Modelo de dados (JPA) e Repositórios

### 5.1 Tipo de ID inconsistente — ✅ corrigido
- `model/purchase/Client.java:35` e `model/finance/Statement.java:37` foram alterados de `long`
  primitivo para `Long`, alinhando com o resto das entidades (`User`, `Purchase`, `Transaction`,
  `CombinedScore`, `GroupedProduct`, `RefreshToken`, `BilletFile`, `FiscalNoteXmlStorage`,
  `ChatSession`). Nenhum caller quebrou (todos já recebiam o id via `Long`/autoboxing).

### 5.2 Enum duplicado — ✅ corrigido
- Extraído `model/enumeration/FileStatus.java` (`ACTIVE`, `CANCELLED`) e reusado em
  `model/billet/BilletFile.java` e `model/invoice/FiscalNoteXmlStorage.java`, removendo os dois
  `enum Status` aninhados idênticos. Atualizados os usos em `BilletFileRepository`,
  `BilletFileStorageService` e `FiscalNoteXmlStorageService`.

### 5.3 Modelagem de relacionamento inconsistente no mesmo domínio — ✅ documentado
- `model/purchase/Purchase.java:27-29` usa `@ManyToOne Client client` (relação JPA de verdade),
  enquanto `model/purchase/CombinedScore.java:23-24` guarda `Long clientId` cru, e
  `model/billet/BilletFile.java:37-38` guarda `Long combinedScoreId` cru também.
- Regra documentada em `model/package-info.java`: `@ManyToOne`/`@OneToMany` quando a entidade
  pertence a outra do mesmo domínio e é comum navegar dela; FK crua só quando a referência for
  fraca/cross-domain ou a entidade relacionada não precisar ser carregada no caso de uso comum.
  Não foi feita migração de `CombinedScore.clientId`/`BilletFile.combinedScoreId` para `@ManyToOne`
  nesta passada — é uma mudança de risco maior (toca repositórios, queries e call sites) e deve ser
  avaliada caso a caso seguindo a regra agora documentada.

### 5.4 Índices ausentes em colunas de consulta frequente — ✅ corrigido
- Adicionado `@Index` explícito em `BilletFile` (`combined_score_id`) e `ChatSession`
  (`phone_number`), seguindo o padrão já usado em `model/RefreshToken.java:24-26`.

### 5.5 Query sem garantia de resultado único — ✅ corrigido
- `ChatSessionRepository.findActiveSessionByPhoneNumber` (JPQL sem `LIMIT`) trocado por
  `findFirstByPhoneNumberOrderByCreatedAtDesc` (query method derivado, `Optional<ChatSession>`),
  eliminando o risco de `IncorrectResultSizeDataAccessException` quando há mais de uma sessão viva
  pro mesmo telefone. Chamadas atualizadas em `ChatSessionService`.

### 5.6 Duplicação e magic strings no repositório — parcialmente corrigido
- ✅ Removido o `findByTransactionDateBetweenAndStatementBank` sem `Pageable` (duplicata exata da
  versão paginada; confirmado via grep que só a versão paginada tinha chamador em
  `TransactionExportService`).
- ✅ `CombinedScoreRepository`: as 7 queries com `'PENDENTE'` hardcoded agora usam o literal de enum
  JPQL `com.hortifruti.sl.hortifruti.model.enumeration.Status.PENDENTE` em vez da string solta —
  Hibernate valida o literal contra o enum real, então um rename do valor quebra a query de forma
  explícita (erro no parse) em vez de silenciosamente não bater com nada.
- ✅ `UserRepository.findByUsername`: removida a `@Query` manual, trocado por query method derivado
  (`User findByUsername(String username)`), no mesmo padrão de `ClientRepository`.
- Pendente (não feito nesta passada, é mudança de risco maior — Fase 3): consolidar os 7+ finders
  não paginados de `TransactionRepository` em `Specifications`, já que a interface estende
  `JpaSpecificationExecutor<Transaction>`.

---

## 6. Estrutura de pacotes inconsistente

Hoje não existe uma regra única para "quando um domínio ganha subpacote próprio":

- `dto/` tem subpacotes `bb`, `freight`, `notification`, `transaction`, `user` sem `model/`
  correspondente (`FreightConfig`, `User`, `RefreshToken` ficam soltos na raiz de `model/`).
- `config/bb/` e `dto/bb/` existem, mas não existe `model/bb/`.
- `service/` tem subpacotes `backup`, `notification`, `scheduler`, `storage` sem `model/`/`repository/`
  correspondentes em lugar nenhum.
- `controller/` é majoritariamente flat (`BackupController`, `BilletController`, `DashboardController`,
  `DistanceController`, `InvoiceController`, `ReportTaxController`, `SchedulerController` na raiz),
  enquanto só `climate`, `finance`, `notification`, `purchase`, `user` ganham subpacote — sem critério
  documentado.
- Dentro de `service/billet/` especificamente: 8 classes para um conceito só
  (`BilletService`, `BilletQuery`, `BilletFactory`, `BilletIssue`, `BilletCancel`, `BilletValidation`,
  `BilletInfoCombinedAndClient`, `BilletConstants`) com divisão que não segue uma lógica clara — ex.
  `BilletService` faz persistência/regra de negócio direto (`generateBillet` 281-309,
  `markBilletAsPaid` 254-269) ao mesmo tempo que delega orquestração HTTP quase idêntica pra
  `BilletIssue`/`BilletCancel`.

**Correção sugerida (a decisão mais importante de arquitetura deste documento):** escolher UMA convenção
e documentá-la em `README.md` ou um `ARQUITETURA.md` curto — ✅ feito, ver `ARQUITETURA.md` na raiz
do backend.

Isso sozinho é o que mais reduz o "preciso ler tudo pra saber onde mexer" — uma vez que a convenção
existe e é seguida, dá pra prever onde uma feature nova deveria morar sem precisar explorar o projeto inteiro.

**Migração — prova de conceito com `billet` (✅ feito, ver `ARQUITETURA.md`):**
- `BilletController` movido de `controller/` (flat) para `controller/billet/`.
- `BilletQuery` injetava `CombinedScoreRepository` direto (violando a regra de "um domínio só
  acessa o próprio repositório" — seção 3.6); trocado por uma chamada a
  `CombinedScoreService.findLatestIdByYourNumber` (método novo, mesma lógica que já existia
  inline em `BilletQuery`).
- Papel de cada uma das 8 classes de `service/billet` documentado em
  `service/billet/package-info.java`, em vez de forçar a fusão delas em menos classes: a lógica de
  emissão/baixa/consulta de boleto tem bastante tratamento de erro específico da API do Sicoob, e
  fundir sem testes de regressão cobrindo esses caminhos é mais risco do que ganho de organização
  nesta passada.
- Demais domínios (restante de `controller/` flat, `service/finance` com 20 arquivos,
  `exception/` com 20 arquivos, `dto/invoice` com 15 arquivos etc.) ainda não migrados — fazer
  conforme forem tocados por outro motivo, seguindo a regra em `ARQUITETURA.md`.

---

## 7. Outros achados pontuais (integrações, chatbot, scheduler)

- **Estado da conversa do chatbot em `ConcurrentHashMap` sem expiração** (`ChatbotService`) — sessões
  nunca são removidas por tempo, só por conclusão explícita; risco de crescimento ilimitado de memória
  em produção ao longo do tempo. Sugerido: TTL/eviction (ex. Caffeine cache) em vez de mapa manual.
- **`Thread.sleep` bloqueante em código de request**: `WhatsAppService.sendMultipleDocuments`
  (`Thread.sleep(2000)`) e `DanfeXmlService.downloadWithRetry` (até ~4 tentativas de 4–7s) — este
  último ainda dentro de um método `@Transactional`, segurando conexão de banco aberta durante I/O
  de rede. Sugerido: mover retry de rede pra fora do escopo transacional, e usar backoff assíncrono
  onde possível.
- **Nomes de arquivo temporário previsíveis** em `FileGenerationService.createZipWithStatements` —
  risco de colisão sob concorrência. Sugerido: usar `UUID`/`Files.createTempFile`.
- **`CsvGeneratorService.generateInvoiceProductsCSV` ignora os parâmetros de intervalo de data** e
  exporta a tabela inteira — parece bug funcional, não só estilo; vale confirmar se é intencional.
- **`TransactionUtil`/`PdfUtil`** são `@Component` com construtor privado e métodos só estáticos —
  contraditório (ou é bean Spring, ou é utilitário estático, não os dois). `TransactionUtil` também
  tem nomes de família/funcionário reais hardcoded como regra de categorização — mover para
  configuração/tabela.
- **`model/enumeration/TemperatureCategory.fromTemperature`** duplica os mesmos limites que já
  existem em `contains(double)` na mesma classe — bastava iterar `values()` chamando `contains`.
- **`model/enumeration/Category.FAMÍLIA`** é o único identificador de enum com acento/não-ASCII do
  projeto — risco em qualquer serialização/URL-encoding do nome do enum.
- **`Role`/`Status`** repetem o mesmo padrão de `fromString` (null-check + `valueOf(upperCase)`) cada
  um do seu jeito — extrair um helper genérico de enum.

---

## 8. Plano de ação sugerido (por fases, sem quebrar nada)

**Fase 1 — Limpeza sem risco (não muda comportamento, só remove/loga):**
1. Apagar `GoogleDriveService.java` e `exception/CombinedScoreRequest.java` (confirmar zero uso antes).
2. Remover `getRoleFromToken` morto de `TokenConfiguration`.
3. Trocar todo `System.out`/`System.err`/`printStackTrace`/catch vazio por `@Slf4j` + log apropriado.
4. Adicionar logging em todos os handlers do `GlobalExceptionHandler`.
5. Rotacionar o segredo do webhook do chatbot e mover pra env var de verdade (item 1.1).

**Fase 2 — Padronização (baixo risco, mecânico):**
6. Padronizar IDs de entidade em `Long`.
7. Padronizar DI em construtor (`@RequiredArgsConstructor`) em todo o projeto.
8. Consolidar os dois enums `Status` (billet/invoice) duplicados.
9. Trocar `protected` sem subclasse por `public`, revisando se algum `@Transactional` estava de fato inerte.
10. Desabilitar Swagger em prod.

**Fase 3 — Extração de duplicação (risco médio, testar bem):**
11. Extrair base compartilhada para os PDF generators (famílias A e B da seção 3.1).
12. Extrair helper de retry único para `BilletHttpClient`.
13. Extrair factory compartilhada de SSL/mTLS para `BBSSLConfig`/`BilletSSLConfig`.
14. Unificar cadastro de regras por cliente (fim do caso "LLINEA" duplicado).

**Fase 4 — Decisão de arquitetura (a mais importante a médio prazo):**
15. Definir e documentar a convenção de pacotes/papéis da seção 6, e ir migrando módulo por módulo
    (começar por `billet`, que é o pior caso hoje) conforme for mexendo neles por outro motivo —
    não precisa de uma refatoração "big bang".

Cada fase é independente e pode ser feita em PRs pequenos. Nenhuma delas exige mudar contrato de API
ou schema de banco de forma incompatível (a fase 5.4/5.5, se aplicada, é aditiva — só cria índice).
