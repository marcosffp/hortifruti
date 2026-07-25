# com.hortifruti.sl.hortifruti.controller.invoice

Endpoints de emissão, consulta, cancelamento e download de notas fiscais eletrônicas via Focus NFe, incluindo emissão combinada com boleto, armazenamento de XML e relatórios mensais de ICMS.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `InvoiceController.java` | `@RestController` (`/invoices`) | `POST /invoices/issue/{combinedScoreId}` emite a NF-e; `POST /invoices/issue-with-billet/{combinedScoreId}` emite NF-e e o boleto vinculado em sequência, cancelando a NF automaticamente se etapa posterior falhar; `GET /invoices/open` lista agrupamentos com NF emitida mas sem boleto, pendentes de confirmação manual; `GET /invoices/consulta/{ref}` consulta status da NF; `GET /invoices/{ref}/danfe` e `/xml/download` baixam DANFE e XML da Focus NFe; `DELETE /invoices/{ref}/cancel` cancela a NF-e por referência (com `justificativa` e flag opcional `extemporaneo` para cancelamento fora do prazo); `GET /invoices/xml-storage` lista XMLs armazenados por período; `GET /invoices/xml-storage/{ref}/download` baixa o XML armazenado localmente. |
| `ReportTaxController.java` | `@RestController` (sem `@RequestMapping` de classe) | `GET /icms-report/monthly/{start}/{end}` gera e retorna um ZIP com relatórios mensais de ICMS para o período informado, via `ReportTaxService`. |
