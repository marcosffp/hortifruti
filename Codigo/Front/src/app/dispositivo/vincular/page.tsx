"use client";

import { Leaf, Smartphone } from "lucide-react";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import CapturaNotaCamera from "@/components/modules/CapturaNotaCamera";
import { API_BASE_URL } from "@/config/api";
import { dispositivoService } from "@/services/dispositivoService";
import { showError, showSuccess } from "@/utils/toastUtils";

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

function nomeDispositivoPadrao(): string {
  const ua = typeof navigator !== "undefined" ? navigator.userAgent : "";
  if (/iphone|ipad/i.test(ua)) return "iPhone";
  if (/android/i.test(ua)) return "Android";
  return "Meu celular";
}

function Cartao({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex-1 flex items-center justify-center p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-2xl overflow-hidden">
        <div className="bg-gradient-to-r from-[var(--primary)] to-green-600 p-6 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white rounded-full shadow-lg mb-3">
            <Image
              src="/icon.png"
              alt="Logo Hortifruti"
              width={40}
              height={40}
            />
          </div>
          <h1 className="text-xl font-bold text-white">Vincular celular</h1>
          <p className="text-green-50 text-xs flex items-center justify-center gap-1.5 mt-1">
            <Leaf className="w-3.5 h-3.5" />
            Captura de notas — Hortifruti Santa Luzia
          </p>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}

export default function VincularDispositivoPage() {
  return (
    <Suspense fallback={null}>
      <VincularDispositivoPageInner />
    </Suspense>
  );
}

function VincularDispositivoPageInner() {
  const searchParams = useSearchParams();
  const codigoDaUrl = searchParams.get("codigo") ?? "";

  const [pareado, setPareado] = useState<boolean | undefined>(undefined);
  const [codigo, setCodigo] = useState(codigoDaUrl);
  const [nomeDispositivo, setNomeDispositivo] = useState("");
  const [confirmando, setConfirmando] = useState(false);

  useEffect(() => {
    setNomeDispositivo(nomeDispositivoPadrao());
    dispositivoService
      .statusPareamento()
      .then((status) => setPareado(status.pareado))
      .catch(() => setPareado(false));
  }, []);

  const confirmar = async () => {
    const codigoLimpo = codigo.trim();
    if (!codigoLimpo) return;

    setConfirmando(true);
    try {
      await dispositivoService.confirmarPareamento(
        codigoLimpo,
        nomeDispositivo.trim(),
      );
      setPareado(true);
      showSuccess("Dispositivo vinculado! Pode voltar pro computador.");
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao confirmar pareamento.",
      );
    } finally {
      setConfirmando(false);
    }
  };

  const esquecerDispositivo = () => {
    dispositivoService.desvincularLocal().catch(() => {});
    setPareado(false);
    setCodigo("");
  };

  // Enquanto não sabemos se já existe vínculo válido, evita piscar o formulário à toa.
  if (pareado === undefined) {
    return <Cartao>{null}</Cartao>;
  }

  if (pareado) {
    return (
      <Cartao>
        <CapturaFoto onDesvincular={esquecerDispositivo} />
      </Cartao>
    );
  }

  return (
    <Cartao>
      <p className="text-sm text-gray-500 mb-4">
        Confira o código gerado no computador e dê um nome pra este aparelho.
      </p>

      <div className="space-y-4">
        <div>
          <label
            htmlFor="pareamento-codigo"
            className="block text-xs font-semibold text-gray-700 uppercase mb-1"
          >
            Código
          </label>
          <input
            id="pareamento-codigo"
            type="text"
            inputMode="numeric"
            maxLength={6}
            value={codigo}
            onChange={(e) => setCodigo(e.target.value)}
            className="block w-full p-3 border border-gray-300 rounded-lg text-center text-2xl font-mono tracking-widest bg-gray-50 focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
            placeholder="000000"
          />
        </div>

        <div>
          <label
            htmlFor="pareamento-nome"
            className="block text-xs font-semibold text-gray-700 uppercase mb-1"
          >
            Nome deste dispositivo
          </label>
          <input
            id="pareamento-nome"
            type="text"
            value={nomeDispositivo}
            onChange={(e) => setNomeDispositivo(e.target.value)}
            className="block w-full p-3 border border-gray-300 rounded-lg bg-gray-50 focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
          />
        </div>

        <button
          type="button"
          disabled={confirmando || !codigo.trim()}
          onClick={confirmar}
          className="w-full py-3 rounded-lg bg-green-600 text-white font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
        >
          {confirmando ? "Confirmando..." : "Confirmar vínculo"}
        </button>
      </div>
    </Cartao>
  );
}

interface CapturaFotoProps {
  onDesvincular: () => void;
}

/**
 * Cada foto vai pra fila do PC assim que enviada — a extração (Gemini) e a revisão acontecem
 * lá, não aqui. Por isso o celular só precisa confirmar o envio, sem esperar resultado. A
 * autenticação vai só pelo cookie `httpOnly` `device_token` (`credentials: "include"`) — nunca
 * mais por header lido de `localStorage`.
 */
function CapturaFoto({ onDesvincular }: CapturaFotoProps) {
  const enviar = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/api/compras/notas/capturas`, {
      method: "POST",
      credentials: "include",
      body: formData,
    });

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
  };

  return (
    <div className="text-center space-y-4">
      <div className="inline-flex items-center justify-center w-12 h-12 bg-green-100 rounded-full">
        <Smartphone className="w-6 h-6 text-green-600" />
      </div>
      <p className="text-sm text-gray-700">
        Dispositivo vinculado — tire a foto da nota, cada uma vai pra fila do
        computador.
      </p>

      <CapturaNotaCamera
        enviar={enviar}
        mensagemSucesso="Foto enviada! Já aparece na fila do PC."
      />

      <button
        type="button"
        onClick={onDesvincular}
        className="text-xs text-gray-400 underline"
      >
        Vincular com outro código
      </button>
    </div>
  );
}
