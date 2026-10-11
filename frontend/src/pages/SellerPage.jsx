import { useEffect, useState } from "react";
import { money } from "../api";
import { useStore } from "../store";
import { compressProductPhoto } from "../image";

const blank = { sku: "", name: "", description: "", category: "Fashion", priceRwf: 8000, stockQty: 20, status: "DRAFT", imageUrl: "" };
export function SellerPage() {
  const { call, notify, refreshCatalog, user } = useStore();
  const [shop, setShop] = useState(null);
  const [loading, setLoading] = useState(true);
  const [application, setApplication] = useState({ name: "", description: "", businessReference: "" });
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(null);
  const [tracking, setTracking] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function load() {
    const { data: next } = await call("/seller/shop");
    setShop(next);
    if (next?.status === "APPROVED" && user?.roles?.includes("SELLER")) {
      const results = await Promise.all([call("/seller/products"), call("/seller/orders"), call("/seller/payouts")]);
      setProducts(results[0].data); setOrders(results[1].data); setPayouts(results[2].data);
    }
  }
  useEffect(() => { load().catch(e => setError(e.message)).finally(() => setLoading(false)); }, [call, user?.roles?.includes("SELLER")]);
  async function action(fn) {
    setError(""); setBusy(true);
    try { await fn(); await load(); await refreshCatalog(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  if (loading) return <div className="wrap empty">Loading your shop…</div>;
  return <div className="wrap">
    <h1 className="page-title">Seller portal</h1>
    {error && <p className="error" role="alert">{error}</p>}
    {!shop ? <form className="summary" onSubmit={e => { e.preventDefault(); action(() => call("/seller/shop", { method: "POST", body: application })); }}>
      <h2>Open your Isoko shop</h2><p className="muted">An administrator reviews your business reference before you can publish listings.</p>
      {[["name", "Shop name"], ["description", "About your shop"], ["businessReference", "Business / KYC reference"]].map(([key, label]) => <div className="field" key={key}><label htmlFor={`shop-${key}`}>{label}</label><input id={`shop-${key}`} required={key !== "description"} maxLength={key === "description" ? 2000 : 120} value={application[key]} onChange={e => setApplication({ ...application, [key]: e.target.value })} /></div>)}
      <button className="btn btn-primary" disabled={busy}>Submit for approval</button>
    </form> : <>
      <section className="order-card"><h2>{shop.name}</h2><span className="status-pill">{shop.status}</span><p>{shop.reviewNote || "Manual business verification"}</p><button className="btn" onClick={() => action(async () => {})} disabled={busy}>Refresh status</button></section>
      {shop.status === "APPROVED" && !user?.roles?.includes("SELLER") && <p>Sign out and sign in again to verify your seller account and open your listings.</p>}
      {shop.status === "APPROVED" && user?.roles?.includes("SELLER") && <div className="admin-grid">
        <form className="summary" onSubmit={e => { e.preventDefault(); action(async () => { await call(editing ? `/seller/products/${editing}` : "/seller/products", { method: editing ? "PATCH" : "POST", body: { ...form, priceRwf: Number(form.priceRwf), stockQty: Number(form.stockQty) } }); setForm(blank); setEditing(null); notify("Listing saved."); }); }}>
          <h2>{editing ? "Edit listing" : "New listing"}</h2>
          {[["sku", "SKU"], ["name", "Product name"], ["description", "Description"], ["category", "Category"], ["priceRwf", "Price (RWF)"], ["stockQty", "Stock"]].map(([key,label]) => <div className="field" key={key}><label htmlFor={`listing-${key}`}>{label}</label><input id={`listing-${key}`} type={key === "priceRwf" || key === "stockQty" ? "number" : "text"} min={key === "priceRwf" ? 1 : 0} step="1" required={key !== "description"} disabled={key === "sku" && Boolean(editing)} value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} /></div>)}
          <div className="field"><label htmlFor="listing-status">Visibility</label><select id="listing-status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}><option>DRAFT</option><option>PUBLISHED</option></select></div>
          <div className="field"><label htmlFor="listing-photo">Product photo</label><input id="listing-photo" type="file" accept="image/*" onChange={async e => { const file = e.target.files?.[0]; if (!file) return; setBusy(true); try { const photo = await compressProductPhoto(file); setForm(f => ({...f,imageUrl:photo})); } catch(error) { setError(error.message); } finally { setBusy(false); } }} /></div>
          {form.imageUrl && <img className="photo-preview" src={form.imageUrl} alt="Listing preview" />}
          <button className="btn btn-primary" disabled={busy}>Save listing</button>{editing && <button type="button" className="btn" onClick={() => { setEditing(null); setForm(blank); }}>Cancel</button>}
        </form>
        <div className="admin-stack">
          <section><h2>Your listings</h2>{products.length === 0 && <p>No listings yet.</p>}{products.map(p => <article className="order-card" key={p.id}><strong>{p.name}</strong><p>{p.sku} · {money(p.priceRwf)} · {p.stockQty} in stock · {p.status}</p><button className="btn" onClick={() => { setEditing(p.id); setForm({ ...p, category: p.categoryName || "Fashion" }); }}>Edit / restock</button></article>)}</section>
          <section><h2>Fulfil your orders</h2>{orders.length === 0 && <p>No orders yet.</p>}{orders.map(o => <article className="order-card" key={o.id}><strong>{o.orderNumber}</strong><p>{o.status} · Escrow {o.escrowState}</p><p>{o.deliveryMethod}: {o.deliveryAddress}</p><ul>{o.lines.map(l => <li key={l.id}>{l.qty} × {l.name}{l.trackingNo ? ` · ${l.trackingNo}` : ""}{l.deliveredAt ? " · Delivered" : ""}</li>)}</ul>{o.status === "PAID" && o.lines.some(l => !l.shippedAt) && <form className="qty-row" onSubmit={e => { e.preventDefault(); action(() => call(`/seller/orders/${o.id}/ship`, { method: "POST", body: { trackingNo: tracking[o.id] } })); }}><input aria-label={`Tracking for ${o.orderNumber}`} required maxLength={64} placeholder="Tracking number" value={tracking[o.id] || ""} onChange={e => setTracking({ ...tracking, [o.id]: e.target.value })} /><button className="btn btn-primary" disabled={busy}>Ship my items</button></form>}</article>)}</section>
          <section><h2>Payouts</h2><p className="muted">Sandbox payouts after the 48-hour dispute window, less the commission and payment fee recorded at checkout (defaults: 8% + 2%).</p>{payouts.length === 0 ? <p>No released payouts yet.</p> : payouts.map((p,i) => <p key={i}>{money(p.amount_rwf)} · {new Date(p.created_at).toLocaleString()}</p>)}</section>
        </div>
      </div>}
    </>}
  </div>;
}
