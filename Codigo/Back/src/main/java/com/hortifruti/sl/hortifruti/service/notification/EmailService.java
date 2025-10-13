package com.hortifruti.sl.hortifruti.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  public boolean sendSimpleEmail(String to, String subject, String text) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, true); // true para HTML
      helper.setFrom("noreply@hortifrutisantaluzia.com");

      // Adicionar logo como anexo inline
      try {
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
        if (logoResource.exists()) {
          helper.addInline("logo", logoResource);
        }
      } catch (Exception e) {
        // Logo não encontrado, continuar sem ele
        System.out.println("Logo não encontrado: " + e.getMessage());
      }

      mailSender.send(mimeMessage);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendEmailWithAttachments(String to, String subject, String text, 
                                         List<byte[]> attachments, List<String> fileNames) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, true); // true para HTML
      helper.setFrom("noreply@hortifrutisantaluzia.com");

      // Adicionar logo como anexo inline
      try {
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
        if (logoResource.exists()) {
          helper.addInline("logo", logoResource);
        }
      } catch (Exception e) {
        // Logo não encontrado, continuar sem ele
        System.out.println("Logo não encontrado: " + e.getMessage());
      }

      // Adicionar anexos
      if (attachments != null && fileNames != null) {
        for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
          ByteArrayResource resource = new ByteArrayResource(attachments.get(i));
          helper.addAttachment(fileNames.get(i), resource);
        }
      }

      mailSender.send(mimeMessage);
      return true;
    } catch (MessagingException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendEmailWithSingleAttachment(String to, String subject, String text, 
                                              byte[] attachment, String fileName) {
    return sendEmailWithAttachments(to, subject, text, List.of(attachment), List.of(fileName));
  }
}