import { useCallback, useEffect, useState } from "react";
import { combinedScoreService } from "@/services/combinedScoreService";
import type { BilletResponse } from "@/types/billetType";
import type { ClientResponse } from "@/types/clientType";
import type { InvoiceResponseGet } from "@/types/invoiceType";
import { showError } from "@/utils/toastUtils";
import type { ScoreWithBilletInfo } from "./types";

function deriveStatusFromBillet(
  currentStatus: string,
  billetInfo: BilletResponse,
): string {
  const billetStatus = billetInfo.situacaoBoleto.toLowerCase();
  if (billetStatus.includes("liquidado") || billetStatus.includes("pago")) {
    return "PAID";
  }
  if (billetStatus.includes("cancelado") || billetStatus.includes("baixado")) {
    return "CANCELLED";
  }
  if (billetStatus.includes("aberto") || billetStatus.includes("pendente")) {
    const vencimento = new Date(billetInfo.dataVencimento);
    const hoje = new Date();
    return vencimento < hoje ? "OVERDUE" : "PENDING";
  }
  return currentStatus;
}

interface UseCombinedScoresParams {
  clientId?: number;
  refreshKey?: number;
  getBilletInfo: (combinedScoreId: number) => Promise<BilletResponse | null>;
  getInvoiceInfo: (ref: string) => Promise<InvoiceResponseGet>;
  getClientById: (clientId: number) => Promise<ClientResponse>;
}

export function useCombinedScores({
  clientId,
  refreshKey,
  getBilletInfo,
  getInvoiceInfo,
  getClientById,
}: UseCombinedScoresParams) {
  const [scores, setScores] = useState<ScoreWithBilletInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [client, setClient] = useState<ClientResponse | null>(null);

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

      const scoresWithInfo = await Promise.all(
        data.content.map(async (score) => {
          let billetInfo: BilletResponse | null = null;
          let invoiceInfo: InvoiceResponseGet | null = null;
          let status = score.status;

          if (score.hasBillet) {
            try {
              billetInfo = await getBilletInfo(score.id);
              if (billetInfo) {
                status = deriveStatusFromBillet(status, billetInfo);
              }
            } catch (error) {
              console.error(
                `Erro ao buscar info do boleto ${score.id}:`,
                error,
              );
            }
          }

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
            status,
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

  return {
    scores,
    loading,
    page,
    setPage,
    totalPages,
    client,
    refetch: fetchScores,
  };
}
