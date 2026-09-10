# Community Call 2026-09: from a slow trace to the line of code

Tempo community call about the **traces → profiles** integration. One
question drives the whole session: a customer says checkout is slow; how do
you get from that complaint to the Java frame that caused it, and then to every
other request with the same problem?

The answer is a loop:

```
Tempo trace  →  span profile  →  span heatmap  →  Top span exemplars 
"which span"    "which frame"    "how common"      "which requests"
```

## Layout

- `bloom/` — the demo app and the Compose stack: Bloom, Postgres, the k6 load
  generator, Pyroscope, Tempo and Grafana. Carried over from
  `2026-08-call-tree-heat-map-adaptive-profiles/`, with the pricing service
  reworked so big carts produce spans expensive enough to profile (see below).
- `notes/` — presenter-only material.
  - `notes/runbook.md` — session flow, the exact clicks and commands
  - `notes/setup.md` — what to do before the call, and how to verify it took

## Running the demo

```sh
cd bloom
make up
```

Then open Grafana at http://localhost:3000 — anonymous access as Admin, no
login. The storefront is at http://localhost:8080.

Give it 30 minutes of load before the views are worth looking at. The tail of
the latency distribution is the whole point, and a short window has too few
expensive requests in it.

## How the integration is wired

Three pieces, all already in `bloom/`:

**In the app** — the OTel Java agent runs with the Pyroscope OTel extension
(`otel-profiling-java` 2.1.0). The extension tags every server span with
`pyroscope.profile.id` and labels profile samples with the span, so a profile
sample can be attributed to one span. Both jars are baked into the `Dockerfile`.

**In the Tempo datasource** — `tracesToProfiles` in
`grafana/provisioning/datasources/pyroscope.yml` points at the Pyroscope
datasource, names the profile type, and maps `service.name` → `service_name`:

```yaml
jsonData:
  tracesToProfiles:
    datasourceUid: bloom-pyroscope
    profileTypeId: process_cpu:cpu:nanoseconds:cpu:nanoseconds
    tags:
      - key: "service.name"
        value: "service_name"
```

With that set, Grafana's span detail renders the inline **Flame graph** for any
span carrying `pyroscope.profile.id`, and offers **Profiles for this span**.
From Explore's trace view it also offers **Open in Profiles Drilldown**, which
hands the span selector to Drilldown's flame graph view.

**In Pyroscope** — the span heatmap and its exemplars come from the
`SelectHeatmap` RPC, which is only served by the v2 read path. The Compose
stack starts Pyroscope with `-architecture.storage=v2` for that reason.

## What changed in Bloom since the 2026-08 call

The 2026-08 stack already had span profiles, but no single span was expensive
enough to read a flame graph from. Two changes fix that:

- `DiscountEngine.bestBundleDiscount` replaces the old inline pair search. It
  re-scores every promotion pair from scratch on each refinement round and
  never memoizes `discountFor`, so cost is quadratic in the eligible promotion
  set. Two knobs in `pricing/src/main/resources/application.yml` control the
  cost: `refinement-rounds` (2) and `max-eligible` (80).
- The k6 checkout flow gives ~15% of shoppers a cart of 8–15 items instead of
  1–4. Large carts saturate the eligible set, so they pay the full 12,640-pair
  search — and they are the expensive tail the heatmap finds.

The result is a per-quote CPU cost that climbs from ~2 ms for a single stem to
~193 ms for a large cart, while the service's average CPU stays flat. That gap
is what the whole session is about.

## Optional: source code integration

One more step past the span profile — right-click a frame in the flame graph and
pick **Function details** to see the profiled line highlighted in the real
source, read from GitHub.

Two of the three pieces ship in this repo:

- `.pyroscope.yaml` at the repository root maps Java frames to files. Java
  profiles carry no filename, so Pyroscope reconstructs
  `com/bloom/pricing/DiscountEngine.java` from the function name and needs a
  mapping to find the module it lives in. Bloom's four modules get one mapping
  each, plus one sending `java/` frames to `openjdk/jdk` — where this demo's
  hottest self time actually is.
- `service_repository` and `service_git_ref` labels on every profile, set in
  `docker-compose.yml`, tell Pyroscope which repository and commit to read.

The third is a GitHub App you register yourself, which supplies
`GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` and `GITHUB_SESSION_SECRET` to the
Pyroscope container. It needs **no permissions** for this public repository.
`notes/setup.md` §6 has the steps, the one-line check that tells you whether the
server side is configured, and the failure modes.

## License

Apache-2.0 — see [LICENSE](../LICENSE).
