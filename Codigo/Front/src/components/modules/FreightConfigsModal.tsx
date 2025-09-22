import { useState, useEffect } from 'react';
import { X, Save, Car, User, DollarSign, AlertCircle } from 'lucide-react';
import { FreightConfigDTO } from '@/types/freightType';
import { freightService } from '@/services/freightService';

interface FreightConfigModalProps {
  isOpen: boolean;
  onClose: (updatedConfig?: FreightConfigDTO) => void;
  initialData?: FreightConfigDTO; 
}

const FreightConfigModal = ({ isOpen, onClose, initialData }: FreightConfigModalProps) => {
  const [activeTab, setActiveTab] = useState('vehicle');
  const [config, setConfig] = useState<FreightConfigDTO>(() => {
    // Define um estado inicial padrão ou usa os dados passados via props
    return initialData || {
      kmPerLiterConsumption: 0,
      fuelPrice: 0,
      maintenanceCostPerKm: 0,
      tireCostPerKm: 0,
      depreciationCostPerKm: 0,
      insuranceCostPerKm: 0,
      baseSalary: 0,
      chargesPercentage: 0,
      monthlyHoursWorked: 0,
      administrativeCostsPercentage: 0,
      marginPercentage: 0,
      fixedFee: 0,
    };
  });
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Efeito para atualizar o estado com os dados iniciais quando o modal abrir
  useEffect(() => {
    if (isOpen && initialData) {
      setConfig({...initialData});
      setError(null);
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setConfig((prev: FreightConfigDTO) => ({
      ...prev,
      [name]: value === '' ? '' : parseFloat(value), 
    }));
  };

  const handleSave = async () => {
    try {
      setIsSaving(true);
      setError(null);
      
      const changedFields: Partial<FreightConfigDTO> = {};
      
      Object.keys(config).forEach(key => {
        const typedKey = key as keyof FreightConfigDTO;
        const currentValue = config[typedKey];
        const initialValue = initialData?.[typedKey];
        
        // Se o valor foi alterado, inclui no objeto de campos alterados
        if (currentValue !== initialValue) {
          // Converte para número se necessário
          let numValue = typeof currentValue === 'string' ? parseFloat(currentValue) : Number(currentValue);
          changedFields[typedKey] = isNaN(numValue) ? 0 : numValue;
        }
      });
      
      // Se não houver campos alterados, não faça a chamada
      if (Object.keys(changedFields).length === 0) {
        onClose();
        return;
      }
      
      // Chamar a API para salvar apenas os campos alterados
      const updatedConfig = await freightService.updateFreightConfig(changedFields);
      
      onClose(updatedConfig);
    } catch (error) {
      console.error("Erro ao salvar configurações:", error);
      setError("Não foi possível salvar as configurações. Tente novamente.");
    } finally {
      setIsSaving(false);
    }
  };

  // Componente auxiliar para os inputs, mantendo o estilo consistente
  const ConfigInput = ({ label, name, value, placeholder, type = "number" }: any) => (
    <div>
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
        {label}
      </label>
      <input
        type={type}
        id={name}
        name={name}
        value={value}
        onChange={handleInputChange}
        placeholder={placeholder}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
        step="0.01" // Para campos de moeda
        disabled={isSaving}
      />
    </div>
  );

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 max-lg:px-4">
      <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] flex flex-col">
        {/* Cabeçalho do Modal */}
        <div className="flex justify-between items-center mb-6 flex-shrink-0">
          <h2 className="text-2xl font-semibold text-gray-800">Configurações de Frete</h2>
          <button 
            onClick={() => onClose()} 
            className="text-gray-500 hover:text-gray-700 p-1"
            disabled={isSaving}
          >
            <X size={24} />
          </button>
        </div>

        {/* Mensagem de erro */}
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 flex items-center">
            <AlertCircle size={18} className="mr-2 flex-shrink-0" />
            <p>{error}</p>
          </div>
        )}

        {/* Abas de Navegação */}
        <div className="flex border-b border-gray-200 mb-6 flex-shrink-0">
          <button
            onClick={() => setActiveTab('vehicle')}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === 'vehicle'
                ? 'border-green-500 text-green-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
            disabled={isSaving}
          >
            <Car size={18} className="inline mr-2" />
            Veículo
          </button>
          <button
            onClick={() => setActiveTab('deliveryPerson')}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === 'deliveryPerson'
                ? 'border-green-500 text-green-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
            disabled={isSaving}
          >
            <User size={18} className="inline mr-2" />
            Entregador
          </button>
          <button
            onClick={() => setActiveTab('margins')}
            className={`px-6 py-3 border-b-2 font-medium transition-colors ${
              activeTab === 'margins'
                ? 'border-green-500 text-green-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
            disabled={isSaving}
          >
            <DollarSign size={18} className="inline mr-2" />
            Margens e Taxas
          </button>
        </div>

        {/* Conteúdo das Abas */}
        <div className="overflow-y-auto pr-2 -mr-2">
          {activeTab === 'vehicle' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
              <ConfigInput label="Consumo (Km/L)" name="kmPerLiterConsumption" value={config.kmPerLiterConsumption} placeholder="Ex: 10.5" />
              <ConfigInput label="Preço do Combustível (R$)" name="fuelPrice" value={config.fuelPrice} placeholder="Ex: 5.80" />
              <ConfigInput label="Custo de Manutenção (R$/Km)" name="maintenanceCostPerKm" value={config.maintenanceCostPerKm} placeholder="Ex: 0.15" />
              <ConfigInput label="Custo do Pneu (R$/Km)" name="tireCostPerKm" value={config.tireCostPerKm} placeholder="Ex: 0.05" />
              <ConfigInput label="Custo de Depreciação (R$/Km)" name="depreciationCostPerKm" value={config.depreciationCostPerKm} placeholder="Ex: 0.20" />
              <ConfigInput label="Custo do Seguro (R$/Km)" name="insuranceCostPerKm" value={config.insuranceCostPerKm} placeholder="Ex: 0.08" />
            </div>
          )}

          {activeTab === 'deliveryPerson' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
              <ConfigInput label="Salário Base (R$)" name="baseSalary" value={config.baseSalary} placeholder="Ex: 2500.00" />
              <ConfigInput label="Percentual de Encargos (%)" name="chargesPercentage" value={config.chargesPercentage} placeholder="Ex: 40" />
              <ConfigInput label="Horas Trabalhadas (Mensal)" name="monthlyHoursWorked" value={config.monthlyHoursWorked} placeholder="Ex: 220" />
              <ConfigInput label="Custos Administrativos (%)" name="administrativeCostsPercentage" value={config.administrativeCostsPercentage} placeholder="Ex: 15" />
            </div>
          )}

          {activeTab === 'margins' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
              <ConfigInput label="Margem de Lucro (%)" name="marginPercentage" value={config.marginPercentage} placeholder="Ex: 25" />
              <ConfigInput label="Taxa Fixa (R$)" name="fixedFee" value={config.fixedFee} placeholder="Ex: 2.50" />
            </div>
          )}
        </div>

        {/* Rodapé com Botões */}
        <div className="flex justify-end space-x-4 mt-8 pt-4 border-t border-gray-200 flex-shrink-0">
          <button
            onClick={() => onClose()}
            className="bg-gray-200 text-gray-800 px-6 py-2 rounded-lg hover:bg-gray-300 transition-colors"
            disabled={isSaving}
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            className={`bg-green-600 text-white px-6 py-2 rounded-lg transition-colors flex items-center ${
              isSaving ? 'opacity-75 cursor-not-allowed' : 'hover:bg-green-700'
            }`}
            disabled={isSaving}
          >
            <Save size={18} className="mr-2" />
            {isSaving ? 'Salvando...' : 'Salvar Alterações'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FreightConfigModal;
