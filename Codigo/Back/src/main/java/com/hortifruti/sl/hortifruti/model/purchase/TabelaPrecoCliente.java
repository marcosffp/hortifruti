package com.hortifruti.sl.hortifruti.model.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabela de preços de um cliente (ex.: LLinea) pra uma competência (mês/ano), importada de um CSV
 * oficial do cliente. {@code versao} existe pra permitir reimport de uma competência já {@code
 * CONFIRMADA} (o cliente reenvia a tabela corrigida): um reimport nunca sobrescreve a versão
 * confirmada anterior, sempre cria uma nova linha com {@code versao+1} — ver {@code
 * TabelaPrecoClienteImportService}. Só uma versão por competência costuma estar {@code CONFIRMADA}
 * de cada vez (a mais recente é a autoritativa), mas versões antigas ficam no histórico.
 */
@Entity
@Table(name = "tabelas_preco_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TabelaPrecoCliente {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(name = "competencia_mes", nullable = false)
  private int competenciaMes;

  @Column(name = "competencia_ano", nullable = false)
  private int competenciaAno;

  @Column(name = "vigencia_inicio", nullable = false)
  private LocalDate vigenciaInicio;

  @Column(name = "vigencia_fim", nullable = false)
  private LocalDate vigenciaFim;

  @Column(nullable = false)
  private int versao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusTabelaPreco status;

  @Column(name = "origem_arquivo_nome")
  private String origemArquivoNome;

  @Column(name = "importado_em", nullable = false)
  private LocalDateTime importadoEm;

  @Column(name = "importado_por")
  private Long importadoPor;

  @Column(name = "confirmado_em")
  private LocalDateTime confirmadoEm;

  @Column(name = "confirmado_por")
  private Long confirmadoPor;

  @PrePersist
  protected void onCreate() {
    this.importadoEm = LocalDateTime.now(BRAZIL_ZONE);
  }
}
