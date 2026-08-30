"use client";

import { UploadCloud } from "lucide-react";
import { useRef, useState } from "react";
import RoleGuard from "@/components/auth/RoleGuard";
import ConversaoCaixaResultado from "@/components/modules/conversao-caixa/ConversaoCaixaResultado";
import Loading from "@/components/ui/Loading";
import { useConversaoCaixa } from "@/hooks/useConversaoCaixa";
import { showError, showSuccess } from "@/utils/toastUtils";

export default function ConversaoCaixaPage() {
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { isLoading, resultado, importar } = useConversaoCaixa();

  async function handleFile(file: File) {
    if (!file.name.toLowerCase().endsWith(".csv")) {
      showError("Envie um arquivo CSV (colunas COD, UNIDADE, KG).");
      return;
    }

    try {
      const resposta = await importar(file);
      const totalCodigos =
        resposta.cadastrados.length +
        resposta.atualizados.length +
        resposta.semAlteracao.length +
        resposta.codigosNaoEncontrados.length;
      showSuccess(
        `Import concluído: ${resposta.cadastrados.length} cadastrado(s) e ${resposta.atualizados.length} atualizado(s) de ${totalCodigos} código(s) de caixa no arquivo.`,
      );
    } catch (err) {
      showError(
        err instanceof Error ? err.message : "Erro ao importar o arquivo.",
      );
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    e.target.value = "";
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  return (
    <RoleGuard roles="MANAGER">
      <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800">
            Conversão Caixa → KG
          </h1>
          <p className="text-gray-600 mt-1">
            Importe o CSV (colunas <code>COD,UNIDADE,KG</code>) com o peso de
            referência de uma caixa por produto — usado pra converter itens em
            caixa pra kg automaticamente na extração de nota, sem depender da IA
            estimar.
          </p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-5 border border-gray-200 mb-6 relative">
          {isLoading && <Loading overlay />}
          <section
            className={`border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-center p-8 transition-colors ${
              isDragging ? "border-primary bg-primary-bg" : "border-gray-300"
            }`}
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={(e) => {
              e.preventDefault();
              setIsDragging(false);
            }}
            onDrop={handleDrop}
            aria-label="Área para soltar o arquivo CSV"
          >
            <div className="h-16 w-16 rounded-full bg-primary-bg flex items-center justify-center mb-4">
              <UploadCloud className="text-primary h-8 w-8" />
            </div>
            <p className="text-base font-medium mb-2">
              Clique ou arraste o CSV de conversão
            </p>
            <p className="text-gray-500 text-sm mb-4">
              Só linhas com UNIDADE=CAIXA são consideradas — as demais (KG,
              UNID) são ignoradas.
            </p>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isLoading}
              className="py-2 px-4 text-sm bg-primary text-white rounded disabled:opacity-50 cursor-pointer hover:bg-[var(--primary-dark)] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
            >
              Selecionar Arquivo
            </button>
            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              accept=".csv,text/csv"
              onChange={handleFileChange}
            />
          </section>
        </div>

        {resultado && <ConversaoCaixaResultado resultado={resultado} />}
      </main>
    </RoleGuard>
  );
}
