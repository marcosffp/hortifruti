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
 * lado de `User` no backend, em vez de dentro do pacote de compras. Visual segue o mesmo padrão de
 * avatar circular da tabela "Usuários do Sistema" logo abaixo, pra ler como uma extensão dela, não
 * como um bloco solto.
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
      const qrDataUrl = await QRCode.toDataURL(url, {
        margin: 1,
        color: { dark: "#1f2937", light: "#ffffff" },
      });
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
      <div className="px-6 py-4 border-b flex flex-wrap justify-between items-center gap-3">
        <div className="flex items-center gap-3">
          <span className="bg-primary-bg p-2.5 rounded-full">
            <Smartphone className="text-primary" size={20} />
          </span>
          <div>
            <h2 className="text-lg font-semibold text-gray-800">
              Dispositivos vinculados
            </h2>
            <p className="text-sm text-gray-500">
              Celulares autorizados a fotografar notas de compra sem precisar
              logar
            </p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={carregarDispositivos}
            className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors"
            title="Atualizar lista"
          >
            <RefreshCw
              className={`w-4 h-4 ${carregando ? "animate-spin" : ""}`}
            />
          </button>
          <Button
            variant="primary"
            onClick={gerarCodigo}
            disabled={gerandoPareamento}
            icon={<QrCode size={18} />}
          >
            {gerandoPareamento ? "Gerando..." : "Vincular novo dispositivo"}
          </Button>
        </div>
      </div>

      {pareamento && (
        <div className="bg-primary-bg border-b border-gray-200 p-6">
          <div className="flex flex-col sm:flex-row items-center gap-6 max-w-xl mx-auto">
            {/* biome-ignore lint: QR gerado dinamicamente como data URL, não é asset do next/image */}
            <img
              src={pareamento.qrDataUrl}
              alt="QR code de pareamento"
              className="w-32 h-32 shrink-0 rounded-lg border border-gray-200 bg-white p-1.5 shadow-sm"
            />
            <div className="text-center sm:text-left space-y-2">
              <p className="text-sm text-gray-600">
                Escaneie com a câmera do celular, ou acesse{" "}
                <span className="font-medium text-gray-800">
                  {origemPareamento()}/dispositivo/vincular
                </span>{" "}
                e digite o código:
              </p>
              <p className="text-3xl font-mono font-bold tracking-[0.3em] text-gray-800">
                {pareamento.codigo}
              </p>
              <p className="text-xs text-gray-500">
                Expira às{" "}
                {new Date(pareamento.expiraEm).toLocaleTimeString("pt-BR")}
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="p-4">
        {carregando ? (
          <div className="flex justify-center items-center h-24">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
          </div>
        ) : dispositivos.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Smartphone size={40} className="text-gray-300 mb-2" />
            <p className="text-sm text-gray-400">
              Nenhum dispositivo vinculado ainda.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {dispositivos.map((dispositivo) => (
              <div
                key={dispositivo.id}
                className="flex items-center justify-between gap-3 border border-gray-200 rounded-lg p-3 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="bg-primary-bg p-2 rounded-full shrink-0">
                    <Smartphone className="text-primary" size={18} />
                  </span>
                  <div className="min-w-0">
                    <p className="font-medium text-gray-800 truncate">
                      {dispositivo.nome}
                    </p>
                    <p className="text-xs text-gray-500 truncate">
                      Pareado em {formatarData(dispositivo.pareadoEm)} —
                      último uso: {formatarData(dispositivo.ultimoUsoEm)}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setParaRevogar(dispositivo)}
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors shrink-0"
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
