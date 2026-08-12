# com.hortifruti.sl.hortifruti.repository.climate

Repositório do catálogo de produtos usado na recomendação climática (associação produto x categoria de temperatura).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `ClimateProductRepository.java` | `JpaRepository<ClimateProduct, Long>` | Entidade `ClimateProduct`. `findByTemperatureCategory(TemperatureCategory)` lista produtos recomendados para uma categoria de temperatura; `findByNameContainingIgnoreCase(String)` busca produtos por nome parcial, ignorando caixa. |
