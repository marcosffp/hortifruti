# com.hortifruti.sl.hortifruti.controller.dashboard

Endpoint único que agrega os dados exibidos no dashboard gerencial, restrito a usuários MANAGER.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `DashboardController.java` | `@RestController` (`/dashboard`), `@PreAuthorize hasRole('MANAGER')` | `GET /dashboard?startDate=&endDate=&month=&year=` retorna um `DashboardResponse` tipado com todas as divisórias/widgets do dashboard para o período e mês/ano de ranking de categorias informados, via `DashboardService.getDashboardData`. |
