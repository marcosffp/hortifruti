# Auditoria de Qualidade e Segurança — Backend Hortifruti SL

**Escopo:** `Codigo/Back/src/main/java/com/hortifruti/sl/hortifruti/**`, `application*.properties`, `products.yml`, `pom.xml`.

> Cada achado tem checkbox `- [ ]`, severidade (🟡 Médio) e localização exata. Marque `[x]` e remova o item conforme for corrigindo — este arquivo reflete só o trabalho pendente.

---

- [ ] 🟡 **[D-P1] Listagens sem paginação apesar do padrão `Page`/`Pageable` já existir em outros endpoints do projeto**
  **Restam em aberto:** `controller/billet/BilletController.java` (`GET /billet/open`), `controller/invoice/InvoiceController.java` (`GET /invoices/open`), `controller/purchase/ClientController.java` (`GET /clients`), `controller/purchase/CombinedScoreController.java` (`GET /combined-scores/last-per-client`) — confirmado via mapeamento do `Codigo/Front`: todos alimentam telas que dependem da lista completa no cliente (busca + "selecionar todos os filtrados" para ações em massa nas abas de Boletos/NF sem boleto; join em memória entre `/clients` e `/combined-scores/last-per-client` na tela de Clientes). Paginar de verdade exige redesenhar esses fluxos (busca/seleção assíncrona por página, ou embutir a "última compra" na própria resposta paginada de `/clients` em vez de dois fetches separados) — não é troca mecânica de tipo de retorno.

- [ ] 🟡 **[E-C1] `ddl-auto=update` continua ativo (não `validate`) — Flyway já introduzido**
  Flyway foi adicionado (`org.flywaydb:flyway-core`/`flyway-mysql` no `pom.xml`) e os 11 scripts que existiam em `static/*.sql` foram versionados em `src/main/resources/db/migration/V1__...` a `V11__...`, com `spring.flyway.baseline-on-migrate=true`/`baseline-version=11` (em `hml`/`prod`, bancos já existentes, o Flyway só marca a versão 11 como baseline, sem reexecutar nada). Mudanças de schema futuras entram como `V12__...` em diante.
  **Resta em aberto:** `spring.jpa.hibernate.ddl-auto` continua `update` nos 3 ambientes — não foi travado para `validate`. Travar exige confirmar, com acesso real ao banco de `hml`/`prod`, que o schema bate exatamente com as entidades no momento da troca; sem isso o risco é a aplicação não subir no próximo deploy. Plano: trocar `hml` para `validate` primeiro, confirmar boot limpo, só então `prod`.
