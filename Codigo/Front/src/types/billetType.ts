export interface BilletResponse {
    nomePagador: string;
    dataEmissao: string;
    dataVencimento: string;
    seuNumero: string;
    situacaoBoleto: string;
    valor: number;
}