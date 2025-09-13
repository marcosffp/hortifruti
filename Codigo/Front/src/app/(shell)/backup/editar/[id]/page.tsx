"use client";

import { useState, useEffect, use } from "react";
import { ArrowLeft, Save } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { backupService } from "@/services/backupService";
import { showError, showSuccess } from "@/services/notificationService";

interface EditarUsuarioPageProps {
    readonly params: Promise<{
        readonly id: string;
    }>;
}

const EditarUsuarioPage = ({ params }: EditarUsuarioPageProps) => {
    const { id } = use(params);
    const router = useRouter();
    const userId = parseInt(id, 10);

    const [formData, setFormData] = useState({
        name: "",
        cargo: "",
        perfil: "Funcionário" as "Funcionário" | "Gestor",
        password: ""
    });

    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                setIsLoading(true);
                setError("");
                const userData = await backupService.getUserById(userId);
                setFormData({
                    name: userData.nome || "", 
                    cargo: userData.cargo || "",
                    perfil: userData.perfil || "Funcionário",
                    password: ""
                });
            } catch (error) {
                setError("Erro ao carregar dados do usuário.");
            } finally {
                setIsLoading(false);
            }
        };
        fetchUserData();
    }, [userId]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setIsSubmitting(true);
            const userData = {
                name: formData.name,
                cargo: formData.cargo,
                perfil: formData.perfil as "Funcionário" | "Gestor" | undefined,
                password: formData.password, // Incluir senha na atualização
            };
            await backupService.updateUser(userId, userData);
            showSuccess("Usuário atualizado com sucesso!");
            // Definir flag para recarregar a lista de usuários
            localStorage.setItem('shouldReloadUsers', 'true');
            router.push("/backup");
        } catch (error) {
            showError("Erro ao atualizar usuário");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
      <main className="flex-1 p-6 bg-gray-50 overflow-auto">
        <div className="max-w-3xl mx-auto">
          <div className="mb-6 flex items-center">
            <Link href="/backup" className="mr-4">
              <Button
                variant="outline"
                icon={<ArrowLeft size={18} />}
                className="px-2 py-1 cursor-pointer"
              />
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Editar Usuário
              </h1>
              <p className="mt-1 text-sm text-gray-500">
                Edite os dados do usuário #{id}.
              </p>
            </div>
          </div>

          {isLoading ? (
            <div className="flex justify-center items-center h-64 bg-white rounded-lg shadow-sm border">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
            </div>
          ) : error ? (
            <div className="bg-red-50 border border-red-200 p-4 rounded-md">
              <p className="text-red-700">{error}</p>
            </div>
          ) : (
            <form
              onSubmit={handleSubmit}
              className="bg-white rounded-lg shadow-sm border"
            >
              <div className="p-6 border-b">
                <h2 className="text-lg font-semibold mb-4">
                  Informações do Usuário
                </h2>
                <div className="grid md:grid-cols-2 gap-4">
                  <div>
                    <label
                      htmlFor="name"
                      className="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Nome *
                    </label>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      required
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  <div>
                    <label
                      htmlFor="cargo"
                      className="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Cargo
                    </label>
                    <input
                      type="text"
                      id="cargo"
                      name="cargo"
                      value={formData.cargo}
                      onChange={handleChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  <div>
                    <label
                      htmlFor="perfil"
                      className="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Perfil
                    </label>
                    <select
                      id="perfil"
                      name="perfil"
                      value={formData.perfil}
                      onChange={handleChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary cursor-pointer"
                    >
                      <option value="Funcionário">Funcionário</option>
                      <option value="Gestor">Gestor</option>
                    </select>
                  </div>
                  <div>
                    <label
                      htmlFor="password"
                      className="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Nova Senha (opcional)
                    </label>
                    <input
                      type="password"
                      id="password"
                      name="password"
                      value={formData.password}
                      onChange={handleChange}
                      minLength={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
                      placeholder="Deixe vazio para manter a senha atual"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Se não informar, será mantida a senha atual
                    </p>
                  </div>
                </div>
              </div>
              <div className="flex justify-end space-x-3 p-6">
                <Link href="/backup">
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
};

export default EditarUsuarioPage;
