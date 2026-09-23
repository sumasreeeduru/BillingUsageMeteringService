package usagebilling.model;

import java.math.BigDecimal;
import java.time.Instant;

public record UsageEvent(
        String eventId,
        String customerId,
        ResourceType resourceType,
        BigDecimal quantity,
        Instant eventTimestamp,
        Instant receivedAt
) {
}