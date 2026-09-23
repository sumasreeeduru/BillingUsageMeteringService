package usagebilling.controller;

import usagebilling.api.BatchUsageEventRequest;
import usagebilling.api.UsageEventRequest;
import usagebilling.service.UsageEventService;
import usagebilling.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UsageEventController
        implements HttpHandler {

    private final UsageEventService service;

    public UsageEventController(
            UsageEventService service) {

        this.service = service;
    }

    @Override
    public void handle(
            HttpExchange exchange) throws IOException {

        try {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, service.listAll());
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                sendJson(
                        exchange,
                        405,
                        Map.of(
                                "error",
                                "METHOD_NOT_ALLOWED",
                                "message",
                                "Only GET and POST are supported"
                        )
                );
                return;
            }

            String path =
                    exchange.getRequestURI()
                            .getPath();

            String body =
                    new String(
                            exchange.getRequestBody()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            if (path.endsWith("/batch")) {
                handleBatch(exchange, body);
            } else {
                handleSingle(exchange, body);
            }

        } catch (Exception e) {
            int status = e instanceof IllegalArgumentException ? 400 : 500;
            String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
            try {
                sendJson(exchange, status, Map.of("message", message));
            } catch (Exception ignored) {
                // Response already failed.
            }
        }
    }

    private void handleSingle(
            HttpExchange exchange,
            String body) throws Exception {

        UsageEventRequest request =
                JsonUtil.fromJson(
                        body,
                        UsageEventRequest.class
                );

        boolean created =
                service.ingest(request);

        if (created) {

            sendJson(
                    exchange,
                    200,
                    Map.of(
                            "status",
                            "accepted",
                            "message",
                            "Event persisted successfully"
                    )
            );

        } else {

            /*
             * Duplicate is intentionally 200.
             */
            sendJson(
                    exchange,
                    200,
                    Map.of(
                            "status",
                            "duplicate",
                            "message",
                            "Event was already processed"
                    )
            );
        }
    }

    private void handleBatch(
            HttpExchange exchange,
            String body) throws Exception {

        BatchUsageEventRequest request =
                JsonUtil.fromJson(
                        body,
                        BatchUsageEventRequest.class
                );

        service.ingestBatch(
                request.events()
        );

        sendJson(
                exchange,
                200,
                Map.of(
                        "status",
                        "accepted",
                        "message",
                        "Batch persisted successfully"
                )
        );
    }

    private void sendJson(
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