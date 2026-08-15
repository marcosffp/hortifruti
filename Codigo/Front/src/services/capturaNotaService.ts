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

/** Lançada quando o dispositivo (cookie httpOnly `device_token`) não está mais vinculado. */
export class CapturaSessaoExpiradaError extends Error {}

export const capturaNotaService = {
  /**
   * Usada tanto por quem já está logado normalmente (cookie de sessão) quanto por um
   * dispositivo pareado (cookie `device_token`) — em ambos os casos a autenticação vai só
   * pelo cookie `httpOnly`, nunca por header lido de `localStorage`.
   */
  async enviarFoto(file: File): Promise<void> {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/api/compras/notas/capturas`, {
      method: "POST",
      credentials: "include",
      body: formData,
    });

    if (response.status === 401) {
      throw new CapturaSessaoExpiradaError(
        "Este dispositivo foi desvinculado. Peça um novo pareamento no PC.",
      );
    }
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao enviar a foto."),
      );
    }
  },

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
