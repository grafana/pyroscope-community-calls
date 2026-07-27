# Answer key — planted inefficiencies (presenter only, keep off screen)

Nine issues across four services, graded by how hard they are to find from
profiling data. "Signal" is where they show up first.

## bloom-gateway

| # | Where | What | Signal | Difficulty |
|---|-------|------|--------|------------|
| 1 | `gateway/.../ApiKeyFilter.java` (`hash`) | PBKDF2 with 25k iterations runs on **every request** to verify the same API key | CPU flamegraph dominated by `ApiKeyFilter.hash` → `SecretKeyFactory.generateSecret` | easy |
| 2 | `gateway/.../DownstreamClient.java` (every method) | `new RestTemplate()` per call — no reuse, no pooling, no timeouts | alloc profile churn (`RestTemplate.<init>`, Jackson converters); latency | medium |
| 3 | `gateway/.../ShopController.java` (`checkout`) | Sequential per-item `GET /products/{id}` calls to catalog | traces: N repeated catalog spans per checkout; wall time | medium (invisible in CPU) |

Fixes: 1) verify once and cache the result (the key is high-entropy, not a
password — a plain SHA-256 compare or constant-time equality on a cached
verification is fine); 2) one shared `RestTemplate`/`RestClient` bean with a
pooled connection manager and timeouts; 3) a batch catalog endpoint
(`GET /products?ids=`) or concurrent fetches.

## bloom-catalog

| # | Where | What | Signal | Difficulty |
|---|-------|------|--------|------------|
| 4 | `catalog/.../CatalogService.java` (`search`/`matches`) | `String.matches` recompiles the regex for every product (6000 products × 2 fields per search) and `findAll()` pulls the whole catalog into memory | CPU flamegraph: `Pattern.compile` under `search`; alloc churn from `findAll` | easy |
| 5 | `catalog/.../CatalogService.java` (`toSummary`) | N+1: one `findByProductId` review query per product (21 queries per listing page, 25 per search); no index on `review.product_id` either | traces: 21 SQL spans per request; wall/latency, barely any CPU | hard |

Fixes: 4) push search into SQL (`ILIKE`/full-text) or at minimum precompile a
pattern / use `toLowerCase().contains()`; 5) aggregate ratings in one grouped
query (or a projection join), plus an index on `review.product_id`.

## bloom-pricing

| # | Where | What | Signal | Difficulty |
|---|-------|------|--------|------------|
| 6 | `pricing/.../DiscountEngine.java` + `Promotion.java` | O(n²) pair search over eligible promotions; `discountFor` recomputed 3× per pair; `Promotion.appliesTo` does a linear `ArrayList.contains` over 400 SKUs; BigDecimal division churn | CPU flamegraph: `DiscountEngine.quote` → `discountFor` → `ArrayList.contains`/`String.equals` + `BigDecimal.divide` | easy–medium |

Fixes: `Set<String>` for promotion SKUs, memoize `discountFor` per promotion
(it only depends on promotion + items), hoist the first promotion's discount
out of the inner loop, filter to stackables before pairing.

Bonus signal: `QuoteController` wraps quotes in dynamic labels
(`customer_tier`, `cart_size`). Grouping pricing CPU by `cart_size` shows the
superlinear cost growth — corroborates the O(n²) diagnosis without reading code.

## bloom-orders

| # | Where | What | Signal | Difficulty |
|---|-------|------|--------|------------|
| 7 | `orders/.../ReceiptRenderer.java` | Receipt built with `String +=` in loops — O(n²) copying | alloc profile: `StringBuilder.toString`/`Arrays.copyOf` under `render` | medium |
| 8 | `orders/.../OrderService.java` (`RECEIPTS`) | Unbounded static receipt cache — every order ever placed stays on the heap (`-Xmx512m` set in compose to make pressure visible) | live/heap growth over time; retained strings from `render` | hard |
| 9 | `orders/.../AuditTrail.java` (`record`) | `synchronized` method doing timestamp formatting, a file write **and an fsync** (`channel.force`) inside the lock, called on every order and receipt view | lock profile (`mutex:delay`): contention on `AuditTrail.record`; wall time waits | hard |

Fixes: 7) `StringBuilder` (or a template); 8) bounded cache (Caffeine
`maximumSize`) or just drop it — rendering is cheap once #7 is fixed; 9) format
outside the lock and hand the line to an async/buffered appender (or use a
logging framework).

## Expected "top CPU" leaderboard under load

`gcx profiles metrics '{}' --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m --top`
should rank roughly: gateway (PBKDF2) ≥ pricing (discount engine) > catalog
(regex search) > orders. The N+1 (#5) and the lock (#9) are intentionally
invisible there — that's the segue to traces, span profiles, and lock/alloc
profile types.
