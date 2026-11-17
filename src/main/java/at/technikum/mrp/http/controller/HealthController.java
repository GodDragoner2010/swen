package at.technikum.mrp.http.controller;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public final class HealthController {
    public Object health(HttpExchange exchange) {
        return Map.of("status", "ok");
    }
}
