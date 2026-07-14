package orderfulfillapp.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for Temporal service stubs and workflow clients using env-based config.
 */
public final class TemporalConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(TemporalConnectionFactory.class);

    private TemporalConnectionFactory() {
    }

    public static WorkflowServiceStubs createServiceStubs(AppConfig config) {
        String target = config.getTemporalTarget();
        logger.info("Connecting to Temporal at {} (namespace={})", target, config.getTemporalNamespace());

        if ("local".equalsIgnoreCase(target) || "localhost:7233".equals(target)) {
            // Prefer explicit target so Docker / remote overrides still work with defaults.
            return WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget(target)
                            .build());
        }

        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(target)
                        .build());
    }

    public static WorkflowClient createWorkflowClient(AppConfig config) {
        WorkflowServiceStubs stubs = createServiceStubs(config);
        return WorkflowClient.newInstance(
                stubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(config.getTemporalNamespace())
                        .build());
    }

    public static WorkflowClient createWorkflowClient(AppConfig config, WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(
                stubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(config.getTemporalNamespace())
                        .build());
    }
}
