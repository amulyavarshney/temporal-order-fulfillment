package orderfulfillapp.exception;

/**
 * Non-retryable exception thrown when a credit card is expired.
 */
public class CreditCardExpiredException extends Exception {

    public CreditCardExpiredException(String message) {
        super(message);
    }

    public CreditCardExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
