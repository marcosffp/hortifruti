# com.hortifruti.sl.hortifruti.service.scheduler

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DatabaseStorageService.java` | `@Service` | Consulta o tamanho atual do banco via query nativa em `information_schema.tables`, compara com o limite fixo (1024 MB, threshold de 80%) e envia e-mail de alerta (template `database-management`) através de `NotificationCoordinator` quando o limite é ultrapassado; expõe também um envio de teste com tamanho simulado. |
