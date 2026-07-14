package orderfulfillapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured result returned by the order fulfillment workflow and exposed via queries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderFulfillmentResult {

    private String orderId;
    private FulfillmentStatus status;
    private double totalAmount;
    private boolean approvalRequired;
    private String paymentResult;
    private String inventoryResult;
    private String deliveryResult;
    private String failureReason;
    private List<String> compensations = new ArrayList<>();
    private long updatedAtEpochMs;

    public OrderFulfillmentResult() {
    }

    public OrderFulfillmentResult(String orderId, FulfillmentStatus status, double totalAmount) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public void setStatus(FulfillmentStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public String getPaymentResult() {
        return paymentResult;
    }

    public void setPaymentResult(String paymentResult) {
        this.paymentResult = paymentResult;
    }

    public String getInventoryResult() {
        return inventoryResult;
    }

    public void setInventoryResult(String inventoryResult) {
        this.inventoryResult = inventoryResult;
    }

    public String getDeliveryResult() {
        return deliveryResult;
    }

    public void setDeliveryResult(String deliveryResult) {
        this.deliveryResult = deliveryResult;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public List<String> getCompensations() {
        return compensations;
    }

    public void setCompensations(List<String> compensations) {
        this.compensations = compensations != null ? compensations : new ArrayList<>();
    }

    public void addCompensation(String compensation) {
        this.compensations.add(compensation);
    }

    public long getUpdatedAtEpochMs() {
        return updatedAtEpochMs;
    }

    public void setUpdatedAtEpochMs(long updatedAtEpochMs) {
        this.updatedAtEpochMs = updatedAtEpochMs;
    }

    @JsonIgnore
    public boolean isTerminalSuccess() {
        return status == FulfillmentStatus.COMPLETED;
    }

    @JsonIgnore
    public boolean isTerminalFailure() {
        return status == FulfillmentStatus.FAILED
                || status == FulfillmentStatus.REJECTED
                || status == FulfillmentStatus.COMPENSATED;
    }
}
