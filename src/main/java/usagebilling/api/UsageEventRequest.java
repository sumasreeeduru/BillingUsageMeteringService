package usagebilling.api;

import usagebilling.model.ResourceType;

import java.math.BigDecimal;
import java.time.Instant;

public record UsageEventRequest(
        String eventId,
        String customerId,
        ResourceType resourceType,
        BigDecimal quantity,
        Instant eventTimestamp
) {
}