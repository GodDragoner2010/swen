package at.technikum.mrp.http.controller;

import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;
import at.technikum.mrp.service.RatingService;

public final class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public HttpResponse confirm(RequestContext context) {
        return HttpResponse.ok(ratingService.confirm(
                context.requireUserId(),
                context.pathLong("ratingId")
        ));
    }
}
