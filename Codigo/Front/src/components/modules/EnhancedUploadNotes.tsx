"use client";

import { useState, useRef } from "react";
import { ArrowUp } from "lucide-react";
import Loading from "@/components/ui/Loading";
import { showError, showSuccess } from "@/services/notificationService";
import { useUpload } from "@/hooks/useUpload";

type EnhancedUploadNotesProps = {
  clientId: number | undefined;
};

export default function EnhancedUploadNotes({ clientId }: EnhancedUploadNotesProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [loading, setLoading] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const { validateFiles, processFiles } = useUpload();

  const handleFileUpload = async (file: File) => {
    setLoading(true);

    try {
      const validFiles = validateFiles([file]);
      if (validFiles.length === 0) return;

      await processFiles(validFiles, "purchase");

      showSuccess(`O arquivo "${file.name}" foi processado com sucesso!`);
    } catch (err) {
      showError(`Erro ao processar o arquivo "${file.name}".`);
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      handleFileUpload(selectedFile);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    if(!clientId) {
      showError("Selecione um cliente antes de enviar arquivos.");
      return;
    }

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleButtonClick = (e: React.MouseEvent) => {
    e.preventDefault();
    fileInputRef.current?.click();
  };

  return (
    <div className="flex-1 flex items-center justify-center bg-white relative rounded-lg shadow-sm p-4">
      {/* Loading overlay */}
      {loading && <Loading />}

      <div
        className={`w-full h-full max-h-full border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-center p-2 transition-colors ${isDragging ? "border-primary bg-primary-bg" : "border-gray-300"
          }`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        aria-disabled={!clientId || loading}
        aria-label="Área para soltar arquivos"
      >
        <div className="flex flex-col items-center">
          <div className="h-16 w-16 rounded-full bg-primary-bg flex items-center justify-center mb-4">
            <ArrowUp className="text-primary h-8 w-8" />
          </div>
          <p className="text-base font-medium mb-2">
            Clique ou arraste um arquivo
          </p>
          <p className="text-gray-500 text-sm mb-4">Apenas arquivos PDF com no máximo 10MB</p>
          <button
            onClick={handleButtonClick}
            type="button"
            className="py-2 px-4 text-sm bg-primary text-white rounded disabled:opacity-50 cursor-pointer hover:bg-[var(--primary-dark)] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
            disabled={loading || !clientId}
          >
            Selecionar Arquivo
          </button>
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            accept=".pdf"
            onChange={handleFileChange}
          />
        </div>
      </div>
    </div>
  );
}
