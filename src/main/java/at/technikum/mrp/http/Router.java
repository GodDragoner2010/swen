package at.technikum.mrp.http;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Router {
    public enum Access { PUBLIC, PROTECTED, OPTIONAL_AUTH }

    private final Map<String, Function<HttpExchange, Object>> routes = new ConcurrentHashMap<>();

    public void add(String method, String path, Access access, Function<HttpExchange, Object> handler) {
        routes.put(method + " " + path, handler);
    }

    public Object dispatch(String method, String path, HttpExchange exchange) {
        Function<HttpExchange, Object> handler = routes.get(method + " " + path);
        if (handler == null) {
            throw new RouteNotFoundException(method + " " + path);
        }
        return handler.apply(exchange);
    }

    public static final class RouteNotFoundException extends RuntimeException {
        public RouteNotFoundException(String route) {
            super("Route not found: " + route);
        }
    }
}
