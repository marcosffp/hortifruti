"use client";

import { QrCode, RefreshCw, Smartphone, Trash2 } from "lucide-react";
import QRCode from "qrcode";
import { useCallback, useEffect, useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import Button from "@/components/ui/Button";
import { useRealtimeSocket } from "@/hooks/useRealtimeSocket";
import {
  type Dispositivo,
  dispositivoService,
} from "@/services/dispositivoService";
import { showError, showSuccess } from "@/services/notificationService";

type PareamentoAtivo = {
  codigo: string;
  expiraEm: string;
  qrDataUrl: string;
};

function formatarData(iso: string | null): string {
  if (!iso) return "nunca";
  return new Date(iso).toLocaleString("pt-BR");
}

/**
 * Em dev, o PC que gera o código muitas vezes precisa estar em `localhost` (cookie de sessão só
 * persiste ali sem HTTPS — profile prod usa `Secure`/`SameSite=None`), mas o celular real só
 * alcança a rede pelo IP local. `NEXT_PUBLIC_PAREAMENTO_URL` existe pra decidir essa origem
 * separado de onde o admin está navegando; vazio (padrão de produção) cai em
 * `window.location.origin`, que já é a origem certa quando PC e celular usam o mesmo domínio.
 */
function origemPareamento(): string {
  return process.env.NEXT_PUBLIC_PAREAMENTO_URL || window.location.origin;
}

/**
 * Gestão de celulares vinculados pra captura de nota (ver `spec-captura-nota-dispositivo-vinculado-
 * tempo-real.md`): gerar código/QR de pareamento, listar dispositivos já vinculados e revogar. Vive
 * no Módulo Acesso porque um dispositivo vinculado é um conceito de autenticação/controle de
 * acesso, não de domínio de compras — mesmo raciocínio usado pra colocar `DispositivoVinculado` ao
 * lado de `User` no backend, em vez de dentro do pacote de compras.
 */
export default function DispositivosVinculados() {
  const [dispositivos, setDispositivos] = useState<Dispositivo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [pareamento, setPareamento] = useState<PareamentoAtivo | null>(null);
  const [gerandoPareamento, setGerandoPareamento] = useState(false);
  const [paraRevogar, setParaRevogar] = useState<Dispositivo | null>(null);
  const [revogando, setRevogando] = useState(false);

  const carregarDispositivos = useCallback(async () => {
    setCarregando(true);
    try {
      setDispositivos(await dispositivoService.listarDispositivos());
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao carregar dispositivos.",
      );
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregarDispositivos();
  }, [carregarDispositivos]);

  // O celular confirma o pareamento em outro aparelho, sem nenhuma relação direta com esta aba —
  // só sabemos que deu certo porque o backend empurra esse evento assim que o pareamento é salvo.
  // Fecha o QR e atualiza a lista na hora, sem precisar de F5.
  useRealtimeSocket((event) => {
    if (event.type !== "dispositivo-pareado") return;
    carregarDispositivos();
    setPareamento((atual) => {
      if (atual) showSuccess("Dispositivo vinculado!");
      return null;
    });
  });

  // O código expira mesmo sem nenhum evento chegar (ex.: ninguém escaneou) — some sozinho da tela
  // no horário certo em vez de ficar mostrando um QR morto indefinidamente.
  useEffect(() => {
    if (!pareamento) return;
    const restanteMs = new Date(pareamento.expiraEm).getTime() - Date.now();
    if (restanteMs <= 0) {
      setPareamento(null);
      return;
    }
    const timeoutId = setTimeout(() => setPareamento(null), restanteMs);
    return () => clearTimeout(timeoutId);
  }, [pareamento]);

  const gerarCodigo = async () => {
    setGerandoPareamento(true);
    try {
      const { codigo, expiraEm } = await dispositivoService.iniciarPareamento();
      const url = `${origemPareamento()}/dispositivo/vincular?codigo=${codigo}`;
      const qrDataUrl = await QRCode.toDataURL(url);
      setPareamento({ codigo, expiraEm, qrDataUrl });
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao gerar código de pareamento.",
      );
    } finally {
      setGerandoPareamento(false);
    }
  };

  const confirmarRevogacao = async () => {
    if (!paraRevogar) return;
    setRevogando(true);
    try {
      await dispositivoService.revogarDispositivo(paraRevogar.id);
      showSuccess(`Dispositivo "${paraRevogar.nome}" revogado.`);
      setParaRevogar(null);
      await carregarDispositivos();
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Falha ao revogar dispositivo.",
      );
    } finally {
      setRevogando(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden mb-6">
      <div className="px-6 py-4 border-b flex flex-wrap justify-between items-center gap-2">
        <div>
          <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
            <Smartphone className="w-5 h-5 text-gray-600" /> Dispositivos
            vinculados
          </h2>
          <span className="text-sm text-gray-500">
            Celulares autorizados a fotografar notas de compra sem precisar
            logar
          </span>
        </div>
        <Button
          variant="primary"
          onClick={gerarCodigo}
          disabled={gerandoPareamento}
          className="bg-green-600 hover:bg-green-700"
          icon={<QrCode size={18} />}
        >
          {gerandoPareamento ? "Gerando..." : "Vincular novo dispositivo"}
        </Button>
      </div>

      {pareamento && (
        <div className="flex flex-col sm:flex-row items-center gap-4 bg-gray-50 border-b border-gray-200 p-4">
          {/* biome-ignore lint: QR gerado dinamicamente como data URL, não é asset do next/image */}
          <img
            src={pareamento.qrDataUrl}
            alt="QR code de pareamento"
            className="w-36 h-36 shrink-0"
          />
          <div className="text-sm text-gray-600 space-y-1">
            <p>
              Peça pra escanear o QR com a câmera do celular — ele abre direto
              na tela de confirmação, sem precisar digitar nada.
            </p>
            <p>
              Ou acesse{" "}
              <span className="font-medium text-gray-800">
                {origemPareamento()}/dispositivo/vincular
              </span>{" "}
              e digite o código:
            </p>
            <p className="text-2xl font-mono font-bold tracking-widest text-gray-800">
              {pareamento.codigo}
            </p>
            <p className="text-xs text-gray-400">
              Expira às{" "}
              {new Date(pareamento.expiraEm).toLocaleTimeString("pt-BR")}
            </p>
          </div>
        </div>
      )}

      <div className="px-6 py-3 border-b bg-gray-50 flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">
          {dispositivos.length} dispositivo(s) ativo(s)
        </span>
        <button
          type="button"
          onClick={carregarDispositivos}
          className="p-1.5 hover:bg-gray-200 rounded-md transition-colors"
          title="Atualizar lista"
        >
          <RefreshCw
            className={`w-4 h-4 text-gray-600 ${carregando ? "animate-spin" : ""}`}
          />
        </button>
      </div>

      <div className="p-4">
        {carregando ? (
          <div className="flex justify-center items-center h-24">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
          </div>
        ) : dispositivos.length === 0 ? (
          <p className="text-sm text-gray-400 italic py-2">
            Nenhum dispositivo vinculado ainda.
          </p>
        ) : (
          <div className="space-y-2">
            {dispositivos.map((dispositivo) => (
              <div
                key={dispositivo.id}
                className="flex items-center justify-between border border-gray-200 rounded-lg p-3"
              >
                <div>
                  <p className="font-medium text-gray-800">
                    {dispositivo.nome}
                  </p>
                  <p className="text-xs text-gray-500">
                    Pareado em {formatarData(dispositivo.pareadoEm)} — último
                    uso: {formatarData(dispositivo.ultimoUsoEm)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setParaRevogar(dispositivo)}
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  title="Revogar dispositivo"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <ConfirmDeleteModal
        open={paraRevogar !== null}
        onClose={() => setParaRevogar(null)}
        onConfirm={confirmarRevogacao}
        confirmDisabled={revogando}
        title={`Revogar "${paraRevogar?.nome ?? ""}"?`}
      >
        <p className="text-sm text-gray-600">
          Esse celular vai parar de conseguir enviar fotos até ser pareado de
          novo. Use se o aparelho foi perdido, roubado, ou trocado de mão.
        </p>
      </ConfirmDeleteModal>
    </div>
  );
}
