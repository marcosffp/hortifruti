"use client";

import { Calculator, Star, Truck } from 'lucide-react';
import { SetStateAction, useState } from 'react';
import AddressAutocomplete from '@/components/modules/AddressAutocomplete';
import MapComponent from '@/components/modules/Map';
import FavoritesModal from '@/components/modals/FavoritesModal';
import { AddressType, RouteData } from '@/types/addressType';
import { freightService } from '@/services/freightService';
import FreightConfigInfo from '@/components/modules/FreightConfigInfo';
import RoleGuard from '@/components/auth/RoleGuard';

export default function FreightCalculationPage() {
    const [origin, setOrigin] = useState('');
    const [destination, setDestination] = useState('');
    const [originData, setOriginData] = useState<AddressType | null>(null);
    const [destinationData, setDestinationData] = useState<AddressType | null>(null);
    const [freightValue, setFreightValue] = useState<number | null>(null);
    const [isCalculating, setIsCalculating] = useState(false);
    const [showFavoritesModal, setShowFavoritesModal] = useState(false);
    const [selectingFor, setSelectingFor] = useState<null | 'origin' | 'destination'>(null);

    const [routeData, setRouteData] = useState<RouteData | null>(null);

    const handleCalculateFreight = async () => {
        if (!originData || !destinationData) {
            alert('Por favor, selecione endereços válidos para origem e destino.');
            return;
        }

        setIsCalculating(true);

        // TODO: Integrar com backend para cálculo real do frete
        // Simular cálculo de frete baseado na distância
        // Na implementação real, enviaria originData.lat, originData.lng, destinationData.lat, destinationData.lng para o backend
        setTimeout(async () => {
            // Simular cálculo baseado na distância (fórmula simples para demonstração)
            const freightData = await freightService.calculateFreight(
                { lat: originData.lat, lng: originData.lng },
                { lat: destinationData.lat, lng: destinationData.lng }
            );

            setFreightValue(freightData.freight);
            setRouteData({ ...freightData, origin: originData, destination: destinationData, polyline: [] });
            setIsCalculating(false);
        }, 2000);
    };

    const handleOriginSelect = (addressData: AddressType) => {
        setOrigin(addressData.address);
        setOriginData(addressData);
    };

    const handleDestinationSelect = (addressData: AddressType) => {
        setDestination(addressData.address);
        setDestinationData(addressData);
    };

    const handleFavoritesSelect = (addressData: AddressType) => {
        if (selectingFor === 'origin') {
            handleOriginSelect(addressData);
        } else if (selectingFor === 'destination') {
            handleDestinationSelect(addressData);
        }
        setSelectingFor(null);
    };

    const openFavoritesForOrigin = () => {
        setSelectingFor('origin');
        setShowFavoritesModal(true);
    };

    const openFavoritesForDestination = () => {
        setSelectingFor('destination');
        setShowFavoritesModal(true);
    };

    return (
        <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col h-full">
            {/* Header Section */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800">Cálculo de Frete</h1>
                <p className="text-gray-600">Calcule o valor do frete entre dois endereços</p>
            </div>

            <RoleGuard roles={["MANAGER"]} ignoreRedirect={true}>
                <FreightConfigInfo />
            </RoleGuard>

            {/* Main Content */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Left Panel - Form */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-xl font-semibold text-gray-800">Dados da Entrega</h2>
                        <button
                            onClick={() => setShowFavoritesModal(true)}
                            className="flex items-center text-green-600 hover:text-green-800 transition-colors"
                        >
                            <Star size={18} className="mr-1" />
                            Favoritos
                        </button>
                    </div>

                    {/* Origin Field */}
                    <div className="mb-4">
                        <div className="flex justify-between items-center mb-2">
                            <label className="block text-sm font-medium text-gray-700">
                                Origem
                            </label>
                            <button
                                onClick={openFavoritesForOrigin}
                                className="text-xs text-green-600 hover:text-green-800"
                            >
                                Escolher dos favoritos
                            </button>
                        </div>
                        <AddressAutocomplete
                            value={origin}
                            onChange={setOrigin}
                            onAddressSelect={handleOriginSelect}
                            placeholder="Digite o endereço de origem..."
                        />
                    </div>

                    {/* Destination Field */}
                    <div className="mb-6">
                        <div className="flex justify-between items-center mb-2">
                            <label className="block text-sm font-medium text-gray-700">
                                Destino
                            </label>
                            <button
                                onClick={openFavoritesForDestination}
                                className="text-xs text-green-600 hover:text-green-800"
                            >
                                Escolher dos favoritos
                            </button>
                        </div>
                        <AddressAutocomplete
                            value={destination}
                            onChange={setDestination}
                            onAddressSelect={handleDestinationSelect}
                            placeholder="Digite o endereço de destino..."
                        />
                    </div>

                    {/* Route Info */}
                    {freightValue && routeData && (
                        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                            <h3 className="text-sm font-medium text-green-800 mb-2">Informações da Rota</h3>
                            <div className="grid grid-cols-2 gap-4 text-sm">
                                <div>
                                    <span className="text-green-600">Distância:</span>
                                    <span className="ml-2 font-medium">{routeData.distance}</span>
                                </div>
                                <div>
                                    <span className="text-green-600">Tempo:</span>
                                    <span className="ml-2 font-medium">{routeData.duration}</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Calculate Button */}
                    <button
                        onClick={handleCalculateFreight}
                        disabled={!originData || !destinationData || isCalculating}
                        className="w-50 bg-green-600 text-white py-3 px-4 rounded-lg flex items-center justify-center disabled:bg-gray-400 disabled:cursor-not-allowed hover:bg-green-700 transition-colors"
                    >
                        {isCalculating ? (
                            <>
                                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                                Calculando...
                            </>
                        ) : (
                            <>
                                <Calculator size={18} className="mr-2" />
                                Calcular Frete
                            </>
                        )}
                    </button>

                    {/* Freight Result */}
                    {freightValue && (
                        <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
                            <div className="flex items-center mb-2">
                                <Truck size={20} className="text-green-600 mr-2" />
                                <h3 className="text-lg font-semibold text-green-800">Valor do Frete</h3>
                            </div>
                            <p className="text-3xl font-bold text-green-600">
                                R$ {freightValue.toFixed(2).replace('.', ',')}
                            </p>
                            {routeData && (
                                <p className="text-sm text-green-700 mt-2">
                                    Baseado em {routeData.distance} de distância
                                </p>
                            )}
                        </div>
                    )}

                  
                </div>

                {/* Right Panel - Map */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <h2 className="text-xl font-semibold text-gray-800 mb-4">Rota no Mapa</h2>
                    <MapComponent
                        routeData={routeData}
                    />
                </div>
            </div>

            {/* Favorites Modal */}
            <FavoritesModal
                isOpen={showFavoritesModal}
                onClose={() => {
                    setShowFavoritesModal(false);
                    setSelectingFor(null);
                }}
                onSelectAddress={handleFavoritesSelect}
            />
        </main>
    );
}

