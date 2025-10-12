package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.sicoob.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.sicoob.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.sicoob.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
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
    String[] addressParts = client.getAddress().split(",");
    if (addressParts.length < 5) {
      throw new BilletException("O endereço do cliente está incompleto ou mal formatado.");
    }

    String bairro = addressParts[0].trim();
    String cidade = addressParts[1].trim();
    String cep = addressParts[2].trim();
    String uf = addressParts[3].trim();
    String rua = addressParts[4].trim();

    return new Pagador(
        client.getDocument(),
        client.getClientName(),
        rua, // Rua
        bairro, // Bairro
        cidade, // Cidade
        cep, // CEP
        uf // Estado
        );
  }
}
