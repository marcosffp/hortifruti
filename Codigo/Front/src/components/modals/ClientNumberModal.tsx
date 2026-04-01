import { useState } from "react";

interface ClientNumberModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (number: string, dueDate?: string) => void;
}

export default function ClientNumberModal({ open, onClose, onConfirm }: ClientNumberModalProps) {
  const [number, setNumber] = useState("");
  const [dueDate, setDueDate] = useState("");

  if (!open) return null;

  const handleConfirm = () => {
    if (number.trim()) {
      // Se a data foi preenchida, envia ela, senão envia null
      const dueDateValue = dueDate.trim() ? dueDate.trim() : undefined;
      onConfirm(number.trim(), dueDateValue);
      
      // Limpa os campos após confirmar
      setNumber("");
      setDueDate("");
    }
  };

  const handleClose = () => {
    // Limpa os campos ao fechar
    setNumber("");
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
            disabled={!number.trim()}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}