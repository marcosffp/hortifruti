"use client";

import { RotateCcw, Trash2, X, ZoomIn, ZoomOut } from "lucide-react";
import { useEffect, useState } from "react";
import ClientAutocompleteField from "@/components/ui/ClientAutocompleteField";
import MaskedDecimalInput from "@/components/ui/MaskedDecimalInput";
import ProductAutocompleteField from "@/components/ui/ProductAutocompleteField";
import { useCapturaNota } from "@/hooks/useCapturaNota";
import { useClient } from "@/hooks/useClient";
import { useFiscalProduct } from "@/hooks/useFiscalProduct";
import type { ClientSelectionInfo } from "@/types/clientType";
import type {
  ItemNotaExtraido,
  NotaExtracaoResponse,
  ProdutoSugerido,
} from "@/types/notaExtracaoType";
import type { FiscalProductType } from "@/types/purchaseType";
import { todaySaoPaulo } from "@/utils/dateUtils";
import { formatCurrency } from "@/utils/formatCurrency";
import {
  type NumericField,
  type NumericRow,
  recalcNumericRow,
  round,
} from "@/utils/numericRow";
import { showError, showSuccess } from "@/utils/toastUtils";

// A "data lida" vem da OCR como texto solto (dd/mm/aaaa) — tenta converter pra ISO (o formato que
// <input type="date"> e o backend esperam); se não bater com esse formato, quem chama cai pro
// default de hoje em vez de mandar uma data inválida/vazia pro backend.
function parseDataLidaParaIso(dataLida: string | null): string | null {
  const match = dataLida?.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (!match) return null;
  const [, dia, mes, ano] = match;
  return `${ano}-${mes}-${dia}`;
}

interface RevisaoRow extends NumericRow {
  produtoLido: string;
  unidadeLida: string | null;
  produtoSugerido: ProdutoSugerido | null;
  confianca: "alta" | "media" | "baixa" | null;
  code: string;
}

const CONFIANCA_BADGE: Record<"alta" | "media" | "baixa", string> = {
  alta: "bg-green-100 text-green-800",
  media: "bg-yellow-100 text-yellow-800",
  baixa: "bg-red-100 text-red-800",
};

// Mesma margem de tolerância usada no backend (NotaConsistenciaChecker) — arredondamento de
// centavos não deve acusar inconsistência à toa.
const MARGEM_CONSISTENCIA = 0.05;

function itemBate(row: RevisaoRow): boolean {
  return Math.abs(row.quantity * row.price - row.total) < MARGEM_CONSISTENCIA;
}

function itemToRow(item: ItemNotaExtraido): RevisaoRow {
  return {
    produtoLido: item.produtoLido,
    unidadeLida: item.unidade,
    produtoSugerido: item.produtoSugerido,
    confianca: item.confianca,
    code: item.produtoSugerido?.codigo ?? "",
    quantity: item.quantidade ?? 0,
    price: item.precoUnitario ?? 0,
    total: item.total ?? 0,
    lastEdited: ["quantity", "price"],
  };
}

interface NotaRevisaoModalProps {
  /**
   * Ausente só na tela de dev que testa `/api/compras/notas/extrair` direto, sem passar pela fila
   * de capturas — nesse caso não existe compra pra lançar, só a comparação visual.
   */
  capturaId?: number;
  imageUrl: string;
  extraction: NotaExtracaoResponse;
  onClose: () => void;
  /** Chamado após confirmar a compra com sucesso — quem usa deve fechar o modal e recarregar a fila. */
  onConfirmado?: () => void;
}

export default function NotaRevisaoModal({
  capturaId,
  imageUrl,
  extraction,
  onClose,
  onConfirmado,
}: NotaRevisaoModalProps) {
  const [products, setProducts] = useState<FiscalProductType[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [clients, setClients] = useState<ClientSelectionInfo[]>([]);
  // Pré-seleciona direto do cliente sugerido pelo ClienteMatchingService (mesmo padrão do
  // produtoSugerido por item) — não espera a lista de clientes carregar, o id já vem confiável do
  // backend.
  const [clienteId, setClienteId] = useState<number | null>(
    extraction.clienteSugerido?.id ?? null,
  );
  const [clienteNome, setClienteNome] = useState(
    extraction.clienteSugerido?.nome ?? extraction.cliente ?? "",
  );
  const [purchaseDate, setPurchaseDate] = useState(
    () => parseDataLidaParaIso(extraction.data) ?? todaySaoPaulo(),
  );
  const [rows, setRows] = useState<RevisaoRow[]>(
    extraction.itens.map(itemToRow),
  );
  const [zoom, setZoom] = useState(1);
  const [confirmando, setConfirmando] = useState(false);
  const { getFiscalProducts } = useFiscalProduct();
  const { getAllClientsForSelection } = useClient();
  const { confirmar } = useCapturaNota();

  useEffect(() => {
    getFiscalProducts()
      .then(setProducts)
      .catch((error) => console.error(error))
      .finally(() => setLoadingProducts(false));
  }, [getFiscalProducts]);

  // Carrega o cadastro completo pro autocomplete poder resolver o clienteId já pré-selecionado
  // (extraction.clienteSugerido) pro nome exato do cadastro, e pra sugerir o resto conforme o
  // usuário digita se precisar trocar.
  useEffect(() => {
    getAllClientsForSelection()
      .then(setClients)
      .catch((error) => console.error(error));
  }, [getAllClientsForSelection]);

  const selecionarCliente = (id: number | null, nome: string) => {
    setClienteId(id);
    setClienteNome(nome);
  };

  const updateRowCode = (index: number, code: string) => {
    setRows((prev) =>
      prev.map((row, i) => (i === index ? { ...row, code } : row)),
    );
  };

  const updateRowField = (
    index: number,
    field: NumericField,
    value: number,
  ) => {
    setRows((prev) =>
      prev.map((row, i) =>
        i === index ? recalcNumericRow(row, field, value) : row,
      ),
    );
  };

  const removeRow = (index: number) => {
    setRows((prev) => prev.filter((_, i) => i !== index));
  };

  const podeConfirmar =
    capturaId !== undefined && clienteId !== null && rows.length > 0;

  const confirmarCompra = async () => {
    if (!podeConfirmar || clienteId === null || capturaId === undefined) return;

    setConfirmando(true);
    try {
      await confirmar(capturaId, {
        clientId: clienteId,
        purchaseDate,
        items: rows.map((row) => ({
          code: row.code,
          quantity: row.quantity,
          price: row.price,
        })),
      });

      showSuccess("Compra lançada com sucesso!");
      onConfirmado?.();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Falha ao lançar a compra.",
      );
    } finally {
      setConfirmando(false);
    }
  };

  const totalCalculado = rows.reduce((sum, row) => sum + row.total, 0);
  const totalLido = extraction.totalGeral ?? 0;
  const bateComOTotalLido = Math.abs(totalCalculado - totalLido) < 0.05;

  return (
    <div className="fixed inset-0 h-full bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-7xl h-[92vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-4 border-b border-gray-300 shrink-0">
          <h2 className="text-lg font-semibold text-gray-800">
            Revisão da extração — compare com a foto e corrija o que precisar
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          <div className="md:w-1/2 flex flex-col border-b md:border-b-0 md:border-r border-gray-200 min-h-0">
            <div className="flex items-center gap-2 p-2 border-b border-gray-200 shrink-0">
              <button
                type="button"
                onClick={() => setZoom((z) => Math.max(1, round(z - 0.25, 2)))}
                className="p-2 hover:bg-gray-100 rounded-lg"
                title="Diminuir zoom"
              >
                <ZoomOut className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => setZoom((z) => Math.min(4, round(z + 0.25, 2)))}
                className="p-2 hover:bg-gray-100 rounded-lg"
                title="Aumentar zoom"
              >
                <ZoomIn className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => setZoom(1)}
                className="p-2 hover:bg-gray-100 rounded-lg"
                title="Resetar zoom"
              >
                <RotateCcw className="w-4 h-4" />
              </button>
              <span className="text-xs text-gray-500">
                {Math.round(zoom * 100)}% — role pra navegar quando ampliado
              </span>
            </div>
            <div className="flex-1 overflow-auto bg-gray-100 p-2">
              {/* biome-ignore lint: preview de dev, não precisa de otimização do next/image */}
              <img
                src={imageUrl}
                alt="Nota original"
                style={{ width: `${zoom * 100}%`, maxWidth: "none" }}
                className="select-none"
                draggable={false}
              />
            </div>
          </div>

          <div className="md:w-1/2 flex-1 overflow-y-auto p-4 space-y-4 min-h-0">
            {extraction.consistente === false && (
              <div className="bg-red-50 border border-red-300 rounded-lg p-3 text-sm text-red-800">
                <p className="font-semibold">
                  ⚠ A soma dos itens não bate com o total lido na nota.
                </p>
                {extraction.itensParaConferir.length > 0 && (
                  <p className="mt-1">
                    Confira primeiro:{" "}
                    <span className="font-medium">
                      {extraction.itensParaConferir.join(", ")}
                    </span>{" "}
                    — a linha desse(s) item(ns) foi marcada abaixo com{" "}
                    <span className="font-medium">⚠ qtd × preço ≠ total</span>{" "}
                    ou é a de maior valor, mais impacto na diferença.
                  </p>
                )}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <p className="flex items-center gap-2 text-xs font-medium text-gray-500 uppercase mb-1">
                  <span>
                    Cliente{" "}
                    {extraction.cliente
                      ? `(lido: "${extraction.cliente}")`
                      : ""}
                  </span>
                  {extraction.clienteConfianca && (
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${CONFIANCA_BADGE[extraction.clienteConfianca]}`}
                    >
                      {extraction.clienteConfianca}
                    </span>
                  )}
                </p>
                <ClientAutocompleteField
                  clients={clients}
                  value={clienteId}
                  onSelect={selecionarCliente}
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
                  onChange={(e) => setPurchaseDate(e.target.value)}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500"
                />
              </div>
            </div>

            <div className="hidden lg:flex gap-2 px-1 text-xs font-semibold text-gray-500 uppercase">
              <span className="flex-1">
                Produto (lido → selecione o do catálogo)
              </span>
              <span className="w-24 text-right">Qtd.</span>
              <span className="w-24 text-right">Preço Unit.</span>
              <span className="w-24 text-right">Total</span>
              <span className="w-9" />
            </div>

            <div className="space-y-3">
              {rows.map((row, index) => (
                <div
                  // biome-ignore lint/suspicious/noArrayIndexKey: linhas vêm de um array fixo retornado pela extração, sem id próprio
                  key={index}
                  className="border border-gray-200 rounded-lg p-3 space-y-2"
                >
                  <p className="text-xs text-gray-500 flex items-center gap-2">
                    <span>
                      Lido:{" "}
                      <span className="font-medium text-gray-700">
                        {row.produtoLido}
                      </span>
                      {row.unidadeLida ? ` (${row.unidadeLida})` : ""}
                    </span>
                    {row.confianca && (
                      <span
                        className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${CONFIANCA_BADGE[row.confianca]}`}
                      >
                        {row.confianca}
                      </span>
                    )}
                    {row.produtoSugerido && (
                      <span className="text-gray-400">
                        sugerido: {row.produtoSugerido.nome} (
                        {Math.round(row.produtoSugerido.score * 100)}%)
                      </span>
                    )}
                    {!itemBate(row) && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase bg-red-100 text-red-800">
                        ⚠ qtd × preço ≠ total
                      </span>
                    )}
                  </p>
                  <div className="flex flex-col lg:flex-row lg:items-center gap-2">
                    <div className="flex-1">
                      <ProductAutocompleteField
                        products={products}
                        value={row.code}
                        onSelect={(code) => updateRowCode(index, code)}
                        disabled={loadingProducts}
                      />
                    </div>
                    <div className="flex gap-2">
                      <MaskedDecimalInput
                        value={row.quantity}
                        onChange={(quantity) =>
                          updateRowField(index, "quantity", quantity)
                        }
                        decimalPlaces={3}
                        placeholder="0,000"
                        className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
                      />
                      <MaskedDecimalInput
                        value={row.price}
                        onChange={(price) =>
                          updateRowField(index, "price", price)
                        }
                        placeholder="0,00"
                        className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
                      />
                      <MaskedDecimalInput
                        value={row.total}
                        onChange={(total) =>
                          updateRowField(index, "total", total)
                        }
                        placeholder="0,00"
                        className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right font-semibold focus:outline-none focus:ring-1 focus:ring-green-500"
                      />
                      <button
                        type="button"
                        onClick={() => removeRow(index)}
                        className="w-9 flex items-center justify-center text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Remover item"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
              {rows.length === 0 && (
                <p className="text-sm text-gray-500 italic">
                  Nenhum item — todos foram removidos.
                </p>
              )}
            </div>

            <div className="pt-2 border-t border-gray-200 space-y-1">
              <div className="flex justify-between text-sm text-gray-500">
                <span>Total lido na nota</span>
                <span>{formatCurrency(totalLido)}</span>
              </div>
              <div className="flex justify-between font-semibold">
                <span>Total calculado (itens acima)</span>
                <span
                  className={
                    bateComOTotalLido ? "text-green-700" : "text-red-700"
                  }
                >
                  {formatCurrency(totalCalculado)}
                </span>
              </div>
              {!bateComOTotalLido && (
                <p className="text-xs text-red-600">
                  ⚠ Não bate com o total lido na nota — confira os itens acima.
                </p>
              )}
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row justify-between items-center gap-3 p-4 border-t border-gray-300 shrink-0">
          <p className="text-xs text-gray-500">
            {capturaId === undefined
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
            {capturaId !== undefined && (
              <button
                type="button"
                onClick={confirmarCompra}
                disabled={!podeConfirmar || confirmando}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
              >
                {confirmando ? "Lançando..." : "Confirmar e lançar compra"}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
