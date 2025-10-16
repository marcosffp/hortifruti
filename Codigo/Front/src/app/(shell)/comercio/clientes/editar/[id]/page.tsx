"use client";

import { useState, useEffect } from "react";
import { ArrowLeft, Save } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { clientService } from "@/services/clientService";
import { showError, showSuccess } from "@/services/notificationService";
import { cepService } from "@/services/cepService";
import {
  validarCPFouCNPJ,
  validarEmail,
  validarTelefone,
  validarCEP,
  formatarCPF,
  formatarCNPJ,
  formatarTelefone,
  formatarCEP
} from "@/utils/validationUtils";

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
    status: "ativo",
  });

  // Estados de carregamento
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  
  // Estado para erros de validação
  const [formErrors, setFormErrors] = useState({
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
  });

  // Carregar os dados do cliente
  useEffect(() => {
    const fetchClienteData = async () => {
      try {
        setIsLoading(true);
        setError("");

        const clientData = await clientService.getClientById(clientId);

        // Extrair informações do endereço (vem como um campo único do backend)
        // Formato esperado: Rua, Número, [Complemento,] Bairro, Cidade - UF, CEP: XXXXX-XXX
        let enderecoParts = {
          endereco: "",
          numero: "",
          complemento: "",
          bairro: "",
          cidade: "",
          estado: "",
          cep: "",
        };

        try {
          if (clientData.address) {
            const fullAddress = clientData.address;

            //[Rua, Número, (Complemento,) Bairro, Cidade - UF, CEP: XXXXX-XXX]
            const addressSplit = fullAddress.split(",");

            // Extrai CEP
            const cepMatch = fullAddress.match(/CEP:\s*([0-9\-]+)/);
            if (cepMatch) enderecoParts.cep = cepMatch[1];

            // Extrai Cidade e Estado
            const cityStateMatch = fullAddress.match(/,\s*([^,]+)\s*-\s*([A-Z]{2})\s*(,|$)/);
            if (cityStateMatch) {
              enderecoParts.cidade = cityStateMatch[1].trim();
              enderecoParts.estado = cityStateMatch[2].trim();
            }

            const bairroPart = addressSplit[addressSplit.length - 3];
            if (bairroPart) {
              enderecoParts.bairro = bairroPart.trim();
            }

            const numeroPart = addressSplit[1];
            if (numeroPart) {
              enderecoParts.numero = numeroPart.trim();
            }

            const ruaPart = addressSplit[0];
            if (ruaPart) {
              enderecoParts.endereco = ruaPart.trim();
            }

            // Verifica se há complemento
            if (addressSplit.length === 6) {
              const complementoPart = addressSplit[2];
              if (complementoPart) {
                enderecoParts.complemento = complementoPart.trim();
              }
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
          cpfCnpj: clientData.document || "", // Agora disponível do backend
          cep: enderecoParts.cep,
          endereco: enderecoParts.endereco,
          numero: enderecoParts.numero,
          complemento: enderecoParts.complemento,
          bairro: enderecoParts.bairro,
          cidade: enderecoParts.cidade,
          estado: enderecoParts.estado,
          observacoes: "", // Não disponível no backend ainda
          status: "ativo", // Não disponível no backend ainda
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
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => {
    const { name, value } = e.target;
    let formattedValue = value;

    // Formatação automática de alguns campos
    if (name === "cpfCnpj") {
      const numericValue = value.replace(/[^\d]/g, '');
      if (numericValue.length <= 11) {
        formattedValue = formatarCPF(numericValue);
      } else {
        formattedValue = formatarCNPJ(numericValue);
      }
    } else if (name === "telefone") {
      formattedValue = formatarTelefone(value);
    } else if (name === "cep") {
      formattedValue = formatarCEP(value);
    }

    setFormData((prev) => ({
      ...prev,
      [name]: formattedValue,
    }));

    // Limpa o erro quando o usuário começa a digitar novamente
    if (formErrors[name as keyof typeof formErrors]) {
      setFormErrors({
        ...formErrors,
        [name]: ""
      });
    }
  };
  
  // Função para buscar endereço pelo CEP
  const buscarEnderecoPorCEP = async (cep: string) => {
    if (!validarCEP(cep)) {
      showError("CEP inválido. Formato correto: XXXXX-XXX");
      return;
    }

    try {
      const endereco = await cepService.consultarCep(cep);
      
      if (!endereco) {
        showError("CEP não encontrado");
        return;
      }
      
      if (endereco.erro) {
        showError("CEP não encontrado");
        return;
      }
      
      setFormData(prev => ({
        ...prev,
        endereco: endereco.logradouro || "",
        bairro: endereco.bairro || "",
        cidade: endereco.localidade || "",
        estado: endereco.uf || ""
      }));
      
      showSuccess("Endereço encontrado com sucesso!");
    } catch (error) {
      console.error("Erro ao buscar CEP:", error);
      showError("Erro ao buscar CEP. Tente novamente.");
    }
  };
  
  // Manipulador para quando o campo de CEP perde o foco
  const handleCepBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const cep = e.target.value;
    if (cep && cep.length >= 8) {
      buscarEnderecoPorCEP(cep);
    }
    // Atualiza o estado de erro do campo
    setFormErrors({...formErrors, cep: validateField("cep", cep)});
  };
  
  // Validação de campos individuais
  const validateField = (name: string, value: string): string => {
    switch (name) {
      case "nome":
        return !value.trim() ? "Nome é obrigatório" : "";
      case "email":
        return !validarEmail(value) ? "Email inválido" : "";
      case "telefone":
        return !validarTelefone(value) ? "Telefone inválido. Formato: (XX) XXXXX-XXXX" : "";
      case "cpfCnpj":
        if (!value) return "CPF/CNPJ é obrigatório"; // Agora é obrigatório
        return !validarCPFouCNPJ(value) ? "CPF/CNPJ inválido" : "";
      case "cep":
        if (!value) return ""; // Não é obrigatório
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
      default:
        return "";
    }
  };

  // Valida o formulário completo
  const validateForm = (): boolean => {
    const errors = {
      nome: validateField("nome", formData.nome),
      email: validateField("email", formData.email),
      telefone: validateField("telefone", formData.telefone),
      cpfCnpj: validateField("cpfCnpj", formData.cpfCnpj),
      cep: validateField("cep", formData.cep),
      endereco: validateField("endereco", formData.endereco),
      numero: validateField("numero", formData.numero),
      bairro: validateField("bairro", formData.bairro),
      cidade: validateField("cidade", formData.cidade),
      estado: validateField("estado", formData.estado),
    };
    
    setFormErrors(errors);
    
    // Retorna true se não houver erros
    return !Object.values(errors).some(error => error);
  };

  // Manipulador de envio do formulário
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Valida o formulário antes de enviar
    if (!validateForm()) {
      showError("Por favor, corrija os erros no formulário.");
      return;
    }

    try {
      setIsSubmitting(true);

      // Mapear os dados do formulário para o formato esperado pelo backend
      const clientData = {
        clientName: formData.nome,
        email: formData.email,
        phoneNumber: formData.telefone,
        address: `${formData.endereco}, ${formData.numero}${formData.complemento ? ", " + formData.complemento : ""}, ${formData.bairro}, ${formData.cidade} - ${formData.estado}${formData.cep ? ", CEP: " + formData.cep : ""}`,
        variablePrice: false, // Valor padrão
        document: formData.cpfCnpj, // Nome do campo conforme esperado pelo backend
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
          <form
            onSubmit={handleSubmit}
            className="bg-white rounded-lg shadow-sm border"
          >
            {/* Informações Pessoais */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">
                Informações Pessoais
              </h2>
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label
                    htmlFor="nome"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Nome Completo *
                  </label>
                  <input
                    type="text"
                    id="nome"
                    name="nome"
                    value={formData.nome}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, nome: validateField("nome", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.nome ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="Digite o nome completo"
                  />
                  {formErrors.nome && <p className="text-red-500 text-xs mt-1">{formErrors.nome}</p>}
                </div>
                <div>
                  <label
                    htmlFor="cpfCnpj"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    CPF/CNPJ *
                  </label>
                  <input
                    type="text"
                    id="cpfCnpj"
                    name="cpfCnpj"
                    value={formData.cpfCnpj}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, cpfCnpj: validateField("cpfCnpj", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.cpfCnpj ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="000.000.000-00 ou 00.000.000/0000-00"
                  />
                  {formErrors.cpfCnpj && <p className="text-red-500 text-xs mt-1">{formErrors.cpfCnpj}</p>}
                </div>
                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    E-mail *
                  </label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, email: validateField("email", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.email ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="exemplo@email.com"
                  />
                  {formErrors.email && <p className="text-red-500 text-xs mt-1">{formErrors.email}</p>}
                </div>
                <div>
                  <label
                    htmlFor="telefone"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Telefone *
                  </label>
                  <input
                    type="tel"
                    id="telefone"
                    name="telefone"
                    value={formData.telefone}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, telefone: validateField("telefone", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.telefone ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="(00) 00000-0000"
                  />
                  {formErrors.telefone && <p className="text-red-500 text-xs mt-1">{formErrors.telefone}</p>}
                </div>
              </div>
            </div>

            {/* Endereço */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">Endereço</h2>
              <div className="grid md:grid-cols-3 gap-4">
                <div>
                  <label
                    htmlFor="cep"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    CEP
                  </label>
                  <div className="flex">
                    <input
                      type="text"
                      id="cep"
                      name="cep"
                      value={formData.cep}
                      onChange={handleChange}
                      onBlur={handleCepBlur}
                      className={`w-full px-3 py-2 border rounded-l-md focus:outline-none focus:ring-2 focus:ring-primary ${
                        formErrors.cep ? "border-red-500" : "border-gray-300"
                      }`}
                      placeholder="00000-000"
                    />
                    <button
                      type="button"
                      onClick={() => formData.cep && buscarEnderecoPorCEP(formData.cep)}
                      className="px-3 py-2 bg-blue-600 text-white rounded-r-md hover:bg-blue-700 transition-colors"
                    >
                      Buscar
                    </button>
                  </div>
                  {formErrors.cep && <p className="text-red-500 text-xs mt-1">{formErrors.cep}</p>}
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
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, endereco: validateField("endereco", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.endereco ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="Rua, Avenida, etc."
                  />
                  {formErrors.endereco && <p className="text-red-500 text-xs mt-1">{formErrors.endereco}</p>}
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
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, numero: validateField("numero", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.numero ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="123"
                  />
                  {formErrors.numero && <p className="text-red-500 text-xs mt-1">{formErrors.numero}</p>}
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
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
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
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, bairro: validateField("bairro", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.bairro ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="Nome do bairro"
                  />
                  {formErrors.bairro && <p className="text-red-500 text-xs mt-1">{formErrors.bairro}</p>}
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
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, cidade: validateField("cidade", e.target.value)})}
                    required
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.cidade ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="Nome da cidade"
                  />
                  {formErrors.cidade && <p className="text-red-500 text-xs mt-1">{formErrors.cidade}</p>}
                </div>
                <div>
                  <label
                    htmlFor="estado"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Estado *
                  </label>
                  <input
                    type="text"
                    id="estado"
                    name="estado"
                    value={formData.estado}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, estado: validateField("estado", e.target.value)})}
                    required
                    maxLength={2}
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.estado ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="UF"
                  />
                  {formErrors.estado && <p className="text-red-500 text-xs mt-1">{formErrors.estado}</p>}
                </div>
              </div>
            </div>

            {/* Informações Adicionais */}
            <div className="p-6 border-b">
              <h2 className="text-lg font-semibold mb-4">
                Informações Adicionais
              </h2>
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label
                    htmlFor="status"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
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
                  <label
                    htmlFor="observacoes"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
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
                <Button variant="outline" disabled={isSubmitting}>
                  Cancelar
                </Button>
              </Link>
              <Button
                variant="primary"
                type="submit"
                icon={<Save size={18} />}
                disabled={isSubmitting}
              >
                {isSubmitting ? "Salvando..." : "Salvar Alterações"}
              </Button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}
