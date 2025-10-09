export interface ProductInfo {
  productId: number;
  productName: string;
  totalQuantity: number;
  totalValue: number;
  [key: string]: any; // Para outros campos que possam vir do backend
}