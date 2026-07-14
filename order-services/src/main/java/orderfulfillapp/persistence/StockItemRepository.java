package orderfulfillapp.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItemEntity, Long> {
    Optional<StockItemEntity> findByItemName(String itemName);
}
