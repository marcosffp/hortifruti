import ClientAutocompleteField from "@/components/ui/ClientAutocompleteField";
import type { ClientSelectionInfo } from "@/types/clientType";
import { CONFIANCA_BADGE } from "./types";

interface NotaClienteDataFieldsProps {
  clients: ClientSelectionInfo[];
  clienteId: number | null;
  clienteNome: string;
  clienteLido: string | null;
  clienteConfianca: "alta" | "media" | "baixa" | null;
  onSelectCliente: (id: number | null, nome: string) => void;
  purchaseDate: string;
  onChangePurchaseDate: (date: string) => void;
  useTodayDate: boolean;
  onToggleUseTodayDate: (checked: boolean) => void;
}

export default function NotaClienteDataFields({
  clients,
  clienteId,
  clienteNome,
  clienteLido,
  clienteConfianca,
  onSelectCliente,
  purchaseDate,
  onChangePurchaseDate,
  useTodayDate,
  onToggleUseTodayDate,
}: NotaClienteDataFieldsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
      <div>
        <p className="flex items-center gap-2 text-xs font-medium text-gray-500 uppercase mb-1">
          <span>Cliente {clienteLido ? `(lido: "${clienteLido}")` : ""}</span>
          {clienteConfianca && (
            <span
              className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${CONFIANCA_BADGE[clienteConfianca]}`}
            >
              {clienteConfianca}
            </span>
          )}
        </p>
        <ClientAutocompleteField
          clients={clients}
          value={clienteId}
          onSelect={onSelectCliente}
          initialQuery={clienteNome}
        />
        {!clienteId && clienteNome.trim() !== "" && (
          <p className="text-xs text-amber-600 mt-1">
            Nenhum cliente do cadastro selecionado ainda.
          </p>
        )}
      </div>
      <div>
        <label
          htmlFor="revisao-data-compra"
          className="block text-xs font-medium text-gray-500 uppercase mb-1"
        >
          Data da compra (usada ao lançar)
        </label>
        <input
          id="revisao-data-compra"
          type="date"
          value={purchaseDate}
          disabled={useTodayDate}
          onChange={(e) => onChangePurchaseDate(e.target.value)}
          className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500 disabled:bg-gray-100 disabled:text-gray-500"
        />
        <label className="flex items-center gap-1.5 mt-1.5 text-xs text-gray-600">
          <input
            type="checkbox"
            checked={useTodayDate}
            onChange={(e) => onToggleUseTodayDate(e.target.checked)}
            className="rounded border-gray-300 text-green-600 focus:ring-green-500"
          />
          Usar data de hoje (horário de Brasília)
        </label>
      </div>
    </div>
  );
}
