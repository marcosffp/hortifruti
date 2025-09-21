"use client";

import { useState, useEffect, useRef } from "react";
import { MapPin } from "lucide-react";
import { RouteData } from "@/types/addressType";
import maplibregl, { Map as MapLibreMap, LngLatBoundsLike } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

const MapComponent = ({ routeData }: { routeData: RouteData | null }) => {
  const [isLoading, setIsLoading] = useState(false);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<MapLibreMap | null>(null);

  // Referências para layers e source
  const routeSourceId = "route-line";
  const originMarkerRef = useRef<maplibregl.Marker | null>(null);
  const destMarkerRef = useRef<maplibregl.Marker | null>(null);

  // Inicializa o mapa apenas uma vez
  useEffect(() => {
    if (!mapContainerRef.current || mapInstance.current) return;

    mapInstance.current = new maplibregl.Map({
      container: mapContainerRef.current,
      style: `https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${process.env.NEXT_PUBLIC_GEOAPIFY_KEY}`,
      zoom: 12,
    });

    mapInstance.current.addControl(new maplibregl.NavigationControl(), "top-right");

    return () => {
      mapInstance.current?.remove();
      mapInstance.current = null;
    };
  }, []);

  // Atualiza quando os dados da rota chegam
  useEffect(() => {
    if (!mapInstance.current) return;

    const map = mapInstance.current;

    // Remove marcadores antigos
    originMarkerRef.current?.remove();
    destMarkerRef.current?.remove();

    // Remove rota antiga
    if (map.getLayer(routeSourceId)) {
      map.removeLayer(routeSourceId);
      map.removeSource(routeSourceId);
    }

    if (!routeData) return;

    const { origin, destination, polyline } = routeData;

    // Cria marcadores
    originMarkerRef.current = new maplibregl.Marker({ color: "green" })
      .setLngLat([origin.lng, origin.lat])
      .addTo(map);

    destMarkerRef.current = new maplibregl.Marker({ color: "red" })
      .setLngLat([destination.lng, destination.lat])
      .addTo(map);

    // Ajusta o zoom para caber os pontos
    const bounds: LngLatBoundsLike = [
      [origin.lng, origin.lat],
      [destination.lng, destination.lat],
    ];
    map.fitBounds(bounds, { padding: 50 });

    // Adiciona a rota
    if (polyline && polyline.length > 0) {
      const coords = polyline.map((p) => [p.lng, p.lat]);

      map.addSource(routeSourceId, {
        type: "geojson",
        data: {
          type: "Feature",
          geometry: {
            type: "LineString",
            coordinates: coords,
          },
          properties: {},
        },
      });

      map.addLayer({
        id: routeSourceId,
        type: "line",
        source: routeSourceId,
        paint: {
          "line-color": "#2563eb",
          "line-width": 4,
        },
      });
    }
  }, [routeData]);

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
          <div ref={mapContainerRef} className="absolute inset-0" />
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
            <p className="text-sm text-gray-600">{routeData.destination.address}</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default MapComponent;
