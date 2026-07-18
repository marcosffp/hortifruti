# Documentação das APIs Sicoob — Portal Developers

> Fonte: [developers.sicoob.com.br/portal/apis](https://developers.sicoob.com.br/portal/apis)
> Documento compilado a partir do conteúdo colado das abas do portal.

## Índice

- [Cobrança Bancária](#cobrança-bancária)

---

## Cobrança Bancária

**Base URL:** `https://api.sicoob.com.br/cobranca-bancaria/v3`

A Cobrança Bancária Sicoob é um conjunto de serviços oferecidos aos associados para recebimento de valores referentes a vendas de produtos e serviços, por meio de boletos de cobrança pagos em toda a rede bancária. A API disponibiliza funcionalidades para gestão da carteira registrada: inclusão de novos boletos, alteração de informações relevantes, protesto/negativação de títulos vencidos e não pagos, e liquidação ou baixa do título.

### Boleto — Inclusão e Manutenção

#### `POST /boletos` — Incluir boletos

Serviço para inclusão de boletos. Permite a inclusão de 1 boleto por requisição.

**Parâmetros**

| Nome | Tipo | Local | Descrição |
|---|---|---|---|
| `boleto` * | object | body | Inclusão das informações detalhadas do boleto de cobrança |

**Body de exemplo**

```json
{
  "numeroCliente": 25546454,
  "codigoModalidade": 1,
  "numeroContaCorrente": 123456,
  "codigoEspecieDocumento": "DM",
  "dataEmissao": "2025-09-01",
  "nossoNumero": "0123456789012345678",
  "seuNumero": "012345678901234567",
  "identificacaoBoletoEmpresa": "01234567890123456789",
  "identificacaoEmissaoBoleto": 1,
  "identificacaoDistribuicaoBoleto": 1,
  "valor": 156.23,
  "dataVencimento": "2025-09-25",
  "dataLimitePagamento": "2025-11-05",
  "valorAbatimento": 5.57,
  "tipoDesconto": 1,
  "dataPrimeiroDesconto": "2025-09-15",
  "valorPrimeiroDesconto": 22.5,
  "dataSegundoDesconto": "2025-09-20",
  "valorSegundoDesconto": 15.5,
  "dataTerceiroDesconto": "2025-09-24",
  "valorTerceiroDesconto": 10.5,
  "tipoMulta": 1,
  "dataMulta": "2025-11-05",
  "valorMulta": 5.5,
  "tipoJurosMora": 1,
  "dataJurosMora": "2025-11-05",
  "valorJurosMora": 4.5,
  "numeroParcela": 1,
  "aceite": true,
  "codigoNegativacao": 2,
  "numeroDiasNegativacao": 60,
  "codigoProtesto": 1,
  "numeroDiasProtesto": 30,
  "pagador": {
    "numeroCpfCnpj": "11122233300",
    "nome": "Nome completo do pagador X",
    "endereco": "Endereço do pagador X",
    "bairro": "Bairro do pagador X",
    "cidade": "Cidade do pagador X",
    "cep": "00000000",
    "uf": "OU",
    "email": "pagador@dominio.com.br"
  },
  "beneficiarioFinal": {
    "numeroCpfCnpj": "11122233300",
    "nome": "Beneficiário Y"
  },
  "mensagensInstrucao": [
    "Descrição da Instrução 1",
    "Descrição da Instrução 2",
    "Descrição da Instrução 3",
    "Descrição da Instrução 4",
    "Descrição da Instrução 5"
  ],
  "gerarPdf": false,
  "rateioCreditos": [
    {
      "numeroBanco": 33,
      "numeroAgencia": 1,
      "numeroContaCorrente": "987654",
      "contaPrincipal": true,
      "codigoTipoValorRateio": 1,
      "valorRateio": "100",
      "codigoTipoCalculoRateio": 1,
      "numeroCpfCnpjTitular": "11122233300",
      "nomeTitular": "Nome completo do titular X",
      "codigoFinalidadeTed": "10",
      "codigoTipoContaDestinoTed": "CC",
      "quantidadeDiasFloat": 1,
      "dataFloatCredito": "2020-12-30"
    }
  ],
  "codigoCadastrarPIX": 1,
  "numeroContratoCobranca": 1
}
```

**Respostas**

| Código | Descrição |
|---|---|
| 200 | Solicitação recebida com sucesso (retorna objeto `resultado` com dados do boleto incluído, incluindo `codigoBarras`, `linhaDigitavel`, `pdfBoleto` em Base64 e `qrCode` Pix quando aplicável) |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

Formato de erro (400/406/500):
```json
{
  "mensagens": [
    { "mensagem": "string", "codigo": "string" }
  ]
}
```

---

#### `GET /boletos` — Consultar boleto

Consulta um boleto bancário usando as informações do beneficiário logado (cooperativa, identificador do beneficiário, conta corrente) junto com o identificador do boleto (nosso número), linha digitável ou código de barras.

**Parâmetros (query)**

| Nome | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `numeroCliente` | integer | Sim | Número que identifica o contrato do beneficiário no Sisbr |
| `codigoModalidade` | integer | Sim | 1 - Simples com registro · 3 - Caucionada · 4 - Vinculada · 5 - Carnê de pagamentos · 8 - Cobrança conta capital |
| `nossoNumero` | integer | Não | Identificador do boleto no Sisbr (dispensa linha digitável/código de barras) |
| `linhaDigitavel` | string | Não | Linha digitável (47 posições) |
| `codigoBarras` | string | Não | Código de barras (44 posições) |
| `numeroContratoCobranca` | integer (int32) | Não | Id do contrato de cobrança |

**Respostas**

| Código | Descrição |
|---|---|
| 200 | Retorna objeto `resultado` completo do boleto, incluindo `situacaoBoleto`, `listaHistorico` e `qrCode` |
| 204 | Requisição processada com êxito, sem conteúdo |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

#### `GET /pagadores/{numeroCpfCnpj}/boletos` — Listar boletos por pagador

**Parâmetros**

| Nome | Tipo | Local | Obrigatório | Descrição |
|---|---|---|---|---|
| `numeroCpfCnpj` | string | path | Sim | CPF ou CNPJ do pagador (máx. 14) |
| `numeroCliente` | integer | query | Sim | Número que identifica o contrato do beneficiário no Sisbr |
| `codigoSituacao` | integer | query | Não | 1 - Entrada normal · 2 - Baixado · 3 - Liquidado |
| `dataInicio` | string ($date) | query | Não | Data de vencimento inicial (`yyyy-MM-dd`) |
| `dataFim` | string ($date) | query | Não | Data de vencimento final (`yyyy-MM-dd`) |

**Respostas**

| Código | Descrição |
|---|---|
| 200 | Retorna `resultado` como array de boletos do pagador |
| 204 | Requisição processada com êxito, sem conteúdo |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

#### `GET /boletos/segunda-via` — Emitir segunda via de um boleto

Emite a segunda via de um boleto já registrado, usando as informações do beneficiário logado junto com o identificador do boleto (nosso número, linha digitável ou código de barras). Quando código de barras ou linha digitável são informados, a pesquisa é feita prioritariamente por eles.

**Parâmetros (query)**

| Nome | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `numeroCliente` | integer | Sim | Número que identifica o contrato do beneficiário no Sisbr |
| `codigoModalidade` | integer | Sim | 1 - Simples com registro · 3 - Caucionada · 4 - Vinculada · 5 - Carnê de pagamentos · 8 - Cobrança conta capital |
| `nossoNumero` | integer | Não | Identificador do boleto no Sisbr |
| `linhaDigitavel` | string | Não | Linha digitável (47 posições) |
| `codigoBarras` | string | Não | Código de barras (44 posições) |
| `gerarPdf` | boolean | Não | Se `true`, devolve o PDF do boleto em Base64 |
| `numeroContratoCobranca` | integer ($int64) | Não | Id do contrato de cobrança |

**Respostas**

| Código | Descrição |
|---|---|
| 200 | Retorna `resultado` com dados do boleto e, se solicitado, `pdfBoleto` em Base64 |
| 204 | Requisição processada com êxito, sem conteúdo |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

#### `GET /boletos/faixas-nosso-numero` — Consultar faixas de nosso número disponíveis

Consulta faixas de "nosso número" disponíveis para uso.

> Quando `validaDigitoVerificadorNossoNumero` retorna `false`, a faixa `numeroInicial`/`numeroFinal` refere-se à numeração final (ex.: 10 e 15 → uso 1-0, 1-1, 1-2, 1-3, 1-4, 1-5).
> Quando retorna `true`, o dígito verificador (DV) deve ser calculado (ex.: 10 e 15 → uso 10-4, 11-8, 12-0, 13-1, 14-7, 15-9).

**Parâmetros (query)**

| Nome | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `numeroCliente` | integer | Sim | Número que identifica o contrato do beneficiário no Sisbr |
| `codigoModalidade` | integer | Sim | 1 - Simples com registro · 3 - Caucionada · 4 - Vinculada · 8 - Cobrança conta capital |
| `quantidade` | integer | Sim | Quantidade mínima de "nosso números" disponíveis na faixa pesquisada |
| `numeroContratoCobranca` | integer ($int64) | Não | Id do contrato de cobrança |

**Respostas**

| Código | Descrição |
|---|---|
| 200 | Retorna `resultado` como array de faixas disponíveis (`numeroInicial`, `numeroFinal`, `quantidade`, `validaDigitoVerificadorNossoNumero` etc.) |
| 204 | Requisição processada com êxito, sem conteúdo |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

#### `PATCH /boletos/{nossoNumero}` — Alterar dados de um boleto

Altera dados de um boleto já registrado. **Deve ser feita a alteração de somente um objeto do boleto por requisição.**

Objetos de alteração possíveis:
- `seuNumero`
- `desconto`
- `abatimento`
- `multa`
- `jurosMora`
- `rateioCredito`
- `pix`
- `prorrogacaoVencimento`
- `prorrogacaoLimitePagamento`

**Parâmetros**

| Nome | Tipo | Local | Descrição |
|---|---|---|---|
| `nossoNumero` * | integer | path | Identifica o boleto de cobrança no Sisbr |
| `boleto` * | object | body | Informações do boleto de cobrança |

**Body de exemplo**

```json
{
  "numeroCliente": 25546454,
  "codigoModalidade": 1,
  "numeroContratoCobranca": 1,
  "especieDocumento": { "codigoEspecieDocumento": "DM" },
  "seuNumero": {
    "seuNumero": "209",
    "identificacaoBoletoEmpresa": "209"
  },
  "desconto": {
    "tipoDesconto": 1,
    "dataPrimeiroDesconto": "2018-09-10",
    "valorPrimeiroDesconto": 33.57,
    "dataSegundoDesconto": "2018-09-15",
    "valorSegundoDesconto": 15.57,
    "dataTerceiroDesconto": "2018-09-20",
    "valorTerceiroDesconto": 10.59
  },
  "abatimento": { "valorAbatimento": 156.23 },
  "multa": {
    "tipoMulta": 1,
    "dataMulta": "2018-09-20",
    "valorMulta": 5
  },
  "jurosMora": {
    "tipoJurosMora": 1,
    "dataJurosMora": "2018-09-20",
    "valorJurosMora": 4
  },
  "rateioCredito": {
    "tipoOperacao": 2,
    "rateioCreditos": [
      {
        "numeroBanco": 33,
        "numeroAgencia": 1,
        "numeroContaCorrente": "987654",
        "contaPrincipal": true,
        "codigoTipoValorRateio": 1,
        "valorRateio": "100",
        "codigoTipoCalculoRateio": 1,
        "numeroCpfCnpjTitular": "11122233300",
        "nomeTitular": "Nome completo do titular X",
        "codigoFinalidadeTed": "10",
        "codigoTipoContaDestinoTed": "CC",
        "quantidadeDiasFloat": 1,
        "dataFloatCredito": "2020-12-30"
      }
    ]
  },
  "pix": { "utilizarPix": false },
  "prorrogacaoVencimento": { "dataVencimento": "2018-09-20" },
  "prorrogacaoLimitePagamento": { "dataLimitePagamento": "2018-09-20" },
  "valorNominal": { "valor": 156.23 }
}
```

**Respostas**

| Código | Descrição |
|---|---|
| 204 | Alteração realizada com sucesso |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

#### `POST /boletos/{nossoNumero}/baixar` — Comandar a baixa de boletos

Comanda a baixa de boletos informados.

**Parâmetros**

| Nome | Tipo | Local | Descrição |
|---|---|---|---|
| `nossoNumero` * | integer | path | Identifica o boleto de cobrança no Sisbr |
| `boleto` * | object | body | Informações do boleto de cobrança |

**Body de exemplo**

```json
{
  "numeroCliente": 5224,
  "codigoModalidade": 1
}
```

**Respostas**

| Código | Descrição |
|---|---|
| 204 | Solicitação recebida com sucesso |
| 400 | Possíveis erros de negócio |
| 406 | Possíveis erros de inconsistência nos dados passados |
| 500 | Erro interno |

---

<!-- Cole a próxima aba abaixo desta linha e eu adiciono ao índice -->
