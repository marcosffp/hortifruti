"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";

interface GameLoadingOverlayProps {
  isOpen: boolean;
  title?: string;
  messages?: string[];
}

const DEFAULT_MESSAGES = [
  "Conectando aos servidores...",
  "Lendo lançamentos financeiros...",
  "Organizando os dados...",
  "Quase lá...",
];

/**
 * Progresso "falso" no estilo tela de carregamento de jogo: acelera no início,
 * desacelera perto do teto (já que o backend não expõe progresso real) e só
 * corre para 100% quando a operação de fato termina. O ritmo é lento de
 * propósito, para acompanhar operações longas (ex.: exportação de relatórios)
 * sem chegar perto do teto rápido demais e ficar parado esperando.
 */
function nextFakeProgress(prev: number): number {
  if (prev >= 90) return prev;
  let amount: number;
  if (prev < 20) amount = Math.random() * 1.5 + 0.5;
  else if (prev < 50) amount = Math.random() * 1 + 0.3;
  else if (prev < 75) amount = Math.random() * 0.6 + 0.15;
  else amount = Math.random() * 0.25 + 0.05;
  return Math.min(90, prev + amount);
}

export default function GameLoadingOverlay({
  isOpen,
  title = "Processando...",
  messages = DEFAULT_MESSAGES,
}: GameLoadingOverlayProps) {
  const [progress, setProgress] = useState(0);
  const [visible, setVisible] = useState(false);
  const [messageIndex, setMessageIndex] = useState(0);
  const wasOpenRef = useRef(false);

  useEffect(() => {
    if (isOpen) {
      wasOpenRef.current = true;
      setVisible(true);
      setProgress(0);
      setMessageIndex(0);

      const progressTimer = setInterval(() => {
        setProgress((prev) => nextFakeProgress(prev));
      }, 350);

      const messageTimer = setInterval(() => {
        setMessageIndex((prev) => (prev + 1) % messages.length);
      }, 2200);

      return () => {
        clearInterval(progressTimer);
        clearInterval(messageTimer);
      };
    }

    if (wasOpenRef.current) {
      wasOpenRef.current = false;
      setProgress(100);
      const hideTimer = setTimeout(() => setVisible(false), 600);
      return () => clearTimeout(hideTimer);
    }
  }, [isOpen, messages.length]);

  if (!visible) return null;

  const isDone = progress >= 100;

  return (
    <div
      role="alert"
      aria-live="polite"
      className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/70 backdrop-blur-sm animate-fadeIn p-4"
    >
      <div className="relative w-full max-w-sm overflow-hidden rounded-2xl border border-[var(--primary-light)]/30 bg-white p-6 shadow-2xl animate-slideUp">
        <div className="pointer-events-none absolute -top-16 -right-16 h-40 w-40 rounded-full bg-[var(--primary-light)]/20 blur-2xl" />
        <div className="pointer-events-none absolute -bottom-16 -left-16 h-40 w-40 rounded-full bg-[var(--primary)]/20 blur-2xl" />

        <div className="relative flex flex-col items-center gap-4 text-center">
          <div className="relative flex h-16 w-16 items-center justify-center">
            <span
              className={`absolute inset-0 rounded-full bg-[var(--primary-light)]/30 ${
                isDone ? "" : "animate-ping"
              }`}
            />
            <Image
              src="/icon.png"
              alt="Logo Hortifruti"
              width={56}
              height={56}
              className="relative rounded-full border-2 border-[var(--primary)] bg-white"
            />
          </div>

          <div>
            <p
              className="font-semibold text-gray-800"
              style={{ fontFamily: "var(--font-title)" }}
            >
              {title}
            </p>
            <p className="mt-1 h-4 text-xs text-gray-500">
              {isDone ? "Concluído!" : messages[messageIndex]}
            </p>
          </div>

          <div className="w-full">
            <div className="relative h-4 w-full overflow-hidden rounded-full border border-gray-200 bg-gray-100">
              <div
                className="relative h-full rounded-full bg-gradient-to-r from-[var(--primary-dark)] via-[var(--primary)] to-[var(--primary-light)] transition-[width] duration-300 ease-out"
                style={{ width: `${progress}%` }}
              >
                <div className="absolute inset-0 animate-loading-stripes bg-[length:1rem_1rem] bg-[linear-gradient(45deg,rgba(255,255,255,0.35)_25%,transparent_25%,transparent_50%,rgba(255,255,255,0.35)_50%,rgba(255,255,255,0.35)_75%,transparent_75%,transparent)]" />
              </div>
            </div>
            <p className="mt-2 text-sm font-semibold tabular-nums text-[var(--primary-dark)]">
              {Math.round(progress)}%
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
