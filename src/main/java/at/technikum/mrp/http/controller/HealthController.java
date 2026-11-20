package at.technikum.mrp.http.controller;

import at.technikum.mrp.dto.HealthResponse;
import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;

public final class HealthController {
    public HttpResponse health(RequestContext context) {
        return HttpResponse.ok(new HealthResponse("ok"));
    }
}
