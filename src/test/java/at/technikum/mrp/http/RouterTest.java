package at.technikum.mrp.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouterTest {
    private final Router router = new Router();

    @Test
    void storesAccessLevelPerRoute() {
        router.add("GET", "/api/health", Router.Access.PUBLIC, context -> HttpResponse.ok("ok"));
        router.add("POST", "/api/media", Router.Access.PROTECTED, context -> HttpResponse.ok("ok"));

        assertEquals(Router.Access.PUBLIC, router.route("GET", "/api/health").access());
        assertEquals(Router.Access.PROTECTED, router.route("POST", "/api/media").access());
        assertNull(router.route("GET", "/api/media"));
    }

    @Test
    void dispatchInvokesTheRegisteredHandler() throws Exception {
        router.add("GET", "/api/health", Router.Access.PUBLIC,
                context -> HttpResponse.ok("hello"));

        assertEquals(HttpResponse.ok("hello"), router.dispatch("GET", "/api/health", null));
    }

    @Test
    void dispatchReportsUnknownRoute() {
        assertThrows(Router.RouteNotFoundException.class,
                () -> router.dispatch("GET", "/api/unknown", null));
    }
}
