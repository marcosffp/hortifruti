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
    String nameFile = files[0].getOriginalFilename();
    Bank bankParam = Bank.parseBank(nameFile);

    Arrays.stream(files)
        .map(file -> saveStatementAndProcess(file, bankParam))
        .collect(Collectors.toList());
  }

  private Statement saveStatementAndProcess(MultipartFile file, Bank bankParam) {
    try {
      Statement statement = new Statement();
      statement.setName(file.getOriginalFilename());
      statement.setFilePath(file.getBytes());
      statement.setBank(resolveBank(bankParam, file.getOriginalFilename()));
      Statement saved = statementRepository.save(statement);
      transactionProcessingService.processFileAsync(file, saved);
      return saved;
    } catch (IOException e) {
      throw new StatementException("Erro ao processar o arquivo: " + file.getOriginalFilename(), e);
    }
  }

  private Bank resolveBank(Bank bankParam, String fileName) {
    if (bankParam == Bank.UNKNOWN) {
      return Bank.parseBank(fileName);
    }
    return bankParam;
  }

  public List<StatementResponse> listAll() {
    return statementRepository.findAll().stream()
        .map(s -> new StatementResponse(s.getId(), s.getName(), s.getBank(), s.getCreatedAt()))
        .collect(Collectors.toList());
  }
}
