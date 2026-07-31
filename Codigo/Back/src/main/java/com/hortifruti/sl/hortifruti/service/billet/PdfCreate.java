package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PdfCreate {
  public ResponseEntity<byte[]> createResponsePdf(String pdfBase64, String nomeArquivo) {
    try {
      if (pdfBase64 == null || pdfBase64.trim().isEmpty()) {
        throw new BilletException("O conteúdo do PDF em Base64 está vazio ou nulo.");
      }

      byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", nomeArquivo);

      return ResponseEntity.ok().headers(headers).body(pdfBytes);

    } catch (IllegalArgumentException e) {
      throw new BilletException(
          "Erro ao decodificar o PDF em Base64. O conteúdo pode estar corrompido ou inválido.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao criar a resposta do PDF.", e);
    }
  }

  public byte[] convertBase64ToBytes(String pdfBase64) {
    try {
      if (pdfBase64 == null || pdfBase64.trim().isEmpty()) {
        throw new BilletException("O conteúdo do PDF em Base64 está vazio ou nulo.");
      }

      return Base64.getDecoder().decode(pdfBase64);

    } catch (IllegalArgumentException e) {
      throw new BilletException(
          "Erro ao decodificar o PDF em Base64. O conteúdo pode estar corrompido ou inválido.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao decodificar o PDF.", e);
    }
  }
}
