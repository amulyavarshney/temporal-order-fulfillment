package orderfulfillapp;

import orderfulfillapp.config.AppConfig;
import orderfulfillapp.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void defaultsAreSensible() {
        AppConfig config = AppConfig.fromEnvironment();
        assertEquals("localhost:7233", config.getTemporalTarget());
        assertEquals("default", config.getTemporalNamespace());
        assertEquals("OrderFulfillTaskQueue", config.getTaskQueue());
        assertEquals(10_000.0, config.getApprovalThreshold());
        assertTrue(config.getActivityMaxAttempts() >= 1);
    }

    @Test
    void orderEnsureIdentityAssignsId() {
        Order order = new Order();
        order.ensureIdentity();
        assertTrue(order.getOrderId() != null && !order.getOrderId().isBlank());
    }
}
