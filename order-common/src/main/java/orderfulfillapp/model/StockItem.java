package orderfulfillapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inventory stock record.
 */
public class StockItem {

    @JsonProperty("itemName")
    private String itemName;

    @JsonProperty("itemPrice")
    private double itemPrice;

    @JsonProperty("stock")
    private int stock;

    public StockItem() {
    }

    public StockItem(String itemName, double itemPrice, int stock) {
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.stock = stock;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "StockItem{itemName='" + itemName + "', itemPrice=" + itemPrice + ", stock=" + stock + '}';
    }
}
