"use client";

import {
  Calendar,
  DollarSign,
  Edit,
  Mail,
  MapPin,
  Phone,
  Trash2,
  User,
} from "lucide-react";
import Link from "next/link";

interface ClientCardProps {
  id: number;
  nome: string;
  apelido?: string;
  email: string;
  telefone: string;
  endereco: string;
  status: "preco-fixo" | "preco-variavel";
  ultimaCompra?: string;
  totalCompras?: number;
  onDelete: (id: number) => void;
  onViewDetails: (id: number) => void;
  displayMode?: "list" | "grid";
}

export default function ClientCard({
  id,
  nome,
  apelido,
  email,
  telefone,
  endereco,
  status,
  ultimaCompra = "-",
  totalCompras = 0,
  onDelete,
  onViewDetails,
  displayMode = "list",
}: Readonly<ClientCardProps>) {
  const handleActivateKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onViewDetails(id);
    }
  };

  if (displayMode === "grid") {
    return (
      // biome-ignore lint/a11y/useSemanticElements: can't be a real <button> since it contains a nested Link and button (edit/delete actions)
      <div
        className="bg-white rounded-lg shadow-md border border-gray-100 overflow-hidden hover:shadow-lg transition-all duration-300 group cursor-pointer"
        onClick={() => onViewDetails(id)}
        onKeyDown={handleActivateKeyDown}
        role="button"
        tabIndex={0}
        aria-label={`Ver detalhes de ${nome}`}
      >
        <div className="p-5">
          <div className="flex justify-between items-start">
            <div className="flex items-center">
              <div className="mr-3">
                <div className="h-10 w-10 bg-green-50 text-green-600 rounded-full flex items-center justify-center group-hover:bg-green-100 transition-colors">
                  <User size={20} />
                </div>
              </div>
              <div>
                <h3 className="font-medium text-lg text-gray-800 group-hover:text-green-600 transition-colors">
                  {nome}
                  {apelido && (
                    <span className="ml-1.5 text-sm font-normal text-gray-400">
                      ({apelido})
                    </span>
                  )}
                </h3>
                <div className="mt-1 flex items-center">
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      status === "preco-fixo"
                        ? "bg-green-100 text-green-800 border border-green-200"
                        : "bg-blue-100 text-blue-800 border border-blue-200"
                    }`}
                  >
                    <DollarSign
                      size={12}
                      className={
                        status === "preco-fixo"
                          ? "mr-1 text-green-600"
                          : "mr-1 text-blue-600"
                      }
                    />
                    {status === "preco-fixo" ? "Preço Fixo" : "Preço Variável"}
                  </span>
                </div>
              </div>
            </div>
            <div className="bg-gray-50 px-3 py-2 rounded-lg">
              <div className="font-medium text-gray-800 flex items-center justify-end">
                <DollarSign
                  size={14}
                  className="text-gray-400 mr-1 flex-shrink-0"
                />
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
                <span className="truncate">
                  {email || "Sem e-mail cadastrado"}
                </span>
              </div>

              {/* Telefone - altura fixa para manter proporção */}
              <div className="h-10 flex items-center text-sm text-gray-600 bg-white p-2.5 rounded border border-gray-100">
                <Phone size={16} className="mr-2 text-gray-400" />
                <span>{telefone || "Sem telefone cadastrado"}</span>
              </div>

              {/* Endereço - altura fixa para manter proporção */}
              <div className="h-10 flex items-center text-sm text-gray-600 relative group/tooltip bg-white p-2.5 rounded border border-gray-100">
                <MapPin
                  size={16}
                  className="mr-2 flex-shrink-0 text-gray-400"
                />
                <span className="truncate">
                  {endereco || "Sem endereço cadastrado"}
                </span>
                {endereco && endereco.length > 35 && (
                  <span
                    className="text-xs text-blue-500 cursor-help ml-1"
                    title="Ver endereço completo"
                  >
                    ...
                  </span>
                )}
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
                <span className="text-xs text-gray-500 whitespace-nowrap">
                  Última compra:
                </span>
                <span className="font-medium text-gray-700 ml-1.5 truncate">
                  {ultimaCompra || "Nenhuma"}
                </span>
              </div>
            </div>
            <div className="flex space-x-1">
              <Link
                href={`/comercio/clientes/editar/${id}`}
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  type="button"
                  className="h-10 w-10 flex items-center justify-center text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-all"
                  aria-label="Editar cliente"
                >
                  <Edit size={16} />
                </button>
              </Link>

              <button
                type="button"
                className="h-10 w-10 flex items-center justify-center text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all"
                aria-label="Excluir cliente"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
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

  const statusBadge = (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap ${
        status === "preco-fixo"
          ? "bg-green-100 text-green-800 border border-green-200"
          : "bg-blue-100 text-blue-800 border border-blue-200"
      }`}
    >
      <DollarSign
        size={12}
        className={
          status === "preco-fixo" ? "mr-1 text-green-600" : "mr-1 text-blue-600"
        }
      />
      {status === "preco-fixo" ? "Preço Fixo" : "Preço Variável"}
    </span>
  );

  const editButton = (
    <Link
      href={`/comercio/clientes/editar/${id}`}
      onClick={(e) => e.stopPropagation()}
    >
      <button
        type="button"
        className="h-9 w-9 flex items-center justify-center text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-all"
        aria-label="Editar cliente"
        title="Editar cliente"
      >
        <Edit size={16} />
      </button>
    </Link>
  );

  const deleteButton = (
    <button
      type="button"
      className="h-9 w-9 flex items-center justify-center text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all"
      aria-label="Excluir cliente"
      title="Excluir cliente"
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        onDelete(id);
      }}
    >
      <Trash2 size={16} />
    </button>
  );

  return (
    // biome-ignore lint/a11y/useSemanticElements: can't be a real <button> since it contains a nested Link and button (edit/delete actions)
    <div
      className="border-b hover:bg-gray-50 transition-all duration-300 group cursor-pointer"
      onClick={() => onViewDetails(id)}
      onKeyDown={handleActivateKeyDown}
      role="button"
      tabIndex={0}
      aria-label={`Ver detalhes de ${nome}`}
    >
      <div className="md:hidden p-4 space-y-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="h-9 w-9 shrink-0 bg-green-50 text-green-600 rounded-full flex items-center justify-center">
              <User size={18} />
            </div>
            <div className="min-w-0">
              <div className="font-medium text-gray-800 truncate">
                {nome}
                {apelido && (
                  <span className="ml-1 font-normal text-gray-400">
                    ({apelido})
                  </span>
                )}
              </div>
              {telefone && (
                <div className="text-sm text-gray-500 truncate">{telefone}</div>
              )}
            </div>
          </div>
          <div className="shrink-0">{statusBadge}</div>
        </div>

        <div className="flex items-center gap-1.5 text-sm text-gray-600">
          <Mail size={14} className="text-gray-400 shrink-0" />
          <span>{email || "Não informado"}</span>
        </div>

        <div className="flex items-center gap-1.5 text-sm text-gray-600 min-w-0">
          <MapPin size={14} className="text-gray-400 shrink-0" />
          <span className="truncate">
            {endereco || "Sem endereço cadastrado"}
          </span>
        </div>

        <div className="flex items-center justify-between gap-3 pt-3 mt-1 border-t border-gray-100">
          <div className="min-w-0 text-sm">
            <div className="flex items-center gap-1.5 text-gray-500">
              <Calendar size={14} className="text-gray-400 shrink-0" />
              <span>Última compra</span>
            </div>
            <div className="font-medium text-gray-700 mt-0.5">
              {ultimaCompra || "Nenhuma"}
            </div>
          </div>
          <div className="flex items-center font-medium text-gray-800 shrink-0">
            <DollarSign size={14} className="text-gray-400 mr-0.5" />
            {totalCompras.toFixed(2).replace(".", ",")}
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-1">
          {editButton}
          {deleteButton}
        </div>
      </div>

      <div className="hidden md:grid grid-cols-[2.2fr_1.2fr_1.7fr_1.3fr_1.2fr_1fr_100px] gap-4 px-6 py-4 items-center">
        <div className="flex items-center min-w-0">
          <div className="mr-3 shrink-0">
            <div className="h-9 w-9 bg-green-50 text-green-600 rounded-full flex items-center justify-center">
              <User size={18} />
            </div>
          </div>
          <div className="min-w-0">
            <div className="font-medium text-gray-800 group-hover:text-green-600 transition-colors truncate">
              {nome}
              {apelido && (
                <span className="ml-1 font-normal text-gray-400">
                  ({apelido})
                </span>
              )}
            </div>
            {telefone && (
              <div className="text-sm text-gray-500 truncate">{telefone}</div>
            )}
          </div>
        </div>

        <div className="text-gray-700 flex items-center min-w-0">
          {email ? (
            <div className="flex items-center min-w-0">
              <Mail size={14} className="text-gray-400 mr-1.5 shrink-0" />
              <span className="truncate">{email}</span>
            </div>
          ) : (
            <span className="text-gray-400 italic text-sm">Não informado</span>
          )}
        </div>

        <div className="text-gray-700 relative group/tooltip min-w-0">
          <div className="flex items-center gap-1.5 min-w-0">
            <MapPin size={14} className="text-gray-400 shrink-0" />
            <span className="truncate">
              {endereco || "Sem endereço cadastrado"}
            </span>
          </div>
          {endereco && endereco.length > 30 && (
            <div className="hidden group-hover/tooltip:block absolute z-10 bg-gray-800 text-white p-2 rounded shadow-lg text-sm max-w-xs left-0 mt-1">
              {endereco}
            </div>
          )}
        </div>

        <div className="min-w-0">{statusBadge}</div>

        <div className="text-sm text-gray-700 flex items-center min-w-0">
          <Calendar size={14} className="text-gray-400 mr-1.5 shrink-0" />
          <span className="truncate">{ultimaCompra || "Nenhuma"}</span>
        </div>

        <div className="flex items-center font-medium text-gray-800 min-w-0">
          <DollarSign size={14} className="text-gray-400 mr-1 shrink-0" />
          <span className="truncate">
            {totalCompras.toFixed(2).replace(".", ",")}
          </span>
        </div>

        <div className="flex items-center justify-end gap-2">
          {editButton}
          {deleteButton}
        </div>
      </div>
    </div>
  );
}
