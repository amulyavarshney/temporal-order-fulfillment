package orderfulfillapp.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalClientConfig {

    @Bean
    public AppConfig appConfig() {
        return AppConfig.fromEnvironment();
    }

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs(AppConfig appConfig) {
        return TemporalConnectionFactory.createServiceStubs(appConfig);
    }

    @Bean
    public WorkflowClient workflowClient(AppConfig appConfig, WorkflowServiceStubs stubs) {
        return TemporalConnectionFactory.createWorkflowClient(appConfig, stubs);
    }
}
