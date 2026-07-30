# agent-o-rama 0.8.0 API Reference

Extracted from JAR source at `/tmp/aor-src/com/rpl/agentorama/`.

## AgentModule
```java
public abstract class AgentModule implements RamaModule {
    protected abstract void defineAgents(AgentTopology topology);
    // define(Setup, Topologies) calls AgentTopology.create() then defineAgents() then at.define()
}
```

## AgentTopology (interface)
```java
AgentGraph newAgent(String name)
AgentGraph newToolsAgent(String name, List<ToolInfo> tools)
AgentGraph newToolsAgent(String name, List<ToolInfo> tools, ToolsAgentOptions options)

void declareKeyValueStore(String name, Class keyClass, Class valClass)       // name starts with $$
void declareDocumentStore(String name, Class keyClass, Object... schema)
PState.Declaration declarePStateStore(String name, Class schema)
PState.Declaration declarePStateStore(String name, PState.Schema schema)

void declareAgentObject(String name, Object o)                               // static shared object
void declareAgentObjectBuilder(String name, RamaFunction1<AgentObjectSetup, Object> builder)
void declareAgentObjectBuilder(String name, RamaFunction1<...> builder, AgentObjectOptions options)

void declareEvaluatorBuilder(name, desc, RamaFunction1<Map<String,String>, RamaFunction4<AgentObjectFetcher, Input, RefOutput, Output, Map>>)
void declareComparativeEvaluatorBuilder(name, desc, RamaFunction1<..., RamaFunction4<..., List<Output>, Map>>)
void declareSummaryEvaluatorBuilder(name, desc, RamaFunction1<..., RamaFunction2<AgentObjectFetcher, List<ExampleRun>, Map>>)
void declareActionBuilder(name, desc, RamaFunction1<..., RamaFunction4<AgentObjectFetcher, List<Input>, Output, RunInfo, Map>>)

StreamTopology getStreamTopology()
void define()    // only needed outside AgentModule
```

## AgentGraph (interface, auto-generated)
```java
AgentGraph setUpdateMode(UpdateMode mode)   // CONTINUE, RESTART, DROP

// node — 0 to 7 typed args; outputNodesSpec = String | String[] | null (terminal)
AgentGraph node(String name, Object outputNodesSpec, RamaVoidFunction1<AgentNode> impl)
AgentGraph node(String name, Object outputNodesSpec, RamaVoidFunction2<AgentNode, T0> impl)
// ... up to RamaVoidFunction8

// aggStartNode — like node but returns Object; passed to downstream aggNode as last arg
AgentGraph aggStartNode(String name, Object outputNodesSpec, RamaFunction1<AgentNode, Object> impl)
// ... up to RamaFunction8

// aggNode — fan-in; receives (agentNode, aggregatedValue, aggStartResult)
AgentGraph aggNode(String name, Object outputNodesSpec, RamaAccumulatorAgg agg, RamaVoidFunction3<AgentNode, S, T> impl)
AgentGraph aggNode(String name, Object outputNodesSpec, RamaCombinerAgg agg, RamaVoidFunction3<AgentNode, S, T> impl)
AgentGraph aggNode(String name, Object outputNodesSpec, MultiAgg.Impl agg, RamaVoidFunction3<AgentNode, S, T> impl)
AgentGraph aggNode(String name, Object outputNodesSpec, BuiltInAgg agg, RamaVoidFunction3<AgentNode, S, T> impl)
```

## AgentNode (interface) — passed into every node function
```java
// Routing
void emit(String node, Object... args)     // target must be in outputNodesSpec
void result(Object arg)                    // set final result; first-wins

// Stores (name must start with $$)
<T extends Store> T getStore(String name)
<T extends Store> T getMirrorStore(String moduleName, String name)   // READ-ONLY

// Depots
Depot getDepot(String name)
Depot getMirrorDepot(String moduleName, String name)   // CAN append()

// Query topologies
<T> QueryTopologyClient<T> getQueryTopologyClient(String name)
<T> QueryTopologyClient<T> getMirrorQueryTopologyClient(String moduleName, String name)

// Agent objects
Object getAgentObject(String name)   // from AgentObjectFetcher

// Sub-agents (from IFetchAgentClient)
AgentClient getAgentClient(String agentName)
AgentClient getMirrorAgentClient(String moduleName, String agentName)

// Streaming
void streamChunk(Object chunk)

// HITL (blocks virtual thread)
String getHumanInput(String prompt)

// Tracing
void recordNestedOp(NestedOpType type, long startMs, long finishMs, Map<String,Object> info)
// NestedOpType: STORE_READ, STORE_WRITE, DB_READ, DB_WRITE, MODEL_CALL, TOOL_CALL, AGENT_CALL, HUMAN_INPUT, OTHER
// MODEL_CALL info keys: "inputTokenCount", "outputTokenCount", "totalTokenCount", "failure"

// Metadata
Map<String, Object> getMetadata()
```

## AgentClient (interface)
```java
// Blocking invocation
<T> T invoke(Object... args)
<T> T invokeWithContext(AgentContext context, Object... args)

// Async invocation
<T> CompletableFuture<T> invokeAsync(Object... args)
<T> CompletableFuture<T> invokeWithContextAsync(AgentContext context, Object... args)

// Non-blocking — returns handle
AgentInvoke initiate(Object... args)
AgentInvoke initiateWithContext(AgentContext context, Object... args)
CompletableFuture<AgentInvoke> initiateAsync(Object... args)

// Get result from handle
<T> T result(AgentInvoke invoke)
<T> CompletableFuture<T> resultAsync(AgentInvoke invoke)
AgentStep nextStep(AgentInvoke invoke)    // returns HumanInputRequest | AgentComplete
boolean isAgentInvokeComplete(AgentInvoke invoke)

// Fork — re-run from mid-graph node with new args
<T> T fork(AgentInvoke invoke, Map<UUID, List> nodeInvokeIdToNewArgs)
AgentInvoke initiateFork(AgentInvoke invoke, Map<UUID, List> nodeInvokeIdToNewArgs)

// Streaming
AgentStream stream(AgentInvoke invoke, String node)
<T> AgentStream stream(AgentInvoke invoke, String node, StreamCallback<T> callback)
AgentStream streamSpecific(AgentInvoke invoke, String node, UUID nodeInvokeId)
AgentStreamByInvoke streamAll(AgentInvoke invoke, String node)
<T> AgentStreamByInvoke streamAll(AgentInvoke invoke, String node, StreamAllCallback<T> callback)

// HITL
List<HumanInputRequest> pendingHumanInputs(AgentInvoke invoke)
void provideHumanInput(HumanInputRequest request, String response)

// Metadata
void setMetadata(AgentInvoke invoke, String key, int|long|float|double|String|boolean value)
void removeMetadata(AgentInvoke invoke, String key)
Map<String, Object> getMetadata(AgentInvoke invoke)
```

## AgentManager (interface)
```java
static AgentManager create(ClusterManagerBase cluster, String moduleName)

Set<String> getAgentNames()
AgentClient getAgentClient(String agentName)   // via IFetchAgentClient

// Datasets
UUID createDataset(String name, String desc, String inputSchema, String outputSchema)
UUID addDatasetExample(UUID datasetId, Object input, AddDatasetExampleOptions options)
CompletableFuture<Void> addDatasetExampleAsync(UUID datasetId, Object input, AddDatasetExampleOptions options)
void setDatasetExampleInput(UUID datasetId, String snapshotName, UUID exampleId, Object input)
void setDatasetExampleReferenceOutput(UUID datasetId, String snapshotName, UUID exampleId, Object referenceOutput)
void removeDatasetExample(UUID datasetId, String snapshotName, UUID exampleId)
void addDatasetExampleTag(UUID datasetId, String snapshotName, UUID exampleId, String tag)
void removeDatasetExampleTag(UUID datasetId, String snapshotName, UUID exampleId, String tag)
void snapshotDataset(UUID datasetId, String from, String to)
void removeDatasetSnapshot(UUID datasetId, String snapshotName)
Map<UUID,String> searchDatasets(String query, int limit)
// NO searchExamples — see "Dataset examples are write-only from Java" below

// Evaluators
void createEvaluator(String name, String builderName, Map params, String desc, CreateEvaluatorOptions options)
Map tryEvaluator(String name, Object input, Object refOutput, Object output)
Map tryComparativeEvaluator(String name, Object input, Object refOutput, List<Object> outputs)
Map trySummaryEvaluator(String name, List<ExampleRun> exampleRuns)

// Human metrics
void createCategoricalHumanMetric(String name, String desc, Set<String> categories)
void createNumericHumanMetric(String name, String desc, int min, int max)
```

## Store Hierarchy
```
Store
  └── PStateStore                     — select(Path), selectOne(Path), transform(partKey, Path)
        └── KeyValueStore<K,V>        — get, getOrDefault, put, update, containsKey
              └── DocumentStore<K>    — getDocumentField, putDocumentField, updateDocumentField
```

## BuiltIn Aggregators (for aggNode)
`LIST_AGG`, `SET_AGG`, `MAP_AGG`, `MERGE_MAP_AGG`, `SUM_AGG`, `MIN_AGG`, `MAX_AGG`,
`FIRST_AGG`, `LAST_AGG`, `AND_AGG`, `OR_AGG`, `MULTI_SET_AGG`

## Source Types (Feedback.getSource() instanceof ...)
CORRECTED 2026-07-26: this heading previously read `RunInfo.getSource()`. `RunInfo` has no
`getSource()` method in 0.8.0. `getSource()` is declared on
`com.rpl.agentorama.analytics.Feedback:27`, returning `InfoSource`
(`com/rpl/agentorama/source/InfoSource.java`, whose one method is `String getSourceString()`).

`InfoSource` → `ApiSource`, `AiSource`, `BulkUploadSource`, `HumanSource` (getName),
`EvalSource` (getEvalName), `ExperimentSource` (getDatasetId, getExperimentId),
`ActionSource` (getRuleName), `AgentRunSource` (getModuleName, getAgentName, getAgentInvoke)

## Reading agent runs and traces — no client-side pull exists
Verified 2026-07-26 against the extracted 0.8.0 JAR source (same `/tmp/aor-src` extraction as the
rest of this file).

**No documented public API exists for reading agent runs or traces programmatically.** `AgentManager`'s
complete surface is: `create`, `getAgentNames`, `getAgentClient` (inherited from
`com.rpl.agentorama.impl.IFetchAgentClient`), eleven dataset-management methods, five
evaluator/human-metric management methods, and the three `try*Evaluator` methods. Nothing on it
returns a run, a trace, a span, or an execution history. Traces are served by the UI assets bundled
in the JAR (`public/main.*.js`, `com/rpl/agent_o_rama/ui.cljs`, started via `UI.start(...)`),
backed by AOR-internal module state with no public Java accessor. **Traces are UI-only.**

Three near-misses that look like a run-reading API and are not:
- `ExampleRun` is built by *you* via `ExampleRun.create(input, referenceOutput, output)` and passed
  *into* `trySummaryEvaluator`. It is an input type, not a read of a historical run.
- `AgentClient.getMetadata(AgentInvoke)` reads metadata you set on a live invoke handle you already
  hold. It is not a query over past runs.
- `RunInfo` genuinely carries run detail — but you can never fetch one. See below.

### `RunInfo` inside `declareActionBuilder` is the only programmatic access to run/trace data
It is **in-cluster and push-based**: AOR hands you a `RunInfo` as the 4th argument of an action
callback, executing on a worker as runs complete. There is no client-side pull — no
`getRun(id)`, no `listRuns()`, nowhere to ask for one.

```java
// AgentTopology.java:290-294
<Input, Output> void declareActionBuilder(
    String name,
    String description,
    RamaFunction1<Map<String, String>,
                  RamaFunction4<AgentObjectFetcher, List<Input>, Output, RunInfo, Map>> builder);
```

Verbatim from that method's javadoc: *"Declares an action builder for real-time evaluation on
production runs. Actions are user-defined hooks running on live agent executions for real-time
evaluation, data capture, etc."* — "data capture" is AOR's own framing of this use.

What `RunInfo` exposes (`RunInfo.java`): `getAgentName`, `getNodeName`, `getRunType`,
`getAgentInvoke`, `getNodeInvoke`, `getRuleName`, `getActionName`, `getStartTimeMillis`,
`getLatencyMillis` (nullable — null if the node failed and never completed), `getFeedback`
(`List<Feedback>`), `getAgentStats` (`AgentInvokeStats`, null for a node-level `RunInfo`), and
`getNodeNestedOps` (`List<NestedOpInfo>`, null for an agent-level `RunInfo`).

**This is the supported path for capturing run data into an application depot.** If run/trace data
needs to outlive the UI or feed anything downstream, the shape is: declare an action builder,
receive `RunInfo` in the callback, project the fields you care about into a plain `Map`, and append
that map to one of your own depots from inside the callback. You own the resulting log; AOR's own
trace store stays unreachable either way. `ActionBuilderOptions` supports `param(name, desc[,
default])` and `limitConcurrency()` (bounded by the global config `max.limited.actions.concurrency`).

### Dataset examples are write-only from Java
There is no `searchExamples`. The only two search methods on `AgentManager` are
`Map<UUID,String> searchDatasets(String searchString, int limit)` and
`Set<String> searchEvaluators(String searchString)` — the first returns dataset IDs and names, never
example contents. Every remaining example-level method (`setDatasetExampleInput`,
`setDatasetExampleReferenceOutput`, `removeDatasetExample`, `addDatasetExampleTag`,
`removeDatasetExampleTag`) requires the example's `UUID` as an argument, and the only place that
`UUID` ever appears is the return value of `addDatasetExample`.

**Consequence: persist the returned `UUID` externally at the moment of the call, or the example
becomes permanently unreachable from Java.** It still exists, still counts toward the dataset, and
is still visible and editable in the UI — but no Java code can ever address it again. Note also
that `addDatasetExampleAsync` returns `CompletableFuture<Void>`, not the UUID, so the async variant
forfeits addressability entirely. Use the synchronous `addDatasetExample` for anything you may need
to update, retag, or remove later.

Datasets are the only programmatic bridge into AOR evaluation, and this is the sharp edge on that
bridge: you can write examples in and run evaluators over them, but you cannot read the examples
back out or enumerate them.
