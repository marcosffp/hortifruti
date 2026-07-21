package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.exception.BBApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parsing dos campos de lançamento retornados pela API Extratos v2 do BB (ver
 * api-extratos-bb-2.md). Só o endpoint de saldo do dia ({@link
 * com.hortifruti.sl.hortifruti.service.finance.BBSaldoService}) foi validado contra uma resposta
 * real até hoje — os demais campos (histórico, documento, complemento) seguem só a documentação. Ao
 * encontrar divergência, não confiar cegamente na doc: conferir a resposta real primeiro (o Sicoob
 * já teve três divergências reais nesse mesmo tipo de integração, ver
 * extrato-via-api-notas-implementacao.md).
 */
public final class BBExtratoParsingUtil {

  private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

  private BBExtratoParsingUtil() {}

  /** "dataLancamento" vem no formato DDMMAAAA (ex.: "19042026"). */
  public static LocalDate parseData(String dataLancamento) {
    if (dataLancamento == null || dataLancamento.isBlank()) {
      throw new BBApiException("Lançamento do BB sem dataLancamento informada.");
    }
    String normalizado = dataLancamento.trim();
    if (normalizado.length() < 8) {
      normalizado = "0".repeat(8 - normalizado.length()) + normalizado;
    }
    try {
      return LocalDate.parse(normalizado, DATA_FORMATTER);
    } catch (DateTimeParseException ex) {
      throw new BBApiException(
          "Formato de dataLancamento inválido no extrato do BB: " + dataLancamento);
    }
  }

  /** "valorLancamento" vem em decimal com ponto, sem sinal (ex.: "1079.00"). */
  public static BigDecimal parseValorAbsoluto(String valorLancamento) {
    if (valorLancamento == null || valorLancamento.isBlank()) {
      throw new BBApiException("Lançamento do BB sem valorLancamento informado.");
    }
    return new BigDecimal(valorLancamento.trim()).abs();
  }

  /** Valor assinado: negativo para débito ("D"), positivo para crédito ("C") ou demais sinais. */
  public static BigDecimal parseValorComSinal(
      String indicadorSinalLancamento, String valorLancamento) {
    BigDecimal absoluto = parseValorAbsoluto(valorLancamento);
    return "D".equals(indicadorSinalLancamento) ? absoluto.negate() : absoluto;
  }
}
