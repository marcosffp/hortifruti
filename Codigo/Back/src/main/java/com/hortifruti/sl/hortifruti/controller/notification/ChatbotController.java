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

@RestController 
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatbot WhatsApp", description = "Endpoints para o chatbot do WhatsApp via UltraMsg API")
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Webhook para receber mensagens do WhatsApp via UltraMsg
     * Endpoint que a UltraMsg chamará quando uma nova mensagem chegar
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
            // ⚠️ LOGS TEMPORÁRIOS PARA DEBUG - REMOVER DEPOIS ⚠️
            System.out.println("🔥 WEBHOOK CHAMADO! Payload completo: " + payload);
            System.out.println("🔥 Headers recebidos - verificar User-Agent, etc.");
            
            log.info("Webhook recebido: {}", payload);
            
            // Processar a mensagem recebida
            chatbotService.processIncomingMessage(payload);
            
            return ResponseEntity.ok("Message processed successfully");
            
        } catch (Exception e) {
            log.error("Erro ao processar webhook do WhatsApp: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error processing message");
        }
    }

    /**
     * Endpoint de verificação do webhook (se necessário para configuração)
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
     * Endpoint para testar o chatbot manualmente (desenvolvimento)
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
            log.info("Teste do chatbot - Telefone: {}, Mensagem: {}", phoneNumber, message);
            
            // Simular mensagem recebida para teste
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
     * Endpoint para testar o chatbot com payload JSON completo
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
            log.info("Teste JSON do chatbot - Payload: {}", testPayload);
            
            chatbotService.processIncomingMessage(testPayload);
            
            return ResponseEntity.ok("Test JSON processed successfully");
            
        } catch (Exception e) {
            log.error("Erro ao testar chatbot com JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error in JSON test: " + e.getMessage());
        }
    }

    /**
     * Endpoint de debug para verificar conectividade e configuração
     */
    @GetMapping("/debug")
    @Operation(
        summary = "Debug do chatbot",
        description = "Endpoint para verificar se o chatbot está funcionando e acessível"
    )
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("status", "🟢 ATIVO");
        info.put("timestamp", System.currentTimeMillis());
        info.put("webhookUrl", "/chatbot/webhook");
        info.put("testUrl", "/chatbot/test");
        info.put("swaggerUrl", "/swagger-ui/index.html");
        info.put("message", "Chatbot está funcionando! ✅");
        
        log.info("🔍 Debug acessado: {}", info);
        System.out.println("🔍 DEBUG CHAMADO: " + info);
        
        return ResponseEntity.ok(info);
    }
}