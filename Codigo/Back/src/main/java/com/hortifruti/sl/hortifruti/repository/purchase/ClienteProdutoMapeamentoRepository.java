package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.ClienteProdutoMapeamento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteProdutoMapeamentoRepository
    extends JpaRepository<ClienteProdutoMapeamento, Long> {

  Optional<ClienteProdutoMapeamento> findByClienteIdAndCodigoProdutoCliente(
      Long clienteId, String codigoProdutoCliente);

  /** Busca em lote pra um import inteiro sem uma consulta por linha do CSV. */
  List<ClienteProdutoMapeamento> findByClienteIdAndCodigoProdutoClienteIn(
      Long clienteId, Collection<String> codigosProdutoCliente);
}
