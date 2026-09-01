import {
  validarCEP,
  validarCPFouCNPJ,
  validarEmail,
  validarTelefone,
} from "@/utils/validationUtils";
import type { ClientFormData, ClientFormErrors } from "./types";

export function validateClientField(
  name: string,
  value: string,
  isCNPJ: boolean,
): string {
  switch (name) {
    case "nome":
      return !value.trim() ? "Nome é obrigatório" : "";
    case "email":
      return value.trim() && !validarEmail(value) ? "Email inválido" : "";
    case "telefone":
      return value.trim() && !validarTelefone(value)
        ? "Telefone inválido. Formato: (XX) XXXXX-XXXX"
        : "";
    case "cpfCnpj":
      if (!value) return "CPF/CNPJ é obrigatório";
      return !validarCPFouCNPJ(value) ? "CPF/CNPJ inválido" : "";
    case "cep":
      if (!value) return "";
      return !validarCEP(value) ? "CEP inválido. Formato: XXXXX-XXX" : "";
    case "endereco":
      return !value.trim() ? "Endereço é obrigatório" : "";
    case "numero":
      return !value.trim() ? "Número é obrigatório" : "";
    case "bairro":
      return !value.trim() ? "Bairro é obrigatório" : "";
    case "cidade":
      return !value.trim() ? "Cidade é obrigatória" : "";
    case "estado":
      return !value.trim() ? "Estado é obrigatório" : "";
    case "stateRegistration":
      return "";
    case "cideCode":
      if (isCNPJ && !value.trim()) {
        return "Código CIDE é obrigatório para empresas (CNPJ)";
      }
      return "";
    default:
      return "";
  }
}

export function validateClientForm(
  formData: ClientFormData,
  isCNPJ: boolean,
): ClientFormErrors {
  return {
    nome: validateClientField("nome", formData.nome, isCNPJ),
    email: validateClientField("email", formData.email, isCNPJ),
    telefone: validateClientField("telefone", formData.telefone, isCNPJ),
    cpfCnpj: validateClientField("cpfCnpj", formData.cpfCnpj, isCNPJ),
    cep: validateClientField("cep", formData.cep, isCNPJ),
    endereco: validateClientField("endereco", formData.endereco, isCNPJ),
    numero: validateClientField("numero", formData.numero, isCNPJ),
    bairro: validateClientField("bairro", formData.bairro, isCNPJ),
    cidade: validateClientField("cidade", formData.cidade, isCNPJ),
    estado: validateClientField("estado", formData.estado, isCNPJ),
    stateRegistration: validateClientField(
      "stateRegistration",
      formData.stateRegistration,
      isCNPJ,
    ),
    cideCode: validateClientField("cideCode", formData.cideCode, isCNPJ),
  };
}
