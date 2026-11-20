package at.technikum.mrp.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {
    private final Router router = new Router();

    @Test
    void matchesPathParametersAndMethod() {
        router.add("GET", "/api/media/{mediaId}", Router.Access.PUBLIC, context -> HttpResponse.ok("ok"));

        Router.Match match = router.match("GET", "/api/media/17");

        assertEquals("17", match.pathParams().get("mediaId"));
        assertTrue(match.pathMatched());
        assertTrue(match.route() != null);
    }

    @Test
    void reportsPathMatchWhenMethodIsWrong() {
        router.add("POST", "/api/ratings/{ratingId}/like", Router.Access.PROTECTED,
                context -> HttpResponse.ok("ok"));

        Router.Match match = router.match("GET", "/api/ratings/3/like");

        assertNull(match.route());
        assertTrue(match.pathMatched());
    }

    @Test
    void reportsNoMatchForUnknownPath() {
        router.add("GET", "/api/leaderboard", Router.Access.PUBLIC, context -> HttpResponse.ok("ok"));

        Router.Match match = router.match("GET", "/api/unknown");

        assertNull(match.route());
        assertFalse(match.pathMatched());
    }

    @Test
    void ignoresTrailingSlashes() {
        router.add("GET", "/api/media", Router.Access.PUBLIC, context -> HttpResponse.ok("ok"));

        assertTrue(router.match("GET", "/api/media/").route() != null);
    }
}
