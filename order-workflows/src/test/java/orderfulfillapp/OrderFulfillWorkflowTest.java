package orderfulfillapp;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowExtension;
import orderfulfillapp.activities.OrderFulfillActivitiesImpl;
import orderfulfillapp.dto.FulfillmentStatus;
import orderfulfillapp.dto.OrderFulfillmentResult;
import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.CreditCard;
import orderfulfillapp.model.Order;
import orderfulfillapp.model.OrderItem;
import orderfulfillapp.model.Payment;
import orderfulfillapp.model.StockItem;
import orderfulfillapp.service.DeliveryService;
import orderfulfillapp.service.InventoryService;
import orderfulfillapp.service.PaymentService;
import orderfulfillapp.workflows.OrderFulfillWorkflow;
import orderfulfillapp.workflows.OrderFulfillWorkflowImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderFulfillWorkflowTest {

    private static final InMemoryPaymentService paymentService = new InMemoryPaymentService();
    private static final InMemoryInventoryService inventoryService = new InMemoryInventoryService();
    private static final DeliveryService deliveryService =
            order -> "Order delivered for " + order.getItems().size() + " items";

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(OrderFulfillWorkflowImpl.class)
                    .setActivityImplementations(
                            new OrderFulfillActivitiesImpl(
                                    paymentService, inventoryService, deliveryService, 10_000))
                    .build();

    @BeforeEach
    void resetState() {
        inventoryService.reset(Map.of("Shirt", 10, "Suit", 25));
        paymentService.reset();
    }

    @Test
    void successfulFulfillment(OrderFulfillWorkflow workflow) {
        Order order = order("Shirt", 49.99, 2, "12/25");

        OrderFulfillmentResult result = workflow.fulfillOrder(order);

        assertEquals(FulfillmentStatus.COMPLETED, result.getStatus());
        assertEquals(order.getOrderId(), result.getOrderId());
        assertEquals(8, inventoryService.stockOf("Shirt"));
        assertNotNull(result.getPaymentResult());
        assertNotNull(result.getInventoryResult());
        assertTrue(result.getDeliveryResult().contains("delivered"));
    }

    @Test
    void expiredCardFailsWithoutConsumingInventory(OrderFulfillWorkflow workflow) {
        Order order = order("Shirt", 49.99, 1, "12/23");

        assertThrows(Exception.class, () -> workflow.fulfillOrder(order));
        assertEquals(10, inventoryService.stockOf("Shirt"));
        assertFalse(paymentService.wasRefunded());
    }

    @Test
    void inventoryFailureCompensatesPayment(OrderFulfillWorkflow workflow) {
        Order order = order("Shirt", 49.99, 50, "12/25");

        assertThrows(Exception.class, () -> workflow.fulfillOrder(order));
        assertEquals(10, inventoryService.stockOf("Shirt"));
        assertTrue(paymentService.wasRefunded());
    }

    @Test
    void highValueOrderRequiresApprovalSignal(OrderFulfillWorkflow workflow) throws Exception {
        Order order = order("Suit", 599.99, 20, "12/25");

        WorkflowClient.start(workflow::fulfillOrder, order);

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (workflow.getStatus().getStatus() != FulfillmentStatus.AWAITING_APPROVAL
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(FulfillmentStatus.AWAITING_APPROVAL, workflow.getStatus().getStatus());

        workflow.approveOrder();

        OrderFulfillmentResult result =
                WorkflowStub.fromTyped(workflow).getResult(OrderFulfillmentResult.class);

        assertEquals(FulfillmentStatus.COMPLETED, result.getStatus());
        assertTrue(result.isApprovalRequired());
    }

    @Test
    void highValueOrderRejection(OrderFulfillWorkflow workflow) throws Exception {
        Order order = order("Suit", 599.99, 20, "12/25");

        WorkflowClient.start(workflow::fulfillOrder, order);

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (workflow.getStatus().getStatus() != FulfillmentStatus.AWAITING_APPROVAL
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        workflow.rejectOrder("Manager declined");

        assertThrows(Exception.class,
                () -> WorkflowStub.fromTyped(workflow).getResult(OrderFulfillmentResult.class));
        assertEquals(FulfillmentStatus.REJECTED, workflow.getStatus().getStatus());
        assertEquals(25, inventoryService.stockOf("Suit"));
    }

    @Test
    void orderTotalCalculation() {
        Order order = order("Shirt", 49.99, 2, "12/25");
        assertEquals(99.98, order.getTotalAmount(), 0.01);
    }

    @Test
    void creditCardMasking() {
        CreditCard card = new CreditCard("1234567890123456", "12/25");
        assertTrue(card.toString().contains("**** **** **** 3456"));
        assertFalse(card.toString().contains("1234567890123456"));
    }

    @Test
    void validationRejectsEmptyItems() {
        Order order = new Order(List.of(), new Payment(new CreditCard("1234", "12/25")));
        assertThrows(IllegalArgumentException.class, order::validate);
    }

    private static Order order(String name, double price, int qty, String expiration) {
        Order created = new Order(
                List.of(new OrderItem(name, price, qty)),
                new Payment(new CreditCard("4111111111111111", expiration)));
        created.ensureIdentity();
        return created;
    }

    static final class InMemoryPaymentService implements PaymentService {
        private final List<String> charges = new ArrayList<>();
        private final List<String> refunds = new ArrayList<>();

        void reset() {
            charges.clear();
            refunds.clear();
        }

        boolean wasRefunded() {
            return !refunds.isEmpty();
        }

        @Override
        public String processPayment(Order order) throws CreditCardExpiredException {
            if ("12/23".equals(order.getPayment().getCreditCard().getExpiration())) {
                throw new CreditCardExpiredException("Payment failed: Credit card expired");
            }
            String ref = "pay-" + UUID.randomUUID();
            charges.add(ref);
            return ref;
        }

        @Override
        public String refundPayment(Order order, String paymentReference) {
            refunds.add(paymentReference);
            return "Refunded " + paymentReference;
        }
    }

    static final class InMemoryInventoryService implements InventoryService {
        private final Map<String, Integer> stock = new ConcurrentHashMap<>();

        void reset(Map<String, Integer> initial) {
            stock.clear();
            stock.putAll(initial);
        }

        int stockOf(String name) {
            return stock.getOrDefault(name, 0);
        }

        @Override
        public String reserveInventory(Order order) throws InsufficientInventoryException {
            for (OrderItem item : order.getItems()) {
                int available = stock.getOrDefault(item.getItemName(), 0);
                if (available < item.getQuantity()) {
                    throw new InsufficientInventoryException(
                            "Insufficient stock for item: " + item.getItemName());
                }
                stock.put(item.getItemName(), available - item.getQuantity());
            }
            return "inv-" + UUID.randomUUID();
        }

        @Override
        public String releaseInventory(Order order, String reservationReference) {
            for (OrderItem item : order.getItems()) {
                stock.merge(item.getItemName(), item.getQuantity(), Integer::sum);
            }
            return "Released " + reservationReference;
        }

        @Override
        public List<StockItem> listStock() {
            List<StockItem> items = new ArrayList<>();
            stock.forEach((k, v) -> items.add(new StockItem(k, 0, v)));
            return items;
        }
    }
}
