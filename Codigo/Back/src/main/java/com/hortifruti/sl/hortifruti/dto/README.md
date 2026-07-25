# com.hortifruti.sl.hortifruti.dto

Pacote raiz dos DTOs (Data Transfer Objects) da aplicação. Não contém classes diretamente — apenas
organiza os DTOs de request/response usados pelos controllers e services em subpacotes temáticos,
um por domínio de negócio (financeiro, boletos, notas fiscais, clima, frete, notificações, compras,
clientes, integrações bancárias e usuários).

## Subpacotes

- `backup/` — resposta simples de operações de backup (Google Drive).
- `bb/` — linhas e resumo de importação de extrato do Banco do Brasil.
- `billet/` — request/response de emissão e consulta de boletos (Sicoob).
- `climate/` — DTOs de clima/previsão do tempo e recomendação de produtos por sazonalidade.
- `finance/` — saldo bancário, extratos e transações financeiras.
- `freight/` — cálculo de frete e distância (Google Maps).
- `invoice/` — emissão e consulta de notas fiscais (Focus NFe).
- `invoice/tax/` — detalhes tributários de notas fiscais e itens.
- `invoice/tax/icms` — relatório consolidado de ICMS sobre vendas.
- `invoice/tax/registerReport` — detalhes de resumo de notas para relatório de registro.
- `invoice/tax/sales` — detalhes de resumo de vendas.
- `notification/` — requests/responses de envio de notificações (e-mail/WhatsApp).
- `purchase/` — compras, boletos avulsos e produtos agrupados/da nota.
- `purchase/client` — cadastro e consulta de clientes.
- `sicoob/` — espelhamento das respostas da API de extrato do Sicoob.
- `user/` — autenticação e cadastro de usuários do sistema.
