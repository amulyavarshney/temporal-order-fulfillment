import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import {
  ClientMode,
  createApiClient,
  getStoredApiBase,
  setStoredApiBase,
} from "./api";
import { DemoBackend } from "./demo";
import {
  CATALOG,
  FulfillmentStatus,
  OrderFulfillmentResult,
  OrderItem,
  StockItem,
  STATUS_STEPS,
  formatMoney,
  orderTotal,
} from "./types";

const FLOW_COPY: Record<string, { title: string; blurb: string }> = {
  PENDING: { title: "Order received", blurb: "Workflow started on the task queue." },
  AWAITING_APPROVAL: {
    title: "Awaiting approval",
    blurb: "High-value orders pause for a human signal.",
  },
  APPROVED: { title: "Approved", blurb: "Approver signal received; continuing." },
  PAYMENT_PROCESSED: {
    title: "Payment processed",
    blurb: "Charge recorded; refundable via saga if later steps fail.",
  },
  INVENTORY_RESERVED: {
    title: "Inventory reserved",
    blurb: "Stock decremented for each line item.",
  },
  DELIVERED: { title: "Delivered", blurb: "Fulfillment handoff complete." },
  COMPLETED: { title: "Completed", blurb: "Workflow finished successfully." },
};

function statusTone(status: FulfillmentStatus): string {
  if (status === "COMPLETED" || status === "DELIVERED") return "ok";
  if (status === "AWAITING_APPROVAL") return "wait";
  if (status === "FAILED" || status === "REJECTED" || status === "COMPENSATED") return "bad";
  return "";
}

function stepState(
  step: FulfillmentStatus,
  current: FulfillmentStatus | null
): "done" | "current" | "" {
  if (!current) return "";
  if (current === "REJECTED" || current === "FAILED" || current === "COMPENSATED") {
    return "";
  }
  const order = STATUS_STEPS.filter((s) => s !== "AWAITING_APPROVAL" && s !== "APPROVED");
  const withApproval = STATUS_STEPS;
  const sequence =
    current === "AWAITING_APPROVAL" || current === "APPROVED" ? withApproval : order;
  const idx = sequence.indexOf(step);
  const cur = sequence.indexOf(current);
  if (idx < 0) return "";
  if (step === current) return "current";
  if (cur >= 0 && idx < cur) return "done";
  if (current === "COMPLETED" && step !== "COMPLETED") return "done";
  return "";
}

export default function App() {
  const [mode, setMode] = useState<ClientMode>("demo");
  const [apiBase, setApiBase] = useState(getStoredApiBase());
  const [demo] = useState(() => new DemoBackend());
  const api = useMemo(() => createApiClient(apiBase), [apiBase]);

  const [catalogItem, setCatalogItem] = useState(CATALOG[0].itemName);
  const [quantity, setQuantity] = useState(1);
  const [cardNumber, setCardNumber] = useState("4111111111111111");
  const [expiration, setExpiration] = useState("12/28");
  const [items, setItems] = useState<OrderItem[]>([]);
  const [inventory, setInventory] = useState<StockItem[]>(CATALOG);
  const [tracked, setTracked] = useState<OrderFulfillmentResult[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(
    "Demo mode runs entirely in the browser — ideal for GitHub Pages."
  );

  const selected = tracked.find((o) => o.orderId === selectedId) ?? null;
  const total = orderTotal(items);

  const refreshInventory = useCallback(async () => {
    try {
      if (mode === "demo") {
        setInventory(demo.listInventory());
        return;
      }
      setInventory(await api.listInventory());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load inventory");
    }
  }, [api, demo, mode]);

  const refreshOrder = useCallback(
    async (orderId: string) => {
      const result =
        mode === "demo" ? demo.getOrder(orderId) : await api.getOrder(orderId);
      setTracked((prev) => {
        const next = prev.filter((o) => o.orderId !== orderId);
        return [result, ...next];
      });
      return result;
    },
    [api, demo, mode]
  );

  useEffect(() => {
    void refreshInventory();
  }, [refreshInventory]);

  useEffect(() => {
    if (!selectedId) return;
    const tick = window.setInterval(() => {
      void refreshOrder(selectedId).then(() => {
        if (mode === "demo") setInventory(demo.listInventory());
      });
    }, mode === "demo" ? 400 : 1500);
    return () => window.clearInterval(tick);
  }, [selectedId, refreshOrder, mode, demo]);

  function addItem() {
    const stock = CATALOG.find((s) => s.itemName === catalogItem);
    if (!stock) return;
    setItems((prev) => {
      const existing = prev.find((i) => i.itemName === stock.itemName);
      if (existing) {
        return prev.map((i) =>
          i.itemName === stock.itemName
            ? { ...i, quantity: i.quantity + quantity }
            : i
        );
      }
      return [
        ...prev,
        { itemName: stock.itemName, itemPrice: stock.itemPrice, quantity },
      ];
    });
  }

  async function submitOrder(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (items.length === 0) {
      setError("Add at least one item to the order.");
      return;
    }
    setBusy(true);
    try {
      const order = {
        items,
        payment: { creditCard: { number: cardNumber, expiration } },
      };
      const created =
        mode === "demo"
          ? await demo.createOrder(order)
          : await api.createOrder(order);
      setNotice(`Started workflow for ${created.orderId}`);
      setSelectedId(created.orderId);
      await refreshOrder(created.orderId);
      await refreshInventory();
      setItems([]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start order");
    } finally {
      setBusy(false);
    }
  }

  async function approve() {
    if (!selectedId) return;
    setBusy(true);
    setError(null);
    try {
      if (mode === "demo") await demo.approve(selectedId);
      else await api.approve(selectedId);
      setNotice(`Approval signal sent for ${selectedId}`);
      await refreshOrder(selectedId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Approve failed");
    } finally {
      setBusy(false);
    }
  }

  async function reject() {
    if (!selectedId) return;
    setBusy(true);
    setError(null);
    try {
      if (mode === "demo") await demo.reject(selectedId, "Rejected from sample UI");
      else await api.reject(selectedId, "Rejected from sample UI");
      setNotice(`Rejection signal sent for ${selectedId}`);
      await refreshOrder(selectedId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Reject failed");
    } finally {
      setBusy(false);
    }
  }

  function switchMode(next: ClientMode) {
    setMode(next);
    setTracked([]);
    setSelectedId(null);
    setError(null);
    setNotice(
      next === "demo"
        ? "Demo mode runs entirely in the browser — ideal for GitHub Pages."
        : `Live mode calls ${apiBase}. Start the API/worker stack locally or remotely.`
    );
  }

  const visibleSteps = STATUS_STEPS.filter((step) => {
    if (step === "AWAITING_APPROVAL" || step === "APPROVED") {
      return Boolean(selected?.approvalRequired);
    }
    return true;
  });

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <p className="brand-mark">Fulfill</p>
          <p className="brand-sub">
            Temporal order orchestration — payment, inventory reservation,
            delivery, and human approval for high-value carts.
          </p>
        </div>
        <div className="mode-panel">
          <div className="mode-toggle" role="group" aria-label="Client mode">
            <button
              type="button"
              className={mode === "demo" ? "active" : ""}
              onClick={() => switchMode("demo")}
            >
              Demo
            </button>
            <button
              type="button"
              className={mode === "live" ? "active" : ""}
              onClick={() => switchMode("live")}
            >
              Live API
            </button>
          </div>
          {mode === "live" && (
            <div className="api-row">
              <input
                aria-label="API base URL"
                value={apiBase}
                onChange={(e) => setApiBase(e.target.value)}
                onBlur={() => setStoredApiBase(apiBase)}
                placeholder="http://localhost:8080"
              />
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setStoredApiBase(apiBase);
                  void refreshInventory();
                }}
              >
                Save
              </button>
            </div>
          )}
        </div>
      </header>

      {notice && <div className="banner">{notice}</div>}
      {error && <div className="banner error">{error}</div>}

      <section className="hero-grid">
        <form className="panel" onSubmit={submitOrder}>
          <h2>Place an order</h2>
          <p className="lede">
            Orders over {formatMoney(10000)} pause for approval. Use card
            expiration <code>12/23</code> to simulate a payment failure.
          </p>

          <div className="form-grid">
            <div className="qty-row">
              <div className="field">
                <label htmlFor="item">Catalog item</label>
                <select
                  id="item"
                  value={catalogItem}
                  onChange={(e) => setCatalogItem(e.target.value)}
                >
                  {CATALOG.map((item) => (
                    <option key={item.itemName} value={item.itemName}>
                      {item.itemName} — {formatMoney(item.itemPrice)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="qty">Qty</label>
                <input
                  id="qty"
                  type="number"
                  min={1}
                  max={99}
                  value={quantity}
                  onChange={(e) => setQuantity(Number(e.target.value) || 1)}
                />
              </div>
            </div>

            <div className="actions">
              <button type="button" className="btn btn-ghost" onClick={addItem}>
                Add item
              </button>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setItems([
                    {
                      itemName: "Wool Suit",
                      itemPrice: 599.99,
                      quantity: 20,
                    },
                  ]);
                }}
              >
                High-value sample
              </button>
            </div>

            <div className="line-items">
              {items.length === 0 && (
                <div className="line-item">No items yet — add from the catalog.</div>
              )}
              {items.map((item) => (
                <div className="line-item" key={item.itemName}>
                  <span>
                    {item.quantity} × {item.itemName}
                  </span>
                  <span>
                    {formatMoney(item.itemPrice * item.quantity)}{" "}
                    <button
                      type="button"
                      onClick={() =>
                        setItems((prev) =>
                          prev.filter((i) => i.itemName !== item.itemName)
                        )
                      }
                    >
                      Remove
                    </button>
                  </span>
                </div>
              ))}
            </div>

            <div className="qty-row">
              <div className="field">
                <label htmlFor="card">Card number</label>
                <input
                  id="card"
                  value={cardNumber}
                  onChange={(e) => setCardNumber(e.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="exp">Expiration</label>
                <input
                  id="exp"
                  value={expiration}
                  onChange={(e) => setExpiration(e.target.value)}
                  placeholder="MM/YY"
                />
              </div>
            </div>

            <div className="totals">
              <span>Order total</span>
              <strong>{formatMoney(total)}</strong>
            </div>

            <button className="btn btn-primary" type="submit" disabled={busy}>
              {busy ? "Starting workflow…" : "Start fulfillment"}
            </button>
          </div>
        </form>

        <aside className="panel flow-panel">
          <h2>Workflow path</h2>
          <p className="lede">
            The same Temporal saga you run in Docker — visualized as it moves.
          </p>
          <ol className="flow-steps">
            {visibleSteps.map((step) => {
              const copy = FLOW_COPY[step];
              const state = stepState(step, selected?.status ?? null);
              return (
                <li key={step} className={state}>
                  <span className="step-dot" aria-hidden />
                  <div className="step-copy">
                    <strong>{copy.title}</strong>
                    <span>{copy.blurb}</span>
                  </div>
                </li>
              );
            })}
          </ol>
        </aside>
      </section>

      <section className="inventory panel">
        <h2>Inventory</h2>
        <p className="lede">Live stock snapshot from the fulfillment service.</p>
        <div className="inventory-scroll">
          {inventory.slice(0, 8).map((item) => (
            <div className="stock-chip" key={item.itemName}>
              <strong>{item.itemName}</strong>
              <span>
                {item.stock} in stock · {formatMoney(item.itemPrice)}
              </span>
            </div>
          ))}
        </div>
      </section>

      <section className="orders-layout">
        <div className="panel">
          <h2>Orders</h2>
          <p className="lede">Select an order to inspect status and signals.</p>
          <div className="order-list">
            {tracked.length === 0 && (
              <div className="detail-empty">No workflows yet.</div>
            )}
            {tracked.map((order) => (
              <button
                type="button"
                key={order.orderId}
                className={order.orderId === selectedId ? "selected" : ""}
                onClick={() => setSelectedId(order.orderId)}
              >
                <span className={`status-pill ${statusTone(order.status)}`}>
                  {order.status}
                </span>
                <strong>{order.orderId.slice(0, 8)}…</strong>
                <span className="meta">{formatMoney(order.totalAmount)}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="panel detail">
          <h2>Order detail</h2>
          {!selected && (
            <div className="detail-empty">
              Place an order to watch Temporal drive payment → inventory →
              delivery.
            </div>
          )}
          {selected && (
            <>
              <div>
                <span className={`status-pill ${statusTone(selected.status)}`}>
                  {selected.status}
                </span>
              </div>
              <dl className="timeline">
                <div className="timeline-row">
                  <dt>Order ID</dt>
                  <dd>{selected.orderId}</dd>
                </div>
                <div className="timeline-row">
                  <dt>Total</dt>
                  <dd>{formatMoney(selected.totalAmount)}</dd>
                </div>
                <div className="timeline-row">
                  <dt>Approval</dt>
                  <dd>{selected.approvalRequired ? "Required" : "Not required"}</dd>
                </div>
                {selected.paymentResult && (
                  <div className="timeline-row">
                    <dt>Payment</dt>
                    <dd>{selected.paymentResult}</dd>
                  </div>
                )}
                {selected.inventoryResult && (
                  <div className="timeline-row">
                    <dt>Inventory</dt>
                    <dd>{selected.inventoryResult}</dd>
                  </div>
                )}
                {selected.deliveryResult && (
                  <div className="timeline-row">
                    <dt>Delivery</dt>
                    <dd>{selected.deliveryResult}</dd>
                  </div>
                )}
                {selected.failureReason && (
                  <div className="timeline-row">
                    <dt>Failure</dt>
                    <dd>{selected.failureReason}</dd>
                  </div>
                )}
                {selected.compensations && selected.compensations.length > 0 && (
                  <div className="timeline-row">
                    <dt>Saga</dt>
                    <dd>{selected.compensations.join("; ")}</dd>
                  </div>
                )}
              </dl>
              {selected.status === "AWAITING_APPROVAL" && (
                <div className="actions">
                  <button
                    type="button"
                    className="btn btn-teal"
                    disabled={busy}
                    onClick={() => void approve()}
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    disabled={busy}
                    onClick={() => void reject()}
                  >
                    Reject
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </section>

      <p className="footer-note">
        Sample UI for{" "}
        <a href="https://github.com/amulyavarshney/temporal-order-fulfillment">
          temporal-order-fulfillment
        </a>
        . Backend stack remains Temporal + Spring Boot.
      </p>
    </div>
  );
}
