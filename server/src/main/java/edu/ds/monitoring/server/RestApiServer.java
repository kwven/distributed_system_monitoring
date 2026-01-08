package edu.ds.monitoring.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RestApiServer {
    private final AgentStore agentStore;
    private final AlertStore alertStore;
    private final MetricStore metricStore;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    public RestApiServer(int port, AgentStore agentStore, AlertStore alertStore, MetricStore metricStore) {
        this.port = port;
        this.agentStore = agentStore;
        this.alertStore = alertStore;
        this.metricStore = metricStore;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/agents", this::handleAgents);
        server.createContext("/api/alerts", this::handleAlerts);
        server.createContext("/api/metrics", this::handleMetrics);
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleAgents(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        sendJson(exchange, 200, new ArrayList<>(agentStore.all().values()));
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        long since = 0L;
        String sinceRaw = query.get("since");
        if (sinceRaw != null && !sinceRaw.isBlank()) {
            try {
                since = Long.parseLong(sinceRaw);
            } catch (NumberFormatException e) {
                sendError(exchange, 400, "invalid since");
                return;
            }
        }
        sendJson(exchange, 200, alertStore.getSince(since));
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String agentId = query.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            sendError(exchange, 400, "missing agentId");
            return;
        }
        long from;
        long to;
        try {
            from = parseLongRequired(query, "from");
            to = parseLongRequired(query, "to");
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
            return;
        }
        sendJson(exchange, 200, metricStore.getMetrics(agentId, from, to));
    }

    private long parseLongRequired(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid " + key);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        String query = uri.getRawQuery();
        Map<String, String> out = new HashMap<>();
        if (query == null || query.isEmpty()) return out;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int idx = pair.indexOf('=');
            String key;
            String value;
            if (idx >= 0) {
                key = decode(pair.substring(0, idx));
                value = decode(pair.substring(idx + 1));
            } else {
                key = decode(pair);
                value = "";
            }
            if (!key.isEmpty()) {
                out.put(key, value);
            }
        }
        return out;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Allow", "GET");
        sendError(exchange, 405, "method not allowed");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("error", message));
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
