# com.hortifruti.sl.hortifruti.controller.scheduler

Endpoints de disparo manual (ou por chamador externo) de jobs agendados: health check, verificação de boletos vencidos e verificação de armazenamento do banco. Todas as rotas são `permitAll` no `SecurityConfig` e protegidas por um token estático validado no `SecurityFilter` (não por papel de usuário).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `SchedulerController.java` | `@RestController` (`/scheduler`) | `GET /scheduler/health` retorna status simples de aplicação ativa; `POST /scheduler/check-overdue` dispara `CombinedScoreOverdueService.scheduledOverdueCheck`; `POST /scheduler/check-database-storage` dispara `DatabaseStorageSchedulerService.scheduledDatabaseCheck`. |
