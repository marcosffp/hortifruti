"use client";

import { authService } from "@/services/authService";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface BulkNotificationRequest {
  files: File[];
  clientIds: number[];
  channels: string[]; // ["email", "whatsapp"]
  destinationType: string; // "clientes" ou "contabilidade"
  customMessage?: string;
  // Campos financeiros para contabilidade
  creditValue?: string;
  debitValue?: string;
  cashValue?: string;
}

export interface BulkNotificationResponse {
  success: boolean;
  message: string;
  totalSent: number;
  totalFailed: number;
  failedRecipients: string[];
}

export const bulkNotificationService = {
  /**
   * Envia notificações em massa para múltiplos destinatários
   */
  async sendBulkNotifications(
    request: BulkNotificationRequest
  ): Promise<BulkNotificationResponse> {
    try {
      // Obter token de autenticação
      const token = authService.getToken();
      const headers: HeadersInit = {};
      
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      // Se for para CONTABILIDADE - usar endpoint accounting/generic-files
      if (request.destinationType === "contabilidade") {
        return await this.sendToAccounting(request, headers);
      } 
      // Se for para CLIENTES - usar endpoint client/documents para cada cliente
      else if (request.destinationType === "clientes") {
        return await this.sendToClients(request, headers);
      }
      
      throw new Error("Tipo de destinatário inválido");
      
    } catch (error) {
      console.error("Falha ao enviar notificações:", error);
      throw error;
    }
  },

  /**
   * Envia para contabilidade (apenas email)
   */
  async sendToAccounting(
    request: BulkNotificationRequest,
    headers: HeadersInit
  ): Promise<BulkNotificationResponse> {
    const formData = new FormData();

    // Adicionar arquivos
    request.files.forEach((file) => {
      formData.append("files", file);
    });

    // Adicionar mensagem personalizada (se houver)
    if (request.customMessage) {
      formData.append("customMessage", request.customMessage);
    }

    // Adicionar campos financeiros para contabilidade (se houver)
    if (request.creditValue) {
      formData.append("creditValue", request.creditValue);
    }
    if (request.debitValue) {
      formData.append("debitValue", request.debitValue);
    }
    if (request.cashValue) {
      formData.append("cashValue", request.cashValue);
    }

    // NÃO definir Content-Type - o browser define automaticamente com boundary correto
    const response = await fetch(
      `${API_BASE_URL}/api/notifications/accounting/generic-files`,
      {
        method: "POST",
        headers,
        body: formData,
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.message || `Erro ao enviar notificações: ${response.status}`
      );
    }

    const data = await response.json();
    return {
      success: data.success,
      message: data.message || "Notificação enviada",
      totalSent: data.success ? 1 : 0,
      totalFailed: data.success ? 0 : 1,
      failedRecipients: data.success ? [] : ["Contabilidade"],
    };
  },

  /**
   * Envia para clientes (email e/ou WhatsApp)
   */
  async sendToClients(
    request: BulkNotificationRequest,
    headers: HeadersInit
  ): Promise<BulkNotificationResponse> {
    const results = {
      totalSent: 0,
      totalFailed: 0,
      failedRecipients: [] as string[],
    };

    // Determinar o canal correto baseado nos canais selecionados
    let channel = "EMAIL";
    if (request.channels.includes("email") && request.channels.includes("whatsapp")) {
      channel = "BOTH";
    } else if (request.channels.includes("whatsapp")) {
      channel = "WHATSAPP";
    }

    // Enviar para cada cliente
    for (const clientId of request.clientIds) {
      try {
        const formData = new FormData();

        // Adicionar arquivos
        request.files.forEach((file) => {
          formData.append("files", file);
        });

        // Adicionar dados do cliente
        formData.append("clientId", clientId.toString());
        formData.append("channel", channel);

        // Adicionar mensagem personalizada (se houver)
        if (request.customMessage) {
          formData.append("customMessage", request.customMessage);
        }

        const response = await fetch(
          `${API_BASE_URL}/api/notifications/client/documents`,
          {
            method: "POST",
            headers,
            body: formData,
          }
        );

        if (!response.ok) {
          const errorData = await response.json().catch(() => null);
          results.totalFailed++;
          results.failedRecipients.push(`Cliente ID: ${clientId}`);
          console.error(`Erro ao enviar para cliente ${clientId}:`, errorData);
        } else {
          results.totalSent++;
        }
      } catch (error) {
        results.totalFailed++;
        results.failedRecipients.push(`Cliente ID: ${clientId}`);
        console.error(`Erro ao enviar para cliente ${clientId}:`, error);
      }
    }

    const success = results.totalSent > 0;
    const message = success
      ? `${results.totalSent} notificação(ões) enviada(s) com sucesso${
          results.totalFailed > 0 ? `, ${results.totalFailed} falhou(aram)` : ""
        }`
      : "Falha ao enviar todas as notificações";

    return {
      success,
      message,
      totalSent: results.totalSent,
      totalFailed: results.totalFailed,
      failedRecipients: results.failedRecipients,
    };
  },

  /**
   * Testa se o serviço de notificações está ativo
   */
  async testService(): Promise<boolean> {
    try {
      const token = authService.getToken();
      const headers: HeadersInit = {
        "Content-Type": "application/json",
      };
      
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }
      
      const response = await fetch(
        `${API_BASE_URL}/api/notifications/test`,
        {
          method: "GET",
          headers,
        }
      );

      return response.ok;
    } catch (error) {
      console.error("Falha ao testar serviço de notificações:", error);
      return false;
    }
  },
};
