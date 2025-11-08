"use client";

import RoleGuard from "@/components/auth/RoleGuard";
import { useBackup } from "@/hooks/useBackup";
import { useEffect, useState } from "react";
import { DatabaseBackup, DatabaseZap, Link as LinkIcon, RefreshCcw } from "lucide-react";

export default function BackupPage() {
  const { isLoading, error, storage, lastMessage, authUrl, refreshStorage, runBackup, setAuthUrl, setLastMessage } =
    useBackup();

  const [startDate, setStartDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 7);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState<string>(() => new Date().toISOString().slice(0, 10));

  useEffect(() => {
    refreshStorage();
  }, [refreshStorage]);

  const onBackupPeriod = () => runBackup(startDate, endDate);

  return (
    <RoleGuard roles="MANAGER">
      <div className="flex-1 p-6 sm:p-8 w-full max-w-6xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Backup do Sistema</h1>

        {/* Status de armazenamento */}
        <div className="bg-white rounded-lg shadow-md p-5 mb-6">
          <div className="flex flex-wrap gap-3 items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <DatabaseZap className="w-5 h-5 text-gray-600" />
              <h2 className="text-lg font-semibold text-gray-800">Armazenamento do Banco</h2>
            </div>
            <button
              onClick={refreshStorage}
              className="inline-flex items-center gap-2 px-3 py-1.5 text-sm rounded-md border border-gray-300 hover:bg-gray-50 disabled:opacity-50"
              disabled={isLoading}
              title="Atualizar"
            >
              <RefreshCcw className="w-4 h-4" />
              Atualizar
            </button>
          </div>

          <div className="w-full">
            <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-500 transition-all"
                style={{ width: `${storage?.percentage ?? 0}%` }}
              />
            </div>
            <div className="mt-2 text-sm text-gray-700">
              {storage ? (
                <span>
                  Uso: {storage.current.toFixed(2)} / {storage.max.toFixed(0)} MB ({storage.percentage}%)
                </span>
              ) : (
                <span>Carregando...</span>
              )}
            </div>
          </div>
        </div>

        {/* Ações de Backup */}
        <div className="bg-white rounded-lg shadow-md p-5">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Executar Backup</h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-sm text-gray-700 mb-1">Data inicial</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-1">Data final</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-3">
            <button
              onClick={onBackupPeriod}
              disabled={isLoading}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-md bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
            >
              <DatabaseBackup className="w-4 h-4" />
              Backup do período
            </button>

            {authUrl && (
              <a
                href={authUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-md border border-blue-300 text-blue-700 hover:bg-blue-50"
                onClick={() => {
                  setLastMessage(null);
                }}
              >
                <LinkIcon className="w-4 h-4" />
                Autorizar no Google Drive
              </a>
            )}
          </div>

          <div className="mt-4 space-y-2">
            {error && (
              <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>
            )}
            {lastMessage && !authUrl && (
              <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded p-2">
                {lastMessage}
              </div>
            )}
            {authUrl && (
              <div className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded p-2">
                É necessária autorização. Conclua o login na janela aberta e depois execute o backup novamente.
              </div>
            )}
          </div>
        </div>
      </div>
    </RoleGuard>
  );
}