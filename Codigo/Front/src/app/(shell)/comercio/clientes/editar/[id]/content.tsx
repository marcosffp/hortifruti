"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { clientService } from "@/services/clientService";
import { showError, showSuccess } from "@/services/notificationService";
import { parseAddressFromBackend, formatAddressForBackend } from "@/utils/addressUtils";
import ClientForm, { ClientFormData } from "@/components/forms/ClientForm";
import { ArrowLeft } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";

interface EditarClientePageProps {
  id: string
}

export default function EditarClientePageContent({ id }: EditarClientePageProps) {
  const router = useRouter();
  const clientId = parseInt(id, 10);

  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [initialData, setInitialData] = useState<Partial<ClientFormData>>();

  useEffect(() => {
    const fetchClienteData = async () => {
      try {
        setIsLoading(true);
        setError("");

        const clientData = await clientService.getClientById(clientId);

        // Parse do endereço usando a função utilitária
        const addressParts = parseAddressFromBackend(clientData.address || "");

        setInitialData({
          nome: clientData.clientName,
          email: clientData.email || "",
          telefone: clientData.phoneNumber || "",
          cpfCnpj: clientData.document || "",
          cep: addressParts.cep,
          endereco: addressParts.endereco,
          numero: addressParts.numero,
          complemento: addressParts.complemento,
          bairro: addressParts.bairro,
          cidade: addressParts.cidade,
          estado: addressParts.estado,
          variablePrice: clientData.variablePrice ? "true" : "false",
          stateRegistration: clientData.stateRegistration || "",
          stateIndicator: clientData.stateIndicator?.toString() || "9",
        });
      } catch (error) {
        console.error("Erro ao carregar dados do cliente:", error);
        setError("Não foi possível carregar os dados do cliente");
        showError("Erro ao carregar cliente");
      } finally {
        setIsLoading(false);
      }
    };

    if (clientId) {
      fetchClienteData();
    }
  }, [clientId]);

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
        stateIndicator: formData.stateIndicator ? parseInt(formData.stateIndicator) : null,
      };

      await clientService.updateClient(clientId, clientData);

      showSuccess("Cliente atualizado com sucesso!");
      router.push("/comercio/clientes");
    } catch (error) {
      showError("Erro ao atualizar cliente");
      console.error("Erro ao atualizar cliente:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <main className="flex-1 p-6 bg-gray-50 overflow-auto">
        <div className="max-w-7xl mx-auto">
          <div className="mb-6 flex items-center">
            <Link href="/comercio/clientes" className="mr-4">
              <Button
                variant="outline"
                icon={<ArrowLeft size={18} />}
                className="px-2 py-1"
              />
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Editar Cliente</h1>
              <p className="mt-1 text-sm text-gray-500">
                Carregando dados do cliente...
              </p>
            </div>
          </div>
          <div className="flex justify-center items-center h-64 bg-white rounded-lg shadow-sm border">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="flex-1 p-6 bg-gray-50 overflow-auto">
        <div className="max-w-7xl mx-auto">
          <div className="mb-6 flex items-center">
            <Link href="/comercio/clientes" className="mr-4">
              <Button
                variant="outline"
                icon={<ArrowLeft size={18} />}
                className="px-2 py-1"
              />
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Editar Cliente</h1>
            </div>
          </div>
          <div className="bg-red-50 border border-red-200 p-4 rounded-md">
            <p className="text-red-700">{error}</p>
            <Button
              variant="outline"
              className="mt-3"
              onClick={() => router.push("/comercio/clientes")}
            >
              Voltar para Clientes
            </Button>
          </div>
        </div>
      </main>
    );
  }

  return (
    <ClientForm
      initialData={initialData}
      onSubmit={handleSubmit}
      isSubmitting={isSubmitting}
      title="Editar Cliente"
      subtitle={`Edite os dados do cliente #${id}.`}
      submitButtonText="Salvar Alterações"
    />
  );
}
