package at.technikum.mrp.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String expiresAt,
        long userId,
        String username
) {
}
