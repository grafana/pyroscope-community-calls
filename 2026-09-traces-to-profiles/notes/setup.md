# Setup before the call

Everything is local and there is no cloud stack this time. The only credential
anywhere is the GitHub App for the optional source code segment in §6; skip that
section and the demo needs no secrets at all.

## 1. Start the stack early

```sh
cd bloom
make up
make ps
make smoke
```

Start it **at least 30 minutes** before going live, ideally a few hours. The
session is about the tail of a distribution: at 6 checkouts/s only ~15% of
requests are large carts, so a short window has too few expensive spans for the
heatmap to look convincing.

The first `catalog` start seeds the database. It is ready when
`docker compose logs catalog` prints `seeded 6000 products`.

JIT dominates CPU profiles for the first few minutes after a restart. Do not
rebuild a service right before going live.

## 2. Verify the wiring, not just the containers

**Feature toggles** — `profilesHeatmap` must be `true`; without it there is no
Time series / Span heatmap switch and segments 3 and 4 have nothing to show.
`flameGraphWithCallTree` is only needed for the 2026-08 aside:

```sh
curl -s localhost:3000/api/frontend/settings \
  | jq '{heatmap: .featureToggles.profilesHeatmap, callTree: .featureToggles.flameGraphWithCallTree}'
```

The span profile in the trace view needs no toggle at all — it is on for
everyone, which is worth saying on stream.

**Traces → profiles on the Tempo datasource** — this is what makes the span
detail render a flame graph. If `tracesToProfiles` is missing, the whole first
half of the session has nothing to show:

```sh
curl -s localhost:3000/api/datasources | jq '.[] | select(.type=="tempo") | .jsonData'
```

Expect `datasourceUid: bloom-pyroscope`, the CPU `profileTypeId`, and the
`service.name` → `service_name` tag mapping.

**Span correlation is actually flowing** — pick any recent checkout trace and
confirm the pricing span carries `pyroscope.profile.id`:

```sh
curl -s "http://localhost:3200/api/traces/<TRACE_ID>" \
  | jq -r '.batches[].scopeSpans[].spans[].attributes[] | select(.key=="pyroscope.profile.id") | .value.stringValue'
```

The value equals the span ID, and only **server** spans carry it — client and
internal spans have no span profile, by design. If nothing comes back at all,
the OTel Pyroscope extension is not correlating and the span heatmap will be
greyed out. The quickest second opinion is the profile side, which only has a
`span_name` label when correlation works:

```sh
gcx --context bloom-local profiles labels --label span_name --since 15m
```

Expect `GET`, `POST`, `catalog`, `orders`.

**Span exemplars exist** — the fastest end-to-end check, since it exercises the
same `SelectHeatmap` RPC the UI panel uses:

```sh
gcx --context bloom-local profiles exemplars span '{service_name="bloom-pricing"}' \
  --profile-type process_cpu:cpu:nanoseconds:cpu:nanoseconds --since 15m --top-n 5
```

Expect spans in the 300–500 ms range, each with a trace ID.

## 3. The gcx context for the local stack

One time, no tokens involved — gcx sends unauthenticated requests and Grafana
treats them as the anonymous Admin:

```sh
gcx config set stacks.bloom-local.grafana.server http://localhost:3000
gcx config set stacks.bloom-local.grafana.org-id 1
gcx config set contexts.bloom-local.stack bloom-local
gcx config set contexts.bloom-local.datasources.pyroscope bloom-pyroscope
gcx config set contexts.bloom-local.datasources.tempo bloom-tempo
```

This does not change the current context. Either pass `--context bloom-local`
on every command, or switch for the duration of the call:

```sh
gcx config use-context bloom-local     # remember to switch back afterwards
```

Switching is worth it on stream — the commands are long enough already, and
`--context bloom-local` in every line reads as noise. Check
`gcx config current-context` on camera before the first command so nothing
private is in scope.

## 4. Tabs to have open

| Tab | URL |
|---|---|
| Storefront | http://localhost:8080 |
| Explore, Tempo datasource | http://localhost:3000/explore |
| Profiles Drilldown | http://localhost:3000/a/grafana-pyroscope-app/explore |
| Terminal with a large font | `gcx config current-context` already run |

Sign in as `admin` / `bloom` before the stream. Anonymous access works for
Drilldown, but Explore's trace view is where **Open in Profiles Drilldown**
appears, and some Explore features resolve their flags only for a signed-in
user.

## 5. Pick the example trace fresh, on the day

Ten minutes before going live, run the queries in
`notes/runbook.md` §1 and write down:

- one slow checkout trace ID (>300 ms) with a large cart
- the pricing span ID inside it
- the top three span exemplar rows

Having them on paper means a cold cache or an unlucky search on stream costs
nothing.

## 6. Source code integration (optional segment)

Turns a flame graph frame into the actual Java source, line by line: right-click
any non-root frame in the flame graph and pick **Function details**.

### One-time: register a GitHub App

Two of the three pieces are already committed — `.pyroscope.yaml` at the repo
root, and the `service_repository` label in `docker-compose.yml`. What is left
is the app.

1. GitHub → Settings → **Developer settings** → **GitHub Apps** → **New GitHub App**.
2. **GitHub App name**: anything, e.g. `bloom-demo-pyroscope`.
3. **Homepage URL**: required but unused, e.g. `https://grafana.com/oss/pyroscope/`.
4. **Callback URL**: `http://localhost:3000/a/grafana-pyroscope-app/github/callback`
5. **Permissions**: none. `pyroscope-community-calls` is public, and the app
   needs no scopes to read a public repository. (A private repo would need
   Repository → Metadata and Contents, both read-only.)
6. Uncheck **Webhook → Active**.
7. Create it and note the **Client ID**, then **Generate a new client secret**
   and copy it immediately — GitHub shows it once.

Then add three settings to `bloom/.env`, which is gitignored. This repository is
public, so they must never go into `docker-compose.yml`:

- `GITHUB_CLIENT_ID` — from step 7
- `GITHUB_CLIENT_SECRET` — from step 7
- `GITHUB_SESSION_SECRET` — any random string; `openssl rand -base64 48`

`bloom/.env.example` lists all three, commented out, with the same note.
Recreate Pyroscope afterwards so it picks them up:

```sh
cd bloom && make up SERVICE=pyroscope
```

### Verify the server side before touching the UI

```sh
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  -H 'Content-Type: application/json' -d '{}' \
  http://localhost:4040/vcs.v1.VCSService/GithubApp
```

**501** means Pyroscope has no client ID, so the settings did not reach the
container. **200** means it is configured. That one status code separates "the
GitHub App is wrong" from "everything else is wrong", so check it first.

Then in Grafana: Function details → **Integrate with Github** → connect and
authorise. The token lands in a `pyroscope_git_session` cookie encrypted with
your session secret. It is per-browser, refreshable for about 184 days, and
**Disconnect from GitHub** clears it.

### The commit ref is the part that will bite you

Source is read from the repository **at the commit in `service_git_ref`**, and
the Makefile derives that from the checkout — so it reads `<sha>-dirty` whenever
the working tree has uncommitted changes, and a `-dirty` ref exists on no
remote. Symptom: *(file information unavailable)*, or an empty commit selector.

The demo therefore needs the Bloom changes committed and **pushed**, and the
stack restarted so profiles carry the pushed sha. Until that happens, either
pass a pushed ref:

```sh
make up SERVICE_GIT_REF=main
```

or set the ref per service in the Function details **override** panel
(Data source / Service name / repository URL / git ref / Path to root).

Be careful with both workarounds on stream: they show the code at *that* ref,
not the code that is running. If `bestBundleDiscount` is not on the ref you
point at, you are showing a different program from the one you just profiled,
which quietly undermines the segment.

### If it does not resolve

| Symptom | Cause |
|---|---|
| **Authentication to data source failed** | `keepCookies` is missing from the Pyroscope datasource — see below |
| No **Function details** in the context menu | You right-clicked the root frame, or the toggle in the plugin's Settings is off (it defaults on) |
| **Integrate with Github** banner never clears | `GithubApp` is returning 501 — see the curl above |
| *(file information unavailable)* | The ref is `-dirty` or unpushed, `.pyroscope.yaml` is missing at that commit, or no `function_name` prefix matched |
| **No commits found** in the selector | Same root cause: the ref does not exist on the remote |
| Bloom files resolve but some frames do not | Expected outside the mapped prefixes — `libjvm.so`, Spring, Tomcat. Add mappings if you care |

### Why "Authentication to data source failed" happens

The GitHub token lives in a `pyroscope_git_session` cookie on the Grafana
origin, and the plugin reaches Pyroscope through
`/api/datasources/proxy/uid/bloom-pyroscope`. That proxy forwards **only**
cookies named in the datasource's `keepCookies` allowlist, so without it the
VCS calls arrive with no cookie at all, Pyroscope answers
`401 unauthenticated`, and Grafana renders that as *Authentication to data
source failed*.

`grafana/provisioning/datasources/pyroscope.yml` now carries:

```yaml
jsonData:
  keepCookies:
    - pyroscope_git_session
```

Measured, by proxying a request to a header-echoing backend through two
datasources that differed only in this field: with the allowlist the backend
received `pyroscope_git_session=...`, without it the Cookie header was absent.
An unrelated cookie sent alongside was dropped either way — it is a strict
allowlist, not a switch.

This is also why the GitHub App can look correctly configured while nothing
works: `GithubApp` needs no cookie and returns 200, while `GetFile` needs the
cookie and returns 401. Check both, not just the first.