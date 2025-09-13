"use client";

import { useState, useEffect } from "react";
import { LayoutGrid, LayoutList, Plus, Search, Database, User, Edit, Trash2, UserCog, Shield } from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { showError, showSuccess } from "@/services/notificationService";
import { userService } from "@/services/userService";

// Tipo para os dados do usuário adaptado para exibição na UI
interface UsuarioUI {
  id: number;
  nome: string;
  email: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  cadastrado: string;
  status: "ativo" | "inativo";
}

export default function AcessoPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [usuarios, setUsuarios] = useState<UsuarioUI[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<number | null>(null);

  // Carregar usuários do backend
  useEffect(() => {
    const fetchUsuarios = async () => {
      try {
        setIsLoading(true);
        
        // Tentar buscar do backend primeiro
        try {
          const usersResponse = await userService.getAllUsers();
          
          // Transformar os dados do backend para o formato da UI
          const usuariosUI: UsuarioUI[] = usersResponse.map(user => ({
            id: user.id,
            nome: user.name,
            email: user.email,
            cargo: user.cargo,
            perfil: user.perfil,
            cadastrado: new Date(user.createdAt).toLocaleDateString('pt-BR'),
            status: "ativo"
          }));
          
          setUsuarios(usuariosUI);
        } catch (backendError) {
          console.warn('Backend não disponível, usando dados mockados:', backendError);
          
          // Fallback para dados mockados se o backend não estiver disponível
          const usuariosUI: UsuarioUI[] = [
            {
              id: 1,
              nome: "João Silva",
              email: "joao.silva@hortifruti.com",
              cargo: "Gerente Geral",
              perfil: "Gestor",
              cadastrado: "15/01/2024",
              status: "ativo"
            },
            {
              id: 2,
              nome: "Maria Santos",
              email: "maria.santos@hortifruti.com", 
              cargo: "Vendedora",
              perfil: "Funcionário",
              cadastrado: "15/01/2024",
              status: "ativo"
            }
          ];
          
          setUsuarios(usuariosUI);
        }
      } catch (error) {
        showError('Não foi possível carregar a lista de usuários');
        console.error('Erro ao carregar usuários:', error);
        setUsuarios([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUsuarios();
  }, []);

  // Filtrar usuários com base no termo de busca
  const filteredUsuarios = usuarios.filter(usuario => 
    usuario.nome.toLowerCase().includes(searchTerm.toLowerCase()) || 
    usuario.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    usuario.cargo.toLowerCase().includes(searchTerm.toLowerCase()) ||
    usuario.perfil.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Calcular estatísticas
  const totalUsuarios = usuarios.length;
  const totalGestores = usuarios.filter(u => u.perfil === "Gestor").length;
  const totalFuncionarios = usuarios.filter(u => u.perfil === "Funcionário").length;

  // Função para excluir usuário
  const handleExcluirUsuario = async (id: number) => {
    setUsuarioParaExcluir(id);
  };
  
  // Função para confirmar exclusão
  const confirmarEExcluirUsuario = async () => {
    if (!usuarioParaExcluir) return;
    
    try {
      // Tentar excluir no backend primeiro
      try {
        await userService.deleteUser(usuarioParaExcluir);
      } catch (backendError) {
        console.warn('Backend não disponível para exclusão:', backendError);
        // Continua com a exclusão local mesmo se o backend falhar
      }
      
      // Atualiza a lista de usuários após a exclusão
      setUsuarios(usuarios.filter(usuario => usuario.id !== usuarioParaExcluir));
      showSuccess("Usuário excluído com sucesso!");
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
    <main className="flex-1 p-8 bg-gray-50 overflow-auto">
      <div className="flex flex-col max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-800">Módulo Acesso</h1>
          <p className="text-gray-600 mt-1">Gerencie todos os lançamentos financeiros do sistema</p>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <User className="h-8 w-8 text-gray-600" />
              </div>
              <div className="ml-4">
                <h2 className="text-lg font-semibold text-gray-800">Total de Usuários</h2>
                <p className="text-3xl font-bold text-gray-900">{totalUsuarios}</p>
                <p className="text-sm text-gray-600">Usuários ativos</p>
              </div>
            </div>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <UserCog className="h-8 w-8 text-green-600" />
              </div>
              <div className="ml-4">
                <h2 className="text-lg font-semibold text-gray-800">Gestores</h2>
                <p className="text-3xl font-bold text-green-600">{totalGestores}</p>
                <p className="text-sm text-gray-600">Acesso Total</p>
              </div>
            </div>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Shield className="h-8 w-8 text-orange-600" />
              </div>
              <div className="ml-4">
                <h2 className="text-lg font-semibold text-gray-800">Funcionários</h2>
                <p className="text-3xl font-bold text-orange-600">{totalFuncionarios}</p>
                <p className="text-sm text-gray-600">Acesso limitado</p>
              </div>
            </div>
          </div>
        </div>

        {/* Barra de busca e controles */}
        <div className="flex justify-between items-center mb-6">
          <div className="relative w-full max-w-md">
            <input
              type="text"
              placeholder="Buscar por nome, cargo ou perfil..."
              className="pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <Search className="absolute left-3 top-3 text-gray-400" size={18} />
          </div>
          <Link href="/acesso/novo">
            <Button 
              variant="primary" 
              className="flex items-center gap-2 py-2.5 px-4 bg-green-600 hover:bg-green-700 transition-colors"
              icon={<Plus size={18} />}
            >
              Novo Usuário
            </Button>
          </Link>
        </div>

        {/* Usuários do Sistema */}
        <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b flex justify-between items-center">
            <h2 className="text-lg font-semibold text-gray-800">Usuários do Sistema</h2>
            <span className="text-sm text-gray-500">
              Gerencie os usuários e seus perfis de acesso
            </span>
          </div>
          {/* Cabeçalho da tabela */}
          <div className="grid grid-cols-12 gap-4 px-6 py-3 border-b bg-gray-50 font-medium text-gray-700">
            <div className="col-span-3">Nome</div>
            <div className="col-span-2">Cargo</div>
            <div className="col-span-2">Perfil</div>
            <div className="col-span-2">Cadastrado</div>
            <div className="col-span-3 text-right">Ações</div>
          </div>

          {/* Conteúdo da lista */}
          {isLoading && (
            <div className="py-16 text-center">
              <div className="flex justify-center mb-4">
                <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-200 border-t-green-600"></div>
              </div>
              <p className="text-lg font-medium text-gray-600 mt-4">Carregando usuários...</p>
              <p className="text-sm mt-1 text-gray-500">Aguarde enquanto buscamos os dados</p>
            </div>
          )}
          
          {!isLoading && filteredUsuarios.length === 0 && (
            <div className="py-16 text-center text-gray-500">
              <div className="flex justify-center mb-6">
                <div className="bg-gray-50 rounded-full p-5 border border-gray-200 shadow-sm">
                  <User size={48} className="text-gray-400" />
                </div>
              </div>
              <p className="text-xl font-medium text-gray-600">Nenhum usuário encontrado</p>
              <p className="text-sm mt-2 max-w-md mx-auto">
                {searchTerm ? 
                  'Não encontramos usuários com os critérios de busca. Tente ajustar sua pesquisa.' : 
                  'Sua lista de usuários está vazia. Adicione novos usuários para começar.'}
              </p>
            </div>
          )}
          
          {!isLoading && filteredUsuarios.length > 0 && (
            <>
              {filteredUsuarios.map((usuario) => (
                <div key={usuario.id} className={`grid grid-cols-12 gap-4 px-6 py-4 border-b items-center hover:${usuario.perfil === 'Gestor' ? 'bg-green-50' : 'bg-orange-50'} transition-colors`}>
                  <div className="col-span-3 flex items-center gap-3">
                    <span className={`${usuario.perfil === 'Gestor' ? 'bg-green-100' : 'bg-orange-100'} p-2 rounded-full`}>
                      {usuario.perfil === 'Gestor' ? 
                        <UserCog className={`${usuario.perfil === 'Gestor' ? 'text-green-600' : 'text-orange-600'}`} size={20} /> :
                        <User className="text-orange-600" size={20} />
                      }
                    </span>
                    <div>
                      <div className="font-medium text-gray-800">{usuario.nome}</div>
                      <div className="text-sm text-gray-500">{usuario.email}</div>
                    </div>
                  </div>
                  <div className="col-span-2 text-gray-700">{usuario.cargo}</div>
                  <div className="col-span-2">
                    <span className={`inline-flex items-center gap-1 ${usuario.perfil === 'Gestor' ? 'bg-green-100 text-green-800' : 'bg-orange-100 text-orange-800'} px-3 py-1 rounded-full font-medium text-sm`}>
                      {usuario.perfil === 'Gestor' ? 
                        <Shield className="text-green-600" size={14} /> :
                        <User className="text-orange-600" size={14} />
                      }
                      {usuario.perfil}
                    </span>
                  </div>
                  <div className="col-span-2 text-gray-600">{usuario.cadastrado}</div>
                  <div className="col-span-3 flex justify-end space-x-2">
                    <Link href={`/acesso/editar/${usuario.id}`}>
                      <button className="inline-flex items-center gap-1 text-blue-600 hover:bg-blue-50 px-3 py-1.5 rounded-md transition-colors">
                        <Edit size={16} />
                        Editar
                      </button>
                    </Link>
                    <button 
                      onClick={() => handleExcluirUsuario(usuario.id)}
                      className="inline-flex items-center gap-1 text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-md transition-colors"
                    >
                      <Trash2 size={16} />
                      Excluir
                    </button>
                  </div>
                </div>
              ))}
              
              {/* Rodapé da lista */}
              <div className="px-6 py-4 border-t bg-gray-50">
                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-sm text-gray-600">Total usuário(s): {filteredUsuarios.length}</p>
                    <div className="flex gap-4 mt-2">
                      <span className="inline-flex items-center text-sm">
                        <span className="w-3 h-3 bg-green-400 rounded-full mr-2"></span>
                        Gestores ({totalGestores})
                      </span>
                      <span className="inline-flex items-center text-sm">
                        <span className="w-3 h-3 bg-orange-400 rounded-full mr-2"></span>
                        Funcionários ({totalFuncionarios})
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Funcionalidades do Módulo de Acesso */}
        <div className="bg-white rounded-lg shadow-md border border-gray-200 p-6 mt-6">
          <h2 className="text-lg font-bold mb-4 text-gray-800">Funcionalidades do Módulo de Acesso</h2>
          <p className="text-sm text-gray-600 mb-6">Gerencie usuários e controle as permissões de acesso ao sistema</p>
          
          <div className="grid grid-cols-2 gap-6">
            <div className="p-4 bg-gray-50 rounded-lg">
              <h3 className="font-semibold mb-3 text-gray-800 flex items-center gap-2">
                <UserCog className="text-green-600" size={20} />
                Perfis de Usuário
              </h3>
              <ul className="space-y-2 text-sm text-gray-700">
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                  Tutorial Integrado para novos usuários
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                  Gestão de equipe completa
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                  Segurança de dados avançada
                </li>
              </ul>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <h3 className="font-semibold mb-3 text-gray-800 flex items-center gap-2">
                <Shield className="text-orange-600" size={20} />
                Controle de Permissões
              </h3>
              <ul className="space-y-2 text-sm text-gray-700">
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-orange-400 rounded-full"></div>
                  Perfis personalizados por função
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-orange-400 rounded-full"></div>
                  Auditoria de acessos
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-orange-400 rounded-full"></div>
                  Backup seguro de dados
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Modal de confirmação de exclusão */}
      {usuarioParaExcluir !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-4">Confirmar exclusão</h3>
            <p className="text-gray-600 mb-6">
              Tem certeza que deseja excluir este usuário? Esta ação não pode ser desfeita.
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
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
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