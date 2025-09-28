'use client';

import React from 'react';
import { Edit, Trash2, Mail, Phone, MapPin } from 'lucide-react';
import Link from 'next/link';

interface ClientCardProps {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  endereco: string;
  status: 'ativo' | 'inativo';
  ultimaCompra?: string;
  totalCompras?: number;
  onDelete: (id: number) => void;
  displayMode?: 'list' | 'grid';
}

export default function ClientCard({
  id,
  nome,
  email,
  telefone,
  endereco,
  status,
  ultimaCompra = '-',
  totalCompras = 0,
  onDelete,
  displayMode = 'list'
}: Readonly<ClientCardProps>) {
  
  // Grid mode card layout
  if (displayMode === 'grid') {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow">
        <div className="p-5">
          <div className="flex justify-between items-start">
            <div>
              <h3 className="font-medium text-lg text-gray-800">{nome}</h3>
              <div className="mt-1 flex items-center">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  status === "ativo" ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
                }`}>
                  {status === "ativo" ? "ativo" : "inativo"}
                </span>
              </div>
            </div>
            <div>
              <div className="font-medium text-gray-800">
                R$ {(totalCompras).toFixed(2).replace(".", ",")}
              </div>
              <div className="text-xs text-gray-500 text-right">total de compras</div>
            </div>
          </div>
          
          <div className="mt-4 space-y-2">
            {email && (
              <div className="flex items-center text-sm text-gray-600">
                <Mail size={16} className="mr-2 text-gray-400" />
                <span className="truncate">{email}</span>
              </div>
            )}
            {telefone && (
              <div className="flex items-center text-sm text-gray-600">
                <Phone size={16} className="mr-2 text-gray-400" />
                <span>{telefone}</span>
              </div>
            )}
            {endereco && (
              <div className="flex items-start text-sm text-gray-600">
                <MapPin size={16} className="mr-2 mt-0.5 flex-shrink-0 text-gray-400" />
                <span className="truncate">{endereco}</span>
              </div>
            )}
          </div>
          
          <div className="mt-5 flex justify-between items-center pt-4 border-t border-gray-100">
            <div className="text-sm text-gray-500">
              <span>Última compra: </span>
              <span className="font-medium text-gray-700">{ultimaCompra}</span>
            </div>
            <div className="flex space-x-2">
              <Link href={`/comercio/clientes/editar/${id}`}>
                <button 
                  className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                  aria-label="Editar cliente"
                >
                  <Edit size={18} />
                </button>
              </Link>
              <button 
                className="p-1.5 text-red-600 hover:bg-red-50 rounded-md transition-colors"
                aria-label="Excluir cliente"
                onClick={() => onDelete(id)}
              >
                <Trash2 size={18} />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }
  
  // Default list mode layout (for use in a table)
  return (
    <div className="grid grid-cols-12 gap-4 px-6 py-4 border-b items-center hover:bg-gray-50 transition-colors">
      <div className="col-span-3">
        <div className="font-medium text-gray-800">{nome}</div>
        <div className="text-sm text-gray-500 mt-0.5">{email}</div>
      </div>
      <div className="col-span-2 text-gray-700">
        {telefone}
      </div>
      <div className="col-span-3 text-gray-700 truncate">
        {endereco}
      </div>
      <div className="col-span-1">
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
          status === "ativo" 
            ? "bg-green-100 text-green-800 border border-green-200" 
            : "bg-gray-100 text-gray-800 border border-gray-200"
        }`}>
          {status === "ativo" ? "ativo" : "inativo"}
        </span>
      </div>
      <div className="col-span-1 text-gray-700">
        {ultimaCompra}
      </div>
      <div className="col-span-1">
        <span className="font-medium text-gray-800">
          R$ {(totalCompras).toFixed(2).replace(".", ",")}
        </span>
      </div>
      <div className="col-span-1 flex justify-end space-x-2">
        <Link href={`/comercio/clientes/editar/${id}`}>
          <button 
            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
            aria-label="Editar cliente"
          >
            <Edit size={18} />
          </button>
        </Link>
        <button 
          className="p-1.5 text-red-600 hover:bg-red-50 rounded-md transition-colors"
          aria-label="Excluir cliente"
          onClick={() => onDelete(id)}
        >
          <Trash2 size={18} />
        </button>
      </div>
    </div>
  );
}
