package com.hortifruti.sl.hortifruti.tools;

import com.hortifruti.sl.hortifruti.model.invoice.FiscalNoteXmlStorage;
import com.hortifruti.sl.hortifruti.repository.invoice.FiscalNoteXmlStorageRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Ferramenta de manutenção one-off: identifica objetos de nota fiscal (XML/DANFE) no R2 que não
 * estão referenciados por nenhuma linha em fiscal_note_xml_storage — órfãos deixados pela corrida
 * corrigida em FiscalNoteXmlStorageService — e reporta ou move para quarentena.
 *
 * <p>Inerte por padrão: só age se a env var CLEANUP_ORPHAN_FISCAL_FILES estiver setada. "report"
 * (ou qualquer valor não reconhecido) = só lista, não move nada. "quarantine" = move os órfãos para
 * notas-fiscais/{env}/_orfaos-removidos/, preservando o restante do caminho.
 *
 * <p>Remover esta classe após o uso único — não é destinada a ficar no serviço em produção.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Integer.MAX_VALUE)
public class OrphanFiscalFileCleanupRunner implements ApplicationRunner {

  private final S3Client r2Client;
  private final FiscalNoteXmlStorageRepository repository;

  @Value("${r2.bucket-name}")
  private String bucketName;

  @Value("${r2.environment}")
  private String environment;

  @Override
  public void run(ApplicationArguments args) {
    String mode = System.getenv("CLEANUP_ORPHAN_FISCAL_FILES");
    if (mode == null || mode.isBlank()) {
      return;
    }

    String prefix = "notas-fiscais/" + environment + "/";
    String quarantinePrefix = prefix + "_orfaos-removidos/";

    log.info("=== LIMPEZA DE ARQUIVOS ÓRFÃOS DE NOTA FISCAL ===");
    log.info("bucket={} prefix={} modo={}", bucketName, prefix, mode);

    Set<String> referencedKeys = new HashSet<>();
    List<FiscalNoteXmlStorage> allRows = repository.findAll();
    for (FiscalNoteXmlStorage s : allRows) {
      if (s.getObjectKey() != null) referencedKeys.add(s.getObjectKey());
      if (s.getDanfeObjectKey() != null) referencedKeys.add(s.getDanfeObjectKey());
    }
    log.info(
        "Linhas no banco: {} | chaves referenciadas (xml+danfe): {}",
        allRows.size(),
        referencedKeys.size());

    List<S3Object> allObjects = listAll(prefix);
    log.info("Objetos no bucket sob prefix={}: {}", prefix, allObjects.size());

    List<S3Object> orphans =
        allObjects.stream()
            .filter(o -> !o.key().startsWith(quarantinePrefix))
            .filter(o -> !referencedKeys.contains(o.key()))
            .toList();

    long totalBytes = orphans.stream().mapToLong(S3Object::size).sum();
    log.info(
        "=== RESULTADO: {} objetos órfãos encontrados ({} bytes / {} MB) ===",
        orphans.size(),
        totalBytes,
        totalBytes / 1024.0 / 1024.0);
    for (S3Object o : orphans) {
      log.info("  ORFAO key={} size={} lastModified={}", o.key(), o.size(), o.lastModified());
    }

    if ("quarantine".equalsIgnoreCase(mode)) {
      int moved = 0;
      for (S3Object o : orphans) {
        String dest = quarantinePrefix + o.key().substring(prefix.length());
        try {
          r2Client.copyObject(
              CopyObjectRequest.builder()
                  .sourceBucket(bucketName)
                  .sourceKey(o.key())
                  .destinationBucket(bucketName)
                  .destinationKey(dest)
                  .build());
          r2Client.deleteObject(
              DeleteObjectRequest.builder().bucket(bucketName).key(o.key()).build());
          log.info("Movido para quarentena: {} -> {}", o.key(), dest);
          moved++;
        } catch (Exception e) {
          log.error("Falha ao mover órfão key={}: {}", o.key(), e.getMessage(), e);
        }
      }
      log.info("=== QUARENTENA CONCLUÍDA: {}/{} objetos movidos ===", moved, orphans.size());
    } else {
      log.info("=== MODO RELATÓRIO: nenhum objeto foi movido ou apagado ===");
    }

    System.exit(0);
  }

  private List<S3Object> listAll(String prefix) {
    List<S3Object> result = new ArrayList<>();
    String continuationToken = null;
    do {
      ListObjectsV2Request.Builder reqBuilder =
          ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix);
      if (continuationToken != null) {
        reqBuilder.continuationToken(continuationToken);
      }
      ListObjectsV2Response resp = r2Client.listObjectsV2(reqBuilder.build());
      result.addAll(resp.contents());
      continuationToken =
          Boolean.TRUE.equals(resp.isTruncated()) ? resp.nextContinuationToken() : null;
    } while (continuationToken != null);
    return result;
  }
}
