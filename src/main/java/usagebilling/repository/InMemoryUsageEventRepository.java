package usagebilling.repository;

import usagebilling.model.UsageEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUsageEventRepository
        implements UsageEventRepository {

    /*
     * eventId -> UsageEvent
     *
     * Using eventId as the key naturally enforces
     * idempotency.
     */
    private final ConcurrentMap<String, UsageEvent> events =
            new ConcurrentHashMap<>();

    @Override
    public Optional<UsageEvent> findByEventId(
            String eventId) {

        return Optional.ofNullable(
                events.get(eventId)
        );
    }

    @Override
    public boolean save(UsageEvent event) {

        /*
         * putIfAbsent is atomic.
         *
         * This is important for concurrent requests:
         *
         * Request A -> E1
         * Request B -> E1
         *
         * Only one can insert the event.
         */
        UsageEvent previous =
                events.putIfAbsent(
                        event.eventId(),
                        event
                );

        return previous == null;
    }

    @Override
    public void saveAll(List<UsageEvent> events) {

        for (UsageEvent event : events) {
            save(event);
        }
    }

    @Override
    public List<UsageEvent> findByCustomerAndTimeRange(
            String customerId,
            Instant start,
            Instant end) {

        List<UsageEvent> result =
                new ArrayList<>();

        for (UsageEvent event : events.values()) {

            if (!event.customerId()
                    .equals(customerId)) {
                continue;
            }

            if (event.eventTimestamp()
                    .isBefore(start)) {
                continue;
            }

            if (!event.eventTimestamp()
                    .isBefore(end)) {
                continue;
            }

            result.add(event);
        }

        return result;
    }

    @Override
    public List<UsageEvent> findAll() {
        return new ArrayList<>(events.values());
    }
}