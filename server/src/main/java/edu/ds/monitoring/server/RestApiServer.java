package edu.ds.monitoring.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RestApiServer {

    private final AgentStore agentStore;
    private final RmiMonitoringServiceImpl rmiService; // pour réutiliser get/setThresholds
    private final int port;
    private HttpServer server;

    public RestApiServer(int port, AgentStore agentStore, RmiMonitoringServiceImpl rmiService) {
        this.port = port;
        this.agentStore = agentStore;
        this.rmiService = rmiService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/agents", this::handleAgents);
        server.createContext("/api/thresholds", this::handleThresholds);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("[REST] Listening on http://localhost:" + port + "/api");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ---------- Handlers ----------

    private void handleHealth(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        sendJson(ex, 200, "{\"status\":\"ok\"}");
    }

    // GET /api/agents               -> list
    // GET /api/agents/{agentId}     -> one
    private void handleAgents(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String path = ex.getRequestURI().getPath(); // /api/agents or /api/agents/xxx
        String[] parts = path.split("/");
        if (parts.length == 3) { // ["", "api", "agents"]
            List<AgentSnapshot> list = agentStore.all().values().stream().collect(Collectors.toList());
            sendJson(ex, 200, toJsonAgents(list));
            return;
        }

        if (parts.length == 4) { // /api/agents/{id}
            String agentId = parts[3];
            AgentSnapshot s = agentStore.getLatest(agentId);
            if (s == null) {
                sendJson(ex, 404, "{\"error\":\"Agent not found\"}");
            } else {
                sendJson(ex, 200, toJsonAgent(s));
            }
            return;
        }

        sendJson(ex, 404, "{\"error\":\"Not Found\"}");
    }

    // GET  /api/thresholds/{agentId}
    // POST /api/thresholds/{agentId}  body: {"cpuWarnPct":70,...}
    private void handleThresholds(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath(); // /api/thresholds/{id}
        String[] parts = path.split("/");
        if (parts.length != 4) {
            sendJson(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }
        String agentId = parts[3];

        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                ThresholdConfig cfg = rmiService.getThresholds(agentId);
                sendJson(ex, 200, toJsonThresholds(cfg));
                return;
            }

            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = readBody(ex);
                ThresholdConfig cfg = parseThresholds(body);
                rmiService.setThresholds(agentId, cfg);
                sendJson(ex, 200, "{\"status\":\"updated\"}");
                return;
            }

            sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    // ---------- JSON helpers (sans lib) ----------

    private static String toJsonAgents(List<AgentSnapshot> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJsonAgent(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJsonAgent(AgentSnapshot s) {
        return "{"
                + "\"agentId\":\"" + esc(s.agentId) + "\","
                + "\"host\":\"" + esc(s.host) + "\","
                + "\"ip\":\"" + esc(s.ip) + "\","
                + "\"lastSeenTs\":" + s.lastSeenTs + ","
                + "\"status\":\"" + (s.status == null ? "UNKNOWN" : esc(s.status.toString())) + "\","
                + "\"cpuPct\":" + round1(s.cpuPct) + ","
                + "\"ramPct\":" + round1(s.ramPct) + ","
                + "\"diskPct\":" + round1(s.diskPct)
                + "}";
    }

    private static String toJsonThresholds(ThresholdConfig t) {
        return "{"
                + "\"cpuWarnPct\":" + t.cpuWarnPct + ","
                + "\"cpuCritPct\":" + t.cpuCritPct + ","
                + "\"ramWarnPct\":" + t.ramWarnPct + ","
                + "\"ramCritPct\":" + t.ramCritPct + ","
                + "\"diskWarnPct\":" + t.diskWarnPct + ","
                + "\"diskCritPct\":" + t.diskCritPct
                + "}";
    }

    private static ThresholdConfig parseThresholds(String json) {
        ThresholdConfig t = new ThresholdConfig();
        // parse numbers via SimpleJson (déjà chez toi)
        Double v;

        v = SimpleJson.getDouble(json, "cpuWarnPct"); if (v != null) t.cpuWarnPct = v;
        v = SimpleJson.getDouble(json, "cpuCritPct"); if (v != null) t.cpuCritPct = v;
        v = SimpleJson.getDouble(json, "ramWarnPct"); if (v != null) t.ramWarnPct = v;
        v = SimpleJson.getDouble(json, "ramCritPct"); if (v != null) t.ramCritPct = v;
        v = SimpleJson.getDouble(json, "diskWarnPct"); if (v != null) t.diskWarnPct = v;
        v = SimpleJson.getDouble(json, "diskCritPct"); if (v != null) t.diskCritPct = v;

        return t;
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static double round1(double x) {
        return Math.round(x * 10.0) / 10.0;
    }
}
