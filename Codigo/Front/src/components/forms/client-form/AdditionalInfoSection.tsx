import type { ClientFormChangeEvent, ClientFormData } from "./types";

interface AdditionalInfoSectionProps {
  formData: ClientFormData;
  onChange: (e: ClientFormChangeEvent) => void;
}

export default function AdditionalInfoSection({
  formData,
  onChange,
}: AdditionalInfoSectionProps) {
  return (
    <div>
      <h2 className="text-lg font-medium mb-4 text-primary">
        Informações Adicionais
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label
            htmlFor="variablePrice"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Tipo de Preço *
          </label>
          <select
            id="variablePrice"
            name="variablePrice"
            value={formData.variablePrice}
            onChange={onChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="false">Preço Fixo</option>
            <option value="true">Preço Variável</option>
          </select>
          <p className="text-xs text-gray-500 mt-1">
            Selecione "Preço Variável" se o cliente tiver valores negociados
            caso a caso.
          </p>
        </div>
        <div>
          <label
            htmlFor="onlyBillet"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Forma de Cobrança *
          </label>
          <select
            id="onlyBillet"
            name="onlyBillet"
            value={formData.onlyBillet}
            onChange={onChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="false">Boleto e Nota Fiscal</option>
            <option value="true">Somente Boleto</option>
          </select>
          <p className="text-xs text-gray-500 mt-1">
            Selecione "Somente Boleto" se o cliente nunca deve receber nota
            fiscal.
          </p>
        </div>
        <div>
          <label
            htmlFor="requiresPurchaseProof"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Exige Comprovante de Compra (Foto) *
          </label>
          <select
            id="requiresPurchaseProof"
            name="requiresPurchaseProof"
            value={formData.requiresPurchaseProof}
            onChange={onChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="false">Não precisa comprovar</option>
            <option value="true">Precisa anexar foto da nota</option>
          </select>
          <p className="text-xs text-gray-500 mt-1">
            Selecione "Precisa anexar foto da nota" para manter a foto da nota
            como comprovante quando a compra vier de uma captura por celular;
            caso contrário, só os dados extraídos são guardados e a foto é
            descartada.
          </p>
        </div>
      </div>
    </div>
  );
}
