"use client";

import { useState, useEffect } from "react";
import { 
  Calendar, 
  CircleCheck, 
  DollarSign, 
  LayoutGrid, 
  LayoutList, 
  MapPin, 
  Phone, 
  Plus, 
  Search, 
  Settings, 
  User 
} from "lucide-react";
import Button from "@/components/ui/Button";
import Link from "next/link";
import { clientService } from "@/services/clientService";
import { showError, showSuccess } from "@/services/notificationService";
import ClientCard from "@/components/modules/ClientCard";

// Tipo para os dados do cliente adaptado para exibição na UI
interface ClienteUI {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  endereco: string;
  status: "preco-fixo" | "preco-variavel";
  ultimaCompra?: string;
  totalCompras?: number;
}

export default function ClientesPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [clientes, setClientes] = useState<ClienteUI[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [viewMode, setViewMode] = useState<"list" | "grid">("list");
  
  // Carregar a preferência de visualização salva
  useEffect(() => {
    const savedViewMode = localStorage.getItem("clientesViewMode");
    if (savedViewMode === "list" || savedViewMode === "grid") {
      setViewMode(savedViewMode as "list" | "grid");
    }
  }, []);

  // Carregar clientes do backend
  useEffect(() => {
    const fetchClientes = async () => {
      try {
        setIsLoading(true);
        const clientesResponse = await clientService.getAllClients();

        // Transformar os dados do backend para o formato da UI
        const clientesUI: ClienteUI[] = clientesResponse.map((client) => ({
          id: client.id,
          nome: client.clientName,
          email: client.email || "",
          telefone: client.phoneNumber || "",
          endereco: client.address || "",
          status: client.variablePrice ? "preco-variavel" : "preco-fixo", // Usar variablePrice para determinar o tipo de preço
          ultimaCompra: "-", // Estes dados ainda não estão disponíveis no backend
          totalCompras: 0, // Estes dados ainda não estão disponíveis no backend
        }));

        setClientes(clientesUI);
      } catch (error) {
        showError("Não foi possível carregar a lista de clientes");
        console.error("Erro ao carregar clientes:", error);
        setClientes([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchClientes();
  }, []);

  // Filtrar clientes com base no termo de busca
  const filteredClientes = clientes.filter(
    (cliente) =>
      cliente.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cliente.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cliente.telefone.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  // State para controlar a confirmação de exclusão
  const [clienteParaExcluir, setClienteParaExcluir] = useState<number | null>(
    null,
  );

  // Função para abrir modal de confirmação
  const confirmarExclusao = (id: number) => {
    setClienteParaExcluir(id);
  };

  // Função para excluir cliente
  const handleExcluirCliente = async (id: number) => {
    confirmarExclusao(id);
  };

  // Função para confirmar exclusão
  const confirmarEExcluirCliente = async () => {
    if (!clienteParaExcluir) return;

    try {
      await clientService.deleteClient(clienteParaExcluir);
      // Atualiza a lista de clientes após a exclusão
      setClientes(
        clientes.filter((cliente) => cliente.id !== clienteParaExcluir),
      );
      showSuccess("Cliente excluído com sucesso!");
      setClienteParaExcluir(null); // Fecha o modal
    } catch (error) {
      showError("Erro ao excluir cliente");
      console.error("Erro ao excluir cliente:", error);
    }
  };

  // Função para cancelar exclusão
  const cancelarExclusao = () => {
    setClienteParaExcluir(null);
  };

  return (
    <>
      <main className="flex-1 p-8 bg-gray-50 overflow-auto">
        <div className="flex flex-col max-w-7xl mx-auto">
          {/* Cabeçalho da página */}
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-gray-800">
              Gestão de Clientes
            </h1>
            <p className="text-gray-600 mt-1">
              Gerencie os dados dos seus clientes, incluindo edição, adição e
              remoção de registros.
            </p>
          </div>

          {/* Barra de pesquisa e botão novo cliente */}
          <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-4">
              <div className="relative w-full max-w-md">
                <input
                  type="text"
                  placeholder="Buscar por nome ou email..."
                  className="pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
                <Search
                  className="absolute left-3 top-3 text-gray-400"
                  size={18}
                />
              </div>
              <div className="flex bg-gray-100 rounded-lg p-1">
                <button
                  onClick={() => {
                    setViewMode("list");
                    localStorage.setItem("clientesViewMode", "list");
                  }}
                  className={`p-2 rounded-md ${viewMode === "list" ? "bg-white shadow-sm" : "text-gray-500 hover:text-gray-700"}`}
                  aria-label="Visualização em lista"
                  title="Visualização em lista"
                >
                  <LayoutList size={20} />
                </button>
                <button
                  onClick={() => {
                    setViewMode("grid");
                    localStorage.setItem("clientesViewMode", "grid");
                  }}
                  className={`p-2 rounded-md ${viewMode === "grid" ? "bg-white shadow-sm" : "text-gray-500 hover:text-gray-700"}`}
                  aria-label="Visualização em grade"
                  title="Visualização em grade"
                >
                  <LayoutGrid size={20} />
                </button>
              </div>
            </div>
            <Link href="/comercio/clientes/novo">
              <Button
                variant="primary"
                className="flex items-center gap-2 py-2.5 px-4 bg-green-600 hover:bg-green-700 transition-colors"
                icon={<Plus size={18} />}
              >
                Novo Cliente
              </Button>
            </Link>
          </div>

          {/* Lista de Clientes */}
          <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-100 flex justify-between items-center bg-white">
              <div className="flex items-center">
                <div className="bg-green-50 p-2 rounded-lg mr-3">
                  <User size={20} className="text-green-600" />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-gray-800">
                    Lista de Clientes
                  </h2>
                  <p className="text-sm text-gray-500 mt-0.5">
                    Gerencie seus clientes e visualize informações detalhadas
                  </p>
                </div>
              </div>
              <div className="bg-green-50 px-3 py-1.5 rounded-full font-medium text-green-700 text-sm">
                {filteredClientes.length}{" "}
                {filteredClientes.length === 1 ? "cliente" : "clientes"}
              </div>
            </div>

            {viewMode === "list" && (
              <div className="grid grid-cols-12 gap-4 px-6 py-4 border-b bg-gradient-to-r from-gray-50 to-gray-100 shadow-sm">
                <div className="col-span-3 flex items-center gap-2">
                  <User size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Cliente</span>
                </div>
                <div className="col-span-2 flex items-center gap-2">
                  <Phone size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Contato</span>
                </div>
                <div className="col-span-3 flex items-center gap-2">
                  <MapPin size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Endereço</span>
                </div>
                <div className="col-span-1 flex items-center gap-2">
                  <CircleCheck size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Preço</span>
                </div>
                <div className="col-span-1 flex items-center gap-2">
                  <Calendar size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Compras</span>
                </div>
                <div className="col-span-1 flex items-center gap-2">
                  <DollarSign size={16} className="text-gray-500" />
                  <span className="font-semibold text-gray-800">Total</span>
                </div>
                <div className="col-span-1 flex justify-end">
                  <Settings size={16} className="text-gray-500" />
                </div>
              </div>
            )}

            {/* Conteúdo da lista */}
            {isLoading && (
              <div className="py-16 text-center">
                <div className="flex justify-center mb-4">
                  <div className="relative animate-pulse">
                    <div className="h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
                      <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-100 border-t-green-600 border-r-green-600"></div>
                    </div>
                    <div className="absolute bottom-0 right-0 bg-green-100 rounded-full p-1.5">
                      <User size={14} className="text-green-600" />
                    </div>
                  </div>
                </div>
                <p className="text-lg font-medium text-gray-700 mt-4">
                  Carregando clientes...
                </p>
                <p className="text-sm mt-1 text-gray-500 max-w-md mx-auto">
                  Aguarde enquanto buscamos os dados dos clientes cadastrados
                </p>
              </div>
            )}

            {!isLoading && filteredClientes.length === 0 && (
              <div className="py-16 text-center text-gray-500">
                <div className="flex justify-center mb-6">
                  {searchTerm ? (
                    <div className="bg-orange-50 rounded-full p-5 border border-orange-100 shadow-sm">
                      <Search size={48} className="text-orange-400" />
                    </div>
                  ) : (
                    <div className="bg-blue-50 rounded-full p-6 border border-blue-100 shadow-sm">
                      <User size={48} className="text-blue-400" />
                    </div>
                  )}
                </div>
                
                <p className="text-xl font-medium text-gray-700">
                  {searchTerm ? "Busca sem resultados" : "Nenhum cliente cadastrado"}
                </p>
                
                <p className="text-sm mt-3 max-w-md mx-auto text-gray-600">
                  {searchTerm
                    ? `Não encontramos clientes com o termo "${searchTerm}". Tente outras palavras-chave ou limpe o campo de busca.`
                    : "Sua lista de clientes está vazia. Adicione novos clientes para começar a gerenciar sua base de clientes."}
                </p>

                <div className="mt-8">
                  {searchTerm ? (
                    <button 
                      onClick={() => setSearchTerm("")}
                      className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors inline-flex items-center gap-2"
                    >
                      <Search size={16} />
                      <span>Limpar busca</span>
                    </button>
                  ) : (
                    <Link href="/comercio/clientes/novo">
                      <Button
                        variant="primary"
                        className="flex items-center gap-2 py-2.5 px-5 bg-green-600 hover:bg-green-700 transition-colors mx-auto shadow-sm"
                        icon={<Plus size={18} />}
                      >
                        Adicionar Cliente
                      </Button>
                    </Link>
                  )}
                </div>
              </div>
            )}

            {!isLoading &&
              filteredClientes.length > 0 &&
              viewMode === "list" &&
              filteredClientes.map((cliente) => (
                <ClientCard
                  key={cliente.id}
                  id={cliente.id}
                  nome={cliente.nome}
                  email={cliente.email}
                  telefone={cliente.telefone}
                  endereco={cliente.endereco}
                  status={cliente.status}
                  ultimaCompra={cliente.ultimaCompra}
                  totalCompras={cliente.totalCompras || 0}
                  onDelete={handleExcluirCliente}
                  displayMode="list"
                />
              ))}

            {!isLoading &&
              filteredClientes.length > 0 &&
              viewMode === "grid" && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 p-6">
                  {filteredClientes.map((cliente) => (
                    <ClientCard
                      key={cliente.id}
                      id={cliente.id}
                      nome={cliente.nome}
                      email={cliente.email}
                      telefone={cliente.telefone}
                      endereco={cliente.endereco}
                      status={cliente.status}
                      ultimaCompra={cliente.ultimaCompra}
                      totalCompras={cliente.totalCompras || 0}
                      onDelete={handleExcluirCliente}
                      displayMode="grid"
                    />
                  ))}
                </div>
              )}
          </div>
        </div>
      </main>

      {/* Modal de confirmação de exclusão */}
      {clienteParaExcluir !== null && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-4">
              Confirmar exclusão
            </h3>
            <p className="text-gray-600 mb-6">
              Tem certeza que deseja excluir este cliente? Esta ação não pode
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
                onClick={confirmarEExcluirCliente}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
