"use client";

import { Camera, Download, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { API_BASE_URL } from "@/config/api";
import { combinedScoreService } from "@/services/combinedScoreService";
import type { PurchaseImageType } from "@/types/combinedScoreType";
import { getAuthHeaders } from "@/utils/httpUtils";
import { showError } from "@/utils/toastUtils";

interface CombinedScoreImagesModalProps {
  combinedScoreId: number;
  scoreNumber: string;
  onClose: () => void;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

export default function CombinedScoreImagesModal({
  combinedScoreId,
  scoreNumber,
  onClose,
}: CombinedScoreImagesModalProps) {
  const [images, setImages] = useState<PurchaseImageType[]>([]);
  const [loading, setLoading] = useState(false);
  const [downloadingPdf, setDownloadingPdf] = useState(false);
  const [ampliada, setAmpliada] = useState<{
    purchaseId: number;
    url: string;
  } | null>(null);

  const fetchImages = useCallback(async () => {
    setLoading(true);
    try {
      setImages(await combinedScoreService.fetchImages(combinedScoreId));
    } catch (error) {
      showError("Erro ao carregar fotos do agrupamento");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [combinedScoreId]);

  useEffect(() => {
    fetchImages();
  }, [fetchImages]);

  const abrirFoto = async (purchaseId: number) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/purchases/${purchaseId}/imagem`,
        {
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
      if (!response.ok) throw new Error("Falha ao carregar a foto");
      const blob = await response.blob();
      setAmpliada({ purchaseId, url: URL.createObjectURL(blob) });
    } catch (error) {
      showError("Erro ao carregar a foto");
      console.error(error);
    }
  };

  const fecharAmpliada = () => {
    if (ampliada) URL.revokeObjectURL(ampliada.url);
    setAmpliada(null);
  };

  const baixarPdf = async () => {
    setDownloadingPdf(true);
    try {
      const pdfBlob =
        await combinedScoreService.downloadPhotosPdf(combinedScoreId);
      const url = URL.createObjectURL(pdfBlob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `FOTOS-AGRUPAMENTO-${scoreNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (error) {
      showError("Erro ao baixar PDF de fotos do agrupamento");
      console.error(error);
    } finally {
      setDownloadingPdf(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-gray-300 border-b">
          <h2 className="text-xl font-semibold">
            Fotos do Agrupamento {scoreNumber}
          </h2>
          <div className="flex items-center gap-2">
            {images.length > 0 && (
              <button
                type="button"
                onClick={baixarPdf}
                disabled={downloadingPdf}
                className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Download className="w-4 h-4" />
                {downloadingPdf ? "Baixando..." : "Baixar PDF"}
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="space-y-2">
              {[...Array(3)].map((_, i) => (
                <div
                  // biome-ignore lint/suspicious/noArrayIndexKey: skeleton placeholder, sem identidade estável
                  key={i}
                  className="h-16 bg-gray-200 animate-pulse rounded-lg"
                />
              ))}
            </div>
          ) : images.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Nenhuma foto anexada a este agrupamento — só compras de clientes
              que exigem comprovante mantêm a foto da nota.
            </div>
          ) : (
            <div className="space-y-2">
              {images.map((image) => (
                <div
                  key={image.purchaseId}
                  className="flex items-center justify-between gap-3 border border-gray-200 rounded-lg p-3 hover:bg-gray-50"
                >
                  <div>
                    <p className="text-sm text-gray-700">
                      Compra #{image.purchaseId}
                    </p>
                    <p className="text-xs text-gray-500">
                      {new Date(image.purchaseDate).toLocaleDateString("pt-BR")}{" "}
                      — {formatCurrency(image.total)}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => abrirFoto(image.purchaseId)}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded bg-purple-600 text-white hover:bg-purple-700"
                  >
                    <Camera className="w-4 h-4" />
                    Ver foto
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {ampliada && (
        <div
          className="fixed inset-0 bg-black/80 flex items-center justify-center z-[60] p-4"
          onClick={fecharAmpliada}
          onKeyDown={(e) => e.key === "Escape" && fecharAmpliada()}
          role="button"
          tabIndex={0}
        >
          {/* biome-ignore lint: foto original baixada do R2 como blob, não é asset do next/image */}
          <img
            src={ampliada.url}
            alt={`Comprovante da compra ${ampliada.purchaseId}`}
            className="max-w-full max-h-full rounded-lg"
          />
        </div>
      )}
    </div>
  );
}
