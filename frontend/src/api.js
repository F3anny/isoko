const TOKEN_KEY = "cs.token";
const CART_KEY = "cs.cartToken";
const THEME_KEY = "cs.theme";

export function money(rwf) {
  return new Intl.NumberFormat("en-RW", {
    style: "currency",
    currency: "RWF",
    maximumFractionDigits: 0,
  }).format(rwf ?? 0);
}

export function initials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return ((parts[0]?.[0] ?? "C") + (parts[1]?.[0] ?? "")).toUpperCase();
}

export function hueFrom(text = "") {
  let h = 0;
  for (const ch of text) h = (h * 31 + ch.charCodeAt(0)) % 360;
  return h;
}

export function problemMessage(body, fallback) {
  if (!body) return fallback;
  return body.detail || body.title || fallback;
}

export async function api(path, { method = "GET", body, token, cartToken, headers = {} } = {}) {
  const nextHeaders = { ...headers };
  if (body !== undefined) nextHeaders["Content-Type"] = "application/json";
  if (token) nextHeaders.Authorization = `Bearer ${token}`;
  if (cartToken) nextHeaders["X-Cart-Token"] = cartToken;

  const res = await fetch(`/api/v1${path}`, {
    method,
    headers: nextHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const guest = res.headers.get("X-Cart-Token");
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  if (!res.ok) {
    const err = new Error(problemMessage(data, `Request failed (${res.status})`));
    err.status = res.status;
    err.body = data;
    throw err;
  }
  return { data, cartToken: guest };
}

export const storage = {
  token: () => localStorage.getItem(TOKEN_KEY),
  setToken: (v) => (v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY)),
  cart: () => localStorage.getItem(CART_KEY),
  setCart: (v) => (v ? localStorage.setItem(CART_KEY, v) : localStorage.removeItem(CART_KEY)),
  theme: () => localStorage.getItem(THEME_KEY),
  setTheme: (v) => (v ? localStorage.setItem(THEME_KEY, v) : localStorage.removeItem(THEME_KEY)),
};
