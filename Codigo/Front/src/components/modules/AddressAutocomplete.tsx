import { useState, useEffect, useRef, useCallback } from "react";
import { MapPin, X } from "lucide-react";
import { AddressSuggestion, AddressType } from "@/types/addressType";
import { getPlacesAutocomplete, getPlaceDetails } from "@/hooks/useAutocomplete";

interface AddressAutocompleteProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  onAddressSelect?: (addressData: AddressType) => void;
  className?: string;
}

const AddressAutocomplete = ({
  value,
  onChange,
  placeholder = "Digite o endereço...",
  onAddressSelect,
  className = "",
}: AddressAutocompleteProps) => {
  const [suggestions, setSuggestions] = useState<AddressSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  // Função de busca chamando o novo hook
  const searchAddresses = useCallback(
    async (query: string) => {
      if (query.length < 3) {
        setSuggestions([]);
        return;
      }

      setIsLoading(true);
      try {
        const results = await getPlacesAutocomplete(query);
        setSuggestions(
          results.map((prediction: any, idx: number) => ({
            id: idx,
            description: prediction.description,
            place_id: prediction.place_id,
          }))
        );
        setShowSuggestions(true);
      } catch (error) {
        console.error("Error fetching suggestions:", error);
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  // Debounce de 300ms
  useEffect(() => {
    const timeoutId = setTimeout(() => searchAddresses(value), 300);
    return () => clearTimeout(timeoutId);
  }, [value, searchAddresses]);

  // Fecha o dropdown ao clicar fora
  useEffect(() => {
    const handleClickOutside = (event: any) => {
      if (
        inputRef.current &&
        !inputRef.current.contains(event.target) &&
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleInputChange = (e: { target: { value: string } }) => {
    onChange(e.target.value);
  };

  const handleSuggestionClick = async (address: AddressSuggestion) => {
    try {
      setIsLoading(true);
      const details = await getPlaceDetails(address.place_id);

      if (details) {
        // Ajuste para refletir a estrutura real retornada por getPlaceDetails
        const {
          address,
          place_id,
          rua,
          bairro,
          cidade,
          estado,
          coordenadas,
          googleMapsUrl,
        } = details;

        const lat = coordenadas?.lat || 0;
        const lng = coordenadas?.lng || 0;

        const detailedAddress = {
          address,
          place_id,
          rua,
          bairro,
          cidade,
          estado,
          lat,
          lng,
          googleMapsUrl,
        };

        // Atualiza o valor do input e chama o callback com os detalhes
        onChange(address);
        onAddressSelect?.(detailedAddress);
        setShowSuggestions(false);
      }
    } catch (error) {
      console.error("Error fetching place details:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const clearInput = () => {
    onChange("");
    setSuggestions([]);
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  return (
    <div className={`relative ${className}`}>
      <div className="relative">
        <MapPin size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          ref={inputRef}
          type="text"
          placeholder={placeholder}
          className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
          value={value}
          onChange={handleInputChange}
          onFocus={() => value.length >= 3 && setShowSuggestions(true)}
        />
        {value && (
          <button
            onClick={clearInput}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            <X size={18} />
          </button>
        )}
      </div>

      {showSuggestions && (
        <div
          ref={suggestionsRef}
          className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
        >
          {isLoading ? (
            <div className="p-4 text-center text-gray-500">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-green-500 mx-auto mb-2"></div>
              Buscando endereços...
            </div>
          ) : suggestions.length > 0 ? (
            suggestions.map((address) => (
              <button
                key={address.id}
                onClick={() => handleSuggestionClick(address)}
                className="w-full text-left p-3 hover:bg-gray-50 border-b border-gray-100 last:border-b-0 flex items-start"
              >
                <MapPin size={16} className="text-gray-400 mt-1 mr-3 flex-shrink-0" />
                <span className="text-gray-700">{address.description}</span>
              </button>
            ))
          ) : value.length >= 3 ? (
            <div className="p-4 text-center text-gray-500">Nenhum endereço encontrado</div>
          ) : null}
        </div>
      )}
    </div>
  );
};

export default AddressAutocomplete;
