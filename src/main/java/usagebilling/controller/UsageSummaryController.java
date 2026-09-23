package usagebilling.controller;

import usagebilling.model.ResourceType;
import usagebilling.model.UsageSummary;
import usagebilling.service.PricingService;
import usagebilling.service.UsageAggregationService;
import usagebilling.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.math.BigDecimal;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class UsageSummaryController
        implements HttpHandler {

    private final UsageAggregationService aggregationService;
    private final PricingService pricingService;

    public UsageSummaryController(
            UsageAggregationService aggregationService,
            PricingService pricingService) {

        this.aggregationService =
                aggregationService;

        this.pricingService =
                pricingService;
    }

    @Override
    public void handle(
            HttpExchange exchange) {

        try {

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("GET")) {

                send(
                        exchange,
                        405,
                        Map.of(
                                "error",
                                "METHOD_NOT_ALLOWED",
                                "message",
                                "Only GET is supported"
                        )
                );

                return;
            }

            /*
             * Expected:
             *
             * /v1/customers/C1/usage
             *
             * Query:
             *
             * ?start=...&end=...
             */

            String path =
                    exchange.getRequestURI()
                            .getPath();

            String[] parts =
                    path.split("/");

            /*
             * ["", "v1", "customers", "C1", "usage"]
             */

            if (parts.length < 5) {

                throw new IllegalArgumentException(
                        "Invalid usage URL"
                );
            }

            String customerId =
                    parts[3];

            Map<String, String> query =
                    parseQuery(
                            exchange.getRequestURI()
                                    .getRawQuery()
                    );

            Instant start;
            Instant end;
            try {
                start = Instant.parse(requireQuery(query, "start"));
                end = Instant.parse(requireQuery(query, "end"));
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "start and end must be ISO-8601 timestamps, e.g. 2026-09-23T00:00:00Z"
                );
            }

            Map<ResourceType, BigDecimal> usage =
                    aggregationService.getUsage(
                            customerId,
                            start,
                            end
                    );

            UsageSummary summary =
                    pricingService.createSummary(
                            customerId,
                            start,
                            end,
                            usage
                    );

            send(
                    exchange,
                    200,
                    summary
            );

        } catch (Exception e) {
            int status = e instanceof IllegalArgumentException ? 400 : 500;
            String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
            try {
                send(exchange, status, Map.of("message", message));
            } catch (Exception ignored) {
                // Response already failed.
            }
        }
    }

    private String requireQuery(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private Map<String, String> parseQuery(
            String query) {

        if (query == null || query.isBlank()) {

            throw new IllegalArgumentException(
                    "start and end are required"
            );
        }

        Map<String, String> result =
                new java.util.HashMap<>();

        for (String parameter :
                query.split("&")) {

            String[] pair =
                    parameter.split("=", 2);

            if (pair.length == 2) {
                result.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                );
            }
        }

        return result;
    }

    private void send(
            HttpExchange exchange,
            int status,
            Object response)
            throws Exception {

        String json =
                JsonUtil.toJson(response);

        byte[] bytes =
                json.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json"
                );

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }
}