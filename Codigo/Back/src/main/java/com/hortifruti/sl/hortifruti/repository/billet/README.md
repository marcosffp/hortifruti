# com.hortifruti.sl.hortifruti.repository.billet

Repositório do arquivo (PDF) de boleto armazenado, usado para servir a via já emitida sem reconsultar o Sicoob.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `BilletFileRepository.java` | `JpaRepository<BilletFile, Long>` | Entidade `BilletFile`. `findByCombinedScoreIdAndStatus(Long, FileStatus)` localiza o arquivo de boleto de um agrupamento em um determinado status (`FileStatus`). |
