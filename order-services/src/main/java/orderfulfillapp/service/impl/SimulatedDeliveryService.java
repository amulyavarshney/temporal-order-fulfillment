package orderfulfillapp.service.impl;

import orderfulfillapp.model.Order;
import orderfulfillapp.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimulatedDeliveryService implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatedDeliveryService.class);

    @Override
    public String deliverOrder(Order order) {
        logger.info("Delivering order {}", order.getOrderId());
        return "Order delivered for " + order.getItems().size() + " items (orderId=" + order.getOrderId() + ")";
    }
}
