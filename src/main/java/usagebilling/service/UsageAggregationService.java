package usagebilling.service;

import usagebilling.model.ResourceType;
import usagebilling.model.UsageEvent;
import usagebilling.repository.UsageEventRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class UsageAggregationService {

    private final UsageEventRepository repository;

    public UsageAggregationService(UsageEventRepository repository) {
        this.repository = repository;
    }

    public Map<ResourceType, BigDecimal> getUsage(
            String customerId,
            Instant start,
            Instant end) {

        validate(customerId, start, end);

        List<UsageEvent> events =
                repository.findByCustomerAndTimeRange(customerId, start, end);

        Map<ResourceType, BigDecimal> totals = new EnumMap<>(ResourceType.class);
        for (UsageEvent event : events) {
            totals.merge(event.resourceType(), event.quantity(), BigDecimal::add);
        }
        return totals;
    }

    private void validate(String customerId, Instant start, Instant end) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }
}
