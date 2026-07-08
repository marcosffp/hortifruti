package com.hortifruti.sl.hortifruti.service.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

  private final ResourceLoader resourceLoader;

  public EmailTemplateService(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public String processTemplate(String templateName, Map<String, String> variables) {
    try {
      String template = loadTemplate(templateName);
      return replaceVariables(template, variables);
    } catch (IOException e) {
      return getFallbackMessage(templateName);
    }
  }

  private String loadTemplate(String templateName) throws IOException {
    // Primeiro tenta carregar a versão clean (sem avisos)
    String cleanTemplateName = templateName + "-clean";
    Resource cleanResource =
        resourceLoader.getResource("classpath:templates/email/" + cleanTemplateName + ".html");

    if (cleanResource.exists()) {
      return cleanResource.getContentAsString(StandardCharsets.UTF_8);
    }

    Resource resource =
        resourceLoader.getResource("classpath:templates/email/" + templateName + ".html");
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }

  private String replaceVariables(String template, Map<String, String> variables) {
    String result = template;

    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";

      // Substituir variáveis simples: {{VARIABLE}}
      result = result.replace("{{" + key + "}}", value);

      // Processar blocos condicionais: {{#VARIABLE}} conteúdo {{/VARIABLE}}
      if (value != null && !value.isEmpty()) {
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}([\\s\\S]*?)\\{\\{/" + key + "\\}\\}", "$1");
      } else {
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}[\\s\\S]*?\\{\\{/" + key + "\\}\\}", "");
      }
    }

    // Remover blocos condicionais não processados
    result = result.replaceAll("\\{\\{#[^}]+\\}\\}[\\s\\S]*?\\{\\{/[^}]+\\}\\}", "");

    for (Map.Entry<String, String> entry : variables.entrySet()) {
      if (entry.getValue() != null && entry.getValue().contains("\n")) {
        String key = "{{" + entry.getKey() + "}}";
        String valueWithBr = entry.getValue().replace("\n", "<br>");
        result = result.replace(key, valueWithBr);
      }
    }

    return result;
  }

  private String getFallbackMessage(String templateName) {
    return """
        <html>
        <body style="font-family: Arial, sans-serif; padding: 20px;">
            <h2>Hortifruti SL</h2>
            <p>Prezados,</p>
            <p>Seguem os documentos solicitados em anexo.</p>
            <p>Atenciosamente,<br>Hortifruti SL</p>
        </body>
        </html>
        """;
  }
}
