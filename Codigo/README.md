# Código do Projeto - Hortifruti SL

Este diretório contém o código-fonte do sistema de gestão do Hortifruti Santa Luzia LTDA.

## Estrutura

### `/Back` - Backend (Spring Boot)
API REST desenvolvida em Java com Spring Boot.

**Tecnologias:**
- Java 21
- Spring Boot 3.x
- Spring Security com JWT
- Spring Data JPA
- MySQL 8.0
- Maven

**Principais funcionalidades:**
- Autenticação e autorização de usuários
- Gerenciamento de clientes e produtos
- Processamento de extratos bancários (PDF)
- Integração com APIs externas (Sicoob, Google Maps, SendGrid, etc.)
- Geração e gestão de boletos
- Sistema de notificações (WhatsApp e E-mail)
- Backup automatizado
- Chatbot de atendimento

### `/Front` - Frontend (Next.js)
Interface web desenvolvida com Next.js e TypeScript.

**Tecnologias:**
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Shadcn/ui

**Principais módulos:**
- Dashboard com visão geral do negócio
- Gerenciamento de usuários e clientes
- Upload e visualização de extratos
- Cálculo de frete com integração Google Maps
- Agrupamento de vendas por cliente
- Geração de boletos
- Sistema de notificações
- Relatórios e recomendações de compra

## Como Executar

Consulte as instruções detalhadas no [README principal](../README.md) do projeto.