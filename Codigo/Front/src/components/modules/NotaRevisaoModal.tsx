"use client";

import { X } from "lucide-react";
import { useEffect, useState } from "react";
import {
  itemToRow,
  parseDataLidaParaIso,
} from "@/components/modules/nota-revisao/helpers";
import NotaClienteDataFields from "@/components/modules/nota-revisao/NotaClienteDataFields";
import NotaDivergenciaPrecoAlert from "@/components/modules/nota-revisao/NotaDivergenciaPrecoAlert";
import NotaImagePanel from "@/components/modules/nota-revisao/NotaImagePanel";
import NotaInconsistenciaAlert from "@/components/modules/nota-revisao/NotaInconsistenciaAlert";
import NotaItensList from "@/components/modules/nota-revisao/NotaItensList";
import NotaRevisaoFooter from "@/components/modules/nota-revisao/NotaRevisaoFooter";
import NotaTotaisResumo from "@/components/modules/nota-revisao/NotaTotaisResumo";
import type { RevisaoRow } from "@/components/modules/nota-revisao/types";
import { useCapturaNota } from "@/hooks/useCapturaNota";
import { useClient } from "@/hooks/useClient";
import { useFiscalProduct } from "@/hooks/useFiscalProduct";
import type { ClientSelectionInfo } from "@/types/clientType";
import type { NotaExtracaoResponse } from "@/types/notaExtracaoType";
import type { FiscalProductType } from "@/types/purchaseType";
import { todaySaoPaulo } from "@/utils/dateUtils";
import type { NumericField } from "@/utils/numericRow";
import { recalcNumericRow, round } from "@/utils/numericRow";
import { showError, showSuccess } from "@/utils/toastUtils";

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
  const [confirmando, setConfirmando] = useState(false);
  const [precosVigentes, setPrecosVigentes] = useState<Record<string, number>>(
    {},
  );
  const { getFiscalProducts } = useFiscalProduct();
  const { getAllClientsForSelection } = useClient();
  const { confirmar, buscarPrecosVigentes } = useCapturaNota();

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

  // Assim que o cliente (selecionado/corrigido no autocomplete) e a data resolvem pra uma tabela de
  // preços confirmada, busca os preços por produto e já aplica nas linhas cujo produto bater — a
  // extração só aplica isso uma vez, com o cliente sugerido pelo Gemini, e não reprocessa se o
  // usuário trocar o cliente aqui na revisão.
  useEffect(() => {
    if (clienteId === null) {
      setPrecosVigentes({});
      return;
    }

    let cancelado = false;
    buscarPrecosVigentes(clienteId, purchaseDate)
      .then((precos) => {
        if (cancelado) return;
        setPrecosVigentes(precos);
        setRows((prev) =>
          prev.map((row) => {
            const precoTabela = row.code ? precos[row.code] : undefined;
            if (precoTabela == null || precoTabela === row.price) return row;
            return {
              ...row,
              price: precoTabela,
              total: round(row.quantity * precoTabela, 2),
            };
          }),
        );
      })
      .catch((error) => console.error(error));

    return () => {
      cancelado = true;
    };
  }, [clienteId, purchaseDate, buscarPrecosVigentes]);

  const updateRowCode = (index: number, code: string) => {
    setRows((prev) =>
      prev.map((row, i) => {
        if (i !== index) return row;
        const precoTabela = precosVigentes[code];
        if (precoTabela == null) return { ...row, code };
        return {
          ...row,
          code,
          price: precoTabela,
          total: round(row.quantity * precoTabela, 2),
        };
      }),
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
          <NotaImagePanel imageUrl={imageUrl} />

          <div className="md:w-1/2 flex-1 overflow-y-auto p-4 space-y-4 min-h-0">
            {extraction.consistente === false && (
              <NotaInconsistenciaAlert
                itensParaConferir={extraction.itensParaConferir}
              />
            )}

            <NotaDivergenciaPrecoAlert
              itensComDivergenciaPreco={extraction.itensComDivergenciaPreco}
              semTabelaPrecoParaCompetencia={
                extraction.semTabelaPrecoParaCompetencia
              }
            />

            <NotaClienteDataFields
              clients={clients}
              clienteId={clienteId}
              clienteNome={clienteNome}
              clienteLido={extraction.cliente}
              clienteConfianca={extraction.clienteConfianca}
              onSelectCliente={selecionarCliente}
              purchaseDate={purchaseDate}
              onChangePurchaseDate={setPurchaseDate}
            />

            <NotaItensList
              rows={rows}
              products={products}
              loadingProducts={loadingProducts}
              onChangeRowCode={updateRowCode}
              onChangeRowField={updateRowField}
              onRemoveRow={removeRow}
            />

            <NotaTotaisResumo
              totalLido={totalLido}
              totalCalculado={totalCalculado}
            />
          </div>
        </div>

        <NotaRevisaoFooter
          isTeste={capturaId === undefined}
          podeConfirmar={podeConfirmar}
          confirmando={confirmando}
          onClose={onClose}
          onConfirmar={confirmarCompra}
        />
      </div>
    </div>
  );
}
