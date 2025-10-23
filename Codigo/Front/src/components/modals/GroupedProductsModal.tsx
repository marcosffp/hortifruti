"use client";

import React, { useState, useEffect } from "react";
import { X } from "lucide-react";
import { toast } from "react-toastify";
import { combinedScoreService } from "@/services/combinedScoreService";
import { GroupedProductType } from "@/types/combinedScoreType";

interface GroupedProductsModalProps {
  combinedScoreId: number;
  scoreNumber: string;
  onClose: () => void;
}

export default function GroupedProductsModal({
  combinedScoreId,
  scoreNumber,
  onClose,
}: GroupedProductsModalProps) {
  const [products, setProducts] = useState<GroupedProductType[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await combinedScoreService.fetchGroupedProducts(combinedScoreId);
      setProducts(data);
    } catch (error) {
      toast.error("Erro ao carregar produtos");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [combinedScoreId]);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-gray-300 border-b">
          <h2 className="text-xl font-semibold">
            Produtos do Agrupamento {scoreNumber}
          </h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-16 bg-gray-200 animate-pulse rounded-lg" />
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Nenhum produto encontrado neste agrupamento
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-gray-100 border-gray-300 border-b">
                    <th className="text-left p-3 font-semibold">Código</th>
                    <th className="text-left p-3 font-semibold">Produto</th>
                    <th className="text-right p-3 font-semibold">Quantidade</th>
                    <th className="text-right p-3 font-semibold">Preço Unit.</th>
                    <th className="text-right p-3 font-semibold">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product, index) => (
                    <tr key={`${product.code}-${index}`} className="border-gray-300 border-b hover:bg-gray-50">
                      <td className="p-3">{product.code}</td>
                      <td className="p-3">{product.name}</td>
                      <td className="p-3 text-right">{product.quantity}</td>
                      <td className="p-3 text-right">{formatCurrency(product.price)}</td>
                      <td className="p-3 text-right font-semibold">
                        {formatCurrency(product.totalValue)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="bg-gray-50 font-semibold">
                    <td colSpan={4} className="p-3 text-right">
                      Total Geral:
                    </td>
                    <td className="p-3 text-right">
                      {formatCurrency(
                        products.reduce((sum, p) => sum + p.totalValue, 0)
                      )}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
