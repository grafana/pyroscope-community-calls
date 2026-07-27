# Community Call 2026-07-30: gcx for profiling data

Pyroscope community call (YouTube live stream, ~1 hour) about discovering and
analyzing profiling data with gcx — first manually in a terminal, then from an
AI agent (Claude Code) that correlates profiles with source code and fixes the
problems it finds.

## Layout

- `bloom/` — the demo app: a flower-shop built as 4 Spring Boot microservices with
  planted inefficiencies, instrumented with Pyroscope + OTel traces, k6 load,
  all in Docker Compose. **Spoiler-free on purpose**: nothing in it may mention
  the planted issues. For the agent segment, run Claude Code in a copy of
  `bloom/` outside this repo (see the runbook pre-flight) — an agent launched
  anywhere under `2026-07-gcx/` could read `notes/`.
- `notes/` — presenter-only material. Keep out of screen share until the reveal.
  - `notes/setup.md` — Grafana Cloud stack + gcx setup before the call
  - `notes/runbook.md` — minute-by-minute session flow with the exact commands
  - `notes/answer-key.md` — every planted inefficiency, how it shows up, and its fix

## Prep checklist

- [x] Demo app built and smoke-tested locally (local Pyroscope rehearsal mode)
- [ ] Create/choose the dedicated Grafana Cloud demo stack, fill `bloom/.env` (see `notes/setup.md`)
- [ ] `gcx login` context for the demo stack + default Pyroscope datasource
- [ ] Full rehearsal against the cloud stack: `make up`, `make load`, run through `notes/runbook.md`
- [ ] Rehearse the agent segment once end-to-end (fixes + redeploy + verify)
- [ ] Start load ~60 min before the stream so charts and profiles are warm
- [ ] Backup material in case the live agent stalls (screenshots or a recorded run)
