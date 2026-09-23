package usagebilling.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DemoPageController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            byte[] body = "Method not allowed".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(405, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
            return;
        }

        try (InputStream input = DemoPageController.class
                .getClassLoader()
                .getResourceAsStream("static/index.html")) {

            if (input == null) {
                byte[] body = "Demo page not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
                return;
            }

            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }
}
