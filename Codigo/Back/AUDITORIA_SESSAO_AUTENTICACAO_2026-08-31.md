# Auditoria — Sessão, Autenticação, Expiração de Token e Hibernação do Railway

**Escopo desta rodada:** apenas o **backend** (`Codigo/Back/`). O frontend (interceptors HTTP, storage de sessão, retry/timeout no cliente) ainda não foi auditado e é a próxima etapa recomendada — várias conclusões abaixo dependem de como o frontend reage a cada código de status, e isso está marcado explicitamente onde relevante.

**Método:** leitura completa da cadeia de autenticação (login → filtros de segurança → validação de JWT → refresh → logout), do datasource/HikariCP, do Dockerfile e das `application-*.properties`, e simulação mental dos cenários pedidos (backend hibernado/reiniciado, token expirado, refresh token expirado, timeout, erro 5xx, erro 401/403, perda momentânea de conexão, múltiplas abas/requisições simultâneas). Nenhuma alteração de código foi feita — auditoria somente leitura.

---

## Resumo executivo

O backend **não perde a sessão do usuário quando reinicia ou "hiberna"**: o JWT é stateless (validado só por HMAC + segredo em variável de ambiente, que sobrevive a restarts) e o refresh token vive no MySQL (também sobrevive a restarts). Ou seja, **um restart do container por si só nunca deveria derrubar o login de ninguém**.

O problema real e comprovável por código está em outro lugar: **há uma condição de corrida em `RefreshTokenService.rotate()` que pode revogar todas as sessões de um usuário legítimo quando duas requisições concorrentes tentam renovar o token ao mesmo tempo** (múltiplas abas, múltiplas chamadas de API que expiram juntas, um interceptor de frontend sem mutex de refresh). Esse é o candidato nº1 para "a sessão cai sozinha depois de um tempo aleatório" — não é aleatório no tempo, é condicionado a concorrência, o que looks aleatório do ponto de vista do usuário.

Em paralelo, todo o backend devolve **403 para praticamente tudo** (token inválido, token expirado, reuso de refresh token, acesso negado por role, origem CSRF bloqueada) em vez de diferenciar 401 (não autenticado / precisa renovar) de 403 (autenticado, mas sem permissão). Isso por si só não derruba sessão nenhuma, mas **tira do frontend qualquer forma confiável de decidir "tento refresh" vs. "desisto e mando pro login"** só olhando o status HTTP — o que é exatamente o tipo de ambiguidade que costuma virar "às vezes desloga sem motivo".

---

## 1) Problemas encontrados, por gravidade

### 🔴 CRÍTICO
**P1 — Corrida em `RefreshTokenService.rotate()` pode revogar todas as sessões de um usuário legítimo (falso positivo de "reuso de token roubado")**
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/config/auth/RefreshTokenService.java:35-71`
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/controller/user/AuthController.java:76-108,131-139`

### 🟠 ALTO
**P2 — Uso uniforme de 403 para "token inválido/expirado", "acesso negado por role", "reuso de refresh token" e "origem bloqueada" — sem 401 em lugar nenhum**
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/exception/auth/TokenException.java:15-18`
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/config/auth/SecurityFilter.java:113-117`
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/controller/user/AuthController.java:131-139`
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/exception/GlobalExceptionHandler.java:94-100`

### 🟡 MÉDIO
**P3 — `/auth/refresh` não tem bucket de rate-limit próprio; compartilha o padrão genérico de 10 req/min por IP**
Arquivo: `src/main/java/com/hortifruti/sl/hortifruti/config/auth/RateLimitingFilter.java:32-44`
*Agrava P1*: se o frontend reagir à corrida do P1 tentando refresh de novo repetidamente (retry sem backoff), pode esbarrar em 429 e piorar a recuperação.

### 🟢 BAIXO / INFORMATIVO (não é a causa do sintoma relatado, mas documentado por completude)
**P4 — Estado de segurança em memória (`ConcurrentHashMap`) é perdido a cada restart/redeploy**: `TokenBlocklist` (denylist de JWT revogado no logout), buckets do `RateLimitingFilter` e do `DeviceTokenAuthFilter`, e os tickets do `RealtimeTicketService`.
Arquivos: `TokenBlocklist.java:13-15`, `RateLimitingFilter.java:53`, `DeviceTokenAuthFilter.java:67`, `RealtimeTicketService.java:28`.
Efeito real: o oposto do sintoma relatado — um token que tinha sido revogado no logout **volta a funcionar** por até ~60 min após um restart, até expirar naturalmente. Isso **não desloga ninguém**; é uma lacuna de segurança menor, não um bug de sessão. Citado aqui só para não ser confundido com a causa do problema.

**P5 — Sem endpoint de health-check dedicado (`/actuator/health` ou custom) e sem `railway.json`/`railway.toml` configurando `healthcheckPath`**
Não há `spring-boot-starter-actuator` no `pom.xml` nem arquivo de configuração do Railway no repositório.
Efeito: não é causa de perda de sessão, mas deixa o Railway sem um sinal de prontidão explícito para orquestrar deploys/restarts, o que pode alongar a janela em que requisições chegam a uma instância ainda subindo (ver seção 5).

---

## 2) Causa provável de cada problema

### P1 — Corrida no rotation do refresh token

**O mecanismo, como está hoje:**

```java
// RefreshTokenService.java:35-71
@Transactional
public RotationResult rotate(String rawToken) {
  String hash = tokenHasher.hash(rawToken);
  RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
      .orElseThrow(() -> new TokenException(...));

  if (existing.getRevokedAt() != null) {
    // "reuso" detectado -> revoga TUDO do usuário
    refreshTokenRepository.revokeAllActiveByUserId(existing.getUserId(), LocalDateTime.now());
    throw new TokenException(...);
  }
  if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new TokenException(...);
  }

  existing.setRevokedAt(LocalDateTime.now());
  refreshTokenRepository.save(existing);
  ...
  String newRawToken = persistNewToken(existing.getUserId());
  return new RotationResult(user, newRawToken);
}
```

A intenção do design é boa e está documentada no próprio arquivo (linhas 14-18): se um refresh token **já revogado** for reapresentado, é sinal de vazamento/roubo, então todas as sessões do usuário são derrubadas por segurança. O problema é que essa checagem (`findByTokenHash` → olhar `revokedAt` → decidir) **não tem nenhum lock** (nem `SELECT ... FOR UPDATE`, nem `@Version` otimista na entidade `RefreshToken` — ver `model/RefreshToken.java:30-60`, sem campo de versão). Com o isolamento padrão do MySQL/InnoDB (REPEATABLE READ), duas transações que leem a mesma linha antes de qualquer uma commitar **não se bloqueiam** num `SELECT` simples.

**Sequência que quebra a sessão de um usuário legítimo (sem token roubado, sem ataque):**

1. O cookie `refresh_token` (valor **T**) está no navegador.
2. Duas requisições concorrentes chegam em `POST /auth/refresh` apresentando o mesmo cookie **T** — isso acontece sempre que: (a) o usuário tem duas abas abertas na mesma sessão, (b) várias chamadas de API do frontend expiram "ao mesmo tempo" e cada uma dispara seu próprio refresh (ausência de mutex/fila única de refresh no cliente — a confirmar na auditoria de frontend), ou (c) um retry duplica a chamada de refresh.
3. Requisição A lê o registro de **T** (`revokedAt == null`), processa, e **comita**: marca **T** como revogado e insere um novo token **T2**.
4. Requisição B, que também leu **T** com `revokedAt == null` (antes de A comitar, ou numa janela próxima o bastante), tenta revogar o mesmo **T** — mas ao (re)ler o estado após o commit de A (ou ao competir pela mesma linha), encontra **T já revogado**.
5. Isso cai no branch de "reuso detectado" (`RefreshTokenService.java:46-50`) → chama `revokeAllActiveByUserId(userId, ...)`, que **revoga inclusive o T2 que a requisição A acabou de emitir com sucesso**.
6. A requisição B lança `TokenException` → `AuthController.refresh()` captura em `catch (TokenException e)` (`AuthController.java:86-88`) e chama `clearedCookiesResponse()` (`AuthController.java:131-139`), que devolve **403 e limpa os dois cookies (`auth_token` e `refresh_token`) com `Max-Age=0`**.
7. Se essa resposta de B chegar ao navegador **depois** da resposta de sucesso de A, o `Set-Cookie` de B **sobrescreve/apaga** o cookie que A tinha acabado de configurar corretamente. Resultado: o navegador fica sem `refresh_token` válido, e o próximo `GET /auth/me`/qualquer chamada autenticada falha assim que o `auth_token` (JWT) expirar — o usuário é forçado ao login **mesmo tendo feito tudo certo**.

Esse cenário não depende do backend estar "hibernando" nem de nenhum timeout de rede — ele é puramente uma corrida de concorrência local, e é agravado quanto mais chamadas simultâneas o frontend disparar perto do momento em que o `auth_token` expira (o que é exatamente quando o refresh é acionado, então a exposição à corrida é maior justo no momento em que o refresh "deveria" ser transparente).

`AuthController.java:83-88` também não faz nenhuma tentativa de distinguir "token inexistente/expirado normalmente" de "reuso detectado por corrida" — os dois caem no mesmo `catch (TokenException e)` e no mesmo `clearedCookiesResponse()`.

### P2 — Conflação de 401/403

`TokenException.getHttpStatus()` (`exception/auth/TokenException.java:15-18`) sempre devolve `HttpStatus.FORBIDDEN`. Essa exceção é lançada tanto por token **inválido** quanto **expirado** quanto por **reuso** (P1). `SecurityFilter.java:113-117` também devolve 403 manualmente para qualquer `TokenException` capturada no meio do filtro. `AuthController.clearedCookiesResponse()` (`AuthController.java:131-139`) devolve 403 (`HttpStatus.FORBIDDEN`) tanto para "sem cookie de refresh" quanto para "refresh inválido/expirado/revogado".

Ao mesmo tempo, `GlobalExceptionHandler.handleAccessDeniedException` (`exception/GlobalExceptionHandler.java:94-100`) — que trata negação por `@PreAuthorize`/role, um problema de **autorização**, não de **autenticação** — também devolve 403. E `SecurityFilter.isForgedCrossOriginRequest` (`SecurityFilter.java:159-169`), que bloqueia uma possível forjação CSRF por Origin, também devolve 403 (`SecurityFilter.java:70-76`).

Ou seja: **token expirado, token roubado/reusado, usuário sem a role certa, e requisição de origem suspeita — os quatro devolvem exatamente o mesmo código HTTP (403)**, cada um com corpo de JSON diferente, mas sem nenhuma convenção de status que permita a um interceptor HTTP genérico decidir "isto aqui eu tento resolver com refresh" vs. "isto aqui eu não devo nem tentar, é erro de permissão do usuário, não da sessão". A única forma seria inspecionar sempre o corpo da resposta — o que só funciona se **todo** interceptor no frontend fizer isso corretamente e nunca tratar 403 genericamente como "sessão morta, desloga". Isso não pode ser confirmado sem a auditoria do frontend, mas é uma armadilha de design real e comprovável só pelo backend.

### P3 — Rate limit genérico também vale para `/auth/refresh`

`RateLimitingFilter.LIMITES_POR_ENDPOINT` (`RateLimitingFilter.java:41-44`) só tem uma entrada especial, para `/api/dispositivos/pareamento/confirmar`. Qualquer outra rota, incluindo `/auth/refresh`, cai no `LIMITE_PADRAO` de 10 requisições/minuto por IP (`RateLimitingFilter.java:32-33`). Isso é razoável isoladamente, mas combinado com P1: se o frontend, ao ver falhas de refresh, tentar de novo sem espaçamento (ex.: várias abas cada uma tentando seu próprio retry), o próprio ato de tentar se recuperar pode esgotar essa cota e produzir 429 em cima do 403 original — piorando a percepção de "trava e não volta mais".

### P4 e P5

Já descritos acima — não são causa do sintoma relatado, documentados por completude do "varredura completa" pedida.

---

## 3) Fluxo atual de autenticação e onde ele falha

```
1. POST /auth (login)
   Auth.autenticar() -> valida credencial, LoginProtectionService (lockout progressivo em banco)
   -> gera JWT (60 min, TokenConfiguration.generateToken)
   -> gera refresh token opaco (30 dias, RefreshTokenService.issueToken, hash SHA-256 em banco)
   -> Set-Cookie auth_token (httpOnly, path=/, 60 min) + refresh_token (httpOnly, path=/auth, 30 dias)

2. Toda requisição autenticada
   SecurityFilter.doFilterInternal()
   -> lê auth_token do cookie (ou header Authorization)
   -> TokenConfiguration.validateToken(): verifica assinatura HMAC + issuer + expiração
      - token ausente -> segue sem autenticação (filtro de autorização do Spring decide 403 se a rota exigir role)
      - token inválido/expirado -> TokenException -> 403  [P2: deveria ser 401]
      - erro inesperado (ex.: banco fora do ar ao carregar o usuário) -> 503, NÃO 403
        (SecurityFilter.java:118-134 — este ponto já está correto e evita
        confundir indisponibilidade com sessão expirada)

3. POST /auth/refresh (deveria ser chamado pelo frontend ao ver 401/403 de token)
   AuthController.refresh() -> lê refresh_token do cookie
   -> RefreshTokenService.rotate(): revoga o token apresentado, emite um novo (rotação)
      - token ausente/inválido/expirado/revogado -> TokenException -> 403 + limpa os dois cookies
        [P1: aqui mora a corrida — duas chamadas concorrentes de refresh podem se
        auto-derrubar mesmo sem token comprometido]
   -> sucesso: novo auth_token (60 min) + novo refresh_token (30 dias, sliding)

4. POST /auth/logout
   -> revoga o JWT atual (TokenBlocklist, em memória) e o refresh token atual (banco)
   -> limpa os dois cookies
```

**Onde ele falha, na ordem de probabilidade de causar o sintoma relatado:**

1. Passo 3, quando duas chamadas de refresh concorrem (P1) — pode derrubar uma sessão 100% válida.
2. Passo 2/3, porque tudo que dá errado devolve 403 (P2) — o frontend não tem como confiar cegamente no status HTTP para decidir "renova" vs. "desiste", a menos que sempre olhe o corpo da resposta (não confirmável sem auditar o frontend).
3. Passo 3, se o frontend retry sem controle esbarrar em 429 (P3) — agrava 1, não é causa isolada.

**O que o fluxo atual NÃO faz e não deveria ser confundido com bug:** o backend não invalida nada quando o processo reinicia. `jwt.secret` vem de variável de ambiente (persistente), e os refresh tokens vivem no MySQL (persistente). Um restart do Railway, sozinho, não desloga ninguém — a única coisa que se perde num restart é o `TokenBlocklist` em memória (P4), que só afeta tokens que tinham sido revogados manualmente por logout, e o efeito é o token **voltar a funcionar**, não parar de funcionar.

---

## 4) Como deveria funcionar o fluxo correto de recuperação automática

Do lado do backend (o que está no escopo desta auditoria):

1. **Serializar a rotação do refresh token por usuário/token**, para que duas chamadas concorrentes com o mesmo `refresh_token` nunca produzam o falso positivo de "reuso": a segunda chamada deveria, na pior das hipóteses, **esperar** a primeira terminar e reaproveitar o resultado (ou receber o token novo que a primeira já gerou), nunca acionar `revokeAllActiveByUserId`. Isso resolve P1 na raiz — ver estratégia na seção 6.
2. **Diferenciar 401 de 403** de forma consistente: 401 para "não autenticado / token ausente, inválido ou expirado" (sinal para o cliente tentar `/auth/refresh` e, só se isso também falhar, redirecionar ao login); 403 reservado só para "autenticado, mas sem permissão" (role incorreta) e para o bloqueio de origem forjada. Isso dá ao frontend um contrato confiável para decidir automaticamente sem precisar inspecionar corpo de resposta.
3. Continuar devolvendo **503** (não 401/403) para falhas de infraestrutura (banco fora do ar, erro inesperado) — isso já está certo hoje em `SecurityFilter.java:118-134` e deve ser preservado e estendido ao restante do fluxo (por exemplo, garantir que uma falha de banco durante `/auth/refresh` também não seja tratada como "refresh token inválido").
4. Um **cold start do Railway** (container subindo, pool do Hikari se reconectando, Flyway checando migrations) deve se manifestar para o cliente como **timeout de rede ou 503**, nunca como 401/403 — e como confirmado no ponto 3 acima, hoje isso já é majoritariamente verdade dentro do `SecurityFilter`. A peça que falta é o cliente (frontend) **esperar/repetir a requisição original em vez de tratar timeout/erro de rede como sessão inválida** — isso é trabalho de frontend, fora do escopo desta rodada, mas a pré-condição do lado do backend (não derrubar sessão por causa de instabilidade transitória) já está parcialmente implementada.

---

## 5) O que é Railway/hibernação vs. o que é bug da aplicação

| Item | Classificação | Evidência |
|---|---|---|
| P1 — corrida no rotation do refresh token | **Bug da aplicação** | Reproduz localmente, sem Railway, só com duas chamadas HTTP concorrentes a `/auth/refresh` com o mesmo cookie. Nada a ver com hibernação. |
| P2 — 401/403 conflados | **Bug/decisão de design da aplicação** | Está no código de status (`TokenException`, `SecurityFilter`, `GlobalExceptionHandler`), não depende de infraestrutura. |
| P3 — rate limit sem exceção para `/auth/refresh` | **Bug/lacuna da aplicação** | Configuração local no `RateLimitingFilter`, nada relacionado a Railway. |
| P4 — estado em memória perdido no restart | **Efeito esperado de single-instance + restart** (Railway ou qualquer outra plataforma) | Já documentado nos comentários do próprio código como limitação aceita; efeito é neutro/positivo para o usuário (token revogado volta a valer), não negativo. |
| P5 — ausência de health-check dedicado | **Configuração de infraestrutura**, não bug de autenticação | Ausência de `spring-boot-starter-actuator`/`railway.json` confirmada; não afeta validade de sessão, só o tempo de warm-up percebido em deploys/restarts. |
| "Backend hibernado demora para responder" | **Comportamento esperado de plataforma (Railway)**, a mitigar no cliente | O backend não tem nenhum código que invalide sessão por estar frio; o atraso em si (JVM + Spring Boot subindo) é inerente à plataforma/arquitetura, não é uma falha a "corrigir" no backend além de manter o boot o mais rápido possível. |

**Conclusão desta seção:** nenhum dos problemas realmente encontrados no backend é causado pelo Railway "hibernar". O Railway pode, na pior das hipóteses, causar **lentidão temporária** (timeout, 503) — e o próprio backend já trata isso separadamente de erro de token na maior parte do fluxo (`SecurityFilter.java:118-134`). O que **de fato** derruba sessão de forma reproduzível é a corrida de concorrência em P1, agravada pela ambiguidade de status em P2 — ambos 100% dentro do código da aplicação.

---

## 6) Estratégia de correção priorizada

1. **[x] P1 (crítico, primeiro):** eliminar a corrida em `RefreshTokenService.rotate()`. **Implementado em 2026-08-31.**
   - Opção mínima: usar uma trava pessimista na leitura (`SELECT ... FOR UPDATE`, via `@Lock(LockModeType.PESSIMISTIC_WRITE)` no método do repositório) para que a segunda transação concorrente espere a primeira commitar e então veja o estado final correto (token já rotacionado) — nesse ponto, decidir explicitamente que "token já revogado há poucos segundos, provavelmente pela minha própria corrida" não deveria disparar `revokeAllActiveByUserId` automaticamente sem alguma janela de tolerância, ou o cliente precisa deduplicar chamadas de refresh antes de chegar aqui (mutex de refresh no frontend — a validar na próxima auditoria).
   - Alternativa complementar: no frontend, garantir que só exista **uma** chamada de refresh em voo por vez (fila/promise compartilhada), o que reduz drasticamente a chance de dois refreshes concorrentes existirem — mas isso não substitui a correção no backend, porque duas abas em processos de navegador diferentes não compartilham essa fila em memória.
   - Adicionar teste automatizado que dispare duas chamadas concorrentes de `/auth/refresh` com o mesmo token e comprove que ambas resultam em sessão válida (ou uma delas falha sem derrubar a outra), nunca em revogação total.
   - **Como ficou:** `RefreshTokenRepository.findByTokenHashForUpdate` (novo método, `@Lock(LockModeType.PESSIMISTIC_WRITE)`) serializa duas rotações concorrentes do mesmo token. Além disso, `RefreshTokenService` ganhou um cache local de curta duração (`recentRotations`, janela de 10s) que guarda, por hash do token antigo, o token novo emitido — se uma segunda chamada concorrente encontrar o token "já revogado" **e** achar essa rotação recente no cache, ela recebe o mesmo token novo em vez de disparar `revokeAllActiveByUserId`. Reuso genuíno (token roubado, sem entrada no cache ou fora da janela) continua revogando tudo normalmente. Teste automatizado de concorrência (item da seção 7) ainda **não foi adicionado** — pendente.

2. **[x] P2 (alto, em seguida):** separar 401 de 403. **Implementado em 2026-08-31.**
   - `TokenException.getHttpStatus()` → `HttpStatus.UNAUTHORIZED` para os casos de token ausente/inválido/expirado/revogado.
   - Manter 403 exclusivamente para `AccessDeniedException` (role) e para o bloqueio de origem forjada em `SecurityFilter.isForgedCrossOriginRequest`.
   - Isso é uma mudança de contrato de API — precisa ser coordenada com o frontend (próxima auditoria) antes de ir para produção, para que o interceptor HTTP seja atualizado no mesmo deploy.
   - **Como ficou:** `TokenException.getHttpStatus()`, o catch de `TokenException` em `SecurityFilter.doFilterInternal` e `AuthController.clearedCookiesResponse()` agora devolvem 401. `AccessDeniedException` (role), o bloqueio de origem forjada e o bloqueio por `PASSWORD_CHANGE_REQUIRED` continuam em 403 (autenticado, mas bloqueado/sem permissão) — não alterados. **Pendência real:** o frontend (`fetchInterceptor.ts`) ainda decide "tento refresh" olhando `status === 403` (ver `Codigo/Front/AUDITORIA_SESSAO_AUTENTICACAO_2026-08-31.md`, achado F2) — precisa ser atualizado para `401` no mesmo deploy desta mudança, senão o interceptor do front para de reagir a token expirado/inválido.

3. **[x] P3 (médio):** dar a `/auth/refresh` (e talvez `/auth` de login) um bucket próprio no `RateLimitingFilter`, com limite adequado para tolerar picos legítimos (múltiplas abas do mesmo usuário) sem abrir brecha para força bruta. **Implementado em 2026-08-31** — `/auth/refresh` recebeu um bucket de 30 req/min por IP (vs. 10/min do padrão genérico); `/auth` (login) manteve o limite padrão, já que a proteção contra força bruta de credencial é feita por `LoginProtectionService` (lockout progressivo por conta/IP), não pelo rate limit genérico.

4. **[ ] P4/P5 (baixo, oportunista):** não é urgente; considerar `spring-boot-starter-actuator` com um `/actuator/health` liberado no `SecurityConfig` e um `railway.json` com `healthcheckPath` apontando para ele, para dar ao Railway um sinal de prontidão explícito em deploys — melhoria de operação, não de segurança de sessão. **Não implementado nesta rodada** (fora do pedido explícito de correção, e o próprio relatório já classificava como não-urgente).

---

## 7) Testes para confirmar que a sessão sobrevive a inatividade, restart e falha temporária

**Testes de concorrência (validam a correção de P1 — os mais importantes):**
- Dois clientes HTTP disparando `POST /auth/refresh` simultaneamente com o **mesmo** `refresh_token` válido → esperar que pelo menos um suceda e que nenhuma sessão legítima do usuário seja revogada como efeito colateral (checar no banco que ainda existe algum `refresh_tokens` ativo para o `userId` depois do teste).
- Repetir o teste acima com 5-10 chamadas concorrentes (simulando várias abas/painéis de dashboard expirando juntos) — mesmo critério de sucesso.
- Chamar `/auth/refresh` com um token **já revogado de propósito** (cenário real de roubo) e confirmar que `revokeAllActiveByUserId` ainda dispara corretamente nesse caso — a correção de P1 não pode enfraquecer essa proteção legítima.

**Testes de expiração/rotação (fluxo normal):**
- Login → esperar o `auth_token` expirar (ou usar `jwt.expiration-minutes` bem baixo em teste) → chamar endpoint protegido → confirmar 401/403 apropriado → chamar `/auth/refresh` → confirmar novo `auth_token` válido → repetir chamada ao endpoint protegido com sucesso, sem novo login.
- Repetir o ciclo de refresh várias vezes seguidas (simulando um usuário com a aba aberta por várias horas/dias) e confirmar que o refresh token sliding (`expiresAt = now + 30 dias` a cada rotação) realmente estende a janela, sem forçar login enquanto o usuário permanecer ativo dentro de 30 dias.
- Deixar o `refresh_token` expirar de verdade (passar dos 30 dias, ou usar `jwt.refresh-expiration-days` baixo em teste) → confirmar que **aí sim** o backend força novo login (esse é o único caso em que perder a sessão é o comportamento correto).

**Testes de indisponibilidade/timeout (não devem ser tratados como sessão inválida):**
- Derrubar a conexão com o MySQL momentaneamente durante uma chamada autenticada → confirmar que a resposta é 503 (não 401/403) e que o cookie de sessão não é limpo.
- Simular um erro inesperado dentro de `SecurityFilter.loadByUserName` (ex.: mock lançando exceção não relacionada a token) → confirmar 503, cookies preservados.
- Simular alta latência (>30s) para emular um "cold start" do Railway → confirmar que o backend, quando finalmente responde, não faz nada que invalide a sessão só por causa da demora (o filtro de segurança não tem nenhum timeout próprio que descarte tokens por lentidão — confirmar que isso continua assim).

**Teste de restart do processo (garante que "hibernação" não é confundida com logout):**
- Login → guardar `auth_token` e `refresh_token` válidos → reiniciar a aplicação (simulando redeploy/restart do Railway) → confirmar que o `auth_token` ainda válido continua sendo aceito (prova que a validação de JWT não depende de nenhum estado em memória) → confirmar que `/auth/refresh` com o `refresh_token` salvo também continua funcionando (prova que a tabela `refresh_tokens` sobreviveu ao restart, como já é o caso hoje).
- Confirmar, no mesmo teste, que um JWT que tinha sido revogado por logout **antes** do restart passa a ser aceito de novo (efeito conhecido de P4 — não é regressão, é a limitação já documentada do `TokenBlocklist` em memória; útil para não confundir esse comportamento esperado com um bug novo).

**Teste de rate limit (garante que a própria recuperação não seja bloqueada):**
- Disparar N chamadas de refresh dentro de 1 minuto (N > limite atual) e confirmar que o comportamento sob 429 é claro o suficiente para o cliente saber que deve esperar, e não interpretar como sessão morta.

---

## Observação final

Este documento cobre **somente o backend**, por instrução explícita para esta rodada. As conclusões sobre "onde a sessão realmente se perde no dia a dia" (se é majoritariamente P1, ou se o frontend também contribui com lógica própria de limpeza de storage/redirecionamento em erro de rede) só podem ser fechadas com a auditoria equivalente do frontend — recomendada como próximo passo, focada em: interceptor HTTP (o que cada status dispara), existência (ou não) de mutex de refresh no cliente, tratamento de timeout/erro de rede (não deve limpar sessão), e o que acontece na inicialização do app com uma sessão já existente.
