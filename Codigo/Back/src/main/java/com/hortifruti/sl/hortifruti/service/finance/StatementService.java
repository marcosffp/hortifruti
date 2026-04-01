package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.transaction.StatementResponse;
import com.hortifruti.sl.hortifruti.exception.StatementException;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StatementService {
  private final StatementRepository statementRepository;
  private final TransactionProcessingService transactionProcessingService;

  public void saveAll(MultipartFile[] files) throws IOException {

    if (files == null || files.length == 0) {
      return;
    }

    String nameFile = files[0].getOriginalFilename();
    Bank bankParam = Bank.parseBank(nameFile, files[0]);

    Arrays.stream(files)
        .map(
            file -> {
              return saveStatementAndProcess(file, bankParam);
            })
        .collect(Collectors.toList());
  }

  private Statement saveStatementAndProcess(MultipartFile file, Bank bankParam) {

    try {
      byte[] fileBytes = file.getBytes();
      String fileName = file.getOriginalFilename();

      Statement statement = new Statement();
      statement.setName(fileName);
      statement.setFilePath(fileBytes);
      statement.setBank(bankParam);
      Statement saved = statementRepository.save(statement);
      transactionProcessingService.processFileAsync(fileBytes, fileName, saved);

      return saved;
    } catch (IOException e) {
      throw new StatementException("Erro ao processar o arquivo: " + file.getOriginalFilename(), e);
    }
  }

  public List<StatementResponse> listAll() {
    return statementRepository.findAll().stream()
        .map(s -> new StatementResponse(s.getId(), s.getName(), s.getBank(), s.getCreatedAt()))
        .collect(Collectors.toList());
  }
}
