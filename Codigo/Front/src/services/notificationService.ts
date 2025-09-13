"use client";

import { toast } from "react-toastify";

// Interface para tipos de erro
export interface ApiError {
  message: string;
  statusCode?: number;
  details?: string[];
}

// Função para lidar com erros da API
export const handleApiError = async (error: unknown): Promise<ApiError> => {
  console.error("API Error:", error);

  if (error instanceof Response) {
    try {
      const errorData = await error.json();
      return {
        message: errorData.message || "Erro desconhecido no servidor",
        statusCode: error.status,
        details: errorData.details || [],
      };
    } catch (_parseError) {
      // Erro ao parsear o JSON da resposta
      return {
        message: `Erro ${error.status}: ${error.statusText}`,
        statusCode: error.status,
      };
    }
  }

  if (error instanceof Error) {
    return {
      message: error.message || "Ocorreu um erro inesperado",
    };
  }

  return {
    message: "Erro desconhecido ao processar a requisição",
  };
};

// Funções para mostrar notificações
export const showSuccess = (message: string) => {
  toast.success(message, {
    position: "top-right",
    autoClose: 3000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
  });
};

export const showError = (message: string) => {
  toast.error(message, {
    position: "top-right",
    autoClose: 5000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
  });
};

export const showInfo = (message: string) => {
  toast.info(message, {
    position: "top-right",
    autoClose: 3000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
  });
};
