# com.hortifruti.sl.hortifruti.service.billet

Domínio de boletos bancários: emissão, segunda via, baixa/cancelamento, consulta e conciliação junto à API de cobrança bancária do Sicoob (v3, via mTLS em `BilletHttpClient`). `BilletService` é a fachada única usada pelo `BilletController`; as demais classes têm papéis explicitamente segregados (ver `package-info.java`) para isolar o tratamento de erro específico da API do Sicoob.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BilletService.java` | `@Service` | Fachada pública do domínio: orquestra emissão (`generateBillet`, com lock pessimista via `findByIdForUpdate` para evitar duplo clique), listagem de boletos em aberto (delega a conciliação real ao Sicoob para `SicoobOpenBilletReconciler`), baixa manual de pagamento (`markBilletAsPaid`) e sincronização de vencidos (`syncAndFindOverdueUnpaidScores`). |
| `BilletIssue.java` | `@Component` | Só emissão e segunda via de boleto via API do Sicoob; converte a resposta (PDF em Base64) em bytes e monta o mapa de retorno com `nossoNumero`/`seuNumero`. |
| `BilletCancel.java` | `@Component` | Só baixa/cancelamento de boleto via API do Sicoob, tanto vinculado a um `CombinedScore` (`cancelBillet`) quanto avulso por "nosso número" (`cancelBilletByNumber`), com atualização best-effort do registro local. |
| `BilletQuery.java` | `@Component` | Só consulta: lista boletos em aberto/filtrados de um pagador e busca um boleto específico por "nosso número", convertendo o JSON da API em `BilletResponse`. |
| `SicoobOpenBilletReconciler.java` | `@Component` | Confirma no Sicoob, por cliente, quais `CombinedScore` marcados localmente como pendentes/com boleto realmente seguem "Em aberto"; corrige o status local (pago/cancelado) quando a confirmação individual resolve a situação. |
| `BilletFactory.java` | `@Component` | Monta os DTOs de requisição (`BilletRequest`, `Pagador`) a partir das entidades `CombinedScore`/`Client`, incluindo o parsing/validação rígida do endereço do cliente (limites de 40/30 caracteres exigidos pelo Sicoob). |
| `BilletValidation.java` | `@Component` | Validações curtas reaproveitadas entre `BilletIssue`/`BilletCancel`/`BilletQuery`: garante que o agrupamento tem boleto associado e que a resposta HTTP não está vazia. |
| `BilletInfoCombinedAndClient.java` | `@Component` | Único ponto de acesso a `CombinedScoreRepository`/`ClientRepository` dentro do domínio de boleto. |
| `BilletConstants.java` | `@Component` | Constantes de configuração da API do Sicoob (número de cliente/conta via `@Value`, código de modalidade, URL base) usadas por várias classes do pacote. |
| `PdfCreate.java` | `@Component` | Utilitário de decodificação/empacotamento do PDF do boleto (Base64 → bytes → `ResponseEntity` para download). |
| `package-info.java` | documentação de pacote | Explica o papel de cada classe e por que não foram fundidas em menos classes "padrão" — consulte antes de adicionar uma nova classe ao domínio de boleto. |
