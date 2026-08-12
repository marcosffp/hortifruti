package com.hortifruti.sl.hortifruti.service.notification.email;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Lógica de anexar o logo inline do e-mail, antes duplicada quase identicamente em {@link
 * GmailSmtpEmailSender} e {@link GmailApiEmailSender} (que usam {@link MimeMessageHelper} e por
 * isso reaproveitam {@link #attachInline}) e em {@link SendGridEmailSender} (que usa a API de
 * anexos própria do SendGrid e por isso só reaproveita a leitura dos bytes via {@link
 * #readLogoBytes}).
 */
@Slf4j
public final class InlineLogoAttacher {

  public static final String CONTENT_ID = "logo";
  private static final String LOGO_RESOURCE_PATH = "static/images/logo.png";

  private InlineLogoAttacher() {}

  public static void attachInline(MimeMessageHelper helper) {
    try {
      ClassPathResource logoResource = new ClassPathResource(LOGO_RESOURCE_PATH);
      if (logoResource.exists()) {
        helper.addInline(CONTENT_ID, logoResource);
      }
    } catch (MessagingException e) {
      log.warn("Não foi possível anexar o logo inline ao email: {}", e.getMessage());
    }
  }

  public static Optional<byte[]> readLogoBytes() {
    try {
      ClassPathResource logoResource = new ClassPathResource(LOGO_RESOURCE_PATH);
      if (logoResource.exists()) {
        return Optional.of(logoResource.getInputStream().readAllBytes());
      }
    } catch (IOException e) {
      log.warn("Não foi possível anexar o logo inline ao email: {}", e.getMessage());
    }
    return Optional.empty();
  }
}
