package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.service.invoice.tax.ReportTaxService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class UserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProductRepository productRepository;
  private final FreightConfigRepository freightConfigRepository;
  private final ClientRepository clientRepository;
  private final CombinedScoreRepository combinedScoreRepository;
  private final Base64FileDecoder base64FileDecoder;
  private final ReportTaxService reportTaxService;

  @Override
  public void run(String... args) throws Exception {
    decodeBase64Files(); // Decodifica os arquivos Base64 primeiro
    initializeUsers();
    initializeFreightConfig();
    initializeClients(); // Inicializa os clientes
    inicializarCombinedScores(); // Inicializa os CombinedScores
    printReportData(); // Gera e exibe os relatórios
  }

  // Método para exibir os relatórios com o período de um mês até hoje
  private void printReportData() {
    LocalDate startDate = LocalDate.now().minusMonths(1); // Um mês atrás
    LocalDate endDate = LocalDate.now(); // Hoje

    log.info("Gerando relatórios para o período de {} até {}", startDate, endDate);

    try {
      // Gerar e exibir o relatório de ICMS
      IcmsSalesReport icmsReport = reportTaxService.generateIcmsSalesReport(startDate, endDate);
      System.out.println("Relatório de ICMS:");
      System.out.println(icmsReport);

      // Gerar e exibir os detalhes do resumo de notas fiscais
      List<InvoiceSummaryDetails> invoiceSummaries =
          reportTaxService.generateInvoiceSummaryDetails(startDate, endDate);
      System.out.println("Resumo de Notas Fiscais:");
      invoiceSummaries.forEach(System.out::println);

      // Gerar e exibir os totais de liquidação bancária
      Map<String, BigDecimal> totalLiquidacaoBancaria =
          reportTaxService.generateBankSettlementTotals(startDate, endDate);
      System.out.println("Total de Liquidação Bancária: " + totalLiquidacaoBancaria);

      // Gerar e exibir os detalhes do resumo de vendas
      List<SalesSummaryDetails> salesSummaries =
          reportTaxService.generateSalesSummaryDetails(startDate, endDate);
      System.out.println("Resumo de Vendas:");
      salesSummaries.forEach(System.out::println);

      // Gerar e exibir a lista de arquivos XML
      List<String> xmlFileList = reportTaxService.generateXmlFileList(startDate, endDate);
      System.out.println("Lista de Arquivos XML:");
      xmlFileList.forEach(System.out::println);

    } catch (Exception e) {
      log.error("Erro ao gerar relatórios: ", e);
    }
  }

  // Decodifica os arquivos Base64 necessários
  private void decodeBase64Files() {
    try {
      log.info("Decodificando arquivos Base64...");
      base64FileDecoder.decodeGoogleDriveCredentials();
      base64FileDecoder.decodePfx();
      log.info("Arquivos Base64 decodificados com sucesso!");
    } catch (Exception e) {
      log.error("Erro ao decodificar arquivos Base64: ", e);
    }
  }

  // Inicializa os usuários padrão
  private void initializeUsers() {
    if (userRepository.count() == 0) {
      createUser("root", "root", Role.MANAGER, "Desenvolvedor");
      createUser("admin", "admin", Role.EMPLOYEE, "Administrador");
    }

    if (productRepository.count() == 0) {
      log.info("Populando dados de exemplo de produtos...");
      createSampleProducts();
      log.info("Dados de exemplo criados com sucesso!");
    } else {
      log.info("Produtos já existem no banco de dados. Pulando inicialização de dados.");
    }
  }

  private void createSampleProducts() {
    // Produtos QUENTES (>=25°C)
    productRepository.save(
        new ClimateProduct(
            "Melancia",
            TemperatureCategory.QUENTE,
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO, Month.MARCO), // Verão
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO) // Inverno
            ));

    productRepository.save(
        new ClimateProduct(
            "Abacaxi",
            TemperatureCategory.QUENTE,
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO)));

    productRepository.save(
        new ClimateProduct(
            "Água de Coco",
            TemperatureCategory.QUENTE,
            List.of(Month.NOVEMBRO, Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO, Month.MARCO),
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO)));

    // Produtos AMENOS (15-24°C)
    productRepository.save(
        new ClimateProduct(
            "Maçã",
            TemperatureCategory.AMENO,
            List.of(Month.MARCO, Month.ABRIL, Month.MAIO, Month.SETEMBRO, Month.OUTUBRO),
            List.of(Month.DEZEMBRO, Month.JANEIRO)));

    productRepository.save(
        new ClimateProduct(
            "Banana",
            TemperatureCategory.AMENO,
            List.of(
                Month.MARCO,
                Month.ABRIL,
                Month.MAIO,
                Month.SETEMBRO,
                Month.OUTUBRO,
                Month.NOVEMBRO),
            List.of(Month.JULHO, Month.AGOSTO)));

    // Produtos FRIOS (6-14°C)
    productRepository.save(
        new ClimateProduct(
            "Batata Doce",
            TemperatureCategory.FRIO,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO)));

    productRepository.save(
        new ClimateProduct(
            "Mandioca",
            TemperatureCategory.FRIO,
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO)));

    // Produtos CONGELANDO (<=5°C)
    productRepository.save(
        new ClimateProduct(
            "Gengibre",
            TemperatureCategory.CONGELANDO,
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO),
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO)));

    log.info("Criados {} produtos de exemplo", productRepository.count());
  }

  // Cria um usuário com os dados fornecidos
  private void createUser(String username, String password, Role role, String position) {
    User user =
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(role)
            .position(position)
            .build();
    userRepository.save(user);
    System.out.println("Usuário " + username + " criado com sucesso!");
  }

  // Inicializa a configuração de frete padrão
  private void initializeFreightConfig() {
    if (freightConfigRepository.count() == 0) {
      FreightConfig defaultConfig = createDefaultFreightConfig();
      freightConfigRepository.save(defaultConfig);
      System.out.println("Configuração de frete padrão criada com sucesso!");
    }
  }

  // Cria a configuração de frete padrão
  private FreightConfig createDefaultFreightConfig() {
    return FreightConfig.builder()
        .kmPerLiterConsumption(10.0)
        .fuelPrice(6.30)
        .maintenanceCostPerKm(0.15)
        .tireCostPerKm(0.04)
        .depreciationCostPerKm(0.53)
        .insuranceCostPerKm(0.14)
        .baseSalary(1600.00)
        .chargesPercentage(39.37)
        .monthlyHoursWorked(192.0)
        .administrativeCostsPercentage(15.0)
        .marginPercentage(20.0)
        .fixedFee(3.00)
        .build();
  }

  // Inicializa os clientes padrão
  private void initializeClients() {
    if (clientRepository.count() == 0) {
      createClient(
          "Llinea Irani",
          "llinea.irani@example.com",
          "123456789",
          "Rua Exemplo, 123",
          "12345678900",
          false);
    }
  }

  // Cria um cliente com os dados fornecidos
  private void createClient(
      String clientName,
      String email,
      String phoneNumber,
      String address,
      String document,
      boolean variablePrice) {
    Client client =
        Client.builder()
            .clientName(clientName)
            .email(email)
            .phoneNumber(phoneNumber)
            .address(address)
            .document(document)
            .variablePrice(variablePrice)
            .build();
    clientRepository.save(client);
    System.out.println("Cliente " + clientName + " criado com sucesso!");
  }

  private void createCombinedScore(
      Long clientId, LocalDate dueDate, BigDecimal totalValue, boolean paid) {
    CombinedScore combinedScore =
        CombinedScore.builder()
            .clientId(clientId)
            .dueDate(dueDate)
            .totalValue(totalValue)
            .status(
                paid
                    ? Status.PAGO
                    : Status.PENDENTE) // Define o status com base no parâmetro 'paid'
            .hasBillet(false) // Valor padrão para 'hasBillet'
            .hasInvoice(false) // Valor padrão para 'hasInvoice'
            .build();

    combinedScoreRepository.save(combinedScore);
    System.out.println("CombinedScore criado para o cliente ID: " + clientId);
  }

  private void inicializarCombinedScores() {
    if (combinedScoreRepository.count() == 0) {
      createCombinedScore(2L, LocalDate.now().plusDays(15), BigDecimal.valueOf(200.00), true);
    }
  }
}
