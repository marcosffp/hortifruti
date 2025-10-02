"use client";

import { useEffect, useState } from "react";
import Card from "@/components/ui/Card";
import { productService, ProductRecommendation, ProductRequest, WeatherForecast } from "@/services/productService";
import { showError, showSuccess } from "@/services/notificationService";
import { RiSunLine, RiCloudyLine, RiDrizzleLine, RiThunderstormsLine, RiSnowyLine, RiLeafLine } from "react-icons/ri";
import { WiHumidity } from "react-icons/wi";
import { FaThermometerHalf, FaWind, FaArrowUp, FaArrowDown } from "react-icons/fa";
import { TbTruckDelivery, TbChartBar } from "react-icons/tb";
import { Edit, Trash2 } from "lucide-react";

export default function RecommendationPage() {
  const [recommendations, setRecommendations] = useState<ProductRecommendation[]>([]);
  const [weather, setWeather] = useState<WeatherForecast | null>(null);
  const [loading, setLoading] = useState(true);
  const [weatherLoading, setWeatherLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [newProduct, setNewProduct] = useState<ProductRequest>({
    name: "",
    temperatureCategory: "AMENO",
    peakSalesMonths: [],
    lowSalesMonths: [],
  });
  const [editingProduct, setEditingProduct] = useState<{id: number, data: ProductRequest} | null>(null);
  const [productToDelete, setProductToDelete] = useState<number | null>(null);
  const [error, setError] = useState("");

  const temperatureCategorias = [
    { value: "CONGELANDO", label: "Congelando (0-5°C)" },
    { value: "FRIO", label: "Frio (6-14°C)" },
    { value: "AMENO", label: "Ameno (15-24°C)" },
    { value: "QUENTE", label: "Quente (25-50°C)" },
  ];

  // Buscar recomendações baseadas em uma data específica
  const loadRecommendationsByDate = async (date: string) => {
    try {
      setLoading(true);
      const data = await productService.getRecommendationsByDate(date);
      setRecommendations(data);
      setSelectedDate(date);
    } catch (err) {
      console.error("Erro ao carregar recomendações para a data:", date, err);
      setError("Não foi possível carregar as recomendações de produtos para esta data.");
    } finally {
      setLoading(false);
    }
  };
  
  // Limpar o filtro por data e mostrar todas as recomendações
  const loadAllRecommendations = async () => {
    try {
      setLoading(true);
      const today = new Date().toISOString().slice(0, 10);
      const data = await productService.getRecommendationsByDate(today);
      setRecommendations(data);
      setSelectedDate(null);
    } catch (err) {
      console.error("Erro ao carregar todas as recomendações:", err);
      setError("Não foi possível carregar as recomendações de produtos.");
    } finally {
      setLoading(false);
    }
  };

  // Buscar recomendações e previsão do tempo do backend
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setWeatherLoading(true);
        
        const today = new Date().toISOString().slice(0, 10);
        // Carregar recomendações para hoje por padrão
        await loadRecommendationsByDate(today);
        
        // Carregar previsão do tempo
        try {
          const weatherData = await productService.getWeatherForecast();
          setWeather(weatherData);
        } catch (weatherErr) {
          console.error("Erro ao carregar previsão do tempo:", weatherErr);
        } finally {
          setWeatherLoading(false);
        }
      } catch (err) {
        console.error("Erro ao carregar recomendações:", err);
        setError("Não foi possível carregar as recomendações de produtos.");
      } finally {
        setLoading(false);
      }
    };
    
    loadData();
  }, []);

  // Adicionar novo produto
  const handleAddProduct = async () => {
    if (!newProduct.name || !newProduct.temperatureCategory) {
      setError("Nome e categoria de temperatura são obrigatórios");
      return;
    }

    try {
      setLoading(true);
      await productService.createProduct(newProduct);
      showSuccess("Produto adicionado com sucesso!");
      setShowAdd(false);
      setNewProduct({ 
        name: "", 
        temperatureCategory: "AMENO", 
        peakSalesMonths: [], 
        lowSalesMonths: [] 
      });
      
      // Atualiza recomendações após adicionar
      const data = await productService.getRecommendationsByDate(new Date().toISOString().slice(0, 10));
      setRecommendations(data);
      setError("");
    } catch (err) {
      console.error("Erro ao adicionar produto:", err);
      setError("Não foi possível adicionar o produto.");
    } finally {
      setLoading(false);
    }
  };
  
  // Função para converter string de meses em números
  const handleMonthsChange = (value: string, field: 'peakSalesMonths' | 'lowSalesMonths', isEditing: boolean = false) => {
    const months = value.split(',')
      .map(m => m.trim())
      .filter(Boolean)
      .map(m => parseInt(m))
      .filter(m => !isNaN(m) && m >= 1 && m <= 12);
    
    if (isEditing && editingProduct) {
      setEditingProduct({
        ...editingProduct,
        data: {
          ...editingProduct.data,
          [field]: months
        }
      });
    } else {
      setNewProduct(prev => ({
        ...prev,
        [field]: months
      }));
    }
  };
  
  // Preparar produto para edição
  const handleEditProduct = (productId: number, productName: string, temperatureCategory: string) => {
    setEditingProduct({
      id: productId,
      data: {
        name: productName,
        temperatureCategory: temperatureCategory as any,
        peakSalesMonths: [],
        lowSalesMonths: []
      }
    });
    setShowEdit(true);
  };

  // Salvar as edições do produto
  const handleSaveEdit = async () => {
    if (!editingProduct) return;
    
    if (!editingProduct.data.name || !editingProduct.data.temperatureCategory) {
      setError("Nome e categoria de temperatura são obrigatórios");
      return;
    }

    try {
      setLoading(true);
      await productService.updateProduct(editingProduct.id, editingProduct.data);
      showSuccess("Produto atualizado com sucesso!");
      setShowEdit(false);
      setEditingProduct(null);
      
      // Atualiza recomendações após editar
      if (selectedDate) {
        const data = await productService.getRecommendationsByDate(selectedDate);
        setRecommendations(data);
      } else {
        const today = new Date().toISOString().slice(0, 10);
        const data = await productService.getRecommendationsByDate(today);
        setRecommendations(data);
      }
      setError("");
    } catch (err) {
      console.error("Erro ao atualizar produto:", err);
      setError("Não foi possível atualizar o produto.");
    } finally {
      setLoading(false);
    }
  };

  // Preparar exclusão do produto
  const handleDeleteProduct = (productId: number) => {
    setProductToDelete(productId);
  };

  // Confirmar exclusão do produto
  const handleConfirmDelete = async () => {
    if (productToDelete === null) return;
    
    try {
      setLoading(true);
      await productService.deleteProduct(productToDelete);
      showSuccess("Produto excluído com sucesso!");
      setProductToDelete(null);
      
      // Atualiza recomendações após excluir
      if (selectedDate) {
        const data = await productService.getRecommendationsByDate(selectedDate);
        setRecommendations(data);
      } else {
        const today = new Date().toISOString().slice(0, 10);
        const data = await productService.getRecommendationsByDate(today);
        setRecommendations(data);
      }
    } catch (err) {
      console.error("Erro ao excluir produto:", err);
      showError("Não foi possível excluir o produto.");
    } finally {
      setLoading(false);
    }
  };

  // Obter cor baseada na tag
  const getTagColor = (tag: string) => {
    switch (tag) {
      case 'BOM': return 'bg-[var(--primary)] text-white';
      case 'MEDIO': return 'bg-[#f39c12] text-white';
      case 'RUIM': return 'bg-[var(--secondary)] text-white';
      default: return 'bg-gray-200';
    }
  };
  
  // Função para selecionar o ícone correto com base na descrição do clima
  const getWeatherIcon = (description: string | undefined) => {
    if (!description) return <RiCloudyLine size={36} />;
    
    const desc = description.toLowerCase();
    if (desc.includes('sol') || desc.includes('limpo') || desc.includes('clear')) {
      return <RiSunLine size={36} className="text-yellow-500" />;
    } else if (desc.includes('chuv') || desc.includes('rain')) {
      return <RiDrizzleLine size={36} className="text-blue-400" />;
    } else if (desc.includes('tempestade') || desc.includes('thunder')) {
      return <RiThunderstormsLine size={36} className="text-purple-500" />;
    } else if (desc.includes('nev') || desc.includes('snow')) {
      return <RiSnowyLine size={36} className="text-blue-200" />;
    } else {
      return <RiCloudyLine size={36} className="text-gray-400" />;
    }
  };

  return (
    <div className="min-h-screen bg-[var(--neutral-100)] p-4 font-[var(--font-body)]">
      <h2 className="text-xl font-semibold mb-2 flex items-center">
        <RiCloudyLine className="mr-2 text-blue-500" /> 
        Previsão do Tempo e Impacto na Demanda
      </h2>
      
      {/* Cards de previsão do tempo - Estilo similar ao mockup */}
      <div className="mb-8">
        <h3 className="text-md font-semibold mb-2 text-gray-700">
          Previsão Estendida do Tempo (próximos 5 dias)
          <span className="text-sm font-normal text-gray-500 ml-2">
            Última atualização: {new Date().toLocaleString('pt-BR')}
          </span>
        </h3>
       
        
        {weatherLoading ? (
          <div className="p-4 bg-white rounded-lg shadow">
            <p className="text-center text-[var(--neutral-600)]">Carregando previsão do tempo...</p>
          </div>
        ) : weather && weather.dailyForecasts && weather.dailyForecasts.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
            {weather.dailyForecasts.slice(0, 5).map((forecast, index) => {
              const date = new Date(forecast.date);
              const formattedDate = `${date.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')}, ${date.getDate()}/${date.getMonth() + 1}`;
              
              // Determinar a condição climática
              let condicao = 'estáveis';
              if (forecast.humidity > 70) condicao = 'úmidas';
              else if (forecast.humidity < 30) condicao = 'secas';
              
              const isSelected = selectedDate === forecast.date;
              return (
                <div 
                  key={index} 
                  className={`bg-white rounded-lg shadow overflow-hidden cursor-pointer transition-all hover:shadow-lg hover:border-[var(--primary)] hover:-translate-y-1 ${isSelected ? 'ring-2 ring-[var(--primary)] scale-105' : ''} relative`}
                  onClick={() => loadRecommendationsByDate(forecast.date)}
                >
                  <div className={`${isSelected ? 'bg-[var(--primary)]' : 'bg-[var(--primary-light)]'} text-center py-2`}>
                    <p className={`font-medium text-white`}>{formattedDate}</p>
                  </div>
                  <div className="p-3 text-center">
                    <div className="flex justify-center">
                      {getWeatherIcon(forecast.weatherDescription)}
                    </div>
                    <p className="text-sm font-medium mt-1 text-gray-600">
                      {forecast.weatherDescription || 'Parcialmente nublado'}
                    </p>
                    <div className="flex items-center justify-center mt-2">
                      <FaThermometerHalf className="text-red-500 mr-1" />
                      <p className="text-2xl font-bold text-amber-500">
                        {Math.round(forecast.avgTemp)}°C
                      </p>
                    </div>
                    <div className="flex items-center justify-center mt-1">
                      <WiHumidity className="text-blue-500 mr-1" size={20} />
                      <p className="text-sm text-gray-500">
                        {forecast.humidity}%
                      </p>
                      <FaWind className="text-gray-500 ml-2 mr-1" size={12} />
                      <p className="text-sm text-gray-500">
                        {Math.round(forecast.windSpeed || 0)} km/h
                      </p>
                    </div>
                    <p className="text-xs mt-2 border-t pt-2 text-[var(--neutral-600)]">
                      Condições climáticas {condicao}
                    </p>
                    <div className="mt-2 text-center">
                      <p className="text-xs font-bold">Impacto na Demanda</p>
                      <p className="text-sm font-medium text-blue-600">{index % 2 === 0 ? '+10%' : '0%'}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="p-4 bg-white rounded-lg shadow">
            <p className="text-center text-[var(--neutral-600)]">Dados climáticos não disponíveis</p>
          </div>
        )}
      </div>
      
      <div className="my-8 flex justify-between items-center">
        <div className="flex items-center">
          <h2 className="text-xl font-semibold flex items-center">
            <TbChartBar className="mr-2 text-[var(--primary)]" /> 
            {selectedDate ? 'Recomendações para ' + new Date(selectedDate).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long' }) : 'Recomendações de Produtos'}
            <span className="bg-[var(--primary)] text-white text-xs py-1 px-2 rounded-full ml-2">
              {recommendations.length}
            </span>
          </h2>
          {selectedDate && (
            <div className="ml-4 flex items-center">
              <span className="text-sm text-gray-600 mr-2">
                para {new Date(selectedDate).toLocaleDateString('pt-BR')}
              </span>
              <button
                className="bg-[var(--neutral-100)] hover:bg-[var(--neutral-200)] text-[var(--primary)] border border-[var(--primary)] px-3 py-1 rounded-md text-sm flex items-center transition-all"
                onClick={loadAllRecommendations}
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
                Ver todas as recomendações
              </button>
            </div>
          )}
        </div>
        <button
          className="bg-[var(--primary)] hover:bg-[var(--primary-dark)] text-white px-4 py-2 rounded flex items-center"
          onClick={() => setShowAdd(prev => !prev)}
        >
          {showAdd ? 
            <span className="flex items-center">
              <span className="mr-1">✕</span> Cancelar
            </span> : 
            <span className="flex items-center">
              <span className="mr-1">+</span> Adicionar Produto
            </span>
          }
        </button>
      </div>

      {showAdd && (
        <Card title="Adicionar Produto">
          <div className="flex flex-col gap-3 mb-6">
            <label className="text-sm text-[var(--neutral-700)]">Nome do Produto*</label>
            <input
              className="border border-[var(--neutral-300)] rounded px-3 py-2"
              placeholder="Ex: Tomate"
              value={newProduct.name}
              onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
            />
            
            <label className="text-sm text-[var(--neutral-700)]">Categoria de Temperatura*</label>
            <select
              className="border border-[var(--neutral-300)] rounded px-3 py-2"
              value={newProduct.temperatureCategory}
              onChange={(e) => setNewProduct({ 
                ...newProduct, 
                temperatureCategory: e.target.value as any 
              })}
            >
              {temperatureCategorias.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
            
            <label className="text-sm text-[var(--neutral-700)]">Meses de Pico de Vendas</label>
            <input
              className="border border-[var(--neutral-300)] rounded px-3 py-2"
              placeholder="Ex: 1,2,3 (separados por vírgula)"
              value={newProduct.peakSalesMonths.join(',')}
              onChange={(e) => handleMonthsChange(e.target.value, 'peakSalesMonths')}
            />
            
            <label className="text-sm text-[var(--neutral-700)]">Meses de Baixa nas Vendas</label>
            <input
              className="border border-[var(--neutral-300)] rounded px-3 py-2"
              placeholder="Ex: 7,8,9 (separados por vírgula)"
              value={newProduct.lowSalesMonths.join(',')}
              onChange={(e) => handleMonthsChange(e.target.value, 'lowSalesMonths')}
            />
            
            <button
              className="bg-[var(--primary)] hover:bg-[var(--primary-dark)] text-white px-4 py-2 rounded mt-2"
              onClick={handleAddProduct}
            >
              Salvar Produto
            </button>
            {error && <span className="text-[var(--secondary)]">{error}</span>}
          </div>
        </Card>
      )}

      {loading ? (
        <div className="flex justify-center items-center h-40">
          <div className="text-[var(--primary-dark)] text-lg">Carregando recomendações...</div>
        </div>
      ) : recommendations.length === 0 ? (
        <div className="bg-white border border-[var(--neutral-300)] rounded-lg p-6 text-center">
          <p className="text-[var(--neutral-600)]">
            Nenhuma recomendação disponível. Adicione produtos para receber recomendações baseadas no clima.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {recommendations.map((rec, index) => {
            const isPositive = rec.tag === 'BOM';
            const isNeutral = rec.tag === 'MEDIO';
            const impactoValue = isPositive ? '+20%' : isNeutral ? '+15%' : '-5%';
            const impactoClass = isPositive ? 'text-green-500' : isNeutral ? 'text-amber-500' : 'text-red-500';
            const recommendedAction = isPositive ? 'Aumentar compra do fornecedor' : isNeutral ? 'Manter compras regulares' : 'Reduzir compras';
            const periodo = `A partir de ${new Date().toLocaleDateString('pt-BR')}`;
            
            return (
              <div key={rec.productId} className="bg-white border border-[var(--neutral-300)] rounded-lg shadow-sm overflow-hidden">
                <div className="p-4">
                  <div className="flex justify-between items-center mb-3">
                    <h3 className="text-lg font-semibold text-[var(--neutral-900)]">{rec.name}</h3>
                    <span className={`text-xs font-medium px-2 py-1 rounded ${isPositive ? 'bg-green-100 text-green-800' : isNeutral ? 'bg-amber-100 text-amber-800' : 'bg-red-100 text-red-800'}`}>
                      {isPositive ? 'Melhor' : isNeutral ? 'Médio' : 'Ruim'}
                    </span>
                  </div>
                  
                  <div className="mb-3">
                    <div className="flex items-center mb-1">
                      <span className="text-sm mr-2">Impacto estimado:</span>
                      <span className={`font-bold ${impactoClass}`}>{impactoValue}</span>
                    </div>
                    
                    <div className="text-sm mb-3">
                      <div className={`p-2 rounded-md mb-2 ${isPositive ? 'bg-green-50 border border-green-100' : isNeutral ? 'bg-amber-50 border border-amber-100' : 'bg-red-50 border border-red-100'}`}>
                        <p className="font-medium mb-1 flex items-center">
                          {isPositive ? 
                            <FaArrowUp className="text-green-500 mr-1" /> : 
                            isNeutral ? 
                              <span className="inline-block w-4 h-0.5 bg-amber-500 mr-1"></span> : 
                              <FaArrowDown className="text-red-500 mr-1" />
                          }
                          Ação sugerida:
                        </p>
                        <p className="text-gray-700">{recommendedAction}</p>
                      </div>
                      <p><span className="font-medium">Período:</span> {periodo}</p>
                    </div>
                    
                    <div className="bg-gray-100 p-3 rounded mt-2 text-sm">
                      <h4 className="font-medium mb-2 text-gray-700 flex items-center">
                        <TbChartBar className="mr-1 text-[var(--primary)]" size={18} /> 
                        Análise de Dados
                      </h4>
                      
                      <div className="grid grid-cols-2 gap-2">
                        <div className="bg-white p-1.5 rounded shadow-sm">
                          <div className="flex items-center mb-1">
                            <FaThermometerHalf className="text-red-500 mr-1" size={14} />
                            <span className="text-gray-700 text-xs">Temperatura</span>
                          </div>
                          <p className="font-medium text-gray-800">{rec.temperatureCategory}</p>
                        </div>
                        
                        <div className="bg-white p-1.5 rounded shadow-sm">
                          <div className="flex items-center mb-1">
                            <WiHumidity className="text-blue-500 mr-1" size={16} />
                            <span className="text-gray-700 text-xs">Umidade</span>
                          </div>
                          <p className="font-medium text-gray-800">{index % 2 === 0 ? 'Alta' : 'Média'}</p>
                        </div>
                        
                        <div className="bg-white p-1.5 rounded shadow-sm">
                          <div className="flex items-center mb-1">
                            <TbChartBar className="text-purple-500 mr-1" size={14} />
                            <span className="text-gray-700 text-xs">Sazonalidade</span>
                          </div>
                          <p className="font-medium text-gray-800">{index % 2 === 0 ? 'Alta' : 'Média'}</p>
                        </div>
                        
                        <div className="bg-white p-1.5 rounded shadow-sm">
                          <div className="flex items-center mb-1">
                            {index % 2 === 0 ? 
                              <FaArrowUp className="text-green-500 mr-1" size={14} /> : 
                              <FaArrowDown className="text-red-500 mr-1" size={14} />
                            }
                            <span className="text-gray-700 text-xs">Histórico</span>
                          </div>
                          <p className={`font-medium ${index % 2 === 0 ? 'text-green-600' : 'text-red-600'}`}>
                            {index % 2 === 0 ? '+15%' : '-10%'}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                {/* Botões de ação */}
                <div className="flex justify-end p-3 border-t border-gray-100">
                  <button
                    onClick={() => handleEditProduct(rec.productId, rec.name, rec.temperatureCategory)}
                    className="h-9 w-9 flex items-center justify-center text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-all mr-2"
                    aria-label="Editar produto"
                    title="Editar produto"
                  >
                    <Edit size={16} />
                  </button>
                  <button
                    onClick={() => handleDeleteProduct(rec.productId)}
                    className="h-9 w-9 flex items-center justify-center text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all"
                    aria-label="Excluir produto"
                    title="Excluir produto"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
                <div className={`h-1.5 ${isPositive ? 'bg-green-500' : isNeutral ? 'bg-amber-500' : 'bg-red-500'}`}></div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal de edição de produto */}
      {showEdit && editingProduct && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center">
              <Edit className="mr-2 text-blue-600" size={20} />
              Editar Produto
            </h3>
            
            <div className="flex flex-col gap-3 mb-6">
              <label className="text-sm text-[var(--neutral-700)]">Nome do Produto*</label>
              <input
                className="border border-[var(--neutral-300)] rounded px-3 py-2"
                placeholder="Ex: Tomate"
                value={editingProduct.data.name}
                onChange={(e) => setEditingProduct({
                  ...editingProduct,
                  data: { ...editingProduct.data, name: e.target.value }
                })}
              />
              
              <label className="text-sm text-[var(--neutral-700)]">Categoria de Temperatura*</label>
              <select
                className="border border-[var(--neutral-300)] rounded px-3 py-2"
                value={editingProduct.data.temperatureCategory}
                onChange={(e) => setEditingProduct({
                  ...editingProduct,
                  data: { ...editingProduct.data, temperatureCategory: e.target.value as any }
                })}
              >
                {temperatureCategorias.map((cat) => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
              
              <label className="text-sm text-[var(--neutral-700)]">Meses de Pico de Vendas</label>
              <input
                className="border border-[var(--neutral-300)] rounded px-3 py-2"
                placeholder="Ex: 1,2,3 (separados por vírgula)"
                value={editingProduct.data.peakSalesMonths.join(',')}
                onChange={(e) => handleMonthsChange(e.target.value, 'peakSalesMonths', true)}
              />
              
              <label className="text-sm text-[var(--neutral-700)]">Meses de Baixa nas Vendas</label>
              <input
                className="border border-[var(--neutral-300)] rounded px-3 py-2"
                placeholder="Ex: 7,8,9 (separados por vírgula)"
                value={editingProduct.data.lowSalesMonths.join(',')}
                onChange={(e) => handleMonthsChange(e.target.value, 'lowSalesMonths', true)}
              />
              
              {error && <span className="text-[var(--secondary)]">{error}</span>}
              
              <div className="flex justify-end space-x-3 pt-4">
                <button
                  onClick={() => {
                    setShowEdit(false);
                    setEditingProduct(null);
                    setError("");
                  }}
                  className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition-colors"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleSaveEdit}
                  className="px-4 py-2 bg-[var(--primary)] hover:bg-[var(--primary-dark)] text-white rounded-md"
                >
                  Salvar Alterações
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* Modal de confirmação de exclusão */}
      {productToDelete !== null && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center">
              <Trash2 className="mr-2 text-red-600" size={20} />
              Confirmar exclusão
            </h3>
            <p className="text-gray-600 mb-6">
              Tem certeza que deseja excluir este produto? Esta ação não pode ser desfeita.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setProductToDelete(null)}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmDelete}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}