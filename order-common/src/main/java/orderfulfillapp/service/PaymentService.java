package orderfulfillapp.service;

import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.model.Order;

/**
 * Pluggable payment gateway abstraction.
 */
public interface PaymentService {

    /**
     * Charge the order payment method.
     *
     * @return opaque payment transaction reference
     */
    String processPayment(Order order) throws CreditCardExpiredException;

    /**
     * Refund a previously processed payment (saga compensation).
     */
    String refundPayment(Order order, String paymentReference);
}
