'use client';

import RoleGuard from "@/components/auth/RoleGuard";

export default function AdminPage() {
  return (
    <RoleGuard roles="MANAGER">
      <div className="flex-1 p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Administração do Sistema</h1>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">Configurações Administrativas</h2>
          <p className="text-gray-600 mb-6">
            Esta página só está disponível para usuários com a role MANAGER.
          </p>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="border border-[var(--neutral-300)] rounded-lg p-4 hover:bg-gray-50 cursor-pointer">
              <h3 className="font-medium text-gray-800">Gerenciamento de Usuários</h3>
              <p className="text-gray-600 text-sm mt-1">
                Adicionar, remover e gerenciar usuários do sistema
              </p>
            </div>
            
            <div className="border border-[var(--neutral-300)] rounded-lg p-4 hover:bg-gray-50 cursor-pointer">
              <h3 className="font-medium text-gray-800">Configurações do Sistema</h3>
              <p className="text-gray-600 text-sm mt-1">
                Ajustar parâmetros e preferências globais
              </p>
            </div>
            
            <div className="border border-[var(--neutral-300)] rounded-lg p-4 hover:bg-gray-50 cursor-pointer">
              <h3 className="font-medium text-gray-800">Backup de Dados</h3>
              <p className="text-gray-600 text-sm mt-1">
                Configurar e executar backups do sistema
              </p>
            </div>
            
            <div className="border border-[var(--neutral-300)] rounded-lg p-4 hover:bg-gray-50 cursor-pointer">
              <h3 className="font-medium text-gray-800">Logs do Sistema</h3>
              <p className="text-gray-600 text-sm mt-1">
                Visualizar logs de atividade e erros
              </p>
            </div>
          </div>
        </div>
      </div>
    </RoleGuard>
  );
}
