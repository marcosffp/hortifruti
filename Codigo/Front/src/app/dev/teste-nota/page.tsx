"use client";

// ⚠️ PÁGINA TEMPORÁRIA DE DEV — REMOVER ANTES DO MERGE FINAL (ver Etapa 7 da spec de
// captura-nota-camera-gemini). Tudo que essa feature usa pra teste fica dentro de
// `src/app/dev/`, então remover é só apagar essa pasta inteira — nada daqui vaza pro
// resto do app.
//
// O pareamento de dispositivo (QR/código) saiu daqui — agora é uma tela real, em
// /(shell)/acesso (componente DispositivoVinculados) + a rota pública /dispositivo/vincular
// pro celular confirmar. Esta tela cobre só o que ainda não tem lar definitivo:
//  1. PC/admin (sem device token no localStorage): teste direto de /upload e /extrair, mais a
//     fila de notas pendentes em tempo real (WebSocket) — ver Etapas 5-8 da spec
//     captura-nota-dispositivo-vinculado-tempo-real.
//  2. Captura pelo celular (device token salvo no localStorage — grava aqui quem parear via
//     /dispositivo/vincular): só a câmera + upload via X-Device-Token, sem login normal.

import { useEffect, useState } from "react";
import NotaRevisaoModal, {
  type NotaExtracaoResponse,
} from "@/components/modules/NotaRevisaoModal";
import NotasPendentesFila from "@/components/modules/NotasPendentesFila";
import { DEVICE_TOKEN_STORAGE_KEY } from "@/services/dispositivoService";
import { showError, showSuccess } from "@/services/notificationService";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

type ResultState = {
  endpoint: string;
  status: number;
  ok: boolean;
  tempoMs: number;
  body: unknown;
} | null;

function isNotaExtracaoResponse(body: unknown): body is NotaExtracaoResponse {
  return (
    typeof body === "object" &&
    body !== null &&
    Array.isArray((body as NotaExtracaoResponse).itens)
  );
}

async function extrairMensagemErro(response: Response, fallback: string) {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

export default function TesteNotaDevPage() {
  const [deviceToken, setDeviceToken] = useState<string | null | undefined>(
    undefined,
  );

  useEffect(() => {
    setDeviceToken(localStorage.getItem(DEVICE_TOKEN_STORAGE_KEY));
  }, []);

  if (deviceToken === undefined) {
    return null;
  }

  if (deviceToken) {
    return (
      <CapturaCelular
        deviceToken={deviceToken}
        onDesvincular={() => {
          localStorage.removeItem(DEVICE_TOKEN_STORAGE_KEY);
          setDeviceToken(null);
        }}
      />
    );
  }

  return <PainelAdmin />;
}

interface CapturaCelularProps {
  deviceToken: string;
  onDesvincular: () => void;
}

function CapturaCelular({ deviceToken, onDesvincular }: CapturaCelularProps) {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(selected ? URL.createObjectURL(selected) : null);
  };

  const enviar = async () => {
    if (!file) return;
    setEnviando(true);
    try {
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(
        `${API_BASE_URL}/api/compras/notas/capturas`,
        {
          method: "POST",
          headers: { "X-Device-Token": deviceToken },
          body: formData,
        },
      );

      if (response.status === 401) {
        onDesvincular();
        throw new Error(
          "Este dispositivo foi desvinculado. Peça um novo pareamento no PC.",
        );
      }
      if (!response.ok) {
        throw new Error(
          await extrairMensagemErro(response, "Falha ao enviar a foto."),
        );
      }

      showSuccess("Foto enviada! Processando no PC...");
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setFile(null);
      setPreviewUrl(null);
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Falha ao enviar a foto.",
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="max-w-sm mx-auto space-y-4">
        <div className="bg-white rounded-lg shadow p-6 space-y-4">
          <h1 className="text-xl font-semibold text-gray-800">Capturar nota</h1>
          <p className="text-sm text-gray-500">
            Dispositivo vinculado — tire quantas fotos precisar, cada uma vai
            pra fila do PC.
          </p>

          <input
            type="file"
            accept="image/*"
            capture="environment"
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:bg-green-600 file:text-white hover:file:bg-green-700"
          />

          {previewUrl && (
            // biome-ignore lint: preview de dev, não precisa de otimização do next/image
            <img
              src={previewUrl}
              alt="Preview da nota selecionada"
              className="max-h-64 rounded border border-gray-300 mx-auto"
            />
          )}

          <button
            type="button"
            disabled={!file || enviando}
            onClick={enviar}
            className="w-full px-4 py-2 rounded bg-blue-600 text-white font-medium disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {enviando ? "Enviando..." : "Enviar foto"}
          </button>
        </div>

        <button
          type="button"
          onClick={onDesvincular}
          className="text-xs text-gray-400 underline w-full text-center"
        >
          Desvincular este dispositivo
        </button>
      </div>
    </div>
  );
}

function PainelAdmin() {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState<"upload" | "extrair" | null>(null);
  const [result, setResult] = useState<ResultState>(null);
  const [showRevisao, setShowRevisao] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    setResult(null);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(selected ? URL.createObjectURL(selected) : null);
  };

  const chamarEndpoint = async (endpoint: "upload" | "extrair") => {
    if (!file) return;
    setLoading(endpoint);
    setResult(null);

    const formData = new FormData();
    formData.append("file", file);

    const start = performance.now();
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/compras/notas/${endpoint}`,
        {
          method: "POST",
          credentials: "include",
          body: formData,
        },
      );
      const tempoMs = Math.round(performance.now() - start);
      const body = await response.json().catch(() => null);
      setResult({
        endpoint,
        status: response.status,
        ok: response.ok,
        tempoMs,
        body,
      });
      if (
        endpoint === "extrair" &&
        response.ok &&
        isNotaExtracaoResponse(body)
      ) {
        setShowRevisao(true);
      }
    } catch (error) {
      const tempoMs = Math.round(performance.now() - start);
      setResult({
        endpoint,
        status: 0,
        ok: false,
        tempoMs,
        body: {
          error:
            error instanceof Error
              ? error.message
              : "Falha de rede desconhecida (backend está no ar?)",
        },
      });
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="max-w-2xl mx-auto space-y-4">
        <div className="bg-yellow-100 border-2 border-yellow-400 rounded-lg p-4">
          <p className="font-bold text-yellow-800">
            🚧 Tela temporária de desenvolvimento
          </p>
          <p className="text-sm text-yellow-800">
            Testa upload/extração diretas e a fila de notas em tempo real. O
            pareamento de dispositivo agora fica em{" "}
            <a href="/acesso" className="underline">
              Módulo Acesso
            </a>
            . Não faz parte do fluxo real do sistema ainda. Pra remover: apague
            a pasta <code>src/app/dev/</code> inteira.
          </p>
          <p className="text-xs text-yellow-700 mt-1">
            Precisa estar logado no sistema (mesma aba/sessão) pra ter o cookie
            de autenticação — se não estiver, faça login em{" "}
            <a href="/login" className="underline">
              /login
            </a>{" "}
            antes.
          </p>
        </div>

        <NotasPendentesFila />

        <div className="bg-white rounded-lg shadow p-6 space-y-4">
          <h1 className="text-xl font-semibold text-gray-800">
            Teste de comunicação — captura de nota (Gemini)
          </h1>

          <input
            type="file"
            accept="image/*"
            capture="environment"
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:bg-green-600 file:text-white hover:file:bg-green-700"
          />

          {previewUrl && (
            // biome-ignore lint: preview de dev, não precisa de otimização do next/image
            <img
              src={previewUrl}
              alt="Preview da nota selecionada"
              className="max-h-64 rounded border border-gray-300"
            />
          )}

          <div className="flex gap-3">
            <button
              type="button"
              disabled={!file || loading !== null}
              onClick={() => chamarEndpoint("upload")}
              className="px-4 py-2 rounded bg-blue-600 text-white disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {loading === "upload" ? "Enviando..." : "Testar /upload"}
            </button>
            <button
              type="button"
              disabled={!file || loading !== null}
              onClick={() => chamarEndpoint("extrair")}
              className="px-4 py-2 rounded bg-green-600 text-white disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {loading === "extrair" ? "Extraindo..." : "Testar /extrair"}
            </button>
          </div>

          {result && (
            <div className="space-y-2">
              <p className="text-sm">
                <span className="font-semibold">
                  POST /api/compras/notas/{result.endpoint}
                </span>{" "}
                →{" "}
                <span
                  className={
                    result.ok
                      ? "text-green-700 font-semibold"
                      : "text-red-700 font-semibold"
                  }
                >
                  HTTP {result.status || "sem resposta"}
                </span>{" "}
                <span className="text-gray-500">({result.tempoMs}ms)</span>
              </p>
              {result.endpoint === "extrair" &&
                result.ok &&
                isNotaExtracaoResponse(result.body) && (
                  <button
                    type="button"
                    onClick={() => setShowRevisao(true)}
                    className="px-3 py-1.5 text-sm rounded bg-purple-600 text-white hover:bg-purple-700"
                  >
                    Abrir comparação (imagem × extração)
                  </button>
                )}
              <pre className="bg-gray-900 text-green-400 text-xs p-4 rounded overflow-auto max-h-96">
                {JSON.stringify(result.body, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>

      {showRevisao &&
        previewUrl &&
        result &&
        isNotaExtracaoResponse(result.body) && (
          <NotaRevisaoModal
            imageUrl={previewUrl}
            extraction={result.body}
            onClose={() => setShowRevisao(false)}
          />
        )}
    </div>
  );
}
