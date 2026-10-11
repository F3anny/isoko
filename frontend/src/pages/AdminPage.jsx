import { useEffect, useMemo, useState } from "react";
import { money } from "../api";
import { compressProductPhoto } from "../image.js";
import { useStore } from "../store";
import { MarketplaceAdmin } from "./MarketplaceAdmin";

const NEW_CATEGORY = "__new__";
const STUDIO_PHONES = [
  { label: "Silver phone", url: "/products/phone-01.jpg" },
  { label: "Graphite phone", url: "/products/phone-02.jpg" },
  { label: "Sky phone", url: "/products/phone-03.jpg" },
  { label: "Champagne phone", url: "/products/phone-04.jpg" },
];

const emptyProduct = {
  sku: "",
  name: "",
  description: "",
  priceRwf: 15000,
  stockQty: 10,
  status: "PUBLISHED",
  category: "Cables",
  imageUrl: "",
};

export function AdminPage() {
  const { call, notify, categories, refreshCatalog } = useStore();
  const [orders, setOrders] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [form, setForm] = useState(emptyProduct);
  const [editingId, setEditingId] = useState(null);
  const [categoryChoice, setCategoryChoice] = useState("");
  const [newCategory, setNewCategory] = useState("");
  const [categoryDrafts, setCategoryDrafts] = useState({});
  const [freshCategory, setFreshCategory] = useState("");
  const [stockDrafts, setStockDrafts] = useState({});
  const [tracking, setTracking] = useState("CS-TRACK-001");
  const [error, setError] = useState("");
  const [compressing, setCompressing] = useState(false);

  async function loadOrders() {
    const { data } = await call("/admin/orders");
    setOrders(data);
  }

  async function loadCatalog() {
    const { data } = await call("/admin/products");
    setCatalog(data);
    setStockDrafts(Object.fromEntries(data.map((p) => [p.id, p.stockQty])));
  }

  async function reload() {
    await Promise.all([loadCatalog(), refreshCatalog(), loadOrders()]);
  }

  useEffect(() => {
    reload().catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (editingId || categoryChoice || !categories.length) return;
    const phones = categories.find((c) => c.name.toLowerCase() === "phones");
    setCategoryChoice((phones ?? categories[0]).name);
  }, [categories, categoryChoice, editingId]);

  const counts = useMemo(() => {
    const next = {};
    for (const p of catalog) {
      const key = p.categoryId || "none";
      next[key] = (next[key] || 0) + 1;
    }
    return next;
  }, [catalog]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function startEdit(p) {
    setEditingId(p.id);
    setForm({
      sku: p.sku,
      name: p.name,
      description: p.description || "",
      priceRwf: p.priceRwf,
      stockQty: p.stockQty,
      status: p.status,
      category: p.categoryName || "",
      imageUrl: p.imageUrl || "",
    });
    setCategoryChoice(p.categoryName || NEW_CATEGORY);
    setNewCategory("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyProduct);
    setNewCategory("");
    setCategoryChoice(categories[0]?.name || "");
  }

  async function onPhoto(e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setError("");
    setCompressing(true);
    try {
      const imageUrl = await compressProductPhoto(file);
      set("imageUrl", imageUrl);
      notify("Photo compressed to a light studio JPEG.");
    } catch (err) {
      setError(err.message);
    } finally {
      setCompressing(false);
    }
  }

  async function saveProduct(e) {
    e.preventDefault();
    setError("");
    const category = categoryChoice === NEW_CATEGORY ? newCategory.trim() : categoryChoice;
    if (!category) {
      setError("Choose a category or add a new one.");
      return;
    }
    const body = {
      ...form,
      category,
      imageUrl: form.imageUrl || null,
      priceRwf: Number(form.priceRwf),
      stockQty: Number(form.stockQty),
    };
    try {
      if (editingId) {
        await call(`/admin/products/${editingId}`, { method: "PATCH", body });
        notify("Product updated.");
      } else {
        await call("/admin/products", { method: "POST", body });
        notify("Product created.");
      }
      cancelEdit();
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function saveStock(p) {
    setError("");
    try {
      await call(`/admin/products/${p.id}/stock`, {
        method: "PATCH",
        body: { stockQty: Number(stockDrafts[p.id] ?? p.stockQty) },
      });
      notify(`Stock saved for ${p.sku}.`);
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function saveCategory(c) {
    const name = (categoryDrafts[c.id] ?? c.name).trim();
    if (!name || name === c.name) return;
    setError("");
    try {
      await call(`/admin/categories/${c.id}`, { method: "PATCH", body: { name } });
      notify("Category renamed.");
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function addCategory(e) {
    e.preventDefault();
    const name = freshCategory.trim();
    if (!name) return;
    setError("");
    try {
      await call("/admin/categories", { method: "POST", body: { name } });
      notify("Category added.");
      setFreshCategory("");
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="wrap">
      <h1 className="page-title">Admin</h1>
      <MarketplaceAdmin />
      {error ? <p className="error">{error}</p> : null}
      <div className="admin-grid">
        <form className="summary" onSubmit={saveProduct}>
          <h2>{editingId ? "Edit product" : "New product"}</h2>
          <div className="field">
            <label>SKU</label>
            <input value={form.sku} onChange={(e) => set("sku", e.target.value)} required disabled={Boolean(editingId)} />
          </div>
          <div className="field"><label>Name</label><input value={form.name} onChange={(e) => set("name", e.target.value)} required /></div>
          <div className="field"><label>Price (RWF)</label><input type="number" min="0" value={form.priceRwf} onChange={(e) => set("priceRwf", e.target.value)} /></div>
          <div className="field"><label>Stock</label><input type="number" min="0" value={form.stockQty} onChange={(e) => set("stockQty", e.target.value)} /></div>
          <div className="field">
            <label>Status</label>
            <select value={form.status} onChange={(e) => set("status", e.target.value)}>
              <option>PUBLISHED</option>
              <option>DRAFT</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="admin-category">Category</label>
            <select id="admin-category" value={categoryChoice} onChange={(e) => setCategoryChoice(e.target.value)}>
              {categories.map((c) => (
                <option key={c.id} value={c.name}>{c.name}</option>
              ))}
              <option value={NEW_CATEGORY}>Add new category…</option>
            </select>
          </div>
          {categoryChoice === NEW_CATEGORY ? (
            <div className="field">
              <label htmlFor="admin-new-category">New category</label>
              <input id="admin-new-category" value={newCategory} onChange={(e) => setNewCategory(e.target.value)} placeholder="e.g. Tablets" required />
            </div>
          ) : null}
          <div className="field">
            <label htmlFor="admin-studio">Studio phone photo</label>
            <select
              id="admin-studio"
              value={STUDIO_PHONES.some((p) => p.url === form.imageUrl) ? form.imageUrl : ""}
              onChange={(e) => set("imageUrl", e.target.value)}
            >
              <option value="">Upload your own…</option>
              {STUDIO_PHONES.map((p) => (
                <option key={p.url} value={p.url}>{p.label}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="admin-photo">Upload photo</label>
            <input id="admin-photo" type="file" accept="image/*" onChange={onPhoto} />
            <span className="photo-hint">Resized to 720px, lightened, and saved as a small JPEG.</span>
          </div>
          {form.imageUrl ? <img className="photo-preview" src={form.imageUrl} alt="Product preview" /> : null}
          {compressing ? <p className="muted">Optimising photo…</p> : null}
          <div className="field"><label>Description</label><textarea rows="3" value={form.description} onChange={(e) => set("description", e.target.value)} /></div>
          <button className="btn btn-primary btn-block" type="submit" disabled={compressing}>
            {editingId ? "Save changes" : "Publish product"}
          </button>
          {editingId ? (
            <button className="btn btn-ghost btn-block" type="button" onClick={cancelEdit} style={{ marginTop: 8 }}>
              Cancel edit
            </button>
          ) : null}
        </form>

        <div className="admin-stack">
          <section>
            <h2>Catalogue</h2>
            <p className="muted">Update stock inline, or open a row to edit name, category, and photo.</p>
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Stock</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {catalog.map((p) => (
                    <tr key={p.id}>
                      <td className="sku">{p.sku}</td>
                      <td>{p.name}</td>
                      <td>{p.categoryName || "—"}</td>
                      <td>
                        <div className="stock-edit">
                          <input
                            type="number"
                            min="0"
                            aria-label={`Stock for ${p.sku}`}
                            value={stockDrafts[p.id] ?? p.stockQty}
                            onChange={(e) => setStockDrafts((s) => ({ ...s, [p.id]: e.target.value }))}
                          />
                          <button className="btn" type="button" onClick={() => saveStock(p)}>Save</button>
                        </div>
                      </td>
                      <td><span className={`status-pill ${p.status.toLowerCase()}`}>{p.status}</span></td>
                      <td><button className="btn btn-ghost" type="button" onClick={() => startEdit(p)}>Edit</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section>
            <h2>Categories</h2>
            {categories.map((c) => (
              <div className="category-row" key={c.id}>
                <input
                  value={categoryDrafts[c.id] ?? c.name}
                  onChange={(e) => setCategoryDrafts((d) => ({ ...d, [c.id]: e.target.value }))}
                  aria-label={`Rename ${c.name}`}
                />
                <span className="muted">{counts[c.id] || 0} products</span>
                <button className="btn" type="button" onClick={() => saveCategory(c)}>Rename</button>
              </div>
            ))}
            <form className="category-row" onSubmit={addCategory}>
              <input value={freshCategory} onChange={(e) => setFreshCategory(e.target.value)} placeholder="New category name" />
              <button className="btn btn-primary" type="submit">Add category</button>
            </form>
          </section>

          <section>
            <h2>Fulfilment</h2>
            {orders.map((o) => (
              <article className="order-card" key={o.id}>
                <strong>{o.orderNumber}</strong> · <span className={`status-pill ${o.status.toLowerCase()}`}>{o.status.replaceAll("_", " ")}</span> · {money(o.totalRwf)}
                <div className="qty-row">
                  <input value={tracking} onChange={(e) => setTracking(e.target.value)} aria-label="Tracking" />
                  <button className="btn" type="button" onClick={() => call(`/admin/orders/${o.id}/ship`, { method: "POST", body: { trackingNo: tracking } }).then(loadOrders).then(() => notify("Shipped")).catch((e) => setError(e.message))}>Ship</button>
                  <button className="btn btn-primary" type="button" onClick={() => call(`/admin/orders/${o.id}/deliver`, { method: "POST" }).then(loadOrders).then(() => notify("Delivered")).catch((e) => setError(e.message))}>Deliver</button>
                </div>
              </article>
            ))}
          </section>
        </div>
      </div>
    </div>
  );
}
