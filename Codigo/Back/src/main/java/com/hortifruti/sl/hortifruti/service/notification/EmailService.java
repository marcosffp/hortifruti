package com.hortifruti.sl.hortifruti.service.notification;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  @Value("${sendgrid.api.key}")
  private String sendGridApiKey;

  @Value("${sendgrid.from.email}")
  private String fromEmail;

  public boolean sendSimpleEmail(String to, String subject, String text) {
    System.out.println("Iniciando envio de email simples...");
    try {
      log.info("Tentando enviar email de '{}' para '{}'", fromEmail, to);
      System.out.println("De: " + fromEmail + ", Para: " + to + ", Assunto: " + subject);

      Email from = new Email(fromEmail);
      Email toEmail = new Email(to);
      Content content = new Content("text/html", text);
      Mail mail = new Mail(from, subject, toEmail, content);

      System.out.println("Adicionando logo inline...");
      addInlineLogo(mail);

      SendGrid sg = new SendGrid(sendGridApiKey);
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      
      System.out.println("Enviando email via SendGrid...");
      Response response = sg.api(request);
      
      System.out.println("Resposta do SendGrid: Status = " + response.getStatusCode() + ", Body = " + response.getBody());
      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        log.info("Email enviado com sucesso para: {}", to);
        return true;
      } else {
        log.error("Erro ao enviar email. Status: {}, Body: {}", 
                  response.getStatusCode(), response.getBody());
        log.error("Email remetente usado: {}", fromEmail);
        return false;
      }
    } catch (IOException e) {
      log.error("Erro ao enviar email para: {}", to, e);
      System.out.println("Exceção ao enviar email: " + e.getMessage());
      return false;
    }
  }

  public boolean sendEmailWithAttachments(
      String to, String subject, String text, List<byte[]> attachments, List<String> fileNames) {
    System.out.println("Iniciando envio de email com anexos...");
    try {
      Email from = new Email(fromEmail);
      Email toEmail = new Email(to);
      Content content = new Content("text/html", text);
      Mail mail = new Mail(from, subject, toEmail, content);

      System.out.println("Adicionando logo inline...");
      addInlineLogo(mail);

      // Adicionar anexos
      if (attachments != null && fileNames != null) {
        System.out.println("Adicionando anexos...");
        for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
          Attachments attachment = new Attachments();
          String encodedFile = Base64.getEncoder().encodeToString(attachments.get(i));
          attachment.setContent(encodedFile);
          attachment.setFilename(fileNames.get(i));
          attachment.setDisposition("attachment");
          mail.addAttachments(attachment);
          System.out.println("Anexo adicionado: " + fileNames.get(i));
        }
      }

      SendGrid sg = new SendGrid(sendGridApiKey);
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      
      System.out.println("Enviando email com anexos via SendGrid...");
      Response response = sg.api(request);
      
      System.out.println("Resposta do SendGrid: Status = " + response.getStatusCode() + ", Body = " + response.getBody());
      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        log.info("Email com anexos enviado com sucesso para: {}", to);
        return true;
      } else {
        log.error("Erro ao enviar email com anexos. Status: {}, Body: {}", 
                  response.getStatusCode(), response.getBody());
        return false;
      }
    } catch (IOException e) {
      log.error("Erro ao enviar email com anexos para: {}", to, e);
      System.out.println("Exceção ao enviar email com anexos: " + e.getMessage());
      return false;
    }
  }

  public boolean sendEmailWithSingleAttachment(
      String to, String subject, String text, byte[] attachment, String fileName) {
    System.out.println("Iniciando envio de email com único anexo...");
    return sendEmailWithAttachments(to, subject, text, List.of(attachment), List.of(fileName));
  }

  private void addInlineLogo(Mail mail) {
    System.out.println("Tentando adicionar logo inline...");
    try {
      ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
      if (logoResource.exists()) {
        System.out.println("Logo encontrado, lendo bytes...");
        byte[] logoBytes = logoResource.getInputStream().readAllBytes();
        String encodedLogo = Base64.getEncoder().encodeToString(logoBytes);
        
        Attachments logo = new Attachments();
        logo.setContent(encodedLogo);
        logo.setType("image/png");
        logo.setFilename("logo.png");
        logo.setDisposition("inline");
        logo.setContentId("logo");
        mail.addAttachments(logo);
        System.out.println("Logo adicionado com sucesso.");
      } else {
        System.out.println("Logo não encontrado.");
      }
    } catch (IOException e) {
      log.warn("Logo não encontrado ou erro ao adicionar: {}", e.getMessage());
      System.out.println("Erro ao adicionar logo: " + e.getMessage());
    }
  }
}