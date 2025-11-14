export interface ParsedAddress {
  endereco: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  estado: string;
  cep: string;
}

/**
 * Parse o endereço que vem do backend no formato:
 * "bairro,cidade,cep,uf,rua, numero, complemento"
 */
export function parseAddressFromBackend(fullAddress: string): ParsedAddress {
  const defaultAddress: ParsedAddress = {
    endereco: "",
    numero: "",
    complemento: "",
    bairro: "",
    cidade: "",
    estado: "",
    cep: "",
  };

  if (!fullAddress) return defaultAddress;

  try {
    // Split por vírgula
    const parts = fullAddress.split(',').map(p => p.trim());

    // Formato vindo do backend: Rua X, 123, Complemento (opcional), Bairro, Cidade - UF, CEP: XXXXX-XXX
    const endereco = parts[0] || "";
    const numero = parts[1] || "";
    const complemento = parts.length === 5 ? parts[2] : "";
    const bairro = parts[parts.length - 3] || "";
    const cidadeUf = parts[parts.length - 2] || "";
    const cepPart = parts[parts.length - 1] || "";

    // Extrai cidade e estado
    const [cidade, estado] = cidadeUf.split(' - ').map(p => p.trim());

    // Extrai CEP
    const cepMatch = cepPart.match(/CEP:\s*(.*)/);
    const cep = cepMatch ? cepMatch[1].trim() : "";

    return {
      endereco,
      numero,
      complemento,
      bairro,
      cidade: cidade || "",
      estado: estado || "",
      cep,
    };
  } catch (error) {
    console.error("Erro ao fazer parsing do endereço:", error);
    return defaultAddress;
  }
}

/**
 * Formata o endereço para enviar ao backend no formato:
 * "Rua X, 123, Complemento (opcional), Bairro, Cidade - UF, CEP: XXXXX-XXX"
 */
export function formatAddressForBackend(address: Partial<ParsedAddress>): string {
  return `
    ${address.endereco || ""}, 
    ${address.numero || ""}
    ${address.complemento ? `, ${address.complemento}` : ""}, 
    ${address.bairro || ""}, 
    ${address.cidade || ""} - ${address.estado || ""}, 
    CEP: ${address.cep || ""}`;
}