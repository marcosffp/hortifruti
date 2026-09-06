package com.hortifruti.sl.hortifruti.service.invoice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Serializa, por ref, o trecho check-then-upload-then-save de {@link FiscalNoteXmlStorageStore}.
 * Sem isso, o job assíncrono {@link FiscalNoteIssuancePoller} e a rede de segurança do download
 * (disparada se o usuário abrir o XML/DANFE antes do job terminar) podem checar se o XML já foi
 * salvo ao mesmo tempo, os dois verem "não existe" e os dois fazerem upload para o R2 — gerando um
 * arquivo duplicado órfão no bucket quando o segundo INSERT falha por violação da constraint
 * UNIQUE(ref). A aplicação roda como instância única (ver docker-compose.yml), então este lock em
 * memória fecha a corrida na prática; a captura de {@code DataIntegrityViolationException} em
 * {@link FiscalNoteXmlStorageStore} é uma segunda camada de defesa.
 */
@Component
class FiscalNoteRefLock {

  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  void withLock(String ref, Runnable action) {
    ReentrantLock lock = locks.computeIfAbsent(ref, k -> new ReentrantLock());
    lock.lock();
    try {
      action.run();
    } finally {
      lock.unlock();
    }
  }
}
