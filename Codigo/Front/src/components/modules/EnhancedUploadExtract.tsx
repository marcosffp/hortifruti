"use client";

import { useState, useRef } from "react";
import { useStatement } from "@/hooks/useStatement";
import Button from "@/components/ui/Button";
import { ArrowUp, FileText, X, AlertCircle } from "lucide-react";

export default function EnhancedUploadExtract() {
  const [files, setFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const { formatFileSize, validateFiles, processFiles, error } = useStatement();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    const validFiles = validateFiles(selectedFiles);
    setFiles((prevFiles) => [...prevFiles, ...validFiles]);
  };

  const handleRemoveFile = (index: number) => {
    setFiles(files.filter((_, i) => i !== index));
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const items = e.dataTransfer.items;
    const fileList: File[] = [];

    if (items) {
      for (const item of Array.from(items)) {
        if (item.kind === "file") {
          const file = item.getAsFile();
          if (file) fileList.push(file);
        }
      }
    }

    const validFiles = validateFiles(fileList);
    setFiles((prevFiles) => [...prevFiles, ...validFiles]);
  };

  const handleButtonClick = (e: React.MouseEvent) => {
    e.preventDefault(); // Impede que o evento se propague para a label
    fileInputRef.current?.click();
  };

  const handleProcessFiles = async () => {
    try {
      await processFiles(files);
      // Limpar arquivos após processamento bem-sucedido
      setFiles([]);
    } catch (err) {
      // O erro já é tratado no hook useStatement
    }
  };

  return (
    <>
      <div
        className={`block border-2 border-dashed rounded-lg p-16 text-center transition-colors w-full min-h-[260px] ${
          isDragging ? "border-primary bg-primary-bg" : "border-gray-300"
        }`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        aria-label="Área para soltar arquivos"
      >
        <div className="flex flex-col items-center justify-center h-full">
          <div className="h-20 w-20 rounded-full bg-primary-bg flex items-center justify-center mb-6">
            <ArrowUp className="text-primary h-10 w-10" />
          </div>
          <p className="text-xl font-medium mb-3">
            Clique para selecionar arquivos
          </p>
          <p className="text-gray-500 mb-6 text-lg">
            ou arraste e solte os PDFs aqui
          </p>
          <Button
            variant="primary"
            onClick={handleButtonClick}
            type="button"
            className="py-3 px-8 text-lg"
          >
            Selecionar Arquivos
          </Button>
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            accept=".pdf"
            multiple
            onChange={handleFileChange}
          />
        </div>
      </div>

      {error && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center">
          <AlertCircle
            className="text-secondary flex-shrink-0 mr-3"
            size={24}
          />
          <p className="text-red-700 text-base">{error}</p>
        </div>
      )}

      {files.length > 0 && (
        <div className="mt-8">
          <h3 className="text-xl font-medium mb-4">Arquivos carregados</h3>
          <div className="space-y-3">
            {files.map((file, index) => (
              <div
                key={`${file.name}-${file.size}-${file.lastModified}`}
                className="flex items-center justify-between p-4 bg-gray-50 border border-gray-200 rounded-lg"
              >
                <div className="flex items-center">
                  <FileText className="text-gray-500 mr-4" size={22} />
                  <div>
                    <p className="font-medium text-lg">{file.name}</p>
                    <p className="text-gray-500">{formatFileSize(file.size)}</p>
                  </div>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveFile(index);
                  }}
                  className="text-gray-500 hover:text-secondary p-2"
                  aria-label={`Remover arquivo ${file.name}`}
                >
                  <X size={22} />
                </button>
              </div>
            ))}
          </div>

          <div className="mt-6 flex justify-end">
            <Button
              variant="primary"
              disabled={files.length === 0}
              className="py-3 px-8 text-lg"
              onClick={handleProcessFiles}
            >
              Processar {files.length}{" "}
              {files.length === 1 ? "arquivo" : "arquivos"}
            </Button>
          </div>
        </div>
      )}
    </>
  );
}
