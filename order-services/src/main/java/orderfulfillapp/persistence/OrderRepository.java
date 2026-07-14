package orderfulfillapp.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderId(String orderId);

    Optional<OrderEntity> findByWorkflowId(String workflowId);
}
