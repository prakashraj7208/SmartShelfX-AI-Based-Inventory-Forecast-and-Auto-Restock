// src/utils/cartUtils.ts

export interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  sku?: string;
  imageData?: string | null;
  imageType?: string | null;
  imageUrl?: string;
}

const CART_KEY = "cart_items";

export function loadCart(): CartItem[] {
  const data = localStorage.getItem(CART_KEY);
  return data ? JSON.parse(data) : [];
}

function saveCart(cart: CartItem[]) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

export function addToCart(item: CartItem) {
  const cart = loadCart();
  const existing = cart.find((c) => c.productId === item.productId);

  if (existing) {
    existing.quantity += item.quantity;
  } else {
    cart.push(item);
  }

  saveCart(cart);
}

export function removeFromCart(productId: number) {
  const cart = loadCart().filter((i) => i.productId !== productId);
  saveCart(cart);
}

export function updateQty(productId: number, qty: number) {
  const cart = loadCart();
  const item = cart.find((i) => i.productId === productId);
  if (item) {
    item.quantity = qty <= 0 ? 1 : qty;
    saveCart(cart);
  }
}

export function clearCart() {
  localStorage.removeItem(CART_KEY);
}
