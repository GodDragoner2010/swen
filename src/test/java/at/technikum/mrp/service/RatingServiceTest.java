package at.technikum.mrp.service;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.MediaType;
import at.technikum.mrp.domain.Rating;
import at.technikum.mrp.domain.User;
import at.technikum.mrp.dto.RatingInput;
import at.technikum.mrp.dto.RatingResponse;
import at.technikum.mrp.testutil.InMemoryMediaRepository;
import at.technikum.mrp.testutil.InMemoryRatingRepository;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import at.technikum.mrp.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingServiceTest {
    private InMemoryUserRepository users;
    private InMemoryMediaRepository media;
    private InMemoryRatingRepository ratings;
    private RatingService service;
    private User alice;
    private User bob;
    private MediaEntry arrival;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        media = new InMemoryMediaRepository();
        ratings = new InMemoryRatingRepository();
        service = new RatingService(media, ratings, new MediaAssembler());
        alice = users.seed("alice");
        bob = users.seed("bob");
        arrival = media.create(TestFixtures.media(0, "Arrival", MediaType.MOVIE, 2016, 12,
                alice.id(), "sci-fi"));
    }

    @Test
    void rateStoresTheRatingAndReturnsIt() {
        RatingResponse response = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        assertEquals(arrival.id(), response.mediaId());
        assertEquals("Arrival", response.mediaTitle());
        assertEquals(bob.id(), response.userId());
        assertEquals(5, response.stars());
        assertEquals("Great", response.comment());
        assertFalse(response.commentConfirmed());
        assertEquals(0, response.likeCount());
        assertTrue(response.id() > 0);
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        assertTrue(ratings.findByUserAndMedia(bob.id(), arrival.id()).isPresent());
    }

    @Test
    void allowsDifferentUsersToRateTheSameMedia() {
        service.rate(bob.id(), arrival.id(), new RatingInput(5, null));

        RatingResponse aliceRating = service.rate(alice.id(), arrival.id(), new RatingInput(3, null));

        assertEquals(alice.id(), aliceRating.userId());
        assertEquals(3, aliceRating.stars());
        assertTrue(ratings.findByUserAndMedia(bob.id(), arrival.id()).isPresent());
        assertTrue(ratings.findByUserAndMedia(alice.id(), arrival.id()).isPresent());
    }

    @Test
    void ratingWithoutCommentIsImmediatelyConfirmed() {
        RatingResponse response = service.rate(bob.id(), arrival.id(), new RatingInput(4, null));

        assertNull(response.comment());
        assertTrue(response.commentConfirmed());
    }

    @Test
    void blankCommentIsStoredAsNull() {
        RatingResponse response = service.rate(bob.id(), arrival.id(), new RatingInput(4, "   "));

        assertNull(response.comment());
        assertTrue(response.commentConfirmed());
    }

    @Test
    void rejectsStarsOutsideOneToFive() {
        assertThrows(ValidationException.class,
                () -> service.rate(bob.id(), arrival.id(), new RatingInput(0, null)));
        assertThrows(ValidationException.class,
                () -> service.rate(bob.id(), arrival.id(), new RatingInput(6, null)));
        assertThrows(ValidationException.class,
                () -> service.rate(bob.id(), arrival.id(), new RatingInput(null, null)));
        assertTrue(ratings.findByUserAndMedia(bob.id(), arrival.id()).isEmpty());
    }

    @Test
    void rejectsSecondRatingForSameUserAndMedia() {
        service.rate(bob.id(), arrival.id(), new RatingInput(5, null));

        assertThrows(ConflictException.class,
                () -> service.rate(bob.id(), arrival.id(), new RatingInput(3, null)));
        assertEquals(5, ratings.findByUserAndMedia(bob.id(), arrival.id())
                .orElseThrow().stars());
    }

    @Test
    void rejectsRatingForUnknownMedia() {
        assertThrows(NotFoundException.class,
                () -> service.rate(bob.id(), 999, new RatingInput(5, null)));
    }

    @Test
    void rejectsOverlyLongComment() {
        assertThrows(ValidationException.class,
                () -> service.rate(bob.id(), arrival.id(),
                        new RatingInput(5, "x".repeat(1001))));
        assertTrue(ratings.findByUserAndMedia(bob.id(), arrival.id()).isEmpty());
    }

    @Test
    void commentIsHiddenFromOtherViewersUntilConfirmed() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        Rating stored = ratings.findById(created.id()).orElseThrow();

        assertTrue(stored.commentVisibleTo(bob.id()));
        assertFalse(stored.commentVisibleTo(alice.id()));
    }

    @Test
    void confirmingMakesCommentVisible() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        RatingResponse confirmed = service.confirm(bob.id(), created.id());

        assertTrue(confirmed.commentConfirmed());
        assertEquals("Great", confirmed.comment());
        assertTrue(ratings.findById(created.id()).orElseThrow().commentVisibleTo(alice.id()));
    }

    @Test
    void confirmIsIdempotentOnceCommentIsConfirmed() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));
        service.confirm(bob.id(), created.id());

        RatingResponse again = service.confirm(bob.id(), created.id());

        assertTrue(again.commentConfirmed());
        assertEquals("Great", again.comment());
    }

    @Test
    void onlyTheOwnerMayConfirmAComment() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        assertThrows(ForbiddenException.class, () -> service.confirm(alice.id(), created.id()));
    }

    @Test
    void confirmingUnknownRatingIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.confirm(bob.id(), 999));
    }

    @Test
    void ratingWithoutCommentCannotBeConfirmed() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(4, null));

        assertThrows(ValidationException.class, () -> service.confirm(bob.id(), created.id()));
    }

    @Test
    void onlyTheOwnerMayUpdateARating() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, null));

        assertThrows(ForbiddenException.class,
                () -> service.update(alice.id(), created.id(), new RatingInput(4, null)));
    }

    @Test
    void updatingARatingChangesStarsAndComment() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        RatingResponse updated = service.update(bob.id(), created.id(), new RatingInput(3, "Changed"));

        assertEquals(3, updated.stars());
        assertEquals("Changed", updated.comment());
        assertEquals(3, ratings.findById(created.id()).orElseThrow().stars());
        assertEquals("Changed", ratings.findById(created.id()).orElseThrow().comment());
    }

    @Test
    void changingCommentResetsConfirmation() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));
        service.confirm(bob.id(), created.id());

        RatingResponse updated = service.update(bob.id(), created.id(), new RatingInput(5, "Changed"));

        assertFalse(updated.commentConfirmed());
        assertEquals("Changed", updated.comment());
    }

    @Test
    void removingCommentMarksRatingAsConfirmed() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, "Great"));

        RatingResponse updated = service.update(bob.id(), created.id(), new RatingInput(4, null));

        assertTrue(updated.commentConfirmed());
        assertNull(updated.comment());
    }

    @Test
    void updatingUnknownRatingIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.update(bob.id(), 999, new RatingInput(4, null)));
    }

    @Test
    void onlyTheOwnerMayDeleteARating() {
        RatingResponse created = service.rate(bob.id(), arrival.id(), new RatingInput(5, null));

        assertThrows(ForbiddenException.class, () -> service.delete(alice.id(), created.id()));

        service.delete(bob.id(), created.id());
        assertTrue(ratings.findById(created.id()).isEmpty());
    }

    @Test
    void deletingUnknownRatingIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete(bob.id(), 999));
    }
}
