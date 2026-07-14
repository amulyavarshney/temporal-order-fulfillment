package orderfulfillapp.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import orderfulfillapp.Shared;
import orderfulfillapp.model.StockItem;
import orderfulfillapp.persistence.StockItemEntity;
import orderfulfillapp.persistence.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Seeds inventory from classpath JSON when the stock table is empty.
 */
@Component
@Order(1)
public class InventoryDataLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InventoryDataLoader.class);

    private final StockItemRepository stockItemRepository;
    private final ObjectMapper objectMapper;

    public InventoryDataLoader(StockItemRepository stockItemRepository, ObjectMapper objectMapper) {
        this.stockItemRepository = stockItemRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (stockItemRepository.count() > 0) {
            logger.info("Stock database already seeded ({} items)", stockItemRepository.count());
            return;
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Shared.STOCK_DATABASE_PATH)) {
            if (inputStream == null) {
                logger.warn("No stock seed file found at classpath:{}", Shared.STOCK_DATABASE_PATH);
                return;
            }

            List<StockItem> items = objectMapper.readValue(inputStream, new TypeReference<>() {});
            for (StockItem item : items) {
                stockItemRepository.save(new StockItemEntity(item.getItemName(), item.getItemPrice(), item.getStock()));
            }
            logger.info("Seeded {} stock items from {}", items.size(), Shared.STOCK_DATABASE_PATH);
        }
    }
}
