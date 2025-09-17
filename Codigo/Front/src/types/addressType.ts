export interface AddressType {
  address: string;
  lat: number;
  lng: number;
  place_id?: string;
}

export interface AddressSuggestion {
  id: number;
  description: string;
  place_id: string;
  lat?: number;
  lng?: number;
}

export interface FavoriteLocation {
  id: number;
  name: string;
  address: string;
  lat: number;
  lng: number;
}

export interface Location {
  address: string;
  lat: number;
  lng: number;
  place_id?: string;
  name?: string;
}

export interface RouteData {
  distance: string;
  duration: string;
  origin: Location;
  destination: Location;
  polyline: { lat: number; lng: number }[];
};

export interface Geolocation {
  lat: number;
  lng: number;
}