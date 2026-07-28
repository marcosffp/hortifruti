package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Sincroniza o catálogo fiscal (products.yml, hoje usado apenas pelo ProductNFService na emissão
 * de NF-e) com a tabela {@code fiscal_products}, usada para alimentar o dropdown de produtos na
 * criação manual de compras. Parsing independente do ProductNFService de propósito, para não
 * arriscar o fluxo de emissão de NF-e que já funciona.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class FiscalProductInitializer implements CommandLineRunner {

  private final FiscalProductRepository fiscalProductRepository;

  @Override
  public void run(String... args) {
    List<Map<String, Object>> yamlProducts = loadYamlProducts();

    Map<String, FiscalProduct> existingByCode =
        fiscalProductRepository.findAll().stream()
            .collect(Collectors.toMap(FiscalProduct::getCode, Function.identity()));

    List<FiscalProduct> toInsert = new ArrayList<>();
    List<FiscalProduct> toUpdate = new ArrayList<>();
    int unchanged = 0;

    for (Map<String, Object> entry : yamlProducts) {
      String code = String.valueOf(entry.get("code"));
      String description = String.valueOf(entry.get("description"));
      String ncm = String.valueOf(entry.get("ncm"));
      String cfop = String.valueOf(entry.get("cfop"));
      String icms = String.valueOf(entry.get("icms"));
      String unidadeComercial = String.valueOf(entry.get("unidade_comercial"));
      String unidadeTributavel = String.valueOf(entry.get("unidade_tributavel"));

      FiscalProduct existing = existingByCode.get(code);
      if (existing == null) {
        toInsert.add(
            FiscalProduct.builder()
                .code(code)
                .description(description)
                .ncm(ncm)
                .cfop(cfop)
                .icms(icms)
                .unidadeComercial(unidadeComercial)
                .unidadeTributavel(unidadeTributavel)
                .build());
        continue;
      }

      boolean identical =
          Objects.equals(existing.getDescription(), description)
              && Objects.equals(existing.getNcm(), ncm)
              && Objects.equals(existing.getCfop(), cfop)
              && Objects.equals(existing.getIcms(), icms)
              && Objects.equals(existing.getUnidadeComercial(), unidadeComercial)
              && Objects.equals(existing.getUnidadeTributavel(), unidadeTributavel);

      if (identical) {
        unchanged++;
        continue;
      }

      existing.setDescription(description);
      existing.setNcm(ncm);
      existing.setCfop(cfop);
      existing.setIcms(icms);
      existing.setUnidadeComercial(unidadeComercial);
      existing.setUnidadeTributavel(unidadeTributavel);
      toUpdate.add(existing);
    }

    if (!toInsert.isEmpty()) {
      fiscalProductRepository.saveAll(toInsert);
    }
    if (!toUpdate.isEmpty()) {
      fiscalProductRepository.saveAll(toUpdate);
    }

    log.info(
        "Catálogo fiscal sincronizado: {} inseridos, {} atualizados, {} inalterados.",
        toInsert.size(),
        toUpdate.size(),
        unchanged);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> loadYamlProducts() {
    Yaml yaml = new Yaml();
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("products.yml")) {
      Map<String, Object> data = yaml.load(inputStream);
      return (List<Map<String, Object>>) data.get("products");
    } catch (Exception e) {
      throw new RuntimeException(
          "Erro ao carregar catálogo fiscal (products.yml): " + e.getMessage(), e);
    }
  }
}
