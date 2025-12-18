# Hortifruti SL

Este trabalho aborda o desenvolvimento de um sistema de gestão para o Hortifruti Santa Luzia LTDA, focado em automatizar processos manuais críticos: conciliação bancária via extração de dados de PDF e agrupamento de vendas por cliente. O software visa eliminar tarefas repetitivas, centralizar informações e fornecer controle operacional, modernizando a gestão do negócio e promovendo eficiência.

## Alunos integrantes da equipe

* Bernado Souza Alvim
* Carlos José Gomes Batista Figueiredo 
* Gabriela Alvarenga Cardoso
* Marcos Alberto Ferreira Pinto
* Mateus Araujo Santos
* Rafael Ganascini de Moura

## Professores responsáveis

* Soraia Lúcia da Silva
* Lucila Ishitani

## Sprints do Projeto

### Sprint 1 - Planejamento e Definições Iniciais

Nesta sprint inicial, a equipe realizou o Kick Off com o cliente Vlanney Gualberto para compreender as necessidades do Hortifruti Santa Luzia LTDA. Foram definidos os requisitos funcionais e não funcionais do projeto, além da escolha das tecnologias a serem utilizadas: Java com Spring Boot para o backend e Next.js com TypeScript para o frontend. Também foram elaborados documentos essenciais como a Ata de Acordo, Termo de Sigilo e Confidencialidade, e a Procuração NIT.

A organização inicial do repositório GitHub foi estabelecida, criando a estrutura de pastas para Artefatos, Código, Divulgação e Documentação. A equipe preparou os slides da primeira apresentação e iniciou a documentação do projeto no Overleaf, estabelecendo as bases para o desenvolvimento nas sprints seguintes.

### Sprint 2 - Fundamentos do Sistema

Durante a Sprint 2, foram criados os diagramas fundamentais do sistema: Diagrama de Entidade-Relacionamento (ER) e Diagrama de Caso de Uso. Os protótipos de telas foram desenvolvidos para validação com o cliente. A implementação iniciou com funcionalidades essenciais como o cadastro e gerenciamento de usuários (RF001), cadastro e edição de clientes (RF002-RF004), e a tela de login junto com a home page.

Também foram implementadas funcionalidades críticas para o negócio: upload e visualização de extratos bancários (RF005-RF006), cálculo e visualização de frete (RF007-RF008), cadastro de produtos (RF009) e o sistema de recomendação de compras (RF010). Esta sprint estabeleceu a base operacional do sistema com os módulos de usuários, clientes e a estrutura inicial de lançamentos financeiros.

### Sprint 3 - Gestão de Compras e Dashboard

A Sprint 3 focou na expansão das funcionalidades de gestão comercial. Foi implementado o upload de notas de compra (RF014), listagem de arquivos (RF015) e a seleção de clientes com configuração de período (RF016-RF017). O módulo de visualização de informações do cliente (RF018) permitiu centralizar dados importantes para análise.

O dashboard do Hortifruti (RF024) foi desenvolvido, oferecendo uma visão consolidada das operações do negócio. A sprint também incluiu a conclusão do cadastro de produtos e visualização de recomendações de compra. Os diagramas de Caso de Uso, ER e Lógico foram atualizados para refletir as novas funcionalidades implementadas.

### Sprint 4 - Boletos e Notificações

Esta sprint concentrou-se no sistema de boletos e comunicação com clientes. Foram implementados filtros por tipo/categoria (RF011), busca de lançamentos (RF012), envio de arquivos (RF019) e personalização de mensagens (RF020). O sistema de notificações ganhou canal de envio configurável (RF021) e alertas automáticos de vencimento (RF022).

O módulo de agrupamento de vendas foi finalizado com confirmação e cancelamento de agrupamentos (RF023, RF025). A geração de boletos (RF026) com download em PDF (RF027), baixa de boleto (RF028) e consulta de boletos pendentes via WhatsApp (RF029-RF030) completaram o ciclo financeiro. Requisitos não funcionais como verificação automatizada de vencimentos, backup automatizado e monitoramento de capacidade também foram implementados.

### Sprint 5 - Documentação e Entrega Final

A Sprint 5 foi dedicada à finalização do projeto e preparação para entrega. A documentação completa foi atualizada no Overleaf, incluindo metodologia, resultados obtidos, conclusão e referências bibliográficas. A ata da reunião final com o cliente foi preparada para formalizar a entrega do sistema.

A equipe elaborou os slides da apresentação final e conduziu avaliação pelos usuários através de questionário para validar a aceitação do sistema. O resumo para a Mostra foi preparado, o vídeo demonstrativo foi criado, e a organização final do GitHub Classroom foi realizada para garantir a entrega adequada de todos os artefatos do projeto.

## Instruções de utilização (Ambiente Local)

### Pré-requisitos

- **Java 21** (JDK)
- **Maven 3.8+**
- **Node.js 20+**
- **MySQL 8.0+**
- **npm**

### Backend (Spring Boot)

1. **Navegue até a pasta do backend:**
   ```bash
   cd Codigo/Back
   ```

2. **Configure as variáveis de ambiente** criando um arquivo `.env` na raiz do backend com as seguintes variáveis:

   ```env
   # ==============================
   # 🔹 CONFIG. DE URLS
   # ==============================
   FRONTEND_URL=http://localhost:3000
   BACKEND_URL=http://localhost:8080

   # ==============================
   # 🔹 CONFIG. DE BANCO DE DADOS
   # ==============================
   MYSQLHOST=localhost
   MYSQLPORT=3306
   MYSQLDATABASE=hortifruti_sl
   MYSQLUSER=seu_usuario
   MYSQLPASSWORD=sua_senha

   # ==============================
   # 🔹 CONFIG. DE AUTENTICAÇÃO
   # ==============================
   JWT_SECRET=sua_chave_secreta_jwt_com_pelo_menos_32_caracteres
   API_SCHEDULER_TOKEN=token_seguro_para_endpoints_scheduler

   # ==============================
   # 🔹 CONFIG. DE GOOGLE
   # ==============================
   CREDENTIALS_GOOGLE=sua_api_key_google_maps
   GOOGLE_DRIVE_CREDENTIALS=suas_credenciais_google_drive_json
   GOOGLE_REDIRECT_URI=http://localhost:8080/oauth2/callback

   # ==============================
   # 🔹 CONFIG. DE SICOOB
   # ==============================
   SICOOB_CLIENT_ID=seu_client_id_sicoob
   SICOOB_API_URL=https://api.sicoob.com.br
   SICOOB_AUTH_URL=https://auth.sicoob.com.br
   SICOOB_SCOPE=cobranca_boletos
   SICOOB_NUM_CLIENTE=numero_cliente_sicoob
   SICOOB_NUM_CONTA_CORRENTE=numero_conta_corrente
   SICOOB_DOMAIN=dominio_sicoob

   # Certificado Digital (.pfx em Base64)
   DOCUMENT_PFX=certificado_digital_base64
   PASSWORD_PFX=senha_do_certificado_pfx

   # ==============================
   # 🔹 CONFIG. DE OPENWEATHER
   # ==============================
   API_TOKEN=sua_api_key_openweather
   API_URL=https://api.openweathermap.org/data/2.5/forecast

   # ==============================
   # 🔹 CONFIG. DE FOCUS NFE
   # ==============================
   FOCUS_NFE_TOKEN=seu_token_focus_nfe
   FOCUS_NFE_API_URL=https://api.focusnfe.com.br
   FOCUS_NFE_ENVIRONMENT=homologacao
   FOCUS_NFE_CNPJ_EMITENTE=cnpj_da_empresa_emitente

   # Dados da Empresa
   COMPANY_NAME=Nome da Empresa LTDA
   COMPANY_STATE_REGISTRATION=inscricao_estadual
   COMPANY_CNPJ=cnpj_da_empresa

   # ==============================
   # 🔹 CONFIG. DE NOTIFICAÇÃO
   # ==============================
   
   # Ultramsg (WhatsApp)
   ULTRAMSG_TOKEN=token_ultramsg_whatsapp
   ULTRAMSG_INSTANCE_ID=instance_id_ultramsg

   # SendGrid (E-mail)
   SENDGRID_API_KEY=sua_api_key_sendgrid
   SENDGRID_FROM_EMAIL=noreply@seudominio.com

   # E-mails e WhatsApp Destinatários
   ACCOUNTING_EMAIL=contabilidade@empresa.com
   ACCOUNTING_WHATSAPP=5531999999999
   OVERDUE_NOTIFICATION_EMAILS=email1@empresa.com,email2@empresa.com
   ```

3. **Execute o backend:**
   ```bash
   mvn spring-boot:run
   ```
   O servidor estará disponível em `http://localhost:8080`

4. **Documentação da API (Swagger):**
   Acesse `http://localhost:8080/swagger-ui.html`

### Frontend (Next.js)

1. **Navegue até a pasta do frontend:**
   ```bash
   cd Codigo/Front
   ```

2. **Instale as dependências:**
   ```bash
   npm install
   ```

3. **Execute o frontend:**
   ```bash
   npm run dev
   ```
   A aplicação estará disponível em `http://localhost:3000`

### Banco de Dados

1. **Instale e configure o MySQL 8.0+**

2. **Crie o banco de dados (opcional - será criado automaticamente):**
   ```sql
   CREATE DATABASE hortifruti_sl;
   ```

3. **O sistema está configurado para:**
   - Criação automática do banco se não existir (`createDatabaseIfNotExist=true`)
   - Timezone: America/Sao_Paulo
   - Hibernate: update (cria/atualiza tabelas automaticamente)

### Funcionalidades Principais

#### 🔧 Módulos Implementados

- **Gestão de Usuários**: Cadastro, autenticação JWT
- **Gestão de Clientes**: CRUD completo com informações de contato
- **Conciliação Bancária**: Upload e processamento de extratos em PDF
- **Sistema de Boletos**: Integração com Sicoob para geração de cobrança
- **Notificações**: E-mail (SendGrid) e WhatsApp (Ultramsg)
- **Gestão de Produtos**: Cadastro e recomendações de compra
- **Dashboard**: Visão consolidada das operações
- **Nota Fiscal Eletrônica**: Integração com Focus NFe
- **Previsão do Tempo**: OpenWeather API para Santa Luzia/MG
- **Armazenamento**: Integração com Google Drive

#### 🔐 Segurança

- Autenticação JWT
- Certificados digitais (.pfx) para APIs bancárias
- Tokens seguros para endpoints de scheduler
- Variáveis de ambiente para credenciais sensíveis

#### 📊 Integrações Externas

- **Sicoob**: Geração de boletos e consulta bancária
- **Google Maps**: Cálculo de frete e rotas
- **Google Drive**: Armazenamento de documentos
- **Focus NFe**: Emissão de notas fiscais eletrônicas
- **SendGrid**: Envio de e-mails transacionais
- **Ultramsg**: Envio de mensagens WhatsApp
- **OpenWeather**: Previsão meteorológica

### Observações Importantes

- ⚠️ **Certifique-se** de que o MySQL está rodando antes de iniciar o backend
- ⚠️ **Todas as APIs externas** precisam de credenciais válidas para funcionamento completo
- ⚠️ **O certificado .pfx** deve estar em formato Base64 na variável `DOCUMENT_PFX`
- ⚠️ **Para produção**, altere `spring.profiles.active` para `prod` no `application.properties`
- 📁 **Diretórios temporários** serão criados automaticamente em `temp/`
- 🕐 **Timezone padrão**: America/Sao_Paulo
- 📅 **Formato de data**: dd/MM/yyyy

### Troubleshooting

1. **Erro de conexão com banco**: Verifique se MySQL está rodando e as credenciais estão corretas
2. **Erro de JWT**: Certifique-se que `JWT_SECRET` tem pelo menos 32 caracteres
3. **Erro de certificado**: Verifique se o arquivo .pfx está codificado corretamente em Base64
4. **APIs externas**: Verifique se todas as chaves de API estão válidas e com as permissões necessárias
