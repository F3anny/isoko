import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { money } from "../api";
import { useStore } from "../store";
export function OrdersPage() {
 const { call, notify }=useStore();const [orders,setOrders]=useState([]);const [error,setError]=useState("");
 const [otps,setOtps]=useState({});const [reasons,setReasons]=useState({});const [evidence,setEvidence]=useState({});const [busy,setBusy]=useState(false);
 async function load(){const {data}=await call("/orders");setOrders(data);}
 useEffect(()=>{load().catch(e=>setError(e.message));},[call]);
 async function act(id,action,body){setBusy(true);setError("");try{await call(`/orders/${id}/${action}`,{method:"POST",body});await load();notify("Order updated.");}catch(e){setError(e.message);}finally{setBusy(false);}}
 return <div className="wrap"><h1 className="page-title">Your orders</h1><Link to="/notifications">Open inbox for delivery codes</Link>{error && <p className="error" role="alert">{error}</p>}{orders.length===0 && <div className="empty">You have not placed an order yet.</div>}
 {orders.map(o=><article className="order-card" key={o.id}><div className="section-head"><h2 style={{fontSize:18}}>{o.orderNumber}</h2><span className={`status-pill ${o.status.toLowerCase()}`}>{o.status.replaceAll("_"," ")}</span></div>
 <p className="muted">{new Date(o.placedAt).toLocaleString()} · {money(o.totalRwf)} · Escrow {o.escrowState}</p><p>{o.deliveryMethod}: {o.deliveryAddress}</p>
 {o.lines.map(l=><section key={l.id} className="order-line"><strong>{l.shopName || "Isoko"}</strong><p>{l.qty} × {l.name} — {money(l.unitPriceRwf)} each{l.trackingNo ? ` · Tracking ${l.trackingNo}`:""}</p>{l.deliveredAt && <p className="muted">Delivered {new Date(l.deliveredAt).toLocaleString()}</p>}
 {l.shippedAt && !l.deliveredAt && <form className="qty-row" onSubmit={e=>{e.preventDefault();act(o.id,"confirm-delivery",{lineId:l.id,otp:otps[l.id]});}}><input aria-label={`Delivery OTP for ${l.name}`} placeholder="6-digit delivery code" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} required value={otps[l.id] || ""} onChange={e=>setOtps({...otps,[l.id]:e.target.value})}/><button className="btn btn-primary" disabled={busy}>Confirm received</button></form>}</section>)}
 <p className="muted">Payment {o.payment?.provider}: {o.payment?.status} {o.payment?.gatewayRef}</p>
 {["PLACED","PAYMENT_TIMED_OUT"].includes(o.status) && <button className="btn" disabled={busy} onClick={()=>act(o.id,"pay",{paymentMethod:o.payment?.provider || "MTN_MOMO"})}>Retry payment</button>}
 {["PLACED","PAYMENT_PENDING","PAYMENT_TIMED_OUT","PAID"].includes(o.status) && o.lines.every(l=>!l.shippedAt) && <button className="btn" disabled={busy} onClick={()=>{if(window.confirm("Cancel this order? Paid orders receive a sandbox refund."))act(o.id,"cancel");}}>Cancel order</button>}
 {o.status==="DELIVERED" && Date.now()<new Date(o.deliveredAt).getTime()+48*3600000 && <form onSubmit={e=>{e.preventDefault();act(o.id,"dispute",{reason:reasons[o.id],evidence:evidence[o.id] || null});}}><p>You have 48 hours after delivery to report a problem. Escrow remains held during this window.</p><div className="field"><label htmlFor={`reason-${o.id}`}>Describe the problem</label><textarea id={`reason-${o.id}`} required maxLength={2000} value={reasons[o.id] || ""} onChange={e=>setReasons({...reasons,[o.id]:e.target.value})}/></div><div className="field"><label htmlFor={`evidence-${o.id}`}>Photo evidence link (optional)</label><input id={`evidence-${o.id}`} type="url" placeholder="https://…" value={evidence[o.id] || ""} onChange={e=>setEvidence({...evidence,[o.id]:e.target.value})}/></div><button className="btn" disabled={busy}>Open dispute</button></form>}
 {o.disputeReason && <p>Dispute: {o.disputeReason}</p>}{o.resolutionNote && <p>Resolution: {o.resolutionNote}</p>}
 </article>)}
 </div>;
}
