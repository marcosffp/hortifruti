"use client";

import { useCallback, useEffect, useState } from "react";
import { useCapturaNota } from "@/hooks/useCapturaNota";
import { useRealtimeSocket } from "@/hooks/useRealtimeSocket";
import type {
  CapturaPendente,
  StatusCaptura,
} from "@/services/capturaNotaService";
import { showError, showInfo } from "@/utils/toastUtils";
import NotaRevisaoModal from "./NotaRevisaoModal";

const STATUS_LABEL: Record<StatusCaptura, string> = {
  RECEBIDA: "Recebida",
  EXTRAINDO: "Extraindo...",
  PRONTA: "Pronta pra revisar",
  ERRO: "Erro na extração",
  CONFIRMADA: "Confirmada",
  DESCARTADA: "Descartada",
};

const STATUS_BADGE: Record<StatusCaptura, string> = {
  RECEBIDA: "bg-gray-100 text-gray-700",
  EXTRAINDO: "bg-yellow-100 text-yellow-800",
  PRONTA: "bg-green-100 text-green-800",
  ERRO: "bg-red-100 text-red-800",
  CONFIRMADA: "bg-blue-100 text-blue-800",
  DESCARTADA: "bg-gray-100 text-gray-400",
};

const STATUS_DESCARTAVEIS: StatusCaptura[] = [
  "RECEBIDA",
  "EXTRAINDO",
  "PRONTA",
  "ERRO",
];

export default function NotasPendentesFila() {
  const [capturas, setCapturas] = useState<CapturaPendente[]>([]);
  const [revisando, setRevisando] = useState<{
    captura: CapturaPendente;
    imageUrl: string;
  } | null>(null);
  const {
    fetchPendentes,
    fetchImagem,
    descartar: descartarCaptura,
    reprocessar: reprocessarCaptura,
  } = useCapturaNota();

  const carregarPendentes = useCallback(async () => {
    try {
      setCapturas(await fetchPendentes());
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao carregar a fila de notas.",
      );
    }
  }, [fetchPendentes]);

  useEffect(() => {
    carregarPendentes();
  }, [carregarPendentes]);

  useRealtimeSocket((event) => {
    if (event.type === "captura-atualizada") {
      carregarPendentes();
    }
  });

  const abrirRevisao = async (captura: CapturaPendente) => {
    try {
      const blob = await fetchImagem(captura.id);
      setRevisando({ captura, imageUrl: URL.createObjectURL(blob) });
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao carregar a foto da captura.",
      );
    }
  };

  const descartar = async (captura: CapturaPendente) => {
    try {
      await descartarCaptura(captura.id);
      showInfo("Captura descartada.");
      carregarPendentes();
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao descartar a captura.",
      );
    }
  };

  const reprocessar = async (captura: CapturaPendente) => {
    try {
      await reprocessarCaptura(captura.id);
      showInfo("Reprocessando a extração...");
      carregarPendentes();
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao reprocessar a captura.",
      );
    }
  };

  const fecharRevisao = () => {
    if (revisando) URL.revokeObjectURL(revisando.imageUrl);
    setRevisando(null);
  };

  return (
    <div className="bg-white rounded-lg shadow p-6 space-y-4">
      <h2 className="text-lg font-semibold text-gray-800">
        Fila de notas pendentes (celular)
      </h2>

      {capturas.length === 0 && (
        <p className="text-sm text-gray-400 italic">
          Nenhuma captura pendente — tire uma foto num dispositivo vinculado pra
          ver aparecer aqui em tempo real.
        </p>
      )}

      <div className="space-y-2">
        {capturas.map((captura) => (
          <div
            key={captura.id}
            className="flex items-center justify-between border border-gray-200 rounded-lg p-3"
          >
            <div>
              <p className="text-sm text-gray-700">
                Captura #{captura.id} —{" "}
                <span
                  className={`px-1.5 py-0.5 rounded text-xs font-semibold ${STATUS_BADGE[captura.status]}`}
                >
                  {STATUS_LABEL[captura.status]}
                </span>
              </p>
              <p className="text-xs text-gray-400">
                {new Date(captura.criadaEm).toLocaleString("pt-BR")}
              </p>
              {captura.status === "ERRO" && captura.mensagemErro && (
                <p className="text-xs text-red-600 mt-1">
                  {captura.mensagemErro}
                </p>
              )}
            </div>
            <div className="flex gap-2">
              {captura.status === "PRONTA" && (
                <button
                  type="button"
                  onClick={() => abrirRevisao(captura)}
                  className="px-3 py-1.5 text-sm rounded bg-purple-600 text-white hover:bg-purple-700"
                >
                  Revisar
                </button>
              )}
              {captura.status === "ERRO" && (
                <button
                  type="button"
                  onClick={() => reprocessar(captura)}
                  className="px-3 py-1.5 text-sm rounded bg-blue-600 text-white hover:bg-blue-700"
                >
                  Reprocessar
                </button>
              )}
              {STATUS_DESCARTAVEIS.includes(captura.status) && (
                <button
                  type="button"
                  onClick={() => descartar(captura)}
                  className="px-3 py-1.5 text-sm rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
                >
                  Descartar
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {revisando?.captura.extracao && (
        <NotaRevisaoModal
          capturaId={revisando.captura.id}
          imageUrl={revisando.imageUrl}
          extraction={revisando.captura.extracao}
          onClose={fecharRevisao}
          onConfirmado={() => {
            fecharRevisao();
            carregarPendentes();
          }}
        />
      )}
    </div>
  );
}
