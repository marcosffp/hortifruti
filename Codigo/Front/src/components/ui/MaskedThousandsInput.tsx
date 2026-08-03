"use client";

import { useEffect, useState } from "react";

function formatThousands(units: string): string {
  const withSeparators = units.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${withSeparators}.000,00`;
}

function unitsToValue(units: string): number {
  if (!units) return 0;
  return parseInt(units, 10) * 1000;
}

function valueToUnits(value: number): string {
  if (!value || Number.isNaN(value)) return "";
  return Math.round(value / 1000).toString();
}

interface MaskedThousandsInputProps {
  value: number;
  onChange: (value: number) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  id?: string;
}

/**
 * Campo para valores fechados em milhares: o usuário digita só a casa dos milhares (ex.: "5" vira
 * R$ 5.000,00; "17" vira R$ 17.000,00) e o campo completa os zeros sozinho, em vez de exigir digitar
 * o valor por extenso — pensado para a contagem de dinheiro em espécie, que costuma fechar em
 * milhares redondos.
 */
export default function MaskedThousandsInput({
  value,
  onChange,
  placeholder,
  className,
  disabled,
  id,
}: MaskedThousandsInputProps) {
  const [units, setUnits] = useState(() => valueToUnits(value));

  useEffect(() => {
    setUnits((prev) =>
      unitsToValue(prev) === value ? prev : valueToUnits(value),
    );
  }, [value]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const onlyDigits = e.target.value
      .replace(/\D/g, "")
      .replace(/^0+(?=\d)/, "")
      .slice(0, 6);
    setUnits(onlyDigits);
    onChange(unitsToValue(onlyDigits));
  };

  return (
    <input
      type="text"
      inputMode="numeric"
      value={units ? formatThousands(units) : ""}
      onChange={handleChange}
      placeholder={placeholder}
      disabled={disabled}
      className={className}
      id={id}
    />
  );
}
