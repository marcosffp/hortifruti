# com.hortifruti.sl.hortifruti.repository.invoice

Repositório do armazenamento local de XML das notas fiscais eletrônicas emitidas via Focus NFe.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FiscalNoteXmlStorageRepository.java` | `JpaRepository<FiscalNoteXmlStorage, Long>` | Entidade `FiscalNoteXmlStorage`. `findByRef(String)` busca o XML armazenado pela referência da NF-e; `existsByRef(String)` verifica existência prévia; `findByIssuedAtBetweenOrderByIssuedAtDesc(LocalDate, LocalDate)` lista XMLs emitidos num período, mais recentes primeiro. |
