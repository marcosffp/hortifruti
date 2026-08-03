package com.hortifruti.sl.hortifruti.service.notification.email;

import java.util.List;

/**
 * Contrato comum para qualquer provedor de envio de email (SendGrid, Gmail SMTP, etc). Novos
 * provedores devem implementar esta interface e ser plugados em {@link EmailService} via a
 * propriedade {@code email.provider}, sem exigir mudanças nos demais serviços que hoje dependem
 * apenas de {@link EmailService}.
 */
public interface EmailSender {

  String providerName();

  boolean sendSimpleEmail(String to, String subject, String text);

  boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames);

  /**
   * Mesmo envio, mas com todos os destinatários no "to" de uma única mensagem (em vez de uma
   * mensagem por destinatário) — usado quando mais de uma pessoa precisa receber exatamente o
   * mesmo e-mail (ex.: contabilidade com várias pessoas cadastradas).
   */
  boolean sendSimpleEmail(List<String> to, String subject, String text);

  boolean sendEmailWithAttachments(
      List<String> to,
      String subject,
      String text,
      List<byte[]> attachments,
      List<String> fileNames);
}
