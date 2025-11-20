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
    System.out.println("Iniciando método processTemplate...");
    try {
      System.out.println("Carregando template: " + templateName);
      String template = loadTemplate(templateName);
      System.out.println("Template carregado com sucesso.");
      System.out.println("Substituindo variáveis no template...");
      return replaceVariables(template, variables);
    } catch (IOException e) {
      System.out.println("Erro ao carregar template: " + e.getMessage());
      return getFallbackMessage(templateName);
    }
  }

  /** Carrega o template HTML do arquivo (prioriza versões clean) */
  private String loadTemplate(String templateName) throws IOException {
    System.out.println("Iniciando método loadTemplate...");
    // Primeiro tenta carregar a versão clean (sem avisos)
    String cleanTemplateName = templateName + "-clean";
    System.out.println("Tentando carregar template clean: " + cleanTemplateName);
    Resource cleanResource =
        resourceLoader.getResource("classpath:templates/email/" + cleanTemplateName + ".html");

    if (cleanResource.exists()) {
      System.out.println("Template clean encontrado.");
      return cleanResource.getContentAsString(StandardCharsets.UTF_8);
    }

    // Fallback para a versão original
    System.out.println("Template clean não encontrado. Tentando carregar template original: " + templateName);
    Resource resource =
        resourceLoader.getResource("classpath:templates/email/" + templateName + ".html");
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }

  /** Substitui as variáveis no template */
  private String replaceVariables(String template, Map<String, String> variables) {
    System.out.println("Iniciando método replaceVariables...");
    String result = template;

    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";
      System.out.println("Substituindo variável: " + key + " com valor: " + value);

      // Substituir variáveis simples: {{VARIABLE}}
      result = result.replace("{{" + key + "}}", value);

      // Processar blocos condicionais: {{#VARIABLE}} conteúdo {{/VARIABLE}}
      if (value != null && !value.isEmpty()) {
        System.out.println("Processando bloco condicional para variável: " + key);
        // Mostrar o bloco se a variável tem valor
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}([\\s\\S]*?)\\{\\{/" + key + "\\}\\}", "$1");
      } else {
        System.out.println("Removendo bloco condicional para variável vazia: " + key);
        // Remover o bloco se a variável está vazia
        result =
            result.replaceAll("\\{\\{#" + key + "\\}\\}[\\s\\S]*?\\{\\{/" + key + "\\}\\}", "");
      }
    }

    // Remover blocos condicionais não processados
    System.out.println("Removendo blocos condicionais não processados...");
    result = result.replaceAll("\\{\\{#[^}]+\\}\\}[\\s\\S]*?\\{\\{/[^}]+\\}\\}", "");

    // Converter quebras de linha em <br> se necessário
    System.out.println("Convertendo quebras de linha em <br>...");
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      if (entry.getValue() != null && entry.getValue().contains("\n")) {
        String key = "{{" + entry.getKey() + "}}";
        String valueWithBr = entry.getValue().replace("\n", "<br>");
        result = result.replace(key, valueWithBr);
      }
    }

    System.out.println("Substituição de variáveis concluída.");
    return result;
  }

  /** Mensagem de fallback caso o template não carregue */
  private String getFallbackMessage(String templateName) {
    System.out.println("Carregando mensagem de fallback para template: " + templateName);
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
