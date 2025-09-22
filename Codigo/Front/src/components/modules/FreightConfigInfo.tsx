"use client";

import { useEffect, useState } from 'react';
import { Car, User, DollarSign, Edit } from 'lucide-react';
import FreightConfigModal from './FreightConfigsModal';
import { FreightConfigDTO } from '@/types/freightTYpe';

// Componente auxiliar para os itens de informação
const InfoItem = ({ label, value, unit }: { label: string; value: number; unit: string }) => (
    <div className="text-sm">
        <span className="text-gray-500">{label}: </span>
        <span className="font-medium text-gray-800">{value?.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0,00'} {unit}</span>
    </div>
);

export default function FreightConfigInfo() {
    const [config, setConfig] = useState<FreightConfigDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);

    useEffect(() => {
        // Simula a chamada GET para /freight-config
        const fetchConfig = async () => {
            setIsLoading(true);
            try {
                // TODO: Substituir pela chamada real à API
                // const data = await freightPropertiesService.getFreightConfig();
                const mockData: FreightConfigDTO = {
                    kmPerLiterConsumption: 10, fuelPrice: 5.89, maintenanceCostPerKm: 0.15,
                    tireCostPerKm: 0.08, depreciationCostPerKm: 0.20, insuranceCostPerKm: 0.05,
                    baseSalary: 2500, chargesPercentage: 40, monthlyHoursWorked: 220,
                    administrativeCostsPercentage: 15, marginPercentage: 20, fixedFee: 2.50
                };
                await new Promise(resolve => setTimeout(resolve, 1000)); // Simula delay de rede
                setConfig(mockData);
            } catch (error) {
                console.error("Falha ao buscar configurações de frete:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchConfig();
    }, []);

    const handleModalClose = () => {
        setIsModalOpen(false);
        // TODO: Opcional - Recarregar os dados após fechar o modal para refletir as mudanças
    };

    if (isLoading) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                {/* Skeleton Loaders */}
                {[...Array(3)].map((_, i) => (
                    <div key={i} className="bg-gray-200 rounded-lg p-4 animate-pulse h-28"></div>
                ))}
            </div>
        );
    }

    if (!config) {
        return (
            <div className="text-center p-4 bg-red-100 border border-red-300 rounded-lg mb-8">
                <p className="text-red-700">Não foi possível carregar as configurações de frete.</p>
            </div>
        );
    }

    return (
        <>
            <div className="flex justify-end mb-4">
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="cursor-pointer text-sm bg-gray-100 text-gray-700 px-3 py-1.5 rounded-md hover:bg-gray-200 transition-colors flex items-center justify-center"
                >
                    <Edit size={14} className="mr-2" />
                    Editar Configurações
                </button>
            </div>
            {/* Cards de Informação */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
                {/* Card Veículo */}
                <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
                    <h3 className="font-semibold text-gray-800 flex items-center mb-2">
                        <Car size={18} className="mr-2 text-green-600" />
                        Custos do Veículo
                    </h3>
                    <div className="space-y-1">
                        <InfoItem label="Consumo" value={config.kmPerLiterConsumption} unit="Km/L" />
                        <InfoItem label="Combustível" value={config.fuelPrice} unit="R$/L" />
                        <InfoItem label="Manutenção" value={config.maintenanceCostPerKm} unit="R$/Km" />
                    </div>
                </div>

                {/* Card Entregador */}
                <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
                    <h3 className="font-semibold text-gray-800 flex items-center mb-2">
                        <User size={18} className="mr-2 text-green-600" />
                        Custos do Entregador
                    </h3>
                    <div className="space-y-1">
                        <InfoItem label="Salário Base" value={config.baseSalary} unit="R$" />
                        <InfoItem label="Encargos" value={config.chargesPercentage} unit="%" />
                        <InfoItem label="Horas/Mês" value={config.monthlyHoursWorked} unit="h" />
                    </div>
                </div>

                {/* Card Margens e Ações */}
                <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-200 flex flex-col justify-between">
                    <div>
                        <h3 className="font-semibold text-gray-800 flex items-center mb-2">
                            <DollarSign size={18} className="mr-2 text-green-600" />
                            Margens e Taxas
                        </h3>
                        <div className="space-y-1">
                            <InfoItem label="Margem" value={config.marginPercentage} unit="%" />
                            <InfoItem label="Taxa Fixa" value={config.fixedFee} unit="R$" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Modal de Configuração */}
            <FreightConfigModal
                isOpen={isModalOpen}
                onClose={handleModalClose}
                initialData={config}
            />
        </>
    );
}
