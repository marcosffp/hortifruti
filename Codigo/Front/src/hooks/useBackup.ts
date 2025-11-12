"use client";

import { useCallback, useState } from "react";
import { backupService } from "@/services/backupService";

export function useBackup() {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [lastMessage, setLastMessage] = useState<string | null>(null);
    const [authUrl, setAuthUrl] = useState<string | null>(null);
    const [storage, setStorage] = useState<{
        current: number; 
        max: number; 
        raw: string; 
        percentage: number
    } | null>(null);

    const refreshStorage = useCallback(async () => {
        setError(null);
        setIsLoading(true);
        try {
            const s = await backupService.getStorage();
            setStorage(s);
        } catch (e: any) {
            setError(e.message || "Erro ao obter armazenamento");
        } finally {
            setIsLoading(false);
        }
    }, []);

    const runBackup = useCallback(
        async (startDate?: string, endDate?: string) => {
            setIsLoading(true);
            setError(null);
            setLastMessage(null);
            setAuthUrl(null);

            try {
                const res = await backupService.startBackup(startDate, endDate);
                setLastMessage(res.message);
                if (res.requiresAuth && res.authUrl) {
                    setAuthUrl(res.authUrl);
                } else {
                    // Atualiza status de armazenamento após concluir backup
                    refreshStorage();
                }
            } catch (e: any) {
                setError(e.message || "Erro ao iniciar backup");
            } finally {
                setIsLoading(false);
            }
        },
        [refreshStorage]
    );

    return {
        isLoading,
        error,
        storage,
        lastMessage,
        authUrl,
        refreshStorage,
        runBackup,
        setAuthUrl,
        setLastMessage,
    };
}