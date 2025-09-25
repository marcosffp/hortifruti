"use client";

import { useState, useRef } from "react";
import Button from "@/components/ui/Button";
import { ArrowUp, FileSpreadsheet, X, AlertCircle, CheckCircle } from "lucide-react";

interface UploadedFile {
  file: File;
  status: 'pending' | 'processing' | 'success' | 'error';
  cliente?: string;
  valorTotal?: string;
  dataUpload?: string;
  error?: string;
}

export default function EnhancedUploadNotes() {
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string>("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const validateFiles = (files: File[]): File[] => {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = [
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    return files.filter(file => {
      if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
        setError('Apenas arquivos Excel (.xls, .xlsx) são permitidos');
        return false;
      }
      if (file.size > maxSize) {
        setError('Arquivo muito grande. Máximo 10MB por arquivo');
        return false;
      }
      return true;
    });
  };

  const processFiles = async (files: File[]) => {
    setError("");
    
    // Simular processamento dos arquivos
    for (const file of files) {
      const uploadedFile: UploadedFile = {
        file,
        status: 'processing'
      };
      
      setUploadedFiles(prev => [...prev, uploadedFile]);

      try {
        // Simular processamento
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Simular dados extraídos do Excel
        const mockData = {
          cliente: file.name.includes('joao') ? 'João Silva Santos' : 'Maria Oliveira',
          valorTotal: file.name.includes('joao') ? 'R$ 1.250,00' : 'R$ 890,00',
          dataUpload: new Date().toLocaleDateString('pt-BR') + ', ' + new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
        };

        setUploadedFiles(prev => 
          prev.map(f => 
            f.file === file 
              ? { ...f, status: 'success', ...mockData }
              : f
          )
        );
      } catch (err) {
        setUploadedFiles(prev => 
          prev.map(f => 
            f.file === file 
              ? { ...f, status: 'error', error: 'Erro ao processar arquivo' }
              : f
          )
        );
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    const validFiles = validateFiles(selectedFiles);
    if (validFiles.length > 0) {
      processFiles(validFiles);
    }
    // Reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveFile = (index: number) => {
    setUploadedFiles(uploadedFiles.filter((_, i) => i !== index));
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
    if (validFiles.length > 0) {
      processFiles(validFiles);
    }
  };

  const handleButtonClick = (e: React.MouseEvent) => {
    e.preventDefault();
    fileInputRef.current?.click();
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
            ou arraste e solte os arquivos Excel aqui
          </p>
          <p className="text-sm text-gray-400 mb-6">
            Selecione os extratos bancários em formato Excel. Máximo 10MB por arquivo.
          </p>
          <Button
            variant="primary"
            onClick={handleButtonClick}
            type="button"
            className="py-3 px-8 text-lg cursor-pointer"
          >
            Selecionar Arquivos
          </Button>
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            multiple
            onChange={handleFileChange}
          />
        </div>
      </div>

      {error && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center">
          <AlertCircle className="text-red-600 flex-shrink-0 mr-3" size={24} />
          <p className="text-red-700 text-base">{error}</p>
        </div>
      )}

      {uploadedFiles.length > 0 && (
        <div className="mt-8">
          <h3 className="text-xl font-medium mb-4">Arquivos Carregados</h3>
          
          <div className="bg-white rounded-lg border border-gray-200">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Arquivo</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Cliente</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Valor Total</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Data Upload</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Status</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {uploadedFiles.map((uploadedFile, index) => (
                    <tr key={`${uploadedFile.file.name}-${index}`} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <div className="flex items-center">
                          <FileSpreadsheet className="text-green-600 mr-3" size={20} />
                          <div>
                            <p className="font-medium text-sm">{uploadedFile.file.name}</p>
                            <p className="text-gray-500 text-xs">{formatFileSize(uploadedFile.file.size)}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {uploadedFile.cliente || '-'}
                      </td>
                      <td className="py-3 px-4 text-sm font-medium text-green-600">
                        {uploadedFile.valorTotal || '-'}
                      </td>
                      <td className="py-3 px-4 text-sm text-gray-600">
                        {uploadedFile.dataUpload || '-'}
                      </td>
                      <td className="py-3 px-4">
                        {uploadedFile.status === 'processing' && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                            Processando...
                          </span>
                        )}
                        {uploadedFile.status === 'success' && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            <CheckCircle className="w-3 h-3 mr-1" />
                            Sucesso
                          </span>
                        )}
                        {uploadedFile.status === 'error' && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            <AlertCircle className="w-3 h-3 mr-1" />
                            Erro
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <button
                          onClick={() => handleRemoveFile(index)}
                          className="text-gray-400 hover:text-red-600 p-1"
                          aria-label={`Remover arquivo ${uploadedFile.file.name}`}
                        >
                          <X size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </>
  );
}