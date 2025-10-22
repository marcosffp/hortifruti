package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BilletFactory {

  @Value("${sicoob.num.cliente}")
  private Integer clientNumber;

  @Value("${sicoob.num.conta.corrente}")
  private Integer accountNumber;

  private Integer MODALITY_CODE = 1;
  private String DOCUMENT_SPECIES_CODE = "DM";
  private Integer BOLETO_ISSUANCE_IDENTIFICATION = 1;
  private Integer BOLETO_DISTRIBUTION_IDENTIFICATION = 1;
  private Integer DISCOUNT_TYPE = 0;
  private Integer FINE_TYPE = 0;
  private Integer INTEREST_TYPE = 3;
  private Integer INSTALLMENT_NUMBER = 1;
  private Boolean GENERATE_PDF = true;

  public BilletRequest createCompleteBoletoRequest(BilletRequestSimplified boletoSimplificado) {
    return new BilletRequest(
        clientNumber, // numeroCliente
        MODALITY_CODE, // codigoModalidade
        accountNumber, // numeroContaCorrente
        DOCUMENT_SPECIES_CODE, // codigoEspecieDocumento
        boletoSimplificado.dataEmissao(),
        boletoSimplificado.seuNumero(),
        BOLETO_ISSUANCE_IDENTIFICATION, // identificacaoEmissaoBoleto
        BOLETO_DISTRIBUTION_IDENTIFICATION, // identificacaoDistribuicaoBoleto
        boletoSimplificado.valor(),
        boletoSimplificado.dataVencimento(),
        DISCOUNT_TYPE, // tipoDesconto
        FINE_TYPE, // tipoMulta
        INTEREST_TYPE, // tipoJurosMora
        INSTALLMENT_NUMBER, // numeroParcela
        boletoSimplificado.pagador(),
        GENERATE_PDF // gerarPdf
        );
  }

  /**
   * Cria o objeto BilletRequestSimplified a partir do CombinedScore e do Pagador.
   *
   * @param combinedScore CombinedScore
   * @param combinedScoreId ID do CombinedScore
   * @param pagador Objeto Pagador
   * @param number Seu número (identificador do boleto)
   * @return Objeto BilletRequestSimplified
   */
  public BilletRequestSimplified createBilletRequest(
      CombinedScore combinedScore, Long combinedScoreId, Pagador pagador, String number) {
    return new BilletRequestSimplified(
        combinedScore.getConfirmedAt().toString(), // Data de emissão
        number, // Seu número (identificador do boleto)
        combinedScore.getTotalValue(), // Valor total
        combinedScore.getDueDate().toString(), // Data de vencimento
        pagador // Pagador
        );
  }

  /**
   * Cria o objeto Pagador a partir dos dados do cliente.
   *
   * @param client Cliente
   * @return Objeto Pagador
   */
  public Pagador createPagadorFromClient(Client client) {
    // Exemplo de address recebido do banco de dados:
    // "Sítio Boa Vista,Santa Luzia,33040-257,MG,Rua Quartzolit, 70"
    String address = client.getAddress();

    try {
      // Divide o endereço em partes
      String[] addressParts = address.split(",");
      if (addressParts.length < 6) {
        throw new BilletException("O endereço do cliente está incompleto. Formato esperado: 'Sítio, Cidade, CEP, UF, Rua, Número'");
      }

      // Extrai as informações com base nas posições fixas
      String rua = addressParts[4].trim(); // Rua está na 5ª posição
      String numero = addressParts[5].trim(); // Número está na 6ª posição
      String bairro = addressParts[0].trim(); // Bairro está na 1ª posição
      String cidade = addressParts[1].trim(); // Cidade está na 2ª posição
      String cep = addressParts[2].trim(); // CEP está na 3ª posição
      String uf = addressParts[3].trim(); // UF está na 4ª posição

      // Remove "CEP:" e caracteres não numéricos do CEP, se necessário
      cep = cep.replaceAll("[^0-9]", "");

      // Validações básicas
      if (rua.isEmpty()) {
        throw new BilletException("Rua não pode estar vazia no endereço do cliente.");
      }
      if (numero.isEmpty()) {
        throw new BilletException("Número não pode estar vazio no endereço do cliente.");
      }
      if (bairro.isEmpty()) {
        throw new BilletException("Bairro não pode estar vazio no endereço do cliente.");
      }
      if (cidade.isEmpty()) {
        throw new BilletException("Cidade não pode estar vazia no endereço do cliente.");
      }
      if (uf.isEmpty()) {
        throw new BilletException("UF não pode estar vazia no endereço do cliente.");
      }
      if (cep.length() != 8) {
        throw new BilletException("CEP deve conter exatamente 8 dígitos numéricos.");
      }

      // Monta endereço completo para o campo "endereco"
      String enderecoCompleto = rua + ", " + numero;

      return new Pagador(
          client.getDocument().replaceAll("[^0-9]", ""), // Remove formatação do CPF/CNPJ
          client.getClientName(),
          enderecoCompleto, // Rua + número
          bairro,
          cidade,
          cep,
          uf);

    } catch (Exception e) {
      throw new BilletException(
          "Erro ao processar endereço do cliente: " + e.getMessage() + 
          ". Endereço recebido: '" + address + "'", e);
    }
  }
}
