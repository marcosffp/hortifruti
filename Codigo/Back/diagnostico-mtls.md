<!-- Título: Diagnóstico de Handshake mTLS — BB e Sicoob -->

# Diagnóstico do Handshake mTLS (BB e Sicoob)

**Metodologia.** Nenhuma linha do repositório foi alterada. Para responder "a JVM realmente
envia o certificado?" sem depender de login de usuário (o endpoint `/finance/bank-balance`
exige `ROLE_MANAGER`) e sem subir o Spring Boot inteiro, foi escrito um programa Java
standalone (fora do repo, em `/tmp`) que **reproduz byte a byte** o código de
`BBSSLConfig.java` e `BilletSSLConfig.java`: mesmo `KeyStore` PKCS12, mesmo
`KeyManagerFactory`, mesmo `LoggingX509KeyManager` (a própria classe do repo, carregada via
classpath), mesmo `PoolingHttpClientConnectionManager`/`ClientTlsStrategyBuilder`. Só então
ele chama, com `-Djavax.net.debug=ssl:handshake:verbose`, os endpoints reais de produção do
BB (`oauth.bb.com.br`, `extratos.mtls.api.bb.com.br`) e do Sicoob (`auth.sicoob.com.br`,
`api.sicoob.com.br`), usando o mesmo `DOCUMENT_PFX`/`PASSWORD_PFX` do `.env` local.

Isso foi executado **localmente (macOS)**, o mesmo ambiente onde o usuário já relatou que a
aplicação funciona. Não há, ainda, logs equivalentes de Railway/Render com
`javax.net.debug` ativo — isso é necessário para a comparação Local × Nuvem (seção 4) e
está descrito no final.

---

## 1. Sequência cronológica completa (5 conexões TLS observadas)

| # | Peer | CertificateRequest? | Certificate (cliente) enviado? | Resultado do handshake TLS | Resultado da chamada HTTP |
|---|------|---|---|---|---|
| 1 | `oauth.bb.com.br:443` (token BB) | **Não** | Não (não pedido) | OK (TLSv1.3) | `200` — token obtido |
| 2 | `extratos.mtls.api.bb.com.br:443` (extrato BB) | **Sim** | **Sim** | OK (TLSv1.3) | `400` — erro de validação de parâmetro (`page-size` do teste, não relacionado a TLS/cert) |
| 3 | `auth.sicoob.com.br:443` (token Sicoob) | **Sim** | **Sim** | OK (TLSv1.3) | `200` — token obtido |
| 4 | `api.sicoob.com.br:443` (API Sicoob, 1ª tentativa) | **Sim** | **Sim** | Handshake completo, depois `Connection reset` lendo a resposta HTTP | Exceção de rede, sem resposta HTTP |
| 5 | `api.sicoob.com.br:443` (API Sicoob, retry automático do HttpClient5) | **Sim** | **Sim** | Idêntico à #4 | Exceção de rede, sem resposta HTTP |

Carimbos de tempo (mesma execução, `2026-07-18`):

```
23:20:18.404  ClientHello  -> oauth.bb.com.br            (conexão #1)
23:20:18.638  ServerHello  -> Negotiated protocol version: TLSv1.3
23:20:18.800  CertificateVerify (do SERVIDOR)
23:20:18.801  Finished (servidor)
23:20:18.802  Finished (cliente)
   [nenhum CertificateRequest nesta conexão]

23:20:20.298  ClientHello  -> extratos.mtls.api.bb.com.br (conexão #2)
23:20:20.788  ServerHello  -> Negotiated protocol version: TLSv1.3
23:20:20.795  CertificateRequest (SERVIDOR PEDE cert do cliente)
23:20:20.918  Certificate (do servidor)
23:20:20.931  CertificateVerify (do servidor)
23:20:20.931  Finished (servidor)
23:20:20.934  [mTLS:BB] alias escolhido='hortifruti santa luzia ltda:...'
23:20:20.936  Certificate (CLIENTE envia certificado)
23:20:20.948  CertificateVerify (CLIENTE prova posse da chave privada)
23:20:20.949  Finished (cliente)
   -> handshake concluído; HTTP 400 de validação de negócio

23:20:22.042  ClientHello  -> auth.sicoob.com.br          (conexão #3)
23:20:22.501  ServerHello  -> TLSv1.3
23:20:22.509  CertificateRequest
23:20:22.631  Certificate (servidor)
23:20:22.637  CertificateVerify / Finished (servidor)
23:20:22.638  [mTLS:Sicoob] alias escolhido='hortifruti santa luzia ltda:...'
23:20:22.638  Certificate (CLIENTE)
23:20:22.649  CertificateVerify / Finished (cliente)
   -> handshake concluído; HTTP 200, token obtido

23:20:23.688  ClientHello  -> api.sicoob.com.br            (conexão #4)
23:20:24.114  ServerHello  -> TLSv1.3
23:20:24.121  CertificateRequest
23:20:24.242  Certificate (servidor)
23:20:24.245  [mTLS:Sicoob] alias escolhido='hortifruti santa luzia ltda:...'
23:20:24.246  Certificate (CLIENTE)
23:20:24.255  CertificateVerify / Finished (cliente)
23:20:24.256  WRITE application_data (requisição HTTP enviada)
23:20:24.554  SocketException: Connection reset (lendo a resposta)
23:20:24.555  Fatal (UNEXPECTED_MESSAGE): Connection reset
23:20:24.557  HttpRequestRetryExec: re-executando (exec count 2)

23:20:24.693  ClientHello  -> api.sicoob.com.br (retry, tenta resumir sessão) (conexão #5)
23:20:25.122  ServerHello  -> TLSv1.3
23:20:25.125  CertificateRequest
23:20:25.254  [mTLS:Sicoob] alias escolhido='hortifruti santa luzia ltda:...'
23:20:25.259  Certificate (CLIENTE) / CertificateVerify / Finished
23:20:25.560  Fatal (UNEXPECTED_MESSAGE): Connection reset (de novo)
   -> exceção final propagada para a aplicação: java.net.SocketException: Connection reset
```

---

## 2. O servidor envia `CertificateRequest`?

**Depende do endpoint — e isso já é uma evidência importante.**

- `oauth.bb.com.br` (token OAuth do BB): **não** envia `CertificateRequest`. O handshake
  completa com autenticação apenas do servidor. Evidência: entre `ServerHello` (18.638) e
  `Finished` (18.802) da conexão #1 não existe nenhuma linha `CertificateRequest.java`.
- `extratos.mtls.api.bb.com.br` (extrato, o endpoint que o nome já indica ser mTLS): **sim**,
  em `23:20:20.795`:
  ```
  javax.net.ssl|DEBUG|30|main|...|CertificateRequest.java:989|Consuming CertificateRequest handshake message (
  "CertificateRequest": {
    "certificate_request_context": "",
    "extensions": [
      "signature_algorithms (13)": { ... }
    ]
  }
  )
  ```
- `auth.sicoob.com.br` e `api.sicoob.com.br`: **sim**, em ambos (`22:20:22.509` e
  `24.121`/`25.125`), com o mesmo formato de extensão `signature_algorithms`.

Conclusão da pergunta 2: BB só exige mTLS no endpoint de Extratos (o oauth token é TLS
comum); o Sicoob exige mTLS tanto no token quanto na API.

---

## 3. A JVM envia um certificado cliente?

**Sim, nas 4 conexões em que o servidor pediu.** Evidência direta — mensagem
`Produced client Certificate message`, ex. para o BB:

```
javax.net.ssl|DEBUG|30|main|2026-07-18 23:20:20.936 GMT-03:00|CertificateMessage.java:1071|Produced client Certificate message (
"Certificate": {
  "certificate_request_context": "",
  "certificate_list": [
  {
    "certificate" : {
      "version"            : "v3",
      "serial number"      : "6b:d5:75:8b:b1:1f:f9:bd",
      "signature algorithm": "SHA256withRSA",
      "issuer"             : "CN=AC SAFEWEB RFB v5, OU=Secretaria da Receita Federal do Brasil - RFB, O=ICP-Brasil, C=BR",
      "not before"         : "2026-05-06 16:35:31.000 GMT-03:00",
      "not  after"         : "2027-05-06 16:35:31.000 GMT-03:00",
      "subject"            : "CN=HORTIFRUTI SANTA LUZIA LTDA:27540906000155, OU=videoconferencia, OU=31014048000182, OU=RFB e-CNPJ A1, OU=Secretaria da Receita Federal do Brasil - RFB, L=SANTA LUZIA, ST=MG, O=ICP-Brasil, C=BR",
      "subject public key" : "RSA",
      ...
```

E, cruzando com o par de mensagens de `CertificateVerify` do cliente logo em seguida
(`23:20:20.948`), o handshake não fica só no "enviei o certificado" — o servidor **valida
criptograficamente** que a JVM possui a chave privada correspondente (é isso que
`CertificateVerify` prova) e só então emite `Finished`. Ou seja: nas 4 conexões mTLS
observadas localmente, o certificado foi enviado **e aceito no nível TLS**.

O mesmo bloco de certificado (mesmo *serial number*, mesmo *subject*) aparece nas 4
conexões (BB extrato, Sicoob auth, Sicoob API ×2) — é sempre o único certificado do keystore.

---

## 4. Qual certificado foi enviado?

| Campo | Valor |
|---|---|
| Alias na keystore | `hortifruti santa luzia ltda:27540906000155` |
| Subject | `CN=HORTIFRUTI SANTA LUZIA LTDA:27540906000155, OU=videoconferencia, OU=31014048000182, OU=RFB e-CNPJ A1, OU=Secretaria da Receita Federal do Brasil - RFB, L=SANTA LUZIA, ST=MG, O=ICP-Brasil, C=BR` |
| Issuer | `CN=AC SAFEWEB RFB v5, OU=Secretaria da Receita Federal do Brasil - RFB, O=ICP-Brasil, C=BR` |
| Serial number | `6b:d5:75:8b:b1:1f:f9:bd` |
| Validade | `2026-05-06 16:35:31 GMT-03:00` até `2027-05-06 16:35:31 GMT-03:00` (**válido** na data do teste, 2026-07-18) |
| Tipo de chave | RSA |
| Tamanho da cadeia enviada | 1 (apenas o certificado da empresa — a keystore PKCS12 não contém a cadeia intermediária/raiz da AC SAFEWEB) |

O `chainLength=1` é visível também no log de decisão do `LoggingX509KeyManager`:
```
23:20:20.934 [main] INFO ...LoggingX509KeyManager -- [mTLS:BB] peer=extratos.mtls.api.bb.com.br/170.66.196.140:443 alias escolhido='hortifruti santa luzia ltda:27540906000155' chainLength=1 subject='CN=HORTIFRUTI SANTA LUZIA LTDA:27540906000155,...' issuersSolicitados=[(nenhum solicitado pelo servidor)]
```
`issuersSolicitados=[(nenhum solicitado pelo servidor)]` é esperado: no `CertificateRequest`
do TLS 1.3 capturado, o servidor só populou a extensão `signature_algorithms`, sem
`certificate_authorities` — então o array de `issuers` que a JVM repassa ao KeyManager vem
vazio por design do protocolo, não por falha de configuração.

---

## 5. O KeyManager participa?

Sim. `chooseClientAlias()` (não `chooseEngineClientAlias()` — o `HttpClient5` usado é o
*classic* baseado em `SSLSocket`, não em `SSLEngine`/NIO) foi chamado **exatamente 4 vezes**,
uma para cada conexão em que o servidor mandou `CertificateRequest`:

1. `23:20:20.934` — peer `extratos.mtls.api.bb.com.br` → alias escolhido
2. `23:20:22.638` — peer `auth.sicoob.com.br` → alias escolhido
3. `23:20:24.245` — peer `api.sicoob.com.br` (1ª tentativa) → alias escolhido
4. `23:20:25.254` — peer `api.sicoob.com.br` (retry) → alias escolhido

Nas 4 vezes o mesmo (e único) alias da keystore foi devolvido — nunca `null`. Isso é
importante porque a própria classe `LoggingX509KeyManager` já foi desenhada para logar em
`ERROR` exatamente o cenário problemático ("NENHUM alias selecionado; o certificado de
cliente NAO sera enviado nesta conexao") — esse log **não apareceu nenhuma vez** na
execução local. Na conexão do oauth do BB (sem `CertificateRequest`), o KeyManager
simplesmente não foi consultado — comportamento correto (não há por que escolher um alias
se ninguém pediu).

---

## 6. O `SSLContext` usa o `KeyManager` correto?

Sim, por construção — não há acoplamento indireto a verificar: `BBSSLConfig.bbRestTemplate()`
e `BilletSSLConfig.billetRestTemplate()` fazem, cada um, `SSLContext.getInstance("TLS")` e
`sslContext.init(keyManagers, null, null)` **dentro do mesmo método** que constrói o
`RestTemplate`. Não existe injeção de um `SSLContext` genérico vindo de outro lugar. O
`keyManagers` usado é sempre o retorno de `LoggingX509KeyManager.wrap(...)`, que por sua vez
embrulha o `KeyManagerFactory` inicializado com o `KeyStore` carregado do PFX. A prova em
runtime é justamente o log do item 5: o `LoggingX509KeyManager` (que só existe se o
`SSLContext` construído ali estiver realmente em uso) foi de fato invocado pelo motor TLS da
JVM durante as conexões reais.

## 7. O `RestTemplate` realmente usa esse `SSLContext`?

Confirmado por leitura de código (não é suposição):

- `BBSSLConfig.java:95-98` e `BilletSSLConfig.java:99-102`: constrói
  `HttpComponentsClientHttpRequestFactory`, chama `factory.setHttpClient(httpClient)` (o
  `CloseableHttpClient` que tem o `PoolingHttpClientConnectionManager` com o
  `TlsSocketStrategy` do `sslContext`), e retorna `new RestTemplate(factory)`.
- Quem consome esses beans usa `@Qualifier` explícito, sem ambiguidade:
  - `BBToken.java:39-40` e `BBExtratoClient.java:45-46` → `@Qualifier("bbRestTemplate")`
  - `SicoobToken.java:36-37` e `BilletHttpClient.java:28-29` → `@Qualifier("billetRestTemplate")`

Não há `@Primary` nem outro bean `RestTemplate` sem nome que possa ser injetado no lugar
por engano nesses 4 pontos.

## 8. Existe algum fallback?

Foram encontrados outros `new RestTemplate()` no projeto, mas nenhum deles é alcançável a
partir do fluxo BB/Sicoob:

```
config/FocusNfeApiClient.java:27      RestTemplate restTemplate = new RestTemplate();   (Focus NFe, não usa mTLS)
config/climate/OpenWeatherClient.java:54  RestTemplate restTemplate = new RestTemplate(); (OpenWeather)
config/email/RestTemplateConfig.java:12   @Bean RestTemplate restTemplate() { return new RestTemplate(); } (bean genérico sem nome)
service/freight/DistanceMatrixService.java:80  RestTemplate restTemplate = new RestTemplate(); (Distance Matrix / Google)
```

O único consumidor do bean genérico e não-qualificado de `RestTemplateConfig` que aparece
no grep é `WhatsAppService` — nada relacionado a BB/Sicoob. **Não existe fallback** que
faça uma chamada ao BB ou ao Sicoob usando um `RestTemplate` sem o `SSLContext` mTLS.

---

## 9. Comparação Local × Nuvem

**Ainda não é possível responder com evidência** — Railway e Render não têm
`javax.net.debug` habilitado hoje, então não existe log de handshake real de lá para
comparar (isso foi confirmado com você antes de eu prosseguir). O que dá para afirmar
agora é só o lado local, que serve de **baseline de "funcionando"**:

- Localmente, com as mesmas credenciais (`DOCUMENT_PFX`/`PASSWORD_PFX`) que a imagem Docker
  usa em produção, o certificado é carregado, o alias é escolhido, o certificado é enviado
  e aceito no handshake TLS tanto pelo BB quanto pelo Sicoob.
- Isso significa que o par certificado+senha em si é válido e a lógica de
  `BBSSLConfig`/`BilletSSLConfig` funciona corretamente **quando executada neste ambiente**.

Para fechar a comparação, é preciso repetir exatamente este teste (ou simplesmente rodar a
aplicação real) dentro do container do Railway/Render com o mesmo flag. Como isso ainda
não foi feito, qualquer afirmação sobre "o que acontece no Railway/Render no nível de TLS"
seria suposição — e a instrução foi explícita para não supor.

**Como habilitar sem tocar em código/infra de certificado** (só uma flag de JVM,
reversível): no serviço do Railway/Render, adicionar a variável de ambiente
`JAVA_TOOL_OPTIONS=-Djavax.net.debug=ssl:handshake` (ou, se o `Dockerfile`/`ENTRYPOINT`
monta o comando `java -jar`, adicionar o mesmo `-D` ali) e redeployar. No próximo boot, os
mesmos marcadores usados neste relatório (`ClientHello`, `CertificateRequest`,
`CertificateMessage.java:1071 Produced client Certificate`, `[mTLS:BB]`/`[mTLS:Sicoob]` do
`LoggingX509KeyManager`) vão aparecer nos logs do próprio Railway/Render, prontos para
comparar linha a linha com este documento.

---

## 10. Classificação do cenário

Nenhuma das 4 conexões mTLS observadas localmente se encaixa nos cenários A ou B —
**localmente o cenário é sempre C ou "sucesso completo"**:

- **BB (extrato)** → **Cenário C, mas com uma ressalva importante**: o certificado foi
  enviado e o handshake TLS terminou com sucesso; o BB só recusou a *requisição HTTP*
  (`400`, "quantidade de registros da página deve ser superior ao limite mínimo") — um erro
  de parâmetro do meu script de teste (usei `quantidadeRegistroPaginaSolicitacao=1`; o app
  real usa `120`), **não** o erro "Failed to verify certificate" relatado em produção. Ou
  seja: localmente eu não consegui *reproduzir* o sintoma de produção — o que aconteceu
  localmente foi outra coisa, e isso é uma pista, não um "tudo certo".
- **Sicoob (token e API)** → mTLS também completa (`Certificate`/`CertificateVerify`/
  `Finished` do cliente presentes nas 4 tentativas relevantes), token obtido com sucesso
  (`200`); mas a chamada à API de boletos teve a conexão resetada pelo servidor **depois**
  do handshake — **Cenário D** (comportamento diferente dos 3 descritos): certificado
  aceito no nível TLS, só que a conexão HTTP é interrompida depois, sem chegar a devolver
  um `403`/`"Certificado digital é obrigatório"` como relatado em produção.

**Conclusão objetiva, restrita ao que os logs mostram:** localmente, com o `.env` atual, o
`LoggingX509KeyManager` nunca loga "NENHUM alias selecionado" e a JVM sempre completa o
handshake mTLS (certificado enviado e validado) tanto para o BB quanto para o Sicoob. Os
sintomas exatos relatados em produção (`400`/"Failed to verify certificate" no BB e
`403`/"Certificado digital é obrigatório" no Sicoob) **não foram reproduzidos localmente**
com este teste — o que foi reproduzido localmente foi (a) um erro de validação de negócio
do BB não relacionado a TLS, e (b) um reset de conexão pós-handshake do Sicoob também sem
evidência de ser causado pelo certificado.

Isso desloca a hipótese mais provável **na ausência de dados de Railway/Render**: como o
mesmo par certificado+senha funciona e completa o handshake mTLS neste ambiente, a causa
mais provável do sintoma relatado em produção está em uma diferença de **ambiente de
execução do container** (Railway/Render) que faz o `LoggingX509KeyManager` devolver
`alias == null` ali — algo que só os logs de lá, com o mesmo flag `-Djavax.net.debug`,
podem confirmar. Sem esse dado, apontar a causa exata na nuvem seria suposição, o que foi
explicitamente pedido para evitar.

---

### Achado secundário (fora do escopo do mTLS, registrado por transparência)

Ao montar o payload de autenticação Basic do BB para este teste, percebi que
`BB_BASIC` no `.env` termina com **três** caracteres `=` de padding
(`...VFUzZlE===`), o que não é base64 válido (o padding correto para esse
comprimento seria só um `=`). Isso não impediu o teste (Python/Java decodificaram
mesmo assim de forma tolerante), e o handshake TLS em si é anterior/independente
desse header — mas é uma anomalia real nos dados, não uma suposição, e pode valer
a pena revisar a origem desse valor separadamente.
