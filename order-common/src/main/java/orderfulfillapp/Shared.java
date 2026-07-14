package orderfulfillapp;

/**
 * Shared constants for the order fulfillment platform.
 */
public final class Shared {

    public static final String STOCK_DATABASE_PATH = "data/stock_database.json";
    public static final String ORDER_FULFILL_TASK_QUEUE = "OrderFulfillTaskQueue";
    public static final String DEFAULT_TEMPORAL_TARGET = "localhost:7233";
    public static final String DEFAULT_TEMPORAL_NAMESPACE = "default";
    public static final double DEFAULT_APPROVAL_THRESHOLD = 10_000.0;
    public static final String EXPIRED_CARD_SENTINEL = "12/23";

    private Shared() {
    }
}
