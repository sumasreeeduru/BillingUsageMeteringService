package usagebilling.api;

import java.util.List;

public record BatchUsageEventRequest(
        List<UsageEventRequest> events
) {
}