"use client";

import { toast } from "react-toastify";

export interface ApiError {
  message: string;
  statusCode?: number;
  details?: string[];
}

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

/**
 * Toast de erro com um link clicável de ação (ex: reautorizar uma integração externa),
 * para o usuário não precisar ir atrás do link nos logs do servidor.
 */
export const showErrorWithLink = (message: string, url: string) => {
  toast.error(
    <div>
      <p style={{ margin: 0 }}>{message}</p>
      <a
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        style={{
          color: "#2563eb",
          textDecoration: "underline",
          fontWeight: 600,
        }}
      >
        Clique aqui para autorizar
      </a>
    </div>,
    {
      position: "top-right",
      autoClose: false,
      hideProgressBar: false,
      closeOnClick: false,
      pauseOnHover: true,
      draggable: true,
    },
  );
};
