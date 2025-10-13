package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PdfCreate {
  /**
   * Cria uma resposta HTTP contendo um PDF.
   *
   * @param pdfBase64 String Base64 contendo o PDF
   * @param nomeArquivo Nome do arquivo PDF para download
   * @return Resposta HTTP com o PDF
   * @throws BilletException Se o PDF em Base64 for inválido ou ocorrer algum erro na decodificação
   */
  public ResponseEntity<byte[]> createResponsePdf(String pdfBase64, String nomeArquivo) {
    try {
      // Verifica se o Base64 está vazio ou nulo
      if (pdfBase64 == null || pdfBase64.trim().isEmpty()) {
        throw new BilletException("O conteúdo do PDF em Base64 está vazio ou nulo.");
      }

      // Decodifica o Base64 para bytes
      byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

      // Configura os headers para retornar o PDF
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", nomeArquivo);

      // Retorna o PDF como resposta
      return ResponseEntity.ok().headers(headers).body(pdfBytes);

    } catch (IllegalArgumentException e) {
      throw new BilletException(
          "Erro ao decodificar o PDF em Base64. O conteúdo pode estar corrompido ou inválido.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao criar a resposta do PDF.", e);
    }
  }
}
