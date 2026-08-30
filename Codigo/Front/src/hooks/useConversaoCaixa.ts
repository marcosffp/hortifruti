"use client";

import { useState } from "react";
import { conversaoCaixaService } from "@/services/conversaoCaixaService";
import type { ConversaoCaixaImportResponse } from "@/types/conversaoCaixaType";

export function useConversaoCaixa() {
  const [isLoading, setIsLoading] = useState(false);
  const [resultado, setResultado] =
    useState<ConversaoCaixaImportResponse | null>(null);

  async function importar(file: File): Promise<ConversaoCaixaImportResponse> {
    setIsLoading(true);
    try {
      const resposta = await conversaoCaixaService.importar(file);
      setResultado(resposta);
      return resposta;
    } finally {
      setIsLoading(false);
    }
  }

  return { isLoading, resultado, importar };
}
