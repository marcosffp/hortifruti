# src/main/resources

Configurações da aplicação Spring Boot por ambiente, dados estáticos usados na inicialização e recursos servidos/utilizados em runtime (templates de e-mail, migrations SQL, imagens).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `application.properties` | Configuração Spring Boot | Configurações comuns a todos os ambientes: datasource (driver, pool HikariCP), JWT/cookie de autenticação, proteção de login (tentativas/lockout), integrações (Google Maps/Drive, Sicoob, Banco do Brasil, OpenWeather, Focus NFe, UltraMsg, SendGrid/Gmail, Cloudflare R2), upload de arquivos e formato de data JSON. O profile ativo é escolhido via `SPRING_PROFILES_ACTIVE` (padrão `local`). |
| `application-local.properties` | Configuração Spring Boot (profile `local`) | Datasource MySQL local (docker-compose ou instalação local), `ddl-auto=update`, URLs de front/back locais, credenciais de homologação da Focus NFe (não há sandbox própria para local) e bucket R2 de homologação. |
| `application-hml.properties` | Configuração Spring Boot (profile `hml`) | Datasource MySQL de homologação (Railway, prefixo `HML_`), cookie de autenticação cross-site (`Secure=true`, `SameSite=None`), URLs de front/back de homologação, credenciais Focus NFe e bucket R2 de homologação. |
| `application-prod.properties` | Configuração Spring Boot (profile `prod`) | Datasource MySQL de produção (Railway, prefixo `PROD_`), cookie cross-site, URLs de produção, credenciais Focus NFe/bucket R2 de produção, e desativação do Swagger/OpenAPI (`springdoc.api-docs.enabled=false`) por segurança. |
| `products.yml` | Dados estáticos (YAML) | Catálogo de produtos usado na emissão de nota fiscal: código, descrição, NCM, CFOP, código ICMS e unidades comercial/tributável de cada item (hortifrutigranjeiros, bebidas, taxa de entrega, etc.). Carregado por serviços de nota fiscal para montar os itens da NF-e a partir do código do produto. |

## Subpacotes

- `static/` — migrations SQL avulsas e imagens estáticas (ver `static/README.md`).
- `templates/` — templates de e-mail (ver `templates/README.md`).
