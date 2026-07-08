import { useState } from "react";

interface WildcardBilletModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (number: string, value: number, dueDate?: string) => void;
}

const formatCentsToBRL = (cents: number) =>
  (cents / 100).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function WildcardBilletModal({ open, onClose, onConfirm }: WildcardBilletModalProps) {
  const [number, setNumber] = useState("113");
  const [valueCents, setValueCents] = useState(0);
  const [dueDate, setDueDate] = useState("");

  if (!open) return null;

  const isValueValid = valueCents > 0;

  const handleValueChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const digitsOnly = e.target.value.replace(/\D/g, "");
    setValueCents(digitsOnly ? parseInt(digitsOnly, 10) : 0);
  };

  const handleConfirm = () => {
    if (!number.trim() || !isValueValid) return;

    const dueDateValue = dueDate.trim() ? dueDate.trim() : undefined;
    onConfirm(number.trim(), valueCents / 100, dueDateValue);

    setNumber("113");
    setValueCents(0);
    setDueDate("");
  };

  const handleClose = () => {
    setNumber("113");
    setValueCents(0);
    setDueDate("");
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
        <h2 className="text-lg font-semibold mb-4">Gerar Boleto</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Número identificador do cliente *
            </label>
            <input
              type="text"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="Digite o número identificador"
              value={number}
              onChange={e => setNumber(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Valor (R$) *
            </label>
            <input
              type="text"
              inputMode="numeric"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="0,00"
              value={formatCentsToBRL(valueCents)}
              onChange={handleValueChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data de vencimento (opcional)
            </label>
            <input
              type="date"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              value={dueDate}
              onChange={e => setDueDate(e.target.value)}
            />
            <p className="text-xs text-gray-500 mt-1">
              Se não informada, será usada a data padrão calculada
            </p>
          </div>
        </div>

        <div className="flex justify-end gap-2 mt-6">
          <button
            className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200"
            onClick={handleClose}
          >
            Cancelar
          </button>
          <button
            className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
            onClick={handleConfirm}
            disabled={!number.trim() || !isValueValid}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}
