"use client";

import { useState } from "react";
import { getAuthHeaders } from "@/utils/httpUtils";
import { API_BASE_URL } from "@/config/api";

export function useClient() {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const getClientById = async (clientId: number) => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await fetch(`${API_BASE_URL}/clients/${clientId}`, {
                method: "GET",
                headers: getAuthHeaders(),
                credentials: "include",
            });

            if (!response.ok) {
                throw new Error(`Erro ao buscar cliente: ${response.status}`);
            }

            const result = await response.json();
            return result;
        } catch (err: any) {
            setError(err.message || "Erro ao buscar cliente");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { getClientById, isLoading, error };
}