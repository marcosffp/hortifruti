export type ProdutoSugerido = {
  id: number;
  codigo: string;
  nome: string;
  score: number;
};

export type ClienteSugerido = {
  id: number;
  nome: string;
  score: number;
};

export type ItemNotaExtraido = {
  produtoLido: string;
  quantidade: number | null;
  unidade: string | null;
  precoUnitario: number | null;
  total: number | null;
  produtoSugerido: ProdutoSugerido | null;
  confianca: "alta" | "media" | "baixa" | null;
  // Preenchidos quando o item está em caixa (CX) e o produto sugerido tem peso de caixa
  // cadastrado — ver ConversaoCaixaService no backend. `null` quando não converteu.
  quantidadeKgConvertida: number | null;
  precoPorKgConvertido: number | null;
  conversaoEstimada: boolean | null;
  // Cross-check contra a tabela de preços do cliente (ver NotaPrecoOficialChecker no backend) —
  // `null` quando não há tabela CONFIRMADA cobrindo a data da nota, ou o cliente/produto ainda
  // não foi identificado. Só informativo aqui: o preço de fato persistido é sobrescrito de novo,
  // server-side, na confirmação da compra — nunca confie só nisso pra saber o preço final.
  precoOficialTabela: number | null;
  divergenciaPreco: boolean | null;
};

export type NotaExtracaoResponse = {
  cliente: string | null;
  data: string | null;
  itens: ItemNotaExtraido[];
  totalGeral: number | null;
  consistente: boolean | null;
  itensParaConferir: string[];
  clienteSugerido: ClienteSugerido | null;
  clienteConfianca: "alta" | "media" | "baixa" | null;
  itensComDivergenciaPreco: string[];
  semTabelaPrecoParaCompetencia: boolean | null;
};
