package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoLinha;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoTransacao;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoParsingUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Monta as linhas de exibição do extrato (usadas pelo PDF e pelo Excel), calculando o saldo
 * acumulado por dia a partir de {@code saldoAnterior} — a API do Sicoob não retorna esse saldo
 * diário pronto, só o saldo atual/anterior do período inteiro e a lista de transações.
 */
@Slf4j
@Service
public class SicoobExtratoLayoutService {

  private record Parsed(
      SicoobExtratoTransacao raw, LocalDateTime dataHora, LocalDate data, BigDecimal valor) {}

  public List<SicoobExtratoLinha> montarLinhas(SicoobExtratoResponse extrato) {
    List<SicoobExtratoTransacao> transacoes =
        extrato.transacoes() != null ? extrato.transacoes() : List.of();

    List<Parsed> parsed =
        transacoes.stream()
            .map(
                t ->
                    new Parsed(
                        t,
                        SicoobExtratoParsingUtil.parseDataHora(t.data()),
                        SicoobExtratoParsingUtil.parseData(t.data()),
                        SicoobExtratoParsingUtil.parseValorComSinal(t.tipo(), t.valor())))
            .sorted(Comparator.comparing(Parsed::dataHora))
            .toList();

    Map<LocalDate, List<Parsed>> porDia = new LinkedHashMap<>();
    for (Parsed p : parsed) {
      porDia.computeIfAbsent(p.data(), d -> new ArrayList<>()).add(p);
    }

    BigDecimal saldoAnterior = SicoobExtratoParsingUtil.parseSaldo(extrato.saldoAnterior());
    BigDecimal saldoCorrente = saldoAnterior;
    Map<LocalDate, BigDecimal> saldoPorDia = new LinkedHashMap<>();
    for (Map.Entry<LocalDate, List<Parsed>> entry : porDia.entrySet()) {
      BigDecimal somaDia =
          entry.getValue().stream().map(Parsed::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
      saldoCorrente = saldoCorrente.add(somaDia);
      saldoPorDia.put(entry.getKey(), saldoCorrente);
    }

    BigDecimal saldoAtualEsperado = SicoobExtratoParsingUtil.parseSaldo(extrato.saldoAtual());
    if (!parsed.isEmpty() && saldoCorrente.compareTo(saldoAtualEsperado) != 0) {
      log.warn(
          "[Sicoob][extrato] saldo calculado ({}) diverge do saldoAtual retornado pela API ({})"
              + " — conferir transações do período.",
          saldoCorrente,
          saldoAtualEsperado);
    }

    List<LocalDate> diasDesc = new ArrayList<>(porDia.keySet());
    Collections.reverse(diasDesc);

    List<SicoobExtratoLinha> linhas = new ArrayList<>();
    for (LocalDate dia : diasDesc) {
      List<Parsed> doDia = new ArrayList<>(porDia.get(dia));
      doDia.sort(Comparator.comparing(Parsed::dataHora).reversed());

      for (Parsed p : doDia) {
        SicoobExtratoTransacao t = p.raw();
        String historicoPrincipal = t.descricao() != null ? t.descricao().trim() : "";
        linhas.add(
            new SicoobExtratoLinha(dia, t.numeroDocumento(), historicoPrincipal, p.valor(), false));

        if (t.descInfComplementar() != null && !t.descInfComplementar().isBlank()) {
          for (String segmento : t.descInfComplementar().split("\\|@")) {
            String trimmed = segmento.trim();
            if (!trimmed.isEmpty()) {
              linhas.add(new SicoobExtratoLinha(null, null, trimmed, null, false));
            }
          }
        }
      }

      linhas.add(new SicoobExtratoLinha(dia, null, "SALDO DO DIA", saldoPorDia.get(dia), true));
    }

    linhas.add(new SicoobExtratoLinha(null, null, "SALDO ANTERIOR", saldoAnterior, true));

    return linhas;
  }
}
