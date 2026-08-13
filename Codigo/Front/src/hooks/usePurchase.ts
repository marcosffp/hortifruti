"use client";

import { useCallback, useState } from "react";
import { purchaseService } from "@/services/purchaseService";
import { getErrorMessage } from "@/types/errorType";
import type {
  InvoiceProductUpdate,
  ManualPurchaseItemRequest,
  ManualPurchaseRequest,
} from "@/types/purchaseType";

export function usePurchase() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async <T>(fn: () => Promise<T>): Promise<T> => {
    setIsLoading(true);
    setError(null);
    try {
      return await fn();
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const createManualPurchase = useCallback(
    (payload: ManualPurchaseRequest) =>
      run(() => purchaseService.createManualPurchase(payload)),
    [run],
  );

  const fetchClientProducts = useCallback(
    (clientId: number, startDate: string, endDate: string) =>
      run(() =>
        purchaseService.fetchClientProducts(clientId, startDate, endDate),
      ),
    [run],
  );

  const fetchPurchaseFiles = useCallback(
    (clientId: number, page = 0, size = 10) =>
      run(() => purchaseService.fetchPurchaseFiles(clientId, page, size)),
    [run],
  );

  const deletePurchaseFile = useCallback(
    (fileId: number) => run(() => purchaseService.deletePurchaseFile(fileId)),
    [run],
  );

  const fetchInvoiceProducts = useCallback(
    (purchaseId: number) =>
      run(() => purchaseService.fetchInvoiceProducts(purchaseId)),
    [run],
  );

  const addInvoiceProduct = useCallback(
    (purchaseId: number, item: ManualPurchaseItemRequest) =>
      run(() => purchaseService.addInvoiceProduct(purchaseId, item)),
    [run],
  );

  const updateInvoiceProduct = useCallback(
    (id: number, update: InvoiceProductUpdate) =>
      run(() => purchaseService.updateInvoiceProduct(id, update)),
    [run],
  );

  const deleteInvoiceProduct = useCallback(
    (productId: number) =>
      run(() => purchaseService.deleteInvoiceProduct(productId)),
    [run],
  );

  const fetchPurchaseImage = useCallback(
    (purchaseId: number) =>
      run(() => purchaseService.fetchPurchaseImage(purchaseId)),
    [run],
  );

  return {
    createManualPurchase,
    fetchClientProducts,
    fetchPurchaseFiles,
    deletePurchaseFile,
    fetchInvoiceProducts,
    addInvoiceProduct,
    updateInvoiceProduct,
    deleteInvoiceProduct,
    fetchPurchaseImage,
    isLoading,
    error,
  };
}
