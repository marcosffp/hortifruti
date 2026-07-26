# com.hortifruti.sl.hortifruti.dto.invoice

DTOs do módulo de notas fiscais, que integra com a API da Focus NFe para emissão, consulta e
armazenamento de NF-e. Inclui o payload de emissão (destinatário, endereço, itens), as diversas
formas de resposta (completa, simplificada, com boleto anexado) e os dados de armazenamento do XML
fiscal. Subpacote `tax` concentra os DTOs de detalhamento tributário.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AddressRequest.java` | record | Endereço do destinatário da nota, com `@JsonProperty` mapeando para os nomes de campo da Focus NFe (`logradouro`, `numero`, `bairro`, `municipio`, `uf`, `cep`, `codigo_municipio`, `codigo_pais`, `nome_pais`) e validação `@NotBlank`/`@Size` nos campos obrigatórios. |
| `FiscalNoteXmlStorageResponse.java` | record | Metadados de uma NF-e armazenada (XML): id, referência, número, nome do cliente, valor total, data de emissão e data de criação do registro. |
| `InvoiceResponse.java` | record | Resposta mínima da Focus NFe (`ref`, `status`), com `@JsonIgnoreProperties(ignoreUnknown = true)` para tolerar campos extras não mapeados. |
| `InvoiceResponseGet.java` | record | Resposta de consulta de nota fiscal com nome, valor total, status, data, número e referência. |
| `InvoiceResponseSimplif.java` | record | Resposta simplificada de nota fiscal mapeando diretamente campos da Focus NFe via `@JsonProperty` (`cnpj_destinatario`, `valor_total`, `numero`, `status`, `data_emissao`, `ref`). |
| `InvoiceWithBilletResponse.java` | record | Resposta combinando nota fiscal e boleto: referência/número da nota, número do boleto e os arquivos em base64 (DANFE, XML e boleto). |
| `IssueInvoiceRequest.java` | record | Payload de request para emissão de nota fiscal: id da compra (`combinedScore_id`), natureza da operação, data de emissão, destinatário (`RecipientRequest`), lista de itens (`ItemRequest`) e informações adicionais ao contribuinte. Validação `@NotNull`/`@NotBlank`/`@NotEmpty` nos campos obrigatórios. |
| `ItemRequest.java` | record | Item de uma nota fiscal a emitir: código, descrição, NCM, CFOP, unidade e quantidade comercial, valores unitário/bruto, dados tributáveis opcionais (unidade/quantidade/valor tributável) e situações tributárias de ICMS/PIS/COFINS. Validado com `@NotBlank`/`@NotNull`/`@Positive`/`@PositiveOrZero`. |
| `OpenInvoiceResponse.java` | record | Representa uma nota fiscal em aberto vinculada a uma compra: id da compra e do cliente, nome do cliente, valor total, data de confirmação, vencimento e referência da nota (`invoiceRef`). |
| `RecipientRequest.java` | record | Dados do destinatário da nota fiscal: CNPJ/CPF, nome (`@NotBlank`), nome fantasia, telefone, e-mail, endereço (`AddressRequest`, `@NotNull`), inscrição estadual e seu indicador. |

## Subpacotes

- `tax/` — detalhes tributários (ICMS/PIS/COFINS) de notas fiscais e seus itens.
- `tax/icms` — relatório consolidado de ICMS sobre vendas.
- `tax/registerReport` — detalhes de resumo de notas para relatório de registro fiscal.
- `tax/sales` — detalhes de resumo de vendas para relatório.
