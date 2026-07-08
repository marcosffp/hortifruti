package com.hortifruti.sl.hortifruti.service.purchase;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feriados nacionais brasileiros (fixos e móveis, calculados a partir da Páscoa), usados para
 * impedir que datas de vencimento caiam em dias não úteis.
 */
final class BrazilianHolidays {

  private static final Map<Integer, Set<LocalDate>> HOLIDAYS_BY_YEAR = new ConcurrentHashMap<>();

  private BrazilianHolidays() {}

  static boolean isHoliday(LocalDate date) {
    return HOLIDAYS_BY_YEAR
        .computeIfAbsent(date.getYear(), BrazilianHolidays::buildHolidays)
        .contains(date);
  }

  private static Set<LocalDate> buildHolidays(int year) {
    Set<LocalDate> holidays = new HashSet<>();

    // Feriados fixos
    holidays.add(LocalDate.of(year, Month.JANUARY, 1)); // Confraternização Universal
    holidays.add(LocalDate.of(year, Month.APRIL, 21)); // Tiradentes
    holidays.add(LocalDate.of(year, Month.MAY, 1)); // Dia do Trabalho
    holidays.add(LocalDate.of(year, Month.SEPTEMBER, 7)); // Independência do Brasil
    holidays.add(LocalDate.of(year, Month.OCTOBER, 12)); // Nossa Senhora Aparecida
    holidays.add(LocalDate.of(year, Month.NOVEMBER, 2)); // Finados
    holidays.add(LocalDate.of(year, Month.NOVEMBER, 15)); // Proclamação da República
    holidays.add(LocalDate.of(year, Month.DECEMBER, 25)); // Natal

    if (year >= 2024) {
      // Feriado nacional a partir da Lei 14.759/2023
      holidays.add(
          LocalDate.of(year, Month.NOVEMBER, 20)); // Dia Nacional de Zumbi e da Consciência Negra
    }

    // Feriados móveis, calculados a partir do Domingo de Páscoa
    LocalDate easter = calculateEaster(year);
    holidays.add(easter.minusDays(48)); // Carnaval (segunda-feira)
    holidays.add(easter.minusDays(47)); // Carnaval (terça-feira)
    holidays.add(easter.minusDays(2)); // Sexta-feira Santa
    holidays.add(easter.plusDays(60)); // Corpus Christi

    return holidays;
  }

  /**
   * Calcula a data do Domingo de Páscoa (algoritmo de Gauss/Meeus para o calendário gregoriano).
   */
  private static LocalDate calculateEaster(int year) {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int month = (h + l - 7 * m + 114) / 31;
    int day = ((h + l - 7 * m + 114) % 31) + 1;

    return LocalDate.of(year, month, day);
  }
}
