# com.hortifruti.sl.hortifruti.tools

Ferramentas de manutenção pontuais ("one-off"), fora do fluxo normal de negócio da aplicação. Diferente de `service/`, o código aqui não é destinado a permanecer ativo indefinidamente em produção.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `OrphanFiscalFileCleanupRunner.java` | `@Component` (`ApplicationRunner`, `@Order(Integer.MAX_VALUE)`) | Identifica objetos de nota fiscal (XML/DANFE) no bucket R2 que não estão referenciados por nenhuma linha em `fiscal_note_xml_storage` (órfãos deixados por uma corrida corrigida em `FiscalNoteXmlStorageService`). Inerte por padrão — só age se a env var `CLEANUP_ORPHAN_FISCAL_FILES` estiver setada: `"report"` apenas lista os órfãos nos logs; `"quarantine"` move-os para `notas-fiscais/{env}/_orfaos-removidos/`. Encerra a JVM (`System.exit(0)`) ao final. Destinado a ser removido após o uso único. |
