import { Pageable, Sort } from "./PagesType";

// Representa uma compra/purchase individual
export interface PurchaseType {
  id: number;
  purchaseDate: string;
  total: number;
  updatedAt: string;
}

// Resposta paginada de compras
export interface PurchaseResponse {
  content: PurchaseType[];
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: Sort;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
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