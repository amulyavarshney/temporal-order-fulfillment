package orderfulfillapp.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {

    public enum PaymentStatus {
        CHARGED,
        REFUNDED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public PaymentTransactionEntity() {
    }

    public PaymentTransactionEntity(String paymentReference, String orderId, double amount, PaymentStatus status) {
        this.paymentReference = paymentReference;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
