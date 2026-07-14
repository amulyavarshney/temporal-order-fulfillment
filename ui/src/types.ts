export type FulfillmentStatus =
  | "PENDING"
  | "AWAITING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "PAYMENT_PROCESSED"
  | "INVENTORY_RESERVED"
  | "DELIVERED"
  | "COMPLETED"
  | "FAILED"
  | "COMPENSATED";

export interface OrderItem {
  itemName: string;
  itemPrice: number;
  quantity: number;
}

export interface CreditCard {
  number: string;
  expiration: string;
}

export interface Order {
  orderId?: string;
  items: OrderItem[];
  payment: { creditCard: CreditCard };
}

export interface OrderFulfillmentResult {
  orderId: string;
  status: FulfillmentStatus;
  totalAmount: number;
  approvalRequired: boolean;
  paymentResult?: string;
  inventoryResult?: string;
  deliveryResult?: string;
  failureReason?: string;
  compensations?: string[];
  updatedAtEpochMs?: number;
}

export interface StockItem {
  itemName: string;
  itemPrice: number;
  stock: number;
}

export interface CreateOrderResponse {
  orderId: string;
  workflowId: string;
  message: string;
}

export const CATALOG: StockItem[] = [
  { itemName: "Pima Cotton T-Shirt", itemPrice: 49.99, stock: 923 },
  { itemName: "Low Top Sneaker (Men)", itemPrice: 67.0, stock: 542 },
  { itemName: "Cloudmonster Running Shoe (Men)", itemPrice: 126.99, stock: 481 },
  { itemName: "Wool Suit", itemPrice: 599.99, stock: 112 },
  { itemName: "Performance Polo", itemPrice: 39.99, stock: 510 },
  { itemName: "Cotton Hoodie", itemPrice: 64.99, stock: 399 },
];

export const STATUS_STEPS: FulfillmentStatus[] = [
  "PENDING",
  "AWAITING_APPROVAL",
  "APPROVED",
  "PAYMENT_PROCESSED",
  "INVENTORY_RESERVED",
  "DELIVERED",
  "COMPLETED",
];

export function formatMoney(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(amount);
}

export function orderTotal(items: OrderItem[]): number {
  return items.reduce((sum, item) => sum + item.itemPrice * item.quantity, 0);
}
