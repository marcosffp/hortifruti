export interface PurchaseType {
  id: number;
  purchaseDate: string;
  total: number;
  updatedAt: string;
}

export interface InvoiceProductType {
  id: number;
  code: string;
  name: string;
  price: number;
  quantity: number;
  unitType: string;
}

export interface InvoiceProductUpdate {
  code?: string;
  name?: string;
  price?: number;
  quantity?: number;
  unitType?: string;
}

export interface FiscalProductType {
  code: string;
  description: string;
  unidadeComercial: string;
}

export interface ManualPurchaseItemRequest {
  code: string;
  quantity: number;
  price: number;
}

export interface ManualPurchaseRequest {
  clientId: number;
  purchaseDate: string;
  items: ManualPurchaseItemRequest[];
}
