# com.hortifruti.sl.hortifruti.service.invoice.factory

Monta as três partes do payload de emissão de NF-e enviado à Focus NFe: itens, destinatário e o
payload JSON final (incluindo o novo cálculo de IBS/CBS da Reforma Tributária, obrigatório a partir
de 01/04/2026).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `InvoiceItem.java` | `@Component` | Converte `GroupedProduct` em `ItemRequest`, resolvendo NCM/CFOP/ICMS via `ProductNFService`; ajusta CFOP de série 5xxx para 6xxx quando o destinatário é de fora de MG. |
| `InvoicePayload.java` | `@Component` | Serializa o `IssueInvoiceRequest` no JSON esperado pela Focus NFe; calcula IBS/CBS por item (CBS 0,9%, IBS UF 0,1%, IBS Mun 0%) com arredondamento em 2 casas e distribuição do resíduo por "largest remainder" para evitar rejeição da SEFAZ (erros 1076/1080). |
| `Recipient.java` | `@Component` | Monta `RecipientRequest` a partir do `Client`: normaliza CPF/CNPJ (aceitando letras a partir de ago/2026), faz parsing heurístico do campo de endereço em texto livre (rua, número, bairro, cidade, UF via regex) e aplica regra especial de faturamento (cliente "APTA" sempre como SP). |
