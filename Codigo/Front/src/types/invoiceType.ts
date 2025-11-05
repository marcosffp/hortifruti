export interface InvoiceResponse {
    ref: string;
    status: string;
}

export interface InvoiceResponseGet {
    name: string;
    totalValue: number;
    status: string;
    date: string;
    number: string;
    reference: string;
}
