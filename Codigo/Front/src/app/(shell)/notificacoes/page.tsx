"use client";

import {
  Building2,
  CheckCircle2,
  FileText,
  Mail,
  MessageCircle,
  RotateCcw,
  Send,
  ShieldAlert,
  Upload,
  Users,
  X,
} from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import RoleGuard from "@/components/auth/RoleGuard";
import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import MaskedDecimalInput from "@/components/ui/MaskedDecimalInput";
import MaskedThousandsInput from "@/components/ui/MaskedThousandsInput";
import { useAuth } from "@/hooks/useAuth";
import { useBulkNotificationSend } from "@/hooks/useBulkNotificationSend";
import { bulkNotificationService } from "@/services/bulkNotificationService";
import { clientService } from "@/services/clientService";
import type {
  Cliente,
  TipoDestinatario,
  TipoReferencia,
} from "@/types/notificacoesTypes";
import { loadDraft, saveDraft } from "@/utils/notificationDraft";
import { showError, showSuccess } from "@/utils/toastUtils";

const MESES_PT_BR = Array.from({ length: 12 }, (_, i) =>
  new Date(2000, i, 1).toLocaleString("pt-BR", { month: "long" }),
);

// "Mês" por padrão sugere o mês anterior ao de hoje (o mês do pedido mais recente já fechado).
function getMesAnoAnteriorPadrao(): { mes: number; ano: number } {
  const hoje = new Date();
  const mesAtual = hoje.getMonth(); // 0-indexado: janeiro = 0
  if (mesAtual === 0) {
    return { mes: 12, ano: hoje.getFullYear() - 1 };
  }
  // mesAtual (0-indexado) coincide numericamente com o mês anterior em 1-indexado.
  return { mes: mesAtual, ano: hoje.getFullYear() };
}

// "Período" por padrão sugere os últimos 7 dias (incluindo hoje).
function getPeriodoPadrao(): { dataInicial: string; dataFinal: string } {
  const hoje = new Date();
  const inicio = new Date(hoje);
  inicio.setDate(hoje.getDate() - 6);
  const paraISO = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return { dataInicial: paraISO(inicio), dataFinal: paraISO(hoje) };
}

function formatarDiaMes(dataISO: string): string {
  const [ano, mes, dia] = dataISO.split("-").map(Number);
  const nomeMes = new Date(ano, mes - 1, dia).toLocaleString("pt-BR", {
    month: "long",
  });
  return `${String(dia).padStart(2, "0")} de ${nomeMes}`;
}

function gerarTextoReferencia(
  tipo: TipoReferencia,
  mes: number,
  ano: number,
  dataInicial: string,
  dataFinal: string,
): string {
  if (tipo === "mes") {
    return `Encaminhamos as informações referentes ao pedido realizado no mês de ${MESES_PT_BR[mes - 1]} de ${ano}.`;
  }
  return `Encaminhamos as informações referentes ao pedido realizado no período de ${formatarDiaMes(dataInicial)} a ${formatarDiaMes(dataFinal)}.`;
}

export default function NotificacoesPage() {
  const { environment, hasRole } = useAuth();
  const searchParams = useSearchParams();
  const [searchTerm, setSearchTerm] = useState("");
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [arquivos, setArquivos] = useState<File[]>([]);
  const [mensagemPersonalizada, setMensagemPersonalizada] = useState("");
  const [canaisEnvio, setCanaisEnvio] = useState<{
    email: boolean;
    whatsapp: boolean;
  }>({ email: true, whatsapp: false });
  const [tipoDestinatario, setTipoDestinatario] =
    useState<TipoDestinatario>("clientes");
  const [tipoReferencia, setTipoReferencia] =
    useState<TipoReferencia>("periodo");
  const [mesReferencia, setMesReferencia] = useState(
    () => getMesAnoAnteriorPadrao().mes,
  );
  const [anoReferencia, setAnoReferencia] = useState(
    () => getMesAnoAnteriorPadrao().ano,
  );
  const [dataInicialReferencia, setDataInicialReferencia] = useState(
    () => getPeriodoPadrao().dataInicial,
  );
  const [dataFinalReferencia, setDataFinalReferencia] = useState(
    () => getPeriodoPadrao().dataFinal,
  );
  const [textoEditadoManualmente, setTextoEditadoManualmente] = useState(false);

  const [cardValue, setCardValue] = useState(0);
  const [cashValue, setCashValue] = useState(0);
  const [contabilidadeEmails, setContabilidadeEmails] = useState<string[]>([]);

  // IDs de clientes selecionados no rascunho, aplicados assim que a lista de clientes carregar
  // (a lista em si vem sempre fresca da API, então a seleção precisa ser reaplicada por id).
  const pendingSelectedClientIdsRef = useRef<number[]>([]);

  const isGestor = hasRole("MANAGER");

  useEffect(() => {
    loadDraft().then((draft) => {
      if (!draft) return;

      setTipoDestinatario(draft.tipoDestinatario);
      setMensagemPersonalizada(draft.mensagemPersonalizada);
      setCanaisEnvio(draft.canaisEnvio);
      setCardValue(draft.cardValue);
      setCashValue(draft.cashValue);
      // "?? padrão" cobre rascunhos salvos antes desses campos existirem.
      setTipoReferencia(draft.tipoReferencia ?? "periodo");
      setMesReferencia(draft.mesReferencia ?? getMesAnoAnteriorPadrao().mes);
      setAnoReferencia(draft.anoReferencia ?? getMesAnoAnteriorPadrao().ano);
      setDataInicialReferencia(
        draft.dataInicialReferencia ?? getPeriodoPadrao().dataInicial,
      );
      setDataFinalReferencia(
        draft.dataFinalReferencia ?? getPeriodoPadrao().dataFinal,
      );
      setTextoEditadoManualmente(draft.textoEditadoManualmente ?? false);
      pendingSelectedClientIdsRef.current = draft.selectedClientIds;
    });
  }, []);

  useEffect(() => {
    saveDraft({
      tipoDestinatario,
      mensagemPersonalizada,
      canaisEnvio,
      cardValue,
      cashValue,
      selectedClientIds: clientes.filter((c) => c.selecionado).map((c) => c.id),
      tipoReferencia,
      mesReferencia,
      anoReferencia,
      dataInicialReferencia,
      dataFinalReferencia,
      textoEditadoManualmente,
    });
  }, [
    tipoDestinatario,
    mensagemPersonalizada,
    canaisEnvio,
    cardValue,
    cashValue,
    clientes,
    tipoReferencia,
    mesReferencia,
    anoReferencia,
    dataInicialReferencia,
    dataFinalReferencia,
    textoEditadoManualmente,
  ]);

  // Regenera o texto automaticamente quando o tipo de referência ou as datas mudam — mas só
  // enquanto o usuário não tiver editado o texto manualmente, pra não sobrescrever uma edição.
  useEffect(() => {
    if (textoEditadoManualmente) return;
    setMensagemPersonalizada(
      gerarTextoReferencia(
        tipoReferencia,
        mesReferencia,
        anoReferencia,
        dataInicialReferencia,
        dataFinalReferencia,
      ),
    );
  }, [
    tipoReferencia,
    mesReferencia,
    anoReferencia,
    dataInicialReferencia,
    dataFinalReferencia,
    textoEditadoManualmente,
  ]);

  const handleRestaurarTextoPadrao = () => {
    setMensagemPersonalizada(
      gerarTextoReferencia(
        tipoReferencia,
        mesReferencia,
        anoReferencia,
        dataInicialReferencia,
        dataFinalReferencia,
      ),
    );
    setTextoEditadoManualmente(false);
  };

  useEffect(() => {
    if (tipoDestinatario === "contabilidade" && !isGestor) {
      setTipoDestinatario("clientes");
    }
  }, [isGestor, tipoDestinatario]);

  useEffect(() => {
    if (tipoDestinatario !== "contabilidade" || !isGestor) return;

    bulkNotificationService
      .getAccountingRecipients()
      .then(setContabilidadeEmails);
  }, [tipoDestinatario, isGestor]);

  useEffect(() => {
    const fetchClientes = async () => {
      try {
        setIsLoading(true);
        const clientesResponse = await clientService.getAllClients();

        const clientesUI: Cliente[] = clientesResponse.map((client) => ({
          id: client.id,
          nome: client.clientName,
          email: client.email || "",
          telefone: client.phoneNumber || "",
          selecionado: pendingSelectedClientIdsRef.current.includes(client.id),
        }));

        setClientes(clientesUI);
      } catch (error) {
        showError("Não foi possível carregar a lista de clientes");
        console.error("Erro ao carregar clientes:", error);
        setClientes([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchClientes();
  }, []);

  useEffect(() => {
    const authStatus = searchParams?.get("auth");
    const authMessage = searchParams?.get("message");

    if (authStatus === "success") {
      showSuccess(
        "Autenticação com o Gmail realizada com sucesso! Você já pode enviar o email novamente.",
      );
    } else if (authStatus === "error") {
      showError(`Erro na autenticação: ${authMessage || "Erro desconhecido"}`);
    }
  }, [searchParams]);

  const filteredClientes = clientes.filter(
    (cliente) =>
      cliente.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cliente.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cliente.telefone.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const clienteElegivel = useCallback(
    (cliente: Cliente) => {
      if (canaisEnvio.email && !cliente.email) return false;
      if (canaisEnvio.whatsapp && !cliente.telefone) return false;
      return true;
    },
    [canaisEnvio],
  );

  useEffect(() => {
    setClientes((prev) =>
      prev.map((c) =>
        c.selecionado && !clienteElegivel(c) ? { ...c, selecionado: false } : c,
      ),
    );
  }, [clienteElegivel]);

  const toggleCliente = (id: number) => {
    setClientes(
      clientes.map((c) =>
        c.id === id && clienteElegivel(c)
          ? { ...c, selecionado: !c.selecionado }
          : c,
      ),
    );
  };

  const toggleTodos = () => {
    const elegiveisFiltrados = filteredClientes.filter(clienteElegivel);
    const todosAtivos = elegiveisFiltrados.every((c) => c.selecionado);
    setClientes(
      clientes.map((c) => ({
        ...c,
        selecionado: elegiveisFiltrados.some((fc) => fc.id === c.id)
          ? !todosAtivos
          : c.selecionado,
      })),
    );
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const newFiles = Array.from(e.target.files);
      const maxSize = 10 * 1024 * 1024;

      const arquivosInvalidos = newFiles.filter((file) => file.size > maxSize);
      if (arquivosInvalidos.length > 0) {
        showError(
          `${arquivosInvalidos.length} arquivo(s) excede(m) o tamanho máximo de 10MB`,
        );
        return;
      }

      setArquivos((prev) => [...prev, ...newFiles]);
      showSuccess(`${newFiles.length} arquivo(s) adicionado(s) com sucesso`);

      e.target.value = "";
    }
  };

  const removerArquivo = (index: number) => {
    setArquivos((prev) => prev.filter((_, i) => i !== index));
    showSuccess("Arquivo removido");
  };

  const removerTodosArquivos = () => {
    setArquivos([]);
    showSuccess("Todos os arquivos foram removidos");
  };

  const { enviando, handleEnviar } = useBulkNotificationSend({
    arquivos,
    canaisEnvio,
    clientes,
    tipoDestinatario,
    mensagemPersonalizada,
    cardValue,
    cashValue,
    onSuccess: () => {
      setArquivos([]);
      setClientes(clientes.map((c) => ({ ...c, selecionado: false })));
      setCanaisEnvio({ email: true, whatsapp: false });

      setCardValue(0);
      setCashValue(0);

      const mesAnoPadrao = getMesAnoAnteriorPadrao();
      const periodoPadrao = getPeriodoPadrao();
      setTipoReferencia("periodo");
      setMesReferencia(mesAnoPadrao.mes);
      setAnoReferencia(mesAnoPadrao.ano);
      setDataInicialReferencia(periodoPadrao.dataInicial);
      setDataFinalReferencia(periodoPadrao.dataFinal);
      setTextoEditadoManualmente(false);
      setMensagemPersonalizada(
        gerarTextoReferencia(
          "periodo",
          mesAnoPadrao.mes,
          mesAnoPadrao.ano,
          periodoPadrao.dataInicial,
          periodoPadrao.dataFinal,
        ),
      );
    },
  });

  const clientesSelecionados = clientes.filter((c) => c.selecionado).length;
  const elegiveisFiltrados = filteredClientes.filter(clienteElegivel);
  const todosAtivos =
    elegiveisFiltrados.length > 0 &&
    elegiveisFiltrados.every((c) => c.selecionado);

  if (environment === "hml") {
    return (
      <div className="p-4 md:p-6 min-h-full flex items-center justify-center bg-[var(--neutral-50)]">
        <div className="max-w-md text-center bg-white rounded-lg shadow-sm border border-[var(--neutral-200)] p-8">
          <ShieldAlert className="mx-auto mb-4 text-amber-500" size={40} />
          <h1 className="text-xl font-bold text-[var(--neutral-900)] mb-2">
            Indisponível em homologação
          </h1>
          <p className="text-[var(--neutral-600)]">
            O envio de notificações usa e-mail/WhatsApp reais e está
            desabilitado neste ambiente para evitar contato acidental com
            clientes de verdade.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 space-y-6 bg-[var(--neutral-50)] min-h-full">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl md:text-3xl font-bold text-[var(--neutral-900)]">
          Módulo Notificações
        </h1>
        <p className="text-[var(--neutral-600)]">
          Envie documentos e comunicados para clientes e contabilidade
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <Card title="Envio de Documentos">
            <div className="space-y-6">
              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Destinatário
                </div>
                <div
                  className={`grid gap-3 ${isGestor ? "grid-cols-2" : "grid-cols-1"}`}
                >
                  <button
                    type="button"
                    onClick={() => setTipoDestinatario("clientes")}
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                      tipoDestinatario === "clientes"
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <Users className="w-5 h-5" />
                    <span className="font-medium">Clientes</span>
                  </button>
                  <RoleGuard roles="MANAGER" ignoreRedirect>
                    <button
                      type="button"
                      onClick={() => setTipoDestinatario("contabilidade")}
                      className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                        tipoDestinatario === "contabilidade"
                          ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                          : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                      }`}
                    >
                      <Building2 className="w-5 h-5" />
                      <span className="font-medium">Contabilidade</span>
                    </button>
                  </RoleGuard>
                </div>
              </div>

              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Canal de Envio
                </div>
                <div className="flex items-center gap-2 p-3 rounded-lg border-2 border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)] w-fit">
                  <Mail className="w-5 h-5" />
                  <span className="font-medium">E-mail</span>
                  <CheckCircle2 className="w-4 h-4" />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="block text-sm font-medium text-[var(--neutral-700)]">
                    Arquivos (PDF, imagens e pacotes — Máx. 10MB cada)
                  </div>
                  {arquivos.length > 0 && (
                    <span className="text-sm text-[var(--primary)] font-medium">
                      {arquivos.length} arquivo(s)
                    </span>
                  )}
                </div>

                <label className="flex flex-col items-center justify-center w-full h-24 border-2 border-dashed border-[var(--neutral-300)] rounded-lg cursor-pointer hover:border-[var(--primary)] hover:bg-[var(--primary-bg)] transition-all mb-3">
                  <div className="flex flex-col items-center justify-center">
                    <Upload className="w-6 h-6 text-[var(--neutral-500)] mb-1" />
                    <p className="text-sm text-[var(--neutral-600)]">
                      <span className="font-semibold">
                        Clique para adicionar
                      </span>{" "}
                      ou arraste arquivos
                    </p>
                  </div>
                  <input
                    type="file"
                    className="hidden"
                    onChange={handleFileChange}
                    multiple
                  />
                </label>

                {arquivos.length > 0 && (
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {arquivos.map((arquivo, index) => (
                      <div
                        key={`${arquivo.name}-${arquivo.size}-${arquivo.lastModified}`}
                        className="flex items-center justify-between p-3 bg-[var(--primary-bg)] border border-[var(--primary)] rounded-lg"
                      >
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          <FileText className="w-6 h-6 text-[var(--primary)] flex-shrink-0" />
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-[var(--neutral-900)] truncate">
                              {arquivo.name}
                            </p>
                            <p className="text-sm text-[var(--neutral-600)]">
                              {(arquivo.size / 1024).toFixed(2)} KB
                            </p>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => removerArquivo(index)}
                          className="p-1 hover:bg-red-100 rounded-full transition-colors flex-shrink-0 cursor-pointer"
                          title="Remover arquivo"
                        >
                          <X className="w-5 h-5 text-[var(--secondary)]" />
                        </button>
                      </div>
                    ))}
                    {arquivos.length > 1 && (
                      <button
                        type="button"
                        onClick={removerTodosArquivos}
                        className="w-full py-2 text-sm text-[var(--secondary)] hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors font-medium cursor-pointer"
                      >
                        Remover todos os arquivos
                      </button>
                    )}
                  </div>
                )}
              </div>

              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Tipo de Referência
                </div>
                <div className="grid grid-cols-2 gap-3 mb-4">
                  <button
                    type="button"
                    onClick={() => setTipoReferencia("mes")}
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                      tipoReferencia === "mes"
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <span className="font-medium">Mês</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setTipoReferencia("periodo")}
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                      tipoReferencia === "periodo"
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <span className="font-medium">Período</span>
                  </button>
                </div>

                {tipoReferencia === "mes" ? (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label
                        htmlFor="mesReferencia"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Mês
                      </label>
                      <select
                        id="mesReferencia"
                        value={mesReferencia}
                        onChange={(e) =>
                          setMesReferencia(Number(e.target.value))
                        }
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      >
                        {MESES_PT_BR.map((nome, index) => (
                          <option key={nome} value={index + 1}>
                            {nome.replace(/^\w/, (c) => c.toUpperCase())}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label
                        htmlFor="anoReferencia"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Ano
                      </label>
                      <input
                        id="anoReferencia"
                        type="number"
                        min={2000}
                        max={2100}
                        value={anoReferencia}
                        onChange={(e) =>
                          setAnoReferencia(Number(e.target.value))
                        }
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label
                        htmlFor="dataInicialReferencia"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Data Inicial
                      </label>
                      <input
                        id="dataInicialReferencia"
                        type="date"
                        value={dataInicialReferencia}
                        onChange={(e) =>
                          setDataInicialReferencia(e.target.value)
                        }
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="dataFinalReferencia"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Data Final
                      </label>
                      <input
                        id="dataFinalReferencia"
                        type="date"
                        value={dataFinalReferencia}
                        onChange={(e) => setDataFinalReferencia(e.target.value)}
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>
                  </div>
                )}
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label
                    htmlFor="mensagem"
                    className="block text-sm font-medium text-[var(--neutral-700)]"
                  >
                    Mensagem Personalizada (opcional)
                  </label>
                  {textoEditadoManualmente && (
                    <button
                      type="button"
                      onClick={handleRestaurarTextoPadrao}
                      className="flex items-center gap-1 text-sm text-[var(--primary)] hover:text-[var(--primary-dark)] font-medium cursor-pointer"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      Restaurar texto padrão
                    </button>
                  )}
                </div>
                <textarea
                  id="mensagem"
                  rows={4}
                  placeholder="Digite uma mensagem para enviar junto com o arquivo..."
                  value={mensagemPersonalizada}
                  onChange={(e) => {
                    setMensagemPersonalizada(e.target.value);
                    setTextoEditadoManualmente(true);
                  }}
                  className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent resize-none"
                />
              </div>

              {tipoDestinatario === "contabilidade" && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold text-[var(--neutral-900)]">
                    Valores Financeiros
                  </h3>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label
                        htmlFor="cartao"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Valor de Cartão (R$)
                      </label>
                      <MaskedDecimalInput
                        id="cartao"
                        value={cardValue}
                        onChange={setCardValue}
                        placeholder="0,00"
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                      <p className="text-xs text-[var(--neutral-500)] mt-1">
                        Apenas 40% deste valor será informado no e-mail
                      </p>
                    </div>

                    <div>
                      <label
                        htmlFor="dinheiro"
                        className="block text-sm font-medium text-[var(--neutral-700)] mb-2"
                      >
                        Valor em Dinheiro (R$)
                      </label>
                      <MaskedThousandsInput
                        id="dinheiro"
                        value={cashValue}
                        onChange={setCashValue}
                        placeholder="0,00"
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                      <p className="text-xs text-[var(--neutral-500)] mt-1">
                        Digite apenas os milhares — ex.: "5" vira R$ 5.000,00
                      </p>
                    </div>
                  </div>
                </div>
              )}

              <Button
                variant="primary"
                fullWidth
                size="lg"
                icon={<Send className="w-5 h-5" />}
                onClick={handleEnviar}
                disabled={enviando}
              >
                {(() => {
                  if (enviando) return "Enviando...";
                  const canais = [];
                  if (canaisEnvio.email) canais.push("E-mail");
                  if (canaisEnvio.whatsapp) canais.push("WhatsApp");
                  const canaisTexto =
                    canais.length > 0 ? canais.join(" e ") : "Notificação";
                  const contador =
                    tipoDestinatario === "clientes" && clientesSelecionados > 0
                      ? ` (${clientesSelecionados})`
                      : "";
                  return `Enviar ${canaisTexto}${contador}`;
                })()}
              </Button>
            </div>
          </Card>
        </div>

        <div className="space-y-4">
          {tipoDestinatario === "clientes" ? (
            <Card title={`Selecionar Clientes (${clientesSelecionados})`}>
              <div className="space-y-4">
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Buscar cliente..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                  />
                  <Users className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-[var(--neutral-400)]" />
                </div>

                {filteredClientes.length > 0 && (
                  <button
                    type="button"
                    onClick={toggleTodos}
                    className="text-sm text-[var(--primary)] hover:text-[var(--primary-dark)] font-medium cursor-pointer"
                  >
                    {todosAtivos ? "Desselecionar" : "Selecionar"} todos
                  </button>
                )}

                <div className="space-y-2 max-h-[400px] overflow-y-auto">
                  {(() => {
                    if (isLoading) {
                      return (
                        <p className="text-center text-[var(--neutral-500)] py-4">
                          Carregando clientes...
                        </p>
                      );
                    }
                    if (filteredClientes.length === 0) {
                      return (
                        <p className="text-center text-[var(--neutral-500)] py-4">
                          Nenhum cliente encontrado
                        </p>
                      );
                    }
                    return filteredClientes.map((cliente) => {
                      const elegivel = clienteElegivel(cliente);
                      return (
                        <label
                          key={cliente.id}
                          title={
                            elegivel
                              ? undefined
                              : "Cliente sem o contato necessário para o canal selecionado"
                          }
                          className={`flex items-start gap-3 p-3 rounded-lg border transition-all ${
                            !elegivel
                              ? "cursor-not-allowed opacity-50 border-[var(--neutral-200)] bg-[var(--neutral-50)]"
                              : "cursor-pointer"
                          } ${
                            cliente.selecionado
                              ? "border-[var(--primary)] bg-[var(--primary-bg)]"
                              : "border-[var(--neutral-200)] hover:border-[var(--neutral-300)] bg-white"
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={cliente.selecionado}
                            disabled={!elegivel}
                            onChange={() => toggleCliente(cliente.id)}
                            className="mt-1 w-4 h-4 text-[var(--primary)] border-[var(--neutral-300)] rounded focus:ring-[var(--primary)] disabled:cursor-not-allowed"
                          />
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-[var(--neutral-900)] truncate">
                              {cliente.nome}
                            </p>
                            <div className="space-y-1">
                              {canaisEnvio.email &&
                                (cliente.email ? (
                                  <p className="text-sm text-[var(--neutral-600)] truncate flex items-center gap-1">
                                    <Mail className="w-3 h-3" />
                                    {cliente.email}
                                  </p>
                                ) : (
                                  <p className="text-xs text-[var(--secondary)] flex items-center gap-1">
                                    <Mail className="w-3 h-3" />
                                    Sem e-mail cadastrado
                                  </p>
                                ))}
                              {canaisEnvio.whatsapp &&
                                (cliente.telefone ? (
                                  <p className="text-sm text-[var(--neutral-600)] truncate flex items-center gap-1">
                                    <MessageCircle className="w-3 h-3" />
                                    {cliente.telefone}
                                  </p>
                                ) : (
                                  <p className="text-xs text-[var(--secondary)] flex items-center gap-1">
                                    <MessageCircle className="w-3 h-3" />
                                    Sem telefone cadastrado
                                  </p>
                                ))}
                            </div>
                          </div>
                          {cliente.selecionado && (
                            <CheckCircle2 className="w-5 h-5 text-[var(--primary)] flex-shrink-0" />
                          )}
                        </label>
                      );
                    });
                  })()}
                </div>
              </div>
            </Card>
          ) : (
            <Card title="Destinatário">
              <div className="space-y-3">
                <div className="p-4 bg-[var(--primary-bg)] border border-[var(--primary)] rounded-lg">
                  <div className="flex items-center gap-3 mb-2">
                    <Building2 className="w-6 h-6 text-[var(--primary)]" />
                    <p className="font-semibold text-[var(--neutral-900)]">
                      Contabilidade Hortifruti
                    </p>
                  </div>
                  <div className="space-y-1">
                    {canaisEnvio.email &&
                      contabilidadeEmails.map((email) => (
                        <p
                          key={email}
                          className="text-sm text-[var(--neutral-600)] flex items-center gap-2"
                        >
                          <Mail className="w-4 h-4" />
                          {email}
                        </p>
                      ))}
                    {canaisEnvio.whatsapp && (
                      <p className="text-sm text-[var(--neutral-600)] flex items-center gap-2">
                        <MessageCircle className="w-4 h-4" />
                        (31) 98765-4321
                      </p>
                    )}
                  </div>
                </div>
                <p className="text-xs text-[var(--neutral-500)]">
                  Os documentos serão enviados automaticamente para o escritório
                  de contabilidade
                </p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
