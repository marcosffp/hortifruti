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
};
