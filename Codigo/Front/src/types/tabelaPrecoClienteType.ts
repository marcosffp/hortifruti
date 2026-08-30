export type StatusTabelaPreco = "RASCUNHO" | "EM_REVISAO" | "CONFIRMADA";

export type StatusMatchItemTabelaPreco =
  | "SUGERIDO"
  | "CONFIRMADO"
  | "SEM_CORRESPONDENCIA"
  | "EDITADO_MANUALMENTE";

export type ProdutoSugeridoTabelaPreco = {
  id: number;
  codigo: string;
  nome: string;
  score: number;
};

export type ItemAutoAplicado = {
  codigoProdutoCliente: string;
  nomeProdutoCliente: string;
  fiscalProductCodigo: string | null;
  fiscalProductDescricao: string | null;
};

export type ItemSugerido = {
  itemId: number;
  codigoProdutoCliente: string;
  nomeProdutoCliente: string;
  produtoSugerido: ProdutoSugeridoTabelaPreco | null;
  confianca: "alta" | "media" | "baixa" | null;
};

export type ItemSemCorrespondencia = {
  itemId: number;
  codigoProdutoCliente: string;
  nomeProdutoCliente: string;
};

export type TabelaPrecoImportResponse = {
  tabelaPrecoClienteId: number;
  autoAplicadosPorMapeamento: ItemAutoAplicado[];
  sugeridosAltaConfianca: ItemSugerido[];
  sugeridosBaixaConfianca: ItemSugerido[];
  semCorrespondencia: ItemSemCorrespondencia[];
  precosEmBrancoNoArquivo: number;
};

export type TabelaPrecoClienteItemResponse = {
  id: number;
  codigoProdutoCliente: string;
  nomeProdutoCliente: string;
  preco: number | null;
  fiscalProductId: number | null;
  fiscalProductCodigo: string | null;
  fiscalProductDescricao: string | null;
  confiancaMatching: number | null;
  statusMatch: StatusMatchItemTabelaPreco;
};

export type TabelaPrecoClienteResponse = {
  id: number;
  clienteId: number;
  competenciaMes: number;
  competenciaAno: number;
  vigenciaInicio: string;
  vigenciaFim: string;
  versao: number;
  status: StatusTabelaPreco;
  importadoEm: string;
  confirmadoEm: string | null;
  itens: TabelaPrecoClienteItemResponse[];
};

/** Item resumido usado na listagem de histórico (`GET /clientes/{clienteId}`) — sem os itens. */
export type TabelaPrecoClienteResumo = Omit<
  TabelaPrecoClienteResponse,
  "itens"
>;
