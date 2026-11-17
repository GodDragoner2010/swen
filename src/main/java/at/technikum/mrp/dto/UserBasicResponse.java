package at.technikum.mrp.dto;

public record UserBasicResponse(
        long id,
        String username,
        String email,
        String favoriteGenre,
        String createdAt
) {
}
