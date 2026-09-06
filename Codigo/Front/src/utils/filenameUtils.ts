import { normalize } from "@/utils/textSearch";

// Remove acentos e qualquer caractere que não seja seguro em nome de arquivo (mantém letras,
// números e "_"), pra evitar nomes de PDF quebrados em downloads.
export function toFilenameSafe(value: string): string {
  return normalize(value)
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export function getFirstName(fullName: string): string {
  return fullName.trim().split(/\s+/)[0] ?? fullName;
}
