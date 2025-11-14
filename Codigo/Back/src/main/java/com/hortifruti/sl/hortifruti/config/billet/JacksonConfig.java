package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper
        .getFactory()
        .setStreamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(2000).build());

    objectMapper.registerModule(new JavaTimeModule());

    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return objectMapper;
  }
}
