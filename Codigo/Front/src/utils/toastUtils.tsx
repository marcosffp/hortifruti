"use client";

import { toast } from "react-toastify";

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
