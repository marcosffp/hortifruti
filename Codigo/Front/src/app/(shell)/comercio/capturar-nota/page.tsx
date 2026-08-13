"use client";

import { Camera } from "lucide-react";
import RoleGuard from "@/components/auth/RoleGuard";
import CapturaNotaCamera from "@/components/modules/CapturaNotaCamera";
import { API_BASE_URL } from "@/config/api";

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

/**
 * Captura rápida pra quem já está logado normalmente no celular (sem passar pelo pareamento de
 * dispositivo em /acesso) — autentica por cookie, igual o resto do sistema. Cada foto enviada cai
 * na mesma fila em tempo real que aparece em Gerenciamento de Compras.
 */
export default function CapturarNotaPage() {
  const enviar = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/api/compras/notas/capturas`, {
      method: "POST",
      credentials: "include",
      body: formData,
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao enviar a foto."),
      );
    }
  };

  return (
    <RoleGuard roles={["MANAGER", "EMPLOYEE"]}>
      <main className="flex-1 p-6 bg-gray-50 overflow-auto min-h-full">
        <div className="max-w-md mx-auto space-y-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
              <Camera className="text-primary" size={26} />
              Capturar Nota
            </h1>
            <p className="text-gray-600 text-sm mt-1">
              Tire a foto da nota de compra (pode ter mais de uma nota na mesma
              foto) — cada uma cai separada na fila de{" "}
              <span className="font-medium">Gerenciamento de Compras</span> pra
              revisão em tempo real.
            </p>
          </div>

          <div className="bg-white rounded-lg shadow-sm p-6">
            <CapturaNotaCamera
              enviar={enviar}
              mensagemSucesso="Foto enviada! Já aparece na fila de Gerenciamento de Compras."
            />
          </div>
        </div>
      </main>
    </RoleGuard>
  );
}
