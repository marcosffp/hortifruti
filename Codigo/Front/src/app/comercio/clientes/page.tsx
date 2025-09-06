"use client";

import { useState } from "react";
import { Edit, Plus, Search, Trash2, User } from "lucide-react";
import Header from "../../../components/layout/Header";
import Sidebar from "../../../components/layout/Sidebar";
import Button from "../../../components/ui/Button";
import Link from "next/link";

// Tipos para os dados do cliente
interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  endereco: string;
  status: "ativo" | "inativo";
  ultimaCompra: string;
  totalCompras: number;
}

export default function ClientesPage() {
  const [searchTerm, setSearchTerm] = useState("");
  
  // Dados simulados de clientes
  const clientes: Cliente[] = [
    {
      id: 1,
      nome: "João Silva Santos",
      email: "joao@email.com",
      telefone: "(31) 99999-0001",
      endereco: "Rua das Flores, 123",
      status: "ativo",
      ultimaCompra: "15/08/2024",
      totalCompras: 2450.00
    },
    {
      id: 2,
      nome: "Maria Oliveira",
      email: "maria@email.com",
      telefone: "(31) 99999-0002",
      endereco: "Av. Central, 456",
      status: "ativo",
      ultimaCompra: "12/08/2024",
      totalCompras: 1890.00
    },
    {
      id: 3,
      nome: "Pedro Costa",
      email: "pedro@email.com",
      telefone: "(31) 99999-0003",
      endereco: "Rua São João, 789",
      status: "inativo",
      ultimaCompra: "05/07/2024",
      totalCompras: 980.00
    }
  ];

  // Filtrar clientes com base no termo de busca
  const filteredClientes = clientes.filter(cliente => 
    cliente.nome.toLowerCase().includes(searchTerm.toLowerCase()) || 
    cliente.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex flex-col h-screen">
      <Header />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6 bg-gray-50 overflow-auto">
          <div className="flex flex-col">
            {/* Cabeçalho da página */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold">Gestão de Clientes</h1>
              <p className="text-gray-600">
                Gerencie os dados dos seus clientes, incluindo edição, adição e remoção de registros.
              </p>
            </div>

            {/* Barra de pesquisa e botão novo cliente */}
            <div className="flex justify-between items-center mb-6">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Buscar por nome ou email..."
                  className="pl-10 pr-4 py-2 border border-gray-300 rounded-md w-96 focus:outline-none focus:ring-2 focus:ring-primary"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
                <Search className="absolute left-3 top-2.5 text-gray-400" size={18} />
              </div>
              <Link href="/comercio/clientes/novo">
                <Button 
                  variant="primary" 
                  className="flex items-center"
                  icon={<Plus size={18} />}
                >
                  Novo Cliente
                </Button>
              </Link>
            </div>

            {/* Lista de Clientes */}
            <div className="bg-white rounded-lg shadow-sm border">
              <div className="p-4 border-b">
                <h2 className="text-lg font-semibold">Lista de Clientes ({filteredClientes.length})</h2>
              </div>

              {/* Cabeçalhos da tabela */}
              <div className="grid grid-cols-12 gap-4 p-4 border-b bg-gray-50 font-medium">
                <div className="col-span-3">Cliente</div>
                <div className="col-span-2">Contato</div>
                <div className="col-span-3">Endereço</div>
                <div className="col-span-1">Status</div>
                <div className="col-span-1">Última Compra</div>
                <div className="col-span-1">Total Compras</div>
                <div className="col-span-1 text-right">Ações</div>
              </div>

              {/* Linhas de clientes */}
              {filteredClientes.length > 0 ? (
                filteredClientes.map(cliente => (
                  <div key={cliente.id} className="grid grid-cols-12 gap-4 p-4 border-b items-center hover:bg-gray-50">
                    <div className="col-span-3">
                      <div className="font-medium">{cliente.nome}</div>
                      <div className="text-sm text-gray-500">{cliente.email}</div>
                    </div>
                    <div className="col-span-2">
                      {cliente.telefone}
                    </div>
                    <div className="col-span-3">
                      {cliente.endereco}
                    </div>
                    <div className="col-span-1">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        cliente.status === "ativo" ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
                      }`}>
                        {cliente.status === "ativo" ? "ativo" : "inativo"}
                      </span>
                    </div>
                    <div className="col-span-1">
                      {cliente.ultimaCompra}
                    </div>
                    <div className="col-span-1">
                      <span className="font-medium">
                        R$ {cliente.totalCompras.toFixed(2).replace(".", ",")}
                      </span>
                    </div>
                    <div className="col-span-1 flex justify-end space-x-2">
                      <Link href={`/comercio/clientes/editar/${cliente.id}`}>
                        <button 
                          className="p-1 text-blue-600 hover:bg-blue-50 rounded-md"
                          aria-label="Editar cliente"
                        >
                          <Edit size={18} />
                        </button>
                      </Link>
                      <button 
                        className="p-1 text-red-600 hover:bg-red-50 rounded-md"
                        aria-label="Excluir cliente"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-8 text-center text-gray-500">
                  <div className="flex justify-center mb-3">
                    <User size={48} className="text-gray-300" />
                  </div>
                  <p className="text-lg">Nenhum cliente encontrado</p>
                  <p className="text-sm">Tente refinar sua busca ou adicione um novo cliente</p>
                </div>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
