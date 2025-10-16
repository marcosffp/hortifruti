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
      const formData = new FormData();

      // Adicionar arquivos
      request.files.forEach((file) => {
        formData.append("files", file);
      });

      // Adicionar IDs dos clientes (cada ID separadamente)
      request.clientIds.forEach((id) => {
        formData.append("clientIds", id.toString());
      });

      // Adicionar canais (cada canal separadamente)
      request.channels.forEach((channel) => {
        formData.append("channels", channel);
      });

      // Adicionar tipo de destinatário
      formData.append("destinationType", request.destinationType);

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

      // Obter token de autenticação
      const token = authService.getToken();
      const headers: HeadersInit = {};
      
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
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

      const data: BulkNotificationResponse = await response.json();
      return data;
    } catch (error) {
      console.error("Falha ao enviar notificações:", error);
      throw error;
    }
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
