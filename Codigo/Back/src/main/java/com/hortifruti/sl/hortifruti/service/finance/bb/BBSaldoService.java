package com.hortifruti.sl.hortifruti.service.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.config.bb.BBExtratoClient;
import com.hortifruti.sl.hortifruti.dto.finance.BankBalanceResponse;
import com.hortifruti.sl.hortifruti.exception.BBApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Consulta o saldo disponivel da conta corrente PJ via API Extratos do BB.
 *
 * <p>Nao aceita agencia/conta/data do chamador: sempre consulta a conta configurada no servidor,
 * para hoje. Isso impede que este endpoint vire um caminho para consultar outras contas ou periodos
 * arbitrarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BBSaldoService {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");
  private static final Set<String> HISTORICOS_SALDO = Set.of("0", "999");
  private static final String HISTORICO_FINAL = "999";
  private static final Set<String> TIPOS_DETALHADOS =
      Set.of("SA", "RA", "LE", "AP", "SD", "LD", "LC", "LU");
  private static final int MAX_PAGINAS = 20;

  private final BBExtratoClient bbExtratoClient;

  @Value("${bb.saldo.cache.ttl.seconds:120}")
  private long cacheTtlSeconds;

  private volatile BankBalanceResponse cached;
  private volatile long cachedAt;

  public synchronized BankBalanceResponse getSaldoDisponivel() {
    long now = System.currentTimeMillis();
    if (cached != null && now - cachedAt < cacheTtlSeconds * 1000) {
      return cached;
    }

    BankBalanceResponse response = consultarSaldo();
    this.cached = response;
    this.cachedAt = now;
    return response;
  }

  private BankBalanceResponse consultarSaldo() {
    String hoje = LocalDate.now(ZONE).format(DATE_FORMAT).replaceFirst("^0+", "");

    Map<String, BigDecimal> saldos = new HashMap<>();
    int pagina = 1;
    while (pagina <= MAX_PAGINAS) {
      JsonNode body = bbExtratoClient.getExtratoPage(pagina, hoje, hoje);
      for (JsonNode lanc : body.path("listaLancamento")) {
        String historico = lanc.path("codigoHistorico").asText();
        if (!HISTORICOS_SALDO.contains(historico)) {
          continue;
        }
        String tipo = lanc.path("indicadorTipoLancamento").asText();
        if (TIPOS_DETALHADOS.contains(tipo)) {
          saldos.put(tipo, valorComSinal(lanc));
        } else if (HISTORICO_FINAL.equals(historico) && "1".equals(tipo)) {
          saldos.put("FECHAMENTO", valorComSinal(lanc));
        }
      }

      int proxima = body.path("numeroPaginaProximo").asInt(0);
      if (proxima == 0 || proxima == pagina) {
        break;
      }
      pagina = proxima;
    }

    LocalDateTime agora = LocalDateTime.now(ZONE);
    if (saldos.containsKey("SD")) {
      return new BankBalanceResponse(saldos.get("SD"), true, agora);
    }
    if (saldos.containsKey("FECHAMENTO")) {
      return new BankBalanceResponse(saldos.get("FECHAMENTO"), false, agora);
    }

    log.error("Nenhuma linha de saldo (historico 0/999) retornada pela API do BB hoje.");
    throw new BBApiException("A API do BB não retornou saldo para a data de hoje.");
  }

  private BigDecimal valorComSinal(JsonNode lancamento) {
    BigDecimal valor = lancamento.path("valorLancamento").decimalValue();
    return "D".equals(lancamento.path("indicadorSinalLancamento").asText())
        ? valor.negate()
        : valor;
  }
}
