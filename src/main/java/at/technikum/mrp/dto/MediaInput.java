package at.technikum.mrp.dto;

import java.util.List;

public record MediaInput(
        String title,
        String description,
        String mediaType,
        Integer releaseYear,
        List<String> genres,
        Integer ageRestriction
) {
}
