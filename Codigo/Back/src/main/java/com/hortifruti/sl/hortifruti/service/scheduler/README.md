# com.hortifruti.sl.hortifruti.service.scheduler

Tarefas de verificação periódica acionadas por endpoint HTTP externo (não por `@Scheduled` interno, exceto onde indicado): checagem de boletos/agrupamentos vencidos com notificação por e-mail à gerência, e monitoramento do tamanho do banco de dados.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `CombinedScoreOverdueService.java` | `@Service` | Aciona `BilletService.syncAndFindOverdueUnpaidScores` para localizar `CombinedScore` vencidos e não pagos, agrupa por cliente e envia um e-mail HTML de resumo (template `overdue-management`, com fallback em HTML simples se o template falhar) para a lista de e-mails configurada em `overdue.notification.emails`. Não usa `@Scheduled` — é chamada via endpoint protegido por token estático ou manualmente por um MANAGER. |
| `DatabaseStorageSchedulerService.java` | `@Service` | Verifica se o banco excedeu o limite configurado (`DatabaseStorageService.isDatabaseOverThreshold`) e, em caso positivo, aciona o envio de notificação; também chamado manualmente via endpoint. |
| `DatabaseStorageService.java` | `@Service` | Consulta o tamanho atual do banco via query nativa em `information_schema.tables`, compara com o limite fixo (1024 MB, threshold de 80%) e envia e-mail de alerta (template `database-management`) através de `NotificationCoordinator` quando o limite é ultrapassado; expõe também um envio de teste com tamanho simulado. |
