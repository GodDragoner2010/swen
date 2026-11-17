package at.technikum.mrp.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ApiServer {
    private final int port;
    private final Router router;
    private HttpServer server;

    public ApiServer(int port, Router router) {
        this.port = port;
        this.router = router;
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Object result = router.dispatch(method, path, exchange);
            writeJson(exchange, 200, new Gson().toJson(result));
        } catch (Router.RouteNotFoundException e) {
            writeJson(exchange, 404, "{\"error\":\"route not found\"}");
        } catch (Exception e) {
            writeJson(exchange, 500, "{\"error\":\"internal error\"}");
        } finally {
            exchange.close();
        }
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int port() {
        return port;
    }
}
