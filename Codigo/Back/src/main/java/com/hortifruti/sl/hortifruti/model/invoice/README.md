# com.hortifruti.sl.hortifruti.model.invoice

Entidade referente ao armazenamento de notas fiscais emitidas via Focus NFe.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `FiscalNoteXmlStorage.java` | `@Entity` (`fiscal_note_xml_storage`) | Armazena os metadados e o conteúdo (XML/DANFE) de uma nota fiscal emitida. Guarda `ref` (referência única na Focus NFe), `nfNumber`, `clientName`, `totalValue`, `issuedAt`, `status` (`@Enumerated` de `model.FileStatus`, default `ACTIVE`) e `cancelledAt`. `xmlContent` (`@Lob`, `LONGTEXT`) é legado; registros novos usam `objectKey`/`danfeObjectKey` (Cloudflare R2). |
