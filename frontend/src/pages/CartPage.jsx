import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function CartPage() {
  const { cart, updateQty, applyCode, checkout, token, notify } = useStore();
  const [code, setCode] = useState("");
  const [method, setMethod] = useState("MTN_MOMO");
  const [delivery, setDelivery] = useState("HOME_KIGALI");
  const [address, setAddress] = useState("");
  const [pending, setPending] = useState(false);
  const key = useRef(null);
  const [stub, setStub] = useState("SUCCESS");
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const items = cart?.items ?? [];
  const fee = cart?.pricedAsTier === "GOLD" || (cart?.subtotalRwf ?? 0) >= 50000 ? 0 : delivery === "PICKUP" ? 1000 : delivery === "HOME_OTHER" ? 4000 : 2500;
  const total = (cart?.subtotalRwf ?? 0) - (cart?.discountRwf ?? 0) + fee;

  async function pay(e) {
    e.preventDefault();
    setError("");
    if (!token) {
      navigate("/login");
      return;
    }
    if (pending) return;
    setPending(true);
    key.current ||= crypto.randomUUID();
    try {
      const placed = await checkout(method, stub, delivery, address, key.current);
      key.current = null;
      setOrder(placed);
      notify(placed.status === "PAID" ? "Order paid." : `Order ${placed.status.replaceAll("_", " ").toLowerCase()}`);
    } catch (err) {
      setError(err.message);
    } finally { setPending(false); }
  }

  return (
    <div className="wrap">
      <div className="section-head" style={{ marginTop: 28 }}>
        <h1 className="page-title" style={{ margin: 0 }}>Your cart</h1>
        <Link to="/">Continue shopping</Link>
      </div>
      <div className="cart-layout">
        <div>
          {items.length === 0 ? (
            <div className="empty">
              Cart is empty. <Link to="/">Continue shopping</Link>
            </div>
          ) : null}
          {items.map((line) => (
            <div className="cart-line" key={line.productId}>
              <Media name={line.name} sku={line.sku} imageUrl={line.imageUrl} className="thumb" />
              <div>
                <Link to={`/product/${line.productId}`}><strong>{line.name}</strong></Link>
                <div className="sku">{line.sku}</div>
                <div className="muted">{money(line.unitPriceRwf)} each</div>
                <div className="qty-row" style={{ margin: "8px 0 0" }}>
                  <button className="btn" type="button" onClick={() => updateQty(line.productId, line.qty - 1)}>−</button>
                  <span>{line.qty}</span>
                  <button className="btn" type="button" onClick={() => updateQty(line.productId, line.qty + 1)}>+</button>
                  <button className="btn btn-ghost" type="button" onClick={() => updateQty(line.productId, 0)}>Remove</button>
                </div>
              </div>
              <div className="price">{money(line.lineTotalRwf)}</div>
            </div>
          ))}
        </div>
        <aside className="summary">
          <h2>Order summary</h2>
          {cart ? (
            <dl>
              <dt>Subtotal</dt><dd>{money(cart.subtotalRwf)}</dd>
              <dt>Discount</dt><dd>− {money(cart.discountRwf)}</dd>
              <dt>Shipping</dt><dd>{money(fee)}</dd>
              <dt className="total">Total</dt><dd className="total">{money(total)}</dd>
            </dl>
          ) : null}
          <p className="muted" style={{ marginTop: 0 }}>Priced as {cart?.pricedAsTier ?? "STANDARD"}</p>
          <form onSubmit={pay}>
            <div className="field"><label htmlFor="delivery-method">Delivery</label><select id="delivery-method" value={delivery} onChange={e => { setDelivery(e.target.value); key.current = null; }}><option value="HOME_KIGALI">Home delivery — Kigali</option><option value="HOME_OTHER">Home delivery — outside Kigali</option><option value="PICKUP">Pickup point</option></select></div>
            <div className="field"><label htmlFor="delivery-address">{delivery === "PICKUP" ? "Pickup point" : "Delivery address"}</label><input id="delivery-address" required maxLength={255} value={address} onChange={e => { setAddress(e.target.value); key.current = null; }} /></div>
            <p className="muted">Your payment stays in escrow until delivery and the 48-hour dispute window close.</p>
            <div className="field">
              <label>Promo code</label>
              <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="SAVE10" />
            </div>
            <button className="btn btn-ghost btn-block" type="button" disabled={!code.trim()} onClick={() => applyCode(code).catch((e) => setError(e.message))}>
              Apply code
            </button>
            <div className="field" style={{ marginTop: 16 }}>
              <label>Payment</label>
              <select value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="MTN_MOMO">MTN MoMo</option>
                <option value="AIRTEL_MONEY">Airtel Money</option>
                <option value="CARD">Card</option>
              </select>
            </div>
            <div className="field">
              <label>Gateway stub (demo)</label>
              <select value={stub} onChange={(e) => setStub(e.target.value)}>
                <option value="SUCCESS">SUCCESS</option>
                <option value="DECLINE">DECLINE</option>
                <option value="TIMEOUT">TIMEOUT</option>
              </select>
            </div>
            {error ? <p className="error">{error}</p> : null}
            {order ? (
              <p>
                Placed <strong>{order.orderNumber}</strong> — {order.status}.
                {" "}<Link to="/orders">View orders</Link>
              </p>
            ) : null}
            <button className="btn btn-primary btn-block" type="submit" disabled={items.length === 0 || pending}>
              {token ? "Place order" : "Sign in to checkout"}
            </button>
          </form>
        </aside>
      </div>
    </div>
  );
}
