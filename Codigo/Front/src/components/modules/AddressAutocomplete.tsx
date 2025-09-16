import { useState, useEffect, useRef } from "react";
import { MapPin, X } from "lucide-react";

interface AddressAutocompleteProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  onAddressSelect?: (addressData: { address: string; place_id: string }) => void;
  className?: string;
}

type AddressSuggestion = {
  id: number;
  description: string;
  place_id: string;
};

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

  // Função de busca chamando o endpoint interno
  const searchAddresses = async (query: string) => {
    if (!query || query.length < 3) {
      setSuggestions([]);
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch("/api/google-autocomplete", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ input: query }),
      });

      const data = await res.json();

      if (data.status === "OK" && data.predictions) {
        const results: AddressSuggestion[] = data.predictions.map(
          (pred: any, idx: number) => ({
            id: idx,
            description: pred.description,
            place_id: pred.place_id,
          })
        );
        setSuggestions(results);
        setShowSuggestions(true);
      } else {
        setSuggestions([]);
        setShowSuggestions(false);
      }
    } catch (err) {
      setSuggestions([]);
      setShowSuggestions(false);
    }

    setIsLoading(false);
  };

  // Debounce de 300ms
  useEffect(() => {
    const timeoutId = setTimeout(() => searchAddresses(value), 300);
    return () => clearTimeout(timeoutId);
  }, [value]);

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

  const handleSuggestionClick = (address: AddressSuggestion) => {
    onChange(address.description);
    setShowSuggestions(false);
    onAddressSelect?.({ address: address.description, place_id: address.place_id });
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
