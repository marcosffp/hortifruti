/**
 * Papel de cada classe deste pacote (ver convenção geral em {@code ARQUITETURA.md} na raiz do
 * backend). Documentado explicitamente em vez de forçar uma fusão das 8 classes em menos papéis
 * "padrão" (XService/XClient/XRepository) porque a lógica de emissão/baixa/consulta de boleto tem
 * bastante tratamento de erro específico da API do Sicoob — fundir sem testes de regressão é risco
 * maior do que o ganho de organização.
 *
 * <ul>
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletService} — fachada pública do
 *       domínio (é o único ponto usado por {@code BilletController}); orquestra as classes abaixo
 *       e concentra a regra de negócio que não é específica de uma chamada HTTP (ex.: {@code
 *       markBilletAsPaid}, {@code generateBillet}).
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletIssue} — só emissão/segunda via de
 *       boleto via API do Sicoob.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletCancel} — só baixa/cancelamento de
 *       boleto via API do Sicoob.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletQuery} — só consulta (listagem e
 *       busca individual) de boletos via API do Sicoob.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.SicoobOpenBilletReconciler} — regra de
 *       conciliação entre o status local do {@code CombinedScore} e a situação real do boleto no
 *       Sicoob; extraída de {@code BilletService} para isolar essa regra da orquestração de
 *       listagem/paginação.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletFactory} — monta os payloads de
 *       requisição (DTOs) para a API do Sicoob a partir das entidades do domínio.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletValidation} — validações curtas e
 *       reaproveitadas entre {@code BilletIssue}/{@code BilletCancel}/{@code BilletQuery} (ex.:
 *       garantir que o agrupamento tem boleto associado antes de chamar a API).
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletInfoCombinedAndClient} — único
 *       ponto de acesso a {@code CombinedScoreRepository}/{@code ClientRepository} dentro do
 *       domínio de boleto, evitando que cada classe acesse esses repositórios diretamente.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.BilletConstants} — constantes de
 *       configuração da API do Sicoob (número de cliente, conta, URL base) usadas por várias
 *       classes acima.
 *   <li>{@link com.hortifruti.sl.hortifruti.service.billet.PdfCreate} — utilitário de
 *       decodificação/empacotamento do PDF (Base64 → bytes → {@code ResponseEntity}).
 * </ul>
 *
 * Não crie uma nova classe solta pra boleto sem encaixar num destes papéis (ou atualizar esta
 * lista com a justificativa).
 */
package com.hortifruti.sl.hortifruti.service.billet;
