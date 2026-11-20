package at.technikum.mrp.http;

import at.technikum.mrp.service.UnauthorizedException;
import at.technikum.mrp.service.ValidationException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RequestContext {
    private static final int MAX_BODY_BYTES = 1_048_576;

    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final Long userId;
    private final Gson gson;
    private byte[] body;

    public RequestContext(HttpExchange exchange,
                          Map<String, String> pathParams,
                          Long userId,
                          Gson gson) {
        this.exchange = exchange;
        this.pathParams = pathParams;
        this.queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
        this.userId = userId;
        this.gson = gson;
    }

    public Long userId() {
        return userId;
    }

    public long viewerId() {
        return userId == null ? 0 : userId;
    }

    public long requireUserId() {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }

    public String path(String name) {
        return pathParams.get(name);
    }

    public long pathLong(String name) {
        String value = pathParams.get(name);
        if (value == null) {
            throw new ValidationException("Missing path parameter: " + name);
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 1) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ValidationException("Path parameter '" + name + "' must be a positive integer");
        }
    }

    public String query(String name) {
        String value = queryParams.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Integer queryInt(String name) {
        String value = query(name);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Query parameter '" + name + "' must be an integer");
        }
    }

    public Double queryDouble(String name) {
        String value = query(name);
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Query parameter '" + name + "' must be a number");
        }
    }

    public <T> T body(Class<T> type) {
        byte[] bytes = bodyBytes();
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            throw HttpException.unsupportedMediaType("Content-Type must be application/json");
        }
        if (bytes.length == 0) {
            throw new ValidationException("Request body is required");
        }
        try {
            T value = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
            if (value == null) {
                throw new ValidationException("Request body is required");
            }
            return value;
        } catch (JsonParseException ex) {
            throw new ValidationException("Malformed JSON body");
        }
    }

    private byte[] bodyBytes() {
        if (body != null) {
            return body;
        }
        try {
            body = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        } catch (IOException ex) {
            throw new ValidationException("Could not read request body");
        }
        if (body.length > MAX_BODY_BYTES) {
            throw HttpException.payloadTooLarge("Request body exceeds 1 MiB");
        }
        return body;
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            values.putIfAbsent(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return values;
    }
}
