import type { BilletResponse } from "@/types/billetType";
import type { CombinedScoreType } from "@/types/combinedScoreType";
import type { InvoiceResponseGet } from "@/types/invoiceType";

export interface ScoreWithBilletInfo extends CombinedScoreType {
  billetInfo?: BilletResponse | null;
  invoiceInfo?: InvoiceResponseGet | null;
  invoiceRef?: string | null;
}

/**
 * Estados de navegação síncrona (aberto por um único clique do usuário) — só um por vez faz
 * sentido, então virou um discriminated union em vez de 10 variáveis booleanas/nuláveis
 * independentes.
 *
 * Os modais de RESULTADO assíncrono (boleto/NF/NF+boleto recém-gerados) ficam de fora
 * propositalmente — ver comentário em CombinedScoresCards.tsx sobre por quê.
 */
export type ScoreModalState =
  | { type: "none" }
  | { type: "clientNumber"; groupId: number }
  | { type: "wildcardBillet" }
  | { type: "deleteConfirm"; score: ScoreWithBilletInfo }
  | { type: "products"; score: ScoreWithBilletInfo }
  | { type: "images"; score: ScoreWithBilletInfo }
  | { type: "billetData"; score: ScoreWithBilletInfo }
  | { type: "invoiceData"; score: ScoreWithBilletInfo }
  | {
      type: "additionalData";
      score: ScoreWithBilletInfo;
      combinedFlow: boolean;
    };
