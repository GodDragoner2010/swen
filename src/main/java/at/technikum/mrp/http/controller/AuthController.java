package at.technikum.mrp.http.controller;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.domain.User;
import at.technikum.mrp.dto.AuthResponse;
import at.technikum.mrp.dto.LoginRequest;
import at.technikum.mrp.dto.RegisterRequest;
import at.technikum.mrp.dto.UserBasicResponse;
import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;
import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.LoginResult;
import at.technikum.mrp.util.Format;

public final class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public HttpResponse register(RequestContext context) {
        RegisterRequest request = context.body(RegisterRequest.class);
        User user = authService.register(request.username(), request.password());
        return HttpResponse.created(new UserBasicResponse(
                user.id(),
                user.username(),
                user.email(),
                user.favoriteGenre(),
                Format.iso(user.createdAt())
        ));
    }

    public HttpResponse login(RequestContext context) {
        LoginRequest request = context.body(LoginRequest.class);
        LoginResult result = authService.login(request.username(), request.password());
        AuthToken token = result.token();
        return HttpResponse.ok(new AuthResponse(
                token.token(),
                "Bearer",
                Format.iso(token.expiresAt()),
                result.user().id(),
                result.user().username()
        ));
    }
}
