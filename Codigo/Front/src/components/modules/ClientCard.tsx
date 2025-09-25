"use client";

import React from "react";
import { 
  Calendar,
  CircleCheck,
  DollarSign,
  Edit, 
  Eye,
  Mail, 
  MapPin, 
  Phone, 
  Trash2, 
  User 
} from "lucide-react";
import Link from "next/link";

interface ClientCardProps {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  endereco: string;
  status: "ativo" | "inativo";
  ultimaCompra?: string;
  totalCompras?: number;
  onDelete: (id: number) => void;
  displayMode?: "list" | "grid";
}

export default function ClientCard({
  id,
  nome,
  email,
  telefone,
  endereco,
  status,
  ultimaCompra = "-",
  totalCompras = 0,
  onDelete,
  displayMode = "list",
}: Readonly<ClientCardProps>) {
  // Grid mode card layout
  if (displayMode === "grid") {
    return (
      <div className="bg-white rounded-lg shadow-md border border-gray-100 overflow-hidden hover:shadow-lg transition-all duration-300 group">
        <div className="p-5">
          <div className="flex justify-between items-start">
            <div className="flex items-center">
              <div className="mr-3">
                <div className="h-10 w-10 bg-green-50 text-green-600 rounded-full flex items-center justify-center group-hover:bg-green-100 transition-colors">
                  <User size={20} />
                </div>
              </div>
              <div>
                <h3 className="font-medium text-lg text-gray-800 group-hover:text-green-600 transition-colors">{nome}</h3>
                <div className="mt-1 flex items-center">
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      status === "ativo"
                        ? "bg-green-100 text-green-800 border border-green-200"
                        : "bg-gray-100 text-gray-800 border border-gray-200"
                    }`}
                  >
                    <CircleCheck size={12} className={status === "ativo" ? "mr-1 text-green-600" : "mr-1 text-gray-500"} />
                    {status === "ativo" ? "ativo" : "inativo"}
                  </span>
                </div>
              </div>
            </div>
            <div className="bg-gray-50 px-3 py-2 rounded-lg">
              <div className="font-medium text-gray-800 flex items-center justify-end">
                <DollarSign size={14} className="text-gray-400 mr-1 flex-shrink-0" />
                <span>{totalCompras.toFixed(2).replace(".", ",")}</span>
              </div>
              <div className="text-xs text-gray-500 text-right mt-0.5">
                total de compras
              </div>
            </div>
          </div>

          <div className="mt-5 pt-4 border-t border-gray-100">
            <div className="grid grid-cols-1 gap-3">
              {/* Email - altura fixa para manter proporção */}
              <div className="h-10 flex items-center text-sm text-gray-600 bg-white p-2.5 rounded border border-gray-100">
                <Mail size={16} className="mr-2 text-gray-400" />
                <span className="truncate">{email || "Sem e-mail cadastrado"}</span>
              </div>
              
              {/* Telefone - altura fixa para manter proporção */}
              <div className="h-10 flex items-center text-sm text-gray-600 bg-white p-2.5 rounded border border-gray-100">
                <Phone size={16} className="mr-2 text-gray-400" />
                <span>{telefone || "Sem telefone cadastrado"}</span>
              </div>
              
              {/* Endereço - altura fixa para manter proporção */}
              <div className="h-10 flex items-center text-sm text-gray-600 relative group/tooltip bg-white p-2.5 rounded border border-gray-100">
                <MapPin size={16} className="mr-2 flex-shrink-0 text-gray-400" />
                <span className="truncate">{endereco || "Sem endereço cadastrado"}</span>
                {endereco && endereco.length > 35 && (
                  <span className="text-xs text-blue-500 cursor-help ml-1" title="Ver endereço completo">...</span>
                )}
                {/* Tooltip para endereço completo no modo grid */}
                {endereco && endereco.length > 0 && (
                  <div className="hidden group-hover/tooltip:block absolute z-10 bg-gray-800 text-white p-2 rounded shadow-lg text-sm left-0 mt-8 max-w-[280px]">
                    {endereco}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="mt-5 flex justify-between items-center pt-4 border-t border-gray-100">
            <div className="h-10 flex items-center text-sm bg-white p-2.5 rounded border border-gray-100 w-1/2">
              <Calendar size={16} className="mr-2 text-gray-400" />
              <div className="flex items-center truncate">
                <span className="text-xs text-gray-500 whitespace-nowrap">Última compra:</span>
                <span className="font-medium text-gray-700 ml-1.5 truncate">{ultimaCompra || 'Nenhuma'}</span>
              </div>
            </div>
            <div className="flex space-x-1">
              <Link href={`/comercio/clientes/editar/${id}`}>
                <button
                  className="h-10 w-10 flex items-center justify-center text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-all"
                  aria-label="Editar cliente"
                >
                  <Edit size={16} />
                </button>
              </Link>
            
              <button
                className="h-10 w-10 flex items-center justify-center text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all"
                aria-label="Excluir cliente"
                onClick={(e) => {
                  e.preventDefault();
                  onDelete(id);
                }}
              >
                <Trash2 size={16} />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Default list mode layout (for use in a table)
  return (
    <div className="grid grid-cols-12 gap-4 px-6 py-5 border-b items-center hover:bg-gray-50 transition-all duration-300 group">
      <div className="col-span-3 flex items-start">
        <div className="mr-3 mt-1">
          <div className="h-8 w-8 bg-green-50 text-green-600 rounded-full flex items-center justify-center">
            <User size={18} />
          </div>
        </div>
        <div>
          <div className="font-medium text-gray-800 group-hover:text-green-600 transition-colors">{nome}</div>
          <div className="text-sm text-gray-500 mt-0.5">{email || "Sem e-mail cadastrado"}</div>
        </div>
      </div>
      <div className="col-span-2 text-gray-700 flex items-center">
        {telefone ? (
          <div className="flex items-center">
            <Phone size={14} className="text-gray-400 mr-1.5 flex-shrink-0" />
            <span>{telefone}</span>
          </div>
        ) : (
          <span className="text-gray-400 italic text-sm">Não informado</span>
        )}
      </div>
      <div className="col-span-3 text-gray-700 relative group/tooltip">
        <div className="flex items-center gap-1.5">
          <MapPin size={14} className="text-gray-400 flex-shrink-0" />
          <div className="truncate max-w-[250px]">{endereco}</div>
          {endereco.length > 30 && (
            <span className="text-xs text-blue-500 cursor-help" title="Ver endereço completo">...</span>
          )}
        </div>
        {/* Tooltip para exibir o endereço completo */}
        <div className="hidden group-hover/tooltip:block absolute z-10 bg-gray-800 text-white p-2 rounded shadow-lg text-sm max-w-xs left-0 mt-1">
          {endereco}
        </div>
      </div>
      <div className="col-span-1">
        <span
          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
            status === "ativo"
              ? "bg-green-100 text-green-800 border border-green-200"
              : "bg-gray-100 text-gray-800 border border-gray-200"
          }`}
        >
          <CircleCheck size={12} className={status === "ativo" ? "mr-1 text-green-600" : "mr-1 text-gray-500"} />
          {status === "ativo" ? "ativo" : "inativo"}
        </span>
      </div>
      <div className="col-span-1 text-gray-700">
        <div className="flex items-center">
          <Calendar size={14} className="text-gray-400 mr-1.5 flex-shrink-0" />
          <div className="flex flex-col">
            <span className="text-xs text-gray-500 leading-tight">Última compra</span>
            <span className="font-medium text-gray-700 text-sm leading-tight">{ultimaCompra || 'Nenhuma'}</span>
          </div>
        </div>
      </div>
      <div className="col-span-1 flex items-center">
        <DollarSign size={14} className="text-gray-400 mr-1 flex-shrink-0" />
        <span className="font-medium text-gray-800">
          {totalCompras.toFixed(2).replace(".", ",")}
        </span>
      </div>
      <div className="col-span-1 flex justify-end space-x-1">
          <Link href={`/comercio/clientes/editar/${id}`}>
            <button
              className="h-9 w-9 flex items-center justify-center text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-all"
              aria-label="Editar cliente"
              title="Editar cliente"
            >
              <Edit size={16} />
            </button>
          </Link>
          <button
            className="h-9 w-9 flex items-center justify-center text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all"
            aria-label="Excluir cliente"
            title="Excluir cliente"
            onClick={(e) => {
              e.preventDefault();
              onDelete(id);
            }}
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

  );
}
