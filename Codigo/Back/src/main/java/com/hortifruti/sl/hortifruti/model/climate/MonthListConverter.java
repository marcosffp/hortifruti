package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class MonthListConverter implements AttributeConverter<List<Month>, String> {

  private static final String SEPARATOR = ",";

  @Override
  public String convertToDatabaseColumn(List<Month> months) {
    if (months == null || months.isEmpty()) {
      return null;
    }
    return months.stream().map(Month::name).collect(Collectors.joining(SEPARATOR));
  }

  @Override
  public List<Month> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.stream(dbData.split(SEPARATOR))
        .map(String::trim)
        .map(Month::valueOf)
        .collect(Collectors.toList());
  }
}
