"use client";

import { useState } from "react";
import { statementApiService } from "@/services/statementApiService";
import { getErrorMessage } from "@/types/errorType";

export type BankGenerateStatus = "success" | "alreadyProcessed" | "error";

export interface SicoobGenerateResult {
  status: BankGenerateStatus;
  message: string;
  mes: number;
  ano: number;
  diaInicial: number;
  diaFinal: number;
}

export interface BBGenerateResult {
  status: BankGenerateStatus;
  message: string;
  dataInicio: string;
  dataFim: string;
}

function summaryMessage(
  alreadyProcessed: boolean,
  periodStart: string,
  periodEnd: string,
  totalSaved: number,
  totalDuplicatedSkipped: number,
) {
  return alreadyProcessed
    ? `Período já processado (${periodStart} a ${periodEnd}).`
    : `${totalSaved} lançamento(s) novo(s) salvo(s) (${totalDuplicatedSkipped} já existiam).`;
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export function useStatementImport() {
  const [extratos, setExtratos] = useState<{
    isGenerating: boolean;
    sicoobResult: SicoobGenerateResult | null;
    bbResult: BBGenerateResult | null;
  }>({ isGenerating: false, sicoobResult: null, bbResult: null });

  const generateExtratos = async (startDate: string, endDate: string) => {
    setExtratos({ isGenerating: true, sicoobResult: null, bbResult: null });

    const [startYearStr, startMonthStr] = startDate.split("-");
    const ano = Number(startYearStr);
    const mes = Number(startMonthStr);
    const diaInicial = Number(startDate.split("-")[2]);
    const diaFinal = Number(endDate.split("-")[2]);

    const [sicoobSettled, bbSettled] = await Promise.allSettled([
      statementApiService.importSicoob(mes, ano, diaInicial, diaFinal),
      statementApiService.importBB(startDate, endDate),
    ]);

    const sicoobResult: SicoobGenerateResult =
      sicoobSettled.status === "fulfilled"
        ? {
            status: sicoobSettled.value.alreadyProcessed
              ? "alreadyProcessed"
              : "success",
            message: summaryMessage(
              sicoobSettled.value.alreadyProcessed,
              sicoobSettled.value.periodStart,
              sicoobSettled.value.periodEnd,
              sicoobSettled.value.totalSaved,
              sicoobSettled.value.totalDuplicatedSkipped,
            ),
            mes,
            ano,
            diaInicial,
            diaFinal,
          }
        : {
            status: "error",
            message: getErrorMessage(sicoobSettled.reason),
            mes,
            ano,
            diaInicial,
            diaFinal,
          };

    const bbResult: BBGenerateResult =
      bbSettled.status === "fulfilled"
        ? {
            status: bbSettled.value.alreadyProcessed
              ? "alreadyProcessed"
              : "success",
            message: summaryMessage(
              bbSettled.value.alreadyProcessed,
              bbSettled.value.periodStart,
              bbSettled.value.periodEnd,
              bbSettled.value.totalSaved,
              bbSettled.value.totalDuplicatedSkipped,
            ),
            dataInicio: startDate,
            dataFim: endDate,
          }
        : {
            status: "error",
            message: getErrorMessage(bbSettled.reason),
            dataInicio: startDate,
            dataFim: endDate,
          };

    setExtratos({ isGenerating: false, sicoobResult, bbResult });

    const anySucceeded =
      sicoobResult.status === "success" || bbResult.status === "success";

    return { sicoobResult, bbResult, anySucceeded };
  };

  const downloadSicoobPdf = async () => {
    if (!extratos.sicoobResult) return;
    const blob = await statementApiService.downloadSicoobPdf(
      extratos.sicoobResult.mes,
      extratos.sicoobResult.ano,
      extratos.sicoobResult.diaInicial,
      extratos.sicoobResult.diaFinal,
    );
    triggerBlobDownload(
      blob,
      `extrato-sicoob_${extratos.sicoobResult.ano}${String(extratos.sicoobResult.mes).padStart(2, "0")}.pdf`,
    );
  };

  const downloadSicoobExcel = async () => {
    if (!extratos.sicoobResult) return;
    const blob = await statementApiService.downloadSicoobExcel(
      extratos.sicoobResult.mes,
      extratos.sicoobResult.ano,
      extratos.sicoobResult.diaInicial,
      extratos.sicoobResult.diaFinal,
    );
    triggerBlobDownload(
      blob,
      `extrato-sicoob_${extratos.sicoobResult.ano}${String(extratos.sicoobResult.mes).padStart(2, "0")}.xlsx`,
    );
  };

  const downloadBBPdf = async () => {
    if (!extratos.bbResult) return;
    const blob = await statementApiService.downloadBBPdf(
      extratos.bbResult.dataInicio,
      extratos.bbResult.dataFim,
    );
    triggerBlobDownload(
      blob,
      `extrato-bb_${extratos.bbResult.dataInicio}_a_${extratos.bbResult.dataFim}.pdf`,
    );
  };

  const downloadBBExcel = async () => {
    if (!extratos.bbResult) return;
    const blob = await statementApiService.downloadBBExcel(
      extratos.bbResult.dataInicio,
      extratos.bbResult.dataFim,
    );
    triggerBlobDownload(
      blob,
      `extrato-bb_${extratos.bbResult.dataInicio}_a_${extratos.bbResult.dataFim}.xlsx`,
    );
  };

  return {
    extratos,
    generateExtratos,
    downloadSicoobPdf,
    downloadSicoobExcel,
    downloadBBPdf,
    downloadBBExcel,
  };
}
