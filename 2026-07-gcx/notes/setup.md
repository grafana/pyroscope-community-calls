# Setup before the call

## 1. Grafana Cloud stack

Use a dedicated demo stack so nothing production-ish shows on stream.

1. Create (or reuse) a stack via grafana.com. Note the stack slug and region.
2. From the stack's details page collect:
   - **Pyroscope** ("Send Profiles" / details): URL `https://profiles-prod-<NNN>.grafana.net`,
     numeric instance ID (basic-auth user).
   - **OTLP** (OpenTelemetry "Configure"): endpoint
     `https://otlp-gateway-prod-<region>.grafana.net/otlp`, OTLP instance ID.
3. Create a Cloud Access Policy token with `profiles:write` and `traces:write`
   scoped to the stack.

## 2. Fill bloom/.env

```sh
cd bloom
cp .env.example .env
```

- `PYROSCOPE_SERVER_ADDRESS` / `PYROSCOPE_BASIC_AUTH_USER` / `PYROSCOPE_BASIC_AUTH_PASSWORD`
- `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic%20<base64 of "otlp-instance-id:token">`

Compute the header value with:

```sh
echo -n "<otlp instance id>:<token>" | base64
```

Then `make up` and `make load`. Verify in the stack's Grafana:
Drilldown > Profiles should show the four `bloom-*` services within a minute,
and Tempo should show `bloom-gateway` traces.

## 3. gcx context

```sh
gcx login bloom-demo --server https://<stack-slug>.grafana.net --oauth
gcx config use-context bloom-demo
```

Set the default Pyroscope datasource so `-d` isn't needed in every command
(the UID of the auto-provisioned `grafanacloud-<slug>-profiles` datasource):

```sh
gcx datasources list
gcx config set contexts.bloom-demo.datasources.pyroscope <UID>
gcx config set contexts.bloom-demo.datasources.tempo <UID of -traces>
```

Sanity check:

```sh
gcx profiles labels --label service_name --since 15m
```

Should list the four bloom services.

## 4. Local rehearsal mode (no cloud)

```sh
cd bloom
cp .env.local .env
make up-local        # bundled Pyroscope on :4040 + Grafana on :3000
make load
```

The `local` compose profile also starts Tempo (traces) and a Grafana with
anonymous auth (org role Admin, no login — same setup as the pyroscope repo
examples) and both datasources provisioned — Pyroscope as UID
`bloom-pyroscope`, Tempo as `bloom-tempo`, linked via traces-to-profiles. The
full gcx flow (profiles *and* traces) works with no cloud at all, and Profiles
Drilldown / Explore can be rehearsed at http://localhost:3000.

Point gcx at the local Grafana (one-time, no tokens involved — gcx sends
unauthenticated requests and Grafana treats them as the anonymous admin):

```sh
gcx config set stacks.bloom-local.grafana.server http://localhost:3000
gcx config set stacks.bloom-local.grafana.org-id 1
gcx config set contexts.bloom-local.stack bloom-local
gcx config set contexts.bloom-local.datasources.pyroscope bloom-pyroscope
gcx config set contexts.bloom-local.datasources.tempo bloom-tempo
```

This does not switch the current context — pass `--context bloom-local` per
command, or opt in with `gcx config use-context bloom-local`.

Verified working against this setup: `list-profile-types`, `labels`,
`metrics --top`, `query` (all profile types), and `exemplars span` (needs the
bundled Pyroscope's `-architecture.storage=v2`; the v1 frontend does not serve
exemplars).

Local quirk: the Pyroscope container profiles itself, so `metrics --top` over
`'{}'` ranks `pyroscope` first. Use `'{service_name=~"bloom-.*"}'` to scope to
the demo services — worth doing on stream anyway.

## 5. Day-of

- Start the stack and `LOAD_DURATION=120m make load` at least an hour before the stream.
- Do **not** restart `orders` right before the demo — the memory-growth issue
  needs uptime to be visible.
- Keep `notes/` off the shared screen; the agent segment runs Claude Code inside
  `bloom/` only.
- Pre-pull docker images on the demo machine; pre-warm a `make build` so the
  live "fix + redeploy" rebuild is fast (Maven cache is already hot).
