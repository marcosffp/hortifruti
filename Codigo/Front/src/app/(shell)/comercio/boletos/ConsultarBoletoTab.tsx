"use client";

import {
  Download,
  ExternalLink,
  FileSearch,
  Filter,
  Receipt,
  Search,
  Trash2,
} from "lucide-react";
import { useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import ClientSelector from "@/components/modules/ClientSelector";
import { useBillet } from "@/hooks/useBillet";
import type { BilletResponse } from "@/types/billetType";
import type { ClientSelectionInfo } from "@/types/clientType";
import { showError, showSuccess } from "@/utils/toastUtils";
import {
  ActionSpinner,
  formatCurrency,
  formatDate,
  getClientBilletKey,
  goToGrouping,
  isBilletCancelable,
  SITUACAO_OPTIONS,
  situacaoBadgeColor,
  triggerPdfDownload,
} from "./shared";

export default function ConsultarBoletoTab() {
  const {
    getClientBillets,
    cancelBillet,
    cancelBilletByNumber,
    downloadStoredBillet,
    isLoading,
  } = useBillet();

  const [selectedClient, setSelectedClient] =
    useState<ClientSelectionInfo | null>(null);
  const [situacao, setSituacao] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [clientBillets, setClientBillets] = useState<BilletResponse[] | null>(
    null,
  );
  const [loadingClientBillets, setLoadingClientBillets] = useState(false);
  const [clientBilletActionKey, setClientBilletActionKey] = useState<
    string | null
  >(null);
  const [downloadingBilletKey, setDownloadingBilletKey] = useState<
    string | null
  >(null);
  const [cancelClientBilletTarget, setCancelClientBilletTarget] =
    useState<BilletResponse | null>(null);

  const handleSearchClientBillets = async (
    client: ClientSelectionInfo | null,
  ) => {
    if (!client) return;
    setLoadingClientBillets(true);
    try {
      const data = await getClientBillets(client.clientId, {
        codigoSituacao: situacao ? Number(situacao) : undefined,
        dataInicio: dataInicio || undefined,
        dataFim: dataFim || undefined,
      });
      setClientBillets(data);
    } catch (error) {
      showError("Não foi possível buscar os boletos do cliente");
      console.error(error);
    } finally {
      setLoadingClientBillets(false);
    }
  };

  const handleClientSelect = (client: ClientSelectionInfo) => {
    setSelectedClient(client);
    handleSearchClientBillets(client);
  };

  const handleApplyFilters = () => {
    handleSearchClientBillets(selectedClient);
  };

  const handleClearFilters = () => {
    setSituacao("");
    setDataInicio("");
    setDataFim("");
    if (selectedClient) {
      setLoadingClientBillets(true);
      getClientBillets(selectedClient.clientId, {})
        .then(setClientBillets)
        .catch((error) => {
          showError("Não foi possível buscar os boletos do cliente");
          console.error(error);
        })
        .finally(() => setLoadingClientBillets(false));
    }
  };

  const handleDownloadClientBillet = async (billet: BilletResponse) => {
    if (!billet.combinedScoreId) return;
    const combinedScoreId = billet.combinedScoreId;
    setDownloadingBilletKey(getClientBilletKey(billet));
    try {
      const blob = await downloadStoredBillet(combinedScoreId);
      triggerPdfDownload(blob, billet.seuNumero, combinedScoreId);
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Não foi possível baixar o PDF do boleto",
      );
      console.error(error);
    } finally {
      setDownloadingBilletKey(null);
    }
  };

  const handleCancelClientBillet = (billet: BilletResponse) => {
    setCancelClientBilletTarget(billet);
  };

  const confirmCancelClientBillet = async () => {
    if (!cancelClientBilletTarget) return;
    const billet = cancelClientBilletTarget;
    setCancelClientBilletTarget(null);
    setClientBilletActionKey(getClientBilletKey(billet));
    try {
      if (billet.combinedScoreId) {
        await cancelBillet(billet.combinedScoreId);
      } else {
        await cancelBilletByNumber(billet.nossoNumero);
      }
      showSuccess("Boleto com baixa realizada com sucesso.");
      await handleSearchClientBillets(selectedClient);
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Não foi possível dar baixa no boleto",
      );
      console.error(error);
    } finally {
      setClientBilletActionKey(null);
    }
  };

  const cancelClientBilletConfirmTitle = cancelClientBilletTarget
    ? `Tem certeza que deseja dar baixa neste boleto (${formatCurrency(
        cancelClientBilletTarget.valor,
      )})? Esta ação não pode ser desfeita.`
    : "";

  return (
    <div>
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,20rem)_1fr] gap-6">
        <div className="bg-gray-50 border border-gray-200 rounded-lg">
          <ClientSelector onClientSelect={handleClientSelect} />
        </div>

        <div>
          <div className="flex items-center gap-2 mb-3 text-gray-700 font-medium">
            <Filter className="w-4 h-4" />
            Filtros
          </div>
          <div className="flex flex-wrap gap-3 items-end mb-5">
            <div className="flex flex-col gap-1">
              <label
                htmlFor="boletos-situacao"
                className="text-xs font-medium text-gray-600"
              >
                Situação
              </label>
              <select
                id="boletos-situacao"
                className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                value={situacao}
                onChange={(e) => setSituacao(e.target.value)}
              >
                {SITUACAO_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label
                htmlFor="boletos-data-inicio"
                className="text-xs font-medium text-gray-600"
              >
                Vencimento de
              </label>
              <input
                id="boletos-data-inicio"
                type="date"
                className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1">
              <label
                htmlFor="boletos-data-fim"
                className="text-xs font-medium text-gray-600"
              >
                até
              </label>
              <input
                id="boletos-data-fim"
                type="date"
                className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
              />
            </div>
            <button
              type="button"
              onClick={handleApplyFilters}
              disabled={!selectedClient}
              className="flex items-center gap-2 px-4 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              <Search className="w-4 h-4" />
              Buscar
            </button>
            <button
              type="button"
              onClick={handleClearFilters}
              disabled={!selectedClient}
              className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              Limpar filtros
            </button>
          </div>

          {!selectedClient && (
            <div className="text-center py-16 text-gray-500">
              <FileSearch className="w-12 h-12 mx-auto mb-3 opacity-40" />
              <p>
                Selecione um cliente para consultar os boletos dele no Sicoob
              </p>
            </div>
          )}

          {selectedClient && (loadingClientBillets || isLoading) && (
            <div className="py-16 text-center">
              <div className="flex justify-center mb-4">
                <div className="h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
                  <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-100 border-t-green-600 border-r-green-600"></div>
                </div>
              </div>
              <p className="text-lg font-medium text-gray-700">
                Buscando boletos no Sicoob...
              </p>
              <p className="text-sm mt-1 text-gray-500">
                Aguarde enquanto consultamos os boletos do cliente
              </p>
            </div>
          )}

          {selectedClient &&
            !loadingClientBillets &&
            clientBillets !== null &&
            clientBillets.length === 0 && (
              <div className="text-center py-16 text-gray-500">
                <Receipt className="w-12 h-12 mx-auto mb-3 opacity-40" />
                <p>Nenhum boleto encontrado para os filtros selecionados</p>
              </div>
            )}

          {selectedClient &&
            !loadingClientBillets &&
            clientBillets !== null &&
            clientBillets.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-gray-500 border-b">
                      <th className="py-3 px-3 font-semibold">Valor</th>
                      <th className="py-3 px-3 font-semibold">Agrupamento</th>
                      <th className="py-3 px-3 font-semibold">Emissão</th>
                      <th className="py-3 px-3 font-semibold">Vencimento</th>
                      <th className="py-3 px-3 font-semibold">Situação</th>
                      <th className="py-3 px-3 font-semibold text-right">
                        Ação
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {clientBillets.map((billet) => (
                      <tr
                        key={`${billet.seuNumero}-${billet.dataVencimento}-${billet.valor}`}
                        className="border-b last:border-0 hover:bg-gray-50 transition-colors"
                      >
                        <td className="py-3 px-3 font-medium text-gray-800">
                          {formatCurrency(billet.valor)}
                        </td>
                        <td className="py-3 px-3 text-gray-500">
                          {billet.combinedScoreId
                            ? `#${billet.combinedScoreId}`
                            : "—"}
                        </td>
                        <td className="py-3 px-3">
                          {formatDate(billet.dataEmissao)}
                        </td>
                        <td className="py-3 px-3">
                          {formatDate(billet.dataVencimento)}
                        </td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-1 rounded text-xs font-medium ${situacaoBadgeColor(
                              billet.situacaoBoleto,
                            )}`}
                          >
                            {billet.situacaoBoleto}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {billet.combinedScoreId ? (
                              <>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleDownloadClientBillet(billet)
                                  }
                                  disabled={
                                    downloadingBilletKey ===
                                    getClientBilletKey(billet)
                                  }
                                  className="inline-flex items-center gap-1 px-3 py-1.5 bg-gray-700 text-white rounded-lg hover:bg-gray-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  {downloadingBilletKey ===
                                  getClientBilletKey(billet) ? (
                                    <ActionSpinner />
                                  ) : (
                                    <Download className="w-3 h-3" />
                                  )}
                                  Baixar PDF
                                </button>
                                <button
                                  type="button"
                                  onClick={() =>
                                    selectedClient &&
                                    goToGrouping(selectedClient.clientId)
                                  }
                                  className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer"
                                >
                                  <ExternalLink className="w-3 h-3" />
                                  Ver Agrupamento
                                </button>
                              </>
                            ) : (
                              <span className="text-xs text-gray-400">
                                Agrupamento não localizado
                              </span>
                            )}
                            {isBilletCancelable(billet.situacaoBoleto) && (
                              <button
                                type="button"
                                onClick={() => handleCancelClientBillet(billet)}
                                disabled={
                                  clientBilletActionKey ===
                                  getClientBilletKey(billet)
                                }
                                className="inline-flex items-center gap-1 px-3 py-1.5 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                {clientBilletActionKey ===
                                getClientBilletKey(billet) ? (
                                  <ActionSpinner />
                                ) : (
                                  <Trash2 className="w-3 h-3" />
                                )}
                                Dar baixa
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
        </div>
      </div>

      <ConfirmDeleteModal
        open={cancelClientBilletTarget !== null}
        onClose={() => setCancelClientBilletTarget(null)}
        onConfirm={confirmCancelClientBillet}
        title={cancelClientBilletConfirmTitle}
      />
    </div>
  );
}
