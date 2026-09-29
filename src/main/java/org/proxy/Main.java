package org.proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.metrics.core.metrics.Counter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.proxy.config.AppConfig;
import org.proxy.config.ConfigLoader;
import org.proxy.metrics.Metrics;

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
import java.util.stream.Collectors;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);
    private static final Set<String> IGNORE_HEADERS = Set.of("Host", "Content-length");

    static void main() throws IOException, InterruptedException {
        Metrics.initialize();
        AppConfig config = ConfigLoader.load();

        HttpServer server;
        try {
             server = HttpServer.create(
                    new InetSocketAddress(8080),
                    0
            );
        } catch (IOException e) {
            log.error("Failed to start HTTP Server", e);
            throw new RuntimeException(e);
        }

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            log.info("Received request from: {}. For {}. With headers: {}",
                    exchange.getRemoteAddress(),
                    path,
                    exchange.getRequestHeaders().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                        .collect(Collectors.joining("\t")));

            if (!config.proxyMappings().containsKey(path)) {
                Metrics.getWebhookRequestsTotal().labelValues("false").inc();
                sendWebhookNotFound(exchange);
            } else {
                Metrics.getWebhookRequestsTotal().labelValues("true").inc();
                forwardWebhook(exchange, config.proxyMappings().get(path));
            }
        });

        server.start();

        log.info("Server running on http://localhost:8080");
    }

    static void sendWebhookNotFound(HttpExchange exchange) throws IOException {
        log.debug("Request webhook endpoint not found {}", exchange.getRequestURI());
        String response = "Page Not Found!";
        exchange.sendResponseHeaders(404, response.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    static void forwardWebhook(HttpExchange exchange, String newPath) throws IOException {
        log.debug("Request map found {} -> {}", exchange.getRequestURI(), newPath);



        HttpResponse<String> response;

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofInputStream(() -> exchange.getRequestBody()))
                    .uri(URI.create(newPath));

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
            log.error("Failed to forward webhook request", e);
            throw new RuntimeException(e);
        }

        response.headers().map().forEach((name, values) -> {
            values.forEach(value -> exchange.getResponseHeaders().add(name, value));
        });

        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(response.statusCode(), body.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        } catch (Exception e) {
            log.error("Failed to write body to response", e);
        }
    }
}
