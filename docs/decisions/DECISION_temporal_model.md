# Decision — Temporal Model and Relation Edge Assertions

**Project:** Lumino / family-assistant
**Status:** DECIDED. Settle now, while the relation-edge schema is still in motion.
**Depends on:** nothing. **Blocks:** graph schema evolution, Brain Dump, Layer 3.
**Companion to:** `BRIEF_provenance_stamping.md`

---

## Why now

Relation edges (`subject-relation-object` triples as forward/inverse PStates) are
mid-build. Adding time and supersession to that schema now costs a few fields. Adding it
after real edges exist costs a migration over live data.

Everything below is additive and replay-safe. None of it requires deleting anything.

---

## 1. Four time axes. They are not interchangeable.

The single most common modeling error here is collapsing these into one `timestamp`.
Each answers a different question and each drives different behavior.

| Field | Question it answers | Known with certainty? |
|---|---|---|
| `eventTime` | When did this happen in the world? | **No — usually a claim** |
| `assertedAt` | When did the system come to believe it? | **Yes — a fact** |
| `derivedAt` | When was this interpretation produced? | Yes (see provenance brief) |
| `accessedAt` | When did Tor last engage with it? | Yes — but an event list, not a field |

### The distinction that matters most

`assertedAt` is a **fact**. The system knows exactly when the append happened.

`eventTime` is a **claim**. It is usually extracted by a model from a document — the draw
date on a lab PDF, the publication date on a paper, "next Thursday" in an email. It can
be wrong, ambiguous, or absent.

**Therefore `eventTime` carries provenance and `assertedAt` does not.** Model this
honestly:

```
eventTime            : Long   (epoch millis, nullable)
eventTimePrecision   : String ("exact" | "day" | "month" | "year" | "unknown")
eventTimeSource      : String ("stated" | "extracted" | "inferred" | "absent")
assertedAt           : Long   (epoch millis, never null)
```

`eventTimePrecision` is not fussiness. A paper dated "2019" and a blood draw dated
2019-03-14T08:22 are both `eventTime`, and a query that treats them as equally precise
will produce confident nonsense in a trend line.

### The bloodwork case, worked

- Blood drawn 2025-11-04 → `eventTime`
- PDF ingested 2026-08-03 → `assertedAt`
- Nine months apart

Sort a panel by `assertedAt` and the trend is the order you happened to upload files in.
Sort by `eventTime` and it is the actual sequence. Both must be stored; queries choose
explicitly. **A query that does not state which axis it sorts on is a bug.**

Backfilling old results is normal and must not corrupt anything: an append in August
carrying a November `eventTime` slots into the series correctly and the depot still
records that you learned it in August.

### Age of content is not age of relevance

A 2003 paper read last week is live. A note from yesterday never reopened may be cold —
or may be avoided. `eventTime` says when something happened in the world; it says nothing
about whether it matters now. Salience (section 4) never keys off `eventTime`.

---

## 2. Relation edges — DECIDED

### 2.1 Edges are assertions, not facts

An edge does not say "Todd prefers X." It says "on 2026-08-03, from source S, the system
came to believe Todd prefers X."

This makes belief change expressible without mutation. Mutating an edge is the same error
as mutating the depot.

### 2.2 Edge identity includes `assertedAt`

**Decision:** `edgeId = hash(subject, relation, object, assertedAt)`

Not `hash(subject, relation, object)`. If assertion time is excluded, re-asserting the
same triple later collides with the earlier one and the history is destroyed at exactly
the moment it becomes interesting.

Deterministic, not random — consistent with existing commitment ID discipline.

### 2.3 The index PStates hold the live edge, the depot holds all of them

- Depot: every assertion, forever, one record each.
- Forward/inverse PStates: for each `(subject, relation)`, the **currently-live** edgeId.

Lookup stays O(1). History stays complete. The PState is a view and can be rebuilt from
the depot at any time — which is the whole architecture in one sentence.

### 2.4 Supersession is a pointer, not a deletion

Fields on each edge assertion:

```
supersededBy   : String  (edgeId of the assertion that replaced it, or null)
supersededAt   : Long    (null while live)
retractedAt    : Long    (null unless withdrawn without replacement)
```

Three distinct end-states, do not collapse them:

- **Live** — no supersession, no retraction.
- **Superseded** — a later assertion replaced it. *Was* true, now something else is.
- **Retracted** — withdrawn with nothing replacing it. It was wrong. Different from
  superseded and a correction event must be able to say which.

### 2.5 Supersession is decided in a topology, never at the edge

When a new assertion arrives for an existing `(subject, relation)`, the topology decides
whether it supersedes. That decision is a write to the *old* record's `supersededBy` and
a PState index update — not an edit to the new one.

Per `EDGE_CODE_RULES.md`: no ingestion code inspects existing edges before appending.

### 2.6 Cardinality is a property of the relation

`prefers` may hold many objects at once. `currentEmployer` holds one. Whether a new
assertion supersedes or coexists depends on the relation type, so the relation registry
must declare cardinality (`one` | `many`). Without it the topology has to guess, and it
will guess wrong on the first relation that breaks the pattern.

---

## 3. Access events — start logging before you need them

**Not yet built. Log first, score later.**

The scoring function can be written any time. The access history cannot be reconstructed
retroactively. This is the same shape as provenance stamping: cheap now, impossible later.

Append `*access-events` on every: digest render, search result surfaced, commitment shown,
Brain Dump item opened.

```
targetId    : String
targetType  : String
accessKind  : String  ("surfaced" | "opened" | "acted-on" | "dismissed")
accessedAt  : Long
```

`accessKind` matters: something surfaced and ignored is weaker evidence of relevance than
something opened, and *dismissed* is negative evidence. One undifferentiated "access"
throws that signal away.

---

## 4. Salience — state-based, not decay-based

**Revised after review. The earlier ACT-R-based recommendation is retracted for
actionable items.**

### Why decay is wrong here

ACT-R base-level activation assumes non-access indicates declining relevance. That
assumption does not hold for this user or this product:

- **Non-access is ambiguous.** An item avoided because it felt overwhelming is often the
  *most* important item in the system, not the least. Decay would bury it.
- **Access is ambiguous in the other direction.** Opening a note frequently means it is
  consumed and resolved — nothing left to return to. That should lower salience, not
  raise it.

The same signal points both ways depending on the item. That makes it not a signal.

### What actually drives salience: open obligation

An item should surface because it has unresolved obligation attached, not because of
attention history. The commitment lifecycle already models this correctly.

**Salience v1:**

```
salience = f(commitment status, deadline proximity)
```

- Status ∈ OPEN / IN_PROGRESS / WAITING / DONE / DISMISSED
- Deadline proximity is deterministic arithmetic
- DONE and DISMISSED are cold by **state**, not by elapsed time

Deterministic, explainable, replay-safe, and largely already built. No new mechanism.

### Deadlines are not the only trigger form — preserve the when-clause

Half of real intentions are **event-cued, not time-cued**: "next time I talk to my son,
mention the insurance thing." Prospective-memory research is clear that this is the form
people naturally hold intentions in — and that time-based remembering is the kind humans
reliably fail at, which is why a deadline-only model quietly discards the intentions the
system is most needed for.

**The foundation requirement is at parse time, not in the salience function:** extraction
must preserve the trigger condition as data. If the parse keeps "mention insurance" and
drops "next time I talk to my son," the trigger is gone from the depot permanently — this
is a lossy-if-skipped item, same class as provenance.

```
triggerCondition : String  (raw text of the cue, e.g. "next call with Marcus"; null if none)
triggerType      : String  ("time" | "event" | "none")
```

Acting on event triggers (detecting "you're about to talk to Marcus") is Layer 3 and may
be far off. Storing them costs two nullable fields today. In the interim, a commitment
with an event trigger and no deadline is not invisible — it appears in retrieval and in
periodic review, it just doesn't escalate by clock.

### Access events are diagnostics in v1, not scoring inputs

Still log them (section 3) — the history cannot be reconstructed later. But their first
real use is **not** ranking. It is detecting the stuck-item pattern:

> Surfaced N times. Never opened, or opened and never acted on. Still OPEN.

This is not evidence of irrelevance. It is evidence of an item that is too vague, too
large, or being avoided — and surfacing *that* ("this has come up nine times and hasn't
moved; want to break it down?") is a core product behavior, not a scoring refinement.

`accessKind` earns its place here: **surfaced-but-not-opened** and
**opened-but-not-acted-on** are different failure modes and want different responses.

### Where decay may still belong

Possibly nowhere. See 4.2 — standing reference material does not decay either. Do not
build a decay mechanism until an item class is found that actually needs one.

---

## 4.1 Three item classes. Different rules. Dispatch on type.

There is no single salience function. Attempting one produces a model that is wrong for
two classes in order to be right for one. The classes are already partly encoded in
existing type tags — reuse them rather than inventing a parallel taxonomy.

### Class D — Held. The default class.

Worries, half-formed ideas, observations, feelings — most of what a real brain dump
contains at 2am. Not a commitment, not reference, not consequence-bearing.

- **The value is in the putting-down.** The mind releases what it trusts is held. This is
  the core psychological mechanism the product serves, and it only works if capture
  carries **no surfacing obligation**.
- **Held is the default.** Items must *earn* Class A status through a clear actionable
  signal; ambiguous items stay held. A classifier that eagerly promotes dumps into OPEN
  commitments turns every capture into a new obligation — the system becomes an anxiety
  amplifier, and the user starts avoiding the tool itself, which is Class C behavior
  aimed at the product.
- **Findable, never pushed.** Held items surface only through retrieval or contextual
  relevance (like Class B), never through the salience layer.
- Promotion from held to A is always possible later — it is an event, appended like any
  other. Demotion the other way is equally an event. Nothing about the class is
  permanent except the record of what it was believed to be, when.

### Class assignment errors are asymmetric

Missing a real commitment (A misfiled as D) costs a dropped ball. Promoting a stray
thought (D misfiled as A) costs trust in the whole system — one is an error, the other is
a reason to stop using the tool. When the classifier is unsure, **held wins.** Ambiguous
items can be batched into a periodic "anything here need to become a task?" review, which
converts classification uncertainty into a small human decision instead of a wrong
automatic one.

### Class A — Lifecycle items

Trips, appointments, permission slips, deadlines, commitments. They arrive, move through
states, complete, and go cold.

- **Salience:** commitment status + deadline proximity.
- **Cold by state, not by time.** DONE and DISMISSED drop out regardless of recency.
- Already built. No new mechanism.

### Transitions carry an actor

The work is joint: the agent drafts, organizes, and moves items through states alongside
the user. A transition event that does not record who caused it is ambiguous history —
and Layer 4 (autonomous actions graduating trust) is impossible without a clean record of
which actions were the agent's and how they went.

On every status-transition append:

```
actor       : String  ("user" | "agent" | "system")
actorBasis  : String  (for agent: "user-approved" | "autonomous" | "scheduled"; else null)
```

`system` covers deterministic transitions (deadline-passed expiry). `actorBasis`
distinguishes an agent action the user approved from one taken autonomously — the exact
line Layer 4 moves over time, which only means anything if the history of where it was is
kept.

### Class B — Standing reference

Bonnard's colour principles. A meditation text. A technique studied across years.

- **Never completes.** There is no DONE state; "finished reading it" is not the point.
- **Does not decay.** A principle returned to across a decade is not less relevant for
  being old.
- **Repeated return is a genuine positive signal** — here and only here, access frequency
  means affinity rather than ambiguity.
- **Salience is contextual, not temporal.** It should surface when working on a painting,
  not on a schedule. This is a *relevance-to-current-activity* problem, which is a
  retrieval question, not a ranking one.

Class B is the case that shows why decay was the wrong frame. Nothing about Bonnard gets
staler.

**Evolving reference is the same class at a different rate.** Fast-moving domains —
medical research, a framework's own docs — differ from Bonnard only in how often their
assertions get superseded. Section 2 already handles this: a finding enters as an
assertion (`assertedAt`, source), and a later finding supersedes it by pointer, old
belief kept visible. Stable reference is simply an assertion that never gets superseded.
No separate mechanism; what changes per domain is at most how actively the agent checks
for successors, which is Layer 3 behavior, not schema.

### Class C — Consequence-bearing, low-engagement

Tax documents. Estate paperwork. Insurance. Financial statements.

- **Engagement is inversely related to importance.** These are avoided *because* they are
  heavy, not because they are unimportant.
- **Importance must come from an external prior — the document category — never from
  behavior.** Any model that infers relevance from attention will rank these last, which
  is precisely backwards and is the single most consequential failure mode in this
  system.
- **Salience:** category prior + hard deadline (filing dates, renewals, expiries).
  Engagement-independent by design.

### Design constraints on Class C — read before building

The system supplies conscientiousness where attention doesn't. That is the point. It is
also one nudge away from being intolerable.

1. **Surface, do not scold.** No shame framing, no counters of how long something has
   been ignored presented back as judgement.
2. **Escalate by consequence, not by repetition.** A tax deadline in four days escalates
   because it is in four days. Nothing escalates merely for having been surfaced often.
3. **DISMISSED is honoured.** If it is dismissed, it is gone until the deadline changes
   or the underlying fact does. An override that ignores dismissal makes the system
   untrustworthy.
4. **Shrink the ask instead of repeating it.** Where a repeated non-response is detected,
   the correct response is a smaller next action ("open it and tell me the due date"),
   not a louder version of the same request.

### What access events are for, revised

Not scoring. Two diagnostic uses:

- **Class A:** stuck-item detection. Repeatedly surfaced, never moved, still OPEN.
- **Class C:** per-category avoidance patterns — which informs *how* something is
  presented, never *whether* it is.

`accessKind` distinguishing surfaced / opened / acted-on / dismissed is what makes both
possible.

### Open

Class assignment for ambiguous items — a research paper that is both standing reference
and attached to an active project. Likely a per-item property rather than derived purely
from type. Do not solve speculatively; revisit after real ingestion.

### Rules that survive unchanged

- Frame as **ranking under attention scarcity**, never as forgetting. No deletion, no
  archival tier.
- **Recompute, never decrement.** Any future scoring is a pure function of the event log
  and current time, computed on read or materialized from a topology — never mutable
  state decremented on a timer.

---

### The record cuts both ways

Everything above is obligation-shaped. But permanent state history also means the system
knows what **moved** — and "here's what actually got done this week" is real product
value for a user whose attention challenges come bundled with self-criticism about the
unfinished. Costs nothing: it is a query over status transitions already stored forever.
Note it so it is built as a view, not forgotten.

---

## 5. Open — not decided here

- Whether `eventTime` extraction gets its own confidence score separate from
  `eventTimePrecision`.
- Whether retraction can itself be superseded (changing your mind about a correction).
- Held→A promotion mechanics: automatic on strong signal, or always via the periodic
  review batch. Lean toward review-only at first; automation earns trust later.
- Event-trigger detection (Layer 3) — how the system knows a cue condition is occurring.

---

## 6. Sequencing — ingestion starts imminently

**Must land before real data:**

- Section 2 (relation edge assertion fields). Retrofitting supersession onto live edges
  is a migration; adding it now is a schema edit.
- Section 1 (`eventTime` / `assertedAt` split). Records written with one timestamp cannot
  be split apart later — the missing axis is genuinely gone.
- Provenance fields (`BRIEF_provenance_stamping.md`), same reasoning.
- **Trigger preservation** (`triggerCondition` / `triggerType`). A when-clause dropped at
  parse time is unrecoverable. Two nullable fields.
- **Actor attribution on transitions** (`actor` / `actorBasis`). Transitions start
  accruing with real data; who caused each one cannot be reconstructed. Layer 4 trust
  graduation depends on this history existing from the start.
- **Held-as-default classification policy.** Not a schema change — a rule for the parse
  prompt and topology: ambiguous items are held, not promoted. Cheap to state now,
  corrosive to discover after a month of over-eager commitment creation.

**Cheap to add any time, but log from day one:**

- Section 3 (`*access-events`). Trivial to write, impossible to reconstruct.

**Deliberately deferred until real data exists:**

- Section 4 scoring beyond commitment state + deadline.
- Any decay curve at all.
- Per-source-type parameters.

The first month of real use is the experiment. The schema decisions above exist so that
month is *recoverable* — every question asked in month three can be re-answered against
week-one data.
