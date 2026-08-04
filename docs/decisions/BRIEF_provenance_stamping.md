# Claude Code Brief — Provenance Stamping on Derived Records

**Project:** Lumino / family-assistant
**Branch:** `feature/raw-ingestion-depot`
**Type:** Audit-first, additive, replay-safe
**Status:** Phase A not started

---

## Standing instruction

**Before implementing anything, write a plan as a numbered list and stop. Wait for my
approval before writing any code.**

Read `CLAUDE_HANDOFF.md`, `EDGE_CODE_RULES.md`, and `RAMA_VERIFIED_LEARNINGS.md` first.
Run the plan through `lumino-plan-review-gate` (SKILL.md) before presenting it.

Plan divergence = stop and report. Never silent substitution.

---

## Goal, one level up

Lumino's core bet is that the depot is an append-only archive and every downstream view
is recomputable. The payoff is being able to re-drain history when parsing logic or
models improve.

That payoff is **unmeasurable without provenance.** If a record does not say which model
and which prompt produced it, then after a model change there is no way to attribute a
regression, no way to compare old output against new on identical inputs, and no way to
answer "why did the system believe this in August."

This brief adds the minimum fields that make model comparison a measurement instead of an
argument. It is four fields. It is irreversible if skipped — records written without
provenance can never have it added retroactively, because the information is gone.

---

## Phase A — Audit only. No code.

Report findings in plain language before proposing any change.

### A1. Where does interpretation currently enter the system?

Trace the path from Gmail arrival to a record landing in `$$family-data`. Identify every
point where an LLM call produces a value that ends up persisted. Name the file, the agent
node, and the model call.

Expected from prior reading (verify, do not assume — the branch may have moved):
- `EmailParsingModule` node `classify` — one Gemini call
- `EmailParsingModule` node `extract-details` — a second Gemini call
- `write-to-store` builds `eventRecord` and appends to `*family-events`

**If there are two model calls, that is a design question, not a detail.** Report it.

### A2. What provenance already exists?

`ParsedEvent` has fields commented as "Provenance fields" (`senderEmail`, `senderName`,
`emailSubject`, `gmailMessageId`). Note in your report that these are **arrival**
provenance — where the data came from — not **derivation** provenance — how it was
interpreted. Confirm whether any derivation provenance exists anywhere today.

### A3. Raw vs derived depot boundary on this branch

The branch is `feature/raw-ingestion-depot`. Report the current shape:
- Is there a raw depot upstream of `*family-events` now, or is `*family-events` still the
  first append?
- If both exist, state which one is the archive of record and which is derived.

This determines *where* the new fields belong. Arrival provenance belongs on the raw
append. Derivation provenance belongs on the derived append. Do not put both in one place
just because it is convenient.

### A4. Where do prompt strings live?

Currently the prompts appear to be inline string concatenation inside the agent node
lambdas. Confirm, and for each prompt report:

- Its full current text
- Which parts are static and which are interpolated at runtime
- Whether any prompt is built conditionally (different branches producing different
  strings) — if so, that is more than one template and needs more than one hash

This feeds B0 directly. `promptVersion` is a hash of the static template, so the
static/runtime split must be exact.

### A5. Schema impact

`$$family-data` leaf is `PState.mapSchema(String.class, Object.class)`. Confirm that
adding keys to the record map requires **no** PState schema change. If any index PState
or downstream reader iterates record keys and would break on unknown keys, name it.

### A6. Existing records

Count how many records already exist in `$$family-data` without provenance. Report the
number. Do not modify them.

**Stop here. Present A1–A6 in plain language. Wait.**

---

## Phase B — Implementation (only after Phase A is approved)

Do not plan Phase B until Phase A findings are reviewed. The design below is the
*intended shape*, not an instruction to build blind — Phase A may change it.

### B0. Prerequisite — extract prompts to hashable template constants

This is a **behavior-preserving refactor** and must be verified as such before B1 begins.

1. Move each inline prompt string out of its node lambda into a named
   `private static final String` template constant.
2. Runtime-varying values (today's date, email body, category) stay as **placeholders**
   in the template and are interpolated at call time. The constant itself must contain no
   runtime data.
3. `promptVersion` = short hash (SHA-256, first 8–12 hex chars) of the **template
   constant**, not the rendered prompt.

**Why the template and not the rendered string:** the `extract-details` prompt currently
interpolates `LocalDate.now()`. Hashing the rendered prompt would produce a new
`promptVersion` every calendar day, making the field noise. The template hash changes if
and only if the prompt logic changes — which is the entire point.

**Verification gate for B0 — non-negotiable:**

Before any provenance field is added, prove the extracted template renders **byte-identical**
output to the current inline concatenation for a fixed set of inputs. A refactor that
silently alters a prompt is a model-behavior change disguised as cleanup, and it would
land on the same commit as the mechanism meant to detect exactly that.

Write a test that asserts byte-equality between the old concatenation and the new
template render. Run it. Report the result. Then delete the old concatenation.

**Do not proceed to B1 until B0 verification passes.**

### B1. Fields to add to each derived record

Plain JDK types in a plain `HashMap`. No POJOs at the persistence boundary (Gate 4).

| Key | Type | Value |
|---|---|---|
| `modelId` | `String` | Exact model string, e.g. `gemini-2.5-flash` — read from the actual model config, not hardcoded a second time |
| `promptVersion` | `String` | Short hash of the prompt template constant from B0 — computed once at class init, not per call |
| `derivedAt` | `Long` | Epoch millis at the time of the model call |
| `sourceId` | `String` | `gmailMessageId` for the email path; the content hash for Brain Dump later |

### B2. Where the stamping happens

**Inside the agent node that makes the model call.** Not in `GmailService`, not in
`WebhookReceiver`, not in any HTTP handler. Stamping is derivation metadata and belongs
in the interior (Gate 8 / `EDGE_CODE_RULES.md`).

### B3. Backfill

Do **not** backfill existing records with guessed values. Records written before this
change genuinely have unknown provenance and should read as unknown. Propose either
leaving the keys absent or writing an explicit sentinel — this is a fork for me, not your
call.

### B4. Test

An InProcessCluster test that appends through the derived path and asserts all four keys
are present and non-null on the resulting record. Must run without `GEMINI_API_KEY` where
possible — if the path requires a live model call, tag it `@Tag("llm")` and add a
non-LLM sibling test that exercises the record-construction helper directly.

The goal every time is that IPC verifies it compiles and the topology drains correctly.

---

## Gate traps specific to this change

- **Gate 3 (record construction):** Adding keys to a map that is *depot-appended* is
  fine. Adding keys via `localTransform` with a whole-map `termVal` is the trap. State
  explicitly which one this change touches.
- **Gate 4 (serialization):** `new HashMap<>()`, JDK types only. No `Map.of()`. No
  `RamaSerializable` POJO at the depot boundary.
- **Gate 8 (edge code):** See B2.
- **Replay safety:** Adding keys to new appends must not break the topology's handling of
  old records that lack them. Confirm the stream topology tolerates absent keys.

---

## Open questions — my decisions, not yours. Surface, do not resolve.

1. **Per-record or per-call provenance?** If `classify` and `extract-details` are two
   separate model calls, one `modelId` on the record is a lie the moment model routing
   arrives. Options: one stamp per record (simplest, wrong later), a nested
   `derivations` map keyed by node name (accurate, more shape), or a separate
   `*derivations` depot (fullest, most work). Enumerate at least three and recommend one.
   Do not pick silently.

2. ~~How is `promptVersion` derived?~~ **DECIDED — see B0.** Short hash of the prompt
   template. No hand-bumped integers. Rationale: a version field that depends on a human
   remembering to bump it fails silently and corrupts the exact comparison this whole
   change exists to enable.

3. ~~Does prompt extraction belong in this change?~~ **DECIDED — yes, as Phase B0, an
   explicit prerequisite step with its own verification.** It is not folded in silently;
   it is named, sequenced first, and gated.

---

## Out of scope for this brief

Named so they do not creep in:

- Local model routing / Ollama
- Shadow-mode dual-model evaluation
- Content-addressed blob storage for Brain Dump media
- The corrections channel
- Any change to `$$family-data` PState schema declarations

Provenance stamping is the enabler for several of these. It is not the place to build
them.

Note: prompt extraction to constants (B0) **is** in scope and is sequenced first. Rewriting
or improving any prompt's wording is **not** — B0 is byte-identical or it has failed.
