package at.technikum.mrp.http.controller;

import at.technikum.mrp.http.RequestContext;

import java.util.Map;

public final class HealthController {
    public Object health(RequestContext context) {
        return Map.of("status", "ok");
    }
}
