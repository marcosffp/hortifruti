import type { ChangeEvent, FocusEvent } from "react";

export interface ClientFormData {
  nome: string;
  apelido: string;
  email: string;
  telefone: string;
  cpfCnpj: string;
  cep: string;
  endereco: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  estado: string;
  variablePrice: string;
  stateRegistration: string;
  stateIndicator: string;
  cideCode: string;
  onlyBillet: string;
  requiresPurchaseProof: string;
}

export interface ClientFormErrors {
  nome: string;
  email: string;
  telefone: string;
  cpfCnpj: string;
  cep: string;
  endereco: string;
  numero: string;
  bairro: string;
  cidade: string;
  estado: string;
  stateRegistration: string;
  cideCode: string;
}

export type ClientFormChangeEvent = ChangeEvent<
  HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
>;

export type ClientFormBlurEvent = FocusEvent<
  HTMLInputElement | HTMLSelectElement
>;
