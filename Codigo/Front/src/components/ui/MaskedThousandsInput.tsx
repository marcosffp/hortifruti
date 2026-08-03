"use client";

import { useLayoutEffect, useRef, useState } from "react";

const SUFFIX = ".000,00";

function formatIntegerWithSeparators(units: string): string {
  return units.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function unitsToValue(units: string): number {
  if (!units) return 0;
  return parseInt(units, 10) * 1000;
}

function valueToUnits(value: number): string {
  if (!value || Number.isNaN(value)) return "";
  return Math.round(value / 1000).toString();
}

/**
 * Extrai os dígitos "reais" (a casa dos milhares) do valor bruto do input, descartando o sufixo fixo
 * ".000,00". Não dá pra simplesmente tirar tudo que não é dígito do valor inteiro: como o sufixo
 * também tem dígitos (os zeros decorativos), isso inflaria o valor caso o cursor termine depois
 * dele — o que é o comportamento padrão do navegador sempre que o texto de um input controlado muda
 * (o cursor pula pro final). Por isso o sufixo é removido primeiro, e só o que sobra é tratado como
 * dígito digitado.
 */
function extractUnits(raw: string): string {
  const withoutSuffix = raw.endsWith(SUFFIX)
    ? raw.slice(0, raw.length - SUFFIX.length)
    : raw;
  return withoutSuffix
    .replace(/\D/g, "")
    .replace(/^0+(?=\d)/, "")
    .slice(0, 6);
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
 *
 * Também reposiciona o cursor a cada tecla, sempre logo antes do sufixo ".000,00": sem isso, o
 * cursor iria parar no final do texto reformatado (comportamento padrão de inputs controlados) e o
 * próximo dígito digitado entraria depois do ",00", corrompendo o valor.
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
  const inputRef = useRef<HTMLInputElement>(null);

  useLayoutEffect(() => {
    setUnits((prev) =>
      unitsToValue(prev) === value ? prev : valueToUnits(value),
    );
  }, [value]);

  useLayoutEffect(() => {
    const input = inputRef.current;
    if (!input || !units || document.activeElement !== input) return;
    const caretPos = formatIntegerWithSeparators(units).length;
    input.setSelectionRange(caretPos, caretPos);
  }, [units]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const onlyDigits = extractUnits(e.target.value);
    setUnits(onlyDigits);
    onChange(unitsToValue(onlyDigits));
  };

  return (
    <input
      ref={inputRef}
      type="text"
      inputMode="numeric"
      value={units ? `${formatIntegerWithSeparators(units)}${SUFFIX}` : ""}
      onChange={handleChange}
      placeholder={placeholder}
      disabled={disabled}
      className={className}
      id={id}
    />
  );
}
