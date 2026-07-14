package orderfulfillapp.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {
    Optional<PaymentTransactionEntity> findByPaymentReference(String paymentReference);
}
