package ecommerce.courier.pathao;

/** A Pathao call that failed. message is already human-readable (validation errors flattened). */
public class PathaoApiException extends RuntimeException {
    private final int status;

    public PathaoApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
