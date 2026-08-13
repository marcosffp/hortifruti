"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import ClientForm, { type ClientFormData } from "@/components/forms/ClientForm";
import { clientService } from "@/services/clientService";
import { formatAddressForBackend } from "@/utils/addressUtils";
import { showError, showSuccess } from "@/utils/toastUtils";

export default function NovoClientePage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (formData: ClientFormData) => {
    try {
      setIsSubmitting(true);

      const addressFormatted = formatAddressForBackend({
        bairro: formData.bairro,
        cidade: formData.cidade,
        cep: formData.cep,
        estado: formData.estado,
        endereco: formData.endereco,
        numero: formData.numero,
        complemento: formData.complemento,
      });

      const clientData = {
        clientName: formData.nome,
        email: formData.email,
        phoneNumber: formData.telefone,
        address: addressFormatted,
        variablePrice: formData.variablePrice === "true",
        document: formData.cpfCnpj,
        stateRegistration: formData.stateRegistration || null,
        stateIndicator: formData.stateIndicator
          ? parseInt(formData.stateIndicator, 10)
          : null,
        cideCode: formData.cideCode || null,
        onlyBillet: formData.onlyBillet === "true",
        requiresPurchaseProof: formData.requiresPurchaseProof === "true",
      };

      await clientService.createClient(clientData);

      showSuccess("Cliente cadastrado com sucesso!");
      router.push("/comercio/clientes");
    } catch (error) {
      showError("Erro ao cadastrar cliente");
      console.error("Erro ao cadastrar cliente:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ClientForm
      onSubmit={handleSubmit}
      isSubmitting={isSubmitting}
      title="Novo Cliente"
      subtitle="Preencha o formulário abaixo para adicionar um novo cliente."
      submitButtonText="Salvar Cliente"
    />
  );
}
