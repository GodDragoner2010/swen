package at.technikum.mrp.http;

import at.technikum.mrp.dto.ErrorResponse;
import at.technikum.mrp.persistence.DataAccessException;
import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.ConflictException;
import at.technikum.mrp.service.ForbiddenException;
import at.technikum.mrp.service.NotFoundException;
import at.technikum.mrp.service.UnauthorizedException;
import at.technikum.mrp.service.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ApiServer {
    private final int port;
    private final Router router;
    private final AuthFilter authFilter;
    private final Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    private HttpServer server;

    public ApiServer(int port, Router router, AuthService authService) {
        this.port = port;
        this.router = router;
        this.authFilter = new AuthFilter(authService);
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Router.Match match = router.match(method, path);
            if (match.route() == null) {
                if (match.pathMatched()) {
                    throw new HttpException(405, "Method not allowed");
                }
                throw new NotFoundException("Route not found");
            }
            Long userId = authFilter.authenticate(
                    exchange.getRequestHeaders().getFirst("Authorization"), match.route().access());
            RequestContext context = new RequestContext(exchange, match.pathParams(), userId, gson);
            HttpResponse response = match.route().handler().handle(context);
            writeJson(exchange, response.status(), gson.toJson(response.body()));
        } catch (ValidationException e) {
            writeJson(exchange, 400, gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (UnauthorizedException e) {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
            writeJson(exchange, 401, gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (ConflictException e) {
            writeJson(exchange, 409, gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (ForbiddenException e) {
            writeJson(exchange, 403, gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (NotFoundException e) {
            writeJson(exchange, 404, gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (HttpException e) {
            writeJson(exchange, e.status(), gson.toJson(new ErrorResponse(e.getMessage())));
        } catch (DataAccessException e) {
            e.printStackTrace(System.err);
            writeJson(exchange, 500, gson.toJson(new ErrorResponse("Internal server error")));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            writeJson(exchange, 500, gson.toJson(new ErrorResponse("Internal server error")));
        } finally {
            exchange.close();
        }
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        if (status == 204 || json == null) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int port() {
        return server == null ? port : server.getAddress().getPort();
    }
}
