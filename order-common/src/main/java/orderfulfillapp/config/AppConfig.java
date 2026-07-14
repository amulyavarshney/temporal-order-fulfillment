package orderfulfillapp.config;

import orderfulfillapp.Shared;

/**
 * Runtime configuration resolved from environment variables with sensible defaults.
 */
public final class AppConfig {

    private final String temporalTarget;
    private final String temporalNamespace;
    private final String taskQueue;
    private final double approvalThreshold;
    private final long approvalTimeoutSeconds;
    private final long activityStartToCloseSeconds;
    private final int activityMaxAttempts;

    private AppConfig(
            String temporalTarget,
            String temporalNamespace,
            String taskQueue,
            double approvalThreshold,
            long approvalTimeoutSeconds,
            long activityStartToCloseSeconds,
            int activityMaxAttempts) {
        this.temporalTarget = temporalTarget;
        this.temporalNamespace = temporalNamespace;
        this.taskQueue = taskQueue;
        this.approvalThreshold = approvalThreshold;
        this.approvalTimeoutSeconds = approvalTimeoutSeconds;
        this.activityStartToCloseSeconds = activityStartToCloseSeconds;
        this.activityMaxAttempts = activityMaxAttempts;
    }

    public static AppConfig fromEnvironment() {
        return new AppConfig(
                env("TEMPORAL_ADDRESS", Shared.DEFAULT_TEMPORAL_TARGET),
                env("TEMPORAL_NAMESPACE", Shared.DEFAULT_TEMPORAL_NAMESPACE),
                env("TEMPORAL_TASK_QUEUE", Shared.ORDER_FULFILL_TASK_QUEUE),
                Double.parseDouble(env("APPROVAL_THRESHOLD", String.valueOf(Shared.DEFAULT_APPROVAL_THRESHOLD))),
                Long.parseLong(env("APPROVAL_TIMEOUT_SECONDS", "300")),
                Long.parseLong(env("ACTIVITY_TIMEOUT_SECONDS", "30")),
                Integer.parseInt(env("ACTIVITY_MAX_ATTEMPTS", "5")));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public String getTemporalTarget() {
        return temporalTarget;
    }

    public String getTemporalNamespace() {
        return temporalNamespace;
    }

    public String getTaskQueue() {
        return taskQueue;
    }

    public double getApprovalThreshold() {
        return approvalThreshold;
    }

    public long getApprovalTimeoutSeconds() {
        return approvalTimeoutSeconds;
    }

    public long getActivityStartToCloseSeconds() {
        return activityStartToCloseSeconds;
    }

    public int getActivityMaxAttempts() {
        return activityMaxAttempts;
    }
}
