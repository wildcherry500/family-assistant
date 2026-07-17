# EDGE_CODE_RULES.md — Keep the Glue Dumb

**Status:** Standing architectural rule. Applies to every session. Claude Code must read this alongside `CLAUDE_HANDOFF.md` and `RAMA_VERIFIED_LEARNINGS.md` before writing or modifying any code.

---

## The Rule

**All business logic lives inside Rama (topologies, PStates, agent graphs). Edge code exists only to move raw data between the outside world and the depot. Edge code makes no decisions.**

Rama collapses database + queue + cache + compute into one model. That advantage is destroyed if logic leaks into the code at the system's edges. The edge is a doorway, not a room.

---

## Definitions

**Edge code (glue):** Any code that touches systems outside Rama —
- Webhook handlers (Cloudflare Tunnel → localhost:8080)
- Gmail API / OAuth / Pub/Sub client code
- LLM API call scaffolding (Gemini request/response transport)
- Any future HTTP endpoint, iOS Shortcut receiver, or external API client

**Interior (where logic belongs):** Depots, stream topologies, PState materialization, deterministic query code, AOR agent graphs.

---

## What Edge Code MAY Do

1. Receive a payload from the outside world.
2. Append it to the appropriate depot **raw and complete** (e.g., `$$raw-emails` write-ahead depot).
3. Return an acknowledgment (HTTP 200, etc.).
4. Handle transport-level errors only: retries, timeouts, auth token refresh, malformed-request rejection.

That is the entire job. Nothing else.

---

## What Edge Code MUST NOT Do — Creep Signals

If any of the following appear in a webhook handler, API client, or other glue code, **stop and report**. This is plan divergence, not a judgment call.

| Creep signal | Example | Where it belongs instead |
|---|---|---|
| **Filtering** | "Skip emails from this sender" / "ignore if subject contains X" | Stream topology reading the depot |
| **Deciding** | "Only append if this looks like a school event" | Topology or agent graph |
| **Transforming** | Parsing fields, extracting dates, restructuring JSON before append | Parse topology / agent node |
| **Deduplicating** | "Check if we've seen this message ID before appending" | Topology with a PState index |
| **Enriching** | Adding tags, confidence scores, personId lookups at the edge | Topology after depot drain |
| **Routing by content** | "Send school emails to depot A, personal to depot B" based on payload inspection | Single raw depot; fan-out in topology |
| **Calling the LLM from the edge** | Parsing with Gemini inside the webhook handler before append | Parse happens downstream of the depot, inside the module |
| **Writing to any store other than the depot** | Edge code updating a PState, a file, or Supabase directly as part of ingestion | Depot append only; PStates materialize from topologies |

---

## Why This Rule Exists (the payoff being protected)

The append-only raw depot is Lumino's compounding advantage:

- **Counterfactual replay.** When a better model ships, re-drain the depot through the new parser and every PState rebuilds from original, unmodified data. Any transformation done at the edge is permanently baked in and cannot be replayed.
- **Auditability.** The depot is the complete, untampered record of what actually arrived.
- **Testability.** Deterministic topologies over raw data are unit-testable without live external services. Logic in edge code is not.

Every line of logic moved into the glue is a line subtracted from replay, audit, and test coverage.

---

## Quick Test for Any New Code

Before writing code that touches the outside world, ask:

> "If I deleted this code and replaced it with 'receive → append raw → ack,' would any *decision* be lost?"

- **No** → correct edge code.
- **Yes** → that decision belongs in a topology. Stop and report before proceeding.

---

## Known Accepted Exceptions (do not "fix" without explicit approval)

- **Supabase session persistence (Focus Player):** acknowledged second store, pragmatic shortcut, on the future purify-later list. Do not migrate without an approved plan.
- **Transport-level auth (OAuth token refresh, watch renewal):** lives at the edge by necessity — it is transport, not business logic.

---

*If uncertain whether something is transport or logic: it's logic. Stop and report.*