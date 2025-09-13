"use client";

import { useState } from "react";
import { ArrowLeft, Save, User, UserCog, Shield } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { showError, showSuccess } from "@/services/notificationService";
import { backupService } from "@/services/backupService";
import { UserRequest } from "@/services/userService";
import { useRouter } from "next/navigation";

export default function NovoUsuarioPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState<UserRequest>({
    name: "",
    email: "",
    cargo: "",
    perfil: "Funcionário",
    password: ""
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name || !formData.email || !formData.cargo) {
      showError("Por favor, preencha todos os campos obrigatórios");
      return;
    }

    try {
      setIsSubmitting(true);
      await backupService.createUser(formData);
      showSuccess("Usuário criado com sucesso!");
      router.push("/backup");
    } catch (error) {
      showError("Erro ao criar usuário. Tente novamente.");
      console.error("Erro ao criar usuário:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex-1 p-8 bg-gray-50 overflow-auto">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8 flex items-center">
          <Link href="/backup" className="mr-4">
            <Button
              variant="outline"
              icon={<ArrowLeft size={20} />}
              className="px-3 py-2 cursor-pointer"
            />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Novo Usuário</h1>
            <p className="mt-1 text-sm text-gray-500">
              Cadastre um novo usuário no sistema
            </p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-md border border-gray-200">
          <div className="p-6 border-b">
            <h2 className="text-lg font-semibold text-gray-800">Informações do Usuário</h2>
            <p className="text-sm text-gray-600 mt-1">Preencha os dados do novo usuário</p>
          </div>
          
          <div className="p-6 space-y-6">
            {/* Nome */}
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                Nome Completo *
              </label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                placeholder="Digite o nome completo"
                required
              />
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                E-mail *
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                placeholder="Digite o e-mail"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              {/* Cargo */}
              <div>
                <label htmlFor="cargo" className="block text-sm font-medium text-gray-700 mb-2">
                  Cargo *
                </label>
                <input
                  type="text"
                  id="cargo"
                  name="cargo"
                  value={formData.cargo}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                  placeholder="Ex: Vendedor, Gerente..."
                  required
                />
              </div>

              {/* Perfil */}
              <div>
                <label htmlFor="perfil" className="block text-sm font-medium text-gray-700 mb-2">
                  Perfil de Acesso *
                </label>
                <select
                  id="perfil"
                  name="perfil"
                  value={formData.perfil}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                  required
                >
                  <option value="Funcionário">Funcionário</option>
                  <option value="Gestor">Gestor</option>
                </select>
              </div>
            </div>

            {/* Info sobre perfis */}
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-800 mb-3">Sobre os Perfis de Acesso:</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div className="flex items-start gap-2">
                  <UserCog className="text-green-600 mt-0.5" size={16} />
                  <div>
                    <strong className="text-green-600">Gestor:</strong>
                    <p className="text-gray-600 text-xs mt-1">Acesso total ao sistema, incluindo relatórios e configurações</p>
                  </div>
                </div>
                <div className="flex items-start gap-2">
                  <Shield className="text-orange-600 mt-0.5" size={16} />
                  <div>
                    <strong className="text-orange-600">Funcionário:</strong>
                    <p className="text-gray-600 text-xs mt-1">Acesso limitado às funcionalidades básicas do sistema</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t bg-gray-50 flex justify-end gap-3">
            <Link href="/backup">
              <Button
                variant="outline"
                className="px-4 py-2"
              >
                Cancelar
              </Button>
            </Link>
            <Button
              type="submit"
              variant="primary"
              className="px-6 py-2 bg-green-600 hover:bg-green-700"
              icon={<Save size={18} />}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Salvando..." : "Salvar Usuário"}
            </Button>
          </div>
        </form>
      </div>
    </main>
  );
}
