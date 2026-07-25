# com.hortifruti.sl.hortifruti.dto.freight

DTOs do módulo de cálculo de frete, que integra com a API do Google Maps para distância/duração de
rotas e aplica uma configuração de custos (combustível, manutenção, mão de obra etc.) para estimar o
valor do frete.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DistanceFreightResponse.java` | record | Resposta combinando distância, duração e o valor de frete calculado (`freight`, double). |
| `DistanceResponse.java` | record | Resposta apenas com distância e duração da rota (string formatada, ex.: vindo da API do Google Maps). |
| `FreightCalculationRequest.java` | record | Request para cálculo de frete a partir de distância (km) e tempo estimado (minutos), ambos como String. |
| `FreightConfigDTO.java` | record | Configuração de custos usada no cálculo de frete: consumo (km/l), preço do combustível, custos por km (manutenção, pneus, depreciação, seguro), salário base, percentual de encargos, horas mensais trabalhadas, percentual de custos administrativos, percentual de margem e taxa fixa. |
| `Location.java` | record | Par de coordenadas geográficas (`lat`, `lng`). |
| `LocationRequest.java` | record | Request de cálculo de rota com origem e destino, cada um representado por um `Location`. |
