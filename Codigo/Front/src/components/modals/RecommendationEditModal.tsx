"use client";

import { Edit } from "lucide-react";
import type {
  ProductRequest,
  TemperatureCategory,
} from "@/services/productService";

interface RecommendationEditModalProps {
  data: ProductRequest;
  temperatureCategorias: { value: string; label: string }[];
  error: string;
  onNameChange: (name: string) => void;
  onCategoryChange: (category: TemperatureCategory) => void;
  onMonthsChange: (
    value: string,
    field: "peakSalesMonths" | "lowSalesMonths",
  ) => void;
  onCancel: () => void;
  onSave: () => void;
}

export default function RecommendationEditModal({
  data,
  temperatureCategorias,
  error,
  onNameChange,
  onCategoryChange,
  onMonthsChange,
  onCancel,
  onSave,
}: RecommendationEditModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center">
          <Edit className="mr-2 text-blue-600" size={20} />
          Editar Produto
        </h3>

        <div className="flex flex-col gap-3 mb-6">
          <label
            htmlFor="rec-edit-nome"
            className="text-sm text-[var(--neutral-700)]"
          >
            Nome do Produto*
          </label>
          <input
            id="rec-edit-nome"
            className="border border-[var(--neutral-300)] rounded px-3 py-2"
            placeholder="Ex: Tomate"
            value={data.name}
            onChange={(e) => onNameChange(e.target.value)}
          />

          <label
            htmlFor="rec-edit-categoria"
            className="text-sm text-[var(--neutral-700)]"
          >
            Categoria de Temperatura*
          </label>
          <select
            id="rec-edit-categoria"
            className="border border-[var(--neutral-300)] rounded px-3 py-2"
            value={data.temperatureCategory}
            onChange={(e) =>
              onCategoryChange(e.target.value as TemperatureCategory)
            }
          >
            {temperatureCategorias.map((cat) => (
              <option key={cat.value} value={cat.value}>
                {cat.label}
              </option>
            ))}
          </select>

          <label
            htmlFor="rec-edit-pico"
            className="text-sm text-[var(--neutral-700)]"
          >
            Meses de Pico de Vendas
          </label>
          <input
            id="rec-edit-pico"
            className="border border-[var(--neutral-300)] rounded px-3 py-2"
            placeholder="Ex: 1,2,3 (separados por vírgula)"
            value={data.peakSalesMonths.join(",")}
            onChange={(e) => onMonthsChange(e.target.value, "peakSalesMonths")}
          />

          <label
            htmlFor="rec-edit-baixa"
            className="text-sm text-[var(--neutral-700)]"
          >
            Meses de Baixa nas Vendas
          </label>
          <input
            id="rec-edit-baixa"
            className="border border-[var(--neutral-300)] rounded px-3 py-2"
            placeholder="Ex: 7,8,9 (separados por vírgula)"
            value={data.lowSalesMonths.join(",")}
            onChange={(e) => onMonthsChange(e.target.value, "lowSalesMonths")}
          />

          {error && <span className="text-[var(--secondary)]">{error}</span>}

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={onSave}
              className="px-4 py-2 bg-[var(--primary)] hover:bg-[var(--primary-dark)] text-white rounded-md"
            >
              Salvar Alterações
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
