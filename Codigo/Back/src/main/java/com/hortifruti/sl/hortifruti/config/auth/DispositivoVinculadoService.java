package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.dto.device.ConfirmarPareamentoResponse;
import com.hortifruti.sl.hortifruti.dto.device.DispositivoResponse;
import com.hortifruti.sl.hortifruti.dto.device.IniciarPareamentoResponse;
import com.hortifruti.sl.hortifruti.exception.auth.DispositivoException;
import com.hortifruti.sl.hortifruti.exception.auth.DispositivoNaoEncontradoException;
import com.hortifruti.sl.hortifruti.exception.auth.TokenException;
import com.hortifruti.sl.hortifruti.model.DispositivoVinculado;
import com.hortifruti.sl.hortifruti.repository.DispositivoVinculadoRepository;
import com.hortifruti.sl.hortifruti.service.realtime.RealtimeNotificationRegistry;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pareamento de dispositivo (celular) a um usuário já autenticado no PC: o código de 6 dígitos
 * gerado em {@link #gerarCodigoPareamento} vive só em memória, com TTL curto e uso único — nunca
 * chega a ser persistido, então não há nada pra "vazar" além da janela de poucos minutos. O token
 * opaco emitido em {@link #confirmarPareamento} é quem passa a autenticar o celular dali em diante,
 * com escopo restrito aos endpoints de captura de nota (ver Etapa 3 da spec, DeviceTokenAuthFilter)
 * — nunca equivalente ao login completo do usuário.
 */
@Service
@RequiredArgsConstructor
public class DispositivoVinculadoService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final DispositivoVinculadoRepository dispositivoVinculadoRepository;
  private final RealtimeNotificationRegistry realtimeNotificationRegistry;
  private final TokenHasher tokenHasher;

  @Value("${dispositivo.pareamento.ttl-minutos:5}")
  private long ttlMinutos = 5;

  /**
   * Defesa em profundidade contra celular perdido/roubado cujo dono nunca chegou a revogar: um
   * dispositivo sem uso por tempo demais é tratado como revogado automaticamente, mesmo que ninguém
   * tenha clicado em "Revogar" no PC — mesma lógica do refresh token (30 dias), só que mais longa
   * porque um celular de captura tende a ficar mais tempo parado que uma sessão de navegador.
   */
  @Value("${dispositivo.inatividade-max-dias:90}")
  private long inatividadeMaxDias = 90;

  private final Map<String, CodigoPareamento> codigosAtivos = new ConcurrentHashMap<>();
  private final Map<Long, String> codigoPorUsuario = new ConcurrentHashMap<>();

  public IniciarPareamentoResponse gerarCodigoPareamento(Long usuarioId) {
    cleanup();

    // Só um código válido por usuário por vez — gerar de novo invalida o anterior, evitando
    // confusão sobre qual QR/código é o vigente.
    String codigoAnterior = codigoPorUsuario.get(usuarioId);
    if (codigoAnterior != null) {
      codigosAtivos.remove(codigoAnterior);
    }

    String codigo = gerarCodigoNumerico();
    LocalDateTime expiraEm = LocalDateTime.now().plusMinutes(ttlMinutos);
    codigosAtivos.put(codigo, new CodigoPareamento(usuarioId, expiraEm));
    codigoPorUsuario.put(usuarioId, codigo);

    return new IniciarPareamentoResponse(codigo, expiraEm);
  }

  @Transactional
  public ConfirmarPareamentoResponse confirmarPareamento(String codigo, String nomeDispositivo) {
    cleanup();

    CodigoPareamento pareamento = codigosAtivos.remove(codigo);
    if (pareamento == null || pareamento.expiraEm().isBefore(LocalDateTime.now())) {
      throw new DispositivoException(
          "Código de pareamento inválido ou expirado. Gere um novo código no PC.");
    }
    codigoPorUsuario.remove(pareamento.usuarioId(), codigo);

    String rawToken = tokenHasher.generateOpaqueToken();

    DispositivoVinculado dispositivo =
        DispositivoVinculado.builder()
            .tokenHash(tokenHasher.hash(rawToken))
            .userId(pareamento.usuarioId())
            .nomeDispositivo(nomeDispositivoOuPadrao(nomeDispositivo))
            .build();
    dispositivoVinculadoRepository.save(dispositivo);

    realtimeNotificationRegistry.notificarDispositivoPareado(pareamento.usuarioId());

    return new ConfirmarPareamentoResponse(rawToken, dispositivo.getId());
  }

  @Transactional
  public DispositivoAutenticado validarToken(String tokenClaro) {
    DispositivoVinculado dispositivo =
        dispositivoVinculadoRepository
            .findByTokenHashAndRevogadoEmIsNull(tokenHasher.hash(tokenClaro))
            .orElseThrow(
                () ->
                    new TokenException(
                        "Este dispositivo não está mais vinculado. Peça um novo pareamento no"
                            + " PC."));

    if (inativoDemais(dispositivo)) {
      dispositivo.setRevogadoEm(LocalDateTime.now());
      dispositivoVinculadoRepository.save(dispositivo);
      throw new TokenException(
          "Este dispositivo ficou muito tempo sem uso e precisa ser pareado novamente.");
    }

    dispositivo.setUltimoUsoEm(LocalDateTime.now());
    dispositivoVinculadoRepository.save(dispositivo);

    return new DispositivoAutenticado(dispositivo.getUserId(), dispositivo.getId());
  }

  public List<DispositivoResponse> listarDispositivos(Long usuarioId) {
    return dispositivoVinculadoRepository
        .findByUserIdAndRevogadoEmIsNullOrderByPareadoEmDesc(usuarioId)
        .stream()
        .map(
            d ->
                new DispositivoResponse(
                    d.getId(), d.getNomeDispositivo(), d.getPareadoEm(), d.getUltimoUsoEm()))
        .toList();
  }

  @Transactional
  public void revogar(Long dispositivoId, Long usuarioId) {
    DispositivoVinculado dispositivo =
        dispositivoVinculadoRepository
            .findByIdAndUserId(dispositivoId, usuarioId)
            .orElseThrow(
                () -> new DispositivoNaoEncontradoException("Dispositivo não encontrado."));

    dispositivo.setRevogadoEm(LocalDateTime.now());
    dispositivoVinculadoRepository.save(dispositivo);
  }

  /**
   * Usa o último uso como referência e, se o dispositivo foi pareado mas nunca chegou a ser usado,
   * cai pra data do pareamento — sem esse fallback, um dispositivo pareado e nunca usado nunca
   * expiraria sozinho (ultimoUsoEm continuaria null pra sempre).
   */
  private boolean inativoDemais(DispositivoVinculado dispositivo) {
    LocalDateTime referencia =
        dispositivo.getUltimoUsoEm() != null
            ? dispositivo.getUltimoUsoEm()
            : dispositivo.getPareadoEm();
    return referencia.isBefore(LocalDateTime.now().minusDays(inatividadeMaxDias));
  }

  private void cleanup() {
    LocalDateTime now = LocalDateTime.now();
    codigosAtivos.entrySet().removeIf(entry -> entry.getValue().expiraEm().isBefore(now));
  }

  private String gerarCodigoNumerico() {
    int numero = SECURE_RANDOM.nextInt(1_000_000);
    return String.format("%06d", numero);
  }

  private String nomeDispositivoOuPadrao(String nomeDispositivo) {
    return (nomeDispositivo == null || nomeDispositivo.isBlank())
        ? "Dispositivo sem nome"
        : nomeDispositivo.trim();
  }

  private record CodigoPareamento(Long usuarioId, LocalDateTime expiraEm) {}

  /** Resultado da validação de um token de dispositivo: quem ele autentica e qual dispositivo é. */
  public record DispositivoAutenticado(Long usuarioId, Long dispositivoId) {}
}
