import { SANTA_LUZIA_CIDE_CODE } from "./constants";
import type {
  ClientFormBlurEvent,
  ClientFormChangeEvent,
  ClientFormData,
  ClientFormErrors,
} from "./types";

interface FiscalDataSectionProps {
  formData: ClientFormData;
  formErrors: ClientFormErrors;
  onChange: (e: ClientFormChangeEvent) => void;
  onFieldBlur: (
    name: keyof ClientFormErrors,
  ) => (e: ClientFormBlurEvent) => void;
}

export default function FiscalDataSection({
  formData,
  formErrors,
  onChange,
  onFieldBlur,
}: FiscalDataSectionProps) {
  return (
    <div className="border-b pb-6">
      <h2 className="text-lg font-medium mb-4 text-primary">Dados Fiscais</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label
            htmlFor="stateIndicator"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Indicador de IE *
          </label>
          <select
            id="stateIndicator"
            name="stateIndicator"
            value={formData.stateIndicator}
            onChange={onChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="1">1 - Contribuinte ICMS</option>
            <option value="2">2 - Contribuinte isento de IE</option>
            <option value="9">9 - Não contribuinte</option>
          </select>
          <p className="text-xs text-gray-500 mt-1">
            Define se a empresa é contribuinte do ICMS
          </p>
        </div>
        <div>
          <label
            htmlFor="stateRegistration"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Inscrição Estadual {formData.stateIndicator === "1" && "*"}
          </label>
          <input
            type="text"
            id="stateRegistration"
            name="stateRegistration"
            value={formData.stateRegistration}
            onChange={onChange}
            onBlur={onFieldBlur("stateRegistration")}
            disabled={formData.stateIndicator !== "1"}
            required={formData.stateIndicator === "1"}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.stateRegistration
                ? "border-red-500"
                : "border-gray-300"
            } ${formData.stateIndicator !== "1" ? "bg-gray-100 cursor-not-allowed" : ""}`}
            placeholder="000.000.000.0000"
            maxLength={16}
          />
          {formErrors.stateRegistration && (
            <p className="text-red-500 text-xs mt-1">
              {formErrors.stateRegistration}
            </p>
          )}
          <p className="text-xs text-gray-500 mt-1">
            {formData.stateIndicator === "1"
              ? "Obrigatório para contribuintes ICMS"
              : "Disponível apenas para contribuintes ICMS"}
          </p>
        </div>
        <div>
          <label
            htmlFor="cideCode"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Código do Município do Destinatário *
          </label>
          <input
            type="text"
            id="cideCode"
            name="cideCode"
            value={formData.cideCode}
            onChange={onChange}
            onBlur={onFieldBlur("cideCode")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.cideCode ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="Digite o código CIDE"
            maxLength={20}
          />
          {formErrors.cideCode && (
            <p className="text-red-500 text-xs mt-1">{formErrors.cideCode}</p>
          )}
          {formData.cideCode === SANTA_LUZIA_CIDE_CODE && (
            <p className="text-xs text-gray-500 mt-1">Santa Luzia</p>
          )}
          <p className="text-xs text-gray-500 mt-1">
            Obrigatório para empresas (CNPJ)
          </p>
        </div>
      </div>
    </div>
  );
}
