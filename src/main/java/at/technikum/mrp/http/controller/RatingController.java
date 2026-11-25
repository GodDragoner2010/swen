package at.technikum.mrp.http.controller;

import at.technikum.mrp.dto.RatingInput;
import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;
import at.technikum.mrp.service.RatingService;

public final class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public HttpResponse update(RequestContext context) {
        RatingInput input = context.body(RatingInput.class);
        return HttpResponse.ok(ratingService.update(
                context.requireUserId(),
                context.pathLong("ratingId"),
                input
        ));
    }

    public HttpResponse delete(RequestContext context) {
        ratingService.delete(context.requireUserId(), context.pathLong("ratingId"));
        return HttpResponse.noContent();
    }

    public HttpResponse confirm(RequestContext context) {
        return HttpResponse.ok(ratingService.confirm(
                context.requireUserId(),
                context.pathLong("ratingId")
        ));
    }
}
