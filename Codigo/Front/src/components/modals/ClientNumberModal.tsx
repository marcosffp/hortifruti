import { useState } from "react";

interface ClientNumberModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (number: string) => void;
}

export default function ClientNumberModal({ open, onClose, onConfirm }: ClientNumberModalProps) {
  const [number, setNumber] = useState("");

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
        <h2 className="text-lg font-semibold mb-2">Informe o número identificador do cliente</h2>
        <input
          type="text"
          className="w-full border border-gray-300 rounded px-3 py-2 mb-4 focus:outline-none focus:ring-2 focus:ring-green-500"
          placeholder="Digite o número identificador"
          value={number}
          onChange={e => setNumber(e.target.value)}
        />
        <div className="flex justify-end gap-2">
          <button
            className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200"
            onClick={onClose}
          >
            Cancelar
          </button>
          <button
            className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
            onClick={() => {
              if (number.trim()) {
                onConfirm(number.trim());
              }
            }}
            disabled={!number.trim()}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}