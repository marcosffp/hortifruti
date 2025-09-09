"use client";

import { useState, useEffect } from "react";
import { ArrowLeft, Save } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { clientService } from "@/services/clientService";
import { showError, showSuccess } from "@/services/notificationService";

// Interface para os parâmetros da página
interface EditarClientePageProps {
  readonly params: {
    readonly id: string;
  };
}

export default function EditarClientePage({ params }: EditarClientePageProps) {
  const { id } = params;
  const router = useRouter();
  const clientId = parseInt(id, 10);

  // Estado para o formulário
  const [formData, setFormData] = useState({
    nome: "",
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
    observacoes: "",
    status: "ativo"
  });

  // Estados de carregamento
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Carregar os dados do cliente
  useEffect(() => {
    const fetchClienteData = async () => {
      try {
        setIsLoading(true);
        setError("");

        const clientData = await clientService.getClientById(clientId);

        // Extrair informações do endereço (vem como um campo único do backend)
        let enderecoParts = {
          endereco: "",
          numero: "",
          complemento: "",
          bairro: "",
          cidade: "",
          estado: "",
          cep: ""
        };

        // Tentar extrair partes do endereço - lógica simplificada
        try {
          if (clientData.address) {
            const fullAddress = clientData.address;

            // Tentar extrair CEP
            const cepMatch = fullAddress.match(/CEP: ([0-9\-]+)/);
            if (cepMatch) enderecoParts.cep = cepMatch[1];

            // Tentar extrair cidade e estado
            const cidadeEstadoMatch = fullAddress.match(/([^,]+) - ([A-Z]{2})/);
            if (cidadeEstadoMatch) {
              enderecoParts.cidade = cidadeEstadoMatch[1].trim();
              enderecoParts.estado = cidadeEstadoMatch[2];
            }

            // Assumir que o primeiro segmento é o endereço e número
            const parts = fullAddress.split(',');
            if (parts.length > 0) {
              const endNumMatch = parts[0].match(/(.*) ([0-9]+)/);
              if (endNumMatch) {
                enderecoParts.endereco = endNumMatch[1].trim();
                enderecoParts.numero = endNumMatch[2];
              } else {
                enderecoParts.endereco = parts[0].trim();
              }
            }

            // Se houver mais de 1 parte, pode ser complemento
            if (parts.length > 1) {
              enderecoParts.complemento = parts[1].trim();
            }

            // Se houver mais de 2 partes, pode ser bairro
            if (parts.length > 2) {
              enderecoParts.bairro = parts[2].trim();
            }
          }
        } catch (e) {
          console.error("Erro ao processar endereço:", e);
        }

        // Mapear os dados para o formato do formulário
        setFormData({
          nome: clientData.clientName,
          email: clientData.email || "",
          telefone: clientData.phoneNumber || "",
          cpfCnpj: "", // Não disponível no backend ainda
          cep: enderecoParts.cep,
          endereco: enderecoParts.endereco,
          numero: enderecoParts.numero,
          complemento: enderecoParts.complemento,
          bairro: enderecoParts.bairro,
          cidade: enderecoParts.cidade,
          estado: enderecoParts.estado,
          observacoes: "", // Não disponível no backend ainda
          status: "ativo" // Não disponível no backend ainda
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

  // Manipulador de mudança de campos
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // Manipulador de envio do formulário
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setIsSubmitting(true);

      // Mapear os dados do formulário para o formato esperado pelo backend
      const clientData = {
        clientName: formData.nome,
        email: formData.email,
        phoneNumber: formData.telefone,
        address: `${formData.endereco}, ${formData.numero}${formData.complemento ? ', ' + formData.complemento : ''}, ${formData.bairro}, ${formData.cidade} - ${formData.estado}, CEP: ${formData.cep}`,
        variablePrice: false // Valor padrão
      };

      // Enviar para o backend
      await clientService.updateClient(clientId, clientData);

      showSuccess("Cliente atualizado com sucesso!");

      // Redirecionar para a página de clientes após salvar
      router.push("/comercio/clientes");
    } catch (error) {
      showError("Erro ao atualizar cliente");
      console.error("Erro ao atualizar cliente:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

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
              Edite os dados do cliente #{id}.
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center h-64 bg-white rounded-lg shadow-sm border">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          </div>
        ) : error ? (
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
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm border">
            {/* Informações Pessoais */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">Informações Pessoais</h2>
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="nome" className="block text-sm font-medium text-gray-700 mb-1">
                    Nome Completo *
                  </label>
                  <input
                    type="text"
                    id="nome"
                    name="nome"
                    value={formData.nome}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="cpfCnpj" className="block text-sm font-medium text-gray-700 mb-1">
                    CPF/CNPJ
                  </label>
                  <input
                    type="text"
                    id="cpfCnpj"
                    name="cpfCnpj"
                    value={formData.cpfCnpj}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                    E-mail *
                  </label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="telefone" className="block text-sm font-medium text-gray-700 mb-1">
                    Telefone *
                  </label>
                  <input
                    type="tel"
                    id="telefone"
                    name="telefone"
                    value={formData.telefone}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            {/* Endereço */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">Endereço</h2>
              <div className="grid md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="cep" className="block text-sm font-medium text-gray-700 mb-1">
                    CEP
                  </label>
                  <input
                    type="text"
                    id="cep"
                    name="cep"
                    value={formData.cep}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div className="md:col-span-2">
                  <label htmlFor="endereco" className="block text-sm font-medium text-gray-700 mb-1">
                    Endereço *
                  </label>
                  <input
                    type="text"
                    id="endereco"
                    name="endereco"
                    value={formData.endereco}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="numero" className="block text-sm font-medium text-gray-700 mb-1">
                    Número *
                  </label>
                  <input
                    type="text"
                    id="numero"
                    name="numero"
                    value={formData.numero}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="complemento" className="block text-sm font-medium text-gray-700 mb-1">
                    Complemento
                  </label>
                  <input
                    type="text"
                    id="complemento"
                    name="complemento"
                    value={formData.complemento}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="bairro" className="block text-sm font-medium text-gray-700 mb-1">
                    Bairro *
                  </label>
                  <input
                    type="text"
                    id="bairro"
                    name="bairro"
                    value={formData.bairro}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="cidade" className="block text-sm font-medium text-gray-700 mb-1">
                    Cidade *
                  </label>
                  <input
                    type="text"
                    id="cidade"
                    name="cidade"
                    value={formData.cidade}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label htmlFor="estado" className="block text-sm font-medium text-gray-700 mb-1">
                    Estado *
                  </label>
                  <input
                    type="text"
                    id="estado"
                    name="estado"
                    value={formData.estado}
                    onChange={handleChange}
                    required
                    maxLength={2}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            {/* Informações Adicionais */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">Informações Adicionais</h2>
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
                    Status
                  </label>
                  <select
                    id="status"
                    name="status"
                    value={formData.status}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    <option value="ativo">Ativo</option>
                    <option value="inativo">Inativo</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label htmlFor="observacoes" className="block text-sm font-medium text-gray-700 mb-1">
                    Observações
                  </label>
                  <textarea
                    id="observacoes"
                    name="observacoes"
                    rows={4}
                    value={formData.observacoes}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            {/* Botões do formulário */}
            <div className="flex justify-end space-x-3 p-6">
              <Link href="/comercio/clientes">
                <Button variant="outline" disabled={isSubmitting}>Cancelar</Button>
              </Link>
              <Button
                variant="primary"
                type="submit"
                icon={<Save size={18} />}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Salvando...' : 'Salvar Alterações'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}
