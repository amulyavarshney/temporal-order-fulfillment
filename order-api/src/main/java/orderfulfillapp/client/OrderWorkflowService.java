package orderfulfillapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import orderfulfillapp.config.AppConfig;
import orderfulfillapp.dto.CreateOrderResponse;
import orderfulfillapp.dto.FulfillmentStatus;
import orderfulfillapp.dto.OrderFulfillmentResult;
import orderfulfillapp.model.Order;
import orderfulfillapp.persistence.OrderEntity;
import orderfulfillapp.persistence.OrderRepository;
import orderfulfillapp.workflows.OrderFulfillWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowService.class);

    private final WorkflowClient workflowClient;
    private final AppConfig appConfig;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderWorkflowService(
            WorkflowClient workflowClient,
            AppConfig appConfig,
            OrderRepository orderRepository,
            ObjectMapper objectMapper) {
        this.workflowClient = workflowClient;
        this.appConfig = appConfig;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateOrderResponse startOrder(Order order) throws Exception {
        order.validate();
        String workflowId = "order-fulfill-" + order.getOrderId();

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(appConfig.getTaskQueue())
                .setWorkflowId(workflowId)
                .build();

        OrderFulfillWorkflow workflow = workflowClient.newWorkflowStub(OrderFulfillWorkflow.class, options);
        WorkflowClient.start(workflow::fulfillOrder, order);

        OrderEntity entity = new OrderEntity(
                order.getOrderId(),
                workflowId,
                order.getTotalAmount(),
                FulfillmentStatus.PENDING,
                objectMapper.writeValueAsString(order));
        orderRepository.save(entity);

        logger.info("Started workflow {} for order {}", workflowId, order.getOrderId());
        return new CreateOrderResponse(order.getOrderId(), workflowId, "Order workflow started");
    }

    public OrderFulfillmentResult getStatus(String orderId) {
        String workflowId = resolveWorkflowId(orderId);
        OrderFulfillWorkflow workflow = workflowClient.newWorkflowStub(OrderFulfillWorkflow.class, workflowId);
        OrderFulfillmentResult result = workflow.getStatus();
        orderRepository.findByOrderId(orderId).ifPresent(entity -> {
            entity.setStatus(result.getStatus());
            orderRepository.save(entity);
        });
        return result;
    }

    public void approve(String orderId) {
        String workflowId = resolveWorkflowId(orderId);
        OrderFulfillWorkflow workflow = workflowClient.newWorkflowStub(OrderFulfillWorkflow.class, workflowId);
        workflow.approveOrder();
        logger.info("Sent approve signal for order {}", orderId);
    }

    public void reject(String orderId, String reason) {
        String workflowId = resolveWorkflowId(orderId);
        OrderFulfillWorkflow workflow = workflowClient.newWorkflowStub(OrderFulfillWorkflow.class, workflowId);
        workflow.rejectOrder(reason != null ? reason : "Rejected via API");
        logger.info("Sent reject signal for order {}", orderId);
    }

    private String resolveWorkflowId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(OrderEntity::getWorkflowId)
                .orElse("order-fulfill-" + orderId);
    }
}
