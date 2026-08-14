import { BAIRRO_MAX_LENGTH, ESTADOS_BRASILEIROS } from "./constants";
import type {
  ClientFormBlurEvent,
  ClientFormChangeEvent,
  ClientFormData,
  ClientFormErrors,
} from "./types";

interface AddressSectionProps {
  formData: ClientFormData;
  formErrors: ClientFormErrors;
  onChange: (e: ClientFormChangeEvent) => void;
  onFieldBlur: (
    name: keyof ClientFormErrors,
  ) => (e: ClientFormBlurEvent) => void;
  onCepBlur: (e: ClientFormBlurEvent) => void;
  onBuscarCep: () => void;
  onNumeroSemNumeroToggle: (semNumero: boolean) => void;
}

export default function AddressSection({
  formData,
  formErrors,
  onChange,
  onFieldBlur,
  onCepBlur,
  onBuscarCep,
  onNumeroSemNumeroToggle,
}: AddressSectionProps) {
  return (
    <div className="border-b pb-6">
      <h2 className="text-lg font-medium mb-4 text-primary">Endereço</h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div>
          <label
            htmlFor="cep"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            CEP
          </label>
          <div className="flex items-center space-x-2">
            <div className="flex-grow relative">
              <input
                type="text"
                id="cep"
                name="cep"
                value={formData.cep}
                onChange={onChange}
                onBlur={onCepBlur}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                  formErrors.cep ? "border-red-500" : "border-gray-300"
                }`}
                placeholder="00000-000"
              />
            </div>
            <button
              type="button"
              onClick={onBuscarCep}
              className="px-3 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-800 transition-colors"
              title="Buscar endereço pelo CEP"
            >
              Buscar
            </button>
          </div>
          {formErrors.cep && (
            <p className="text-red-500 text-xs mt-1">{formErrors.cep}</p>
          )}
        </div>
        <div className="md:col-span-2">
          <label
            htmlFor="endereco"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Endereço *
          </label>
          <input
            type="text"
            id="endereco"
            name="endereco"
            value={formData.endereco}
            onChange={onChange}
            onBlur={onFieldBlur("endereco")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.endereco ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="Rua, Avenida, etc."
          />
          {formErrors.endereco && (
            <p className="text-red-500 text-xs mt-1">{formErrors.endereco}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="numero"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Número *
          </label>
          <input
            type="text"
            id="numero"
            name="numero"
            value={formData.numero}
            onChange={onChange}
            onBlur={onFieldBlur("numero")}
            disabled={formData.numero === "S/N"}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.numero ? "border-red-500" : "border-gray-300"
            } ${formData.numero === "S/N" ? "bg-gray-100 cursor-not-allowed" : ""}`}
            placeholder="123"
          />
          <label className="flex items-center mt-1 text-xs text-gray-600">
            <input
              type="checkbox"
              checked={formData.numero === "S/N"}
              onChange={(e) => onNumeroSemNumeroToggle(e.target.checked)}
              className="mr-1.5"
            />
            Endereço sem número (usar S/N)
          </label>
          {formErrors.numero && (
            <p className="text-red-500 text-xs mt-1">{formErrors.numero}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="complemento"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Complemento
          </label>
          <input
            type="text"
            id="complemento"
            name="complemento"
            value={formData.complemento}
            onChange={onChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="Apto, Sala, etc."
          />
        </div>
        <div>
          <label
            htmlFor="bairro"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Bairro *
          </label>
          <input
            type="text"
            id="bairro"
            name="bairro"
            value={formData.bairro}
            onChange={onChange}
            onBlur={onFieldBlur("bairro")}
            required
            maxLength={BAIRRO_MAX_LENGTH}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.bairro ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="Nome do bairro"
          />
          {formErrors.bairro && (
            <p className="text-red-500 text-xs mt-1">{formErrors.bairro}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="cidade"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Cidade *
          </label>
          <input
            type="text"
            id="cidade"
            name="cidade"
            value={formData.cidade}
            onChange={onChange}
            onBlur={onFieldBlur("cidade")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.cidade ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="Nome da cidade"
          />
          {formErrors.cidade && (
            <p className="text-red-500 text-xs mt-1">{formErrors.cidade}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="estado"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Estado *
          </label>
          <select
            id="estado"
            name="estado"
            value={formData.estado}
            onChange={onChange}
            onBlur={onFieldBlur("estado")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.estado ? "border-red-500" : "border-gray-300"
            }`}
          >
            <option value="">Selecione</option>
            {ESTADOS_BRASILEIROS.map((estado) => (
              <option key={estado.value} value={estado.value}>
                {estado.label}
              </option>
            ))}
          </select>
          {formErrors.estado && (
            <p className="text-red-500 text-xs mt-1">{formErrors.estado}</p>
          )}
        </div>
      </div>
    </div>
  );
}
