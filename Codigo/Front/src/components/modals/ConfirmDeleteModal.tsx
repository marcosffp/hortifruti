interface ConfirmDeleteModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  confirmDisabled?: boolean;
  children?: React.ReactNode;
}

export default function ConfirmDeleteModal({
  open,
  onClose,
  onConfirm,
  title = "Tem certeza que deseja deletar este item?",
  confirmDisabled = false,
  children,
}: ConfirmDeleteModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
        <h2 className="text-lg font-semibold mb-2">{title}</h2>
        {children && <div className="mb-4">{children}</div>}
        <div className="flex justify-end gap-2">
          <button
            type="button"
            className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200"
            onClick={onClose}
          >
            Cancelar
          </button>
          <button
            type="button"
            disabled={confirmDisabled}
            className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={() => onConfirm()}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}
