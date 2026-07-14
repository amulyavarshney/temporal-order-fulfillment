package orderfulfillapp.exception;

/**
 * Thrown when an order is rejected during the approval step.
 */
public class OrderRejectedException extends Exception {

    public OrderRejectedException(String message) {
        super(message);
    }
}
