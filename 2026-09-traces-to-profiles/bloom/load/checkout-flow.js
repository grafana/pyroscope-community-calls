import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const DURATION = __ENV.DURATION || '20m';

const params = {
  headers: {
    'X-Api-Key': __ENV.API_KEY || 'bloom-demo-key',
    'Content-Type': 'application/json',
  },
};

const TERMS = [
  'rose', 'tulip', 'orchid', 'peony', 'lily', 'fern', 'ivy', 'daisy',
  'bouquet', 'wreath', 'basket', 'white', 'crimson', 'golden', 'violet',
  'grand', 'petite', 'garden', 'midnight', 'amber',
];

const TIERS = ['STANDARD', 'STANDARD', 'STANDARD', 'SILVER', 'SILVER', 'GOLD'];

export const options = {
  scenarios: {
    browsers: {
      executor: 'ramping-vus',
      exec: 'browse',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 8 },
        { duration: DURATION, target: 8 },
      ],
    },
    searchers: {
      executor: 'ramping-vus',
      exec: 'search',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 6 },
        { duration: DURATION, target: 6 },
      ],
    },
    buyers: {
      executor: 'constant-arrival-rate',
      exec: 'checkout',
      rate: parseInt(__ENV.CHECKOUT_RATE || '6'),
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 30,
      maxVUs: 150,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick(arr) {
  return arr[randomInt(0, arr.length - 1)];
}

export function browse() {
  const page = randomInt(0, 299);
  const res = http.get(`${BASE}/shop/products?page=${page}&size=20`, params);
  check(res, { 'browse 200': (r) => r.status === 200 });
  sleep(randomInt(1, 3));
}

export function search() {
  const res = http.get(`${BASE}/shop/search?q=${pick(TERMS)}`, params);
  check(res, { 'search 200': (r) => r.status === 200 });
  sleep(randomInt(1, 3));
}

export function checkout() {
  // Browse a page first so the cart references real product IDs.
  const page = randomInt(0, 299);
  const browseRes = http.get(`${BASE}/shop/products?page=${page}&size=20`, params);
  if (browseRes.status !== 200) {
    return;
  }

  const products = browseRes.json('content');
  if (!products || products.length === 0) {
    return;
  }

  // Most shoppers buy a few stems; ~15% fill a big cart. The big carts drive
  // the pricing discount optimizer into its expensive tail, which is what the
  // span heatmap surfaces.
  const items = [];
  const big = Math.random() < 0.15;
  const count = big ? randomInt(8, 15) : randomInt(1, 4);
  for (let i = 0; i < count; i++) {
    items.push({
      productId: pick(products).id,
      quantity: randomInt(1, 3),
    });
  }

  const body = JSON.stringify({
    customerId: `customer-${randomInt(1, 5000)}`,
    customerTier: pick(TIERS),
    items: items,
  });

  const res = http.post(`${BASE}/shop/checkout`, body, params);
  check(res, { 'checkout 200': (r) => r.status === 200 });

  // Most customers open their receipt right after ordering.
  if (res.status === 200) {
    const orderId = res.json('orderId');
    const receipt = http.get(`${BASE}/shop/orders/${orderId}/receipt`, params);
    check(receipt, { 'receipt 200': (r) => r.status === 200 });
  }
}
