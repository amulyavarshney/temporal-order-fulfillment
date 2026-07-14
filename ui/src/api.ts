import type {
  CreateOrderResponse,
  Order,
  OrderFulfillmentResult,
  StockItem,
} from "./types";

export type ClientMode = "demo" | "live";

const STORAGE_KEY = "fulfill-api-base";

export function getStoredApiBase(): string {
  return localStorage.getItem(STORAGE_KEY) || "http://localhost:8080";
}

export function setStoredApiBase(value: string): void {
  localStorage.setItem(STORAGE_KEY, value);
}

export function createApiClient(baseUrl: string) {
  const root = baseUrl.replace(/\/$/, "");

  async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${root}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {}),
      },
    });
    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      try {
        const body = (await response.json()) as { error?: string };
        if (body.error) message = body.error;
      } catch {
        /* ignore */
      }
      throw new Error(message);
    }
    return response.json() as Promise<T>;
  }

  return {
    createOrder(order: Order): Promise<CreateOrderResponse> {
      return request<CreateOrderResponse>("/api/orders", {
        method: "POST",
        body: JSON.stringify({ order }),
      });
    },
    getOrder(orderId: string): Promise<OrderFulfillmentResult> {
      return request<OrderFulfillmentResult>(`/api/orders/${orderId}`);
    },
    approve(orderId: string): Promise<void> {
      return request(`/api/orders/${orderId}/approve`, { method: "POST" }).then(
        () => undefined
      );
    },
    reject(orderId: string, reason: string): Promise<void> {
      const q = encodeURIComponent(reason);
      return request(`/api/orders/${orderId}/reject?reason=${q}`, {
        method: "POST",
      }).then(() => undefined);
    },
    listInventory(): Promise<StockItem[]> {
      return request<StockItem[]>("/api/inventory");
    },
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
