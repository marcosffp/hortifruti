# com.hortifruti.sl.hortifruti

Pacote raiz da aplicação Spring Boot do sistema de gestão Hortifruti Santa Luzia LTDA. Contém apenas a classe de bootstrap; toda a lógica de negócio, integrações e camadas da aplicação estão organizadas nos subpacotes abaixo.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `HortifrutiSlApplication.java` | `@SpringBootApplication` | Ponto de entrada da aplicação. Habilita `@EnableAsync` (tarefas assíncronas), `@EnableScheduling` (jobs agendados, ex.: limpeza de refresh tokens) e `@EnableSpringDataWebSupport` com serialização de `Page` via DTO. |

## Subpacotes

- `config/` — configurações do Spring (segurança, JWT, mTLS, clientes HTTP externos, Swagger, inicialização de dados) e integrações de baixo nível com BB, Sicoob, Focus NFe, OpenWeather, Cloudflare R2 (ver `config/README.md` e subpacotes).
- `controller/` — endpoints REST da API.
- `dto/` — objetos de transferência de dados (requests/responses) usados pelos controllers e serviços.
- `exception/` — exceções de domínio e handlers globais.
- `mapper/` — interfaces MapStruct que convertem entre entidades JPA e DTOs (ver `mapper/README.md`).
- `model/` — entidades JPA e enums de domínio.
- `repository/` — interfaces Spring Data JPA de acesso ao banco.
- `service/` — regras de negócio: conciliação bancária, boletos, notas fiscais, compras/vendas, backup, clima, frete, notificações, storage e dashboard.
- `tools/` — ferramentas de manutenção pontuais, fora do fluxo normal da aplicação (ver `tools/README.md`).
- `util/` — utilitários estáticos compartilhados entre serviços (ver `util/README.md`).
