package com.hortifruti.sl.hortifruti.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hortifruti.sl.hortifruti.service.notification.ChatbotService;

import java.util.Map;

/**
 * Controller responsável pelos endpoints do chatbot WhatsApp.
 * 
 * Gerencia a comunicação entre a API UltraMsg e o sistema interno,
 * recebendo mensagens através de webhooks e fornecendo endpoints
 * de teste e debug para desenvolvimento.
 */
@RestController 
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatbot WhatsApp", description = "Endpoints para o chatbot do WhatsApp via UltraMsg API")
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Webhook principal para receber mensagens do WhatsApp via UltraMsg.
     * 
     * Este endpoint é chamado automaticamente pela UltraMsg quando uma
     * nova mensagem é enviada por um cliente no WhatsApp.
     * 
     * @param payload Dados da mensagem recebida contendo informações como
     *                remetente, conteúdo, tipo e timestamp
     * @return ResponseEntity com status da operação
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "Webhook para receber mensagens do WhatsApp",
        description = "Endpoint que recebe mensagens enviadas pelos clientes via WhatsApp através da UltraMsg API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mensagem processada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<String> receiveWhatsAppMessage(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Payload da mensagem recebida via UltraMsg",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Exemplo de mensagem",
                    value = """
                    {
                      "from": "5531999999999",
                      "body": "Quero receber meus boletos",
                      "type": "chat",
                      "timestamp": "1645123456"
                    }
                    """
                )
            )
        )
        @RequestBody Map<String, Object> payload) {
        try {
            chatbotService.processIncomingMessage(payload);
            return ResponseEntity.ok("Message processed successfully");
        } catch (Exception e) {
            log.error("Erro ao processar webhook do WhatsApp: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error processing message");
        }
    }

    /**
     * Endpoint de verificação do webhook para configuração inicial.
     * 
     * Utilizado para validar e confirmar a configuração do webhook
     * na plataforma UltraMsg.
     * 
     * @param hub_challenge Token de desafio enviado pela plataforma
     * @return ResponseEntity com o token de verificação ou status ativo
     */
    @GetMapping("/webhook")
    @Operation(
        summary = "Verificar webhook",
        description = "Endpoint para verificação e validação do webhook"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook verificado com sucesso")
    })
    public ResponseEntity<String> verifyWebhook(
        @Parameter(description = "Challenge token para verificação", required = false)
        @RequestParam(required = false) String hub_challenge) {
        if (hub_challenge != null) {
            return ResponseEntity.ok(hub_challenge);
        }
        return ResponseEntity.ok("Webhook is active");
    }

    /**
     * Endpoint para teste manual do chatbot em ambiente de desenvolvimento.
     * 
     * Permite simular o envio de mensagens sem depender da integração
     * com o WhatsApp, facilitando testes e debug.
     * 
     * @param phoneNumber Número do WhatsApp do cliente (formato: DDD+número)
     * @param message Conteúdo da mensagem a ser testada
     * @return ResponseEntity com resultado do processamento
     */
    @PostMapping("/test")
    @Operation(
        summary = "Testar chatbot manualmente",
        description = "Endpoint para testar o funcionamento do chatbot simulando uma mensagem recebida"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teste executado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro durante o teste")
    })
    public ResponseEntity<String> testChatbot(
        @Parameter(description = "Número do WhatsApp do cliente (ex: 31999999999)", required = true, example = "31999999999")
        @RequestParam String phoneNumber, 
        @Parameter(description = "Mensagem a ser testada", required = true, example = "quero meus boletos")
        @RequestParam String message) {
        try {
            Map<String, Object> testPayload = Map.of(
                "from", phoneNumber,
                "body", message,
                "type", "chat"
            );
            
            chatbotService.processIncomingMessage(testPayload);
            
            return ResponseEntity.ok("Test message processed successfully");
        } catch (Exception e) {
            log.error("Erro ao testar chatbot: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error in test: " + e.getMessage());
        }
    }

    /**
     * Endpoint para teste do chatbot usando payload JSON completo.
     * 
     * Permite testar o chatbot com um payload JSON estruturado
     * similar ao que seria enviado pela UltraMsg, possibilitando
     * testes mais realistas e completos.
     * 
     * @param testPayload JSON contendo dados simulados da mensagem
     * @return ResponseEntity com resultado do processamento
     */
    @PostMapping("/test-json")
    @Operation(
        summary = "Testar chatbot com JSON",
        description = "Endpoint para testar o chatbot com um payload JSON similar ao que seria enviado pela UltraMsg"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teste executado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro durante o teste")
    })
    public ResponseEntity<String> testChatbotWithJson(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Payload de teste simulando mensagem da UltraMsg",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Solicitar boletos",
                        value = """
                        {
                          "from": "5531999999999",
                          "body": "Quero receber meus boletos",
                          "type": "chat",
                          "timestamp": "1645123456"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Saudação",
                        value = """
                        {
                          "from": "5531999999999",
                          "body": "Oi, bom dia!",
                          "type": "chat",
                          "timestamp": "1645123456"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Solicitar ajuda",
                        value = """
                        {
                          "from": "5531999999999",
                          "body": "Preciso de ajuda",
                          "type": "chat",
                          "timestamp": "1645123456"
                        }
                        """
                    )
                }
            )
        )
        @RequestBody Map<String, Object> testPayload) {
        try {
            chatbotService.processIncomingMessage(testPayload);
            return ResponseEntity.ok("Test JSON processed successfully");
        } catch (Exception e) {
            log.error("Erro ao testar chatbot com JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error in JSON test: " + e.getMessage());
        }
    }
}