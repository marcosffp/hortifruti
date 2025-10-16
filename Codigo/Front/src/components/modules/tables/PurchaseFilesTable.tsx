import { useEffect, useState } from "react";
import SkeletonTableLoading from "@/components/ui/SkeletonTableLoading";
import { showError, showSuccess } from "@/services/notificationService";
import { purchaseService } from "@/services/purchaseService";
import { PurchaseType } from "@/types/purchaseType";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import { Trash } from "lucide-react";

interface PurchaseFilesTableProps {
    clientId: number | undefined;
    refreshKey?: number;
}

export default function PurchaseFilesTable({ clientId, refreshKey }: PurchaseFilesTableProps) {
    const [files, setFiles] = useState<PurchaseType[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [confirmDeleteModal, setConfirmDeleteModal] = useState({ state: false, fileId: -1 });

    const columnLabels: Record<string, string> = {
        id: "ID",
        purchaseDate: "Data de Compra",
        total: "Valor Total",
        actions: "Ações",
    };

    useEffect(() => {
        if (!clientId) return;

        const loadFiles = async () => {
            setIsLoading(true);
            setError(null);
            purchaseService.fetchPurchaseFiles(clientId, page)
                .then((response) => {
                    setFiles(response.content);
                    setTotalPages(response.totalPages);
                })
                .catch((err) => {
                    console.error(err);
                    setError("Erro ao carregar arquivos de compra.");
                })
                .finally(() => {
                    setIsLoading(false);
                });
        };

        loadFiles();
    }, [clientId, refreshKey, page]);

    const handleDelete = (fileId: number) => {
        purchaseService.deletePurchaseFile(fileId)
            .then((res) => {
                setFiles((prev) => prev.filter((file) => file.id !== fileId));
                showSuccess(res.message);
                refreshKey && refreshKey++; // Trigger refresh in parent
            })
            .catch((err) => {
                console.error(err);
                showError("Erro ao deletar arquivo de compra.");
            });
    };

    if (isLoading) {
        return <SkeletonTableLoading title="Arquivos de Compra" cols={Object.values(columnLabels)} />;
    }

    if (error) {
        return (
            <div>
                <div className="bg-red-100 border border-red-300 rounded-lg p-4 text-center mt-6">
                    {error}
                </div>
            </div>
        );
    }

    return (
        <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Arquivos de Compra</h2>
            <table className="min-w-full border border-gray-200">
                <thead>
                    <tr>
                        {Object.entries(columnLabels).map(([col, label]) => (
                            <th key={col} className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">
                                {label}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {files.map((file, idx) => (
                        <tr key={file.id || idx} className="hover:bg-gray-50">
                            {Object.keys(columnLabels).map((col) => {
                                if (col === "actions") {
                                    return (
                                        <td key={col} className="px-4 py-2 border-b border-gray-200">
                                            <button
                                                title="Cancelar agrupamento"
                                                className="text-red-500 hover:text-red-800 transition-colors"
                                                onClick={() => setConfirmDeleteModal({ state: true, fileId: file.id })}
                                            >
                                                <Trash></Trash>
                                            </button>
                                        </td>
                                    );
                                }

                                let value = (file as any)[col];
                                if (col === "purchaseDate") {
                                    value = new Date(value).toLocaleDateString();
                                } else if (col === "total") {
                                    value = value.toLocaleString("pt-BR", {
                                        style: "currency",
                                        currency: "BRL",
                                    });
                                }
                                return (
                                    <td key={col} className="px-4 py-2 border-b border-gray-200">{value}</td>
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="flex justify-center items-center gap-2 mt-4">
                <button
                    className="px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
                    onClick={() => setPage((p) => Math.max(p - 1, 0))}
                    disabled={page === 0}
                >
                    Anterior
                </button>
                <span className="text-sm text-gray-700">
                    Página {page + 1} de {totalPages}
                </span>
                <button
                    className="px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
                    onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                    disabled={page >= totalPages - 1}
                >
                    Próxima
                </button>
            </div>
            <ConfirmDeleteModal
                open={confirmDeleteModal.state}
                onClose={() => setConfirmDeleteModal({ state: false, fileId: -1 })}
                onConfirm={() => {
                    handleDelete(confirmDeleteModal.fileId);
                    setConfirmDeleteModal({ state: false, fileId: -1 });
                }}
            />
        </div>
    );
}