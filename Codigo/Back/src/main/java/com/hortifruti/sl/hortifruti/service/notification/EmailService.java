package com.hortifruti.sl.hortifruti.service.notification;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    public boolean sendSimpleEmail(String to, String subject, String text) {
        try {

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", text);
            Mail mail = new Mail(from, subject, toEmail, content);

            addInlineLogo(mail);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return true;
            } else {
                throw new NotificationException(
                        "Erro ao enviar email. Status: " + response.getStatusCode()
                );
            }

        } catch (IOException e) {
            throw new NotificationException("Falha ao enviar email: " + e.getMessage());
        }
    }

    public boolean sendEmailWithAttachments(
            String to, String subject, String text, List<byte[]> attachments, List<String> fileNames
    ) {
        try {

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", text);
            Mail mail = new Mail(from, subject, toEmail, content);

            addInlineLogo(mail);

            if (attachments != null && fileNames != null) {
                for (int i = 0; i < attachments.size() && i < fileNames.size(); i++) {
                    Attachments attachment = new Attachments();
                    String encodedFile = Base64.getEncoder().encodeToString(attachments.get(i));
                    attachment.setContent(encodedFile);
                    attachment.setFilename(fileNames.get(i));
                    attachment.setDisposition("attachment");
                    mail.addAttachments(attachment);
                }
            }

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return true;
            } else {
                throw new NotificationException(
                        "Erro ao enviar email com anexos. Status: " + response.getStatusCode()
                );
            }

        } catch (IOException e) {
            throw new NotificationException("Falha ao enviar email com anexos: " + e.getMessage());
        }
    }

    public boolean sendEmailWithSingleAttachment(
            String to, String subject, String text, byte[] attachment, String fileName
    ) {
        return sendEmailWithAttachments(to, subject, text, List.of(attachment), List.of(fileName));
    }

    private void addInlineLogo(Mail mail) {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");

            if (logoResource.exists()) {
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                String encodedLogo = Base64.getEncoder().encodeToString(logoBytes);

                Attachments logo = new Attachments();
                logo.setContent(encodedLogo);
                logo.setType("image/png");
                logo.setFilename("logo.png");
                logo.setDisposition("inline");
                logo.setContentId("logo");

                mail.addAttachments(logo);
            }

        } catch (IOException ignored) {
            // Sem log, sem erro → apenas não adiciona logo
        }
    }
}
