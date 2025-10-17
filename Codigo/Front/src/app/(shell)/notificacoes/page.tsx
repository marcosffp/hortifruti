"use client";

import { useState, useEffect } from "react";
import {
  Mail,
  MessageCircle,
  Upload,
  Send,
  Users,
  Building2,
  X,
  FileText,
  Calendar,
  CheckCircle2,
} from "lucide-react";
import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import { clientService } from "@/services/clientService";
import { showError, showSuccess } from "@/services/notificationService";
import { bulkNotificationService, BulkNotificationRequest } from "@/services/bulkNotificationService";

interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  selecionado: boolean;
}

type CanalEnvio = "email" | "whatsapp";
type TipoDestinatario = "clientes" | "contabilidade";

export default function NotificacoesPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [arquivos, setArquivos] = useState<File[]>([]);
  const [mensagemPersonalizada, setMensagemPersonalizada] = useState("");
  const [canaisEnvio, setCanaisEnvio] = useState<{
    email: boolean;
    whatsapp: boolean;
  }>({ email: false, whatsapp: false });
  const [tipoDestinatario, setTipoDestinatario] = useState<TipoDestinatario>("clientes");
  const [dataVencimento, setDataVencimento] = useState("");
  const [valorBoleto, setValorBoleto] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Estados específicos para contabilidade
  const [debitValue, setDebitValue] = useState("");
  const [creditValue, setCreditValue] = useState("");
  const [cashValue, setCashValue] = useState("");

  // Efeito para ajustar canais de envio quando muda o tipo de destinatário
  useEffect(() => {
    if (tipoDestinatario === "contabilidade") {
      // Para contabilidade, apenas email
      setCanaisEnvio({ email: true, whatsapp: false });
    } else {
      // Para clientes, permitir escolha
      setCanaisEnvio({ email: false, whatsapp: false });
    }
  }, [tipoDestinatario]);

  // Carregar clientes
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
          selecionado: false,
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

  // Filtrar clientes
  const filteredClientes = clientes.filter((cliente) =>
    cliente.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cliente.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cliente.telefone.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Selecionar/desselecionar cliente
  const toggleCliente = (id: number) => {
    setClientes(
      clientes.map((c) =>
        c.id === id ? { ...c, selecionado: !c.selecionado } : c
      )
    );
  };

  // Selecionar todos
  const toggleTodos = () => {
    const todosAtivos = filteredClientes.every((c) => c.selecionado);
    setClientes(
      clientes.map((c) => ({
        ...c,
        selecionado: filteredClientes.some((fc) => fc.id === c.id)
          ? !todosAtivos
          : c.selecionado,
      }))
    );
  };

  // Upload de arquivos
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const newFiles = Array.from(e.target.files);
      const maxSize = 10 * 1024 * 1024; // 10MB por arquivo

      // Validar tamanho de cada arquivo
      const arquivosInvalidos = newFiles.filter(file => file.size > maxSize);
      if (arquivosInvalidos.length > 0) {
        showError(`${arquivosInvalidos.length} arquivo(s) excede(m) o tamanho máximo de 10MB`);
        return;
      }

      setArquivos(prev => [...prev, ...newFiles]);
      showSuccess(`${newFiles.length} arquivo(s) adicionado(s) com sucesso`);
      
      // Limpar o input para permitir adicionar o mesmo arquivo novamente
      e.target.value = '';
    }
  };

  // Remover arquivo específico
  const removerArquivo = (index: number) => {
    setArquivos(prev => prev.filter((_, i) => i !== index));
    showSuccess("Arquivo removido");
  };

  // Remover todos os arquivos
  const removerTodosArquivos = () => {
    setArquivos([]);
    showSuccess("Todos os arquivos foram removidos");
  };

  // Validações básicas
  const validarArquivoECanais = () => {
    if (arquivos.length === 0) {
      showError("Por favor, selecione pelo menos um arquivo para enviar");
      return false;
    }
    if (!canaisEnvio.email && !canaisEnvio.whatsapp) {
      showError("Por favor, selecione pelo menos um canal de envio");
      return false;
    }
    return true;
  };

  // Validar clientes selecionados
  const validarClientes = () => {
    const clientesSelecionados = clientes.filter((c) => c.selecionado);
    if (clientesSelecionados.length === 0) {
      showError("Por favor, selecione pelo menos um cliente");
      return false;
    }

    if (canaisEnvio.email) {
      const clientesSemEmail = clientes
        .filter((c) => c.selecionado && !c.email)
        .map((c) => c.nome);
      if (clientesSemEmail.length > 0) {
        showError(
          `Os seguintes clientes não possuem e-mail cadastrado: ${clientesSemEmail.join(", ")}`
        );
        return false;
      }
    }

    if (canaisEnvio.whatsapp) {
      const clientesSemTelefone = clientes
        .filter((c) => c.selecionado && !c.telefone)
        .map((c) => c.nome);
      if (clientesSemTelefone.length > 0) {
        showError(
          `Os seguintes clientes não possuem telefone cadastrado: ${clientesSemTelefone.join(", ")}`
        );
        return false;
      }
    }

    return true;
  };

  // Validações
  const validarFormulario = () => {
    if (!validarArquivoECanais()) return false;
    if (tipoDestinatario === "clientes" && !validarClientes()) return false;
    return true;
  };

  // Enviar notificação
  const handleEnviar = async () => {
    if (!validarFormulario()) {
      return;
    }

    try {
      setEnviando(true);

      // Preparar dados para envio
      const clientesSelecionados = clientes.filter((c) => c.selecionado);
      const clientIds = tipoDestinatario === "clientes" 
        ? clientesSelecionados.map((c) => c.id)
        : []; // Vazio para contabilidade

      const channels: string[] = [];
      if (canaisEnvio.email) channels.push("email");
      if (canaisEnvio.whatsapp) channels.push("whatsapp");

      // Chamar serviço de notificação
      const requestData: BulkNotificationRequest = {
        files: arquivos,
        clientIds,
        channels,
        destinationType: tipoDestinatario,
        customMessage: mensagemPersonalizada || undefined,
      };

      // Adicionar campos financeiros se for contabilidade
      if (tipoDestinatario === "contabilidade") {
        if (creditValue) requestData.creditValue = creditValue;
        if (debitValue) requestData.debitValue = debitValue;
        if (cashValue) requestData.cashValue = cashValue;
      }

      const response = await bulkNotificationService.sendBulkNotifications(requestData);

      if (response.success) {
        showSuccess(response.message);

        // Limpar formulário
        setArquivos([]);
        setMensagemPersonalizada("");
        setDataVencimento("");
        setValorBoleto("");
        setClientes(clientes.map((c) => ({ ...c, selecionado: false })));
        setCanaisEnvio({ email: false, whatsapp: false });
        
        // Limpar campos financeiros
        setCreditValue("");
        setDebitValue("");
        setCashValue("");

      } else {
        showError(response.message);
        
        // Mostrar falhas específicas se houver
        if (response.failedRecipients && response.failedRecipients.length > 0) {
          const failedList = response.failedRecipients.join(", ");
          showError(`Falha ao enviar para: ${failedList}`);
        }
      }

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Erro desconhecido";
      showError(`Erro ao enviar notificação: ${errorMessage}`);
      console.error("Erro ao enviar:", error);
    } finally {
      setEnviando(false);
    }
  };

  const clientesSelecionados = clientes.filter((c) => c.selecionado).length;
  const todosAtivos = filteredClientes.length > 0 && filteredClientes.every((c) => c.selecionado);

  return (
    <div className="p-4 md:p-6 space-y-6 bg-[var(--neutral-50)] min-h-full">
      {/* Header */}
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl md:text-3xl font-bold text-[var(--neutral-900)]">
          Módulo Notificações
        </h1>
        <p className="text-[var(--neutral-600)]">
          Envie documentos e comunicados para clientes e contabilidade
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Formulário de Envio */}
        <div className="lg:col-span-2 space-y-4">
          <Card title="Envio de Documentos">
            <div className="space-y-6">
              {/* Tipo de Destinatário */}
              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Destinatário
                </div>
                <div className="grid grid-cols-2 gap-3">
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
                </div>
              </div>

              {/* Canal de Envio */}
              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Canal de Envio (selecione um ou ambos)
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setCanaisEnvio((prev) => ({ ...prev, email: !prev.email }))}
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                      canaisEnvio.email
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <Mail className="w-5 h-5" />
                    <span className="font-medium">E-mail</span>
                    {canaisEnvio.email && <CheckCircle2 className="w-4 h-4 ml-auto" />}
                  </button>
                  {tipoDestinatario !== "contabilidade" && (
                    <button
                      type="button"
                      onClick={() => setCanaisEnvio((prev) => ({ ...prev, whatsapp: !prev.whatsapp }))}
                      className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all cursor-pointer ${
                        canaisEnvio.whatsapp
                          ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                          : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                      }`}
                    >
                      <MessageCircle className="w-5 h-5" />
                      <span className="font-medium">WhatsApp</span>
                      {canaisEnvio.whatsapp && <CheckCircle2 className="w-4 h-4 ml-auto" />}
                    </button>
                  )}
                </div>
              </div>

              {/* Upload de Arquivos */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="block text-sm font-medium text-[var(--neutral-700)]">
                    Arquivos (PDF, JPG, PNG - Máx. 10MB cada)
                  </div>
                  {arquivos.length > 0 && (
                    <span className="text-sm text-[var(--primary)] font-medium">
                      {arquivos.length} arquivo(s)
                    </span>
                  )}
                </div>

                {/* Área de Upload */}
                <label className="flex flex-col items-center justify-center w-full h-24 border-2 border-dashed border-[var(--neutral-300)] rounded-lg cursor-pointer hover:border-[var(--primary)] hover:bg-[var(--primary-bg)] transition-all mb-3">
                  <div className="flex flex-col items-center justify-center">
                    <Upload className="w-6 h-6 text-[var(--neutral-500)] mb-1" />
                    <p className="text-sm text-[var(--neutral-600)]">
                      <span className="font-semibold">Clique para adicionar</span> ou arraste arquivos
                    </p>
                  </div>
                  <input
                    type="file"
                    className="hidden"
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={handleFileChange}
                    multiple
                  />
                </label>

                {/* Lista de Arquivos */}
                {arquivos.length > 0 && (
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {arquivos.map((arquivo, index) => (
                      <div
                        key={`${arquivo.name}-${index}`}
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

              {/* Mensagem Personalizada */}
              <div>
                <label htmlFor="mensagem" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Mensagem Personalizada (opcional)
                </label>
                <textarea
                  id="mensagem"
                  rows={4}
                  placeholder="Digite uma mensagem para enviar junto com o arquivo..."
                  value={mensagemPersonalizada}
                  onChange={(e) => setMensagemPersonalizada(e.target.value)}
                  className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent resize-none"
                />
              </div>

              {/* Campos específicos para Contabilidade */}
              {tipoDestinatario === "contabilidade" && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold text-[var(--neutral-900)]">Valores Financeiros</h3>
                  
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* Campo Crédito */}
                    <div>
                      <label htmlFor="credito" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                        Valor de Crédito (R$)
                      </label>
                      <input
                        type="number"
                        id="credito"
                        step="0.01"
                        min="0"
                        placeholder="0,00"
                        value={creditValue}
                        onChange={(e) => setCreditValue(e.target.value)}
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>

                    {/* Campo Débito */}
                    <div>
                      <label htmlFor="debito" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                        Valor de Débito (R$)
                      </label>
                      <input
                        type="number"
                        id="debito"
                        step="0.01"
                        min="0"
                        placeholder="0,00"
                        value={debitValue}
                        onChange={(e) => setDebitValue(e.target.value)}
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>

                    {/* Campo Dinheiro */}
                    <div>
                      <label htmlFor="dinheiro" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                        Valor em Dinheiro (R$)
                      </label>
                      <input
                        type="number"
                        id="dinheiro"
                        step="0.01"
                        min="0"
                        placeholder="0,00"
                        value={cashValue}
                        onChange={(e) => setCashValue(e.target.value)}
                        className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                      />
                    </div>
                  </div>
                </div>
              )}

              {/* Botão de Enviar */}
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
                  const canaisTexto = canais.length > 0 ? canais.join(" e ") : "Notificação";
                  const contador = tipoDestinatario === "clientes" && clientesSelecionados > 0
                    ? ` (${clientesSelecionados})`
                    : "";
                  return `Enviar ${canaisTexto}${contador}`;
                })()}
              </Button>
            </div>
          </Card>
        </div>

        {/* Seleção de Clientes */}
        <div className="space-y-4">
          {tipoDestinatario === "clientes" ? (
            <Card title={`Selecionar Clientes (${clientesSelecionados})`}>
              <div className="space-y-4">
                {/* Busca */}
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

                {/* Selecionar Todos */}
                {filteredClientes.length > 0 && (
                  <button
                    type="button"
                    onClick={toggleTodos}
                    className="text-sm text-[var(--primary)] hover:text-[var(--primary-dark)] font-medium cursor-pointer"
                  >
                    {todosAtivos ? "Desselecionar" : "Selecionar"} todos
                  </button>
                )}

                {/* Lista de Clientes */}
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
                    return filteredClientes.map((cliente) => (
                      <label
                        key={cliente.id}
                        className={`flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-all ${
                          cliente.selecionado
                            ? "border-[var(--primary)] bg-[var(--primary-bg)]"
                            : "border-[var(--neutral-200)] hover:border-[var(--neutral-300)] bg-white"
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={cliente.selecionado}
                          onChange={() => toggleCliente(cliente.id)}
                          className="mt-1 w-4 h-4 text-[var(--primary)] border-[var(--neutral-300)] rounded focus:ring-[var(--primary)]"
                        />
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-[var(--neutral-900)] truncate">
                            {cliente.nome}
                          </p>
                          <div className="space-y-1">
                            {canaisEnvio.email && (
                              cliente.email ? (
                                <p className="text-sm text-[var(--neutral-600)] truncate flex items-center gap-1">
                                  <Mail className="w-3 h-3" />
                                  {cliente.email}
                                </p>
                              ) : (
                                <p className="text-xs text-[var(--secondary)] flex items-center gap-1">
                                  <Mail className="w-3 h-3" />
                                  Sem e-mail cadastrado
                                </p>
                              )
                            )}
                            {canaisEnvio.whatsapp && (
                              cliente.telefone ? (
                                <p className="text-sm text-[var(--neutral-600)] truncate flex items-center gap-1">
                                  <MessageCircle className="w-3 h-3" />
                                  {cliente.telefone}
                                </p>
                              ) : (
                                <p className="text-xs text-[var(--secondary)] flex items-center gap-1">
                                  <MessageCircle className="w-3 h-3" />
                                  Sem telefone cadastrado
                                </p>
                              )
                            )}
                          </div>
                        </div>
                        {cliente.selecionado && (
                          <CheckCircle2 className="w-5 h-5 text-[var(--primary)] flex-shrink-0" />
                        )}
                      </label>
                    ));
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
                    {canaisEnvio.email && (
                      <p className="text-sm text-[var(--neutral-600)] flex items-center gap-2">
                        <Mail className="w-4 h-4" />
                        carlosdybala.fig@gmail.com
                      </p>
                    )}
                    {canaisEnvio.whatsapp && (
                      <p className="text-sm text-[var(--neutral-600)] flex items-center gap-2">
                        <MessageCircle className="w-4 h-4" />
                        (31) 98765-4321
                      </p>
                    )}
                  </div>
                </div>
                <p className="text-xs text-[var(--neutral-500)]">
                  Os documentos serão enviados automaticamente para o escritório de
                  contabilidade
                </p>
              </div>
            </Card>
          )}
        </div>
      </div>

    </div>
  );
}
