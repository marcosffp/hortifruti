"use client";

import L from "leaflet";
import { MapPin } from "lucide-react";
import { useEffect, useState } from "react";
import {
  MapContainer,
  Marker,
  Polyline,
  Popup,
  TileLayer,
} from "react-leaflet";
import { getRoute } from "@/actions/routeActions";
import type { RouteData } from "@/types/addressType";
import "leaflet/dist/leaflet.css";

const MapComponent = ({ routeData }: { routeData: RouteData | null }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [routePoints, setRoutePoints] = useState<[number, number][]>([]);

  useEffect(() => {
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: "/leaflet/marker-icon-2x.png",
      iconUrl: "/leaflet/marker-icon.png",
      shadowUrl: "/leaflet/marker-shadow.png",
    });
  }, []);

  useEffect(() => {
    if (!routeData) return;

    let cancelled = false;

    (async () => {
      setIsLoading(true);
      const points = await getRoute(routeData.origin, routeData.destination);

      if (cancelled) return;
      setRoutePoints(points.map((point) => [point.lat, point.lng]));
      setIsLoading(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [routeData]);

  const center: [number, number] = routeData
    ? [
        (routeData.origin.lat + routeData.destination.lat) / 2,
        (routeData.origin.lng + routeData.destination.lng) / 2,
      ]
    : [0, 0];

  const zoom = routeData ? 12 : 2;

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
        ) : routeData ? (
          <MapContainer
            center={center}
            zoom={zoom}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Origem */}
            <Marker position={[routeData.origin.lat, routeData.origin.lng]}>
              <Popup>Origem: {routeData.origin.address}</Popup>
            </Marker>

            {/* Destino */}
            <Marker
              position={[routeData.destination.lat, routeData.destination.lng]}
            >
              <Popup>Destino: {routeData.destination.address}</Popup>
            </Marker>

            {/* Linha da rota obtida da API */}
            {routePoints.length > 0 && (
              <Polyline
                positions={routePoints}
                color="#2563eb"
                weight={4}
                opacity={0.7}
              />
            )}
          </MapContainer>
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

      {routeData && (
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-50 p-3 rounded-lg">
            <h4 className="font-medium text-gray-800 mb-1">Origem</h4>
            <p className="text-sm text-gray-600">{routeData.origin.address}</p>
          </div>
          <div className="bg-gray-50 p-3 rounded-lg">
            <h4 className="font-medium text-gray-800 mb-1">Destino</h4>
            <p className="text-sm text-gray-600">
              {routeData.destination.address}
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default MapComponent;
