package org.proxy.metrics;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Metrics {
    private static final Logger log = LogManager.getLogger(Metrics.class);
    private static final Counter WEBHOOK_REQUESTS_TOTAL = Counter.builder()
            .name("webhook_requests_total")
                .help("Requests made to valid/invalid webhooks")
                .labelNames("valid")
                .register();

    public static void initialize() throws IOException {
        JvmMetrics.builder().register();

        HTTPServer server = HTTPServer.builder()
                .port(9400)
                .buildAndStart();

        log.debug("HTTPServer listening on port http://localhost: {}/metrics", server.getPort());
    }

    public static Counter getWebhookRequestsTotal() {
        return WEBHOOK_REQUESTS_TOTAL;
    }
}
