package usagebilling.service;

import usagebilling.api.UsageEventRequest;
import usagebilling.model.ResourceType;
import usagebilling.repository.InMemoryUsageEventRepository;
import usagebilling.repository.UsageEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class UsageEventServiceTest {

    private final Instant now = Instant.parse("2026-09-23T10:00:00Z");

    private UsageEventService createService(UsageEventRepository repository) {
        return new UsageEventService(
                repository,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofMinutes(5)
        );
    }

    @Test
    void duplicateEventIsStoredOnlyOnce() {
        UsageEventRepository repository = new InMemoryUsageEventRepository();
        UsageEventService service = createService(repository);

        UsageEventRequest request = new UsageEventRequest(
                "event-1",
                "customer-1",
                ResourceType.DATABASE,
                new BigDecimal("10"),
                now
        );

        assertTrue(service.ingest(request));
        assertFalse(service.ingest(request));
        assertEquals(
                1,
                repository.findByCustomerAndTimeRange(
                        "customer-1",
                        now.minusSeconds(1),
                        now.plusSeconds(1)
                ).size()
        );
    }

    @Test
    void negativeQuantityIsRejected() {
        UsageEventService service = createService(new InMemoryUsageEventRepository());

        UsageEventRequest request = new UsageEventRequest(
                "event-1",
                "customer-1",
                ResourceType.DATABASE,
                new BigDecimal("-10"),
                now
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.ingest(request)
        );
        assertTrue(exception.getMessage().contains("quantity"));
    }

    @Test
    void missingEventIdIsRejected() {
        UsageEventService service = createService(new InMemoryUsageEventRepository());

        UsageEventRequest request = new UsageEventRequest(
                null,
                "customer-1",
                ResourceType.DATABASE,
                new BigDecimal("10"),
                now
        );

        assertThrows(IllegalArgumentException.class, () -> service.ingest(request));
    }

    @Test
    void futureTimestampBeyondSkewIsRejected() {
        UsageEventService service = createService(new InMemoryUsageEventRepository());

        UsageEventRequest request = new UsageEventRequest(
                "event-1",
                "customer-1",
                ResourceType.DATABASE,
                new BigDecimal("10"),
                now.plusSeconds(6 * 60)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.ingest(request)
        );
        assertTrue(exception.getMessage().contains("future"));
    }
}
