package usagebilling.service;

import usagebilling.model.ResourceType;
import usagebilling.model.UsageSummary;
import usagebilling.repository.InMemoryPriceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    @Test
    void createSummary_appliesUnitPricesAndTotalsCost() {
        PricingService pricingService =
                new PricingService(new InMemoryPriceRepository());

        Instant start = Instant.parse("2026-09-23T00:00:00Z");
        Instant end = Instant.parse("2026-09-24T00:00:00Z");

        Map<ResourceType, BigDecimal> usage = new EnumMap<>(ResourceType.class);
        usage.put(ResourceType.DATABASE, new BigDecimal("100"));
        usage.put(ResourceType.COMPUTE, new BigDecimal("50"));

        UsageSummary summary =
                pricingService.createSummary("C1", start, end, usage);

        assertEquals("C1", summary.customerId());
        assertEquals(start, summary.startTime());
        assertEquals(end, summary.endTime());
        assertEquals("USD", summary.currency());
        assertEquals(2, summary.resources().size());
        // DATABASE: 100 * 0.05 = 5.00; COMPUTE: 50 * 0.03 = 1.50
        assertEquals(new BigDecimal("6.50"), summary.totalCost());
    }
}
