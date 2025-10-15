export interface GroupedScoreType {
  id: number;
  clientId: number;
  totalValue: number;
  paid: boolean;
  dueDate: string | null;
  confirmedAt: string;
}

export interface GroupedScoreResponsePageable {
  pageNumber: number;
  pageSize: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface GroupedScoreResponseSort {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
}

export interface GroupedScoreResponse {
  content: GroupedScoreType[];
  pageable: GroupedScoreResponsePageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: GroupedScoreResponseSort;
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