import type {
  CreateOrderResponse,
  FulfillmentStatus,
  Order,
  OrderFulfillmentResult,
  StockItem,
} from "./types";
import { CATALOG, orderTotal } from "./types";

type DemoOrder = OrderFulfillmentResult & {
  order: Order;
  timers: number[];
};

const APPROVAL_THRESHOLD = 10_000;

function uid(): string {
  return crypto.randomUUID();
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export class DemoBackend {
  private stock: StockItem[] = CATALOG.map((item) => ({ ...item }));
  private orders = new Map<string, DemoOrder>();

  listInventory(): StockItem[] {
    return this.stock.map((item) => ({ ...item }));
  }

  async createOrder(order: Order): Promise<CreateOrderResponse> {
    const orderId = order.orderId || uid();
    const total = orderTotal(order.items);
    const approvalRequired = total > APPROVAL_THRESHOLD;

    const result: DemoOrder = {
      orderId,
      status: "PENDING",
      totalAmount: total,
      approvalRequired,
      order: { ...order, orderId },
      timers: [],
      updatedAtEpochMs: Date.now(),
    };
    this.orders.set(orderId, result);

    if (approvalRequired) {
      this.advance(orderId, "AWAITING_APPROVAL");
    } else {
      void this.runHappyPath(orderId);
    }

    await sleep(120);
    return {
      orderId,
      workflowId: `order-fulfill-${orderId}`,
      message: "Order workflow started (demo)",
    };
  }

  getOrder(orderId: string): OrderFulfillmentResult {
    const order = this.orders.get(orderId);
    if (!order) {
      throw new Error(`Order not found: ${orderId}`);
    }
    const { order: _o, timers: _t, ...result } = order;
    return { ...result };
  }

  async approve(orderId: string): Promise<void> {
    const order = this.require(orderId);
    if (order.status !== "AWAITING_APPROVAL") {
      throw new Error("Order is not awaiting approval");
    }
    this.advance(orderId, "APPROVED");
    await this.runHappyPath(orderId, true);
  }

  async reject(orderId: string, reason: string): Promise<void> {
    const order = this.require(orderId);
    if (order.status !== "AWAITING_APPROVAL") {
      throw new Error("Order is not awaiting approval");
    }
    order.failureReason = reason || "Rejected in demo UI";
    this.advance(orderId, "REJECTED");
  }

  private require(orderId: string): DemoOrder {
    const order = this.orders.get(orderId);
    if (!order) throw new Error(`Order not found: ${orderId}`);
    return order;
  }

  private advance(orderId: string, status: FulfillmentStatus): void {
    const order = this.require(orderId);
    order.status = status;
    order.updatedAtEpochMs = Date.now();
  }

  private async runHappyPath(orderId: string, fromApproval = false): Promise<void> {
    const demo = this.require(orderId);

    if (demo.order.payment.creditCard.expiration === "12/23") {
      await sleep(400);
      demo.failureReason = "Payment failed: Credit card expired";
      this.advance(orderId, "FAILED");
      return;
    }

    if (!fromApproval) {
      await sleep(450);
    } else {
      await sleep(350);
    }

    demo.paymentResult = `pay-demo-${orderId.slice(0, 8)}`;
    this.advance(orderId, "PAYMENT_PROCESSED");
    await sleep(450);

    try {
      for (const item of demo.order.items) {
        const stock = this.stock.find((s) => s.itemName === item.itemName);
        if (!stock || stock.stock < item.quantity) {
          throw new Error(`Insufficient stock for item: ${item.itemName}`);
        }
        stock.stock -= item.quantity;
      }
      demo.inventoryResult = `inv-demo-${orderId.slice(0, 8)}`;
      this.advance(orderId, "INVENTORY_RESERVED");
    } catch (error) {
      demo.failureReason = error instanceof Error ? error.message : "Inventory failed";
      demo.compensations = [`Refunded ${demo.paymentResult}`];
      this.advance(orderId, "COMPENSATED");
      return;
    }

    await sleep(450);
    demo.deliveryResult = `Order delivered for ${demo.order.items.length} items`;
    this.advance(orderId, "DELIVERED");
    await sleep(300);
    this.advance(orderId, "COMPLETED");
  }
}
