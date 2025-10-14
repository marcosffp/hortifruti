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
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [mensagemPersonalizada, setMensagemPersonalizada] = useState("");
  const [canaisEnvio, setCanaisEnvio] = useState<{
    email: boolean;
    whatsapp: boolean;
  }>({ email: false, whatsapp: false });
  const [tipoDestinatario, setTipoDestinatario] = useState<TipoDestinatario>("clientes");
  const [dataVencimento, setDataVencimento] = useState("");
  const [valorBoleto, setValorBoleto] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Estatísticas
  const [stats, setStats] = useState({
    enviadosHoje: 24,
    whatsappEnviados: 156,
    alertasVencimento: 8,
    dadosContador: 12,
  });

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

  // Upload de arquivo
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const maxSize = 10 * 1024 * 1024; // 10MB

      if (file.size > maxSize) {
        showError("O arquivo deve ter no máximo 10MB");
        return;
      }

      setArquivo(file);
      showSuccess("Arquivo carregado com sucesso");
    }
  };

  // Remover arquivo
  const removerArquivo = () => {
    setArquivo(null);
  };

  // Validações básicas
  const validarArquivoECanais = () => {
    if (!arquivo) {
      showError("Por favor, selecione um arquivo para enviar");
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

      // Simular envio (aqui você implementaria a chamada real para o backend)
      await new Promise((resolve) => setTimeout(resolve, 2000));

      const clientesSelecionados = clientes.filter((c) => c.selecionado);
      const qtdDestinatarios =
        tipoDestinatario === "clientes"
          ? clientesSelecionados.length
          : 1;

      const canais = [];
      if (canaisEnvio.email) canais.push("e-mail");
      if (canaisEnvio.whatsapp) canais.push("WhatsApp");
      const canaisTexto = canais.join(" e ");

      showSuccess(
        `Notificação enviada com sucesso para ${qtdDestinatarios} destinatário(s) via ${canaisTexto}!`
      );

      // Limpar formulário
      setArquivo(null);
      setMensagemPersonalizada("");
      setDataVencimento("");
      setValorBoleto("");
      setClientes(clientes.map((c) => ({ ...c, selecionado: false })));
      setCanaisEnvio({ email: false, whatsapp: false });

      // Atualizar estatísticas
      const incrementoWhatsapp = canaisEnvio.whatsapp ? qtdDestinatarios : 0;
      setStats((prev) => ({
        ...prev,
        enviadosHoje: prev.enviadosHoje + qtdDestinatarios,
        whatsappEnviados: prev.whatsappEnviados + incrementoWhatsapp,
        dadosContador:
          tipoDestinatario === "contabilidade"
            ? prev.dadosContador + 1
            : prev.dadosContador,
      }));
    } catch (error) {
      showError("Erro ao enviar notificação. Tente novamente.");
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

      {/* Cards de Estatísticas */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-[var(--neutral-600)]">Enviados Hoje</p>
              <p className="text-2xl font-bold text-[var(--primary)]">
                {stats.enviadosHoje}
              </p>
            </div>
            <div className="p-3 bg-[var(--primary-bg)] rounded-lg">
              <Mail className="w-6 h-6 text-[var(--primary)]" />
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-[var(--neutral-600)]">WhatsApp/E-mail</p>
              <p className="text-2xl font-bold text-[var(--primary)]">
                {stats.whatsappEnviados}
              </p>
            </div>
            <div className="p-3 bg-[var(--primary-bg)] rounded-lg">
              <MessageCircle className="w-6 h-6 text-[var(--primary)]" />
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-[var(--neutral-600)]">Alertas Vencimento</p>
              <p className="text-2xl font-bold text-[var(--warning)]">
                {stats.alertasVencimento}
              </p>
            </div>
            <div className="p-3 bg-orange-50 rounded-lg">
              <Calendar className="w-6 h-6 text-[var(--warning)]" />
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-[var(--neutral-600)]">Dados p/ Contador</p>
              <p className="text-2xl font-bold text-[var(--info)]">
                {stats.dadosContador}
              </p>
            </div>
            <div className="p-3 bg-blue-50 rounded-lg">
              <Building2 className="w-6 h-6 text-[var(--info)]" />
            </div>
          </div>
        </Card>
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
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all ${
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
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all ${
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
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all ${
                      canaisEnvio.email
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <Mail className="w-5 h-5" />
                    <span className="font-medium">E-mail</span>
                    {canaisEnvio.email && <CheckCircle2 className="w-4 h-4 ml-auto" />}
                  </button>
                  <button
                    type="button"
                    onClick={() => setCanaisEnvio((prev) => ({ ...prev, whatsapp: !prev.whatsapp }))}
                    className={`flex items-center justify-center gap-2 p-3 rounded-lg border-2 transition-all ${
                      canaisEnvio.whatsapp
                        ? "border-[var(--primary)] bg-[var(--primary-bg)] text-[var(--primary)]"
                        : "border-[var(--neutral-300)] bg-white text-[var(--neutral-600)] hover:border-[var(--neutral-400)]"
                    }`}
                  >
                    <MessageCircle className="w-5 h-5" />
                    <span className="font-medium">WhatsApp</span>
                    {canaisEnvio.whatsapp && <CheckCircle2 className="w-4 h-4 ml-auto" />}
                  </button>
                </div>
              </div>

              {/* Upload de Arquivo */}
              <div>
                <div className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                  Arquivo (PDF, JPG, PNG - Máx. 10MB)
                </div>
                {!arquivo ? (
                  <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-[var(--neutral-300)] rounded-lg cursor-pointer hover:border-[var(--primary)] hover:bg-[var(--primary-bg)] transition-all">
                    <div className="flex flex-col items-center justify-center pt-5 pb-6">
                      <Upload className="w-8 h-8 text-[var(--neutral-500)] mb-2" />
                      <p className="text-sm text-[var(--neutral-600)]">
                        <span className="font-semibold">Clique para enviar</span> ou
                        arraste o arquivo
                      </p>
                    </div>
                    <input
                      type="file"
                      className="hidden"
                      accept=".pdf,.jpg,.jpeg,.png"
                      onChange={handleFileChange}
                    />
                  </label>
                ) : (
                  <div className="flex items-center justify-between p-4 bg-[var(--primary-bg)] border border-[var(--primary)] rounded-lg">
                    <div className="flex items-center gap-3">
                      <FileText className="w-8 h-8 text-[var(--primary)]" />
                      <div>
                        <p className="font-medium text-[var(--neutral-900)]">
                          {arquivo.name}
                        </p>
                        <p className="text-sm text-[var(--neutral-600)]">
                          {(arquivo.size / 1024).toFixed(2)} KB
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={removerArquivo}
                      className="p-1 hover:bg-red-100 rounded-full transition-colors"
                    >
                      <X className="w-5 h-5 text-[var(--secondary)]" />
                    </button>
                  </div>
                )}
              </div>

              {/* Informações do Boleto (opcional) */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="dataVencimento" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                    Data de Vencimento (opcional)
                  </label>
                  <input
                    id="dataVencimento"
                    type="date"
                    value={dataVencimento}
                    onChange={(e) => setDataVencimento(e.target.value)}
                    className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                  />
                </div>
                <div>
                  <label htmlFor="valorBoleto" className="block text-sm font-medium text-[var(--neutral-700)] mb-2">
                    Valor do Boleto (opcional)
                  </label>
                  <input
                    id="valorBoleto"
                    type="text"
                    placeholder="R$ 0,00"
                    value={valorBoleto}
                    onChange={(e) => setValorBoleto(e.target.value)}
                    className="w-full px-3 py-2 border border-[var(--neutral-300)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--primary)] focus:border-transparent"
                  />
                </div>
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
                    className="text-sm text-[var(--primary)] hover:text-[var(--primary-dark)] font-medium"
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
                        contabilidade@hortifruti.com.br
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

      {/* Atividades Recentes */}
      <Card title="Atividades Recentes">
        <div className="space-y-3">
          <div className="flex items-start gap-4 p-3 bg-[var(--neutral-50)] rounded-lg">
            <div className="p-2 bg-green-100 rounded-full">
              <Mail className="w-5 h-5 text-green-600" />
            </div>
            <div className="flex-1">
              <p className="font-medium text-[var(--neutral-900)]">Boleto enviado</p>
              <p className="text-sm text-[var(--neutral-600)]">
                João Silva Gomes • 2024-10-15
              </p>
            </div>
            <span className="text-xs text-green-600 bg-green-100 px-2 py-1 rounded">
              enviado
            </span>
          </div>

          <div className="flex items-start gap-4 p-3 bg-[var(--neutral-50)] rounded-lg">
            <div className="p-2 bg-green-100 rounded-full">
              <MessageCircle className="w-5 h-5 text-green-600" />
            </div>
            <div className="flex-1">
              <p className="font-medium text-[var(--neutral-900)]">
                Alerta de vencimento
              </p>
              <p className="text-sm text-[var(--neutral-600)]">
                Maria Oliveira • 2024-09-14
              </p>
            </div>
            <span className="text-xs text-green-600 bg-green-100 px-2 py-1 rounded">
              whatsapp
            </span>
          </div>

          <div className="flex items-start gap-4 p-3 bg-[var(--neutral-50)] rounded-lg">
            <div className="p-2 bg-blue-100 rounded-full">
              <FileText className="w-5 h-5 text-blue-600" />
            </div>
            <div className="flex-1">
              <p className="font-medium text-[var(--neutral-900)]">
                Dados enviados
              </p>
              <p className="text-sm text-[var(--neutral-600)]">
                Contador • Silva Associados • 2024-09-14
              </p>
            </div>
            <span className="text-xs text-blue-600 bg-blue-100 px-2 py-1 rounded">
              enviado
            </span>
          </div>
        </div>
      </Card>
    </div>
  );
}
