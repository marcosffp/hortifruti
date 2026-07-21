package com.hortifruti.sl.hortifruti.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/** Formatação de valores/datas no padrão do extrato do Sicoob, compartilhada entre PDF e Excel. */
public final class SicoobExtratoFormatUtil {

  public static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM");
  public static final DateTimeFormatter DATA_LONGA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private SicoobExtratoFormatUtil() {}

  /** Ex.: "R$ 1.234,56 C" / "R$ 1.234,56 D". */
  public static String formatValorComSinal(BigDecimal valor) {
    String sufixo = valor.signum() < 0 ? "D" : "C";
    return "R$ " + formatValorAbsoluto(valor) + " " + sufixo;
  }

  /** Ex.: "1.234,56" (sem "R$" nem sinal). */
  public static String formatValorAbsoluto(BigDecimal valor) {
    BigDecimal abs = valor.abs().setScale(2, RoundingMode.HALF_UP);
    String semLocale = abs.toPlainString(); // "1234.56"
    String[] partes = semLocale.split("\\.");
    String inteiro = partes[0];
    String decimais = partes.length > 1 ? partes[1] : "00";

    StringBuilder comMilhar = new StringBuilder();
    int len = inteiro.length();
    for (int i = 0; i < len; i++) {
      if (i > 0 && (len - i) % 3 == 0) {
        comMilhar.append('.');
      }
      comMilhar.append(inteiro.charAt(i));
    }

    return comMilhar + "," + decimais;
  }

  public static String capitalizarMes(int mes) {
    String[] meses = {
      "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
      "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };
    return meses[Math.max(0, Math.min(11, mes - 1))];
  }
}
