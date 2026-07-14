package orderfulfillapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * A single line item in an order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItem {

    @JsonProperty("itemName")
    private String itemName;

    @JsonProperty("itemPrice")
    private double itemPrice;

    @JsonProperty("quantity")
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(String itemName, double itemPrice, int quantity) {
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @JsonIgnore
    public double getLineTotal() {
        return itemPrice * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItem that)) {
            return false;
        }
        return Double.compare(that.itemPrice, itemPrice) == 0
                && quantity == that.quantity
                && Objects.equals(itemName, that.itemName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName, itemPrice, quantity);
    }

    @Override
    public String toString() {
        return "OrderItem{itemName='" + itemName + "', itemPrice=" + itemPrice
                + ", quantity=" + quantity + '}';
    }
}
