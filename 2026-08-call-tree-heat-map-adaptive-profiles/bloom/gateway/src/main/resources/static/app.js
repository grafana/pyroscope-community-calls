'use strict';

const API_KEY = 'bloom-demo-key';
const PAGE_SIZE = 24;

const state = {
  page: 0,
  totalPages: 1,
  totalElements: 0,
  query: null,
  cart: loadCart(),
};

const el = (id) => document.getElementById(id);

/* ---------- api ---------- */

async function api(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: {
      'X-Api-Key': API_KEY,
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });
  if (!res.ok) {
    throw new Error(`${res.status} on ${path}`);
  }
  return res;
}

/* ---------- bloom marks: the product visual is derived from its name ---------- */

const TINTS = {
  White: ['#F1F0EA', '#C9C4B4'],
  Crimson: ['#F6E0E4', '#C2354B'],
  Golden: ['#F6EBCF', '#D2A032'],
  Violet: ['#EAE4F4', '#8371BD'],
  Blush: ['#F8E7E7', '#DE93A0'],
  Ivory: ['#F6F2E2', '#CDBF92'],
  Coral: ['#FAE5DC', '#DE7351'],
  Midnight: ['#E0E5EC', '#3E4E68'],
  Amber: ['#F6E7D2', '#CE8F3C'],
  Emerald: ['#E1EFE6', '#3E8460'],
};

const SIZES = { Mini: 0.62, Petite: 0.74, Classic: 0.86, Grand: 1.0, Deluxe: 1.1 };

const ARCHETYPES = {
  Rose: 'dense', Peony: 'dense', Camellia: 'dense', Dahlia: 'dense',
  Tulip: 'classic', Lily: 'classic', Orchid: 'classic', Magnolia: 'classic',
  Iris: 'classic', Jasmine: 'classic',
  Daisy: 'daisy', Sunflower: 'daisy',
  Lavender: 'spike',
  Fern: 'frond', Ivy: 'frond',
};

const STEM = '#4A7A5C';

function rosette(petal, count, rx, ry, center) {
  let out = '';
  for (let i = 0; i < count; i++) {
    out += `<ellipse rx="${rx}" ry="${ry}" cy="-${ry * 0.72}" fill="${petal}" fill-opacity="0.85" transform="rotate(${(360 / count) * i})"/>`;
  }
  return out + `<circle r="${center}" fill="#E9B94E"/>`;
}

function spike(petal) {
  let out = `<path d="M0,30 C 2,10 -2,-6 0,-26" stroke="${petal}" stroke-width="3" fill="none"/>`;
  for (let i = 0; i < 6; i++) {
    const y = -26 + i * 7;
    out += `<circle cx="-5" cy="${y + 2}" r="4.4" fill="${petal}" fill-opacity="0.85"/>`;
    out += `<circle cx="5" cy="${y}" r="4.4" fill="${petal}" fill-opacity="0.85"/>`;
  }
  return out;
}

function frond(petal) {
  let out = `<path d="M0,30 C 4,10 -4,-12 0,-28" stroke="${petal}" stroke-width="3" fill="none"/>`;
  for (let i = 0; i < 5; i++) {
    const y = -22 + i * 10;
    const len = 11 + i * 1.5;
    out += `<ellipse cx="-${len / 2 + 2}" cy="${y}" rx="${len / 2}" ry="3.6" fill="${petal}" fill-opacity="0.8" transform="rotate(-18 -${len / 2 + 2} ${y})"/>`;
    out += `<ellipse cx="${len / 2 + 2}" cy="${y + 5}" rx="${len / 2}" ry="3.6" fill="${petal}" fill-opacity="0.8" transform="rotate(18 ${len / 2 + 2} ${y + 5})"/>`;
  }
  return out;
}

function bloomGlyph(archetype, petal) {
  if (archetype === 'dense') return rosette(petal, 9, 11, 20, 7);
  if (archetype === 'classic') return rosette(petal, 6, 13, 21, 6.5);
  if (archetype === 'daisy') return rosette(petal, 13, 5.5, 21, 9);
  if (archetype === 'spike') return spike(petal);
  return frond(petal);
}

function bloomAt(archetype, petal, scale, x, y, rot = 0) {
  return `<g transform="translate(${x} ${y}) rotate(${rot}) scale(${scale})">${bloomGlyph(archetype, petal)}</g>`;
}

/* Each form arranges the flower glyph the way a florist would sell it. */
function composeForm(form, archetype, petal) {
  const upright = archetype === 'spike' || archetype === 'frond';
  switch (form) {
    case 'Stem':
      return upright
        ? bloomAt(archetype, petal, 1.05, 0, 2)
        : `<path d="M0,40 C 1,24 -1,10 0,-2" stroke="${STEM}" stroke-width="2.5" fill="none"/>`
          + `<ellipse cx="6" cy="20" rx="7" ry="3" fill="${STEM}" fill-opacity="0.7" transform="rotate(-32 6 20)"/>`
          + bloomAt(archetype, petal, 0.62, 0, -14);
    case 'Bouquet':
      return `<path d="M-16,-8 L0,32 M16,-6 L0,32 M0,-4 L0,34" stroke="${STEM}" stroke-width="2" fill="none"/>`
        + `<path d="M-15,18 L0,44 L15,18 Z" fill="#FFFFFF" fill-opacity="0.55" stroke="${STEM}" stroke-width="1.6"/>`
        + bloomAt(archetype, petal, 0.5, -19, -14, -16)
        + bloomAt(archetype, petal, 0.5, 19, -12, 14)
        + bloomAt(archetype, petal, 0.66, 0, -20);
    case 'Bundle':
      return `<path d="M-9,-4 L-3,28 M9,-6 L3,28" stroke="${STEM}" stroke-width="2" fill="none"/>`
        + `<ellipse cx="0" cy="28" rx="8" ry="3.6" fill="none" stroke="${STEM}" stroke-width="2.2"/>`
        + bloomAt(archetype, petal, 0.58, -14, -14, -12)
        + bloomAt(archetype, petal, 0.58, 14, -16, 12);
    case 'Basket':
      return bloomAt(archetype, petal, 0.55, -13, -16, -10)
        + bloomAt(archetype, petal, 0.55, 13, -18, 10)
        + `<path d="M-13,-2 A 14 15 0 0 1 13,-2" stroke="${STEM}" stroke-width="2.2" fill="none"/>`
        + `<path d="M-25,4 L-19,32 L19,32 L25,4 Z" fill="#FFFFFF" fill-opacity="0.5" stroke="${STEM}" stroke-width="2.2"/>`;
    case 'Wreath': {
      let out = `<circle r="25" fill="none" stroke="${STEM}" stroke-width="2.5" stroke-dasharray="2 6"/>`;
      for (let i = 0; i < 7; i++) {
        const angle = (Math.PI * 2 * i) / 7 - Math.PI / 2;
        out += bloomAt(archetype, petal, 0.3, 25 * Math.cos(angle), 25 * Math.sin(angle), (angle * 180) / Math.PI + 90);
      }
      return out;
    }
    case 'Pot':
      return bloomAt(archetype, petal, 0.62, 0, -16)
        + `<rect x="-17" y="8" width="34" height="6" rx="2" fill="#FFFFFF" fill-opacity="0.5" stroke="${STEM}" stroke-width="2.2"/>`
        + `<path d="M-14,14 L-10,36 L10,36 L14,14 Z" fill="#FFFFFF" fill-opacity="0.5" stroke="${STEM}" stroke-width="2.2"/>`;
    case 'Box':
      return bloomAt(archetype, petal, 0.6, 0, -12)
        + `<rect x="-24" y="10" width="48" height="24" rx="3" fill="#FFFFFF" fill-opacity="0.55" stroke="${STEM}" stroke-width="2.2"/>`
        + `<line x1="-24" y1="18" x2="24" y2="18" stroke="${STEM}" stroke-width="1.6"/>`;
    case 'Garland': {
      let out = `<path d="M-40,12 C -20,-16 20,-16 40,12" stroke="${STEM}" stroke-width="2.2" fill="none"/>`;
      const spots = [[-32, 2, -28], [-11, -9, -10], [11, -9, 10], [32, 2, 28]];
      for (const [x, y, rot] of spots) {
        out += bloomAt(archetype, petal, 0.32, x, y, rot);
      }
      return out;
    }
    default:
      return bloomAt(archetype, petal, 0.95, 0, 0);
  }
}

function bloomMark(name, category) {
  const words = (name || '').split(' ');
  const [tint, petal] = TINTS[words[1]] || ['#E9EEE7', STEM];
  const scale = SIZES[words[0]] || 0.86;
  const archetype = ARCHETYPES[category] || 'classic';
  const form = words[words.length - 1];

  return {
    tint,
    svg: `<svg viewBox="-50 -50 100 100" aria-hidden="true"><g transform="scale(${scale})">${composeForm(form, archetype, petal)}</g></svg>`,
  };
}

/* ---------- catalog ---------- */

function money(cents) {
  return '$' + (cents / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function petals(rating) {
  const filled = Math.round(rating);
  return '✿'.repeat(filled) + '<span style="opacity:.25">' + '✿'.repeat(5 - filled) + '</span>';
}

function ratingLine(p) {
  if (!p.reviewCount) return '<span class="card-rating">No reviews yet</span>';
  return `<span class="card-rating"><span class="petals">${petals(p.rating)}</span> ${p.rating.toFixed(1)} (${p.reviewCount})</span>`;
}

function productCard(p) {
  const mark = bloomMark(p.name, p.category);
  return `
    <article class="card">
      <button class="card-mark" style="background:${mark.tint}" data-detail="${p.id}"
              aria-label="View ${p.name}">${mark.svg}</button>
      <span class="card-eyebrow">${p.category} · ${p.sku}</span>
      <button class="card-name" data-detail="${p.id}">${p.name}</button>
      ${ratingLine(p)}
      <div class="card-foot">
        <span class="price">${money(p.priceCents)}</span>
        <button class="add" data-add="${p.id}" data-name="${p.name}" data-price="${p.priceCents}">Add</button>
      </div>
    </article>`;
}

function renderProducts(products) {
  const grid = el('grid');
  if (!products.length) {
    grid.innerHTML = `<p class="grid-empty">No stems match “${escapeHtml(state.query)}”. Try a flower or a color.</p>`;
    return;
  }
  grid.innerHTML = products.map(productCard).join('');
}

async function loadPage(page) {
  const res = await api(`/shop/products?page=${page}&size=${PAGE_SIZE}`);
  const data = await res.json();
  state.page = data.page.number;
  state.totalPages = data.page.totalPages;
  state.totalElements = data.page.totalElements;
  state.query = null;

  renderProducts(data.content);
  el('result-note').textContent =
    `${state.totalElements.toLocaleString('en-US')} arrangements in the catalog`;
  el('pager').hidden = false;
  el('page-note').textContent = `page ${state.page + 1} of ${state.totalPages}`;
  el('prev-page').disabled = state.page === 0;
  el('next-page').disabled = state.page >= state.totalPages - 1;
}

async function runSearch(query) {
  const res = await api(`/shop/search?q=${encodeURIComponent(query)}`);
  const products = await res.json();
  state.query = query;
  renderProducts(products);
  el('result-note').textContent = `top matches for “${query}” — ${products.length} shown`;
  el('pager').hidden = true;
}

/* ---------- detail ---------- */

async function openDetail(id) {
  const res = await api(`/shop/products/${id}`);
  const p = await res.json();
  const mark = bloomMark(p.name, p.category);
  const reviews = (p.reviews || []).slice(0, 6).map((r) => `
      <div class="review">
        <span class="petals">${petals(r.rating)}</span>
        <p>${escapeHtml(r.comment)}</p>
      </div>`).join('');

  el('detail-card').innerHTML = `
    <div class="drawer-head" style="padding:0 0 6px;border:0">
      <span class="detail-sku">${p.sku}</span>
      <button class="close" data-close aria-label="Close details">×</button>
    </div>
    <div class="detail-head">
      <div class="detail-mark" style="background:${mark.tint}">${mark.svg}</div>
      <div>
        <span class="card-eyebrow">${p.category}</span>
        <h2 class="detail-name">${p.name}</h2>
        ${p.reviewCount === 0 ? ratingLine(p) : ratingLine({ ...p, reviewCount: (p.reviews || []).length })}
      </div>
    </div>
    <p class="detail-desc">${escapeHtml(p.description || '')}</p>
    <div class="detail-buy">
      <span class="price" style="font-size:18px">${money(p.priceCents)}</span>
      <button class="add" data-add="${p.id}" data-name="${p.name}" data-price="${p.priceCents}">Add to basket</button>
    </div>
    ${reviews ? `<h3 class="reviews-title">What people say</h3>${reviews}` : ''}`;

  el('overlay').hidden = false;
  el('detail-modal').hidden = false;
  el('detail-card').querySelector('[data-close]').focus();
}

/* ---------- basket ---------- */

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem('bloom-cart') || '[]');
  } catch {
    return [];
  }
}

function saveCart() {
  localStorage.setItem('bloom-cart', JSON.stringify(state.cart));
  const count = state.cart.reduce((n, line) => n + line.quantity, 0);
  el('cart-count').textContent = count;
  el('cart-count').hidden = count === 0;
}

function addToCart(id, name, priceCents) {
  const line = state.cart.find((l) => l.productId === id);
  if (line) line.quantity += 1;
  else state.cart.push({ productId: id, name, priceCents, quantity: 1 });
  saveCart();
  toast(`Added ${name}`);
  if (!el('cart-drawer').hidden) renderCart();
}

function changeQty(id, delta) {
  const line = state.cart.find((l) => l.productId === id);
  if (!line) return;
  line.quantity += delta;
  if (line.quantity <= 0) state.cart = state.cart.filter((l) => l.productId !== id);
  saveCart();
  renderCart();
}

function renderCart() {
  const body = el('cart-body');
  if (!state.cart.length) {
    body.innerHTML = '<p class="cart-empty">Your basket is empty. Add a few stems.</p>';
    return;
  }
  const subtotal = state.cart.reduce((n, l) => n + l.priceCents * l.quantity, 0);
  body.innerHTML = `
    ${state.cart.map((l) => `
      <div class="cart-line">
        <span class="cart-line-name">${l.name}</span>
        <span class="qty">
          <button data-qty="-1" data-id="${l.productId}" aria-label="One less">−</button>
          ${l.quantity}
          <button data-qty="1" data-id="${l.productId}" aria-label="One more">+</button>
        </span>
        <span class="price">${money(l.priceCents * l.quantity)}</span>
      </div>`).join('')}
    <div class="cart-summary"><span>Subtotal</span><span>${money(subtotal)}</span></div>
    <p class="cart-summary" style="color:var(--ink-soft)">
      <span>Promotions</span><span>applied at checkout</span>
    </p>
    <div class="tier-row">
      <label for="tier">Member tier</label>
      <select id="tier">
        <option value="STANDARD">Standard</option>
        <option value="SILVER">Silver</option>
        <option value="GOLD">Gold</option>
      </select>
    </div>
    <button class="place-order" id="place-order">Place order</button>`;
}

async function placeOrder() {
  const button = el('place-order');
  button.disabled = true;
  button.textContent = 'Arranging…';
  try {
    const res = await api('/shop/checkout', {
      method: 'POST',
      body: JSON.stringify({
        customerId: customerId(),
        customerTier: el('tier').value,
        items: state.cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
      }),
    });
    const order = await res.json();
    const receipt = await (await api(`/shop/orders/${order.orderId}/receipt`)).text();

    state.cart = [];
    saveCart();
    el('cart-body').innerHTML = `
      <p class="order-confirmed">Order #${order.orderId} confirmed</p>
      <p class="order-savings">${order.discountCents > 0
        ? `You saved ${money(order.discountCents)} on ${money(order.subtotalCents)}.`
        : 'No promotions matched this order.'}</p>
      ${order.appliedPromotions?.length
        ? `<div class="promo-tags">${order.appliedPromotions.map((c) => `<span class="promo-tag">${c}</span>`).join('')}</div>`
        : ''}
      <pre class="ticket">${escapeHtml(receipt)}</pre>
      <button class="keep-shopping" data-close>Keep shopping</button>`;
  } catch (err) {
    button.disabled = false;
    button.textContent = 'Place order';
    toast("Couldn't place the order. Try again.");
  }
}

function customerId() {
  let id = localStorage.getItem('bloom-customer');
  if (!id) {
    id = 'web-' + Math.random().toString(36).slice(2, 8);
    localStorage.setItem('bloom-customer', id);
  }
  return id;
}

/* ---------- chrome ---------- */

function openCart() {
  renderCart();
  el('overlay').hidden = false;
  el('cart-drawer').hidden = false;
  el('cart-close').focus();
}

function closePanels() {
  el('overlay').hidden = true;
  el('cart-drawer').hidden = true;
  el('detail-modal').hidden = true;
}

let toastTimer;
function toast(message) {
  const node = el('toast');
  node.textContent = message;
  node.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { node.hidden = true; }, 2200);
}

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

/* ---------- events ---------- */

document.addEventListener('click', (event) => {
  const target = event.target.closest('button');
  if (!target) return;
  if (target.dataset.add) {
    addToCart(Number(target.dataset.add), target.dataset.name, Number(target.dataset.price));
  } else if (target.dataset.detail) {
    openDetail(target.dataset.detail).catch(() => toast("Couldn't load that arrangement."));
  } else if (target.dataset.qty) {
    changeQty(Number(target.dataset.id), Number(target.dataset.qty));
  } else if (target.id === 'place-order') {
    placeOrder();
  } else if (target.hasAttribute('data-close')) {
    closePanels();
  }
});

el('cart-button').addEventListener('click', openCart);
el('cart-close').addEventListener('click', closePanels);
el('overlay').addEventListener('click', closePanels);
el('detail-modal').addEventListener('click', (event) => {
  if (event.target === el('detail-modal')) closePanels();
});
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closePanels();
});

el('search-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const query = el('search-input').value.trim();
  if (query) {
    runSearch(query).catch(() => toast("Couldn't reach the shop. Try again."));
  } else {
    loadPage(0).catch(() => toast("Couldn't reach the shop. Try again."));
  }
});

el('prev-page').addEventListener('click', () => loadPage(state.page - 1));
el('next-page').addEventListener('click', () => loadPage(state.page + 1));

/* ---------- boot ---------- */

saveCart();
loadPage(0).catch(() => {
  el('result-note').textContent = "Couldn't reach the shop.";
  toast("Couldn't reach the shop. Is the stack running?");
});
