import type {
  ClientFormBlurEvent,
  ClientFormChangeEvent,
  ClientFormData,
  ClientFormErrors,
} from "./types";

interface PersonalDataSectionProps {
  formData: ClientFormData;
  formErrors: ClientFormErrors;
  onChange: (e: ClientFormChangeEvent) => void;
  onFieldBlur: (
    name: keyof ClientFormErrors,
  ) => (e: ClientFormBlurEvent) => void;
}

export default function PersonalDataSection({
  formData,
  formErrors,
  onChange,
  onFieldBlur,
}: PersonalDataSectionProps) {
  return (
    <div className="border-b pb-6">
      <h2 className="text-lg font-medium mb-4 text-primary">Dados Pessoais</h2>
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
            onChange={onChange}
            onBlur={onFieldBlur("nome")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.nome ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="Digite o nome completo"
          />
          {formErrors.nome && (
            <p className="text-red-500 text-xs mt-1">{formErrors.nome}</p>
          )}
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
            onChange={onChange}
            onBlur={onFieldBlur("cpfCnpj")}
            required
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.cpfCnpj ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="000.000.000-00 ou 00.000.000/0000-00"
          />
          {formErrors.cpfCnpj && (
            <p className="text-red-500 text-xs mt-1">{formErrors.cpfCnpj}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            E-mail
          </label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={onChange}
            onBlur={onFieldBlur("email")}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.email ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="exemplo@email.com"
          />
          {formErrors.email && (
            <p className="text-red-500 text-xs mt-1">{formErrors.email}</p>
          )}
        </div>
        <div>
          <label
            htmlFor="telefone"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Telefone
          </label>
          <input
            type="tel"
            id="telefone"
            name="telefone"
            value={formData.telefone}
            onChange={onChange}
            onBlur={onFieldBlur("telefone")}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
              formErrors.telefone ? "border-red-500" : "border-gray-300"
            }`}
            placeholder="(00) 00000-0000"
          />
          {formErrors.telefone && (
            <p className="text-red-500 text-xs mt-1">{formErrors.telefone}</p>
          )}
        </div>
      </div>
    </div>
  );
}
