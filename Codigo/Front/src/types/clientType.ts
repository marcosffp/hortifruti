export interface ClientRequest {
  clientName: string;
  nickname: string | null;
  variablePrice: boolean;
  document: string;
  phoneNumber: string | null;
  email: string | null;
  address: string;
  stateRegistration?: string | null;
  stateIndicator?: number | null;
  cideCode?: string | null;
  onlyBillet: boolean;
  requiresPurchaseProof: boolean;
}

export interface ClientResponse {
  id: number;
  clientName: string;
  nickname: string | null;
  variablePrice: boolean;
  document: string;
  phoneNumber: string | null;
  address: string;
  email: string | null;
  stateRegistration: string | null;
  stateIndicator: number | null;
  cideCode: string | null;
  onlyBillet: boolean;
  requiresPurchaseProof: boolean;
  lastPurchaseDate: string | null;
  totalPurchaseValue: number | null;
}

export interface ClientSelectionInfo {
  clientId: number;
  clientName: string;
  nickname: string | null;
}

export interface ClientWithLastPurchaseResponse {
  clientId: number;
  clientName: string;
  lastPurchaseDate: string | null;
  totalPurchases: number;
}

export interface ClientInfo {
  clientName: string;
  clientAddress: string;
  totalProducts: number;
  totalValue: number;
}
