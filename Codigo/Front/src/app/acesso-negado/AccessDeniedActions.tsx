"use client";

import { useRouter } from "next/navigation";

export default function AccessDeniedActions() {
  const router = useRouter();

  return (
    <div className="mt-8">
      <button
        type="button"
        onClick={() => router.push("/")}
        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors mr-4"
      >
        Voltar para o início
      </button>
      <button
        type="button"
        onClick={() => router.back()}
        className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
      >
        Voltar à página anterior
      </button>
    </div>
  );
}
