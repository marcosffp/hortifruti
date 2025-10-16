import { Pageable, Sort } from "./PagesType";

export interface PurchaseType {
  id: number;
  purchaseDate: string;
  total: number;
  updatedAt: string;
}

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