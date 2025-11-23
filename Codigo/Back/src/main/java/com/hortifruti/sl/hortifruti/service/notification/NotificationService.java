package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationCoordinator notificationCoordinator;
    private final ClientRepository clientRepository;
    private final EmailTemplateService emailTemplateService;

    @Value("${accounting.email}")
    private String accountingEmail;

    /** Envio para contabilidade */
    public NotificationResponse sendGenericFilesToAccounting(
            List<MultipartFile> files, GenericFilesAccountingRequest request) {

        try {
            List<byte[]> fileContents = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    fileContents.add(file.getBytes());
                    fileNames.add(file.getOriginalFilename());
                }
            }

            BigDecimal cardValue = request.cardValue() != null ? request.cardValue() : BigDecimal.ZERO;
            BigDecimal cashValue = request.cashValue() != null ? request.cashValue() : BigDecimal.ZERO;
            BigDecimal discountedCardValue = cardValue.multiply(BigDecimal.valueOf(0.4));

            String subject = "Arquivos Contábeis - Resumo Financeiro";

            String emailBody = buildGenericFilesMessage(
                    request,
                    discountedCardValue,
                    cashValue,
                    !fileContents.isEmpty(),
                    fileContents.size()
            );

            boolean emailSent = notificationCoordinator.sendEmailOnly(
                    accountingEmail,
                    subject,
                    emailBody,
                    fileContents,
                    fileNames
            );

            return new NotificationResponse(
                    emailSent,
                    emailSent ? "Email enviado com sucesso" : "Falha no envio do email"
            );

        } catch (IOException e) {
            throw new NotificationException("Erro ao processar arquivos: " + e.getMessage());
        }
    }

    /** Envio de documentos para cliente (WhatsApp + Email) */
    public NotificationResponse sendDocumentsToClient(
            List<MultipartFile> files, ClientDocumentsRequest request) {

        try {
            Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new NotificationException("Cliente não encontrado"));

            List<byte[]> attachments = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    attachments.add(file.getBytes());
                    fileNames.add(file.getOriginalFilename());
                }
            }

            String subject = "Documentos - " + client.getClientName();
            String emailBody = buildClientMessage(request, client);

            var whatsappContext = NotificationCoordinator.WhatsAppMessageContext.builder()
                    .client(client)
                    .customMessage(request.customMessage());

            return notificationCoordinator.sendNotification(
                    client.getEmail(),
                    client.getPhoneNumber(),
                    request.channel(),
                    subject,
                    emailBody,
                    NotificationCoordinator.WhatsAppMessageType.CLIENT_DOCUMENTS,
                    whatsappContext,
                    attachments,
                    fileNames
            );

        } catch (IOException e) {
            throw new NotificationException("Erro ao processar arquivos: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Templates
    // -------------------------------------------------------------

    private String buildGenericFilesMessage(
            GenericFilesAccountingRequest request,
            BigDecimal discountedCardValue,
            BigDecimal cashValue,
            boolean hasFiles,
            int filesCount) {

        Map<String, String> variables = new HashMap<>();
        variables.put("CARD_VALUE", String.format("%.2f", discountedCardValue));
        variables.put("CASH_VALUE", String.format("%.2f", cashValue));

        boolean hasFinancialValues =
                discountedCardValue.compareTo(BigDecimal.ZERO) != 0 ||
                cashValue.compareTo(BigDecimal.ZERO) != 0;

        variables.put("HAS_FINANCIAL_VALUES", hasFinancialValues ? "true" : "");
        variables.put("NO_FINANCIAL_VALUES", hasFinancialValues ? "" : "true");

        variables.put("HAS_FILES", hasFiles ? "true" : "");
        variables.put("NO_FILES", hasFiles ? "" : "true");
        variables.put("FILES_COUNT", String.valueOf(filesCount));

        if (request.customMessage() != null && !request.customMessage().isEmpty()) {
            variables.put("CUSTOM_MESSAGE", request.customMessage());
            variables.put("DEFAULT_MESSAGE", "");
        } else {
            variables.put("CUSTOM_MESSAGE", "");
            variables.put("DEFAULT_MESSAGE", "true");
        }

        return emailTemplateService.processTemplate("generic-files", variables);
    }

    private String buildClientMessage(ClientDocumentsRequest request, Client client) {

        Map<String, String> variables = new HashMap<>();
        variables.put("CLIENT_NAME", client.getClientName());

        LocalDate today = LocalDate.now();
        variables.put("CURRENT_DATE", today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        if (request.customMessage() != null && !request.customMessage().isEmpty()) {
            variables.put("CUSTOM_MESSAGE", request.customMessage());
            variables.put("DEFAULT_MESSAGE", "");
        } else {
            variables.put("CUSTOM_MESSAGE", "");
            variables.put("DEFAULT_MESSAGE", "true");
        }

        return emailTemplateService.processTemplate("client-documents", variables);
    }
}
