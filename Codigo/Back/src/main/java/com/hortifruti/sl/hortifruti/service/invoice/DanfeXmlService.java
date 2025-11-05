package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Service
public class DanfeXmlService {

  private final WebClient webClient;
  private final FocusNfeApiClient focusNfeApiClient;
  private final int COMPLETE = 1;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  private String getFilePathFromApi(String ref, String jsonPath) {
    try {
      String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(response);
      
      // Verifica o status da nota
      String status = rootNode.path("status").asText();
      if (status.contains("processando") || status.contains("pendente")) {
        throw new InvoiceException("A nota fiscal ainda está sendo processada. Aguarde alguns instantes e tente novamente.");
      }
      
      String filePath = rootNode.path(jsonPath).asText();
      
      // Verifica se o caminho do arquivo foi retornado
      if (filePath == null || filePath.trim().isEmpty()) {
        throw new InvoiceException("Arquivo ainda não disponível. A nota fiscal pode estar em processamento.");
      }

      return filePath;
    } catch (InvoiceException e) {
      throw e;
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar arquivo com referência: " + ref, e);
    }
  }

  private String getDanfePath(String ref) {
    return getFilePathFromApi(ref, "caminho_danfe");
  }

  private String getXmlPath(String ref) {
    return getFilePathFromApi(ref, "caminho_xml_nota_fiscal");
  }

  private ResponseEntity<Resource> downloadFileStream(
      String ref, String fileUrl, MediaType mediaType, String filePrefix) {
    try {
      String fullUrl = focusNfeApiUrl + fileUrl;

      byte[] fileBytes =
          webClient
              .get()
              .uri(fullUrl)
              .accept(MediaType.ALL)
              .retrieve()
              .bodyToMono(byte[].class)
              .timeout(java.time.Duration.ofSeconds(100)) 
              .block();

      if (fileBytes == null || fileBytes.length == 0) {
        throw new InvoiceException("Arquivo não disponível ou vazio. A nota fiscal pode ainda estar sendo processada.");
      }

      Resource resource = new ByteArrayResource(fileBytes);

      return ResponseEntity.ok()
          .contentType(mediaType)
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\""
                  + filePrefix
                  + "-"
                  + ref
                  + getFileExtension(mediaType)
                  + "\"")
          .body(resource);

    } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
      throw new InvoiceException("Erro de conexão ao baixar arquivo. A nota fiscal pode ainda estar sendo processada. Tente novamente em alguns instantes.", e);
    } catch (Exception e) {
      throw new InvoiceException("Erro ao fazer download do arquivo: " + e.getMessage(), e);
    }
  }

  private String getFileExtension(MediaType mediaType) {
    if (mediaType.equals(MediaType.APPLICATION_PDF)) {
      return ".pdf";
    } else if (mediaType.equals(MediaType.APPLICATION_XML)) {
      return ".xml";
    }
    return "";
  }

  private ResponseEntity<Resource> downloadWithRetry(
      String ref, 
      String fileType, 
      MediaType mediaType, 
      String filePrefix,
      int initialDelay) {
    
    // Delay inicial (usado apenas para DANFE logo após criação)
    if (initialDelay > 0) {
      sleep(initialDelay);
    }
    
    int maxRetries = 4;
    int retryDelay = 4000;
    
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        String filePath = "danfe".equals(fileType) 
            ? getDanfePath(ref) 
            : getXmlPath(ref);
            
        return downloadFileStream(ref, filePath, mediaType, filePrefix);
        
      } catch (InvoiceException e) {
        if (attempt == maxRetries) {
          String errorMsg = "danfe".equals(fileType)
              ? "DANFE ainda não disponível. A nota fiscal foi criada com sucesso mas ainda está sendo processada. Aguarde alguns instantes e clique em 'Ver NF' para visualizar."
              : "XML ainda não disponível. Aguarde alguns instantes e tente novamente.";
          throw new InvoiceException(errorMsg, e);
        }
        
        if (e.getMessage().contains("processando") || e.getMessage().contains("não disponível")) {
          sleep(retryDelay);
          retryDelay += 1000;
        } else {
          throw e;
        }
      }
    }
    
    throw new InvoiceException("Não foi possível baixar o " + fileType.toUpperCase() + " após " + maxRetries + " tentativas");
  }

  private void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new InvoiceException("Timeout ao aguardar processamento da nota fiscal", ie);
    }
  }

  @Transactional
  protected ResponseEntity<Resource> downloadDanfe(String ref) {
    return downloadWithRetry(ref, "danfe", MediaType.APPLICATION_PDF, "danfe", 4000);
  }

  @Transactional
  protected ResponseEntity<Resource> downloadXml(String ref) {
    return downloadWithRetry(ref, "xml", MediaType.APPLICATION_XML, "nota-fiscal", 0);
  }
}
