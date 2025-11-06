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
      String filePath = rootNode.path(jsonPath).asText();

      return filePath;
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar arquivo com referência: " + ref, e);
    }
  }

  private String getDanfePath(String ref) {
    try {
      System.out.println("========================================");
      System.out.println("DanfeXmlService.getDanfePath()");
      System.out.println("Ref: " + ref);
      
      String response = focusNfeApiClient.sendGetRequest(ref, COMPLETE);
      
      System.out.println("Response recebida:");
      System.out.println("  É null? " + (response == null));
      System.out.println("  Length: " + (response != null ? response.length() : 0));
      
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(response);
      String filePath = rootNode.path("caminho_danfe").asText();
      
      System.out.println("Campo 'caminho_danfe':");
      System.out.println("  Existe? " + rootNode.has("caminho_danfe"));
      System.out.println("  Valor: '" + filePath + "'");
      System.out.println("  Está vazio? " + (filePath == null || filePath.isEmpty()));
      System.out.println("========================================");
      
      if (filePath == null || filePath.isEmpty()) {
        throw new InvoiceException("Campo 'caminho_danfe' não encontrado ou vazio para ref: " + ref);
      }
      
      return filePath;
    } catch (Exception e) {
      System.err.println("✗ ERRO em getDanfePath para ref: " + ref);
      System.err.println("Erro: " + e.getMessage());
      e.printStackTrace();
      throw new InvoiceException("Erro ao obter caminho do DANFE para ref: " + ref, e);
    }
  }

  private String getXmlPath(String ref) {
    return getFilePathFromApi(ref, "caminho_xml_nota_fiscal");
  }

  private ResponseEntity<Resource> downloadFileStream(
      String ref, String fileUrl, MediaType mediaType, String filePrefix) {
    try {
      System.out.println("========================================");
      System.out.println("DanfeXmlService.downloadFileStream()");
      System.out.println("Ref: " + ref);
      System.out.println("FileUrl recebido: '" + fileUrl + "'");
      System.out.println("FileUrl é null? " + (fileUrl == null));
      System.out.println("FileUrl está vazio? " + (fileUrl != null && fileUrl.isEmpty()));
      
      if (fileUrl == null || fileUrl.trim().isEmpty()) {
        System.err.println("✗ ERRO: fileUrl está vazio ou null!");
        System.err.println("Não é possível fazer download sem o caminho do arquivo");
        System.err.println("========================================");
        return ResponseEntity.notFound().build();
      }
      
      String fullUrl = focusNfeApiUrl + fileUrl;
      System.out.println("URL completa: " + fullUrl);
      System.out.println("Iniciando download...");

      byte[] fileBytes =
          webClient
              .get()
              .uri(fullUrl)
              .accept(MediaType.ALL)
              .retrieve()
              .bodyToMono(byte[].class)
              .block();

      System.out.println("Download concluído");
      System.out.println("  fileBytes é null? " + (fileBytes == null));
      System.out.println("  fileBytes length: " + (fileBytes != null ? fileBytes.length : 0));

      if (fileBytes == null || fileBytes.length == 0) {
        System.err.println("✗ Arquivo vazio ou não encontrado!");
        System.err.println("========================================");
        return ResponseEntity.notFound().build();
      }

      Resource resource = new ByteArrayResource(fileBytes);
      System.out.println("✓ Resource criado com sucesso - " + fileBytes.length + " bytes");
      System.out.println("========================================");

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

    } catch (Exception e) {
      System.err.println("========================================");
      System.err.println("✗ ERRO em downloadFileStream");
      System.err.println("Ref: " + ref);
      System.err.println("FileUrl: " + fileUrl);
      System.err.println("Erro: " + e.getMessage());
      e.printStackTrace();
      System.err.println("========================================");
      throw new InvoiceException("Erro ao fazer download do arquivo", e);
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

  @Transactional
  protected ResponseEntity<Resource> downloadDanfe(String ref) {
    String danfePath = getDanfePath(ref);
    return downloadFileStream(ref, danfePath, MediaType.APPLICATION_PDF, "danfe");
  }

  @Transactional
  protected ResponseEntity<Resource> downloadXml(String ref) {
    String xmlPath = getXmlPath(ref);
    return downloadFileStream(ref, xmlPath, MediaType.APPLICATION_XML, "nota-fiscal");
  }
}
