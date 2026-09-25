package org.example;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final String TARGET_ENDPOINT = "https://portainer.internal.fitzmaurice.me";
    private static final Set<String> IGNORE_HEADERS = Set.of("Host");

    static void main() {
        Map<String, String> webhookMapping = Map.of(
                "/hello", "/api/stacks/webhooks/2a933c8a-e9d6-492a-906a-a69e7dfc4c9b"
        );
        HttpServer server;
        try {
             server = HttpServer.create(
                    new InetSocketAddress(8080),
                    0
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();

            if (!webhookMapping.containsKey(path)) {
                sendWebhookNotFound(exchange);
            } else {
                forwardWebhook(exchange, webhookMapping.get(path));
            }
        });

        server.start();

        System.out.println("Server running on http://localhost:8080");
    }

    static void sendWebhookNotFound(HttpExchange exchange) throws IOException {
        String response = "Page Not Found!";
        exchange.sendResponseHeaders(404, response.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    static void forwardWebhook(HttpExchange exchange, String newPath) throws IOException {
        HttpResponse<String> response;

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofInputStream(() -> exchange.getRequestBody()))
                    .uri(URI.create(TARGET_ENDPOINT + newPath));

            exchange.getRequestHeaders().forEach(
                    (name, values) -> values.forEach(
                            value -> {
                                if (!IGNORE_HEADERS.contains(name)) builder.header(name, value);
                            }
                    )
            );

            response = client.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        response.headers().map().forEach((name, values) -> {
            values.forEach(value -> exchange.getResponseHeaders().add(name, value));
        });

        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(response.statusCode(), body.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
