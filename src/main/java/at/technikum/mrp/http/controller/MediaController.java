package at.technikum.mrp.http.controller;

import at.technikum.mrp.dto.MediaInput;
import at.technikum.mrp.http.HttpResponse;
import at.technikum.mrp.http.RequestContext;
import at.technikum.mrp.service.MediaService;

public final class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    public HttpResponse list(RequestContext context) {
        return HttpResponse.ok(mediaService.list());
    }

    public HttpResponse create(RequestContext context) {
        MediaInput input = context.body(MediaInput.class);
        return HttpResponse.created(mediaService.create(context.requireUserId(), input));
    }

    public HttpResponse get(RequestContext context) {
        return HttpResponse.ok(mediaService.get(context.pathLong("mediaId")));
    }

    public HttpResponse update(RequestContext context) {
        MediaInput input = context.body(MediaInput.class);
        return HttpResponse.ok(mediaService.update(
                context.requireUserId(),
                context.pathLong("mediaId"),
                input
        ));
    }

    public HttpResponse delete(RequestContext context) {
        mediaService.delete(context.requireUserId(), context.pathLong("mediaId"));
        return HttpResponse.noContent();
    }
}
