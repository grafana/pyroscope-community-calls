# Session runbook — 2026-07-30 community call (~60 min)

Pre-flight (before going live): stack running (`make up`), load running for ≥60
min (`LOAD_DURATION=120m make load`), gcx context `bloom-demo` active, Grafana
tab with Profiles Drilldown open, terminal font large, `notes/` closed.

**Agent isolation**: the agent must not be able to reach `notes/` (the answer
key). Run the agent segment from a copy of `bloom/` outside this repo — the
copy is what gets screen-shared:

```sh
rsync -a --exclude target bloom/ ~/demo/bloom/
cd ~/demo/bloom
```

Run the stack (`make up`, `make load`) from that copy too, so the agent's
fix-rebuild-verify loop works in place. Never launch Claude Code from
`2026-07-gcx/` itself.

**Secrets on stream**: `.env` in the copy holds the write-only cloud token; if
the agent ever `cat`s it, that's recoverable — the token is scoped to
profiles/traces write on the demo stack. Rotate it after the call regardless.

## 0. Intro (5 min)

- What's new in Pyroscope since last call.
- Introduce gcx: one CLI for querying Grafana Cloud observability data —
  including profiles — designed for both humans and AI agents.

## 1. Meet Bloom (5 min)

- Open the storefront at http://localhost:8080 — browse, search "midnight",
  add a couple of stems, check out, show the printed receipt with the applied
  promotions. This is the app we'll be profiling.
- Show `bloom/README.md` architecture diagram; four Spring Boot services behind
  the storefront, k6 load running in the background.
- Quick Grafana peek: Profiles Drilldown, the four `bloom-*` services. Framing:
  "the UI is great — but let's live in the terminal today."

## 2. gcx profiles, manually (20 min)

Discovery — what is there?

```sh
gcx profiles list-profile-types --since 30m
gcx profiles labels --since 30m
gcx profiles labels --label service_name --since 30m
```

Which service burns the most CPU? (leaderboard)

```sh
gcx profiles metrics '{}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m --top
```

Expected: gateway and pricing on top. Drill into the gateway flamegraph:

```sh
gcx profiles query '{service_name="bloom-gateway"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m
gcx profiles query '{service_name="bloom-gateway"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m -o graph
```

Reveal #1 live: ~half of gateway CPU is `ApiKeyFilter.hash` →
`SecretKeyFactory.generateSecret` — PBKDF2 on every request. Show the code.

Pricing next:

```sh
gcx profiles query '{service_name="bloom-pricing"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m
```

`Promotion.appliesTo` (a linear `ArrayList.contains`) under
`DiscountEngine.quote` — the O(n²) promotion pairing. Show the code briefly.

Dynamic code-level labels — pricing wraps each quote in
`customer_tier` / `cart_size` labels (show the try-with-resources in
`QuoteController`, ~3 lines):

```sh
gcx profiles labels --since 30m        # customer_tier and cart_size appear
gcx profiles metrics '{service_name="bloom-pricing"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds \
  --since 30m --top --group-by cart_size
gcx profiles query '{service_name="bloom-pricing", cart_size="4"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m
```

The `cart_size` leaderboard is the punchline: CPU per cart size grows much
faster than linearly — a 4-item cart costs far more than 4× a 1-item cart,
which is the O(n²) pairing showing up in a business dimension. Mention label
cardinality discipline: tiers (3) and cart sizes (5) are bounded by design.

Beyond CPU — other profile types on the orders service:

```sh
# allocation churn: receipt rendering
gcx profiles query '{service_name="bloom-orders"}' \
  --profile-type memory:alloc_in_new_tlab_bytes:bytes:space:bytes --since 30m

# lock contention: the audit trail
gcx profiles query '{service_name="bloom-orders"}' \
  --profile-type mutex:delay:nanoseconds:mutex:count --since 30m

# live objects: what stays on the heap (the receipt cache)
gcx profiles query '{service_name="bloom-orders"}' \
  --profile-type memory:live:count:objects:count --since 30m
```

Span profiles — connect traces and profiles:

```sh
gcx profiles exemplars span '{service_name="bloom-gateway"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m
# then take one span/trace ID:
gcx profiles query '{service_name="bloom-gateway"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 30m \
  --span-id <ID>
```

Mention: `-o pprof` exports for `go tool pprof`/speedscope; `--stacktrace-selector`
scopes a flamegraph to one call site; `-o json --jq` for scripting.

Traces from the same CLI — the N+1 that CPU profiles can't show
(note: this OTel agent version uses `span.db.system`, not `db.system.name`):

```sh
gcx traces query '{ resource.service.name = "bloom-catalog" && span.db.system = "postgresql" } | count() > 15' --since 30m
```

Every catalog listing/search trace carries 15–25 PostgreSQL spans — one review
query per product.

Segue: "the CPU leaderboard doesn't explain why catalog listing latency is
bad — nothing in its CPU profile stands out for `/products`. The traces do.
Correlating traces, multiple profile types, and code is a lot of manual
cross-referencing… which is exactly what agents are good at."

## 3. Agent segment (20–25 min)

Open Claude Code in the isolated copy (`~/demo/bloom`, fresh session,
screen-shared). Prompt 1 (investigation):

> Our Bloom shop (services bloom-gateway, bloom-catalog, bloom-pricing,
> bloom-orders) has high latency and a suspicious cloud bill. Use gcx to
> analyze profiling data (all profile types, last hour) and traces, correlate
> what you find with the code in this repo, and give me a ranked list of
> inefficiencies with evidence and proposed fixes.

While it works, narrate what it's doing (gcx calls it makes). Expected finds:
PBKDF2, regex search, discount engine, receipt rendering, N+1 (from traces),
audit-trail lock, receipt cache, RestTemplate churn.

Prompt 2 (fix + verify):

> Fix the top three CPU issues. Then rebuild and restart with
> `docker compose up -d --build`, wait a few minutes of load, and use gcx to
> verify the improvement — compare CPU per service before/after.

- The before/after `gcx profiles metrics --top` comparison is the money shot.
- If time allows, prompt 3: "Anything you found that the CPU profile alone
  would have missed?" (N+1, lock, cache growth.)

Fallbacks: rehearsal screenshots of prompts 1 and 2 results; if the rebuild is
slow on stream, switch to the pre-built fixed images while narrating.

## 4. Wrap (5 min)

- Recap: discover → leaderboard → flamegraph → other profile types → span
  profiles → agent does the cross-referencing.
- Where to get gcx, docs, community links; ask for topic requests.

## Tuning knobs (if signals are weak)

- `docker compose run --rm -e CHECKOUT_RATE=25 k6 run /scripts/checkout-flow.js` —
  more checkout pressure (default 6/s).
- JIT compilation dominates profiles for the first ~10 minutes after a restart;
  always give it warm-up time.
- Memory growth on `orders` needs uptime (hours) — don't restart it before the
  demo if you want to show the leak trend.
