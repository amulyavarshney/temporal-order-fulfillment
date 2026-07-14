package orderfulfillapp.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import orderfulfillapp.dto.OrderFulfillmentResult;
import orderfulfillapp.model.Order;

/**
 * Order fulfillment workflow with approval signals and status queries.
 */
@WorkflowInterface
public interface OrderFulfillWorkflow {

    @WorkflowMethod
    OrderFulfillmentResult fulfillOrder(Order order);

    @SignalMethod
    void approveOrder();

    @SignalMethod
    void rejectOrder(String reason);

    @QueryMethod
    OrderFulfillmentResult getStatus();
}
