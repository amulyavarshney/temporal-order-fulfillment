package orderfulfillapp.dto;

/**
 * Lifecycle status of an order fulfillment workflow.
 */
public enum FulfillmentStatus {
    PENDING,
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    PAYMENT_PROCESSED,
    INVENTORY_RESERVED,
    DELIVERED,
    COMPLETED,
    FAILED,
    COMPENSATED
}
