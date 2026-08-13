// Faixa Unicode dos diacríticos combinantes (acentos após NFD); construída via charCode em vez de
// escape \u literal no código-fonte para evitar ambiguidade de encoding entre editores.
const DIACRITICS_REGEX = new RegExp(
  `[${String.fromCharCode(0x0300)}-${String.fromCharCode(0x036f)}]`,
  "g",
);

export function normalize(value: string): string {
  return value
    .normalize("NFD")
    .replace(DIACRITICS_REGEX, "")
    .toLowerCase()
    .trim();
}
