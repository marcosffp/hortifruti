import {
  Calendar,
  Camera,
  CheckCircle,
  Eye,
  FileText,
  Info,
  Trash2,
} from "lucide-react";
import type { ClientResponse } from "@/types/clientType";
import {
  formatCurrency,
  formatDate,
  getStatusColor,
  getStatusLabel,
  isBilletOpen,
} from "./formatters";
import type { ScoreWithBilletInfo } from "./types";

interface CombinedScoreCardProps {
  score: ScoreWithBilletInfo;
  client: ClientResponse | null;
  isProcessing: boolean;
  isActionProcessing: boolean;
  onViewProducts: (score: ScoreWithBilletInfo) => void;
  onViewImages: (score: ScoreWithBilletInfo) => void;
  onCombinedClick: (score: ScoreWithBilletInfo) => void;
  onShowBillet: (score: ScoreWithBilletInfo) => void;
  onOpenClientNumberModal: (groupId: number) => void;
  onShowInvoice: (score: ScoreWithBilletInfo) => void;
  onInvoiceClick: (score: ScoreWithBilletInfo) => void;
  onTogglePayment: (score: ScoreWithBilletInfo) => void;
  onDeleteClick: (score: ScoreWithBilletInfo) => void;
}

export default function CombinedScoreCard({
  score,
  client,
  isProcessing,
  isActionProcessing,
  onViewProducts,
  onViewImages,
  onCombinedClick,
  onShowBillet,
  onOpenClientNumberModal,
  onShowInvoice,
  onInvoiceClick,
  onTogglePayment,
  onDeleteClick,
}: CombinedScoreCardProps) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
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

      <div className="space-y-2">
        <button
          type="button"
          onClick={() => onViewProducts(score)}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm cursor-pointer"
        >
          <Eye className="w-4 h-4" />
          Ver Produtos
        </button>

        {/* Botão Ver Fotos — comprovantes de compras deste agrupamento que vieram de
            captura por celular de clientes que exigem foto (ver Client.requiresPurchaseProof) */}
        <button
          type="button"
          onClick={() => onViewImages(score)}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-purple-700 text-white rounded-lg hover:bg-purple-800 transition-colors text-sm cursor-pointer"
        >
          <Camera className="w-4 h-4" />
          Ver Fotos
        </button>

        <div className="space-y-2">
          {!client?.onlyBillet && !score.hasBillet && !score.hasInvoice && (
            <button
              type="button"
              onClick={() => onCombinedClick(score)}
              disabled={isProcessing}
              className="w-full flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <FileText className="w-3 h-3" />
              {isProcessing ? "Gerando..." : "Gerar NF + Boleto"}
            </button>
          )}

          <div
            className={`grid gap-2 ${client?.onlyBillet ? "grid-cols-1" : "grid-cols-2"}`}
          >
            {score.hasBillet ? (
              <button
                type="button"
                onClick={() => onShowBillet(score)}
                className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors text-xs cursor-pointer"
              >
                <Info className="w-3 h-3" />
                Ver Boleto
              </button>
            ) : (
              <button
                type="button"
                onClick={() => onOpenClientNumberModal(score.id)}
                disabled={isProcessing}
                className="flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <FileText className="w-3 h-3" />
                {isProcessing ? "Gerando..." : "Gerar Boleto"}
              </button>
            )}

            {!client?.onlyBillet &&
              (score.hasInvoice ? (
                <button
                  type="button"
                  onClick={() => onShowInvoice(score)}
                  className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors text-xs cursor-pointer"
                >
                  <Info className="w-3 h-3" />
                  Ver NF
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => onInvoiceClick(score)}
                  disabled={isProcessing}
                  className="flex items-center justify-center gap-1 px-2 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <FileText className="w-3 h-3" />
                  {isProcessing ? "Gerando..." : "Gerar NF"}
                </button>
              ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2">
          {!score.hasBillet && (
            <button
              type="button"
              onClick={() => onTogglePayment(score)}
              disabled={
                (score.hasInvoice && score.status === "PAGO") ||
                isActionProcessing
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
              onClick={() => onDeleteClick(score)}
              disabled={isActionProcessing}
              className="flex items-center justify-center gap-1 px-2 py-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Trash2 className="w-3 h-3" />
              Deletar
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
