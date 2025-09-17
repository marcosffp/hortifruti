import { useState, useEffect } from 'react';
import { MapPin, Route } from 'lucide-react';
import { AddressType, RouteData } from '@/types/addressType';

interface MapComponentProps {
  origin: AddressType | null;
  destination: AddressType | null;
  onRouteCalculated?: (data: RouteData | null) => void;
}

const MapComponent = ({ origin, destination, onRouteCalculated }: MapComponentProps) => {
  const [mapData, setMapData] = useState<RouteData | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Simular dados de rota (substituir por integração real com Google Maps depois)
  const calculateRoute = async () => {
    if (!origin || !destination) {
      setMapData(null);
      return;
    }

    setIsLoading(true);

    // Simular cálculo de rota
    setTimeout(() => {
      const mockRouteData = {
        distance: '15.2 km',
        duration: '22 min',
        origin: {
          address: origin.address,
          lat: origin.lat || -19.7697,
          lng: origin.lng || -43.8516
        },
        destination: {
          address: destination.address,
          lat: destination.lat || -19.9320,
          lng: destination.lng || -43.9378
        },
        polyline: [
          { lat: -19.7697, lng: -43.8516 },
          { lat: -19.8000, lng: -43.8700 },
          { lat: -19.8500, lng: -43.9000 },
          { lat: -19.9320, lng: -43.9378 }
        ]
      };

      setMapData(mockRouteData);
      setIsLoading(false);

      if (onRouteCalculated) {
        onRouteCalculated(mockRouteData);
      }
    }, 1500);
  };

  useEffect(() => {
    calculateRoute();
  }, [origin, destination]);

  return (
    <div className="w-full h-full">
      <div className="w-full h-96 bg-gray-100 rounded-lg relative overflow-hidden">
        {isLoading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-100">
            <div className="text-center text-gray-500">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500 mx-auto mb-3"></div>
              <p>Calculando rota...</p>
            </div>
          </div>
        ) : mapData ? (
          <div className="absolute inset-0 bg-gradient-to-br from-green-100 to-blue-100 p-4">
            {/* Simulated Map View */}
            <div className="w-full h-full relative bg-white rounded border-2 border-gray-200">
              {/* Origin Marker */}
              <div className="absolute top-4 left-4 bg-green-500 text-white p-2 rounded-full shadow-lg">
                <MapPin size={16} />
              </div>
              
              {/* Destination Marker */}
              <div className="absolute bottom-4 right-4 bg-red-500 text-white p-2 rounded-full shadow-lg">
                <MapPin size={16} />
              </div>

              {/* Route Line (Simulated) */}
              <svg className="absolute inset-0 w-full h-full pointer-events-none">
                <path
                  d="M 20 20 Q 150 100 300 320"
                  stroke="#10B981"
                  strokeWidth="4"
                  fill="none"
                  strokeDasharray="10,5"
                  className="animate-pulse"
                />
              </svg>

              {/* Route Info Overlay */}
              <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-white p-3 rounded-lg shadow-lg border">
                <div className="flex items-center text-sm text-gray-600">
                  <Route size={16} className="mr-2 text-green-500" />
                  <span className="font-medium">{mapData.distance}</span>
                  <span className="mx-2">•</span>
                  <span>{mapData.duration}</span>
                </div>
              </div>

              {/* Map Labels */}
              <div className="absolute top-8 left-12 bg-white px-2 py-1 rounded text-xs text-gray-600 shadow">
                Origem
              </div>
              <div className="absolute bottom-8 right-12 bg-white px-2 py-1 rounded text-xs text-gray-600 shadow">
                Destino
              </div>
            </div>
          </div>
        ) : (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center text-gray-500">
              <MapPin size={48} className="mx-auto mb-2" />
              <p>Digite origem e destino</p>
              <p className="text-sm">para visualizar a rota</p>
            </div>
          </div>
        )}
      </div>

      {/* Route Details */}
      {mapData && (
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-50 p-3 rounded-lg">
            <h4 className="font-medium text-gray-800 mb-1">Origem</h4>
            <p className="text-sm text-gray-600">{mapData.origin.address}</p>
          </div>
          <div className="bg-gray-50 p-3 rounded-lg">
            <h4 className="font-medium text-gray-800 mb-1">Destino</h4>
            <p className="text-sm text-gray-600">{mapData.destination.address}</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default MapComponent;

