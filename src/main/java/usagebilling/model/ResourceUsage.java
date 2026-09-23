package usagebilling.model;

import java.math.BigDecimal;

public record ResourceUsage(
        ResourceType resourceType,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        BigDecimal cost
) {
}