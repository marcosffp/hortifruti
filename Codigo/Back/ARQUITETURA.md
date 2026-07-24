# Arquitetura — convenção de pacotes (Backend)

> Decisão referente à seção 6 de `CORRECOES_BACKEND.md` (estrutura de pacotes inconsistente).
> Vale a partir de agora para código novo; código existente é migrado aos poucos, conforme for
> mexido por outro motivo — não é uma refatoração "big bang".

## Regra

Todo domínio de negócio (`billet`, `invoice`, `purchase`, `finance`, `chatbot`, `backup`,
`climate`, `freight`, `bb`, `sicoob`, `storage`...) ganha um subpacote homônimo em `controller/`,
`service/`, `dto/`, `model/`, `repository/` e `exception/` **quando aplicável a esse domínio** —
nem todo domínio precisa dos seis (ex.: um domínio sem entidade JPA própria não tem
`model/<dominio>/`).

Uma classe solta na raiz de `controller/`, `service/`, `dto/`, `model/`, `repository/` ou
`exception/` só é aceitável quando não pertence a nenhum domínio específico (ex.: `User`,
`RefreshToken`, `FreightConfig` em `model/` — são conceitos transversais/de infraestrutura, não
parte de um domínio de negócio maior; `GlobalExceptionHandler` em `exception/` pelo mesmo motivo —
é o `@RestControllerAdvice` único que trata exceções de todos os domínios).

## Papéis dentro de `service/<dominio>/`

Usar no máximo estes três papéis por classe:

- **`XService`** — orquestração + regra de negócio pública; é o único ponto de entrada do domínio
  usado por controllers/outros domínios.
- **`XClient` / `XHttpClient`** — só chamada externa (API de banco, Focus NFe, etc.), sem regra de
  negócio.
- **`XRepository`** — acesso a dados via Spring Data, sem lógica.

Evitar `XQuery` / `XFactory` / `XValidation` como classes soltas sem justificativa. Quando um
domínio precisar de mais granularidade que isso (ex.: `service/billet`, que tem chamadas HTTP
distintas para emitir/baixar/consultar boleto, cada uma com tratamento de erro específico da API
do Sicoob), documente explicitamente o papel de cada classe num `package-info.java` do pacote —
ver `service/billet/package-info.java` como exemplo aplicado.

## Acesso a repositório entre domínios

Um serviço de domínio X só injeta o repositório de X. Para ler dados de outro domínio Y, chama o
`YService` (ou, se o acesso for só um lookup por ID sem regra de negócio, um método pontual
exposto pelo `YService` — não o `YRepository` direto).

## Migração incremental

Não é necessário mover tudo de uma vez. Ao tocar em um domínio por outro motivo (bug, feature),
aproveite para:

1. Mover o `controller/`, `dto/`, `model/`, `repository/` desse domínio para o subpacote homônimo,
   se ainda não estiverem lá.
2. Revisar se as classes de `service/<dominio>/` cabem nos três papéis acima; se não, documentar
   via `package-info.java` em vez de forçar uma fusão arriscada sem cobertura de testes.

`billet` foi o primeiro domínio migrado como prova de conceito: `BilletController` movido de
`controller/` para `controller/billet/`; acesso direto de `BilletQuery` a `CombinedScoreRepository`
substituído por chamada a `CombinedScoreService` (regra de "um domínio só acessa o próprio
repositório"); papéis das 8 classes de `service/billet` documentados em
`service/billet/package-info.java`.

## Segunda passada — arrumação geral de `controller/`, `service/`, `dto/`, `model/`, `repository/`

Depois da prova de conceito com `billet`, os demais arquivos soltos na raiz das cinco pastas
(quando já existia um subpacote de domínio equivalente em outra camada) foram movidos para o
subpacote correto, sem mudar comportamento (só `package`/imports; validado com `mvn compile` a
cada lote):

- `controller/`: `BackupController` → `controller/backup/`; `DashboardController` →
  `controller/dashboard/`; `DistanceController` → `controller/freight/`; `InvoiceController` e
  `ReportTaxController` → `controller/invoice/`; `SchedulerController` → `controller/scheduler/`.
- `service/`: `DashboardService` → `service/dashboard/`; `UserService` → `service/user/` (alinhando
  com `controller/user/`/`dto/user/`, que já existiam).
- `dto/`: `BackupResponse` → `dto/backup/`. O pacote `dto/transaction/` (nome que não batia com
  nenhuma camada — o domínio é `finance` em `controller/`, `service/`, `model/` e `repository/`)
  foi fundido em `dto/finance/`.
- `model/`/`repository/`: `ClimateProduct`/`ProductRepository` (nomeados de forma genérica, mas
  são do domínio `climate`) → `model/climate/`/`repository/climate/`.
- `chatbot`: já existia `model/chatbot/` e `repository/chatbot/`, mas a camada de serviço estava
  dividida entre `service/notification/ChatbotService.java` (solto) e
  `service/notification/chatbot/*` (3 classes, um nível a mais de aninhamento sob `notification`).
  Consolidado tudo em `service/chatbot/`, e `controller/notification/ChatbotController` movido para
  `controller/chatbot/`, alinhando as cinco camadas sob o mesmo nome de domínio.

`model/FreightConfig`, `model/RefreshToken`, `model/User` (e seus respectivos repositórios)
continuam soltos na raiz — são o caso explicitamente aceito na regra acima, não pendência.
`mapper/` não entra nessa convenção (não tem noção de domínio própria — os mappers já usam DTO e
model de `dto/<dominio>/`/`model/<dominio>/`, não faz diferença prática agrupá-los de novo por
domínio) e continua flat de propósito.

## Terceira passada — dividir pastas grandes que misturam sub-sistemas diferentes

Além de arquivo solto na raiz, o outro sintoma de "pasta bagunçada" é uma pasta de domínio com
muitos arquivos onde dá pra ver, só pelo nome, que existem 2+ sub-sistemas prefixados
(`BB*`/`Sicoob*`/`Transaction*`, `Email*`/`WhatsApp*`) morando juntos sem separação. Quando isso
acontece, o subpacote de domínio ganha subpacotes por sub-sistema (mesma ideia de
`service/invoice/tax/{icms,nfSales,payment,registerReport,sales}`, só que aplicada a integração
externa em vez de tipo de relatório):

- `service/finance/` (20 arquivos flat) → `service/finance/bb/` (tudo que fala com a API do BB:
  `BBSaldoService`, `BBStatementService`, `BBExtrato*`, `TransactionBBApiService`),
  `service/finance/sicoob/` (equivalente para Sicoob) e `service/finance/transaction/` (relatório e
  processamento de transação genérico, banco-agnóstico: `TransactionReport*`, `TransactionExport*`,
  `TransactionProcessingService`, `TransactionImportPersistenceService`). `StatementService`
  (orquestrador comum aos dois bancos), `MacroExportService` e `AbstractPdfPageWriter` (base
  compartilhada dos 3 PDF generators) continuam na raiz de `service/finance/` por serem
  transversais aos três subpacotes.
- `service/notification/` (13 arquivos flat) → `service/notification/email/` (`Email*`,
  `Gmail*EmailSender`, `SendGridEmailSender`) e `service/notification/whatsapp/`
  (`WhatsAppService`, `WhatsAppMessageBuilder`). `BulkNotificationService`, `NotificationService`,
  `NotificationCoordinator` e `StatementSelectionService` continuam na raiz por orquestrarem os dois
  canais.

Efeito colateral encontrado nessa passada: `AbstractPdfPageWriter` e `EmailGreetingUtil` eram
package-private (dependiam de estar no mesmo pacote que quem os usava) — precisaram virar `public`
para continuar acessíveis depois da divisão em subpacotes. Ao criar um novo subpacote por
sub-sistema, revise se alguma classe/método usada de fora ainda está com visibilidade de pacote.

Pastas de tamanho parecido que **não** foram divididas nesta passada porque não misturam
sub-sistemas (são um domínio só, com várias entidades/papéis do mesmo sub-sistema):
`service/billet/` (8 classes, já documentado em `service/billet/package-info.java`),
`service/invoice/` raiz (8 classes core do domínio, fora de `factory/`/`tax/`) e `service/purchase/`
(9 classes, uma por entidade do domínio). Dividir essas por dividir seria mais nível de pasta sem
reduzir a mistura de conceitos — o problema que essa seção resolve é sub-sistema diferente na mesma
pasta, não "muitos arquivos" por si só.

## Quarta passada — `exception/` também entra na convenção

`exception/` tinha ficado de fora das primeiras passadas (a regra falava só de 5 pastas), mas tinha
o mesmo problema das outras: 19 classes de exceção de domínios diferentes soltas juntas na raiz,
sem nenhuma relação com a organização já usada em `controller/`/`service/`/`dto/`/`model/`. A regra
do topo desta página agora inclui `exception/` como sexta pasta. Mapeamento (cada exceção foi pro
subpacote do domínio que a lança e captura — confirmado por grep de uso, não só pelo nome):

- `exception/auth/`: `AuthException`, `TokenException` (mecanismo de autenticação/JWT, mesmo
  agrupamento de `config/auth/`).
- `exception/billet/`, `exception/invoice/`, `exception/notification/`, `exception/storage/`,
  `exception/user/`: uma exceção cada, mapeamento direto pro domínio homônimo.
- `exception/climate/`: `ProductException`, `RecommendationException`, `WeatherApiException`.
- `exception/freight/`: `DistanceException`, `FreightException`.
- `exception/purchase/`: `ClientException`, `CombinedScoreException`, `PurchaseException`.
- `exception/bb/` e `exception/sicoob/`: `BBApiException`/`SicoobExtratoException` — pacote de
  nível superior (não aninhado em `exception/finance/`), espelhando `dto/bb/`/`dto/sicoob/` e
  `config/bb/`/`config/sicoob/`, que também são top-level em vez de `dto/finance/bb/`.
- `exception/finance/`: só `TransactionException` — é a única exceção do domínio finance que não é
  específica de um banco (lançada tanto no fluxo BB quanto Sicoob quanto no processamento genérico
  de transação).

`GlobalExceptionHandler` continua na raiz de `exception/` (é o `@RestControllerAdvice` único,
importa as 19 classes explicitamente agora que não estão mais no mesmo pacote — antes disso não
precisava de nenhum import porque tudo vivia junto em `exception/`).

## Quinta passada — `model/enumeration/` e `dto/invoice/` flat

Mesmo problema da terceira/quarta passada, em mais dois lugares:

- `model/enumeration/` (13 enums de todos os domínios juntos, pasta técnica em vez de pasta de
  domínio) foi extinta: cada enum foi pro `model/<dominio>/` de quem realmente o usa —
  `Bank`/`Category`/`StatementOrigin`/`TransactionType` → `model/finance/`;
  `Month`/`RecommendationTag`/`TemperatureCategory` → `model/climate/` (`Month` só é usado por
  `ClimateProduct`/`MonthListConverter`, apesar do nome genérico); `Status` → `model/purchase/`
  (é o status do `CombinedScore`); `NotificationChannel`/`NotificationRecipient`/`NotificationType`
  → novo `model/notification/` (domínio sem entidade JPA própria, só os enums — igual já acontecia
  com `dto/notification`/`service/notification` antes de terem `model/`). `FileStatus` (usado por
  `BilletFile` **e** `FiscalNoteXmlStorage`, dois domínios) e `Role` (enum do `User`, que já é
  transversal) ficaram soltos na raiz de `model/`, junto de `User`/`RefreshToken`/`FreightConfig` —
  são o mesmo caso de "conceito transversal", não pendência.
- `dto/invoice/` (15 arquivos flat) tinha exatamente o padrão da seção anterior: DTOs de tipo de
  relatório fiscal específico misturados com DTO core do domínio, sem espelhar o
  `service/invoice/tax/{icms,nfSales,payment,registerReport,sales}/` que já existia. Migrado pra
  `dto/invoice/tax/icms/IcmsSalesReport`, `dto/invoice/tax/registerReport/InvoiceSummaryDetails`,
  `dto/invoice/tax/sales/SalesSummaryDetails` (uso exclusivo de cada subpacote de relatório) e
  `dto/invoice/tax/{InvoiceTaxDetails,ItemTaxDetails}` (usados pelos 4 subpacotes de tax **e** por
  `InvoiceQuery` no core — por isso ficam na raiz de `dto/invoice/tax/`, não em um subpacote
  específico). `dto/invoice/` raiz ficou só com os 10 DTOs do domínio core (request/response de
  emissão/consulta de nota).

Antes de dividir uma pasta grande por sub-sistema, sempre confirme por grep de uso (não só pelo
nome) — foi assim que se descobriu que `Month` é exclusivo de `climate` apesar do nome parecer
genérico, e que `InvoiceTaxDetails`/`ItemTaxDetails` são compartilhados entre 4 subpacotes de tax
em vez de pertencer a um só.

`util/` também tinha o mesmo sintoma (`BBExtratoParsingUtil`, `SicoobExtratoParsingUtil` e
`MonthListConverter` — cada um usado por um único domínio) misturado com utilitário de verdade
(`PdfUtil`, `FileZipUtils`, `TransactionUtil`, `SicoobExtratoFormatUtil` — usados por 2+ domínios
apesar do nome de alguns sugerir um banco só). Os três de uso único foram pra junto de quem os usa
(`service/finance/bb/`, `service/finance/sicoob/`, `model/climate/`); o resto ficou em `util/`
porque é genuinamente compartilhado entre domínios — `util/`, assim como `mapper/`, não entra na
convenção de subpacote por domínio (não é uma das seis pastas da regra do topo), então o critério
pra mover um arquivo de lá não é "nome parece de um domínio", é "só um domínio usa isso".
