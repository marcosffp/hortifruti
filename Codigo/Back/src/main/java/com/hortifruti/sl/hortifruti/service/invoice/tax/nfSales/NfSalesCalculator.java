package com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales;

import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.service.invoice.FiscalNoteXmlStorageService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sources XMLs from the locally persisted {@link FiscalNoteXmlStorageService} instead of live Focus
 * NFe calls per note — the live-per-note approach silently dropped notes whenever the external API
 * call failed (timeout, "processando" status, rate limiting), causing incomplete "Exportar
 * Completo" ZIPs.
 */
@Service
@AllArgsConstructor
@Slf4j
public class NfSalesCalculator {
  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;

  public List<File> generateXmlFileList(LocalDate startDate, LocalDate endDate) throws IOException {
    List<FiscalNoteXmlStorageResponse> storedNotes =
        fiscalNoteXmlStorageService.findByPeriod(startDate, endDate);

    Path tempDir = Files.createTempDirectory("nf-sales-xmls-");

    return storedNotes.stream()
        .map(note -> writeXmlToTempFile(tempDir, note.ref()))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private File writeXmlToTempFile(Path tempDir, String ref) {
    try {
      byte[] xmlBytes = fiscalNoteXmlStorageService.getXmlContent(ref);
      File xmlFile = tempDir.resolve(ref + ".xml").toFile();
      try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
        fos.write(xmlBytes);
      }
      return xmlFile;
    } catch (Exception e) {
      log.error("Erro ao gravar XML armazenado para ref={}: {}", ref, e.getMessage());
      return null;
    }
  }
}
