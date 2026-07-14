package orderfulfillapp.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.Order;

/**
 * Activity contract for order fulfillment side effects.
 */
@ActivityInterface
public interface OrderFulfillActivities {

    @ActivityMethod
    boolean requireApproval(Order order);

    @ActivityMethod
    String processPayment(Order order) throws CreditCardExpiredException;

    @ActivityMethod
    String refundPayment(Order order, String paymentReference);

    @ActivityMethod
    String reserveInventory(Order order) throws InsufficientInventoryException;

    @ActivityMethod
    String releaseInventory(Order order, String reservationReference);

    @ActivityMethod
    String deliverOrder(Order order);
}
