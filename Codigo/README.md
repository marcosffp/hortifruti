<img width="1600" style="height:auto; border-radius: 12px;" alt="banner" src="../Documentacao/images/banner.png" />

# 💻 Código — Hortifruti SL

> [⬅ Voltar ao README principal](../README.md)

Este diretório contém o código-fonte do sistema de gestão do Hortifruti Santa Luzia LTDA, dividido em duas aplicações independentes que se comunicam via API REST.

## 🗂️ Estrutura

```
Codigo/
├── Back/      # API REST — Java + Spring Boot
└── Front/     # Aplicação web — Next.js + TypeScript
```

## ⚙️ `/Back` — Backend ([README completo](Back/README.md))

API REST desenvolvida em **Java 21** com **Spring Boot 4**, responsável por toda a regra de negócio, persistência e integrações externas.

**Stack principal:** Spring Boot (Web, Security, Data JPA) · MySQL + HikariCP · JWT (Auth0) · MapStruct · Springdoc OpenAPI · Maven

**Principais funcionalidades:**
- Autenticação JWT e gerenciamento de usuários, clientes e produtos
- Conciliação bancária — processamento de extratos em PDF
- Agrupamento de vendas por cliente e geração de boletos (Sicoob)
- Emissão de notas fiscais eletrônicas (Focus NFe)
- Cálculo de frete (Google Maps), recomendações de compra por clima (OpenWeather)
- Notificações por e-mail (SendGrid) e WhatsApp (Ultramsg)
- Backup automatizado no Google Drive e chatbot de atendimento

## 🎨 `/Front` — Frontend ([README completo](Front/README.md))

Interface web (SPA) desenvolvida com **Next.js 15 (App Router)**, **React 19** e **TypeScript**.

**Stack principal:** Tailwind CSS · Material UI · Chart.js · Leaflet/OSRM · Axios · Biome

**Principais módulos:**
- Dashboard com visão financeira consolidada (gráficos)
- Gerenciamento de usuários, clientes e papéis de acesso
- Upload e visualização de extratos e notas de compra
- Cálculo de frete com mapa interativo (Google Maps/Leaflet)
- Agrupamento de vendas, geração de boletos e notas fiscais
- Central de notificações, backup e relatórios

## 🚀 Como executar

Consulte as instruções rápidas no [README principal](../README.md#-instalação-e-execução) ou as instruções completas — incluindo variáveis de ambiente, scripts e deploy — em cada subprojeto: [`Back/README.md`](Back/README.md) e [`Front/README.md`](Front/README.md).

---

<div align="center">
  <img width="70%" alt="pucminas" src="../Documentacao/images/banner-institucional.svg"/>
</div>
<p align="center">Fonte do banner: <a href="https://github.com/joaopauloaramuni">João Paulo Carneiro Aramuni</a></p>

---
