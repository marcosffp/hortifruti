package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.model.*;
import com.hortifruti.sl.hortifruti.repository.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CsvGeneratorService {

  private final ClientRepository clientRepository;
  private final PurchaseRepository purchaseRepository;
  private final TransactionRepository transactionRepository;
  private final CombinedScoreRepository combinedScoreRepository;
  private final FreightConfigRepository freightConfigRepository;
  private final ProductRepository climateProductRepository;
  private final InvoiceProductRepository invoiceProductRepository;
  private final UserRepository userRepository;
  private final StatementRepository statementRepository;

  /**
   * Gera todos os arquivos CSV necessários para o backup.
   *
   * @return Lista de caminhos dos arquivos CSV gerados.
   */
  protected List<String> generateAllCSVs() {
    String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    String tempDir = System.getProperty("java.io.tmpdir");

    List<String> csvFiles = new ArrayList<>();
    csvFiles.add(generateClientsCSV(dateStr, tempDir));
    csvFiles.add(generatePurchasesCSV(dateStr, tempDir));
    csvFiles.add(generateTransactionsCSV(dateStr, tempDir));
    csvFiles.add(generateCombinedScoresCSV(dateStr, tempDir));
    csvFiles.add(generateFreightConfigCSV(dateStr, tempDir));
    csvFiles.add(generateClimateProductsCSV(dateStr, tempDir));
    csvFiles.add(generateInvoiceProductsCSV(dateStr, tempDir));
    csvFiles.add(generateUsersCSV(dateStr, tempDir));
    csvFiles.add(generateStatementsCSV(dateStr, tempDir));

    return csvFiles;
  }

  private String generateClientsCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "clientes_" + dateStr + ".csv");
    List<Client> clients = clientRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Nome",
                "CPF/CNPJ",
                "Email",
                "Telefone",
                "Endereço",
                "Preço Variável",
                "Valor Total de Compras",
                "Criado Em",
                "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Client client : clients) {
        csvPrinter.printRecord(
            client.getId(),
            client.getClientName(),
            client.getDocument(),
            client.getEmail(),
            client.getPhoneNumber(),
            client.getAddress(),
            client.isVariablePrice(),
            client.getTotalPurchaseValue(),
            client.getCreatedAt(),
            client.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de clientes.", e);
    }
  }

  private String generatePurchasesCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "compras_" + dateStr + ".csv");
    List<Purchase> purchases = purchaseRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID", "Cliente ID", "Data da Compra", "Valor Total", "Criado Em", "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Purchase purchase : purchases) {
        csvPrinter.printRecord(
            purchase.getId(),
            purchase.getClient().getId(),
            purchase.getPurchaseDate(),
            purchase.getTotal(),
            purchase.getCreatedAt(),
            purchase.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de compras.", e);
    }
  }

  private String generateTransactionsCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "transacoes_" + dateStr + ".csv");
    List<Transaction> transactions = transactionRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Data da Transação",
                "Código Histórico",
                "Histórico",
                "Valor",
                "Categoria",
                "Tipo de Transação",
                "Documento",
                "Agência de Origem",
                "Lote",
                "Hash",
                "Criado Em",
                "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Transaction transaction : transactions) {
        csvPrinter.printRecord(
            transaction.getId(),
            transaction.getTransactionDate(),
            transaction.getCodHistory(),
            transaction.getHistory(),
            transaction.getAmount(),
            transaction.getCategory(),
            transaction.getTransactionType(),
            transaction.getDocument(),
            transaction.getSourceAgency(),
            transaction.getBatch(),
            transaction.getHash(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de transações.", e);
    }
  }

  private String generateCombinedScoresCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "pontuacoes_combinadas_" + dateStr + ".csv");
    List<CombinedScore> combinedScores = combinedScoreRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Cliente ID",
                "Confirmado Em",
                "Data de Vencimento",
                "Atualizado Em",
                "Valor Total",
                "Pago")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (CombinedScore combinedScore : combinedScores) {
        csvPrinter.printRecord(
            combinedScore.getId(),
            combinedScore.getClientId(),
            combinedScore.getConfirmedAt(),
            combinedScore.getDueDate(),
            combinedScore.getTotalValue(),
            combinedScore.isPaid());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de pontuações combinadas.", e);
    }
  }

  private String generateFreightConfigCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "configuracoes_frete_" + dateStr + ".csv");
    List<FreightConfig> freightConfigs = freightConfigRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Consumo por Km",
                "Preço do Combustível",
                "Custo de Manutenção por Km",
                "Custo de Pneus por Km",
                "Custo de Depreciação por Km",
                "Custo de Seguro por Km",
                "Salário Base",
                "Percentual de Encargos",
                "Horas Trabalhadas Mensais",
                "Percentual de Custos Administrativos",
                "Percentual de Margem",
                "Taxa Fixa")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (FreightConfig config : freightConfigs) {
        csvPrinter.printRecord(
            config.getId(),
            config.getKmPerLiterConsumption(),
            config.getFuelPrice(),
            config.getMaintenanceCostPerKm(),
            config.getTireCostPerKm(),
            config.getDepreciationCostPerKm(),
            config.getInsuranceCostPerKm(),
            config.getBaseSalary(),
            config.getChargesPercentage(),
            config.getMonthlyHoursWorked(),
            config.getAdministrativeCostsPercentage(),
            config.getMarginPercentage(),
            config.getFixedFee());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de configurações de frete.", e);
    }
  }

  private String generateClimateProductsCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "produtos_climaticos_" + dateStr + ".csv");
    List<ClimateProduct> climateProducts = climateProductRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Nome",
                "Categoria de Temperatura",
                "Meses de Pico de Vendas",
                "Meses de Baixa de Vendas")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (ClimateProduct product : climateProducts) {
        csvPrinter.printRecord(
            product.getId(),
            product.getName(),
            product.getTemperatureCategory(),
            product.getPeakSalesMonths(),
            product.getLowSalesMonths());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de produtos climáticos.", e);
    }
  }

  private String generateInvoiceProductsCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "produtos_fatura_" + dateStr + ".csv");
    List<InvoiceProduct> invoiceProducts = invoiceProductRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(
                "ID",
                "Código",
                "Nome",
                "Preço",
                "Tipo de Unidade",
                "Quantidade",
                "Compra ID",
                "Criado Em",
                "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (InvoiceProduct product : invoiceProducts) {
        csvPrinter.printRecord(
            product.getId(),
            product.getCode(),
            product.getName(),
            product.getPrice(),
            product.getUnitType(),
            product.getQuantity(),
            product.getPurchase().getId(),
            product.getCreatedAt(),
            product.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de produtos da fatura.", e);
    }
  }

  private String generateUsersCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "usuarios_" + dateStr + ".csv");
    List<User> users = userRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader("ID", "Nome de Usuário", "Cargo", "Função", "Criado Em", "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (User user : users) {
        csvPrinter.printRecord(
            user.getId(),
            user.getUsername(),
            user.getPosition(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de usuários.", e);
    }
  }

  private String generateStatementsCSV(String dateStr, String tempDir) {
    Path filePath = Paths.get(tempDir, "extratos_" + dateStr + ".csv");
    List<Statement> statements = statementRepository.findAll();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader("ID", "Nome", "Banco", "Criado Em", "Atualizado Em")
            .build();

    try (FileWriter fileWriter = new FileWriter(filePath.toFile());
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)) {

      for (Statement statement : statements) {
        csvPrinter.printRecord(
            statement.getId(),
            statement.getName(),
            statement.getBank(),
            statement.getCreatedAt(),
            statement.getUpdatedAt());
      }

      csvPrinter.flush();
      return filePath.toString();
    } catch (IOException e) {
      throw new BackupException("Erro ao gerar o arquivo CSV de extratos.", e);
    }
  }
}
