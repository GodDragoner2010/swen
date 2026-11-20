package at.technikum.mrp.http;

public record HttpResponse(int status, Object body) {
    public static HttpResponse ok(Object body) {
        return new HttpResponse(200, body);
    }

    public static HttpResponse created(Object body) {
        return new HttpResponse(201, body);
    }

    public static HttpResponse noContent() {
        return new HttpResponse(204, null);
    }
}
