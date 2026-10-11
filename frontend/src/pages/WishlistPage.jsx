import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function WishlistPage() {
  const { call, addToCart, notify } = useStore();
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");

  async function load() {
    const { data } = await call("/wishlist");
    setItems(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, []);

  return (
    <div className="wrap">
      <h1 className="page-title">Wishlist</h1>
      {items.length === 0 ? <div className="empty">Nothing saved. Browse the <Link to="/">shop</Link>.</div> : null}
      <div className="product-grid">
        {items.map((w) => (
          <article className="product-card" key={w.productId}>
            <Link to={`/product/${w.productId}`}>
              <Media name={w.name} sku={w.sku} imageUrl={w.imageUrl} />
              <div className="product-body">
                <div className="sku">{w.sku}</div>
                <h3>{w.name}</h3>
                <div className="price">{money(w.priceRwf)}</div>
              </div>
            </Link>
            <div className="product-body" style={{ paddingTop: 0, gap: 8 }}>
              <button className="btn btn-primary" type="button" onClick={() => addToCart(w.productId, 1).catch((e) => notify(e.message))}>
                Add to cart
              </button>
              <button
                className="btn btn-ghost"
                type="button"
                onClick={async () => {
                  const { data } = await call(`/wishlist/${w.productId}`, { method: "DELETE" });
                  setItems(data);
                }}
              >
                Remove
              </button>
            </div>
          </article>
        ))}
      </div>
      {error ? <p className="error">{error}</p> : null}
    </div>
  );
}
