package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.NotaUploadResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.InvalidNotaFileException;
import com.hortifruti.sl.hortifruti.exception.purchase.NotaFileTooLargeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida a foto de uma nota de compra e (opcionalmente) salva temporariamente em disco. O tipo real
 * do arquivo é determinado pelos magic bytes do conteúdo, nunca pelo Content-Type declarado ou pela
 * extensão do nome — ambos são facilmente forjáveis. {@link #validate} é reaproveitado pelo fluxo
 * de extração via Gemini, que não precisa persistir o arquivo em disco.
 */
@Slf4j
@Service
public class NotaUploadService {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  @Value("${nota.upload.max-size-bytes}")
  private long maxSizeBytes;

  @Value("${nota.upload.temp-directory}")
  private String tempDirectory;

  public record ValidatedFile(byte[] bytes, String contentType) {}

  public NotaUploadResponse upload(MultipartFile file) {
    ValidatedFile validated = validate(file);
    String arquivoId = UUID.randomUUID().toString();
    saveTemp(validated, arquivoId);

    log.info(
        "Nota recebida: arquivoId={}, tamanhoBytes={}, contentType={}",
        arquivoId,
        validated.bytes().length,
        validated.contentType());

    return new NotaUploadResponse(arquivoId, validated.bytes().length, validated.contentType());
  }

  public ValidatedFile validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidNotaFileException("Nenhum arquivo enviado.");
    }
    if (file.getSize() > maxSizeBytes) {
      throw new NotaFileTooLargeException(
          "Arquivo excede o tamanho máximo permitido de " + (maxSizeBytes / (1024 * 1024)) + "MB.");
    }

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new InvalidNotaFileException("Não foi possível ler o arquivo enviado.", e);
    }

    String contentType = detectRealContentType(bytes);
    if (contentType == null) {
      throw new InvalidNotaFileException(
          "Arquivo inválido. Envie uma foto em formato JPEG ou PNG.");
    }

    return new ValidatedFile(bytes, contentType);
  }

  private String detectRealContentType(byte[] bytes) {
    if (matchesSignature(bytes, PNG_SIGNATURE)) {
      return MediaType.IMAGE_PNG_VALUE;
    }
    if (matchesSignature(bytes, JPEG_SIGNATURE)) {
      return MediaType.IMAGE_JPEG_VALUE;
    }
    return null;
  }

  private boolean matchesSignature(byte[] bytes, byte[] signature) {
    if (bytes.length < signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if (bytes[i] != signature[i]) {
        return false;
      }
    }
    return true;
  }

  private void saveTemp(ValidatedFile file, String arquivoId) {
    try {
      Path dir = Path.of(tempDirectory);
      Files.createDirectories(dir);
      String extension = MediaType.IMAGE_PNG_VALUE.equals(file.contentType()) ? ".png" : ".jpg";
      Path target = dir.resolve(arquivoId + extension);
      Files.copy(
          new ByteArrayInputStream(file.bytes()), target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new InvalidNotaFileException("Falha ao salvar o arquivo enviado.", e);
    }
  }
}
