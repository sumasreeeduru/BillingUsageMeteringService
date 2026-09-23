package usagebilling.service;

import usagebilling.api.UsageEventRequest;
import usagebilling.model.UsageEvent;
import usagebilling.repository.UsageEventRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class UsageEventService {

    private final UsageEventRepository repository;
    private final Clock clock;
    private final Duration maxFutureSkew;

    public UsageEventService(UsageEventRepository repository) {
        this(repository, Clock.systemUTC(), Duration.ofMinutes(5));
    }

    public UsageEventService(
            UsageEventRepository repository,
            Clock clock,
            Duration maxFutureSkew) {
        this.repository = repository;
        this.clock = clock;
        this.maxFutureSkew = maxFutureSkew;
    }

    public boolean ingest(UsageEventRequest request) {
        validate(request);
        boolean created = repository.save(createEvent(request));
        List<UsageEvent> all = repository.findAll();
        System.out.println("Stored events (" + all.size() + "): " + all);
        return created;
    }

    public List<UsageEvent> listAll() {
        List<UsageEvent> all = repository.findAll();
        System.out.println("Stored events (" + all.size() + "): " + all);
        return all;
    }

    public void clearAll() {
        repository.clear();
        System.out.println("Stored events cleared");
    }

    public void ingestBatch(List<UsageEventRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("events must contain at least one event");
        }

        for (UsageEventRequest request : requests) {
            validate(request);
        }

        Instant receivedAt = clock.instant();
        for (UsageEventRequest request : requests) {
            repository.save(new UsageEvent(
                    request.eventId(),
                    request.customerId(),
                    request.resourceType(),
                    request.quantity(),
                    request.eventTimestamp(),
                    receivedAt
            ));
        }
    }

    private UsageEvent createEvent(UsageEventRequest request) {
        return new UsageEvent(
                request.eventId(),
                request.customerId(),
                request.resourceType(),
                request.quantity(),
                request.eventTimestamp(),
                clock.instant()
        );
    }

    private void validate(UsageEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        if (isBlank(request.eventId())
                || isBlank(request.customerId())
                || request.resourceType() == null
                || request.quantity() == null
                || request.eventTimestamp() == null) {
            throw new IllegalArgumentException(
                    "eventId, customerId, resourceType, quantity and eventTimestamp are required"
            );
        }

        if (request.quantity().signum() < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }

        Instant latestAllowed = clock.instant().plus(maxFutureSkew);
        if (request.eventTimestamp().isAfter(latestAllowed)) {
            throw new IllegalArgumentException(
                    "eventTimestamp cannot be more than "
                            + maxFutureSkew.toMinutes()
                            + " minutes in the future"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
