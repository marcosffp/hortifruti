"use client";

import { useState } from "react";
import {
  type BulkNotificationRequest,
  bulkNotificationService,
} from "@/services/bulkNotificationService";
import type { Cliente, TipoDestinatario } from "@/types/notificacoesTypes";
import { clearDraft } from "@/utils/notificationDraft";
import { validarFormulario } from "@/utils/notificationValidation";
import { showError, showErrorWithLink, showSuccess } from "@/utils/toastUtils";

interface UseBulkNotificationSendParams {
  arquivos: File[];
  canaisEnvio: { email: boolean; whatsapp: boolean };
  clientes: Cliente[];
  tipoDestinatario: TipoDestinatario;
  mensagemPersonalizada: string;
  cardValue: number;
  cashValue: number;
  onSuccess: () => void;
}

export function useBulkNotificationSend({
  arquivos,
  canaisEnvio,
  clientes,
  tipoDestinatario,
  mensagemPersonalizada,
  cardValue,
  cashValue,
  onSuccess,
}: UseBulkNotificationSendParams) {
  const [enviando, setEnviando] = useState(false);

  const handleEnviar = async () => {
    if (!validarFormulario(arquivos, canaisEnvio, tipoDestinatario, clientes)) {
      return;
    }

    try {
      setEnviando(true);

      const clientesSelecionados = clientes.filter((c) => c.selecionado);
      const clientIds =
        tipoDestinatario === "clientes"
          ? clientesSelecionados.map((c) => c.id)
          : [];

      const channels: string[] = [];
      if (canaisEnvio.email) channels.push("email");
      if (canaisEnvio.whatsapp) channels.push("whatsapp");

      const requestData: BulkNotificationRequest = {
        files: arquivos,
        clientIds,
        channels,
        destinationType: tipoDestinatario,
        customMessage: mensagemPersonalizada || undefined,
      };

      if (tipoDestinatario === "contabilidade") {
        if (cardValue > 0) requestData.cardValue = cardValue.toFixed(2);
        if (cashValue > 0) requestData.cashValue = cashValue.toFixed(2);
      }

      const response =
        await bulkNotificationService.sendBulkNotifications(requestData);

      if (response.success) {
        showSuccess(response.message);
        clearDraft();
        onSuccess();
      } else if (response.authorizationUrl) {
        showErrorWithLink(response.message, response.authorizationUrl);
      } else {
        showError(response.message);

        if (response.failedRecipients && response.failedRecipients.length > 0) {
          const failedList = response.failedRecipients.join(", ");
          showError(`Falha ao enviar para: ${failedList}`);
        }
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Erro desconhecido";
      showError(`Erro ao enviar notificação: ${errorMessage}`);
      console.error("Erro ao enviar:", error);
    } finally {
      setEnviando(false);
    }
  };

  return { enviando, handleEnviar };
}
