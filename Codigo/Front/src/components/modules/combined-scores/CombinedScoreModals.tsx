import AdditionalDataModal from "@/components/modals/AdditionalDataModal";
import ClientNumberModal from "@/components/modals/ClientNumberModal";
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
import type { InvoiceWithBilletResult } from "@/types/invoiceType";
import type { ScoreModalState, ScoreWithBilletInfo } from "./types";

export interface BilletResultModalState {
  score: ScoreWithBilletInfo;
  pdf: Blob;
  clientNumber: string | null;
}

export interface InvoiceResultModalState {
  score: ScoreWithBilletInfo;
  pdf: Blob;
}

export interface InvoiceBilletResultModalState {
  score: ScoreWithBilletInfo;
  result: InvoiceWithBilletResult;
}

interface CombinedScoreModalsProps {
  modal: ScoreModalState;
  onCloseModal: () => void;
  isActionProcessing: (id: number) => boolean;
  onConfirmDelete: (id: number) => void;
  onConfirmWildcardBillet: (
    number: string,
    value: number,
    dueDate?: string,
  ) => void;
  onConfirmClientNumber: (
    groupId: number,
    number: string,
    dueDate?: string,
  ) => void;
  onConfirmAdditionalData: (
    scoreId: number,
    combinedFlow: boolean,
    dadosAdicionais: string,
  ) => void;
  onRefetch: () => void;

  // Modais de resultado assíncrono — ver comentário em CombinedScoresCards.tsx sobre por que
  // ficam fora do discriminated union acima.
  billetResultModal: BilletResultModalState | null;
  onCloseBilletResult: () => void;
  invoiceResultModal: InvoiceResultModalState | null;
  onCloseInvoiceResult: () => void;
  invoiceBilletResultModal: InvoiceBilletResultModalState | null;
  onCloseInvoiceBilletResult: () => void;

  isGenerating: boolean;
}

export default function CombinedScoreModals({
  modal,
  onCloseModal,
  isActionProcessing,
  onConfirmDelete,
  onConfirmWildcardBillet,
  onConfirmClientNumber,
  onConfirmAdditionalData,
  onRefetch,
  billetResultModal,
  onCloseBilletResult,
  invoiceResultModal,
  onCloseInvoiceResult,
  invoiceBilletResultModal,
  onCloseInvoiceBilletResult,
  isGenerating,
}: CombinedScoreModalsProps) {
  return (
    <>
      {/* Modal de produtos */}
      {modal.type === "products" && (
        <GroupedProductsModal
          combinedScoreId={modal.score.id}
          scoreNumber={modal.score.number}
          onClose={onCloseModal}
        />
      )}

      {/* Modal de fotos (comprovantes) */}
      {modal.type === "images" && (
        <CombinedScoreImagesModal
          combinedScoreId={modal.score.id}
          scoreNumber={modal.score.number}
          onClose={onCloseModal}
        />
      )}

      <ConfirmDeleteModal
        open={modal.type === "deleteConfirm"}
        onClose={onCloseModal}
        confirmDisabled={
          modal.type === "deleteConfirm"
            ? isActionProcessing(modal.score.id)
            : false
        }
        onConfirm={() => {
          if (modal.type === "deleteConfirm") {
            onConfirmDelete(modal.score.id);
          }
          onCloseModal();
        }}
        title={`Tem certeza que deseja deletar o agrupamento ${
          modal.type === "deleteConfirm"
            ? modal.score.number || modal.score.id
            : ""
        }? Esta ação não pode ser desfeita.`}
      />

      <WildcardBilletModal
        open={modal.type === "wildcardBillet"}
        onClose={onCloseModal}
        onConfirm={(number, value, dueDate) => {
          onCloseModal();
          onConfirmWildcardBillet(number, value, dueDate);
        }}
      />

      {/* Modal de boleto recém-gerado */}
      {billetResultModal && (
        <ShowBilletModal
          isOpen={true}
          onClose={onCloseBilletResult}
          billetData={billetResultModal.pdf}
          scoreNumber={
            billetResultModal.score.number || billetResultModal.score.id
          }
          clientNumber={billetResultModal.clientNumber}
        />
      )}

      {modal.type === "billetData" && modal.score.billetInfo && (
        <ShowBilletDataModal
          isOpen={true}
          onClose={onCloseModal}
          billetData={modal.score.billetInfo}
          combinedScoreId={modal.score.id}
          clientNumber={
            modal.score.number || modal.score.billetInfo?.seuNumero || null
          }
          onBilletCancelled={onRefetch}
        />
      )}

      <ClientNumberModal
        open={modal.type === "clientNumber"}
        onClose={onCloseModal}
        onConfirm={(number, dueDate) => {
          if (modal.type === "clientNumber") {
            const { groupId } = modal;
            onCloseModal();
            onConfirmClientNumber(groupId, number, dueDate);
          }
        }}
      />

      {/* Modal de Nota Fiscal recém-gerada */}
      {invoiceResultModal && (
        <ShowInvoiceModal
          isOpen={true}
          onClose={onCloseInvoiceResult}
          invoiceData={invoiceResultModal.pdf}
          scoreNumber={
            invoiceResultModal.score.number || invoiceResultModal.score.id
          }
          ref={invoiceResultModal.score.invoiceRef || ""}
          invoiceNumber={invoiceResultModal.score.invoiceInfo?.number}
        />
      )}

      {modal.type === "invoiceData" && modal.score.invoiceInfo && (
        <ShowInvoiceDataModal
          isOpen={true}
          onClose={onCloseModal}
          invoiceData={modal.score.invoiceInfo}
          onInvoiceCancelled={onRefetch}
        />
      )}

      {/* Modal de Dados Adicionais */}
      {modal.type === "additionalData" && (
        <AdditionalDataModal
          isOpen={true}
          onClose={onCloseModal}
          onConfirm={(dadosAdicionais) => {
            const { score, combinedFlow } = modal;
            onCloseModal();
            onConfirmAdditionalData(score.id, combinedFlow, dadosAdicionais);
          }}
          scoreNumber={modal.score.number || modal.score.id}
        />
      )}

      <GameLoadingOverlay
        isOpen={isGenerating}
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
          onClose={onCloseInvoiceBilletResult}
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
    </>
  );
}
