import { MapPin, Store, X } from "lucide-react";
import { useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import FavoriteLocationsTab from "@/components/modals/favorites/FavoriteLocationsTab";
import SacolaoTab from "@/components/modals/favorites/SacolaoTab";
import type {
  AddressType,
  FavoriteLocation,
  Location,
} from "@/types/addressType";

interface FavoritesModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectAddress: (addressData: AddressType) => void;
}

const FavoritesModal = ({
  isOpen,
  onClose,
  onSelectAddress,
}: FavoritesModalProps) => {
  const [activeTab, setActiveTab] = useState("sacolao");
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [sacolaoData, setSacolaoData] = useState(() => {
    const saved = localStorage.getItem("sacolaoData");
    return saved
      ? JSON.parse(saved)
      : {
          name: "Hortifruti Santa Luzia",
          address:
            "R. Jaime Carlos Afonso Teixeira, 70 - Centro, Santa Luzia - MG",
          lat: -19.7647797,
          lng: -43.8500743,
        };
  });
  const [newSacolaoName, setNewSacolaoName] = useState("");
  const [newSacolaoAddress, setNewSacolaoAddress] = useState("");
  const [favoriteLocations, setFavoriteLocations] = useState<
    FavoriteLocation[]
  >(() => {
    const saved = localStorage.getItem("favoriteLocations");
    return saved ? JSON.parse(saved) : [];
  });
  const [newLocationName, setNewLocationName] = useState("");
  const [newLocationAddress, setNewLocationAddress] = useState("");
  const [newLocationCoords, setNewLocationCoords] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [deleteLocationTarget, setDeleteLocationTarget] = useState<
    number | null
  >(null);

  if (!isOpen) return null;

  const handleSaveSacolao = () => {
    if (newSacolaoName && newSacolaoAddress) {
      const updated = {
        name: newSacolaoName,
        address: newSacolaoAddress,
        lat: sacolaoData.lat,
        lng: sacolaoData.lng,
      };
      setSacolaoData(updated);
      localStorage.setItem("sacolaoData", JSON.stringify(updated));
      setNewSacolaoName("");
      setNewSacolaoAddress("");
      setIsEditing(false);
    }
  };

  const handleAddFavoriteLocation = () => {
    if (newLocationName && newLocationAddress && newLocationCoords) {
      const newLocation: FavoriteLocation = {
        id: Date.now(),
        name: newLocationName,
        address: newLocationAddress,
        lat: newLocationCoords.lat,
        lng: newLocationCoords.lng,
      };
      const updated = [...favoriteLocations, newLocation];
      setFavoriteLocations(updated);
      localStorage.setItem("favoriteLocations", JSON.stringify(updated));
      setNewLocationName("");
      setNewLocationAddress("");
      setNewLocationCoords(null);
    }
  };

  const handleEditFavoriteLocation = (id: number) => {
    const location = favoriteLocations.find(
      (loc: FavoriteLocation) => loc.id === id,
    );
    if (location) {
      setNewLocationName(location.name);
      setNewLocationAddress(location.address);
      setNewLocationCoords({ lat: location.lat, lng: location.lng });
      setEditingId(id);
    }
  };

  const handleUpdateFavoriteLocation = () => {
    if (
      newLocationName &&
      newLocationAddress &&
      editingId &&
      newLocationCoords
    ) {
      const updated = favoriteLocations.map((loc: FavoriteLocation) =>
        loc.id === editingId
          ? {
              ...loc,
              name: newLocationName,
              address: newLocationAddress,
              lat: newLocationCoords.lat,
              lng: newLocationCoords.lng,
            }
          : loc,
      );
      setFavoriteLocations(updated);
      localStorage.setItem("favoriteLocations", JSON.stringify(updated));
      setNewLocationName("");
      setNewLocationAddress("");
      setNewLocationCoords(null);
      setEditingId(null);
    }
  };

  const handleDeleteFavoriteLocation = (id: number) => {
    setDeleteLocationTarget(id);
  };

  const confirmDeleteFavoriteLocation = () => {
    if (deleteLocationTarget === null) return;
    const updated = favoriteLocations.filter(
      (loc: FavoriteLocation) => loc.id !== deleteLocationTarget,
    );
    setFavoriteLocations(updated);
    localStorage.setItem("favoriteLocations", JSON.stringify(updated));
    setDeleteLocationTarget(null);
  };

  const handleSelectLocation = (location: Location) => {
    if (onSelectAddress) {
      onSelectAddress({
        address: location.address,
        lat: location.lat,
        lng: location.lng,
      });
    }
    onClose();
  };

  const resetLocationForm = () => {
    setEditingId(null);
    setNewLocationName("");
    setNewLocationAddress("");
    setNewLocationCoords(null);
  };

  return (
    <div className="absolute inset-0 bg-black/60 flex items-center justify-center z-10 max-lg:px-4">
      <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-gray-800">
            Gerenciar Locais Favoritos
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 p-1"
          >
            <X size={24} />
          </button>
        </div>

        <div className="flex border-b border-gray-200 mb-6">
          <button
            type="button"
            onClick={() => setActiveTab("sacolao")}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === "sacolao"
                ? "border-green-500 text-green-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            <Store size={18} className="inline mr-2" />
            Sacolão
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("favorites")}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === "favorites"
                ? "border-green-500 text-green-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            <MapPin size={18} className="inline mr-2" />
            Locais Favoritos
          </button>
        </div>

        {activeTab === "sacolao" && (
          <SacolaoTab
            sacolaoData={sacolaoData}
            isEditing={isEditing}
            newSacolaoName={newSacolaoName}
            newSacolaoAddress={newSacolaoAddress}
            onSelectLocation={handleSelectLocation}
            onStartEditing={() => {
              setIsEditing(true);
              setNewSacolaoName(sacolaoData.name);
              setNewSacolaoAddress(sacolaoData.address);
            }}
            onNameChange={setNewSacolaoName}
            onAddressChange={setNewSacolaoAddress}
            onSave={handleSaveSacolao}
            onCancel={() => {
              setIsEditing(false);
              setNewSacolaoName("");
              setNewSacolaoAddress("");
            }}
          />
        )}

        {activeTab === "favorites" && (
          <FavoriteLocationsTab
            favoriteLocations={favoriteLocations}
            editingId={editingId}
            newLocationName={newLocationName}
            newLocationAddress={newLocationAddress}
            newLocationCoords={newLocationCoords}
            onNameChange={setNewLocationName}
            onAddressChange={(value) => {
              setNewLocationAddress(value);
              setNewLocationCoords(null);
            }}
            onAddressSelect={(addressData) => {
              setNewLocationAddress(addressData.address);
              setNewLocationCoords({
                lat: addressData.lat,
                lng: addressData.lng,
              });
            }}
            onSubmit={
              editingId
                ? handleUpdateFavoriteLocation
                : handleAddFavoriteLocation
            }
            onCancelEdit={resetLocationForm}
            onSelectLocation={handleSelectLocation}
            onEditLocation={handleEditFavoriteLocation}
            onDeleteLocation={handleDeleteFavoriteLocation}
          />
        )}
      </div>

      <ConfirmDeleteModal
        open={deleteLocationTarget !== null}
        onClose={() => setDeleteLocationTarget(null)}
        onConfirm={confirmDeleteFavoriteLocation}
        title="Tem certeza que deseja excluir este local favorito?"
      />
    </div>
  );
};

export default FavoritesModal;
