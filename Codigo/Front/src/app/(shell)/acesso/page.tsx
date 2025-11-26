"use client";

import { useState, useEffect } from "react";
import {
  Plus,
  Search,
  User,
  Edit,
  Trash2,
  UserCog,
  Shield,
} from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { showError, showSuccess } from "@/services/notificationService";
import { backupService } from "@/services/acessoService";

// Tipo para os dados do usuário adaptado para exibição na UI
interface UsuarioUI {
  id: number;
  nome: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  cadastrado: string;
  status: "ativo" | "inativo";
}

export default function AcessoPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [usuarios, setUsuarios] = useState<UsuarioUI[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<number | null>(
    null
  );
  const [viewMode, setViewMode] = useState<"lista" | "cards">("lista");
  
  // Carregar preferência de visualização salva
  useEffect(() => {
    const savedViewMode = localStorage.getItem("accessViewMode");
    if (savedViewMode === "cards" || savedViewMode === "lista") {
      setViewMode(savedViewMode);
    }
  }, []);

  // Função para carregar usuários do backend
  const fetchUsuarios = async () => {
    try {
      setIsLoading(true);
      const usuariosFormatados = await backupService.getFormattedUsers();
      setUsuarios(usuariosFormatados);
    } catch (error) {
      showError("Não foi possível carregar a lista de usuários");
      console.error("Erro ao carregar usuários:", error);
      setUsuarios([]);
    } finally {
      setIsLoading(false);
    }
  };

  // Carregar usuários na primeira vez
  useEffect(() => {
    fetchUsuarios();
  }, []);

  // Recarregar usuários quando a página voltar ao foco (útil quando volta da página de criação/edição)
  useEffect(() => {
    const handleFocus = () => {
      fetchUsuarios();
    };

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        fetchUsuarios();
      }
    };

    // Verificar se há um flag indicando que precisa recarregar
    const checkReloadFlag = () => {
      const shouldReload = localStorage.getItem("shouldReloadUsers");
      if (shouldReload) {
        localStorage.removeItem("shouldReloadUsers");
        fetchUsuarios();
      }
    };

    // Verificar imediatamente ao montar o componente
    checkReloadFlag();

    // Adicionar listeners
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    // Verificar periodicamente por flags de reload
    const interval = setInterval(checkReloadFlag, 500);

    return () => {
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      clearInterval(interval);
    };
  }, []);

  // Filtrar usuários com base no termo de busca
  const filteredUsuarios = usuarios.filter(
    (usuario) =>
      (usuario.nome ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (usuario.cargo ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (usuario.perfil ?? "").toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Calcular estatísticas
  const totalUsuarios = usuarios.length;
  const totalGestores = usuarios.filter((u) => u.perfil === "Gestor").length;
  const totalFuncionarios = usuarios.filter(
    (u) => u.perfil === "Funcionário"
  ).length;

  // Função para excluir usuário
  const handleExcluirUsuario = async (id: number) => {
    setUsuarioParaExcluir(id);
  };

  // Função para confirmar exclusão
  const confirmarEExcluirUsuario = async () => {
    if (!usuarioParaExcluir) return;

    try {
      const sucesso = await backupService.deleteUser(usuarioParaExcluir);

      if (sucesso) {
        // Recarregar a lista de usuários do servidor após a exclusão
        await fetchUsuarios();
        showSuccess("Usuário excluído com sucesso!");
      } else {
        showError("Erro ao excluir usuário");
      }

      setUsuarioParaExcluir(null);
    } catch (error) {
      showError("Erro ao excluir usuário");
      console.error("Erro ao excluir usuário:", error);
    }
  };

  // Função para cancelar exclusão
  const cancelarExclusao = () => {
    setUsuarioParaExcluir(null);
  };

  return (
    <main className="flex-1 p-4 sm:p-6 bg-gray-50 overflow-auto">
      <div className="flex flex-col max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-6 text-center sm:text-left">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-800">
            Módulo Acesso
          </h1>
          <p className="text-gray-600 mt-1">
            Gerencie todos os lançamentos financeiros do sistema
          </p>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
          <div className="bg-white p-4 sm:p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center sm:items-start">
            <User className="h-8 w-8 text-gray-600 mb-2 sm:mb-0" />
            <div className="text-center sm:text-left">
              <h2 className="text-lg font-semibold text-gray-800">
                Total de Usuários
              </h2>
              <p className="text-3xl font-bold text-gray-900">
                {totalUsuarios}
              </p>
              <p className="text-sm text-gray-600">Usuários ativos</p>
            </div>
          </div>
          <div className="bg-white p-4 sm:p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center sm:items-start">
            <UserCog className="h-8 w-8 text-green-600 mb-2 sm:mb-0" />
            <div className="text-center sm:text-left">
              <h2 className="text-lg font-semibold text-gray-800">Gestores</h2>
              <p className="text-3xl font-bold text-green-600">
                {totalGestores}
              </p>
              <p className="text-sm text-gray-600">Acesso Total</p>
            </div>
          </div>
          <div className="bg-white p-4 sm:p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center sm:items-start">
            <Shield className="h-8 w-8 text-orange-600 mb-2 sm:mb-0" />
            <div className="text-center sm:text-left">
              <h2 className="text-lg font-semibold text-gray-800">
                Funcionários
              </h2>
              <p className="text-3xl font-bold text-orange-600">
                {totalFuncionarios}
              </p>
              <p className="text-sm text-gray-600">Acesso limitado</p>
            </div>
          </div>
        </div>

        {/* Barra de busca e controles */}
        <div className="flex flex-col sm:flex-row justify-between items-center mb-6 gap-4">
          <div className="flex w-full max-w-md gap-2">
            <div className="relative flex-grow">
              <input
                type="text"
                placeholder="Buscar por nome, cargo ou perfil..."
                className="pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              <Search className="absolute left-3 top-3 text-gray-400" size={18} />
            </div>
            
            {/* Alternador de visualização (lista/cards) */}
            <div className="flex border border-gray-300 rounded-lg overflow-hidden">
              <button
                onClick={() => {
                  setViewMode("lista");
                  localStorage.setItem("accessViewMode", "lista");
                }}
                className={`px-3 py-2 flex items-center justify-center ${
                  viewMode === "lista"
                    ? "bg-green-600 text-white"
                    : "bg-white text-gray-700 hover:bg-gray-100"
                }`}
                aria-label="Visualizar em lista"
                title="Visualizar em lista"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="8" y1="6" x2="21" y2="6"></line>
                  <line x1="8" y1="12" x2="21" y2="12"></line>
                  <line x1="8" y1="18" x2="21" y2="18"></line>
                  <line x1="3" y1="6" x2="3.01" y2="6"></line>
                  <line x1="3" y1="12" x2="3.01" y2="12"></line>
                  <line x1="3" y1="18" x2="3.01" y2="18"></line>
                </svg>
              </button>
              <button
                onClick={() => {
                  setViewMode("cards");
                  localStorage.setItem("accessViewMode", "cards");
                }}
                className={`px-3 py-2 flex items-center justify-center ${
                  viewMode === "cards"
                    ? "bg-green-600 text-white"
                    : "bg-white text-gray-700 hover:bg-gray-100"
                }`}
                aria-label="Visualizar em cards"
                title="Visualizar em cards"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="7" height="7"></rect>
                  <rect x="14" y="3" width="7" height="7"></rect>
                  <rect x="14" y="14" width="7" height="7"></rect>
                  <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
              </button>
            </div>
          </div>
          
          <Link href="/acesso/novo">
            <Button
              variant="primary"
              className="flex items-center gap-2 py-2.5 px-4 bg-green-600 hover:bg-green-700 transition-colors cursor-pointer"
              icon={<Plus size={18} />}
            >
              Novo Usuário
            </Button>
          </Link>
        </div>

        {/* Usuários do Sistema */}
        <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b flex justify-between items-center">
            <h2 className="text-lg font-semibold text-gray-800">
              Usuários do Sistema
            </h2>
            <span className="text-sm text-gray-500">
              Gerencie os usuários e seus perfis de acesso
            </span>
          </div>
          
          {/* Vista em Lista */}
          {viewMode === "lista" && (
            <>
              {/* Cabeçalho da tabela */}
              <div className="flex justify-between gap-4 px-6 py-3 border-b bg-gray-50 font-medium text-gray-700">
                <div className="col-span-3">Nome</div>
                <div className="col-span-2">Cargo</div>
                <div className="col-span-2">Perfil</div>
                <div className="col-span-2">Cadastrado</div>
                <div className="col-span-3 text-right">Ações</div>
              </div>

              {/* Conteúdo da lista */}
              <div className="overflow-x-auto">
                {isLoading ? (
                  <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500"></div>
                  </div>
                ) : filteredUsuarios.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-8">
                    {searchTerm ? (
                      <>
                        <Search size={48} className="text-gray-300 mb-2" />
                        <p className="text-gray-500">Nenhum usuário encontrado com o termo "{searchTerm}"</p>
                      </>
                    ) : (
                      <>
                        <User size={48} className="text-gray-300 mb-2" />
                        <p className="text-gray-500">Nenhum usuário cadastrado</p>
                      </>
                    )}
                  </div>
                ) : (
                  <div className="min-w-[600px]">
                    {filteredUsuarios.map((usuario) => (
                      <div
                        key={usuario.id}
                        className={`flex justify-between gap-4 px-6 py-4 border-b items-center hover:${
                          usuario.perfil === "Gestor"
                            ? "bg-green-50"
                            : "bg-orange-50"
                        } transition-colors`}
                      >
                        {/* Nome */}
                        <div className="col-span-3 flex items-center gap-2">
                          <span
                            className={`${
                              usuario.perfil === "Gestor"
                                ? "bg-green-100"
                                : "bg-orange-100"
                            } p-2 rounded-full`}
                          >
                            {usuario.perfil === "Gestor" ? (
                              <UserCog
                                className={`${
                                  usuario.perfil === "Gestor"
                                    ? "text-green-600"
                                    : "text-orange-600"
                                }`}
                                size={20}
                              />
                            ) : (
                              <User className="text-orange-600" size={20} />
                            )}
                          </span>
                          <div className="truncate">{usuario.nome}</div>
                        </div>

                        {/* Cargo */}
                        <div className="col-span-2 text-gray-700 truncate min-w-[80px]">
                          {usuario.cargo}
                        </div>

                        {/* Perfil */}
                        <div className="col-span-2 flex items-center gap-2">
                          <span
                            className={`inline-flex items-center gap-1 ${
                              usuario.perfil === "Gestor"
                                ? "bg-green-100 text-green-800"
                                : "bg-orange-100 text-orange-800"
                            } px-3 py-1 rounded-full font-medium text-sm`}
                          >
                            {usuario.perfil === "Gestor" ? (
                              <Shield className="text-green-600" size={14} />
                            ) : (
                              <User className="text-orange-600" size={14} />
                            )}
                            <span className="truncate">{usuario.perfil}</span>
                          </span>
                        </div>

                        {/* Cadastrado */}
                        <div className="col-span-2 text-gray-600 truncate">
                          {usuario.cadastrado}
                        </div>

                        {/* Ações */}
                        <div className="col-span-3 flex justify-end space-x-2">
                          <Link href={`/acesso/editar/${usuario.id}`}>
                            <button className="inline-flex items-center gap-1 text-blue-600 hover:bg-blue-50 px-3 py-1.5 rounded-md transition-colors cursor-pointer">
                              <Edit size={16} />
                              <span className="max-lg:hidden">Editar</span>
                            </button>
                          </Link>
                          <button
                            onClick={() => handleExcluirUsuario(usuario.id)}
                            className="inline-flex items-center gap-1 text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-md transition-colors cursor-pointer"
                          >
                            <Trash2 size={16} />
                            <span className="max-lg:hidden">Excluir</span>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}

          {/* Vista em Cards */}
          {viewMode === "cards" && (
            <div className="p-4">
              {isLoading ? (
                <div className="flex justify-center items-center h-64">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500"></div>
                </div>
              ) : filteredUsuarios.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8">
                  {searchTerm ? (
                    <>
                      <Search size={48} className="text-gray-300 mb-2" />
                      <p className="text-gray-500">Nenhum usuário encontrado com o termo "{searchTerm}"</p>
                    </>
                  ) : (
                    <>
                      <User size={48} className="text-gray-300 mb-2" />
                      <p className="text-gray-500">Nenhum usuário cadastrado</p>
                    </>
                  )}
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {filteredUsuarios.map((usuario) => (
                    <div 
                      key={usuario.id} 
                      className={`border rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow ${
                        usuario.perfil === "Gestor" ? "border-green-200" : "border-orange-200"
                      }`}
                    >
                      <div className={`${
                        usuario.perfil === "Gestor" ? "bg-green-50" : "bg-orange-50"
                      } p-4 border-b ${
                        usuario.perfil === "Gestor" ? "border-green-100" : "border-orange-100"
                      }`}>
                        <div className="flex items-center gap-3">
                          <span className={`${
                            usuario.perfil === "Gestor" ? "bg-green-100" : "bg-orange-100"
                          } p-2.5 rounded-full`}>
                            {usuario.perfil === "Gestor" ? (
                              <UserCog className="text-green-600" size={24} />
                            ) : (
                              <User className="text-orange-600" size={24} />
                            )}
                          </span>
                          <div>
                            <h3 className="font-semibold text-gray-900 truncate">{usuario.nome}</h3>
                            <p className="text-sm text-gray-600 truncate">{usuario.cargo}</p>
                          </div>
                        </div>
                      </div>
                      <div className="p-4">
                        <div className="flex flex-col space-y-2">
                          <div className="flex justify-between items-center">
                            <span className="text-sm text-gray-600">Perfil:</span>
                            <span className={`inline-flex items-center gap-1 ${
                              usuario.perfil === "Gestor"
                                ? "bg-green-100 text-green-800"
                                : "bg-orange-100 text-orange-800"
                            } px-2.5 py-0.5 rounded-full font-medium text-sm`}>
                              {usuario.perfil === "Gestor" ? (
                                <Shield className="text-green-600" size={12} />
                              ) : (
                                <User className="text-orange-600" size={12} />
                              )}
                              {usuario.perfil}
                            </span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-sm text-gray-600">Cadastrado:</span>
                            <span className="text-sm text-gray-800">{usuario.cadastrado}</span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-sm text-gray-600">Status:</span>
                            <span className={`inline-block px-2 py-1 text-xs rounded-full ${
                              usuario.status === "ativo" 
                                ? "bg-green-100 text-green-800" 
                                : "bg-red-100 text-red-800"
                            }`}>
                              {usuario.status === "ativo" ? "Ativo" : "Inativo"}
                            </span>
                          </div>
                        </div>
                      </div>
                      <div className="flex border-t">
                        <Link href={`/acesso/editar/${usuario.id}`} className="flex-1">
                          <button className="w-full py-2 flex items-center justify-center gap-1 text-blue-600 hover:bg-blue-50 transition-colors">
                            <Edit size={16} />
                            <span>Editar</span>
                          </button>
                        </Link>
                        <div className="w-px bg-gray-200"></div>
                        <button 
                          onClick={() => handleExcluirUsuario(usuario.id)}
                          className="flex-1 py-2 flex items-center justify-center gap-1 text-red-600 hover:bg-red-50 transition-colors"
                        >
                          <Trash2 size={16} />
                          <span>Excluir</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Modal de confirmação de exclusão */}
      {usuarioParaExcluir !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-4">
              Confirmar exclusão
            </h3>
            <p className="text-gray-600 mb-6">
              Tem certeza que deseja excluir este usuário? Esta ação não pode
              ser desfeita.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={cancelarExclusao}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={confirmarEExcluirUsuario}
                className="px-4 py-2 bg-red-600/80 text-white rounded-md hover:bg-red-700 transition-colors"
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
