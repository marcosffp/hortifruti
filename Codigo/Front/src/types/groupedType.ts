import { Pageable, Sort } from "./PagesType";

export interface GroupedScoreType {
  id: number;
  clientId: number;
  totalValue: number;
  paid: boolean;
  dueDate: string | null;
  confirmedAt: string;
}

export interface GroupedScoreResponse {
  content: GroupedScoreType[];
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

export interface GroupedProductRequest {
  code: string;
  name: string;
  quantity: number;
  price: number;
  totalValue: number;
}