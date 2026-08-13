"use server";

import type { Geolocation } from "@/types/addressType";

export async function getRoute(
  origin: Geolocation,
  destination: Geolocation,
): Promise<Geolocation[]> {
  try {
    const url = `https://router.project-osrm.org/route/v1/driving/${origin.lng},${origin.lat};${destination.lng},${destination.lat}?overview=full&geometries=geojson`;

    const response = await fetch(url, { cache: "no-store" });
    const data = await response.json();

    if (!data.routes || data.routes.length === 0) {
      return [];
    }

    return data.routes[0].geometry.coordinates.map(
      ([lng, lat]: [number, number]) => ({ lat, lng }),
    );
  } catch (error) {
    console.error("Route API request failed:", error);
    return [];
  }
}
