import { UserRoundSearch } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { clientService } from "@/services/clientService";
import type { ClientSelectionInfo } from "@/types/clientType";
import { normalize } from "@/utils/textSearch";

type ClientSelectorProps = {
  onClientSelect?: (client: ClientSelectionInfo) => void;
};

export default function ClientSelector({
  onClientSelect,
}: ClientSelectorProps) {
  const [clientes, setClientes] = useState<ClientSelectionInfo[]>([]);
  const [selected, setSelected] = useState<ClientSelectionInfo | null>(null);
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchClients = async () => {
      const data = await clientService.getAllClientsForSelection();
      setClientes(data);
    };
    fetchClients();
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const suggestions = clientes
    .filter((client) => {
      const normQuery = normalize(query);
      return (
        normalize(client.clientName).includes(normQuery) ||
        (client.nickname && normalize(client.nickname).includes(normQuery))
      );
    })
    .sort((a, b) => a.clientName.localeCompare(b.clientName));

  const selectClient = (client: ClientSelectionInfo) => {
    setSelected(client);
    setQuery(client.clientName);
    setOpen(false);
    onClientSelect?.(client);
  };

  const handleChange = (text: string) => {
    setQuery(text);
    setOpen(true);
    setHighlighted(0);
    if (selected) setSelected(null);
  };

  const handleFocus = () => {
    setOpen(true);
    if (selected) setQuery("");
  };

  const handleBlur = () => {
    if (!open) return;
    setQuery(selected ? selected.clientName : "");
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
      handleBlur();
    }
  };

  return (
    <div className="flex flex-col gap-4 p-6 bg-white">
      <h2 className="flex items-center gap-2 text-xl font-bold text-gray-800 mb-2">
        <UserRoundSearch className="w-6 h-6 text-green-600" />
        Selecionar Cliente
      </h2>
      <div className="flex flex-col gap-2">
        <label htmlFor="client" className="font-medium text-gray-700">
          Cliente
        </label>
        <div className="relative w-full" ref={containerRef}>
          <input
            id="client"
            type="text"
            value={query}
            onChange={(e) => handleChange(e.target.value)}
            onFocus={handleFocus}
            onKeyDown={handleKeyDown}
            placeholder="Digite o nome do cliente"
            autoComplete="off"
            className="border border-gray-300 rounded-md p-2 focus:outline-none focus:ring-2 focus:ring-green-500 transition w-full truncate"
          />
          {open && suggestions.length > 0 && (
            <ul className="absolute z-10 mt-1 w-full max-h-64 overflow-y-auto bg-white border border-gray-200 rounded-lg shadow-lg">
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
                    {client.nickname && (
                      <span className="text-gray-400">
                        {" "}
                        ({client.nickname})
                      </span>
                    )}
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
      </div>
    </div>
  );
}
