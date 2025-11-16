package com.hortifruti.sl.hortifruti.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  public boolean sendSimpleEmail(String to, String subject, String text) {
    System.out.println("Iniciando envio de email simples...");
    System.out.println("Destinatário: " + to);
    System.out.println("Assunto: " + subject);
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, true); // true para HTML
      helper.setFrom(fromEmail);
      System.out.println("Email configurado com sucesso.");

      // Adicionar logo como anexo inline
      try {
        System.out.println("Tentando adicionar logo ao email...");
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
        if (logoResource.exists()) {
          helper.addInline("logo", logoResource);
          System.out.println("Logo adicionado com sucesso.");
        } else {
          System.out.println("Logo não encontrado.");
        }
      } catch (Exception e) {
        System.out.println("Erro ao adicionar logo: " + e.getMessage());
      }

      System.out.println("Enviando email...");
      mailSender.send(mimeMessage);
      System.out.println("Email enviado com sucesso!");
      return true;
    } catch (Exception e) {
      System.out.println("Erro ao enviar email: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    System.out.println("Iniciando envio de email com anexos...");
    System.out.println("Destinatário: " + to);
    System.out.println("Assunto: " + subject);
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, true); // true para HTML
      helper.setFrom(fromEmail);
      System.out.println("Email configurado com sucesso.");

      // Adicionar logo como anexo inline
      try {
        System.out.println("Tentando adicionar logo ao email...");
        ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
        if (logoResource.exists()) {
          helper.addInline("logo", logoResource);
          System.out.println("Logo adicionado com sucesso.");
        } else {
          System.out.println("Logo não encontrado.");
        }
      } catch (Exception e) {
        System.out.println("Erro ao adicionar logo: " + e.getMessage());
      }

      // Adicionar anexos
      if (attachments != null && fileNames != null) {
        System.out.println("Adicionando anexos ao email...");
        for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
          ByteArrayResource resource = new ByteArrayResource(attachments.get(i));
          helper.addAttachment(fileNames.get(i), resource);
          System.out.println("Anexo adicionado: " + fileNames.get(i));
        }
      } else {
        System.out.println("Nenhum anexo encontrado para adicionar.");
      }

      System.out.println("Enviando email...");
      mailSender.send(mimeMessage);
      System.out.println("Email enviado com sucesso!");
      return true;
    } catch (MessagingException e) {
      System.out.println("Erro ao enviar email: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendEmailWithSingleAttachment(
      String to, String subject, String text, byte[] attachment, String fileName) {
    System.out.println("Iniciando envio de email com um único anexo...");
    return sendEmailWithAttachments(to, subject, text, List.of(attachment), List.of(fileName));
  }
}
