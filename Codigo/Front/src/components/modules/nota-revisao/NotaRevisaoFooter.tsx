interface NotaRevisaoFooterProps {
  isTeste: boolean;
  podeConfirmar: boolean;
  confirmando: boolean;
  onClose: () => void;
  onConfirmar: () => void;
}

export default function NotaRevisaoFooter({
  isTeste,
  podeConfirmar,
  confirmando,
  onClose,
  onConfirmar,
}: NotaRevisaoFooterProps) {
  return (
    <div className="flex flex-col sm:flex-row justify-between items-center gap-3 p-4 border-t border-gray-300 shrink-0">
      <p className="text-xs text-gray-500">
        {isTeste
          ? "Revisão isolada de teste — não está ligada a nenhuma captura pendente."
          : podeConfirmar
            ? "Confira os itens e o cliente antes de lançar a compra."
            : "Selecione um cliente do cadastro pra poder lançar a compra."}
      </p>
      <div className="flex gap-2 shrink-0">
        <button
          type="button"
          onClick={onClose}
          disabled={confirmando}
          className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50"
        >
          Fechar
        </button>
        {!isTeste && (
          <button
            type="button"
            onClick={onConfirmar}
            disabled={!podeConfirmar || confirmando}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {confirmando ? "Lançando..." : "Confirmar e lançar compra"}
          </button>
        )}
      </div>
    </div>
  );
}
