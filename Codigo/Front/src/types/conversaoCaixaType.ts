export type ProdutoConversaoCadastrado = {
  codigo: string;
  produto: string;
  pesoCaixaKg: number;
};

export type ProdutoConversaoAtualizado = {
  codigo: string;
  produto: string;
  pesoAnterior: number;
  pesoNovo: number;
};

export type ConflitoConversaoCaixa = {
  codigo: string;
  valoresEncontrados: number[];
  valorAplicado: number;
};

export type ConversaoCaixaImportResponse = {
  cadastrados: ProdutoConversaoCadastrado[];
  atualizados: ProdutoConversaoAtualizado[];
  semAlteracao: string[];
  codigosNaoEncontrados: string[];
  conflitosNoArquivo: ConflitoConversaoCaixa[];
};
