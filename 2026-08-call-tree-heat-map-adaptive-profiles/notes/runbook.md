# Session runbook — 2026-08 community call

Pre-flight: `make up` at least 30 minutes before going live so the time range is
full, `make smoke` passes, storefront tab open at http://localhost:8080, and
Profiles Drilldown open at
http://localhost:3000/a/grafana-pyroscope-app/explore (no login needed).

Verify the toggles took before the stream:

```sh
curl -s localhost:3000/api/frontend/settings \
  | jq '{callTree: .featureToggles.flameGraphWithCallTree, heatmap: .featureToggles.profilesHeatmap}'
```

Both must be `true`. Note this endpoint says `true` even in the one case where
the UI disagrees — see the table at the bottom.

## 1. Meet Bloom again (3 min)

- Storefront at http://localhost:8080 — browse, search "midnight", check out.
- Same four Spring Boot services as the July call; this time Pyroscope, Tempo
  and Grafana run alongside them in the same Compose stack.

## 2. Call Tree (15 min)

Drilldown → Profiles → **All services** shows the four `bloom-*` services. Click
**Flame graph** on `bloom-pricing`.

Beats to hit:

- Three views now: Top Table, Flame Graph, **Call Tree** — and in Drilldown's
  split layout you get the selector twice, so put Call Tree on the left and the
  flame graph on the right and talk about the same profile in two ways.
- Sort by Total, then expand down into the promotion loop. The tree is where
  "which caller is responsible" reads more directly than in the flame graph.
- Use Search to filter; use **Swap views** to flip the panes.
- Note it is public preview, behind `flameGraphWithCallTree`.

## 3. Span heatmap (20 min)

Same tab. The top panel is "CPU time consumed" — a normal averaged time series.
Ask what it hides, then hit **Span heatmap**.

- The panel becomes "CPU time consumed per trace span": one cell per bucket of
  spans by CPU consumed, counts in the dozens per column. The dense band near
  the bottom is normal checkouts, the sparse cells above are the expensive tail.
- Scroll to **Top span exemplars** underneath — the heaviest spans with their
  CPU time, wall duration, and a **Trace ID** link. Click through to the trace in
  Tempo, which closes the loop from "this bucket is slow" to "this request".
- Switch the Service picker across `bloom-gateway` / `bloom-catalog` /
  `bloom-pricing` / `bloom-orders` — the shapes differ per service.
- Note it is experimental, behind `profilesHeatmap`, and needs Pyroscope's v2
  read path server-side.

Optional aside if there is time: the *Bloom / profile heatmaps* dashboard shows
the same thing driven straight from the query editor, plus the `individual`
mode (one point per uploaded profile) that Drilldown does not expose.

## 4. Wrap (5 min)

- Both views are in Grafana 13.1.3 today, toggles off by default, Drilldown
  preinstalled.
- Repo with this exact stack: one `make up`, no cloud account, no login.

## If something breaks live

| Symptom | Cause | Fix |
|---|---|---|
| No Call Tree tab **in Explore** | Anonymous session — Explore's flag needs a signed-in user | Sign in as `admin`/`bloom`, or just demo it in Drilldown |
| No Call Tree tab in Drilldown | Toggle genuinely off | Check `GF_FEATURE_TOGGLES_ENABLE` in `docker-compose.yml` |
| Span heatmap empty or missing | Traces not flowing, or Pyroscope on the v1 read path | `docker compose logs tempo`; confirm `-architecture.storage=v2` |
| Trace ID links dead | Tempo datasource not selected | Pick "Tempo (bloom)" in the exemplars table |
| Everything empty | Load stopped | `docker compose ps k6`, `docker compose restart k6` |
