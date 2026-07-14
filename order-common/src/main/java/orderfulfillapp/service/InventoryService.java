package orderfulfillapp.service;

import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.Order;
import orderfulfillapp.model.StockItem;

import java.util.List;

/**
 * Pluggable inventory backend abstraction.
 */
public interface InventoryService {

    String reserveInventory(Order order) throws InsufficientInventoryException;

    String releaseInventory(Order order, String reservationReference);

    List<StockItem> listStock();
}
