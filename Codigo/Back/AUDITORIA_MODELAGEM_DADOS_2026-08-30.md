# Auditoria do Modelo de Dados e Arquitetura de Persistência

**Data:** 2026-08-30
**Escopo:** `Codigo/Back/src` (backend Spring Boot / JPA / MySQL / Flyway)
**Método:** leitura direta do código-fonte atual — entidades (`model/`), repositórios, services, mappers, DTOs e as 17 migrations Flyway (`db/migration/V1..V17`). Não foram usadas conclusões de auditorias anteriores (inclusive o `AUDITORIA.md` já existente neste diretório, que **não foi alterado** por este documento) como premissa; cada achado abaixo foi confirmado lendo o código real e, sempre que possível, rastreando o caminho de execução completo (service → repository → coluna).
**Regra seguida:** nenhuma alteração foi feita no projeto. Este arquivo é só diagnóstico.

> Nota sobre o `AUDITORIA.md` existente: vários comentários no código (`PurchaseRepository`, `ClientMapper`, `application.properties`) referenciam itens desse arquivo (ex.: "item E-C1") como já resolvidos — por exemplo, a remoção do contador `totalPurchaseValue`. Este documento trata esses pontos como **já corrigidos e validados** (ver seção 6), não como achados novos, e foca em problemas que continuam presentes no código atual ou que a análise anterior não cobriu.

---

## 1. Sumário executivo

O modelo de dados é, em geral, bem cuidado: há comentários extensos explicando *por quê* de decisões não óbvias, um histórico de migrations que mostra aprendizado real com incidentes de produção, e pelo menos um caso (`Client.totalPurchaseValue`) de uma redundância perigosa que já foi identificada e removida corretamente. Isso não é um projeto começando do zero — é um sistema que já passou por ciclos de correção.

Dito isso, a auditoria encontrou um pequeno número de problemas **concretos e verificáveis**, não hipotéticos:

| # | Achado | Categoria | Severidade |
|---|--------|-----------|------------|
| 1 | `CombinedScore.status` e `hasBillet`/`hasInvoice` podem ficar **de fato inconsistentes** — caminho de código demonstrado (`SicoobOpenBilletReconciler`) que muda o status sem sincronizar os booleanos | **BUG / INCONSISTÊNCIA** | Alta |
| 2 | Cancelar/excluir um `CombinedScore` deixa `Purchase.combined_score_id` **órfão** (apontando para um agrupamento que não existe mais), sem nenhuma rotina de limpeza | **BUG / RISCO** | Alta |
| 3 | `createCombinedScore` não impede que a mesma compra entre em **dois agrupamentos diferentes** (sem checagem de "já agrupada") | **RISCO** | Alta |
| 4 | Regras de negócio por cliente (`ClientBusinessRules`) vivem **hardcoded em Java**, casadas por um `String` de nome de cliente digitado igual ao banco, em vez de colunas na entidade `Client` | **DÍVIDA TÉCNICA / OPORTUNIDADE DE NORMALIZAÇÃO** | Alta |
| 5 | `Client.lastPurchaseDate` é um campo derivado que pode dessincronizar (não é corrigido ao excluir a compra mais recente; é gravado com `createdAt`, não `purchaseDate`) — o mesmo tipo de bug que já foi corrigido para `totalPurchaseValue`, mas não foi replicado aqui | **REDUNDÂNCIA / RISCO** | Média-Alta |
| 6 | Só `Transaction.category` tem proteção (`columnDefinition = VARCHAR(32)`) contra o bug de ENUM nativo do MySQL que já causou 4 migrations de correção (V9, V12–V15). **Todas as outras ~12 colunas `@Enumerated(STRING)`** do sistema estão expostas ao mesmo risco | **DÍVIDA TÉCNICA sistêmica** | Média-Alta |
| 7 | `Client.document` (CPF/CNPJ, o identificador legal/fiscal real do cliente) não tem `unique`, enquanto `Client.email` (opcional) tem | **OPORTUNIDADE DE NORMALIZAÇÃO** | Média |
| 8 | `InvoiceProduct`/`GroupedProduct` guardam `code`/`name` como texto solto, sem FK para `FiscalProduct` — vínculo só por igualdade de string | **MELHORIA DE MODELAGEM** (design majoritariamente justificável) | Baixa-Média |
| 9 | Tabela `products` (entidade `ClimateProduct`, domínio de clima/sazonalidade) tem nome genérico que colide conceitualmente com o catálogo real de produtos (`fiscal_products`) | **MELHORIA DE MODELAGEM** (nomenclatura) | Baixa |
| 10 | Convenção do projeto de usar `Long` cru em vez de `@ManyToOne` para a maioria das relações (sem FK no banco, com `ddl-auto=update`) | **DÍVIDA TÉCNICA reconhecida e documentada** — decisão de design com trade-off explícito, não descuido | Informativo |

As seções 3–5 detalham cada achado no formato pedido (como está hoje / o que é o problema / por que existe / se é justificável / modelagem ideal / benefício / risco / impacto em dados existentes / necessidade de migration / dificuldade). A seção 6 lista explicitamente o que foi investigado e **não deveria ser mudado** — decisões de design válidas que só parecem redundância à primeira vista.

---

## 2. Visão geral da arquitetura de persistência

- **ORM:** Hibernate/JPA com `spring.jpa.hibernate.ddl-auto=update` em **todos** os ambientes, inclusive produção (`application-prod.properties` provavelmente segue o padrão comum documentado em `application.properties`). Isso é uma decisão consciente e documentada: travar em `validate` exigiria confirmar antes, com acesso real ao banco de hml/prod, que o schema bate exatamente com as entidades.
- **Migrations:** Flyway (`spring.flyway.*`), mas só a partir da V1 — as tabelas "de sempre" (clients, purchases, combined_scores, users etc.) nunca tiveram uma migration `CREATE TABLE`: nasceram do `ddl-auto=update` e continuam sendo mantidas por ele. As 17 migrations existentes cobrem apenas mudanças pontuais que o `ddl-auto=update` comprovadamente não sabe fazer sozinho (larguras de BLOB, ENUM nativo, renomear coluna, backfill de dado). Isso está documentado explicitamente no `application.properties` (baseline em V11, histórico dos scripts antigos de `static/*.sql`).
- **Convenção de FK:** a esmagadora maioria das relações entre agregados (`Purchase.combinedScoreId`, `CombinedScore.clientId`, `BilletFile.combinedScoreId`, `TabelaPrecoClienteItem.fiscalProductId`, `ClienteProdutoMapeamento.fiscalProductId`, `CapturaNotaPendente.usuarioId`/`dispositivoId`, `RefreshToken.userId`, `DispositivoVinculado.userId` etc.) é modelada como `Long` simples + índice, **não** como `@ManyToOne`/`@JoinColumn`. Isso é intencional e comentado no próprio código (`V16`: "Sem FK explícita, seguindo o padrão já usado no projeto... só BIGINT + índice"). A minoria que usa relação JPA real é `Purchase.client`, `InvoiceProduct.purchase`, `GroupedProduct.combinedScore`, `Transaction.statement`.
- **Domínios principais:** `purchase` (cliente, compra, item de nota, agrupamento de cobrança, tabela de preço de cliente), `product` (catálogo fiscal), `finance` (extrato bancário/transações — independente do resto), `invoice`/`billet` (documentos fiscais/bancários emitidos), `notification`, `climate` (recomendação de produto por clima — cuidado: não é o mesmo "produto" do domínio fiscal), `googleauth`, autenticação (`User`, `Role`, `RefreshToken`, `LoginAuditLog`, `LoginLockout`, `DispositivoVinculado`).

---

## 3. Achados críticos (requerem decisão/priorização)

### 3.1 `CombinedScore.status` × `hasBillet`/`hasInvoice` podem ficar de fato inconsistentes

**Como está hoje:** `CombinedScore` guarda simultaneamente um enum `status` (`PENDENTE, PAGO, CANCELADO, CANCELADO_BOLETO, CANCELADO_NOTA_FISCAL`) **e** dois booleanos independentes `hasBillet`/`hasInvoice`. As variantes `CANCELADO_BOLETO`/`CANCELADO_NOTA_FISCAL` só fazem sentido combinadas com um valor específico dos booleanos no momento em que são gravadas (ex.: `CANCELADO_BOLETO` deveria implicar `hasBillet=false`, já que foi o boleto que foi cancelado).

Existem hoje **dois caminhos de código diferentes** que fazem essa transição:
- `CombinedScoreService.updateStatusAfterBilletCancellation` / `updateStatusAfterInvoiceCancellation`: atualizam status **e** zeram o booleano correspondente na mesma operação — consistentes.
- `SicoobOpenBilletReconciler.tryUpdateClosedStatus` (reconciliação automática com o Sicoob, `service/billet/SicoobOpenBilletReconciler.java:116`): chama `combinedScoreService.updateStatus(cs.getId(), Status.CANCELADO_BOLETO)` — o método genérico `updateStatus` (`CombinedScoreService.java:428`) só grava o enum, **sem tocar em `hasBillet`**.

**Por que existe:** `updateStatus` foi criado como "ponto único de escrita de status usado por outros domínios" (comentário no próprio método), mas seu contrato não deixa claro que ele *não* sincroniza os booleanos — e o reconciliador do Sicoob assumiu (implicitamente) que bastava setar o status.

**É justificável?** Não. Não há razão de negócio para `CANCELADO_BOLETO` conviver com `hasBillet=true` — é uma contradição de dado, não uma escolha de design.

**Consequência prática rastreada:** um `CombinedScore` que passa por esse caminho fica com `status=CANCELADO_BOLETO` e `hasBillet=true` ao mesmo tempo. Isso não quebra a query `findAllOpenBillets` (filtra por `status=PENDENTE`, então não aparece mais), mas contamina qualquer lógica que confie em `hasBillet` isoladamente — por exemplo, `CombinedScoreCancellationService.cancelGrouping` decide se deve chamar `cancelBilletOrThrow` olhando só `isHasBillet()`; para um registro nessa condição, ele tentaria cancelar de novo no Sicoob um boleto que o próprio sistema já sabe (pelo status) que está encerrado. O código tem uma rede de segurança parcial (trata `HttpStatus.CONFLICT` do Sicoob), mas isso é proteção contra o sintoma, não contra a causa.

**Modelagem ideal:** reduzir para uma única fonte de verdade. Duas opções razoáveis:
1. Eliminar os booleanos e derivar "tem boleto ativo"/"tem NF ativa" do `status` + de uma FK real para o documento (billet/invoice) mais recente não cancelado.
2. Manter os booleanos como a única fonte de verdade sobre "documento ativo agora" e mover a informação "qual documento causou o cancelamento" para um campo dedicado (`cancelamentoOrigem: BOLETO|NOTA_FISCAL|MANUAL`), sem reduplicar isso dentro do próprio enum de status.

Qualquer uma das duas elimina a possibilidade de as duas representações divergirem, porque passam a existir em **um lugar só**.

**O que eliminar/relacionar:** o enum `Status` deixaria de carregar a informação "por causa de qual documento" — isso vira um campo/coluna própria, e todo caminho de escrita (inclusive `SicoobOpenBilletReconciler`) passaria a atualizar um único ponto.

**Benefício:** impossível ficar inconsistente porque não há mais duas cópias do mesmo fato.

**Riscos da mudança:** média — toda a lógica de cancelamento/reconciliação usa esses campos; qualquer refator precisa de testes de regressão cobrindo os fluxos de billet/invoice (que já são complexos e envolvem integrações externas irreversíveis).

**Impacto em dados existentes:** exigiria decidir o que fazer com linhas já gravadas nesse estado inconsistente (poucas, mas existem se o reconciliador já rodou em produção) — provavelmente um backfill que reconcilia `hasBillet`/`hasInvoice` a partir do `status` atual.

**Migration necessária:** sim, se a coluna nova (`cancelamentoOrigem`) for adotada; não, se só o comportamento de `updateStatus` for corrigido (fix de código, sem mudança de schema) — essa é a correção mais barata e deveria ser considerada separadamente da modelagem ideal, por ser bug puro.

**Dificuldade:** o fix pontual (fazer `SicoobOpenBilletReconciler` chamar o método correto, com sincronia) é baixa. A remodelagem completa do trio status/hasBillet/hasInvoice é média.

---

### 3.2 `Purchase.combined_score_id` fica órfão após cancelamento/exclusão de um agrupamento

**Como está hoje:** `Purchase.combinedScoreId` é um `Long` (não FK JPA) preenchido em exatamente um lugar (`CombinedScoreService.createCombinedScore`, linha 120) e **nunca resetado** em nenhum outro lugar do código (confirmado por busca em todo o `service/`). `CombinedScoreCancellationService.hardDeleteLocally` apaga a linha de `CombinedScore` (`combinedScoreRepository.delete`) quando o agrupamento é cancelado, mas não toca nas `Purchase` que apontavam para ele.

**Problema:** depois de um cancelamento com exclusão, as compras que faziam parte daquele agrupamento continuam com `combined_score_id` apontando para um ID que não existe mais no banco. Como não há FK real no MySQL (ver §2), isso não gera erro de integridade — só um dado morto e silencioso.

**Por que existe:** é uma consequência direta e não intencional da convenção "FK sem constraint real" (§2, decisão válida em si) combinada com a ausência de uma rotina simétrica de "desvincular" ao cancelar.

**É justificável?** Não como está. O código de cancelamento até comenta que arquivos órfãos no R2 são um risco aceito ("requer limpeza manual"), mas isso nunca foi dito para `Purchase.combinedScoreId` — parece um esquecimento, não uma decisão.

**Consequência prática:** essas compras ficam "presas" — não aparecem mais em nenhuma consulta feita por `combined_score_id` (porque o ID não existe mais em lugar nenhum útil), mas também não voltam a ficar "livres" para reagrupamento de forma sinalizada; na prática, como `createCombinedScore` nem filtra por `combinedScoreId IS NULL` (ver §3.3), elas *podem* ser reagrupadas, só que sem nenhum rastro de que já estiveram num agrupamento cancelado.

**Modelagem ideal:** ao fazer `hardDeleteLocally`, resetar explicitamente `combined_score_id = NULL` nas `Purchase` associadas (mesmo padrão de "limpeza simétrica" que o método já aplica a `BilletFile`/`FiscalNoteXmlStorage`/R2). Não precisa de migration — é puramente uma correção de comportamento no service.

**Benefício:** elimina dado morto e deixa claro, de novo, quais compras estão "livres" para um novo agrupamento.

**Riscos:** baixíssimo — é um `UPDATE` adicional dentro de uma transação que já existe.

**Impacto em dados existentes:** se já houver linhas órfãs em produção, um backfill (`UPDATE purchases SET combined_score_id = NULL WHERE combined_score_id NOT IN (SELECT id FROM combined_scores)`) resolveria o passivo.

**Migration necessária:** não para o fix de comportamento; opcionalmente sim para o backfill de dados já órfãos (script de dado, não de schema).

**Dificuldade:** baixa.

---

### 3.3 Nenhuma garantia contra uma compra entrar em dois agrupamentos

**Como está hoje:** `CombinedScoreService.createCombinedScore` busca compras por `purchaseRepository.findByClientIdAndPurchaseDateBetween(clientId, startDate, endDate)` — **sem** filtrar por `combinedScoreId IS NULL` nem verificar se alguma delas já pertence a um agrupamento ativo. Ele simplesmente sobrescreve `purchase.setCombinedScoreId(novoId)` para todas as compras encontradas no período.

**Problema:** se dois agrupamentos forem criados para o mesmo cliente com períodos que se sobrepõem (erro de operação, duplo clique após falha de rede, ou reprocessamento), a mesma compra pode acabar contabilizada — via `GroupedProduct`/`totalValue` — em **dois** `CombinedScore` diferentes, ou ter seu `combinedScoreId` silenciosamente "roubado" pelo agrupamento mais recente sem que o primeiro agrupamento saiba que perdeu uma de suas origens. Isso é um risco direto de cobrança duplicada ou de boleto/NF com valor que não bate mais com a soma real das compras de origem.

**Por que existe:** o modelo trata `combinedScoreId` como um rótulo informativo ("permite depois listar as fotos de todas as compras de um agrupamento", segundo o comentário em `Purchase.java`), não como uma relação com invariante protegida.

**É justificável?** Parcialmente — no fluxo normal de uso (operador escolhe um período, confirma, sistema gera o agrupamento), a chance de sobreposição é baixa. Mas não é uma garantia de sistema, é uma expectativa de processo, e o código não tem nenhuma rede de segurança para o caso de erro humano ou concorrência.

**Modelagem ideal:** antes de criar o agrupamento, `createCombinedScore` deveria rejeitar (ou pelo menos avisar) compras cujo `combinedScoreId` já não seja nulo e aponte para um `CombinedScore` ainda ativo (`status` diferente de `CANCELADO*`). Alternativa mais forte: `Purchase.combinedScoreId` só pode ser setado se hoje for `NULL` (constraint de aplicação, verificada dentro da própria transação que já existe).

**Benefício:** elimina risco de dupla contagem de receita.

**Riscos da correção:** baixo tecnicamente, mas precisa decidir a UX (bloquear, ou permitir com confirmação explícita — pode haver um caso de negócio legítimo de "reagrupar" que a auditoria não teve visibilidade suficiente para descartar).

**Impacto em dados existentes:** nenhum, é uma validação nova, não uma mudança de schema.

**Migration necessária:** não.

**Dificuldade:** baixa a média (depende da decisão de UX acima).

---

### 3.4 `ClientBusinessRules`: regra de negócio por cliente hardcoded em Java, casada por nome digitado

**Como está hoje (`service/purchase/ClientBusinessRules.java`):** um `Map<String, ClientRule> RULES_BY_NAME` estático, populado em bloco `static {}`, com regras de vencimento de boleto/NF (`dueDateDaysToAdd`, ajuste de fim de semana, dias úteis/corridos, template de texto da NF, exigência de "dados adicionais") para **quatro clientes nomeados por string literal**: `"LLINEA"`, `"APTA"`, `"INDUSTRIA"`, `"ROCA"`. O comentário no próprio arquivo alerta: *"Os nomes devem estar EXATAMENTE como aparecem no banco de dados"*. `DueDateCalculator.getApplicableRule` casa por `extractFirstName(client.getClientName())` (primeira palavra do nome, comparação case-insensitive) contra esse mapa.

**Problema:** essa é uma configuração de negócio por cliente — dado, por natureza — vivendo como código-fonte, e pior, vinculada não ao `Client.id` (chave estável) e sim a um recorte de texto do campo `clientName`, que é editável livremente pela tela de cadastro de cliente (`ClientService.updateClient` permite trocar `clientName` sem nenhuma validação contra esse mapa). Se alguém renomear o cliente "LLINEA" para "LLínea Distribuidora" (ou simplesmente corrigir um espaço/acento), a regra específica silenciosamente deixa de ser aplicada e o sistema cai para `CNPJ_DEFAULT_RULE` — sem erro, sem log, sem aviso, e potencialmente mudando a data de vencimento de boletos reais sem que ninguém perceba.

**Por que existe:** provavelmente nasceu como solução rápida para poucos casos especiais conhecidos, numa época em que parecia mais simples que criar colunas na entidade `Client`. O próprio comentário da classe já demonstra consciência do problema ("Centraliza aqui qualquer regra hoje espalhada por caso especial de cliente... para evitar que cada regra seja reimplementada de forma independente") — ou seja, foi uma melhoria sobre um estado ainda pior (regra espalhada por vários services), mas não chegou a virar dado.

**É justificável?** Não como modelo de dados definitivo. É aceitável como *estado intermediário* (a classe já resolveu o problema de duplicação entre services), mas o acoplamento por string de nome — mutável e sem constraint — é um risco real de regra de negócio quebrar silenciosamente.

**Modelagem ideal:** promover os campos de `ClientRule` para colunas nullable em `Client` (ou uma tabela `client_billing_rule` 1:1, se o time preferir não poluir a entidade principal): `due_date_days_to_add`, `due_date_weekend_adjustment`, `due_date_business_days`, `invoice_note_template`, `requires_dados_adicionais`. `DueDateCalculator` passaria a ler esses campos diretamente do `Client` carregado (que ele já recebe como parâmetro), com fallback para as regras-padrão por tipo de documento (CPF/CNPJ) quando os campos estiverem `NULL` — preservando exatamente o comportamento atual linha a linha, já que os quatro conjuntos de regra viram só dado de seed migrado.

**O que eliminar/relacionar:** o `Map` estático e o casamento por `extractFirstName` deixam de existir; a relação passa a ser pelo `Client.id`, que é estável e não depende de digitação exata.

**Benefício:** regra de negócio muda por UI/dado (sem deploy), não quebra silenciosamente ao renomear o cliente, e fica auditável/rastreável como qualquer outro campo do cadastro.

**Riscos da mudança:** baixo tecnicamente; o risco real é operacional — a migração de dados precisa mapear corretamente os 4 clientes existentes para as novas colunas, sem trocar nenhuma regra por engano (a auditoria não teve acesso ao banco real para confirmar que `clientName` bate exatamente com os literais hoje, então essa validação precisa ser feita manualmente antes de migrar).

**Impacto em dados existentes:** baixo — só 4 clientes precisam de backfill; os demais mantêm o comportamento default.

**Migration necessária:** sim (novas colunas em `Client`, todas nullable, + `UPDATE` pontual para os 4 clientes conhecidos).

**Dificuldade:** média (o código de `DueDateCalculator`/`ClientBusinessRules` precisa ser reescrito com cuidado para preservar exatamente a lógica de ajuste de fim de semana/feriado, que é sutil).

---

### 3.5 `Client.lastPurchaseDate`: mesma classe de bug que já foi corrigida para `totalPurchaseValue`, mas não para este campo

**Como está hoje:** `Client.lastPurchaseDate` (`LocalDate`) é um campo denormalizado, atualizado em dois pontos de `PurchaseService` (`processPurchaseFile` e `createManualPurchase`) com `client.setLastPurchaseDate(purchase.getCreatedAt().toLocalDate())`.

**Dois problemas distintos, ambos verificados no código:**

1. **Não é recalculado ao excluir a compra mais recente.** `PurchaseService.deletePurchaseById` só faz `purchaseRepository.delete(purchase)` — não toca em `client.lastPurchaseDate`. Se a compra excluída era a mais recente do cliente, o campo fica apontando para uma data cuja compra não existe mais, e nada no sistema o corrige depois (não há um recálculo em batch nem uma trigger).
2. **Usa `createdAt`, não `purchaseDate`.** O nome do campo (`lastPurchaseDate`, e seu uso em `ClientResponse`/`ClientWithLastPurchaseResponse` como "última compra") sugere que ele deveria refletir a data *da compra em si*. Mas ele é gravado com `purchase.getCreatedAt()` — o momento em que o registro foi criado no sistema, não `purchase.getPurchaseDate()`. Isso diverge sempre que uma compra manual é lançada com data retroativa (`ManualPurchaseRequest.purchaseDate()`, que o formulário permite escolher livremente) depois de já existir uma compra mais recente: `lastPurchaseDate` passa a refletir "a última vez que alguém mexeu no sistema para este cliente", não "a data da compra mais recente dele" — s

emanticamente errado para o nome do campo.

Ironicamente, o mesmo arquivo (`PurchaseRepository.java`) já documenta, no método `sumTotalByClientId`, a lição aprendida com exatamente esse padrão de bug para `totalPurchaseValue`: *"Soma real das compras do cliente — fonte da verdade... em vez de um contador mutável que pode dessincronizar"*. `lastPurchaseDate` é o mesmo tipo de contador mutável, só que para "data" em vez de "valor", e não recebeu a mesma correção.

**Por que existe:** provavelmente foi implementado antes da correção de `totalPurchaseValue`/depois, sem que alguém conectasse os dois como o mesmo padrão de risco.

**É justificável?** Como cache de leitura (evitar um `MAX(purchase_date)` a cada listagem de clientes), sim — é um padrão de otimização legítimo. Como está implementado hoje (sem recomputação em delete, e com a métrica errada gravada), não.

**Modelagem ideal:** duas opções, ambas preservando o comportamento observável de "mostrar a última data de compra":
1. Substituir por uma query derivada (`MAX(purchaseDate)` por `clientId`), exatamente como já foi feito para o total — mais simples, sempre correto, mas paga o custo de uma agregação a mais na listagem de clientes (`ClientService.getClientsWithLastPurchase` já faz algo parecido: `findTopByClientIdOrderByPurchaseDateDesc` por cliente, então o padrão de query já existe no código).
2. Manter o cache, mas corrigi-lo para usar `purchaseDate` (não `createdAt`) e recalculá-lo (via `MAX`) tanto ao criar quanto ao excluir uma compra.

A opção 1 é mais alinhada com o precedente já estabelecido (`totalPurchaseValue`) e elimina de vez a classe de bug.

**Benefício:** "última compra" exibida ao operador deixa de poder mostrar uma data que não corresponde a nenhuma compra existente, ou que ignora um lançamento retroativo mais antigo que uma compra recente.

**Riscos da mudança:** baixo — é o mesmo padrão já testado e validado para o total.

**Impacto em dados existentes:** nenhum dado é perdido; a coluna `last_purchase_date` pode ser removida de `Client` (opção 1) sem perda de informação, já que é 100% derivável de `purchases`.

**Migration necessária:** sim, se a coluna for removida (`ALTER TABLE clients DROP COLUMN last_purchase_date`) — mudança de schema simples e reversível; ou nenhuma, se a opção 2 (só corrigir o cálculo) for escolhida.

**Dificuldade:** baixa.

---

### 3.6 Risco sistêmico de ENUM nativo do MySQL — só uma de ~13 colunas está protegida

**Como está hoje:** o histórico de migrations `V9 → V12 → V13 → V14 → V15` documenta, com riqueza de detalhes, um incidente real: renomear o valor `FAMÍLIA` para `FAMILIA` no enum Java `Category` quebrou em produção porque o Hibernate, ao criar a coluna `transactions.category` pela primeira vez, gerou um **ENUM nativo do MySQL** (lista fixa de valores permitidos gravada na definição da coluna) em vez de `VARCHAR`. `ddl-auto=update` nunca reescreve essa lista quando o enum Java muda — quatro migrations tentaram corrigir o **dado** (comparação de string, hex, BINARY, LENGTH×CHAR_LENGTH) antes de alguém descobrir que o problema era a **definição da coluna**, não o valor gravado. A correção definitiva (`V15`) converteu a coluna para `VARCHAR(32)` e, em paralelo, `Transaction.java` passou a declarar `columnDefinition = "VARCHAR(32)"` explicitamente no campo `category`, com um comentário longo explicando exatamente por quê.

**Problema:** essa proteção (`columnDefinition` explícito) foi aplicada **somente** em `Transaction.category`. Buscando todas as ocorrências de `@Enumerated(EnumType.STRING)` no projeto, nenhuma outra coluna tem esse mesmo cuidado:

- `User.role`
- `LoginAuditLog.failureReason`
- `LoginLockout.identifierType`
- `com.hortifruti.sl.hortifruti.model.billet.BilletFile.status`
- `CombinedScore.status`
- `TabelaPrecoCliente.status`
- `TabelaPrecoClienteItem.statusMatch`
- `CapturaNotaPendente.status`
- `CombinedScorePhotoFile.status`
- `Statement.bank` / `Statement.origin`
- `ClimateProduct.temperatureCategory`
- `FiscalNoteXmlStorage.status`

Todas essas colunas, se a tabela já existir com dados quando um valor do enum Java correspondente for renomeado, **vão reproduzir exatamente o mesmo incidente** — e o time já sabe, por experiência própria (4 migrations, vários dias de investigação documentados nos comentários), quanto isso custa para diagnosticar, porque o sintoma (`Data truncated for column`, ou pior, um `UPDATE` que roda sem erro mas não corrige nada) não aponta direto para a causa.

**Por que existe:** a correção foi feita pontualmente, no calor do incidente, para a coluna que estava quebrada — não houve (ou não há evidência de) uma varredura preventiva das demais colunas `@Enumerated(STRING)` depois de entender a causa raiz.

**É justificável?** Não — é exatamente o tipo de problema sistêmico que, uma vez entendido, vale a pena corrigir em todo lugar de uma vez, porque o custo de prevenção (adicionar `columnDefinition` a uma anotação já existente) é muito menor que o custo de descobrir de novo o mesmo bug.

**Modelagem ideal:** adicionar `columnDefinition = "VARCHAR(n)"` (com `n` = o `length` já declarado ou adequado) em todas as colunas listadas acima. Isso **não muda o comportamento observável do sistema nem os dados armazenados** quando a coluna já foi criada como `VARCHAR` (a maioria, já que muitas têm `length = ...` — o que também sugere ao Hibernate `VARCHAR`, mas não é garantia absoluta contra o mesmo problema em todo dialect/versão) — é puramente uma trava preventiva. Para colunas que já tenham sido criadas como ENUM nativo em bancos existentes (não é possível confirmar sem acesso ao MySQL real de hml/prod), seria necessário o mesmo tipo de migration de conversão feita em `V15`.

**Benefício:** elimina uma classe inteira de incidente futuro, coberta por 4 migrations de aprendizado que já foram pagas uma vez.

**Riscos da mudança:** baixíssimo para o código Java (é só anotação); a única parte que precisa de cautela é confirmar, para cada tabela, se a coluna já existe como ENUM nativo em produção antes de decidir se precisa de uma migration de conversão como a `V15`, ou se basta a anotação (proteção só "daqui para frente").

**Impacto em dados existentes:** nenhum, se a coluna já for `VARCHAR`; uma migration `MODIFY COLUMN ... VARCHAR(n)` é necessária caso a caso onde não for.

**Migration necessária:** possivelmente, uma por tabela, no mesmo padrão defensivo (guarda por `INFORMATION_SCHEMA`) já usado em `V15`/`V16`/`V17` — mas isso só pode ser confirmado inspecionando o schema real, não só o código.

**Dificuldade:** baixa por coluna; o trabalho é majoritariamente de verificação (checar o schema real de cada tabela antes de decidir se precisa de `MODIFY COLUMN`).

---

## 4. Achados de modelagem (moderados)

### 4.1 `Client.document` (CPF/CNPJ) sem `unique`, enquanto `Client.email` (opcional) tem

**Como está hoje:** `Client.email` é `@Column(unique = true)` (embora opcional — `nullable = true`); `Client.document`, o identificador fiscal/legal de fato do cliente (usado para emitir boleto no Sicoob e NF-e no Focus NFe — `BilletFactory`, `Recipient`), é só `@Column(nullable = false)`, sem `unique`.

**Problema:** nada no banco (nem no código) impede duas linhas de `Client` com o mesmo CPF/CNPJ. `ClientRepository.findByDocument` retorna `Optional<Client>` (assumindo, pelo tipo de retorno, que existe no máximo um resultado), mas isso é uma suposição do código, não uma garantia do schema.

**Por que existe:** provavelmente o `unique` em `email` foi adicionado cedo (é um campo mais "óbvio" de identidade num CRUD típico), e o `document` nunca recebeu o mesmo tratamento — possivelmente porque, na prática, o cadastro de clientes é feito por poucas pessoas e duplicidade nunca aconteceu.

**É justificável?** Não como está — para um sistema que emite documento fiscal por cliente, ter o e-mail (dado de contato, opcional) mais protegido que o documento fiscal (dado legal, obrigatório) é uma inversão de prioridade no modelo.

**Modelagem ideal:** `@Column(nullable = false, unique = true)` em `document`, normalizado (remover pontuação antes de persistir, para não permitir "123.456.789-00" e "12345678900" como "diferentes") — hoje a limpeza de máscara é feita ad-hoc em cada consumidor (`BilletFactory`, `Recipient`), o que é outro pequeno sintoma do mesmo problema: normalização deveria acontecer uma vez, na entrada de dado, não em cada leitura.

**Benefício:** impossível cadastrar o mesmo cliente fiscal duas vezes por engano, o que hoje fragmentaria histórico de compras, tabela de preço e cobrança entre dois IDs.

**Riscos da mudança:** exige checar/limpar duplicidades existentes antes de aplicar a constraint — se já houver duas linhas com o mesmo documento em produção, a migration falha até isso ser resolvido manualmente (decisão de negócio: qual das duas é a "certa"?).

**Impacto em dados existentes:** potencialmente alto se já existir duplicidade — precisa de investigação manual antes da migration, não é uma mudança "segura por padrão".

**Migration necessária:** sim (`ALTER TABLE clients ADD UNIQUE ...`), condicionada a uma auditoria de dados prévia.

**Dificuldade:** baixa no código; a parte de dado (achar e resolver duplicidades reais, se houver) pode ser trabalhosa dependendo do volume.

---

### 4.2 `InvoiceProduct`/`GroupedProduct` sem FK explícita para `FiscalProduct`

**Como está hoje:** `InvoiceProduct` (item de uma compra/nota) e `GroupedProduct` (item agregado de um agrupamento de cobrança) guardam `code` e `name` como `String` livre, copiados de `FiscalProduct.code`/`FiscalProduct.description` no momento da criação (via `ProdutoMatchingService` na extração por IA, ou diretamente de `fiscalProduct.getCode()`/`getDescription()` em `PurchaseService.createManualPurchase`/`addInvoiceProduct`). **Não existe** um campo `fiscalProductId` em nenhuma das duas entidades.

**Por que existe / é justificável:** isso é, em grande parte, uma decisão de design **correta**: itens de nota/cobrança precisam ser um retrato imutável do que foi vendido naquele momento (nome, preço) — se o catálogo mudar a descrição de um produto depois, o histórico de compras antigas não deveria mudar retroativamente. Esse é exatamente o padrão "snapshot vs. dado canônico" que o enunciado da tarefa pede para não confundir com redundância ruim — aqui, a cópia é **intencional e correta**.

**O que é, de fato, uma lacuna:** como não há nenhum FK (nem soft, por ID), a única forma de ligar um `InvoiceProduct`/`GroupedProduct` de volta ao `FiscalProduct` que ele representa é reconstruir a busca por `code` (que é `unique` em `FiscalProduct`, então funciona como chave natural estável — mas só enquanto ninguém excluir ou recodificar aquele produto no catálogo). Isso dificulta consultas analíticas (ex.: "todas as vendas históricas do produto X, mesmo que o código tenha mudado uma vez") e significa que excluir um `FiscalProduct` não deixa rastro de que ele existiu, além do texto solto em compras antigas.

**Modelagem ideal:** adicionar `fiscalProductId` (nullable, sem FK real — mantendo a convenção já usada no projeto) a `InvoiceProduct` e `GroupedProduct`, preenchido no momento da criação (o código que já sabe qual `FiscalProduct` foi usado só precisa gravar o ID também, além do `code`/`name`). Isso **não altera nenhum comportamento hoje observável** — é aditivo — e passa a permitir joins/analytics futuros sem quebrar a garantia de imutabilidade do snapshot histórico (`code`/`name`/`price` continuam sendo o que foi de fato vendido).

**Benefício:** rastreabilidade histórica sem abrir mão do snapshot; facilita relatórios por produto do catálogo atual.

**Riscos:** nenhum ao comportamento atual (campo aditivo, nullable).

**Impacto em dados existentes:** linhas antigas ficam com `fiscalProductId = NULL` (não é possível reconstruir retroativamente com certeza, já que o `code` pode ter mudado de dono) — aceitável, é só um enriquecimento a partir de agora.

**Migration necessária:** sim, simples (`ADD COLUMN fiscal_product_id BIGINT NULL` nas duas tabelas).

**Dificuldade:** baixa.

---

### 4.3 Tabela `products` (domínio clima) colide de nome com o catálogo fiscal

**Como está hoje:** `ClimateProduct` (`model/climate/ClimateProduct.java`) é mapeada para a tabela `products` — um nome genérico — e representa uma **categoria de produto para recomendação sazonal/climática** (`name`, `temperatureCategory`, meses de pico/baixa de venda), com CRUD independente (`service/climate/ProductService.java`) e busca por nome (`findByNameContainingIgnoreCase`). Não tem nenhuma relação estrutural (FK, ID) com `FiscalProduct` (tabela `fiscal_products`, o catálogo real usado em compras/preços). O vínculo entre os dois, quando existe, é feito só por comparação de texto em telas/relatórios do domínio climático — não foi encontrada nenhuma junção formal no backend entre os dois.

**Por que existe / é justificável:** os dois representam níveis de granularidade genuinamente diferentes e sem correspondência 1:1 — `ClimateProduct` é uma categoria ampla e curada manualmente ("Alface", "Melancia") pensada para recomendação (poucas dezenas de linhas), enquanto `FiscalProduct` é o catálogo fiscal detalhado por SKU (ex.: "ALFACE AMERIC KG", "ALFACE ROXA UNI" — potencialmente muitas variantes por categoria). Mantê-los como conceitos separados é **razoável e provavelmente correto** — forçar uma FK 1:N exigiria decidir, para cada `FiscalProduct`, qual `ClimateProduct` genérico ele pertence, um trabalho de curadoria manual que talvez nunca tenha sido priorizado.

**O que é, de fato, o problema:** só o nome da tabela (`products`, sem prefixo) e da entidade (`ClimateProduct`, mas em módulo `climate` que não deixa isso óbvio para quem lê o schema direto no banco). Alguém explorando o banco de dados sem contexto do código pode facilmente presumir que `products` é o catálogo principal.

**Modelagem ideal:** renomear a tabela para algo que deixe o domínio explícito no próprio schema — ex. `climate_products` ou `product_categories_climate` — mantendo a classe Java como está (só muda `@Table(name = ...)`).

**Benefício:** elimina ambiguidade para quem inspeciona o banco diretamente (DBA, ferramenta de BI, novo desenvolvedor).

**Riscos:** baixo — é um rename de tabela, comportamento do sistema não muda.

**Impacto em dados existentes:** nenhum (dado preservado, só nome muda).

**Migration necessária:** sim (`RENAME TABLE products TO climate_products`), trivial.

**Dificuldade:** baixa — o único cuidado é garantir que nenhum SQL nativo (fora do JPA) referencie `products` por nome literal em algum lugar não coberto pela busca de código-fonte.

---

## 5. Convenção de FK sem constraint real (`ddl-auto=update`): risco reconhecido, mas vale reafirmar o trade-off

Como descrito em §2, a decisão de usar `Long` cru + índice em vez de `@ManyToOne`/FK real é **deliberada e documentada no próprio código** (`V16`: "seguindo o padrão já usado no projeto"). Não é um achado novo tratá-la como problema — mas vale registrar, de forma independente, os dois lados reais desse trade-off, porque a auditoria encontrou consequências concretas dele (§3.2, §3.3):

- **A favor:** evita os dois problemas de `ddl-auto=update` que o próprio histórico de migrations documenta sofrer (V8: incapacidade de ampliar coluna BLOB; V15: ENUM nativo travado) — FKs reais adicionam ainda mais superfície onde `ddl-auto=update` pode se comportar de forma imprevisível ao evoluir o schema (ex.: `ON DELETE`/`ON UPDATE` não expressos em anotação JPA simples, ordem de criação de tabela por causa de dependência circular entre módulos). Também evita lock contention de FK em operações em lote.
- **Contra:** qualquer invariante de integridade referencial (não apagar um `Client` com `Purchase` associada, não deixar `Purchase.combinedScoreId` órfão, não duplicar vínculo) passa a depender **inteiramente** de disciplina no código de aplicação — e a auditoria já encontrou pelo menos um caso real onde essa disciplina falhou (§3.2).

**Recomendação, sem contradizer a decisão original:** manter a convenção (não vale a pena reverter um padrão consistente e já testado só por purismo), mas tratar cada uma dessas relações "soltas" como um contrato que precisa de teste de integração cobrindo o ciclo de vida completo (criar → cancelar/excluir → verificar que não sobrou referência morta) — o tipo de teste que teria pego o achado de §3.2 antes de chegar em produção.

---

## 6. O que foi investigado e **não deve ser mudado** (decisões de design válidas)

Para não tratar toda diferença estrutural como problema, os itens abaixo foram olhados com a mesma profundidade dos achados acima e concluídos como **corretos como estão**:

- **`Client.totalPurchaseValue` já foi removido da entidade** e substituído por uma agregação (`SUM`) calculada sob demanda em `PurchaseRepository`/`ClientService`/`ClientMapper` (com `@Mapping(target = "totalPurchaseValue", ignore = true)` explícito). É exatamente o padrão que se quer replicar para `lastPurchaseDate` (§3.5) — não uma redundância a corrigir, e sim o exemplo a seguir.
- **`GroupedProduct` vs. `InvoiceProduct`** não são a mesma coisa duplicada: `InvoiceProduct` é o item de uma nota individual; `GroupedProduct` é o resultado, **calculado por `GroupedProductService`**, de somar/ponderar `InvoiceProduct`s de várias compras num período, com preço fixo (soma simples) ou variável (média ponderada, com correção de arredondamento). É um snapshot de faturamento deliberado — mudar o catálogo ou uma compra antiga depois não deve, e não vai, alterar um agrupamento já fechado. Manter as duas tabelas separadas é correto.
- **`ClienteProdutoMapeamento` vs. `TabelaPrecoClienteItem.fiscalProductId`** também não são redundantes entre si, apesar de guardarem informação parecida: `TabelaPrecoClienteItem` é o registro histórico de **cada import** (uma linha por competência/versão, imutável depois de confirmada); `ClienteProdutoMapeamento` é a **memória corrente** "de/para" (cliente + código do cliente → produto fiscal), usada só para acelerar/automatizar o próximo import, e sua tabela tem `UNIQUE (cliente_id, codigo_produto_cliente)` (`V17`), reforçando que é, de fato, um mapeamento 1:1 correntemente válido, não um histórico. `TabelaPrecoClienteReviewService.marcarSemCorrespondencia` inclusive comenta explicitamente por que **não** apaga o mapeamento em determinado fluxo — decisão de negócio pensada, não descuido.
- **`ProdutoMatchingService` vs. `ProdutoClienteMatchingService`** (duas implementações de fuzzy-matching de produto) não são duplicação de código por acidente — o comentário em `ProdutoClienteMatchingService` explica que os dois lidam com entradas de qualidade diferente (texto de OCR de nota manuscrita, com sinais de unidade/quantidade separados, vs. texto digitado limpo de planilha) e compartilham o núcleo comum via `FuzzyTextMatchUtils`, só divergindo no cálculo de score. Consolidar os dois forçaria uma abstração prematura para dois problemas parecidos, mas não idênticos.
- **`ConversaoCaixaService` + `ProductBoxWeightHistory`**: `FiscalProduct.pesoCaixaKg` é o valor corrente, e `ProductBoxWeightHistory` é uma tabela de auditoria histórica de cada mudança (`pesoAnterior`/`pesoNovo`/`origem`/`criadoEm`), gravada a cada import — um padrão de auditoria correto (valor corrente na entidade principal + histórico apend-only separado), não uma cópia solta do mesmo dado.
- **`CapturaNotaPendente.extracaoJson`** (JSON serializado da extração por IA) é um dado de estágio (inbox), descartável depois de confirmado (`Purchase`/`InvoiceProduct` viram a fonte canônica) — não há risco real de divergência porque o registro de captura não é consultado como fonte de verdade após a confirmação.
- **Convenção de nomear tabelas em português** (`tabelas_preco_cliente`, `cliente_produto_mapeamento`, `capturas_nota_pendentes`) misturada com entidades legadas em inglês (`purchases`, `clients`, `combined_scores`) é uma inconsistência estética de convenção de nomes, não um problema de modelagem de dados — não há ambiguidade semântica nem risco de dado, então não foi tratada como achado.

---

## 7. Recomendações priorizadas

| Prioridade | Ação | Tipo de mudança | Migration? | Esforço |
|---|---|---|---|---|
| 1 | Corrigir `SicoobOpenBilletReconciler`/`updateStatus` para sempre sincronizar `hasBillet`/`hasInvoice` junto com `status` (§3.1) | Fix de comportamento | Não (ou backfill pontual de dado já inconsistente) | Baixo |
| 2 | Zerar `Purchase.combinedScoreId` ao excluir/cancelar um `CombinedScore` (§3.2) | Fix de comportamento | Backfill opcional | Baixo |
| 3 | Impedir que uma `Purchase` já vinculada a um agrupamento ativo entre em outro (§3.3) | Validação nova | Não | Baixo-Médio |
| 4 | Adicionar `columnDefinition=VARCHAR(n)` em todas as colunas `@Enumerated(STRING)` que ainda não têm (§3.6) | Trava preventiva | Caso a caso, após checar schema real | Baixo (por coluna) |
| 5 | Migrar `ClientBusinessRules` para colunas em `Client` (§3.4) | Normalização de dado | Sim | Médio |
| 6 | Substituir `Client.lastPurchaseDate` por cálculo derivado, no mesmo padrão já usado para `totalPurchaseValue` (§3.5) | Remoção de redundância | Sim (drop de coluna) ou ajuste de cálculo | Baixo |
| 7 | `UNIQUE` em `Client.document`, após checagem/limpeza de duplicidade existente (§4.1) | Constraint de integridade | Sim | Baixo (código) / variável (dado) |
| 8 | Adicionar `fiscalProductId` nullable a `InvoiceProduct`/`GroupedProduct` (§4.2) | Enriquecimento aditivo | Sim | Baixo |
| 9 | Renomear tabela `products` → `climate_products` (§4.3) | Clareza de nomenclatura | Sim | Baixo |

Os itens 1–3 são os únicos com risco financeiro/operacional direto (cobrança/boleto/NF) e deveriam ser tratados como prioridade real, independentemente de quando o resto for planejado. Os demais são dívida técnica genuína, mas de impacto mais contido e sem urgência.

---

## 8. Observação final sobre o processo desta auditoria

Este documento foi produzido lendo diretamente as ~40 classes de entidade, todos os repositórios do domínio `purchase`/`product`, os services centrais de compra/agrupamento/tabela de preço/matching, as 17 migrations Flyway e a configuração de banco/Flyway em `application.properties`. Trechos de código foram citados com caminho de arquivo para permitir verificação rápida. Onde a auditoria não teve acesso a informação que só existe em runtime (schema real de hml/prod, volume de dados, existência atual de duplicidade de `document`), isso foi declarado explicitamente em vez de presumido — em particular, os achados de §3.6 e §4.1 dependem de confirmação contra o banco real antes de qualquer migration ser escrita.
