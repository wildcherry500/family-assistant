# Claude Instructions — Family Assistant Project

## HIGHEST PRIORITY RULE: Protect Business Logic — Diagnose Root Cause, Never Simplify

**The planned business logic is ground truth. Never strip it down, stub it out, or replace it with a simplified version just to get the code to compile.**

When something does not compile or behave correctly, the cause is almost always a platform-specific issue — not a flaw in the logic. Rama and Agent-o-rama are novel frameworks. They run on the JVM and use Java/Clojure syntax, but they have their own paradigms, their own symbols, their own dataflow model. They are NOT standard Java. Common root causes of failures include:

- **Wrong nomenclature** — Rama dataflow variables use `*` prefix (e.g. `*eventId`), PStates use `$$` prefix, depots use `*`. Getting these wrong causes subtle failures.
- **Misapplied or missing Path expressions** — PState reads and writes go through `Path` combinators (`key`, `mapVals`, `all`, `sortedMapRange`, `nullToSet`, `voidSetElem`, `termVal`, etc.). Wrong combinator order or type produces wrong results or fails silently.
- **PState not declared correctly** — PStates must be declared with the right structure in the owning module before they can be used anywhere.
- **Wrong topology type** — stream vs. microbatch vs. query topology each have different semantics and constraints. Using the wrong one breaks the intended behavior.
- **Agent-o-rama wiring issues** — nodes, node dependencies, agent objects, and AgentManager setup each have specific ordering and registration requirements.
- **IPC vs. cluster API mismatch** — `InProcessCluster` and `RamaClusterManager` have different wiring. Mixing them causes runtime failures.

**The right response to any build or runtime failure is to diagnose the platform-level root cause and fix it correctly.** Reference `docs/Agent_O_Rama_Complete_Documentation.md`, `CLAUDE_HANDOFF.md`, and the existing test suite for working patterns. Understand exactly what is wrong — then fix that, not the logic.

Rama 1.5.0 and Agent-o-rama 0.8.0 are fully composable. Whatever the business logic requires is achievable on this platform.

---

## Reference Documents

- **Agent-o-rama full docs:** `docs/Agent_O_Rama_Complete_Documentation.md` — read this before writing any agent code. It contains the complete API for AgentModule, AgentTopology, AgentNode, AgentManager, AgentClient, streaming, human input, datasets, experiments, and observability.
- **Project state & PState schema:** `CLAUDE_HANDOFF.md` — canonical reference for all PStates, indexes, module structure, test suite, GCP config, and cluster setup.

---

## Production Cluster vs. Tests — Critical Distinction

| Context | API |
|---|---|
| **Production app** (`FamilyAssistantApp`) | `RamaClusterManager.open()` — connects to running ZK + Conductor + Supervisor |
| **Tests** | `InProcessCluster.create()` — in-process simulation, data does not persist |

**Do NOT use `InProcessCluster` in production code.** Tests use IPC because they are self-contained. The production app runs against the real local cluster (see `rama.yaml`).

When working on `FamilyAssistantApp` or any non-test class, always use `RamaClusterManager`. When writing tests, IPC is correct and expected.

---

## Agent-o-rama Patterns (Quick Reference)

Agents are defined in a class extending `AgentModule`, inside `defineAgents(AgentTopology topology)`.

```java
// Module definition
public class MyAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareAgentObjectBuilder("gemini-model", setup -> { ... });
    topology.newAgent("my-agent")
            .node("step1", null, (AgentNode node, InputType input) -> {
                // do work
                node.result(output);
            })
            .node("step2", List.of("step1"), (AgentNode node) -> {
                // fan-in from step1
            });
  }
}

// Production wiring
AgentManager manager = AgentManager.create(cluster, moduleName);
AgentClient agent = manager.getAgentClient("my-agent");
String result = agent.invoke(inputPayload);
```

Key facts:
- Nodes run in parallel unless you declare dependencies via the second argument (`List.of("nodeName")`)
- `node.result(value)` emits output from a node
- `AgentObject` instances (LLM clients, DB clients, etc.) are declared once per topology and retrieved in nodes via `node.getAgentObject("key")`
- Modules are deployed to the real cluster with `rama deploy --action launch --jar ... --module FullClassName`
- The Agent-o-rama UI runs at `http://localhost:1974` when started with `UI.start(cluster)`

---

## Rama 1.5.0 PState Patterns (Verified)

### Reading from PState
```java
// Range query on subindexed PState
List<String> ids = (List<String>)(List<?>) pstate.select(
    Path.key(familyId).sortedMapRange(startMs, endMs).mapVals().all());

// Single key lookup
SomeType val = (SomeType) pstate.selectOne(Path.key(key1).key(key2));
```

### Writing in stream topology
```java
// subindexed write
Path.key("*familyId", "*epochMs").nullToSet().voidSetElem().termVal("*eventId")
```

### DO NOT
- Call `selectOne(Path.key(familyId))` on a subindexed PState — returns raw `RocksDBWrapper`, not usable
- Use `InProcessCluster` in production code
- Use `ipc.launchModule()` in production — modules are deployed via CLI

---

## Module Inventory

| Module | Purpose |
|---|---|
| `FamilySchemaModule` | Owns `$$family-data` PState + all index PStates + `*family-events` depot |
| `EmailParsingModule` | Raw email → structured event → appends to depot |
| `EmailIngestionModule` | Batch fan-out to EmailParsingModule |
| `GmailIngestionModule` | Fetches unread Gmail, deduplicates, routes |
| `DigestModule` | Reads mirror store → returns digest string |
| `QueryModule` | LLM natural language query over PState indexes |

**Next module to build:** `SearchIndexModule` — full-text/keyword search over event titles and descriptions, new PState in FamilySchemaModule.

---

## Build Commands

```bash
# All tests (no LLM key needed)
cd /Users/toddkeelingfolder/CORSAIR/family_assistant
mvn test

# Build fat JAR
mvn clean package -DskipTests

# Deploy a module to local cluster
~/rama-release/rama deploy --action launch \
  --jar target/family-assistant-1.0.0-jar-with-dependencies.jar \
  --module com.family.assistant.schema.FamilySchemaModule

# Start app (connects to running cluster)
mvn compile exec:exec
```

Working directory is always `/Users/toddkeelingfolder/CORSAIR/family_assistant`. Do NOT use `/Volumes/CORSAIR/family-assistant` (hyphen) — that is an old stub with no git history.
