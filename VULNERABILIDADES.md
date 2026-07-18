# Relatório de Vulnerabilidades — Sistema Hortifruti SL

**Data da varredura:** 2026-07-17
**Escopo:** Código completo de `Codigo/Back` (Java/Spring Boot) e `Codigo/Front` (Next.js/React/TypeScript).
**Método:** Revisão manual de código (leitura completa dos módulos de autenticação, autorização, configuração de rede/TLS, controllers, services e integrações externas). Não foram executados testes de intrusão ativos (pentest real) contra um servidor rodando — isto é uma revisão estática de código-fonte.

## Resumo executivo

Foram encontrados **4 problemas HIGH**, **3 MEDIUM** e **4 LOW/hardening**. Os mais graves permitem, hoje, que **qualquer pessoa sem login** baixe a base completa de clientes (nome, telefone, endereço, CPF/CNPJ, histórico de compras) e, combinando isso com uma segunda falha, envie boletos financeiros de qualquer cliente para um WhatsApp escolhido pelo atacante — sem autenticação nenhuma. Há também uma configuração que desliga a criptografia da conexão com o banco de dados em produção, o que é exatamente o cenário de "alguém capturando dados durante a comunicação com o banco" que você mencionou.

Nenhum segredo real (.env) foi encontrado commitado no git — isso está correto nos dois projetos.

**Prioridade de correção (faça nesta ordem):**

1. [BACK-1] Fechar o acesso público a `GET /clients/**`
2. [BACK-2] Remover a criação automática dos usuários `root/root` e `admin/admin`
3. [BACK-3] Proteger o webhook `/chatbot/webhook` contra chamadas forjadas
4. [BACK-4] Ativar TLS na conexão com o banco de dados (prod/hml)
5. [FRONT-1] Migrar o token JWT de `localStorage` para cookie `HttpOnly`
6. Demais itens (MEDIUM/LOW), conforme tempo disponível

---

## Backend (`Codigo/Back`)

### 🔴 BACK-1 — Base de clientes exposta sem autenticação

- **Severidade:** HIGH
- **Categoria:** `auth_bypass` / `info_disclosure`
- **Local:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/config/auth/SecurityConfig.java` (regra `permitAll()` para `GET /clients/**`)
- **Descrição:** Toda requisição `GET` para `/clients/**` é liberada sem exigir login, incluindo `GET /clients` (lista todos os clientes), `GET /clients/{id}`, busca por nome, resumo de compras etc. A resposta (`ClientResponse`) inclui nome, e-mail, telefone, endereço, **CPF/CNPJ** e valor total de compras.
- **Cenário de exploração:** Qualquer pessoa na internet roda `curl https://seu-dominio/clients` e recebe a lista completa de clientes com dados pessoais e fiscais, sem precisar de senha nem token.
- **Correção:** Remover essa liberação genérica; exigir autenticação (e provavelmente um papel como `EMPLOYEE`/`MANAGER`) em todos os `GET` de `/clients/**`, exceto uma rota específica que realmente precise ser pública (se existir alguma).

### 🔴 BACK-2 — Usuários administrativos padrão criados automaticamente

- **Severidade:** HIGH
- **Categoria:** `hardcoded_secret` / `weak_auth`
- **Local:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/config/UserInitializer.java` (linhas ~70-74)
- **Descrição:** Se a tabela de usuários estiver vazia (o que acontece naturalmente num banco novo/produção recém-criada), o sistema cria sozinho a conta `root`/senha `root` com papel **MANAGER** (o mais alto), e `admin`/senha `admin`.
- **Cenário de exploração:** Logo após o primeiro deploy, um atacante tenta login com `root`/`root` (credencial extremamente óbvia) e ganha acesso total ao sistema antes mesmo de você trocar a senha.
- **Correção:** Nunca semear credenciais reais e adivinháveis automaticamente em um ambiente que pode rodar em produção. Restringir essa criação ao perfil local/dev, ou gerar uma senha aleatória forte no primeiro boot e obrigar troca imediata.

### 🔴 BACK-3 — Webhook do chatbot (WhatsApp) sem verificação de origem, permite exfiltrar boletos de qualquer cliente

- **Severidade:** HIGH
- **Categoria:** `auth_bypass` / `idor` / `info_disclosure`
- **Local:**
  - `SecurityConfig.java` — `/chatbot/webhook` liberado (`permitAll()`)
  - `Codigo/Back/.../controller/notification/ChatbotController.java` — endpoint `POST /webhook` aceita qualquer corpo JSON
  - `Codigo/Back/.../service/notification/ChatbotService.java` — usa o campo `from` (número de telefone) enviado no próprio corpo da requisição, sem validar que a chamada realmente veio do provedor UltraMsg
  - Busca o cliente só pelo CPF/CNPJ informado na "conversa" e envia os boletos em aberto para o número de telefone do payload
- **Descrição:** Não há verificação de assinatura/segredo compartilhado confirmando que a chamada ao webhook realmente veio da UltraMsg. Um atacante pode simular a conversa do bot diretamente via HTTP.
- **Cenário de exploração:** Combinando com o BACK-1 (que vaza todos os CPF/CNPJ de clientes de graça), um atacante forja requisições ao `/chatbot/webhook` fingindo ser cada cliente, colocando o próprio número de WhatsApp como destino, e o sistema envia os boletos (valores, vencimentos, boleto PDF) desse cliente diretamente para o atacante — tudo sem login.
- **Correção:** Validar autenticidade do webhook (segredo compartilhado na URL/header, ou lista de IPs permitidos da UltraMsg) antes de processar qualquer payload; não confiar no campo `from` sem vínculo com uma sessão validada.

### 🔴 BACK-4 — Conexão com o banco de dados sem TLS em produção/homologação

- **Severidade:** HIGH
- **Categoria:** `transport_security` / risco de MITM
- **Local:**
  - `Codigo/Back/src/main/resources/application-prod.properties` — `useSSL=false&allowPublicKeyRetrieval=true`
  - `Codigo/Back/src/main/resources/application-hml.properties` — mesma configuração
- **Descrição:** Esta é exatamente a preocupação que você mencionou ("alguém pegar dados durante a comunicação com o banco"). A URL JDBC do MySQL desativa explicitamente TLS (`useSSL=false`) tanto em produção quanto em homologação, e ainda ativa `allowPublicKeyRetrieval=true`, que permite troca de chave RSA sem autenticação prévia sobre esse mesmo canal não criptografado.
- **Cenário de exploração:** Qualquer ponto capaz de observar o tráfego de rede entre o servidor da aplicação e o servidor do banco (rede compartilhada, roteador comprometido, proxy malicioso) consegue ler em texto puro as credenciais do banco e todos os dados que trafegam — dados de clientes, financeiro, hashes de senha da tabela de usuários — ou, de forma mais ativa, interceptar a conexão (MITM) aproveitando o `allowPublicKeyRetrieval`.
- **Correção:** Trocar para `useSSL=true&requireSSL=true&verifyServerCertificate=true` (ou `sslMode=VERIFY_IDENTITY` em conectores mais novos) em prod/hml, e remover `allowPublicKeyRetrieval=true` assim que o TLS estiver ativo.

### 🟠 BACK-5 — Verificação de certificado enfraquecida globalmente na integração Sicoob

- **Severidade:** MEDIUM
- **Categoria:** `weak_crypto`
- **Local:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/config/billet/SSLTrustAll.java` (linhas ~54-66)
- **Descrição:** O código altera `HttpsURLConnection.setDefaultSSLSocketFactory`/`setDefaultHostnameVerifier` de forma **global na JVM** (afeta toda conexão HTTPS do processo, não só a do Sicoob), e o verificador de hostname usa `hostname.endsWith(sicoobDomain)`, um match "termina com" ingênuo — um certificado para um domínio forjado tipo `evil-sicoob.com.br` também passaria nessa checagem.
- **Correção:** Restringir essa customização de TLS a um `HttpClient`/`RestTemplate` dedicado só para chamadas ao Sicoob (o projeto já faz isso corretamente em `BilletSSLConfig.java` — usar o mesmo padrão aqui) e trocar o `endsWith` por comparação exata de domínio/subdomínio.

### 🟠 BACK-6 — Mensagens de erro internas/de terceiros repassadas direto na resposta da API

- **Severidade:** MEDIUM
- **Categoria:** `info_disclosure`
- **Local:** `BilletController.java`, `InvoiceController.java`, `NotificationController.java` — vários blocos `catch (Exception e)` que devolvem `e.getMessage()` (inclusive respostas cruas de erro do Sicoob/FocusNFe) direto no corpo da resposta ao invés de passar pelo `GlobalExceptionHandler`.
- **Descrição:** Esses endpoints exigem login, então o vazamento fica restrito a usuários já autenticados, mas ainda expõe detalhes internos (respostas de APIs de terceiros, mensagens de exceção) que podem ajudar um atacante a mapear o sistema.
- **Correção:** Fazer esses `catch` usarem o mesmo padrão genérico do `GlobalExceptionHandler`, mantendo o detalhe completo só nos logs do servidor.

### 🟡 Observações LOW / boas práticas (backend)

- **Sem revogação de token:** o JWT (`TokenConfiguration.java`) expira só por tempo (padrão 60 min) e não há blacklist/logout real — um token roubado continua válido até expirar naturalmente.
- **CSRF desabilitado:** correto, já que a aplicação é stateless e usa `Authorization: Bearer` (não usa cookie de sessão) — **não é uma falha**.
- **SQL/JPQL Injection:** todas as `@Query` verificadas usam parâmetros vinculados (`:param`); a única query nativa encontrada é estática, sem input do usuário — **nenhum problema encontrado**.
- **CORS:** configurado corretamente, restrito às URLs de front/back definidas em config (não usa wildcard `*`) — só é preciso confirmar que essas propriedades apontam para as URLs reais de produção no deploy final, e não para `localhost`.
- **Hash de senha:** usa BCrypt corretamente.

---

## Frontend (`Codigo/Front`)

### 🔴 FRONT-1 — Token JWT guardado em `localStorage`

- **Severidade:** HIGH
- **Categoria:** `insecure_token_storage`
- **Local:** `Codigo/Front/src/services/authService.ts` (linhas ~55-56, 86, 93, 98), também lido em `acessoService.ts` e `backupService.ts`
- **Descrição:** O token JWT retornado no login é salvo em `localStorage` e reenviado como header `Bearer` em toda requisição. `localStorage` pode ser lido por **qualquer script JavaScript** que rode na página.
- **Cenário de exploração:** Se no futuro surgir uma falha de XSS em qualquer lugar do app (uma dependência comprometida, um novo componente que renderize HTML sem sanitizar, etc.), basta `localStorage.getItem("auth_token")` dentro do script injetado para roubar uma sessão válida e completa — sem precisar quebrar nada no backend. Hoje não foi encontrado um XSS ativo no código (ver observações abaixo), mas esse é o vetor que transformaria qualquer XSS futuro em roubo total de conta.
- **Correção:** Migrar o token para um cookie `HttpOnly`, `Secure`, `SameSite=Strict/Lax` definido pelo próprio backend (exige ajuste no backend também). Enquanto isso não é feito, uma Content Security Policy (ver FRONT-3) reduz o risco.

### 🟠 FRONT-2 — URL do backend cai silenciosamente para `http://localhost:8080` se a variável de ambiente não for configurada

- **Severidade:** MEDIUM
- **Categoria:** `mixed_content` / configuração insegura por padrão
- **Local:** Padrão repetido em mais de 20 arquivos em `Codigo/Front/src/services/*` e alguns hooks/componentes: `process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"`
- **Descrição:** Essa variável é embutida no bundle já na hora do build. Não existe checagem garantindo que a URL final seja `https://`, nem um `.env.example` no projeto Front documentando essa variável.
- **Cenário de exploração:** Não é algo que um atacante dispara diretamente, mas é uma armadilha de deploy real: se o pipeline de build esquecer de configurar `NEXT_PUBLIC_API_URL`, o site em produção passa a tentar enviar login/senha e tokens para `http://localhost:8080` (texto puro) sem nenhum erro visível.
- **Correção:** Fazer o build falhar se `NEXT_PUBLIC_API_URL` não estiver definida ou não começar com `https://`; centralizar essa constante em um único lugar em vez de repetir em 20 arquivos; adicionar um `.env.example` no Front.

### 🟡 FRONT-3 — Nenhum header de segurança configurado (CSP/HSTS/X-Frame-Options)

- **Severidade:** LOW
- **Categoria:** hardening ausente
- **Local:** `Codigo/Front/next.config.ts` (praticamente vazio)
- **Descrição:** Não isso não é uma falha explorável por si só, mas reforça o risco do FRONT-1: com uma CSP básica (`default-src 'self'`), mesmo se aparecer um XSS futuro, o script injetado teria muito menos capacidade de agir. Também não há proteção contra clickjacking nas páginas com ações sensíveis (boletos, acesso, admin).
- **Correção:** Adicionar um bloco `headers()` no `next.config.ts` com pelo menos `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e uma CSP básica.

### 🟡 FRONT-4 — Arquivo de backend commitado por engano dentro do repositório do Front

- **Severidade:** LOW
- **Categoria:** organização/higiene do repositório
- **Local:** `Codigo/Front/package com.hortifruti.sl.hortifruti.java`
- **Descrição:** Um arquivo Java do backend (`GoogleDriveService`, referenciando o caminho esperado `/credentials/drive_credentials.json` e escopo total do Google Drive) está commitado dentro da pasta do Front por engano. Não expõe uma chave real, mas ajuda um invasor que já tenha algum outro acesso a saber onde procurar as credenciais reais de backup no Drive.
- **Correção:** Remover esse arquivo do diretório/histórico do Front; confirmar que o `drive_credentials.json` real não está commitado em nenhum lugar do backend também.

### ✅ Itens revisados e considerados SEM problema (frontend)

- Nenhum uso perigoso de `dangerouslySetInnerHTML` (só CSS estático hardcoded, sem input do usuário), nenhum `eval`, `innerHTML` ou `document.write`.
- Nenhuma chave/API key hardcoded no código do Front (integrações com OpenStreetMap/OSRM/ViaCEP são todas sem chave).
- Nenhuma variável `NEXT_PUBLIC_*` sensível exposta (só URL do backend, links de planilha pública e e-mail de contato).
- Token sempre enviado via header `Authorization: Bearer`, nunca em query string da URL.
- Nenhum `.env` real com segredo foi commitado no histórico do git (`.gitignore` cobre corretamente `.env*`).

---

## Perguntas frequentes (para você, dono do sistema)

**"Alguém pode invadir meu banco de dados pela rede?"**
O ponto mais direto é o BACK-4 (TLS desligado no MySQL). Corrigir isso é o que mais se conecta à sua preocupação sobre "vazamento durante a comunicação com o banco".

**"Meus dados de clientes estão seguros hoje, sem eu mudar nada?"**
Não — o BACK-1 permite que qualquer pessoa na internet baixe a lista completa de clientes agora mesmo, sem senha. Esse é o item mais urgente de todos, mais até que o do banco de dados, porque não exige nenhuma posição privilegiada de rede — só uma URL.

**"O sistema pode ser invadido por alguém adivinhando senha?"**
Sim, via BACK-2 — as contas `root/root` e `admin/admin` são criadas automaticamente se o banco estiver vazio (situação normal em um primeiro deploy para o cliente).

Nenhum desses 4 itens HIGH exige conhecimento avançado de hacking — são todos exploráveis com ferramentas básicas (`curl`, Postman). Recomendo fortemente corrigi-los antes de colocar o sistema na mão do cliente.
