"use client";

import React, { useState } from "react";
import { X } from "lucide-react";

interface AdditionalDataModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: (dadosAdicionais: string) => void;
    scoreNumber: string | number;
}

export default function AdditionalDataModal({ 
    isOpen, 
    onClose, 
    onConfirm, 
    scoreNumber 
}: AdditionalDataModalProps) {
    const [dadosAdicionais, setDadosAdicionais] = useState("");

    if (!isOpen) return null;

    const handleConfirm = () => {
        onConfirm(dadosAdicionais);
        setDadosAdicionais("");
    };

    const handleClose = () => {
        setDadosAdicionais("");
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                <div className="flex justify-between items-center p-6 border-b border-gray-300">
                    <h3 className="text-xl font-semibold">Dados Adicionais da NF</h3>
                    <button
                        onClick={handleClose}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <div className="p-6">
                    <div className="mb-4">
                        <p className="text-sm text-gray-600 mb-2">
                            Agrupamento: <strong>{scoreNumber}</strong>
                        </p>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Numerações dos Pedidos
                            </label>
                            <textarea
                                value={dadosAdicionais}
                                onChange={(e) => setDadosAdicionais(e.target.value)}
                                placeholder="Digite as numerações dos pedidos deste período da combinação..."
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[100px] resize-vertical"
                                rows={4}
                            />
                            <p className="text-xs text-gray-500 mt-1">
                                Informe as numerações dos pedidos que fazem parte deste agrupamento.
                                Este campo aparecerá nas informações adicionais da nota fiscal.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
                    <button
                        onClick={handleClose}
                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
                    >
                        Cancelar
                    </button>
                    <button
                        onClick={handleConfirm}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        Gerar Nota Fiscal
                    </button>
                </div>
            </div>
        </div>
    );
}