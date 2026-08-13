"use client";

import { useEffect, useState } from "react";
import type { FiscalProductType } from "@/types/purchaseType";
import { normalize } from "@/utils/textSearch";

// Distância de edição (Levenshtein) — usada como último critério, pra tolerar erro de digitação
// (troca/falta de letra) quando não há match exato nem por prefixo/substring.
function levenshtein(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  if (m === 0) return n;
  if (n === 0) return m;

  const dp = new Array(n + 1);
  for (let j = 0; j <= n; j++) dp[j] = j;

  for (let i = 1; i <= m; i++) {
    let prev = dp[0];
    dp[0] = i;
    for (let j = 1; j <= n; j++) {
      const temp = dp[j];
      dp[j] =
        a[i - 1] === b[j - 1] ? prev : 1 + Math.min(prev, dp[j], dp[j - 1]);
      prev = temp;
    }
  }
  return dp[n];
}

/**
 * Quanto menor o score, melhor o match. `null` = não é um match razoável.
 * Ordem de prioridade: código exato > código por prefixo > nome por prefixo > nome contém >
 * palavra do nome por prefixo > fuzzy (tolera erro de digitação).
 */
function matchScore(product: FiscalProductType, query: string): number | null {
  const normQuery = normalize(query);
  if (!normQuery) return 5;

  const normCode = normalize(product.code);
  const normDescription = normalize(product.description);

  if (normCode === normQuery) return 0;
  if (normCode.startsWith(normQuery)) return 1;
  if (normDescription.startsWith(normQuery)) return 2;
  if (normDescription.includes(normQuery)) return 3;

  const words = normDescription.split(/\s+/).filter(Boolean);
  if (words.some((word) => word.startsWith(normQuery))) return 3.5;

  let bestRatio = Infinity;
  for (const candidate of [normDescription, ...words]) {
    if (!candidate) continue;
    const distance = levenshtein(normQuery, candidate);
    const ratio = distance / Math.max(normQuery.length, candidate.length);
    if (ratio < bestRatio) bestRatio = ratio;
  }

  return bestRatio <= 0.4 ? 4 + bestRatio : null;
}

function getSuggestions(
  products: FiscalProductType[],
  query: string,
  limit = 8,
): FiscalProductType[] {
  return products
    .map((product) => ({ product, score: matchScore(product, query) }))
    .filter(
      (entry): entry is { product: FiscalProductType; score: number } =>
        entry.score !== null,
    )
    .sort(
      (a, b) =>
        a.score - b.score ||
        a.product.description.localeCompare(b.product.description),
    )
    .slice(0, limit)
    .map((entry) => entry.product);
}

function displayLabel(product: FiscalProductType): string {
  return `${product.code} — ${product.description}`;
}

interface ProductAutocompleteFieldProps {
  products: FiscalProductType[];
  value: string;
  onSelect: (code: string) => void;
  disabled?: boolean;
  className?: string;
}

/**
 * Campo de texto com autocomplete que aceita código ou nome do produto (catálogo fiscal),
 * tolerante a erro de digitação — em vez de um <select> que exige escolher exatamente da lista.
 */
export default function ProductAutocompleteField({
  products,
  value,
  onSelect,
  disabled,
  className,
}: ProductAutocompleteFieldProps) {
  const selected = products.find((product) => product.code === value);
  const [query, setQuery] = useState(selected ? displayLabel(selected) : "");
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);

  // Depende também de `products`, não só de `value`: quando o pai já inicializa a linha com um
  // código selecionado (ex.: sugestão de matching) mas o catálogo ainda está carregando, o
  // catálogo chega DEPOIS do primeiro render sem que `value` mude — sem essa dependência o texto
  // exibido nunca resincroniza e o campo fica com o código certo por baixo mas aparência vazia.
  // Seguro: só reescreve `query` quando acha um produto pro `value` atual, então não atropela
  // texto que o usuário está digitando (nesse caso `product` é undefined e o guard abaixo pula).
  useEffect(() => {
    const product = products.find((p) => p.code === value);
    if (product) setQuery(displayLabel(product));
  }, [value, products]);

  const suggestions = open ? getSuggestions(products, query) : [];

  const tryAutoResolve = (text: string) => {
    const normQuery = normalize(text);
    const exact = products.find(
      (product) =>
        normalize(product.code) === normQuery ||
        normalize(product.description) === normQuery,
    );
    if (exact) {
      onSelect(exact.code);
    } else if (value) {
      onSelect("");
    }
  };

  const handleChange = (text: string) => {
    setQuery(text);
    setOpen(true);
    setHighlighted(0);
    tryAutoResolve(text);
  };

  const selectProduct = (product: FiscalProductType) => {
    onSelect(product.code);
    setQuery(displayLabel(product));
    setOpen(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!open || suggestions.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlighted((h) => Math.min(h + 1, suggestions.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const candidate = suggestions[highlighted] ?? suggestions[0];
      if (candidate) selectProduct(candidate);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  };

  return (
    <div className="relative w-full">
      <input
        type="text"
        value={query}
        disabled={disabled}
        onChange={(e) => handleChange(e.target.value)}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={handleKeyDown}
        placeholder="Digite o código ou o nome do produto"
        className={
          className ??
          `w-full p-2 border rounded-lg focus:outline-none focus:ring-1 focus:ring-green-500 ${
            value ? "border-gray-300" : "border-red-300"
          }`
        }
      />
      {open && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 w-full max-h-56 overflow-y-auto bg-white border border-gray-200 rounded-lg shadow-lg">
          {suggestions.map((product, index) => (
            <li key={product.code}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => selectProduct(product)}
                className={`w-full text-left px-3 py-2 text-sm ${
                  index === highlighted ? "bg-green-50" : "hover:bg-gray-50"
                }`}
              >
                <span className="font-medium">{product.code}</span> —{" "}
                {product.description}
              </button>
            </li>
          ))}
        </ul>
      )}
      {open && query.trim() !== "" && suggestions.length === 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg px-3 py-2 text-sm text-gray-500">
          Nenhum produto encontrado
        </div>
      )}
    </div>
  );
}
