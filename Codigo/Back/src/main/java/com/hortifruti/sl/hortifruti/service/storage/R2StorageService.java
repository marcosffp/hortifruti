package com.hortifruti.sl.hortifruti.service.storage;

import com.hortifruti.sl.hortifruti.exception.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

  private final S3Client r2Client;

  @Value("${r2.bucket-name}")
  private String bucketName;

  /**
   * Envia o conteúdo para o R2. Lança {@link StorageException} em caso de falha — quem chama não
   * deve considerar a operação (geração de boleto/nota/extrato) concluída com sucesso.
   */
  public String upload(byte[] content, String key, String contentType) {
    try {
      r2Client.putObject(
          PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build(),
          RequestBody.fromBytes(content));
      return key;
    } catch (S3Exception | software.amazon.awssdk.core.exception.SdkClientException e) {
      log.error("[R2Storage] upload key={} outcome=FAILURE message={}", key, e.getMessage());
      throw new StorageException("Falha ao enviar arquivo para o R2.", e);
    }
  }

  /**
   * Remove um objeto do R2. Usado para desfazer um upload já feito quando o registro correspondente
   * no banco não pôde ser persistido (ex: colisão de ref concorrente) — evita deixar arquivos
   * órfãos duplicados no bucket.
   */
  public void delete(String key) {
    try {
      r2Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    } catch (S3Exception | software.amazon.awssdk.core.exception.SdkClientException e) {
      log.error("[R2Storage] delete key={} outcome=FAILURE message={}", key, e.getMessage());
      throw new StorageException("Falha ao remover arquivo do R2.", e);
    }
  }

  public byte[] download(String key) {
    try {
      return r2Client
          .getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())
          .readAllBytes();
    } catch (S3Exception
        | software.amazon.awssdk.core.exception.SdkClientException
        | java.io.IOException e) {
      log.error("[R2Storage] download key={} outcome=FAILURE message={}", key, e.getMessage());
      throw new StorageException("Falha ao baixar arquivo do R2.", e);
    }
  }

  /**
   * Move um objeto (copy + delete) para uma chave de cancelados/canceladas. Se a cópia falhar, o
   * objeto original nunca é apagado — falha de forma segura sem perder o arquivo.
   */
  public void moveToCancelled(String sourceKey, String destinationKey) {
    try {
      r2Client.copyObject(
          CopyObjectRequest.builder()
              .sourceBucket(bucketName)
              .sourceKey(sourceKey)
              .destinationBucket(bucketName)
              .destinationKey(destinationKey)
              .build());
    } catch (S3Exception | software.amazon.awssdk.core.exception.SdkClientException e) {
      log.error(
          "[R2Storage] move sourceKey={} destinationKey={} outcome=FAILURE message={}",
          sourceKey,
          destinationKey,
          e.getMessage());
      throw new StorageException("Falha ao mover arquivo para cancelados no R2.", e);
    }

    try {
      r2Client.deleteObject(
          DeleteObjectRequest.builder().bucket(bucketName).key(sourceKey).build());
    } catch (S3Exception | software.amazon.awssdk.core.exception.SdkClientException e) {
      log.error(
          "[R2Storage] delete-original-after-copy sourceKey={} outcome=FAILURE message={}"
              + " — objeto duplicado em destinationKey={}, requer limpeza manual",
          sourceKey,
          e.getMessage(),
          destinationKey);
      throw new StorageException(
          "Falha ao remover arquivo original após mover para cancelados.", e);
    }
  }
}
