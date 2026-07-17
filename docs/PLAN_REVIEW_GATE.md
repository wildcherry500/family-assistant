# Lumino Plan Review Gate

**Purpose:** A Claude Code plan is not approved until it passes this gate. This
checklist exists because the same failure categories recur across sessions. Run
every item. If any item fails, the verdict is **stop and report** — not a silent
fix and not conditional approval.

Roles in this workflow: Claude Code writes the plan and does the audit; Claude
(chat) runs this gate and surfaces forks; Tor makes the call at each fork.

---

## Gate 0 — Audit-first evidence present

The plan must open with a **Step 0 audit finding stated in plain language**,
including the actual quote or line reference for any wiring it depends on
(topology `.source()` wiring, PState declaration, existing method signature).

- **Fail if:** the plan proposes changes but the audit finding is missing, vague,
  or asserted without the quote. "I reviewed the file" is not an audit finding.
- **Why:** every session where this was skipped, the plan was built on a stale or
  assumed wiring. The quote is the proof the plan is grounded in the current code.

---

## Gate 1 — Replay-safe and additive

Everything must be additive and recomputable via depot re-drain. No destructive
migration, no rewriting history.

- **Fail if:** the plan deletes or rewrites existing depot records, or changes a
  field in a way that can't be rebuilt by replaying the depot.
- **Allowed:** new depots, new PStates, new fields with null defaults, new
  topologies reading existing depots.
- **Remember:** recomputed-on-redrain state (creation) vs. permanent event-sourced
  state (user status changes) are different by design. Status transitions are
  write-once permanent records; content fields refresh from source on every redrain.

---

## Gate 2 — PState write ownership

A PState may only be written by its declaring topology.

- **Fail if:** the plan writes a PState from a different module, from edge code,
  or via `getMirrorStore().transform()`.
- **Correct:** cross-module writes go through `getMirrorDepot().append()`; the
  owning topology materializes the PState.

---

## Gate 3 — Record construction pattern

Records are built key-by-key via sequential `localTransform` calls.

- **Fail if:** the plan assembles a full record as one `HashMap` and writes it
  with `termVal` — this throws `ClassCastException` on later partial writes.
- **Correct:** sequential `localTransform` per key.

---

## Gate 4 — Serialization boundary

- **Fail if:** the plan passes `List.of()`, `Arrays.asList()`, or `Map.of()` into
  an agent node or depot append.
- **Correct:** always `new ArrayList<>(...)` / `new HashMap<>(...)`.
- **Fail if:** the plan introduces typed `RamaSerializable` POJOs at a persistence
  boundary (depot append or PState leaf). Persistence leaves stay plain JDK maps of
  JDK types. Typed classes are for in-flight data between agent nodes only. (If
  depot-boundary typing is ever truly needed, the upgrade path is Protobuf — not
  POJOs — and it requires its own approved brief.)

---

## Gate 5 — PState read shape

Reading a subindexed map with `selectOne(Path.key(...))` returns a
`RocksDBWrapper` at runtime, not a `Map`.

- **Fail if:** the plan assumes a `Map` comes back from a subindexed `selectOne`.
- **Correct workaround:** wide `sortedMapRange(Long.MIN_VALUE, Long.MAX_VALUE)`
  query, or confirm the PState is not subindexed before relying on `selectOne`.
- **Gate action:** the plan must state whether the target PState is subindexed. If
  it doesn't say, that's a fail — ask before approving.

---

## Gate 6 — Auto-vivify / unknown-ID handling

Rama auto-vivifies keys on read/write. An endpoint that accepts an ID and writes
without validating existence creates a permanent stub record with no source data
and an unbounded key-space.

- **Fail if:** a mark-done / update / status endpoint appends for an arbitrary
  incoming ID without an existence check, and returns success regardless.
- **Correct:** validate the ID resolves to a real record before append, or make the
  unknown-ID decision an explicit fork for Tor.

---

## Gate 7 — Agent graph completeness

- **Fail if:** any agent node branch does not end in `result(...)` — the
  invocation hangs forever.
- **Fail if:** `agentTopology.define()` is not the last call in the module — agents
  are silently unavailable.

---

## Gate 8 — Edge-code discipline

Cross-check against `EDGE_CODE_RULES.md`. Glue code (webhook, Gmail/OAuth/Pub-Sub,
LLM transport) receives → appends raw → acks. No filtering, deciding, transforming,
deduplicating, enriching, content-routing, or edge-side LLM calls.

- **Fail if:** business logic appears in a handler or API client instead of a
  topology or agent graph.

---

## Gate 9 — Deterministic IDs

- **Fail if:** any entity or commitment ID is randomly generated. IDs are
  deterministic hashes of their source fields (e.g. `hash(eventId|objectType|object)`,
  commitment ID hashed from source edge fields) so redrain reproduces them exactly.

---

## Gate 10 — Test evidence and scope

- The plan must state the expected test outcome (green count) and that new logic is
  covered by non-LLM tests following the `InProcessCluster` / `ZooEmailTest` pattern.
- **Fail if:** the plan silently expands scope beyond the brief. "Simpler/easier" is
  never a valid reason for divergence. Divergence = stop and report.
- **Fail if:** the plan drags in a deferred item (entity resolution, co-occurrence,
  Layer 3 proactive loop, a new index) that wasn't in the brief.

---

## Verify-at-source reminder

Any architecture-shaping Rama or AOR API behavior must be confirmed via
Chat-o-rama (chat.redplanetlabs.com) or RPL docs before locking — not from memory
and not from the learnings file alone. If the plan asserts a novel API behavior
without a source, that's a fail: verify first.

---

## Gate verdict format

After running the gates, respond to Tor as:

- **PASS** — plan is sound, here's the one-line why, approve when ready.
- **FAIL at Gate N** — plain-language statement of the problem, what belongs
  instead, and (if it's a product choice) the fork stated as Tor's decision.

Never approve conditionally. Either it passes, or it goes back to Claude Code with
the specific gate it failed.
