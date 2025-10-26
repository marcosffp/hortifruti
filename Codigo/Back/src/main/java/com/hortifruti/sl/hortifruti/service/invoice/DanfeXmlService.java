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
              .block();

      if (fileBytes == null || fileBytes.length == 0) {
        return ResponseEntity.notFound().build();
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

    } catch (Exception e) {
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
