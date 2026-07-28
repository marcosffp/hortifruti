"use client";

import { useEffect, useState } from "react";

function formatDigitsAsDecimal(digits: string, decimalPlaces: number): string {
  const padded = digits.padStart(decimalPlaces + 1, "0");
  const integerPart = padded
    .slice(0, padded.length - decimalPlaces)
    .replace(/^0+(?=\d)/, "");
  const decimalPart = padded.slice(padded.length - decimalPlaces);
  const withThousands = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return decimalPlaces > 0 ? `${withThousands},${decimalPart}` : withThousands;
}

function digitsToNumber(digits: string, decimalPlaces: number): number {
  if (!digits) return 0;
  return parseInt(digits, 10) / 10 ** decimalPlaces;
}

function numberToDigits(value: number, decimalPlaces: number): string {
  if (!value || Number.isNaN(value)) return "";
  return Math.round(value * 10 ** decimalPlaces).toString();
}

interface MaskedDecimalInputProps {
  value: number;
  onChange: (value: number) => void;
  decimalPlaces?: number;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

/**
 * Campo numérico no estilo "boleto/PIX": só aceita dígitos, e a vírgula decimal vai se ajustando
 * conforme o usuário digita (ex.: "1" -> 0,01; "150" -> 1,50), em vez de exigir que o usuário
 * digite o separador decimal manualmente.
 */
export default function MaskedDecimalInput({
  value,
  onChange,
  decimalPlaces = 2,
  placeholder,
  className,
  disabled,
}: MaskedDecimalInputProps) {
  const [digits, setDigits] = useState(() =>
    numberToDigits(value, decimalPlaces),
  );

  // biome-ignore lint/correctness/useExhaustiveDependencies: só deve resincronizar quando o valor vier de fora (ex.: reset de linha), não a cada dígito já refletido em `onChange`
  useEffect(() => {
    setDigits((prev) =>
      digitsToNumber(prev, decimalPlaces) === value
        ? prev
        : numberToDigits(value, decimalPlaces),
    );
  }, [value]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const onlyDigits = e.target.value
      .replace(/\D/g, "")
      .replace(/^0+(?=\d)/, "")
      .slice(0, 9);
    setDigits(onlyDigits);
    onChange(digitsToNumber(onlyDigits, decimalPlaces));
  };

  return (
    <input
      type="text"
      inputMode="numeric"
      value={digits ? formatDigitsAsDecimal(digits, decimalPlaces) : ""}
      onChange={handleChange}
      placeholder={placeholder}
      disabled={disabled}
      className={className}
    />
  );
}
