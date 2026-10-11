import { useEffect, useState } from "react";
import { useStore } from "../store";
export function NotificationsPage() {
  const { call } = useStore();
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  async function load() { try { const { data } = await call("/notifications"); setItems(data); setError(""); } catch(e) { setError(e.message); } }
  useEffect(() => { load(); }, [call]);
  return <div className="wrap"><h1 className="page-title">Your inbox</h1><p className="muted">Order updates, shop approval and delivery codes appear here in the lab environment.</p><button className="btn" onClick={load}>Refresh</button>{error && <p className="error">{error}</p>}{items.length === 0 && <div className="empty">No notifications yet.</div>}{items.map(item => <article className="order-card" key={item.id}><p>{item.message}</p><small className="muted">{new Date(item.created_at).toLocaleString()}</small></article>)}</div>;
}
