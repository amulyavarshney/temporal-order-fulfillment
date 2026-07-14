package orderfulfillapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order containing line items and payment information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("items")
    private List<OrderItem> items = new ArrayList<>();

    @JsonProperty("payment")
    private Payment payment;

    public Order() {
    }

    public Order(List<OrderItem> items, Payment payment) {
        this.orderId = UUID.randomUUID().toString();
        this.items = items != null ? items : new ArrayList<>();
        this.payment = payment;
    }

    public Order(String orderId, List<OrderItem> items, Payment payment) {
        this.orderId = orderId;
        this.items = items != null ? items : new ArrayList<>();
        this.payment = payment;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    @JsonIgnore
    public double getTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream().mapToDouble(OrderItem::getLineTotal).sum();
    }

    /**
     * Ensures the order has an ID and basic structural integrity.
     */
    public void ensureIdentity() {
        if (orderId == null || orderId.isBlank()) {
            orderId = UUID.randomUUID().toString();
        }
    }

    public void validate() {
        ensureIdentity();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        for (OrderItem item : items) {
            if (item.getItemName() == null || item.getItemName().isBlank()) {
                throw new IllegalArgumentException("Order item name is required");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Order item quantity must be positive: " + item.getItemName());
            }
            if (item.getItemPrice() < 0) {
                throw new IllegalArgumentException("Order item price cannot be negative: " + item.getItemName());
            }
        }
        if (payment == null || payment.getCreditCard() == null) {
            throw new IllegalArgumentException("Order payment credit card is required");
        }
        CreditCard card = payment.getCreditCard();
        if (card.getNumber() == null || card.getNumber().isBlank()) {
            throw new IllegalArgumentException("Credit card number is required");
        }
        if (card.getExpiration() == null || card.getExpiration().isBlank()) {
            throw new IllegalArgumentException("Credit card expiration is required");
        }
    }

    @Override
    public String toString() {
        return "Order{orderId='" + orderId + "', items=" + items + ", payment=" + payment
                + ", totalAmount=" + getTotalAmount() + '}';
    }
}
