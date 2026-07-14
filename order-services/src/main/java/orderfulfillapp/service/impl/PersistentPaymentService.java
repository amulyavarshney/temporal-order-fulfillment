package orderfulfillapp.service.impl;

import orderfulfillapp.Shared;
import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.model.CreditCard;
import orderfulfillapp.model.Order;
import orderfulfillapp.persistence.PaymentTransactionEntity;
import orderfulfillapp.persistence.PaymentTransactionRepository;
import orderfulfillapp.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PersistentPaymentService implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PersistentPaymentService.class);

    private final PaymentTransactionRepository paymentRepository;

    public PersistentPaymentService(PaymentTransactionRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public String processPayment(Order order) throws CreditCardExpiredException {
        if (order.getPayment() == null || order.getPayment().getCreditCard() == null) {
            throw new IllegalArgumentException("Order payment credit card is required");
        }

        CreditCard card = order.getPayment().getCreditCard();
        if (Shared.EXPIRED_CARD_SENTINEL.equals(card.getExpiration())) {
            throw new CreditCardExpiredException("Payment failed: Credit card expired");
        }

        String paymentReference = "pay-" + UUID.randomUUID();
        PaymentTransactionEntity entity = new PaymentTransactionEntity(
                paymentReference,
                order.getOrderId(),
                order.getTotalAmount(),
                PaymentTransactionEntity.PaymentStatus.CHARGED);
        paymentRepository.save(entity);

        logger.info("Payment charged orderId={} reference={} amount={}",
                order.getOrderId(), paymentReference, order.getTotalAmount());
        return paymentReference;
    }

    @Override
    @Transactional
    public String refundPayment(Order order, String paymentReference) {
        PaymentTransactionEntity entity = paymentRepository.findByPaymentReference(paymentReference)
                .orElseGet(() -> new PaymentTransactionEntity(
                        paymentReference != null ? paymentReference : "pay-" + UUID.randomUUID(),
                        order.getOrderId(),
                        order.getTotalAmount(),
                        PaymentTransactionEntity.PaymentStatus.CHARGED));

        entity.setStatus(PaymentTransactionEntity.PaymentStatus.REFUNDED);
        paymentRepository.save(entity);

        String message = "Refunded payment " + entity.getPaymentReference() + " for order " + order.getOrderId();
        logger.info(message);
        return message;
    }
}
