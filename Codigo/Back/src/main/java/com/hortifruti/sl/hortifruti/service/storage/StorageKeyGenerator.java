package com.hortifruti.sl.hortifruti.service.storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class StorageKeyGenerator {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private StorageKeyGenerator() {}

  /** Ex: boletos/prod/2026/07/42_20260718_143210.pdf */
  public static String generate(String prefix, String environment, String businessId, String extension) {
    LocalDateTime now = LocalDateTime.now();
    return "%s/%s/%04d/%02d/%s_%s.%s"
        .formatted(
            prefix,
            environment,
            now.getYear(),
            now.getMonthValue(),
            businessId,
            now.format(TIMESTAMP),
            extension);
  }

  /** Insere um segmento (ex: "cancelados") logo após o prefixo/ambiente, preservando o resto da chave. */
  public static String withCancelledSegment(String originalKey, String cancelledSegment) {
    int firstSlash = originalKey.indexOf('/');
    int secondSlash = originalKey.indexOf('/', firstSlash + 1);
    String head = originalKey.substring(0, secondSlash + 1);
    String tail = originalKey.substring(secondSlash + 1);
    return head + cancelledSegment + "/" + tail;
  }
}
