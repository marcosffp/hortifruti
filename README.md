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

2. **Configure as variáveis de ambiente** criando um arquivo `.env` na raiz do backend:
   ```env
   # URLs
   FRONTEND_URL=http://localhost:3000
   BACKEND_URL=http://localhost:8080

   # Banco de Dados MySQL
   MYSQLHOST=localhost
   MYSQLPORT=3306
   MYSQLDATABASE=hortifruti_sl
   MYSQLUSER=seu_usuario
   MYSQLPASSWORD=sua_senha

   # Autenticação
   JWT_SECRET=sua_chave_secreta_jwt
   API_SCHEDULER_TOKEN=token_para_scheduler

   # Google APIs
   CREDENTIALS_GOOGLE=sua_api_key_google_maps
   GOOGLE_DRIVE_CREDENTIALS=credenciais_google_drive
   GOOGLE_REDIRECT_URI=http://localhost:8080/oauth2/callback

   # Sicoob API (Boletos)
   SICOOB_CLIENT_ID=seu_client_id
   SICOOB_API_URL=url_api_sicoob
   SICOOB_AUTH_URL=url_auth_sicoob
   SICOOB_SCOPE=escopo_sicoob
   SICOOB_NUM_CLIENTE=numero_cliente
   SICOOB_NUM_CONTA_CORRENTE=conta_corrente
   SICOOB_DOMAIN=dominio_sicoob
   DOCUMENT_PFX=certificado_base64
   PASSWORD_PFX=senha_certificado

   # OpenWeather API
   API_TOKEN=sua_api_key_openweather
   API_URL=https://api.openweathermap.org/data/2.5/forecast

   # Focus NFe API
   FOCUS_NFE_TOKEN=seu_token_focus
   FOCUS_NFE_API_URL=url_api_focus
   FOCUS_NFE_ENVIRONMENT=homologation
   FOCUS_NFE_CNPJ_EMITENTE=cnpj_emitente
   COMPANY_NAME=nome_empresa
   COMPANY_STATE_REGISTRATION=inscricao_estadual
   COMPANY_CNPJ=cnpj_empresa

   # Notificações
   ULTRAMSG_TOKEN=token_ultramsg
   ULTRAMSG_INSTANCE_ID=instance_id_ultramsg
   SENDGRID_API_KEY=sua_api_key_sendgrid
   SENDGRID_FROM_EMAIL=email_remetente
   ACCOUNTING_EMAIL=email_contabilidade
   ACCOUNTING_WHATSAPP=whatsapp_contabilidade
   OVERDUE_NOTIFICATION_EMAILS=emails_notificacao
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

### Observações

- Certifique-se de que o MySQL está rodando antes de iniciar o backend
- O banco de dados será criado automaticamente se não existir
- Algumas funcionalidades dependem de APIs externas (Sicoob, SendGrid, Ultramsg, Google, etc.) que precisam ser configuradas com credenciais válidas
