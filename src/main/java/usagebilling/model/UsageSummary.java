package usagebilling.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UsageSummary(
        String customerId,
        Instant startTime,
        Instant endTime,
        List<ResourceUsage> resources,
        BigDecimal totalCost,
        String currency
) {
}