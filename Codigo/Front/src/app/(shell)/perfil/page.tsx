'use client';

import { useAuth } from "@/hooks/useAuth";
import RoleGuard from "@/components/auth/RoleGuard";
import { useEffect, useState } from "react";

export default function ProfilePage() {
  const { userName, userRoles, isAuthenticated } = useAuth();
  const [loginTime, setLoginTime] = useState<string>('');
  
  useEffect(() => {
    // Definir a hora de login quando o componente é montado
    const now = new Date();
    setLoginTime(now.toLocaleString());
  }, []);
  
  return (
    <div className="flex-1 p-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Meu Perfil</h1>
      
      <div className="bg-white rounded-lg shadow-md p-6 max-w-xl">
        <div className="flex items-center mb-6">
          <div className="w-16 h-16 bg-green-600 rounded-full flex items-center justify-center text-white text-xl font-bold">
            {userName?.charAt(0)?.toUpperCase() || 'U'}
          </div>
          <div className="ml-4">
            <h2 className="text-xl font-semibold text-gray-800">{userName}</h2>
            <div className="flex mt-1">
              {userRoles.map(role => (
                <span 
                  key={role}
                  className="px-2 py-1 text-xs font-medium rounded-full mr-2"
                  style={{ 
                    backgroundColor: role === 'MANAGER' ? '#dcfce7' : '#f3f4f6', 
                    color: role === 'MANAGER' ? '#166534' : '#4b5563'
                  }}
                >
                  {role === 'MANAGER' ? 'Gerente' : role === 'EMPLOYEE' ? 'Funcionário' : role}
                </span>
              ))}
            </div>
          </div>
        </div>
        
        <div className="border-t pt-4">
          <p className="text-gray-600">
            <span className="font-medium">Último login:</span> {loginTime}
          </p>
          
          <RoleGuard roles="MANAGER" ignoreRedirect={true}>
            <div className="mt-6 p-4 bg-green-50 rounded-lg border border-green-200">
              <h3 className="text-green-800 font-medium mb-2">Acesso de Gerente</h3>
              <p className="text-green-700 text-sm">
                Você tem acesso às funcionalidades administrativas do sistema, incluindo:
              </p>
              <ul className="list-disc list-inside mt-2 text-sm text-green-700 space-y-1">
                <li>Gerenciamento de usuários</li>
                <li>Exclusão de registros</li>
                <li>Relatórios financeiros</li>
                <li>Configurações do sistema</li>
              </ul>
            </div>
          </RoleGuard>
        </div>
      </div>
    </div>
  );
}
