export interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  selecionado: boolean;
}

export type TipoDestinatario = "clientes" | "contabilidade";
export type TipoReferencia = "mes" | "periodo";

export interface NotificacoesDraft {
  tipoDestinatario: TipoDestinatario;
  mensagemPersonalizada: string;
  canaisEnvio: { email: boolean; whatsapp: boolean };
  cardValue: number;
  cashValue: number;
  selectedClientIds: number[];
  tipoReferencia: TipoReferencia;
  mesReferencia: number;
  anoReferencia: number;
  dataInicialReferencia: string;
  dataFinalReferencia: string;
  textoEditadoManualmente: boolean;
}
