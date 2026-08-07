# com.hortifruti.sl.hortifruti.service.backup.folders

Camada de acesso ao Google Drive usada pelo backup: busca/criação de pastas e upload de arquivos, isolando as chamadas cruas da API Drive (`com.google.api.services.drive`) do restante do domínio de backup.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `DriveQueryBuilder.java` | `@Component` | Monta a query de busca do Drive (`mimeType=... and name=... and trashed=false [and 'parent' in parents]`), escapando aspas simples no nome da pasta. |
| `FileMetadataFactory.java` | `@Component` | Cria os objetos `File` (metadata) do SDK do Drive para upload de arquivo ou criação de pasta, definindo nome, mimeType e pasta pai. |
| `FileUploader.java` | `@Component` | Valida caminho/nome do arquivo local, monta o `FileContent` (`text/csv`) e executa o upload via `Drive.files().create(...)`, retornando o ID do arquivo criado. |
| `FolderManager.java` | `@Component` | Busca o ID de uma pasta por nome (+ pasta pai opcional) via `DriveQueryBuilder`, ou cria a pasta caso não exista (metadata montada por `FileMetadataFactory`); mantém um método de compatibilidade que extrai o nome a partir de um "caminho". |
| `GoogleFolderService.java` | `@Service` | Fachada pública do pacote: delega para `FolderManager`/`FileUploader`, sendo o único ponto usado por `BackupPathService`/`BackupService`. |
