# Fluxo do Chatbot de Boletos - Hortifruti SL

## Visão Geral
Este chatbot permite que clientes consultem e recebam boletos em aberto (pendentes) via WhatsApp, apenas enviando seu CPF ou CNPJ. O sistema integra backend Java Spring Boot, UltraMsg (API WhatsApp) e banco de dados relacional.

---

## Fluxo Resumido
1. **Cliente envia mensagem no WhatsApp**
   - Pode ser uma saudação, "boletos", ou diretamente o CPF/CNPJ.
2. **Webhook recebe a mensagem**
   - Endpoint público recebe o payload do UltraMsg.
3. **ChatbotService processa a mensagem**
   - Ignora grupos, só responde mensagens privadas.
   - Se for comando de boletos, pede o documento.
   - Se for CPF/CNPJ, busca o cliente e seus boletos pendentes.
4. **Busca de boletos pendentes**
   - Usa o método `findAllPendingWithBilletByClient` do `BilletService`.
   - Retorna todos os `CombinedScore` do cliente com `status = 'PENDENTE'` e `hasBillet = true`.
5. **Resposta ao cliente**
   - Se não houver boletos: mensagem informando que não há pendências.
   - Se houver boletos: envia resumo dos boletos e os PDFs via WhatsApp.

---

## Detalhes Técnicos

### 1. Estrutura dos principais arquivos
- `ChatbotService.java`: Orquestra o fluxo do chatbot.
- `BilletService.java`: Lógica de boletos, consulta e geração de PDFs.
- `CombinedScoreRepository.java`: Query customizada para buscar boletos pendentes.

### 2. Consulta de boletos pendentes
- Query:
  ```java
  @Query("SELECT cs FROM CombinedScore cs WHERE cs.clientId = :clientId AND cs.status = 'PENDENTE' AND cs.hasBillet = true")
  List<CombinedScore> findAllPendingWithBilletByClient(@Param("clientId") Long clientId);
  ```
- Usada pelo serviço para garantir que só boletos realmente em aberto e com PDF sejam enviados.

### 3. Geração e envio de PDF
- Para cada boleto pendente, o sistema gera o PDF e envia via UltraMsg.
- Se não houver PDF, loga o erro e não envia arquivo vazio.

### 4. Mensagens e comandos
- Comandos reconhecidos: "boletos", "ajuda", saudações, CPF/CNPJ.
- Mensagens são profissionais, sem emojis.
- Respostas de erro e ajuda são amigáveis e claras.

---

## Como testar
1. Gere um boleto para um cliente (garanta que `hasBillet = true` e `status = 'PENDENTE'`).
2. Envie o CPF/CNPJ do cliente para o WhatsApp do bot.
3. O bot deve responder com o resumo e os PDFs dos boletos em aberto.

---

## Observações
- O filtro considera apenas boletos realmente pendentes e com PDF disponível.
- O fluxo é extensível para outros comandos e integrações.

---

**Dúvidas ou problemas?**
Entre em contato com o time de desenvolvimento ou consulte os logs do backend para detalhes do fluxo.
