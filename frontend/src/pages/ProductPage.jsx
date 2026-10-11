import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function ProductPage() {
  const { id } = useParams();
  const { call, addToCart, notify, user, categories } = useStore();
  const [product, setProduct] = useState(null);
  const [qty, setQty] = useState(1);
  const [error, setError] = useState("");

  useEffect(() => {
    call(`/products/${id}`)
      .then(({ data }) => setProduct(data))
      .catch((e) => setError(e.message));
  }, [id, call]);

  if (error) {
    return (
      <div className="wrap">
        <p className="empty">{error}</p>
        <Link to="/">Back to shop</Link>
      </div>
    );
  }
  if (!product) return <div className="wrap empty">Loading…</div>;

  const inStock = product.stockBadge === "IN_STOCK";
  const category = categories.find((c) => c.id === product.categoryId);

  return (
    <div className="wrap pdp">
      <Media name={product.name} sku={product.sku} imageUrl={product.imageUrl} className="pdp-media" />
      <div>
        <nav className="crumbs">
          <Link to="/">Shop</Link>
          <span>/</span>
          {category ? <span>{category.name}</span> : <span>Catalogue</span>}
          <span>/</span>
          <span>{product.name}</span>
        </nav>
        <div className="sku">{product.sku}</div>
        <h1>{product.name}</h1><p className="muted">Sold by {product.shopName}</p>
        <p className="lead">{product.description || "A Isoko catalogue item. Money is integer RWF."}</p>
        <div className="price" style={{ fontSize: 28 }}>{money(product.priceRwf)}</div>
        <p className={`stock ${inStock ? "in" : "out"}`}>
          {inStock ? `${product.stockQty} available` : "Currently out of stock"}
        </p>
        <p className="muted">Quantity 5+ gets 10% off. Gold members, or baskets ≥ 50,000 RWF, ship free.</p>
        <div className="qty-row">
          <button className="btn" type="button" onClick={() => setQty((q) => Math.max(1, Number(q) - 1))}>−</button>
          <input type="number" min="1" max={product.stockQty || 1} value={qty} onChange={(e) => setQty(e.target.value)} aria-label="Quantity" />
          <button className="btn" type="button" onClick={() => setQty((q) => Number(q) + 1)}>+</button>
        </div>
        <div className="buy-row">
          <button
            className="btn btn-primary"
            disabled={!inStock}
            type="button"
            onClick={async () => {
              try {
                await addToCart(product.id, Number(qty) || 1);
              } catch (e) {
                notify(e.message);
              }
            }}
          >
            Add to cart
          </button>
          {user ? (
            <button
              className="btn btn-ghost"
              type="button"
              onClick={async () => {
                try {
                  await call(`/wishlist/${product.id}`, { method: "POST" });
                  notify("Saved to wishlist.");
                } catch (e) {
                  notify(e.message);
                }
              }}
            >
              Save to wishlist
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}
