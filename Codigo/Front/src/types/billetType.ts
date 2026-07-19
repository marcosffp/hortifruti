export interface BilletResponse {
  nomePagador: string;
  dataEmissao: string;
  dataVencimento: string;
  seuNumero: string;
  situacaoBoleto: string;
  valor: number;
  combinedScoreId: number | null;
}

export interface OpenBilletResponse {
  combinedScoreId: number;
  clientId: number;
  clientName: string;
  totalValue: number;
  dueDate: string | null;
  yourNumber: string | null;
  confirmadoNoSicoob: boolean;
}

export interface BilletFilters {
  codigoSituacao?: number;
  dataInicio?: string;
  dataFim?: string;
}
