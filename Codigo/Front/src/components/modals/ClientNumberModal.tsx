import { useState } from "react";

interface ClientNumberModalProps {
  open: boolean;
  onClose: () => void;
  clientName?: string | null;
  onConfirm: (
    number: string,
    dueDate?: string,
    useStandardFileName?: boolean,
  ) => void;
}

export default function ClientNumberModal({
  open,
  onClose,
  clientName,
  onConfirm,
}: ClientNumberModalProps) {
  const [number, setNumber] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [useStandardFileName, setUseStandardFileName] = useState(false);

  if (!open) return null;

  const handleConfirm = () => {
    if (number.trim()) {
      const dueDateValue = dueDate.trim() ? dueDate.trim() : undefined;
      onConfirm(number.trim(), dueDateValue, useStandardFileName);

      setNumber("");
      setDueDate("");
      setUseStandardFileName(false);
    }
  };

  const handleClose = () => {
    setNumber("");
    setDueDate("");
    setUseStandardFileName(false);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
        <h2 className="text-lg font-semibold mb-4">Gerar Boleto</h2>

        <div className="space-y-4">
          <div>
            <label
              htmlFor="client-number-id"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Número identificador do cliente *
            </label>
            <input
              id="client-number-id"
              type="text"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="Digite o número identificador"
              value={number}
              onChange={(e) => setNumber(e.target.value)}
            />
          </div>

          <div>
            <label
              htmlFor="client-number-due-date"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Data de vencimento (opcional)
            </label>
            <input
              id="client-number-due-date"
              type="date"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
            />
            <p className="text-xs text-gray-500 mt-1">
              Se não informada, será usada a data padrão calculada
            </p>
          </div>

          <div className="flex items-start gap-2">
            <input
              id="client-number-standard-filename"
              type="checkbox"
              className="mt-1 h-4 w-4 rounded border-gray-300 text-green-600 focus:ring-green-500"
              checked={useStandardFileName}
              onChange={(e) => setUseStandardFileName(e.target.checked)}
              disabled={!clientName}
            />
            <label
              htmlFor="client-number-standard-filename"
              className="text-sm text-gray-700"
            >
              Numeração padrão
              <p className="text-xs text-gray-500">
                {clientName
                  ? `Nomeia o arquivo do boleto como BOLETO_${clientName.trim().split(/\s+/)[0].toUpperCase()}.pdf`
                  : "Indisponível: cliente sem nome cadastrado"}
              </p>
            </label>
          </div>
        </div>

        <div className="flex justify-end gap-2 mt-6">
          <button
            type="button"
            className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200"
            onClick={handleClose}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
            onClick={handleConfirm}
            disabled={!number.trim()}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}
