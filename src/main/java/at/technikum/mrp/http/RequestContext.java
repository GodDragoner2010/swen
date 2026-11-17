package at.technikum.mrp.http;

import at.technikum.mrp.service.ValidationException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class RequestContext {
    private static final int MAX_BODY_BYTES = 1_048_576;

    private final HttpExchange exchange;
    private final Gson gson;
    private byte[] body;

    public RequestContext(HttpExchange exchange, Gson gson) {
        this.exchange = exchange;
        this.gson = gson;
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
}
