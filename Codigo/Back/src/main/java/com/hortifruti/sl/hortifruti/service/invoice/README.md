# com.hortifruti.sl.hortifruti.service.invoice

Emissão, consulta, cancelamento e download (DANFE/XML) de notas fiscais eletrônicas (NF-e) via API
externa Focus NFe. Também orquestra a emissão combinada de NF-e + boleto e mantém uma cópia local
(R2) dos arquivos fiscais para reduzir dependência de consultas repetidas à Focus NFe.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DanfeXmlService.java` | `@Service` | Baixa DANFE (PDF) e XML da NF-e na Focus NFe, com retry/backoff enquanto a nota está "processando"; usa cache local via `FiscalNoteXmlStorageService` antes de baixar ao vivo. |
| `FiscalNoteXmlStorageService.java` | `@Service` | Persiste XML/DANFE de cada NF no R2 (bucket `notas-fiscais`) após emissão (polling assíncrono até status "autorizado") ou como rede de segurança no download; usa lock por `ref` e trata colisões de índice único para evitar upload duplicado órfão; move arquivo para pasta "canceladas" ao cancelar NF. |
| `InvoiceCancelService.java` | `@Service` | Cancela NF-e na Focus NFe (com suporte a cancelamento extemporâneo); atualiza `CombinedScore` e XML armazenado de forma best-effort, tolerando cancelamento avulso sem registro local. |
| `InvoiceQuery.java` | `@Component` | Consulta status/dados de uma NF-e na Focus NFe e monta `InvoiceResponseGet`; extrai detalhes fiscais (ICMS, itens, CFOP) com cache indefinido por `ref` e retry, usado pelos relatórios fiscais mensais. |
| `InvoiceService.java` | `@Service` | Fachada que delega emissão, consulta, download, cancelamento e listagem de NFs para os serviços especializados do pacote. |
| `IssueInvoice.java` | `@Service` | Monta e envia o payload de emissão de NF-e para a Focus NFe a partir de um `CombinedScore`; trava a linha do agrupamento (`SELECT FOR UPDATE`) para evitar emissão duplicada por duplo clique; aplica regra de texto customizado por cliente (`ClientBusinessRules`). |
| `IssueInvoiceWithBilletService.java` | `@Service` | Orquestra emissão de NF seguida da emissão do boleto vinculado (número do boleto = número da NF); faz polling até a NF ser autorizada e cancela automaticamente (rollback) a NF já emitida se qualquer etapa seguinte falhar. |
| `ProductNFService.java` | `@Service` | Carrega o catálogo fiscal de produtos (NCM, CFOP, ICMS, unidades) de `products.yml` e resolve por código de produto. |

## Subpacotes

- `factory/` — construção de itens, payload FocusNfe (com IBS/CBS 2026) e destinatário da NF-e (ver factory/README.md)
- `tax/` — geração dos relatórios fiscais mensais (ICMS, pagamento, registro, vendas, XMLs de saída) (ver tax/README.md)
