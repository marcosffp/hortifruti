"use client";

import { FileText } from "lucide-react";
import { useCallback, useRef, useState } from "react";
import CombinedScoreCard from "@/components/modules/combined-scores/CombinedScoreCard";
import CombinedScoreModals, {
  type BilletResultModalState,
  type InvoiceBilletResultModalState,
  type InvoiceResultModalState,
} from "@/components/modules/combined-scores/CombinedScoreModals";
import { clientRequiresAdditionalInvoiceData } from "@/components/modules/combined-scores/fiscalRules";
import { useCombinedScores } from "@/components/modules/combined-scores/useCombinedScores";
import { useBillet } from "@/hooks/useBillet";
import { useClient } from "@/hooks/useClient";
import { useCombinedScore } from "@/hooks/useCombinedScore";
import { useInvoice } from "@/hooks/useInvoice";
import { showError, showInfo, showSuccess } from "@/utils/toastUtils";
import type {
  ScoreModalState,
  ScoreWithBilletInfo,
} from "./combined-scores/types";

interface CombinedScoresCardsProps {
  clientId?: number;
  refreshKey?: number;
}

export default function CombinedScoresCards({
  clientId,
  refreshKey,
}: CombinedScoresCardsProps) {
  const { generateBillet, getBilletInfo } = useBillet();
  const {
    generateInvoice,
    generateInvoiceWithBillet,
    getInvoiceInfo,
    getDanfe,
    reconcileInvoiceStatus,
  } = useInvoice();
  const { getClientById } = useClient();
  const {
    cancelGrouping: cancelGroupingRequest,
    cancelPayment: cancelPaymentRequest,
    confirmPayment: confirmPaymentRequest,
    createWildcardBillet,
  } = useCombinedScore();

  const { scores, loading, page, setPage, totalPages, client, refetch } =
    useCombinedScores({
      clientId,
      refreshKey,
      getBilletInfo,
      getInvoiceInfo,
      getClientById,
    });

  // Estado de navegação síncrona (o que o usuário abriu clicando em um botão) — ver
  // ScoreModalState em combined-scores/types.ts para o porquê do discriminated union.
  const [modal, setModal] = useState<ScoreModalState>({ type: "none" });
  const closeModal = useCallback(() => setModal({ type: "none" }), []);

  // Os 3 modais de RESULTADO assíncrono abaixo ficam FORA do union acima de propósito.
  // Cada um guarda seu PRÓPRIO agrupamento (score), em vez de compartilhar o mesmo slot
  // de "modal ativo". Isso evita que duas operações assíncronas para agrupamentos
  // diferentes (ex.: gerar NF+boleto do 349, que demora minutos, enquanto o usuário abre
  // "Ver Boleto" do 335 nesse meio tempo) acabem se sobrescrevendo: se estivessem no mesmo
  // union, a resposta tardia do 349 fecharia à força o modal que o usuário tem aberto do 335.
  const [billetResultModal, setBilletResultModal] =
    useState<BilletResultModalState | null>(null);
  const [invoiceResultModal, setInvoiceResultModal] =
    useState<InvoiceResultModalState | null>(null);
  const [invoiceBilletResultModal, setInvoiceBilletResultModal] =
    useState<InvoiceBilletResultModalState | null>(null);

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

  const handleDelete = async (id: number) => {
    if (!beginAction(id)) return;

    try {
      await cancelGroupingRequest(id);
      showSuccess("Agrupamento deletado com sucesso");
      refetch();
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
        await cancelPaymentRequest(score.id);
        showSuccess("Pagamento cancelado com sucesso");
      } else {
        await confirmPaymentRequest(score.id);
        showSuccess("Pagamento confirmado com sucesso");
      }
      refetch();
    } catch (error) {
      showError("Erro ao atualizar pagamento");
      console.error(error);
    } finally {
      endAction(score.id);
    }
  };

  const handleViewProducts = (score: ScoreWithBilletInfo) => {
    setModal({ type: "products", score });
  };

  const handleGenerateBillet = async (
    scoreId: number,
    clientNumber: string,
    dueDate?: string,
    useStandardFileName?: boolean,
  ) => {
    if (!beginProcessing(scoreId)) return;
    try {
      const score = scores.find((s) => s.id === scoreId);
      if (!score) {
        showError("Agrupamento não encontrado");
        return;
      }

      const pdfBlob = await generateBillet(scoreId, clientNumber, dueDate, {
        clientName: client?.clientName,
        useStandardFileName,
      });

      setBilletResultModal({
        score,
        pdf: pdfBlob,
        clientNumber,
        useStandardFileName,
      });

      showSuccess("Boleto gerado com sucesso!");

      refetch();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao gerar boleto",
      );
      console.error(error);
      // O backend pode ter reconciliado e registrado o boleto como emitido mesmo reportando um
      // erro (ex: falha só no pós-processamento local) — resincroniza a tela com o estado real
      // em vez de continuar mostrando "sem boleto" para um boleto que já existe.
      refetch();
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
      const newScoreId = await createWildcardBillet(clientId, value);
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
      refetch();
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
        setModal({ type: "billetData", score });
        return;
      }

      // billetInfo não foi carregado no fetchScores — tenta buscar agora
      showInfo("Buscando informações do boleto...");
      const billetInfo = await getBilletInfo(score.id);
      if (billetInfo) {
        setModal({ type: "billetData", score: { ...score, billetInfo } });
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
          refetch();
        } catch (_danfeError) {
          refetch();

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
      refetch();
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
      const requiresAdditionalData = clientRequiresAdditionalInvoiceData(
        clientData.clientName,
      );

      if (requiresAdditionalData) {
        // A geração real só ocorre depois que o usuário preencher o modal,
        // então libera o "processando" aqui — ele será reativado ao confirmar.
        endProcessing(score.id);
        setModal({ type: "additionalData", score, combinedFlow: false });
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
      refetch();
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Erro ao gerar nota fiscal e boleto",
      );
      console.error(error);
      refetch();
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
      const requiresAdditionalData = clientRequiresAdditionalInvoiceData(
        clientData.clientName,
      );

      if (requiresAdditionalData) {
        endProcessing(score.id);
        setModal({ type: "additionalData", score, combinedFlow: true });
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
    if (score.invoiceRef && score.invoiceInfo) {
      setModal({ type: "invoiceData", score });
      return;
    }

    if (!score.invoiceRef) {
      showError("Referência da nota fiscal não encontrada");
      return;
    }

    showInfo("Buscando nota fiscal...");
    try {
      const invoiceInfo = await getInvoiceInfo(score.invoiceRef);
      setModal({ type: "invoiceData", score: { ...score, invoiceInfo } });
    } catch (error) {
      console.error(error);
      // A nota nunca chegou a ser autorizada pela Sefaz (rejeitada/denegada/cancelada) — o
      // agrupamento ficou com hasInvoice=true indevidamente. Reconcilia automaticamente com a
      // Focus NFe: se confirmado que a nota foi mesmo rejeitada, libera o agrupamento (hasInvoice
      // volta a false) para que "Gerar NF" e "Deletar" reapareçam no card.
      try {
        const reconcileResult = await reconcileInvoiceStatus(score.id);
        await refetch();
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

  const handleConfirmAdditionalData = (
    scoreId: number,
    combinedFlow: boolean,
    dadosAdicionais: string,
  ) => {
    if (combinedFlow) {
      handleGenerateInvoiceAndBillet(scoreId, dadosAdicionais);
    } else {
      handleGenerateInvoice(scoreId, dadosAdicionais);
    }
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
            onClick={() => setModal({ type: "wildcardBillet" })}
            className="flex items-center justify-center gap-2 px-4 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm cursor-pointer"
          >
            <FileText className="w-4 h-4" />
            Gerar Boleto
          </button>
        </div>
      )}

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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {scores.map((score) => (
              <CombinedScoreCard
                key={score.id}
                score={score}
                client={client}
                isProcessing={processingScoreIds.has(score.id)}
                isActionProcessing={actionProcessingIds.has(score.id)}
                onViewProducts={handleViewProducts}
                onViewImages={(s) => setModal({ type: "images", score: s })}
                onCombinedClick={handleCombinedButtonClick}
                onShowBillet={handleShowBillet}
                onOpenClientNumberModal={(groupId) =>
                  setModal({ type: "clientNumber", groupId })
                }
                onShowInvoice={handleShowInvoice}
                onInvoiceClick={handleInvoiceButtonClick}
                onTogglePayment={handleTogglePayment}
                onDeleteClick={(s) =>
                  setModal({ type: "deleteConfirm", score: s })
                }
              />
            ))}
          </div>

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

      <CombinedScoreModals
        modal={modal}
        clientName={client?.clientName ?? null}
        onCloseModal={closeModal}
        isActionProcessing={(id) => actionProcessingIds.has(id)}
        onConfirmDelete={handleDelete}
        onConfirmWildcardBillet={handleGenerateWildcardBillet}
        onConfirmClientNumber={handleGenerateBillet}
        onConfirmAdditionalData={handleConfirmAdditionalData}
        onRefetch={refetch}
        billetResultModal={billetResultModal}
        onCloseBilletResult={() => setBilletResultModal(null)}
        invoiceResultModal={invoiceResultModal}
        onCloseInvoiceResult={() => setInvoiceResultModal(null)}
        invoiceBilletResultModal={invoiceBilletResultModal}
        onCloseInvoiceBilletResult={() => setInvoiceBilletResultModal(null)}
        isGenerating={processingScoreIds.size > 0}
      />
    </div>
  );
}
