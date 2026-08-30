import { API_BASE_URL } from "@/config/api";
import type { ConversaoCaixaImportResponse } from "@/types/conversaoCaixaType";
import { getAuthHeadersForFormData } from "@/utils/httpUtils";

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

export const conversaoCaixaService = {
  async importar(file: File): Promise<ConversaoCaixaImportResponse> {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(
      `${API_BASE_URL}/api/produtos/conversao-caixa/importar`,
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
          "Falha ao importar o arquivo de conversão.",
        ),
      );
    }

    return await response.json();
  },
};
