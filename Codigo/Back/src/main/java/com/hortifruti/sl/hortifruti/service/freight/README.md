# com.hortifruti.sl.hortifruti.service.freight

Calcula o valor do frete de entrega a partir da distância/tempo entre dois pontos (Google Maps Distance Matrix API) e de uma configuração de custos operacionais (combustível, manutenção, pneus, depreciação, seguro, mão de obra) persistida em banco.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DistanceMatrixService.java` | `@Service` | Chama a API do Google Maps Distance Matrix (`RestTemplate` + `apiKey` via `@Value`) para obter distância/duração entre origem e destino, e encaminha o resultado para `FreightService.calculateFreight`, retornando distância, duração e valor do frete juntos. |
| `FreightService.java` | `@Service` | Calcula o frete: custo operacional por km (combustível/consumo + manutenção + pneu + depreciação + seguro) mais custo por minuto do entregador (salário base + encargos, dividido pelas horas mensais, acrescido de custos administrativos), aplicados à distância/tempo estimado, e por fim soma margem percentual e taxa fixa configuradas. |
| `FreightPropertiesService.java` | `@Service` | CRUD (get/update) da configuração única de frete (`FreightConfig`, id fixo = 1) usada pelo cálculo. |
