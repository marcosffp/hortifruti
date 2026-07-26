# com.hortifruti.sl.hortifruti.controller.freight

Endpoint de cálculo de distância/frete via Google Maps Distance Matrix e configuração dos parâmetros de cálculo de frete.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `DistanceController.java` | `@RestController` (`/distance`) | `POST /distance` calcula distância e valor de frete a partir de uma `LocationRequest` (sem restrição de papel); `GET /distance/freight-config` (`@PreAuthorize hasRole('MANAGER')`) retorna a configuração atual de frete; `PATCH /distance/freight-config` (`@PreAuthorize hasRole('MANAGER')`) atualiza a configuração de frete. |
