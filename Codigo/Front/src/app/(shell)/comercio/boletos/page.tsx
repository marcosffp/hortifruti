"use client";

import {
  AlertTriangle,
  FileSearch,
  FileText,
  ListChecks,
  Receipt,
} from "lucide-react";
import { useState } from "react";
import ManualCancelModal from "@/components/modals/ManualCancelModal";
import BoletosAbertosTab from "./BoletosAbertosTab";
import ConsultarBoletoTab from "./ConsultarBoletoTab";
import NfSemBoletoTab from "./NfSemBoletoTab";

type Tab = "abertos" | "porCliente" | "nfSemBoleto";

export default function BoletosPage() {
  const [tab, setTab] = useState<Tab>("abertos");
  const [showManualCancelModal, setShowManualCancelModal] = useState(false);

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <Receipt className="w-7 h-7 text-primary" />
            Cobranças
          </h1>
          <p className="text-gray-600">
            Acompanhe boletos, notas fiscais pendentes e cancelamentos de
            cobrança dos clientes.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowManualCancelModal(true)}
          className="flex items-center gap-1.5 px-3 py-2 bg-white text-red-700 border border-gray-200 rounded-lg hover:bg-red-50 hover:border-red-200 transition-colors text-sm font-medium cursor-pointer"
        >
          <AlertTriangle className="w-4 h-4" />
          Cancelamento Manual (NF)
        </button>
      </div>

      <ManualCancelModal
        open={showManualCancelModal}
        onClose={() => setShowManualCancelModal(false)}
      />

      <div className="flex gap-2 flex-nowrap overflow-x-auto w-full">
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "abertos"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("abertos")}
        >
          <ListChecks className="w-5 h-5 mr-2" />
          Boletos em Aberto
        </button>
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "porCliente"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("porCliente")}
        >
          <FileSearch className="w-5 h-5 mr-2" />
          Consultar Boleto
        </button>
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "nfSemBoleto"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("nfSemBoleto")}
        >
          <FileText className="w-5 h-5 mr-2" />
          NF sem Boleto
        </button>
      </div>

      <div className="bg-white rounded-b-lg rounded-tr-lg shadow-sm p-6 flex-1">
        <div className={tab === "abertos" ? undefined : "hidden"}>
          <BoletosAbertosTab />
        </div>
        <div className={tab === "porCliente" ? undefined : "hidden"}>
          <ConsultarBoletoTab />
        </div>
        <div className={tab === "nfSemBoleto" ? undefined : "hidden"}>
          <NfSemBoletoTab />
        </div>
      </div>
    </main>
  );
}
