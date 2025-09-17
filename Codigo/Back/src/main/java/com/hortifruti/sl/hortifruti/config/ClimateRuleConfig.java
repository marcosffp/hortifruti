package com.hortifruti.sl.hortifruti.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "climate.rules")
public class ClimateRuleConfig {
    private Map<String, TagConfig> tags;
    
    @Data
    public static class TagConfig {
        private List<RuleCondition> rules;
        private double baseValue; // base suggestion value for this tag
    }
    
    @Data
    public static class RuleCondition {
        private String type; // "temperature" or "rainfall"
        private String condition; // "gte", "lte", "gt", "lt", "eq", "between"
        private double value;
        private double min; // for "between" condition
        private double max; // for "between" condition
    }
}