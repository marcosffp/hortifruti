package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.exception.SicoobExtratoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parsing dos campos de transação retornados pela API de conta corrente do Sicoob (ver
 * documentacao_sicoob_api.md). Fonte única desses formatos — reaproveitado tanto pela importação
 * (persiste {@code Transaction}) quanto pela exportação de PDF/Excel, para não divergir em dois
 * lugares (já tivemos um bug real de formato de valor/data descoberto só ao testar contra a API de
 * verdade).
 */
public final class SicoobExtratoParsingUtil {

  private static final DateTimeFormatter BR_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter API_DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private SicoobExtratoParsingUtil() {}

  /** "tipo" costuma vir como "CREDITO"/"DEBITO"; usa o sinal de "valor" como fallback. */
  public static boolean isCredito(String tipo, String valor) {
    String normalizedTipo = tipo != null ? tipo.trim().toUpperCase() : "";
    if (normalizedTipo.contains("DEB")) {
      return false;
    }
    if (normalizedTipo.contains("CRED")) {
      return true;
    }
    return valor == null || !valor.trim().startsWith("-");
  }

  /**
   * "valor" vem em formato decimal com ponto e sem sinal (ex.: "5546.11", "9.54") — diferente do
   * formato brasileiro do PDF ("5.546,11"). Retorna o valor sempre positivo (o sinal é aplicado
   * separadamente, ver {@link #parseValorComSinal}).
   */
  public static BigDecimal parseValorAbsoluto(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new SicoobExtratoException("Transação do Sicoob sem valor informado.");
    }
    // Remove um eventual "R$" e uma eventual vírgula de milhar (formato "12,345.67"), sem tocar
    // no ponto decimal.
    String cleaned = valor.trim().replace("R$", "").trim().replace(",", "");
    if (cleaned.startsWith("-")) {
      cleaned = cleaned.substring(1);
    }
    return new BigDecimal(cleaned);
  }

  /** Valor assinado: negativo para débito, positivo para crédito. */
  public static BigDecimal parseValorComSinal(String tipo, String valor) {
    BigDecimal absoluto = parseValorAbsoluto(valor);
    return isCredito(tipo, valor) ? absoluto : absoluto.negate();
  }

  /**
   * Campos de saldo (`saldoAtual`, `saldoAnterior`, ...) já vêm como decimal simples e assinado.
   */
  public static BigDecimal parseSaldo(String saldo) {
    if (saldo == null || saldo.isBlank()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(saldo.trim());
  }

  /** "data" vem como data-hora local (ex.: "2026-05-04T00:00"). Retorna só a data. */
  public static LocalDate parseData(String data) {
    return parseDataHora(data).toLocalDate();
  }

  /** "data" vem como data-hora local (ex.: "2026-05-04T00:00") — usado para ordenar entre si. */
  public static LocalDateTime parseDataHora(String data) {
    if (data == null || data.isBlank()) {
      throw new SicoobExtratoException("Transação do Sicoob sem data informada.");
    }
    String trimmed = data.trim();
    if (trimmed.indexOf('T') >= 0) {
      try {
        return LocalDateTime.parse(trimmed, API_DATETIME_FORMATTER);
      } catch (DateTimeParseException ignored) {
        // cai para as tentativas de data pura abaixo
      }
    }
    try {
      return LocalDate.parse(trimmed).atStartOfDay();
    } catch (DateTimeParseException isoFailure) {
      try {
        return LocalDate.parse(trimmed, BR_DATE_FORMATTER).atStartOfDay();
      } catch (DateTimeParseException brFailure) {
        throw new SicoobExtratoException("Formato de data inválido no extrato do Sicoob: " + data);
      }
    }
  }
}
