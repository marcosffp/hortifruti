package com.hortifruti.sl.hortifruti.service.notification.chatbot;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Constrói o texto de todas as mensagens enviadas pelo chatbot, isolando o "copy" do fluxo de
 * estados em {@code ChatbotConversationHandler}.
 */
public final class ChatbotMessageTemplates {

  public static final String CONTACT_PHONE = "(31) 3641-2244";

  private ChatbotMessageTemplates() {}

  public static String mainMenu() {
    return "Olá! Bem-vindo ao Hortifruti SL!\n\n"
        + "Como posso te ajudar hoje? Digite o número da opção:\n\n"
        + "*1* - 📋 Pedido - Fazer novo pedido\n"
        + "*2* - 💬 Outro assunto - Falar com atendimento\n"
        + "*3* - 💰 Boletos - Consultar boletos pendentes\n"
        + "*4* - 📄 Nota Fiscal - Consultar NF por número\n\n"
        + "Digite o número da opção desejada (1, 2, 3 ou 4)\n\n"
        + "💡 A qualquer momento, digite MENU para voltar aqui";
  }

  public static String pedidoPrompt() {
    return "📋 *Fazer Pedido*\n\n"
        + "Por favor, envie a lista de produtos que deseja:\n"
        + "Nossa equipe vai receber seu pedido e responder em breve com disponibilidade e valores.\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String outroPrompt() {
    return "💬 *Falar com Atendimento*\n\n"
        + "Por favor, descreva seu assunto ou dúvida:\n"
        + "Nossa equipe vai receber sua mensagem e responder em breve.\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String boletoPrompt() {
    return "💰 *Consultar Boletos Pendentes*\n\n"
        + "Para consultar seus boletos, por favor, envie seu CPF *(apenas números)* ou CNPJ.\n\n"
        + "Exemplo: 12345678900 ou 12345678000190\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String notaFiscalPrompt() {
    return "📄 *Consultar Nota Fiscal*\n\n"
        + "Por favor, envie o *número da nota fiscal* que deseja consultar.\n\n"
        + "Exemplo: 123456\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String invalidDocument() {
    return "❌ Documento inválido. Por favor, envie um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.\n\n"
        + "Exemplo: 12345678900 ou 12345678000190\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String invalidInvoiceNumber() {
    return "❌ Número da nota fiscal inválido.\n\n"
        + "Por favor, envie apenas números.\n"
        + "Exemplo: 123456\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String invoiceNotFound(String cleanNumber) {
    return "❌ Nota fiscal não encontrada.\n\n"
        + "Verifique se o número *"
        + cleanNumber
        + "* está correto.\n\n"
        + "💡 Digite MENU para voltar ao início ou entre em contato:\n"
        + "📞 "
        + CONTACT_PHONE;
  }

  public static String invoiceQueryFailed() {
    return "❌ Erro ao consultar a nota fiscal.\n\n"
        + "Por favor, tente novamente ou entre em contato:\n"
        + "📞 "
        + CONTACT_PHONE
        + "\n\n"
        + "💡 Digite MENU para voltar ao início";
  }

  public static String invoiceQueryException() {
    return "❌ Erro ao consultar a nota fiscal.\n\n"
        + "Por favor, verifique o número e tente novamente ou entre em contato:\n"
        + "📞 "
        + CONTACT_PHONE;
  }

  public static String invoiceSummaryHeader(InvoiceResponseGet invoiceResponse) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("📄 *Nota Fiscal Encontrada*\n\n");
    messageBuilder.append(String.format("*Número:* %s\n", invoiceResponse.number()));
    messageBuilder.append(String.format("*Status:* %s\n", invoiceResponse.status()));
    messageBuilder.append(String.format("*Valor Total:* R$ %.2f\n", invoiceResponse.totalValue()));
    messageBuilder.append(String.format("*Data:* %s\n", invoiceResponse.date()));
    messageBuilder.append(String.format("*Cliente:* %s\n\n", invoiceResponse.name()));
    return messageBuilder.toString();
  }

  public static String documentAvailableNotice(InvoiceResponseGet invoiceResponse) {
    return invoiceSummaryHeader(invoiceResponse)
        + "✅ *Documento Disponível*\n\n"
        + "Aguarde enquanto preparo o PDF da nota fiscal...";
  }

  public static String documentUnavailable() {
    return "⚠️ Documento não disponível no momento. Entre em contato: " + CONTACT_PHONE;
  }

  public static String documentProcessingError() {
    return "❌ Erro ao processar o documento. Entre em contato: " + CONTACT_PHONE;
  }

  public static String invoiceUnavailableStatus(InvoiceResponseGet invoiceResponse) {
    StringBuilder messageBuilder = new StringBuilder(invoiceSummaryHeader(invoiceResponse));
    messageBuilder.append("⚠️ *Documento Indisponível*\n\n");
    messageBuilder.append("Esta nota fiscal não está autorizada para download.\n");
    messageBuilder.append("Status atual: ").append(invoiceResponse.status()).append("\n\n");
    messageBuilder.append("Para mais informações, entre em contato:\n");
    messageBuilder.append("📞 " + CONTACT_PHONE);
    return messageBuilder.toString();
  }

  public static String clientNotFound() {
    return "Desculpe, não encontrei nenhum cliente com esse documento em nosso sistema.\n\n"
        + "Verifique se o CPF ou CNPJ está correto ou entre em contato conosco:\n"
        + CONTACT_PHONE;
  }

  public static String noPendingCharges(String clientName) {
    return String.format(
        "Olá, %s!\n\n"
            + "Boa notícia! Você não possui cobranças pendentes no momento.\n\n"
            + "Se tiver alguma dúvida, entre em contato conosco:\n"
            + CONTACT_PHONE,
        clientName);
  }

  public static String pendingChargesMessage(
      String clientName, List<CombinedScore> allPending, int totalWithBillet) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(String.format("Olá, %s!\n\n", clientName));

    messageBuilder.append(String.format("📋 *Cobranças Pendentes:* %d\n\n", allPending.size()));

    int i = 1;
    for (CombinedScore cs : allPending) {
      messageBuilder.append(String.format("*Cobrança %d:*\n", i));
      messageBuilder.append(String.format("Valor: R$ %.2f\n", cs.getTotalValue()));
      messageBuilder.append(
          String.format(
              "Vencimento: %s\n",
              cs.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

      if (cs.isHasBillet()) {
        messageBuilder.append(
            String.format(
                "✓ Boleto: %s\n", cs.getYourNumber() != null ? cs.getYourNumber() : "Disponível"));
      } else {
        messageBuilder.append("○ Boleto: Não emitido ainda\n");
      }

      if (i < allPending.size()) {
        messageBuilder.append("────────────────\n\n");
      }
      i++;
    }

    messageBuilder.append("\n────────────────\n\n");
    messageBuilder.append("📦 *Boletos Disponíveis:*\n");
    if (totalWithBillet > 0) {
      messageBuilder.append(String.format("✓ %d Boleto(s) para download\n", totalWithBillet));
    } else {
      messageBuilder.append("⚠️ Nenhum boleto disponível no momento\n");
    }

    return messageBuilder.toString();
  }

  public static String pendingBilletsNotice(int totalWithoutBillet) {
    return "⚠️ *Boletos Pendentes de Emissão*\n\n"
        + String.format(
            "Você possui %d cobrança(s) sem boleto emitido ainda.\n\n", totalWithoutBillet)
        + "*Entre em contato para mais informações:*\n"
        + "📞 "
        + CONTACT_PHONE
        + "\n\n"
        + "Horário de atendimento:\n"
        + "• Segunda a Sábado, 7h às 20h\n"
        + "• Domingo, 7h às 12h";
  }

  public static String unknownCommand() {
    return "Desculpe, não entendi sua solicitação.\n\n"
        + "Comandos disponíveis:\n"
        + "- 'boletos' - Ver cobranças em aberto\n"
        + "- 'ajuda' - Lista de comandos\n"
        + "- 'oi' - Saudação e boas-vindas\n\n"
        + "Tente usar uma dessas palavras-chave!\n\n"
        + "Para outras dúvidas: "
        + CONTACT_PHONE;
  }

  public static String errorMessage() {
    return "Ops! Ocorreu um erro temporário.\n\n"
        + "Por favor, tente novamente em alguns minutos ou entre em contato:\n\n"
        + CONTACT_PHONE
        + "\n"
        + "Segunda a Sexta, 8h às 18h";
  }
}
