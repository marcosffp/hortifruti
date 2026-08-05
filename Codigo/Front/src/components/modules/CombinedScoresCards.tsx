"use client";

import {
  Calendar,
  Camera,
  CheckCircle,
  Eye,
  FileText,
  Info,
  Trash2,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import AdditionalDataModal from "@/components/modals/AdditionalDataModal";
import CombinedScoreImagesModal from "@/components/modals/CombinedScoreImagesModal";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import GroupedProductsModal from "@/components/modals/GroupedProductsModal";
import ShowBilletDataModal from "@/components/modals/ShowBilletDataModal";
import ShowBilletModal from "@/components/modals/ShowBilletModal";
import ShowInvoiceAndBilletModal from "@/components/modals/ShowInvoiceAndBilletModal";
import ShowInvoiceDataModal from "@/components/modals/ShowInvoiceDataModal";
import ShowInvoiceModal from "@/components/modals/ShowInvoiceModal";
import WildcardBilletModal from "@/components/modals/WildcardBilletModal";
import GameLoadingOverlay from "@/components/ui/GameLoadingOverlay";
import { useBillet } from "@/hooks/useBillet";
import { useClient } from "@/hooks/useClient";
import { useInvoice } from "@/hooks/useInvoice";
import { combinedScoreService } from "@/services/combinedScoreService";
import {
  showError,
  showInfo,
  showSuccess,
} from "@/services/notificationService";
import type { BilletResponse } from "@/types/billetType";
import type { ClientResponse } from "@/types/clientType";
import type { CombinedScoreType } from "@/types/combinedScoreType";
import type {
  InvoiceResponseGet,
  InvoiceWithBilletResult,
} from "@/types/invoiceType";
import ClientNumberModal from "../modals/ClientNumberModal";

interface CombinedScoresCardsProps {
  clientId?: number;
  refreshKey?: number;
}

interface ScoreWithBilletInfo extends CombinedScoreType {
  billetInfo?: BilletResponse | null;
  invoiceInfo?: InvoiceResponseGet | null;
  invoiceRef?: string | null;
}

export default function CombinedScoresCards({
  clientId,
  refreshKey,
}: CombinedScoresCardsProps) {
  const [scores, setScores] = useState<ScoreWithBilletInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [clientNumberModal, setClientNumberModal] = useState({
    state: false,
    groupId: -1,
  });
  const [showAdditionalDataModal, setShowAdditionalDataModal] = useState(false);
  const [pendingInvoiceScore, setPendingInvoiceScore] =
    useState<ScoreWithBilletInfo | null>(null);
  const [pendingCombinedFlow, setPendingCombinedFlow] = useState(false);
  const [client, setClient] = useState<ClientResponse | null>(null);
  const [showWildcardBilletModal, setShowWildcardBilletModal] = useState(false);
  const [deleteConfirmScore, setDeleteConfirmScore] =
    useState<ScoreWithBilletInfo | null>(null);

  // Cada modal abaixo guarda seu PRÓPRIO agrupamento (score), em vez de compartilhar
  // um único "selectedScore" global. Isso evita que duas operações assíncronas para
  // agrupamentos diferentes (ex.: gerar NF+boleto do 349, que demora minutos, enquanto
  // o usuário abre "Ver Boleto" do 335) acabem sobrescrevendo uma à outra e abrindo o
  // modal errado com os dados do agrupamento errado quando a operação demorada resolver.
  const [productsModalScore, setProductsModalScore] =
    useState<ScoreWithBilletInfo | null>(null);
  const [imagesModalScore, setImagesModalScore] =
    useState<ScoreWithBilletInfo | null>(null);
  const [billetResultModal, setBilletResultModal] = useState<{
    score: ScoreWithBilletInfo;
    pdf: Blob;
    clientNumber: string | null;
  } | null>(null);
  const [billetDataModalScore, setBilletDataModalScore] =
    useState<ScoreWithBilletInfo | null>(null);
  const [invoiceResultModal, setInvoiceResultModal] = useState<{
    score: ScoreWithBilletInfo;
    pdf: Blob;
  } | null>(null);
  const [invoiceDataModalScore, setInvoiceDataModalScore] =
    useState<ScoreWithBilletInfo | null>(null);
  const [invoiceBilletResultModal, setInvoiceBilletResultModal] = useState<{
    score: ScoreWithBilletInfo;
    result: InvoiceWithBilletResult;
  } | null>(null);

  // Guarda de duplo clique: bloqueia uma segunda geração de boleto/NF para o mesmo
  // agrupamento enquanto a primeira ainda está em andamento. Usa ref (além do state, só para
  // refletir na UI) porque duas chamadas síncronas de clique podem ocorrer antes do state
  // atualizar via re-render.
  const processingScoreIdsRef = useRef<Set<number>>(new Set());
  const [processingScoreIds, setProcessingScoreIds] = useState<Set<number>>(
    new Set(),
  );

  const beginProcessing = useCallback((id: number): boolean => {
    if (processingScoreIdsRef.current.has(id)) return false;
    processingScoreIdsRef.current.add(id);
    setProcessingScoreIds(new Set(processingScoreIdsRef.current));
    return true;
  }, []);

  const endProcessing = useCallback((id: number) => {
    processingScoreIdsRef.current.delete(id);
    setProcessingScoreIds(new Set(processingScoreIdsRef.current));
  }, []);

  // Mesmo guard de duplo clique, mas para as ações rápidas (confirmar pagamento/deletar)
  // — separado de processingScoreIds pra não acionar o modal de carregamento de NF/boleto.
  const actionProcessingIdsRef = useRef<Set<number>>(new Set());
  const [actionProcessingIds, setActionProcessingIds] = useState<Set<number>>(
    new Set(),
  );

  const beginAction = useCallback((id: number): boolean => {
    if (actionProcessingIdsRef.current.has(id)) return false;
    actionProcessingIdsRef.current.add(id);
    setActionProcessingIds(new Set(actionProcessingIdsRef.current));
    return true;
  }, []);

  const endAction = useCallback((id: number) => {
    actionProcessingIdsRef.current.delete(id);
    setActionProcessingIds(new Set(actionProcessingIdsRef.current));
  }, []);

  const { generateBillet, getBilletInfo } = useBillet();
  const {
    generateInvoice,
    generateInvoiceWithBillet,
    getInvoiceInfo,
    getDanfe,
    reconcileInvoiceStatus,
  } = useInvoice();
  const { getClientById } = useClient();

  // biome-ignore lint/correctness/useExhaustiveDependencies: getBilletInfo/getInvoiceInfo are recreated on every render by useBillet/useInvoice and are not part of the fetch identity
  const fetchScores = useCallback(async () => {
    if (!clientId) {
      setScores([]);
      return;
    }

    setLoading(true);
    try {
      const data = await combinedScoreService.fetchCombinedScores(
        clientId,
        page,
        20,
      );

      // Para cada score que tem boleto ou nota fiscal, busca as informações
      const scoresWithInfo = await Promise.all(
        data.content.map(async (score) => {
          let billetInfo = null;
          let invoiceInfo = null;

          // Busca informações do boleto se existir
          if (score.hasBillet) {
            try {
              billetInfo = await getBilletInfo(score.id);

              // Atualiza o status do score baseado no status do boleto
              if (billetInfo) {
                const billetStatus = billetInfo.situacaoBoleto.toLowerCase();
                if (
                  billetStatus.includes("liquidado") ||
                  billetStatus.includes("pago")
                ) {
                  score.status = "PAID";
                } else if (
                  billetStatus.includes("cancelado") ||
                  billetStatus.includes("baixado")
                ) {
                  score.status = "CANCELLED";
                } else if (
                  billetStatus.includes("aberto") ||
                  billetStatus.includes("pendente")
                ) {
                  // Verifica se está vencido
                  const vencimento = new Date(billetInfo.dataVencimento);
                  const hoje = new Date();
                  if (vencimento < hoje) {
                    score.status = "OVERDUE";
                  } else {
                    score.status = "PENDING";
                  }
                }
              }
            } catch (error) {
              console.error(
                `Erro ao buscar info do boleto ${score.id}:`,
                error,
              );
            }
          }

          // Busca informações da nota fiscal se existir e tiver referência
          if (score.hasInvoice && score.invoiceRef) {
            try {
              invoiceInfo = await getInvoiceInfo(score.invoiceRef);
            } catch (error) {
              console.error(
                `Erro ao buscar info da nota fiscal ${score.id}:`,
                error,
              );
            }
          }

          return {
            ...score,
            billetInfo,
            invoiceInfo,
            invoiceRef: score.invoiceRef,
          };
        }),
      );

      setScores(scoresWithInfo);
      setTotalPages(data.totalPages);
    } catch (error) {
      showError("Erro ao carregar agrupamentos");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [clientId, page]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: refreshKey is intentionally unused inside the effect — it only exists to force a refetch when the parent bumps it
  useEffect(() => {
    fetchScores();
  }, [fetchScores, refreshKey]);

  useEffect(() => {
    if (!clientId) {
      setClient(null);
      return;
    }
    getClientById(clientId)
      .then(setClient)
      .catch((error) => {
        console.error("Erro ao buscar cliente:", error);
        setClient(null);
      });
  }, [clientId, getClientById]);

  const handleDelete = async (id: number) => {
    if (!beginAction(id)) return;

    try {
      await combinedScoreService.cancelGrouping(id);
      showSuccess("Agrupamento deletado com sucesso");
      fetchScores();
    } catch (error) {
      showError("Erro ao deletar agrupamento");
      console.error(error);
    } finally {
      endAction(id);
    }
  };

  const handleTogglePayment = async (score: ScoreWithBilletInfo) => {
    if (!beginAction(score.id)) return;
    try {
      if (score.status === "PAID") {
        await combinedScoreService.cancelPayment(score.id);
        showSuccess("Pagamento cancelado com sucesso");
      } else {
        await combinedScoreService.confirmPayment(score.id);
        showSuccess("Pagamento confirmado com sucesso");
      }
      fetchScores();
    } catch (error) {
      showError("Erro ao atualizar pagamento");
      console.error(error);
    } finally {
      endAction(score.id);
    }
  };

  const handleViewProducts = (score: ScoreWithBilletInfo) => {
    setProductsModalScore(score);
  };

  const handleGenerateBillet = async (
    scoreId: number,
    clientNumber: string,
    dueDate?: string,
  ) => {
    if (!beginProcessing(scoreId)) return;
    try {
      const score = scores.find((s) => s.id === scoreId);
      if (!score) {
        showError("Agrupamento não encontrado");
        return;
      }

      const pdfBlob = await generateBillet(scoreId, clientNumber, dueDate);

      setBilletResultModal({ score, pdf: pdfBlob, clientNumber });

      showSuccess("Boleto gerado com sucesso!");

      fetchScores();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao gerar boleto",
      );
      console.error(error);
    } finally {
      endProcessing(scoreId);
    }
  };

  const creatingWildcardBilletRef = useRef(false);

  const handleGenerateWildcardBillet = async (
    number: string,
    value: number,
    dueDate?: string,
  ) => {
    if (!clientId) return;
    if (creatingWildcardBilletRef.current) return;
    creatingWildcardBilletRef.current = true;

    try {
      const newScoreId = await combinedScoreService.createWildcardBillet(
        clientId,
        value,
      );
      const pdfBlob = await generateBillet(newScoreId, number, dueDate);

      setBilletResultModal({
        score: {
          id: newScoreId,
          clientId,
          totalValue: value,
          dueDate: dueDate || null,
          confirmedAt: new Date().toISOString(),
          status: "PENDENTE",
          hasBillet: true,
          hasInvoice: false,
          number,
        },
        pdf: pdfBlob,
        clientNumber: number,
      });

      showSuccess("Boleto gerado com sucesso!");
      fetchScores();
    } catch (error) {
      showError("Erro ao gerar boleto");
      console.error(error);
    } finally {
      creatingWildcardBilletRef.current = false;
    }
  };

  const handleShowBillet = async (score: ScoreWithBilletInfo) => {
    try {
      if (score.billetInfo) {
        setBilletDataModalScore(score);
        return;
      }

      // billetInfo não foi carregado no fetchScores — tenta buscar agora
      showInfo("Buscando informações do boleto...");
      const billetInfo = await getBilletInfo(score.id);
      if (billetInfo) {
        setBilletDataModalScore({ ...score, billetInfo });
      } else {
        showError("Não foi possível buscar as informações do boleto");
      }
    } catch (error) {
      showError("Erro ao buscar boleto");
      console.error(error);
    }
  };

  const handleGenerateInvoice = async (
    scoreId: number,
    dadosAdicionais?: string,
    options?: { alreadyProcessing?: boolean },
  ) => {
    if (!options?.alreadyProcessing && !beginProcessing(scoreId)) return;
    try {
      const score = scores.find((s) => s.id === scoreId);
      if (!score) {
        showError("Agrupamento não encontrado");
        return;
      }

      const response = await generateInvoice(scoreId, dadosAdicionais);

      if (response.ref) {
        try {
          const danfeBlob = await getDanfe(response.ref);

          const invoiceInfo = await getInvoiceInfo(response.ref);

          setInvoiceResultModal({
            score: {
              ...score,
              invoiceRef: response.ref,
              invoiceInfo: invoiceInfo,
            },
            pdf: danfeBlob,
          });

          showSuccess("Nota fiscal gerada com sucesso!");
          fetchScores();
        } catch (_danfeError) {
          fetchScores();

          showInfo(
            "Nota fiscal gerada! O documento está sendo processado e estará disponível em alguns instantes. Clique em 'Ver NF' para visualizar.",
          );
        }
      }
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao gerar nota fiscal",
      );
      console.error(error);
      fetchScores();
    } finally {
      endProcessing(scoreId);
    }
  };

  const handleInvoiceButtonClick = async (score: ScoreWithBilletInfo) => {
    // Marca como "processando" já no clique (mostra o overlay de carregamento
    // imediatamente) em vez de só depois da checagem do cliente abaixo, que faz
    // uma chamada de rede e deixava a tela parecendo travada até ela responder.
    if (!beginProcessing(score.id)) return;
    try {
      const clientData = await getClientById(score.clientId);
      const firstName =
        clientData.clientName?.split(/\s+/)[0]?.toUpperCase()?.trim() || "";
      const isLlineaClient = firstName.includes("LLINEA");

      if (isLlineaClient) {
        // A geração real só ocorre depois que o usuário preencher o modal,
        // então libera o "processando" aqui — ele será reativado ao confirmar.
        endProcessing(score.id);
        setPendingInvoiceScore(score);
        setPendingCombinedFlow(false);
        setShowAdditionalDataModal(true);
      } else {
        await handleGenerateInvoice(score.id, undefined, {
          alreadyProcessing: true,
        });
      }
    } catch (error) {
      // Não gera a NF sem confirmar se o cliente exige a descrição especial:
      // se não foi possível checar o cliente, é mais seguro abortar do que
      // arriscar gerar a NF sem o preenchimento obrigatório.
      console.error("Erro ao verificar cliente:", error);
      endProcessing(score.id);
      showError(
        "Não foi possível verificar os dados do cliente. Tente novamente.",
      );
    }
  };

  // Gera a NF e, em seguida, o boleto vinculado a ela (número da NF usado como identificador do
  // boleto). Se qualquer etapa após a emissão da NF falhar, o backend cancela a NF automaticamente.
  const handleGenerateInvoiceAndBillet = async (
    scoreId: number,
    dadosAdicionais?: string,
    options?: { alreadyProcessing?: boolean },
  ) => {
    if (!options?.alreadyProcessing && !beginProcessing(scoreId)) return;
    const score = scores.find((s) => s.id === scoreId);
    if (!score) {
      showError("Agrupamento não encontrado");
      endProcessing(scoreId);
      return;
    }

    try {
      const result = await generateInvoiceWithBillet(scoreId, dadosAdicionais);

      setInvoiceBilletResultModal({
        score: { ...score, invoiceRef: result.invoiceRef },
        result,
      });

      showSuccess("Nota fiscal e boleto vinculado gerados com sucesso!");
      fetchScores();
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Erro ao gerar nota fiscal e boleto",
      );
      console.error(error);
      fetchScores();
    } finally {
      endProcessing(scoreId);
    }
  };

  const handleCombinedButtonClick = async (score: ScoreWithBilletInfo) => {
    // Mesmo raciocínio de handleInvoiceButtonClick: mostra o overlay já no
    // clique, e não gera a NF+boleto sem confirmar a exigência de descrição
    // especial caso a checagem do cliente falhe.
    if (!beginProcessing(score.id)) return;
    try {
      const clientData = await getClientById(score.clientId);
      const firstName =
        clientData.clientName?.split(/\s+/)[0]?.toUpperCase()?.trim() || "";
      const isLlineaClient = firstName.includes("LLINEA");

      if (isLlineaClient) {
        endProcessing(score.id);
        setPendingInvoiceScore(score);
        setPendingCombinedFlow(true);
        setShowAdditionalDataModal(true);
      } else {
        await handleGenerateInvoiceAndBillet(score.id, undefined, {
          alreadyProcessing: true,
        });
      }
    } catch (error) {
      console.error("Erro ao verificar cliente:", error);
      endProcessing(score.id);
      showError(
        "Não foi possível verificar os dados do cliente. Tente novamente.",
      );
    }
  };

  const handleShowInvoice = async (score: ScoreWithBilletInfo) => {
    // Se já tem a referência e info da invoice
    if (score.invoiceRef && score.invoiceInfo) {
      setInvoiceDataModalScore(score);
      return;
    }

    if (!score.invoiceRef) {
      showError("Referência da nota fiscal não encontrada");
      return;
    }

    // Tem a referência mas não tem a info, busca novamente
    showInfo("Buscando nota fiscal...");
    try {
      const invoiceInfo = await getInvoiceInfo(score.invoiceRef);
      setInvoiceDataModalScore({ ...score, invoiceInfo });
    } catch (error) {
      console.error(error);
      // A nota nunca chegou a ser autorizada pela Sefaz (rejeitada/denegada/cancelada) — o
      // agrupamento ficou com hasInvoice=true indevidamente. Reconcilia automaticamente com a
      // Focus NFe: se confirmado que a nota foi mesmo rejeitada, libera o agrupamento (hasInvoice
      // volta a false) para que "Gerar NF" e "Deletar" reapareçam no card.
      try {
        const reconcileResult = await reconcileInvoiceStatus(score.id);
        await fetchScores();
        showInfo(reconcileResult);
      } catch (reconcileError) {
        console.error(reconcileError);
        showError(
          error instanceof Error
            ? error.message
            : "Erro ao buscar informações da nota fiscal",
        );
      }
    }
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Não definida";
    try {
      const datePart = dateString.split("T")[0];
      const [year, month, day] = datePart.split("-");
      return `${day}/${month}/${year}`;
    } catch {
      return dateString;
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PAGO":
        return "bg-green-100 text-green-800";
      case "PENDENTE":
        return "bg-blue-100 text-blue-800";
      case "BAIXADO":
      case "CANCELADO":
      case "CANCELADO_BOLETO":
        return "bg-red-100 text-red-800";
      default:
        return "bg-blue-100 text-blue-800";
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case "PAID":
        return "PAGO";
      case "PENDING":
        return "PENDENTE";
      case "OVERDUE":
        return "VENCIDO";
      case "CANCELLED":
        return "CANCELADO";
      case "CANCELADO_BOLETO":
        return "BOLETO CANCELADO";
      default:
        return status;
    }
  };

  // Verifica se o boleto está em aberto (não permite deletar)
  const isBilletOpen = (score: ScoreWithBilletInfo): boolean => {
    if (!score.billetInfo) return false;
    const status = score.billetInfo.situacaoBoleto.toLowerCase();
    return status.includes("aberto") || status.includes("pendente");
  };

  if (!clientId) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>Selecione um cliente para visualizar os agrupamentos</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {client?.onlyBillet && (
        <div className="flex justify-end">
          <button
            type="button"
            onClick={() => setShowWildcardBilletModal(true)}
            className="flex items-center justify-center gap-2 px-4 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm cursor-pointer"
          >
            <FileText className="w-4 h-4" />
            Gerar Boleto
          </button>
        </div>
      )}

      {/* Loading state */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div
              // biome-ignore lint/suspicious/noArrayIndexKey: static-length skeleton placeholder list, no stable identity available
              key={i}
              className="h-72 bg-gray-200 animate-pulse rounded-lg"
            />
          ))}
        </div>
      ) : scores.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <FileText className="w-12 h-12 mx-auto mb-3 opacity-50" />
          <p>Nenhum agrupamento encontrado</p>
          <p className="text-sm mt-2">
            Use o botão "Criar Agrupamento por Período" na aba anterior
          </p>
        </div>
      ) : (
        <>
          {/* Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {scores.map((score) => (
              <div
                key={score.id}
                className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow"
              >
                {/* Header do Card */}
                <div className="flex justify-between gap-2 flex-wrap items-start mb-4">
                  <div>
                    <h3 className="font-semibold text-lg">
                      Agrupamento {score.number || score.id}
                    </h3>
                    <p className="text-sm text-gray-500 flex items-center gap-1 mt-1">
                      <Calendar className="w-3 h-3" />
                      {formatDate(score.confirmedAt)}
                    </p>
                  </div>
                </div>

                {/* Informações */}
                <div className="space-y-2 mb-4 pb-4 border-b">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Valor Total:</span>
                    <span className="font-semibold">
                      {formatCurrency(score.totalValue)}
                    </span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Vencimento:</span>
                    <span>{formatDate(score.dueDate)}</span>
                  </div>
                  <span
                    className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(
                      score.status,
                    )}`}
                  >
                    {getStatusLabel(score.status)}
                  </span>
                </div>

                {/* Ações */}
                <div className="space-y-2">
                  {/* Botão Ver Produtos */}
                  <button
                    type="button"
                    onClick={() => handleViewProducts(score)}
                    className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm cursor-pointer"
                  >
                    <Eye className="w-4 h-4" />
                    Ver Produtos
                  </button>

                  {/* Botão Ver Fotos — comprovantes de compras deste agrupamento que vieram de
                      captura por celular de clientes que exigem foto (ver Client.requiresPurchaseProof) */}
                  <button
                    type="button"
                    onClick={() => setImagesModalScore(score)}
                    className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-purple-700 text-white rounded-lg hover:bg-purple-800 transition-colors text-sm cursor-pointer"
                  >
                    <Camera className="w-4 h-4" />
                    Ver Fotos
                  </button>

                  {/* Botões de Boleto e Nota Fiscal */}
                  <div className="space-y-2">
                    {!client?.onlyBillet &&
                      !score.hasBillet &&
                      !score.hasInvoice && (
                        <button
                          type="button"
                          onClick={() => handleCombinedButtonClick(score)}
                          disabled={processingScoreIds.has(score.id)}
                          className="w-full flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <FileText className="w-3 h-3" />
                          {processingScoreIds.has(score.id)
                            ? "Gerando..."
                            : "Gerar NF + Boleto"}
                        </button>
                      )}

                    <div
                      className={`grid gap-2 ${client?.onlyBillet ? "grid-cols-1" : "grid-cols-2"}`}
                    >
                      {score.hasBillet ? (
                        <button
                          type="button"
                          onClick={() => handleShowBillet(score)}
                          className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors text-xs cursor-pointer"
                        >
                          <Info className="w-3 h-3" />
                          Ver Boleto
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() =>
                            setClientNumberModal({
                              state: true,
                              groupId: score.id,
                            })
                          }
                          disabled={processingScoreIds.has(score.id)}
                          className="flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <FileText className="w-3 h-3" />
                          {processingScoreIds.has(score.id)
                            ? "Gerando..."
                            : "Gerar Boleto"}
                        </button>
                      )}

                      {!client?.onlyBillet &&
                        (score.hasInvoice ? (
                          <button
                            type="button"
                            onClick={() => handleShowInvoice(score)}
                            className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors text-xs cursor-pointer"
                          >
                            <Info className="w-3 h-3" />
                            Ver NF
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleInvoiceButtonClick(score)}
                            disabled={processingScoreIds.has(score.id)}
                            className="flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            <FileText className="w-3 h-3" />
                            {processingScoreIds.has(score.id)
                              ? "Gerando..."
                              : "Gerar NF"}
                          </button>
                        ))}
                    </div>
                  </div>

                  {/* Confirmar Pagamento e Deletar */}
                  <div className="grid grid-cols-2 gap-2">
                    {!score.hasBillet && (
                      <button
                        type="button"
                        onClick={() => handleTogglePayment(score)}
                        disabled={
                          (score.hasInvoice && score.status === "PAGO") ||
                          actionProcessingIds.has(score.id)
                        }
                        className={`flex items-center justify-center gap-1 px-2 py-2 rounded-lg transition-colors text-xs cursor-pointer text-white bg-primary disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[var(--primary-dark)] ${score.hasInvoice ? "col-span-2 w-full" : ""}`}
                      >
                        <CheckCircle className="w-3 h-3" />
                        Confirmar Pag.
                      </button>
                    )}
                    {/* Esconde botão deletar se boleto ou fatura estiver em aberto */}
                    {!isBilletOpen(score) && !score.hasInvoice && (
                      <button
                        type="button"
                        onClick={() => setDeleteConfirmScore(score)}
                        disabled={actionProcessingIds.has(score.id)}
                        className="flex items-center justify-center gap-1 px-2 py-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <Trash2 className="w-3 h-3" />
                        Deletar
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Paginação */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Anterior
              </button>
              <span className="text-sm">
                Página {page + 1} de {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Próxima
              </button>
            </div>
          )}
        </>
      )}

      {/* Modal de produtos */}
      {productsModalScore && (
        <GroupedProductsModal
          combinedScoreId={productsModalScore.id}
          scoreNumber={productsModalScore.number}
          onClose={() => setProductsModalScore(null)}
        />
      )}

      {/* Modal de fotos (comprovantes) */}
      {imagesModalScore && (
        <CombinedScoreImagesModal
          combinedScoreId={imagesModalScore.id}
          scoreNumber={imagesModalScore.number}
          onClose={() => setImagesModalScore(null)}
        />
      )}

      <ConfirmDeleteModal
        open={!!deleteConfirmScore}
        onClose={() => setDeleteConfirmScore(null)}
        confirmDisabled={
          deleteConfirmScore
            ? actionProcessingIds.has(deleteConfirmScore.id)
            : false
        }
        onConfirm={() => {
          if (deleteConfirmScore) {
            handleDelete(deleteConfirmScore.id);
          }
          setDeleteConfirmScore(null);
        }}
        title={`Tem certeza que deseja deletar o agrupamento ${
          deleteConfirmScore?.number || deleteConfirmScore?.id
        }? Esta ação não pode ser desfeita.`}
      />

      <WildcardBilletModal
        open={showWildcardBilletModal}
        onClose={() => setShowWildcardBilletModal(false)}
        onConfirm={(number, value, dueDate) => {
          setShowWildcardBilletModal(false);
          handleGenerateWildcardBillet(number, value, dueDate);
        }}
      />

      {/* Modal de boleto recém-gerado */}
      {billetResultModal && (
        <ShowBilletModal
          isOpen={true}
          onClose={() => setBilletResultModal(null)}
          billetData={billetResultModal.pdf}
          scoreNumber={
            billetResultModal.score.number || billetResultModal.score.id
          }
          clientNumber={billetResultModal.clientNumber}
        />
      )}

      {billetDataModalScore?.billetInfo && (
        <ShowBilletDataModal
          isOpen={true}
          onClose={() => setBilletDataModalScore(null)}
          billetData={billetDataModalScore.billetInfo}
          combinedScoreId={billetDataModalScore.id}
          clientNumber={
            billetDataModalScore.number ||
            billetDataModalScore.billetInfo?.seuNumero ||
            null
          }
          onBilletCancelled={() => {
            fetchScores();
          }}
        />
      )}

      <ClientNumberModal
        open={clientNumberModal.state}
        onClose={() => setClientNumberModal({ state: false, groupId: -1 })}
        onConfirm={(number, dueDate) => {
          setClientNumberModal({ state: false, groupId: -1 });
          handleGenerateBillet(clientNumberModal.groupId, number, dueDate);
        }}
      />

      {/* Modal de Nota Fiscal recém-gerada */}
      {invoiceResultModal && (
        <ShowInvoiceModal
          isOpen={true}
          onClose={() => setInvoiceResultModal(null)}
          invoiceData={invoiceResultModal.pdf}
          scoreNumber={
            invoiceResultModal.score.number || invoiceResultModal.score.id
          }
          ref={invoiceResultModal.score.invoiceRef || ""}
          invoiceNumber={invoiceResultModal.score.invoiceInfo?.number}
        />
      )}

      {invoiceDataModalScore?.invoiceInfo && (
        <ShowInvoiceDataModal
          isOpen={true}
          onClose={() => setInvoiceDataModalScore(null)}
          invoiceData={invoiceDataModalScore.invoiceInfo}
          onInvoiceCancelled={() => {
            fetchScores();
          }}
        />
      )}

      {/* Modal de Dados Adicionais */}
      {showAdditionalDataModal && pendingInvoiceScore && (
        <AdditionalDataModal
          isOpen={showAdditionalDataModal}
          onClose={() => {
            setShowAdditionalDataModal(false);
            setPendingInvoiceScore(null);
            setPendingCombinedFlow(false);
          }}
          onConfirm={(dadosAdicionais) => {
            setShowAdditionalDataModal(false);
            if (pendingInvoiceScore) {
              if (pendingCombinedFlow) {
                handleGenerateInvoiceAndBillet(
                  pendingInvoiceScore.id,
                  dadosAdicionais,
                );
              } else {
                handleGenerateInvoice(pendingInvoiceScore.id, dadosAdicionais);
              }
            }
            setPendingInvoiceScore(null);
            setPendingCombinedFlow(false);
          }}
          scoreNumber={pendingInvoiceScore.number || pendingInvoiceScore.id}
        />
      )}

      <GameLoadingOverlay
        isOpen={processingScoreIds.size > 0}
        title="Gerando documento"
        messages={[
          "Conectando aos servidores fiscais...",
          "Processando nota fiscal e/ou boleto...",
          "Isso pode levar alguns instantes...",
          "Quase lá...",
        ]}
      />

      {/* Modal de NF + Boleto gerados juntos */}
      {invoiceBilletResultModal && (
        <ShowInvoiceAndBilletModal
          isOpen={true}
          onClose={() => setInvoiceBilletResultModal(null)}
          danfeBlob={invoiceBilletResultModal.result.danfeBlob}
          xmlBlob={invoiceBilletResultModal.result.xmlBlob}
          billetBlob={invoiceBilletResultModal.result.billetBlob}
          scoreNumber={
            invoiceBilletResultModal.score.number ||
            invoiceBilletResultModal.score.id
          }
          invoiceNumber={invoiceBilletResultModal.result.invoiceNumber}
          billetNumber={invoiceBilletResultModal.result.billetNumber}
        />
      )}
    </div>
  );
}
