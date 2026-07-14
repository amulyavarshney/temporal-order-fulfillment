package orderfulfillapp;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import orderfulfillapp.activities.OrderFulfillActivitiesImpl;
import orderfulfillapp.config.AppConfig;
import orderfulfillapp.config.TemporalConnectionFactory;
import orderfulfillapp.service.DeliveryService;
import orderfulfillapp.service.InventoryService;
import orderfulfillapp.service.PaymentService;
import orderfulfillapp.workflows.OrderFulfillWorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TemporalWorkerRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TemporalWorkerRunner.class);

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;
    private final AppConfig appConfig;

    private WorkerFactory workerFactory;
    private WorkflowServiceStubs serviceStubs;

    public TemporalWorkerRunner(
            PaymentService paymentService,
            InventoryService inventoryService,
            DeliveryService deliveryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
        this.appConfig = AppConfig.fromEnvironment();
    }

    @Override
    public void run(ApplicationArguments args) {
        serviceStubs = TemporalConnectionFactory.createServiceStubs(appConfig);
        WorkflowClient client = TemporalConnectionFactory.createWorkflowClient(appConfig, serviceStubs);
        workerFactory = WorkerFactory.newInstance(client);

        Worker worker = workerFactory.newWorker(appConfig.getTaskQueue());
        worker.registerWorkflowImplementationTypes(OrderFulfillWorkflowImpl.class);
        worker.registerActivitiesImplementations(
                new OrderFulfillActivitiesImpl(
                        paymentService,
                        inventoryService,
                        deliveryService,
                        appConfig.getApprovalThreshold()));

        workerFactory.start();
        logger.info("Temporal worker started on task queue {} targeting {}",
                appConfig.getTaskQueue(), appConfig.getTemporalTarget());
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Temporal worker");
        if (workerFactory != null) {
            workerFactory.shutdown();
        }
        if (serviceStubs != null) {
            serviceStubs.shutdown();
        }
    }
}
