# com.hortifruti.sl.hortifruti.repository.invoice

Repositório do armazenamento local de XML das notas fiscais eletrônicas emitidas via Focus NFe.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FiscalNoteXmlStorageRepository.java` | `JpaRepository<FiscalNoteXmlStorage, Long>` | Entidade `FiscalNoteXmlStorage`. `findByRef(String)` busca o XML armazenado pela referência da NF-e; `findByIssuedAtBetweenAndStatusOrderByIssuedAtDesc(LocalDate, LocalDate, FileStatus)` lista XMLs emitidos num período filtrando por status (ex.: só `ACTIVE`, excluindo NFs canceladas), mais recentes primeiro. |
