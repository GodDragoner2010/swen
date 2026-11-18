package at.technikum.mrp.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Router {
    public enum Access { PUBLIC, PROTECTED, OPTIONAL_AUTH }

    @FunctionalInterface
    public interface Handler {
        Object handle(RequestContext context) throws Exception;
    }

    public record Route(String method, String path, Access access, Handler handler) {
    }

    private final Map<String, Route> routes = new ConcurrentHashMap<>();

    public void add(String method, String path, Access access, Handler handler) {
        routes.put(method + " " + path, new Route(method, path, access, handler));
    }

    public Route route(String method, String path) {
        return routes.get(method + " " + path);
    }

    public Object dispatch(String method, String path, RequestContext context) throws Exception {
        Route route = route(method, path);
        if (route == null) {
            throw new RouteNotFoundException(method + " " + path);
        }
        return route.handler().handle(context);
    }

    public static final class RouteNotFoundException extends RuntimeException {
        public RouteNotFoundException(String route) {
            super("Route not found: " + route);
        }
    }
}
