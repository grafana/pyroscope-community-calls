# Community Call 2026-08: Call Tree and profile heatmaps

Pyroscope community call about two new ways to read profiling data in Grafana:
the **Call Tree** view in the Flame Graph package, and the **span profile
heatmap**. Both ship in Grafana 13.1.3 behind feature toggles, and both are
demoed in **Profiles Drilldown** against Bloom, the flower-shop app from the
2026-07 call.

## Layout

- `bloom/` — the demo app, carried over from `2026-07-gcx/` with the Compose
  stack rewired: Bloom, Postgres, the k6 load generator, Pyroscope, Tempo and
  Grafana all come up together, locally, with no Grafana Cloud account needed.
- `notes/` — presenter-only material.
  - `notes/runbook.md` — session flow, the exact clicks, and what each view should show

## Running the demo

```sh
cd bloom
make up
```

Then open http://localhost:3000/a/grafana-pyroscope-app/explore — no login, the
stack runs with anonymous access as Admin.

Give it ~5 minutes of load before the views are worth looking at, and ~30
minutes to fill a comfortable time range.

Profiles Drilldown (`grafana-pyroscope-app`) is preinstalled in Grafana 13.1.3 —
version 2.2.0, which is new enough for both features. Nothing to install.

## The two features

**Call Tree** — a third view next to Top Table and Flame Graph, showing the
profile as an expandable tree with per-function totals and percentages. In
Drilldown's *Flame graph* tab it is available in both panes of the split view,
so you can put the Call Tree next to the flame graph for the same profile.
Toggle: `flameGraphWithCallTree` (public preview).

**Span heatmap** — in the *Flame graph* tab, the "CPU time consumed" panel has a
**Time series / Span heatmap** switch. The heatmap buckets spans by CPU time
consumed, and lists the heaviest ones underneath as *Top span exemplars* with
links straight into the trace in Tempo. Toggle: `profilesHeatmap`
(experimental). Server-side this is Pyroscope's `SelectHeatmap` RPC.

## Gotchas worth knowing

- **Pyroscope must run with `-architecture.storage=v2`.** `SelectHeatmap` is
  unimplemented on the v1 read path, and the default is `v1-v2-dual`.
- **Anonymous is fine for Drilldown, not for Explore.** The plugin registers its
  own OpenFeature provider, so the toggles resolve for anonymous visitors. Core
  Explore instead relies on Grafana's own OpenFeature client, which is only
  initialized for a signed-in user — so the Call Tree tab is missing in Explore
  until you sign in (`admin` / `bloom`). Both flags still report `true` from
  `/api/frontend/settings` either way, which makes this confusing to debug.
- **Drilldown only exposes the span heatmap.** The datasource also supports an
  `individual` mode (one point per uploaded profile), reachable only from a
  Heatmap panel — see the optional dashboard below.

## Optional extra: the raw query option

The provisioned *Bloom / profile heatmaps* dashboard drives the heatmap straight
from the Pyroscope datasource query editor (Options → Heatmap / Heatmap Type),
including the `individual` mode Drilldown does not surface. Useful as a
"here's what's underneath" aside; not needed for the main demo.

## License

Apache-2.0 — see [LICENSE](../LICENSE).
