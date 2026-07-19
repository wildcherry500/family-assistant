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

---

## Unverified — do not use without confirming

*(none currently — every constraint referenced this session was resolved above)*
