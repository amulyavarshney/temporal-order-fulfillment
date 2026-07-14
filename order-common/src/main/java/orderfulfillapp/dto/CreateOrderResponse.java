package orderfulfillapp.dto;

/**
 * Response returned when an order workflow is started.
 */
public class CreateOrderResponse {

    private String orderId;
    private String workflowId;
    private String message;

    public CreateOrderResponse() {
    }

    public CreateOrderResponse(String orderId, String workflowId, String message) {
        this.orderId = orderId;
        this.workflowId = workflowId;
        this.message = message;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
