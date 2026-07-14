package orderfulfillapp.api;

import orderfulfillapp.client.OrderWorkflowService;
import orderfulfillapp.dto.CreateOrderRequest;
import orderfulfillapp.dto.CreateOrderResponse;
import orderfulfillapp.dto.OrderFulfillmentResult;
import orderfulfillapp.model.Order;
import orderfulfillapp.model.StockItem;
import orderfulfillapp.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderWorkflowService orderWorkflowService;
    private final InventoryService inventoryService;

    public OrderController(OrderWorkflowService orderWorkflowService, InventoryService inventoryService) {
        this.orderWorkflowService = orderWorkflowService;
        this.inventoryService = inventoryService;
    }

    @PostMapping("/orders")
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) throws Exception {
        Order order = request.getOrder();
        if (order == null) {
            throw new IllegalArgumentException("Request body must include an order");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderWorkflowService.startOrder(order));
    }

    @GetMapping("/orders/{orderId}")
    public OrderFulfillmentResult getOrder(@PathVariable String orderId) {
        return orderWorkflowService.getStatus(orderId);
    }

    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<Map<String, String>> approve(@PathVariable String orderId) {
        orderWorkflowService.approve(orderId);
        return ResponseEntity.ok(Map.of("orderId", orderId, "message", "Approval signal sent"));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "Rejected via API") String reason) {
        orderWorkflowService.reject(orderId, reason);
        return ResponseEntity.ok(Map.of("orderId", orderId, "message", "Rejection signal sent", "reason", reason));
    }

    @GetMapping("/inventory")
    public List<StockItem> listInventory() {
        return inventoryService.listStock();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Unexpected error"));
    }
}
