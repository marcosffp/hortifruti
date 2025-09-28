"use client";

/**
 * Interface para o resultado da consulta de CEP
 */
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
  /**
   * Busca informações de endereço a partir de um CEP
   * @param cep CEP a ser consultado (apenas números ou formatado)
   * @returns Dados do endereço ou null se não encontrado
   */
  async consultarCep(cep: string): Promise<CepResponse | null> {
    try {
      // Remover caracteres não numéricos
      const cepLimpo = cep.replace(/\D/g, '');
      
      // Verificar se o CEP tem 8 dígitos
      if (cepLimpo.length !== 8) {
        return null;
      }
      
      // Fazer a requisição para a API ViaCEP
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