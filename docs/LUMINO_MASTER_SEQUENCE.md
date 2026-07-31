# Lumino — Master Sequencing Roadmap

*Written 2026-07 to resolve confusion between two product tracks running in parallel
chats. This is the single reference for "what comes first." Lives in the repo so it
survives chat fragmentation.*

---

## The decisive fact: it's ONE codebase

Family Assistant / Personal Assistant and Brain Dump are **not two systems.** They
share:

- The same repo (`/Users/toddkeelingfolder/CORSAIR/family_assistant/`)
- The same six Rama modules
- The same depot and PState architecture
- The same commitments-shaped PState (distinguished only by a `source` tag)
- The same extraction pipeline pattern (classify → extract → materialize)
- The same standing docs: `CLAUDE_HANDOFF.md`, `REASONING.md`,
  `RAMA_VERIFIED_LEARNINGS.md`, `EDGE_CODE_RULES.md`, `PLAN_REVIEW_GATE.md`

**Therefore: do NOT split into a separate Claude project.** Two projects operating on
one codebase would produce diverging memory, conflicting handoff docs, and duplicated
learnings. The split is *conceptual* (two products), not *infrastructural*.

They are two **product surfaces on one backbone.**

---

## The unblocking insight

**The cloud deploy is not "the Family Assistant's next step." It is shared
infrastructure that both products require.**

This is the source of the confusion. It got discussed inside the Family Assistant
thread, so it feels like it belongs to that track. It doesn't — Brain Dump can't run
anywhere either until there's a working cluster.

**Current reality: nothing is running anywhere.** The Mac Mini deploy ran into real
memory pressure and was shut down cleanly; all state remains on disk. A cloud move is
still the direction, but **which** box is no longer a settled question — the previously
"decided" Hetzner CX53 (32GB, ~€29.99/mo) is sold out EU-wide, and the 32GB target it
was sized to came from ceiling arithmetic that has since been retracted. See
**"Measure before sizing"** at the end of this file. Both product tracks are blocked on
the same infrastructure, and that infrastructure is now blocked on one measurement.

**Updated 2026-07-30:** that measurement (Step 0) is **done — ~6GB idle.** The 24GB
figure is retracted against real numbers and consolidation is off the table for RAM
reasons. But ~6GB is an *idle* floor, so the box question is still open: it now waits on
**load testing under real ingestion (Step 0b)**, not on Step 0. The shape of the block
is unchanged — one measurement still gates both product tracks — but it is a different,
better-specified measurement than it was yesterday.

---

## The sequence

### Step 0 — Measure actual RSS ✅ **COMPLETE (2026-07-30)**

**Result: ~6GB measured idle footprint.** 23GB used with the cluster up, minus a
17GB clean floor taken with IntelliJ gone and Rama fully down. Sum-of-RSS reads
8.03GB but double-counts pages shared across nine JVMs on an identical
classpath, so **~6GB is the honest figure.**

**What it decided, and what it did not:**

- **The 24GB ceiling-arithmetic figure is retracted against measurement**, not
  just against reasoning. The ~9.5GB hypothesis was slightly conservative —
  right direction, right order of magnitude.
- **Box sizing is DEFERRED, not decided.** ~6GB is an *idle* number: no app
  running, no ingestion, no LLM calls, no depot appends. It is a floor for the
  cluster, not a working figure. **Do not provision against it.** Sizing waits
  on load testing, which is gated on OAuth re-auth and the Gemini cost gate.
- **Consolidation is OFF THE TABLE for RAM reasons.** ~6GB against a 24GB Mini
  is not a squeeze. The three-module split stays pre-audited in `REASONING.md`
  (2026-07-29) as a fallback if loaded numbers ever say otherwise, but there is
  no memory case for starting that refactor. Do not start it.
- **RocksDB's block cache is per WORKER, resolved as a side effect** exactly as
  this step predicted. `FamilySchemaModule` (15 PStates) settled at 865.6MB —
  *smaller* than `DigestModule` (0 PStates) at 1011.3MB. The ~3.8GB unknown that
  could have invalidated the sizing does not exist.
- **`-Xmx` tuning is demoted to a near-worthless RAM lever**: a 2.67× ceiling
  difference produced 12.6% more RSS (1536m → 956.1MB vs 4096m → 1076.2MB).
- **`conductor.child.opts` is promoted to the highest-value untried lever.** The
  three daemons cost **2.33GB — ~39% of the idle total — before any module
  loads.** Unlike worker `-Xmx`, it needs no redeploy.

**One caveat that must travel with the number:** the floor retained 1.78GB of
non-Rama compressor state, so the subtraction is slightly generous to Rama.

**A methodological finding worth carrying forward:** the first attempt was
contaminated and would have been wrong in the dangerous direction. Compressed
pages stay compressed, so RSS read from processes that lived through a pressure
event understates by ~2× — ZooKeeper read 413.4MB contaminated vs 812.8MB clean.
The shut-down → verify-settled → restart → plateau procedure is recorded in
`RAMA_VERIFIED_LEARNINGS.md` ("Measurement contamination"). Any future
measurement follows it.

**➡️ Current state, and what to do next, live in `CLAUDE_HANDOFF.md`.** Findings
are in `RAMA_VERIFIED_LEARNINGS.md` (four verified entries); reasoning and
caveats in `REASONING.md` (2026-07-30).

<details>
<summary>Original Step 0 specification (retained — it produced the right experiment)</summary>

Added 2026-07-29. **This is now the first action, ahead of provisioning anything.**
Its entire purpose is to replace an estimate with data, because no one has ever
recorded resident memory for a single Rama worker in this project.

Start ZooKeeper + Conductor + Supervisor and all six modules on the Mac Mini — state
survived the last cold restart intact, so this does not require a redeploy. **Let it
idle 5–10 minutes** so JIT, metaspace, and caches settle and the numbers aren't
launch-transient. Then record:

- **RSS per worker**, all six, mapped to module via the
  `rpl.rama.distributed.daemon.worker <port> <ModuleName>` argv on each command line,
  noting the launched `-Xmx` and `-XX:MaxDirectMemorySize` alongside.
- **RSS per daemon** — Conductor, Supervisor, ZooKeeper.
- **System total** — free pages, compressor size, swap used, load average. Capture the
  same fields as the 2026-07-19 failure (73MB free, 10GB compressor, load 6.2) so the
  two sessions are directly comparable.

Then **apply the two free levers and re-measure**: `worker.max.direct.memory.size`
(Rama defaults it to 500m *per worker*, passed as `-XX:MaxDirectMemorySize=500m` on top
of `-Xmx`, and never set in `worker-heap-overrides.yaml`) and `conductor.child.opts`
(defaults to `-Xmx1024m` for a pure coordination process; try 512m).

Watch `FamilySchemaModule` (15 PStates) against `DigestModule` (0) specifically — that
one comparison resolves RocksDB's undocumented block-cache scope for free. Full
procedure, including the redeploy-risk caveat and the two verification traps, is in
`CLAUDE_HANDOFF.md`.

**Outcome determines the box size, the per-module heap plan, and whether module
consolidation is needed at all.** Nothing gets provisioned or refactored before it.

</details>

---

### Step 0b — Load testing under real ingestion (NEW first action)
**This is now what everything is blocked behind.** Step 0 produced an idle floor;
a box cannot be sized from it. This step produces the working figure.

Gated on two things that are real gates, not formalities: **OAuth re-auth**
(browser consent, Tor's hands, cannot be automated) and the **Gemini cost gate**
(backlog count, then Tor's explicit sign-off on projected spend). Both are why
this is its own session rather than a continuation.

Measure the same fields as Step 0 — per-worker RSS, per-daemon RSS, system
totals — but under load and sampled over time. **The delta between the ~6GB idle
floor and the loaded peak is the number that sizes the box.** Start from a clean,
settled floor per the contamination procedure. Read the compressor and swap, not
free memory: low free is normal macOS behavior; the 2026-07-19 failure signature
was a 10GB compressor at load 6.2.

Full task spec in `CLAUDE_HANDOFF.md` ("Next Task", set 2026-07-30).

---

### Step 1 — Cloud deploy (SHARED — unblocks everything)
**Box choice: PENDING the loaded measurement (Step 0b), not Step 0.** Step 0's
~6GB is idle-only and must not be provisioned against. Provision → install Java 21 + Rama
release → `local.dir` on Linux disk → deploy six modules fresh with heap sized
deliberately from launch #1 → systemd auto-start.

On the box: the previously-approved Hetzner CX53 (32GB, ~€29.99/mo, IPv4) is **sold out
EU-wide** and not currently purchasable regardless. The 32GB target came from
**summing `-Xmx` ceilings**, which is retracted — see "Measure before sizing" below.
Step 0 supplies the real number; size from that.

Heap plan (per-module ceilings, still fine as ceilings): FamilySchema 4096m,
Query 3072m, EmailParsing 2048m, EmailIngestion / Gmail / Digest 1536m each. **These
values are retained.** What is retracted is *summing* them and treating the total as
committed memory — a `-Xmx` value caps a runaway; it does not reserve or predict a
footprint. Do not derive a box size from this list.

Carry-forward rules: `--configOverrides` is never inherited (re-pass on every deploy,
forever, or workers silently revert to 4096m). `moduleStatus: RUNNING` does not prove
a cutover — verify `appendTargetId` against the new instance ID.

**Nothing else proceeds until this is done — and this does not start until Step 0
does.**

---

### Step 2 — Finish Family Assistant ingestion (proves the backbone)
This is the "Part 3" that never happened: OAuth re-auth (browser consent, Tor's hands)
→ backlog count + Gemini cost gate (Tor's sign-off) → first real ingestion → verify
the loop on real email (open items appear, mark-done works).

**Why this comes before building Brain Dump, even though Brain Dump is the commercial
priority:**

1. **It's ~90% done.** The code is written, tested (145/145 green), and committed.
   It needs roughly one session, not a build project.
2. **It proves the extraction machinery works on real data.** Brain Dump uses the
   *same* pipeline — classify, extract, materialize to PStates, query. If email
   ingestion works end-to-end, that's proof the machinery Brain Dump depends on is
   sound. Building Brain Dump on proven ground is far easier than building it on
   untested ground.
3. **It gives Tor something working for himself immediately** — which feeds
   dogfooding and motivation.

---

### Step 3 — Brain Dump v1 (the commercial track)
Now build on a running, proven foundation. Order within this step:

1. **Capture endpoint** — `POST /webhooks/braindump`, shared-secret auth, edge-dumb
   (receive → tag `source: brain-dump` → append raw → ack). Text-first; images as
   fast-follow once object storage exists.
2. **iOS Shortcuts** — "Dump it" (record → transcribe → POST) and "Snap it" (camera →
   POST). Two taps from idea to captured. ~20 min in the Shortcuts app, no code.
3. **Extraction for captures** — entities, dates, recurring rules, action items into
   the shared PState with the source tag.
4. **Views** — query ("what's on my plate"), calendar (day/week/month/coming-up),
   and the focus/"one thing right now" immersion view.
5. **Minimal proactive surfacing** — "here's today," unprompted. Core value prop for
   the ADHD audience, not an optional extra.

Full detail: `BRAIN_DUMP_V1_PLAN.md`.

---

### Later / deliberately deferred
- Full Layer 3 proactive loop (beyond the minimal daily surface)
- External calendar-app sync (needs OAuth; internal views deliver ~80% of value)
- Scoped external read-access API (agents/partners querying Brain Dump)
- Family/Personal Assistant as a *separate commercial product* or premium tier —
  decided once Brain Dump has real users
- On-prem box (~$400 refurb, 32-64GB) — revisit in a few months; would solve voice
  latency completely and pay for itself vs. cloud in ~13 months
- LLM cost optimization (batch API, prompt caching, cheaper open models) — only
  matters at real user volume, not at one-user scale

---

## How to coordinate across chats

**The repo docs are the coordination mechanism, not chat organization.** That's what
they were built for.

- `CLAUDE_HANDOFF.md` — current state, what's next. **Every session reads it first
  and updates it before closing.**
- `REASONING.md` — decision log, including confusion notes.
- `RAMA_VERIFIED_LEARNINGS.md` — authoritative Rama facts.
- **This file** — the master sequence, so "what comes first" never lives only in a
  chat.

As long as every session reads and updates these, it doesn't matter which chat a
decision was made in. Chat fragmentation stops causing drift the moment the repo is
the source of truth.

**Practical chat discipline:** one chat per work stream is fine (infrastructure vs.
Brain Dump product vs. strategy), but each session still opens audit-first against
the repo docs. Never let a decision live only in a chat.

---

## Research-informed design influences (NOT in the critical path)

From prior sessions on memory/retrieval research and the Obsidian-vs-Rama analysis.
**None of this blocks the deploy or v1.** Recorded so it informs later design instead
of being rediscovered — and explicitly fenced off so interesting research doesn't
become another reason not to ship.

### Higher-order relations (Hypergraph RAG / RAG 3.0)
Current schema models **pairwise** relations (subject-relation-object triples as
forward/inverse PStates). Research shows pairwise structures can't represent
higher-order associations — "dinner with Sarah and Mike Thursday at Luigi's" is
genuinely one n-ary relation, not a bag of pairs.

**Does this paint us into a corner? No.** Append-only depot + deterministic replay
means edges can be re-materialized into a different structure later without data
loss. That's precisely what the architecture was built for. Note it; don't
pre-optimize for it.

### Confidence propagation / Chain of Evidence (HyCE-RAG / RAG 4.0)
The schema already carries a `confidence` field per extraction. The research insight
goes further: confidence should **propagate across the graph during retrieval** —
query-conditioned diffusion rather than nearest-neighbor vector search — to form
evidence chains for multi-hop answers. Relevant when query sophistication becomes the
bottleneck, i.e. well after v1.

### Memory as an active action space
Research direction: the LLM as an active *navigator* of memory rather than a passive
consumer of pre-selected retrieved chunks. Contrast with the current QueryModule
pattern (deterministic PState reads + set math, LLM only at parse time). The current
approach is deliberately chosen for cost and determinism — worth revisiting only if
query quality proves limiting, not on principle.

### "Rama as engine, open formats as vault" — the one with v1 implications
The strongest conclusion from the Obsidian comparison: for a multi-decade personal
archive, a proprietary runtime is a portability and abandonment risk. Rama should be
the *engine*; open, append-only, backed-up formats should be the *vault*.

**Why this touches Brain Dump v1 commercially:** users will accumulate years of
captures and will ask how to get their data out. Absent an answer, it's a liability;
present, it's a differentiator — especially against note apps that lock users in.
Doesn't need building in v1, but **export/portability should be a known commitment**,
not a surprise later.

### Falsifiability discipline
Prior build specs deliberately included acceptance criteria that could honestly show
the approach *loses* to a simpler baseline. Keep this habit: when Brain Dump's
extraction and resurfacing are built, define in advance what "this is actually better
than a plain notes app" would look like — and be willing to see the answer.

---

## One-line answer to "what do I do next"

*(Superseded 2026-07-30 — Step 0 is complete. Current answer below.)*

~~**Start the six modules on the Mini and measure actual RSS (Step 0).** Then provision a
box sized from that measurement and deploy. Everything else — both products — is
blocked behind those two, in that order.~~

**Get real ingestion running and measure RSS under load (Step 0b).** That needs OAuth
re-auth and the Gemini cost gate first. Step 0 measured ~6GB *idle*, which retracted the
24GB figure and took consolidation off the table, but an idle floor cannot size a box.
Then provision from the loaded number and deploy. Everything else — both products — is
still blocked behind those, in that order.

---

## Measure before sizing

*Added 2026-07-29, after a near-miss that would have cost 2.2× to 5× budget.*

**The rule: never size infrastructure — or refactor to fit infrastructure — from
configured limits, estimates, or extrapolation. Measure actual resident memory under
real conditions first.**

Today's near-miss: we came close to buying a **$152.99/mo** box, and then a
**$66.90/mo** one, to satisfy a 32GB requirement that came from arithmetic on `-Xmx`
ceilings rather than from measured usage. `FamilySchemaModule`'s worker crash log —
sitting in the repo the whole time — showed that worker committing **352MB of a 4096m
ceiling** and using **197MB** of that. Six ceilings summed to 24GB. Actual need looks
closer to **~9.5GB**. Nobody had ever recorded RSS for a single worker across the
entire Mac Mini deploy.

**A 40-minute measurement run is cheaper than a wrong box or a week of refactoring.**

This is the resource-sizing sibling of the existing verify-at-source and
destructive-ops rules: **don't act on a number nobody has observed.** Verify-at-source
says don't trust a claim you haven't read in the docs or the jar. Destructive-ops says
don't run a command whose blast radius you haven't checked. This one says don't buy
hardware, and don't restructure working code to fit hardware, against a figure you
haven't measured.

**Why the bad number survived as long as it did** — worth recording, because the
mechanism will recur. The Mini's memory pressure was *real and directly observed*
(73MB free, 10GB compressor, load average 6.2 against a 1.5 idle baseline). Something
genuinely ran out of room. So when the ceiling arithmetic produced "24GB is
undersized," it *agreed with a real symptom*, and that agreement felt like
confirmation. It wasn't: the symptom was real, the diagnosis of how much RAM would fix
it was never measured. **A plausible calculation that matches an observed symptom is
still not a measurement.**

### What is retracted, and what is not

Precision matters here, because the retraction is narrower than it first looks:

- **Retracted:** summing `-Xmx` values across workers and treating the total as
  committed or required memory. The "~24.25GB committed, ~7.75GB margin" figure that
  used to appear in Step 1 is gone for this reason, as is the 32GB box target derived
  from it.
- **Not retracted:** the per-module `-Xmx` values themselves. They remain sensible
  *ceilings* and are retained in Step 1. A ceiling caps a runaway; it does not reserve,
  commit, or predict a footprint.
- **Not retracted:** the observed memory pressure on the Mini, or the
  `UPDATE-PREPARE-HANDOVER` failure. Both were real and measured. Only the inference
  from them to "therefore 32GB" is withdrawn.
- **Separately true, and independent of all the above:** Hetzner CX/CAX Cost-Optimized
  instances are sold out EU-wide as of 2026-07-29 — not backordered, unavailable. The
  CX53 plan was unbuyable regardless of whether its sizing was right.
- **Never applied:** two free levers worth possibly ~1GB together —
  `worker.max.direct.memory.size` (defaults to 500m *per worker*, passed on top of
  `-Xmx`, never set in `worker-heap-overrides.yaml`) and `conductor.child.opts`
  (defaults to `-Xmx1024m` for a coordination process). Both are now in Step 0.

### One caveat, so this rule isn't misapplied

The ~9.5GB figure is itself an **extrapolation** from a single crash-log snapshot of a
single module taken under active memory pressure. By its own logic, this rule does not
license acting on ~9.5GB either. It is a hypothesis Step 0 tests, not a result.

One term could still move it materially: RocksDB's default **256MB block cache**, whose
scope is genuinely undocumented — per PState, per partition, or per worker. At
per-PState scope that is ~3.8GB for `FamilySchemaModule`'s 15 PStates alone, and it is
off-heap, so it appears in RSS but in none of the heap numbers quoted above. Logged in
`RAMA_VERIFIED_LEARNINGS.md`'s Unverified section; Step 0's
`FamilySchemaModule`-vs-`DigestModule` comparison resolves it as a side effect.

**RESOLVED 2026-07-30 — the cache is per WORKER, and the ~9.5GB hypothesis held.**
`FamilySchemaModule` (15 PStates) settled at 865.6MB, *smaller* than `DigestModule`
(0 PStates) at 1011.3MB. The ~3.8GB term does not exist. Measured total: ~6GB idle, so
the ~9.5GB extrapolation was slightly conservative — the right direction for a
hypothesis to be wrong in. Note what this vindicates and what it doesn't: the error in
the 2026-07-29 session was never the ~9.5GB estimate, it was **summing ceilings to
24GB**. The estimate was directionally sound; the arithmetic on top of it was not.

**And the rule caught itself one level down.** This section says don't act on a number
nobody has observed. Step 0's first reading *was* an observation — and still wrong,
because the instrument was contaminated: compressed pages stay compressed, understating
RSS by ~2×. **Before trusting a number, check the instrument, not just the arithmetic.**
Recorded in `RAMA_VERIFIED_LEARNINGS.md` ("Measurement contamination").

The rule's own logic still applies to what Step 0 produced: **~6GB is idle**, and this
section does not license buying a box on it either. That is what Step 0b is for.

*Source: `RAMA_VERIFIED_LEARNINGS.md` ("`-Xmx` is a ceiling, not a reservation"),
`REASONING.md` (2026-07-29), `CLAUDE_HANDOFF.md` (RSS measurement run).*