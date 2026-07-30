# com.hortifruti.sl.hortifruti.service.invoice.factory

Monta as três partes do payload de emissão de NF-e enviado à Focus NFe: itens, destinatário e o
payload JSON final (incluindo o novo cálculo de IBS/CBS da Reforma Tributária, obrigatório a partir
de 01/04/2026).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvoiceItem.java` | `@Component` | Converte `GroupedProduct` em `ItemRequest`, resolvendo NCM/CFOP/ICMS via `ProductNFService`; ajusta CFOP de série 5xxx para 6xxx quando o destinatário é de fora de MG. |
| `InvoicePayload.java` | `@Component` | Serializa o `IssueInvoiceRequest` no JSON esperado pela Focus NFe; calcula IBS/CBS por item usando a classificação tributária resolvida por `IbsCbsClassificador` a partir do NCM, com arredondamento em 2 casas por item e totais do raiz somados diretamente (soma de valores já arredondados, sem redistribuição de resíduo). |
| `IbsCbsClassificador.java` | `@Component` | Decide CST/cClassTrib e alíquotas de CBS/IBS UF/IBS Mun por item a partir do NCM: produtos hortícolas, frutas e ovos (capítulos 07, 08 e NCM 0407 — Anexo XV da LC 214/2025) têm alíquota zero (CST 200 / cClassTrib 200014); os demais seguem tributação integral (CST 000 / cClassTrib 000001). |
| `IbsCbsClassificacao.java` | `record` | CST, cClassTrib e alíquotas de CBS/IBS UF/IBS Mun retornados por `IbsCbsClassificador` para um item. |
| `Recipient.java` | `@Component` | Monta `RecipientRequest` a partir do `Client`: normaliza CPF/CNPJ (aceitando letras a partir de ago/2026), faz parsing heurístico do campo de endereço em texto livre (rua, número, bairro, cidade, UF via regex) e aplica regra especial de faturamento (cliente "APTA" sempre como SP). |
