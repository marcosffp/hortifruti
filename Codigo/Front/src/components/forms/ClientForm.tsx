"use client";

import { useState, useEffect } from "react";
import { ArrowLeft, Save } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { cepService } from "@/services/cepService";
import { showError, showSuccess } from "@/services/notificationService";
import {
  validarCPFouCNPJ,
  validarEmail,
  validarTelefone,
  validarCEP,
  formatarCPF,
  formatarCNPJ,
  formatarTelefone,
  formatarCEP,
  formatarIEMinasGerais,
  validarIEMinasGerais
} from "@/utils/validationUtils";

export interface ClientFormData {
  nome: string;
  email: string;
  telefone: string;
  cpfCnpj: string;
  cep: string;
  endereco: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  estado: string;
  variablePrice: string;
  stateRegistration: string;
  stateIndicator: string;
}

interface ClientFormProps {
  initialData?: Partial<ClientFormData>;
  onSubmit: (data: ClientFormData) => Promise<void>;
  isSubmitting: boolean;
  title: string;
  subtitle: string;
  submitButtonText: string;
}

export default function ClientForm({
  initialData,
  onSubmit,
  isSubmitting,
  title,
  subtitle,
  submitButtonText
}: ClientFormProps) {
  // Estado para o formulário
  const [formData, setFormData] = useState<ClientFormData>({
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
    variablePrice: "false",
    stateRegistration: "",
    stateIndicator: "9", // Padrão: Não contribuinte
    ...initialData
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
    stateRegistration: "",
  });

  // Verifica se é CNPJ (empresa)
  const isCNPJ = formData.cpfCnpj.replace(/\D/g, "").length > 11;

  // Atualiza formData quando initialData mudar
  useEffect(() => {
    if (initialData) {
      setFormData(prev => ({
        ...prev,
        ...initialData
      }));
    }
  }, [initialData]);

  // Quando mudar o indicador estadual para "não contribuinte", limpa a IE
  useEffect(() => {
    if (formData.stateIndicator !== "1") {
      setFormData(prev => ({
        ...prev,
        stateRegistration: ""
      }));
      setFormErrors(prev => ({
        ...prev,
        stateRegistration: ""
      }));
    }
  }, [formData.stateIndicator]);

  // Função para buscar e preencher endereço pelo CEP
  const buscarEnderecoPorCEP = async (cep: string) => {
    try {
      if (cep.replace(/\D/g, "").length < 8) return;

      showSuccess("Buscando informações do CEP...");
      
      const endereco = await cepService.consultarCep(cep);
      
      if (!endereco) {
        showError("CEP não encontrado");
        return;
      }

      setFormData((prev) => ({
        ...prev,
        endereco: endereco.logradouro || prev.endereco,
        bairro: endereco.bairro || prev.bairro,
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

  // Manipulador de mudança de campos
  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    let formattedValue = value;

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
    } else if (name === "stateRegistration") {
      // Formata IE de Minas Gerais
      formattedValue = formatarIEMinasGerais(value);
    }

    setFormData((prev) => ({
      ...prev,
      [name]: formattedValue,
    }));

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
    
    if (cep.length === 8) {
      await buscarEnderecoPorCEP(cep);
    }
    
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
        // Validação de Inscrição Estadual desabilitada - aceita qualquer valor
        // // Só é obrigatório se for contribuinte ICMS (indicador = 1)
        // if (formData.stateIndicator === "1") {
        //   if (!value.trim()) {
        //     return "Inscrição Estadual é obrigatória para contribuintes ICMS";
        //   }
        //   // Valida formato de MG
        //   if (!validarIEMinasGerais(value)) {
        //     return "Inscrição Estadual inválida.";
        //   }
        // }
        return "";
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
      stateRegistration: validateField("stateRegistration", formData.stateRegistration),
    };
    
    setFormErrors(errors);
    return !Object.values(errors).some(error => error);
  };

  // Manipulador de envio do formulário
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      showError("Por favor, corrija os erros no formulário.");
      return;
    }

    formData.stateRegistration = formData.stateRegistration.replace(/\D/g, "");

    await onSubmit(formData);
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
          <h1 className="text-2xl font-bold">{title}</h1>
          <p className="text-gray-600">{subtitle}</p>
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
                <label htmlFor="nome" className="block text-sm font-medium text-gray-700 mb-1">
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
                <label htmlFor="cpfCnpj" className="block text-sm font-medium text-gray-700 mb-1">
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
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
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
                <label htmlFor="telefone" className="block text-sm font-medium text-gray-700 mb-1">
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

          {/* Dados Fiscais - Só aparece para CNPJ */}
          {isCNPJ && (
            <div className="border-b pb-6">
              <h2 className="text-lg font-medium mb-4 text-primary">
                Dados Fiscais
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="stateIndicator" className="block text-sm font-medium text-gray-700 mb-1">
                    Indicador de IE *
                  </label>
                  <select
                    id="stateIndicator"
                    name="stateIndicator"
                    value={formData.stateIndicator}
                    onChange={handleChange}
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
                  <label htmlFor="stateRegistration" className="block text-sm font-medium text-gray-700 mb-1">
                    Inscrição Estadual {formData.stateIndicator === "1" && "*"}
                  </label>
                  <input
                    type="text"
                    id="stateRegistration"
                    name="stateRegistration"
                    value={formData.stateRegistration}
                    onChange={handleChange}
                    onBlur={(e) => setFormErrors({...formErrors, stateRegistration: validateField("stateRegistration", e.target.value)})}
                    disabled={formData.stateIndicator !== "1"}
                    required={formData.stateIndicator === "1"}
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                      formErrors.stateRegistration ? "border-red-500" : "border-gray-300"
                    } ${formData.stateIndicator !== "1" ? "bg-gray-100 cursor-not-allowed" : ""}`}
                    placeholder="000.000.000.0000"
                    maxLength={16}
                  />
                  {formErrors.stateRegistration && (
                    <p className="text-red-500 text-xs mt-1">{formErrors.stateRegistration}</p>
                  )}
                  <p className="text-xs text-gray-500 mt-1">
                    {formData.stateIndicator === "1" 
                      ? "Obrigatório para contribuintes ICMS" 
                      : "Disponível apenas para contribuintes ICMS"}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Endereço */}
          <div className="border-b pb-6">
            <h2 className="text-lg font-medium mb-4 text-primary">Endereço</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label htmlFor="cep" className="block text-sm font-medium text-gray-700 mb-1">
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
                    className="px-3 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-800 transition-colors"
                    title="Buscar endereço pelo CEP"
                  >
                    Buscar
                  </button>
                </div>
                {formErrors.cep && <p className="text-red-500 text-xs mt-1">{formErrors.cep}</p>}
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
                <label htmlFor="numero" className="block text-sm font-medium text-gray-700 mb-1">
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
                  placeholder="Apto, Sala, etc."
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
                <label htmlFor="cidade" className="block text-sm font-medium text-gray-700 mb-1">
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
                <label htmlFor="estado" className="block text-sm font-medium text-gray-700 mb-1">
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
                {formErrors.estado && <p className="text-red-500 text-xs mt-1">{formErrors.estado}</p>}
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
                <label htmlFor="variablePrice" className="block text-sm font-medium text-gray-700 mb-1">
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
              {isSubmitting ? "Salvando..." : submitButtonText}
            </Button>
          </div>
        </form>
      </div>
    </main>
  );
}