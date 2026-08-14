interface NotaInconsistenciaAlertProps {
  itensParaConferir: string[];
}

export default function NotaInconsistenciaAlert({
  itensParaConferir,
}: NotaInconsistenciaAlertProps) {
  return (
    <div className="bg-red-50 border border-red-300 rounded-lg p-3 text-sm text-red-800">
      <p className="font-semibold">
        ⚠ A soma dos itens não bate com o total lido na nota.
      </p>
      {itensParaConferir.length > 0 && (
        <p className="mt-1">
          Confira primeiro:{" "}
          <span className="font-medium">{itensParaConferir.join(", ")}</span> —
          a linha desse(s) item(ns) foi marcada abaixo com{" "}
          <span className="font-medium">⚠ qtd × preço ≠ total</span> ou é a de
          maior valor, mais impacto na diferença.
        </p>
      )}
    </div>
  );
}
