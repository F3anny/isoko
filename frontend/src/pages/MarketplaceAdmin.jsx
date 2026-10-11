import { useEffect, useState } from "react";
import { money } from "../api";
import { useStore } from "../store";
export function MarketplaceAdmin() {
  const { call, refreshCatalog } = useStore();
  const [shops, setShops] = useState([]); const [orders, setOrders] = useState([]); const [ledger, setLedger] = useState([]);
  const [notes, setNotes] = useState({}); const [error, setError] = useState("");
  const [fees, setFees] = useState({ commissionPercent: 8, paymentFeePercent: 2 });
  async function load() { const results = await Promise.all([call("/admin/shops"),call("/admin/orders"),call("/admin/ledger"),call("/admin/settings")]); setShops(results[0].data); setOrders(results[1].data); setLedger(results[2].data); setFees({commissionPercent:results[3].data.commission_percent,paymentFeePercent:results[3].data.payment_fee_percent}); }
  useEffect(() => { load().catch(e => setError(e.message)); }, [call]);
  async function act(path,method,body) { try { await call(path,{method,body}); await load(); await refreshCatalog(); setError(""); } catch(e) { setError(e.message); } }
  return <section className="section">
    <h2>Marketplace operations</h2>{error && <p className="error" role="alert">{error}</p>}
    <form className="qty-row" onSubmit={e => { e.preventDefault(); act("/admin/settings","PATCH",fees); }}>
      <label htmlFor="commission-rate">Commission %</label><input id="commission-rate" type="number" min={0} max={50} value={fees.commissionPercent} onChange={e => setFees({...fees,commissionPercent:Number(e.target.value)})} />
      <label htmlFor="payment-fee-rate">Payment fee %</label><input id="payment-fee-rate" type="number" min={0} max={50} value={fees.paymentFeePercent} onChange={e => setFees({...fees,paymentFeePercent:Number(e.target.value)})} />
      <button className="btn">Save rates for new orders</button>
    </form>
    <div className="admin-grid"><section><h3>Seller approvals</h3>{shops.map(s => <article className="order-card" key={s.id}><strong>{s.name}</strong><p>{s.status} · Business reference: {s.businessReference}</p><p>{s.description}</p><input aria-label={`Review note for ${s.name}`} placeholder="Review note" value={notes[s.id] || ""} onChange={e => setNotes({...notes,[s.id]:e.target.value})} /><div className="qty-row">{["APPROVED","REJECTED","SUSPENDED"].map(status => <button className="btn" key={status} onClick={() => act(`/admin/shops/${s.id}`,"PATCH",{status,note:notes[s.id]})}>{status === "APPROVED" ? "Approve" : status === "REJECTED" ? "Reject" : "Suspend"}</button>)}</div></article>)}</section>
    <section><h3>Disputes</h3>{orders.filter(o => o.status === "DISPUTED").length === 0 && <p>No open disputes.</p>}{orders.filter(o => o.status === "DISPUTED").map(o => <article className="order-card" key={o.id}><strong>{o.orderNumber} · {money(o.totalRwf)}</strong><p>{o.disputeReason}</p>{o.disputeEvidence && <a href={o.disputeEvidence} target="_blank" rel="noreferrer">View evidence</a>}<div className="field"><label htmlFor={`resolution-${o.id}`}>Resolution note</label><input id={`resolution-${o.id}`} value={notes[o.id] || ""} onChange={e => setNotes({...notes,[o.id]:e.target.value})} /></div><div className="qty-row"><button className="btn" disabled={!notes[o.id]?.trim()} onClick={() => act(`/admin/orders/${o.id}/resolve`,"POST",{refund:true,note:notes[o.id]})}>Refund buyer</button><button className="btn" disabled={!notes[o.id]?.trim()} onClick={() => act(`/admin/orders/${o.id}/resolve`,"POST",{refund:false,note:notes[o.id]})}>Release to sellers</button></div></article>)}</section></div>
    <h3>Escrow ledger</h3><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Time</th><th>Debit</th><th>Credit</th><th>Amount</th></tr></thead><tbody>{ledger.map(l => <tr key={l.id}><td>{new Date(l.created_at).toLocaleString()}</td><td>{l.debit_account}</td><td>{l.credit_account}</td><td>{money(l.amount_rwf)}</td></tr>)}</tbody></table></div>
  </section>;
}
