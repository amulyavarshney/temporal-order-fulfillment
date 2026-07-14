package orderfulfillapp.service;

import orderfulfillapp.model.Order;

/**
 * Pluggable delivery backend abstraction.
 */
public interface DeliveryService {

    String deliverOrder(Order order);
}
