package at.technikum.mrp.http.controller;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.dto.RegisterRequest;
import at.technikum.mrp.dto.UserBasicResponse;
import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;
import at.technikum.mrp.service.AuthService;
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
}
