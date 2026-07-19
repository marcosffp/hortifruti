<!-- Título: Diagnóstico de Handshake mTLS — BB e Sicoob -->

# Diagnóstico do Handshake mTLS (BB e Sicoob)

## CAUSA RAIZ CONFIRMADA (2026-07-19, rodada com logs de diagnóstico em `Sem Título.rtf`)

O `Dockerfile` copiava `pom.xml` e `src/` para dentro do estágio de build do Maven, mas
**nunca copiava `lombok.config`**. Sem esse arquivo presente durante `mvn clean package`
dentro do container, a diretiva `lombok.copyableAnnotations +=
org.springframework.beans.factory.annotation.Qualifier` nunca é aplicada — o Lombok descarta
silenciosamente o `@Qualifier` do campo ao gerar o construtor via `@RequiredArgsConstructor`
em `BBToken`, `BBExtratoClient`, `SicoobToken` e `BilletHttpClient`.

Sem `@Qualifier` no construtor, o Spring cai no fallback de autowiring por **nome do
parâmetro** — que é literalmente `restTemplate` nessas quatro classes — e isso batia por
acidente com o bean `RestTemplateConfig.restTemplate()` (um `RestTemplate` genérico, sem
`SSLContext`/certificado). Era esse bean, e não `bbRestTemplate`/`billetRestTemplate`, que
estava sendo injetado em produção.

Confirmado com o log `[mTLS:diag]` de `RestTemplateDiagnostics` na EC2:
`requestFactory=org.springframework.http.client.SimpleClientHttpRequestFactory httpClient=n/a
(nao e HttpComponentsClientHttpRequestFactory)` — o factory padrão do Spring, não o
`HttpComponentsClientHttpRequestFactory` com o `SSLContext` mTLS configurado nos beans. O
self-test do `KeyManager` no boot (`[mTLS:BB][bean][selftest]` /
`[Sicoob][bean][selftest]`) resolveu o alias corretamente nos dois casos — confirmando que o
`KeyStore`/PFX sempre esteve correto; o problema era puramente qual `RestTemplate` chegava
até `SicoobToken`/`BBExtratoClient`/`BBToken`/`BilletHttpClient` em runtime.

**Correções aplicadas:**
1. `Dockerfile`: `COPY lombok.config ./` adicionado ao estágio de build, antes do
   `mvn clean package` — restaura o comportamento pretendido de `lombok.copyableAnnotations`.
2. `RestTemplateConfig.java`: bean genérico renomeado de `restTemplate` para
   `genericRestTemplate`, para que uma futura perda de `@Qualifier` (por qualquer motivo) não
   volte a colidir por nome com o bean errado. `WhatsAppService` (único outro consumidor do
   bean genérico) atualizado com `@Qualifier("genericRestTemplate")` explícito.

Confirmado via bytecode (`javap -v`) que o construtor gerado pelo Lombok agora carrega
`RuntimeVisibleParameterAnnotations` com `@Qualifier("billetRestTemplate")` no parâmetro —
com `lombok.config` presente no build.

---

## Veredito (histórico — antes da causa raiz acima ser encontrada)

**A causa não é infraestrutura de rede (Railway/Render/WAF/IP), é código: o app real, em
execução, não está enviando o certificado de cliente nas chamadas ao BB e ao Sicoob — e não
é porque o servidor remoto recusa negociar mTLS.**

Isso foi confirmado rodando a aplicação de verdade (mesmo commit do `prod`, `bdd4e06`) numa
EC2 Ubuntu "crua", sem nenhuma camada de rede do Railway/Render, com
`-Djavax.net.debug=ssl:handshake:verbose` ligado (ver `contexto-ec2-teste.md`). Essa é a
**primeira reprodução real** dos dois sintomas exatos de produção:

- BB: `400 Bad Request — "Failed to verify certificate"`
- Sicoob: `403 Forbidden — "Certificado digital e obrigatorio para este recurso."`

E o log de handshake, byte a byte, mostra exatamente onde a coisa quebra:

1. `auth.sicoob.com.br` **e** `extratos.mtls.api.bb.com.br` mandam `CertificateRequest`
   normalmente — o servidor remoto está pedindo o certificado. Isso **descarta** a hipótese
   anterior (seção 11.4, baseada só nos logs do Railway) de que um WAF/borda decide, por
   IP/geolocalização, nem pedir o certificado.
2. Em resposta, a JVM manda `Certificate` **vazio** (`certificate_list: []`), com o log
   nativo do JSSE dizendo `No X.509 cert selected` / `No available authentication scheme`.
   O certificado nunca chega a ser oferecido.
3. A classe própria do projeto, `LoggingX509KeyManager` — que loga **toda vez** que é
   consultada, com sucesso (`INFO ... alias escolhido=`) ou falha (`ERROR ... NENHUM alias
   selecionado`) — **não aparece nenhuma vez em todo o log da EC2**. O mesmo vale para o
   interceptor `[Sicoob][tls]` de `BilletSSLConfig`, que loga depois de **toda** resposta
   HTTP feita pelo `billetRestTemplate`. Nenhum dos dois apareceu, apesar de `SicoobToken`
   e `BBExtratoClient` terem, comprovadamente, feito as chamadas (é o próprio log de erro
   deles, `[Sicoob][token] falha HTTP 403...`, que aparece).

Ou seja: os `RestTemplate`s configurados com o `SSLContext`/`KeyManager` customizado
(`bbRestTemplate`, `billetRestTemplate`) **não são o HTTP client que de fato executou essas
conexões** na aplicação real rodando em container. O teste da seção 1 (mais abaixo), que
"comprovou" que o certificado é enviado, rodou um **programa Java avulso que só copia o
código** de `BBSSLConfig`/`BilletSSLConfig` — nunca testou o app de verdade. A EC2 é a
primeira vez que o app real foi observado nesse nível de detalhe, e ele se comporta
diferente do que o código, lido isoladamente, sugere.

**Não é mais um problema de rede/PaaS. É preciso descobrir por que, em runtime, a chamada
real do `SicoobToken`/`BBExtratoClient` não está passando pela conexão TLS configurada nos
beans `bbRestTemplate`/`billetRestTemplate`** — apesar do `@Qualifier` no código apontar
corretamente para eles (conferido nesta sessão, código atual).

### Próximo passo concreto — já implementado, falta rodar em produção/EC2

Adicionados logs de diagnóstico (`[mTLS:BB][bean]`, `[Sicoob][bean]`,
`RestTemplateDiagnostics`) que respondem duas perguntas na próxima execução:

1. **O `KeyManager` resolve algum alias neste runtime, fora do handshake real?**
   `BBSSLConfig`/`BilletSSLConfig` agora chamam `chooseClientAlias` diretamente no bean, na
   hora da construção (`logKeyManagerSelfTest`). Se isso já logar `NENHUM alias
   selecionado` no boot, o problema é no `KeyStore`/PFX deste ambiente — não precisa nem
   chegar a fazer uma chamada de rede para saber.
2. **A chamada real usa o mesmo `CloseableHttpClient` construído no bean?**
   `RestTemplateDiagnostics.logIdentity(...)` foi chamado logo antes de toda chamada de
   rede em `SicoobToken.getAccessToken`, `BilletHttpClient.doGet/doPost`,
   `BBToken.getAccessToken` e `BBExtratoClient.doGet`, logando o `identityHashCode` do
   `RestTemplate` e do `HttpClient` por trás dele — para comparar com o `httpClient=...`
   logado na construção do bean (`[mTLS:BB][bean]`/`[Sicoob][bean]`).

Falta rodar de novo na EC2 (ou já com esse commit em produção) e olhar, na ordem: (a) o
self-test no boot, (b) se o `httpClient=` do bean bate com o da chamada, (c) se o self-test
do KeyManager aparece de novo (ou não) durante a chamada real via `LoggingX509KeyManager`.

---

## Como o veredito foi construído

### 1. Teste local (macOS) — programa avulso, não é o app real

**Metodologia.** Nenhuma linha do repositório foi alterada. Para testar o handshake sem
depender de login (`ROLE_MANAGER`) nem subir o Spring Boot inteiro, foi escrito um programa
Java standalone (fora do repo) que replica o código de `BBSSLConfig`/`BilletSSLConfig`
(mesmo `KeyStore` PKCS12, `KeyManagerFactory`, `LoggingX509KeyManager` carregado via
classpath, `PoolingHttpClientConnectionManager`), com `-Djavax.net.debug=ssl:handshake:verbose`
contra os endpoints reais do BB e do Sicoob, usando o mesmo `DOCUMENT_PFX`/`PASSWORD_PFX` do
`.env` local.

**Resultado:** as 4 conexões que pediram `CertificateRequest` (BB extrato, Sicoob auth,
Sicoob API ×2) tiveram o certificado enviado e aceito no handshake TLS
(`Certificate`/`CertificateVerify`/`Finished` do cliente completos, alias
`hortifruti santa luzia ltda:27540906000155` sempre escolhido, nunca `null`). BB extrato
devolveu `400` por parâmetro de teste inválido (não relacionado a TLS); Sicoob token
devolveu `200`; Sicoob API teve `Connection reset` pós-handshake (rede, não certificado).

**Limitação importante (só percebida depois, com a seção 2 abaixo):** isso prova que o
*código*, executado isoladamente, monta e envia o certificado corretamente. Não prova que o
*app Spring Boot real*, em container, usa esse mesmo caminho em runtime — e a seção 2 mostra
que, no ambiente real, ele não usa.

Achado secundário (fora do escopo do mTLS): `BB_BASIC` no `.env` tem padding base64 inválido
(`...VFUzZlE===`, deveria ter só um `=`) — não quebrou o teste, mas vale revisar a origem.

### 2. Logs reais do Railway (prod, `deploy a8cab465`, commit `bdd4e06`)

Deploy incluiu `EgressIpLogger`, logging estruturado no catch de
`HttpClientErrorException` de `SicoobToken`, e o interceptor `[Sicoob][tls]` em
`BilletSSLConfig`. Capturado em 2026-07-19 ~08:58 GMT-3: rajada de ~19 falhas em 5,7s,
todas `403 FORBIDDEN — "Certificado digital e obrigatorio para este recurso"`,
`elapsedMs` entre 157–826ms (round-trip HTTP real, não timeout nem reset de conexão),
`egressIp=52.8.230.75` estável em todas as tentativas.

**Nenhuma linha `[mTLS:Sicoob]`** (nem sucesso nem erro) apareceu no período — na época,
isso foi interpretado como "o servidor nunca pediu certificado" (hipótese de WAF/borda por
IP, seção 11.4 da versão anterior deste documento). **A seção 3 abaixo mostra que essa
leitura provavelmente estava incompleta**: a ausência de `[mTLS:Sicoob]` não significa que
o servidor não pediu — significa que o `KeyManager` do app não foi consultado, e isso
acontece mesmo quando o servidor pede (como a EC2 comprova com captura TLS direta).

### 3. Teste na EC2 (app real, Linux, `-Djavax.net.debug` ligado) — a evidência decisiva

Ver `contexto-ec2-teste.md` para a infraestrutura. Rodando o container real (mesmo commit
`bdd4e06`) numa VM Linux crua, com debug de SSL nativo da JVM ligado, os logs capturados em
`Sem Título.rtf` (2026-07-19, ~15:30 UTC) mostram, para os dois bancos:

**BB — `extratos.mtls.api.bb.com.br`:**
```
15:30:19.932  Consuming CertificateRequest handshake message (servidor pede cert)
15:30:20.023  X509KeyManager class: sun.security.ssl.SunX509KeyManagerImpl
15:30:20.023  No X.509 cert selected for [EC, EdDSA, RSASSA-PSS, RSA]
15:30:20.023  No available authentication scheme
15:30:20.024  Produced client Certificate message ( certificate_list: [] )   <- vazio
...
15:30:20.201  ERROR BBExtratoClient: Falha ao consultar extrato do BB (400 BAD_REQUEST):
              {"message":"Failed to verify certificate","error":"Bad Request","statusCode":400}
```

**Sicoob — `auth.sicoob.com.br`:**
```
15:30:21.212  Consuming CertificateRequest handshake message (servidor pede cert)
15:30:21.283  X509KeyManager class: sun.security.ssl.SunX509KeyManagerImpl
15:30:21.284  No X.509 cert selected for [RSA, EC]
15:30:21.284  No available authentication scheme
15:30:21.284  Produced client Certificate message ( certificate_list: [] )   <- vazio
...
15:30:21.608  ERROR SicoobToken: [Sicoob][token] falha HTTP 403 FORBIDDEN - corpo=
              O seu acesso nao foi autorizado. Certificado digital e obrigatorio para
              este recurso. elapsedMs=826 egressIp=18.217.146.147
```

Padrão idêntico se repete em toda a rajada de falhas do Sicoob que seguiu (múltiplos
clientes, mesma thread `nio-8080-exec-9`, `elapsedMs` 157–826ms, `egressIp` sempre o IP
público da própria EC2).

**Confirmado por busca no log inteiro (36 mil linhas):**
- Zero ocorrências de `[mTLS:BB]` ou `[mTLS:Sicoob]` (log do `LoggingX509KeyManager`,
  que dispara em toda chamada a `chooseClientAlias`/`chooseEngineClientAlias`, sucesso ou
  falha).
- Zero ocorrências de `[Sicoob][tls]` (interceptor de `BilletSSLConfig`, que loga depois de
  toda resposta HTTP do `billetRestTemplate`).

Como essas duas classes só existem dentro dos beans `bbRestTemplate`/`billetRestTemplate` e
logam incondicionalmente, a ausência total dos dois — enquanto `SicoobToken`/
`BBExtratoClient` comprovadamente executaram a chamada (é o log de erro deles que aparece) —
significa que **a conexão real não passou pelo `SSLContext` configurado nesses beans**.
Em vez disso, o handshake usou um `X509KeyManager` sem nenhum certificado carregado
(`SunX509KeyManagerImpl` "vazio").

### 4. O que muda com isso

| | Seção 2 (Railway, hipótese antiga) | Seção 3 (EC2, evidência direta) |
|---|---|---|
| Servidor pede certificado? | Suposto que não (sem log `[mTLS]`) | **Confirmado que sim** (`CertificateRequest` capturado) |
| Causa suposta | WAF/borda decide por IP não negociar mTLS | App não está usando o `RestTemplate`/`SSLContext` certo |
| Camada do problema | Rede/infra externa (BB/Sicoob) | Aplicação (Spring/Java, em runtime) |
| Ação possível | Nenhuma (dependia do lado do banco) | Instrumentar/depurar os beans `bbRestTemplate`/`billetRestTemplate` |

O código de `BBSSLConfig`/`BilletSSLConfig`/`LoggingX509KeyManager`, os `@Qualifier`
corretos em `BBToken`/`BBExtratoClient`/`SicoobToken`/`BilletHttpClient`, e a ausência de
qualquer `RestTemplate` genérico alcançável a partir do fluxo BB/Sicoob já foram conferidos
por leitura de código nesta investigação e continuam corretos hoje — o problema não está em
"o que o código deveria fazer", está em por que, em runtime, esse caminho não é o que
realmente executa a chamada.

---

## Notas de transparência (fora do escopo direto do mTLS)

- `BB_BASIC` no `.env` tem padding base64 inválido (achado na seção 1) — revisar a origem
  desse valor separadamente.
- As mudanças que geraram os logs da seção 2 (`EgressIpLogger`, logging em
  `SicoobToken`/`BilletHttpClient`, interceptor `[Sicoob][tls]`) foram editadas nesta
  investigação mas apareceram commitadas e *pushadas* para `origin/prod`
  (`82f6a34`/`bdd4e06`) sem `git commit`/`git push` explícito nesta sessão — vale confirmar
  se existe hook/automação de auto-commit+push configurado no ambiente, já que isso publica
  direto em produção sem revisão manual no meio do caminho.
