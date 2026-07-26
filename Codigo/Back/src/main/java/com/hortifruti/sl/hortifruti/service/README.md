# com.hortifruti.sl.hortifruti.service

Pacote raiz de toda a lógica de negócio do backend. Não contém classes próprias — apenas subpacotes, cada um responsável por um domínio do Sistema de Gestão Hortifruti Santa Luzia. Cada subpacote tem seu próprio README com detalhes de arquivos e regras de negócio.

## Subpacotes

- `backup/` — backup periódico de dados (compras, transações, extratos) para CSV enviado ao Google Drive; possui subpacotes `auth/`, `folders/` e `oauth/` para autenticação/OAuth2 e manipulação de pastas/arquivos do Drive (ver `backup/README.md`).
- `billet/` — emissão, consulta, baixa e conciliação de boletos via API do Sicoob (mTLS) (ver `billet/README.md`).
- `climate/` — recomendação de produtos hortifrúti conforme previsão do tempo (OpenWeather) e sazonalidade, além do CRUD de produtos climáticos (ver `climate/README.md`).
- `dashboard/` — agregações e métricas de receita, custo, margem, fluxo de caixa e ranking de produtos/categorias para o painel administrativo (ver `dashboard/README.md`).
- `finance/` — extratos bancários, transações e saldo do Banco do Brasil/Sicoob (documentado por outra tarefa).
- `freight/` — cálculo de frete a partir de distância/tempo (Google Maps Distance Matrix) e configuração de custos operacionais (ver `freight/README.md`).
- `invoice/` — emissão e consulta de notas fiscais via Focus NFe (documentado por outra tarefa).
- `notification/` — envio de e-mails (SendGrid) e mensagens WhatsApp (Ultramsg) (documentado por outra tarefa).
- `purchase/` — cadastro de clientes, processamento de compras (PDF), agrupamento de produtos e cálculo de vencimento/CombinedScore (ver `purchase/README.md`).
- `storage/` — upload/download/movimentação de arquivos (PDFs de boletos) no Cloudflare R2 (ver `storage/README.md`).
- `user/` — cadastro, atualização e consulta de usuários e papéis (autenticação/JWT tratados em outro pacote) (ver `user/README.md`).
