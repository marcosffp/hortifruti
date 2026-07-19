export interface ErrorType {
  message: string;
  code?: number;
}

export function isErrorType(error: unknown): error is ErrorType {
  return (
    typeof error === "object" &&
    error !== null &&
    typeof (error as ErrorType).message === "string"
  );
}

export function getErrorMessage(error: unknown): string {
  if (isErrorType(error)) {
    return error.message;
  }
  return "Erro desconhecido";
}
