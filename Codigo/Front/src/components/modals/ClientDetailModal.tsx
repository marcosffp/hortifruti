"use client";

import {
  Building2,
  Calendar,
  DollarSign,
  FileText,
  Mail,
  MapPin,
  Phone,
  Receipt,
  User,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { clientService } from "@/services/clientService";
import type { ClientResponse } from "@/types/clientType";

interface ClientDetailModalProps {
  clientId: number | null;
  onClose: () => void;
}

function formatDate(dateString: string | null): string {
  if (!dateString) return "Nenhuma compra registrada";
  const [year, month, day] = dateString.split("T")[0].split("-");
  return `${day}/${month}/${year}`;
}

function formatCurrency(value: number | null): string {
  if (value === null) return "R$ 0,00";
  return `R$ ${value.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;
}

interface CopyToast {
  x: number;
  y: number;
}

function CopyableField({
  icon,
  label,
  value,
  copyValue,
  fullWidth,
  onCopy,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
  copyValue?: string;
  fullWidth?: boolean;
  onCopy: (text: string, e: React.MouseEvent) => void;
}) {
  const copyable = Boolean(copyValue);

  return (
    <button
      type="button"
      disabled={!copyable}
      onClick={(e) => copyValue && onCopy(copyValue, e)}
      title={copyable ? "Clique para copiar" : undefined}
      className={`group flex items-start gap-3 rounded-xl p-3 text-left transition-colors ${
        copyable ? "cursor-pointer hover:bg-green-50" : "cursor-default"
      } ${fullWidth ? "col-span-1 sm:col-span-2" : ""}`}
    >
      <div
        className={`mt-0.5 text-gray-400 transition-colors ${copyable ? "group-hover:text-green-600" : ""}`}
      >
        {icon}
      </div>
      <div className="flex flex-col min-w-0">
        <span
          className={`text-xs text-gray-500 transition-colors ${copyable ? "group-hover:text-green-600" : ""}`}
        >
          {label}
        </span>
        <span
          className={`text-sm font-medium text-gray-800 break-words transition-colors ${copyable ? "group-hover:text-green-700" : ""}`}
        >
          {value}
        </span>
      </div>
    </button>
  );
}

export default function ClientDetailModal({
  clientId,
  onClose,
}: Readonly<ClientDetailModalProps>) {
  const [client, setClient] = useState<ClientResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copyToast, setCopyToast] = useState<CopyToast | null>(null);

  useEffect(() => {
    if (clientId === null) {
      setClient(null);
      return;
    }

    const fetchClient = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await clientService.getClientById(clientId);
        setClient(data);
      } catch {
        setError("Não foi possível carregar os dados do cliente");
      } finally {
        setIsLoading(false);
      }
    };

    fetchClient();
  }, [clientId]);

  useEffect(() => {
    if (clientId === null) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [clientId, onClose]);

  useEffect(() => {
    if (!copyToast) return;
    const timeout = setTimeout(() => setCopyToast(null), 1200);
    return () => clearTimeout(timeout);
  }, [copyToast]);

  if (clientId === null) return null;

  const handleCopy = (text: string, e: React.MouseEvent) => {
    navigator.clipboard.writeText(text);
    setCopyToast({ x: e.clientX, y: e.clientY });
  };

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: click-outside-to-close backdrop; closing is also reachable via the Escape key and the Fechar button
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4"
      role="presentation"
      onClick={onClose}
    >
      {/* biome-ignore lint/a11y/noStaticElementInteractions: only stops the backdrop click from bubbling, not an interactive control itself */}
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: only stops the backdrop click from bubbling, not an interactive control itself */}
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[88vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-5 bg-gradient-to-r from-green-600 to-green-700">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 bg-white/15 text-white rounded-full flex items-center justify-center">
              <User size={20} />
            </div>
            <h2 className="text-lg font-semibold text-white">
              Dados do Cliente
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="text-white/80 hover:text-white transition-colors"
          >
            <X size={22} />
          </button>
        </div>

        <div className="px-4 py-4 overflow-y-auto">
          {isLoading && (
            <div className="py-10 text-center text-gray-500">
              Carregando dados do cliente...
            </div>
          )}

          {!isLoading && error && (
            <div className="py-10 text-center text-red-600">{error}</div>
          )}

          {!isLoading && !error && client && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1">
              <CopyableField
                icon={<User size={16} />}
                label="Nome"
                value={client.clientName}
                copyValue={client.clientName}
                fullWidth
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<MapPin size={16} />}
                label="Endereço"
                value={client.address || "Não informado"}
                copyValue={client.address || undefined}
                fullWidth
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<FileText size={16} />}
                label="CPF/CNPJ"
                value={client.document || "Não informado"}
                copyValue={client.document || undefined}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Mail size={16} />}
                label="E-mail"
                value={client.email || "Não informado"}
                copyValue={client.email || undefined}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Phone size={16} />}
                label="Telefone"
                value={client.phoneNumber || "Não informado"}
                copyValue={client.phoneNumber || undefined}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<DollarSign size={16} />}
                label="Tipo de Preço"
                value={client.variablePrice ? "Preço Variável" : "Preço Fixo"}
                copyValue={
                  client.variablePrice ? "Preço Variável" : "Preço Fixo"
                }
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Building2 size={16} />}
                label="Inscrição Estadual"
                value={client.stateRegistration || "Não informado"}
                copyValue={client.stateRegistration || undefined}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Receipt size={16} />}
                label="Indicador de IE"
                value={client.stateIndicator ?? "Não informado"}
                copyValue={
                  client.stateIndicator !== null
                    ? String(client.stateIndicator)
                    : undefined
                }
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<FileText size={16} />}
                label="Código CIDE"
                value={client.cideCode || "Não informado"}
                copyValue={client.cideCode || undefined}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Receipt size={16} />}
                label="Somente Boleto"
                value={client.onlyBillet ? "Sim" : "Não"}
                copyValue={client.onlyBillet ? "Sim" : "Não"}
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<Calendar size={16} />}
                label="Última Compra"
                value={formatDate(client.lastPurchaseDate)}
                copyValue={
                  client.lastPurchaseDate
                    ? formatDate(client.lastPurchaseDate)
                    : undefined
                }
                onCopy={handleCopy}
              />
              <CopyableField
                icon={<DollarSign size={16} />}
                label="Valor Total em Compras"
                value={formatCurrency(client.totalPurchaseValue)}
                copyValue={formatCurrency(client.totalPurchaseValue)}
                onCopy={handleCopy}
              />
            </div>
          )}
        </div>

        <div className="flex justify-end px-6 py-4 bg-gray-50 border-t border-gray-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-white border border-gray-200 hover:bg-gray-100 text-gray-700 transition-colors"
          >
            Fechar
          </button>
        </div>
      </div>

      {copyToast && (
        <div
          className="fixed z-[60] pointer-events-none bg-green-600 text-white text-xs font-medium px-2.5 py-1 rounded-md shadow-lg animate-[fadeIn_0.1s_ease-out]"
          style={{
            left: copyToast.x,
            top: copyToast.y,
            transform: "translate(-50%, -140%)",
          }}
        >
          Copiado!
        </div>
      )}
    </div>
  );
}
