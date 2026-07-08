/**
 * Utilidades para validação de formulários no frontend
 */

export function validarCPF(cpf: string): boolean {
  if (!cpf) return false;

  cpf = cpf.replace(/[^\d]/g, '');

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

export function validarCNPJ(cnpj: string): boolean {
  if (!cnpj) return false;

  cnpj = cnpj.replace(/[^\d]/g, '');

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

export function validarCPFouCNPJ(valor: string): boolean {
  if (!valor) return false;

  const apenasNumeros = valor.replace(/[^\d]/g, '');

  if (apenasNumeros.length === 11) {
    return validarCPF(apenasNumeros);
  } else if (apenasNumeros.length === 14) {
    return validarCNPJ(apenasNumeros);
  }

  return false;
}

export function formatarCPF(cpf: string): string {
  if (!cpf) return '';

  cpf = cpf.replace(/[^\d]/g, '');

  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

export function formatarCNPJ(cnpj: string): string {
  if (!cnpj) return '';

  cnpj = cnpj.replace(/[^\d]/g, '');

  return cnpj.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
}

export function validarTelefone(telefone: string): boolean {
  if (!telefone) return false;

  const apenasNumeros = telefone.replace(/[^\d]/g, '');

  // Telefone precisa ter entre 10 (fixo) e 11 (celular) dígitos
  if (apenasNumeros.length < 10 || apenasNumeros.length > 11) return false;

  // Se for celular (11 dígitos), o nono dígito deve ser 9
  if (apenasNumeros.length === 11 && apenasNumeros.charAt(2) !== '9') return false;

  return true;
}

export function formatarTelefone(telefone: string): string {
  if (!telefone) return '';

  telefone = telefone.replace(/[^\d]/g, '');

  if (telefone.length === 11) {
    return telefone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  } else if (telefone.length === 10) {
    return telefone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
  }

  return telefone;
}

export function validarCEP(cep: string): boolean {
  if (!cep) return false;

  const apenasNumeros = cep.replace(/[^\d]/g, '');

  return apenasNumeros.length === 8;
}

export function formatarCEP(cep: string): string {
  if (!cep) return '';

  cep = cep.replace(/[^\d]/g, '');

  return cep.replace(/(\d{5})(\d{3})/, '$1-$2');
}

export function validarEmail(email: string): boolean {
  if (!email) return false;

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
  const numbers = value.replace(/\D/g, "");

  const limited = numbers.slice(0, 13);

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
  const numbers = ie.replace(/\D/g, "");

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

    let soma = 0;
    for (let i = 0; i < 12; i++) {
      let produto = ieArray[i] * pesos1[i];
      // Se o produto for >= 10, soma os dígitos
      if (produto >= 10) {
        produto = Math.floor(produto / 10) + (produto % 10);
      }
      soma += produto;
    }

    let digito = 10 - (soma % 10);
    if (digito === 10) digito = 0;

    return digito === ieArray[12];
  } catch {
    return false;
  }
}
