package orderfulfillapp.service.impl;

import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.Order;
import orderfulfillapp.model.OrderItem;
import orderfulfillapp.model.StockItem;
import orderfulfillapp.persistence.StockItemEntity;
import orderfulfillapp.persistence.StockItemRepository;
import orderfulfillapp.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PersistentInventoryService implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(PersistentInventoryService.class);

    private final StockItemRepository stockItemRepository;

    public PersistentInventoryService(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @Override
    @Transactional
    public String reserveInventory(Order order) throws InsufficientInventoryException {
        for (OrderItem item : order.getItems()) {
            StockItemEntity stock = stockItemRepository.findByItemName(item.getItemName())
                    .orElseThrow(() -> new InsufficientInventoryException(
                            "Couldn't find item in stock database: " + item.getItemName()));

            if (stock.getStock() < item.getQuantity()) {
                throw new InsufficientInventoryException(
                        "Insufficient stock for item: " + item.getItemName()
                                + " (requested=" + item.getQuantity() + ", available=" + stock.getStock() + ")");
            }

            stock.setStock(stock.getStock() - item.getQuantity());
            stockItemRepository.save(stock);
            logger.info("Reserved {} of {} (remaining={})",
                    item.getQuantity(), item.getItemName(), stock.getStock());
        }

        return "inv-" + UUID.randomUUID();
    }

    @Override
    @Transactional
    public String releaseInventory(Order order, String reservationReference) {
        for (OrderItem item : order.getItems()) {
            stockItemRepository.findByItemName(item.getItemName()).ifPresent(stock -> {
                stock.setStock(stock.getStock() + item.getQuantity());
                stockItemRepository.save(stock);
                logger.info("Released {} of {} (stock={})",
                        item.getQuantity(), item.getItemName(), stock.getStock());
            });
        }
        return "Released inventory reservation " + reservationReference + " for order " + order.getOrderId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> listStock() {
        return stockItemRepository.findAll().stream()
                .map(entity -> new StockItem(entity.getItemName(), entity.getItemPrice(), entity.getStock()))
                .collect(Collectors.toList());
    }
}
