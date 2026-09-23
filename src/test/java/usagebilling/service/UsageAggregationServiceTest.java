package usagebilling.service;

import usagebilling.model.ResourceType;
import usagebilling.model.UsageEvent;
import usagebilling.repository.InMemoryUsageEventRepository;
import usagebilling.repository.UsageEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageAggregationServiceTest {

    @Test
    void outOfOrderEventsProduceCorrectTotal() {

        UsageEventRepository repository =
                new InMemoryUsageEventRepository();

        UsageEventService eventService =
                new UsageEventService(
                        repository
                );

        UsageAggregationService
                aggregationService =
                new UsageAggregationService(
                        repository
                );


        /*
         * Events arrive OUT OF ORDER.
         *
         * E3 -> timestamp 12:00 -> quantity 30
         * E1 -> timestamp 10:00 -> quantity 10
         * E2 -> timestamp 11:00 -> quantity 20
         */

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E3",
                        "C1",
                        ResourceType.DATABASE,
                        new BigDecimal("30"),
                        Instant.parse(
                                "2026-09-23T12:00:00Z"
                        )
                )
        );

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E1",
                        "C1",
                        ResourceType.DATABASE,
                        new BigDecimal("10"),
                        Instant.parse(
                                "2026-09-23T10:00:00Z"
                        )
                )
        );

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E2",
                        "C1",
                        ResourceType.DATABASE,
                        new BigDecimal("20"),
                        Instant.parse(
                                "2026-09-23T11:00:00Z"
                        )
                )
        );


        Map<ResourceType, BigDecimal> result =
                aggregationService.getUsage(
                        "C1",
                        Instant.parse(
                                "2026-09-23T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-24T00:00:00Z"
                        )
                );


        /*
         * Arrival order:
         *
         * 30 + 10 + 20
         *
         * Total = 60
         */

        assertEquals(
                new BigDecimal("60"),
                result.get(
                        ResourceType.DATABASE
                )
        );
    }

    @Test
    void usageIsGroupedByResourceType() {

        UsageEventRepository repository =
                new InMemoryUsageEventRepository();

        UsageEventService eventService =
                new UsageEventService(
                        repository
                );

        UsageAggregationService
                aggregationService =
                new UsageAggregationService(
                        repository
                );

        Instant day =
                Instant.parse(
                        "2026-09-23T10:00:00Z"
                );

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E1",
                        "C1",
                        ResourceType.DATABASE,
                        new BigDecimal("100"),
                        day
                )
        );

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E2",
                        "C1",
                        ResourceType.COMPUTE,
                        new BigDecimal("50"),
                        day
                )
        );

        eventService.ingest(
                new usagebilling.api.UsageEventRequest(
                        "E3",
                        "C1",
                        ResourceType.DATABASE,
                        new BigDecimal("25"),
                        day
                )
        );

        Map<ResourceType, BigDecimal> result =
                aggregationService.getUsage(
                        "C1",
                        day.minusSeconds(3600),
                        day.plusSeconds(3600)
                );

        assertEquals(
                new BigDecimal("125"),
                result.get(
                        ResourceType.DATABASE
                )
        );

        assertEquals(
                new BigDecimal("50"),
                result.get(
                        ResourceType.COMPUTE
                )
        );
    }
}