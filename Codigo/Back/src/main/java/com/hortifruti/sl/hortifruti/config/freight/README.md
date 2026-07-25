# com.hortifruti.sl.hortifruti.config.freight

Propriedades de configuração usadas no cálculo de custo/preço de frete das entregas.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FreightProperties.java` | `@Component` (`@ConfigurationProperties(prefix = "freight")`) | Agrupa três blocos de configuração: `VehicleConfig` (consumo, combustível, manutenção, pneus, depreciação, seguro por km), `DeliveryPersonConfig` (salário base, encargos, horas mensais, custos administrativos) e `MarginConfig` (margem percentual e taxa fixa). Os valores padrão equivalentes são usados por `UserInitializer` para popular `FreightConfig` no banco. |
