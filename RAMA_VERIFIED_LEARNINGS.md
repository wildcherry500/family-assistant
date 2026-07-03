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

---

## Unverified — do not use without confirming

*(none currently — every constraint referenced this session was resolved above)*
