package at.technikum.mrp.http;

import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.UnauthorizedException;

/**
 * Resolves the authenticated user from an Authorization header according to the
 * access level declared by the matched route.
 */
public final class AuthFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    public Long authenticate(String authorizationHeader, Router.Access access) {
        String token = bearerToken(authorizationHeader);
        if (access == Router.Access.PROTECTED) {
            if (token == null) {
                throw new UnauthorizedException("Authorization: Bearer <token> is required");
            }
            return authenticatedUser(token);
        }
        if (access == Router.Access.OPTIONAL_AUTH && token != null) {
            return authenticatedUser(token);
        }
        return null;
    }

    private Long authenticatedUser(String token) {
        return authService.authenticate(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));
    }

    private static String bearerToken(String header) {
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
