# com.hortifruti.sl.hortifruti.controller.billet

Endpoints para emissão, consulta, segunda via, download, cancelamento e baixa manual de boletos bancários (Sicoob), vinculados a um `CombinedScore`.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `BilletController.java` | `@RestController` (`/billet`) | `GET /billet/generate/{combinedScoreId}` emite o boleto e retorna o PDF; `GET /billet/client/{clientId}` lista boletos de um pagador (com filtros opcionais de situação e período); `GET /billet/open` lista todos os boletos em aberto; `GET /billet/issue-copy/{idCombinedScore}` emite segunda via em PDF; `GET /billet/{combinedScoreId}/file` baixa o PDF já armazenado no bucket (R2) sem reemitir; `POST /billet/cancel/{idCombinedScore}` cancela (baixa) o boleto pelo CombinedScore; `POST /billet/cancel-by-number` cancela pelo "nosso número" avulso; `GET /billet/{combinedScoreId}` retorna detalhes do boleto do agrupamento; `PATCH /billet/mark-paid/{combinedScoreId}` marca o agrupamento como pago manualmente. |
