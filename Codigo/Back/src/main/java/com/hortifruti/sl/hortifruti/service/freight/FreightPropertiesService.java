package com.hortifruti.sl.hortifruti.service.freight;

import com.hortifruti.sl.hortifruti.dto.freight.FreightConfigDTO;
import com.hortifruti.sl.hortifruti.exception.FreightException;
import com.hortifruti.sl.hortifruti.mapper.FreightConfigMapper;
import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FreightPropertiesService {

  private final FreightConfigRepository freightConfigRepository;
  private final FreightConfigMapper freightConfigMapper;

  public FreightConfigDTO getFreightConfig() {
    FreightConfig config =
        freightConfigRepository
            .findById(1L)
            .orElseThrow(() -> new FreightException("Configuração de frete não encontrada"));
    return freightConfigMapper.toDTO(config);
  }

  public FreightConfigDTO updateFreightConfig(FreightConfigDTO dto) {
    FreightConfig existingConfig =
        freightConfigRepository
            .findById(1L)
            .orElseThrow(() -> new FreightException("Configuração de frete não encontrada"));

    freightConfigMapper.updateEntityFromDTO(existingConfig, dto);
    FreightConfig updatedConfig = freightConfigRepository.save(existingConfig);

    return freightConfigMapper.toDTO(updatedConfig);
  }
}
