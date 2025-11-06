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

export interface CombinedScoreResponse {
  content: CombinedScoreType[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
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
}