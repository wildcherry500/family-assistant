# Phase A Audit + Phase B Plan — Provenance Stamping & Must-Land Temporal Fields

**Branch:** `feature/raw-ingestion-depot`
**Scope:** all of `BRIEF_provenance_stamping.md`, plus ONLY the "must land before real data"
list in `DECISION_temporal_model.md` §6. Nothing from deferred sections.
**Status:** Phase A complete. Phase B plan below is NOT approved and NOT started.

---

## PHASE A — AUDIT FINDINGS

### A1. Where interpretation enters the system

Two Gemini calls, both in `EmailParsingModule`, both on the path to persistence:

| Node | File:line | Call |
|---|---|---|
| `classify` | `EmailParsingModule.java:228` | `model.chat(classifyPrompt)` → category, silo, intent |
| `extract-details` | `EmailParsingModule.java:298` | `model.chat(extractPrompt)` → title, startTime, deadline, childName, relations |

Path to persistence: `persist-raw` → `classify` → `extract-details` → `write-to-store`
(builds `eventRecord`, `EmailParsingModule.java:370-408`) → `depot.append(eventRecord)` to
`*family-events` (`:410-411`) → `FamilySchemaModule.java:273` stream drains it into
`$$family-data` and every index.

**Yes, there are two model calls.** Reported as the brief requires — it is the design
question behind open question 1.

**Third interpretation source the brief did not anticipate — a non-LLM fallback.**
`EmailParsingModule.java:259-261`:

```java
if ("UNKNOWN".equals(categoryStr)) {
    categoryStr = classifyByKeyword(message.body);
}
```

`classifyByKeyword` (`:432-439`) is deterministic Java string matching. It fires whenever the
model returns UNKNOWN *or* the JSON parse throws (`:254-257` swallows the exception). So a
record's `category` — and therefore its `tags`, and therefore its `$$events-by-tag` membership —
may have been produced by hardcoded keyword rules with no model involved at all. A single
`modelId` stamp on such a record asserts something false.

`QueryModule` also calls the model twice (`:208`, `:276`) but persists nothing — answers return
to the caller. Not a provenance surface today.

### A2. What provenance already exists

**Arrival provenance — exists.** `ParsedEvent`'s fields commented "Provenance fields"
(`EmailParsingModule.java:95-101`): `senderEmail`, `senderName`, `emailSubject`,
`gmailMessageId`, plus `receivedAt` and `accountLabel`. These say where the data came from.

**Derivation provenance — partially exists already, and one part is in the form the brief
explicitly rejects.** Added 2026-08-01 (`CLAUDE_HANDOFF.md:685-689`):

- `MODEL_ID = "gemini-2.5-flash"` (`:63`) — referenced by the `gemini-model` agent-object
  builder at `:156`, so the stamp genuinely cannot drift from the model called. This is sound.
- `PROMPT_VERSION = "v1"` (`:70`), with the comment *"BUMP THIS whenever either prompt's text
  changes — that is the entire point of the field."*

Both are stamped onto `ParsedEvent` (`:331`) and onto the event record (`:404-405`).

The brief's open question 2 is **DECIDED against exactly this mechanism**: *"No hand-bumped
integers. Rationale: a version field that depends on a human remembering to bump it fails
silently and corrupts the exact comparison this whole change exists to enable."*

So B0 is not new work bolted onto nothing — **it is a correction of an existing field's
derivation mechanism.** The field name and its position in the record stay; only how the value
is produced changes.

Also note `PROMPT_VERSION` is **one constant covering both prompts** ("Generation of the
classify + extract-details prompts"). If one prompt changes and the other does not, a single
shared value cannot express that.

**Missing entirely:** `derivedAt`, `sourceId`.

### A3. Raw vs derived depot boundary on this branch

**Both exist. The boundary is real and already correct.**

- **Archive of record:** `*raw-emails`, declared `FamilySchemaModule.java:149`, appended by the
  `persist-raw` node (`EmailParsingModule.java:194-197`) with `AckLevel.APPEND_ACK` — durably
  written *before* any parsing. Drained into the inspectable `$$raw-emails` view
  (`FamilySchemaModule.java:376-382`).
- **Derived:** `*family-events`, appended at `EmailParsingModule.java:410-411` after both model
  calls.

So arrival provenance belongs on the raw append; derivation provenance on the derived append.

**Observation, not a defect:** arrival provenance is currently on *both* — `senderEmail`,
`senderName`, `emailSubject`, `gmailMessageId`, `receivedAt`, `accountLabel` appear in the raw
record (`:184-192`) and are copied onto the derived record (`:395-399`). Duplication is
harmless and arguably convenient, but it means "where do the new fields go" must be answered
deliberately rather than by copying the existing pattern.

### A4. Where prompt strings live

Both are inline string concatenation inside their node lambdas, as expected.

**`classifyPrompt` — `EmailParsingModule.java:211-226`.**
Fully static text; the only interpolation is `+ message.body` at `:226` (the very end).
No conditional construction — one template, one branch.

**`extractPrompt` — `EmailParsingModule.java:277-297`.**
Static text with **two** interpolations:
- `String today = java.time.LocalDate.now().toString()` (`:276`), interpolated at `:277`
- `+ message.body` at `:297` (the very end)

No conditional construction — one template, one branch.

**This directly confirms the brief's B0 rationale.** `LocalDate.now()` means hashing the
*rendered* prompt would mint a new `promptVersion` every calendar day. The template hash is the
correct target. Static/runtime split is clean in both cases: all runtime data is appended at the
tail or is a single named date token, so extracting to a template constant with placeholders is
mechanical.

**Two templates ⇒ two hashes are available if wanted.** Neither prompt branches, so there is no
case needing more than one hash per prompt.

### A5. Schema impact

**No PState schema change is required.** `$$family-data`'s leaf is
`PState.mapSchema(String.class, Object.class)` (`FamilySchemaModule.java:166-169`) — an open map.
New keys need no declaration.

**No downstream reader breaks on unknown keys.** Every whole-map iteration in `src/main/` walks
a map *of records*, then reads named keys off each record — none iterates the keys *within* an
event record:

- `WebhookReceiver.java:169` — iterates the commitments map
- `DigestModule.java:64,80` — iterate leverage/weakness config entries
- `QueryModule.java:472,483,489` — iterate the events map and the by-dimension map

`EventUtils.tokenizeEvent` reads only `title` + `description` + `emailSubject`, so new keys will
not leak into `$$events-by-keyword`.

**Gate 3 trap does not bite.** `FamilySchemaModule.java:279-280` writes the whole record with
`termVal("*record")`, and it is the only write to `$$family-data` anywhere — no later, narrower
write navigates into a stored event record. (Same conclusion the 2026-08-01 gate review reached;
re-verified against current code.)

### A6. Existing records without provenance

**Zero. There is no live data at all.**

- No `rama` / ZooKeeper / Conductor / Supervisor processes running.
- `~/rama-data` is **12K** total, with `workers/` and `objects/` **empty** — no PState data on
  disk.
- `CLAUDE_HANDOFF.md:547` confirms: *"BLOCKER FOUND: cwd is overloaded (ZK dataset vs `tokens/`).
  Nothing deployed."*

**This is the most consequential finding in the audit.** The entire "irreversible if skipped"
window is still fully open, and the B3 backfill fork has no live consequence *yet*. It acquires
one the moment the cwd/ZooKeeper blocker clears and the first real ingest runs.

---

## ADDITIONAL AUDIT QUESTIONS

### Q1. Does the email path write relation edges today?

**Yes — fully wired, end to end, in production code.** Section 2's fields land now, not with
future edge work.

The LLM emits `relations` (prompt `:284-285`) → validated by `parseRelations` (`:466-491`) →
onto the record at `:380` → `FamilySchemaModule` materializes:

| Target | Line |
|---|---|
| `$$edges-forward` | `:335-336` |
| `$$edges-inverse` | `:337-338` |
| `$$entities` | `:340-341` |
| `$$entities-by-type` | `:342-343` |
| `$$commitments` (ACTION_NEEDED only) | `:353-364` |

**But there is a structural gap the decision doc's §2 assumes away: edges have no identity and
no records.** An edge today is pure set membership —
`$$edges-forward[familyId][eventId][relation] → Set<objectId>`. There is no `edgeId` anywhere in
the codebase, and no per-edge record. So `supersededBy` / `supersededAt` / `retractedAt` have
**nothing to attach to**. §2 is therefore not "add three fields" — it needs a new per-assertion
PState.

Two further gaps:
- **No relation registry exists**, so §2.6 cardinality (`one` | `many`) has no home. Relations
  are validated by a hardcoded regex at `EmailParsingModule.java:481`.
- **Edges are recomputed on redrain**, derived from event records. §2.3 wants the depot to hold
  every assertion. Today the de-facto assertion log *is* `*family-events` — an edge has no
  independent depot record. Compatible with §2.3's intent, but the history comes from event
  history, not an edge depot.

### Q2. Where do email-derived records get their timestamp — one field or already split?

**Four time fields exist, but the axes are conflated. Not split.**

| Field | Set at | Axis it actually is |
|---|---|---|
| `startTime`, `deadline` | LLM-extracted, `:393-394` via `parseIsoToEpoch` | `eventTime` — a claim |
| `receivedAt` | Gmail receive time, `:395` | arrival — neither axis exactly |
| `created`, `updated` | `System.currentTimeMillis()` at `:350`, written `:407-408` | **`assertedAt` AND `derivedAt`, conflated into one value** |

- `$$events-by-date` indexes `effectiveTime = startTime ?? deadline`
  (`FamilySchemaModule.java:72-78`, `:299-303`) — so the date index is already an **eventTime**
  index. That axis choice is correct and needs no change.
- `$$commitments.createdAt` is fed from the event record's `created`
  (`FamilySchemaModule.java:276`, `:364`) — commitment creation time = event parse time.

**`eventTimePrecision` and `eventTimeSource` do not exist.** Nothing records that `"2026-06-20"`
was a date-only extraction versus an exact timestamp — even though `parseIsoToEpoch`
(`:441-458`) *knows which of three formats it matched* and discards that at `:452`. That is a
concrete, nearly free place to capture precision.

### Q3. Where do commitment status transitions get appended, and what do they carry?

**One production append site:** `WebhookReceiver.markDone` (`:198-205`).

```java
change.put("familyId", familyId);
change.put("commitmentId", commitmentId);
change.put("newStatus", "DONE");
change.put("changedAt", System.currentTimeMillis());
```

Consumed by `FamilySchemaModule.java:429-440`, which writes `status` and `updatedAt` onto
`$$commitments`, guarded by an existence `localSelect` (`:435-436`).

**No `actor`. No `actorBasis`.**

**Correction worth recording:** `FamilySchemaModule.java:415` comments that *"actor is durably
captured in this depot's own replay log but not projected into `$$commitments` this session."*
That is **factually wrong against current code** — `grep` confirms `actor` appears nowhere in
`src/` except that comment. The depot payload has no actor field. The comment misdescribes the
code and should be corrected whether or not §6's actor work proceeds.

Also: `markDone` only ever writes `DONE`. Five states are documented as a closed set, but `DONE`
is the only transition reachable from code today.

---

## THREE PLATFORM-LEVEL CONFLICTS FOUND (must be settled before Phase B)

**C1 — `edgeId = hash(subject, relation, object, assertedAt)` is not redrain-deterministic if
`assertedAt` is computed in the topology.** Gate 9 requires deterministic IDs, and Rama requires
determinism for PStates recomputed from depot data (the rule cited at
`FamilySchemaModule.java:85-87`, `operating-rama.html` "Task scaling"). If `assertedAt` comes
from `System.currentTimeMillis()` inside the stream topology, every redrain mints different
edgeIds and the supersession graph is destroyed.

**Resolution:** `assertedAt` must be stamped **once, into the depot payload, at append time**,
and read back out of the record by the topology — never computed inside it. Then `edgeId` is a
pure function of depot data and redrain reproduces it exactly. This generalizes: *every* new
timestamp must be stamped at append time, or redrain silently rewrites history.

**C2 — "held-as-default" and "trigger preservation" both require prompt changes, which collide
with B0's byte-identity gate.** B0 is "byte-identical or it has failed." Changing the classify
prompt to bias toward held, or the extract prompt to emit `triggerCondition`, are prompt edits.
They cannot ride on the same step. **They must be sequenced strictly after B0 verification
passes**, each as its own change — at which point the template hash changes on its own, which is
the mechanism working as designed.

**C3 — one `PROMPT_VERSION` for two prompts cannot express a single-prompt change.** Ties
directly into open question 1: if provenance goes per-node, each prompt gets its own hash and
this resolves itself.

---

## PHASE B — PROPOSED PLAN (not approved, not started)

### Step 0 — record the audit
0. Correct the stale `actor` comment at `FamilySchemaModule.java:415`. Comment-only, no
   behavior change. (Can be done independently of everything below.)

### Phase B0 — prompt extraction, byte-identical (prerequisite, gated)
1. Extract `classifyPrompt` to `private static final String CLASSIFY_PROMPT_TEMPLATE` with a
   placeholder for the body. No wording changes.
2. Extract `extractPrompt` to `private static final String EXTRACT_PROMPT_TEMPLATE` with
   placeholders for `today` and the body. No wording changes.
3. Add a `promptHash(String template)` helper — SHA-256, first 12 hex chars, computed once at
   class init.
4. **Verification gate:** a non-LLM test asserting the rendered template is **byte-identical** to
   the current inline concatenation, for a fixed set of inputs including a fixed `today` value.
   Run it. Report the result. Only then delete the old concatenation.
5. **STOP. Do not proceed to B1 until step 4 passes and is reported.**

### Phase B1 — provenance fields
6. Replace `PROMPT_VERSION = "v1"` with hash-derived constants (shape depends on **Fork 1**).
7. Add `derivedAt` (Long, epoch millis at the model call) and `sourceId` (String, `gmailMessageId`
   for this path).
8. Stamp inside the node that makes the model call (B2) — never in `GmailService`,
   `WebhookReceiver`, or any HTTP handler.
9. Represent the keyword-fallback case honestly (A1) — a record whose category came from
   `classifyByKeyword` must not claim a `modelId`.
10. Update `NonLlmPipelineTest`'s hardcoded `assertEquals("v1", ...)` (`:384`) — it will fail by
    construction once the value is a hash.

### Phase B2 — temporal axes (§1)
11. Add `assertedAt` (Long, never null) stamped at append time in `write-to-store`.
12. Keep `created`/`updated` untouched so `$$commitments.createdAt` and every existing test keep
    working. `assertedAt` is additive alongside them, not a rename. (Renaming would be a
    non-additive change and a Gate 1 fail.)
13. Add `eventTimePrecision` — derived for free from which branch of `parseIsoToEpoch` matched
    (`exact` / `day`), `unknown` when null.
14. Add `eventTimeSource` (`stated` | `extracted` | `inferred` | `absent`) — `extracted` for the
    LLM path, `absent` when null.

### Phase B3 — edge assertions (§2)
15. Declare a new **`$$edge-assertions`** PState in `FamilySchemaModule`, written by the existing
    `family-events-stream` (Gate 2: same topology that declares it):
    `familyId -> edgeId -> {subject, relation, object, objectId, assertedAt, supersededBy,
    supersededAt, retractedAt}`. **Not subindexed** (Gate 5).
16. `edgeId = nameUUIDFromBytes(subject|relation|object|assertedAt)` where `assertedAt` is read
    **from the event record** (per C1), never computed in the topology. Gate 9 satisfied.
17. Build the record via **sequential `localTransform` calls**, key by key — not one assembled
    `HashMap` + `termVal` (Gate 3; this PState *will* take later partial writes for
    `supersededBy`, which is exactly the trap).
18. `$$edges-forward` / `$$edges-inverse` stay exactly as they are — the O(1) live-lookup view.
    No change, no migration.
19. Supersession decided **in the topology** (§2.5), writing `supersededBy` onto the *old*
    record. Gated on relation cardinality — which requires **Fork 4**.

### Phase B4 — actor attribution (§6)
20. Add `actor` / `actorBasis` to the `*commitment-status-changes` depot payload and project them
    into `$$commitments` via the existing guarded branch.
21. Value supplied by the caller of `markDone` (see **Fork 5**), not decided inside the handler.

### Phase B5 — prompt-touching items (strictly after B0 verification, per C2)
22. Trigger preservation: extend the extract prompt to emit `triggerCondition` / `triggerType`;
    validate them the same never-guess way `parseRelations` does; add to the record.
23. Held-as-default: revise the classify prompt so ambiguous items stay held rather than being
    promoted. Prompt + topology rule only, no schema change.
24. Both naturally bump their template hash — the mechanism working as intended.

### Testing
25. Non-LLM `InProcessCluster` tests for: byte-identical templates (step 4), all provenance keys
    present and non-null on a derived record, `assertedAt` present, edge-assertion record shape
    and deterministic `edgeId` **under a simulated redrain** (re-append identical record, assert
    same edgeId, no duplicate assertion), actor round-trip through the status-change depot.
26. Follow the existing `CommitmentsTest` / `EdgesEntityIndexTest` pattern. Anything needing a
    live model call gets `@Tag("llm")` plus a non-LLM sibling exercising the helper directly.

---

## FORKS — Tor's decisions, not mine

**Fork 1 (brief open question 1) — per-record or per-call provenance?**
- **(a) One flat stamp per record.** What exists today. Simplest. Already cannot express the
  keyword-fallback case (A1), and becomes a lie the moment model routing arrives.
- **(b) Nested `derivations` map keyed by node name** —
  `{"classify": {modelId, promptVersion, derivedAt}, "extract-details": {...}}`. Plain
  `HashMap` of JDK types (Gate 4 clean), no new PState, no new depot name. Expresses the
  fallback honestly (`classify` can carry `modelId: null` / a fallback marker). Strict superset
  of (a).
- **(c) Separate `*derivations` depot + PState.** Fullest. But a depot name is a **permanent,
  irreversible decision** (`RAMA_VERIFIED_LEARNINGS.md:398`), and it is the most work.

**Recommendation: (b).** It is the only option that costs no permanent name while still being
able to say "this category came from keyword rules, not the model." Also resolves C3 for free.

**Fork 2 — `promptVersion` shape.** One hash per prompt template (falls out of Fork 1b), or one
combined hash over both. Recommend per-prompt.

**Fork 3 (brief B3) — backfill.** Currently **moot: zero records exist** (A6). Recommend
deciding it now anyway so the answer is on record before the first deploy: leave keys absent
(cheapest, and `null` round-trips cleanly per `RAMA_VERIFIED_LEARNINGS.md:225`) versus an
explicit `"unknown"` sentinel.

**Fork 4 — relation cardinality registry (§2.6).** Supersession cannot be decided without knowing
whether a relation is `one` or `many`. Options: a static map in `FamilySchemaModule`; a new
config depot like the existing `*weakness-leverage-config` pattern; or treat every relation as
`many` for now (no supersession fires, fields present but always null). The third is the
smallest thing that satisfies "the fields exist before real data."

**Fork 5 — where `actor` comes from.** `markDone` is edge code. Hardcoding `"user"` at the call
site is honest today and wrong the moment the agent calls it. Recommend `markDone` taking actor
as a parameter with the HTTP endpoint passing `"user"` / `"user-approved"` — caller identity is
transport-level, so this stays Gate 8 clean, but it is a fork because it changes a public
signature.

**Fork 6 — scope boundary I am respecting.** `*access-events` (§3) sits in §6's *"cheap to add
any time, but log from day one"* bucket, **not** the "must land before real data" list. Per your
scope instruction it is **out**. Flagging only because "log from day one" and "first deploy is
imminent" point the same direction — if you want it in, say so and it becomes a new depot name
decision (permanent, per `RAMA_VERIFIED_LEARNINGS.md:398`).

---

## GATE SELF-CHECK (`docs/PLAN_REVIEW_GATE.md`)

| Gate | Verdict | Note |
|---|---|---|
| 0 — Audit-first evidence | **PASS** | A1–A6 + Q1–Q3 above, with file:line and quoted code for every dependency. |
| 1 — Replay-safe & additive | **PASS** | All new fields/PStates additive. `created`/`updated` deliberately **not** renamed (step 12). No depot rewritten, no record deleted. |
| 2 — PState write ownership | **PASS** | `$$edge-assertions` declared and written by `family-events-stream` only. No cross-module or edge writes. |
| 3 — Record construction | **PASS** | Step 17 uses sequential `localTransform` precisely because `$$edge-assertions` takes later partial writes. Event-record additions are depot-payload keys, not PState partial writes. |
| 4 — Serialization boundary | **PASS** | `new HashMap<>()` / JDK types throughout; no `Map.of()`; no new `RamaSerializable` POJO at a persistence boundary. |
| 5 — PState read shape | **PASS** | Stated explicitly: `$$edge-assertions` **not subindexed**; `$$family-data` and `$$commitments` not subindexed; `$$events-by-date` is subindexed and is **not touched**. |
| 6 — Auto-vivify / unknown ID | **PASS** | Existing `localSelect`+`ifTrue` guard on the status-change branch is preserved; step 20 adds fields inside that guard, not around it. |
| 7 — Agent graph completeness | **PASS** | No new agent nodes; `finalize`→`result(...)` untouched. |
| 8 — Edge-code discipline | **PASS, with Fork 5 surfaced** | Stamping stays in the agent node (step 8). `markDone` carries caller identity only — transport-level, not a decision — but the signature change is a fork, not a silent call. |
| 9 — Deterministic IDs | **PASS, and this is C1** | `edgeId` derives `assertedAt` from depot data, never `currentTimeMillis()` in-topology. Flagged as the specific thing that makes this gate pass. |
| 10 — Test evidence & scope | **PASS, with one caveat** | Steps 25–26. Scope held to the brief + §6 must-land only; `*access-events` explicitly excluded (Fork 6). **Caveat:** the 152/152 non-LLM baseline is quoted from `CLAUDE_HANDOFF.md:667` and was **not re-verified this session** — the test run was declined. It should be established green before B0 begins. |
| Verify-at-source | **PARTIAL** | Determinism rule (C1) is sourced to `operating-rama.html` "Task scaling" via `FamilySchemaModule.java:85-87`; permanence of depot/PState names to `RAMA_VERIFIED_LEARNINGS.md:398` (verified against the docs 2026-07-26). **No novel API behavior is asserted** — the plan uses only patterns already working in this codebase. |

**Self-assessed verdict: PASS**, contingent on (a) Forks 1–5 being decided, and (b) the 152/152
baseline being re-established green before step 1.

---

## FORK 1 LOCKED → (b) nested `derivations` map (Tor, 2026-08-09) — gate answers

Baseline precondition from Gate 10 is now satisfied: **156 tests, 0 failures, 0 errors,
1 skipped, BUILD SUCCESS** (`env -u GEMINI_API_KEY mvn test`, 2026-08-09). The "152/152"
figure quoted in Gate 10 was stale — see `CLAUDE_HANDOFF.md`.

### Gate 3 — record construction: **PASS as-is, but the stated premise does not apply**

The gate asks that the nested map be built key-by-key via sequential `localTransform`
rather than assembled as one `HashMap` and written with `termVal`. **`derivations` is never
a PState partial-write target at all**, so neither construction style applies to it:

1. `EmailParsingModule` builds `eventRecord` (a plain `HashMap`) and calls
   `depot.append(eventRecord)` on `*family-events`.
2. `FamilySchemaModule.java:279` writes the **whole record** in one shot:
   `.localTransform("$$family-data", Path.key("*familyId").key("events").key("*eventId").termVal("*record"))`.

`derivations` is therefore one more key on a **depot payload** — the same shape as
`modelId`/`promptVersion` (2026-08-01) and `relations` (2026-07-15) before it.

The `termVal` trap fires only when a value is *both* assembled as one `Map` *and* later
targeted by a narrower write that navigates inside it. Re-verified 2026-08-09:

```
$ grep -rn 'localTransform("$$family-data"' src/main/java
src/main/java/com/family/assistant/schema/FamilySchemaModule.java:279
```

Exactly one writer. No later, narrower write navigates into a stored event record.
(Contrast `$$commitments`, which *does* take later partial writes for `status`/`updatedAt`
and correctly uses sequential `localTransform` calls for exactly that reason.)

Forcing sequential `localTransform` for `derivations` today would mean decomposing the
single whole-record write that every existing field depends on — strictly more risk, no
benefit.

**Invariant this verdict rests on, and which B1 must not break:** the moment anyone adds a
second, narrower write into `$$family-data` — e.g.
`Path.key(fam).key("events").key(id).key("derivations").key("classify")…` — the
whole-record `termVal` becomes the Gate 3 trap and must be decomposed *first*. Worth a
comment at `FamilySchemaModule.java:279` saying so.

### Gate 4 — serialization boundary: **PASS, conditional on construction discipline**

Two boundaries, both requiring plain JDK types:

1. **`ParsedEvent`** (`implements RamaSerializable`, crosses the agent-node boundary) gains
   `Map<String, Map<String, Object>> derivations`.
2. **`eventRecord`** (depot payload) gains `eventRecord.put("derivations", event.derivations)`.

Both the outer map and every inner map must be `new HashMap<>()` — **no `Map.of()`**, which
is the easy slip when writing a small fixed-shape fallback entry
(`RAMA_VERIFIED_LEARNINGS.md:142`). Precedent already working in the tree: `relations` is a
`List<Map<String,String>>` built with `new ArrayList<>()`/`new HashMap<>()` in
`parseRelations` and round-trips fine. Null values also round-trip cleanly
(`RAMA_VERIFIED_LEARNINGS.md:225`), so an absent/null `modelId` inside an entry is safe.

**Required test:** extend `NonLlmPipelineTest`'s existing serialization round-trip to assert
the nested `derivations` map survives — same treatment `relations` already gets.

### Gate 5 — PState read shape: **PASS, `$$family-data` is NOT subindexed**

Declared at `FamilySchemaModule.java:166-169` as three nested `mapSchema`s with no
`.subindexed()`. Verified 2026-08-09 that the **only** `.subindexed()` in the entire main
source tree is line 214, on `$$events-by-date`.

So `selectOne(Path.key(familyId).key("events").key(eventId))` returns a real `Map`, not a
`RocksDBWrapper`. Reading `derivations` back is plain nested key navigation — no
`sortedMapRange(MIN, MAX)` workaround needed.

### Gate 9 — deterministic `sourceId`: **PASS**

`sourceId` on the email path is `gmailMessageId` — Gmail's own stable identifier, already
carried `GmailMessage` → `ParsedEvent` → `eventRecord`. Not minted, not random, identical
across redrains. No `UUID.randomUUID()` anywhere near it.

Two conditions hold the gate:

1. When `gmailMessageId` is absent (any future non-Gmail source), `sourceId` must be
   **null/absent — never a fallback random UUID**. Precedent: the `$$raw-emails` branch
   already guards `isPresent("*rawId")` rather than inventing an id.
2. `derivedAt` must **not** feed any deterministic ID. It does not today, and B3's `edgeId`
   — the one place that would have consumed a timestamp — is deferred. Flagged because a
   timestamp leaking into an ID is exactly the C1 failure mode.

### Correction to B1 step 9 — "no fake `modelId`" is mis-specified

Step 9 says a record whose category came from `classifyByKeyword` "must not claim a
`modelId`". Reading the actual node (`EmailParsingModule.java:310-352`) shows that is not
what happens:

- The model **is always called** (`model.chat(classifyPrompt)`, line 316) — and billed.
- `classifyByKeyword` runs only *afterward*, and only when `category` came back `UNKNOWN`
  or the JSON failed to parse (lines 347-349).
- **`silo` and `intent` still come from the model** in that same path. Only `category` is
  overridden.

So `modelId: null` would erase two true facts: that the model ran, and that it produced
silo/intent. That is a different lie from the one step 9 set out to prevent.

**Recommended shape** — a per-node `basis` field carrying the outcome, which is what Fork 1b
exists to make expressible:

```
derivations: {
  "classify":         {modelId, promptVersion: CLASSIFY_PROMPT_VERSION, derivedAt, basis},
  "extract-details":  {modelId, promptVersion: EXTRACT_PROMPT_VERSION,  derivedAt, basis}
}
```

with `basis` ∈ `model` | `keyword-fallback` (model answered UNKNOWN) |
`model-error-keyword-fallback` (the JSON parse threw). Closed value set, validated the same
never-guess way `parseRelations` validates relations.

### Note for B2 — `receivedAt` is NOT `assertedAt`

The go-live checklist's verify-at-source item asks to confirm `*raw-emails` captures raw
arrival time "and that B2 copies `assertedAt` from that raw record." The first half holds;
the second half would introduce an error.

`persist-raw` (`EmailParsingModule.java:282-303`) does capture `receivedAt` — *when Gmail
received the email*. That is an **external event time**, not the time this system came to
believe anything. Copying it into `assertedAt` would be wrong in exactly the case that is
about to happen: a backlog ingest of a July email on the go-live date would claim
`assertedAt` = July, when the system in fact asserted it in August. On a first-ingest of a
months-deep backlog that skew is the norm, not the edge case.

Three distinct times, to keep separate:

| field | meaning | where it comes from |
|---|---|---|
| `receivedAt` | Gmail received the email | already on the raw + event record |
| `assertedAt` | this system came to believe the derived claim | **stamp at append time**, like `created` |
| `startTime`/`deadline` | the event's own time | extracted from content |

`assertedAt` should be stamped in the parsing node exactly the way `created`/`updated`
already are (`eventRecord.put("created", now)`) — computed outside the topology, carried in
the depot payload. That satisfies C1: a `*family-events` redrain replays the stored payload
and reproduces the original value, while a genuine reparse-of-raw produces a new assertion
with a new `assertedAt`, which is correct.

The checklist's escalation clause ("if raw arrival time is NOT captured, `assertedAt` is not
reconstructable → escalate") does not fire, but note the gate passes for a different reason
than stated: **nothing** makes `assertedAt` reconstructable after the fact — that is
precisely why it must be stamped now rather than derived later.

`eventTimePrecision` (step 13) does fall out for free: `parseIsoToEpoch`
(`EmailParsingModule.java:509-524`) already branches `Instant.parse` → `LocalDateTime` →
`LocalDate` → null, which maps onto `exact` / `exact` / `day` / `unknown`.

---

## VERIFY-BEFORE-WIRING RESULTS (2026-08-09) — three checklist claims checked at source, two are WRONG

The go-live checklist asked for two of these to be confirmed before B1/B2 wiring. Both
confirmations came back negative. **B1 must not be written to the checklist's stated shape.**

### ❌ CONFLICT 1 — `classifyByKeyword` DOES fire on an in-schema `UNKNOWN`

Checklist claim: *"`classifyByKeyword` fires only on **off-schema** model output; `UNKNOWN`
is IN-schema and does NOT trigger it. Set `basis = "keyword"` ONLY on the off-schema
fallback, `"model"` otherwise (including a legitimate `UNKNOWN`)."*

Actual code, `EmailParsingModule.java:317-349`:

```java
String categoryStr = "UNKNOWN";                       // 318 — sentinel default
try {
    String cat = parsed.get("category");
    if (cat != null && cat.toUpperCase()
            .matches("SCHOOL_EVENT|DEADLINE|PERMISSION_SLIP|TASK|UNKNOWN")) {
        categoryStr = cat.toUpperCase();              // 331 — UNKNOWN IS in this enum
    }
} catch (Exception e) { /* keep defaults */ }

if ("UNKNOWN".equals(categoryStr)) {                  // 347 — fires for BOTH cases
    categoryStr = classifyByKeyword(message.body);
}
```

`UNKNOWN` is a member of the accepted enum, so a legitimate in-schema `UNKNOWN` assigns
`categoryStr = "UNKNOWN"` and then **satisfies the line-347 condition**. The keyword
classifier runs.

**Root cause: the sentinel default and the valid value are the same string.** All four
paths — model said `UNKNOWN`, model said something off-schema, `category` field missing,
JSON parse threw — collapse to the identical `"UNKNOWN"`, so the current code **cannot
distinguish them at line 347**. The information the checklist wants `basis` to carry does
not exist in the code yet; B1 has to create it.

**Implication for B1:** tracking must be added *alongside* the existing control flow, which
must not change (behavior-preserving, same discipline as B0). Two closed-set fields express
it honestly, because "what the model did" and "what decided the category" are different
questions:

| field | values | meaning |
|---|---|---|
| `outcome` | `ok` \| `off-schema` \| `parse-error` | what happened to the model's classify response |
| `categoryBasis` | `model` \| `keyword` \| `none` | who actually decided the final category |

`categoryBasis` is computed by comparing the category before and after line 347 — the only
honest way to know whether the keyword rule actually changed anything. Note `none` is a real
case: off-schema output *and* no keyword match leaves `UNKNOWN` with nothing having decided
it. Collapsing that into `keyword` would claim a decision that was never made.

### ❌ CONFLICT 2 — `created` is NOT recomputed on redrain; mirroring it is SAFE

Checklist claim: *"Do NOT blindly 'mirror `created`': `created` is recomputed-on-redrain per
our own Gate 1 note. If `assertedAt` copied that mechanism it would drift on every redrain."*

`created` is computed at `EmailParsingModule.java:418` — `long now = System.currentTimeMillis();`
— which sits **inside the `write-to-store` agent node**, not inside a topology. It is stamped
into the payload (`eventRecord.put("created", now)`) *before* `depot.append(eventRecord)`.

That is precisely the append-time-stamping discipline C1 demands, and
`RAMA_VERIFIED_LEARNINGS.md` (Gate 9 entry, added 2026-08-03) already states the rule it
satisfies: a value stamped into the depot payload survives redrain unchanged; only values
computed *inside the topology* get rewritten. `FamilySchemaModule` confirms it consumes
`created` read-only — `.select("*record", Path.key("created")).out("*eventCreatedAt")` — it
never recomputes it.

**So `created` is already write-once and redrain-stable, and `assertedAt` should use exactly
the same mechanism.** The warning inverted the risk.

**The distinction the warning was probably reaching for** is real but is a different
operation: re-running the *parser* over `*raw-emails` (reparse-on-replay) does re-execute the
agent node and would produce a new `created`/`assertedAt`. That is correct behavior — a
genuinely new assertion made at a new time — and is not what "redrain" means for
`*family-events`, where the stored payload is replayed verbatim.

### ✅ CONFLICT 3 (minor) — the `llm` exclusion the checklist proposed was already in place

Checklist recommendation: *"Durable fix: exclude the `llm` tag from the default Surefire run
(`<excludedGroups>llm</excludedGroups>`)."*

Already present since before this session: `pom.xml:16` set `excluded.groups` to `llm` and
`:121` wired it to `<excludedGroups>`. So `EmailIngestionTest`, `FamilyAssistantTest`,
`QueryAgentTest` and `CohenFamilyDatasetTest`'s tagged methods were **never** running by
default — which also corrects an overstatement in my own previous summary that named them as
live spend.

**The actual gap was one test:** `GmailIngestionTest` is `@Tag("gmail")`, not `@Tag("llm")`,
so nothing excluded it. Fixed 2026-08-09 by widening the default to `llm,gmail`. Live spend
from a default `mvn test` is now zero, without depending on anyone remembering `env -u`.

---

**This file:** `/Users/toddkeelingfolder/CORSAIR/family_assistant/docs/decisions/PLAN_provenance_temporal.md`
