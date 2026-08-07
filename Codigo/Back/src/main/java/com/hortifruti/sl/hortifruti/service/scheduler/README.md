# com.hortifruti.sl.hortifruti.service.scheduler

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DatabaseStorageMonitorService.java` | `@Service` | Consulta o tamanho atual do banco via query nativa em `information_schema.tables` e compara com o limite fixo (1024 MB, threshold de 80%). Sem dependência de notificação/e-mail — usado também por `BackupService`, que só precisa dos números de tamanho. |
| `DatabaseStorageAlertService.java` | `@Service` | Compõe e envia o e-mail de alerta (template `database-management`) através de `NotificationCoordinator` quando o limite apurado por `DatabaseStorageMonitorService` é ultrapassado; expõe também um envio de teste com tamanho simulado. |
