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
void snapshotDataset(UUID datasetId, String from, String to)
Map<UUID,String> searchDatasets(String query, int limit)

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

## Source Types (RunInfo.getSource() instanceof ...)
`InfoSource` → `ApiSource`, `AiSource`, `BulkUploadSource`, `HumanSource` (getName),
`EvalSource` (getEvalName), `ExperimentSource` (getDatasetId, getExperimentId),
`ActionSource` (getRuleName), `AgentRunSource` (getModuleName, getAgentName, getAgentInvoke)
