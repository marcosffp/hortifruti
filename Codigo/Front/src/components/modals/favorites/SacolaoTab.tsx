import { Edit, MapPin } from "lucide-react";
import AddressAutocomplete from "@/components/modules/AddressAutocomplete";
import type { Location } from "@/types/addressType";

interface SacolaoData {
  name: string;
  address: string;
  lat: number;
  lng: number;
}

interface SacolaoTabProps {
  sacolaoData: SacolaoData;
  isEditing: boolean;
  newSacolaoName: string;
  newSacolaoAddress: string;
  onSelectLocation: (location: Location) => void;
  onStartEditing: () => void;
  onNameChange: (value: string) => void;
  onAddressChange: (value: string) => void;
  onSave: () => void;
  onCancel: () => void;
}

export default function SacolaoTab({
  sacolaoData,
  isEditing,
  newSacolaoName,
  newSacolaoAddress,
  onSelectLocation,
  onStartEditing,
  onNameChange,
  onAddressChange,
  onSave,
  onCancel,
}: SacolaoTabProps) {
  return (
    <div>
      <h3 className="text-lg font-medium text-gray-800 mb-4">
        Endereço do Sacolão
      </h3>

      <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
        <div className="flex justify-between items-start">
          <div className="flex-1">
            <h4 className="font-medium text-green-800 mb-1">
              {sacolaoData.name}
            </h4>
            <p className="text-green-700 text-sm">{sacolaoData.address}</p>
          </div>
          <div className="flex space-x-2">
            <button
              type="button"
              onClick={() => onSelectLocation(sacolaoData)}
              className="text-green-600 hover:text-green-800 p-1"
              title="Usar este endereço"
            >
              <MapPin size={18} />
            </button>
            <button
              type="button"
              onClick={onStartEditing}
              className="text-blue-600 hover:text-blue-800 p-1"
              title="Editar"
            >
              <Edit size={18} />
            </button>
          </div>
        </div>
      </div>

      {isEditing && (
        <div className="bg-gray-50 rounded-lg p-4 mb-4">
          <h4 className="font-medium text-gray-800 mb-4">Editar Sacolão</h4>
          <div className="space-y-4">
            <input
              type="text"
              placeholder="Nome do sacolão..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              value={newSacolaoName}
              onChange={(e) => onNameChange(e.target.value)}
            />
            <AddressAutocomplete
              value={newSacolaoAddress}
              onChange={onAddressChange}
              placeholder="Endereço completo do sacolão..."
            />
            <div className="flex space-x-3">
              <button
                type="button"
                onClick={onSave}
                className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
              >
                Salvar
              </button>
              <button
                type="button"
                onClick={onCancel}
                className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
