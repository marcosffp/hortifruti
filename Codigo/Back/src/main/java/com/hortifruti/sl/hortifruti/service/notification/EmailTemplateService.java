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

  /** Carrega um template HTML e substitui as variáveis */
  public String processTemplate(String templateName, Map<String, String> variables) {
    try {
      String template = loadTemplate(templateName);
      return replaceVariables(template, variables);
    } catch (IOException e) {
      return getFallbackMessage(templateName);
    }
  }

  /** Carrega o template HTML do arquivo (prioriza versões clean) */
  private String loadTemplate(String templateName) throws IOException {
    // Primeiro tenta carregar a versão clean (sem avisos)
    String cleanTemplateName = templateName + "-clean";
    Resource cleanResource =
        resourceLoader.getResource("classpath:templates/email/" + cleanTemplateName + ".html");

    if (cleanResource.exists()) {
      return cleanResource.getContentAsString(StandardCharsets.UTF_8);
    }

    // Fallback para a versão original
    Resource resource =
        resourceLoader.getResource("classpath:templates/email/" + templateName + ".html");
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }

  /** Substitui as variáveis no template */
  private String replaceVariables(String template, Map<String, String> variables) {
    String result = template;

    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";

      // Substituir variáveis simples: {{VARIABLE}}
      result = result.replace("{{" + key + "}}", value);

      // Processar blocos condicionais: {{#VARIABLE}} conteúdo {{/VARIABLE}}
      if (value != null && !value.isEmpty()) {
        // Mostrar o bloco se a variável tem valor
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}([\\s\\S]*?)\\{\\{/" + key + "\\}\\}", "$1");
      } else {
        // Remover o bloco se a variável está vazia
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}[\\s\\S]*?\\{\\{/" + key + "\\}\\}", "");
      }
    }

    // Remover blocos condicionais não processados
    result = result.replaceAll("\\{\\{#[^}]+\\}\\}[\\s\\S]*?\\{\\{/[^}]+\\}\\}", "");

    // Converter quebras de linha em <br> se necessário
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      if (entry.getValue() != null && entry.getValue().contains("\n")) {
        String key = "{{" + entry.getKey() + "}}";
        String valueWithBr = entry.getValue().replace("\n", "<br>");
        result = result.replace(key, valueWithBr);
      }
    }

    return result;
  }

  /** Mensagem de fallback caso o template não carregue */
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
