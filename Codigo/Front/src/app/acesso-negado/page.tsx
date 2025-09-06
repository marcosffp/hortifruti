'use client';

import { useRouter } from 'next/navigation';

export default function AccessDeniedPage() {
  const router = useRouter();
  
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-red-600">403</h1>
        <h2 className="text-3xl font-semibold text-gray-800 mt-4">Acesso Negado</h2>
        <p className="text-gray-600 mt-2 max-w-md">
          Você não tem permissão para acessar esta página. Entre em contato com o administrador do sistema se você acredita que isto é um erro.
        </p>
        
        <div className="mt-8">
          <button
            onClick={() => router.push('/')}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors mr-4"
          >
            Voltar para o início
          </button>
          <button
            onClick={() => router.back()}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
          >
            Voltar à página anterior
          </button>
        </div>
      </div>
    </div>
  );
}
