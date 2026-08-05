# Spec — Captura de Nota via Câmera + Extração Automática (Gemini Vision)

## Como usar este documento

Cada etapa é independente e deve ser implementada, testada e validada antes de passar pra próxima. Não pule etapa. Cada uma tem: objetivo, o que implementar, critério de aceite, como testar de forma temporária (sem depender das etapas seguintes ainda existirem) e pontos de segurança/assertividade específicos daquele pedaço.

Instrução geral pro Claude Code: implementar uma etapa por vez, parar ao final de cada uma e aguardar validação antes de seguir pra próxima, mesmo que o contexto pareça suficiente pra continuar.

---

## Contexto do problema

O cadastro de compras (RF014 — upload de notas de compra) hoje depende de digitação manual a partir de fotos de notas manuscritas de fornecedor (letra cursiva, papel picado, várias linhas item/qtd/preço/total, às vezes com rasura). O objetivo é fotografar a nota pela câmera do celular, extrair os itens automaticamente via LLM de visão (Gemini, free tier) e pré-preencher o pedido — sempre com revisão humana antes de confirmar, porque testes de benchmark mostram que leitura de manuscrito bagunçado por LLM fica na faixa de ~70-90% de acurácia, não 100%. A extração é um rascunho assistido, não uma leitura automática confiável.

---

## Etapa 0 — Setup e infraestrutura base

### Objetivo
Preparar o projeto pra receber a integração, sem lógica de negócio ainda.

### O que implementar
- Criar conta/projeto no Google AI Studio e gerar `GEMINI_API_KEY` (free tier).
- Adicionar variáveis de ambiente no backend: `GEMINI_API_KEY`, `GEMINI_MODEL` (default `gemini-2.5-flash`), `GEMINI_TIMEOUT_MS` (default 15000).
- Adicionar dependência HTTP client já usada no projeto (o backend já tem client REST pra Sicoob/Focus NFe — reaproveitar o mesmo padrão, não introduzir lib nova).
- Criar pacote `nota` (ou `ocr`, ou nome consistente com a convenção do projeto) dentro do módulo de compras no backend.

### Critério de aceite
- Variáveis de ambiente documentadas no `README.md` do backend (seção de env vars).
- Projeto builda normalmente sem nenhuma chamada real à API ainda.

### Como testar (temporário)
- Rodar `./mvnw spring-boot:run` e confirmar que a aplicação sobe sem erro mesmo com `GEMINI_API_KEY` vazia (a env var só precisa existir, não ser usada ainda).

### Segurança
- `GEMINI_API_KEY` nunca commitada — confirmar que está no `.env.example` como placeholder e no `.gitignore` o `.env` real.

---

## Etapa 1 — Endpoint de upload de imagem (sem IA ainda)

### Objetivo
Ter um endpoint que recebe a foto, valida e salva/repassa, sem nenhuma extração ainda. Isola o problema de "receber arquivo" do problema de "ler o conteúdo".

### O que implementar
```
POST /api/compras/notas/upload
Content-Type: multipart/form-data
Body: file (image/jpeg | image/png)

Response 200:
{ "arquivoId": "uuid", "tamanhoBytes": number, "contentType": "string" }
```
- Validar `content-type` (só `image/jpeg` e `image/png`).
- Validar tamanho máximo (ex: 10MB — foto de celular moderno já resolve com folga).
- Validar que o usuário autenticado tem papel permitido (`Gestor` ou `Funcionário`, conforme os papéis já existentes no sistema).
- Salvar temporariamente (pode ser em memória/pasta temp local nessa etapa — a etapa de persistência definitiva vem depois, se necessário).

### Critério de aceite
- Upload de imagem válida retorna 200 com metadados.
- Upload de arquivo não-imagem (ex: `.pdf`, `.txt`) retorna 400.
- Upload maior que o limite retorna 413.
- Requisição sem token JWT válido retorna 401.
- Requisição de usuário sem papel permitido retorna 403.

### Como testar (temporário)
```bash
# sucesso
curl -X POST http://localhost:8080/api/compras/notas/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@nota-teste.jpg"

# arquivo inválido
curl -X POST http://localhost:8080/api/compras/notas/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@documento.pdf"

# sem token
curl -X POST http://localhost:8080/api/compras/notas/upload -F "file=@nota-teste.jpg"
```
Testar manualmente com 2-3 fotos reais de notas (pode usar as mesmas fotos do dia a dia da loja) só pra validar que o multipart está funcionando fim a fim.

### Segurança
- Nunca confiar só na extensão do arquivo — validar magic bytes/content-type real, não só o nome.
- Rate limit básico no endpoint (o projeto já usa Bucket4J — reaproveitar o padrão existente) pra evitar abuso que geraria custo de chamadas à API do Gemini nas próximas etapas.

---

## Etapa 2 — Integração com Gemini (extração bruta, sem matching ainda)

### Objetivo
Enviar a imagem pro Gemini e receber a extração estruturada em JSON, sem ainda cruzar com o catálogo de produtos.

### O que implementar
`GeminiExtractionService`:
```
POST /api/compras/notas/extrair (evolução do endpoint da Etapa 1, ou endpoint separado)
Body: file (multipart)

Response 200:
{
  "cliente": "string | null",
  "data": "dd/MM/yyyy | null",
  "itens": [
    {
      "produtoLido": "string",
      "quantidade": "number | null",
      "unidade": "string | null",
      "precoUnitario": "number | null",
      "total": "number | null"
    }
  ],
  "totalGeral": "number | null"
}
```
- Prompt de extração (usar `response_schema` nativo da API do Gemini pra forçar JSON, não parsear texto livre):
```
Você está lendo uma nota manuscrita de compra de hortifrúti (fornecedor → cliente).
Extraia cada linha de item com: nome do produto, quantidade, unidade (kg/un/cx quando aplicável),
preço unitário e valor total da linha. Também extraia o nome do cliente/destinatário (se houver)
e o total geral da nota.
Se um campo estiver ilegível, retorne null nesse campo em vez de inventar um valor.
Preserve o nome do produto como está escrito, sem corrigir ortografia.
```
- Timeout e tratamento de erro: se a API do Gemini falhar ou estourar timeout, retornar 502 com mensagem clara (não deixar a requisição travar o usuário).
- Log da chamada (sem logar a imagem inteira, só metadados: tamanho, tempo de resposta, sucesso/falha) pra acompanhar uso do free tier.

### Critério de aceite
- Enviar uma foto de nota real retorna JSON estruturado com pelo menos os itens principais legíveis extraídos corretamente.
- Campo ilegível vem como `null`, não inventado.
- Falha da API do Gemini (simular com API key inválida) retorna erro tratado, não exception vazando pro cliente.
- Nenhuma imagem enviada ao Gemini é persistida em log ou storage sem necessidade.

### Como testar (temporário)
- Testar com pelo menos 5 fotos reais diferentes de notas (variando: letra mais legível, letra mais bagunçada, nota com rasura, nota bem iluminada, nota com sombra) e comparar manualmente a extração com o que está escrito na nota.
- Criar uma planilha simples de acompanhamento: `foto | itens_corretos | itens_errados | campo_ilegível_reconhecido_como_null`. Isso vira sua baseline de acurácia real do modelo com as notas de vocês, não a de benchmark genérico.
- Testar erro proposital: mandar `GEMINI_API_KEY` inválida e confirmar que o erro é tratado.

### Segurança
- Nunca logar o conteúdo bruto da imagem ou dados de cliente extraídos em log persistente sem necessidade (dado de negócio da loja).
- `GEMINI_API_KEY` só no backend — o frontend nunca deve ter acesso direto à chave nem chamar a API do Gemini diretamente.

### Assertividade
- Essa é a etapa que define a baseline de qualidade do produto. Se a acurácia real ficar muito baixa nos testes manuais (abaixo de ~60-70% dos campos principais corretos), vale reavaliar o prompt antes de seguir pra próxima etapa (ex: pedir explicitamente pro modelo ser conservador com números que pareçam ambíguos, ou testar `gemini-2.5-pro` em vez de `flash` pra ver se compensa a diferença de velocidade).

---

## Etapa 3 — Matching de produtos contra o catálogo

### Objetivo
Cruzar o `produtoLido` (texto solto vindo do Gemini) com os produtos já cadastrados no sistema, pra sugerir o produto correto em vez do usuário digitar de novo.

### O que implementar
`ProdutoMatchingService`:
- Buscar todos os produtos ativos cadastrados.
- Calcular similaridade entre `produtoLido` e `nome` de cada produto (ex: distância de Levenshtein normalizada, ou similaridade de trigramas — usar o que for mais simples de integrar na stack atual).
- Retornar o candidato de maior score.
- Se `score < 0.6` (limiar ajustável), retornar `produtoSugerido: null` e marcar `confianca: baixa`.

Ajustar a resposta do endpoint da Etapa 2 pra incluir:
```json
{
  "produtoLido": "string",
  "produtoSugerido": { "id": "long", "nome": "string", "score": 0.0 } | null,
  "confianca": "alta | media | baixa"
}
```

### Critério de aceite
- Nome extraído com pequena variação de escrita (ex: "couv-flor" vs "Couve Flor" cadastrado) resolve pro produto certo.
- Nome sem nenhum produto parecido no catálogo retorna `null` em vez de forçar um match ruim.
- Limiar de score é configurável (não hardcoded espalhado pelo código).

### Como testar (temporário)
- Escrever um teste unitário simples com pares conhecidos: `("couv-flor", esperar match "Couve Flor")`, `("banana prata", esperar match "Banana Prata")`, `("xyz123", esperar null)`.
- Rodar o endpoint da Etapa 2 de novo com as mesmas 5 fotos de teste e conferir manualmente se os matches fazem sentido, anotando na mesma planilha de acompanhamento: `produto_lido | produto_sugerido | correto (sim/não)`.

### Segurança
- Nenhuma, essa etapa é só lógica interna sobre dados já no banco.

### Assertividade
- Esse matching é o que mais reduz o retrabalho do usuário — vale testar o limiar (0.6) com casos reais e ajustar se estiver sugerindo produto errado com frequência ou deixando de sugerir produto óbvio.

---

## Etapa 4 — Checagem de consistência

### Objetivo
Sinalizar automaticamente linhas ou notas que provavelmente têm erro de leitura, com base em matemática simples, sem depender de mais IA.

### O que implementar
- Por item: se `quantidade * precoUnitario` não bate com `total` da linha (fora de uma margem de tolerância, ex: R$0,05 pra arredondamento), marcar `confianca: baixa` nessa linha, mesmo que o matching de produto tenha dado certo.
- Pra nota inteira: `consistente = abs(soma(itens.total) - totalGeral) < margem_tolerancia`.
- Adicionar esses dois campos na resposta final do endpoint.

### Critério de aceite
- Nota com soma batendo retorna `consistente: true`.
- Nota com soma não batendo retorna `consistente: false` e indica quais itens conferir primeiro (ex: os de maior valor, que têm mais impacto na diferença).
- Item com conta interna errada (qtd × preço ≠ total da linha) fica marcado mesmo se o produto foi identificado certo.

### Como testar (temporário)
- Reusar as mesmas 5 fotos de teste; forçar um caso de inconsistência editando manualmente o JSON de resposta simulado, pra confirmar que a lógica de flag funciona antes mesmo de esperar o Gemini errar de verdade.
- Testar também com uma nota real que você sabe que bate (soma manualmente e compara).

### Segurança
- Nenhuma.

### Assertividade
- Essa é a rede de segurança mais barata do sistema (não depende de IA, é só aritmética) — garante que erro de leitura vira alerta visual em vez de passar despercebido.

---

## Etapa 5 — Componente de captura de câmera (frontend, isolado)

### Objetivo
Ter a tela de captura funcionando sozinha, sem ainda estar ligada ao fluxo de revisão.

### O que implementar
- Componente `CapturarNotaCamera`:
  - `<input type="file" accept="image/*" capture="environment">` pra abrir a câmera direto no mobile.
  - Preview da imagem tirada antes de enviar.
  - Botão "Tirar outra foto" (descarta e reabre a câmera).
  - Botão "Usar esta foto" (segue pro próximo passo).
- No desktop, cair no fallback de seleção de arquivo normal (o mesmo input já cobre isso).

### Critério de aceite
- No celular, tocar em "Tirar foto" abre a câmera nativa (não uma galeria).
- Preview mostra exatamente a foto tirada.
- Componente funciona isolado, renderizado numa página de teste temporária, sem depender do backend ainda (upload pode ser mockado/no-op nessa etapa).

### Como testar (temporário)
- Criar uma rota temporária tipo `/dev/teste-camera` (removida antes do merge final) só renderizando esse componente, pra testar em device real (celular) sem precisar navegar pelo fluxo completo do sistema.
- Testar em pelo menos: um Android com Chrome e um iPhone com Safari (comportamento de `capture="environment"` varia entre navegadores).

### Segurança
- Nenhuma nessa etapa isolada (upload real vem na integração final).

---

## Etapa 6 — Tela de revisão/edição (frontend, com dados mockados)

### Objetivo
Construir a tela onde o usuário confere e corrige a extração, usando dados de exemplo fixos (sem depender do backend real ainda), pra validar a UX antes de plugar tudo junto.

### O que implementar
- Componente `RevisaoNotaExtraida`:
  - Tabela editável reaproveitando o componente de itens já usado no cadastro de compra manual.
  - Campo de produto: se veio `produtoSugerido`, já vem selecionado mas trocável (busca no catálogo); se veio `null`, campo em aberto.
  - Badge visual de confiança por linha (alta/média/baixa).
  - Alerta no topo se `consistente: false`.
  - Botão "Confirmar pedido" desabilitado até que toda linha de baixa confiança tenha sido tocada/editada pelo usuário (ex: marcar como revisada ao clicar/editar o campo).

### Critério de aceite
- Com um JSON de exemplo fixo (mock com 2-3 itens, incluindo um de baixa confiança e um inconsistente), a tela renderiza corretamente os alertas visuais.
- Botão de confirmar só habilita depois que a linha de baixa confiança é tocada.
- Edição de campo funciona (trocar produto, quantidade, preço) e reflete no total calculado em tela.

### Como testar (temporário)
- Criar arquivo de mock (`nota-extraida-mock.json`) com casos variados (tudo certo, um item de baixa confiança, nota inconsistente) e renderizar a tela com cada um, sem chamar o backend.

### Segurança
- Nenhuma nessa etapa isolada.

---

## Etapa 7 — Integração fim a fim

### Objetivo
Ligar tudo: câmera → upload → extração Gemini → matching → consistência → tela de revisão → confirmação → criação do pedido/compra no fluxo já existente (RF014/RF016).

### O que implementar
- Frontend chama o endpoint real da Etapa 2 (já com matching e consistência das Etapas 3-4) em vez do mock.
- Ao confirmar na tela de revisão, os dados seguem exatamente o mesmo caminho que uma compra cadastrada manualmente hoje (reaproveitar o service/endpoint já existente de criação de compra, não duplicar lógica).
- Remover rotas/telas temporárias de teste (`/dev/teste-camera`) antes de considerar a etapa concluída.

### Critério de aceite
- Fluxo completo funciona: tirar foto → ver rascunho → editar se precisar → confirmar → pedido aparece na listagem de compras do cliente, igual a um cadastrado manualmente.
- Cancelar no meio do fluxo (antes de confirmar) não cria nenhum registro no banco.
- Recarregar a página no meio do fluxo não deixa lixo de estado (nenhum registro "pendente" órfão).

### Como testar (temporário)
- Teste manual de ponta a ponta com pelo menos 5 notas reais diferentes (as mesmas usadas nas etapas anteriores, pra comparar evolução), do celular até o pedido aparecer na tela de compras.
- Testar o caminho de cancelamento (tirar foto, ver rascunho, desistir) e confirmar que nada foi persistido.

### Segurança
- Confirmar que o endpoint de extração exige o mesmo controle de papel/autenticação que o resto do módulo de compras (não é um endpoint "esquecido" sem `@PreAuthorize`).

### Assertividade
- Atualizar a planilha de acompanhamento com o resultado fim a fim: de todas as notas testadas, quantas precisaram de correção manual e em qual campo (produto, quantidade, preço). Isso vira o dado real pra decidir se vale ajustar prompt/limiar antes de colocar em uso na loja.

---

## Etapa 8 — Segurança e limites de uso (hardening)

### Objetivo
Fechar pontas soltas de segurança e custo antes de considerar a feature pronta pra uso real.

### O que implementar
- Rate limit específico no endpoint de extração (mais restritivo que o upload genérico, já que cada chamada consome cota do free tier do Gemini) — reaproveitar Bucket4J já usado no projeto.
- Sanitização: garantir que texto vindo do Gemini (nome de produto, nome de cliente) passa pelas mesmas validações de tamanho/caracteres que campos digitados manualmente antes de ir pro banco (evitar que uma leitura estranha da IA vire dado inconsistente no cadastro).
- Monitoramento simples de uso da cota do Gemini (contador de chamadas por dia, log ou métrica) pra saber quando o free tier está perto do limite.
- Fallback claro se o free tier estourar: endpoint retorna erro amigável ("Extração automática indisponível no momento, cadastre manualmente") em vez de travar o fluxo de compra inteiro — a criação manual de compra (fluxo já existente) precisa continuar funcionando independente dessa feature.

### Critério de aceite
- Passar do limite de requisições configurado retorna 429 com mensagem clara.
- Nome de produto/cliente absurdamente longo vindo da extração não quebra a criação da compra nem excede limite de coluna no banco.
- Simular indisponibilidade do Gemini (derrubar a chave temporariamente) e confirmar que o cadastro manual de compra continua funcionando normalmente.

### Como testar (temporário)
```bash
# testar rate limit
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/compras/notas/extrair \
    -H "Authorization: Bearer $TOKEN" -F "file=@nota-teste.jpg"
done
# esperar 429 a partir de algum ponto
```
- Trocar `GEMINI_API_KEY` por valor inválido temporariamente e confirmar que só a extração automática falha — o cadastro manual de compra continua ok.

### Segurança
- Essa etapa inteira é sobre segurança/custo — não pular mesmo com pressa de entregar.

---

## Resumo da ordem de implementação

| Etapa | Entrega | Depende de |
|---|---|---|
| 0 | Setup/env vars | — |
| 1 | Upload de imagem | 0 |
| 2 | Extração Gemini bruta | 1 |
| 3 | Matching de produto | 2 |
| 4 | Checagem de consistência | 2, 3 |
| 5 | Componente de câmera | — (paralelo às 1-4) |
| 6 | Tela de revisão (mock) | — (paralelo às 1-4) |
| 7 | Integração fim a fim | 4, 5, 6 |
| 8 | Segurança/hardening | 7 |

As Etapas 5 e 6 (frontend) podem ser feitas em paralelo às 1-4 (backend), já que uma usa mock e a outra não depende do frontend. A Etapa 7 é onde tudo se junta.
