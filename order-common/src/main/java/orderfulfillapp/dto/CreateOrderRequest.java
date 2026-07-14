package orderfulfillapp.dto;

import orderfulfillapp.model.Order;

/**
 * Request payload for creating an order via the REST API.
 */
public class CreateOrderRequest {

    private Order order;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
