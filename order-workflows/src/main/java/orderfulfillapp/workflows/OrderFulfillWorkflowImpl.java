package orderfulfillapp.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import orderfulfillapp.activities.OrderFulfillActivities;
import orderfulfillapp.dto.FulfillmentStatus;
import orderfulfillapp.dto.OrderFulfillmentResult;
import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.exception.OrderRejectedException;
import orderfulfillapp.model.Order;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Orchestrates validate → optional approval → payment → inventory → delivery
 * with saga compensations on failure.
 */
public class OrderFulfillWorkflowImpl implements OrderFulfillWorkflow {

    private static final Logger logger = Workflow.getLogger(OrderFulfillWorkflowImpl.class);

    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setMaximumAttempts(5)
                    .setDoNotRetry(
                            CreditCardExpiredException.class.getName(),
                            InsufficientInventoryException.class.getName(),
                            IllegalArgumentException.class.getName())
                    .build())
            .build();

    private final OrderFulfillActivities activities =
            Workflow.newActivityStub(OrderFulfillActivities.class, activityOptions);

    private OrderFulfillmentResult status =
            new OrderFulfillmentResult(null, FulfillmentStatus.PENDING, 0);
    private boolean approvalDecisionReceived;
    private boolean approved;
    private String rejectionReason;

    @Override
    public OrderFulfillmentResult fulfillOrder(Order order) {
        order.ensureIdentity();
        status = new OrderFulfillmentResult(order.getOrderId(), FulfillmentStatus.PENDING, order.getTotalAmount());

        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        try {
            order.validate();

            boolean needsApproval = activities.requireApproval(order);
            status.setApprovalRequired(needsApproval);

            if (needsApproval) {
                status.setStatus(FulfillmentStatus.AWAITING_APPROVAL);
                logger.info("Order {} awaiting approval (total={})", order.getOrderId(), order.getTotalAmount());

                boolean received = Workflow.await(
                        Duration.ofSeconds(300),
                        () -> approvalDecisionReceived);

                if (!received) {
                    throw new OrderRejectedException("Approval timed out for order " + order.getOrderId());
                }
                if (!approved) {
                    throw new OrderRejectedException(
                            rejectionReason != null ? rejectionReason : "Order rejected by approver");
                }
                status.setStatus(FulfillmentStatus.APPROVED);
            }

            String paymentReference = activities.processPayment(order);
            status.setPaymentResult(paymentReference);
            status.setStatus(FulfillmentStatus.PAYMENT_PROCESSED);
            saga.addCompensation(() -> {
                String refund = activities.refundPayment(order, paymentReference);
                status.addCompensation(refund);
            });

            String reservationReference = activities.reserveInventory(order);
            status.setInventoryResult(reservationReference);
            status.setStatus(FulfillmentStatus.INVENTORY_RESERVED);
            saga.addCompensation(() -> {
                String release = activities.releaseInventory(order, reservationReference);
                status.addCompensation(release);
            });

            String deliveryResult = activities.deliverOrder(order);
            status.setDeliveryResult(deliveryResult);
            status.setStatus(FulfillmentStatus.DELIVERED);

            status.setStatus(FulfillmentStatus.COMPLETED);
            status.setUpdatedAtEpochMs(Workflow.currentTimeMillis());
            logger.info("Order {} fulfilled successfully", order.getOrderId());
            return status;

        } catch (OrderRejectedException e) {
            status.setFailureReason(e.getMessage());
            status.setStatus(FulfillmentStatus.REJECTED);
            logger.warn("Order {} rejected: {}", order.getOrderId(), e.getMessage());
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "OrderRejected");
        } catch (Exception e) {
            logger.error("Order {} failed, running compensations: {}", order.getOrderId(), e.getMessage());
            try {
                saga.compensate();
                status.setStatus(FulfillmentStatus.COMPENSATED);
            } catch (Exception compensationError) {
                logger.error("Compensation failed for order {}: {}",
                        order.getOrderId(), compensationError.getMessage());
                status.setStatus(FulfillmentStatus.FAILED);
            }
            status.setFailureReason(e.getMessage());
            if (status.getStatus() != FulfillmentStatus.COMPENSATED) {
                status.setStatus(FulfillmentStatus.FAILED);
            }
            throw ApplicationFailure.newFailureWithCause(
                    "Order fulfillment failed: " + e.getMessage(),
                    e.getClass().getSimpleName(),
                    e);
        }
    }

    @Override
    public void approveOrder() {
        this.approved = true;
        this.approvalDecisionReceived = true;
        this.rejectionReason = null;
    }

    @Override
    public void rejectOrder(String reason) {
        this.approved = false;
        this.approvalDecisionReceived = true;
        this.rejectionReason = reason;
    }

    @Override
    public OrderFulfillmentResult getStatus() {
        return status;
    }

}
