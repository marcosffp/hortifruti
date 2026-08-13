export interface GroupedScoreType {
  id: number;
  clientId: number;
  totalValue: number;
  paid: boolean;
  dueDate: string | null;
  confirmedAt: string;
}

export interface GroupedProductRequest {
  code: string;
  name: string;
  quantity: number;
  price: number;
  totalValue: number;
}
