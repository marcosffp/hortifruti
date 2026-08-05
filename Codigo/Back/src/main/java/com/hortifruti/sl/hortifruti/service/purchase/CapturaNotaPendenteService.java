package com.hortifruti.sl.hortifruti.service.purchase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.dto.purchase.CapturaIniciadaResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.CapturaPendenteResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaExtracaoResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.CapturaNotaPendenteNaoEncontradaException;
import com.hortifruti.sl.hortifruti.model.purchase.CapturaNotaPendente;
import com.hortifruti.sl.hortifruti.model.purchase.StatusCaptura;
import com.hortifruti.sl.hortifruti.repository.purchase.CapturaNotaPendenteRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import com.hortifruti.sl.hortifruti.service.storage.StorageKeyGenerator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recebe a foto de uma nota (de um dispositivo vinculado ou do próprio usuário autenticado, pra
 * teste sem celular), guarda no R2 e dispara a extração em segundo plano — ver {@link
 * CapturaExtracaoAsyncService}. A resposta ao chamador é imediata (a captura fica {@code
 * RECEBIDA}/{@code EXTRAINDO}); o PC descobre quando a extração termina consultando {@link
 * #listarPendentes} (ver Etapa 5 da spec, notificação em tempo real via SSE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapturaNotaPendenteService {

  private static final List<StatusCaptura> STATUS_PENDENTES =
      List.of(
          StatusCaptura.RECEBIDA,
          StatusCaptura.EXTRAINDO,
          StatusCaptura.PRONTA,
          StatusCaptura.ERRO);

  private final CapturaNotaPendenteRepository capturaNotaPendenteRepository;
  private final NotaUploadService notaUploadService;
  private final R2StorageService r2StorageService;
  private final CapturaExtracaoAsyncService capturaExtracaoAsyncService;
  private final ObjectMapper objectMapper;

  @Value("${r2.environment}")
  private String environment;

  public CapturaIniciadaResponse receberCaptura(
      MultipartFile file, Long usuarioId, Long dispositivoId) {
    NotaUploadService.ValidatedFile validated = notaUploadService.validate(file);

    String extensao = MediaType.IMAGE_PNG_VALUE.equals(validated.contentType()) ? "png" : "jpg";
    String r2Key =
        StorageKeyGenerator.generate(
            "notas-pendentes", environment, UUID.randomUUID().toString(), extensao);
    r2StorageService.upload(validated.bytes(), r2Key, validated.contentType());

    CapturaNotaPendente captura =
        CapturaNotaPendente.builder()
            .usuarioId(usuarioId)
            .dispositivoId(dispositivoId)
            .r2Key(r2Key)
            .contentType(validated.contentType())
            .status(StatusCaptura.RECEBIDA)
            .build();
    capturaNotaPendenteRepository.save(captura);

    log.info(
        "Captura de nota recebida: capturaId={}, usuarioId={}, dispositivoId={}",
        captura.getId(),
        usuarioId,
        dispositivoId);
    capturaExtracaoAsyncService.processar(captura.getId());

    return new CapturaIniciadaResponse(captura.getId());
  }

  public List<CapturaPendenteResponse> listarPendentes(Long usuarioId) {
    return capturaNotaPendenteRepository
        .findByUsuarioIdAndStatusInOrderByCriadaEmDesc(usuarioId, STATUS_PENDENTES)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public void descartar(Long capturaId, Long usuarioId) {
    CapturaNotaPendente captura =
        capturaNotaPendenteRepository
            .findByIdAndUsuarioId(capturaId, usuarioId)
            .orElseThrow(
                () -> new CapturaNotaPendenteNaoEncontradaException("Captura não encontrada."));

    captura.setStatus(StatusCaptura.DESCARTADA);
    capturaNotaPendenteRepository.save(captura);
  }

  /**
   * A tela de revisão compara a extração lado a lado com a foto original — sem isso, o PC não tem
   * como mostrar a imagem de uma captura recebida de outro dispositivo (só quem fotografou viu a
   * foto até baixar isso de volta do R2).
   */
  public ImagemCaptura buscarImagem(Long capturaId, Long usuarioId) {
    CapturaNotaPendente captura =
        capturaNotaPendenteRepository
            .findByIdAndUsuarioId(capturaId, usuarioId)
            .orElseThrow(
                () -> new CapturaNotaPendenteNaoEncontradaException("Captura não encontrada."));

    byte[] bytes = r2StorageService.download(captura.getR2Key());
    return new ImagemCaptura(bytes, captura.getContentType());
  }

  private CapturaPendenteResponse toResponse(CapturaNotaPendente captura) {
    return new CapturaPendenteResponse(
        captura.getId(),
        captura.getStatus(),
        parseExtracao(captura.getExtracaoJson()),
        captura.getMensagemErro(),
        captura.getCriadaEm());
  }

  private NotaExtracaoResponse parseExtracao(String extracaoJson) {
    if (extracaoJson == null) {
      return null;
    }
    try {
      return objectMapper.readValue(extracaoJson, NotaExtracaoResponse.class);
    } catch (JsonProcessingException e) {
      log.warn("Falha ao desserializar extração persistida: {}", e.getMessage());
      return null;
    }
  }

  public record ImagemCaptura(byte[] bytes, String contentType) {}
}
