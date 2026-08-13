import AccessDeniedActions from "./AccessDeniedActions";

export default function AccessDeniedPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-red-600">403</h1>
        <h2 className="text-3xl font-semibold text-gray-800 mt-4">
          Acesso Negado
        </h2>
        <p className="text-gray-600 mt-2 max-w-md">
          Você não tem permissão para acessar esta página. Entre em contato com
          o administrador do sistema se você acredita que isto é um erro.
        </p>

        <AccessDeniedActions />
      </div>
    </div>
  );
}
