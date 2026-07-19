export interface ClientRequest {
  clientName: string;
  variablePrice: boolean;
  document: string;
  phoneNumber: string;
  email: string;
  address: string;
  stateRegistration?: string | null;
  stateIndicator?: number | null;
  cideCode?: string | null;
  onlyBillet: boolean;
}

export interface ClientResponse {
  id: number;
  clientName: string;
  variablePrice: boolean;
  document: string;
  phoneNumber: string;
  address: string;
  email: string;
  stateRegistration: string | null;
  stateIndicator: number | null;
  cideCode: string | null;
  onlyBillet: boolean;
  lastPurchaseDate: string | null;
  totalPurchaseValue: number | null;
}

export interface ClientSelectionInfo {
  clientId: number;
  clientName: string;
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
