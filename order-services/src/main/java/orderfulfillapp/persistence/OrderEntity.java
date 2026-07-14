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
import orderfulfillapp.dto.FulfillmentStatus;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String workflowId;

    @Column(nullable = false)
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus status;

    @Column(nullable = false, length = 4000)
    private String orderJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public OrderEntity() {
    }

    public OrderEntity(String orderId, String workflowId, double totalAmount, FulfillmentStatus status, String orderJson) {
        this.orderId = orderId;
        this.workflowId = workflowId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderJson = orderJson;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public void setStatus(FulfillmentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getOrderJson() {
        return orderJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
