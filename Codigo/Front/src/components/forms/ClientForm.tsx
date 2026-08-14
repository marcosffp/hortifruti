"use client";

import { ArrowLeft, Save } from "lucide-react";
import Link from "next/link";
import Button from "@/components/ui/Button";
import AdditionalInfoSection from "./client-form/AdditionalInfoSection";
import AddressSection from "./client-form/AddressSection";
import FiscalDataSection from "./client-form/FiscalDataSection";
import PersonalDataSection from "./client-form/PersonalDataSection";
import type { ClientFormData } from "./client-form/types";
import { useClientForm } from "./client-form/useClientForm";

export type { ClientFormData } from "./client-form/types";

interface ClientFormProps {
  initialData?: Partial<ClientFormData>;
  onSubmit: (data: ClientFormData) => Promise<void>;
  isSubmitting: boolean;
  title: string;
  subtitle: string;
  submitButtonText: string;
}

export default function ClientForm({
  initialData,
  onSubmit,
  isSubmitting,
  title,
  subtitle,
  submitButtonText,
}: ClientFormProps) {
  const {
    formData,
    formErrors,
    isCNPJ,
    handleChange,
    handleFieldBlur,
    handleCepBlur,
    buscarEnderecoPorCEP,
    handleNumeroSemNumeroToggle,
    handleSubmit,
  } = useClientForm({ initialData, onSubmit });

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto">
      <div className="mb-6 flex items-center">
        <Link href="/comercio/clientes" className="mr-4">
          <Button
            variant="outline"
            icon={<ArrowLeft size={18} />}
            className="px-2 py-1"
          />
        </Link>
        <div>
          <h1 className="text-2xl font-bold">{title}</h1>
          <p className="text-gray-600">{subtitle}</p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          <PersonalDataSection
            formData={formData}
            formErrors={formErrors}
            onChange={handleChange}
            onFieldBlur={handleFieldBlur}
          />

          {isCNPJ && (
            <FiscalDataSection
              formData={formData}
              formErrors={formErrors}
              onChange={handleChange}
              onFieldBlur={handleFieldBlur}
            />
          )}

          <AddressSection
            formData={formData}
            formErrors={formErrors}
            onChange={handleChange}
            onFieldBlur={handleFieldBlur}
            onCepBlur={handleCepBlur}
            onBuscarCep={() => buscarEnderecoPorCEP(formData.cep)}
            onNumeroSemNumeroToggle={handleNumeroSemNumeroToggle}
          />

          <AdditionalInfoSection formData={formData} onChange={handleChange} />

          <div className="flex justify-end space-x-3 pt-6 border-t">
            <Link href="/comercio/clientes">
              <Button variant="outline" disabled={isSubmitting}>
                Cancelar
              </Button>
            </Link>
            <Button
              variant="primary"
              type="submit"
              icon={<Save size={18} />}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Salvando..." : submitButtonText}
            </Button>
          </div>
        </form>
      </div>
    </main>
  );
}
