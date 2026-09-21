export interface InvoiceResponse {
  ref: string;
  status: string;
}

/**
 * Resposta do cancelamento de NF-e. `status` é `"CANCELADO"` quando a Focus NFe/SEFAZ já
 * confirmou o cancelamento na hora, ou `"PROCESSANDO"` quando foi aceito mas ainda depende de
 * confirmação assíncrona da SEFAZ — nesse caso a nota ainda não está de fato cancelada.
 */
export interface InvoiceCancelResponse {
  ref: string;
  status: "CANCELADO" | "PROCESSANDO";
  message: string;
}

export interface InvoiceResponseGet {
  name: string;
  totalValue: number;
  status: string;
  date: string;
  number: string;
  reference: string;
}

export interface InvoiceWithBilletResponse {
  invoiceRef: string;
  invoiceNumber: string;
  billetNumber: string;
  danfeBase64: string;
  xmlBase64: string;
  billetBase64: string;
}

export interface InvoiceWithBilletResult {
  invoiceRef: string;
  invoiceNumber: string;
  billetNumber: string;
  danfeBlob: Blob;
  xmlBlob: Blob;
  billetBlob: Blob;
}

export interface OpenInvoiceResponse {
  combinedScoreId: number;
  clientId: number;
  clientName: string;
  totalValue: number;
  confirmedAt: string | null;
  dueDate: string | null;
  invoiceRef: string | null;
}
