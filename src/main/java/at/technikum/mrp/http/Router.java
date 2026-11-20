package at.technikum.mrp.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Router {
    public enum Access {
        PUBLIC,
        OPTIONAL_AUTH,
        PROTECTED
    }

    @FunctionalInterface
    public interface Handler {
        HttpResponse handle(RequestContext context) throws Exception;
    }

    public record Route(String method, List<String> pattern, Access access, Handler handler) {
    }

    public record Match(Route route, Map<String, String> pathParams, boolean pathMatched) {
    }

    private final List<Route> routes = new ArrayList<>();

    public void add(String method, String pattern, Access access, Handler handler) {
        routes.add(new Route(
                method.toUpperCase(Locale.ROOT),
                split(pattern),
                access,
                handler
        ));
    }

    public Match match(String method, String path) {
        List<String> requested = split(path);
        boolean pathMatched = false;
        for (Route route : routes) {
            Map<String, String> params = matchPath(route.pattern(), requested);
            if (params == null) {
                continue;
            }
            pathMatched = true;
            if (route.method().equalsIgnoreCase(method)) {
                return new Match(route, params, true);
            }
        }
        return new Match(null, Map.of(), pathMatched);
    }

    private Map<String, String> matchPath(List<String> pattern, List<String> requested) {
        if (pattern.size() != requested.size()) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < pattern.size(); i++) {
            String expected = pattern.get(i);
            String actual = requested.get(i);
            if (expected.startsWith("{") && expected.endsWith("}")) {
                params.put(expected.substring(1, expected.length() - 1), actual);
            } else if (!expected.equals(actual)) {
                return null;
            }
        }
        return params;
    }

    private List<String> split(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts;
    }
}
