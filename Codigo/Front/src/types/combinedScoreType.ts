export interface CombinedScoreType {
  id: number;
  clientId: number;
  totalValue: number;
  dueDate: string | null;
  confirmedAt: string;
  status: string;
  hasBillet: boolean;
  hasInvoice: boolean;
  number: string;
  invoiceRef?: string | null;
}

export interface GroupedProductType {
  code: string;
  name: string;
  price: number;
  quantity: number;
  totalValue: number;
}

export interface CombinedScoreRequest {
  clientId: number;
  startDate: string;
  endDate: string;
  confirmedAt?: string;
}

export interface ClientLastGroupingType {
  clientId: number;
  confirmedAt: string | null;
  totalValue: number | null;
}

export interface PurchaseImageType {
  purchaseId: number;
  purchaseDate: string;
  total: number;
}
