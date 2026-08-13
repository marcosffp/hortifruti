const CLIENTS_REQUIRING_ADDITIONAL_INVOICE_DATA = ["LLINEA"];

export function clientRequiresAdditionalInvoiceData(
  clientName: string | undefined,
): boolean {
  const firstName = clientName?.split(/\s+/)[0]?.toUpperCase()?.trim() || "";
  return CLIENTS_REQUIRING_ADDITIONAL_INVOICE_DATA.some((name) =>
    firstName.includes(name),
  );
}
