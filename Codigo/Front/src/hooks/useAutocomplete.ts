'use server';

import { cache } from 'react';

export const getPlacesAutocomplete = cache(async (input: string) => {
  if (!input || input.length < 3) {
    return { predictions: [] };
  }

  try {
    const apiKey = process.env.GOOGLE_MAPS_KEY;
    if (!apiKey) {
      console.error("Missing Google Maps API Key");
      throw new Error("API key not configured");
    }

    const url = `https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${encodeURIComponent(input)}&language=pt-BR&key=${apiKey}`;
    
    const response = await fetch(url, { cache: 'no-store' });
    const data = await response.json();
    
    if (data.status !== 'OK') {
      console.error("Places API error:", data.status, data.error_message);
      return { predictions: [] };
    }
    
    return data.predictions.map((prediction: any) => ({
      description: prediction.description,
      place_id: prediction.place_id,
    }));
  } catch (error) {
    console.error("Places API request failed:", error);
    return { predictions: [] };
  }
});

export const getPlaceDetails = cache(async (placeId: string) => {
  try {
    const apiKey = process.env.GOOGLE_MAPS_KEY;
    if (!apiKey) {
      console.error("Missing Google Maps API Key");
      throw new Error("API key not configured");
    }

    const url = `https://maps.googleapis.com/maps/api/place/details/json?place_id=${placeId}&fields=address_components,geometry,formatted_address&key=${apiKey}`;
    
    const response = await fetch(url, { cache: 'no-store' });
    const data = await response.json();

    if (data.status !== 'OK') {
      console.error("Places API error:", data.status, data.error_message);
      return null;
    }

    const addressComponents = data.result.address_components;
    const rua = addressComponents?.find((c: any) => c.types.includes('route'))?.long_name;
    const bairro = addressComponents?.find((c: any) => c.types.includes('sublocality_level_1'))?.long_name;
    const cidade = addressComponents?.find((c: any) => c.types.includes('administrative_area_level_2'))?.long_name;
    const estado = addressComponents?.find((c: any) => c.types.includes('administrative_area_level_1'))?.short_name;
    const coordenadas = data.result.geometry?.location
      ? {
          lat: data.result.geometry.location.lat,
          lng: data.result.geometry.location.lng,
        }
      : undefined;

    return {
      address: data.result.formatted_address,
      place_id: placeId,
      rua,
      bairro,
      cidade,
      estado,
      coordenadas,
      googleMapsUrl: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(data.result.formatted_address)}`,
    };
  } catch (error) {
    console.error("Places API request failed:", error);
    return null;
  }
});