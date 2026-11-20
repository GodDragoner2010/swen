package at.technikum.mrp.service;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.MediaType;
import at.technikum.mrp.domain.User;
import at.technikum.mrp.dto.MediaDetailResponse;
import at.technikum.mrp.dto.MediaInput;
import at.technikum.mrp.dto.MediaSummaryResponse;
import at.technikum.mrp.persistence.MediaRepository;
import at.technikum.mrp.persistence.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MediaService {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_GENRES = 10;
    private static final int MAX_GENRE_LENGTH = 80;

    private final MediaRepository media;
    private final UserRepository users;
    private final MediaAssembler assembler;

    public MediaService(MediaRepository media, UserRepository users, MediaAssembler assembler) {
        this.media = media;
        this.users = users;
        this.assembler = assembler;
    }

    public MediaDetailResponse create(long requesterId, MediaInput input) {
        User creator = users.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        ValidatedMedia validated = validate(input);
        MediaEntry entry = new MediaEntry(
                0,
                validated.title(),
                validated.description(),
                validated.mediaType(),
                validated.releaseYear(),
                validated.genres(),
                validated.ageRestriction(),
                creator.id(),
                creator.username(),
                Instant.now()
        );
        return assembler.detail(media.create(entry));
    }

    public MediaDetailResponse get(long mediaId) {
        return assembler.detail(requireMedia(mediaId));
    }

    public List<MediaSummaryResponse> list() {
        return media.findAll().stream().map(assembler::summary).toList();
    }

    private MediaEntry requireMedia(long mediaId) {
        return media.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media entry not found"));
    }

    private ValidatedMedia validate(MediaInput input) {
        if (input == null) {
            throw new ValidationException("Request body is required");
        }
        if (input.title() == null || input.title().isBlank()) {
            throw new ValidationException("Title is required");
        }
        String title = input.title().trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }
        String description = input.description() == null ? "" : input.description().trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        MediaType mediaType = MediaType.fromString(input.mediaType())
                .orElseThrow(() -> new ValidationException("Media type must be movie, series or game"));
        if (input.releaseYear() == null || input.releaseYear() < 1888 || input.releaseYear() > 2200) {
            throw new ValidationException("Release year must be between 1888 and 2200");
        }
        if (input.ageRestriction() == null || input.ageRestriction() < 0 || input.ageRestriction() > 21) {
            throw new ValidationException("Age restriction must be between 0 and 21");
        }
        List<String> genres = normalizeGenres(input.genres());
        return new ValidatedMedia(title, description, mediaType, input.releaseYear(), genres,
                input.ageRestriction());
    }

    private List<String> normalizeGenres(List<String> rawGenres) {
        if (rawGenres == null || rawGenres.isEmpty()) {
            throw new ValidationException("At least one genre is required");
        }
        Set<String> seen = new HashSet<>();
        List<String> genres = new ArrayList<>();
        for (String raw : rawGenres) {
            if (raw == null || raw.isBlank()) {
                throw new ValidationException("Genres must not be blank");
            }
            String genre = raw.trim();
            if (genre.length() > MAX_GENRE_LENGTH) {
                throw new ValidationException("Genre must not exceed " + MAX_GENRE_LENGTH + " characters");
            }
            if (seen.add(genre.toLowerCase(Locale.ROOT))) {
                genres.add(genre);
            }
        }
        if (genres.size() > MAX_GENRES) {
            throw new ValidationException("A media entry may have at most " + MAX_GENRES + " genres");
        }
        return List.copyOf(genres);
    }

    private record ValidatedMedia(
            String title,
            String description,
            MediaType mediaType,
            int releaseYear,
            List<String> genres,
            int ageRestriction
    ) {
    }
}
