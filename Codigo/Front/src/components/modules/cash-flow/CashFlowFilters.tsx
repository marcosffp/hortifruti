import { Filter } from "lucide-react";
import type { Dispatch, SetStateAction } from "react";
import Card from "@/components/ui/Card";

interface CashFlowFiltersProps {
  draftMonth: number;
  draftYear: number;
  draftStartDate: string;
  draftEndDate: string;
  setDraftMonth: Dispatch<SetStateAction<number>>;
  setDraftYear: Dispatch<SetStateAction<number>>;
  setDraftStartDate: Dispatch<SetStateAction<string>>;
  setDraftEndDate: Dispatch<SetStateAction<string>>;
  hasPendingChanges: boolean;
  isLoading: boolean;
  onApplyFilters: () => void;
}

export default function CashFlowFilters({
  draftMonth,
  draftYear,
  draftStartDate,
  draftEndDate,
  setDraftMonth,
  setDraftYear,
  setDraftStartDate,
  setDraftEndDate,
  hasPendingChanges,
  isLoading,
  onApplyFilters,
}: CashFlowFiltersProps) {
  return (
    <Card title="Filtros">
      <p className="text-sm text-gray-500 mb-4">
        Ajuste os campos abaixo e clique em <strong>Aplicar Filtro</strong> para
        atualizar os gráficos. O intervalo de <strong>Data Inicial</strong> e{" "}
        <strong>Data Final</strong> aplicado aqui também é usado pelo botão{" "}
        <strong>Baixar Relatório → Relatório por Período</strong>, no card
        acima.
      </p>
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
        <div>
          <label
            htmlFor="cashflow-mes"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Mês
          </label>
          <select
            id="cashflow-mes"
            value={draftMonth}
            onChange={(e) => setDraftMonth(Number(e.target.value))}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            {Array.from({ length: 12 }, (_, i) => (
              // biome-ignore lint/suspicious/noArrayIndexKey: i+1 is the month number (1-12), a stable domain value not a positional index
              <option key={i + 1} value={i + 1}>
                {new Date(0, i)
                  .toLocaleString("pt-BR", { month: "long" })
                  .replace(/^\w/, (c) => c.toUpperCase())}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label
            htmlFor="cashflow-ano"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Ano
          </label>
          <input
            id="cashflow-ano"
            type="number"
            value={draftYear}
            onChange={(e) => setDraftYear(Number(e.target.value))}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <div>
          <label
            htmlFor="cashflow-data-inicial"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Data Inicial
          </label>
          <input
            id="cashflow-data-inicial"
            type="date"
            value={draftStartDate}
            onChange={(e) => setDraftStartDate(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <div>
          <label
            htmlFor="cashflow-data-final"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Data Final
          </label>
          <input
            id="cashflow-data-final"
            type="date"
            value={draftEndDate}
            onChange={(e) => setDraftEndDate(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={onApplyFilters}
          disabled={isLoading || !hasPendingChanges}
          className="inline-flex items-center gap-2 px-4 py-2 bg-[var(--primary)] text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <Filter className="w-4 h-4" />
          Aplicar Filtro
        </button>
        {hasPendingChanges && (
          <span className="text-sm text-amber-600">
            Você tem alterações não aplicadas.
          </span>
        )}
      </div>
    </Card>
  );
}
