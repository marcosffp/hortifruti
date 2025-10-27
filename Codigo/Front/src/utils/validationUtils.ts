/**
 * Utilidades para validação de formulários no frontend
 */

/**
 * Valida um CPF
 * @param cpf CPF a ser validado (pode conter pontuação ou não)
 * @returns true se o CPF é válido, false caso contrário
 */
export function validarCPF(cpf: string): boolean {
  if (!cpf) return false;
  
  // Remove caracteres não numéricos
  cpf = cpf.replace(/[^\d]/g, '');
  
  // Verifica se tem 11 dígitos
  if (cpf.length !== 11) return false;
  
  // Verifica se todos os dígitos são iguais (CPF inválido, mas com formato correto)
  if (/^(\d)\1+$/.test(cpf)) return false;
  
  // Validação do primeiro dígito verificador
  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += parseInt(cpf.charAt(i)) * (10 - i);
  }
  
  let resto = soma % 11;
  let digitoVerificador1 = resto < 2 ? 0 : 11 - resto;
  
  if (digitoVerificador1 !== parseInt(cpf.charAt(9))) return false;
  
  // Validação do segundo dígito verificador
  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(cpf.charAt(i)) * (11 - i);
  }
  
  resto = soma % 11;
  let digitoVerificador2 = resto < 2 ? 0 : 11 - resto;
  
  return digitoVerificador2 === parseInt(cpf.charAt(10));
}

/**
 * Valida um CNPJ
 * @param cnpj CNPJ a ser validado (pode conter pontuação ou não)
 * @returns true se o CNPJ é válido, false caso contrário
 */
export function validarCNPJ(cnpj: string): boolean {
  if (!cnpj) return false;
  
  // Remove caracteres não numéricos
  cnpj = cnpj.replace(/[^\d]/g, '');
  
  // Verifica se tem 14 dígitos
  if (cnpj.length !== 14) return false;
  
  // Verifica se todos os dígitos são iguais (CNPJ inválido, mas com formato correto)
  if (/^(\d)\1+$/.test(cnpj)) return false;
  
  // Validação do primeiro dígito verificador
  let tamanho = cnpj.length - 2;
  let numeros = cnpj.substring(0, tamanho);
  const digitos = cnpj.substring(tamanho);
  let soma = 0;
  let pos = tamanho - 7;
  
  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i)) * pos--;
    if (pos < 2) pos = 9;
  }
  
  let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  if (resultado !== parseInt(digitos.charAt(0))) return false;
  
  // Validação do segundo dígito verificador
  tamanho = tamanho + 1;
  numeros = cnpj.substring(0, tamanho);
  soma = 0;
  pos = tamanho - 7;
  
  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i)) * pos--;
    if (pos < 2) pos = 9;
  }
  
  resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  
  return resultado === parseInt(digitos.charAt(1));
}

/**
 * Valida um CPF ou CNPJ
 * @param valor CPF ou CNPJ a ser validado
 * @returns true se o documento é válido, false caso contrário
 */
export function validarCPFouCNPJ(valor: string): boolean {
  if (!valor) return false;
  
  // Remove caracteres não numéricos
  const apenasNumeros = valor.replace(/[^\d]/g, '');
  
  // Verifica o tamanho para identificar se é CPF ou CNPJ
  if (apenasNumeros.length === 11) {
    return validarCPF(apenasNumeros);
  } else if (apenasNumeros.length === 14) {
    return validarCNPJ(apenasNumeros);
  }
  
  return false;
}

/**
 * Formata um CPF com a pontuação padrão
 * @param cpf CPF sem formatação
 * @returns CPF formatado (XXX.XXX.XXX-XX)
 */
export function formatarCPF(cpf: string): string {
  if (!cpf) return '';
  
  // Remove caracteres não numéricos
  cpf = cpf.replace(/[^\d]/g, '');
  
  // Formata o CPF
  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

/**
 * Formata um CNPJ com a pontuação padrão
 * @param cnpj CNPJ sem formatação
 * @returns CNPJ formatado (XX.XXX.XXX/XXXX-XX)
 */
export function formatarCNPJ(cnpj: string): string {
  if (!cnpj) return '';
  
  // Remove caracteres não numéricos
  cnpj = cnpj.replace(/[^\d]/g, '');
  
  // Formata o CNPJ
  return cnpj.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
}

/**
 * Valida um número de telefone brasileiro
 * @param telefone Telefone a ser validado
 * @returns true se o telefone é válido, false caso contrário
 */
export function validarTelefone(telefone: string): boolean {
  if (!telefone) return false;
  
  // Remove caracteres não numéricos
  const apenasNumeros = telefone.replace(/[^\d]/g, '');
  
  // Telefone precisa ter entre 10 (fixo) e 11 (celular) dígitos
  if (apenasNumeros.length < 10 || apenasNumeros.length > 11) return false;
  
  // Se for celular (11 dígitos), o nono dígito deve ser 9
  if (apenasNumeros.length === 11 && apenasNumeros.charAt(2) !== '9') return false;
  
  return true;
}

/**
 * Formata um telefone com a pontuação padrão
 * @param telefone Telefone sem formatação
 * @returns Telefone formatado ((XX) XXXXX-XXXX ou (XX) XXXX-XXXX)
 */
export function formatarTelefone(telefone: string): string {
  if (!telefone) return '';
  
  // Remove caracteres não numéricos
  telefone = telefone.replace(/[^\d]/g, '');
  
  // Formata o telefone
  if (telefone.length === 11) {
    return telefone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  } else if (telefone.length === 10) {
    return telefone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
  }
  
  return telefone;
}

/**
 * Valida um CEP
 * @param cep CEP a ser validado
 * @returns true se o CEP é válido, false caso contrário
 */
export function validarCEP(cep: string): boolean {
  if (!cep) return false;
  
  // Remove caracteres não numéricos
  const apenasNumeros = cep.replace(/[^\d]/g, '');
  
  // CEP precisa ter 8 dígitos
  return apenasNumeros.length === 8;
}

/**
 * Formata um CEP com a pontuação padrão
 * @param cep CEP sem formatação
 * @returns CEP formatado (XXXXX-XXX)
 */
export function formatarCEP(cep: string): string {
  if (!cep) return '';
  
  // Remove caracteres não numéricos
  cep = cep.replace(/[^\d]/g, '');
  
  // Formata o CEP
  return cep.replace(/(\d{5})(\d{3})/, '$1-$2');
}

/**
 * Valida um email
 * @param email Email a ser validado
 * @returns true se o email é válido, false caso contrário
 */
export function validarEmail(email: string): boolean {
  if (!email) return false;
  
  // Expressão regular para validação de email
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

export function validarArquivos(selectedFiles: File[]): File[] | string {
  let error = '';
  const validFiles = selectedFiles.filter((file) => {
    // Verificar o tamanho do arquivo (10MB = 10 * 1024 * 1024 bytes)
    if (file.size > 10 * 1024 * 1024) {
      error += `O arquivo ${file.name} excede o limite de 10MB.`;
      return false;
    }

    // Verificar se é um PDF
    if (file.type !== "application/pdf") {
      error += `O arquivo ${file.name} não é um PDF.`;
      return false;
    }

    return true;
  });

  if (error) {
    return error;
  }

  return validFiles;
};

/**
 * Formata Inscrição Estadual de Minas Gerais
 * Formato: XXX.XXX.XXX.XXXX (13 dígitos)
 */
export function formatarIEMinasGerais(value: string): string {
  // Remove tudo que não é dígito
  const numbers = value.replace(/\D/g, "");
  
  // Limita a 13 dígitos
  const limited = numbers.slice(0, 13);
  
  // Aplica a máscara XXX.XXX.XXX.XXXX
  if (limited.length <= 3) {
    return limited;
  } else if (limited.length <= 6) {
    return `${limited.slice(0, 3)}.${limited.slice(3)}`;
  } else if (limited.length <= 9) {
    return `${limited.slice(0, 3)}.${limited.slice(3, 6)}.${limited.slice(6)}`;
  } else {
    return `${limited.slice(0, 3)}.${limited.slice(3, 6)}.${limited.slice(6, 9)}.${limited.slice(9)}`;
  }
}

/**
 * Valida Inscrição Estadual de Minas Gerais
 * Formato esperado: XXX.XXX.XXX.XXXX (13 dígitos)
 */
export function validarIEMinasGerais(ie: string): boolean {
  // Remove pontos e espaços
  const numbers = ie.replace(/\D/g, "");
  
  // Deve ter exatamente 13 dígitos
  if (numbers.length !== 13) {
    return false;
  }
  
  // Validação do dígito verificador (algoritmo de MG)
  try {
    // IE de MG: AAABBBCCCD001P
    // Onde D é o dígito verificador
    const ieArray = numbers.split('').map(Number);
    
    // Pesos para o cálculo: 1,2,1,2,1,2,1,2,1,2,1,2
    const pesos1 = [1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2];
    
    // Multiplica cada dígito pelo peso correspondente
    let soma = 0;
    for (let i = 0; i < 12; i++) {
      let produto = ieArray[i] * pesos1[i];
      // Se o produto for >= 10, soma os dígitos
      if (produto >= 10) {
        produto = Math.floor(produto / 10) + (produto % 10);
      }
      soma += produto;
    }
    
    // Calcula o dígito verificador
    let digito = 10 - (soma % 10);
    if (digito === 10) digito = 0;
    
    // Verifica se o dígito calculado é igual ao informado
    return digito === ieArray[12];
  } catch {
    return false;
  }
}