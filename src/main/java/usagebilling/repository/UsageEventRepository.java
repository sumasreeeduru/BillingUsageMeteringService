package usagebilling.repository;

import usagebilling.model.UsageEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsageEventRepository {

    Optional<UsageEvent> findByEventId(String eventId);

    boolean save(UsageEvent event);

    void saveAll(List<UsageEvent> events);

    List<UsageEvent> findByCustomerAndTimeRange(
            String customerId,
            Instant start,
            Instant end
    );

    List<UsageEvent> findAll();

    void clear();
}