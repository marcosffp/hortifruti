import { Edit, MapPin, Plus, Trash2 } from "lucide-react";
import AddressAutocomplete from "@/components/modules/AddressAutocomplete";
import type { FavoriteLocation, Location } from "@/types/addressType";

interface FavoriteLocationsTabProps {
  favoriteLocations: FavoriteLocation[];
  editingId: number | null;
  newLocationName: string;
  newLocationAddress: string;
  newLocationCoords: { lat: number; lng: number } | null;
  onNameChange: (value: string) => void;
  onAddressChange: (value: string) => void;
  onAddressSelect: (addressData: {
    address: string;
    lat: number;
    lng: number;
  }) => void;
  onSubmit: () => void;
  onCancelEdit: () => void;
  onSelectLocation: (location: Location) => void;
  onEditLocation: (id: number) => void;
  onDeleteLocation: (id: number) => void;
}

export default function FavoriteLocationsTab({
  favoriteLocations,
  editingId,
  newLocationName,
  newLocationAddress,
  newLocationCoords,
  onNameChange,
  onAddressChange,
  onAddressSelect,
  onSubmit,
  onCancelEdit,
  onSelectLocation,
  onEditLocation,
  onDeleteLocation,
}: FavoriteLocationsTabProps) {
  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-medium text-gray-800">Locais Favoritos</h3>
        <span className="text-sm text-gray-500">
          {favoriteLocations.length} locais
        </span>
      </div>

      <div className="bg-gray-50 rounded-lg p-4 mb-6">
        <h4 className="font-medium text-gray-800 mb-4">
          {editingId ? "Editar Local" : "Adicionar Novo Local"}
        </h4>
        <div className="space-y-4">
          <input
            type="text"
            placeholder="Nome do local..."
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            value={newLocationName}
            onChange={(e) => onNameChange(e.target.value)}
          />
          <AddressAutocomplete
            value={newLocationAddress}
            onChange={onAddressChange}
            onAddressSelect={onAddressSelect}
            placeholder="Endereço completo..."
          />
          {newLocationAddress && !newLocationCoords && (
            <p className="text-sm text-amber-600">
              Selecione um endereço da lista de sugestões para obter as
              coordenadas.
            </p>
          )}
          <div className="flex space-x-3">
            <button
              type="button"
              disabled={
                !newLocationName || !newLocationAddress || !newLocationCoords
              }
              onClick={onSubmit}
              className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Plus size={18} className="mr-2" />
              {editingId ? "Atualizar" : "Adicionar"}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={onCancelEdit}
                className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600"
              >
                Cancelar
              </button>
            )}
          </div>
        </div>
      </div>

      {favoriteLocations.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <MapPin size={48} className="mx-auto mb-3 text-gray-300" />
          <p>Nenhum local favorito cadastrado</p>
          <p className="text-sm">
            Adicione locais para facilitar o cálculo de frete
          </p>
          <button
            type="button"
            className="mt-4 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center mx-auto"
            onClick={onCancelEdit}
          >
            <Plus size={18} className="mr-2" />
            Criar Favorito
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {favoriteLocations.map((location: FavoriteLocation) => (
            <div
              key={location.id}
              className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <h4 className="font-medium text-gray-800 mb-1">
                    {location.name}
                  </h4>
                  <p className="text-gray-600 text-sm">{location.address}</p>
                </div>
                <div className="flex space-x-2">
                  <button
                    type="button"
                    onClick={() => onSelectLocation(location)}
                    className="text-green-600 hover:text-green-800 p-1"
                    title="Usar este endereço"
                  >
                    <MapPin size={18} />
                  </button>
                  <button
                    type="button"
                    onClick={() => onEditLocation(location.id)}
                    className="text-blue-600 hover:text-blue-800 p-1"
                    title="Editar"
                  >
                    <Edit size={18} />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDeleteLocation(location.id)}
                    className="text-red-600 hover:text-red-800 p-1"
                    title="Excluir"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
