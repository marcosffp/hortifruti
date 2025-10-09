export interface ClientRequest {
    clientName: string;
    variablePrice: boolean;
    document: string;
    phoneNumber: string;
    email: string;
    address: string;
}

export interface ClientResponse {
    id: number;
    clientName: string;
    variablePrice: boolean;
    document: string;
    phoneNumber: string;
    address: string;
    email: string;
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