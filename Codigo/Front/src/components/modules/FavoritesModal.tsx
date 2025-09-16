import { SetStateAction, useState } from 'react';
import { X, Plus, Edit, Trash2, MapPin, Store } from 'lucide-react';
import AddressAutocomplete from '@/components/modules/AddressAutocomplete';

interface FavoritesModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectAddress: (addressData: { address: string; lat: number; lng: number }) => void;
}

const FavoritesModal = ({ isOpen, onClose, onSelectAddress }: FavoritesModalProps) => {
  const [activeTab, setActiveTab] = useState('sacolao');
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  
  // Estados para Sacolão
  const [sacolaoData, setSacolaoData] = useState(() => {
    const saved = localStorage.getItem('sacolaoData');
    return saved ? JSON.parse(saved) : {
      name: 'Hortifruti Santa Luzia',
      address: 'R. Jaime Carlos Afonso Teixeira, 70 - Centro, Santa Luzia - MG',
      lat: -19.7647402,
      lng: -43.850199
    };
  });
  const [newSacolaoName, setNewSacolaoName] = useState('');
  const [newSacolaoAddress, setNewSacolaoAddress] = useState('');

  // Estados para Locais Favoritos
  interface FavoriteLocation {
    id: number;
    name: string;
    address: string;
    lat: number;
    lng: number;
  }
  const [favoriteLocations, setFavoriteLocations] = useState<FavoriteLocation[]>(() => {
    const saved = localStorage.getItem('favoriteLocations');
    return saved ? JSON.parse(saved) : [];
  });
  const [newLocationName, setNewLocationName] = useState('');
  const [newLocationAddress, setNewLocationAddress] = useState('');

  if (!isOpen) return null;

  const handleSaveSacolao = () => {
    if (newSacolaoName && newSacolaoAddress) {
      const updated = {
        name: newSacolaoName,
        address: newSacolaoAddress,
        lat: -19.7697, // Seria obtido do autocomplete
        lng: -43.8516
      };
      setSacolaoData(updated);
      localStorage.setItem('sacolaoData', JSON.stringify(updated));
      setNewSacolaoName('');
      setNewSacolaoAddress('');
      setIsEditing(false);
    }
  };

  const handleAddFavoriteLocation = () => {
    if (newLocationName && newLocationAddress) {
      const newLocation = {
        id: Date.now(),
        name: newLocationName,
        address: newLocationAddress,
        lat: -19.7697, // Seria obtido do autocomplete
        lng: -43.8516
      };
    const updated = [...favoriteLocations, newLocation];
    setFavoriteLocations(updated);
    localStorage.setItem('favoriteLocations', JSON.stringify(updated));
      setNewLocationName('');
      setNewLocationAddress('');
    }
  };

  const handleEditFavoriteLocation = (id: number) => {
  const location = favoriteLocations.find((loc: FavoriteLocation) => loc.id === id);
    if (location) {
      setNewLocationName(location.name);
      setNewLocationAddress(location.address);
      setEditingId(id);
    }
  };

  const handleUpdateFavoriteLocation = () => {
    if (newLocationName && newLocationAddress && editingId) {
      const updated = favoriteLocations.map((loc: FavoriteLocation) => 
        loc.id === editingId 
          ? { ...loc, name: newLocationName, address: newLocationAddress }
          : loc
      );
      setFavoriteLocations(updated);
      localStorage.setItem('favoriteLocations', JSON.stringify(updated));
      setNewLocationName('');
      setNewLocationAddress('');
      setEditingId(null);
    }
  };

  const handleDeleteFavoriteLocation = (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este local favorito?')) {
  const updated = favoriteLocations.filter((loc: FavoriteLocation) => loc.id !== id);
    setFavoriteLocations(updated);
    localStorage.setItem('favoriteLocations', JSON.stringify(updated));
    }
  };

  const handleSelectLocation = (location: { name?: string; address: any; lat: any; lng: any; id?: number; }) => {
    if (onSelectAddress) {
      onSelectAddress({
        address: location.address,
        lat: location.lat,
        lng: location.lng
      });
    }
    onClose();
  };

  return (
    <div className="absolute inset-0 bg-black/60 flex items-center justify-center z-10 max-lg:px-4">
      <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-gray-800">Gerenciar Locais Favoritos</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 p-1"
          >
            <X size={24} />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200 mb-6">
          <button
            onClick={() => setActiveTab('sacolao')}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === 'sacolao'
                ? 'border-green-500 text-green-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Store size={18} className="inline mr-2" />
            Sacolão
          </button>
          <button
            onClick={() => setActiveTab('favorites')}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === 'favorites'
                ? 'border-green-500 text-green-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <MapPin size={18} className="inline mr-2" />
            Locais Favoritos
          </button>
        </div>

        {/* Sacolão Tab */}
        {activeTab === 'sacolao' && (
          <div>
            <h3 className="text-lg font-medium text-gray-800 mb-4">Endereço do Sacolão</h3>
            
            {/* Current Sacolão */}
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <h4 className="font-medium text-green-800 mb-1">{sacolaoData.name}</h4>
                  <p className="text-green-700 text-sm">{sacolaoData.address}</p>
                </div>
                <div className="flex space-x-2">
                  <button
                    onClick={() => handleSelectLocation(sacolaoData)}
                    className="text-green-600 hover:text-green-800 p-1"
                    title="Usar este endereço"
                  >
                    <MapPin size={18} />
                  </button>
                  <button
                    onClick={() => {
                      setIsEditing(true);
                      setNewSacolaoName(sacolaoData.name);
                      setNewSacolaoAddress(sacolaoData.address);
                    }}
                    className="text-blue-600 hover:text-blue-800 p-1"
                    title="Editar"
                  >
                    <Edit size={18} />
                  </button>
                </div>
              </div>
            </div>

            {/* Edit Sacolão Form */}
            {isEditing && (
              <div className="bg-gray-50 rounded-lg p-4 mb-4">
                <h4 className="font-medium text-gray-800 mb-4">Editar Sacolão</h4>
                <div className="space-y-4">
                  <input
                    type="text"
                    placeholder="Nome do sacolão..."
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                    value={newSacolaoName}
                    onChange={(e) => setNewSacolaoName(e.target.value)}
                  />
                  <AddressAutocomplete
                    value={newSacolaoAddress}
                    onChange={setNewSacolaoAddress}
                    placeholder="Endereço completo do sacolão..."
                  />
                  <div className="flex space-x-3">
                    <button
                      onClick={handleSaveSacolao}
                      className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
                    >
                      Salvar
                    </button>
                    <button
                      onClick={() => {
                        setIsEditing(false);
                        setNewSacolaoName('');
                        setNewSacolaoAddress('');
                      }}
                      className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Locais Favoritos Tab */}
        {activeTab === 'favorites' && (
          <div>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-medium text-gray-800">Locais Favoritos</h3>
              <span className="text-sm text-gray-500">{favoriteLocations.length} locais</span>
            </div>

            {/* Add New Location Form */}
            <div className="bg-gray-50 rounded-lg p-4 mb-6">
              <h4 className="font-medium text-gray-800 mb-4">
                {editingId ? 'Editar Local' : 'Adicionar Novo Local'}
              </h4>
              <div className="space-y-4">
                <input
                  type="text"
                  placeholder="Nome do local..."
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                  value={newLocationName}
                  onChange={(e) => setNewLocationName(e.target.value)}
                />
                <AddressAutocomplete
                  value={newLocationAddress}
                  onChange={setNewLocationAddress}
                  placeholder="Endereço completo..."
                />
                <div className="flex space-x-3">
                  <button
                    onClick={editingId ? handleUpdateFavoriteLocation : handleAddFavoriteLocation}
                    className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center"
                  >
                    <Plus size={18} className="mr-2" />
                    {editingId ? 'Atualizar' : 'Adicionar'}
                  </button>
                  {editingId && (
                    <button
                      onClick={() => {
                        setEditingId(null);
                        setNewLocationName('');
                        setNewLocationAddress('');
                      }}
                      className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600"
                    >
                      Cancelar
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Favorite Locations List */}
            {favoriteLocations.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <MapPin size={48} className="mx-auto mb-3 text-gray-300" />
                <p>Nenhum local favorito cadastrado</p>
                <p className="text-sm">Adicione locais para facilitar o cálculo de frete</p>
                <button
                  className="mt-4 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center mx-auto"
                  onClick={() => {
                    setEditingId(null);
                    setNewLocationName('');
                    setNewLocationAddress('');
                  }}
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
                        <h4 className="font-medium text-gray-800 mb-1">{location.name}</h4>
                        <p className="text-gray-600 text-sm">{location.address}</p>
                      </div>
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleSelectLocation(location)}
                          className="text-green-600 hover:text-green-800 p-1"
                          title="Usar este endereço"
                        >
                          <MapPin size={18} />
                        </button>
                        <button
                          onClick={() => handleEditFavoriteLocation(location.id)}
                          className="text-blue-600 hover:text-blue-800 p-1"
                          title="Editar"
                        >
                          <Edit size={18} />
                        </button>
                        <button
                          onClick={() => handleDeleteFavoriteLocation(location.id)}
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
        )}
      </div>
    </div>
  );
};

export default FavoritesModal;

