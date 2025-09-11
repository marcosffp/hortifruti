export interface ErrorType {
  message: string;
  code?: number;
}

export function isErrorType(error: any): error is ErrorType {
  return error && typeof error.message === 'string';
}

export function getErrorMessage(error: any): string {
  if (isErrorType(error)) {
    return error.message;
  }
  return 'Erro desconhecido';
}