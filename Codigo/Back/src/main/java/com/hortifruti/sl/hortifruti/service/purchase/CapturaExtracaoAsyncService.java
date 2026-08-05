package com.hortifruti.sl.hortifruti.service.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaExtracaoResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.GeminiExtractionException;
import com.hortifruti.sl.hortifruti.model.purchase.CapturaNotaPendente;
import com.hortifruti.sl.hortifruti.model.purchase.StatusCaptura;
import com.hortifruti.sl.hortifruti.repository.purchase.CapturaNotaPendenteRepository;
import com.hortifruti.sl.hortifruti.service.realtime.RealtimeNotificationRegistry;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Processa a extração de uma captura em segundo plano, disparado logo após o upload (ver {@code
 * CapturaNotaPendenteService#receberCaptura}), pra que a resposta HTTP ao dispositivo não espere os
 * ~17-30s reais do Gemini. Cada mudança de status é salva com sua própria chamada de repositório
 * (sem {@code @Transactional} envolvendo a chamada ao Gemini) — de propósito, pra não segurar uma
 * transação de banco aberta pela duração inteira de uma chamada HTTP externa lenta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapturaExtracaoAsyncService {

  private final CapturaNotaPendenteRepository capturaNotaPendenteRepository;
  private final R2StorageService r2StorageService;
  private final GeminiExtractionService geminiExtractionService;
  private final RealtimeNotificationRegistry realtimeNotificationRegistry;
  private final ObjectMapper objectMapper;

  @Async
  public void processar(Long capturaId) {
    CapturaNotaPendente captura = capturaNotaPendenteRepository.findById(capturaId).orElse(null);
    if (captura == null) {
      log.warn("Captura {} não encontrada ao iniciar extração assíncrona.", capturaId);
      return;
    }

    captura.setStatus(StatusCaptura.EXTRAINDO);
    capturaNotaPendenteRepository.save(captura);
    notificar(captura);

    try {
      byte[] bytes = r2StorageService.download(captura.getR2Key());
      NotaExtracaoResponse extracao =
          geminiExtractionService.extrairDeArquivoValidado(
              new NotaUploadService.ValidatedFile(bytes, captura.getContentType()));

      captura.setExtracaoJson(objectMapper.writeValueAsString(extracao));
      captura.setStatus(StatusCaptura.PRONTA);
    } catch (Exception e) {
      log.warn("Falha na extração assíncrona da captura {}: {}", capturaId, e.getMessage());
      captura.setStatus(StatusCaptura.ERRO);
      captura.setMensagemErro(mensagemAmigavel(e));
    }

    capturaNotaPendenteRepository.save(captura);
    notificar(captura);
  }

  /**
   * Melhor esforço: o PC pode não ter nenhuma conexão WebSocket aberta no momento (ex.: fechou o
   * navegador) — nesse caso não há nada a notificar, e quando reconectar/recarregar a página o
   * {@code GET /pendentes} já traz o estado atual normalmente.
   */
  private void notificar(CapturaNotaPendente captura) {
    realtimeNotificationRegistry.notificarCapturaAtualizada(
        captura.getUsuarioId(), captura.getId(), captura.getStatus());
  }

  private String mensagemAmigavel(Exception e) {
    return e instanceof GeminiExtractionException
        ? e.getMessage()
        : "Não foi possível processar esta captura. Tente novamente ou cadastre manualmente.";
  }
}
