package at.technikum.mrp.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Router {
    public enum Access { PUBLIC, PROTECTED, OPTIONAL_AUTH }

    @FunctionalInterface
    public interface Handler {
        Object handle(RequestContext context) throws Exception;
    }

    private final Map<String, Handler> routes = new ConcurrentHashMap<>();

    public void add(String method, String path, Access access, Handler handler) {
        routes.put(method + " " + path, handler);
    }

    public Object dispatch(String method, String path, RequestContext context) throws Exception {
        Handler handler = routes.get(method + " " + path);
        if (handler == null) {
            throw new RouteNotFoundException(method + " " + path);
        }
        return handler.handle(context);
    }

    public static final class RouteNotFoundException extends RuntimeException {
        public RouteNotFoundException(String route) {
            super("Route not found: " + route);
        }
    }
}
