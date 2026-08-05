# Spec — Captura de Nota via Dispositivo Vinculado + Fila em Tempo Real

## Como usar este documento

Esta spec é uma **camada adicional** sobre `spec-captura-nota-camera-gemini-completa.md` (câmera → upload → extração Gemini → matching → consistência → revisão num único dispositivo/sessão). Ela resolve um problema que a spec original não cobre: o usuário tira a foto no **celular** e quer ver a nota chegando e sendo extraída **no PC**, sem repetir um pareamento a cada foto e com atualização o mais próxima possível de instantânea.

Cada etapa é independente, deve ser implementada/testada/validada antes da próxima, e assume que as Etapas 0-4 da spec original (upload validado, extração Gemini, matching de produto, checagem de consistência) já existem e serão **reaproveitadas**, não duplicadas. Esta spec não reimplementa extração — ela reimplementa **quem chama a extração, onde a foto fica guardada até isso terminar, e como o PC fica sabendo**.

Instrução geral pro Claude Code: implementar uma etapa por vez, parar ao final de cada uma e aguardar validação antes de seguir pra próxima.

---

## Contexto do problema

Hoje (spec original) o fluxo é: mesmo navegador, mesma sessão, tira foto → sobe → extrai → revisa. Isso é ótimo pra quem só usa o celular sozinho, mas não serve pro caso de uso real da loja: o funcionário fotografa várias notas ao longo do dia no celular, e quem revisa/confirma o pedido está sentado no PC. Duas exigências novas, na ordem em que foram levantadas:

1. **Não pode pedir pareamento (QR/login) a cada foto.** O vínculo celular↔usuário precisa ser feito uma vez só e persistir.
2. **O PC precisa saber "na hora"**, não só ao recarregar a página — cada nota tirada no celular deve aparecer numa fila no PC assim que a extração terminar, sem ação manual de "atualizar".

### Decisões de arquitetura e por quê

| Decisão | Alternativa descartada | Motivo |
|---|---|---|
| Vínculo de dispositivo com token próprio, opaco e de escopo restrito (só endpoints de captura de nota) | Reusar o cookie de login (`auth_token`/`refresh_token`) no celular | O cookie de login é `HttpOnly` e pensado pra uma sessão de navegador completa (acesso a todo o sistema). Um celular “dedicado a fotografar nota” não deveria carregar um JWT com acesso total a boletos/NF-e/financeiro se for perdido ou roubado — precisa de um credencial de menor privilégio, revogável independente do login normal. Ver Etapa 1. |
| Pareamento via QR **escaneado uma vez** (usa a câmera nativa do SO, não uma leitura de QR dentro da página) | QR reaberto/escaneado a cada captura | Resolve a exigência nº 1 diretamente — o token de dispositivo fica salvo no `localStorage` do celular depois do primeiro pareamento e não expira a cada uso. |
| Notificação PC via **Server-Sent Events (SSE)**, carregando só um "algo mudou, refaça o GET" | WebSocket | SSE é unidirecional (servidor→PC), que é exatamente o que precisamos (o PC nunca precisa mandar nada de volta pelo canal). Spring já tem `SseEmitter` nativo, sem precisar de broker (STOMP/RabbitMQ). WebSocket resolveria também, mas é infraestrutura a mais pra um requisito que só anda numa direção. |
| SSE carrega só um evento "tipo + id", o payload real vem de um `GET` REST logo em seguida | SSE carregando o JSON completo da extração | Isso faz o **reconexão** (que vai acontecer — ver Etapa 5 sobre o proxy do Next.js matando conexão longa) ser inofensiva: se o PC perder um evento entre reconexões, o próximo `GET /pendentes` já traz o estado real, sem precisar de lógica de "replay de eventos perdidos". |
| Fotos ficam no **Cloudflare R2** (reaproveitando `R2StorageService`), não em `temp/notas/` local | Manter em disco local como o `NotaUploadService` faz hoje | O container do Railway roda com filesystem efêmero — nada garante que `/app/temp` sobrevive a um redeploy/restart, e se a app algum dia escalar pra mais de uma instância, cada uma tem seu próprio disco. R2 é o padrão que o resto do projeto já usa pra esse exato problema (boletos, XMLs, extratos). |

---

## Etapa 1 — Modelo de dados: dispositivo vinculado

### Objetivo
Ter a entidade que representa "este celular está autorizado a mandar fotos em nome deste usuário", com o mesmo rigor de segurança que já existe pra refresh token.

### O que implementar
- Entidade `DispositivoVinculado` em `model/` (raiz do pacote, ao lado de `User.java` e `RefreshToken.java` — é um conceito de autenticação, não de domínio de compras):
  ```java
  @Entity
  @Table(name = "dispositivos_vinculados")
  class DispositivoVinculado {
    Long id;
    @ManyToOne User usuario;
    String tokenHash;      // SHA-256 do token opaco, nunca o token em claro (mesmo padrão de RefreshToken)
    String nomeDispositivo; // ex.: "iPhone do João" — definido pelo usuário no momento do pareamento
    LocalDateTime pareadoEm;
    LocalDateTime ultimoUsoEm;
    LocalDateTime revogadoEm; // null = ativo
  }
  ```
- `DispositivoVinculadoRepository` com `findByTokenHashAndRevogadoEmIsNull(String hash)`.
- `DispositivoVinculadoService`:
  - `gerarCodigoPareamento(Long usuarioId)`: gera um código curto (6 dígitos, tipo os de 2FA) + um `pairingId` (UUID), guarda em memória (`ConcurrentHashMap`, TTL 5 minutos, um scheduled cleanup como o `RefreshTokenCleanupService` já faz) associado ao `usuarioId` de quem pediu — **este é gerado pelo PC, autenticado normalmente**.
  - `confirmarPareamento(String codigo, String nomeDispositivo)`: valida o código (existe, não expirou, não foi usado), gera um token opaco de 32 bytes (`SecureRandom`, mesmo padrão de `RefreshTokenService`), salva o hash SHA-256 em `DispositivoVinculado`, invalida o código (uso único), retorna o token **em claro apenas nesta resposta** (nunca mais é recuperável, só revogável).
  - `validarToken(String tokenClaro)`: hash e busca; se achar e não revogado, atualiza `ultimoUsoEm` e retorna o `usuarioId`.
  - `listarDispositivos(Long usuarioId)` / `revogar(Long dispositivoId, Long usuarioIdSolicitante)` (checar que o dispositivo pertence ao usuário que está pedindo a revogação).

### Critério de aceite
- Gerar código de pareamento duas vezes seguidas pro mesmo usuário invalida o anterior (não dá pra ter dois códigos válidos simultâneos por usuário — evita confusão de qual QR é o vigente).
- Confirmar pareamento com código expirado ou já usado falha com erro claro.
- Token de dispositivo nunca é armazenado em claro no banco (só o hash, como já é feito pra refresh token).
- Revogar um dispositivo faz qualquer chamada subsequente com aquele token falhar imediatamente.

### Como testar (temporário)
- Teste unitário: gerar código, confirmar, validar token retorna o `usuarioId` certo.
- Teste unitário: confirmar duas vezes o mesmo código → segunda falha.
- Teste unitário: revogar e depois validar → falha.

### Segurança
- Código de pareamento de 6 dígitos com TTL de 5 minutos e uso único já reduz bastante o risco de força bruta, mas o endpoint de confirmação (Etapa 2) precisa do rate limiting da Etapa 8 abaixo antes de ir pra produção — não é aceitável isolado.
- `SecureRandom`, não `Random`, pro token opaco (mesmo cuidado do `RefreshTokenService`).

---

## Etapa 2 — Endpoints de pareamento

### Objetivo
Expor a Etapa 1 via API: o PC gera o código/QR, o celular confirma.

### O que implementar
```
POST /api/dispositivos/pareamento/iniciar   (autenticado normalmente — cookie do PC)
Response 200: { "codigo": "384921", "pairingId": "uuid", "expiraEm": "2026-08-05T14:30:00Z" }

POST /api/dispositivos/pareamento/confirmar  (SEM autenticação normal — é o celular "de fora")
Body: { "codigo": "384921", "nomeDispositivo": "iPhone do João" }
Response 200: { "deviceToken": "abc123...", "dispositivoId": 7 }

GET /api/dispositivos          (autenticado normalmente — lista do usuário logado)
Response 200: [ { "id": 7, "nome": "iPhone do João", "pareadoEm": ..., "ultimoUsoEm": ... }, ... ]

DELETE /api/dispositivos/{id}  (autenticado normalmente — revoga)
```
- `/pareamento/confirmar` é público no `SecurityConfig` (lista ao lado de `/auth`, `/auth/refresh` etc.) porque o celular ainda não tem nenhum credencial nesse momento — a segurança dele é o código de 6 dígitos de vida curta, não um token.
- A URL que vai dentro do QR (Etapa 3) aponta pra uma página do frontend que já chama esse endpoint com o código pré-preenchido.

### Critério de aceite
- Fluxo completo funciona: PC pede código, celular confirma com o código certo, celular recebe um `deviceToken` válido.
- Código errado no `/confirmar` retorna 400 com mensagem clara, sem revelar se o código existe/expirou (mesma cautela de anti-enumeração que já existe no login).
- `GET /api/dispositivos` só lista dispositivos do usuário autenticado (nunca de outro usuário).
- `DELETE /api/dispositivos/{id}` de um dispositivo que não é do usuário logado retorna 403/404 (não vaza existência).

### Como testar (temporário)
```bash
# PC gera código (usar o cookie de sessão já autenticado)
curl -X POST http://localhost:8080/api/dispositivos/pareamento/iniciar -b cookies.txt

# celular confirma (sem cookie nenhum)
curl -X POST http://localhost:8080/api/dispositivos/pareamento/confirmar \
  -H "Content-Type: application/json" \
  -d '{"codigo":"384921","nomeDispositivo":"Teste"}'

# listar dispositivos
curl http://localhost:8080/api/dispositivos -b cookies.txt
```

### Segurança
- Rate limit específico (mais restritivo que o padrão de 10/min) em `/pareamento/confirmar` via Bucket4j (mesmo `RateLimitingFilter`, nova rota na config) — é o único endpoint público novo desta feature, então é o alvo óbvio de força bruta.
- `@PreAuthorize` normal (`MANAGER`/`EMPLOYEE`) em `/iniciar`, `/dispositivos` (GET) e `/dispositivos/{id}` (DELETE) — só `/confirmar` é público.

---

## Etapa 3 — Autenticação por token de dispositivo (escopo restrito)

### Objetivo
Fazer o backend aceitar requisições autenticadas com o `deviceToken` do celular, mas **só** nos endpoints de captura de nota — nunca em qualquer outra rota do sistema.

### O que implementar
- `DeviceTokenAuthFilter` (novo `OncePerRequestFilter`, ao lado de `SecurityFilter` em `config/auth/`): lê o header `Authorization: Bearer <deviceToken>` (mesmo header do JWT normal, mas o valor é opaco, não um JWT — o filtro tenta primeiro como JWT normal; se falhar o parse, tenta como device token antes de rejeitar). Se válido, popula o `SecurityContext` com uma authority própria, ex. `ROLE_DEVICE_CAPTURE`, associada ao `usuarioId` resolvido.
- No `SecurityConfig`, os endpoints de captura (Etapa 4) exigem `hasAnyRole('MANAGER', 'EMPLOYEE', 'DEVICE_CAPTURE')`, mas **todos os outros endpoints do sistema continuam exigindo só `MANAGER`/`EMPLOYEE`** — ou seja, `ROLE_DEVICE_CAPTURE` sozinho não abre porta nenhuma além da captura. Isso é o núcleo da restrição de escopo: vale a pena um teste de integração dedicado provando que um `deviceToken` válido recebe 403 em, por exemplo, `GET /products`.

### Critério de aceite
- Requisição com `deviceToken` válido acessa os endpoints de captura normalmente.
- A mesma requisição, com o mesmo `deviceToken`, recebe 403 em qualquer endpoint fora do escopo de captura de nota (testar pelo menos um endpoint de cada módulo sensível: produtos, boletos, financeiro).
- Requisição com `deviceToken` revogado recebe 401.
- Requisição com JWT normal de login continua funcionando em tudo que já funcionava (não pode quebrar o fluxo existente).

### Como testar (temporário)
- Teste de integração: `deviceToken` válido + `POST /api/compras/notas/capturas` → 200/202.
- Teste de integração: mesmo `deviceToken` + `GET /products` → 403.
- Teste de integração: `deviceToken` revogado + qualquer endpoint de captura → 401.

### Segurança
- Esta é a etapa mais sensível de toda a spec — um erro aqui vira escalação de privilégio (um celular perdido virando acesso total ao sistema). Não pular os testes de integração de "fora do escopo".

---

## Etapa 4 — Upload assíncrono com persistência (fila de notas pendentes)

### Objetivo
O celular manda a foto, o backend responde rápido (não fica a requisição esperando os 17-30s da extração do Gemini), guarda a foto de forma durável e processa em background.

### O que implementar
- Entidade `CapturaNotaPendente` em `model/purchase/`:
  ```java
  @Entity
  @Table(name = "capturas_nota_pendentes")
  class CapturaNotaPendente {
    Long id;
    @ManyToOne User usuario;
    @ManyToOne DispositivoVinculado dispositivo;
    String r2Key;
    StatusCaptura status; // RECEBIDA, EXTRAINDO, PRONTA, ERRO, CONFIRMADA, DESCARTADA
    String extracaoJson; // NotaExtracaoResponse serializado, preenchido quando status = PRONTA
    String mensagemErro;  // preenchido quando status = ERRO
    LocalDateTime criadaEm;
    LocalDateTime atualizadaEm;
  }
  ```
- `POST /api/compras/notas/capturas` (`hasAnyRole('DEVICE_CAPTURE')` — reaproveitando também `MANAGER`/`EMPLOYEE` pra permitir teste direto sem celular):
  - Reaproveita a validação de magic bytes/tamanho já existente em `NotaUploadService`.
  - Sobe a foto pro R2 via `R2StorageService.upload(...)` (chave ex. `notas-pendentes/{uuid}.jpg`).
  - Cria `CapturaNotaPendente` com `status = RECEBIDA`.
  - Dispara `@Async` a extração (reaproveitando `GeminiExtractionService` + `ProdutoMatchingService` + `NotaConsistenciaChecker` já existentes, só trocando a origem do arquivo de "multipart da requisição atual" pra "baixar de volta do R2").
  - Responde **202 Accepted** imediatamente: `{ "capturaId": "uuid" }`.
  - Ao terminar a extração (sucesso ou falha), atualiza o registro (`PRONTA` + `extracaoJson`, ou `ERRO` + `mensagemErro`) e notifica a fila SSE (Etapa 5).
- `GET /api/compras/notas/pendentes` (autenticado normal, PC): lista as capturas do usuário logado com `status != CONFIRMADA/DESCARTADA`, ordenadas por `criadaEm` — é o endpoint que o PC chama ao carregar a página e sempre que um evento SSE chegar.
- `POST /api/compras/notas/pendentes/{id}/descartar`: marca `DESCARTADA` (usuário decidiu não usar aquela captura — ex. foto borrada). Não apaga do banco/R2 imediatamente (rastro auditável), mas some da fila.

### Critério de aceite
- Upload pelo celular retorna 202 em menos de 1s (não espera o Gemini).
- Alguns segundos depois, `GET /pendentes` mostra a mesma captura com `status: PRONTA` e o JSON de extração preenchido.
- Falha do Gemini (ex. API key inválida simulada) resulta em `status: ERRO` com mensagem legível, não em captura travada pra sempre em `EXTRAINDO`.
- Descartar uma captura tira ela da listagem de `/pendentes` sem apagar o histórico.

### Como testar (temporário)
- `curl` de upload com `deviceToken` de teste, depois pollar `GET /pendentes` manualmente até ver `PRONTA`.
- Forçar erro (API key inválida) e confirmar que vira `ERRO` e não trava.

### Segurança
- Mesma sanitização de tamanho/tipo/conteúdo já usada no upload síncrono existente — não reinventar validação.
- Rate limit em `/capturas` (Bucket4j) por dispositivo, não só por IP — um dispositivo comprometido não deveria conseguir esgotar a cota do Gemini sozinho (ver Etapa 8).

---

## Etapa 5 — Notificação em tempo real (SSE)

### Objetivo
O PC saber que uma captura mudou de status sem precisar ficar recarregando a página nem dar polling agressivo.

### O que implementar
- `SseEmitterRegistry` (`ConcurrentHashMap<Long usuarioId, List<SseEmitter>>`, com a mesma ressalva documentada de `TokenBlocklist`/`RateLimitingFilter`: **só funciona em instância única** — se o Railway algum dia escalar horizontalmente, isso precisa virar Redis pub/sub ou equivalente. Deixar esse comentário explícito no código, igual já é feito em `TokenBlocklist`).
- `GET /api/compras/notas/stream` (autenticado normal — cookie do PC): abre um `SseEmitter` (timeout generoso, ex. 4 minutos — ver nota de deploy abaixo), registra na lista do `usuarioId`, remove no `onCompletion`/`onTimeout`/`onError`.
- Emite eventos com **payload mínimo**, nunca o JSON completo da extração:
  ```
  event: captura-atualizada
  data: { "capturaId": "uuid", "status": "PRONTA" }
  ```
  O frontend, ao receber qualquer evento (não importa o conteúdo exato), simplesmente rechama `GET /pendentes` — mantém client e servidor sempre consistentes mesmo se um evento se perder.
- Heartbeat: comentário SSE (`: ping\n\n`) a cada ~15s pra manter a conexão viva através do proxy do Next.js e evitar que ela seja percebida como uma conexão morta antes da hora.
- Quando o backend termina a extração (Etapa 4), chama `sseEmitterRegistry.notificar(usuarioId, capturaId, status)` — se não houver nenhum emitter aberto pro usuário (PC fechado/desconectado), não faz nada de especial: quando o PC reconectar e/ou carregar a tela, o `GET /pendentes` já traz o estado atual normalmente.

### Critério de aceite
- Abrir a stream, disparar uma captura em outra aba/dispositivo, ver o evento chegar na aba com a stream aberta em menos de 1-2s.
- Fechar a aba com a stream aberta não deixa o emitter "vazando" no servidor (tem que ser removido do registry).
- Derrubar a conexão (matar o processo do backend localmente, ou simular timeout) e reabrir a página faz o `EventSource` reconectar sozinho (comportamento nativo do browser) e a fila continuar funcionando via `GET /pendentes`.

### Como testar (temporário)
```bash
curl -N http://localhost:8080/api/compras/notas/stream -b cookies.txt
# em outro terminal, disparar uma captura e ver o evento aparecer no curl acima
```
- Testar no Chrome DevTools (aba Network → EventStream) pra visualizar os eventos chegando.

### Segurança
- Endpoint autenticado normalmente (cookie), nada de novo aqui em termos de superfície de ataque — mas vale confirmar que um usuário só recebe eventos das próprias capturas (`usuarioId` do emitter tem que bater com o `usuarioId` da captura notificada).

---

## Etapa 6 — Deploy: o proxy do Next.js e conexões longas

### Objetivo
Garantir que a stream SSE funciona de verdade no ambiente publicado (Railway + rewrite do Next.js), não só em `localhost`, já que essa é a maior fonte de risco concreta pra essa feature.

### O que implementar
- **O problema real**: `Codigo/Front/next.config.ts` proxeia `/api/:path*` pro backend via rewrite same-origin (pra manter o cookie `HttpOnly` first-party). Esse rewrite já teve que ter o timeout aumentado de 30s (padrão do Next) pra 5 minutos (`experimental.proxyTimeout: 300_000`) por causa de um relatório fiscal lento — uma conexão SSE é *longa por natureza* (fica aberta o tempo todo), então ela esbarra na mesma configuração.
- Estratégia: **o servidor fecha a conexão SSE de forma controlada antes do timeout do proxy** (ex. `SseEmitter` com timeout de 4 minutos, menor que os 5 minutos do `proxyTimeout`), e o `EventSource` do navegador reconecta automaticamente quando a conexão cai — isso é comportamento nativo dele, não precisa de código extra no frontend além de tratar o evento `onerror` sem quebrar a UI. Ou seja: em vez de brigar pra manter uma conexão aberta indefinidamente através de um proxy que não foi desenhado pra isso, a spec assume reconexões periódicas como parte normal do fluxo.
- Confirmar que o rewrite do Next não faz buffering completo da resposta antes de repassar (o `http-proxy` interno do Next tipicamente faz streaming de chunks, não buffering total — mas isso precisa ser **validado no ambiente de deploy real**, não só assumido, porque é o tipo de coisa que só aparece em produção).
- Testar em HML/staging antes de considerar essa etapa concluída — não validar só em `localhost`, já que o comportamento do proxy é justamente o que muda entre os dois ambientes.

### Critério de aceite
- Em ambiente de deploy (Railway/HML), abrir a stream, disparar uma captura de outro dispositivo e ver o evento chegar (não só em localhost).
- Deixar a stream aberta por mais de 5 minutos e confirmar que ela reconecta sozinha sem erro visível pro usuário (ex. sem travar a UI da fila).
- Medir a latência real entre "Gemini terminou" e "evento chegou no PC" em produção — deve ficar na casa de 1-3s, não minutos (se ficar muito mais que isso, o buffering do proxy é o suspeito nº 1).

### Como testar (temporário)
- Deploy de teste em HML, repetir o teste `curl -N`/DevTools da Etapa 5 apontando pro domínio de HML em vez de `localhost`.

### Segurança
- Nenhuma nova além do já coberto — esta etapa é sobre corretude operacional, não superfície de ataque.

---

## Etapa 7 — Frontend: pareamento de dispositivo (QR) e gestão de dispositivos

### Objetivo
Tela no PC pra gerar o QR/código e gerenciar dispositivos já pareados; tela leve no celular pra confirmar o pareamento sem precisar do login completo.

### O que implementar
- **PC** — nova seção "Dispositivos" (dentro da área de compras/notas): botão "Vincular novo dispositivo" chama `POST /pareamento/iniciar`, renderiza o QR (biblioteca leve tipo `qrcode`, gerado client-side a partir da URL `https://{frontend}/m/vincular?codigo=XXXXXX`) junto com o código em texto grande (fallback pra quem não quer/consegue escanear — digita o código manualmente no celular). Lista de dispositivos já pareados (`GET /dispositivos`) com nome, último uso, botão "Revogar" (`DELETE /dispositivos/{id}`, com confirmação — ação irreversível, o celular perde o acesso na hora).
- **Celular** — nova rota pública `/m/vincular` (fora do `AuthGuard` normal — não existe login aqui): lê `?codigo=` da URL (pré-preenchido ao abrir via QR) ou permite digitar manualmente, campo pra "nome deste dispositivo" (sugestão automática via `navigator.userAgent`, editável), botão "Confirmar vínculo" chama `POST /pareamento/confirmar`, guarda o `deviceToken` retornado em `localStorage` (chave própria, ex. `hortifruti_device_token`), redireciona pra tela de captura (Etapa 8).

### Critério de aceite
- QR gerado no PC, escaneado pela câmera nativa do celular (não uma leitura dentro da página — é só um link), abre o navegador do celular direto na tela de confirmação com o código preenchido.
- Depois de confirmado uma vez, fechar e reabrir o navegador do celular (ou o app, se for PWA) não pede pareamento de novo — vai direto pra tela de captura porque o token já está salvo.
- Revogar no PC faz a próxima tentativa de captura naquele celular falhar com mensagem clara ("dispositivo desvinculado, peça um novo pareamento").

### Como testar (temporário)
- Testar em dispositivo real (Android + iPhone), não só emulador — igual a Etapa 5 da spec original recomenda pra câmera, comportamento de QR/redirect também varia entre navegadores mobile.

### Segurança
- `/m/vincular` é uma rota pública do frontend (sem sessão) — garantir que ela não vaza nenhuma informação além do necessário pro fluxo (não deve, por exemplo, aceitar um `usuarioId` na URL; o vínculo é resolvido inteiramente pelo código de 6 dígitos no backend).

---

## Etapa 8 — Frontend: fila em tempo real no PC + captura no celular

### Objetivo
Ligar tudo: celular fotografa → aparece na fila do PC quase instantaneamente → reaproveita o modal de revisão já existente.

### O que implementar
- **PC** — componente "Fila de notas pendentes": ao montar, chama `GET /pendentes` (estado inicial) e abre `new EventSource("/api/compras/notas/stream")`; em qualquer mensagem recebida, rechama `GET /pendentes` (não confia no conteúdo do evento, só usa como gatilho). Cada item da fila mostra status (badge: recebida/extraindo/pronta/erro) e, ao clicar num item `PRONTA`, abre o `NotaRevisaoModal.tsx` **já existente** (reaproveitar tal como está, só trocando a origem dos dados: hoje ele recebe o resultado de uma chamada síncrona feita na mesma tela; agora ele recebe os dados de uma `CapturaNotaPendente` já pronta, buscados por `capturaId`).
- **Celular** — tela de captura (`/m/capturar`, protegida por "existe device token no localStorage?" em vez de `AuthGuard` normal): reaproveita o mesmo padrão de `<input type="file" accept="image/*" capture="environment">` do `CapturarNotaCamera`, sobe pro `POST /capturas` com o `deviceToken` no header, mostra feedback simples ("Enviada! Processando...") e permite tirar a próxima foto imediatamente — sem esperar a extração terminar, já que ela é assíncrona (o funcionário pode fotografar 10 notas em sequência sem esperar nenhuma).

### Critério de aceite
- Fluxo ponta a ponta: parear celular uma vez → tirar 3 fotos em sequência no celular sem esperar → todas aparecem na fila do PC em poucos segundos cada, sem recarregar a página.
- Clicar numa captura `PRONTA` na fila abre o modal de revisão com os dados certos daquela captura específica (não confundir capturas quando há várias simultâneas).
- Confirmar uma nota no modal chama o fluxo de criação de compra já existente (mesmo caminho da Etapa 7 da spec original) e marca a `CapturaNotaPendente` como `CONFIRMADA`, removendo-a da fila.

### Como testar (temporário)
- Teste manual com o celular físico e o PC lado a lado: tirar foto, cronometrar até aparecer na fila do PC.
- Testar 2-3 fotos em sequência rápida pra garantir que não há mistura de resultado entre capturas concorrentes.

### Segurança
- Confirmar que os endpoints de captura/fila continuam exigindo o papel/token correto mesmo depois de toda essa integração (fácil de esquecer um `@PreAuthorize` numa rota nova adicionada por último).

---

## Etapa 9 — Segurança e limites (hardening específico desta feature)

### Objetivo
Fechar as pontas soltas de segurança introduzidas pelo conceito novo de "dispositivo vinculado", complementando (não substituindo) a Etapa 8 da spec original.

### O que implementar
- Rate limit dedicado (Bucket4j) em `/pareamento/confirmar` — mais restritivo que o padrão (ex. 5/min por IP), já que é o único endpoint público novo.
- Rate limit em `/capturas` por **dispositivo** (não só IP) — um `deviceToken` comprometido não deveria conseguir esgotar sozinho a cota diária do Gemini.
- Expiração por inatividade do `deviceToken`: se `ultimoUsoEm` passar de N dias (ex. 90, configurável), tratar como revogado automaticamente — mesma lógica de defesa em profundidade que o refresh token já tem com seus 30 dias, adaptada (dispositivo de captura tende a ficar mais tempo sem trocar que uma sessão de navegador).
- Página de "Dispositivos" no PC deixa claro, visualmente, que revogar é a ação certa em caso de celular perdido/roubado — não é só uma feature de limpeza, é o mecanismo de resposta a incidente dessa feature.
- Confirmar que nenhuma foto de nota nem dado de cliente extraído é logado (mesma regra da Etapa 2 da spec original, agora valendo também pro caminho assíncrono).

### Critério de aceite
- Passar do limite em `/pareamento/confirmar` retorna 429.
- `deviceToken` sem uso há mais que o período configurado passa a ser rejeitado como se tivesse sido revogado manualmente.
- Simular Gemini fora do ar (API key inválida) não impede o cadastro manual de compra de continuar funcionando (mesma garantia da spec original, agora também no caminho por dispositivo).

### Como testar (temporário)
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/dispositivos/pareamento/confirmar \
    -H "Content-Type: application/json" -d '{"codigo":"000000","nomeDispositivo":"teste"}'
done
# esperar 429 a partir de algum ponto
```

### Segurança
- Etapa inteira é sobre segurança — não pular mesmo com pressa, pelo mesmo motivo que a Etapa 8 da spec original não pode ser pulada: aqui é onde um dispositivo perdido deixa de ser um incidente e vira só um "revogar e seguir".

---

## Resumo da ordem de implementação

| Etapa | Entrega | Depende de |
|---|---|---|
| 1 | Modelo de dados: dispositivo vinculado | Etapas 0-4 da spec original |
| 2 | Endpoints de pareamento | 1 |
| 3 | Autenticação por token de dispositivo (escopo restrito) | 1, 2 |
| 4 | Upload assíncrono + fila persistida | 3, Etapas 1-4 da spec original |
| 5 | Notificação em tempo real (SSE) | 4 |
| 6 | Validação em deploy (proxy do Next.js) | 5 |
| 7 | Frontend: pareamento (QR) e gestão de dispositivos | 2 |
| 8 | Frontend: fila em tempo real + captura no celular | 4, 5, 7, Etapa 6 (mock) e 7 (integração) da spec original |
| 9 | Segurança e limites | 4, 5, 7 |

As Etapas 1-3 (backend de pareamento) e a Etapa 7 (frontend de pareamento) podem ser feitas em paralelo. A Etapa 4 depende do backend de extração já existente (spec original) e da Etapa 3. A Etapa 6 é obrigatoriamente testada em ambiente de deploy real, não só localhost — é o ponto de maior risco concreto levantado nesta spec.

---

## Limitações conhecidas (aceitas para o estado atual do projeto)

- **Registro de SSE em memória (`SseEmitterRegistry`) e cache do código de pareamento são single-instance**, assim como `TokenBlocklist` e `RateLimitingFilter` já são hoje. Isso é consistente com o resto do projeto e é aceitável enquanto o Railway rodar uma única instância do backend — mas se algum dia escalar horizontalmente, os três (blocklist, rate limiter, SSE registry) precisam migrar juntos pra um mecanismo compartilhado (Redis pub/sub, por exemplo). Não é uma limitação nova introduzida por esta feature, é a mesma que já existe, só reaparecendo aqui.
- **Sem Flyway/Liquibase** neste projeto (`ddl-auto=update`) — as tabelas novas (`dispositivos_vinculados`, `capturas_nota_pendentes`) sobem automaticamente via Hibernate, sem rollback formal, do mesmo jeito que todo o resto do schema hoje.
