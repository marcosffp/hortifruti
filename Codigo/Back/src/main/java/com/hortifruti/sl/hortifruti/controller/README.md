# com.hortifruti.sl.hortifruti.controller

Pacote raiz dos controllers REST da aplicação. Não contém classes próprias: todos os endpoints ficam organizados em subpacotes por domínio de negócio.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| (nenhum arquivo direto) | - | - |

## Subpacotes

- `backup/` — endpoints de backup do banco para o Google Drive e callback OAuth2 (ver backup/README.md).
- `billet/` — emissão, consulta, cancelamento e baixa de boletos Sicoob (ver billet/README.md).
- `chatbot/` — webhook e testes do chatbot de WhatsApp via UltraMsg (ver chatbot/README.md).
- `climate/` — clima, catálogo de produtos e recomendações por temperatura (ver climate/README.md).
- `dashboard/` — dados agregados do dashboard gerencial (ver dashboard/README.md).
- `finance/` — saldo bancário, extratos e transações (ver finance/README.md).
- `freight/` — cálculo de distância/frete e configuração de frete (ver freight/README.md).
- `invoice/` — emissão/cancelamento de notas fiscais (Focus NFe) e relatórios de ICMS (ver invoice/README.md).
- `notification/` — envio de notificações e documentos por email/WhatsApp (ver notification/README.md).
- `purchase/` — clientes, compras, agrupamentos (CombinedScore) e produtos de nota (ver purchase/README.md).
- `user/` — autenticação e gestão de usuários (ver user/README.md).
