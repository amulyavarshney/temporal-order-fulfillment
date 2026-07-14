package orderfulfillapp.service.impl;

import orderfulfillapp.exception.CreditCardExpiredException;
import orderfulfillapp.exception.InsufficientInventoryException;
import orderfulfillapp.model.CreditCard;
import orderfulfillapp.model.Order;
import orderfulfillapp.model.OrderItem;
import orderfulfillapp.model.Payment;
import orderfulfillapp.model.StockItem;
import orderfulfillapp.persistence.PaymentTransactionEntity;
import orderfulfillapp.persistence.PaymentTransactionRepository;
import orderfulfillapp.persistence.StockItemEntity;
import orderfulfillapp.persistence.StockItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistentServicesTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @InjectMocks
    private PersistentPaymentService paymentService;

    @InjectMocks
    private PersistentInventoryService inventoryService;

    @Test
    void processPaymentPersistsCharge() throws Exception {
        Order order = sampleOrder("12/25");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String ref = paymentService.processPayment(order);

        assertTrue(ref.startsWith("pay-"));
        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentTransactionEntity.PaymentStatus.CHARGED, captor.getValue().getStatus());
    }

    @Test
    void processPaymentRejectsExpiredCard() {
        Order order = sampleOrder("12/23");
        assertThrows(CreditCardExpiredException.class, () -> paymentService.processPayment(order));
    }

    @Test
    void reserveInventoryDecrementsStock() throws Exception {
        StockItemEntity entity = new StockItemEntity("Shirt", 49.99, 10);
        when(stockItemRepository.findByItemName("Shirt")).thenReturn(Optional.of(entity));
        when(stockItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String ref = inventoryService.reserveInventory(sampleOrder("12/25"));

        assertTrue(ref.startsWith("inv-"));
        assertEquals(8, entity.getStock());
    }

    @Test
    void reserveInventoryFailsWhenMissing() {
        when(stockItemRepository.findByItemName("Shirt")).thenReturn(Optional.empty());
        assertThrows(InsufficientInventoryException.class,
                () -> inventoryService.reserveInventory(sampleOrder("12/25")));
    }

    @Test
    void listStockMapsEntities() {
        when(stockItemRepository.findAll()).thenReturn(List.of(new StockItemEntity("Shirt", 10.0, 3)));
        List<StockItem> stock = inventoryService.listStock();
        assertEquals(1, stock.size());
        assertEquals("Shirt", stock.get(0).getItemName());
        assertEquals(3, stock.get(0).getStock());
    }

    private static Order sampleOrder(String expiration) {
        Order order = new Order(
                List.of(new OrderItem("Shirt", 49.99, 2)),
                new Payment(new CreditCard("4111111111111111", expiration)));
        order.setOrderId("order-1");
        return order;
    }
}
