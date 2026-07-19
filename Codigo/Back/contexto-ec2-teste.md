# Contexto — EC2 de teste (Hortifruti Backend)

## Objetivo do teste

Determinar se as falhas de mTLS (Sicoob/BB) que ocorriam no ambiente PaaS (Railway/Render) eram causadas pela infraestrutura de saída de rede desses provedores, ou se o problema estava em outro lugar (ex: configuração do certificado no código).

**Resultado do teste:** o mesmo erro (`403 FORBIDDEN — "Certificado digital é obrigatório para este recurso"`) ocorreu também na EC2, uma VM Linux "crua", sem nenhuma camada de abstração de rede do Railway/Render. Isso **descarta o ambiente PaaS como causa raiz** — o problema está em como a aplicação está montando/enviando o certificado no handshake mTLS, não em onde ela está hospedada.

---

## Infraestrutura criada

- **Provedor:** AWS EC2
- **Região:** us-east-2 (Ohio)
- **Instância:** `i-07b3190bfc9618d76`
- **Tipo:** t3.micro (Free Tier)
- **AMI:** Ubuntu Server 24.04 LTS (`ami-0ea1cddefe0c4aed5`)
- **IP público:** `18.217.146.147`
- **Key pair:** `hortifruti-key.pem` (salvo em `~/Downloads` no Mac)
- **Security Group:** `launch-wizard-1`, com portas liberadas: 22 (SSH), 80 (HTTP), 443 (HTTPS), 8080 (API), todas com origem `0.0.0.0/0`

## Acesso via SSH

```bash
cd ~/Downloads
ssh -i hortifruti-key.pem ubuntu@18.217.146.147
```

## Software instalado na EC2

- **Docker** (`docker.io` + `docker-compose-v2`, via apt)
- Usuário `ubuntu` adicionado ao grupo `docker` (permite rodar `docker` sem `sudo`)
- **Caddy** (servidor web/proxy reverso com HTTPS automático via Let's Encrypt)

## Estrutura do projeto na EC2

Repositório clonado em `~/hortifruti` (branch usada para testes locais, não necessariamente `prod`):

```
~/hortifruti/Codigo/Back/
├── Dockerfile
├── docker-compose.yml
├── .env          (enviado manualmente via scp, não versionado)
├── pom.xml
├── src/
```

## Banco de dados

**Não usa o MySQL local do `docker-compose.yml`.** As credenciais no `.env` apontam para um banco externo já existente no Railway (`shuttle.proxy.rlwy.net:24604/railway`). Por isso a aplicação sobe **sem** o compose, direto via Docker:

```bash
docker build -t hortifruti-api .
docker run -d --name hortifruti-api --env-file .env -p 8080:8080 hortifruti-api
```

> Nota: o `docker-compose.yml` tem o serviço `backend` atrás de um profile `"full"` — só sobe com `docker compose --profile full up -d --build`. Como o banco já é externo, esse fluxo não está sendo usado neste teste.

## Comandos de manutenção do container

```bash
# Ver logs em tempo real (Ctrl+C para sair, não mata o container)
docker logs -f hortifruti-api

# Ver só as últimas N linhas
docker logs --tail 100 hortifruti-api

# Parar e remover para rebuildar com código novo
docker stop hortifruti-api
docker rm hortifruti-api

# Rebuild + subir de novo
cd ~/hortifruti/Codigo/Back
docker build -t hortifruti-api .
docker run -d --name hortifruti-api --env-file .env -p 8080:8080 hortifruti-api
```

## Envio de arquivos do Mac para a EC2

Sempre a partir do terminal **local** (Mac), nunca dentro da sessão SSH:

```bash
cd ~/Downloads
scp -i hortifruti-key.pem hortifruti/Codigo/Back/<ARQUIVO> ubuntu@18.217.146.147:~/hortifruti/Codigo/Back/
```

Usado até agora para: `.env`, `Dockerfile`.

## HTTPS via Caddy (para o frontend na Vercel acessar a API)

Como a EC2 não tem HTTPS nativo (diferente de Railway/Render, que fornecem isso automaticamente) e o frontend roda em `https://` na Vercel — o navegador bloqueia chamadas para `http://` por mixed content —, foi configurado um proxy reverso com certificado SSL automático via **nip.io** (serviço de DNS gratuito que resolve `SEU-IP.nip.io` para o próprio IP) + Let's Encrypt.

**Domínio de teste gerado:** `https://18-217-146-147.nip.io` → proxy reverso para `localhost:8080` (a API).

Configuração em `/etc/caddy/Caddyfile`:

```
18-217-146-147.nip.io {
    reverse_proxy localhost:8080
}
```

Comandos:

```bash
# Editar/sobrescrever o Caddyfile
sudo tee /etc/caddy/Caddyfile > /dev/null << 'EOF'
18-217-146-147.nip.io {
    reverse_proxy localhost:8080
}
EOF

# Aplicar mudanças
sudo systemctl restart caddy
sudo systemctl status caddy

# Ver logs do Caddy
sudo journalctl -u caddy -f
```

Certificado emitido com sucesso pelo Let's Encrypt (confirmado no log: `"certificate obtained successfully"`).

## URL para configurar no frontend (Vercel)

```
BACKEND_URL=https://18-217-146-147.nip.io
```

> Pendência: ainda não confirmado o teste ponta a ponta do frontend na Vercel batendo nessa URL.

---

## Próximos passos / pendências

1. Investigar `diagnostico-mtls.md` no repositório — já existe documentação de uma investigação anterior sobre o mTLS.
2. Confirmar como o `SicoobToken.java` monta o `RestTemplate`/`SSLContext` para o handshake mTLS — provável causa raiz do 403.
3. Testar o Dockerfile atualizado (alteração feita localmente, ainda pendente de rebuild na EC2 no momento deste registro).
4. Validar chamada real do frontend (Vercel) → backend (EC2) via HTTPS.

---

## Notas de custo

- Instância `t3.micro` está dentro do Free Tier da AWS (12 meses), mas vale confirmar em **Billing → Free Tier** se a conta ainda está dentro do período gratuito.
- Essa é uma instância de **teste/diagnóstico**, não de produção — considerar desligar (`Stop` ou `Terminate`) quando o teste for concluído, para evitar custos residuais.
