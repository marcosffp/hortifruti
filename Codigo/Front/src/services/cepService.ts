"use client";

interface CepResponse {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  localidade: string; // Cidade
  uf: string; // Estado
  ibge?: string;
  gia?: string;
  ddd?: string;
  siafi?: string;
  erro?: boolean;
}

/**
 * Serviço para consulta de CEPs via API ViaCEP
 */
export const cepService = {
  async consultarCep(cep: string): Promise<CepResponse | null> {
    try {
      const cepLimpo = cep.replace(/\D/g, '');

      if (cepLimpo.length !== 8) {
        return null;
      }

      const response = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`);

      if (!response.ok) {
        throw new Error(`Erro ao consultar CEP: ${response.status}`);
      }
      
      const data: CepResponse = await response.json();
      
      // Verificar se a API retornou erro
      if (data.erro) {
        return null;
      }
      
      return data;
    } catch (error) {
      console.error("Erro ao consultar CEP:", error);
      return null;
    }
  }
};