import { API_BASE_URL } from "@/config/api";
import type { NotaExtracaoResponse } from "@/types/notaExtracaoType";

export type StatusCaptura =
  | "RECEBIDA"
  | "EXTRAINDO"
  | "PRONTA"
  | "ERRO"
  | "CONFIRMADA"
  | "DESCARTADA";

export interface CapturaPendente {
  id: number;
  status: StatusCaptura;
  extracao: NotaExtracaoResponse | null;
  mensagemErro: string | null;
  criadaEm: string;
}

export interface ConfirmarCapturaRequest {
  clientId: number;
  purchaseDate: string;
  items: { code: string; quantity: number; price: number }[];
}

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

export const capturaNotaService = {
  async fetchPendentes(): Promise<CapturaPendente[]> {
    const response = await fetch(
      `${API_BASE_URL}/api/compras/notas/pendentes`,
      {
        credentials: "include",
      },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao carregar a fila de notas.",
        ),
      );
    }
    return await response.json();
  },

  async fetchImagem(capturaId: number): Promise<Blob> {
    const response = await fetch(
      `${API_BASE_URL}/api/compras/notas/pendentes/${capturaId}/imagem`,
      { credentials: "include" },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao carregar a foto da captura.",
        ),
      );
    }
    return await response.blob();
  },

  async descartar(capturaId: number): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/api/compras/notas/pendentes/${capturaId}/descartar`,
      { method: "POST", credentials: "include" },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao descartar a captura."),
      );
    }
  },

  async confirmar(
    capturaId: number,
    payload: ConfirmarCapturaRequest,
  ): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/api/compras/notas/pendentes/${capturaId}/confirmar`,
      {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao lançar a compra."),
      );
    }
  },
};
