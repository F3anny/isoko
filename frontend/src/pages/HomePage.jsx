import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function HomePage() {
  const { products, categories, query, refreshCatalog, addToCart, notify } = useStore();
  const [categoryId, setCategoryId] = useState("");

  useEffect(() => {
    const t = setTimeout(() => refreshCatalog(query).catch(() => {}), 180);
    return () => clearTimeout(t);
  }, [query, refreshCatalog]);

  const visible = useMemo(() => {
    const filtered = categoryId
      ? products.filter((p) => p.categoryId === categoryId)
      : products;
    return [...filtered].sort((a, b) => Number(b.stockBadge === "IN_STOCK") - Number(a.stockBadge === "IN_STOCK"));
  }, [products, categoryId]);

  return (
    <>
      <section className="hero">
        <div className="wrap">
          <p className="eyebrow">Rwanda · integer RWF</p>
          <h1>Shop Isoko</h1>
          <p>Discover local shops across Rwanda. Pay with mobile money, track each seller’s delivery, and shop with escrow protection.</p>
          <div className="hero-actions">
            <a className="btn btn-on-hero" href="#catalogue">Browse catalogue</a>
            <Link className="btn btn-ghost-hero" to="/cart">View cart</Link>
          </div>
        </div>
      </section>
      <section className="benefits">
        <div className="wrap benefits-grid">
          <div>
            <strong>Free shipping</strong>
            <p>Gold members, or baskets of 50,000 RWF and above.</p>
          </div>
          <div>
            <strong>Bulk discount</strong>
            <p>10% off your basket when you buy five or more items.</p>
          </div>
          <div>
            <strong>Pay locally</strong>
            <p>MTN MoMo, Airtel Money or card, with prices in whole francs.</p>
          </div>
          <div>
            <strong>Saved lists</strong>
            <p>Wishlist stays with your account across sessions.</p>
          </div>
        </div>
      </section>
      <section className="section" id="catalogue">
        <div className="wrap">
          <div className="section-head">
            <h2>Catalogue</h2>
            <span className="muted">{visible.length} published items</span>
          </div>
          <div className="chips">
            <button type="button" className={`chip ${categoryId === "" ? "active" : ""}`} onClick={() => setCategoryId("")}>
              All
            </button>
            {categories.map((c) => (
              <button
                key={c.id}
                type="button"
                className={`chip ${categoryId === c.id ? "active" : ""}`}
                onClick={() => setCategoryId(c.id)}
              >
                {c.name}
              </button>
            ))}
          </div>
          {visible.length === 0 ? <div className="empty">No products match that search.</div> : null}
          <div className="product-grid">
            {visible.map((p) => {
              const inStock = p.stockBadge === "IN_STOCK";
              return (
                <article key={p.id} className="product-card">
                  <Link to={`/product/${p.id}`}>
                    <Media name={p.name} sku={p.sku} imageUrl={p.imageUrl} />
                    <div className="product-body">
                      <div className="sku">{p.sku}</div>
                      <h3>{p.name}</h3>
                      <p className="muted">{p.shopName}</p>
                      <div className="price">{money(p.priceRwf)}</div>
                      <div className={`stock ${inStock ? "in" : "out"}`}>
                        {inStock ? `${p.stockQty} in stock` : "Out of stock"}
                      </div>
                    </div>
                  </Link>
                  <div className="product-body" style={{ paddingTop: 0 }}>
                    <button
                      className="btn btn-primary btn-block"
                      disabled={!inStock}
                      type="button"
                      onClick={async () => {
                        try {
                          await addToCart(p.id, 1);
                        } catch (e) {
                          notify(e.message);
                        }
                      }}
                    >
                      Add to cart
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      </section>
    </>
  );
}
