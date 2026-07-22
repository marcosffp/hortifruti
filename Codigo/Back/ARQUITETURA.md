# Arquitetura — convenção de pacotes (Backend)

> Decisão referente à seção 6 de `CORRECOES_BACKEND.md` (estrutura de pacotes inconsistente).
> Vale a partir de agora para código novo; código existente é migrado aos poucos, conforme for
> mexido por outro motivo — não é uma refatoração "big bang".

## Regra

Todo domínio de negócio (`billet`, `invoice`, `purchase`, `finance`, `chatbot`, `backup`,
`climate`, `freight`, `bb`, `sicoob`, `storage`...) ganha um subpacote homônimo em `controller/`,
`service/`, `dto/`, `model/` e `repository/` **quando aplicável a esse domínio** — nem todo
domínio precisa dos cinco (ex.: um domínio sem entidade JPA própria não tem `model/<dominio>/`).

Uma classe solta na raiz de `controller/`, `service/`, `dto/`, `model/` ou `repository/` só é
aceitável quando não pertence a nenhum domínio específico (ex.: `User`, `RefreshToken`,
`FreightConfig` em `model/` — são conceitos transversais/de infraestrutura, não parte de um
domínio de negócio maior).

## Papéis dentro de `service/<dominio>/`

Usar no máximo estes três papéis por classe:

- **`XService`** — orquestração + regra de negócio pública; é o único ponto de entrada do domínio
  usado por controllers/outros domínios.
- **`XClient` / `XHttpClient`** — só chamada externa (API de banco, Focus NFe, etc.), sem regra de
  negócio.
- **`XRepository`** — acesso a dados via Spring Data, sem lógica.

Evitar `XQuery` / `XFactory` / `XValidation` como classes soltas sem justificativa. Quando um
domínio precisar de mais granularidade que isso (ex.: `service/billet`, que tem chamadas HTTP
distintas para emitir/baixar/consultar boleto, cada uma com tratamento de erro específico da API
do Sicoob), documente explicitamente o papel de cada classe num `package-info.java` do pacote —
ver `service/billet/package-info.java` como exemplo aplicado.

## Acesso a repositório entre domínios

Um serviço de domínio X só injeta o repositório de X. Para ler dados de outro domínio Y, chama o
`YService` (ou, se o acesso for só um lookup por ID sem regra de negócio, um método pontual
exposto pelo `YService` — não o `YRepository` direto).

## Migração incremental

Não é necessário mover tudo de uma vez. Ao tocar em um domínio por outro motivo (bug, feature),
aproveite para:

1. Mover o `controller/`, `dto/`, `model/`, `repository/` desse domínio para o subpacote homônimo,
   se ainda não estiverem lá.
2. Revisar se as classes de `service/<dominio>/` cabem nos três papéis acima; se não, documentar
   via `package-info.java` em vez de forçar uma fusão arriscada sem cobertura de testes.

`billet` foi o primeiro domínio migrado como prova de conceito: `BilletController` movido de
`controller/` para `controller/billet/`; acesso direto de `BilletQuery` a `CombinedScoreRepository`
substituído por chamada a `CombinedScoreService` (regra de "um domínio só acessa o próprio
repositório"); papéis das 8 classes de `service/billet` documentados em
`service/billet/package-info.java`.
