"use client";

import { useCallback, useState } from "react";
import { tabelaPrecoClienteService } from "@/services/tabelaPrecoClienteService";
import type {
  TabelaPrecoClienteResponse,
  TabelaPrecoClienteResumo,
  TabelaPrecoImportResponse,
} from "@/types/tabelaPrecoClienteType";

function baixarBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

// Todas as funções expostas usam useCallback com deps vazias (ou só de `tabela`, quando
// precisam do id atual) — do contrário, referências novas a cada render fariam qualquer
// `useEffect` de quem consome este hook (ex.: a página carregando o histórico ao trocar de
// cliente) reexecutar em loop infinito, mesmo mecanismo já visto/corrigido em useClient.ts.
export function useTabelaPrecoCliente() {
  const [isLoading, setIsLoading] = useState(false);
  const [tabela, setTabela] = useState<TabelaPrecoClienteResponse | null>(null);
  const [historico, setHistorico] = useState<TabelaPrecoClienteResumo[]>([]);

  const carregarTabela = useCallback(
    async (tabelaId: number): Promise<void> => {
      setIsLoading(true);
      try {
        const resposta = await tabelaPrecoClienteService.buscarTabela(tabelaId);
        setTabela(resposta);
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const importar = useCallback(
    async (
      clienteId: number,
      file: File,
    ): Promise<TabelaPrecoImportResponse> => {
      setIsLoading(true);
      try {
        const resposta = await tabelaPrecoClienteService.importar(
          clienteId,
          file,
        );
        await carregarTabela(resposta.tabelaPrecoClienteId);
        return resposta;
      } finally {
        setIsLoading(false);
      }
    },
    [carregarTabela],
  );

  const carregarHistorico = useCallback(
    async (clienteId: number): Promise<void> => {
      const resposta =
        await tabelaPrecoClienteService.listarPorCliente(clienteId);
      setHistorico(resposta);
    },
    [],
  );

  const confirmarItem = useCallback(
    async (itemId: number, fiscalProductCode: string): Promise<void> => {
      if (!tabela) return;
      const resposta = await tabelaPrecoClienteService.confirmarItem(
        tabela.id,
        itemId,
        fiscalProductCode,
      );
      setTabela(resposta);
    },
    [tabela],
  );

  const marcarSemCorrespondencia = useCallback(
    async (itemId: number): Promise<void> => {
      if (!tabela) return;
      const resposta = await tabelaPrecoClienteService.marcarSemCorrespondencia(
        tabela.id,
        itemId,
      );
      setTabela(resposta);
    },
    [tabela],
  );

  const confirmarEmLote = useCallback(async (): Promise<number> => {
    if (!tabela) return 0;
    const confirmados = await tabelaPrecoClienteService.confirmarEmLote(
      tabela.id,
    );
    await carregarTabela(tabela.id);
    return confirmados;
  }, [tabela, carregarTabela]);

  const confirmarTabela = useCallback(async (): Promise<void> => {
    if (!tabela) return;
    const resposta = await tabelaPrecoClienteService.confirmarTabela(tabela.id);
    setTabela(resposta);
  }, [tabela]);

  const exportarCsv = useCallback(async (): Promise<void> => {
    if (!tabela) return;
    const blob = await tabelaPrecoClienteService.exportarCsv(tabela.id);
    baixarBlob(
      blob,
      `tabela-preco-cliente-${tabela.competenciaAno}-${tabela.competenciaMes}.csv`,
    );
  }, [tabela]);

  const exportarPdf = useCallback(async (): Promise<void> => {
    if (!tabela) return;
    const blob = await tabelaPrecoClienteService.exportarPdf(tabela.id);
    baixarBlob(
      blob,
      `tabela-preco-cliente-${tabela.competenciaAno}-${tabela.competenciaMes}.pdf`,
    );
  }, [tabela]);

  return {
    isLoading,
    tabela,
    historico,
    importar,
    carregarTabela,
    carregarHistorico,
    confirmarItem,
    marcarSemCorrespondencia,
    confirmarEmLote,
    confirmarTabela,
    exportarCsv,
    exportarPdf,
  };
}
