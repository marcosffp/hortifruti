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

export default function NovoClientePage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    variablePrice: "false", // Novo campo para preço variável (como string para uso no select)
  });
  
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

  // Função para buscar e preencher endereço pelo CEP
  const buscarEnderecoPorCEP = async (cep: string) => {
    try {
      // Verifica se o CEP tem pelo menos 8 dígitos (sem contar traços e pontos)
      if (cep.replace(/\D/g, "").length < 8) return;

      // Indica ao usuário que está buscando o CEP
      showSuccess("Buscando informações do CEP...");
      
      const endereco = await cepService.consultarCep(cep);
      
      if (!endereco) {
        showError("CEP não encontrado");
        return;
      }

      // Preenche os campos de endereço com os dados retornados
      setFormData((prev) => ({
        ...prev,
        endereco: endereco.logradouro || prev.endereco,
        bairro: endereco.bairro || prev.bairro,
        cidade: endereco.localidade || prev.cidade,
        estado: endereco.uf || prev.estado,
        // Mantém o número e complemento que já foram preenchidos
      }));

      // Limpar erros de validação dos campos preenchidos
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
  
  // Manipulador para quando o campo CEP perde o foco
  const handleCepBlur = async (e: React.FocusEvent<HTMLInputElement>) => {
    const { value } = e.target;
    const cep = value.replace(/\D/g, "");
    
    // Validar se é um CEP válido
    if (cep.length === 8) {
      await buscarEnderecoPorCEP(cep);
    }
    
    // Executar validação normal do CEP
    setFormErrors({
      ...formErrors,
      cep: validateField("cep", value)
    });
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
        variablePrice: formData.variablePrice === "true", // Convertendo string para booleano
        document: formData.cpfCnpj, // Nome do campo conforme esperado pelo backend
      };

      // Enviar para o backend
      const response = await clientService.createClient(clientData);

      showSuccess("Cliente cadastrado com sucesso!");

      // Redirecionar para a página de clientes após salvar
      router.push("/comercio/clientes");
    } catch (error) {
      showError("Erro ao cadastrar cliente");
      console.error("Erro ao cadastrar cliente:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto">
      <div className="mb-6 flex items-center">
        <Link href="/comercio/clientes" className="mr-4">
          <Button
            variant="outline"
            icon={<ArrowLeft size={18} />}
            className="px-2 py-1"
          />
        </Link>
        <div>
          <h1 className="text-2xl font-bold">Novo Cliente</h1>
          <p className="text-gray-600">
            Preencha o formulário abaixo para adicionar um novo cliente.
          </p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Dados Pessoais */}
          <div className="border-b pb-6">
            <h2 className="text-lg font-medium mb-4 text-primary">
              Dados Pessoais
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                      onChange={handleChange}
                      onBlur={handleCepBlur}
                      className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                        formErrors.cep ? "border-red-500" : "border-gray-300"
                      }`}
                      placeholder="00000-000"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => buscarEnderecoPorCEP(formData.cep)}
                    className="px-3 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
                    title="Buscar endereço pelo CEP"
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
                <select
                  id="estado"
                  name="estado"
                  value={formData.estado}
                  onChange={handleChange}
                  onBlur={(e) => setFormErrors({...formErrors, estado: validateField("estado", e.target.value)})}
                  required
                  className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                    formErrors.estado ? "border-red-500" : "border-gray-300"
                  }`}
                >
                  <option value="">Selecione</option>
                  <option value="AC">Acre</option>
                  <option value="AL">Alagoas</option>
                  <option value="AP">Amapá</option>
                  <option value="AM">Amazonas</option>
                  <option value="BA">Bahia</option>
                  <option value="CE">Ceará</option>
                  <option value="DF">Distrito Federal</option>
                  <option value="ES">Espírito Santo</option>
                  <option value="GO">Goiás</option>
                  <option value="MA">Maranhão</option>
                  <option value="MT">Mato Grosso</option>
                  <option value="MS">Mato Grosso do Sul</option>
                  <option value="MG">Minas Gerais</option>
                  <option value="PA">Pará</option>
                  <option value="PB">Paraíba</option>
                  <option value="PR">Paraná</option>
                  <option value="PE">Pernambuco</option>
                  <option value="PI">Piauí</option>
                  <option value="RJ">Rio de Janeiro</option>
                  <option value="RN">Rio Grande do Norte</option>
                  <option value="RS">Rio Grande do Sul</option>
                  <option value="RO">Rondônia</option>
                  <option value="RR">Roraima</option>
                  <option value="SC">Santa Catarina</option>
                  <option value="SP">São Paulo</option>
                  <option value="SE">Sergipe</option>
                  <option value="TO">Tocantins</option>
                </select>
              </div>
            </div>
          </div>

          {/* Informações Adicionais */}
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
                  onChange={handleChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="false">Preço Fixo</option>
                  <option value="true">Preço Variável</option>
                </select>
                <p className="text-xs text-gray-500 mt-1">
                  Selecione "Preço Variável" se o cliente tiver valores negociados caso a caso.
                </p>
              </div>
            </div>
          </div>

          {/* Botões do formulário */}
          <div className="flex justify-end space-x-3 pt-6 border-t">
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
              {isSubmitting ? "Salvando..." : "Salvar Cliente"}
            </Button>
          </div>
        </form>
      </div>
    </main>
  );
}
