export interface BilletResponse {
    nomePagador: string;
    dataEmissao: string;
    dataVencimento: string;
    seuNumero: string;
    situacaoBoleto: string;
    nossoNumero: string;
    valor: number;
}