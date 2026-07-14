package orderfulfillapp.activities;

import orderfulfillapp.Shared;
import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.Order;
import orderfulfillapp.service.DeliveryService;
import orderfulfillapp.service.InventoryService;
import orderfulfillapp.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activity implementation that delegates to pluggable domain services.
 */
public class OrderFulfillActivitiesImpl implements OrderFulfillActivities {

    private static final Logger logger = LoggerFactory.getLogger(OrderFulfillActivitiesImpl.class);

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;
    private final double approvalThreshold;

    public OrderFulfillActivitiesImpl(
            PaymentService paymentService,
            InventoryService inventoryService,
            DeliveryService deliveryService,
            double approvalThreshold) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
        this.approvalThreshold = approvalThreshold;
    }

    public OrderFulfillActivitiesImpl(
            PaymentService paymentService,
            InventoryService inventoryService,
            DeliveryService deliveryService) {
        this(paymentService, inventoryService, deliveryService, Shared.DEFAULT_APPROVAL_THRESHOLD);
    }

    @Override
    public boolean requireApproval(Order order) {
        double total = order.getTotalAmount();
        boolean required = total > approvalThreshold;
        logger.info("Approval check orderId={} total={} threshold={} required={}",
                order.getOrderId(), total, approvalThreshold, required);
        return required;
    }

    @Override
    public String processPayment(Order order) throws CreditCardExpiredException {
        logger.info("Processing payment for order {}", order.getOrderId());
        return paymentService.processPayment(order);
    }

    @Override
    public String refundPayment(Order order, String paymentReference) {
        logger.info("Refunding payment for order {} ref={}", order.getOrderId(), paymentReference);
        return paymentService.refundPayment(order, paymentReference);
    }

    @Override
    public String reserveInventory(Order order) throws InsufficientInventoryException {
        logger.info("Reserving inventory for order {}", order.getOrderId());
        return inventoryService.reserveInventory(order);
    }

    @Override
    public String releaseInventory(Order order, String reservationReference) {
        logger.info("Releasing inventory for order {} ref={}", order.getOrderId(), reservationReference);
        return inventoryService.releaseInventory(order, reservationReference);
    }

    @Override
    public String deliverOrder(Order order) {
        logger.info("Delivering order {}", order.getOrderId());
        return deliveryService.deliverOrder(order);
    }
}
