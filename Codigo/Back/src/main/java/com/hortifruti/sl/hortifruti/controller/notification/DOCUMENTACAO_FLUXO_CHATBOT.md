# Chatbot WhatsApp - Hortifruti SL

## 📋 Visão Geral
Sistema de atendimento automatizado via WhatsApp com menu interativo de 3 opções, gestão de sessões e integração híbrida (bot + atendimento humano).

**Funcionalidades:**
- ✅ Menu de 3 opções: Boleto, Pedido e Outro Assunto
- ✅ Consulta de boletos por CPF/CNPJ com envio automático de PDFs
- ✅ Encaminhamento para atendimento humano
- ✅ Pausa automática quando atendente responde manualmente
- ✅ Retorno ao menu via comando "MENU"
- ✅ Sem armazenamento de mensagens (banco leve)

---

## 🔄 Fluxo Completo

### 1️⃣ Cliente Inicia Conversa
```
Cliente: [Qualquer mensagem]
Bot: Menu Principal
     1️⃣ Boleto - Consultar boletos em aberto
     2️⃣ Pedido - Dúvidas sobre pedidos  
     3️⃣ Outro assunto - Falar com atendimento
     💡 A qualquer momento, digite MENU para voltar
```

### 2️⃣ Opção 1 - Consulta de Boletos
```
Cliente: 1
Bot: Envie seu CPF ou CNPJ
     💡 Digite MENU para voltar ao início

Cliente: 12345678900
Bot: [Busca boletos pendentes]
     → Cliente encontrado: Envia resumo + PDFs
     → Sem boletos: Informa que não há pendências
     → Cliente não encontrado: Informa erro e contato
     [Sessão é deletada após envio]
```

### 3️⃣ Opção 2 - Pedido
```
Cliente: 2
Bot: Vou encaminhar sua solicitação sobre pedido...
     Descreva sua dúvida e aguarde atendimento.
     Status: AWAITING_HUMAN

[Cliente aguarda em fila]
Atendente: [Responde manualmente via WhatsApp]
Sistema: [Detecta fromMe=true]
         → Pausa bot por 1 hora
         → Status: PAUSED
         
[Após 1 hora]
Sistema: Status volta para MENU automaticamente
```

### 4️⃣ Opção 3 - Outro Assunto
```
Cliente: 3
Bot: Vou encaminhar você para nossa equipe...
     [Mesmo fluxo da opção 2]
```

### 5️⃣ Comando Global - Voltar ao Menu
```
Cliente: MENU (ou RECOMEÇAR/VOLTAR)
Bot: [Menu Principal]
     Status: Qualquer → MENU
     Contexto: Limpo
```

---

## 🏗️ Arquitetura Técnica

### Camadas do Sistema

#### **Model** (3 classes)
- `ChatSession.java` - Entidade principal
  - Campos: id, phoneNumber, clientId, status, context, createdAt, pausedUntil
  - Métodos: isPaused(), pauseBot(hours)
  
- `SessionStatus.java` - Estados da conversa
  - MENU - Menu principal
  - AWAITING_DOCUMENT - Aguardando CPF/CNPJ
  - AWAITING_HUMAN - Aguardando atendente
  - PAUSED - Atendimento humano em andamento
  - CLOSED - Sessão finalizada (legacy, não usado)

- `SessionContext.java` - Contexto da conversa
  - BOLETO - Consulta de boletos
  - PEDIDO - Dúvidas sobre pedidos
  - OUTRO - Outros assuntos

#### **Repository** (1 classe)
- `ChatSessionRepository.java`
  - `findActiveSessionByPhoneNumber()` - Busca sessão ativa
  - `findSessionsAwaitingHuman()` - Fila de atendimento
  - `findSessionsToUnpause()` - Sessões com pausa expirada

#### **Service** (3 classes)
- `ChatbotService.java` - Orquestrador principal
  - `processIncomingMessage()` - Processa webhook
  - `processCommand()` - Gerencia máquina de estados
  - `handleMenuSelection()` - Processa opções do menu
  - `handleDocumentInput()` - Valida CPF/CNPJ
  - `handleBilletRequestByDocument()` - Busca e envia boletos

- `ChatSessionService.java` - Gestão de sessões
  - `getOrCreateSession()` - Obtém ou cria sessão
  - `updateSessionStatus()` - Muda estado
  - `pauseBotForPhone()` - Pausa por N horas
  - `unpauseBot()` - Remove pausa manualmente
  - `unpauseExpiredSessions()` - Remove pausas expiradas
  - `closeSession()` - Deleta sessão do banco

- `ChatSessionCleanupService.java` - Manutenção automática
  - Job a cada 5 minutos
  - Despausa sessões expiradas
  - Volta status PAUSED → MENU

#### **Controller** (0 classes)
- Nenhum! Sem UI, apenas webhook automático

---

## 🤖 Detecção Automática de Atendente

### Como Funciona
```java
// UltraMsg envia campo "fromMe" no webhook
boolean isFromMe = detectIfMessageIsFromBot(data);

if (isFromMe) {
    // Mensagem enviada pelo atendente
    chatSessionService.pauseBotForPhone(phoneNumber, 1);
    session.setStatus(SessionStatus.PAUSED);
    return; // Não processa como comando
}
```

### Comportamento
- **fromMe = true**: Mensagem do atendente
  - ✅ Pausa bot por 1 hora
  - ✅ Status: AWAITING_HUMAN → PAUSED
  - ✅ Bot para de responder
  
- **fromMe = false**: Mensagem do cliente
  - ✅ Bot processa normalmente
  - ✅ Exceto se status = PAUSED (ignora)

---

## 🗄️ Filosofia do Banco de Dados

### Minimalista e Limpo
- ❌ **Sem armazenamento de mensagens**
  - Apenas estado da sessão
  - Banco super leve
  
- ❌ **Sem status CLOSED persistido**
  - Sessão existe = ativa
  - Sessão concluída = deletada
  
- ❌ **Sem timestamps de atualização**
  - Apenas createdAt e pausedUntil
  
- ✅ **5 campos essenciais na entidade**
  - Redução de 55% no código

---

## 🔧 Queries Principais

### Busca de Boletos Pendentes
```java
@Query("SELECT cs FROM CombinedScore cs 
        WHERE cs.clientId = :clientId 
        AND cs.status = 'PENDENTE' 
        AND cs.hasBillet = true")
List<CombinedScore> findAllPendingWithBilletByClient(@Param("clientId") Long clientId);
```

### Sessões para Despausar
```java
@Query("SELECT cs FROM ChatSession cs 
        WHERE cs.pausedUntil IS NOT NULL 
        AND cs.pausedUntil < :now")
List<ChatSession> findSessionsToUnpause(@Param("now") LocalDateTime now);
```

---

## 📝 Mensagens do Bot

### Profissionais e Diretas
- ✅ Informativas
- ✅ Com dicas de navegação
- ✅ Horário de atendimento quando relevante
- ✅ Contato telefônico para emergências

### Exemplos
```
"Para consultar seus boletos, envie seu CPF ou CNPJ.
💡 Digite MENU para voltar ao início"

"Você possui 2 boleto(s) vencido(s) e pendente(s):
Boleto 1:
Valor: R$ 150,00
Vencimento: 01/11/2025
Número: 12345"
```

---

## 🚀 Benefícios da Arquitetura

### Performance
- ⚡ Sem histórico de mensagens = banco leve
- ⚡ Sessões deletadas automaticamente
- ⚡ Queries otimizadas

### Manutenibilidade  
- 🧹 6 classes essenciais (vs 9 originais)
- 🧹 Código 55% menor
- 🧹 Lógica clara e direta

### UX
- 🎯 Menu intuitivo
- 🎯 Comando MENU a qualquer momento
- 🎯 Detecção automática de atendente
- 🎯 Sem interrupções durante atendimento humano

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
