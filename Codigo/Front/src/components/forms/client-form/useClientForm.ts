"use client";

import { type FormEvent, useEffect, useState } from "react";
import { useCep } from "@/hooks/useCep";
import { showError, showSuccess } from "@/utils/toastUtils";
import {
  formatarCEP,
  formatarCNPJ,
  formatarCPF,
  formatarIEMinasGerais,
  formatarTelefone,
} from "@/utils/validationUtils";
import { BAIRRO_MAX_LENGTH, SANTA_LUZIA_CIDE_CODE } from "./constants";
import type {
  ClientFormBlurEvent,
  ClientFormChangeEvent,
  ClientFormData,
  ClientFormErrors,
} from "./types";
import { validateClientField, validateClientForm } from "./validation";

interface UseClientFormParams {
  initialData?: Partial<ClientFormData>;
  onSubmit: (data: ClientFormData) => Promise<void>;
}

export function useClientForm({ initialData, onSubmit }: UseClientFormParams) {
  const [formData, setFormData] = useState<ClientFormData>({
    nome: "",
    apelido: "",
    email: "",
    telefone: "",
    cpfCnpj: "",
    cep: "",
    endereco: "",
    numero: "",
    complemento: "",
    bairro: "",
    cidade: "",
    estado: "",
    variablePrice: "false",
    stateRegistration: "",
    stateIndicator: "9", // Padrão: Não contribuinte
    cideCode: SANTA_LUZIA_CIDE_CODE,
    onlyBillet: "false",
    requiresPurchaseProof: "false",
    ...initialData,
  });

  const [formErrors, setFormErrors] = useState<ClientFormErrors>({
    nome: "",
    email: "",
    telefone: "",
    cpfCnpj: "",
    cep: "",
    endereco: "",
    numero: "",
    bairro: "",
    cidade: "",
    estado: "",
    stateRegistration: "",
    cideCode: "",
  });

  const { consultarCep } = useCep();

  const isCNPJ = formData.cpfCnpj.replace(/[^0-9A-Za-z]/g, "").length > 11;

  useEffect(() => {
    if (initialData) {
      setFormData((prev) => ({
        ...prev,
        ...initialData,
      }));
    }
  }, [initialData]);

  // Quando mudar o indicador estadual para "não contribuinte", limpa a IE
  useEffect(() => {
    if (formData.stateIndicator !== "1") {
      setFormData((prev) => ({
        ...prev,
        stateRegistration: "",
      }));
      setFormErrors((prev) => ({
        ...prev,
        stateRegistration: "",
      }));
    }
  }, [formData.stateIndicator]);

  const buscarEnderecoPorCEP = async (cep: string) => {
    try {
      if (cep.replace(/\D/g, "").length < 8) return;

      showSuccess("Buscando informações do CEP...");

      const endereco = await consultarCep(cep);

      if (!endereco) {
        showError("CEP não encontrado");
        return;
      }

      // O ViaCEP pode retornar bairros com nomes compostos que passam do limite
      // aceito pelo boleto (30 caracteres). Trunca aqui para nunca deixar passar.
      const bairro = (endereco.bairro || formData.bairro).slice(
        0,
        BAIRRO_MAX_LENGTH,
      );

      setFormData((prev) => ({
        ...prev,
        endereco: endereco.logradouro || prev.endereco,
        bairro,
        cidade: endereco.localidade || prev.cidade,
        estado: endereco.uf || prev.estado,
      }));

      setFormErrors((prev) => ({
        ...prev,
        endereco: "",
        bairro: "",
        cidade: "",
        estado: "",
      }));

      showSuccess("Endereço preenchido com sucesso!");
    } catch (error) {
      console.error("Erro ao buscar endereço pelo CEP:", error);
      showError("Não foi possível buscar o endereço pelo CEP");
    }
  };

  const handleChange = (e: ClientFormChangeEvent) => {
    const { name, value } = e.target;
    let formattedValue = value;

    if (name === "cpfCnpj") {
      // CNPJ passa a aceitar letras (A-Z) a partir de ago/2026 — só remove a máscara,
      // sem descartar letras. CPF continua só numérico.
      const cleanedValue = value.replace(/[^0-9A-Za-z]/g, "").toUpperCase();
      if (!/[A-Z]/.test(cleanedValue) && cleanedValue.length <= 11) {
        formattedValue = formatarCPF(cleanedValue);
      } else {
        formattedValue = formatarCNPJ(cleanedValue);
      }
    } else if (name === "telefone") {
      formattedValue = formatarTelefone(value);
    } else if (name === "cep") {
      formattedValue = formatarCEP(value);
    } else if (name === "stateRegistration") {
      formattedValue = formatarIEMinasGerais(value);
    } else if (name === "bairro") {
      formattedValue = value.slice(0, BAIRRO_MAX_LENGTH);
    }

    setFormData((prev) => ({
      ...prev,
      [name]: formattedValue,
    }));

    if (formErrors[name as keyof typeof formErrors]) {
      setFormErrors({
        ...formErrors,
        [name]: "",
      });
    }
  };

  const handleFieldBlur =
    (name: keyof ClientFormErrors) => (e: ClientFormBlurEvent) => {
      setFormErrors({
        ...formErrors,
        [name]: validateClientField(name, e.target.value, isCNPJ),
      });
    };

  const handleCepBlur = async (e: ClientFormBlurEvent) => {
    const { value } = e.target;
    const cep = value.replace(/\D/g, "");

    if (cep.length === 8) {
      await buscarEnderecoPorCEP(cep);
    }

    setFormErrors({
      ...formErrors,
      cep: validateClientField("cep", value, isCNPJ),
    });
  };

  const handleNumeroSemNumeroToggle = (semNumero: boolean) => {
    setFormData((prev) => ({
      ...prev,
      numero: semNumero ? "S/N" : "",
    }));
    setFormErrors((prev) => ({
      ...prev,
      numero: semNumero ? "" : validateClientField("numero", "", isCNPJ),
    }));
  };

  const validateForm = (): boolean => {
    const errors = validateClientForm(formData, isCNPJ);
    setFormErrors(errors);
    return !Object.values(errors).some((error) => error);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      showError("Por favor, corrija os erros no formulário.");
      return;
    }

    formData.stateRegistration = formData.stateRegistration.replace(/\D/g, "");

    await onSubmit(formData);
  };

  return {
    formData,
    formErrors,
    isCNPJ,
    handleChange,
    handleFieldBlur,
    handleCepBlur,
    buscarEnderecoPorCEP,
    handleNumeroSemNumeroToggle,
    handleSubmit,
  };
}
