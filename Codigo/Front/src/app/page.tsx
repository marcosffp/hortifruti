import Header from "../components/layout/Header";
import Sidebar from "../components/layout/Sidebar";
import CashFlow from "../components/modules/CashFlow";
import UploadExtract from "../components/modules/UploadExtract";
import Alerts from "../components/ui/Alerts";
import Card from "../components/ui/Card";

export default function Home() {
  return (
    <div className="flex flex-col h-screen">
      <Header />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6 bg-gray-50 overflow-auto">
          <h1 className="text-2xl font-bold mb-6">Dashboard</h1>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <Card title="Bem-vindo ao Hortifruti SL">
              <p className="text-gray-600">
                Sistema de gestão para hortifruti com módulos integrados para
                controle financeiro, gestão de estoque, vendas e muito mais.
              </p>
            </Card>
            <UploadExtract />
          </div>

          {/* Alerts Component */}
          <div className="mb-6">
            <Alerts />
          </div>

          {/* Cash Flow Component */}
          <CashFlow />
        </main>
      </div>
    </div>
  );
}
