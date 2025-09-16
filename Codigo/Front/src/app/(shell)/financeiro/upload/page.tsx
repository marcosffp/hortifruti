import EnhancedUploadExtract from "@/components/modules/EnhancedUploadExtract";
import { ArrowUp } from "lucide-react";

export default function UploadExtratsPage() {
  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col">
      <div className="mb-8 text-center mx-auto max-w-3xl">
        <h1 className="text-3xl font-bold mb-3">
          Upload de Extratos Bancários
        </h1>
        <p className="text-gray-600 text-lg">
          Faça upload dos dados bancários em PDF dos bancos Sicoob e Banco do
          Brasil para processamento automático.
        </p>
      </div>

      <div className="bg-white border rounded-lg shadow-sm p-8 mx-auto w-full max-w-6xl flex-grow flex flex-col">
        <div className="flex flex-col items-center mb-6 text-center">
          <div className="flex items-center text-primary mb-2">
            <ArrowUp className="mr-2" />
            <h2 className="text-2xl font-semibold">Carregar Arquivos PDF</h2>
          </div>

          <p className="text-gray-600 max-w-lg mx-auto">
            Selecione os extratos em formato PDF. Máximo 10MB por arquivo.
          </p>
        </div>

        <div className="flex-grow flex flex-col justify-center">
          <EnhancedUploadExtract />
        </div>
      </div>
    </main>
  );
}
