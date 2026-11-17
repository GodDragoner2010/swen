package at.technikum.mrp.http;

public class HttpException extends RuntimeException {
    private final int status;

    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static HttpException unsupportedMediaType(String message) {
        return new HttpException(415, message);
    }

    public static HttpException payloadTooLarge(String message) {
        return new HttpException(413, message);
    }
}
