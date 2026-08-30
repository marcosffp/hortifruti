import { API_BASE_URL } from "@/config/api";
import type {
  TabelaPrecoClienteResponse,
  TabelaPrecoClienteResumo,
  TabelaPrecoImportResponse,
} from "@/types/tabelaPrecoClienteType";
import { getAuthHeaders, getAuthHeadersForFormData } from "@/utils/httpUtils";

const BASE_PATH = `${API_BASE_URL}/api/compras/tabelas-preco-cliente`;

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

export const tabelaPrecoClienteService = {
  async importar(
    clienteId: number,
    file: File,
  ): Promise<TabelaPrecoImportResponse> {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(
      `${BASE_PATH}/clientes/${clienteId}/importar`,
      {
        method: "POST",
        headers: getAuthHeadersForFormData(),
        credentials: "include",
        body: formData,
      },
    );

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao importar a tabela de preços.",
        ),
      );
    }
    return await response.json();
  },

  async buscarTabela(tabelaId: number): Promise<TabelaPrecoClienteResponse> {
    const response = await fetch(`${BASE_PATH}/${tabelaId}`, {
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao carregar a tabela de preços.",
        ),
      );
    }
    return await response.json();
  },

  async listarPorCliente(
    clienteId: number,
  ): Promise<TabelaPrecoClienteResumo[]> {
    const response = await fetch(`${BASE_PATH}/clientes/${clienteId}`, {
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao carregar o histórico de tabelas.",
        ),
      );
    }
    return await response.json();
  },

  async confirmarItem(
    tabelaId: number,
    itemId: number,
    fiscalProductCode: string,
  ): Promise<TabelaPrecoClienteResponse> {
    const response = await fetch(
      `${BASE_PATH}/${tabelaId}/itens/${itemId}/confirmar`,
      {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
        body: JSON.stringify({ fiscalProductCode }),
      },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao confirmar o item."),
      );
    }
    return await response.json();
  },

  async marcarSemCorrespondencia(
    tabelaId: number,
    itemId: number,
  ): Promise<TabelaPrecoClienteResponse> {
    const response = await fetch(
      `${BASE_PATH}/${tabelaId}/itens/${itemId}/sem-correspondencia`,
      { method: "POST", headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao marcar o item como sem correspondência.",
        ),
      );
    }
    return await response.json();
  },

  async confirmarEmLote(tabelaId: number): Promise<number> {
    const response = await fetch(`${BASE_PATH}/${tabelaId}/confirmar-lote`, {
      method: "POST",
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha na confirmação em lote."),
      );
    }
    return await response.json();
  },

  async confirmarTabela(tabelaId: number): Promise<TabelaPrecoClienteResponse> {
    const response = await fetch(`${BASE_PATH}/${tabelaId}/confirmar`, {
      method: "POST",
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao confirmar a tabela de preços.",
        ),
      );
    }
    return await response.json();
  },

  async exportarCsv(tabelaId: number): Promise<Blob> {
    const response = await fetch(`${BASE_PATH}/${tabelaId}/exportar/csv`, {
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao exportar o CSV."),
      );
    }
    return await response.blob();
  },

  async exportarPdf(tabelaId: number): Promise<Blob> {
    const response = await fetch(`${BASE_PATH}/${tabelaId}/exportar/pdf`, {
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao exportar o PDF."),
      );
    }
    return await response.blob();
  },
};
