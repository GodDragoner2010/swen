package at.technikum.mrp.service;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.dto.MediaDetailResponse;
import at.technikum.mrp.dto.MediaInput;
import at.technikum.mrp.dto.MediaSummaryResponse;
import at.technikum.mrp.testutil.InMemoryMediaRepository;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaServiceTest {
    private InMemoryUserRepository users;
    private InMemoryMediaRepository mediaRepository;
    private MediaService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        mediaRepository = new InMemoryMediaRepository();
        service = new MediaService(mediaRepository, users, new MediaAssembler());
        alice = users.seed("alice");
        bob = users.seed("bob");
    }

    @Test
    void createsMediaWithNormalizedFieldsAndCreator() {
        MediaDetailResponse created = service.create(alice.id(), new MediaInput(
                "  Inception  ",
                "  A dream heist  ",
                "MOVIE",
                2010,
                List.of("sci-fi", " Sci-Fi ", "thriller"),
                12
        ));

        assertEquals("Inception", created.title());
        assertEquals("A dream heist", created.description());
        assertEquals("movie", created.mediaType());
        assertEquals(List.of("sci-fi", "thriller"), created.genres());
        assertEquals(alice.id(), created.creatorId());
        assertEquals("alice", created.creatorUsername());
        assertEquals(0.0, created.averageScore());
        assertEquals(0, created.ratingCount());
        assertEquals(List.of(), created.ratings());
    }

    @Test
    void rejectsUnknownMediaType() {
        assertThrows(ValidationException.class, () -> service.create(alice.id(), new MediaInput(
                "Inception", "desc", "book", 2010, List.of("sci-fi"), 12)));
    }

    @Test
    void rejectsReleaseYearOutsideSupportedRange() {
        assertThrows(ValidationException.class, () -> service.create(alice.id(), new MediaInput(
                "Inception", "desc", "movie", 1800, List.of("sci-fi"), 12)));
    }

    @Test
    void rejectsMediaCreationForUnknownUser() {
        assertThrows(NotFoundException.class, () -> service.create(999, new MediaInput(
                "Inception", "desc", "movie", 2010, List.of("sci-fi"), 12)));
    }

    @Test
    void requiresAtLeastOneGenre() {
        assertThrows(ValidationException.class, () -> service.create(alice.id(), new MediaInput(
                "Inception", "desc", "movie", 2010, List.of(), 12)));
    }

    @Test
    void rejectsBlankGenres() {
        assertThrows(ValidationException.class, () -> service.create(alice.id(), new MediaInput(
                "Inception", "desc", "movie", 2010, List.of("  "), 12)));
    }

    @Test
    void rejectsMoreThanTenGenres() {
        List<String> genres = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            genres.add("genre" + i);
        }

        assertThrows(ValidationException.class, () -> service.create(alice.id(), new MediaInput(
                "Inception", "desc", "movie", 2010, genres, 12)));
    }

    @Test
    void getReturnsDetailForExistingMedia() {
        MediaDetailResponse created = create("Inception", "movie", 2010, 12, List.of("sci-fi"));

        MediaDetailResponse detail = service.get(created.id());

        assertEquals(created.id(), detail.id());
        assertEquals("Inception", detail.title());
        assertEquals("movie", detail.mediaType());
        assertEquals(List.of("sci-fi"), detail.genres());
        assertEquals(alice.id(), detail.creatorId());
        assertEquals(List.of(), detail.ratings());
    }

    @Test
    void getRejectsUnknownMediaId() {
        assertThrows(NotFoundException.class, () -> service.get(999));
    }

    @Test
    void onlyTheCreatorMayUpdateMedia() {
        MediaDetailResponse created = create("Inception", "movie", 2010, 12, List.of("sci-fi"));

        assertThrows(ForbiddenException.class, () -> service.update(bob.id(), created.id(),
                new MediaInput("Changed", "desc", "movie", 2010, List.of("sci-fi"), 12)));
    }

    @Test
    void updatesMediaFieldsForTheCreator() {
        MediaDetailResponse created = create("Inception", "movie", 2010, 12, List.of("sci-fi"));

        MediaDetailResponse updated = service.update(alice.id(), created.id(),
                new MediaInput("Inception Updated", "new", "movie", 2011, List.of("action"), 16));

        assertEquals("Inception Updated", updated.title());
        assertEquals("new", updated.description());
        assertEquals(2011, updated.releaseYear());
        assertEquals(List.of("action"), updated.genres());
        assertEquals(alice.id(), updated.creatorId());
        assertEquals("alice", updated.creatorUsername());
    }

    @Test
    void updateRejectsUnknownMediaId() {
        assertThrows(NotFoundException.class, () -> service.update(alice.id(), 999,
                new MediaInput("Changed", "desc", "movie", 2010, List.of("sci-fi"), 12)));
    }

    @Test
    void onlyTheCreatorMayDeleteMedia() {
        MediaDetailResponse created = create("Inception", "movie", 2010, 12, List.of("sci-fi"));

        assertThrows(ForbiddenException.class, () -> service.delete(bob.id(), created.id()));

        service.delete(alice.id(), created.id());
        assertTrue(mediaRepository.findAll().isEmpty());
    }

    @Test
    void deleteRejectsUnknownMediaId() {
        assertThrows(NotFoundException.class, () -> service.delete(alice.id(), 999));
    }

    @Test
    void listReturnsSummariesOfAllMedia() {
        create("Inception", "movie", 2010, 12, List.of("sci-fi"));
        create("Interstellar", "movie", 2014, 12, List.of("sci-fi"));

        List<MediaSummaryResponse> summaries = service.list();

        assertEquals(List.of("Inception", "Interstellar"),
                summaries.stream().map(MediaSummaryResponse::title).toList());
        assertTrue(summaries.stream().allMatch(summary -> summary.averageScore() == 0.0));
        assertFalse(mediaRepository.snapshot().isEmpty());
    }

    private MediaDetailResponse create(String title,
                                        String type,
                                        int year,
                                        int age,
                                        List<String> genres) {
        return service.create(alice.id(), new MediaInput(
                title, "Description", type, year, genres, age));
    }
}
