# Rama / Agent-o-rama Verified Learnings

This file did not exist before 2026-07-03. It is seeded from constraints actually
verified against this project's real Rama 1.5.0 dependency and its actual Agent-o-rama
usage — not backfilled from memory of a prior version. Every entry below states its
verification source. If a doc or example conflicts with this file, this file wins,
because it's checked against our exact pinned version (`com.rpl:rama:1.5.0`,
`com.rpl:agent-o-rama:0.8.0` — see `pom.xml`), not the latest release.

Add new entries only after verifying against one of: (1) chat.redplanetlabs.com,
(2) redplanetlabs.com/docs, (3) direct inspection of the jar in
`~/.m2`/`/Volumes/CORSAIR/.m2` (`javap` against the actual class beats trusting docs
that may describe a newer version — current Rama is 1.8.0; we run 1.5.0), or (4) an
existing working pattern in this codebase. Keep verified and unverified items in
separate sections — never mix them.

---

## Verified

### Chat-o-rama scope limitation — what it will and won't answer
Status: VERIFIED — observed directly, 2026-07 (Rama 1.5.0 / AOR 0.8.0 era).

Chat-o-rama (chat.redplanetlabs.com) remains the first stop for Rama/AOR API questions, but
it has a hard boundary. Verbatim refusal received:

> "This question is specifically about Rama's dataflow language and Java dataflow API
> behavior. I'm not allowed to write or infer dataflow/topology behavior beyond what's in the
> official docs, and I can't provide or reason about concrete dataflow code examples
> (including how particular bindings like *result behave, or which predicate you should use in
> a given topology)."

**What Chat-o-rama WILL answer (use it here — it's authoritative)**
- Operational / cluster questions: deploy actions, CLI flags, config keys, `--configOverrides`
  semantics, parallelism rules, module lifecycle, licensing.
- Documented behavior quoted from official docs, with citations.
- "Does X exist / is X supported" questions about the documented API surface.
- Version-pinned facts (e.g. task count fixed at launch; update accepts no parallelism flags).

**What Chat-o-rama WILL NOT answer (do not expect it — find another source)**
- Dataflow language semantics beyond literal doc text.
- How specific bindings behave at runtime (e.g. `*result`, anonymous vars, scoping).
- Which predicate/operation to use in a given topology — i.e. design questions.
- Concrete dataflow/topology code examples.
- Inferred or reasoned-out behavior not explicitly stated in the docs.

**Fallback order when Chat-o-rama declines**
REVISED 2026-07-26 — jar extraction promoted above official docs; see "A cited doc quote is not
verification" below for the evidence that forced the reorder.
1. **Direct jar inspection** — `javap` against the actual class in `~/.m2`, or `unzip` the jar and
   read the `.java` sources it ships (the `agent-o-rama` jar ships all 78 of them; the `rama` jar
   ships classes only, so `javap` there). This is the highest authority for anything about the API
   surface: it is our exact pinned version, it cannot describe a release we don't run, and it
   cannot be stale. Prefer it over any doc quote, from Chat-o-rama or otherwise.
2. Official RPL docs directly — fetch and read the relevant page, don't ask about it. Quote the
   doc text. Authoritative for *behavior* the jar can't show (trimming cadence, migration
   semantics, cutover rules), but subordinate to the jar on *what exists and what its signature
   is*.
3. rama-examples repo — with the caveat it targets older Rama (0.11.4), so patterns may be
   stale; treat as a hint, not a fact.
4. Empirical test in InProcessCluster — the authoritative answer for runtime binding/dataflow
   behavior. Write the smallest test that isolates the question, observe, and record the result
   HERE as a new verified learning.

**A cited doc quote is not verification.** Verified 2026-07-26 (depot-lifecycle audit session). A
citation proves a doc *says* something; it does not prove the claim is true of our pinned version,
and it does not prove the claim is complete. Three dictated "doc-sourced" depot facts were checked
against `redplanetlabs.com/docs/~/depots.html` and `operating-rama.html` that session: one was
confirmed verbatim, and two were contradicted by the very pages they were attributed to — a claim
that no depot retention policy exists (the docs document depot trimming, with four dynamic options),
and a claim that `migration` iterates the full depot (the docs say it "takes effect instantly
regardless of the size of the depot" and applies lazily on read). The failure mode is not that the
docs are wrong; it's that a plausible-sounding attribution to them had never actually been read back
against them. Read the page. Quote the sentence. If the sentence doesn't say it, the fact doesn't
go in this file.

Never guess and proceed. A dataflow assumption that "seems right" is exactly the class of error
that produces silent wrong behavior (cf. the `selectOne`/`RocksDBWrapper` and
`moduleStatus: RUNNING` traps).

**Practical consequence:** the standing rule "check Chat-o-rama before guessing any Rama API"
holds for operational and API-surface questions. For dataflow semantics and topology design,
Chat-o-rama is not a source — InProcessCluster empiricism is, and every answer found that way
must be written back into this file.

### `waitForStreamProcessedCount` does not exist
There is no such method for stream topologies. Source: `redplanetlabs.com/docs/~/testing.html`.
Stream topology processing synchronizes via `AckLevel` on the append call itself (see
next entry); a `waitForMicrobatchProcessedCount`-style method exists only for
**microbatch** topologies, which this project doesn't use in tests anyway (stream
topology only, per the entry below).

### `Depot.append` / `AckLevel` — signature confirmed directly against the 1.5.0 jar
Verified 2026-07-03 by decompiling `com.rpl.rama.Depot` and `com.rpl.rama.AckLevel`
from `/Volumes/CORSAIR/.m2/repository/com/rpl/rama/1.5.0/rama-1.5.0.jar` with `javap`
(not from docs, which may describe 1.8.0):

```java
public interface com.rpl.rama.Depot extends PartitionedObject, Closeable {
    Map<String, Object> append(Object data);
    Map<String, Object> append(Object data, AckLevel ackLevel);
    CompletableFuture<Map<String, Object>> appendAsync(Object data);
    CompletableFuture<Map<String, Object>> appendAsync(Object data, AckLevel ackLevel);
    ...
}

public final class com.rpl.rama.AckLevel extends Enum<AckLevel> {
    NONE, APPEND_ACK, ACK
}
```

Note the return type is `Map<String, Object>`, not `void`.

Behavioral semantics (per `redplanetlabs.com/docs/~/depots.html` — not independently
exercised by a passing test in this repo yet): `AckLevel.APPEND_ACK` returns once the
data is appended and replicated. `AckLevel.ACK` waits for that plus all **colocated**
stream topologies to finish processing, including PState replication. If there are no
colocated stream topologies, `ACK` behaves like `APPEND_ACK`.

### Mirror depots (cross-module) do not synchronize via AckLevel — poll instead
When one module appends to another module's depot via `getMirrorDepot(...)` (e.g.
`EmailParsingModule` appending to `FamilySchemaModule`'s `*family-events`), processing
is asynchronous regardless of ack level. Source: `redplanetlabs.com/docs/~/testing.html`,
which prescribes a condition-poll-with-timeout. This project already does this
correctly in `QueryAgentTest.java` (lines 66–72): loop on the target PState value with
`Thread.sleep(500)` up to a 30s deadline, not a fixed sleep and not
`waitForStreamProcessedCount`. Use this pattern for any new test that goes through a
mirror depot. `IndexPStateTest.java`'s flat `Thread.sleep(2000)` (same-module depot,
no mirror) is weaker than this and should not be copied for new mirror-depot tests.

### `topology.define()` is NOT required when extending `AgentModule`
Verified by cross-referencing `docs/Agent_O_Rama_Complete_Documentation.md` (lines
2176–2201 and 7203–7251) against this codebase. `topology.define()` is only needed
when you implement `RamaModule` directly and construct the agent topology manually via
`AgentTopology.create(setup, topologies)` inside your own `define(Setup, Topologies)`.
When extending `AgentModule` and overriding `defineAgents(AgentTopology topology)` —
the pattern used by every agent module in this project (`EmailParsingModule`,
`DigestModule`, `QueryModule`) — the framework invokes `.define()` for you; none of
these three modules call it explicitly, and all pass tests. Don't add an explicit
`.define()` call to an `AgentModule` subclass — it isn't part of that pattern.

### `new ArrayList<>()` / `new HashMap<>()`, never `List.of()`/`Arrays.asList()`, in agent nodes or depots
Confirmed as this codebase's actual convention by grep: `DigestModule` and
`QueryModule` use `new ArrayList<>()`/`new HashMap<>()` throughout their node bodies.
The only `List.of()` calls anywhere in the repo are in Gmail SDK builder calls
(`GmailIngestionModule`, `GmailWatchSetup`, `GmailService`) — external API arguments,
not agent nodes or depot payloads, so not a violation of this rule as scoped.

### Every agent node branch ends in `agentNode.result(...)`
Confirmed in the terminal `finalize` node of `EmailParsingModule`, `DigestModule`, and
`QueryModule` — all three call `agentNode.result(...)` and nothing else in that node.

### Index PStates live in the same module as their primary PState
Confirmed pattern in `FamilySchemaModule`: `$$events-by-child`, `$$events-by-category`,
`$$events-by-account`, and `$$events-by-date` are all declared and populated in the
same module that owns `$$family-data`, via the same stream topology off
`*family-events`.

### Subindexed PState gotcha
`selectOne(Path.key(familyId))` on a subindexed PState (e.g. `$$events-by-date`)
returns a raw `RocksDBWrapper`, not a `Map` — not usable directly. Use a wide range
query instead, e.g. `select(Path.key(familyId).sortedMapRange(Long.MIN_VALUE, Long.MAX_VALUE).mapVals().all())`.
Already documented in `CLAUDE_HANDOFF.md`; carried here as the canonical reference
going forward.

### `InProcessCluster` in tests only, `RamaClusterManager` in production
Confirmed: every test file in `src/test/java` uses `InProcessCluster.create()`.
Production (`FamilyAssistantApp`) uses `RamaClusterManager.open()` per
`CLAUDE_HANDOFF.md`'s Phase 2 migration notes. Do not mix the two.

### `Ops.EXPLODE` — fan-out one list into N downstream emits, verified against the 1.5.0 jar
Verified 2026-07-03 two ways: (1) `redplanetlabs.com/docs/~/intermediate-dataflow.html`
describes an `explode` operation that "emits one time for each element of the list"; (2)
decompiled `com.rpl.rama.ops.Ops` from the pinned
`/Volumes/CORSAIR/.m2/repository/com/rpl/rama/1.5.0/rama-1.5.0.jar` with `javap` and
confirmed `Ops.EXPLODE`, `Ops.EXPLODE_INDEXED`, `Ops.EXPLODE_MAP` all exist as
`NativeRamaOperation1<Object>` static fields — i.e. this is not a docs-only/newer-version
feature, it's present in our exact pinned dependency.

**Important:** `Block`'s own static factories only expose `explodeMaterialized(String)`
and `explodeMicrobatch(String)` — narrower, context-specific variants. The general
list-fan-out is NOT one of those; it's `Ops.EXPLODE` used through `Block`'s existing
`.each(RamaOperation1<T0>, Object)` overload (confirmed `NativeRamaOperation1<T0>
implements RamaOperation1<T0>`, so `Ops.EXPLODE` type-matches that overload), which
returns `Block$MultiOutImpl` — chain `.out(String...)` from there (confirmed on
`Block$Out`, inherited by `Block$OutImpl`/`Block$MultiOutImpl`). Verified call shape:

```java
.macro(Block.each(Ops.EXPLODE, "*tokenList").out("*token"))
```

Per the docs, this emits once per element of `*tokenList`, continuing the downstream
topology once per emission — the mechanism for one input record (e.g. one event) to
write N index entries (one per keyword token) in a single stream-topology pass. Now
exercised by passing tests as of 2026-07-05: `$$events-by-keyword` (`KeywordIndexTest`)
plus the `$$events-by-tag` / `$$events-by-person` fan-outs (`MultiValueIndexTest`).

### Multiple `Ops.EXPLODE` fan-outs in one topology → isolate with `anchor`/`hook`
Verified 2026-07-05 against `redplanetlabs.com/docs/~/intermediate-dataflow.html`: operations
after an EXPLODE run **once per emitted element** ("the subsequent code is executed for each
element emitted"). So chaining two-or-more list fan-outs on the same branch NESTS them —
`N` tags × `M` persons × `K` tokens. For inverted indexes whose values are **Sets** this does
NOT corrupt membership (Set dedup absorbs the redundant writes), but it is severe **write
amplification**: `N·M·K` writes instead of `N+M+K`. Contain each fan-out as an independent
branch off the same input node with `.anchor("name")` … `.hook("name")` (anchor labels a node;
hook reattaches the following ops to it; branches run in unspecified order, no cartesian
product). Verified call shape in `FamilySchemaModule`'s `family-events-stream`:

```java
.anchor("fanoutRoot")
.select("*record", Path.key("tags")).out("*tags")
.macro(Block.each(Ops.EXPLODE, "*tags").out("*tag"))
.localTransform("$$events-by-tag",
    Path.key("*familyId").key("*tag").nullToSet().voidSetElem().termVal("*eventId"))
.hook("fanoutRoot")
// … personId branch, then keyword branch, each hooking back to "fanoutRoot" …
```

`.anchor`/`.hook` confirmed present on the stream-topology chain (compiles + full suite green).
Exercised by `MultiValueIndexTest` — fan-out completeness, branch isolation (no field bleed
between the tag/person indexes), and all three branches firing for one record. Note: a
single-element-per-list test (like the existing `IndexPStateTest`) cannot catch a missed
containment because Set membership is identical either way — you need a multi-element record.

### Null map values round-trip through Rama serialization (1.5.0)
Verified 2026-07-05 by `MultiValueIndexTest.classifierOutputFieldsRoundTripAsNullOrEmpty`: a
record appended with `map.put("confidence", (Double) null)` (and null `reason`/`documentType`)
drains into `$$family-data` and reads back with `containsKey("confidence") == true` and
`get("confidence") == null`. So a plumbed-but-unpopulated field can be stored as an explicit
`null` value — no sentinel or key-omission needed. This is why the schema refactor defaults
`confidence`/`reason` to `null` rather than a `0.0`/"" sentinel.

### `agentNode.getAgentClient(String)` — same-module agent-to-agent invocation, verified against the 0.8.0 jar
Verified 2026-07-03 against `/Volumes/CORSAIR/.m2/repository/com/rpl/agent-o-rama/0.8.0/agent-o-rama-0.8.0.jar`
with `javap`: `com.rpl.agentorama.AgentNode` extends
`com.rpl.agentorama.impl.IFetchAgentClient`, which declares
`AgentClient getAgentClient(String)` — distinct from `getMirrorAgentClient(String, String)`
(cross-module, two-arg, already used by `EmailIngestionModule` → `EmailParsingModule`).
`getAgentClient` takes just the agent name because it resolves an agent defined in the
**same module's** topology. Cross-checked against
`docs/Agent_O_Rama_Complete_Documentation.md` (lines ~3688-3702, the `TextProcessor`/
`MainAgent` example, and ~3741-3750, `Factorial`'s self-recursive-call example) — same
method name and same one-arg same-module semantics as the decompiled interface. Not yet
exercised by a passing test in this repo; first real use is the planned `search-agent`
(inside `QueryModule`) being invoked from `query-agent`'s fetch-data node.

### `localSelect`/3-arg `ifTrue` read-then-conditional-write — verified against the 1.5.0 jar and a minimal InProcessCluster probe
Verified 2026-07-16 (Layer 2 Commitments, Fork 1 mechanism session) by decompiling
`com.rpl.rama.Block$Impl` with `javap`:
```java
public abstract Block$OutImpl localSelect(String pstateName, Path path);
public abstract Block$Impl ifTrue(Object predicate, Block thenBranch);
public abstract Block$Impl ifTrue(Object predicate, Block thenBranch, Block elseBranch);
```
`localSelect(String, Path)` is the read half — same `Block$OutImpl` shape as `.select(Object,
Path)`, chained with `.out("*var")` exactly like every existing `.select(...)` call in this
codebase. On a never-seen key it binds `null`; on an existing key it binds the current value
— confirmed empirically (not just from the signature) with a throwaway probe module outside
`src/`, deleted after this entry was written. The 3-arg `ifTrue(Object, Block, Block)`
overload (unused anywhere in this codebase before this session — every prior `.ifTrue(...)`
call is 2-arg) is the genuine if/else branch.

**Real failure mode the probe caught, with a confirmed fix:** building a record as one
assembled `java.util.HashMap` and writing it with a single whole-value
`Path.key(...).termVal(wholeMapObject)` works for the first write, but a **later, separate**
write that navigates one key deeper into that same stored value (e.g.
`Path.key(...).key("status").termVal(...)`) throws at runtime:
```
java.lang.ClassCastException: class java.util.HashMap cannot be cast to class
clojure.lang.Associative
	at com.rpl.ramaspecter.keypath_termvalRichNav.transform_STAR_(ramaspecter.cljc:5367)
```
Root cause: every existing PState write in this codebase before this session either replaces
a whole leaf value (`termVal` at the final path segment, never re-navigated by a later,
separate write) or appends into a `Set` — no prior code stored a raw Java `HashMap` as a
schema-declared nested-map level and then, in a different depot event, navigated one key
deeper into that same stored value. Rama's Specter-based path engine (`ramaspecter`) needs
the container at that point to be `clojure.lang.Associative` (a Clojure persistent map) to
`assoc` a single key into it — a plain `java.util.HashMap`, however schema-declared as
`Object`, doesn't satisfy that once it's already the thing sitting in the PState.

**Fix, now the standing rule for any PState value that will ever receive a later partial-field
write:** build the nested map key-by-key through sequential `.localTransform(...)` calls (one
per field), never `termVal` one assembled `Map` object as a stand-in for a schema-managed
level you intend to path into again later. Applied in `FamilySchemaModule`'s `$$commitments`
creation branch: `sourceEventId`/`objectId`/`createdAt`/`status` are each written by their own
`.localTransform(...)` call, which is exactly what makes the later, separate
`status`/`updatedAt` partial write (from the `*commitment-status-changes` branch) succeed
without clobbering the rest of the record. `$$entities`/`$$leverage-map`/`$$weakness-map`
still use whole-map `termVal` safely, because nothing ever partially updates them afterward —
this constraint only bites when a PState value is BOTH built as one assembled `Map` AND later
targeted by a different, narrower write.

### A PState can only be written by the ONE topology that declared it — multiple depots must share ONE topology via successive `.source(...)` calls
Verified 2026-07-16 (Layer 2 Commitments implementation session) the hard way first, then
confirmed against the docs. First attempt declared `$$commitments` in the existing
`family-events-stream` (`stream.pstate("$$commitments", ...)`) but consumed
`*commitment-status-changes` from a **separate** `topologies.stream(...)` object
(`commitment-status-changes-stream`) — every single append to that depot then failed at
runtime, 100% reproducible, with:
```
rpl.rama.distributed.exceptions.IllegalWriteException
[$$commitments, module FamilySchemaModule,
 :topology-id :family-events-stream, :curr-topology-id :commitment-status-changes-stream]
```
The exception's own payload names the PState's owning topology (`:topology-id`) versus the
topology that attempted the illegal write (`:curr-topology-id`) — a PState is scoped to
exactly one topology for writes, regardless of which topology declared it or how similar the
schemas are. Confirmed via `redplanetlabs.com/docs/~/stream.html`: a single `StreamTopology`
object can consume from **multiple depots** via successive `.source(...)` calls, and doing so
is the documented pattern for exactly this shape of problem — *"When consuming multiple
depots from a topology, it's typical for each source block to modify the same PStates in
different ways."* No partitioning-match requirement between the two depots is documented or
needed. **Fix:** `*commitment-status-changes` is consumed as a second `.source(...)` branch on
the SAME `stream` topology variable that declared `$$commitments` (`family-events-stream`),
not a separate topology object — every other PState in this module that's written from more
than one logical source (`$$leverage-map`/`$$weakness-map`, both routed by a `mapType`
discriminator) already followed this rule by accident, since they were always single-source
per topology; `$$commitments` is the first PState in this codebase genuinely fed by two
different depots, and is the first place this constraint became visible.

### `worker.child.opts` heap only reliably applies via `--configOverrides` per deploy, and it is NOT inherited between deploys — verified by actually doing it
**Confirmed 2026-07-19 (Part 3 restart session) by direct action, not just reading docs:**
`--configOverrides <file>.yaml` passed on `rama deploy --action update` (pointing at a small YAML
file containing `worker.child.opts: "-Xmx<value>"`) does reliably set the worker JVM heap — proven
twice, `FamilySchemaModule` and `EmailParsingModule` both redeployed with a
`worker-heap-overrides.yaml` containing `worker.child.opts: "-Xmx1536m"`, and both confirmed by
grepping the fresh `supervisor.log`'s `Launching process` line for the new worker instance: actual
launched command showed `-Xmx1536m`, not the prior `-Xmx4096m`. This grep-verify step is mandatory
after any heap change — trusting `moduleStatus: RUNNING` alone is not enough, since that only
confirms the module came up, not what heap it came up with.

Also confirmed via Chat-o-rama (chat.redplanetlabs.com, citing `rama-shared → "Operating Rama
clusters" → "Updating modules"`): `--action update` always requires `--jar` even for a pure config
change (no lighter config-only/reconfigure command exists), always performs a full module-instance
transition (new worker processes launched, old torn down), and **config overrides from a previous
deploy are never inherited** — every future `--action update` for a module must resupply
`--configOverrides` or that module's workers revert to whatever the fallback default resolves to.
Practically: each of this project's six modules needs its OWN `--configOverrides` redeploy any time
its heap should change, and this must be repeated on every future code-change redeploy too, not just
this one-time right-sizing pass.

**Open question, NOT resolved — see "Unverified" section below:** whether `rama.yaml`'s own
`worker.child.opts` can ALSO work as a cluster-wide default (the docs say it should; our observed
history says it hasn't, three separate times). Do not treat `rama.yaml`'s value as either reliably
inert or reliably authoritative until that's tested in isolation — `--configOverrides` is the only
mechanism verified end-to-end in this project so far.

### `rama moduleStatus` takes the short module name, not the fully-qualified class name
Verified 2026-07-19 (Part 2 cluster deploy session) the hard way: `rama moduleStatus
com.family.assistant.schema.FamilySchemaModule` returned `{"moduleState":"NOT_ALIVE", ...}`
even immediately after Conductor's own log confirmed `Launch of module FamilySchemaModule
complete!` / `module-state [running]`. Re-running as `rama moduleStatus FamilySchemaModule`
(short name — matches what Conductor's log itself calls the module throughout its state-machine
handlers) correctly returned `{"moduleState":"RUNNING", "appendTargetId":"...", ...}`. The
`--module` flag on `rama deploy` takes the fully-qualified class name (confirmed working:
`--module com.family.assistant.schema.FamilySchemaModule` launched successfully), but
`moduleStatus`/`moduleInstanceStatus` want the short name Conductor assigns internally. Don't
trust a `NOT_ALIVE` from `moduleStatus` as proof a module was never deployed without first
confirming you queried the short name — cross-check against `local.dir/conductor/jars/` contents
or the Conductor log directly if in doubt.

### `moduleStatus: RUNNING` does NOT prove a `--action update` actually cut over — check `appendTargetId` against the real new instance ID
Verified 2026-07-19 (Part 3 pre-restart session) the hard way, on real hardware under real memory
pressure: `FamilySchemaModule`'s `rama deploy --action update --configOverrides` (changing worker
heap from `-Xmx4096m` to `-Xmx1536m`) was initially reported as confirmed successful, based on (1)
`moduleStatus` returning `"moduleState":"RUNNING"` and (2) a `supervisor.log` grep showing a
`Launching process` line with the new `-Xmx1536m`. **Both checks passed and the conclusion was
still wrong.** The new worker instance (`ada41606-...`, port 3007) got stuck at the
`UPDATE-PREPARE-HANDOVER` state-machine stage (visible in its own `worker-3007.log`, which simply
stops emitting lines mid-sequence) and was killed by Supervisor's heartbeat watchdog 36 seconds
after launch (`supervisor.log`: `"Port 3007 heartbeat is no longer valid, moving to KILLING"`). The
module never stopped serving traffic throughout this — it just silently kept serving the OLD
instance (`55b3c805-...`, port 3001, still `-Xmx4096m`) — which is exactly why `moduleStatus`
legitimately said `RUNNING` the whole time without that being evidence the update took effect.

**The check that actually catches this:** compare `moduleStatus`'s `appendTargetId` (and
`readTargetId`) against the specific instance ID from the update's own launch log line (Supervisor
logs `:module-instance-id` at worker launch, e.g. `d-worker-supervision - Launching worker
{:port 3007, :module-name FamilySchemaModule, ...}` paired with the worker's own `Worker launch
start...` line naming `:module-instance-id`). If `appendTargetId` still matches the PRE-update
instance ID, the cutover never completed, regardless of what `moduleState` says or whether a
`Launching process` line with the right `-Xmx` briefly appeared in the log. A launch attempt is not
a successful cutover — only a matching serving instance ID proves that. `EmailParsingModule`'s
update, run immediately after by the same procedure, was re-checked this way and DID genuinely
succeed (`appendTargetId` matched its new instance, confirmed still alive and processing 19+
minutes later) — so this isn't a universal failure of the update mechanism, but a per-module risk
that scales with how much state a module owns and how loaded the machine is at the time.

**Correlated but unconfirmed:** this failure happened while system free RAM was at 73MB, load
average 6.2 (vs. an idle 1.5), and the memory compressor held 10GB — i.e. under real, active memory
pressure. `FamilySchemaModule` owns far more PState/depot surface (15 PStates + 3 depots) than any
other module in this project, so its handover likely has more RocksDB/task-state sync work to fit
inside Supervisor's ~30-second heartbeat window than a lighter module's does. Whether the RAM
pressure caused the timeout (slower sync → miss the window) or the two are merely coincidental was
NOT isolated with a controlled test — flagged as a real risk, not proven causation.

### Depot and PState names are permanent — an undeclared depot is DESTROYED on module update, partitions deleted from disk
VERIFIED FACT (Rama 1.5.0). Verified 2026-07-26 by fetching and reading
`redplanetlabs.com/docs/~/operating-rama.html` ("Updating modules") directly. Two verbatim
sentences, both from that page:

> "There's currently no way to rename a depot or PState in a module update."

> "Any depot or PState defined in the old module that's not defined in the new module is
> considered destroyed."

and, on the consequence:

> "Because removing a PState or depot is destructive – Rama will delete all partitions from the
> filesystems of worker nodes..."

**The two combine into a rule the docs never state in one sentence:** since a rename is impossible
and an omission is a destroy-with-disk-delete, a depot name is a permanent decision. There is no
safe "rename" path — the closest equivalent is declare-new + migrate-forward + drop-old, which
means a full re-drain into the new name and an accepted, irreversible deletion of the old
partitions. Typos, prefixes, and pluralization in a depot name are load-bearing forever. Get the
name right at `declareDepot` time.

This is not theoretical for us: `FamilySchemaModule` declares four depots (`*family-events`,
`*weakness-leverage-config`, `*raw-emails`, `*commitment-status-changes`,
`FamilySchemaModule.java:143-159`), and `*raw-emails` is the write-ahead log that makes any future
re-parse possible. Dropping it from the module definition — even accidentally, even for one
deploy — deletes the only copy of every raw email body from disk.

**Jar-level anchor for the migration API this rule interacts with** — verified 2026-07-26 by
`javap` against `~/.m2/repository/com/rpl/rama/1.5.0/rama-1.5.0.jar` (the rama jar ships no
`.java` sources, unlike the agent-o-rama jar):

```java
public interface com.rpl.rama.Depot$Declaration {
  Depot.Declaration global();
  Depot.Declaration migration(String, com.rpl.rama.ops.RamaFunction1<?, ?>);
}

public interface com.rpl.rama.RamaModule$Setup {
  Depot.Declaration declareDepot(String, com.rpl.rama.impl.NativeDepotPartitioning);
  <T extends Depot.Partitioning> Depot.Declaration declareDepot(String, Class<T>);
  ...
}
```

`Depot.Declaration` has exactly those two methods and no others — `global()` IS real (it was in
doubt), and `migration` is the only other thing you can attach to a depot declaration. Both
`declareDepot` overloads return the `Declaration`, which is why attaching a migration is a
fluent call on the `declareDepot(...)` result. The first overload takes the result of a `Depot`
static factory — `javap com.rpl.rama.Depot` confirms those are `random()`,
`hashBy(NativeRamaFunction1)`, `hashBy(Class<T extends RamaFunction1>)`, `hashBy(String)`, and
`disallow()`, all returning `NativeDepotPartitioning` (a marker interface, zero methods). The
second is for a custom partitioner passed as a `Class`, where `Depot.Partitioning<T>` declares
`int choosePartitionIndex(T, int)`.

Two consequences for this codebase. Our four calls use the `hashBy(String)` overload, whose
declared type parameter `<T extends RamaFunction1>` is vestigial — it appears nowhere in the
parameter list and is inferred to nothing at the call site, which is why the bare
`Depot.hashBy("familyId")` compiles clean. And all four calls discard the returned
`Depot.Declaration` (`FamilySchemaModule.java:143,144,149,159`), so we currently use neither
`global()` nor `migration(...)` anywhere in the project.

### `-Xmx` is a ceiling, not a reservation — worker heap arithmetic on `-Xmx` overstates real RAM need
Verified 2026-07-29 (RAM-reduction audit session) from `hs_err_pid19834.log` in the project root —
the crash log of the real `FamilySchemaModule` worker on port 3001, the module with the largest
PState/depot surface in this project (15 PStates + 4 depots). That worker was launched with
Rama's default `worker.child.opts`, confirmed verbatim from the crash log's own
`Command Line:` / `jvm_args:` lines:

```
-Xmx4096m -XX:MaxDirectMemorySize=500m ... rpl.rama.distributed.daemon.worker 3001 FamilySchemaModule
```

and its heap at crash time was:

```
Heap:
 garbage-first heap   total 352256K, used 196606K [0x0000000700000000, 0x0000000800000000)
 Metaspace       used 290453K, committed 291968K, reserved 1310720K
  class space    used 63116K, committed 63744K, reserved 1048576K
```

**G1 had committed only 352MB of the 4096MB ceiling, and was using 197MB of that.** So
`-Xmx4096m` bought an address-space reservation the JVM never cashed in. **The consequence for
sizing: "6 workers × 4096m = 24GB committed" is arithmetic on ceilings, not a real memory
requirement, and must not be used to size a machine.** This is the specific error that produced
this project's earlier "24GB is undersized, need 32GB" conclusion (see `REASONING.md`, 2026-07-29).
The *observed* memory pressure on the Mini (73MB free, 10GB compressor) was real and measured; the
leap from that to a 32GB requirement was not, because it was computed from `-Xmx` sums.

**What the fixed per-JVM cost actually consists of**, from the same crash log plus the documented
config defaults:
- **Metaspace ≈ 290MB committed** (`used 290453K, committed 291968K`). This is Rama + Clojure +
  Netty + every jar in `~/rama-release/lib/` + module classes. It is **near-identical across all six
  workers**, because all six are launched with the same `rama.jar` + `lib/` classpath (confirmed
  from `supervisor.log`'s `Launching process` command lines). This is the largest *duplicated*
  per-worker cost, and it is the cost that consolidating modules would actually eliminate.
- Code cache + thread stacks + GC metadata: not itemized in the crash log; Rama runs many threads
  per worker by default (`worker.worp.server.threads` 10, `worker.weft.client.max.threads` 10,
  plus task threads and Netty event loops — see `all-configs.html`).
- Netty direct buffers, bounded by `-XX:MaxDirectMemorySize=500m` (Rama's
  `worker.max.direct.memory.size` default, per `redplanetlabs.com/docs/~/all-configs.html`:
  *"amount of direct memory to allocate to each worker process. Defaults to `500m`"*). Note this is
  passed **in addition to** `-Xmx` and is NOT set by our `worker-heap-overrides.yaml`.

Working estimate: **≈500–700MB fixed per worker, independent of workload.** Flagged as an
*estimate extrapolated from a single snapshot of a single module under active memory pressure* —
not a measured RSS profile of six healthy workers. The estimate is superseded the moment real RSS
is recorded; that measurement is the next session's task (see `CLAUDE_HANDOFF.md`).

**SUPERSEDED 2026-07-30 by the actual RSS measurement run.** Six healthy workers were measured on a
clean, settled floor: the real figure is **866–1076MB per worker**, not 500–700MB. The estimate was
low by roughly 50%. The *direction* of the `-Xmx` finding held completely — see
"`-Xmx` ceiling ratio vs. RSS ratio, measured" below for the direct measurement that replaces this
entry's extrapolation.

**Rule going forward: size worker RAM from measured RSS, never from summed `-Xmx`.** A `-Xmx`
value's job is to cap a runaway, not to declare a footprint.

---

### RocksDB's 256MB block cache is per WORKER, not per PState — measured
Verified 2026-07-30 (Step 0 RSS measurement run). **This resolves the OPEN QUESTION previously
logged in the Unverified section**, which has been removed from that section accordingly. It was
resolved exactly as that entry proposed — empirically, from measured RSS, without needing a doc
answer.

The decisive comparison, both settled and plateaued on a clean floor:

| Module | PStates | -Xmx | Settled RSS |
|---|---|---|---|
| `FamilySchemaModule` | **15** | 4096m | **865.6 MB** ← *smallest worker* |
| `DigestModule` | **0** | 4096m | **1011.3 MB** ← *larger* |

**`FamilySchemaModule`, with 15 PStates, is 146MB SMALLER than the module with zero PStates.** At
per-PState scope it would have carried roughly 3.8GB of extra block cache; it carries none. The
per-PState and per-partition readings are both eliminated. The cache is effectively per worker.

Corroborating: all six workers land in a tight **866–1076MB** band despite PState counts spanning 0
to 15, with the two extremes of PState count sitting at opposite ends of the band *in the opposite
direction* from what per-PState scaling predicts. **Worker RSS is dominated by fixed JVM + Rama +
Metaspace overhead, not by PState count.**

Consequence: the ~3.8GB term flagged as able to invalidate the fixed-cost estimate does not exist,
and box sizing no longer needs to reserve for it. It also means module *consolidation* saves the
duplicated fixed cost (~866MB+ per worker) rather than any PState-proportional cost.

---

### `-Xmx` ceiling ratio vs. RSS ratio, measured — a 2.7× ceiling difference produced ~11% RSS difference
Verified 2026-07-30 (Step 0 RSS measurement run). This is the direct measurement behind the
`-Xmx`-is-a-ceiling entry above, which until now rested on a single crash-log extrapolation.

| Module | Launched `-Xmx` | Settled RSS |
|---|---|---|
| `EmailParsingModule` | **1536m** | 956.1 MB |
| `GmailIngestionModule` | **4096m** | 1076.2 MB |

**2.67× the ceiling bought 12.6% more resident memory.** Two workers, same classpath, same
`MaxDirectMemorySize=500m`, differing only in ceiling. This is what "a ceiling does not reserve or
predict a footprint" looks like in measured numbers.

**Practical consequence: tuning `-Xmx` downward is a near-worthless RAM-reduction lever.** Dropping
five workers from 4096m to 1536m would reclaim on the order of 100MB each, not 2.5GB each. Do not
plan a memory reduction around it, and do not accept the redeploy risk of an `--action update`
purely to change `-Xmx`.

---

### Measurement contamination: compressed pages understate RSS by ~2× — always measure from a settled, uncontaminated floor
Verified 2026-07-30 (Step 0 RSS measurement run). Recorded because this nearly corrupted the
measurement it was meant to produce, and the mechanism will recur on any long-running box.

When a process's pages are compressed by macOS under memory pressure, **they stay compressed** until
touched again. Reading RSS from a process that lived through a pressure event therefore reports the
*post-compression* figure, not the true working set. Measured on the same processes, same machine,
same day:

| Process | RSS, cluster up through the pressure period | RSS, clean restart on a settled floor |
|---|---|---|
| ZooKeeper | 413.4 MB | **812.8 MB** |
| Conductor | 493.9 MB | 768.7 MB |
| Supervisor | 508.8 MB | 753.0 MB |

**ZooKeeper read at ~51% of its true footprint.** Every daemon was understated the same way. A
sizing decision taken from the contaminated readings would have undersized the box by roughly a
factor of two on the daemon tier.

**Procedure that produces a trustworthy number** (this is the sequence that was actually run):
1. Shut down everything being measured — `rama shutdownCluster`, then SIGTERM supervisor → conductor
   → ZooKeeper in that order. Verify `pgrep -f java` returns 0.
2. Take the floor reading, and **confirm it is settled, not still draining**. Sample repeatedly:
   compressor and free must be flat. Watch for background work — a Spotlight (`mds_stores`) reindex
   triggered by the data churn moved free memory by 2.4GB after shutdown, and was only distinguished
   from a real floor by sampling until it showed **0.0% CPU** with flat free/compressor.
3. Restart, settle 10 minutes, then measure. Confirm RSS has plateaued across samples before reading.

**Also note:** low free memory alone is not the failure signal. The 2026-07-19 failure signature was
a **10GB compressor** with load 6.2. A healthy settled cluster showed free at 0.25GB with 9.63GB
*inactive* (reclaimable), 0.00M swap, and the compressor flat at 2.12GB — low free, but no pressure.
Read the compressor and swap, not free.

---

### Rama's three daemons cost ~2.33GB before any module loads
Verified 2026-07-30 (Step 0 RSS measurement run), settled clean-floor figures:

| Daemon | `-Xmx` | Settled RSS |
|---|---|---|
| ZooKeeper (`devZookeeper`) | — | 812.8 MB |
| Conductor | 1024m | 768.7 MB |
| Supervisor | 1024m | 753.0 MB |
| **Total** | | **≈2.33 GB** |

That is roughly **39% of the ~6GB measured idle footprint**, spent before a single module runs — a
fixed tax on any single-node deployment.

**This makes `conductor.child.opts` the highest-value untried lever in the project.** The Conductor
is a pure coordination process holding 768.7MB against a 1024m ceiling; Rama's default is
`-Xmx1024m`. Still never applied. Per the ceiling-vs-RSS entry above, expect the gain to be modest
rather than proportional — but unlike `-Xmx` on workers, this one costs no redeploy risk to try.
`worker.max.direct.memory.size` (500m × 6 = 3GB of ceiling above `-Xmx`) likewise remains unset.

---

## Unverified — do not use without confirming

### OPEN INVESTIGATION: does `rama.yaml`'s `worker.child.opts` apply to CLI-deployed modules at all?
Logged 2026-07-19 (Part 3 restart session), deliberately deferred to a dedicated future session —
this is a clean-experiment task, not something to resolve mid-grind while other modules are being
redeployed.

**The contradiction:** Chat-o-rama (chat.redplanetlabs.com), citing `rama-shared → "All configs"`,
states plainly that configs including `worker.child.opts` "are set either through the rama.yaml
file, through the --configOverrides flag..., or programmatically when creating a
RamaClusterManager" — i.e. `rama.yaml` should be a valid, working, cluster-wide default. But this
project's actual observed history contradicts that, three separate times: `rama.yaml` has read
`worker.child.opts: "-Xmx2g"` continuously since the file was created in March 2026 (confirmed via
`git log -p -- rama.yaml`), yet workers launched at `-Xmx4096m` (Rama's undocumented-here-but-
jar-confirmed built-in default) on the April deploy, the 2026-07-19 fresh six-module deploy, AND
tonight's Supervisor auto-recovery of all six modules after the cold restart — none of which passed
`--configOverrides`. Three independent real-world data points, zero in which `rama.yaml`'s value
took effect.

**Leading hypothesis, NOT tested:** `rama.yaml`'s config values may only reach a worker JVM via the
**programmatic** path — i.e. when an application constructs its own `RamaClusterManager` (as
`FamilySchemaModule` etc.'s owning app, `FamilyAssistantApp`, does via `RamaClusterManager.open()`
using this same `rama.yaml`) — and may simply not be consulted by the **CLI-deploy path**
(`rama deploy` → Conductor → Supervisor launching a worker process), which might only ever consult
`--configOverrides` or fall back straight to Rama's own hardcoded default, skipping `rama.yaml`
entirely for that code path. This has NOT been verified against source or by a clean test.

**How to test cleanly (future session, not mid-task):** pick one already-`--configOverrides`-tuned
module (e.g. `FamilySchemaModule`, currently at `-Xmx1536m`), edit `rama.yaml`'s `worker.child.opts`
to a third, distinct value (e.g. `-Xmx1234m` — deliberately not a round number so it's unambiguous
in a log grep), redeploy with `--action update` and **no** `--configOverrides` flag at all, and grep
the resulting `supervisor.log` `Launching process` line. If it shows `-Xmx1234m`, `rama.yaml` DOES
apply to CLI deploys (contradicts observed history — worth understanding why prior deploys differed,
e.g. maybe `rama.yaml` needs to be re-synced to `~/rama-release/rama.yaml` at the exact right moment
relative to Conductor's own read of it). If it shows `-Xmx4096m` (Rama's hardcoded default), the
programmatic-vs-CLI-path hypothesis is confirmed. Do not run this test opportunistically inside a
different task's redeploy — it needs to be the one deliberate variable changed, isolated from
whatever redeploy work is otherwise in progress.

### ~~OPEN QUESTION: is RocksDB's 256MB block cache per PState, per partition, or per worker?~~ — RESOLVED 2026-07-30, MOVED TO VERIFIED
**Answer: per worker.** Resolved empirically by the Step 0 RSS measurement run, exactly as the
"how to resolve it cheaply" note below proposed — `FamilySchemaModule` (15 PStates) settled at
865.6MB, *smaller* than `DigestModule` (0 PStates) at 1011.3MB. See
**"RocksDB's 256MB block cache is per WORKER, not per PState — measured"** in the Verified section.

The original entry is retained below for its doc citations and its reasoning, which were sound and
led to the correct experiment. It is no longer an open question and must not be treated as one.

---

Logged 2026-07-29 (RAM-reduction audit session). Kept in the Unverified section deliberately —
this file's own rule is never to mix verified and unverified items, and this one could not be
resolved from the docs.

**What IS confirmed**, verbatim from `redplanetlabs.com/docs/~/all-configs.html`, under the
`pstate.rocksdb.options.builder` entry (fetched and read directly this session):

> "PStates with a top-level map in the schema use RocksDB as the underlying durable storage. This
> config lets you provide the full name of a class implementing
> `com.rpl.rama.RocksDBOptionsBuilder` to configure the RocksDB instances. By default, RocksDB is
> configured to use two-level indexing and have a 256MB block cache."

**What is NOT stated anywhere in the docs:** the *scope* of that 256MB. The sentence says "the
RocksDB instances" (plural) without saying whether one block cache is shared across them or each
gets its own. `pstates.html` was also fetched and checked this session and does not resolve it
either.

**Why it matters here specifically, and why it isn't academic:** `FamilySchemaModule` declares 15
PStates, all with top-level maps, across 4 tasks. The three candidate readings span two orders of
magnitude for that one module:
- per worker → 256MB (negligible)
- per PState → ~3.8GB (dominates every other RAM item combined)
- per PState partition → larger still

This is off-heap, so it appears in RSS but NOT in the `-Xmx`/heap numbers recorded in the verified
`-Xmx` entry above — meaning it is exactly the term that could invalidate the ≈500–700MB/worker
fixed-cost estimate for this one module. **Do not finalize a box size on that estimate without
resolving this.**

**How to resolve it cheaply:** the next session's RSS measurement run answers it empirically without
needing a doc answer at all — if `FamilySchemaModule`'s measured RSS lands near the other five
workers', the cache is effectively per-worker; if it is GBs higher, it scales with PState count.
Compare `FamilySchemaModule` (15 PStates) against `DigestModule` (0 declared PStates) on the same
idle cluster; that single comparison discriminates between the readings. Failing that,
`javap`/decompile `com.rpl.rama.RocksDBOptionsBuilder` and its call sites in the pinned
`rama-1.5.0.jar` (jar inspection is this file's highest authority per the fallback order above).
