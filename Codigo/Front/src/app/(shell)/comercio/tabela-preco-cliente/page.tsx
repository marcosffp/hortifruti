"use client";

import { UploadCloud } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import RoleGuard from "@/components/auth/RoleGuard";
import TabelaPrecoImportResultado from "@/components/modules/tabela-preco-cliente/TabelaPrecoImportResultado";
import TabelaPrecoItensReview from "@/components/modules/tabela-preco-cliente/TabelaPrecoItensReview";
import Loading from "@/components/ui/Loading";
import { useClient } from "@/hooks/useClient";
import { useTabelaPrecoCliente } from "@/hooks/useTabelaPrecoCliente";
import type { ClientSelectionInfo } from "@/types/clientType";
import type { TabelaPrecoImportResponse } from "@/types/tabelaPrecoClienteType";
import { showError, showSuccess } from "@/utils/toastUtils";

export default function TabelaPrecoClientePage() {
  const [clients, setClients] = useState<ClientSelectionInfo[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [importResultado, setImportResultado] =
    useState<TabelaPrecoImportResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { getAllClientsForSelection } = useClient();
  const {
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
  } = useTabelaPrecoCliente();

  useEffect(() => {
    getAllClientsForSelection()
      .then(setClients)
      .catch((err) =>
        showError(
          err instanceof Error ? err.message : "Falha ao carregar clientes.",
        ),
      );
  }, [getAllClientsForSelection]);

  useEffect(() => {
    if (clienteId !== null) {
      setImportResultado(null);
      carregarHistorico(clienteId).catch((err) =>
        showError(
          err instanceof Error ? err.message : "Falha ao carregar histórico.",
        ),
      );
    }
  }, [clienteId, carregarHistorico]);

  async function handleFile(file: File) {
    if (clienteId === null) {
      showError("Selecione o cliente antes de importar o arquivo.");
      return;
    }
    if (!file.name.toLowerCase().endsWith(".csv")) {
      showError(
        "Envie o CSV oficial do cliente (colunas VALIDADE_INI, VALIDADE_FIN, PRODUTO, NOME_PR, VRUNI).",
      );
      return;
    }

    try {
      const resposta = await importar(clienteId, file);
      setImportResultado(resposta);
      await carregarHistorico(clienteId);
      showSuccess("Tabela de preços importada — revise os itens abaixo.");
    } catch (err) {
      showError(
        err instanceof Error ? err.message : "Erro ao importar o arquivo.",
      );
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    e.target.value = "";
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  return (
    <RoleGuard roles="MANAGER">
      <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800">
            Tabela de Preços do Cliente
          </h1>
          <p className="text-gray-600 mt-1">
            Importe o CSV oficial de um cliente (ex.: LLinea) por competência,
            confirme os vínculos com o catálogo interno linha a linha e exporte
            a tabela confirmada. Nenhum vínculo automático vira preço oficial
            sem confirmação humana.
          </p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-5 border border-gray-200 mb-6">
          <label
            htmlFor="cliente-select"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Cliente
          </label>
          <select
            id="cliente-select"
            value={clienteId ?? ""}
            onChange={(e) => {
              const id = e.target.value ? Number(e.target.value) : null;
              setClienteId(id);
            }}
            className="w-full max-w-md p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500"
          >
            <option value="">Selecione um cliente…</option>
            {clients.map((client) => (
              <option key={client.clientId} value={client.clientId}>
                {client.clientName}
              </option>
            ))}
          </select>
        </div>

        {clienteId !== null && (
          <>
            <div className="bg-white rounded-lg shadow-sm p-5 border border-gray-200 mb-6 relative">
              {isLoading && <Loading overlay />}
              <section
                className={`border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-center p-8 transition-colors ${
                  isDragging
                    ? "border-primary bg-primary-bg"
                    : "border-gray-300"
                }`}
                onDragOver={(e) => {
                  e.preventDefault();
                  setIsDragging(true);
                }}
                onDragLeave={(e) => {
                  e.preventDefault();
                  setIsDragging(false);
                }}
                onDrop={handleDrop}
                aria-label="Área para soltar o CSV do cliente"
              >
                <div className="h-16 w-16 rounded-full bg-primary-bg flex items-center justify-center mb-4">
                  <UploadCloud className="text-primary h-8 w-8" />
                </div>
                <p className="text-base font-medium mb-2">
                  Clique ou arraste o CSV oficial do cliente
                </p>
                <p className="text-gray-500 text-sm mb-4">
                  A competência (mês/ano) é lida do próprio arquivo
                  (VALIDADE_INI/VALIDADE_FIN). Reimportar cria uma nova versão,
                  nunca sobrescreve uma versão já confirmada.
                </p>
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isLoading}
                  className="py-2 px-4 text-sm bg-primary text-white rounded disabled:opacity-50 cursor-pointer hover:bg-[var(--primary-dark)] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
                >
                  Selecionar Arquivo
                </button>
                <input
                  type="file"
                  ref={fileInputRef}
                  className="hidden"
                  accept=".csv,text/csv"
                  onChange={handleFileChange}
                />
              </section>
            </div>

            {historico.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 mb-6">
                <h2 className="text-sm font-semibold text-gray-800 mb-2">
                  Competências importadas
                </h2>
                <div className="flex flex-wrap gap-2">
                  {historico.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => carregarTabela(item.id)}
                      className={`px-3 py-1.5 rounded-lg text-sm border ${
                        tabela?.id === item.id
                          ? "border-primary bg-primary-bg text-primary"
                          : "border-gray-300 text-gray-700 hover:bg-gray-50"
                      }`}
                    >
                      {String(item.competenciaMes).padStart(2, "0")}/
                      {item.competenciaAno} — v{item.versao} ({item.status})
                    </button>
                  ))}
                </div>
              </div>
            )}

            {importResultado && (
              <div className="mb-6">
                <TabelaPrecoImportResultado resultado={importResultado} />
              </div>
            )}

            {tabela && (
              <TabelaPrecoItensReview
                tabela={tabela}
                onConfirmarItem={confirmarItem}
                onSemCorrespondencia={marcarSemCorrespondencia}
                onConfirmarEmLote={confirmarEmLote}
                onConfirmarTabela={confirmarTabela}
                onExportarCsv={exportarCsv}
                onExportarPdf={exportarPdf}
              />
            )}
          </>
        )}
      </main>
    </RoleGuard>
  );
}
