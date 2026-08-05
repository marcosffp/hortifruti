"use client";

import { Camera } from "lucide-react";
import { useId, useState } from "react";
import { showError, showSuccess } from "@/services/notificationService";

interface CapturaNotaCameraProps {
  /** Envia o arquivo pro backend — lança erro em caso de falha (mensagem já tratada). */
  enviar: (file: File) => Promise<void>;
  mensagemSucesso: string;
}

/**
 * Câmera + preview + envio de foto de nota, compartilhado entre a captura autenticada por cookie
 * (`/comercio/capturar-nota`, usuário logado normalmente) e a captura por dispositivo vinculado
 * (`/dispositivo/vincular`, token no localStorage) — só muda quem chama e como autentica o upload.
 * O botão usa um `<label>` ligado a um input escondido porque o texto do botão nativo de um
 * `<input type="file">` é controlado pelo navegador (ex.: "Escolher arquivo") e não dá pra
 * customizar.
 */
export default function CapturaNotaCamera({
  enviar,
  mensagemSucesso,
}: CapturaNotaCameraProps) {
  const inputId = useId();
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(selected ? URL.createObjectURL(selected) : null);
  };

  const limparSelecao = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setFile(null);
    setPreviewUrl(null);
  };

  const handleEnviar = async () => {
    if (!file) return;
    setEnviando(true);
    try {
      await enviar(file);
      showSuccess(mensagemSucesso);
      limparSelecao();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Falha ao enviar a foto.",
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="space-y-4">
      <input
        id={inputId}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="hidden"
      />

      {previewUrl && (
        // biome-ignore lint: preview da foto selecionada, não é asset do next/image
        <img
          src={previewUrl}
          alt="Preview da nota"
          className="max-h-64 mx-auto rounded-lg border border-gray-200"
        />
      )}

      <label
        htmlFor={inputId}
        className="flex items-center justify-center gap-2 w-full py-3 rounded-lg bg-white border-2 border-green-600 text-green-700 font-semibold hover:bg-green-50 transition-colors cursor-pointer"
      >
        <Camera className="w-5 h-5" />
        {file ? "Tirar outra foto" : "Tirar foto da nota"}
      </label>

      <button
        type="button"
        disabled={!file || enviando}
        onClick={handleEnviar}
        className="w-full py-3 rounded-lg bg-green-600 text-white font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
      >
        {enviando ? "Enviando..." : "Enviar foto"}
      </button>
    </div>
  );
}
