"use client";

import { useEffect, useState } from "react";
import type { ClientSelectionInfo } from "@/types/clientType";
import { normalize } from "@/utils/textSearch";

function getSuggestions(
  clients: ClientSelectionInfo[],
  query: string,
  limit = 8,
): ClientSelectionInfo[] {
  const normQuery = normalize(query);
  return clients
    .filter((client) => normalize(client.clientName).includes(normQuery))
    .sort((a, b) => a.clientName.localeCompare(b.clientName))
    .slice(0, limit);
}

interface ClientAutocompleteFieldProps {
  clients: ClientSelectionInfo[];
  value: number | null;
  onSelect: (clientId: number | null, clientName: string) => void;
  /** Pré-preenche a busca (ex.: nome lido pela OCR) sem já resolver pra um cliente selecionado. */
  initialQuery?: string;
  disabled?: boolean;
}

/**
 * Campo de texto com autocomplete de cliente (mesma busca por nome do `ClientSelector`), pra
 * ligar a revisão de uma nota extraída a um cliente real do cadastro em vez de deixar o nome lido
 * (frequentemente incompleto/errado na OCR) como texto solto sem vínculo.
 */
export default function ClientAutocompleteField({
  clients,
  value,
  onSelect,
  initialQuery,
  disabled,
}: ClientAutocompleteFieldProps) {
  const selected = clients.find((client) => client.clientId === value);
  const [query, setQuery] = useState(
    selected?.clientName ?? initialQuery ?? "",
  );
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);

  // Mesmo motivo do ProductAutocompleteField: o catálogo de clientes pode chegar depois do
  // primeiro render, então resincroniza o texto exibido quando isso acontecer.
  useEffect(() => {
    const client = clients.find((c) => c.clientId === value);
    if (client) setQuery(client.clientName);
  }, [value, clients]);

  const suggestions = open ? getSuggestions(clients, query) : [];

  const handleChange = (text: string) => {
    setQuery(text);
    setOpen(true);
    setHighlighted(0);
    if (value !== null) onSelect(null, text);
  };

  const selectClient = (client: ClientSelectionInfo) => {
    onSelect(client.clientId, client.clientName);
    setQuery(client.clientName);
    setOpen(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!open || suggestions.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlighted((h) => Math.min(h + 1, suggestions.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const candidate = suggestions[highlighted] ?? suggestions[0];
      if (candidate) selectClient(candidate);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  };

  return (
    <div className="relative w-full">
      <input
        type="text"
        value={query}
        disabled={disabled}
        onChange={(e) => handleChange(e.target.value)}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={handleKeyDown}
        placeholder="Digite o nome do cliente"
        autoComplete="off"
        className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500"
      />
      {open && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 w-full max-h-56 overflow-y-auto bg-white border border-gray-200 rounded-lg shadow-lg">
          {suggestions.map((client, index) => (
            <li key={client.clientId}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => selectClient(client)}
                className={`w-full text-left px-3 py-2 text-sm truncate ${
                  index === highlighted ? "bg-green-50" : "hover:bg-gray-50"
                }`}
              >
                {client.clientName}
              </button>
            </li>
          ))}
        </ul>
      )}
      {open && query.trim() !== "" && suggestions.length === 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg px-3 py-2 text-sm text-gray-500">
          Nenhum cliente encontrado
        </div>
      )}
    </div>
  );
}
