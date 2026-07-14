package orderfulfillapp.exception;

/**
 * Thrown when inventory cannot fulfill a reservation request.
 */
public class InsufficientInventoryException extends Exception {

    public InsufficientInventoryException(String message) {
        super(message);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
