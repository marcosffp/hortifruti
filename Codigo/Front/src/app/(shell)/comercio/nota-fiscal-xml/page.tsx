'use client';

import { useState } from "react";
import { Download, FileX, Search } from "lucide-react";
import { fiscalNoteXmlStorageService } from "@/services/fiscalNoteXmlStorageService";
import { FiscalNoteXmlStorageResponse } from "@/types/fiscalNoteXmlStorageType";
import { showError } from "@/services/notificationService";

export default function FiscalNoteXmlPage() {
    const [startDate, setStartDate] = useState(() => {
        const now = new Date();
        return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split("T")[0];
    });
    const [endDate, setEndDate] = useState(() => {
        return new Date().toISOString().split("T")[0];
    });
    const [records, setRecords] = useState<FiscalNoteXmlStorageResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);
    const [downloadingRef, setDownloadingRef] = useState<string | null>(null);

    async function handleSearch() {
        setLoading(true);
        setSearched(true);
        try {
            const data = await fiscalNoteXmlStorageService.getByPeriod(startDate, endDate);
            setRecords(data);
        } catch {
            showError("Erro ao buscar XMLs. Tente novamente.");
            setRecords([]);
        } finally {
            setLoading(false);
        }
    }

    async function handleDownload(ref: string, nfNumber: string) {
        setDownloadingRef(ref);
        try {
            await fiscalNoteXmlStorageService.downloadXml(ref, nfNumber);
        } catch {
            showError("Erro ao baixar o XML. Tente novamente.");
        } finally {
            setDownloadingRef(null);
        }
    }

    function formatDate(dateStr: string) {
        const [year, month, day] = dateStr.split("-");
        return `${day}/${month}/${year}`;
    }

    function formatCurrency(value: number) {
        return value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    return (
        <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800">Consulta de XMLs de Nota Fiscal</h1>
                <p className="text-gray-600 mt-1">
                    Consulte os XMLs das notas fiscais emitidas salvos no banco. Selecione o período desejado.
                </p>
            </div>

            {/* Filtro de período */}
            <div className="bg-white rounded-lg shadow-sm p-5 border border-gray-200 mb-6">
                <div className="flex flex-wrap gap-4 items-end">
                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-medium text-gray-700">Data inicial</label>
                        <input
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-medium text-gray-700">Data final</label>
                        <input
                            type="date"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <button
                        onClick={handleSearch}
                        disabled={loading}
                        className="flex items-center gap-2 px-5 py-2 bg-blue-800 text-white rounded-lg hover:bg-blue-900 transition-colors font-medium text-sm disabled:opacity-60"
                    >
                        <Search className="w-4 h-4" />
                        {loading ? "Buscando..." : "Buscar"}
                    </button>
                </div>
            </div>

            {/* Tabela de resultados */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 flex-1">
                {!searched ? (
                    <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                        <Search className="w-12 h-12 mb-3 opacity-40" />
                        <p className="text-base">Selecione o período e clique em Buscar.</p>
                    </div>
                ) : loading ? (
                    <div className="flex items-center justify-center py-20 text-gray-400">
                        <p className="text-base">Carregando...</p>
                    </div>
                ) : records.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                        <FileX className="w-12 h-12 mb-3 opacity-40" />
                        <p className="text-base">Nenhum XML encontrado para o período selecionado.</p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <div className="px-5 py-3 border-b border-gray-100 text-sm text-gray-500">
                            {records.length} {records.length === 1 ? "nota fiscal encontrada" : "notas fiscais encontradas"}
                        </div>
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-gray-100 bg-gray-50">
                                    <th className="text-left px-5 py-3 font-semibold text-gray-700">Número NF</th>
                                    <th className="text-left px-5 py-3 font-semibold text-gray-700">Cliente</th>
                                    <th className="text-left px-5 py-3 font-semibold text-gray-700">Data de Emissão</th>
                                    <th className="text-right px-5 py-3 font-semibold text-gray-700">Valor Total</th>
                                    <th className="text-center px-5 py-3 font-semibold text-gray-700">XML</th>
                                </tr>
                            </thead>
                            <tbody>
                                {records.map((record) => (
                                    <tr
                                        key={record.ref}
                                        className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                                    >
                                        <td className="px-5 py-3 font-mono font-medium text-blue-800">
                                            {record.nfNumber}
                                        </td>
                                        <td className="px-5 py-3 text-gray-700">{record.clientName}</td>
                                        <td className="px-5 py-3 text-gray-600">
                                            {formatDate(record.issuedAt)}
                                        </td>
                                        <td className="px-5 py-3 text-right text-gray-700 font-medium">
                                            R$ {formatCurrency(record.totalValue)}
                                        </td>
                                        <td className="px-5 py-3 text-center">
                                            <button
                                                onClick={() => handleDownload(record.ref, record.nfNumber)}
                                                disabled={downloadingRef === record.ref}
                                                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors text-xs font-medium disabled:opacity-60"
                                            >
                                                <Download className="w-3.5 h-3.5" />
                                                {downloadingRef === record.ref ? "Baixando..." : "Baixar XML"}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </main>
    );
}
