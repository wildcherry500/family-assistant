# Agent-o-rama

Agent-o-rama is an end-to-end LLM agent platform for building, tracing, testing, and monitoring agents with integrated storage and one-click deployment. Agent-o-rama provides two first-class APIs, one for Java and one for Clojure, with feature parity between them.

Building LLM-based applications requires being rigorous about testing and monitoring. Inspired by [LangGraph](https://www.langchain.com/langgraph) and [LangSmith](https://www.langchain.com/langsmith/observability), Agent-o-rama provides similar capabilities to support the end-to-end workflow of building LLM applications: datasets and experiments for evaluation, and detailed tracing, online evaluation, and time-series telemetry (e.g. model latency, token usage, database latency) for observability. All of this is exposed in a comprehensive web UI.

Agents are defined as simple graphs of Java or Clojure functions that execute in parallel, with built-in high-performance storage for any data model and integrated deployment. Agents have full support for streaming, and they're easy to scale by just adding more nodes.

<p align="center">
  <img src="images/trace.png" width="30%" alt="Agent execution trace">
  <img src="images/dataset.png" width="30%" alt="Dataset">
  <img src="images/experiments.png" width="30%" alt="Experiments listing">
</p>
<p align="center">
  <img src="images/human-input.png" width="30%" alt="Human input">
  <img src="images/fork.png" width="30%" alt="Forking">
  <img src="images/create-llm-judge.png" width="30%" alt="Create LLM judge evaluator">
</p>
<p align="center">
  <img src="images/telemetry1.png" width="30%" alt="Telemetry 1">
  <img src="images/telemetry2.png" width="30%" alt="Telemetry 2">
  <img src="images/telemetry3.png" width="30%" alt="Telemetry 3">
</p>

- [Overview](#overview)
- [Detailed comparisons against other agent tools](#detailed-comparisons-against-other-agent-tools)
- [Downloads](#downloads)
- [Learning Agent-o-rama](#learning-agent-o-rama)
- [Tour of Agent-o-rama](#tour-of-agent-o-rama)
  - [Defining and deploying agents](#defining-and-deploying-agents)
  - [Viewing agent traces](#viewing-agent-traces)
  - [Forking agent invokes](#forking-agent-invokes)
  - [Incorporating human input into agent execution](#incorporating-human-input-into-agent-execution)
  - [Streaming agent nodes to clients](#streaming-agent-nodes-to-clients)
  - [Creating and managing datasets](#creating-and-managing-datasets)
  - [Running experiments](#running-experiments)
  - [Online actions](#online-actions)
  - [Time-series telemetry](#time-series-telemetry)

## Overview

LLMs are powerful but inherently unpredictable, so building applications with LLMs that are helpful and performant with minimal hallucination requires extensive testing and monitoring. Agent-o-rama addresses this by making evaluation and observability a first-class part of the development process, not an afterthought.

Agent-o-rama is deployed onto your own infrastructure on a [Rama cluster](https://redplanetlabs.com/). Rama is free to use for clusters up to two nodes and can scale to thousands with a commercial license. Every part of Agent-o-rama is built-in and requires no other dependency besides Rama, including high-performance, durable, and replicated storage of any data model that can be used as part of agents. Agent-o-rama also integrates seamlessly with any other tool, such as databases, vector stores, external APIs, or anything else. Unlike hosted observability tools, all data and traces stay within your infrastructure.

Agent-o-rama integrates with [Langchain4j](https://docs.langchain4j.dev/) to capture detailed traces of model calls and embedding-store operations, and to stream model interactions to clients in real time. Integration is fully optional – if you prefer to use other APIs for model access, Agent-o-rama supports that as well.

Rama can be downloaded [here](https://redplanetlabs.com/download), and instructions for setting up a cluster are [here](https://redplanetlabs.com/docs/~/operating-rama.html#_setting_up_a_rama_cluster). A cluster can be [as small as one node](https://redplanetlabs.com/docs/~/operating-rama.html#_running_single_node_cluster) or as big as thousands of nodes. There's also one-click deploys [for AWS](https://github.com/redplanetlabs/rama-aws-deploy) and [for Azure](https://github.com/redplanetlabs/rama-azure-deploy). Instructions for developing with and deploying Agent-o-rama [are below](#downloads).

Development of Agent-o-rama applications is done with "in-process cluster" (IPC), which simulates Rama clusters in a single process. IPC is great for unit testing or experimentation at a REPL. [See below](#defining-and-deploying-agents) for examples in both Java and Clojure of agents utilizing an LLM run with IPC.


## Detailed comparisons against other agent tools

- [Agent-o-rama vs. LangGraph / LangSmith](https://github.com/redplanetlabs/agent-o-rama/wiki/LangGraph-and-LangSmith-comparison)
- [Agent-o-rama vs. LangChain4j](https://github.com/redplanetlabs/agent-o-rama/wiki/LangChain4j-comparison)
- [Agent-o-rama vs. Spring AI](https://github.com/redplanetlabs/agent-o-rama/wiki/Spring-AI-comparison)
- [Agent-o-rama vs. Koog](https://github.com/redplanetlabs/agent-o-rama/wiki/Koog-comparison)
- [Agent-o-rama vs. Embabel](https://github.com/redplanetlabs/agent-o-rama/wiki/Embabel-comparison)
- [Agent-o-rama vs. LangGraph4j](https://github.com/redplanetlabs/agent-o-rama/wiki/LangGraph4j-comparison)


## Downloads

Download Agent-o-rama releases [here](https://github.com/redplanetlabs/agent-o-rama/releases). A release is used to run the Agent-o-rama frontend. See [this section](https://github.com/redplanetlabs/agent-o-rama/wiki/Quickstart#running-on-a-local-rama-cluster) for instructions on deploying. For building agent modules, add these repositories to the Maven dependencies for your project:

```
<repositories>
  <repository>
    <id>nexus-releases</id>
    <url>https://nexus.redplanetlabs.com/repository/maven-public-releases</url>
  </repository>
  <repository>
    <id>clojars</id>
    <url>https://repo.clojars.org/</url>
  </repository>
</repositories>
```

The Maven target for Agent-o-rama is:

```
<dependency>
  <groupId>com.rpl</groupId>
  <artifactId>agent-o-rama</artifactId>
  <version>0.7.0</version>
</dependency>
```

## Learning Agent-o-rama

* [Quickstart](https://github.com/redplanetlabs/agent-o-rama/wiki/Quickstart)
* [Full documentation](https://github.com/redplanetlabs/agent-o-rama/wiki)
* [Documentation chatbot](https://chat.redplanetlabs.com/)
* [Javadoc](https://redplanetlabs.com/aor/javadoc/index.html)
* [Clojuredoc](https://redplanetlabs.com/aor/clojuredoc/index.html)
* [Mailing list](https://groups.google.com/u/1/g/rama-user)
* [Discord server](https://discord.gg/RX6UgQNR)
* #rama channel on [Clojurians](https://clojurians.slack.com/)


## Tour of Agent-o-rama

Below is a quick tour of all aspects of Agent-o-rama, starting with defining agents through running experiments and analyzing telemetry.

### Defining and deploying agents

Agents are defined in "modules" which also contain storage definitions, agent objects (such as LLM or database clients), custom [evaluators](https://github.com/redplanetlabs/agent-o-rama/wiki/Datasets,-evaluators,-and-experiments), and custom [actions](https://github.com/redplanetlabs/agent-o-rama/wiki/Actions,-rules,-and-telemetry). A module can have any number of agents in it, and a module is launched on a cluster with one-line commands with the Rama CLI. For example, here's how to define a module `BasicAgentModule` with one agent that does a single LLM call and run it in the "in-process cluster" (IPC) development environment in both Java and Clojure:

#### Java example

```java
public class BasicAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));
    topology.declareAgentObjectBuilder(
      "openai-model",
      setup -> {
        String apiKey = setup.getAgentObject("openai-api-key");
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .build();
      });
    topology.newAgent("basic-agent")
            .node("chat",
                  null,
                  (AgentNode node, String prompt) -> {
                    ChatModel model = node.getAgentObject("openai-model");
                    node.result(model.chat(prompt));
                  });
  }
}

try (InProcessCluster ipc = InProcessCluster.create();
     AutoCloseable ui = UI.start(ipc)) {
  BasicAgentModule module = new BasicAgentModule();
  ipc.launchModule(module, new LaunchConfig(1, 1));
  String moduleName = module.getModuleName();
  AgentManager manager = AgentManager.create(ipc, moduleName);
  AgentClient agent = manager.getAgentClient("basic-agent");

  String result = agent.invoke("What are use cases for AI agents?");
  System.out.println("Result: " + result);
}
```

#### Clojure example

```clojure
(aor/defagentmodule BasicAgentModule
  [topology]
  (aor/declare-agent-object topology "openai-api-key" (System/getenv "OPENAI_API_KEY"))
  (aor/declare-agent-object-builder
   topology
   "openai-model"
   (fn [setup]
     (-> (OpenAiStreamingChatModel/builder)
         (.apiKey (aor/get-agent-object setup "openai-api-key"))
         (.modelName "gpt-4o-mini")
         .build)))
  (-> topology
      (aor/new-agent "basic-agent")
      (aor/node
       "start"
       nil
       (fn [agent-node prompt]
         (let [openai (aor/get-agent-object agent-node "openai-model")]
           (aor/result! agent-node (lc4j/basic-chat openai prompt))
         )))))

(with-open [ipc (rtest/create-ipc)
            ui (aor/start-ui ipc)]
  (rtest/launch-module! ipc BasicAgentModule {:tasks 4 :threads 2})
  (let [module-name (rama/get-module-name BasicAgentModule)
        agent-manager (aor/agent-manager ipc module-name)
        agent (aor/agent-client agent-manager "basic-agent")]
    (println "Result:" (aor/agent-invoke agent "What are use cases for AI agents?"))
    ))
```

These examples also launch the Agent-o-rama UI locally at `http://localhost:1974`.

See [this page](https://github.com/redplanetlabs/agent-o-rama/wiki/Programming-agents) for all the details of coding agents, including having multiple nodes, getting human input as part of execution, and aggregation. For lots of examples of agents in either Java or Clojure, see the [examples](https://github.com/redplanetlabs/agent-o-rama/tree/master/examples) directory in the repository.

#### Managing modules on a real cluster

Modules are launched, updated, and scaled on a real cluster with the Rama CLI. Here's an example of launching:

```
rama deploy --action launch \
--jar my-application-1.0.0.jar \
--module com.mycompany.BasicAgentModule \
--tasks 32 \
--threads 8 \
--workers 4 \
--replicationFactor 2
```

The launch parameters are detailed more in [this section](https://redplanetlabs.com/docs/~/operating-rama.html#_launching_modules) of the Rama docs.

Updating a module to change agent definitions, add/remove storage definitions, or any other change looks like:

```
rama deploy \
  --action update \
  --jar my-application-1.0.1.jar \
  --module com.mycompany.BasicAgentModule
```

Finally, scaling a module to add or remove resources looks like:

```
rama scaleExecutors \
--module com.mycompany.BasicAgentModule \
--threads 16 \
--workers 8
```

![Forking UI](images/fork.png)

### Incorporating human input into agent execution

Agent-o-rama has a first-class API for dynamically requesting [human input](https://github.com/redplanetlabs/agent-o-rama/wiki/Human%E2%80%90in%E2%80%90the%E2%80%90loop) in the middle of agent execution. Pending human input is viewable in traces and can be provided there or via the API.

![Human input](images/human-input.png)

Human input is requested in a node function with the blocking call `agentNode.getHumanInput(prompt)` in Java and `(aor/get-human-input agent-node prompt)` in Clojure. Since nodes run on [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), this is efficient. This returns the string the human provided that can be utilized in the rest of the agent execution.


### Streaming agent nodes to clients

The client API can stream results from individual nodes. Nested model calls are automatically streamed back for the node, and node functions can also use the Agent-o-rama API to explicitly stream chunks as well. Here's what it looks like in Java and Clojure to register a callback to stream a node:

```java
client.stream(invoke, "someNode", (List<String> allChunks, List<String> newChunks, boolean isReset, boolean isComplete) -> {
  System.out.println("Received new chunks: " + newChunks);
};
```

```clojure
(aor/agent-stream client invoke "some-node"
 (fn [all-chunks new-chunks reset? complete?]
   (println "Received new chunks:" new-chunks)))
```

See [this page](https://github.com/redplanetlabs/agent-o-rama/wiki/Streaming) for all the info on streaming.


### Creating and managing datasets

Datasets of examples can be created and managed via the UI or API. Examples can be added manually, imported in bulk via [JSONL](https://jsonlines.org/examples/) files, or added automatically from production runs with [actions](https://github.com/redplanetlabs/agent-o-rama/wiki/Actions,-rules,-and-telemetry). See all the info about datasets [on this page](https://github.com/redplanetlabs/agent-o-rama/wiki/Datasets,-evaluators,-and-experiments).

![Dataset](images/dataset.png)

### Running experiments

Datasets can then be used to run experiments to track agent performance, do regression testing, or test new agents. Experiments can be run on entire agents or on individual nodes of an agent. A single target can be tested at a time, or comparative experiments can be done to evaluate multiple different targets (e.g. the same agent parameterized to use different models).

![Experiments list](images/experiments.png)

Experiment results look like:

![Experiment results](images/experiment-results.png)

Experiments use "evaluators" to score performance. Evaluators are functions that return scores, and they can use models or databases during their execution. Agent-o-rama has built-in evaluators like the "LLM as judge" evaluator which uses an LLM with a prompt to produce a score.

![Create LLM judge](images/create-llm-judge.png)

See all the info about experiments [on this page](https://github.com/redplanetlabs/agent-o-rama/wiki/Datasets,-evaluators,-and-experiments).

### Online actions

Actions can be set up via the UI to run automatically on the results of production runs. Actions can do online evaluation, add to datasets, trigger webhooks, or run any custom function. Actions receive as input the run input/output, run statistics (e.g. latency, token counts), and any errors during the run. Actions can set a sampling rate or filter for runs matching particular parameters.

Online evaluation gets added as feedback on the run that is viewable in traces, and time-series charts are automatically created that are viewable in the [analytics section](https://github.com/redplanetlabs/agent-o-rama/wiki/Actions,-rules,-and-telemetry#time-series-telemetry). Here's an example of setting up an action to do online evaluation:

![Online evaluation](images/action-eval.png)

Here's an example of creating an action to add slow runs to a dataset:

![Add to dataset](images/action-dataset.png)

See [this page](https://github.com/redplanetlabs/agent-o-rama/wiki/Actions,-rules,-and-telemetry) for the details on creating actions.

### Human feedback

A comprehensive [human feedback](https://github.com/redplanetlabs/agent-o-rama/wiki/Human-feedback) system integrates both structured and unstructured human evaluation directly into your agent development workflow.

Feedback queues organize evaluation work across your team. Automatically route agent runs for review, configure which feedback to collect, and give reviewers a streamlined interface that displays inputs and outputs alongside the feedback form, advancing to the next item after submission.

![Human feedback](images/human-queue-item-form.png)

Feedback can also be recorded directly on traces, making it easy to record insights during debugging or analysis.

Human feedback flows into the telemetry system where you can visualize trends over time and track whether agent improvements align with human evaluations.


### Time-series telemetry

Agent-o-rama automatically tracks time-series telemetry for all aspects of agent execution.

![Metrics dashboards 1](images/telemetry1.png)
![Metrics dashboards 2](images/telemetry2.png)
![Metrics dashboards 3](images/telemetry3.png)

You can also attach metadata to any agent invoke, and all time-series telemetry can be split by the values for each metadata key. So if one of your metadata keys is the choice of model to use, you can see how invokes, token counts, latencies, and everything else vary by choice of model.

![Metrics dashboards 1](images/telemetry-metadata.png)


# Actions, Rules, and Telemetry

Agent-o-rama's actions and rules system enables you to set up automatic actions that run on production agent or node executions. You define filtering criteria to select which runs should trigger actions, and the system automatically executes those actions asynchronously.

This system is useful for everything from running evaluators on production runs to triggering webhooks to running whatever custom logic you need. You might use actions to continuously evaluate agent quality, automatically add interesting examples to training datasets, or send alerts when errors occur.

## Table of Contents
- [Rules and Actions](#rules-and-actions)
- [Built-in Actions](#built-in-actions)
- [Custom Actions](#custom-actions)
- [Action Logs](#action-logs)
- [Time-Series Telemetry](#time-series-telemetry)

## Rules and Actions

Rules specify when to run an action. When an agent execution completes, the system evaluates all active rules against that execution. If the filter matches the run, the action executes with access to the run's inputs, outputs, metadata, timing information, and any feedback from other actions. The system maintains per-task cursors that track processing progress, ensuring actions only run once per matching execution even across cluster restarts or module updates.

Rules can be configured to run an action against either the entire agent execution or individual node executions within an agent, giving you fine-grained control over what gets processed. When targeting a specific node, the action receives data from just that node's execution, including its inputs, outputs, and any nested operations like model calls or database queries.

### Creating a rule

Rules are created on the "Rules/Actions" page for an agent. The form to create a rule looks like this:

![Create rule](images/create-rule.png)

The initial fields in the form are:

1. **Rule name**: this names the rule, and if the rule performs online evaluation can also be referenced by other rules as a dependency
2. **Scope**: Specify whether to run on the agent as a whole or on particular node runs of the agent
3. **Status filter**: Whether to execute on successful runs, failed runs, or all runs
4. **Sampling rate**: Number between 0 and 1.0 specifying on what percentage of runs to execute
5. **Start time**: How far back to start applying the rule. This is used to backfill application of the rule, especially for online evaluation where you may want to know about production performance in the past.
6. **Action**: Select which action to run when the rule matches. The list of actions includes built-in actions as well as any custom actions declared in the module. Upon selecting an action, you will be prompted to provide the parameters for the action builder.

The last section is to specify filters for the rule, which is optional. If you don't specify any filters, it will match all runs for the preceding criteria. Filters are able to be based on the input/output of the run, the stats on the run (e.g. latency, token counts), the results of other rules doing online evaluation, and more. Filters can be composed with "and", "not", and "or" to specify even more fine-grained behavior. For example, here are filters set up to only match runs with a latency more than 5s and with a feedback score filter from another rule (zoomed out so you can see most of the form):

![Create rule](images/filters-example.png)



## Built-in Actions

### aor/eval

The `aor/eval` action runs an evaluator on an agent or node execution and attaches the resulting feedback to that execution. This enables continuous evaluation of agent performance in production, with the feedback viewable in traces and contributing to agent analytics. Selecting this action simply requires specifying which evaluator to use.

### aor/add-to-dataset

The `aor/add-to-dataset` action automatically adds agent or node runs to a dataset. This is particularly useful for building training datasets from production usage, collecting edge cases that surfaced in real deployments, or systematically gathering examples for future experimentation.

![Add to Dataset Action Builder](images/add-to-dataset-action.png)

The action requires specifying which dataset to add to as well as [JSON path templates](Datasets,-evaluators,-and-experiments#experiments) for the input and output for the new example. The JSON path templates allow the input/output from the run to be transformed to the shape desired for the dataset.

The input to the run is a list of arguments to the agent or node. For an agent, the output is the agent result. For a node, the run output is the agent result if the node set it, or otherwise a list of emit information that looks like:

```
[{"node": "node2", "args": ["arg1", 2]}, {"node": "node3", "args": [1]}]
```

If you set the JSON path template for the input to `$[0]`, then the example's input will be the first argument to the run. Setting the output JOSN path template to `{"result": "$[0].args[0]"}` would set the example's output to `{"result": "arg1"}` when run on the above node emit example.

### aor/webhook

The `aor/webhook` action posts a JSON payload to an external URL, enabling integration with things like monitoring systems and alerting platforms.

![Webhook Action Builder](images/webhook-action.png)

You configure the URL, timeout, HTTP headers, and the payload template. The template can include placeholders that get replaced with the actual input, output, and comprehensive run information including timing, feedback, and operation details.

## Custom Actions

You can declare custom action builders in your agent topology to create actions tailored to your specific needs. Action builders are similar to evaluator builders - they take in a params map and return the function to perform the action.

The action function receives four arguments. The first is a fetcher object that provides access to all declared agent objects like models, stores, or databases. The second is the input from the agent or node execution. The third is the run's output. The last is a "run info" object containing comprehensive metadata about the execution like stats, feedback, and timing information.

Actions return a map with string keys. This map gets recorded in the action log for any rules using this builder, allowing you to track what each action produced and use that information in dependent actions or telemetry queries. More information about action logs is in the next section.

Here's an example of declaring a custom action in a module:

#### Java API

```java
@Override
protected void defineAgents(AgentTopology topology) {
  topology.declareActionBuilder("print-action",
    "Prints info about the run to stdout",
    (Map<String, String> params) -> {
      String logLevel = params.get("logLevel");
      return (AgentObjectFetcher fetcher, Object input, Object output, RunInfo runInfo) -> {
        // Custom action logic here
        System.out.println(String.format("[%s] Agent: %s, Latency: %d ms",
          logLevel, runInfo.getAgentName(), runInfo.getLatencyMillis()));

        return new HashMap<String, Object>() {{
          put("logged", true);
        }};
      };
    },
    ActionBuilderOptions.params("logLevel", "Log level (info/debug)", "info"));
}
```

#### Clojure API

```clojure
(aor/defagentmodule MyModule
  [topology]
  (aor/declare-action-builder
    topology
    "print-action"
    "Prints info about the run to stdout"
    (fn [params]
      (let [log-level (get params "logLevel")]
        (fn [fetcher input output run-info]
          ;; Custom action logic here
          (println (format "[%s] Agent: %s, Latency: %s ms"
                           log-level
                           (:agent-name run-info)
                           (:latency-millis run-info)))

          {"logged" true})))
    {:params {"logLevel" {:description "Log level (info/debug)" :default "info"}}})

  ;; ... rest of module definition ...
  )
```


## Action Logs

Every action execution is logged with detailed information about what happened. The action log includes when the action started and finished, which agent or node execution it processed, whether it succeeded or failed, and any information returned from the action.

Action logs are accessible from the list of rules for the agent:

![Rules list](images/rules-list.png)

Clicking on "View" in the "Action Log" column goes to this page:

![Action](images/action-log.png)

From here you can navigate to the trace of the agent run associated with each log and see detailed information about the action's execution.

When actions fail, the error information is captured in the log, making it easy to understand what went wrong without needing to query external systems. The logs include both the exception message and stack trace, providing full context for troubleshooting.

## Time-Series Telemetry


The analytics page for an agent shows time-series telemetry from all agent and node executions, aggregating a wide variety metrics at multiple granularities. Agent-level metrics include success rates, end-to-end latency, token counts, and time-to-first-token for streaming responses. Model-level metrics track call counts, success rates, and latencies for LLM interactions. Store and database operations are tracked with read and write latencies,

![Metrics dashboards 1](images/telemetry1.png)
![Metrics dashboards 2](images/telemetry2.png)
![Metrics dashboards 3](images/telemetry3.png)

You can change the granularity and time window viewed for all charts. Additionally, you can split all charts by any [metadata](Programming-agents#metadata) key used across all agent invocations:

![Metrics dashboards 1](images/telemetry-metadata.png)

In this case, one of the metadata keys is "model", and there are twice as many invokes with "openai" as the other models. You're able to see how the choice of model changes agent latency, token counts, or any other metric.

Besides being a parameter for agent execution, metadata also is used for analytics to provide further capabilities. Agent-o-rama automatically aggregates per metadata value as well as for the time bucket as a whole.

At most five metadata values are tracked per bucket, so metadata telemetry works best for low-cardinality metadata keys.

### Evaluator Telemetry

When rules run evaluators on agent executions, the resulting feedback scores automatically become additional telemetry metrics at the bottom of the analytics page. Each evaluator rule produces time-series data for every score it generates, enabling continuous monitoring of quality metrics alongside operational metrics.

![Evaluator telemetry](images/telemetry-eval.png)


# Agent Client API

The Agent Client API is how you interact with deployed agents from your application code. It provides methods for invoking agents, tracking executions, and retrieving results.

## Table of Contents

1. [Getting an Agent Client](#getting-an-agent-client)
2. [Invoking Agents](#invoking-agents)
3. [Initiating Agent Executions](#initiating-agent-executions)
4. [Invoking with Metadata](#invoking-with-metadata)
5. [Getting Agent Results](#getting-agent-results)
6. [Other Features](#other-features)

## Getting an Agent Client

Before you can invoke an agent, you need to get an `AgentClient` instance. This requires an `AgentManager`, which you create from a Rama cluster and module name.

### Cluster Managers

The `AgentManager` is created from a cluster manager, which is the interface to a Rama cluster. There are two types of cluster managers:

1. **InProcessCluster (IPC)**: For local development and testing. Runs a complete Rama cluster in a single JVM process.
2. **RamaClusterManager**: For production. Connects to a deployed Rama cluster.

Both implement the same `ClusterManagerBase` interface, so your agent code works the same way in development and production.

### Local Development with InProcessCluster

For development and testing, use `InProcessCluster` which runs everything locally:

#### Java API

```java
import com.rpl.agentorama.*;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

try (InProcessCluster ipc = InProcessCluster.create()) {
  // Launch your agent module
  MyAgentModule module = new MyAgentModule();
  ipc.launchModule(module, new LaunchConfig(4, 2));

  // Create agent manager from IPC
  String moduleName = module.getModuleName();
  AgentManager manager = AgentManager.create(ipc, moduleName);

  // Get client for a specific agent
  AgentClient agent = manager.getAgentClient("MyAgent");

  // List all available agents in the module
  Set<String> agentNames = manager.getAgentNames();
  System.out.println("Available agents: " + agentNames);
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor]
         '[com.rpl.rama :as rama]
         '[com.rpl.rama.test :as rtest])

(with-open [ipc (rtest/create-ipc)]
  ;; Launch your agent module
  (rtest/launch-module! ipc MyAgentModule {:tasks 4 :threads 2})

  ;; Create agent manager from IPC
  (let [module-name (rama/get-module-name MyAgentModule)
        manager (aor/agent-manager ipc module-name)]

    ;; Get client for a specific agent
    (let [agent (aor/agent-client manager "MyAgent")]

      ;; List all available agents in the module
      (println "Available agents:" (aor/agent-names manager)))))
```

### Production with RamaClusterManager

For production, connect to a deployed Rama cluster using `RamaClusterManager`:

#### Java API

```java
import com.rpl.agentorama.*;
import com.rpl.rama.RamaClusterManager;

// Connect to production cluster
try (RamaClusterManager cluster = RamaClusterManager.open(Map.of("conductor.host", "1.2.3.4"))) {
  // Create agent manager for deployed module
  AgentManager manager = AgentManager.create(cluster, "MyModule");

  // Get client for a specific agent
  AgentClient agent = manager.getAgentClient("MyAgent");

  // Use the agent
  String result = agent.invoke("input data");
  System.out.println("Result: " + result);
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor]
         '[com.rpl.rama :as rama])

;; Connect to production cluster
(with-open [cluster (rama/open-cluster {"conductor.host" "1.2.3.4"})]
  ;; Create agent manager for deployed module
  (let [manager (aor/agent-manager cluster "MyModule")]

    ;; Get client for a specific agent
    (let [agent (aor/agent-client manager "MyAgent")]

      ;; Use the agent
      (let [result (aor/agent-invoke agent "input data")]
        (println "Result:" result)))))
```

## Invoking Agents

The simplest way to use an agent is to invoke it synchronously. This blocks until the agent completes and returns the final result.

### Synchronous Invocation

Use `invoke` when you want to wait for the agent to complete before continuing.

#### Java API

```java
// Single argument
String result = agent.invoke("Hello, world!");

// Multiple arguments
Map<String, Object> result = agent.invoke("query", "context", 42);
```

#### Clojure API

```clojure
;; Single argument
(def result (aor/agent-invoke agent "Hello, world!"))

;; Multiple arguments
(def result (aor/agent-invoke agent "query" "context" 42))
```

### Asynchronous Invocation

Use `invokeAsync` / `agent-invoke-async` when you want non-blocking execution. This returns a `CompletableFuture` that completes with the result.

#### Java API

```java
import java.util.concurrent.CompletableFuture;

// Start async invocation
CompletableFuture<String> future = agent.invokeAsync("Hello, world!");

// Do other work while agent executes
System.out.println("Agent is running...");

// Wait for result when needed
String result = future.get();
System.out.println("Result: " + result);

// Or use callbacks
future.thenAccept(result -> {
  System.out.println("Agent completed with: " + result);
});
```

#### Clojure API

```clojure
;; Start async invocation
(let [future (aor/agent-invoke-async agent "Hello, world!")]

  ;; Do other work while agent executes
  (println "Agent is running...")

  ;; Wait for result when needed
  (let [result (.get future)]
    (println "Result:" result))

  ;; Or use callbacks
  (.thenAccept future
    (reify java.util.function.Consumer
      (accept [_ result]
        (println "Agent completed with:" result)))))
```

## Initiating Agent Executions

For more control over agent execution (e.g., for streaming or human input), use `initiate` to start an execution and get a handle for tracking it.

The `initiate` method returns an `AgentInvoke` handle that you can use with other methods like `result`, `nextStep`, `stream`, etc.

### Java API

```java
// Initiate agent execution
AgentInvoke invoke = agent.initiate("Hello, world!");

// The agent is now running asynchronously
// You can use the invoke handle to:
// - Get the result: agent.result(invoke)
// - Stream data: agent.stream(invoke, "node-name", callback)
// - Handle human input: agent.nextStep(invoke)

// Get the final result (blocks until complete)
String result = agent.result(invoke);
```

### Clojure API

```clojure
;; Initiate agent execution
(let [invoke (aor/agent-initiate agent "Hello, world!")]

  ;; The agent is now running asynchronously
  ;; You can use the invoke handle to:
  ;; - Get the result: (aor/agent-result agent invoke)
  ;; - Stream data: (aor/agent-stream agent invoke "node-name" callback)
  ;; - Handle human input: (aor/agent-next-step agent invoke)

  ;; Get the final result (blocks until complete)
  (let [result (aor/agent-result agent invoke)]
    (println "Result:" result)))
```

### Async Initiation

You can also initiate asynchronously to get a future that completes with the invoke handle. This usually complete within a few milliseconds:

#### Java API

```java
CompletableFuture<AgentInvoke> future = agent.initiateAsync("Hello, world!");

future.thenAccept(invoke -> {
  // Use the invoke handle
  String result = agent.result(invoke);
  System.out.println("Result: " + result);
});
```

#### Clojure API

```clojure
(let [future (aor/agent-initiate-async agent "Hello, world!")]
  (.thenAccept future
    (reify java.util.function.Consumer
      (accept [_ invoke]
        ;; Use the invoke handle
        (let [result (aor/agent-result agent invoke)]
          (println "Result:" result))))))
```

## Invoking with Metadata

Metadata allows you to attach custom key-value data to agent executions. This is useful for:
- **Tracking**: User IDs, session IDs, request IDs for correlating agent executions
- **A/B Testing**: Model versions, feature flags, experimental configurations
- **Configuration**: Runtime parameters like model names that agents can access
- **Debugging**: Additional context for troubleshooting specific executions

Metadata is automatically included in traces and analytics, making it easy to filter and analyze agent performance by any metadata dimension. For example, you can set a `"model"` metadata field and then view separate analytics for each model version to compare performance.

### Creating Metadata Context

Metadata is passed via an `AgentContext` object in Java or a map with a `:metadata` key in Clojure. Metadata keys must be strings, and values must be strings, numbers (int, long, float, double), or booleans.

#### Java API

```java
import com.rpl.agentorama.AgentContext;

AgentContext context = AgentContext.metadata("user-id", "user-123")
                                   .metadata("model", "gpt-4");
```

#### Clojure API

```clojure
;; Create context with metadata
(def context {:metadata {"user-id" "user-123"
                         "model" "gpt-4"}})
```

### Invoking with Metadata

Use `invokeWithContext` / `agent-invoke-with-context` to invoke an agent synchronously with metadata.

#### Java API

```java
// Invoke with metadata (blocks until complete)
AgentContext context = AgentContext.metadata("user-id", "user-123")
                                   .metadata("model", "gpt-4");

String result = agent.invokeWithContext(context, "Hello, world!");
System.out.println("Result: " + result);

// Invoke asynchronously with metadata
CompletableFuture<String> future = agent.invokeWithContextAsync(context, "Hello, world!");
String result2 = future.get();
```

#### Clojure API

```clojure
;; Invoke with metadata (blocks until complete)
(let [context {:metadata {"user-id" "user-123"
                          "model" "gpt-4"}}
      result (aor/agent-invoke-with-context agent context "Hello, world!")]
  (println "Result:" result))

;; Invoke asynchronously with metadata
(let [context {:metadata {"user-id" "user-123"
                          "model" "gpt-4"}}
      future (aor/agent-invoke-with-context-async agent context "Hello, world!")
      result (.get future)]
  (println "Result:" result))
```

### Initiating with Metadata

Use `initiateWithContext` / `agent-initiate-with-context` to start an agent execution with metadata and get a handle for tracking.

#### Java API

```java
// Initiate with metadata
AgentContext context = AgentContext.metadata("user-id", "user-123")
                                   .metadata("session-id", "session-456");

AgentInvoke invoke = agent.initiateWithContext(context, "Hello, world!");

// Use the invoke handle for streaming, human input, etc.
agent.stream(invoke, "process", (allChunks, newChunks, reset, complete) -> {
  // Handle streaming...
});

// Get the final result
String result = agent.result(invoke);

// Initiate asynchronously with metadata
CompletableFuture<AgentInvoke> futureInvoke =
  agent.initiateWithContextAsync(context, "Hello, world!");
```

#### Clojure API

```clojure
;; Initiate with metadata
(let [context {:metadata {"user-id" "user-123"
                          "session-id" "session-456"}}
      invoke (aor/agent-initiate-with-context agent context "Hello, world!")]

  ;; Use the invoke handle for streaming, human input, etc.
  (aor/agent-stream agent invoke "process"
    (fn [all-chunks new-chunks reset? complete?]
      ;; Handle streaming...
      ))

  ;; Get the final result
  (let [result (aor/agent-result agent invoke)]
    (println "Result:" result)))

;; Initiate asynchronously with metadata
(let [context {:metadata {"user-id" "user-123"
                          "session-id" "session-456"}}
      future-invoke (aor/agent-initiate-with-context-async agent context "Hello, world!")]
  ;; Use the future...
  )
```

## Getting Agent Results

There are several ways to get results from agent executions, depending on whether you used `invoke` or `initiate`.

### With invoke/invokeAsync

When using `invoke`, the result is returned directly:

```java
// Java - result is returned
String result = agent.invoke("input");
```

```clojure
;; Clojure - result is returned
(def result (aor/agent-invoke agent "input"))
```

### With initiate

When using `initiate`, use the `result` method with the invoke handle:

#### Java API

```java
// Initiate execution
AgentInvoke invoke = agent.initiate("input");

// Get result (blocks until complete)
String result = agent.result(invoke);

// Or get result asynchronously
CompletableFuture<String> futureResult = agent.resultAsync(invoke);
```

#### Clojure API

```clojure
;; Initiate execution
(let [invoke (aor/agent-initiate agent "input")]

  ;; Get result (blocks until complete)
  (let [result (aor/agent-result agent invoke)]
    (println "Result:" result))

  ;; Or get result asynchronously
  (let [future-result (aor/agent-result-async agent invoke)]
    (.thenAccept future-result
      (reify java.util.function.Consumer
        (accept [_ result]
          (println "Result:" result))))))
```

### Checking Completion Status

You can check if an agent execution is complete:

#### Java API

```java
AgentInvoke invoke = agent.initiate("input");

if (agent.isAgentInvokeComplete(invoke)) {
  String result = agent.result(invoke);
  System.out.println("Already complete: " + result);
} else {
  System.out.println("Still running...");
}
```

#### Clojure API

```clojure
(let [invoke (aor/agent-initiate agent "input")]
  (if (aor/agent-invoke-complete? agent invoke)
    (let [result (aor/agent-result agent invoke)]
      (println "Already complete:" result))
    (println "Still running...")))
```

## Other Features

The Agent Client API provides additional features:

### Streaming

For real-time feedback as agents process data, use streaming. See the [Streaming documentation](Streaming) for details.

### Human Input

For human-in-the-loop patterns, agents can request human input during execution. See the [Human-in-the-loop documentation](Human‐in‐the‐loop) for details.


# Datasets, Evaluators, and Experiments

Building reliable LLM agents requires systematic evaluation. Without proper testing, you can't know if your agent is actually improving or if changes are making things worse. Agent-o-rama's evaluation framework addresses the core challenges of LLM agent development:

- **Subjective Assessment**: Manual testing is time-consuming and inconsistent
- **Edge Case Discovery**: Real-world usage reveals unexpected behaviors
- **Regression Detection**: Changes can break previously working functionality
- **Performance Measurement**: Quantifying improvement requires systematic metrics

This page explodes Agent-o-rama's features for evaluating agent performance systematically and rigorously:

- **Structured Datasets**: Organized test cases with inputs and optional reference outputs
- **Evaluators**: Measure performance using either custom functions or LLM judges
- **Experiments**: Run controlled tests across different agents and configurations

## Table of Contents
- [Datasets](#datasets)
- [Remote datasets](#remote-datasets)
- [Evaluators](#evaluators)
- [Experiments](#experiments)
- [Comparative experiments](#comparative-experiments)

## Datasets

Datasets are collections of structured examples used for testing and evaluating agents. Each example contains input data, an optional reference output, and any number of tags. Experiments are able to run on entire datasets or subsets of a dataset.

Reference outputs are the "correct" or expected responses for a given input. They serve multiple purposes:

- **Quality Assessment**: Compare agent outputs against known good responses
- **Regression Testing**: Ensure changes don't break previously working functionality
- **Performance Measurement**: Quantify how close agent outputs are to ideal responses

There are many cases where you don't have a good reference output for an example, which is why its optional. Perhaps you're just collecting a set of real world prompts, or you want a subject matter expert to review the example and develop a reference output later.

### Creating Datasets

Datasets can be created through the UI or programmatically. Each dataset has a name, description, and optional [JSON schemas](https://json-schema.org/) for input and output validation. Configuring JSON schemas is highly recommended to prevent mistakes.

![Dataset Creation UI](images/dataset-creation.png)

Here are examples of simple JSON schemas for common use cases:

**Object Schema:**
```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "User's question or request"
    },
    "context": {
      "type": "object",
      "properties": {
        "user_type": {"type": "string"},
        "priority": {"type": "string", "enum": ["low", "medium", "high"]}
      }
    }
  },
  "required": ["query"]
}
```

**String Schema:**
```json
{
  "type": "string",
  "maxLength": 1000
}
```

#### Java API

```java
AgentManager manager = AgentManager.create(cluster, moduleName);

// Create a dataset with JSON schemas for validation
UUID datasetId = manager.createDataset(
    "customer-support-examples",
    "Customer support conversation examples for testing response quality",
    "{\"type\": \"object\", \"properties\": {\"query\": {\"type\": \"string\"}, \"context\": {\"type\": \"object\"}}}",
    "{\"type\": \"string\", \"maxLength\": 1000}"
);
```

#### Clojure API

```clojure
(def manager (aor/agent-manager cluster module-name))

;; Create a dataset with JSON schemas for validation
(def dataset-id
  (aor/create-dataset! manager
    "customer-support-examples"
    {:description "Customer support conversation examples for testing response quality"
     :input-json-schema "{\"type\": \"object\", \"properties\": {\"query\": {\"type\": \"string\"}, \"context\": {\"type\": \"object\"}}}"
     :output-json-schema "{\"type\": \"string\", \"maxLength\": 1000}"}))
```

### Adding Examples

There are several ways to add examples to datasets:

#### 1. Manually via the UI

From the dataset page, you can add an example manually:

![Add from Trace](images/add-dataset-manual.png)


#### 2. From an agent or node in a trace

You can also add an example from the trace UI:

![Add from Trace](images/add-dataset-trace.png)

On the right is a button to add the input/output of the agent as a whole, and on the bottom is a button to add just the input/output for the selected node. In either case, it brings up a dialog to select the dataset to add to and lets you edit the input/output before submitting it.

#### 3. Bulk import from a JSONL file

You can import from JSON files via the UI:

![Bulk Import UI](images/bulk-import.png)

A [JSONL](https://jsonlines.org/) file encodes a JSON-encoded example on each line with "input", "output", and "tags" fields, like so:

```json
{"input": "The Earth orbits the Sun.", "output": "factual"}
{"input": "Fish can breathe underwater.", "output": "factual", "tags": ["tag1", "tag2"]}
{"input": "Bananas are made of steel.", "output": "nonsense", "tags": ["tag1"]}
```


#### 4. Programmatically

You can add examples programmatically using the AgentManager API. This is useful for automated data collection or integration with other systems.

**Java:**
```java
// Add a single example with reference output and tags
AddDatasetExampleOptions options = new AddDatasetExampleOptions();
options.referenceOutput = new HashMap<String, Object>() {{
    put("response", "You can reset your password by clicking 'Forgot Password' on the login page.");
    put("confidence", 0.95);
}};

UUID exampleId = manager.addDatasetExample(datasetId,
    "How do I reset my password?"
    options);
```

**Clojure:**
```clojure
;; Add a single example with reference output and tags
(aor/add-dataset-example! manager dataset-id
  "How do I reset my password?"
  {:reference-output {"response" "You can reset your password by clicking 'Forgot Password' on the login page."
                      "confidence" 0.95}
   :tags #{"password" "authentication" "common"}})
```

#### 5. Add a filtered sample from production runs with an online action

You can automatically collect examples from production agent runs using [actions](Actions,-rules,-and-telemetry). Actions are rules that run automatically when certain conditions are met during agent execution. You could use actions to:

**Example Use Cases:**
- Collect successful customer support interactions
- Make a dataset of all high latency interactions for later analysis
- Make a dataset of 0.1% of all runs
- Capture edge cases that caused agent failures

This enables continuous agent improvement based on actual performance in production.

### Dataset Snapshots

Snapshots create immutable versions of datasets at specific points in time, enabling reproducible experiments and version control. You can snapshot datasets in the UI:

![Dataset Snapshots](images/new-snapshot.png)


You can also snapshot datasets programmatically ([Javadoc](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentManager.html#snapshotDataset(java.util.UUID,java.lang.String,java.lang.String)) and [Clojuredoc](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.html#var-snapshot-dataset.21)).

### Example Management

You can edit example inputs/outputs/tags at any time either via the UI or programmatically, and the UI allows you to search for examples by a matching string.


## Remote datasets

When creating a dataset, there's an option to create a remote dataset:

![Remote Datasets](images/remote-dataset.png)

A remote dataset can be on a different module in the same cluster, or it can be on a module in a different cluster. When you provide the location in this form, it creates a local proxy for that dataset which you can use to run experiments against its examples.

Note that creating a remote dataset on a different cluster requires that cluster to be using the same version of Rama.

When creating an experiment on a remote dataset, there's also an option when selecting evaluators to select remote evaluators. This uses the evaluator definitions from that remote module locally. This requires that you have the same evaluator builders defined locally. Using remote evaluators saves a step during testing of having to create those evaluators in your local environment before running the experiment.

The primary use case for remote datasets are to test agents or new versions of agents in [IPC](Programming-agents#local-development) before deploying to a production cluster.

## Evaluators

Evaluators are arbitrary functions that measure agent performance by analyzing outputs and producing scores. They're the core of systematic agent evaluation. They're used for both offline experiments against datasets and for [online evaluation](Actions,-rules,-and-telemetry) of production runs. They can be anything from a simple custom Java function checking for a regular expression match to an LLM judge prompted to provide a score for helpfulness.

Evaluators are created through a two-step process:

1. **Select an Evaluator Builder**: Choose from built-in builders or a custom builder declared as part of the module
2. **Create an Evaluator Instance**: Provide a name and parameters to instantiate the evaluator

Evaluator builders define what parameters the evaluator accepts, a description of what it does (displayed in the UI), and the actual evaluation logic.

You can declare your own evaluator builders in your module to create evaluators tailored to your specific needs.

### What Evaluators Do

Evaluators can be run against an agent as a whole or a particular node of an agent execution. These are classified generally as "runs".

Evaluators take the run input, example reference output (if there is one), and the run output, and return a map of score names to score values. For an agent, the run outputs is just the result of the agent. For a node, the run output is the agent result if the node set it, or otherwise a list of emit information that looks like:

```
[{"node": "node2", "args": ["arg1", 2]}, {"node": "node3", "args": [1]}]
```

This shows which nodes were emitted to with which arguemnts and in which order.

Evaluators return maps where each key is a score name and each value is a score. Scores can be:

- **Numbers**: Quantitative metrics (0.0 to 1.0, 1 to 10, percentages, etc.)
- **Booleans**: Binary classifications (true/false, pass/fail)
- **Strings**: Categorical classifications ("factual" vs "nonsense", "toxic" vs "non-toxic")

**Examples:**
```clojure
;; Number scores for quantitative assessment
{"accuracy" 0.85, "helpfulness" 8.5, "relevance" 0.92}

;; Boolean scores for binary classification  
{"concise?" true, "factual?" false, "appropriate?" true}

;; String scores for categorical classification
{"sentiment" "positive", "category" "technical", "quality" "high"}
```

Most evaluators return just one score, but there are cases where you may want an evaluator to compute multiple scores at the same time. For instance, you may want an LLM to judge both "relevance" and "completeness".

There are three types of evaluators:

#### Regular Evaluators

Regular evaluators assess an individual agent or node run. They receive three inputs:

1. **Input**: The input to the agent
2. **Reference Output**: The "correct" or expected output from the example, if one exists
3. **Run Output**: What the run actually produced

**Example Scenario:**
- **Input**: `"What is the capital of France?"`
- **Reference output**: `"Paris"`
- **Run Output**: `{"answer": "The capital of France is Paris.", "confidence": 0.95}`
- **Evaluator Result**: `{"accuracy": 0.95}`

#### Comparative Evaluators

Comparative evaluators are used in [comparative experiments](#comparative-experiments). Sometimes its easier to say which response is better than to independently score each response individually. For example, consider these two responses to "How can I fall asleep faster at night?"

```
Response 1
----------
Try keeping a consistent bedtime and avoiding screens before bed.


Response 2
----------
Go to bed at the same time each night, keep your room dark and cool, and avoid screens or bright lights for at least 30 minutes before bed. If your mind is racing, try a simple routine like slow breathing or writing down tomorrow’s tasks. Avoid caffeine after noon and heavy meals close to bedtime — consistency and calm are what cue your body to sleep faster.
```

Individually, they would both score high for being accurate, helpful, and concise. But response 2 is clearly better.

Comparative evaluators receive as input:
1. **Input**: The input to the run
2. **Reference Output**: The "correct" or expected output from the example, if one exists.
3. **Multiple Run Outputs**: A list of outputs to compare

Just like other evaluator types, comparative evaluators return a map of scores. The `"index"` score is treated specially in the UI, as in the comparative experiment results it will highlight the winning output. For example, `{"index": 1}` means the second output (index 1) is the best.

#### Summary Evaluators

Some metrics can only be calculated by looking at all examples together, like false positive rate, precision, and recall scores. Those cannot be scored by looking at one example at a time. Summary evaluators are able to calculate these kinds of scores.

Summary evaluators receive a list of all the individual example runs from the experiment. Each example run contains the input, reference output, and output.

**Example Scenario:**
- **Input**: 100 question-answering examples
- **Individual Results**: Each example gets scored individually
- **Summary Evaluation**: Calculate accuracy across all examples
- **Result**: `{"accuracy": 0.87}`


### Creating Evaluators

You can create evaluators in the evaluators section of the UI. After selecting an evaluator builder, this dialog comes up:

![Create evaluator](images/create-evaluator-conciseness.png)

An evaluator is given a name, description, and values for all parameters for the selected evaluator builder. Additionally, you need to specify how to extract data from the input, reference output, and outputs.

The input, reference output, and output may not be in the shape necessary for the evaluator function. For example, consider the built-in "aor/conciseness" evaluator. This requires the output to be a plain string so it can check the length. But perhaps your agent outputs data of the form:

```
{"result": "abcdef",
 "metadata": {"confidence": 0.8, "documents-searched": 25}}
```

Rather than have to write the same evaluator again for a slightly different input shape, the input/reference output/output lets you use the same evaluator builder on data of any shape.

[JSON paths](https://en.wikipedia.org/wiki/JSONPath) are used to specify what data to extract. For this particular example, the JSON path would be `$.result`.

Evaluator builders may not look at all of the input, reference output, and output (e.g. "aor/conciseness" only looks at the output), and their definitions specify which JSON paths need to be provided. In this screenshot you can see it only requests a JSON path for the output. JSON paths default to `$`, which just passes the whole object in as the argument.

### Built-in Evaluator Builders

Agent-o-rama provides several built-in evaluator builders for common evaluation tasks:

#### LLM Judge
**Type**: Regular
**Purpose**: Uses LLM to assess output quality

**Parameters:**
- `model`: Name of the model to use (must be declared as an agent object)
- `prompt`: Evaluation prompt with %input, %output, %referenceOutput variables
- `temperature`: Model temperature (default: 0.0)
- `outputSchema`: JSON schema for the output (default: single "score" field 0-10)

**Example Use Cases:**
- Rate helpfulness on a scale of 1-10
- Check if output is factual vs. opinion
- Assess creativity or engagement

![LLM Judge Configuration](images/create-evaluator-llm-judge.png)

#### Conciseness
**Type**: Regular  
**Purpose**: Checks if outputs meet length requirements

**Parameters:**
- `threshold`: Maximum character length allowed

![Concisenss evaluator](images/create-evaluator-conciseness.png)

#### F1 Score
**Type**: Summary  
**Purpose**: Measures [F1 score](https://en.wikipedia.org/wiki/F-score), precision, and recall

**Parameters:**
- `positiveValue`: Value considered a positive classification, e.g. "toxic" for a task where the possible outputs are "toxic" or "not toxic"

![F1 Score Configuration](images/create-evaluator-f1.png)



### Custom Evaluator Builders

You can declare custom evaluator builders in your agent topology to create evaluators tailored to your specific needs. Custom evaluators give you complete control over the evaluation logic and can access any agent objects declared in your topology like LLMs, stores, or databases.

**Evaluator Function Signatures:**

The function signature depends on the evaluator type. All custom evaluators receive a fetcher object as their first argument, which provides access to all declared agent objects (models, stores, databases, etc.) using the [same API](Programming-agents#agent-objects) as used within node functions. The remaining arguments depend on the evaluator type:

- **Regular Evaluators**: `(fetcher, input, referenceOutput, output) -> Map<String, Object>`
- **Comparative Evaluators**: `(fetcher, input, referenceOutput, outputs) -> Map<String, Object>`
- **Summary Evaluators**: `(fetcher, exampleRuns) -> Map<String, Object>`

The evaluator builder is a function taking in a params map that returns the evaluator function. The parms map is always a map of param name (string) to param value (also string).

The Java API allows you to specify whatever types you want for the input, referenceOutput, and output args.

**Regular Evaluator Example:**

```java
topology.declareEvaluatorBuilder("accuracy-checker",
    "Checks if output contains the correct answer",
    (Map<String, String> params) -> {
        return (AgentObjectFetcher fetcher, Integer input, String referenceOutput, Map output) -> {
            // Custom evaluation logic here
            return new HashMap<String, Object>() {{
                put("accuracy", 0.85);
            }};
        };
    },
    EvaluatorBuilderOptions.param("threshold", "Accuracy threshold", "0.8"));
```

```clojure
(aor/declare-evaluator-builder
  topology
  "accuracy-checker"
  "Checks if output contains the correct answer"
  (fn [params]
    (fn [fetcher input reference-output output]
      ;; Custom evaluation logic here
      {"accuracy" 0.85}))
  {:params {"threshold" {:description "Accuracy threshold" :default "0.8"}}})
```

**Comparative Evaluator Example:**

```java
topology.declareComparativeEvaluatorBuilder("quality-ranker",
    "Ranks outputs by quality",
    (Map<String, String> params) -> {
        return (AgentObjectFetcher fetcher, Object input, String referenceOutput, List<Object> outputs) -> {
            // Custom comparison logic here
            return new HashMap<String, Object>() {{
                put("index", 1);  // Second output is best
                put("confidence", 0.9);
            }};
        };
    },
    EvaluatorBuilderOptions.param("model", "Model to use for ranking", "gpt-4"));
```

```clojure
(aor/declare-comparative-evaluator-builder
  topology
  "quality-ranker"
  "Ranks outputs by quality"
  (fn [params]
    (fn [fetcher input reference-output outputs]
      ;; Custom comparison logic here
      {"index" 1 "confidence" 0.9}))
  {:params {"model" {:description "Model to use for ranking" :default "gpt-4"}}})
```

**Summary Evaluator Example:**

Summary evaluators analyze collections of example runs to provide overall metrics. In Clojure, the fields of `example-runs` can be accessed with `:input`, `:reference-output`, and `:output`.

```java
topology.declareSummaryEvaluatorBuilder("overall-quality",
    "Calculates overall quality metrics",
    (Map<String, String> params) -> {
        return (AgentObjectFetcher fetcher, List<ExampleRun> exampleRuns) -> {
            // Custom summary logic here
            return new HashMap<String, Object>() {{
                put("average_score", 0.82);
                put("consistency", 0.75);
            }};
        };
    },
    EvaluatorBuilderOptions.param("metric", "Quality metric to use", "helpfulness"));
```

```clojure
(aor/declare-summary-evaluator-builder
  topology
  "overall-quality"
  "Calculates overall quality metrics"
  (fn [params]
    (fn [fetcher example-runs]
      ;; Custom summary logic here
      {"average_score" 0.82 "consistency" 0.75}))
  {:params {"metric" {:description "Quality metric to use" :default "helpfulness"}}})
```


## Experiments

Experiments evaluate performance by running agents or individual nodes against datasets and applying evaluators to measure results. Experiments are part of the dataset UI, and starting one brings up this form (zoomed out so all fields are visible):


![Experiment UI](images/experiment-form.png)


You can select to run the experiment against a particular snapshot, against all examples in the dataset, against a particular tag, or against the specific examples that were selected before clicking on "Run New Experiment".

Next, you set up the targets of the experiment. For a regular experiment, there's only one target, and for a comparative experiment you can have any number of targets. Each target is either an agent or a particular node within an agent. For each target you can optionally provide [metadata](Programming-agents#metadata) for its execution, and then you must provide templates for the input arguments. Input argument templates specify how to translate an example's input, which is one object, into arguments to the agent/node, which is a list of parameters.

### JSON Path Templates

Input arguments are specified with "JSON Path templates". JSON path templates are not a standard like JSON paths, but are an Agent-o-rama specific way to provide a flexible way to specify input arguments. A JSON path template simply specifies each argument as normal JSON, but any nested JSON paths are substituted with the result of running that path on the example input. For example, suppose the example input is:

```
{"name": "Jack Benny",
 "age": 39}
```

Now suppose the agent takes in three arguments. Suppose you set the input arguments like so:

```
Input arg 1: $.name
Input arg 2: {"model": "openai"}
Input arg 3: {"user-age": "$.age", "foo": "$$"}
```

The agent will run with the arguments `("Jack Benny", {"model": "openai"}, {"user-age": 39, "foo": "$"})`. Note that it's not running with JSON strings, but with those objects. The last arg also demonstrates how escape a literal "$" without it being interpreted as a JSON path with "$$".

Next, you select any number of evaluators to use. Regular experiments can only use **regular** or **summary** evaluators, while comparative experiments can only use **comparative** evaluators.

Finally, you can specify the amount of concurrency to use for the experiment and the number of repetitions for each examples. Limiting concurrency can help avoid hitting model API rate limits, but the higher it is the faster the experiment will run. The number of repetitions setting controls how many times each example is run, which can reduce noise and produce a more accurate view of performance.


### Experiment Results

Experiment results look like this:

![Experiment Results](images/experiment-results.png)

For each example run, you can see the example input, the example reference output, and the output from the agent or node target. Along with each output is the result of each evaluator, how long that run took, and the number of tokens used.

You can view a complete trace for each example run by clicking on "View Trace" in the input box. This lets you see detailed info on how exactly that agent or node produced its result. Likewise, you can see a trace for each evaluator run by clicking on the evaluator result. This can be really helpful to understand if your evaluators are working correctly, especially for evaluators that use LLMs.

At the top of the page are aggregate results like average/P99 latencies of runs and the average tokens used. If there were any summary evaluators configured for the experiment, those will also display at the top.

This view will also display any failed runs or any evaluators that had an error during their execution.

The experiments listing page shows additional info:

![Experiment Listing](images/experiment-listing.png)

Along with the aggregated latency/token stats, each row shows an aggregation of each eval. So if you had an eval that scores "helpfulness" from 0 to 10, you'll see the average of all those scores in that row. If an eval scored as a boolean value, like a score named "concise?", you'll see the fraction of values that were true for that eval.

Finally, the top of the listing automatically displays charts showing the trend across all he displayed experiments. You can control which experiments are displayed by using the search box, and the charts will automatically update.


### Comparative experiments

Comparative experiments evaluate two or more targets side-by-side on the same dataset examples, enabling you to compare different agents or nodes against each other. They're useful when it's easier to determine which output is better than to independently score each output against a fixed standard. You can view and create comparative experiments on the datasets page on the UI.

Rather than having to establish what constitutes a "good" score in isolation, a comparative experiment can tell you directly whether the new version produces better outputs than the current one. This is valuable for subjective tasks like creative writing, where defining objective quality metrics is difficult, but judges can reliably say which version is better.

The main differences between regular and comparative experiments are in how targets and evaluators are configured. For comparative experiments, you can specify any number of targets instead of just one. Each target can be an entire agent or a specific node within an agent, and you can mix and match as needed. You might compare three different agents, or the same agent with three different sets of metadata or parameter configurations. You could also compare a full agent against a specific node within another agent to understand how individual components perform.

When configuring a comparative experiment, each target requires the same input argument templates as regular experiments, mapping the dataset example input to the target's function arguments. The experiment runs each example against all targets and collects all outputs before evaluation.

Comparative experiments can only use comparative evaluators, not regular or summary evaluators. This restriction is because comparative evaluators are specifically designed to rank multiple outputs relative to each other. Regular evaluators score individual outputs independently, while summary evaluators analyze metrics across all examples, neither of which aligns with how comparative experiments work.

Comparative evaluators return the same map of scores as other evaluator types, but with one important distinction: the "index" score receives special treatment in the UI. The index score indicates which output is the best, with 0 meaning the first output, 1 meaning the second output, and so on. In the comparative experiment results view, the winning output is highlighted with a green background, making it immediately clear which target performed best for each example. This visual highlighting makes it easy to scan through results and see patterns in which configurations or agents are winning across different types of inputs.

If multiple evaluators are configured, each can return its own index score, and the UI allows you to select which evaluator's index to use for highlighting. This is useful when different evaluators might have different criteria for what makes an output "best"—perhaps one evaluator prioritizes accuracy while another prioritizes conciseness.


# Agent-o-rama vs [Embabel](https://github.com/embabel/embabel-agent)

## Similarities

| Feature | Summary |
|--------|---------|
| JVM-based | Both run on the JVM. Embabel has Java and Kotlin templates; Agent-o-rama has first-class Java and Clojure APIs. |
| Agent workflows | Both support multi-step agent workflows, though with completely different approaches. |
| Tool integration | Both allow exposing regular functions as tools for LLMs. |
| RAG |  Both integrate with database and vector stores to support RAG-style retrieval workflows. |


---

## Differences

| Area | Agent-o-rama | Embabel |
|------|--------------|---------|
| Scope | Complete platform: runtime, storage, datasets, experiments, telemetry, UI. | Agent framework on top of Spring AI; focuses on agent logic Spring integration. |
| Control model | Agents are explicit graphs of functions with named nodes/edges and parallel execution. | Agents are non-deterministic and modeled with actions, goals, conditions. A planner decides which actions to run and in what order. |
| Execution model | Distributed, parallel graph execution with built-in scaling on a Rama cluster. | Runs inside your process (typically a Spring Boot app); any clustering or horizontal scaling is up to you. |
| Human-in-the-loop | Built-in pause/resume API for requesting human input during execution. | No equivalent feature, must be built manually. |
| Storage | Built-in, high-performance, scalable, replicated storage (any data model) or external databases. | No general-purpose storage engine; applications rely on Spring data sources or separate databases that you operate. |
| Datasets | Built-in versioned datasets for capturing inputs/outputs for use in experiments. | No equivalent feature. |
| Experiments | Built-in experiment runner for evaluating whole agents or individual nodes with LLM or function evaluators. | No experiment runner. |
| Actions / online evaluation | Easy to set up custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. | No equivalent feature. |
| Telemetry | Built-in time-series telemetry for agent performance, latency, token usage, model costs, and online evaluation. | Provides telemetry integrations (e.g. OpenTelemetry) but no built-in time-series storage or dashboards. No online evaluation. |
| UI | Includes UI for traces, datasets, experiments, telemetry. | No general-purpose evaluation/observability UI. |
| Deployment | Runs on a Rama cluster (in-process, single-node, or distributed). Rama is the only dependency. Deploying/updating/scaling agents are one-line CLI commands. | Runs inside Spring-based applications or other JVM apps; deployment, scaling, and orchestration are your responsibility. |

---

# Missing pieces you must build yourself if using Embabel alone

Embabel gives you a planning-based agent framework on the JVM, but you must supply the platform around it.

### Runtime and execution
- Distributed or parallel agent execution across threads/machines
- Backpressure, retries, timeouts, and fault-tolerance across agent steps 
- Pause/resume mechanics for human-in-the-loop 

### Storage
- One or more stores for:
  - agent state  
  - datasets  
  - experiments  
  - traces  
  - telemetry  

### Deployment and operations
- Infrastructure for:
  - scaling  
  - clustering  
  - job scheduling  
  - distributed state  
  - durability  
- Rolling updates of new agent versions
- Developer tooling for local runs, testing, and debugging

### Datasets and experiments
- Versioned datasets with reproducibility guarantees  
- Experiment runner to measure agent/node performance and quality
- LLM or code-based evaluators  


### Telemetry and tracing
- Time-series metrics (latency, tokens, errors, costs) can be emitted, but you must choose and operate the backend yourself  
- Dashboards, alerting, and visualizations must be built using external tools (Prometheus, Grafana, Langfuse, OpenTelemetry backends, etc.)  
- Long-term storage and querying of traces and metrics must also be set up and maintained by your team  





Agent-o-rama is a library for building scalable and stateful LLM agents in Java or Clojure. Agents are defined as simple graphs of pure Java or Clojure functions, and Agent-o-rama automatically captures detailed traces and provides facilities and a web UI for offline experimentation, online evaluation, time-series telemetry (e.g. latencies, token usage), and much more.

## Documentation

- **[Quickstart](Quickstart)** - Get up and running in minutes with an agent with an agent running locally or on a Rama cluster
- **[Programming Agents Guide](Programming-agents)** - Comprehensive guide to programming and deploying agents
- **[Client API](Agent-client-API)** - How to invoke and interact with agents from your applications
- **[Interfacing with LLMs](Interfacing-with-LLMs)** - How to use the capabilities LLMs within agents
- **[Tools](Tools)** - Creating tools agents for AI model function calling
- **[Streaming](Streaming)** - How to create and consume streams from agents
- **[Human-in-the-loop](Human‐in‐the‐loop)** - Human-in-the-loop patterns for agent interactions
- **[Datasets, evaluators, and experiments](Datasets,-evaluators,-and-experiments)** - Evaluate performance of agents and nodes
- **[Human feedback](Human-feedback)** – Complement automated feedback with structured human feedback
- **[Actions, online evaluation, and telemetry](Actions,-rules,-and-telemetry)** - Automate actions on production runs and view comprehensive analytics
- **[Integrating with regular Rama modules](Integrating-with-regular-Rama-modules)** – How to add agents to regular Rama modules and use depots, PStates, and query topologies from agent nodes

## API docs

- **[Javadoc](https://redplanetlabs.com/aor/javadoc/index.html)**
- **[Clojuredoc](https://redplanetlabs.com/aor/clojuredoc/index.html)**

## Resources

- **Mailing List**: [Rama mailing list](https://groups.google.com/u/1/g/rama-user)
- **Community Chat**: [Discord server](https://discord.gg/xSRjMTvDSj)
- **Chatbot**: [https://chat.redplanetlabs.com/](https://chat.redplanetlabs.com/)
- **Examples**: Explore the [examples/](https://github.com/redplanetlabs/agent-o-rama/tree/master/examples) directory for lots of example agents
- **Rama Documentation**: [redplanetlabs.com/docs](https://redplanetlabs.com/docs)


# Human Feedback

Human feedback is essential for evaluating and improving LLM agents. While automated evaluators can measure many aspects of agent performance, some assessments require human judgment—especially for subjective qualities like helpfulness, tone, or appropriateness. Agent-o-rama provides a comprehensive system for collecting, managing, and analyzing human feedback on agent executions.

This page explains Agent-o-rama's human feedback features: **Human Metrics** define the criteria by which humans evaluate agent performance. **Human Feedback Queues** organize collections of agent runs for systematic human review. You can provide structured feedback on individual agent executions and manage that feedback through the UI. The system also tracks human feedback metrics over time in telemetry.

## Table of Contents

- [Human Metrics](#human-metrics)
- [Human Feedback Queues](#human-feedback-queues)
- [Providing Feedback](#providing-feedback)
- [Managing Feedback](#managing-feedback)
- [Adding to Feedback Queues from Traces](#adding-to-feedback-queues-from-traces)
- [Human Feedback Telemetry](#human-feedback-telemetry)

## Human Metrics

Human metrics define the criteria by which humans evaluate agent performance. Each metric specifies what aspect of the agent's output is being assessed and how that assessment should be structured.

There are two types of human metrics: **Categorical Metrics** evaluate outputs using predefined categories (e.g., "helpful" vs "not helpful", "factual" vs "opinion" vs "nonsense"). **Numeric Metrics** evaluate outputs using numeric scores within a specified range (e.g., 1-10 for quality).

### Creating Human Metrics

Human metrics are created through the "Human metrics" page in the module side panel. You can add new metrics or delete existing ones. They can also be created via the API.

When adding a metric, you first select whether it's categorical or numeric:

#### Categorical Metrics

For categorical metrics, you specify one or more category names (non-empty strings). At least one category must be provided. These categories become the options available when providing feedback.

![Add Human Metric - Categorical](images/add-human-metric2.png)

**Java API:**

```java
AgentManager manager = AgentManager.create(cluster, moduleName);

manager.createCategoricalHumanMetric(
    "helpfulness",
    "description of metric",
    Set.of("very helpful", "somewhat helpful", "not helpful")
);
```

**Clojure API:**

```clojure
(def manager (aor/agent-manager cluster module-name))

(aor/create-categorical-human-metric! manager
  "helpfulness"
  "description of metric"
  #{"very helpful" "somewhat helpful" "not helpful"})
```

#### Numeric Metrics

For numeric metrics, you specify a minimum and maximum value. Both must be integers, and the maximum must be greater than the minimum. These bounds define the valid range for feedback scores.

![Add Human Metric - Type Selection](images/add-human-metric1.png)


**Java API:**

```java
manager.createNumericHumanMetric(
    "quality-score",
    "description of metric",
    1,  // min
    10  // max
);
```

**Clojure API:**

```clojure
(def metric-id
  (aor/create-numeric-human-metric! manager
    "quality-score"
    "description of metric"
    1   ; min
    10)) ; max
```

### Managing Human Metrics

The Human metrics page displays all defined metrics in a paginated list. You can search for metrics by name using the search box at the top.

To delete a metric, click the delete button next to it. Note that deleting a metric will remove it from any human feedback queues that reference it, but existing feedback using that metric will remain.

**Java API:**

```java
manager.removeHumanMetric(metricId);
```

**Clojure API:**

```clojure
(aor/remove-human-metric! manager metric-id)
```

## Human Feedback Queues

Human feedback queues organize collections of agent runs for systematic human review. Each queue defines which metrics should be evaluated and which are required versus optional. Queues enable structured workflows for reviewing agent performance, whether for quality assurance, training data collection, or continuous improvement. Agent runs in queues can either be root agent runs or individual node runs.

### Creating Human Feedback Queues

Human feedback queues are created through the "Human feedback queues" page in the module side panel.

![Create Human Feedback Queue](images/add-human-queue.png)

When creating a queue, you specify a **name** (a descriptive name for the queue), an optional **description** (text describing the purpose or criteria for this queue), and **rubrics** (a list of human metrics to evaluate). Each rubric includes the **metric** to use (selected from a dropdown with search) and a **required** checkbox indicating whether this metric must be provided when reviewing items.

### Managing Human Feedback Queues

The Human feedback queues page displays all queues in a paginated list. You can search for queues by name using the search box at the top.

![Human Feedback Queues List](images/human-queue-list.png)

To delete a queue, click the delete button next to it. This will also remove all items in the queue.

### Viewing a Human Feedback Queue

Clicking on a human feedback queue opens its detail page, which shows **queue information** (name, description, and the list of rubrics configured for this queue – rubrics that reference deleted metrics are automatically filtered out) and a **paginated list of queue items** (agent or node runs waiting for review).

![Human Feedback Queue Items](images/human-queue-items-list.png)


#### Editing a Queue

You can edit a queue's description and rubrics by clicking the "Edit" button. This brings up a form similar to the creation form, allowing you to modify the description and add, remove, or change the required status of rubrics.


## Providing Feedback

### Reviewing Queue Items

To review an item in a human feedback queue, click on it from the queue items list. This opens the evaluation form.

![Human Feedback Queue Item Form](images/human-queue-item-form.png)

The evaluation form displays the **input** that was provided to the agent or node, the **output** produced by the agent or node, and a **feedback form** to provide structured feedback. There's also a link to the full trace for the run. For each metric in the queue's rubrics, the form provides a component to collect feedback. The form also includes a required **reviewer name** field and an optional **comment** text area for additional notes.

The form includes navigation controls: **Previous/Next buttons** to navigate to adjacent items in the queue, and a **Dismiss button** to remove the item from the queue without providing feedback.

When you submit feedback, the system records the feedback on the trace and  automatically opens the evaluation form for the next item in the queue.

This workflow enables efficient batch review of queue items, automatically advancing through the queue as you provide feedback.


## Managing Feedback

### Adding Feedback Manually

You can add human feedback directly to any agent execution or node run through the feedback panel in the trace view. The feedback panel for agent roots and nodes includes an "Add feedback" button.

![Trace Human Feedback](images/trace-human-feedback.png)

Clicking "Add feedback" opens a form that allows you to dynamically select any number of human metrics using a dropdown selector with search. As you select each metric, the appropriate form component appears (dropdown for categorical, text input for numeric).


### Editing Feedback

All human feedback can be edited. In the feedback panel, click the edit button next to any human feedback entry. This opens the same form used for adding feedback, pre-populated with the existing values.

You can modify any aspect of the feedback: change metric values, update the comment, or even change which metrics are included.

### Deleting Feedback

You can delete human feedback by clicking the delete button in the feedback panel. This permanently removes the feedback from the system.

## Adding to Feedback Queues from Traces

Just like the "Add to dataset" buttons for agents and nodes, there are also "Add to human feedback queue" buttons in the trace view.

Clicking this button brings up a dropdown selector with search for choosing which human feedback queue to add the run to. This makes it easy to collect interesting examples from production runs for later review.


### Automatic Queue Addition via Actions

You can also automatically add runs to human feedback queues using [actions](Actions,-rules,-and-telemetry).

![Human Feedback Queue Rule](images/human-queue-rule.png)

This enables continuous collection of runs for human review, ensuring important examples don't get missed.

## Human Feedback Telemetry

The telemetry section for agents is expanded to include charts for each human metric defined in the system. These charts show time-series data of human feedback scores, enabling you to track how human-assessed quality metrics change over time.

Human feedback telemetry complements automated evaluator telemetry, giving you visibility into both objective and subjective aspects of agent performance. Together, they provide a complete picture of how your agents are performing in production.


# Human-in-the-loop

Human-in-the-loop is a critical pattern for AI agents that need human judgment, approval, or clarification during execution. Agent-o-rama provides built-in support for agents to pause execution and request input from humans, then resume once the input is provided.

## Table of Contents

1. [Requesting Human Input in Agents](#requesting-human-input-in-agents)
2. [Handling Human Input from Clients](#handling-human-input-from-clients)
3. [Viewing and Providing Input in the UI](#viewing-and-providing-input-in-the-ui)

## Requesting Human Input in Agents

Inside an agent node, you can request human input at any point using `getHumanInput` (Java) or `get-human-input` (Clojure). This method pauses the agent execution until a human provides a response. Since agent nodes run on [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), this doesn't actually block any actual threads.

The method takes a prompt string and returns the human's response as a string. Your agent code can then use that response to make decisions or continue processing.

### Java API

```java
import com.rpl.agentorama.*;

public class HumanInputAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("ApprovalAgent")
            .node("process-request", null, (AgentNode agentNode, String itemName, Double cost) -> {
              // Request human approval
              String response = agentNode.getHumanInput(
                String.format("Approve purchase of %s for $%.2f? (yes/no): ", itemName, cost)
              );

              if ("yes".equalsIgnoreCase(response)) {
                agentNode.result(Map.of("status", "approved", "item", itemName));
              } else {
                agentNode.result(Map.of("status", "rejected", "item", itemName));
              }
            });
  }
}
```

### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])

(aor/defagentmodule HumanInputAgentModule
  [topology]
  (-> (aor/new-agent topology "ApprovalAgent")
      (aor/node
       "process-request"
       nil
       (fn [agent-node item cost]
         ;; Request human approval
         (let [response (aor/get-human-input
                         agent-node
                         (format "Approve purchase of %s for $%.2f? (yes/no): " item cost))]
           (if (= "yes" (clojure.string/lower-case response))
             (aor/result! agent-node {:status "approved" :item item})
             (aor/result! agent-node {:status "rejected" :item item})))))))
```

### Multiple Human Input Requests

An agent can request human input multiple times during execution. Each call to `getHumanInput` / `get-human-input` will pause execution until that specific request is answered. Additionally, if multiple nodes are running in parallel there can be multiple pending human input requests.

```java
// Java - Multiple requests
String name = agentNode.getHumanInput("What is your name? ");
String email = agentNode.getHumanInput("What is your email? ");
String confirmation = agentNode.getHumanInput(
  String.format("Confirm: Name=%s, Email=%s. Is this correct? (yes/no): ", name, email)
);
```

```clojure
;; Clojure - Multiple requests
(let [name (aor/get-human-input agent-node "What is your name? ")
      email (aor/get-human-input agent-node "What is your email? ")
      confirmation (aor/get-human-input
                    agent-node
                    (format "Confirm: Name=%s, Email=%s. Is this correct? (yes/no): " name email))]
  ...)
```

## Handling Human Input from Clients

When an agent requests human input, the execution pauses. From the client side, you have two main approaches for handling these requests:

1. **Step-by-step with `nextStep`**: Get the next execution step, check if it's a human input request, provide input, and continue
2. **Batch processing with `pendingHumanInputs`**: Get all pending requests at once and provide responses

### Using nextStep

The `nextStep` method returns the next step in the agent execution, which is either:
- A **human input request** (check with `instanceof HumanInputRequest` in Java or `human-input-request?` in Clojure)
- The **final result** (an `AgentComplete` object in Java, or a map with `:result` key in Clojure)

This approach is ideal for interactive command-line tools or UIs where you handle requests one at a time. If there are multiple human input requests pending, `nextStep` will return whichever one was requested first.

#### Java API

```java
import com.rpl.agentorama.*;
import java.util.Scanner;

public class InteractiveClient {
  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create();
         Scanner scanner = new Scanner(System.in)) {

      // Launch module and get agent client
      HumanInputAgentModule module = new HumanInputAgentModule();
      ipc.launchModule(module, new LaunchConfig(4, 2));

      AgentManager manager = AgentManager.create(ipc, module.getModuleName());
      AgentClient agent = manager.getAgentClient("ApprovalAgent");

      // Start agent execution
      AgentInvoke invoke = agent.initiate(Map.of("item", "laptop", "cost", 1200.0));

      // Handle execution step by step
      AgentStep step = agent.nextStep(invoke);
      while (step instanceof HumanInputRequest) {
        HumanInputRequest request = (HumanInputRequest) step;

        // Display the prompt
        System.out.println(request.getPrompt());
        System.out.print(">> ");
        String response = scanner.nextLine();

        // Provide the response
        agent.provideHumanInput(request, response);

        // Get next step
        step = agent.nextStep(invoke);
      }

      System.out.println("Final result: " + ((AgentComplete) step).getResult());
    }
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor]
         '[com.rpl.rama.test :as rtest])

(with-open [ipc (rtest/create-ipc)]
  ;; Launch module and get agent client
  (rtest/launch-module! ipc HumanInputAgentModule {:tasks 4 :threads 2})
  (let [manager (aor/agent-manager ipc (rama/get-module-name HumanInputAgentModule))
        agent (aor/agent-client manager "ApprovalAgent")]

    ;; Start agent execution
    (let [invoke (aor/agent-initiate agent {:item "laptop" :cost 1200.0})]

      ;; Handle execution step by step
      (loop [step (aor/agent-next-step agent invoke)]
        (if (aor/human-input-request? step)
          (do
            ;; Display the prompt
            (println (:prompt step))
            (print ">> ")
            (flush)
            (let [response (read-line)]
              ;; Provide the response
              (aor/provide-human-input agent step response)
              ;; Get next step
              (recur (aor/agent-next-step agent invoke))))
          ;; step is now the final result
          (let [result (:result step)]
            (println "Final result:" result)))))))
```

### Using pendingHumanInputs

The `pendingHumanInputs` method returns all human input requests that are currently waiting for responses. This is useful when:
- You want to see all pending requests at once
- You're building a UI that displays multiple pending requests
- You want to batch process multiple requests

Each request object has:
- **Java**: `.getPrompt()` for the prompt text, `.getNode()` for the node name
- **Clojure**: `:prompt` for the prompt text, `:node` for the node name

#### Java API

```java
import com.rpl.agentorama.*;
import java.util.List;

public class BatchClient {
  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("ApprovalAgent");

      AgentInvoke invoke = agent.initiate(Map.of("item", "laptop", "cost", 1200.0));

      // Wait a bit for the agent to reach the human input request
      Thread.sleep(100);

      // Get all pending requests
      List<HumanInputRequest> pending = agent.pendingHumanInputs(invoke);
      System.out.println("Pending requests: " + pending.size());

      for (HumanInputRequest request : pending) {
        System.out.println("Node: " + request.getNode());
        System.out.println("Prompt: " + request.getPrompt());

        // Provide response (in real app, get from user)
        agent.provideHumanInput(request, "yes");
      }

      // Check if execution is complete
      if (agent.isAgentInvokeComplete(invoke)) {
        Object result = agent.result(invoke);
        System.out.println("Result: " + result);
      }
    }
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])

(let [agent (aor/agent-client manager "ApprovalAgent")
      invoke (aor/agent-initiate agent {:item "laptop" :cost 1200.0})]

  ;; Wait a bit for the agent to reach the human input request
  (Thread/sleep 100)

  ;; Get all pending requests
  (let [pending (aor/pending-human-inputs agent invoke)]
    (println "Pending requests:" (count pending))

    (doseq [request pending]
      (println "Node:" (:node request))
      (println "Prompt:" (:prompt request))

      ;; Provide response (in real app, get from user)
      (aor/provide-human-input agent request "yes")))

  ;; Check if execution is complete
  (when (aor/agent-invoke-complete? agent invoke)
    (let [result (aor/agent-result agent invoke)]
      (println "Result:" result))))
```

## Viewing and Providing Input in the UI

The Agent-o-rama web UI provides a visual interface for viewing and responding to human input requests. In an agent invoke trace, nodes with pending human input have a human icon on them, and clicking on the node will show the request below with an input box to provide a response.

![Human-in-the-loop](images/human-input.png)


# Integrating with regular Rama modules

Agent-o-rama can be integrated with regular Rama modules, allowing you to use agents alongside full Rama features like depots, stream processing, and query topologies. This page covers two main integration patterns:

1. **Accessing Rama objects from within agent nodes** - Using depots, PStates, and query topologies from agent node functions
2. **Adding agents to regular Rama modules** - Defining agents within a standard `RamaModule` implementation

## Accessing Rama objects from agent nodes

Within agent node functions, you can access Rama depots, PStates (via stores), and query topologies using methods on the `AgentNode` interface. This allows agents to interact with the broader Rama infrastructure.

### Accessing depots

Depots are Rama's append-only logs. You can access both local depots (from the same module) and mirror depots (from other modules).

**Local depot:**
```java
.node("process", null, (AgentNode agentNode, String input) -> {
    Depot depot = agentNode.getDepot("*events-depot");
    depot.append(input);
    // ...
})
```

**Mirror depot (from another module):**
```java
.node("process", null, (AgentNode agentNode, String input) -> {
    Depot mirrorDepot = agentNode.getMirrorDepot("com.mycompany.OtherModule", "*analytics-depot");
    mirrorDepot.append(input);
    // ...
})
```

### Accessing PStates via stores

PStates are accessed through the store interface. Agent-o-rama provides three types of stores that wrap PStates:

1. **KeyValueStore** - Simple key-value storage
2. **DocumentStore** - Schema-flexible nested data storage  
3. **PStateStore** - Direct PState access

**Local store:**
```java
// Declare the store in the topology
topology.declareKeyValueStore("$$users", String.class, UserData.class);

// Use it in a node
topology.newAgent("myAgent")
        .node("process", null, (AgentNode agentNode, String userId) -> {
            KeyValueStore<String, UserData> store = agentNode.getStore("$$users");

            // Read from store
            UserData user = store.get(userId);
            if (user == null) {
                user = fetchUserData(userId);
                store.put(userId, user);
            }

            agentNode.result(user);
        });
```

**Mirror store (read-only from another module):**
```java
.node("process", null, (AgentNode agentNode, String userId) -> {
    // Read-only access to a store in another module
    KeyValueStore<String, UserData> mirrorStore =
        agentNode.getMirrorStore("com.mycompany.UserModule", "$$user-db");

    UserData user = mirrorStore.get(userId);
    // ...
})
```

**PStateStore for direct PState access:**

PState stores can be fetched for PStates declared as part of the agent topology or for PStates declared outside the agent topology. PStates are read-only if declared in the module outside the agent topology.

```java
topology.newAgent("myAgent")
        .node("process", null, (AgentNode agentNode, String id) -> {
            PStateStore store = agentNode.getStore("$$myPState");
            // Direct PState operations available through the store interface
            Long value = store.selectOne(Path.key(id));
            // ...
        });
```

### Accessing query topologies

Query topologies allow you to invoke queries from within agent nodes.

**Local query topology:**
```java
.node("process", null, (AgentNode agentNode, String query) -> {
    QueryTopologyClient<Map> queryClient = agentNode.getQueryTopologyClient("search-query");
    Map results = queryClient.invoke(query, 100);
    // ...
})
```

**Mirror query topology (from another module):**
```java
.node("process", null, (AgentNode agentNode, String userId) -> {
    QueryTopologyClient<UserInfo> queryClient =
        agentNode.getMirrorQueryTopologyClient("com.mycompany.UserModule", "user-lookup");
    UserInfo user = queryClient.invoke(userId);
    // ...
})
```

## Adding agents to regular Rama modules

You can add agents to any regular Rama module by creating an `AgentTopology` manually and calling `define()` when done. This allows you to use agents alongside full Rama features like stream processing, custom depots, and other topologies.

### Basic pattern

Instead of extending `AgentModule`, implement `RamaModule` directly and create the agent topology manually:

```java
public class MyRamaModule implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
        // Declare regular Rama depots, topologies, PStates, etc.
        setup.declareDepot("*events-depot", Depot.random());

        // Create agent topology manually
        AgentTopology agentTopology = AgentTopology.create(setup, topologies);

        // Define agents
        agentTopology.declareKeyValueStore("$$cache", String.class, String.class);
        agentTopology.newAgent("myAgent")
            .node("process", null, (AgentNode agentNode, String input) -> {
                KeyValueStore<String, String> store = agentNode.getStore("$$cache");
                store.put("last-input", input);
                agentNode.result("Processed: " + input);
            });

        // Must call define() to finalize agent definitions
        agentTopology.define();
    }
}
```


# Interfacing with LLMs

Agent-o-rama integrates with [LangChain4j](https://docs.langchain4j.dev/) for AI model interactions, automatically capturing traces and streaming model calls without requiring any special configuration.  It's important to understand that Agent-o-rama is fundamentally an orchestration tool where you write arbitrary code to integrate with any tools or services you need. So using LangChain4j is completely optional. You can use any LLM library or API client you prefer. If you choose not to use LangChain4j, you'll need to create your own wrappers to record nested operations for tracing and telemetry, but Agent-o-rama doesn't require any particular AI library or model provider.

## Automatic Tracing

When you declare LangChain4j chat models as [agent objects](Programming-agents#agent-objects), Agent-o-rama automatically wraps them to capture detailed execution information. Every model call is recorded in the agent trace with timing information, token counts, request and response data, and metadata about the model used. This information appears in the UI trace view and contributes to time-series telemetry, giving you complete visibility into how your agents interact with AI models.

## Example usage

Here's a basic example of an agent interfacing with an LLM by just doing a single prompt. It declares the model as an agent object, retrieves it in the node, and then invokes it using LangChain4j's API:

#### Java API

```java
public class AgentObjectsModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));
    topology.declareAgentObjectBuilder("openai-model", setup -> {
      String apiKey = setup.getAgentObject("openai-api-key");
      return OpenAiStreamingChatModel.builder()
                                     .apiKey(apiKey)
                                     .modelName("gpt-4o-mini")
                                     .build();
      });
    topology.newAgent("AgentWithObjects")
            .node("process", null, (AgentNode agentNode, String input) -> {
              ChatModel model = agentNode.getAgentObject("openai-model");
              String response = model.chat(input);
              agentNode.result(response);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule AgentObjectsModule
  [topology]
  (aor/declare-agent-object topology "openai-api-key" (System/getenv "OPENAI_API_KEY"))
  (aor/declare-agent-object-builder
   topology
   "openai-model"
   (fn [setup]
     (-> (OpenAiStreamingChatModel/builder)
         (.apiKey (aor/get-agent-object setup "openai-api-key"))
         (.modelName "gpt-4o-mini")
         .build)))
  (-> (aor/new-agent topology "AgentWithObjects")
      (aor/node
       "process"
       nil
       (fn [agent-node input]
         (let [model (aor/get-agent-object agent-node "openai-model")]
           (aor/result! agent-node (lc4j/chat model input)))))))
```


## Streaming Model Wrapping

When you declare a `StreamingChatModel` as an agent object, Agent-o-rama automatically wraps it to stream tokens from the model to the node for the agent. The key detail is that when you fetch the model in an agent node, you always receive a `ChatModel` interface, not `StreamingChatModel`. This design allows you to use streaming models in a blocking style within your agent nodes while clients can subscribe to streams to receive tokens in real-time.

This wrapping happens automatically when you fetch the agent object in a node. You declare it as a `StreamingChatModel` in the topology, but retrieve it as a `ChatModel` when you use it. The framework handles converting between the streaming interface and the blocking interface, capturing all streaming tokens along the way. See the [Streaming documentation](Streaming) for more details.

If you don't want streaming behavior, declare the model as a non-streaming `ChatModel` instead. The framework will still automatically trace all calls, but won't capture or forward streaming tokens.

## Structured Outputs

LangChain4j supports structured outputs through JSON schema enforcement, allowing you to define the exact structure you want the model to return. This is useful for extracting structured data or integrating with APIs that require specific schemas.

To use structured outputs, you create a JSON schema defining your desired structure and configure the model request with a `ResponseFormat` object. The model will then return responses matching that schema, making it easy to parse and use the structured data in your agent logic.

Currently, structured outputs with JSON schema enforcement only work with non-streaming chat models due to limitations in LangChain4j. If you need structured outputs, use a regular `ChatModel` rather than a `StreamingChatModel`. The framework still provides full tracing and telemetry for these structured calls, just without the streaming behavior.

Here's an example of using LangChain4j with structured outputs. This code can be used directly within agent nodes:

### Java API

```java
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.data.message.UserMessage;

JsonSchema analysisSchema = JsonSchema.builder()
  .name("QuestionAnalysis")
  .rootElement(JsonObjectSchema.builder()
    .addStringProperty("questionType", "Type of question")
    .addStringProperty("complexity", "Complexity level")
    .addProperty("mainTopics", JsonArraySchema.builder()
      .items(JsonStringSchema.builder().build())
      .build())
    .build())
  .build();

ResponseFormat responseFormat = ResponseFormat.builder()
  .type(ResponseFormatType.JSON)
  .jsonSchema(analysisSchema)
  .build();

ChatRequest request = ChatRequest.builder()
  .messages(List.of(new UserMessage("Analyze this question")))
  .responseFormat(responseFormat)
  .build();

ChatResponse response = model.chat(request);
```

### Clojure API

```clojure
(require '[com.rpl.agent-o-rama.langchain4j :as lc4j]
         '[com.rpl.agent-o-rama.langchain4j.json :as lj])

(def QuestionAnalysis
  (lj/object
   {:description "Analysis of a user question"}
   {"question_type" (lj/enum "Type of question"
                            ["factual" "analytical" "creative"])
    "complexity" (lj/enum "Complexity level"
                         ["simple" "moderate" "complex"])
    "main_topics" (lj/array "Key topics"
                           (lj/string "A main topic"))}))

(let [model (aor/get-agent-object agent-node "openai-model")
      response (lc4j/chat model
                          (lc4j/chat-request
                           [(UserMessage. "Analyze this question")]
                           {:response-format
                            (lc4j/json-response-format
                             "QuestionAnalysis"
                             QuestionAnalysis)}))]
  ;; ...
  )
```

The `com.rpl.agent-o-rama.langchain4j.json` namespace provides helper functions for building JSON schemas with types like `lj/object`, `lj/string`, `lj/number`, `lj/array`, and `lj/enum`, making it straightforward to define complex schemas in Clojure.

## Using Other Tools

Agent-o-rama doesn't require LangChain4j – you can integrate with any LLM provider or API client you prefer. If you're not using LangChain4j, you'll need to manually record nested operations for tracing and telemetry using the `recordNestedOp` method on `AgentNode`.

When you manually record nested operations, you specify the operation type, timing information, and any metadata you want to include. This information appears in traces and contributes to telemetry just like automatic LangChain4j tracing, giving you the same observability regardless of which tools you use.

### Java API

```java
topology.newAgent("MyAgent")
        .node("process", null, (AgentNode agentNode, String input) -> {
          long startTime = System.currentTimeMillis();

          String response = callCustomLLMService(input);

          long finishTime = System.currentTimeMillis();

          Map<String, Object> info = new HashMap<>();
          info.put("model", "custom-model");
          info.put("inputTokenCount", estimateTokens(input));
          info.put("outputTokenCount", estimateTokens(response));
          info.put("totalTokenCount", estimateTotalTokens(input, response));
          // add any other metadata you wish to track to the info map

          agentNode.recordNestedOp(
            NestedOpType.MODEL_CALL,
            startTime,
            finishTime,
            info
          );

          agentNode.result(response);
        });
```

### Clojure API

```clojure
(aor/node
 "process"
 nil
 (fn [agent-node input]
   (let [start-time (System/currentTimeMillis)
         ;; Call your LLM API or custom service
         response (call-custom-llm-service input)
         finish-time (System/currentTimeMillis)]

     ;; Record the operation for tracing and telemetry
     (aor/record-nested-op!
      agent-node
      :model-call
      start-time
      finish-time
      {"model" "custom-model"
       "inputTokenCount" (estimate-tokens input)
       "outputTokenCount" (estimate-tokens response)
       "totalTokenCount" (estimate-total-tokens input response)
       ;; add any other metadata you wish to track to the info map
       })

     (aor/result! agent-node response))))
```

Available operation types include `MODEL_CALL`, `TOOL_CALL`, `STORE_READ`, `STORE_WRITE`, `DB_READ`, `DB_WRITE`, `AGENT_CALL`, `HUMAN_INPUT`, and `OTHER`. Choose the type that best matches your operation, as this categorization helps organize telemetry and make traces more readable.

By manually recording nested operations, you maintain full control over how you integrate with external services while still getting the same rich tracing and telemetry that LangChain4j integration provides automatically.


# Agent-o-rama vs [Koog](https://github.com/JetBrains/koog)

## Similarities

| Feature | Summary |
|--------|---------|
| JVM-based | Both run on the JVM. Koog has a Kotlin API while Agent-o-rama has first-class Java and Clojure APIs.|
| Workflow model | Both support explicit agent workflows. |
| Tool integration | Both allow exposing regular methods/functions as tools. |
| Streaming | Both support streaming LLM outputs. |
| Tracing | Both provide support to trace or inspect model activity. |

---

## Differences

| Area | Agent-o-rama | Koog |
|------|--------------|------|
| Scope | Complete platform: runtime, storage, datasets, experiments, telemetry, UI. | Framework/library focused on agent logic; you bring your own infrastructure. |
| Execution model | Distributed, parallel graph execution with built-in scaling on a Rama cluster. | Runs inside your process; no distributed execution layer. |
| Human-in-the-loop | Built-in pause/resume API for requesting human input during execution. | No equivalent feature. |
| Storage | Built-in, high-performance, scalable, replicated storage (any data model) or external databases | No integrated storage engine; relies on external stores you manage. |
| Datasets | Built-in versioned datasets for capturing inputs/outputs for use in experiments | No dataset system. |
| Experiments | Built-in experiment runner for evaluating agents or individual nodes. | No experiment runner. |
| Actions / online evaluation | Easy to set up custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. | No equivalent feature |
| Telemetry | Built-in time-series telemetry for agent performance, latency, token usage, model costs, online evaluation | Provides telemetry integrations (e.g. OpenTelemetry) but no built-in time-series storage or dashboards. No online evaluation. |
| UI | Includes UI for traces, datasets, experiments, telemetry. | No built-in UI; depends on external observability tools. |
| Deployment | Runs on a Rama cluster (in-process, single-node, or distributed). Rama is the only dependency. Deploying/updating/scaling agents are one-line CLI commands. | Embedded in your application; scaling, orchestration, and clustering left to you. |
| Platform targets | Server-side JVM focus. | Kotlin Multiplatform (JVM, JS/Wasm, Android, iOS). |

---

# Missing pieces you must build yourself if using Koog alone

Koog gives you an agent framework and DSL, but you must provide the rest:

### Runtime and execution
- Distributed or parallel agent execution across threads/machines
- Backpressure, retries, timeouts, and fault-tolerance across agent steps 
- ause/resume mechanics for human-in-the-loop 

### Storage
- One or more stores for:
  - agent state  
  - datasets  
  - experiments  
  - traces  
  - telemetry  


### Deployment and operations
- Infrastructure for:
  - scaling  
  - clustering  
  - job scheduling  
  - distributed state  
  - durability  
- Rolling updates of new agent versions
- Developer tooling for local runs, testing, and debugging

### Datasets and experiments
- Versioned datasets with reproducibility guarantees  
- Experiment runner to measure agent/node performance and quality
- LLM or code-based evaluators  

### Telemetry and tracing
- Time-series metrics (latency, tokens, errors, costs) can be emitted, but you must choose and operate the backend yourself  
- Dashboards, alerting, and visualizations must be built using external tools (Prometheus, Grafana, Langfuse, OpenTelemetry backends, etc.)  
- Long-term storage and querying of traces and metrics must also be set up and maintained by your team  


# Agent-o-rama vs [LangChain4j](https://docs.langchain4j.dev/)

## Similarities

| Feature | Summary |
|--------|---------|
| JVM-first | Both target JVM developers with first-class Java APIs (Agent-o-rama also has a Clojure API). |
| Tool integration | Both allow exposing regular Java methods as LLM-callable tools. |
| Streaming | Both support streaming LLM responses back to the client. |
| RAG | Both can integrate with vector stores and databases for RAG workflows. |

---

## Differences

| Area | Agent-o-rama | LangChain4j |
|------|--------------|-------------|
| Scope | End-to-end agent platform: runtime, orchestration, storage, tracing, datasets, experiments, telemetry, UI. | Library only: provides LLMs, tools, embeddings, and basic agent patterns; all surrounding infra is user-built. |
| Execution model | Agents run as distributed, parallel graphs on a Rama cluster with built-in scaling. | Runs inside a single JVM process; no built-in distribution or parallel graph execution. |
| Storage | Built-in, high-performance, scalable, replicated storage (any data model) or external databases  | No storage; you must run external databases yourself. |
| Tracing | Full structured tracing for every agent and node run, with tokens, latencies, DB calls, and model calls. | No tracing system |
| Datasets | Built-in versioned datasets for capturing inputs/outputs for use in experiments | No dataset concept |
| Experiments | First-class experiment runner to evaluate agent quality and performance with LLM or function evaluators  | No experiment runner |
| Online evaluation / actions | Easy to set up custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. | No equivalent feature |
| Telemetry | Built-in time-series telemetry for agent performance, latency, token usage, model costs, online evaluation | No built-in telemetry |
| Agent model | Explicit graphs of Java/Clojure functions with parallel node execution | Agent control-flow is ad hoc in code via AiServices + program logic |
| Deployment | Runs on a Rama cluster (in-process, single-node, or distributed). Rama is the only dependency. Deploying/updating/scaling agents are one-line CLI commands. | Embeds in your application; all infra design (deployment, scaling, orchestration, monitoring) is your responsibility. |
| Integration | Integrates with LangChain4j for model access and adds a full platform on top. | Standalone; provides no equivalent observability, storage, evaluation, or orchestration layers. |

---

# Missing pieces you must build yourself if using LangChain4j alone

LangChain4j is intentionally a **library**, not a platform.  
Here is what JVM teams must engineer from scratch if using LangChain4j without Agent-o-rama:

### Runtime & Execution
- Distributed or parallel agent execution across threads/machines  
- Backpressure, retries, timeouts, and fault-tolerance across agent steps  
- Pause/resume mechanics for human-in-the-loop  

### Storage
- One or more stores for:
  - agent state  
  - datasets  
  - experiments  
  - traces  
  - telemetry  

### Deployment & Operations
- Infrastructure for:
  - scaling  
  - clustering  
  - job scheduling  
  - distributed state  
  - durability  
- Rolling updates of new agent versions
- Developer tooling for local runs, testing, and debugging


### Tracing
- A way to persist, query, and visualize structured traces:
  - every node/step  
  - model calls  
  - tool calls  
  - retries/failures
- Time-series metrics (latency, tokens, error rates, online evaluation, etc.)  
- Dashboards and alerts (Prometheus/Grafana/etc.)

### Datasets and experiments
- Versioned datasets with reproducibility guarantees  
- Experiment runner to measure agent/node performance and quality
- LLM or code-based evaluators 



# Agent-o-rama vs [LangGraph](https://www.langchain.com/langgraph) / [LangSmith](https://www.langchain.com/langsmith/observability)

## Similarities

| Feature | Summary |
|--------|----------|
| Agent definitions | Both define agents as explicit graphs of regular functions. |
| Streaming | LLM and other outputs for agent nodes can be streamed with a first-class client API. |
| Forking | Previous executions can be partially modified and executed to test logic variations. |
| Tracing | Both capture structured traces of each run with node-level details. |
| Datasets | Both enable inputs and outputs can be captured into versioned datasets. |
| Experiments | Both enable testing agents or individual nodes over datasets with arbitrary evaluators. |
| Online evaluation / actions | Both support custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. |
| Human feedback | Both have features for collecting structured and unstructured human feedback directly on runs or via human feedback queues |
| Human input | Both can pause agent execution to collect human input, then resume. |
| Telemetry | Both expose time-series metrics over agent performance, model usage, and online evaluation. |

---

## Differences

| Area | Agent-o-rama | LangGraph / LangSmith |
|------|--------------|------------------------|
| Runtime | JVM  with first-class Java and Clojure APIs | Python |
| Execution model | Distributed, parallel execution with no central coordinator | Executed inside one Python process with central state and limited ability to parallelize |
| Thread model | All agent code runs on [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), making long-running and blocking code easy and efficient | Regular threads |
| Storage | Built-in, high-performance, scalable, replicated storage (any data model) or external databases | External databases only |
| Human input | Explicit function call: node suspends → waits → resumes | Exception-based breakpoint pattern |
| Deployment | Self-hosted with Rama cluster, free to use up to two nodes and easy to run locally. Deploying/updating/scaling agents are one-line CLI commands. | LangSmith SaaS or self-hosted with Enterprise plan | 
| Feature parity | Missing few-shot examples | 



# Agent-o-rama vs [LangGraph4j](https://github.com/langgraph4j/langgraph4j)

## Similarities

| Feature | Summary |
|--------|---------|
| JVM-based | LangGraph4j has a Java API. Agent-o-rama has both Java and Clojure APIs. |
| Agent workflows | Both define agents as explicit graphs of functions. |
| Tool integration | Both allow exposing regular functions as tools callable by LLMs. |
| Control-flow patterns | Both support graph constructs such as branching, looping, and conditional logic. |
| Streaming | Both support streaming model responses. |

---

## Differences

| Area | Agent-o-rama | LangGraph4j |
|------|--------------|-------------|
| Scope | Complete platform: runtime, storage, datasets, experiments, telemetry, UI. | Library for building agent graphs; no platform components. |
| Execution model | Distributed, parallel graph execution with no central coordination. | Executed inside one process with central state and limited ability to parallelize. |
| Threading model | Nodes run on [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html) so code is easy and efficient to write in blocking style. | Uses traditional CompletableFuture-based async code, which is considerably more complex to write and harder to reason about.  |
| Storage | Built-in, scalable, replicated storage (any data model) or external databases. | External databases only. |
| Datasets | Built-in versioned datasets for capturing inputs/outputs for use in experiments. | No dataset concept. |
| Experiments | First-class experiment runner to evaluate agent quality and performance with LLM or function evaluators. | No experiment runner. |
| Actions / online evaluation | Easy to set up custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. | No equivalent feature. |
| Telemetry | Built-in time-series telemetry for performance, latency, tokens, costs, and online evaluation. | No equivalent feature. |
| Tracing | Full node-level tracing with a UI for graph execution, inputs/outputs, model/tool calls, database calls. | No equivalent feature. |
| UI | Unified UI for traces, datasets, experiments, and telemetry. | No UI. |
| Deployment | Runs on a Rama cluster (in-process, single-node, or distributed). Rama is the only dependency. Deploying/updating/scaling agents are one-line CLI commands. | Embedded in your application; deployment/scaling left entirely to the user. |

---

# Missing pieces you must build yourself if using LangGraph4j alone

LangGraph4j provides graph construction and execution patterns, but everything resembling a full agent platform must be built by you:

### Runtime and execution
- Distributed or parallel agent execution across threads/machines  
- Backpressure, retries, timeouts, and fault-tolerance across agent steps  

### Storage
- One or more stores for:
  - agent state  
  - datasets  
  - experiments  
  - traces  
  - telemetry  

### Deployment and operations
- Infrastructure for:
  - scaling  
  - clustering  
  - job scheduling  
  - distributed state  
  - durability  
- Rolling updates of new agent versions
- Developer tooling for local runs, testing, and debugging

### Datasets and experiments
- Versioned datasets with reproducibility guarantees  
- Experiment runner to measure agent/node performance and quality
- LLM or code-based evaluators 

### Tracing
- A way to persist, query, and visualize structured traces:
  - every node/step  
  - model calls  
  - tool calls  
  - retries/failures
- Time-series metrics (latency, tokens, error rates, online evaluation, etc.)  
- Dashboards and alerts (Prometheus/Grafana/etc.)


### Telemetry
- Time-series metrics (latency, tokens, errors, costs)  
- Dashboards, alerts, and visualizations  
- Long-term storage and indexing of traces and metrics


# Programming agents

This page explains how to code agents with Agent-o-rama. All examples are shown in both Java and Clojure.

## Table of Contents

1. [Basic Concepts](#basic-concepts)
2. [Routing in Agent Graphs](#routing-in-agent-graphs)
3. [Aggregation Subgraphs](#aggregation-subgraphs)
4. [Metadata](#metadata)
5. [Fault-tolerance and retries](#fault-tolerance-and-retries)
6. [Agent Objects](#agent-objects)
7. [Stores](#stores)
8. [Subagents and Recursion](#subagents-and-recursion)
9. [Deploying Modules](#deploying-modules)
10. [Updating Modules](#updating-modules)
11. [Learn next](#learn-next)

## Basic Concepts

Agent-o-rama is a library for building LLM agents as directed graphs. Nodes are the fundamental computation units in agent graphs. Each node is a plain Java or Clojure function that receives data, processes it, and either passes it along to other nodes or returns a final result. This is the basic building block that enables all other agent patterns. Agent-o-rama executes all nodes on [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html), which means node functions can be long-running and written in a blocking style without wasting thread resources.

Agent-o-rama captures all inputs, nested operations (e.g. model calls or database operations), and outputs from each node for viewing in the web UI. This information is also used and for to produce and display aggregated analytics about individual agent executions and time-series analytics for all agent executions.

Besides tracing, nodes are also the granularity at which streaming is consumed by agent clients. Things like calls to [Langchain4j](https://docs.langchain4j.dev/) models are automatically streamed for the node, and node functionsd can explicitly stream chunks back as well. This is discussed more on the [agent client](Agent-client-API) page.

### Key Components

- **AgentGraph**: The builder interface for defining agent execution graphs
- **AgentNode**: The interface for interacting with the agent execution environment from within nodes
- **AgentTopology**: The interface for defining agents, stores, and objects
- **AgentClient**: The interface for invoking agents and managing executions

### Understanding the Flow

Every agent execution starts with an invocation that provides input data to the first node. From there, data flows through the graph via `emit()` calls, which send data to downstream nodes. The execution continues until a node calls `result()`, which terminates the agent and returns the final output.

The `outputNodesSpec` parameter when defining nodes is crucial - it declares which nodes can receive data from this node. This creates a contract that the runtime enforces, preventing errors from emitting to undeclared nodes.

### Simple Example: Greeting Pipeline

This example shows a basic two-node pipeline where the first node processes the input and the second node creates the final result.

#### Java API

```java
import com.rpl.agentorama.*;

public class BasicAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("BasicAgent")
            .node("start", "process", (AgentNode agentNode, String input) -> {
                agentNode.emit("process", "Hello " + input);
            })
            .node("process", null, (AgentNode agentNode, String data) -> {
                agentNode.result("Processed: " + data);
            });
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])

(aor/defagentmodule BasicAgentModule
  [topology]
  (-> (aor/new-agent topology "BasicAgent")
      (aor/node
       "start"
       "process"
       (fn [agent-node input]
         (aor/emit! agent-node "process" (str "Hello " input))))
      (aor/node
       "process"
       nil
       (fn [agent-node data]
         (aor/result! agent-node (str "Processed: " data))))))
```

### Key Concepts

- **emit()**: Sends data to another node in the agent graph
- **result()**: Sets the final result of the agent execution (first-one-wins)
- **outputNodesSpec**: Declares which nodes can receive emissions from this node. This is either a single node name string, a list of node names, or null to indicate a terminal node.

## Routing in Agent Graphs

While simple linear pipelines are useful, real-world agents often need complex control flow. Agent graphs support loops, conditional routing, and multiple execution paths that can reconverge. This enables sophisticated decision-making and parallel processing within a single agent.

### Conditional Routing Example

This example demonstrates how an agent can route different types of messages through different processing paths, then reconverge to a single result. In this example each node emits only once, but the first node can emit to one of two nodes. In either cases, they reconverge to the node "finalize" which emits the final result.

#### Java API

```java
public class RouterAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("RouterAgent")
            .node("route", new String[]{"handle-urgent", "handle-default"},
                  (AgentNode agentNode, String message) -> {
              if (message.startsWith("urgent:")) {
                  agentNode.emit("handle-urgent", message);
              } else {
                  agentNode.emit("handle-default", message);
              }
            })
            .node("handle-urgent", "finalize", (AgentNode agentNode, String message) -> {
              String content = message.substring(7);
              agentNode.emit("finalize", Map.of("priority", "HIGH", "message", content));
            })
            .node("handle-default", "finalize", (AgentNode agentNode, String message) -> {
              agentNode.emit("finalize", Map.of("priority", "NORMAL", "message", message));
            })
            .node("finalize", null, (AgentNode agentNode, Map<String, String> data) -> {
              String result = String.format("[%s] %s", data.get("priority"), data.get("message"));
              agentNode.result(result);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule RouterAgentModule
  [topology]
  (-> (aor/new-agent topology "RouterAgent")
      (aor/node
       "route"
       ["handle-urgent" "handle-default"]
       (fn [agent-node message]
         (if (str/starts-with? message "urgent:")
           (aor/emit! agent-node "handle-urgent" message)
           (aor/emit! agent-node "handle-default" message))))
      (aor/node
       "handle-urgent"
       "finalize"
       (fn [agent-node message]
         (aor/emit! agent-node "finalize" {"priority" "HIGH" "message" (subs message 7)})))
      (aor/node
       "handle-default"
       "finalize"
       (fn [agent-node message]
         (aor/emit! agent-node "finalize" {"priority" "NORMAL" "message" message})))
      (aor/node
       "finalize"
       nil
       (fn [agent-node {:strs [priority message]}]
         (aor/result! agent-node (format "[%s] %s" priority message))))))
```


### Emitting Multiple Times

When a node emits multiple times, the first emit runs on the same node/thread, but subsequent emits will run in parallel on other threads or even other nodes. This means agent graphs automatically parallelize and distribute execution, which is powerful for performance but requires consideration if nodes might access the same resources (e.g. a database) in parallel. A node can emit any number of times to any number of downstream nodes.

If multiple nodes call `result()`, only the first one wins – subsequent results are ignored. This "first-wins" behavior is useful when you want to try multiple approaches and return the first successful result for expediency. That said, most agents will only call `result()` once and any parallel processing triggered by multiple emits will be combined with [agggregation](#aggregation-subgraphs).

#### Java API

```java
public class MultiEmitAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("MultiEmitAgent")
            .node("start", new String[]{"process-a", "process-b"}, (AgentNode agentNode, String input) -> {
              // Emit to multiple nodes in parallel
              agentNode.emit("process-a", input + "-A1");
              agentNode.emit("process-b", input + "-B");
              agentNode.emit("process-a", input + "-A2");
            })
            .node("process-a", "finalize", (AgentNode agentNode, String data) -> {
              // Simulate some work
              Thread.sleep(100);
              agentNode.emit("finalize", "Result A: " + data);
            })
            .node("process-b", "finalize", (AgentNode agentNode, String data) -> {
              // Simulate some work
              Thread.sleep(50);
              agentNode.emit("finalize", "Result B: " + data);
            })
            .node("finalize", null, (AgentNode agentNode, String result) -> {
              agentNode.result(result);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule MultiEmitAgentModule
  [topology]
  (-> (aor/new-agent topology "MultiEmitAgent")
      (aor/node
       "start"
       ["process-a" "process-b"]
       (fn [agent-node input]
         ;; Emit to both processing nodes in parallel
         (aor/emit! agent-node "process-a" (str input "-A"))
         (aor/emit! agent-node "process-b" (str input "-B"))
         (aor/emit! agent-node "process-a" (str input "-A"))))
      (aor/node
       "process-a"
       "finalize"
       (fn [agent-node data]
         ;; Simulate some work
         (Thread/sleep 100)
         (aor/emit! agent-node "finalize" (str "Result A: " data))))
      (aor/node
       "process-b"
       "finalize"
       (fn [agent-node data]
         ;; Simulate some work
         (Thread/sleep 50)
         (aor/emit! agent-node "finalize" (str "Result B: " data))))
      (aor/node
       "finalize"
       nil
       (fn [agent-node result]
         (aor/result! agent-node result)))))
```

## Aggregation Subgraphs

Aggregation subgraphs enable fan-out/fan-in patterns where work is distributed to multiple parallel nodes and results are collected and combined. This is essential for handling multiple concurrent operations, like making multiple LLM calls in parallel (since they're slow) and then combining the results.

### Basic Aggregation Example

This example shows how to distribute work across multiple parallel processors and then collect the results. The agg node runs once the subgraph preceding it has finished running/emitting.

#### Java API

```java
public class AggregationAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("AggregationAgent")
            .aggStartNode("distribute-work", "process-item", (AgentNode agentNode, List<String> items) -> {
              // Emit each item for parallel processing
              for (String item: items) {
                  agentNode.emit("process-item", item);
              }
              return null;
            })
            .node("process-item", "collect-results", (AgentNode agentNode, String item) -> {
              // Simulate processing each item
              String processed = "Processed: " + item.toUpperCase();
              agentNode.emit("collect-results", processed);
            })
            .aggNode("collect-results", null, BuiltIn.LIST_AGG,
                     (AgentNode agentNode, List<String> results, Object nodeStartRes) -> {
              agentNode.result(results);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule AggregationAgentModule
  [topology]
  (-> (aor/new-agent topology "AggregationAgent")
      (aor/agg-start-node
       "distribute-work"
       "process-item"
       (fn [agent-node items]
         ;; Emit each item for parallel processing
         (doseq [item items]
           (aor/emit! agent-node "process-item" item))))
      (aor/node
       "process-item"
       "collect-results"
       (fn [agent-node item]
         ;; Simulate processing each item
         (let [processed (str "Processed: " (str/upper-case item))]
           (aor/emit! agent-node "collect-results" processed))))
      (aor/agg-node
       "collect-results"
       nil
       aggs/+vec-agg
       (fn [agent-node results _]
         (aor/result! agent-node results)))))
```

### Aggregation Scope

Aggregation subgraphs can be nested, where each invocation of an agg start node creates a new aggregation context. This means you can have a first agg start node that emits multiple times to another agg start node, and the nested aggregation results get collected into the outer aggregation context.

For example, imagine processing multiple documents where each document needs to be analyzed by multiple experts in parallel, then the expert results for each document need to be combined, and finally all document results need to be aggregated together.

Agg start nodes are the only nodes that have return values. The return value is passed as the last argument to the corresponding agg node, allowing you to pass non-aggregated information through the aggregation.

#### Java API

```java
public class NestedAggregationModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("NestedAggregationAgent")
            // Outer aggregation: process multiple documents
            .aggStartNode("distribute-docs", "analyze-doc", (AgentNode agentNode, List<String> docs) -> {
              for (String doc : docs) {
                agentNode.emit("analyze-doc", doc);
              }
              return docs.size(); // Return value passed to outer agg node
            })
            // Inner aggregation: analyze each document with multiple methods
            .aggStartNode("analyze-doc", "analyze-method", (AgentNode agentNode, String doc) -> {
              agentNode.emit("analyze-method", doc, "sentiment");
              agentNode.emit("analyze-method", doc, "keywords");
              agentNode.emit("analyze-method", doc, "summary");
              return doc; // Return value passed to inner agg node
            })
            .node("analyze-method", "combine-analysis", (AgentNode agentNode, String doc, String method) -> {
              String result = method + " analysis of: " + doc;
              agentNode.emit("combine-analysis", Map.of("method", method, "result", result));
            })
            // Inner agg node: combine analyses for one document
            .aggNode("combine-analysis", "collect-docs", BuiltIn.LIST_AGG,
                     (AgentNode agentNode, List<Map<String, String>> analyses, String originalDoc) -> {
              agentNode.emit("collect-docs", Map.of("doc", originalDoc, "analyses", analyses));
            })
            // Outer agg node: collect all document results
            .aggNode("collect-docs", null, BuiltIn.LIST_AGG,
                     (AgentNode agentNode, List<Map<String, Object>> allResults, Integer totalDocs) -> {
              agentNode.result(Map.of("total-docs", totalDocs, "results", allResults));
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule NestedAggregationModule
  [topology]
  (-> (aor/new-agent topology "NestedAggregationAgent")
      ;; Outer aggregation: process multiple documents
      (aor/agg-start-node
       "distribute-docs"
       "analyze-doc"
       (fn [agent-node docs]
         (doseq [doc docs]
           (aor/emit! agent-node "analyze-doc" doc))
         (count docs))) ; Return value passed to outer agg node
      ;; Inner aggregation: analyze each document with multiple methods
      (aor/agg-start-node
       "analyze-doc"
       "analyze-method"
       (fn [agent-node doc]
         (aor/emit! agent-node "analyze-method" doc "sentiment")
         (aor/emit! agent-node "analyze-method" doc "keywords")
         (aor/emit! agent-node "analyze-method" doc "summary")
         doc)) ; Return value passed to inner agg node
      (aor/node
       "analyze-method"
       "combine-analysis"
       (fn [agent-node doc method]
         (let [result (str method " analysis of: " doc)]
           (aor/emit! agent-node "combine-analysis" {:method method :result result}))))
      ;; Inner agg node: combine analyses for one document
      (aor/agg-node
       "combine-analysis"
       "collect-docs"
       aggs/+vec-agg
       (fn [agent-node analyses original-doc]
         (aor/emit! agent-node "collect-docs" {:doc original-doc :analyses analyses})))
      ;; Outer agg node: collect all document results
      (aor/agg-node
       "collect-docs"
       nil
       aggs/+vec-agg
       (fn [agent-node all-results total-docs]
         (aor/result! agent-node {:total-docs total-docs :results all-results})))))
```

### Custom Aggregators

Built-in aggregators handle most use cases, but sometimes you need custom logic on how to aggregate inputs. You can do that by defining custom Rama aggregators, which is explained [here for Java](https://redplanetlabs.com/docs/~/aggregators.html#_defining_aggregators) and [here for Clojure](https://redplanetlabs.com/docs/~/clj-dataflow-lang.html#_aggregators).

Agent-o-rama also has a special aggregator type called "multi aggregator" which can process different kinds of inputs. When using multi-aggregators, aggregation inputs specify which "target" to run by including a tag as the first argument to `emit()`. The multi-agg then routes each input to the appropriate handler based on this tag.

This example shows how to process different types of data (numbers and text) with different logic, then combine the results.

#### Java API

```java
public class MultiAggAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("MultiAggAgent")
            .aggStartNode("distribute-data", Arrays.asList("process-numbers", "process-text"),
                          (AgentNode agentNode, Map<String, Object> data) -> {
              List<Integer> numbers = (List<Integer>) data.get("numbers");
              List<String> text = (List<String>) data.get("text");

              for (Integer num : numbers) {
                  agentNode.emit("process-numbers", num);
              }
              for (String txt : text) {
                  agentNode.emit("process-text", txt);
              }
              return null;
            })
            .node("process-numbers", "combine-results", (AgentNode agentNode, Integer number) -> {
              agentNode.emit("combine-results", "number", number);
            })
            .node("process-text", "combine-results", (AgentNode agentNode, String text) -> {
              agentNode.emit("combine-results", "text", text);
            })
            .aggNode("combine-results", null,
                     MultiAgg.init(() -> {
                         Map<String, Object> state = new HashMap<>();
                         state.put("number-sum", 0);
                         state.put("text", "");
                         return state;
                     })
                     .on("number", (Map<String, Object> state, Integer num) -> {
                         state.put("number-sum", (Integer) state.get("number-sum") + num);
                         return state;
                     })
                     .on("text", (Map<String, Object> state, String txt) -> {
                         state.put("text", state.get("text") + txt + " ");
                         return state;
                     }),
                     (AgentNode agentNode, Map<String, Object> state, Object _) -> {
              agentNode.result(state);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule MultiAggAgentModule
  [topology]
  (-> (aor/new-agent topology "MultiAggAgent")
      (aor/agg-start-node
       "distribute-data"
       ["process-numbers" "process-text"]
       (fn [agent-node {:strs [numbers text]}]
         (doseq [num numbers] (aor/emit! agent-node "process-numbers" num))
         (doseq [txt text] (aor/emit! agent-node "process-text" txt))))
      (aor/node
       "process-numbers"
       "combine-results"
       (fn [agent-node number]
         (aor/emit! agent-node "combine-results" "number" number)))
      (aor/node
       "process-text"
       "combine-results"
       (fn [agent-node text]
         (aor/emit! agent-node "combine-results" "text" text)))
      (aor/agg-node
       "combine-results"
       nil
       (aor/multi-agg
        (init [] {"number-sum" 0 "text" ""})
        (on "number" [state num] (update state "number-sum" + num))
        (on "text" [state txt] (update state "text" str txt " ")))
       (fn [agent-node state _]
         (aor/result! agent-node state)))))
```

### Early Aggregation Return

Aggregators can be written to return early, which causes aggregation to immediately finish (before all incoming data has been processed) and run the agg node. In Clojure, this is done by returning a value wrapped in `reduced`, and in Java with `FinishedAgg`. This is useful when you want to stop processing as soon as you have enough data or when a certain condition is met.

#### Java API

```java
import com.rpl.agentorama.FinishedAgg;
import com.rpl.rama.ops.RamaAccumulatorAgg1;

// Custom aggregator that stops when sum exceeds 100
public class SumUntil100 implements RamaAccumulatorAgg1<Integer, Integer> {
  @Override
  public Integer initVal() {
    return 0;
  }

  @Override
  public Integer accumulate(Integer curr, Integer value) {
    Integer newSum = curr + value;
    if (newSum > 100) {
      return new FinishedAgg(newSum); // Stop aggregating early
    }
    return newSum;
  }
}
```

#### Clojure API

```clojure
;; Custom aggregator that stops when sum exceeds 100
(def +sum-until-100
  (accumulator
   (fn [v]
     (term (fn [curr]
             (let [ret (+ curr v)]
               (if (> ret 100)
                 (reduced ret) ; Stop aggregating early
                 ret
               ))
           )))
   :init-fn
   (constantly 0)))
```


## Metadata

Metadata allows you to attach custom key-value data to agent executions. Metadata is set when invoking an agent and can be accessed from any node within the agent execution.

Common use cases for metadata include:

- **Tracking**: User IDs, session IDs, request IDs for correlating agent executions with application events
- **A/B Testing**: Feature flags, model versions, or experimental configurations
- **Configuration**: Runtime parameters that affect agent behavior without changing code, like model names to use
- **Debugging**: Additional context for troubleshooting specific executions

Metadata is automatically included in traces and analytics, making it easy to filter and analyze agent performance by any metadata dimension.

### Setting Metadata

Metadata is set when invoking an agent using the "with context" methods. Metadata keys must be strings, and values must be strings, numbers (int, long, float, double), or booleans.

Here's a quick example of invoking an agent with metadata:

#### Java API

```java
import com.rpl.agentorama.AgentContext;

// Create context with metadata
AgentContext context = AgentContext.metadata("user-id", "user-123")
                                   .metadata("model", "gpt-4");

// Invoke with context
String result = agent.invokeWithContext(context, "Hello, world!");
```

#### Clojure API

```clojure
;; Create context with metadata
(let [context {:metadata {"user-id" "user-123"
                          "model" "gpt-4"}}]

  ;; Invoke with context
  (let [result (aor/agent-invoke-with-context agent context "Hello, world!")]
    (println "Result:" result)))
```

### Accessing Metadata in Agents

Inside agent nodes, you can access the metadata using `getMetadata` / `get-metadata`. The metadata is immutable during execution – it reflects what was set at invocation time even if its modified after initiation through the UI or API.

#### Java API

```java
public class MetadataAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("MetadataAgent")
            .node("process", null, (AgentNode agentNode) -> {
              // Get metadata
              Map<String, Object> metadata = agentNode.getMetadata();

              String userId = (String) metadata.get("user-id");
              String model = (String) metadata.get("model");

              System.out.println("Processing for user: " + userId);
              System.out.println("Using model: " + model);

              agentNode.result("Processed for " + userId);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule MetadataAgentModule
  [topology]
  (-> (aor/new-agent topology "MetadataAgent")
      (aor/node
       "process"
       nil
       (fn [agent-node]
         ;; Get metadata
         (let [metadata (aor/get-metadata agent-node)
               user-id (get metadata "user-id")
               model (get metadata "model")]

           (println "Processing for user:" user-id)
           (println "Using model:" model)

           (aor/result! agent-node (str "Processed for " user-id)))))))
```

## Fault-tolerance and retries

Agent-o-rama has built-in fault-tolerance for agents. If a node fails, like due to an exception making an API call or a hardware failure on a cluster node, it will retry. By default, an agent can have at most two retries, and this is configurable in the web UI in the config page for the agent on the `max.retries` config.

## Agent Objects

Agent objects are shared resources like AI models, database connections, or API clients that agents can access during execution. They enable agents to interact with external systems and maintain expensive resources efficiently. Many resources like AI models and database connections are expensive to create and maintain persistent connections. Agent object builders allow you to create these resources once and reuse them across multiple agent invocations, rather than recreating them for every agent execution.

### Thread Safety and Pooling

Agent objects are all about thread safety. There are two modes:

1. **Thread-safe objects**: When declared with `threadSafe()`, one object is built for the entire process and reused across all node invokes on all threads. Use this if you know the object you're creating (like a database client) is thread-safe.

2. **Pooled objects**: By default, a pool of objects is maintained, and nodes get exclusive access to an instance during execution. When the node finishes, the object goes back into the pool. The pool size can be configured with the `workerObjectLimit(amt)` option (defaults to 100).

This ensures that your agents can safely use shared resources without worrying about concurrency issues.

### Static and Dynamic Objects

This example shows both static objects (like API keys) and dynamic objects (like AI models that need to be built with configuration). Static objects are created once and shared, while dynamic objects are built on-demand with proper pooling and thread safety.

#### Java API

```java
public class AgentObjectsModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    // Declare static agent object
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));

    // Declare agent object builder
    topology.declareAgentObjectBuilder("openai-model", setup -> {
      String apiKey = setup.getAgentObject("openai-api-key");
      return OpenAiStreamingChatModel.builder()
                                    .apiKey(apiKey)
                                    .modelName("gpt-4o-mini")
                                    .build();
      },
      AgentObjectOptions.workerObjectLimit(200));

    topology.newAgent("AgentWithObjects")
            .node("process", null, (AgentNode agentNode, String input) -> {
              ChatModel model = agentNode.getAgentObject("openai-model");
              String response = model.chat(input);
              agentNode.result(response);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule AgentObjectsModule
  [topology]
  ;; Declare static agent object
  (aor/declare-agent-object topology "openai-api-key" (System/getenv "OPENAI_API_KEY"))

  ;; Declare agent object builder
  (aor/declare-agent-object-builder
   topology
   "openai-model"
   (fn [setup]
     (-> (OpenAiStreamingChatModel/builder)
         (.apiKey (aor/get-agent-object setup "openai-api-key"))
         (.modelName "gpt-4o-mini")
         .build))
    {:worker-object-limit 200})

  (-> (aor/new-agent topology "AgentWithObjects")
      (aor/node
       "process"
       nil
       (fn [agent-node input]
         (let [model (aor/get-agent-object agent-node "openai-model")]
           (aor/result! agent-node (lc4j/basic-chat model input)))))))
```


### Advanced Object Configuration

Agent objects support several configuration options:

- **Pool size**: Control the maximum number of objects in the pool with `workerObjectLimit`
- **Thread safety**: Mark objects as thread-safe with `threadSafe` to share a single instance
- **Auto-tracing**: LangChain4j chat models and embedding stores are automatically wrapped and traced, but this can be turned off with the `autoTracing` option

### Streaming Chat Models

When you declare a `StreamingChatModel` as an agent object, Agent-o-rama automatically captures the stream and forwards chunks to the node. However, when you fetch the object in a node, you always get a `ChatModel` interface (not `StreamingChatModel`). This means you can use streaming models in a blocking style within agent nodes, while agent clients can stream the node to get the stream of all model calls. If you don't want streaming behavior, declare the object as a non-streaming `ChatModel`.

#### Java API

```java
// Declare streaming model
topology.declareAgentObjectBuilder("streaming-model", setup -> {
  return OpenAiStreamingChatModel.builder()
                                 .apiKey(apiKey)
                                 .modelName("gpt-4")
                                 .build();
});

// In node: fetch as ChatModel (not StreamingChatModel)
topology.newAgent("MyAgent")
        .node("process", null, (AgentNode agentNode, String input) -> {
          ChatModel model = agentNode.getAgentObject("streaming-model"); // Always ChatModel
          String response = model.chat(input); // Blocking call, but streaming happens automatically
          agentNode.result(response);
        });

// Non-streaming model - no streaming behavior
topology.declareAgentObjectBuilder("blocking-model", setup -> {
  return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4")
                        .build();
});
```

#### Clojure API

```clojure
;; Declare streaming model
(aor/declare-agent-object-builder
 topology
 "streaming-model"
 (fn [setup]
   (-> (OpenAiStreamingChatModel/builder)
       (.apiKey api-key)
       (.modelName "gpt-4")
       .build)))

;; In node: fetch as ChatModel (not StreamingChatModel)
(-> (aor/new-agent topology "MyAgent")
    (aor/node
     "process"
     nil
     (fn [agent-node input]
       (let [model (aor/get-agent-object agent-node "streaming-model")] ; Always ChatModel
         (aor/result! agent-node (aor/chat model input)))))) ; Blocking call, but streaming happens automatically

;; Non-streaming model - no streaming behavior
(aor/declare-agent-object-builder
 topology
 "blocking-model"
 (fn [setup]
   (-> (OpenAiChatModel/builder)
       (.apiKey api-key)
       (.modelName "gpt-4")
       .build)))
```

## Stores

Real agents need to remember information, maintain user sessions, cache results, and share data between executions. Agent-o-rama stores provide persistent data access for agents, enabling them to maintain state across invocations and share data between different agent executions. Stores are built-in and are high-performance, durable, scalable, and replicated. Because they're built-in, they require no additional work for deployment or configuration. While it's easy to use databases from Agent-o-rama, it's usually much more convenient and higher performance to just use a store.

There are three types of stores available in Agent-o-rama: key-value store, document store, and [PState](https://redplanetlabs.com/docs/~/pstates.html) store. Stores are declared as part of the agent module definition, and store names always begin with `$$`. Stores are fetched within agent nodes by calling `getStore`.

### Key-Value Store Example

Key-value stores are perfect for simple data like counters, flags, or cached values.

#### Java API

```java
public class KeyValueStoreModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareKeyValueStore("$$counters", String.class, Integer.class);

    topology.newAgent("KeyValueStoreAgent")
            .node("manage-counter", null, (AgentNode agentNode, String counterName, String operation) -> {
              KeyValueStore<String, Integer> store = agentNode.getStore("$$counters");
              switch (operation) {
                  case "get":
                      Integer value = store.get(counterName);
                      agentNode.result(Map.of("counter", counterName, "value", value));
                      break;
                  case "increment":
                      Integer currentValue = store.get(counterName);
                      if (currentValue == null) currentValue = 0;
                      store.put(counterName, currentValue + 1);
                      agentNode.result(Map.of("counter", counterName, "new-value", currentValue + 1));
                      break;
              }
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule KeyValueStoreModule
  [topology]
  (aor/declare-key-value-store topology "$$counters" String Long)

  (-> (aor/new-agent topology "KeyValueStoreAgent")
      (aor/node
       "manage-counter"
       nil
       (fn [agent-node counter-name operation]
         (let [store (aor/get-store agent-node "$$counters")]
           (case operation
             "get"
             (aor/result! agent-node {:counter counter-name :value (store/get store counter-name)})
             "increment"
             (let [current-value (or (store/get store counter-name) 0)
                   new-value (inc current-value)]
               (store/put! store counter-name new-value)
               (aor/result! agent-node {:counter counter-name :new-value new-value}))))))))
```

### Document Store Example

Document stores are essentially key-value stores where the values are maps with their own schema for each field. You can perform operations on individual nested values without reading or writing the entire document. This is ideal for structured data with multiple fields, like user profiles.

#### Java API

```java
public class DocumentStoreModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareDocumentStore("$$user-profiles", String.class,
                                   "name", String.class,
                                   "age", Long.class);

    topology.newAgent("DocumentStoreAgent")
            .node("update-profile", "read-profile", (AgentNode agentNode, Map<String, Object> data) -> {
              DocumentStore store = agentNode.getStore("$$user-profiles");
              String userId = (String) data.get("user-id");
              Map<String, Object> updates = (Map<String, Object>) data.get("updates");

              if (updates.containsKey("name")) {
                store.putDocumentField(userId, "name", updates.get("name"));
              }
              if (updates.containsKey("age")) {
                store.putDocumentField(userId, "age", updates.get("age"));
              }

              agentNode.emit("read-profile", userId);
            })
            .node("read-profile", null, (AgentNode agentNode, String userId) -> {
              DocumentStore store = agentNode.getStore("$$user-profiles");
              String name = store.getDocumentField(userId, "name");
              Long age = store.getDocumentField(userId, "age");
              agentNode.result(Map.of("user-id", userId, "name", name, "age", age));
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule DocumentStoreModule
  [topology]
  (aor/declare-document-store topology "$$user-profiles" String
                              "name" String
                              "age" Long)

  (-> (aor/new-agent topology "DocumentStoreAgent")
      (aor/node
       "update-profile"
       "read-profile"
       (fn [agent-node {:strs [user-id updates]}]
         (let [store (aor/get-store agent-node "$$user-profiles")]
           (when (contains? updates "name") (store/put-document-field! store user-id "name" (get updates "name")))
           (when (contains? updates "age") (store/put-document-field! store user-id "age" (get updates "age")))
           (aor/emit! agent-node "read-profile" user-id))))
      (aor/node
       "read-profile"
       nil
       (fn [agent-node user-id]
         (let [store (aor/get-store agent-node "$$user-profiles")
               name (store/get-document-field store user-id "name")
               age (store/get-document-field store user-id "age")]
           (aor/result! agent-node {:user-id user-id :name name :age age}))))))
```

### PState Store Example

PState stores provide direct access to Rama's [PStates](https://redplanetlabs.com/docs/~/pstates.html). PStates are declared as any combination of data structures of any size with any amount of nesting. PState stores are extremely flexible and are used when you need more sophisticated structures than key-value or document stores provide, such as nested maps, lists with subindexing, or complex hierarchical data.

#### Java API

```java
import com.rpl.rama.Path;
import com.rpl.rama.PState;

public class PStateStoreModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    // Declare PState store with nested schema
    topology.declarePStateStore(
      "$$user-data",
      PState.mapSchema(
        String.class,  // user-id
        PState.fixedKeysSchema(
          "age", Integer.class,
          "memories", PState.listSchema(String.class).subindexed())));

    topology.newAgent("PStateStoreAgent")
            .node("update-user", "read-user", (AgentNode agentNode, Map<String, Object> data) -> {
              PStateStore store = agentNode.getStore("$$user-data");
              String userId = (String) data.get("user-id");
              Integer age = (Integer) data.get("age");
              String memory = (String) data.get("memory");

              // Update age if provided
              if (age != null) {
                store.transform(userId, Path.key(userId, "age").termVal(age));
              }

              // Append memory if provided
              if (memory != null) {
                store.transform(userId, Path.key(userId, "memories").afterElem().termVal(memory));
              }

              agentNode.emit("read-user", userId);
            })
            .node("read-user", null, (AgentNode agentNode, String userId) -> {
              PStateStore store = agentNode.getStore("$$user-data");

              // Read age
              Integer age = (Integer) store.selectOne(Path.key(userId, "age"));

              // Read all memories
              List<String> memories = store.select(Path.key(userId, "memories").all());

              agentNode.result(Map.of("user-id", userId, "age", age, "memories", memories));
            });
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.rama.path :as path])

(aor/defagentmodule PStateStoreModule
  [topology]
  ;; Declare PState store with nested schema
  (aor/declare-pstate-store
   topology
   "$$user-data"
   {String (fixed-keys-schema
            {:age Long
             :memories (vector-schema String {:subindex? true})})})

  (-> topology
      (aor/new-agent "PStateStoreAgent")
      (aor/node
       "update-user"
       "read-user"
       (fn [agent-node {:strs [user-id age memory]}]
         (let [store (aor/get-store agent-node "$$user-data")]
           ;; Update age if provided
           (when age
             (store/pstate-transform!
              [(path/keypath user-id :age) (path/termval age)]
              store
              user-id))

           ;; Append memory if provided
           (when memory
             (store/pstate-transform!
              [(path/keypath user-id :memories) AFTER-ELEM (path/termval memory)]
              store
              user-id))

           (aor/emit! agent-node "read-user" user-id))))
      (aor/node
       "read-user"
       nil
       (fn [agent-node user-id]
         (let [store (aor/get-store agent-node "$$user-data")
               ;; Read age using path
               age (store/pstate-select-one (path/keypath user-id :age) store user-id)
               ;; Read all memories using path
               memories (store/pstate-select [(path/keypath user-id :memories) ALL] store user-id)]
           (aor/result! agent-node {:user-id user-id :age age :memories memories}))))))
```

## Subagents and Recursion

Agents can call other agents within the same module or across modules, including recursively and mutually recursively. This makes it trivial to orchestrate complex applications consisting of many agents working together.

Real-world systems often need to break down complex tasks into smaller, manageable pieces. Subagents enable this decomposition while maintaining the benefits of the agent execution model. They also enable recursive patterns for algorithms that naturally decompose into smaller instances of the same problem.

### Calling Agents in the Same Module

The simplest form of subagent invocation is calling another agent within the same module. You get an agent client within node functions and invoke them directly. Subagent calls are also tracked in traces and incorporated into agent analytics.

#### Java API

```java
public class SubagentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    // Helper agent that processes text
    topology.newAgent("TextProcessor")
            .node("process", null, (AgentNode agentNode, String text) -> {
              String processed = text.toUpperCase();
              agentNode.result(processed);
            });

    // Main agent that uses the helper
    topology.newAgent("MainAgent")
            .node("orchestrate", null, (AgentNode agentNode, String input) -> {
              AgentClient processor = agentNode.getAgentClient("TextProcessor");
              String result = processor.invoke(input);
              agentNode.result("Processed: " + result);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule SubagentModule
  [topology]
  ;; Helper agent that processes text
  (-> topology
      (aor/new-agent "TextProcessor")
      (aor/node
       "process"
       nil
       (fn [agent-node text]
         (aor/result! agent-node (str/upper-case text)))))

  ;; Main agent that uses the helper
  (-> topology
      (aor/new-agent "MainAgent")
      (aor/node
       "orchestrate"
       nil
       (fn [agent-node input]
         (let [processor (aor/agent-client agent-node "TextProcessor")
               result (aor/agent-invoke processor input)]
           (aor/result! agent-node (str "Processed: " result)))))))
```

### Recursive Agent Invocation

Agents can call themselves recursively, enabling elegant implementations of recursive algorithms.

#### Java API

```java
public class RecursiveModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("Factorial")
            .node("compute", null, (AgentNode agentNode, Integer n) -> {
              if (n <= 1) {
                agentNode.result(1);
              } else {
                AgentClient self = agentNode.getAgentClient("Factorial");
                Integer subResult = (Integer) self.invoke(n - 1);
                agentNode.result(n * subResult);
              }
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule RecursiveModule
  [topology]
  (-> topology
      (aor/new-agent "Factorial")
      (aor/node
       "compute"
       nil
       (fn [agent-node n]
         (if (<= n 1)
           (aor/result! agent-node 1)
           (let [self (aor/agent-client agent-node "Factorial")
                 sub-result (aor/agent-invoke self (dec n))]
             (aor/result! agent-node (* n sub-result))))))))
```

### Cross-Module Agent Calls

This example shows how one agent can call another agent in a different module.

#### Java API

```java
// Module 1: Greeter agent
public class GreeterModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("Greeter")
            .node("greet", null, (AgentNode agentNode, String name) -> {
              agentNode.result("Hello, " + name + "!");
            });
  }
}

// Module 2: Mirror agent that calls Greeter
public class MirrorModule extends AgentModule {
  private static final String GREETER_MODULE_NAME = new GreeterModule().getModuleName();

  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("MirrorAgent")
            .node("process", null, (AgentNode agentNode, String name) -> {
                AgentClient greeterClient = agentNode.getMirrorAgentClient(GREETER_MODULE_NAME, "Greete");
                String greeting = (String) greeterClient.invoke(name);
                agentNode.result("Mirror says: " + greeting);
            });
  }
}
```

#### Clojure API

```clojure
;; Module 1: Greeter agent
(aor/defagentmodule GreeterModule
  [topology]
  (-> topology
      (aor/new-agent "Greeter")
      (aor/node
       "greet"
       nil
       (fn [agent-node name]
         (aor/result! agent-node (str "Hello, " name "!"))))))

;; Module 2: Mirror agent that calls Greeter
(aor/defagentmodule MirrorModule
 [topology]
 (-> topology
     (aor/new-agent "MirrorAgent")
     (aor/node
      "process"
      nil
      (fn [agent-node name]
        (let [greeter-client (aor/mirror-agent-client agent-node (get-module-name GreeterModule) "Greeter")
              greeting (aor/agent-invoke greeter-client name)]
          (aor/result! agent-node (str "Mirror says: " greeting)))))))
```

## Serialization

Agent-o-rama needs to know how to serialize any objects sent to agents as arguments, used as results, or passed between agents in emits. Most commonly used types are already supported, and it's easy to add serializers for your own types. See Rama's serialization documentation [for Java](https://redplanetlabs.com/docs/~/serialization.html) and [for Clojure](https://redplanetlabs.com/docs/~/clj-serialization.html) to learn how.

## Deploying Modules

Once you've defined your agents, you need to deploy them to a Rama cluster. Agent-o-rama modules are Rama modules, so they follow the standard Rama deployment process. For deploying a Rama cluster, consult the Rama docs on [setting up a cluster](https://redplanetlabs.com/docs/~/operating-rama.html#_setting_up_a_rama_cluster). There are also one-click deploys available [for AWS](https://github.com/redplanetlabs/rama-aws-deploy) and [for Azure](https://github.com/redplanetlabs/rama-azure-deploy).

### Local Development

For local development and testing, you can use `InProcessCluster` (IPC) which runs everything in a single JVM process. This is perfect for development and doesn't require any cluster setup. You can also start the full UI with IPC, which by default will launch at `http://localhost:1974`. When the UI is launched this way, it stays running until you press Enter in the terminal, letting you manually invoke agents, inspect traces, and explore datasets and experiments before shutting it down.

#### Java API

```java
import com.rpl.rama.test.*;

public class Main {
  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      try(AutoCloseable ui = UI.start(ipc)) {
        // Launch your module
        MyAgentModule module = new MyAgentModule();
        ipc.launchModule(module, new LaunchConfig(4, 2));

        // Get agent manager and interact with agents
        String moduleName = module.getModuleName();
        AgentManager manager = AgentManager.create(ipc, moduleName);
        AgentClient agent = manager.getAgentClient("MyAgent");

        // Invoke the agent
        Object result = agent.invoke("input data");
        System.out.println("Result: " + result);
      }
    }
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.rama.test :as rtest])

(with-open [ipc (rtest/create-ipc)
            ui (aor/start-ui ipc)]
  ;; Launch your module
  (rtest/launch-module! ipc MyAgentModule {:tasks 4 :threads 2})

  ;; Get agent manager and interact with agents
  (let [module-name (rama/get-module-name MyAgentModule)
        manager (aor/agent-manager ipc module-name)
        agent (aor/agent-client manager "MyAgent")]

    ;; Invoke the agent
    (let [result (aor/agent-invoke agent "input data")]
      (println "Result:" result))))
```

### Testing with remote datasets

When developing a new version of an agent, you may want to test it locally against real data before deploying it to an actual cluster. Agent-o-rama supports this by letting you create "remote datasets" in IPC and then running experiments against that. See [this section](Datasets,-evaluators,-and-experiments#remote-datasets) for the details.


### Deploying to a Cluster

To deploy to a production Rama cluster, you use the Rama CLI. First, package your module as a JAR file with all dependencies included.

#### Building the JAR

For Maven projects, use the Maven Assembly or Shade plugin to create an uber-jar:

```bash
mvn clean package
```

For Leiningen projects:

```bash
lein uberjar
```

#### Deploying with Rama CLI

Once you have your JAR, deploy it using the `rama deploy` command:

```bash
rama deploy \
  --action launch
  --jar target/my-agents.jar \
  --module com.mycompany.MyAgentModule \
  --tasks 32 \
  --threads 8 \
  --workers 4
```

See the [Rama documentation on launching modules](https://redplanetlabs.com/docs/~/operating-rama.html#_launching_modules) for a full explanation of these parameters.

## Updating Modules

Modules aren't static – their code evolves over time as you add features, fix bugs, or optimize performance. To update a module, you use Rama's one-line [update command](https://redplanetlabs.com/docs/~/operating-rama.html#_updating_modules), like so:


```bash
rama deploy \
  --action update \
  --jar target/my-agents-v2.jar \
  --module com.mycompany.MyAgentModule
```

It's possible or even likely there are some agent invocations mid-execution when you perform an update, especially if you have long-running agents. Agent-o-rama lets you decide what to do with these in-flight agent invocations on update by setting an "update mode" on the agent definition.


Update mode is set on the agent graph when defining the agent. Here's how to set it:

#### Java API

```java
public class MyAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("MyAgent")
            .setUpdateMode(UpdateMode.CONTINUE)  // or RESTART or DROP
            .node("process", null, (AgentNode agentNode, String input) -> {
              // Agent logic here
              agentNode.result("processed: " + input);
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule MyAgentModule
  [topology]
  (-> (aor/new-agent topology "MyAgent")
      (aor/set-update-mode :continue)  ; or :restart or :drop
      (aor/node
       "process"
       nil
       (fn [agent-node input]
         (aor/result! agent-node (str "processed: " input))))))
```

### Update Modes

You can choose from three update modes:

#### 1. **CONTINUE Mode** (Default)

In-flight executions continue where they left off with the new agent definition. The agent's execution state is preserved and it resumes on the new code version.

Use this mode when:
- You want agents to complete their work without interruption
- The new code is compatible with in-flight execution state
- You're making incremental changes that don't fundamentally alter the agent's logic

#### 2. **RESTART Mode**

In-flight executions restart from the beginning with the new agent definition. The agent is invoked again with its original input arguments.

Use this mode when:
- The new code has significant changes that make continuing problematic
- You want all executions to use the new logic from start to finish

#### 3. **DROP Mode**

In-flight executions are terminated and not restarted. The agent invocation is simply dropped.

Use this mode when:
- The agent's work is no longer needed
- You're deprecating functionality
- Completing in-flight work would be problematic or wasteful

### Scaling Modules

You can also scale a module to change its resource allocation without changing code:

```bash
rama scaleExecutors \
  --module com.mycompany.MyAgentModule \
  --threads 32
  --workers 16
```

The docs on scaling are [here](https://redplanetlabs.com/docs/~/operating-rama.html#_scaling_modules).

## Learn next

- [Agent clients](Agent-client-API)
- [Human-in-the-loop](Human‐in‐the‐loop)
- [Streaming](Streaming)
- [Tools agent](Tools)


# Quickstart

This guide will get you up and running with Agent-o-rama in just a few minutes. You'll learn how to run examples locally and then deploy to a Rama cluster.

## Table of Contents

1. [Running Examples Locally](#running-examples-locally)
2. [Running on a Local Rama Cluster](#running-on-a-local-rama-cluster)

## Running Examples Locally

The fastest way to try Agent-o-rama is to run the examples locally using InProcessCluster (IPC). This runs a complete Rama cluster in a single JVM process - perfect for development and testing.

The `examples/` directory in the Agent-o-rama repo contains `java/` and `clj/` subfolders with lots of examples of agents. One of those examples is a simple ReAct agent that can search the web to answer questions. Below are instructions for running the Java or Clojure versions of that agent.

For both, the example will run an IPC cluster, launch the UI at `http://localhost:1974`, prompt you for a question, invoke the agent, and then print the result. The UI will remain open for viewing traces, performing more invokes, and other exploration until you press enter in the terminal.

This agent takes in a single list of messages as an argument, so to invoke from the UI give input of the form `[["What is the 5th tallest mountain?"]]`.

### Java Example

#### Prerequisites

- Maven installed
- OpenAI API key
- [Tavily](https://www.tavily.com/) API key (the free tier is sufficient)

#### Running the Example

1. **Set up environment variables:**
   ```bash
   export OPENAI_API_KEY=your_openai_key_here
   export TAVILY_API_KEY=your_tavily_key_here
   ```

2. **Build and run the example:**
   ```bash
   cd examples/java
   ./run-example com.rpl.agent.react.ReActExample
   ```


### Clojure Example

#### Prerequisites

- [Leiningen](https://leiningen.org/) installed
- OpenAI API key
- [Tavily](https://www.tavily.com/) API key (the free tier is sufficient)

#### Running the Example

1. **Set up environment variables:**
   ```bash
   export OPENAI_API_KEY=your_openai_key_here
   export TAVILY_API_KEY=your_tavily_key_here
   ```

2. **Start a REPL:**
   ```bash
   cd examples/clj
   lein repl
   ```

3. **Load the example and run it:**
   ```clojure
   (require '[com.rpl.agent.react :as react])
   (react/run-agent)
   ```

## Running on a Local Rama Cluster

For a more production-like experience, you can run Agent-o-rama on a local Rama cluster. This gives you the full distributed capabilities of Rama while running on your local machine.

### Step 1: Download Rama

Download the latest Rama release from [https://redplanetlabs.com/download](https://redplanetlabs.com/download) and unpack it somewhere.

### Step 2: Set Up a Local Rama Cluster

Run a single node cluster by running these commands:

```bash
./rama devZookeeper &
./rama conductor &
./rama supervisor &
```

This isn't a production-worthy setup – see the docs on [setting up Rama clusters](https://redplanetlabs.com/docs/~/operating-rama.html#_setting_up_a_rama_cluster) for more details. There are also one-click deploys available [for AWS](https://github.com/redplanetlabs/rama-aws-deploy) and [for Azure](https://github.com/redplanetlabs/rama-azure-deploy).

### Step 3: Download Agent-o-rama Release

Download the latest Agent-o-rama release from [https://github.com/redplanetlabs/agent-o-rama/releases](https://github.com/redplanetlabs/agent-o-rama/releases) and unpack it.


### Step 4: Start the Agent-o-rama UI

Launch the Agent-o-rama frontend:

```bash
./aor --rama /path/to/rama-root &
```

The UI will be available at [http://localhost:1974](http://localhost:1974).

### Step 5: Build and Deploy Your Module

Build an uberjar witn your module code. To do so for the examples:

```bash
# For Java projects  
cd examples/java
mvn clean package -Dmaven.test.skip=true

# For Clojure projects
cd examples/clj
lein uberjar
```

Deploy the module to your local Rama cluster using the Rama CLI:

```bash
# Navigate to your Rama installation
cd /path/to/rama-root

# Deploy the module (Java uberjar)
./rama deploy \
  --action launch \
  --jar /path/to/your-module.jar \
  --module com.rpl.agent.react.ReActModule \
  --tasks 4 \
  --threads 2 \
  --workers 1

# Deploy the module (Clojure uberjar)
./rama deploy \
  --action launch \
  --jar /path/to/your-module.jar \
  --module com.rpl.agent.react/ReActModule \
  --tasks 4 \
  --threads 2 \
  --workers 1
```

This agent takes in a single list of messages as an argument, so to invoke from the UI give input of the form `[["What is the 5th tallest mountain?"]]`.

## Next Steps

Now that you have a module deployed you can play with it in the UI by invoking the module from the web interface, exploring traces, building datasets, running experiments, and viewing analytics.

## Getting Help

- **Documentation**: Check out the comprehensive guides on the Agent-o-rama [wiki](https://github.com/redplanetlabs/agent-o-rama/wiki)
- **Examples**: Explore the `examples/` directory for more patterns
- **Community**: Join discussions on [Discord](https://discord.gg/xSRjMTvDSj), the [Rama mailing list](https://groups.google.com/u/1/g/rama-user), or the #rama channel on [Clojurians](https://clojurians.slack.com/)

Happy building with Agent-o-rama!


# Agent-o-rama vs [Spring AI](https://github.com/spring-projects/spring-ai)

## Similarities

| Feature | Summary |
|--------|---------|
| JVM-based | Spring AI is Spring Boot–native; Agent-o-rama provides first-class Java and Clojure APIs. |
| Model abstraction | Both provide unified client APIs over multiple LLM providers. |
| Tool integration | Both allow exposing regular functions as tools for LLMs. |
| Streaming | Both support streaming model responses. |
| RAG | Both integrate with database and vector stores to support RAG-style retrieval workflows. |
| Evaluation utilities | Both provide LLM and code-based evaluators, but only Agent-o-rama includes the surrounding system for datasets, experiments, and online evaluation; Spring AI leaves all of that to the user. |

---

## Differences

| Area | Agent-o-rama | Spring AI |
|------|--------------|-----------|
| Scope | Complete platform: runtime, storage, datasets, experiments, telemetry, UI. | Library focused on AI integration inside Spring apps; platform pieces left to the user. |
| Agent model | Explicit graphs of Java/Clojure functions with parallel execution. | No agent graph runtime; control flow is ad hoc in application code. |
| Execution model | Distributed, parallel graph execution with built-in scaling on a Rama cluster. | Executes inside a Spring Boot service; no built-in distributed orchestration. |
| Human-in-the-loop | Built-in pause/resume API for requesting human input during execution. | No equivalent feature. |
| Storage | Built-in, scalable, replicated storage (any data model) or external databases. | No general-purpose storage engine; applications rely on Spring data sources or separate databases that you operate. |
| Datasets | Built-in versioned datasets for capturing inputs/outputs. | No equivalent feature. |
| Experiments | Built-in experiment runner for evaluating whole agents or individual nodes with LLM or function evaluators. | Provides evaluators but no experiment runner. |
| Actions / online evaluation | Easy to set up custom hooks on production runs for online evaluation, adding to datasets, webhooks, and more. | No equivalent feature. |
| Telemetry | Built-in time-series telemetry for agent performance, latency, token usage, model costs, and online evaluation. | Uses Spring observability integrations; no built-in dashboards or time-series store. |
| UI | Includes UI for traces, datasets, experiments, telemetry. | No agent-level UI; relies on generic Spring observability tools. |
| Deployment | Runs on a Rama cluster (in-process, single-node, or distributed). Rama is the only dependency. Deploying/updating/scaling agents are one-line CLI commands. | Embedded in Spring Boot apps; deployment and scaling rely on your own infrastructure and offer no built-in orchestration. |

---

# Missing pieces you must build yourself if using Spring AI

Spring AI provides model clients, tools, RAG utilities, and basic evaluators, but everything resembling an agent platform must be built by you:

### Runtime and execution
- Distributed or parallel agent execution across threads/machines
- Backpressure, retries, timeouts, and fault-tolerance across agent steps 
- Pause/resume mechanics for human-in-the-loop 

### Storage
- One or more stores for:
  - agent state  
  - datasets  
  - experiments  
  - traces  
  - telemetry  


### Deployment and operations
- Infrastructure for:
  - scaling  
  - clustering  
  - job scheduling  
  - distributed state  
  - durability  
- Rolling updates of new agent versions
- Developer tooling for local runs, testing, and debugging

### Datasets and experiments
- Versioned datasets with reproducibility guarantees  
- Experiment runner to measure agent/node performance and quality
- LLM or code-based evaluators  

### Telemetry and tracing
- End-to-end tracing across workflow steps, database calls, and services, plus any UI for viewing and querying traces; Spring AI only emits data about its own model/tool calls.
- Provides tracing hooks for model and tool calls, but no agent-level tracing or UI; all workflow, database, and cross-service tracing must be instrumented and maintained by the user.
- Time-series metrics (latency, tokens, errors, costs) can be emitted, but you must choose and operate the backend yourself  
- Dashboards, alerting, and visualizations must be built using external tools (Prometheus, Grafana, Langfuse, OpenTelemetry backends, etc.)  
- Long-term storage and querying of traces and metrics must also be set up and maintained by your team  


# Streaming

Streaming enables agents to provide real-time feedback to users as they process requests, rather than waiting for the entire invoke to complete. This is useful for creating responsive applications, especially when working with LLMs that can take seconds or minutes to generate complete responses.

## Table of Contents

1. [Manual Streaming](#manual-streaming)
2. [Automatically Streamed Models](#automatically-streamed-models)
3. [Consuming Streams from Agent Clients](#consuming-streams-from-agent-clients)
4. [Streaming Analytics](#streaming-analytics)

## Manual Streaming

You can manually stream data from any agent node using the `streamChunk` method on `AgentNode`. This is useful for providing progress updates, incremental results, or any real-time feedback during agent execution.

### Java API

```java
import com.rpl.agentorama.*;

public class StreamingAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.newAgent("StreamingAgent")
            .node("process", null, (AgentNode agentNode, Integer numChunks) -> {
              // Stream chunks one at a time
              for (int i = 0; i < numChunks; i++) {
                Thread.sleep(100); // Simulate work
                agentNode.streamChunk("chunk" + i);
              }
              // Return final result
              agentNode.result("done");
            });
  }
}
```

### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])

(aor/defagentmodule StreamingAgentModule
  [topology]
  (-> (aor/new-agent topology "StreamingAgent")
      (aor/node
       "process"
       nil
       (fn [agent-node num-chunks]
         ;; Stream chunks one at a time
         (doseq [i (range num-chunks)]
           (Thread/sleep 100) ; Simulate work
           (aor/stream-chunk! agent-node (str "chunk" i)))
         ;; Return final result
         (aor/result! agent-node "done")))))
```

### Key Points

- **streamChunk** can be called any number of times from a node
- Chunks can contain any serializable data
- Streaming happens in real-time as the agent executes

## Automatic Streaming Models

When you declare a `StreamingChatModel` from LangChain4j as an agent object, Agent-o-rama automatically captures the streaming tokens and forwards them as chunks. This means you get real-time streaming of LLM responses without any manual `streamChunk` calls.

### How It Works

1. **Declare the model as StreamingChatModel** in your topology
2. **Fetch it as ChatModel** in your node (Agent-o-rama wraps it automatically)
3. **Use it normally** – streaming happens automatically
4. **Clients receive tokens** in real-time as the model generates them

### Java API

```java
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;

public class StreamingLLMModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    // Declare API key as static agent object
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));

    // Declare as StreamingChatModel
    topology.declareAgentObjectBuilder("openai-model", setup -> {
      String apiKey = setup.getAgentObject("openai-api-key");
      return OpenAiStreamingChatModel.builder()
                                     .apiKey(apiKey)
                                     .modelName("gpt-4")
                                     .build();
    });

    topology.newAgent("StreamingLLMAgent")
            .node("chat", null, (AgentNode agentNode, String userMessage) -> {
              // Fetch as ChatModel (not StreamingChatModel!)
              ChatModel model = agentNode.getAgentObject("openai-model");

              // Use it normally – streaming happens automatically
              String response = model.chat(userMessage);

              // Return final complete response
              agentNode.result(response);
            });
  }
}
```

### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])
(import '[dev.langchain4j.model.openai OpenAiStreamingChatModel])

(aor/defagentmodule StreamingLLMModule
  [topology]
  ;; Declare API key as static agent object
  (aor/declare-agent-object topology "openai-api-key" (System/getenv "OPENAI_API_KEY"))

  ;; Declare as StreamingChatModel
  (aor/declare-agent-object-builder
   topology
   "openai-model"
   (fn [setup]
     (-> (OpenAiStreamingChatModel/builder)
         (.apiKey (aor/get-agent-object setup "openai-api-key"))
         (.modelName "gpt-4")
         .build)))

  (-> (aor/new-agent topology "StreamingLLMAgent")
      (aor/node
       "chat"
       nil
       (fn [agent-node user-message]
         ;; Fetch as ChatModel (not StreamingChatModel!)
         (let [model (aor/get-agent-object agent-node "openai-model")]
           ;; Use it normally - streaming happens automatically
           (let [response (aor/chat model user-message)]
             ;; Return final complete response
             (aor/result! agent-node response)))))))
```

## Consuming Streams from Agent Clients

Agent clients can subscribe to streaming data in two ways: `stream` for the first invocation of a node, or `streamAll` for all invocations of a node.

Use `stream` to subscribe to chunks from the first time a specific node is invoked during an agent execution. This is the most common case – if your node is only called once, or you only care about the first call, use `stream`.

The `stream` method can be called with or without a callback. The callback is invoked each time new chunks arrive.

Either way, the returned stream object can be queried at any time:
- **Java**: Call `.get()` to get the current list of chunks
- **Clojure**: Call `deref` or `@` to get the current list of chunks

The callback receives four arguments:
1. **allChunks**: Complete list of all chunks received so far from this node invocation
2. **newChunks**: List of only the newly received chunks since the last callback
3. **reset**: Boolean indicating the node failed and retried, resetting the chunks to empty when it restarted
4. **complete**: Boolean indicating the node execution has finished and there will be no more streaming chunks

#### Java API

```java
import com.rpl.agentorama.*;
import java.util.List;

public class StreamingConsumer {
  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch module and get agent client
      StreamingAgentModule module = new StreamingAgentModule();
      ipc.launchModule(module, new LaunchConfig(4, 2));

      AgentManager manager = AgentManager.create(ipc, module.getModuleName());
      AgentClient agent = manager.getAgentClient("StreamingAgent");

      // Start async agent execution
      AgentInvoke invoke = agent.initiate(5);

      // Option 1: Subscribe with callback
      AgentStream stream = agent.stream(
        invoke,
        "process",  // Node name to stream from
        (List allChunks, List newChunks, boolean reset, boolean complete) -> {
          for (Object chunk : newChunks) {
            System.out.println("New chunk: " + chunk);
          }
          if (reset) {
            System.out.println("Stream reset due to node retry");
          }
          if (complete) {
            System.out.println("Streaming complete!");
          }
        });

      // Option 2: Poll without callback
      AgentStream stream2 = agent.stream(invoke, "process");
      List currentChunks = stream2.get();  // Get current chunks at any time

      // Wait for final result
      String result = agent.result(invoke);
      System.out.println("Final result: " + result);
    }
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor]
         '[com.rpl.rama.test :as rtest])

(with-open [ipc (rtest/create-ipc)]
  ;; Launch module and get agent client
  (rtest/launch-module! ipc StreamingAgentModule {:tasks 4 :threads 2})
  (let [manager (aor/agent-manager ipc (rama/get-module-name StreamingAgentModule))
        agent (aor/agent-client manager "StreamingAgent")]

    ;; Start async agent execution
    (let [invoke (aor/agent-initiate agent 5)]

      ;; Option 1: Subscribe with callback
      (let [stream (aor/agent-stream
                    agent
                    invoke
                    "process"  ; Node name to stream from
                    (fn [all-chunks new-chunks reset? complete?]
                      (doseq [chunk new-chunks]
                        (println "New chunk:" chunk))
                      (when reset?
                        (println "Stream reset due to node retry"))
                      (when complete?
                        (println "Streaming complete!"))))]

        ;; Option 2: Poll without callback
        (let [stream2 (aor/agent-stream agent invoke "process")
              current-chunks @stream2]  ; Deref to get current chunks at any time
          (println "Current chunks:" current-chunks))

        ;; Wait for final result
        (let [result (aor/agent-result agent invoke)]
          (println "Final result:" result))))))
```

### Streaming from All Node Invocations

Use `streamAll` to subscribe to chunks from all invocations of a specific node. This is useful when a node is called multiple times (e.g. in parallel within an aggregation subgraph) and you want to track all of them.

Like `stream`, `streamAll` can be called with or without a callback. The key difference is that `streamAll` tracks multiple node invocations:

The returned stream object can be queried at any time:
- **Java**: Call `.get()` to get a map from node invoke ID to list of chunks
- **Clojure**: Call `deref` or `@` to get a map from node invoke ID to list of chunks

The callback receives four arguments, but with different types than `stream`:
1. **invokeIdToAllChunks**: Map from node invoke ID to complete list of all chunks for that invocation
2. **invokeIdToNewChunks**: Map from node invoke ID to newly received chunks for that invocation
3. **resetInvokeIds**: Set of node invoke IDs that were reset due to retry since the last callback
4. **complete**: Boolean indicating the agent has finished and thus all possible invokes of this node are complete

#### Java API

```java
import com.rpl.agentorama.*;
import java.util.*;

public class StreamAllConsumer {
  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("ParallelAgent");

      // Start agent execution that will invoke a node multiple times
      AgentInvoke invoke = agent.initiate(List.of(1, 2, 3, 4, 5));

      // Option 1: Subscribe with callback
      AgentStreamByInvoke stream = agent.streamAll(
        invoke,
        "process-item",
        (Map<UUID, List> invokeIdToAllChunks,
         Map<UUID, List> invokeIdToNewChunks,
         Set<UUID> resetInvokeIds,
         boolean complete) -> {
          // Process new chunks for each node invocation
          for (Map.Entry<UUID, List> entry : invokeIdToNewChunks.entrySet()) {
            UUID nodeInvokeId = entry.getKey();
            List newChunks = entry.getValue();

            for (Object chunk : newChunks) {
              System.out.printf("Node invoke %s: %s\n", nodeInvokeId, chunk);
            }
          }
          if (!resetInvokeIds.isEmpty()) {
            System.out.println("Some node invocations were reset: " + resetInvokeIds);
          }
          if (complete) {
            System.out.println("All node invocations complete!");
          }
        });

      // Option 2: Poll without callback
      AgentStreamByInvoke stream2 = agent.streamAll(invoke, "process-item");
      Map<UUID, List> currentChunks = stream2.get();  // Get map of all chunks at any time

      // Wait for final result
      Object result = agent.result(invoke);
      System.out.println("Final result: " + result);
    }
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor])

(let [agent (aor/agent-client manager "ParallelAgent")
      ;; Start agent execution that will invoke a node multiple times
      invoke (aor/agent-initiate agent [1 2 3 4 5])]

  ;; Option 1: Subscribe with callback
  (let [stream (aor/agent-stream-all
                agent
                invoke
                "process-item"
                (fn [invoke-id->all-chunks invoke-id->new-chunks reset-invoke-ids complete?]
                  ;; Process new chunks for each node invocation
                  (doseq [[node-invoke-id new-chunks] invoke-id->new-chunks]
                    (doseq [chunk new-chunks]
                      (println (format "Node invoke %s: %s" node-invoke-id chunk))))
                  (when (seq reset-invoke-ids)
                    (println "Some node invocations were reset:" reset-invoke-ids))
                  (when complete?
                    (println "All node invocations complete!"))))]

    ;; Option 2: Poll without callback
    (let [stream2 (aor/agent-stream-all agent invoke "process-item")
          current-chunks @stream2]  ; Deref to get map of all chunks at any time
      (println "Current chunks by invoke ID:" current-chunks))

    ;; Wait for final result
    (let [result (aor/agent-result agent invoke)]
      (println "Final result:" result))))
```

## Streaming Analytics

Agent-o-rama automatically tracks streaming performance metrics that are displayed in the web UI. These metrics help you understand the responsiveness of your agents.

### Time to First Token Metrics

Two key metrics are tracked for streaming:

#### 1. Time to First Token (Agent)

Measures the time from when the agent invocation starts until the first chunk is streamed to the client. This includes:
- Agent initialization time
- Time to reach the first `streamChunk` call
- Any processing before streaming begins

This metric tells you how quickly your agent starts providing feedback to users.

#### 2. Time to First Token (Model)

This measures the time from when the model call starts until the first token is received from the LLM. This is tracked automatically for all `StreamingChatModel` calls.

This metric tells you how quickly the underlying LLM starts generating responses.

### Viewing Streaming Analytics

Streaming metrics are available in the Agent-o-rama web UI on the analytics section for the agent. Here's an example of what it looks like:

![Streaming telemetry](images/telemetry3.png)

You can see how long until the agent produces the first token for any node, and you can see how long individual model calls take to produce their first token.


# Tools

Tools agents are specialized agents that execute LangChain4j tool functions with automatic parallel execution and result aggregation. They enable AI models to interact with external functions, APIs, and services through structured tool calling.

## Table of Contents

1. [What are Tools Agents?](#what-are-tools-agents)
2. [Creating Tools Agents](#creating-tools-agents)
3. [Invoking Tools Agents](#invoking-tools-agents)
4. [Parallel Tool Execution](#parallel-tool-execution)
5. [Error Handling](#error-handling)

## What are Tools Agents?

Tools agents are a special type of agent designed specifically for executing LangChain4j tools. When an AI model (like GPT-4) decides it needs to call one or more tools to answer a query, the tools agent:

1. **Receives tool execution requests** from the AI model
2. **Executes tools in parallel** if multiple tools are requested
3. **Aggregates results** automatically
4. **Returns structured responses** that can be sent back to the AI model

Tools agents are invoked as subagents from other agents.

## Creating Tools Agents

To create a tools agent, you define your tools with specifications and implementations, then use `newToolsAgent` / `new-tools-agent`.

### Defining Tools

Each tool needs:
1. **Tool specification**: Describes the tool's name, parameters, and purpose (for the AI model)
2. **Implementation function**: The actual code that executes when the tool is called

#### Java API

```java
import com.rpl.agentorama.*;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.JsonSchemaProperty;

public class ToolsModule extends AgentModule {
  // Define calculator tool specification
  static ToolSpecification calcSpec = ToolSpecification.builder()
      .name("calculator")
      .description("Performs basic arithmetic operations")
      .parameters(ToolParameters.builder()
          .required("operation", "a", "b")
          .properties(Map.of(
              "operation", JsonSchemaProperty.enums("add", "subtract", "multiply", "divide"),
              "a", JsonSchemaProperty.number("First number"),
              "b", JsonSchemaProperty.number("Second number")
          ))
          .build())
      .build();

  // Create tool info combining spec and implementation
  static ToolInfo calcTool = ToolInfo.create(calcSpec, (Map args) -> {
    String op = (String) args.get("operation");
    double a = ((Number) args.get("a")).doubleValue();
    double b = ((Number) args.get("b")).doubleValue();

    double result = switch (op) {
      case "add" -> a + b;
      case "subtract" -> a - b;
      case "multiply" -> a * b;
      case "divide" -> b != 0 ? a / b : Double.NaN;
      default -> Double.NaN;
    };

    return "" + result;
  });

  static List allTools = Arrays.asList(calcTool);


  @Override
  protected void defineAgents(AgentTopology topology) {
    // Create tools agent
    topology.newToolsAgent("ToolsAgent", allTools);
  }
}
```

#### Clojure API

```clojure
(require '[com.rpl.agent-o-rama :as aor]
         '[com.rpl.agent-o-rama.tools :as tools]
         '[com.rpl.agent-o-rama.langchain4j.json :as lj])

(defn calculate-tool
  "Calculator implementation function"
  [args]
  (let [operation (args "operation")
        a (args "a")
        b (args "b")
        result (case operation
                 "add" (+ a b)
                 "subtract" (- a b)
                 "multiply" (* a b)
                 "divide" (if (zero? b) "Error: Division by zero" (/ a b))
                 "Error: Unknown operation")]
    (str result)))

;; Define tool specification
(def CALCULATOR-TOOL
  (tools/tool-info
   (tools/tool-specification
    "calculator"
    (lj/object
     {:description "Parameters for calculator operations"
      :required ["operation" "a" "b"]}
     {"operation" (lj/enum "The arithmetic operation to perform"
                           ["add" "subtract" "multiply" "divide"])
      "a" (lj/number "The first number")
      "b" (lj/number "The second number")})
    "Performs basic arithmetic operations on two numbers")
   calculate-tool))

(def TOOLS [CALCULATOR-TOOL])

(aor/defagentmodule ToolsModule
  [topology]
  ;; Create tools agent
  (tools/new-tools-agent
   topology
   "ToolsAgent"
   TOOLS))
```

### Multiple Tools

You can define multiple tools in a single tools agent. Each tool is independent and can be called by the AI model as needed.

#### Java API

```java
// Define multiple tools
ToolInfo calcTool = ToolInfo.create(calcSpec, calcImpl);
ToolInfo stringTool = ToolInfo.create(stringSpec, stringImpl);
ToolInfo weatherTool = ToolInfo.create(weatherSpec, weatherImpl);
List allTools = Arrays.asList(calcTool, stringTool, weatherTool);

// Create tools agent with all tools
topology.newToolsAgent("ToolsAgent", allTools);
```

#### Clojure API

```clojure
(def TOOLS [CALCULATOR-TOOL STRING-TOOL WEATHER-TOOL])

;; Create tools agent with multiple tools
(tools/new-tools-agent
 topology
 "ToolsAgent"
 TOOLS)
```

## Invoking Tools Agents

Tools agents are invoked as subagents from other agents. The typical pattern is:

1. **Main agent** receives user input and calls AI model with tools registered in the request
2. **AI model** processes input and decides which tools to call
3. **Main agent** sends tool execution request from the AI model to the tools agent
4. **Tools agent** executes the requested tools
5. **Results** are sent back to the AI model for the next response, possibly looping for additional tool calls

### Basic Invocation Pattern

#### Java API

```java
public class MyAgentModule extends AgentModule {
  @Override
  protected void defineAgents(AgentTopology topology) {
    // ... declare tools agent ...

    topology.newAgent("Coordinator")
            .node("process", null, (AgentNode agentNode, String userQuery) -> {
              // Get AI model
              ChatModel model = agentNode.getAgentObject("openai-model");

              // Send query to AI model with tool specifications
              ChatResponse response = model.chat(
                  ChatRequest.builder()
                      .messages(List.of(new UserMessage(userQuery)))
                      .toolSpecifications(calcSpec, stringSpec)
                      .build());

              // Check if AI wants to call tools
              List<ToolExecutionRequest> toolCalls =
                  response.aiMessage().toolExecutionRequests();

              if (!toolCalls.isEmpty()) {
                // Get tools agent client
                AgentClient toolsAgent = agentNode.getAgentClient("ToolsAgent");

                // Execute tools (they run in parallel automatically)
                List<ToolExecutionResultMessage> results =
                    (List<ToolExecutionResultMessage>) toolsAgent.invoke(toolCalls);

                // Build message list with original query, AI response, and tool results
                List messages = new ArrayList();
                messages.add(new UserMessage(userQuery));
                messages.add(response.aiMessage());
                messages.addAll(results);

                // Send results back to AI model for final response
                ChatResponse finalResponse = model.chat(
                    ChatRequest.builder()
                        .messages(messages)
                        .build());

                agentNode.result(finalResponse.aiMessage().text());
              } else {
                // AI didn't need tools, return direct response
                agentNode.result(response.aiMessage().text());
              }
            });
  }
}
```

#### Clojure API

```clojure
(aor/defagentmodule MyAgentModule
  [topology]
  ;; ... declare tools agent ...

  (-> topology
      (aor/new-agent "Coordinator")
      (aor/node
       "process"
       nil
       (fn [agent-node user-query]
         ;; Get AI model
         (let [model (aor/get-agent-object agent-node "openai-model")

               ;; Send query to AI model with tool specifications
               response (lc4j/chat model
                                   (lc4j/chat-request
                                    [(UserMessage. user-query)]
                                    ;; same argument here as provided to the tools agent
                                    {:tools TOOLS}))
               ai-message (.aiMessage response)
               tool-calls (vec (.toolExecutionRequests ai-message))]

           (if (seq tool-calls)
             ;; AI wants to call tools
             (let [tools-agent (aor/agent-client agent-node "ToolsAgent")

                   ;; Execute tools (they run in parallel automatically)
                   results (aor/agent-invoke tools-agent tool-calls)

                   ;; Send results back to AI model for final response
                   final-response (lc4j/chat model
                                             (lc4j/chat-request
                                              (concat
                                              [(UserMessage. user-query) ai-message]
                                               results)))]
               (aor/result! agent-node (.text (.aiMessage final-response))))

             ;; AI didn't need tools, return direct response
             (aor/result! agent-node (.text ai-message))))))))
```

## Error Handling

Tools agents support custom error handling for tool execution failures. You can configure how tool errors should be handled, such as returning a static error message to the AI model, re-throwing the error back to the invoking agent, or providing different responses based on exception type.

By default, if a tool throws an exception, a formatted error message is returned as the tool result.

### Error Handler Options

Agent-o-rama provides several built-in error handlers:

1. **Default Handler**: Formats exceptions as user-friendly error messages (used if no handler specified)
2. **Static String**: Returns a fixed string for any error
3. **Rethrow**: Re-throws the exception to the calling agent
4. **Static String by Type**: Returns different strings based on exception type
5. **Function by Type**: Applies different handler functions based on exception type


### Static String Handler

Returns a fixed error message for any tool execution failure. Useful for providing a simple, user-friendly message to the AI model.

#### Java API

```java
// Return static string for all errors
topology.newToolsAgent(
    "ToolsAgent",
    allTools,
    ToolsAgentOptions.errorHandlerStaticString("Tool execution failed. Please try again."));
```

#### Clojure API

```clojure
(tools/new-tools-agent
 topology
 "ToolsAgent"
 TOOLS
 {:error-handler (tools/error-handler-static-string
                  "Tool execution failed. Please try again.")})
```

### Rethrow Handler

Re-throws exceptions, allowing the agent to handle the error in its own logic.

#### Java API

```java
// Rethrow errors to calling agent
topology.newToolsAgent(
    "ToolsAgent",
    allTools,
    ToolsAgentOptions.errorHandlerRethrow());
```

#### Clojure API

```clojure
(tools/new-tools-agent
 topology
 "ToolsAgent"
 TOOLS
 {:error-handler (tools/error-handler-rethrow)})
```

### Static String by Type Handler

Returns different static strings based on exception type. If the exception type doesn't match any handler, it is re-thrown.

#### Java API

```java
import com.rpl.agentorama.ToolsAgentOptions.StaticStringHandler;

// Different messages for different exception types
topology.newToolsAgent(
    "ToolsAgent",
    allTools,
    ToolsAgentOptions.errorHandlerStaticStringByType(
        StaticStringHandler.create(ArithmeticException.class, "Math error occurred"),
        StaticStringHandler.create(IllegalArgumentException.class, "Invalid input provided"),
        StaticStringHandler.create(ClassCastException.class, "Type conversion failed")
    ));
```

#### Clojure API

```clojure
(tools/new-tools-agent
 topology
 "ToolsAgent"
 TOOLS
 {:error-handler (tools/error-handler-static-string-by-type
                  [[ArithmeticException "Math error occurred"]
                   [IllegalArgumentException "Invalid input provided"]
                   [ClassCastException "Type conversion failed"]])})
```

### Function by Type Handler

Applies different handler functions based on exception type. Each handler receives the exception and returns a string. If the exception type doesn't match any handler, it is re-thrown.

#### Java API

```java
import com.rpl.agentorama.ToolsAgentOptions.FunctionHandler;

// Custom logic for different exception types
topology.newToolsAgent(
    "ToolsAgent",
    allTools,
    ToolsAgentOptions.errorHandlerByType(
        FunctionHandler.create(
            ArithmeticException.class,
            (ArithmeticException e) -> "Math error: " + e.getMessage()),
        FunctionHandler.create(
            IllegalArgumentException.class,
            (IllegalArgumentException e) -> "Invalid: " + e.getMessage())
    ));
```

#### Clojure API

```clojure
(tools/new-tools-agent
 topology
 "ToolsAgent"
 TOOLS
 {:error-handler (tools/error-handler-by-type
                  [[ArithmeticException (fn [e] (str "Math error: " (.getMessage e)))]
                   [IllegalArgumentException (fn [e] (str "Invalid: " (.getMessage e)))]])})
```


# Agent-O-Rama Java Examples

This document contains all Java example source code from the Agent-O-Rama repository.

## Getting Started with Examples

This directory contains example implementations of AI agents using Agent-o-rama.
Here's an example of running an agent from this directory:
```
./run-example com.rpl.agent.react.ReActExample
```
This will prompt for an input to the agent and then print the result. It also opens the UI at http://localhost:1974, which will remain open for viewing traces, performing more invokes, and other exploration until you press enter in the terminal.

## Maven Project Configuration (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.rpl.agent</groupId>
  <artifactId>java-examples</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>
  <name>Agent Examples</name>
  <description>Java implementations of agents using Agent-o-rama framework</description>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <repositories>
    <repository>
      <id>maven-releases</id>
      <url>https://nexus.redplanetlabs.com/repository/maven-public-releases</url>
    </repository>
  </repositories>
  <dependencies>
    <dependency>
      <groupId>com.rpl</groupId>
      <artifactId>agent-o-rama</artifactId>
      <version>0.7.0</version>
    </dependency>
    <dependency>
      <groupId>com.rpl</groupId>
      <artifactId>rama</artifactId>
      <version>1.2.0</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-open-ai</artifactId>
      <version>1.4.0</version>
    </dependency>
    <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-web-search-engine-tavily</artifactId>
      <version>1.4.0-beta10</version>
    </dependency>
  </dependencies>
</project>
```

---

## Table of Contents

### Agent Source Files

- [AgentObjectsAgent](#agentobjectsagent)
- [AggregationAgent](#aggregationagent)
- [AsyncAgent](#asyncagent)
- [BasicAgent](#basicagent)
- [DocumentStoreAgent](#documentstoreagent)
- [HumanInputAgent](#humaninputagent)
- [KeyValueStoreAgent](#keyvaluestoreagent)
- [LangChain4jAgent](#langchain4jagent)
- [MirrorAgent](#mirroragent)
- [MultiAggAgent](#multiaggagent)
- [MultiNodeAgent](#multinodeagent)
- [PStateStoreAgent](#pstatestoreagent)
- [RamaModuleAgent](#ramamoduleagent)
- [RecordOpAgent](#recordopagent)
- [RouterAgent](#routeragent)
- [StreamingAgent](#streamingagent)
- [StreamingLangchain4jAgent](#streaminglangchain4jagent)
- [StructuredLangchain4jAgent](#structuredlangchain4jagent)
- [ReActExample](#reactexample)
- [ReActModule](#reactmodule)
- [ToolsFactory](#toolsfactory)
- [ResearchAgentExample](#researchagentexample)
- [ResearchAgentModule](#researchagentmodule)

### Test Files

- [AgentObjectsAgentTest](#agentobjectsagenttest-test)
- [AggregationAgentTest](#aggregationagenttest-test)
- [AsyncAgentTest](#asyncagenttest-test)
- [BasicAgentTest](#basicagenttest-test)
- [DocumentStoreAgentTest](#documentstoreagenttest-test)
- [HumanInputAgentTest](#humaninputagenttest-test)
- [KeyValueStoreAgentTest](#keyvaluestoreagenttest-test)
- [LangChain4jAgentTest](#langchain4jagenttest-test)
- [MirrorAgentTest](#mirroragenttest-test)
- [MultiAggAgentTest](#multiaggagenttest-test)
- [MultiNodeAgentTest](#multinodeagenttest-test)
- [PStateStoreAgentTest](#pstatestoreagenttest-test)
- [RamaModuleAgentTest](#ramamoduleagenttest-test)
- [RecordOpAgentTest](#recordopagenttest-test)
- [RouterAgentTest](#routeragenttest-test)
- [StreamingAgentTest](#streamingagenttest-test)
- [StreamingLangchain4jAgentTest](#streaminglangchain4jagenttest-test)
- [StructuredLangchain4jAgentTest](#structuredlangchain4jagenttest-test)

---

## Agent Source Files

### AgentObjectsAgent

**File:** `src/main/java/com/rpl/agent/basic/AgentObjectsAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

/**
 * Java example demonstrating agent objects for sharing resources across agent nodes.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>declareAgentObject: Static shared objects
 *   <li>declareAgentObjectBuilder: Dynamic object creation with setup context
 *   <li>getAgentObject: Access shared objects from agent nodes
 *   <li>Thread-unsafe objects: Safely using non-thread-safe objects via pooling
 *   <li>Object sharing across multiple nodes and invocations
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class AgentObjectsAgent {

  /**
   * Thread-unsafe service using volatile for fast, non-thread-safe state.
   *
   * <p>This service demonstrates how non-thread-safe objects can be safely used in agent-o-rama
   * through object pooling.
   */
  public static class MessageService {
    private final String version;
    private int counter;

    public MessageService(String version) {
      this.version = version;
      this.counter = 0;
    }

    public void resetForNewInvocation() {
      this.counter = 0;
    }

    public String useService(String input, String sendTo) {
      this.counter++;
      return String.format("v%s: %s (#%d -> %s)", version, input, counter, sendTo);
    }

    @Override
    public String toString() {
      return String.format("MessageService[version=%s, counter=%d]", version, counter);
    }
  }

  /** Agent Module demonstrating agent objects. */
  public static class AgentObjectsModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Static agent objects - simple values
      topology.declareAgentObject("app-version", "1.2.3");
      topology.declareAgentObject("send-to", "alerts");

      // Dynamic agent object builder - service that uses version and object name
      topology.declareAgentObjectBuilder(
          "message-service",
          setup -> {
            String version = (String) setup.getAgentObject("app-version");
            String objectName = setup.getObjectName();
            System.out.println("Building object: " + objectName + " with version: " + version);
            return new MessageService(version);
          });

      topology.newAgent("AgentObjectsAgent").node("use-service", null, (AgentNode agentNode, String input) -> {
        MessageService service = (MessageService) agentNode.getAgentObject("message-service");
        String sendTo = (String) agentNode.getAgentObject("send-to");

        // Reset the thread-unsafe service for this new invocation
        service.resetForNewInvocation();

        // Use the thread-unsafe service (safe due to pooling)
        String result = service.useService(input, sendTo);

        agentNode.result(result);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Agent Objects Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      AgentObjectsModule module = new AgentObjectsModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AgentObjectsAgent");

      System.out.println("Agent Objects Example:");

      // Multiple concurrent invocations to show shared state
      System.out.println("\n--- Initiating concurrent invocations ---");
      AgentInvoke invoke1 = agent.initiate("Hello");
      AgentInvoke invoke2 = agent.initiate("World");
      AgentInvoke invoke3 = agent.initiate("Again");

      System.out.println("Getting results...");
      String result1 = (String) agent.result(invoke1);
      String result2 = (String) agent.result(invoke2);
      String result3 = (String) agent.result(invoke3);

      System.out.println("Result 1: " + result1);
      System.out.println("Result 2: " + result2);
      System.out.println("Result 3: " + result3);

      System.out.println("\nEach message includes version and send-to from static objects");
      System.out.println("and the counter is always #1 -> alerts as the service is reset");
      System.out.println("at the start of each invocation,");
      System.out.println("and message-service instances are not shared.");
    }
  }
}

```

---

### AggregationAgent

**File:** `src/main/java/com/rpl/agent/basic/AggregationAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.BuiltIn;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java example demonstrating fan-out/fan-in aggregation patterns with aggStartNode and aggNode.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>aggStartNode: Start aggregation by emitting to multiple targets
 *   <li>aggNode: Collect and combine results from multiple executions
 *   <li>Fan-out/fan-in execution patterns
 *   <li>Built-in aggregators for common operations
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class AggregationAgent {

  /** Agent Module demonstrating aggregation functionality. */
  public static class AggregationModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology
          .newAgent("AggregationAgent")
          // Start aggregation by distributing work to parallel processors
          .aggStartNode("distribute-work", "process-chunk", (AgentNode agentNode, Map<String, Object> request) -> {
            List<Integer> data = (List<Integer>) request.get("data");
            int chunkSize = (Integer) request.get("chunkSize");

            // Create chunks from the data
            List<List<Integer>> chunks = new ArrayList<>();
            for (int i = 0; i < data.size(); i += chunkSize) {
              int end = Math.min(i + chunkSize, data.size());
              chunks.add(new ArrayList<>(data.subList(i, end)));
            }

            // Emit each chunk for parallel processing
            for (List<Integer> chunk : chunks) {
              agentNode.emit("process-chunk", chunk);
            }

            return null; // aggStartNode doesn't need to return meaningful data
          })
          // Process individual chunks in parallel
          .node("process-chunk", "collect-results", (AgentNode agentNode, List<Integer> chunk) -> {
            // Transform the chunk data (square each value)
            List<Integer> processedChunk = new ArrayList<>();
            int chunkSum = 0;
            for (Integer value : chunk) {
              int squared = value * value;
              processedChunk.add(squared);
              chunkSum += squared;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("originalChunk", chunk);
            result.put("processedChunk", processedChunk);
            result.put("chunkSum", chunkSum);

            agentNode.emit("collect-results", result);
          })
          // Aggregate all results using built-in vector aggregator
          .aggNode("collect-results", null, BuiltIn.LIST_AGG, (AgentNode agentNode, List<Map<String, Object>> aggregatedResults, Object startNodeResult) -> {
            // Sort chunks by their first element to ensure consistent order
            List<Map<String, Object>> sortedResults = new ArrayList<>(aggregatedResults);
            sortedResults.sort(
                Comparator.comparing(result -> ((List<Integer>) result.get("originalChunk")).get(0)));

            int totalSum = 0;
            int totalItems = 0;
            for (Map<String, Object> result : sortedResults) {
              totalSum += (Integer) result.get("chunkSum");
              totalItems += ((List<Integer>) result.get("originalChunk")).size();
            }

            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("totalItems", totalItems);
            finalResult.put("totalSum", totalSum);
            finalResult.put("chunksProcessed", sortedResults.size());
            finalResult.put("chunkResults", sortedResults);

            agentNode.result(finalResult);
          });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Aggregation Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      AggregationModule module = new AggregationModule();
      ipc.launchModule(module, new LaunchConfig(2, 2));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AggregationAgent");

      System.out.println("Aggregation Agent Example:");
      System.out.println("Processing data in parallel chunks with result aggregation");

      // Process data with different chunk sizes
      List<Integer> testData = new ArrayList<>();
      for (int i = 1; i <= 20; i++) {
        testData.add(i); // [1, 2, 3, ..., 20]
      }

      System.out.println("\n--- Processing with chunk size 5 ---");
      Map<String, Object> request1 = new HashMap<>();
      request1.put("data", testData);
      request1.put("chunkSize", 5);

      Map<String, Object> result1 = (Map<String, Object>) agent.invoke(request1);
      System.out.println("Result 1:");
      System.out.println("  Total items: " + result1.get("totalItems"));
      System.out.println("  Total sum: " + result1.get("totalSum"));
      System.out.println("  Chunks processed: " + result1.get("chunksProcessed"));

      System.out.println("\n--- Processing with chunk size 3 ---");
      Map<String, Object> request2 = new HashMap<>();
      request2.put("data", testData);
      request2.put("chunkSize", 3);

      Map<String, Object> result2 = (Map<String, Object>) agent.invoke(request2);
      System.out.println("Result 2:");
      System.out.println("  Total items: " + result2.get("totalItems"));
      System.out.println("  Total sum: " + result2.get("totalSum"));
      System.out.println("  Chunks processed: " + result2.get("chunksProcessed"));

      System.out.println("\nNotice how:");
      System.out.println("- Work is distributed in parallel to multiple nodes");
      System.out.println("- Results are automatically aggregated back together");
      System.out.println("- Different chunk sizes create different parallelization");
      System.out.println("- Built-in aggregators simplify result collection");
    }
  }
}

```

---

### AsyncAgent

**File:** `src/main/java/com/rpl/agent/basic/AsyncAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

/**
 * Java example demonstrating asynchronous agent initiation and result handling.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>agent.initiate: Start agent execution asynchronously
 *   <li>agent.result: Get result from async execution
 *   <li>AgentInvoke handle for tracking execution
 *   <li>Concurrent agent execution patterns
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class AsyncAgent {

  /** Agent Module that simulates processing time in a single node. */
  public static class AsyncAgentModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("AsyncAgent").node("process", null, (AgentNode agentNode, String taskName) -> {
        System.out.printf("Starting task '%s'%n", taskName);

        // Simulate work
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }

        System.out.printf("Completed task '%s'%n", taskName);

        agentNode.result(String.format("Task '%s' completed successfully", taskName));
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Async Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      AsyncAgentModule module = new AsyncAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AsyncAgent");

      System.out.println("Async Agent Example - Starting multiple concurrent tasks");

      // Start multiple async executions of the agent
      AgentInvoke task1Invoke = agent.initiate("Data Processing");
      AgentInvoke task2Invoke = agent.initiate("Report Generation");
      AgentInvoke task3Invoke = agent.initiate("Email Sending");

      System.out.println("All tasks initiated, waiting for completion...");

      // Get results, waiting for each one to complete
      System.out.println("\n--- Results ---");
      System.out.println("Task 3 result: " + agent.result(task3Invoke));
      System.out.println("Task 2 result: " + agent.result(task2Invoke));
      System.out.println("Task 1 result: " + agent.result(task1Invoke));

      System.out.println("\nAll tasks completed!");
    }
  }
}

```

---

### BasicAgent

**File:** `src/main/java/com/rpl/agent/basic/BasicAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

/**
 * Java example demonstrating basic agent definition with nested classes.
 *
 * <p>This example demonstrates:
 *
 * <ul>
 *   <li>Agent module definition as nested class extending AgentModule
 *   <li>Node function implementation as nested class
 *   <li>Single-node agent topology
 *   <li>Querying available agent names
 *   <li>Agent invocation and result handling
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class BasicAgent {

  /**
   * Basic Agent Module demonstrating fundamental agent-o-rama concepts.
   *
   * <p>This nested module implements a simple agent with a single node that processes input and
   * returns a result.
   */
  public static class BasicModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("BasicAgent").node("process", null, (AgentNode agentNode, String userName) -> {
        // Extract user name from arguments (corresponds to the value in agent-invoke)

        // Create a welcome message for the user
        String result = "Welcome to agent-o-rama, " + userName + "!";

        // Return the final result
        agentNode.result(result);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Basic Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      BasicModule module = new BasicModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);

      // List all available agents in this module
      System.out.println("Available agents: " + manager.getAgentNames());

      // Get client for our specific agent
      AgentClient agent = manager.getAgentClient("BasicAgent");

      // Invoke agent synchronously with sample user names
      System.out.println("\nBasic Agent Results:");
      System.out.println("User: \"Alice\" -> Result: " + agent.invoke("Alice"));
      System.out.println("User: \"Bob\" -> Result: " + agent.invoke("Bob"));
    }
  }
}

```

---

### DocumentStoreAgent

**File:** `src/main/java/com/rpl/agent/basic/DocumentStoreAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.store.DocumentStore;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * Java example demonstrating document store operations for structured multi-field data.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>declareDocumentStore: Create a document store with multiple fields
 *   <li>getStore: Access document stores from agent nodes
 *   <li>Store.getDocumentField: Retrieve specific field values
 *   <li>Store.putDocumentField: Store values in specific fields
 *   <li>Store.updateDocumentField: Update specific field values
 *   <li>Store.containsDocumentField: Check if fields exist
 *   <li>Structured document storage with multiple typed fields
 * </ul>
 *
 * <p>Uses HashMap for request and response data structures with keys:
 *
 * <ul>
 *   <li>Request: "userId" (String), "profileUpdates" (Map with "name", "age", "preferences")
 *   <li>Response: "userId" (String), "name" (String), "age" (Long), "preferences" (Map)
 * </ul>
 */
public class DocumentStoreAgent {

  /** Agent Module demonstrating document store usage. */
  public static class DocumentStoreModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare document store for user profiles
      // Key: String (user-id), Fields: name (String), age (Long), preferences (Object)
      topology.declareDocumentStore(
          "$$user-profiles",
          String.class,
          "name",
          String.class,
          "age",
          Long.class,
          "preferences",
          Object.class);

      topology
          .newAgent("DocumentStoreAgent")
          .node("update-profile", "read-profile", (AgentNode agentNode, Map<String, Object> request) -> {
            DocumentStore<String> profilesStore = agentNode.getStore("$$user-profiles");
            String userId = (String) request.get("userId");
            Map<String, Object> profileUpdates = (Map<String, Object>) request.get("profileUpdates");

            // Update individual profile fields
            if (profileUpdates.containsKey("name")) {
              profilesStore.putDocumentField(userId, "name", profileUpdates.get("name"));
            }

            if (profileUpdates.containsKey("age")) {
              profilesStore.putDocumentField(userId, "age", profileUpdates.get("age"));
            }

            if (profileUpdates.containsKey("preferences")) {
              // Demonstrate field update with function
              profilesStore.updateDocumentField(
                  userId,
                  "preferences",
                  existing -> {
                    Map<String, Object> existingPrefs =
                        existing != null ? (Map<String, Object>) existing : new HashMap<>();
                    Map<String, Object> newPrefs =
                        (Map<String, Object>) profileUpdates.get("preferences");
                    Map<String, Object> merged = new HashMap<>(existingPrefs);
                    merged.putAll(newPrefs);
                    return merged;
                  });
            }

            // Emit to next node for reading
            agentNode.emit("read-profile", userId);
          })
          .node("read-profile", null, (AgentNode agentNode, String userId) -> {
            DocumentStore<String> profilesStore = agentNode.getStore("$$user-profiles");

            // Check which fields exist
            boolean hasName = profilesStore.containsDocumentField(userId, "name");
            boolean hasAge = profilesStore.containsDocumentField(userId, "age");
            boolean hasPrefs = profilesStore.containsDocumentField(userId, "preferences");

            // Retrieve all profile fields
            String name = hasName ? (String) profilesStore.getDocumentField(userId, "name") : null;
            Long age = hasAge ? (Long) profilesStore.getDocumentField(userId, "age") : null;
            Map<String, Object> preferences =
                hasPrefs
                    ? (Map<String, Object>) profilesStore.getDocumentField(userId, "preferences")
                    : null;

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("name", name);
            result.put("age", age);
            result.put("preferences", preferences);

            agentNode.result(result);
          });
    }
  }


  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      DocumentStoreModule module = new DocumentStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("DocumentStoreAgent");

      System.out.println("Document Store Agent Example:");

      // First invocation: Create user profile
      System.out.println("\n--- Creating user profile ---");
      Map<String, Object> request1 = new HashMap<>();
      request1.put("userId", "user123");
      Map<String, Object> updates1 = new HashMap<>();
      updates1.put("name", "Alice Smith");
      updates1.put("age", 28L);
      Map<String, Object> prefs1 = new HashMap<>();
      prefs1.put("theme", "dark");
      prefs1.put("newsletter", true);
      updates1.put("preferences", prefs1);
      request1.put("profileUpdates", updates1);

      Map<String, Object> result1 = (Map<String, Object>) agent.invoke(request1);
      System.out.println("Profile created:");
      System.out.println("  Name: " + result1.get("name"));
      System.out.println("  Age: " + result1.get("age"));
      System.out.println("  Preferences: " + result1.get("preferences"));

      // Second invocation: Update specific fields
      System.out.println("\n--- Updating age and preferences ---");
      Map<String, Object> request2 = new HashMap<>();
      request2.put("userId", "user123");
      Map<String, Object> updates2 = new HashMap<>();
      updates2.put("age", 29L);
      Map<String, Object> prefs2 = new HashMap<>();
      prefs2.put("notifications", true);
      updates2.put("preferences", prefs2);
      request2.put("profileUpdates", updates2);

      Map<String, Object> result2 = (Map<String, Object>) agent.invoke(request2);
      System.out.println("Profile updated:");
      System.out.println("  Name: " + result2.get("name"));
      System.out.println("  Age: " + result2.get("age"));
      System.out.println("  Preferences: " + result2.get("preferences"));

      // Third invocation: Different user
      System.out.println("\n--- Creating second user ---");
      Map<String, Object> request3 = new HashMap<>();
      request3.put("userId", "user456");
      Map<String, Object> updates3 = new HashMap<>();
      updates3.put("name", "Bob Jones");
      updates3.put("age", 35L);
      Map<String, Object> prefs3 = new HashMap<>();
      prefs3.put("theme", "light");
      updates3.put("preferences", prefs3);
      request3.put("profileUpdates", updates3);

      Map<String, Object> result3 = (Map<String, Object>) agent.invoke(request3);
      System.out.println("Second profile created:");
      System.out.println("  Name: " + result3.get("name"));
      System.out.println("  Age: " + result3.get("age"));
      System.out.println("  Preferences: " + result3.get("preferences"));

      System.out.println("\nNotice how:");
      System.out.println("- Document fields can be updated independently");
      System.out.println("- Field updates persist across invocations");
      System.out.println("- Complex field merging is supported with updateDocumentField");
      System.out.println("- containsDocumentField checks field existence before retrieval");
      System.out.println("- Multiple users are stored in the same document store");
    }
  }
}

```

---

### HumanInputAgent

**File:** `src/main/java/com/rpl/agent/basic/HumanInputAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentStep;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.HumanInputRequest;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Java example demonstrating human input requests and handling within agent nodes.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>getHumanInput: Request input from human users
 *   <li>agent.nextStep: Handle human input requests in execution flow
 *   <li>provideHumanInput: Supply responses to human input requests
 *   <li>pendingHumanInputs: List all pending human input requests
 *   <li>isAgentInvokeComplete: Check if an agent invocation has completed
 *   <li>Human-in-the-loop agent execution patterns
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class HumanInputAgent {

  /** Agent Module demonstrating human input integration with AI models. */
  public static class HumanInputModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare OpenAI model builder
      topology.declareAgentObjectBuilder(
          "openai",
          setup -> {
            String apiKey = System.getenv("OPENAI_API_KEY");
            return OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-4o-mini").build();
          });

      topology.newAgent("HumanInputAgent").node("chat", null, (AgentNode agentNode, String userMessage) -> {
        // NOTE you can not use OpenAiChatModel as the type here
        ChatModel openai = (ChatModel) agentNode.getAgentObject("openai");

        // Get AI response
        String response = openai.chat(userMessage);

        // Ask human if response was helpful
        boolean helpful = isHumanHelpful(agentNode, response);

        // Return result as HashMap
        // Expected structure: {"response": String, "helpful": boolean}
        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("helpful", helpful);
        agentNode.result(result);
      });
    }
  }

  /** Ask user if the response was helpful and loop until valid y/n answer. */
  private static boolean isHumanHelpful(AgentNode agentNode, String response) {
    while (true) {
      String input =
          agentNode.getHumanInput(
              String.format("AI Response: %s%n%nWas this response helpful? (y/n): ", response));

      if ("y".equals(input)) {
        return true;
      } else if ("n".equals(input)) {
        return false;
      } else {
        // Loop again with clarification
        input = agentNode.getHumanInput("Please answer 'y' or 'n'.");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null) {
      apiKey = System.getProperty("OPENAI_API_KEY");
    }
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("Human Input Agent Example:");
      System.out.println("OPENAI_API_KEY environment variable not set.");
      System.out.println("Please set your OpenAI API key to run this example:");
      System.out.println("  export OPENAI_API_KEY=your-api-key-here");
      return;
    }

    System.out.println("Starting Human Input Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create();
        Scanner scanner = new Scanner(System.in)) {

      // Launch the agent module
      HumanInputModule module = new HumanInputModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("HumanInputAgent");

      System.out.print("Enter your message: ");
      String userMessage = scanner.nextLine();

      System.out.println();
      System.out.println(userMessage);

      AgentInvoke invoke = agent.initiate(userMessage);

      System.out.println(
          String.format("\nAgent invoke complete? %s", agent.isAgentInvokeComplete(invoke)));

      // Handle execution steps including human input requests
      AgentStep step = agent.nextStep(invoke);
      while (step instanceof HumanInputRequest) {
        HumanInputRequest humanInput = (HumanInputRequest) step;

        // Check for multiple pending human inputs
        List<HumanInputRequest> pending = agent.pendingHumanInputs(invoke);
        if (pending.size() > 1) {
          System.out.println(String.format("\n[%d pending human input requests]", pending.size()));
        }

        System.out.println(humanInput.getPrompt());
        System.out.print(">> ");
        String response = scanner.nextLine();

        agent.provideHumanInput(humanInput, response);
        System.out.println();

        step = agent.nextStep(invoke);
      }

      // Get final result as HashMap
      Map<String, Object> result = (Map<String, Object>) agent.result(invoke);
      System.out.println("Final result:");
      System.out.println("Response: " + result.get("response"));
      System.out.println("Helpful: " + result.get("helpful"));

      System.out.println(
          String.format("\nAgent invoke complete? %s", agent.isAgentInvokeComplete(invoke)));

      System.out.println("\nNotice how:");
      System.out.println("- Agents can request human input during execution");
      System.out.println(
          "- instanceof HumanInputRequest checks if a step is a human input request");
      System.out.println("- isAgentInvokeComplete checks if an agent invocation has completed");
      System.out.println("- pendingHumanInputs lists all pending requests");
      System.out.println("- Input validation and defaults are handled gracefully");
      System.out.println("- Human responses influence the final result");
    }
  }
}

```

---

### KeyValueStoreAgent

**File:** `src/main/java/com/rpl/agent/basic/KeyValueStoreAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.store.KeyValueStore;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Java example demonstrating key-value store operations for persistent agent state.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>declareKeyValueStore: Create a key-value store
 *   <li>getStore: Access stores from agent nodes
 *   <li>Store.get: Retrieve values from store
 *   <li>Store.put: Store values in store
 *   <li>Store.update: Update existing values in store
 *   <li>Persistent state across agent invocations
 * </ul>
 *
 * <p>Uses HashMap for request and response data structures with keys:
 *
 * <ul>
 *   <li>Request: "operation" (String), "counterName" (String), "value" (Long)
 *   <li>Response: "action" (String), "counter" (String), "value" (Long), "previousValue" (Long),
 *       "newValue" (Long), "addedValue" (Long), "timestamp" (Long)
 * </ul>
 */
public class KeyValueStoreAgent {

  /** Available counter operations. */
  public enum Operation {
    GET,
    INCREMENT,
    SET,
    UPDATE
  }

  /** Helper method to create a counter request HashMap. */
  public static Map<String, Object> createCounterRequest(
      String counterName, Operation operation, Long value) {
    Map<String, Object> request = new HashMap<>();
    request.put("counterName", counterName);
    request.put("operation", operation.toString());
    if (value != null) {
      request.put("value", value);
    }
    return request;
  }

  /** Helper method to create a counter response HashMap with common fields. */
  public static Map<String, Object> createCounterResponse(String action, String counterName) {
    Map<String, Object> response = new HashMap<>();
    response.put("action", action);
    response.put("counter", counterName);
    response.put("timestamp", Instant.now().toEpochMilli());
    return response;
  }

  /** Agent Module demonstrating key-value store usage. */
  public static class KeyValueStoreModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare a key-value store for counters (String -> Long)
      topology.declareKeyValueStore("$$counters", String.class, Long.class);

      topology
          .newAgent("KeyValueStoreAgent")
          .node("manage-counter", null, (AgentNode agentNode, Map<String, Object> request) -> {
            KeyValueStore<String, Long> countersStore = agentNode.getStore("$$counters");
            String counterName = (String) request.get("counterName");
            String operationStr = (String) request.get("operation");
            Operation operation = Operation.valueOf(operationStr);
            Long value = (Long) request.get("value");

            Map<String, Object> result;

            switch (operation) {
              case GET:
                Long currentValue = countersStore.get(counterName);
                result = createCounterResponse("get", counterName);
                result.put("value", currentValue);
                break;

              case INCREMENT:
                Long current = countersStore.get(counterName);
                if (current == null) current = 0L;
                Long newValue = current + 1;
                countersStore.put(counterName, newValue);
                result = createCounterResponse("increment", counterName);
                result.put("previousValue", current);
                result.put("newValue", newValue);
                break;

              case SET:
                countersStore.put(counterName, value);
                result = createCounterResponse("set", counterName);
                result.put("value", value);
                break;

              case UPDATE:
                Long currentVal = countersStore.get(counterName);
                if (currentVal == null) currentVal = 0L;
                Long updatedValue = currentVal + value;
                countersStore.update(counterName, v -> (v == null ? 0L : v) + value);
                result = createCounterResponse("update", counterName);
                result.put("previousValue", currentVal);
                result.put("addedValue", value);
                result.put("newValue", updatedValue);
                break;

              default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
            }

            System.out.printf(
                "Counter '%s' %s: %s%n",
                counterName,
                operation.toString().toLowerCase(),
                result.get("value") != null
                    ? result.get("value")
                    : result.get("newValue") != null ? result.get("newValue") : "completed");

            agentNode.result(result);
          });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Key-Value Store Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      KeyValueStoreModule module = new KeyValueStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("KeyValueStoreAgent");

      System.out.println("Key-Value Store Agent Example:");

      // Demonstrate different counter operations
      System.out.println("\n--- Setting initial counter value ---");
      Map<String, Object> result1 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("page-views", Operation.SET, 10L));
      System.out.printf(
          "Result: action=%s, counter=%s, value=%d%n",
          result1.get("action"), result1.get("counter"), result1.get("value"));

      System.out.println("\n--- Getting current counter value ---");
      Map<String, Object> result2 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("page-views", Operation.GET, null));
      System.out.printf(
          "Result: action=%s, counter=%s, value=%d%n",
          result2.get("action"), result2.get("counter"), result2.get("value"));

      System.out.println("\n--- Incrementing counter ---");
      Map<String, Object> result3 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("page-views", Operation.INCREMENT, null));
      System.out.printf(
          "Result: action=%s, counter=%s, previous-value=%d, new-value=%d%n",
          result3.get("action"),
          result3.get("counter"),
          result3.get("previousValue"),
          result3.get("newValue"));

      System.out.println("\n--- Updating counter by adding value ---");
      Map<String, Object> result4 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("page-views", Operation.UPDATE, 5L));
      System.out.printf(
          "Result: action=%s, counter=%s, previous-value=%d, added-value=%d, new-value=%d%n",
          result4.get("action"),
          result4.get("counter"),
          result4.get("previousValue"),
          result4.get("addedValue"),
          result4.get("newValue"));

      System.out.println("\n--- Working with different counter ---");
      Map<String, Object> result5 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("api-calls", Operation.INCREMENT, null));
      System.out.printf(
          "Result: action=%s, counter=%s, previous-value=%d, new-value=%d%n",
          result5.get("action"),
          result5.get("counter"),
          result5.get("previousValue"),
          result5.get("newValue"));

      System.out.println("\n--- Final state check ---");
      Map<String, Object> result6 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("page-views", Operation.GET, null));
      Map<String, Object> result7 =
          (Map<String, Object>)
              agent.invoke(createCounterRequest("api-calls", Operation.GET, null));
      System.out.println("page-views final value: " + result6.get("value"));
      System.out.println("api-calls final value: " + result7.get("value"));

      System.out.println("\nNotice how:");
      System.out.println("- Counter values persist across invocations");
      System.out.println("- Different counters maintain separate state");
      System.out.println("- Various store operations (get, put, update) work correctly");
      System.out.println("- HashMap provides flexible key-value data structure");
    }
  }
}

```

---

### LangChain4jAgent

**File:** `src/main/java/com/rpl/agent/basic/LangChain4jAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Java example demonstrating LangChain4j chat model integration with agent-o-rama.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>OpenAI chat model configuration as agent object
 *   <li>Message handling with SystemMessage and UserMessage
 *   <li>Chat request with temperature and token limits
 *   <li>Simple single-node chat completion
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class LangChain4jAgent {

  /** Agent Module demonstrating LangChain4j integration. */
  public static class LangChain4jModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare OpenAI API key as agent object
      topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));

      // Build OpenAI chat model with configuration
      topology.declareAgentObjectBuilder(
        "openai-model",
        setup -> {
          String apiKey = setup.getAgentObject("openai-api-key");
          return OpenAiChatModel.builder()
              .apiKey(apiKey)
              .modelName("gpt-4o-mini")
              .temperature(0.7)
              .maxTokens(500)
              .build();
        });

      topology.newAgent("LangChain4jAgent").node("chat", null, (AgentNode agentNode, String userMessage) -> {
        ChatModel model = agentNode.getAgentObject("openai-model");

        // Send chat request to OpenAI using simple API
        String responseText = model.chat(userMessage);

        agentNode.result(responseText);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("LangChain4j Agent Example:");
      System.out.println("OPENAI_API_KEY environment variable not set.");
      System.out.println("Please set your OpenAI API key to run this example:");
      System.out.println("  export OPENAI_API_KEY=your-api-key-here");
      return;
    }

    System.out.println("Starting LangChain4j Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      LangChain4jModule module = new LangChain4jModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("LangChain4jAgent");

      System.out.println("LangChain4j Agent Example:");
      System.out.println("Sending message to OpenAI...\n");

      String result = (String) agent.invoke("What is agent-o-rama?");
      System.out.println("User: What is agent-o-rama?");
      System.out.println("\nAssistant: " + result);

      System.out.println("\nNotice how:");
      System.out.println("- OpenAI model is configured as an agent object");
      System.out.println("- Single node handles the complete chat interaction");
      System.out.println("- Temperature and token limits are customizable");
    }
  }
}
```

---

### MirrorAgent

**File:** `src/main/java/com/rpl/agent/basic/MirrorAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

/**
 * Java example demonstrating cross-module agent invocation using mirror agents.
 *
 * <p>This example demonstrates:
 *
 * <ul>
 *   <li>Multiple agent module definitions
 *   <li>getAgentClient: Get client for mirror agent within agent node
 *   <li>invoke: Invoke mirror agent across modules
 *   <li>getModuleName: Get module name for cross-module references
 *   <li>Multiple module deployment to IPC
 * </ul>
 */
public class MirrorAgent {

  /**
   * Module 1: Greeter agent that creates greeting messages.
   *
   * <p>This module contains a simple agent that takes a name and returns a greeting.
   */
  public static class GreeterModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("Greeter").node("greet", null, (AgentNode agentNode, String name) -> {
        String greeting = "Hello, " + name + "!";
        agentNode.result(greeting);
      });
    }
  }


  /**
   * Module 2: Mirror agent that invokes Greeter from Module 1.
   *
   * <p>This module declares a mirror reference to the Greeter agent and uses it to demonstrate
   * cross-module agent invocation.
   */
  public static class MirrorModule extends AgentModule {
    private static final String GREETER_MODULE_NAME = new GreeterModule().getModuleName();

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("MirrorAgent")
              .node("process", null, (AgentNode agentNode, String name) -> {
        // Get client for the mirror agent
        AgentClient greeterClient = agentNode.getMirrorAgentClient(GREETER_MODULE_NAME, "Greeter");

        // Invoke the mirror agent (cross-module call)
        String greeting = (String) greeterClient.invoke(name);

        // Add prefix to result
        String result = "Mirror says: " + greeting;
        agentNode.result(result);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Mirror Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch GreeterModule first
      GreeterModule greeterModule = new GreeterModule();
      ipc.launchModule(greeterModule, new LaunchConfig(1, 1));
      String greeterModuleName = greeterModule.getModuleName();

      // Launch MirrorModule with reference to GreeterModule
      MirrorModule mirrorModule = new MirrorModule();
      ipc.launchModule(mirrorModule, new LaunchConfig(1, 1));

      // Get agent manager for MirrorModule
      String mirrorModuleName = mirrorModule.getModuleName();
      AgentManager manager = AgentManager.create(ipc, mirrorModuleName);
      AgentClient mirrorAgent = manager.getAgentClient("MirrorAgent");

      // Invoke the mirror agent
      System.out.println("\nMirror Agent Results:");
      System.out.println("Input: \"Alice\" -> Result: " + mirrorAgent.invoke("Alice"));
      System.out.println("Input: \"Bob\" -> Result: " + mirrorAgent.invoke("Bob"));
    }
  }
}

```

---

### MultiAggAgent

**File:** `src/main/java/com/rpl/agent/basic/MultiAggAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.MultiAgg;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java example demonstrating custom aggregation logic with multi-agg for complex data combination.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>multiAgg: Custom aggregation with multiple tagged input streams
 *   <li>init clause: Initialize aggregation state
 *   <li>on clauses: Handle different types of incoming data
 *   <li>Complex aggregation patterns and state management
 * </ul>
 *
 * <p>Uses HashMap for request and response data structures with keys:
 *
 * <ul>
 *   <li>Request: "numbers" (List&lt;Integer&gt;), "text" (List&lt;String&gt;)
 *   <li>Response: "summary" (Map), "details" (Map)
 * </ul>
 */
public class MultiAggAgent {

  /** Agent Module demonstrating multi-agg functionality. */
  public static class MultiAggModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology
          .newAgent("MultiAggAgent")
          // Start by distributing different types of data
          .aggStartNode(
              "distribute-data",
              List.of("process-numbers", "process-text"),
              (AgentNode agentNode, Map<String, Object> request) -> {
                List<Integer> numbers = (List<Integer>) request.get("numbers");
                List<String> text = (List<String>) request.get("text");

                System.out.println("Distributing data for parallel processing");

                // Send numbers for mathematical analysis
                if (numbers != null) {
                  for (Integer num : numbers) {
                    agentNode.emit("process-numbers", num);
                  }
                }

                // Send text for linguistic analysis
                if (text != null) {
                  for (String txt : text) {
                    agentNode.emit("process-text", txt);
                  }
                }
                return request;
              })
          // Process numbers - compute statistics
          .node("process-numbers", "combine-results", (AgentNode agentNode, Integer number) -> {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("value", number);
            analysis.put("square", number * number);
            analysis.put("even", number % 2 == 0);

            agentNode.emit("combine-results", "number", analysis);
          })
          // Process text - analyze content
          .node("process-text", "combine-results", (AgentNode agentNode, String text) -> {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("value", text);
            analysis.put("length", text.length());
            analysis.put("uppercase", text.toUpperCase());
            analysis.put("words", text.split("\\s+").length);

            agentNode.emit("combine-results", "text", analysis);
          })
          // Combine all analysis using multi-agg with tagged inputs
          .aggNode(
              "combine-results",
              null,
              MultiAgg.init(() -> new AggregationState())
                  .on(
                      "number",
                      (AggregationState state, Map<String, Object> analysis) -> {
                        state.numbers.add(analysis);
                        return state;
                      })
                  .on(
                      "text",
                      (AggregationState state, Map<String, Object> analysis) -> {
                        state.text.add(analysis);
                        return state;
                      }),
              (AgentNode agentNode, AggregationState aggregatedState, Object startNodeArg) -> {
                List<Map<String, Object>> numbers = aggregatedState.numbers;
                List<Map<String, Object>> textEntries = aggregatedState.text;

                // Calculate statistics from numbers
                int numberSum = 0;
                int squareSum = 0;
                int evenCount = 0;
                for (Map<String, Object> num : numbers) {
                  numberSum += (Integer) num.get("value");
                  squareSum += (Integer) num.get("square");
                  if ((Boolean) num.get("even")) {
                    evenCount++;
                  }
                }

                // Calculate statistics from text
                int totalWords = 0;
                int totalCharacters = 0;
                for (Map<String, Object> txt : textEntries) {
                  totalWords += (Integer) txt.get("words");
                  totalCharacters += (Integer) txt.get("length");
                }

                System.out.printf(
                    "Processed %d numbers and %d text entries%n", numbers.size(), textEntries.size());

                // Create final result
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> summary = new HashMap<>();
                summary.put("numberSum", numberSum);
                summary.put("squareSum", squareSum);
                summary.put("evenCount", evenCount);
                summary.put("totalWords", totalWords);
                summary.put("totalCharacters", totalCharacters);

                Map<String, Object> details = new HashMap<>();
                details.put("numbers", numbers);
                details.put("text", textEntries);

                result.put("summary", summary);
                result.put("details", details);

                agentNode.result(result);
              });
    }
  }

  /** Custom aggregator state for combining multiple data types. */
  public static class AggregationState implements com.rpl.rama.RamaSerializable {
    public List<Map<String, Object>> numbers = new ArrayList<>();
    public List<Map<String, Object>> text = new ArrayList<>();

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      byte[] ser = com.rpl.agentorama.impl.AORHelpers.freeze(this.toMap());
      out.writeInt(ser.length);
      out.write(ser);
    }

    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
      int size = in.readInt();
      byte[] ser = new byte[size];
      in.readFully(ser);
      Map<String, Object> data = (Map<String, Object>) com.rpl.agentorama.impl.AORHelpers.thaw(ser);
      this.fromMap(data);
    }

    private Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("numbers", numbers);
      map.put("text", text);
      return map;
    }

    private void fromMap(Map<String, Object> map) {
      this.numbers = (List<Map<String, Object>>) map.get("numbers");
      this.text = (List<Map<String, Object>>) map.get("text");
    }
  }





  public static void main(String[] args) throws Exception {
    System.out.println("Starting Multi-Agg Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      MultiAggModule module = new MultiAggModule();
      ipc.launchModule(module, new LaunchConfig(2, 2));

      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("MultiAggAgent");

      System.out.println("Multi-Agg Agent Example:");
      System.out.println("Processing mixed data types with custom aggregation logic");

      Map<String, Object> request = new HashMap<>();
      request.put("numbers", new ArrayList(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
      request.put(
          "text",
          new ArrayList(
              List.of("Hello world", "Multi-agg is powerful", "Parallel processing rocks")));

      Map<String, Object> result = (Map<String, Object>) agent.invoke(request);

      System.out.println("\nResults:");
      Map<String, Object> summary = (Map<String, Object>) result.get("summary");
      System.out.println("  Summary:");
      System.out.println("    Numbers processed: " + summary.get("numbersProcessed"));
      System.out.println("    Text entries processed: " + summary.get("textProcessed"));
      System.out.println("    Sum of numbers: " + summary.get("numberSum"));
      System.out.println("    Sum of squares: " + summary.get("squareSum"));
      System.out.println("    Even numbers: " + summary.get("evenCount"));
      System.out.println("    Total words: " + summary.get("totalWords"));
      System.out.println("    Total characters: " + summary.get("totalCharacters"));

      Map<String, Object> details = (Map<String, Object>) result.get("details");
      List<Map<String, Object>> numberDetails = (List<Map<String, Object>>) details.get("numbers");
      List<Map<String, Object>> textDetails = (List<Map<String, Object>>) details.get("text");

      System.out.println("\n  Sample detailed results:");
      System.out.println("    First number analysis: " + numberDetails.get(0));
      System.out.println("    First text analysis: " + textDetails.get(0));

      System.out.println("\nNotice how:");
      System.out.println("- Multi-agg handles different types of tagged inputs");
      System.out.println("- Each 'on' clause processes specific data types");
      System.out.println("- State accumulation works across multiple input streams");
      System.out.println("- Parallel processing with custom aggregation logic");
    }
  }
}

```

---

### MultiNodeAgent

**File:** `src/main/java/com/rpl/agent/basic/MultiNodeAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

/**
 * Java example demonstrating agent graphs with multiple nodes and inter-node emissions.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>Agent graph with multiple connected nodes
 *   <li>emit!: Send data from one node to another
 *   <li>Node chaining and data flow
 *   <li>Multi-step greeting process through graph traversal
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class MultiNodeAgent {

  /**
   * Multi-node Agent Module demonstrating greeting workflow through graph.
   *
   * <p>This nested module implements an agent with multiple nodes that process a user name through
   * a greeting workflow: receive -> personalize -> finalize.
   */
  public static class MultiNodeModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology
          .newAgent("MultiNodeAgent")
          // First node: receive user name from the invoke call and forward it
          .node("receive", "personalize", (AgentNode agentNode, String userName) -> {
            // Forward the user name to the personalize node
            agentNode.emit("personalize", userName);
          })
          // Second node: personalize the greeting message
          .node("personalize", "finalize", (AgentNode agentNode, String userName) -> {
            // Create a personalized greeting message
            String greeting = "Hello, " + userName + "!";

            // Emit both the user name and greeting to the finalize node
            agentNode.emit("finalize", userName, greeting);
          })
          // Final node: create complete welcome message
          .node("finalize", null, (AgentNode agentNode, String userName, String greeting) -> {
            // Create the complete welcome message
            String result =
                greeting + " Welcome to agent-o-rama! " + "Thanks for joining us, " + userName + ".";

            // Return the final result
            agentNode.result(result);
          });
    }
  }




  public static void main(String[] args) throws Exception {
    System.out.println("Starting Multi-Node Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      MultiNodeModule module = new MultiNodeModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("MultiNodeAgent");

      // Invoke agent synchronously with sample user names
      System.out.println("Multi-Node Agent Results:");
      System.out.println();
      System.out.println("--- Greeting Alice ---");
      String result1 = (String) agent.invoke("Alice");
      System.out.println("Result: " + result1);

      System.out.println();
      System.out.println("--- Greeting Bob ---");
      String result2 = (String) agent.invoke("Bob");
      System.out.println("Result: " + result2);

      System.out.println();
      System.out.println("--- Greeting Charlie ---");
      String result3 = (String) agent.invoke("Charlie");
      System.out.println("Result: " + result3);
    }
  }
}

```

---

### PStateStoreAgent

**File:** `src/main/java/com/rpl/agent/basic/PStateStoreAgent.java`

```java
package com.rpl.agent.basic;

import static com.rpl.rama.helpers.TopologyUtils.*;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.store.PStateStore;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Java example demonstrating PState store operations for complex path-based data structures.
 *
 * <p>Features demonstrated:bcommit for now
 *
 * <ul>
 *   <li>declarePStateStore: Create a PState store with schema
 *   <li>getStore: Access PState stores from agent nodes
 *   <li>PStateStore.select: Query data using path expressions
 *   <li>PStateStore.selectOne: Query single values using path expressions
 *   <li>PStateStore.transform: Update data using path-based transformations
 *   <li>Complex nested data structures and path-based operations
 *   <li>Schema-based storage with Rama's native persistent state
 * </ul>
 *
 * <p>Uses HashMap for request and response data structures with keys:
 *
 * <ul>
 *   <li>Request: "companyId", "companyName", "deptId", "deptName", "employee" (Map)
 *   <li>Response: "action", "companyId", "companyName", "deptId", "deptName", "employeeCount",
 *       "averageSalary", "departmentCount", "allCompanyEmployeeNames", "queriedEmployee"
 * </ul>
 */
public class PStateStoreAgent {

  /** Agent Module demonstrating PState store usage. */
  public static class PStateStoreModule extends AgentModule {

    @Override
     protected void defineAgents(AgentTopology topology) {
      // Declare PState store for hierarchical organization data
      // Schema: {company-id -> {:name String, :departments {dept-id -> {:name String, :employees
      // {emp-id -> {:id String, :name String, :salary Long, :metadata Object}}}}}}
      topology.declarePStateStore(
          "$$organizations",
          PState.mapSchema(
              String.class,
              PState.fixedKeysSchema(
                  "name",
                  String.class,
                  "departments",
                  PState.mapSchema(
                      String.class,
                      PState.fixedKeysSchema(
                          "name",
                          String.class,
                          "employees",
                          PState.mapSchema(
                              String.class,
                              PState.fixedKeysSchema(
                                  "id",
                                  String.class,
                                  "name",
                                  String.class,
                                  "salary",
                                  Long.class,
                                  "metadata",
                                  Object.class)))))));

      topology
          .newAgent("PStateStoreAgent")
          .node("update-org", "query-data", (AgentNode agentNode, Map<String, Object> request) -> {
            PStateStore orgStore = agentNode.getStore("$$organizations");
            String companyId = (String) request.get("companyId");
            String companyName = (String) request.get("companyName");
            String deptId = (String) request.get("deptId");
            String deptName = (String) request.get("deptName");
            Map<String, Object> employee = (Map<String, Object>) request.get("employee");

            // Initialize company if it doesn't exist
            if (companyName != null) {
              orgStore.transform(
                companyId,
                Path
                .key(companyId, "name")
                .term(existing -> existing != null ? existing : companyName));
            }

            // Initialize department if it doesn't exist
            if (deptName != null) {
              orgStore.transform(
                companyId,
                Path.key(companyId, "departments", deptId, "name")
                .term(existing -> existing != null ? existing : deptName));
            }

            // Add or update employee
            if (employee != null) {
              String empId = (String) employee.get("id");
              orgStore.transform(
                companyId,
                Path.key(companyId, "departments", deptId, "employees", empId)
                .termVal(employee));
            }

            Map<String, Object> emitData = new HashMap<>();
            emitData.put("companyId", companyId);
            emitData.put("deptId", deptId);
            emitData.put("employeeId", employee != null ? employee.get("id") : null);

            agentNode.emit("query-data", emitData);
          })
          .node("query-data", "calculate-metrics", (AgentNode agentNode, Map<String, Object> request) -> {
            PStateStore orgStore = agentNode.getStore("$$organizations");
            String companyId = (String) request.get("companyId");
            String deptId = (String) request.get("deptId");
            String employeeId = (String) request.get("employeeId");

            // Query various data paths
            String companyName =
                (String) orgStore.selectOne(companyId, Path.key(companyId, "name"));

            String deptName =
                (String)
                orgStore.selectOne(
                  companyId,
                  Path.key(companyId, "departments", deptId, "name"));

            Map<String, Object> allEmployees =
                (Map<String, Object>) orgStore.selectOne(
                  companyId,
                  Path.key(companyId, "departments", deptId, "employees"));

            Map<String, Object> specificEmployee = null;
            if (employeeId != null) {
              specificEmployee =
                  (Map<String, Object>)
                      orgStore.selectOne(
                      companyId,
                      Path.key(companyId, "departments", deptId, "employees", employeeId));
            }

            Map<String, Object> allDepartments =
                (Map<String, Object>)
                orgStore.selectOne(
                  companyId,
                  Path.key(companyId, "departments"));

            Map<String, Object> emitData = new HashMap<>();
            emitData.put("companyId", companyId);
            emitData.put("companyName", companyName);
            emitData.put("deptId", deptId);
            emitData.put("deptName", deptName);
            emitData.put("allEmployees", allEmployees);
            emitData.put("specificEmployee", specificEmployee);
            emitData.put("allDepartments", allDepartments);

            agentNode.emit("calculate-metrics", emitData);
          })
          .node("calculate-metrics", null, (AgentNode agentNode, Map<String, Object> request) -> {
            PStateStore orgStore = agentNode.getStore("$$organizations");
            String companyId = (String) request.get("companyId");
            String companyName = (String) request.get("companyName");
            String deptId = (String) request.get("deptId");
            String deptName = (String) request.get("deptName");
            Map<String, Object> allEmployees = (Map<String, Object>) request.get("allEmployees");
            Map<String, Object> specificEmployee = (Map<String, Object>) request.get("specificEmployee");
            Map<String, Object> allDepartments = (Map<String, Object>) request.get("allDepartments");

            // Calculate department metrics
            List<Map<String, Object>> employeeList = new ArrayList<>();
            if (allEmployees != null) {
              for (Object emp : allEmployees.values()) {
                if (emp instanceof Map) {
                  employeeList.add((Map<String, Object>) emp);
                }
              }
            }

            int totalEmployees = employeeList.size();
            double avgSalary = 0;
            if (!employeeList.isEmpty()) {
              long sum = 0;
              for (Map<String, Object> emp : employeeList) {
                Long salary = (Long) emp.get("salary");
                if (salary != null) {
                  sum += salary;
                }
              }
              avgSalary = (double) sum / totalEmployees;
            }

            int deptCount = allDepartments != null ? allDepartments.size() : 0;

            // Demonstrate complex path querying - get all employee names across all departments
            List<String> allCompanyEmployeeNames =
                orgStore.select(
                  companyId,
                  Path.key(companyId, "departments")
                  .mapVals()
                  .key("employees")
                  .mapVals()
                  .key("name")).stream()
              .map(obj -> (String) obj)
              .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("action", "pstate-query");
            result.put("companyId", companyId);
            result.put("companyName", companyName);
            result.put("deptId", deptId);
            result.put("deptName", deptName);
            result.put("employeeCount", totalEmployees);
            result.put("averageSalary", avgSalary);
            result.put("departmentCount", deptCount);
            result.put("allCompanyEmployeeNames", allCompanyEmployeeNames);
            result.put("queriedEmployee", specificEmployee);
            result.put("processedAt", System.currentTimeMillis());

            agentNode.result(result);
          });
    }
  }




  public static void main(String[] args) throws Exception {
    try (InProcessCluster ipc = InProcessCluster.create()) {
      PStateStoreModule module = new PStateStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("PStateStoreAgent");

      System.out.println("PState Store Agent Example:");

      // First invocation: Create company and first employee
      System.out.println("\n--- Creating company and first employee ---");
      Map<String, Object> request1 = new HashMap<>();
      request1.put("companyId", "tech-corp");
      request1.put("companyName", "TechCorp Inc");
      request1.put("deptId", "engineering");
      request1.put("deptName", "Engineering");
      Map<String, Object> emp1 = new HashMap<>();
      emp1.put("id", "emp001");
      emp1.put("name", "Alice Johnson");
      emp1.put("salary", 95000L);
      Map<String, Object> meta1 = new HashMap<>();
      meta1.put("level", "senior");
      meta1.put("skills", List.of("clojure", "java"));
      emp1.put("metadata", meta1);
      request1.put("employee", emp1);

      Map<String, Object> result1 = (Map<String, Object>) agent.invoke(request1);
      System.out.println("Result 1:");
      System.out.println("  Company: " + result1.get("companyName"));
      System.out.println("  Department: " + result1.get("deptName"));
      System.out.println("  Employee count: " + result1.get("employeeCount"));
      System.out.println("  Average salary: " + result1.get("averageSalary"));

      // Second invocation: Add another employee to same department
      System.out.println("\n--- Adding second employee ---");
      Map<String, Object> request2 = new HashMap<>();
      request2.put("companyId", "tech-corp");
      request2.put("deptId", "engineering");
      Map<String, Object> emp2 = new HashMap<>();
      emp2.put("id", "emp002");
      emp2.put("name", "Bob Smith");
      emp2.put("salary", 85000L);
      Map<String, Object> meta2 = new HashMap<>();
      meta2.put("level", "mid");
      meta2.put("skills", List.of("python", "sql"));
      emp2.put("metadata", meta2);
      request2.put("employee", emp2);

      Map<String, Object> result2 = (Map<String, Object>) agent.invoke(request2);
      System.out.println("Result 2:");
      System.out.println("  Employee count: " + result2.get("employeeCount"));
      System.out.println("  Average salary: " + result2.get("averageSalary"));
      System.out.println("  All company employees: " + result2.get("allCompanyEmployeeNames"));

      // Third invocation: Add different department
      System.out.println("\n--- Adding marketing department ---");
      Map<String, Object> request3 = new HashMap<>();
      request3.put("companyId", "tech-corp");
      request3.put("deptId", "marketing");
      request3.put("deptName", "Marketing");
      Map<String, Object> emp3 = new HashMap<>();
      emp3.put("id", "emp003");
      emp3.put("name", "Carol Davis");
      emp3.put("salary", 75000L);
      Map<String, Object> meta3 = new HashMap<>();
      meta3.put("level", "manager");
      meta3.put("skills", List.of("strategy", "analytics"));
      emp3.put("metadata", meta3);
      request3.put("employee", emp3);

      Map<String, Object> result3 = (Map<String, Object>) agent.invoke(request3);
      System.out.println("Result 3:");
      System.out.println("  Department count: " + result3.get("departmentCount"));
      System.out.println("  Current dept employee count: " + result3.get("employeeCount"));
      System.out.println("  All company employees: " + result3.get("allCompanyEmployeeNames"));

      System.out.println("\nNotice how:");
      System.out.println("- Complex nested data structures are supported");
      System.out.println("- Path-based querying allows precise data access");
      System.out.println("- Updates can target specific nested elements");
      System.out.println("- Cross-department queries are possible with path expressions");
    }
  }
}

```

---

### RamaModuleAgent

**File:** `src/main/java/com/rpl/agent/basic/RamaModuleAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.Depot;
import com.rpl.rama.RamaModule;
import com.rpl.rama.module.StreamTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;

/**
 * Java example demonstrating direct Rama module usage instead of AgentModule.
 *
 * <p>This example demonstrates:
 *
 * <ul>
 *   <li>Implementing RamaModule directly (not extending AgentModule)
 *   <li>Creating AgentTopology manually via AgentTopology.create()
 *   <li>Accessing StreamTopology via getStreamTopology()
 *   <li>Declaring Rama depots alongside agents
 *   <li>Using Rama's stream processing with agents
 *   <li>Explicitly calling topology.define()
 * </ul>
 *
 * <p>This approach allows integration of agent-o-rama with full Rama features when you need access
 * to depots, stream processing, or other Rama primitives.
 */
public class RamaModuleAgent {

  /**
   * Direct Rama Module implementation showing integration with full Rama features.
   *
   * <p>This module demonstrates manual topology creation and depot integration, providing access to
   * the complete Rama feature set alongside agent-o-rama agents.
   */
  public static class RamaModule implements com.rpl.rama.RamaModule {

    public RamaModule() {
      super();
    }

    @Override
    public String getModuleName() {
      return "RamaModuleAgent";
    }

    @Override
    public void define(Setup setup, Topologies topologies) {
      // Declare a depot to demonstrate Rama feature integration
      setup.declareDepot("*example-depot", Depot.random());

      // Create agents topology manually
      AgentTopology topology = AgentTopology.create(setup, topologies);

      // Access underlying stream topology for Rama features (available for custom processing)
      StreamTopology streamTopology = topology.getStreamTopology();

      // Note: Stream topology can be used here for custom Rama stream processing
      // For simplicity, this example focuses on the agent topology integration

      // Define a simple feedback agent
      topology
          .newAgent("FeedbackAgent")
          .node("process-feedback", null, (AgentNode agentNode, String feedbackText) -> {
            // Process feedback and return success response
            HashMap<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Processed: " + feedbackText);
            response.put("length", feedbackText.length());

            agentNode.result(response);
          });

      // Explicitly finalize agent definitions
      topology.define();
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Rama Module Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the Rama module
      RamaModule module = new RamaModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);

      System.out.println("\n=== Agent Results ===");
      System.out.println("Available agents: " + manager.getAgentNames());

      // Note: The depot "*example-depot" is declared and available for use
      // For demonstration purposes, we focus on the agent functionality

      // Get client for our agent
      AgentClient agent = manager.getAgentClient("FeedbackAgent");

      // Invoke agent with sample feedback
      Object result1 = agent.invoke("Great product!");
      Object result2 = agent.invoke("Needs improvement");

      System.out.println("Feedback 1: " + result1);
      System.out.println("Feedback 2: " + result2);
    }
  }
}

```

---

### RecordOpAgent

**File:** `src/main/java/com/rpl/agent/basic/RecordOpAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.NestedOpType;
import com.rpl.agentorama.UI;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Java example demonstrating recording custom operations in agent traces.
 *
 * <p>This example demonstrates:
 *
 * <ul>
 *   <li>recordNestedOp: Add custom operation info to agent trace
 *   <li>UIServer.start: Launch web UI for viewing traces
 *   <li>Agent trace visualization in UI
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class RecordOpAgent {

  /**
   * RecordOp Agent Module demonstrating trace recording.
   *
   * <p>This module implements an agent that records custom operations in its trace for debugging
   * and monitoring purposes.
   */
  public static class RecordOpModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("RecordOpAgent").node("process", null, (AgentNode agentNode, String userName) -> {
        // Record timing of a custom operation
        long startTime = System.currentTimeMillis();

        // Simulate some work
        String greeting = "Hello, " + userName + "!";
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        long finishTime = System.currentTimeMillis();

        // Record the operation in the agent trace
        Map<String, Object> info = new HashMap<>();
        info.put("operation", "generate-greeting");
        info.put("input", userName);
        info.put("output", greeting);

        agentNode.recordNestedOp(NestedOpType.OTHER, startTime, finishTime, info);

        agentNode.result(greeting);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting RecordOp Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      RecordOpModule module = new RecordOpModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("RecordOpAgent");

      System.out.println("RecordOp Agent Example");
      System.out.println("======================\n");

      System.out.println("Result: " + agent.invoke("Alice"));
      System.out.println("Result: " + agent.invoke("Bob"));

      System.out.println("\n✓ Agent invocations complete!");
      System.out.println("\nTo view the recorded operations in the trace:");
      System.out.println("  1. Open the UI at http://localhost:8080");
      System.out.println("  2. Click on an agent invocation");
      System.out.println("  3. Look for the 'generate-greeting' operation in the trace details");
      System.out.println("\nPress Enter to exit and shut down the UI...");

      Scanner scanner = new Scanner(System.in);
      scanner.nextLine();
    }
  }
}

```

---

### RouterAgent

**File:** `src/main/java/com/rpl/agent/basic/RouterAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java example demonstrating conditional routing between different nodes in an agent graph.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>Conditional routing based on input
 *   <li>Multiple emit calls to different nodes
 *   <li>Branching execution paths that reconverge
 *   <li>Different processing for different input types
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class RouterAgent {

  /** Agent Module that routes messages to different processing nodes based on content. */
  public static class RouterAgentModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology
          .newAgent("RouterAgent")
          // Router node: decides which processing node to send to
          .node("route", List.of("handle-urgent", "handle-default"), (AgentNode agentNode, String message) -> {
            if (message.startsWith("urgent:")) {
              agentNode.emit("handle-urgent", message);
            } else {
              agentNode.emit("handle-default", message);
            }
          })
          // Urgent message handler
          .node("handle-urgent", "finalize", (AgentNode agentNode, String message) -> {
            String content = message.substring(7); // remove "urgent:" prefix
            Map<String, Object> processed = new HashMap<>();
            processed.put("priority", "HIGH");
            processed.put("message", content);
            agentNode.emit("finalize", processed);
          })
          // Default message handler
          .node("handle-default", "finalize", (AgentNode agentNode, String message) -> {
            Map<String, Object> processed = new HashMap<>();
            processed.put("priority", "NORMAL");
            processed.put("message", message);
            agentNode.emit("finalize", processed);
          })
          // Final node: creates the result
          // Both urgent and default handlers emit to this node - reconvergence point
          .node("finalize", null, (AgentNode agentNode, Map<String, Object> processed) -> {
            String priority = (String) processed.get("priority");
            String message = (String) processed.get("message");
            String result = String.format("[%s] %s", priority, message);
            agentNode.result(result);
          });
    }
  }





  public static void main(String[] args) throws Exception {
    System.out.println("Starting Router Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      RouterAgentModule module = new RouterAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("RouterAgent");

      System.out.println("Router Agent Results:");

      System.out.println("\n--- Urgent Message ---");
      String result1 = (String) agent.invoke("urgent:system failure detected");
      System.out.println("Result: " + result1);

      System.out.println("\n--- Default Message ---");
      String result2 = (String) agent.invoke("just a regular message");
      System.out.println("Result: " + result2);
    }
  }
}

```

---

### StreamingAgent

**File:** `src/main/java/com/rpl/agent/basic/StreamingAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java example demonstrating streaming chunk emission from agent nodes.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>streamChunk: Emit streaming data from nodes
 *   <li>agent.stream: Subscribe to streaming data from specific nodes
 *   <li>Real-time data flow with incremental results
 *   <li>Streaming completion and callbacks
 * </ul>
 *
 * <p>All required classes are defined as nested classes within this single file for simplicity and
 * self-containment.
 */
public class StreamingAgent {

  /** Agent Module demonstrating streaming functionality. */
  public static class StreamingAgentModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      topology.newAgent("StreamingAgent").node("process-data", null, (AgentNode agentNode, Map<String, Object> request) -> {
        int dataSize = (Integer) request.get("dataSize");
        int chunkSize = (Integer) request.get("chunkSize");
        int totalChunks = (int) Math.ceil((double) dataSize / chunkSize);

        System.out.printf("Processing %d items in chunks of %d%n", dataSize, chunkSize);

        // Stream progress as we process chunks
        for (int chunkNum = 0; chunkNum < totalChunks; chunkNum++) {
          int startIdx = chunkNum * chunkSize;
          int endIdx = Math.min(startIdx + chunkSize, dataSize);
          List<Integer> items = new ArrayList<>();
          for (int i = startIdx; i < endIdx; i++) {
            items.add(i);
          }
          double progress = (double) (chunkNum + 1) / totalChunks;

          // Simulate processing time
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException("Processing interrupted", e);
          }

          // Stream chunk progress
          Map<String, Object> chunkData = new HashMap<>();
          chunkData.put("chunkNumber", chunkNum);
          chunkData.put("itemsProcessed", items.size());
          chunkData.put("progress", progress);
          chunkData.put("items", items);
          agentNode.streamChunk(chunkData);

          System.out.printf(
              "Processed chunk %d/%d (%.1f%%)%n", chunkNum + 1, totalChunks, progress * 100.0);
        }

        // Return final result
        Map<String, Object> result = new HashMap<>();
        result.put("action", "data-processing");
        result.put("totalItems", dataSize);
        result.put("totalChunks", totalChunks);
        result.put("chunkSize", chunkSize);
        result.put("completedAt", System.currentTimeMillis());
        agentNode.result(result);
      });
    }
  }


  public static void main(String[] args) throws Exception {
    System.out.println("Starting Streaming Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Launch the agent module
      StreamingAgentModule module = new StreamingAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StreamingAgent");

      System.out.println("Streaming Agent Example:");
      System.out.println("Processing data with real-time streaming updates...");

      // Start async processing
      Map<String, Object> request = new HashMap<>();
      request.put("dataSize", 50);
      request.put("chunkSize", 10);
      AgentInvoke invoke = agent.initiate(request);
      AtomicInteger chunksReceived = new AtomicInteger(0);

      // Subscribe to streaming chunks
      agent.stream(
          invoke,
          "process-data",
          (allChunks, newChunks, reset, complete) -> {
            for (Object chunkObj : newChunks) {
              Map<String, Object> chunk = (Map<String, Object>) chunkObj;
              chunksReceived.incrementAndGet();
              System.out.printf(
                  "Received chunk %d: %d items (%.1f%% complete)%n",
                  (Integer) chunk.get("chunkNumber"),
                  (Integer) chunk.get("itemsProcessed"),
                  (Double) chunk.get("progress") * 100.0);
            }
          });

      // Wait for completion
      Map<String, Object> result = (Map<String, Object>) agent.result(invoke);

      System.out.println("\nFinal result:");
      System.out.println("  Total items processed: " + result.get("totalItems"));
      System.out.println("  Total chunks: " + result.get("totalChunks"));
      System.out.println("  Chunk size: " + result.get("chunkSize"));
      System.out.println("  Chunks received via streaming: " + chunksReceived.get());

      System.out.println("\nNotice how:");
      System.out.println("- Streaming provides real-time progress updates");
      System.out.println("- Chunks are received while processing continues");
      System.out.println("- Final result provides summary information");
    }
  }
}

```

---

### StreamingLangchain4jAgent

**File:** `src/main/java/com/rpl/agent/basic/StreamingLangchain4jAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.List;

/**
 * Java example demonstrating LangChain4j streaming chat model integration.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>OpenAI streaming chat model configuration as agent object
 *   <li>Streaming chat completion
 *   <li>agent-stream subscription for real-time token reception
 * </ul>
 *
 * <p>This example requires OPENAI_API_KEY environment variable to be set.
 */
public class StreamingLangchain4jAgent {

  /** Agent Module demonstrating streaming LangChain4j integration. */
  public static class StreamingLangChain4jModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare OpenAI API key as agent object
      topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));

      // Build OpenAI streaming chat model with configuration
      topology.declareAgentObjectBuilder(
          "openai-streaming-model",
          setup -> {
            String apiKey = (String) setup.getAgentObject("openai-api-key");
            return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .build();
          });

      topology
          .newAgent("StreamingLangChain4jAgent")
          .node("streaming-chat", null, (AgentNode agentNode, String userMessage) -> {
            // Agent objects wrapping StreamingChatModel are returned as ChatModel
            ChatModel model = (ChatModel) agentNode.getAgentObject("openai-streaming-model");

            // Build chat request
            ChatRequest request =
                ChatRequest.builder().messages(List.<ChatMessage>of(new UserMessage(userMessage))).build();

            // Send chat request - streaming happens automatically with agent-o-rama
            ChatResponse response = model.chat(request);
            String responseText = response.aiMessage().text();

            agentNode.result(responseText);
          });
    }
  }


  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("Streaming LangChain4j Agent Example:");
      System.out.println("OPENAI_API_KEY environment variable not set.");
      System.out.println("Please set your OpenAI API key to run this example:");
      System.out.println("  export OPENAI_API_KEY=your-api-key-here");
      return;
    }

    try (InProcessCluster ipc = InProcessCluster.create()) {
      StreamingLangChain4jModule module = new StreamingLangChain4jModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StreamingLangChain4jAgent");

      System.out.println("Streaming LangChain4j Agent Example:");
      System.out.println("Asking OpenAI a question with streaming...\n");

      System.out.println("User: Explain what machine learning is in simple terms");

      String result = (String) agent.invoke("Explain what machine learning is in simple terms");

      System.out.println("\nAssistant: " + result);

      System.out.println("\nNotice how:");
      System.out.println("- OpenAI streaming model is automatically wrapped by agent-o-rama");
      System.out.println("- Streaming chunks are emitted automatically during execution");
      System.out.println("- Final result contains the complete response");
    }
  }
}

```

---

### StructuredLangchain4jAgent

**File:** `src/main/java/com/rpl/agent/basic/StructuredLangchain4jAgent.java`

```java
package com.rpl.agent.basic;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.AgentModule;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import java.util.List;

/**
 * Java example demonstrating LangChain4j structured output with JSON response format.
 *
 * <p>Features demonstrated:
 *
 * <ul>
 *   <li>Structured output with Java records
 *   <li>OpenAI chat model integration with structured responses
 *   <li>Single-node agent returning structured data
 * </ul>
 *
 * <p>This example requires OPENAI_API_KEY environment variable to be set.
 */
public class StructuredLangchain4jAgent {

  /** Structured output class for question analysis. */
  public record QuestionAnalysis(
      @Description("Type of question being asked") String questionType,
      @Description("Complexity level of the question") String complexity,
      @Description("Key topics covered in the question") List<String> mainTopics,
      @Description("Direct answer to the user's question") String answer,
      @Description("Confidence level in the response") String confidence) {}

  /** Agent Module demonstrating structured LangChain4j output. */
  public static class StructuredLangChain4jModule extends AgentModule {

    @Override
    protected void defineAgents(AgentTopology topology) {
      // Declare OpenAI API key as agent object
      topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));

      // Build OpenAI chat model with configuration
      topology.declareAgentObjectBuilder(
          "openai-model",
          setup -> {
            String apiKey = (String) setup.getAgentObject("openai-api-key");
            return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.3)
                .maxTokens(300)
                .responseFormat("json_object")
                .build();
          });

      topology
          .newAgent("StructuredLangChain4jAgent")
          .node("analyze-question", null, (AgentNode agentNode, String userQuestion) -> {
            ChatModel model = (ChatModel) agentNode.getAgentObject("openai-model");

            // Request structured output from OpenAI
            String systemPrompt =
                "You are an intelligent question analyzer. Analyze the user's question and provide a JSON"
                    + " response with these fields: questionType (one of: factual, analytical, creative,"
                    + " technical, personal), complexity (one of: simple, moderate, complex), mainTopics"
                    + " (array of key topics), answer (direct answer to the question), confidence (one"
                    + " of: high, medium, low).";

            String fullPrompt = systemPrompt + "\n\nUser question: " + userQuestion;

            String response = model.chat(fullPrompt);

            agentNode.result(response);
          });
    }
  }


  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("Structured LangChain4j Agent Example:");
      System.out.println("OPENAI_API_KEY environment variable not set.");
      System.out.println("Please set your OpenAI API key to run this example:");
      System.out.println("  export OPENAI_API_KEY=your-api-key-here");
      return;
    }

    try (InProcessCluster ipc = InProcessCluster.create()) {
      StructuredLangChain4jModule module = new StructuredLangChain4jModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StructuredLangChain4jAgent");

      System.out.println("Structured LangChain4j Agent Example:");
      System.out.println("Analyzing questions with structured output...\n");

      // Test with different types of questions
      String[] questions = {
        "What is artificial intelligence?",
        "How can I improve my programming skills?",
        "Write a creative story about a robot"
      };

      for (String question : questions) {
        System.out.println("Question: " + question);
        String result = (String) agent.invoke(question);
        System.out.println("Analysis: " + result);
        System.out.println();
      }

      System.out.println("Notice how:");
      System.out.println("- JSON response format ensures structured output");
      System.out.println("- Different question types are automatically categorized");
      System.out.println("- Response includes metadata about the analysis");
    }
  }
}

```

---

### ReActExample

**File:** `src/main/java/com/rpl/agent/react/ReActExample.java`

```java
package com.rpl.agent.react;

import com.rpl.agentorama.*;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.*;
import java.io.*;

/**
 * Main class for running the ReAct agent example.
 *
 * This example demonstrates how to create and run a ReAct (Reasoning and Acting) agent that can
 * search the web and answer questions.
 *
 * The agent uses OpenAI GPT-4o-mini for language processing and Tavily for web search
 * capabilities
 *
 * Required environment variables:
 *   - OPENAI_API_KEY: Your OpenAI API key
 *   - TAVILY_API_KEY: Your * Tavily search API key
 */
public class ReActExample {
  private static void validateEnvironmentVariables() {
    String openaiKey = System.getenv("OPENAI_API_KEY");
    String tavilyKey = System.getenv("TAVILY_API_KEY");

    if (openaiKey == null || openaiKey.trim().isEmpty()) {
      System.err.println("Error: OPENAI_API_KEY environment variable is not set.");
      System.err.println("Please set your OpenAI API key: export OPENAI_API_KEY=your_key_here");
      System.exit(1);
    }

    if (tavilyKey == null || tavilyKey.trim().isEmpty()) {
      System.err.println("Error: TAVILY_API_KEY environment variable is not set.");
      System.err.println("Please set your Tavily API key: export TAVILY_API_KEY=your_key_here");
      System.exit(1);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && args[0].equals("-showcp")) {
      System.out.println("Classpath: " + System.getProperty("java.class.path"));
      return;
    }
    validateEnvironmentVariables();
    System.out.println("Starting ReAct Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create()) {
      try (AutoCloseable ui = UI.start(ipc)) {
        ReActModule module = new ReActModule();
        ipc.launchModule(module, new LaunchConfig(1, 1));

        String moduleName = module.getModuleName();
        AgentManager agentManager = AgentManager.create(ipc, moduleName);
        AgentClient agent = agentManager.getAgentClient("ReActAgent");

        System.out.println("This agent can search the web to answer your questions.");
        System.out.println();
        System.out.print("Ask your question (agent has web search access): ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput = reader.readLine();
        Object result = agent.invoke(Arrays.asList(userInput));

        System.out.println("\nAgent: " + result);
        System.out.println();
      }
    }
  }
}

```

---

### ReActModule

**File:** `src/main/java/com/rpl/agent/react/ReActModule.java`

```java
package com.rpl.agent.react;

import com.rpl.agentorama.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.*;

/**
 * ReAct (Reasoning and Acting) Agent Module using agent-o-rama framework.
 *
 * <p>This module implements a conversational agent that can reason about user queries and take
 * actions using web search tools. It demonstrates the ReAct pattern where the agent alternates
 * between reasoning about what to do and taking actions.
 */
public class ReActModule extends AgentModule {

  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));
    topology.declareAgentObject("tavily-api-key", System.getenv("TAVILY_API_KEY"));

    topology.declareAgentObjectBuilder(
        "openai",
        (AgentObjectSetup setup) -> {
          String apiKey = (String) setup.getAgentObject("openai-api-key");
          return OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-4o-mini").build();
        });

    topology.declareAgentObjectBuilder(
        "tavily",
        (AgentObjectSetup setup) -> {
          String apiKey = (String) setup.getAgentObject("tavily-api-key");
          return TavilyWebSearchEngine.builder()
              .apiKey(apiKey)
              .excludeDomains(Arrays.asList("en.wikipedia.org"))
              .build();
        });

    topology.newToolsAgent("tools", ToolsFactory.createTools());

    topology.newAgent("ReActAgent")
            .node("chat", "chat", (AgentNode agentNode, List<Object> inputMessages) -> {
              // allow messages to be strings so this can be invoked more easily from the UI
              List<ChatMessage> messages = new ArrayList();
              for(Object m: inputMessages) {
                if(m instanceof String) {
                  messages.add(new UserMessage((String) m));
                } else {
                  messages.add((ChatMessage) m);
                }
              }
              ChatModel openai = agentNode.getAgentObject("openai");
              AgentClient tools = agentNode.getAgentClient("tools");

              ChatRequest request =
                  ChatRequest.builder()
                      .messages(messages)
                      .toolSpecifications(ToolsFactory.createToolSpecifications())
                      .build();
              ChatResponse response = openai.chat(request);
              AiMessage aiMessage = response.aiMessage();
              List<ToolExecutionRequest> toolCalls = aiMessage.toolExecutionRequests();

              if (toolCalls != null && !toolCalls.isEmpty()) {
                List<ToolExecutionResultMessage> toolResults = tools.invoke(toolCalls);

                List<ChatMessage> nextMessages = new ArrayList<>(messages);
                nextMessages.add(aiMessage);
                nextMessages.addAll(toolResults);
                agentNode.emit("chat", nextMessages);
              } else {
                agentNode.result(aiMessage.text());
              }
            });
  }
}

```

---

### ToolsFactory

**File:** `src/main/java/com/rpl/agent/react/ToolsFactory.java`

```java
package com.rpl.agent.react;

import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.ToolInfo;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory class for creating tools used by the ReAct agent.
 *
 * This class provides web search capabilities using the Tavily search engine, allowing the agent
 * to search for information on the web and incorporate the results into its reasoning process.
 */
public class ToolsFactory {

  public static List<ToolInfo> createTools() {
    return Arrays.asList(createTavilySearchTool());
  }

  public static List<ToolSpecification> createToolSpecifications() {
    return createTools().stream().map(ToolInfo::getToolSpecification).collect(Collectors.toList());
  }

  private static ToolInfo createTavilySearchTool() {
    ToolSpecification spec =
        ToolSpecification.builder()
            .name("tavily")
            .description("Search the web")
            .parameters(
                JsonObjectSchema.builder()
                    .addStringProperty("terms", "The terms to search for")
                    .build())
            .build();

    return ToolInfo.createWithContext(spec, ToolsFactory::executeTavilySearch);
  }

  /**
   * Executes a web search using the Tavily search engine.
   *
   * @param agentNode The agent node for accessing shared objects
   * @param unused Unused parameter for compatibility
   * @param arguments The tool arguments containing search terms
   * @return Search results as concatenated text
   */
  private static String executeTavilySearch(
      AgentNode agentNode, Object unused, Map<String, Object> arguments) {
    try {
      String terms = (String) arguments.get("terms");
      if (terms == null || terms.trim().isEmpty()) {
        return "Error: No search terms provided";
      }
      TavilyWebSearchEngine tavily = agentNode.getAgentObject("tavily");
      WebSearchRequest searchRequest = WebSearchRequest.from(terms, 3);
      List<Document> searchResults = tavily.search(searchRequest).toDocuments();

      if (searchResults.isEmpty()) {
        return "No search results found for: " + terms;
      }
      return searchResults.stream().map(Document::text).collect(Collectors.joining("\n---\n"));
    } catch (Exception e) {
      return "Error during search: " + e.getMessage();
    }
  }
}

```

---

### ResearchAgentExample

**File:** `src/main/java/com/rpl/agent/research/ResearchAgentExample.java`

```java
package com.rpl.agent.research;

import com.rpl.agentorama.*;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Example demonstrating the Research Agent Module.
 *
 * This example shows how to use the ResearchAgentModule to conduct
 * multi-step research with analyst personas, web search, and report generation.
 */
public class ResearchAgentExample {

  public static void main(String[] args) throws Exception {
    System.out.println("Starting Research Agent Example...");

    try (InProcessCluster ipc = InProcessCluster.create();
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      try (AutoCloseable ui = UI.start(ipc)) {
        ResearchAgentModule module = new ResearchAgentModule();
        ipc.launchModule(module, new LaunchConfig(4, 2));

        String moduleName = module.getModuleName();
        AgentManager manager = AgentManager.create(ipc, moduleName);
        AgentClient researcher = manager.getAgentClient("researcher");

        System.out.print("Enter a topic: ");
        System.out.flush();
        String topic = reader.readLine();
        System.out.println();

        Map<String, Object> input = new HashMap<>();
        input.put("topic", topic);
        AgentInvoke invoke = researcher.initiate("", input);

        Object step = researcher.nextStep(invoke);
        String finalResult = null;
        while (step != null) {
          if (step instanceof HumanInputRequest) {
            HumanInputRequest request = (HumanInputRequest) step;
            System.out.println(request.getPrompt());
            System.out.print(">> ");
            System.out.flush();

            String response = reader.readLine();
            researcher.provideHumanInput(request, response);
            System.out.println();
          } else {
            System.out.println("Final Research Report:");
            System.out.println("====================");
            System.out.println(((AgentComplete) step).getResult());
            break;
          }

          step = researcher.nextStep(invoke);
        }
      }
    }
  }
}

```

---

### ResearchAgentModule

**File:** `src/main/java/com/rpl/agent/research/ResearchAgentModule.java`

```java
package com.rpl.agent.research;

import com.rpl.agentorama.*;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.*;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.openai.*;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import java.net.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.*;

public class ResearchAgentModule extends AgentModule {
  private static final String ANALYST_INSTRUCTIONS =
    "You are tasked with creating a set of AI analyst personas. Follow these instructions carefully:\n\n" +
    "1. First, review the research topic: %s\n\n" +
    "2. Examine any editorial feedback that has been optionally provided to guide creation of the analysts:\n\n" +
    "%s\n\n" +
    "3. Determine the most interesting themes based upon documents and / or feedback above.\n\n" +
    "4. Pick the top %s themes.\n\n" +
    "5. Assign one analyst to each theme.";

  private static final String GENERATE_QUESTION_INSTRUCTIONS =
    "You are an analyst tasked with interviewing an expert to learn about a specific topic.\n\n" +
    "Your goal is boil down to interesting and specific insights related to your topic.\n\n" +
    "1. Interesting: Insights that people will find surprising or non-obvious.\n\n" +
    "2. Specific: Insights that avoid generalities and include specific examples from the expert.\n\n" +
    "Here is your topic of focus and set of goals: %s\n\n" +
    "Begin by introducing yourself using a name that fits your persona, and then ask your question.\n\n" +
    "Continue to ask questions to drill down and refine your understanding of the topic.\n\n" +
    "When you are satisfied with your understanding, complete the interview with: \"Thank you so much for your help!\"\n\n" +
    "Remember to stay in character throughout your response, reflecting the persona and goals provided to you.";

  private static final String SEARCH_INSTRUCTIONS =
    "You will be given a conversation between an analyst and an expert.\n\n" +
    "Your goal is to generate a well-structured query for use in retrieval and / or web-search related to the conversation.\n\n" +
    "First, analyze the full conversation.\n\n" +
    "Pay particular attention to the final question posed by the analyst.\n\n" +
    "Convert this final question into a well-structured web search query no more than 400 characters.";

  private static final String WEB_DOCUMENT_TEMPLATE =
    "<Document href=\"%s\">\n%s\n</Document>";

  private static final String WIKIPEDIA_DOCUMENT_TEMPLATE =
    "<Document source=\"%s\" page=\"%s\">\n%s\n</Document>";

  private static final String ANSWER_INSTRUCTIONS =
    "You are an expert being interviewed by an analyst.\n\n" +
    "Here is analyst area of focus: %s.\n\n" +
    "You goal is to answer a question posed by the interviewer.\n\n" +
    "To answer question, use this context:\n\n" +
    "%s\n\n" +
    "When answering questions, follow these guidelines:\n\n" +
    "1. Use only the information provided in the context.\n\n" +
    "2. Do not introduce external information or make assumptions beyond what is explicitly stated in the context.\n\n" +
    "3. The context contain sources at the topic of each individual document.\n\n" +
    "4. Include these sources your answer next to any relevant statements. For example, for source # 1 use [1].\n\n" +
    "5. List your sources in order at the bottom of your answer. [1] Source 1, [2] Source 2, etc\n\n" +
    "6. If the source is: <Document source=\"assistant/docs/llama3_1.pdf\" page=\"7\"/>' then just list:\n\n" +
    "[1] assistant/docs/llama3_1.pdf, page 7\n\n" +
    "And skip the addition of the brackets as well as the Document source preamble in your citation.";

  private static final String SECTION_WRITER_INSTRUCTIONS =
    "You are an expert technical writer.\n\n" +
    "Your task is to create a short, easily digestible section of a report based on a set of source documents.\n\n" +
    "1. Analyze the content of the source documents:\n" +
    "- The name of each source document is at the start of the document, with the <Document tag.\n\n" +
    "2. Create a report structure using markdown formatting:\n" +
    "- Use ## for the section title\n" +
    "- Use ### for sub-section headers\n\n" +
    "3. Write the report following this structure:\n" +
    "a. Title (## header)\n" +
    "b. Summary (### header)\n" +
    "c. Sources (### header)\n\n" +
    "4. Make your title engaging based upon the focus area of the analyst:\n" +
    "%s\n\n" +
    "5. For the summary section:\n" +
    "- Set up summary with general background / context related to the focus area of the analyst\n" +
    "- Emphasize what is novel, interesting, or surprising about insights gathered from the interview\n" +
    "- Create a numbered list of source documents, as you use them\n" +
    "- Do not mention the names of interviewers or experts\n" +
    "- Aim for approximately 400 words maximum\n" +
    "- Use numbered sources in your report (e.g., [1], [2]) based on information from source documents\n\n" +
    "6. In the Sources section:\n" +
    "- Include all sources used in your report\n" +
    "- Provide full links to relevant websites or specific document paths\n" +
    "- Separate each source by a newline. Use two spaces at the end of each line to create a newline in Markdown.\n" +
    "- It will look like:\n\n" +
    "### Sources\n" +
    "[1] Link or Document name\n" +
    "[2] Link or Document name\n\n" +
    "7. Be sure to combine sources. For example this is not correct:\n\n" +
    "[3] https://ai.meta.com/blog/meta-llama-3-1/\n" +
    "[4] https://ai.meta.com/blog/meta-llama-3-1/\n\n" +
    "There should be no redundant sources. It should simply be:\n\n" +
    "[3] https://ai.meta.com/blog/meta-llama-3-1/\n\n" +
    "8. Final review:\n" +
    "- Ensure the report follows the required structure\n" +
    "- Include no preamble before the title of the report\n" +
    "- Check that all guidelines have been followed";

  private static final String REPORT_WRITER_INSTRUCTIONS =
    "You are a technical writer creating a report on this overall topic:\n\n" +
    "%s\n\n" +
    "You have a team of analysts. Each analyst has done two things:\n\n" +
    "1. They conducted an interview with an expert on a specific sub-topic.\n" +
    "2. They write up their finding into a memo.\n\n" +
    "Your task:\n\n" +
    "1. You will be given a collection of memos from your analysts.\n" +
    "2. Think carefully about the insights from each memo.\n" +
    "3. Consolidate these into a crisp overall summary that ties together the central ideas from all of the memos.\n" +
    "4. Summarize the central points in each memo into a cohesive single narrative.\n\n" +
    "To format your report:\n\n" +
    "1. Use markdown formatting.\n" +
    "2. Include no pre-amble for the report.\n" +
    "3. Use no sub-heading.\n" +
    "4. Start your report with a single title header: ## Insights\n" +
    "5. Do not mention any analyst names in your report.\n" +
    "6. Preserve any citations in the memos, which will be annotated in brackets, for example [1] or [2].\n" +
    "7. Create a final, consolidated list of sources and add to a Sources section with the `## Sources` header.\n" +
    "8. List your sources in order and do not repeat.\n\n" +
    "[1] Source 1\n" +
    "[2] Source 2\n\n" +
    "Here are the memos from your analysts to build your report from:\n\n" +
    "%s";

  private static final String INTRO_CONCLUSION_INSTRUCTIONS =
    "You are a technical writer finishing a report on %s\n\n" +
    "You will be given all of the sections of the report.\n\n" +
    "You job is to write a crisp and compelling introduction or conclusion section.\n\n" +
    "The user will instruct you whether to write the introduction or conclusion.\n\n" +
    "Include no pre-amble for either section.\n\n" +
    "Target around 100 words, crisply previewing (for introduction) or recapping (for conclusion) all of the sections of the report.\n\n" +
    "Use markdown formatting.\n\n" +
    "For your introduction, create a compelling title and use the # header for the title.\n\n" +
    "For your introduction, use ## Introduction as the section header.\n\n" +
    "For your conclusion, use ## Conclusion as the section header.\n\n" +
    "Here are the sections to reflect on for writing: %s";

  private static final HttpClient httpClient = HttpClient.newHttpClient();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final JsonSchema ANALYST_RESPONSE_SCHEMA = JsonSchema.builder()
    .name("analysts")
    .rootElement(JsonObjectSchema.builder()
      .addProperty("analysts", JsonArraySchema.builder()
        .items(JsonObjectSchema.builder()
          .addStringProperty("name", "Name of the analyst")
          .addStringProperty("role", "Role of the analyst in the context of the topic")
          .addStringProperty("affiliation", "Primary affiliation of the analyst")
          .addStringProperty("description", "Description of the analyst focus, concerns, and motives")
          .build())
        .build())
      .build())
    .build();

  @Override
  protected void defineAgents(AgentTopology topology) {
    topology.declareAgentObject("openai-api-key", System.getenv("OPENAI_API_KEY"));
    topology.declareAgentObject("tavily-api-key", System.getenv("TAVILY_API_KEY"));
    topology.declareAgentObjectBuilder("openai", setup ->
      OpenAiStreamingChatModel.builder()
        .apiKey(setup.getAgentObject("openai-api-key"))
        .modelName("gpt-4o-mini")
        .build()
    );
    topology.declareAgentObjectBuilder("openai-non-streaming", setup ->
      OpenAiChatModel.builder()
        .apiKey(setup.getAgentObject("openai-api-key"))
        .modelName("gpt-4o-mini")
        .build()
    );
    topology.declareAgentObjectBuilder("tavily", setup ->
      TavilyWebSearchEngine.builder()
        .apiKey(setup.getAgentObject("tavily-api-key"))
        .excludeDomains(Arrays.asList("en.wikipedia.org"))
        .timeout(Duration.ofMinutes(1))
        .build()
    );
    topology.newAgent("researcher")
      .node("create-analysts", "feedback", (AgentNode agentNode, String humanFeedback, Map<String, Object> options) -> {
        Map<String, Object> config = new HashMap<>();
        config.put("max-analysts", 4);
        config.put("max-turns", 2);
        config.putAll(options);

        String topic = (String) config.get("topic");
        Integer maxAnalysts = (Integer) config.get("max-analysts");
        ChatModel openai = agentNode.getAgentObject("openai-non-streaming");
        String instructions = String.format(ANALYST_INSTRUCTIONS, topic, humanFeedback, maxAnalysts);
        List<ChatMessage> messages = Arrays.asList(new SystemMessage(instructions));
        ResponseFormat responseFormat = ResponseFormat.builder()
          .type(ResponseFormatType.JSON)
          .jsonSchema(ANALYST_RESPONSE_SCHEMA)
          .build();
        ChatRequest chatRequest = ChatRequest.builder()
          .messages(messages)
          .responseFormat(responseFormat)
          .build();

        String response = openai.chat(chatRequest).aiMessage().text();
        List<Map<String, Object>> analysts = parseAnalystsResponse(response);
        agentNode.emit("feedback", analysts, config);
      })
      .node("feedback", Arrays.asList("create-analysts", "questions"), (AgentNode agentNode, List<Map<String, Object>> analysts, Map<String, Object> options) -> {
        String prompt = "Do you have any feedback on this set of analysts? Answer 'yes' or 'no'.\n\n" +
          analysts.stream()
            .map(analyst -> analyst.toString())
            .collect(Collectors.joining("\n"));

        boolean hasFeedback = humanYes(agentNode, prompt);
        if (hasFeedback) {
          String feedback = agentNode.getHumanInput("What is your feedback?");
          agentNode.emit("create-analysts", feedback, options);
        } else {
          agentNode.emit("questions", analysts, options);
        }
      })
      .aggStartNode("questions", "generate-question", (AgentNode agentNode, List<Map<String, Object>> analysts, Map<String, Object> config) -> {
        for (Map<String, Object> analyst : analysts) {
          String persona = formatAnalystPersona(analyst);
          agentNode.emit("generate-question", persona, new ArrayList<ChatMessage>(), (Integer) config.get("max-turns"));
        }
        return config;
      })
      .aggStartNode("generate-question", Arrays.asList("search-web", "search-wikipedia"), (AgentNode agentNode, String persona, List<ChatMessage> messages, Integer maxTurns) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String instructions = String.format(GENERATE_QUESTION_INSTRUCTIONS, persona);

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage(instructions));
        chatMessages.addAll(messages);

        AiMessage question = openai.chat(chatMessages).aiMessage();
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(question);

        String searchQuery = generateSearchQuery(openai, newMessages);

        agentNode.emit("search-web", searchQuery);
        agentNode.emit("search-wikipedia", searchQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("persona", persona);
        result.put("messages", newMessages);
        result.put("max-turns", maxTurns);
        return result;
      })
      .node("search-web", "agg-research", (AgentNode agentNode, String searchQuery) -> {
        TavilyWebSearchEngine tavily = agentNode.getAgentObject("tavily");
        List<Document> docs = tavily.search(WebSearchRequest.from(searchQuery, 3)).toDocuments();
        for (Document doc : docs) {
          String url = doc.metadata().getString("url");
          String content = doc.text();
          String formattedDoc = String.format(WEB_DOCUMENT_TEMPLATE, url, content);
          agentNode.emit("agg-research", formattedDoc);
        }
      })
      .node("search-wikipedia", "agg-research", (AgentNode agentNode, String searchQuery) -> {
        try {
          List<Map<String, Object>> docs = wikipediaLoader(searchQuery.replace("\"", ""), 2);
          for (Map<String, Object> doc : docs) {
            String source = (String) doc.get("source");
            String page = (String) doc.get("page");
            String content = (String) doc.get("content");
            String formattedDoc = String.format(WIKIPEDIA_DOCUMENT_TEMPLATE, source, page, content);
            agentNode.emit("agg-research", formattedDoc);
          }
        } catch (Exception e) {
          throw new RuntimeException("Wikipedia search failed", e);
        }
      })
      .aggNode("agg-research", Arrays.asList("generate-question", "write-section"), BuiltIn.LIST_AGG, (AgentNode agentNode, List<String> searches, Map<String, Object> context) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String persona = (String) context.get("persona");
        List<ChatMessage> messages = (List<ChatMessage>) context.get("messages");
        Integer maxTurns = (Integer) context.get("max-turns");

        String searchContext = String.join("\n---\n", searches);
        String instructions = String.format(ANSWER_INSTRUCTIONS, persona, searchContext);

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage(instructions));
        chatMessages.addAll(messages);

        String answer = openai.chat(chatMessages).aiMessage().text();
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(new UserMessage("expert", answer));

        long expertTurns = newMessages.stream()
          .filter(msg -> msg instanceof UserMessage)
          .map(msg -> (UserMessage) msg)
          .filter(msg -> "expert".equals(msg.name()))
          .count();

        if (expertTurns >= maxTurns) {
          agentNode.emit("write-section", persona, newMessages, searchContext);
        } else {
          agentNode.emit("generate-question", persona, newMessages, maxTurns);
        }
      })
      .node("write-section", "agg-sections", (AgentNode agentNode, String persona, List<ChatMessage> messages, String context) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String interview = extractInterview(messages);
        String instructions = String.format(SECTION_WRITER_INSTRUCTIONS, persona);

        List<ChatMessage> chatMessages = Arrays.asList(
          new SystemMessage(instructions),
          new UserMessage("Here is the interview:\n" + interview),
          new UserMessage("Here are the sources:\n" + context)
        );

        String section = openai.chat(chatMessages).aiMessage().text();
        agentNode.emit("agg-sections", section);
      })
      .aggNode("agg-sections", "begin-report", BuiltIn.LIST_AGG, (AgentNode agentNode, List<String> sections, Map<String, Object> context) -> {
        String topic = (String) context.get("topic");
        agentNode.emit("begin-report", sections, topic);
      })
      .aggStartNode("begin-report", Arrays.asList("write-report", "write-intro", "write-conclusion"), (AgentNode agentNode, List<String> sections, String topic) -> {
        String sectionsText = String.join("\n\n", sections);
        agentNode.emit("write-report", sectionsText, topic);
        agentNode.emit("write-intro", sectionsText, topic);
        agentNode.emit("write-conclusion", sectionsText, topic);
        return null;
      })
      .node("write-report", "finish-report", (AgentNode agentNode, String sections, String topic) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String instructions = String.format(REPORT_WRITER_INSTRUCTIONS, topic, sections);

        List<ChatMessage> chatMessages = Arrays.asList(
          new SystemMessage(instructions),
          new UserMessage("Write a report based upon these memos.")
        );

        String report = openai.chat(chatMessages).aiMessage().text();
        agentNode.emit("finish-report", "report", report);
      })
      .node("write-intro", "finish-report", (AgentNode agentNode, String sections, String topic) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String instructions = String.format(INTRO_CONCLUSION_INSTRUCTIONS, topic, sections);
        List<ChatMessage> chatMessages = Arrays.asList(
          new SystemMessage(instructions),
          new UserMessage("Write the report introduction"));
        String intro = openai.chat(chatMessages).aiMessage().text();
        agentNode.emit("finish-report", "intro", intro);
      })
      .node("write-conclusion", "finish-report", (AgentNode agentNode, String sections, String topic) -> {
        ChatModel openai = agentNode.getAgentObject("openai");
        String instructions = String.format(INTRO_CONCLUSION_INSTRUCTIONS, topic, sections);
        List<ChatMessage> chatMessages = Arrays.asList(
          new SystemMessage(instructions),
          new UserMessage("Write the report conclusion"));
        String conclusion = openai.chat(chatMessages).aiMessage().text();
        agentNode.emit("finish-report", "conclusion", conclusion);
      })
      .aggNode("finish-report", null, BuiltIn.MAP_AGG, (AgentNode agentNode, Map<String, String> reportParts, Object startNodeResult) -> {
        String report = reportParts.get("report");
        String intro = reportParts.get("intro");
        String conclusion = reportParts.get("conclusion");

        report = report.replaceAll("## Insights", "");
        String[] parts = report.split("## Sources");
        String reportBody = parts[0];
        String sources = parts.length > 1 ? parts[1] : null;

        StringBuilder finalReportBuilder = new StringBuilder();
        if (intro != null) {
          finalReportBuilder.append(intro).append("\n\n---\n");
        }
        finalReportBuilder.append(reportBody);
        if (conclusion != null) {
          finalReportBuilder.append("\n---\n\n").append(conclusion);
        }
        if (sources != null) {
          finalReportBuilder.append("\n\n## Sources").append(sources);
        }

        String finalReport = finalReportBuilder.toString();
        agentNode.result(finalReport);
      });
  }

  private static List<Map<String, Object>> parseAnalystsResponse(String response) {
    try {
      JsonNode root = objectMapper.readTree(response);
      JsonNode analystsArray = root.path("analysts");
      if (!analystsArray.isArray()) {
        throw new RuntimeException("Invalid JSON schema: 'analysts' field must be an array");
      }
      List<Map<String, Object>> analysts = new ArrayList<>();
      for (JsonNode analystNode : analystsArray) {
        if (!analystNode.has("name") || !analystNode.has("role") ||
            !analystNode.has("affiliation") || !analystNode.has("description")) {
          throw new RuntimeException("Invalid JSON schema: analyst missing required fields (name, role, affiliation, description)");
        }
        Map<String, Object> analyst = new HashMap<>();
        analyst.put("name", analystNode.path("name").asText());
        analyst.put("role", analystNode.path("role").asText());
        analyst.put("affiliation", analystNode.path("affiliation").asText());
        analyst.put("description", analystNode.path("description").asText());
        analysts.add(analyst);
      }

      if (analysts.isEmpty()) {
        throw new RuntimeException("No valid analysts found in response");
      }
      return analysts;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse analysts response: " + e.getMessage(), e);
    }
  }

  private static String formatAnalystPersona(Map<String, Object> analyst) {
    String name = analyst.get("name") != null ? analyst.get("name").toString() : "Unknown";
    String role = analyst.get("role") != null ? analyst.get("role").toString() : "Unknown";
    String affiliation = analyst.get("affiliation") != null ? analyst.get("affiliation").toString() : "Unknown";
    String description = analyst.get("description") != null ? analyst.get("description").toString() : "No description available";
    return String.format("Name: %s\nRole: %s\nAffiliation: %s\nDescription: %s",
      name, role, affiliation, description);
  }

  private static boolean humanYes(AgentNode agentNode, String prompt) {
    String currentPrompt = prompt;
    while (true) {
      String response = agentNode.getHumanInput(currentPrompt);
      if ("yes".equalsIgnoreCase(response.trim())) {
        return true;
      } else if ("no".equalsIgnoreCase(response.trim())) {
        return false;
      } else {
        currentPrompt = "Please answer 'yes' or 'no'.";
      }
    }
  }

  private static String generateSearchQuery(ChatModel openai, List<ChatMessage> messages) {
    List<ChatMessage> chatMessages = new ArrayList<>();
    chatMessages.add(new SystemMessage(SEARCH_INSTRUCTIONS));
    chatMessages.addAll(messages);
    int iters = 0;
    while (iters < 3) {
      String query = openai.chat(chatMessages).aiMessage().text();
      if (query.length() <= 400) return query;
      chatMessages.add(new UserMessage(String.format("You last generated: %s\nTry again and keep the query under 400 chars.", query)));
      iters++;
    }
    throw new RuntimeException("Failed to generate search query <= 400 chars");
  }

  private static String extractInterview(List<ChatMessage> messages) {
    StringBuilder interview = new StringBuilder();
    for (ChatMessage msg : messages) {
      if (msg instanceof UserMessage) {
        UserMessage userMsg = (UserMessage) msg;
        String prefix = "expert".equals(userMsg.name()) ? "Expert: " : "Human: ";
        interview.append(prefix).append(userMsg.singleText()).append("\n\n");
      } else if (msg instanceof AiMessage) {
        AiMessage aiMsg = (AiMessage) msg;
        interview.append("AI: ").append(aiMsg.text()).append("\n\n");
      }
    }
    return interview.toString();
  }

  private static List<Map<String, Object>> wikipediaLoader(String query, int maxDocs) throws IOException, InterruptedException {
    List<String> titles = wikiSearch(query);
    List<Map<String, Object>> docs = new ArrayList<>();
    if (titles == null || titles.isEmpty()) {
      return docs;
    }
    int docCount = Math.min(titles.size(), maxDocs);
    for (int i = 0; i < docCount; i++) {
      String title = titles.get(i);
      Map<String, Object> doc = wikiExtract(title);
      docs.add(doc);
    }
    return docs;
  }

  private static List<String> wikiSearch(String query) throws IOException, InterruptedException {
    String url = "https://en.wikipedia.org/w/api.php" +
      "?action=query&list=search&format=json&srsearch=" +
      URLEncoder.encode(query, StandardCharsets.UTF_8);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(java.net.URI.create(url))
      .header("User-Agent", "Agent-o-rama/1.0 (Research Agent)")
      .header("Accept", "application/json")
      .GET()
      .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 200) {
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode queryNode = root.path("query");
      JsonNode searchResults = queryNode.path("search");

      List<String> titles = new ArrayList<>();
      if (searchResults.isArray()) {
        for (JsonNode result : searchResults) {
          String title = result.path("title").asText();
          if (title != null && !title.isEmpty()) {
            titles.add(title);
          }
        }
      }
      return titles;
    } else {
      throw new RuntimeException("Wikipedia search failed with status: " + response.statusCode());
    }
  }

  private static Map<String, Object> wikiExtract(String title) throws IOException, InterruptedException {
    String url = "https://en.wikipedia.org/w/api.php" +
      "?action=query&prop=extracts&explaintext=true&format=json&titles=" +
      URLEncoder.encode(title, StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder()
      .uri(java.net.URI.create(url))
      .header("User-Agent", "Agent-o-rama/1.0 (Research Agent)")
      .header("Accept", "application/json")
      .GET()
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode queryNode = root.path("query");
      JsonNode pages = queryNode.path("pages");
      Map<String, Object> doc = new HashMap<>();
      if (pages.isObject() && pages.size() > 0) {
        JsonNode firstPage = pages.iterator().next();
        String extract = firstPage.path("extract").asText();
        doc.put("content", extract != null ? extract : "");
        doc.put("source", "https://en.wikipedia.org/wiki/" + title.replace(" ", "_"));
        doc.put("page", title);
      } else {
        doc.put("content", "");
        doc.put("source", "https://en.wikipedia.org/wiki/" + title.replace(" ", "_"));
        doc.put("page", title);
      }
      return doc;
    } else {
      throw new RuntimeException("Wikipedia extract failed with status: " + response.statusCode());
    }
  }
}

```

---

## Test Files

### AgentObjectsAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/AgentObjectsAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for AgentObjectsAgent demonstrating agent object functionality.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Static agent objects sharing across invocations
 *   <li>Dynamic agent object builders with dependencies
 *   <li>Thread-unsafe services working safely via pooling
 * </ul>
 */
public class AgentObjectsAgentTest {

  @Test
  public void testAgentObjectsAgent() throws Exception {
    // Tests that agent objects are properly shared and used
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      AgentObjectsAgent.AgentObjectsModule module = new AgentObjectsAgent.AgentObjectsModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AgentObjectsAgent");

      // Test single invocation
      String result = (String) agent.invoke("TestMessage");
      assertNotNull("Agent should return a result", result);
      assertTrue("Result should contain version", result.contains("v1.2.3"));
      assertTrue("Result should contain message", result.contains("TestMessage"));
      assertTrue("Result should contain counter", result.contains("#1"));
      assertTrue("Result should contain send-to", result.contains("alerts"));
    }
  }
}

```

---

### AggregationAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/AggregationAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Test class for AggregationAgent demonstrating fan-out/fan-in aggregation patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>aggStartNode distributing work to multiple parallel processors
 *   <li>aggNode collecting and combining results from multiple executions
 *   <li>Built-in LIST_AGG aggregator functionality
 *   <li>Fan-out/fan-in execution patterns with different chunk sizes
 * </ul>
 */
public class AggregationAgentTest {

  @Test
  public void testBasicAggregation() throws Exception {
    // Tests basic aggregation functionality with simple data
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      AggregationAgent.AggregationModule module = new AggregationAgent.AggregationModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AggregationAgent");

      // Test with simple data set
      List<Integer> testData = new ArrayList<>();
      for (int i = 1; i <= 10; i++) {
        testData.add(i); // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
      }

      Map<String, Object> request = new HashMap<>();
      request.put("data", testData);
      request.put("chunkSize", 3);

      Map<String, Object> result = (Map<String, Object>) agent.invoke(request);

      assertNotNull("Result should not be null", result);
      assertEquals("Should process all 10 items", 10, result.get("totalItems"));
      assertEquals("Should create 4 chunks", 4, result.get("chunksProcessed"));

      // Expected: chunks [1,2,3], [4,5,6], [7,8,9], [10]
      // Squared: [1,4,9], [16,25,36], [49,64,81], [100]
      // Sums: 14 + 77 + 194 + 100 = 385
      assertEquals("Total sum should be correct", 385, result.get("totalSum"));
    }
  }
}

```

---

### AsyncAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/AsyncAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for AsyncAgent demonstrating asynchronous agent invocation patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>agent.initiate: Starting agent execution asynchronously
 *   <li>agent.result: Getting results from async execution
 *   <li>AgentInvoke handles for tracking execution
 * </ul>
 */
public class AsyncAgentTest {

  @Test
  public void testAsyncAgent() throws Exception {
    // Tests basic async agent invocation and result retrieval
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      AsyncAgent.AsyncAgentModule module = new AsyncAgent.AsyncAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("AsyncAgent");

      // Test single async invocation
      AgentInvoke invoke = agent.initiate("TestTask");
      assertNotNull("AgentInvoke should not be null", invoke);

      String result = (String) agent.result(invoke);
      assertNotNull("Result should not be null", result);
      assertEquals("Task 'TestTask' completed successfully", result);
    }
  }
}

```

---

### BasicAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/BasicAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for BasicAgent demonstrating agent testing patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Setting up an in-process cluster for testing
 *   <li>Deploying and invoking agents in tests
 *   <li>Asserting on agent results
 *   <li>Testing with different input values
 * </ul>
 */
public class BasicAgentTest {

  @Test
  public void testBasicAgent() throws Exception {
    // Tests basic agent invocation with a single input
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      BasicAgent.BasicModule module = new BasicAgent.BasicModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("BasicAgent");

      // Test with single input
      String result = (String) agent.invoke("TestUser");
      assertNotNull("Agent should return a result", result);
      assertEquals("Welcome to agent-o-rama, TestUser!", result);
    }
  }
}

```

---

### DocumentStoreAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/DocumentStoreAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Test class for DocumentStoreAgent demonstrating structured multi-field data storage.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>declareDocumentStore: Creating document storage with multiple fields
 *   <li>getStore: Accessing document stores from agent nodes
 *   <li>Store operations: getDocumentField, putDocumentField, updateDocumentField,
 *       containsDocumentField
 *   <li>HashMap usage for request and response data structures
 * </ul>
 */
public class DocumentStoreAgentTest {

  @Test
  public void testDocumentStoreAgent() throws Exception {
    // Tests document store operations with structured multi-field data using HashMap
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      DocumentStoreAgent.DocumentStoreModule module = new DocumentStoreAgent.DocumentStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("DocumentStoreAgent");

      // Test creating a user profile
      Map<String, Object> request = new HashMap<>();
      request.put("userId", "test-user");
      Map<String, Object> updates = new HashMap<>();
      updates.put("name", "Test User");
      updates.put("age", 30L);
      Map<String, Object> prefs = new HashMap<>();
      prefs.put("theme", "dark");
      prefs.put("newsletter", true);
      updates.put("preferences", prefs);
      request.put("profileUpdates", updates);

      Map<String, Object> result = (Map<String, Object>) agent.invoke(request);

      assertNotNull("Result should not be null", result);
      assertEquals("User ID should match", "test-user", result.get("userId"));
      assertEquals("Name should match", "Test User", result.get("name"));
      assertEquals("Age should match", 30L, result.get("age"));

      Map<String, Object> resultPrefs = (Map<String, Object>) result.get("preferences");
      assertNotNull("Preferences should not be null", resultPrefs);
      assertEquals("Theme should match", "dark", resultPrefs.get("theme"));
      assertEquals("Newsletter should match", true, resultPrefs.get("newsletter"));
    }
  }
}

```

---

### HumanInputAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/HumanInputAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for HumanInputAgent demonstrating human input integration patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>getHumanInput: Requesting input from human users
 *   <li>agent.nextStep: Handling human input requests in execution flow
 *   <li>provideHumanInput: Supplying responses to human input requests
 * </ul>
 *
 * <p>Note: This test uses a test API key and expects the agent to fail gracefully without a real
 * API key.
 */
public class HumanInputAgentTest {

  @Test
  public void testHumanInputAgent() throws Exception {
    // Tests human input agent with mock API key (will fail gracefully)
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Set a test API key to avoid null pointer
      System.setProperty("OPENAI_API_KEY", "test-key");

      try {
        // Deploy the agent module
        HumanInputAgent.HumanInputModule module = new HumanInputAgent.HumanInputModule();
        ipc.launchModule(module, new LaunchConfig(1, 1));

        // Get agent manager and client
        String moduleName = module.getModuleName();
        AgentManager manager = AgentManager.create(ipc, moduleName);
        AgentClient agent = manager.getAgentClient("HumanInputAgent");

        // Start agent execution
        AgentInvoke invoke = agent.initiate("Hello, how are you?");
        assertNotNull("Agent invoke should not be null", invoke);

        // The agent will likely fail due to invalid API key, but we can test the structure
        // In a real test environment, you would use a valid API key or mock the OpenAI client

      } catch (Exception e) {
        // Expected to fail with test API key - this demonstrates the agent structure
        assertTrue(
            "Should fail with authentication error",
            e.getMessage().contains("HTTP")
                || e.getMessage().contains("auth")
                || e.getMessage().contains("API")
                || e.getMessage().contains("key"));
      } finally {
        // Clean up test property
        System.clearProperty("OPENAI_API_KEY");
      }
    }
  }
}

```

---

### KeyValueStoreAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/KeyValueStoreAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.Map;
import org.junit.Test;

/**
 * Test class for KeyValueStoreAgent demonstrating persistent state management.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>declareKeyValueStore: Creating persistent key-value storage
 *   <li>getStore: Accessing stores from agent nodes
 *   <li>Store operations: get, put, update for persistent state
 *   <li>HashMap usage for request and response data structures
 * </ul>
 */
public class KeyValueStoreAgentTest {

  @Test
  public void testKeyValueStoreAgent() throws Exception {
    // Tests basic key-value store operations using HashMap
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      KeyValueStoreAgent.KeyValueStoreModule module = new KeyValueStoreAgent.KeyValueStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("KeyValueStoreAgent");

      // Test SET operation
      Map<String, Object> setRequest =
          KeyValueStoreAgent.createCounterRequest(
              "test-counter", KeyValueStoreAgent.Operation.SET, 42L);
      Map<String, Object> setResult = (Map<String, Object>) agent.invoke(setRequest);

      assertNotNull("Set result should not be null", setResult);
      assertEquals("Action should be set", "set", setResult.get("action"));
      assertEquals("Counter name should match", "test-counter", setResult.get("counter"));
      assertEquals("Value should be 42", 42L, setResult.get("value"));

      // Test GET operation
      Map<String, Object> getRequest =
          KeyValueStoreAgent.createCounterRequest(
              "test-counter", KeyValueStoreAgent.Operation.GET, null);
      Map<String, Object> getResult = (Map<String, Object>) agent.invoke(getRequest);

      assertNotNull("Get result should not be null", getResult);
      assertEquals("Action should be get", "get", getResult.get("action"));
      assertEquals("Counter name should match", "test-counter", getResult.get("counter"));
      assertEquals("Value should be 42", 42L, getResult.get("value"));
    }
  }
}

```

---

### LangChain4jAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/LangChain4jAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for LangChain4jAgent demonstrating AI model integration.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>LangChain4j integration with agent framework
 *   <li>AI model agent object configuration
 *   <li>Chat model invocation from agent nodes
 * </ul>
 *
 * <p>Note: This test uses a test API key and expects the agent to fail gracefully without a real
 * API key.
 */
public class LangChain4jAgentTest {

  @Test
  public void testLangChain4jAgent() throws Exception {
    // Tests LangChain4j agent with mock API key (will fail gracefully)
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Set a test API key to avoid null pointer
      System.setProperty("OPENAI_API_KEY", "test-key");

      try {
        // Deploy the agent module
        LangChain4jAgent.LangChain4jModule module = new LangChain4jAgent.LangChain4jModule();
        ipc.launchModule(module, new LaunchConfig(1, 1));

        // Get agent manager and client
        String moduleName = module.getModuleName();
        AgentManager manager = AgentManager.create(ipc, moduleName);
        AgentClient agent = manager.getAgentClient("LangChain4jAgent");

        // Start agent execution
        String result = (String) agent.invoke("What is 2+2?");
        // This will likely fail with test API key, but we can test the structure

      } catch (Exception e) {
        // Expected to fail with test API key
        assertTrue(
            "Should fail with authentication or API error",
            e.getMessage().contains("HTTP")
                || e.getMessage().contains("auth")
                || e.getMessage().contains("API")
                || e.getMessage().contains("key")
                || e.getMessage().contains("unauthorized"));
      } finally {
        System.clearProperty("OPENAI_API_KEY");
      }
    }
  }
}

```

---

### MirrorAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/MirrorAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for MirrorAgent demonstrating cross-module agent invocation testing.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Deploying multiple modules to IPC
 *   <li>Testing cross-module agent invocation
 *   <li>Verifying mirror agent behavior
 * </ul>
 */
public class MirrorAgentTest {

  @Test
  public void testMirrorAgent() throws Exception {
    // Tests cross-module agent invocation via mirror agent
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy GreeterModule first
      MirrorAgent.GreeterModule greeterModule = new MirrorAgent.GreeterModule();
      ipc.launchModule(greeterModule, new LaunchConfig(1, 1));

      // Deploy MirrorModule with reference to GreeterModule
      MirrorAgent.MirrorModule mirrorModule = new MirrorAgent.MirrorModule();
      ipc.launchModule(mirrorModule, new LaunchConfig(1, 1));

      // Get agent manager and client for MirrorModule
      String mirrorModuleName = mirrorModule.getModuleName();
      AgentManager manager = AgentManager.create(ipc, mirrorModuleName);
      AgentClient mirrorAgent = manager.getAgentClient("MirrorAgent");

      // Test cross-module invocation
      String result = (String) mirrorAgent.invoke("TestUser");
      assertNotNull("Mirror agent should return a result", result);
      assertEquals("Mirror says: Hello, TestUser!", result);
    }
  }
}

```

---

### MultiAggAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/MultiAggAgentTest.java`

```java
package com.rpl.agent.basic;

import org.junit.Test;

/**
 * Test class for MultiAggAgent demonstrating custom aggregation logic.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Testing that the multi-agg agent main method runs without errors
 *   <li>Verifying custom aggregation with multiple tagged input streams executes successfully
 * </ul>
 */
public class MultiAggAgentTest {

  @Test
  public void testMultiAggAgent() throws Exception {
    // Test that the multi-agg agent runs without errors
    MultiAggAgent.main(new String[] {});
  }
}

```

---

### MultiNodeAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/MultiNodeAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for MultiNodeAgent demonstrating multi-step agent execution flow.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Multi-node agent graph execution
 *   <li>Data flow between agent nodes using emit
 *   <li>Processing pipeline with greeting generation
 *   <li>Sequential node execution patterns
 * </ul>
 */
public class MultiNodeAgentTest {

  @Test
  public void testMultiNodeAgent() throws Exception {
    // Tests basic multi-node execution flow
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      MultiNodeAgent.MultiNodeModule module = new MultiNodeAgent.MultiNodeModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("MultiNodeAgent");

      // Test with a user name
      String result = (String) agent.invoke("Alice");

      assertNotNull("Result should not be null", result);
      assertTrue("Should contain welcome message", result.contains("Welcome to agent-o-rama"));
      assertTrue("Should contain user name", result.contains("Alice"));
      assertTrue(
          "Should contain greeting",
          result.contains("Hello") || result.contains("Hi") || result.contains("Good"));
      assertTrue("Should contain thanks", result.contains("Thanks for joining"));
    }
  }
}

```

---

### PStateStoreAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/PStateStoreAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Test class for PStateStoreAgent demonstrating complex path-based data structures.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>declarePStateStore: Creating PState storage with schema
 *   <li>getStore: Accessing PState stores from agent nodes
 *   <li>Store operations: pstateSelect, pstateSelectOne, pstateTransform
 *   <li>HashMap usage for request and response data structures
 * </ul>
 */
public class PStateStoreAgentTest {

  @Test
  public void testPStateStoreAgent() throws Exception {
    // Tests PState store operations with path-based queries using HashMap
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      PStateStoreAgent.PStateStoreModule module = new PStateStoreAgent.PStateStoreModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("PStateStoreAgent");

      // Test creating company with employee
      Map<String, Object> request = new HashMap<>();
      request.put("companyId", "test-corp");
      request.put("companyName", "Test Corp");
      request.put("deptId", "eng");
      request.put("deptName", "Engineering");
      Map<String, Object> employee = new HashMap<>();
      employee.put("id", "e001");
      employee.put("name", "Test Employee");
      employee.put("salary", 80000L);
      employee.put("metadata", new HashMap<>());
      request.put("employee", employee);

      Map<String, Object> result = (Map<String, Object>) agent.invoke(request);

      assertNotNull("Result should not be null", result);
      assertEquals("Company ID should match", "test-corp", result.get("companyId"));
      assertEquals("Company name should match", "Test Corp", result.get("companyName"));
      assertEquals("Department name should match", "Engineering", result.get("deptName"));
      assertEquals("Employee count should be 1", 1, result.get("employeeCount"));
      assertTrue(
          "Average salary should be 80000", ((Double) result.get("averageSalary")) == 80000.0);

      List<String> allEmployees = (List<String>) result.get("allCompanyEmployeeNames");
      assertNotNull("All employees list should not be null", allEmployees);
      assertTrue("Should contain test employee", allEmployees.contains("Test Employee"));
    }
  }
}

```

---

### RamaModuleAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/RamaModuleAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.HashMap;
import java.util.Set;
import org.junit.Test;

/**
 * Test class for RamaModuleAgent demonstrating Rama module with depot integration.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Testing direct RamaModule implementation
 *   <li>Accessing and testing depot functionality
 *   <li>Verifying agent availability in the module
 *   <li>Testing agent invocations with structured responses
 * </ul>
 */
public class RamaModuleAgentTest {

  @Test
  public void testRamaModuleAgent() throws Exception {
    // Tests Rama module with manual agent topology creation
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the Rama module
      RamaModuleAgent.RamaModule module = new RamaModuleAgent.RamaModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);

      // Verify agent is available
      Set<String> agentNames = manager.getAgentNames();
      assertTrue("FeedbackAgent should be available", agentNames.contains("FeedbackAgent"));

      AgentClient agent = manager.getAgentClient("FeedbackAgent");

      // Test first feedback processing
      HashMap<String, Object> result1 = (HashMap<String, Object>) agent.invoke("Great product!");
      assertNotNull("Agent should return a result", result1);
      assertEquals("success", result1.get("status"));
      assertEquals("Processed: Great product!", result1.get("message"));
      assertEquals(14, result1.get("length"));

      // Test second feedback processing
      HashMap<String, Object> result2 = (HashMap<String, Object>) agent.invoke("Needs work");
      assertNotNull("Agent should return a result", result2);
      assertEquals("success", result2.get("status"));
      assertEquals("Processed: Needs work", result2.get("message"));
      assertEquals(10, result2.get("length"));
    }
  }
}

```

---

### RecordOpAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/RecordOpAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for RecordOpAgent demonstrating trace recording patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Agent invocation with recordNestedOp calls
 *   <li>Verifying agent results when using trace recording
 * </ul>
 */
public class RecordOpAgentTest {

  @Test
  public void testRecordOpAgent() throws Exception {
    // Tests agent invocation with recorded operations
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      RecordOpAgent.RecordOpModule module = new RecordOpAgent.RecordOpModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("RecordOpAgent");

      // Test with single input
      String result = (String) agent.invoke("Alice");
      assertNotNull("Agent should return a result", result);
      assertEquals("Hello, Alice!", result);

      // Test with different input
      result = (String) agent.invoke("Bob");
      assertNotNull("Agent should return a result", result);
      assertEquals("Hello, Bob!", result);
    }
  }
}

```

---

### RouterAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/RouterAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for RouterAgent demonstrating conditional routing and branching.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Conditional routing based on input values
 *   <li>Different execution paths for different inputs
 *   <li>Router node functionality and branching logic
 * </ul>
 */
public class RouterAgentTest {

  @Test
  public void testRouterAgent() throws Exception {
    // Tests routing for high priority messages
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      RouterAgent.RouterAgentModule module = new RouterAgent.RouterAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("RouterAgent");

      // Test with high priority message
      String result = (String) agent.invoke("urgent:System alert");

      assertNotNull("Result should not be null", result);
      assertTrue("Should contain HIGH priority", result.contains("[HIGH]"));
      assertTrue("Should contain the message", result.contains("System alert"));
    }
  }
}

```

---

### StreamingAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/StreamingAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentInvoke;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Test class for StreamingAgent demonstrating streaming data patterns.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>streamChunk: Emitting streaming data from agent nodes
 *   <li>agent.stream: Subscribing to streaming data from specific nodes
 *   <li>Real-time data flow with incremental results
 *   <li>Streaming completion and callbacks
 * </ul>
 */
public class StreamingAgentTest {

  @Test
  public void testStreamingAgentBasicFunctionality() throws Exception {
    // Tests basic streaming functionality
    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      StreamingAgent.StreamingAgentModule module = new StreamingAgent.StreamingAgentModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StreamingAgent");

      // Start agent execution with small numbers for testing
      Map<String, Object> request = new HashMap<>();
      request.put("dataSize", 10); // 10 total items
      request.put("chunkSize", 5); // chunk size 5
      AgentInvoke invoke = agent.initiate(request);

      // Track chunks received via streaming
      AtomicInteger chunksReceived = new AtomicInteger(0);
      List<Map<String, Object>> receivedChunks = new ArrayList<>();

      // Subscribe to streaming chunks
      agent.stream(
          invoke,
          "process-data",
          (allChunks, newChunks, reset, complete) -> {
            for (Object chunkObj : newChunks) {
              Map<String, Object> chunk = (Map<String, Object>) chunkObj;
              receivedChunks.add(chunk);
              chunksReceived.incrementAndGet();
            }
          });

      // Get final result
      Map<String, Object> result = (Map<String, Object>) agent.result(invoke);

      // Verify streaming chunks were emitted
      assertTrue("Should have received streaming chunks", chunksReceived.get() > 0);
      assertTrue("Should have received chunks", receivedChunks.size() > 0);

      // Verify final result
      assertNotNull("Final result should not be null", result);
      assertEquals("Should process 10 items", 10, (int) result.get("totalItems"));
      assertEquals("Should have chunk size 5", 5, (int) result.get("chunkSize"));
      assertTrue("Should have total chunks", (Integer) result.get("totalChunks") > 0);
    }
  }
}

```

---

### StreamingLangchain4jAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/StreamingLangchain4jAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for StreamingLangchain4jAgent demonstrating streaming chat with LangChain4j.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>OpenAI streaming chat model integration
 *   <li>Streaming response handling
 *   <li>Real-time token processing
 * </ul>
 */
public class StreamingLangchain4jAgentTest {

  @Test
  public void testStreamingLangchain4jAgent() throws Exception {
    // Tests streaming output from LangChain4j
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("Skipping test - OPENAI_API_KEY not set");
      return;
    }

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      StreamingLangchain4jAgent.StreamingLangChain4jModule module =
          new StreamingLangchain4jAgent.StreamingLangChain4jModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StreamingLangChain4jAgent");

      // Test with a simple question
      String result = (String) agent.invoke("What is 2+2?");

      assertNotNull("Result should not be null", result);
    }
  }
}

```

---

### StructuredLangchain4jAgentTest (Test)

**File:** `src/test/java/com/rpl/agent/basic/StructuredLangchain4jAgentTest.java`

```java
package com.rpl.agent.basic;

import static org.junit.Assert.assertNotNull;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.Test;

/**
 * Test class for StructuredLangchain4jAgent demonstrating structured output with LangChain4j.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Structured output with JSON response format
 *   <li>OpenAI chat model integration
 *   <li>Question analysis with structured data
 * </ul>
 */
public class StructuredLangchain4jAgentTest {

  @Test
  public void testStructuredLangchain4jAgent() throws Exception {
    // Tests structured output from LangChain4j
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("Skipping test - OPENAI_API_KEY not set");
      return;
    }

    try (InProcessCluster ipc = InProcessCluster.create()) {
      // Deploy the agent module
      StructuredLangchain4jAgent.StructuredLangChain4jModule module =
          new StructuredLangchain4jAgent.StructuredLangChain4jModule();
      ipc.launchModule(module, new LaunchConfig(1, 1));

      // Get agent manager and client
      String moduleName = module.getModuleName();
      AgentManager manager = AgentManager.create(ipc, moduleName);
      AgentClient agent = manager.getAgentClient("StructuredLangChain4jAgent");

      // Test with a simple question
      String result = (String) agent.invoke("What is 2+2?");

      assertNotNull("Result should not be null", result);
    }
  }
}

```

---


# API Reference

This section contains the Javadoc and Clojuredoc API references.

# Agent-O-Rama Javadoc API Reference

This document contains the Java API reference extracted from the Agent-O-Rama Javadoc.

**Source:** [https://redplanetlabs.com/aor/javadoc/index.html](https://redplanetlabs.com/aor/javadoc/index.html)

---

## Package `com.rpl.agentorama`

### Classes and Interfaces

- [ActionBuilderOptions](#actionbuilderoptions)
- [ActionBuilderOptions.Impl](#actionbuilderoptions.impl)
- [AddDatasetExampleOptions](#adddatasetexampleoptions)
- [AgentClient](#agentclient)
- [AgentClient.StreamAllCallback](#agentclient.streamallcallback)
- [AgentClient.StreamCallback](#agentclient.streamcallback)
- [AgentComplete](#agentcomplete)
- [AgentContext](#agentcontext)
- [AgentContext.Impl](#agentcontext.impl)
- [AgentFailedException](#agentfailedexception)
- [AgentGraph](#agentgraph)
- [AgentInvoke](#agentinvoke)
- [AgentManager](#agentmanager)
- [AgentModule](#agentmodule)
- [AgentNode](#agentnode)
- [AgentObjectFetcher](#agentobjectfetcher)
- [AgentObjectOptions](#agentobjectoptions)
- [AgentObjectOptions.Impl](#agentobjectoptions.impl)
- [AgentObjectSetup](#agentobjectsetup)
- [AgentRef](#agentref)
- [AgentStep](#agentstep)
- [AgentStream](#agentstream)
- [AgentStreamByInvoke](#agentstreambyinvoke)
- [AgentTopology](#agenttopology)
- [BuiltIn](#builtin)
- [CreateEvaluatorOptions](#createevaluatoroptions)
- [EvaluatorBuilderOptions](#evaluatorbuilderoptions)
- [EvaluatorBuilderOptions.Impl](#evaluatorbuilderoptions.impl)
- [ExampleRun](#examplerun)
- [FinishedAgg](#finishedagg)
- [HumanInputRequest](#humaninputrequest)
- [IUnderlying](#iunderlying)
- [MultiAgg](#multiagg)
- [MultiAgg.Impl](#multiagg.impl)
- [NestedOpType](#nestedoptype)
- [NodeInvoke](#nodeinvoke)
- [RunInfo](#runinfo)
- [RunType](#runtype)
- [StreamingRecorder](#streamingrecorder)
- [ToolInfo](#toolinfo)
- [ToolsAgentOptions](#toolsagentoptions)
- [ToolsAgentOptions.FunctionHandler](#toolsagentoptions.functionhandler)
- [ToolsAgentOptions.Impl](#toolsagentoptions.impl)
- [ToolsAgentOptions.StaticStringHandler](#toolsagentoptions.staticstringhandler)
- [UI](#ui)
- [UI.Options](#ui.options)
- [UIOptions](#uioptions)
- [UpdateMode](#updatemode)

### ActionBuilderOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ActionBuilderOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ActionBuilderOptions.html)

```java
public interfaceActionBuilderOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates an empty ActionBuilderOptions |
| `limitConcurrency()` |  |
| `param(Stringname,Stringdescription)` |  |
| `param(Stringname,Stringdescription,StringdefaultValue)` |  |

#### Method Details

**`staticActionBuilderOptions.Implcreate()`**

> Creates an empty ActionBuilderOptions

**`staticActionBuilderOptions.Implparam(Stringname,Stringdescription)`**

**`staticActionBuilderOptions.Implparam(Stringname,Stringdescription,StringdefaultValue)`**

**`staticActionBuilderOptions.ImpllimitConcurrency()`**

---
### ActionBuilderOptions.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ActionBuilderOptions.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ActionBuilderOptions.Impl.html)

```java
public static interfaceActionBuilderOptions.ImplextendsActionBuilderOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `limitConcurrency()` |  |
| `param(Stringname,Stringdescription)` |  |
| `param(Stringname,Stringdescription,StringdefaultValue)` |  |

#### Method Details

**`ActionBuilderOptions.Implparam(Stringname,Stringdescription)`**

**`ActionBuilderOptions.Implparam(Stringname,Stringdescription,StringdefaultValue)`**

**`ActionBuilderOptions.ImpllimitConcurrency()`**

---
### AddDatasetExampleOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AddDatasetExampleOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AddDatasetExampleOptions.html)

```java
public classAddDatasetExampleOptionsextendsObject
```

---
### AgentClient

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.html)

```java
public interfaceAgentClientextendsCloseable
```

Client for interacting with a specific agent.

 Agent clients provide the interface for invoking agents, streaming data,
 handling human input, and managing agent executions.

 When called from within an agent node function, this enables subagent execution:Can invoke any other agent in the same module (including the current agent)Enables recursive agent execution patternsEnables mutually recursive agent execution between different agentsSubagent calls are tracked and displayed in the UI traceExample:// From client code
 AgentClient client = manager.getAgentClient("my-agent");
 String result = client.invoke("Hello world");

 // From within an agent node (subagent execution)
 AgentClient subClient = agentNode.getAgentClient("other-agent");
 String subResult = subClient.invoke("Process this data");

#### Method Summary

| Method | Description |
|---|---|
| `fork(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)` | Synchronously forks an agent execution with new arguments for specific nodes. |
| `forkAsync(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)` | Asynchronously forks an agent execution with new arguments for specific nodes. |
| `getMetadata(AgentInvokeinvoke)` | Gets all metadata for an agent execution. |
| `initiate(Object... args)` | Initiates an agent execution and returns a handle for tracking. |
| `initiateAsync(Object... args)` | Asynchronously initiates an agent execution and returns a CompletableFuture with a handle for tracking. |
| `initiateFork(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)` | Initiates a fork of an agent execution and returns a handle for tracking. |
| `initiateForkAsync(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)` | Asynchronously initiates a fork of an agent execution. |
| `initiateWithContext(AgentContextcontext,Object... args)` | Initiates an agent execution with context metadata. |
| `initiateWithContextAsync(AgentContextcontext,Object... args)` | Asynchronously initiates an agent execution with context metadata. |
| `invoke(Object... args)` | Synchronously invokes an agent with the provided arguments. |
| `invokeAsync(Object... args)` | Asynchronously invokes an agent with the provided arguments. |
| `invokeWithContext(AgentContextcontext,Object... args)` | Synchronously invokes an agent with context metadata. |
| `invokeWithContextAsync(AgentContextcontext,Object... args)` | Asynchronously invokes an agent with context metadata. |
| `isAgentInvokeComplete(AgentInvokeinvoke)` | Checks if an agent execution has completed. |
| `nextStep(AgentInvokeinvoke)` | Gets the next execution step of an agent. |
| `nextStepAsync(AgentInvokeinvoke)` | Asynchronously gets the next execution step of an agent. |
| `pendingHumanInputs(AgentInvokeinvoke)` | Gets all pending human input requests for an agent execution. |
| `pendingHumanInputsAsync(AgentInvokeinvoke)` | Asynchronously gets all pending human input requests for an agent execution. |
| `provideHumanInput(HumanInputRequestrequest,Stringresponse)` | Provides a response to a human input request. |
| `provideHumanInputAsync(HumanInputRequestrequest,Stringresponse)` | Asynchronously provides a response to a human input request. |
| `removeMetadata(AgentInvokeinvoke,Stringkey)` | Removes metadata from an agent execution. |
| `result(AgentInvokeinvoke)` | Gets the final result of an agent execution. |
| `resultAsync(AgentInvokeinvoke)` | Asynchronously gets the final result of an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,
 boolean value)` | Sets metadata for an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,
 double value)` | Sets metadata for an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,
 float value)` | Sets metadata for an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,
 int value)` | Sets metadata for an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,
 long value)` | Sets metadata for an agent execution. |
| `setMetadata(AgentInvokeinvoke,Stringkey,Stringvalue)` | Sets metadata for an agent execution. |
| `stream(AgentInvokeinvoke,Stringnode)` | Creates a stream for data emitted from a specific node. |
| `stream(AgentInvokeinvoke,Stringnode,AgentClient.StreamCallback<T> callback)` | Creates a stream for data emitted from a specific node with a callback. |
| `streamAll(AgentInvokeinvoke,Stringnode)` | Creates a stream for data emitted from all invocations of a specific node. |
| `streamAll(AgentInvokeinvoke,Stringnode,AgentClient.StreamAllCallback<T> callback)` | Creates a stream for data emitted from all invocations of a specific node with a callback. |
| `streamSpecific(AgentInvokeinvoke,Stringnode,UUIDnodeInvokeId)` | Creates a stream for data emitted from a specific node invocation. |
| `streamSpecific(AgentInvokeinvoke,Stringnode,UUIDnodeInvokeId,AgentClient.StreamCallback<T> callback)` | Creates a stream for data emitted from a specific node invocation with a callback. |

#### Method Details

**`<T>Tinvoke(Object... args)`**

> Synchronously invokes an agent with the provided arguments.

 This method blocks until the agent execution completes and returns
 the final result. For long-running agents, consider using initiate()
 with result() for better control.

- Parameters:: args- arguments to pass to the agent
- Returns:: the final result from the agent execution

**`<T>CompletableFuture<T>invokeAsync(Object... args)`**

> Asynchronously invokes an agent with the provided arguments.

 Returns a CompletableFuture that will complete with the agent's result.
 This allows for non-blocking agent execution and better resource utilization.

- Parameters:: args- arguments to pass to the agent
- Returns:: future that completes with the agent result

**`<T>TinvokeWithContext(AgentContextcontext,Object... args)`**

> Synchronously invokes an agent with context metadata.

 Metadata allows attaching custom key-value data to agent executions.
 Metadata is an additional optional parameter to agent execution, and
 is also used for analytics. Metadata can be accessed anywhere inside
 agents by calling getMetadata() within node functions.

- Parameters:: context- context containing metadata for the execution
- Returns:: the final result from the agent execution

**`<T>CompletableFuture<T>invokeWithContextAsync(AgentContextcontext,Object... args)`**

> Asynchronously invokes an agent with context metadata.

- Parameters:: context- context containing metadata for the execution
- Returns:: future that completes with the agent result

**`AgentInvokeinitiate(Object... args)`**

> Initiates an agent execution and returns a handle for tracking.

 This method starts an agent execution but doesn't wait for completion.
 Use the returned result handle with result(), nextStep(), or
 streaming methods to interact with the running agent.

- Parameters:: args- arguments to pass to the agent
- Returns:: agent invoke handle for tracking and interacting with the execution

**`CompletableFuture<AgentInvoke>initiateAsync(Object... args)`**

> Asynchronously initiates an agent execution and returns a CompletableFuture with a handle for tracking.

- Parameters:: args- arguments to pass to the agent
- Returns:: future that completes with the agent invoke handle

**`AgentInvokeinitiateWithContext(AgentContextcontext,Object... args)`**

> Initiates an agent execution with context metadata.

- Parameters:: context- context containing metadata for the execution
- Returns:: agent invoke handle for tracking and interacting with the execution

**`CompletableFuture<AgentInvoke>initiateWithContextAsync(AgentContextcontext,Object... args)`**

> Asynchronously initiates an agent execution with context metadata.

- Parameters:: context- context containing metadata for the execution
- Returns:: future that completes with the agent invoke handle

**`<T>Tfork(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)`**

> Synchronously forks an agent execution with new arguments for specific nodes.

 Forking creates new execution branches from an existing agent invocation.
 The nodeInvokeIdToNewArgs map specifies which nodes to fork and what new
 arguments to use for each fork. Node invoke IDs can be found in the trace UI.

- Parameters:: invoke- the agent invoke handle to fork from
- Returns:: the result from the forked execution

**`<T>CompletableFuture<T>forkAsync(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)`**

> Asynchronously forks an agent execution with new arguments for specific nodes.

- Parameters:: invoke- the agent invoke handle to fork from
- Returns:: future that completes with the result from the forked execution

**`AgentInvokeinitiateFork(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)`**

> Initiates a fork of an agent execution and returns a handle for tracking.

- Parameters:: invoke- the agent invoke to fork from
- Returns:: agent invoke handle for the forked execution

**`CompletableFuture<AgentInvoke>initiateForkAsync(AgentInvokeinvoke,Map<UUID,List> nodeInvokeIdToNewArgs)`**

> Asynchronously initiates a fork of an agent execution.

- Parameters:: invoke- the agent invoke handle to fork from
- Returns:: future that completes with the agent invoke handle for the forked execution

**`AgentStepnextStep(AgentInvokeinvoke)`**

> Gets the next execution step of an agent.

 The next execution step is either a human input request or an agent result.
 Check which one by checking if the returned object is an instance ofHumanInputRequestorAgentComplete.
 If the agent fails, it will throw an exception.

- Parameters:: invoke- the agent invoke handle to get the next step for
- Returns:: the next execution step

**`CompletableFuture<AgentStep>nextStepAsync(AgentInvokeinvoke)`**

> Asynchronously gets the next execution step of an agent.

- Parameters:: invoke- the agent invoke handle to get the next step for
- Returns:: future that completes with the next execution step

**`<T>Tresult(AgentInvokeinvoke)`**

> Gets the final result of an agent execution.

 This method blocks until the agent execution completes and returns
 the final result. If the agent fails, it will throw an exception.

- Parameters:: invoke- the agent invoke handle to get the result for
- Returns:: the final result from the agent execution

**`<T>CompletableFuture<T>resultAsync(AgentInvokeinvoke)`**

> Asynchronously gets the final result of an agent execution.

- Parameters:: invoke- the agent invoke handle to get the result for
- Returns:: future that completes with the final result from the agent execution

**`booleanisAgentInvokeComplete(AgentInvokeinvoke)`**

> Checks if an agent execution has completed.

- Parameters:: invoke- the agent invoke handle to check
- Returns:: true if the agent execution is complete

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,
 int value)`**

> Sets metadata for an agent execution.

 Note: For agent execution, only the metadata that was set at the start
 with the *WithContext functions is used.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,
 long value)`**

> Sets metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,
 float value)`**

> Sets metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,
 double value)`**

> Sets metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,Stringvalue)`**

> Sets metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidsetMetadata(AgentInvokeinvoke,Stringkey,
 boolean value)`**

> Sets metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to set metadata for

**`voidremoveMetadata(AgentInvokeinvoke,Stringkey)`**

> Removes metadata from an agent execution.

- Parameters:: invoke- the agent invoke handle to remove metadata from

**`Map<String,Object>getMetadata(AgentInvokeinvoke)`**

> Gets all metadata for an agent execution.

- Parameters:: invoke- the agent invoke handle to get metadata for
- Returns:: map of metadata key-value pairs

**`AgentStreamstream(AgentInvokeinvoke,Stringnode)`**

> Creates a stream for data emitted from a specific node.

 The returned object can have close() called on it to immediately stop streaming.

- Parameters:: invoke- the agent invoke handle to stream from
- Returns:: stream object for accessing chunks and controlling streaming

**`<T>AgentStreamstream(AgentInvokeinvoke,Stringnode,AgentClient.StreamCallback<T> callback)`**

> Creates a stream for data emitted from a specific node with a callback.

- Parameters:: invoke- the agent invoke handle to stream from
- Returns:: stream object for accessing chunks and controlling streaming

**`AgentStreamstreamSpecific(AgentInvokeinvoke,Stringnode,UUIDnodeInvokeId)`**

> Creates a stream for data emitted from a specific node invocation.

- Parameters:: invoke- the agent invoke to stream from
- Returns:: stream object for accessing chunks and controlling streaming

**`<T>AgentStreamstreamSpecific(AgentInvokeinvoke,Stringnode,UUIDnodeInvokeId,AgentClient.StreamCallback<T> callback)`**

> Creates a stream for data emitted from a specific node invocation with a callback.

- Parameters:: invoke- the agent invoke to stream from
- Returns:: stream object for accessing chunks and controlling streaming

**`AgentStreamByInvokestreamAll(AgentInvokeinvoke,Stringnode)`**

> Creates a stream for data emitted from all invocations of a specific node.

 The returned object can have close() called on it to immediately stop streaming.

- Parameters:: invoke- the agent invoke handle to stream from
- Returns:: stream object for accessing chunks grouped by invoke ID

**`<T>AgentStreamByInvokestreamAll(AgentInvokeinvoke,Stringnode,AgentClient.StreamAllCallback<T> callback)`**

> Creates a stream for data emitted from all invocations of a specific node with a callback.

- Parameters:: invoke- the agent invoke to stream from
- Returns:: stream object for accessing chunks grouped by invoke ID

**`List<HumanInputRequest>pendingHumanInputs(AgentInvokeinvoke)`**

> Gets all pending human input requests for an agent execution.

- Parameters:: invoke- the agent invoke to get pending inputs for
- Returns:: list of pending human input requests

**`CompletableFuture<List<HumanInputRequest>>pendingHumanInputsAsync(AgentInvokeinvoke)`**

> Asynchronously gets all pending human input requests for an agent execution.

- Parameters:: invoke- the agent invoke to get pending inputs for
- Returns:: future that completes with the list of pending human input requests

**`voidprovideHumanInput(HumanInputRequestrequest,Stringresponse)`**

> Provides a response to a human input request.

- Parameters:: request- the human input request to respond to

**`CompletableFuture<Void>provideHumanInputAsync(HumanInputRequestrequest,Stringresponse)`**

> Asynchronously provides a response to a human input request.

- Parameters:: request- the human input request to respond to
- Returns:: future that completes when the response is provided

---
### AgentClient.StreamAllCallback

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.StreamAllCallback.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.StreamAllCallback.html)

```java
public static interfaceAgentClient.StreamAllCallback<T>
```

Callback interface for streaming data from all invocations of a specific node.

#### Method Summary

| Method | Description |
|---|---|
| `onUpdate(Map<UUID,List<T>> allChunks,Map<UUID,List<T>> newChunks,Set<UUID> resetInvokeIds,
 boolean isComplete)` | Called when new data chunks are available from any node invocation. |

#### Method Details

**`voidonUpdate(Map<UUID,List<T>> allChunks,Map<UUID,List<T>> newChunks,Set<UUID> resetInvokeIds,
 boolean isComplete)`**

> Called when new data chunks are available from any node invocation.

- Parameters:: allChunks- all chunks received so far, grouped by invoke ID

---
### AgentClient.StreamCallback

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.StreamCallback.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentClient.StreamCallback.html)

```java
public static interfaceAgentClient.StreamCallback<T>
```

Callback interface for streaming data from a single node invoke.

#### Method Summary

| Method | Description |
|---|---|
| `onUpdate(List<T> allChunks,List<T> newChunks,
 boolean isReset,
 boolean isComplete)` | Called when new data chunks are available. |

#### Method Details

**`voidonUpdate(List<T> allChunks,List<T> newChunks,
 boolean isReset,
 boolean isComplete)`**

> Called when new data chunks are available.

- Parameters:: allChunks- all chunks received so far

---
### AgentComplete

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentComplete.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentComplete.html)

```java
public interfaceAgentComplete<T>extendsAgentStep
```

Represents the completion of an agent execution with a result.
 
 When an agent execution completes successfully, it returns an AgentComplete
 containing the final result value.
 
 Example:AgentStep step = client.nextStep(invoke);
 if (step instanceof AgentComplete) {
   AgentComplete<String> complete = (AgentComplete<String>) step;
   String result = complete.getResult();
   System.out.println("Agent result: " + result);
 }

#### Method Summary

| Method | Description |
|---|---|
| `getResult()` | Gets the final result of the agent execution. |

#### Method Details

**`TgetResult()`**

> Gets the final result of the agent execution.

- Returns:: the agent's result value

---
### AgentContext

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentContext.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentContext.html)

```java
public interfaceAgentContext
```

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates an empty AgentContext |
| `metadata(Stringname,
 boolean val)` |  |
| `metadata(Stringname,
 double val)` |  |
| `metadata(Stringname,
 float val)` |  |
| `metadata(Stringname,
 int val)` |  |
| `metadata(Stringname,
 long val)` |  |
| `metadata(Stringname,Stringval)` |  |

#### Method Details

**`staticAgentContext.Implcreate()`**

> Creates an empty AgentContext

**`staticAgentContext.Implmetadata(Stringname,
 long val)`**

**`staticAgentContext.Implmetadata(Stringname,
 int val)`**

**`staticAgentContext.Implmetadata(Stringname,Stringval)`**

**`staticAgentContext.Implmetadata(Stringname,
 float val)`**

**`staticAgentContext.Implmetadata(Stringname,
 double val)`**

**`staticAgentContext.Implmetadata(Stringname,
 boolean val)`**

---
### AgentContext.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentContext.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentContext.Impl.html)

```java
public static interfaceAgentContext.ImplextendsAgentContext
```

#### Method Summary

| Method | Description |
|---|---|
| `metadata(Stringname,
 boolean val)` |  |
| `metadata(Stringname,
 double val)` |  |
| `metadata(Stringname,
 float val)` |  |
| `metadata(Stringname,
 int val)` |  |
| `metadata(Stringname,
 long val)` |  |
| `metadata(Stringname,Stringval)` |  |

#### Method Details

**`AgentContext.Implmetadata(Stringname,
 long val)`**

**`AgentContext.Implmetadata(Stringname,
 int val)`**

**`AgentContext.Implmetadata(Stringname,Stringval)`**

**`AgentContext.Implmetadata(Stringname,
 float val)`**

**`AgentContext.Implmetadata(Stringname,
 double val)`**

**`AgentContext.Implmetadata(Stringname,
 boolean val)`**

---
### AgentFailedException

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentFailedException.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentFailedException.html)

```java
public classAgentFailedExceptionextendsRuntimeException
```

Exception thrown when an agent execution fails.
 
 This exception is thrown when an agent encounters an error during execution
 and cannot complete its task. It wraps the underlying cause of the failure.
 
 Example:try {
   String result = client.invoke("Hello world");
 } catch (AgentFailedException e) {
   System.err.println("Agent failed: " + e.getMessage());
   e.getCause().printStackTrace();
 }

---
### AgentGraph

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentGraph.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentGraph.html)

```java
public interfaceAgentGraph
```

Builder interface for defining agent execution graphs.
 
 AgentGraph provides a fluent API for building agent execution graphs with nodes,
 aggregation subgraphs, and control flow. Each agent is defined as a directed
 graph where nodes represent computational units and edges represent data flow.
 
 Example usage:topology.newAgent("my-agent")
         .node("start", "process", (AgentNode agentNode, String input) -> {
             agentNode.emit("process", "Hello " + input);
         })
         .node("process", null, (AgentNode agentNode, String data) -> {
             agentNode.result("Processed: " + data);
         });

#### Method Summary

| Method | Description |
|---|---|
| `aggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.agentorama.impl.BuiltInAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)` | Adds an aggregation node that collects and combines results using a built-in aggregator. |
| `aggNode(Stringname,ObjectoutputNodesSpec,MultiAgg.Implagg,RamaVoidFunction3<AgentNode,S,T> impl)` | Adds an aggregation node that collects and combines results using a multi-aggregator. |
| `aggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaAccumulatorAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)` | Adds an aggregation node that collects and combines results using a Rama accumulator aggregator. |
| `aggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaCombinerAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)` | Adds an aggregation node that collects and combines results using a Rama combiner aggregator. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction1<AgentNode,Object> impl)` | Adds an aggregation start node with zero arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction2<AgentNode,T0,Object> impl)` | Adds an aggregation start node with one argument that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction3<AgentNode,T0,T1,Object> impl)` | Adds an aggregation start node with two arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction4<AgentNode,T0,T1,T2,Object> impl)` | Adds an aggregation start node with three arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction5<AgentNode,T0,T1,T2,T3,Object> impl)` | Adds an aggregation start node with four arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction6<AgentNode,T0,T1,T2,T3,T4,Object> impl)` | Adds an aggregation start node with five arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction7<AgentNode,T0,T1,T2,T3,T4,T5,Object> impl)` | Adds an aggregation start node with six arguments that scopes aggregation within a subgraph. |
| `aggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction8<AgentNode,T0,T1,T2,T3,T4,T5,T6,Object> impl)` | Adds an aggregation start node with seven arguments that scopes aggregation within a subgraph. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction1<AgentNode> impl)` | Adds a node to the agent graph with zero arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction2<AgentNode,T0> impl)` | Adds a node to the agent graph with one argument. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction3<AgentNode,T0,T1> impl)` | Adds a node to the agent graph with two arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction4<AgentNode,T0,T1,T2> impl)` | Adds a node to the agent graph with three arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction5<AgentNode,T0,T1,T2,T3> impl)` | Adds a node to the agent graph with four arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction6<AgentNode,T0,T1,T2,T3,T4> impl)` | Adds a node to the agent graph with five arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction7<AgentNode,T0,T1,T2,T3,T4,T5> impl)` | Adds a node to the agent graph with six arguments. |
| `node(Stringname,ObjectoutputNodesSpec,RamaVoidFunction8<AgentNode,T0,T1,T2,T3,T4,T5,T6> impl)` | Adds a node to the agent graph with seven arguments. |
| `setUpdateMode(UpdateModemode)` | Sets the update mode for this agent graph. |

#### Method Details

**`AgentGraphsetUpdateMode(UpdateModemode)`**

> Sets the update mode for this agent graph.
 
 Controls how in-flight agent executions are handled after the module is updated.

- Parameters:: mode- the update mode (CONTINUE, RESTART, or DROP)
- Returns:: this agent graph for method chaining

**`AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction1<AgentNode> impl)`**

> Adds a node to the agent graph with zero arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction2<AgentNode,T0> impl)`**

> Adds a node to the agent graph with one argument.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction3<AgentNode,T0,T1> impl)`**

> Adds a node to the agent graph with two arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1,T2>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction4<AgentNode,T0,T1,T2> impl)`**

> Adds a node to the agent graph with three arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction5<AgentNode,T0,T1,T2,T3> impl)`**

> Adds a node to the agent graph with four arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction6<AgentNode,T0,T1,T2,T3,T4> impl)`**

> Adds a node to the agent graph with five arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4,T5>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction7<AgentNode,T0,T1,T2,T3,T4,T5> impl)`**

> Adds a node to the agent graph with six arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4,T5,T6>AgentGraphnode(Stringname,ObjectoutputNodesSpec,RamaVoidFunction8<AgentNode,T0,T1,T2,T3,T4,T5,T6> impl)`**

> Adds a node to the agent graph with seven arguments.
 
 Nodes are the fundamental computation units in agent graphs. Each node
 receives data from upstream nodes and can emit data to downstream nodes
 or return a final result.

- Parameters:: name- the name of the node (must be unique within the agent)
- Returns:: this agent graph for method chaining

**`AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction1<AgentNode,Object> impl)`**

> Adds an aggregation start node with zero arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction2<AgentNode,T0,Object> impl)`**

> Adds an aggregation start node with one argument that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction3<AgentNode,T0,T1,Object> impl)`**

> Adds an aggregation start node with two arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1,T2>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction4<AgentNode,T0,T1,T2,Object> impl)`**

> Adds an aggregation start node with three arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction5<AgentNode,T0,T1,T2,T3,Object> impl)`**

> Adds an aggregation start node with four arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction6<AgentNode,T0,T1,T2,T3,T4,Object> impl)`**

> Adds an aggregation start node with five arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4,T5>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction7<AgentNode,T0,T1,T2,T3,T4,T5,Object> impl)`**

> Adds an aggregation start node with six arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<T0,T1,T2,T3,T4,T5,T6>AgentGraphaggStartNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaFunction8<AgentNode,T0,T1,T2,T3,T4,T5,T6,Object> impl)`**

> Adds an aggregation start node with seven arguments that scopes aggregation within a subgraph.
 
 Aggregation start nodes work like regular nodes but define the beginning
 of an aggregation subgraph. They must have a correspondingaggNode(String, Object, RamaAccumulatorAgg, RamaVoidFunction3)downstream. Within the aggregation subgraph, edges must stay within
 the subgraph and cannot connect to nodes outside of it.
 
 The return value of the node function is passed to the downstream aggregation node
 as its last argument, allowing propagation of non-aggregated information
 downstream post-aggregation.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<S,T>AgentGraphaggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaAccumulatorAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)`**

> Adds an aggregation node that collects and combines results using a Rama accumulator aggregator.
 
 Aggregation nodes gather results from parallel processing nodes and combine
 them using a specified aggregation function. They receive both the collected
 results and any return value from the aggregation start node.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<S,T>AgentGraphaggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.rama.ops.RamaCombinerAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)`**

> Adds an aggregation node that collects and combines results using a Rama combiner aggregator.
 
 Aggregation nodes gather results from parallel processing nodes and combine
 them using a specified aggregation function. They receive both the collected
 results and any return value from the aggregation start node.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<S,T>AgentGraphaggNode(Stringname,ObjectoutputNodesSpec,MultiAgg.Implagg,RamaVoidFunction3<AgentNode,S,T> impl)`**

> Adds an aggregation node that collects and combines results using a multi-aggregator.
 
 Aggregation nodes gather results from parallel processing nodes and combine
 them using a specified aggregation function. They receive both the collected
 results and any return value from the aggregation start node.
 
 Multi-aggregators provide dispatch-based aggregation where different values
 are processed by different aggregation functions based on a dispatch key.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

**`<S,T>AgentGraphaggNode(Stringname,ObjectoutputNodesSpec,
 com.rpl.agentorama.impl.BuiltInAgg agg,RamaVoidFunction3<AgentNode,S,T> impl)`**

> Adds an aggregation node that collects and combines results using a built-in aggregator.
 
 Aggregation nodes gather results from parallel processing nodes and combine
 them using a specified aggregation function. They receive both the collected
 results and any return value from the aggregation start node.
 
 Built-in aggregators provide common aggregation patterns like sum, count, min, max, etc.
 SeeBuiltInfor available built-in aggregators.

- Parameters:: name- the name of the node
- Returns:: this agent graph for method chaining

---
### AgentInvoke

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentInvoke.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentInvoke.html)

```java
public interfaceAgentInvoke
```

Handle representing a specific execution instance of an agent.
 
 Agent invoke handles are used to track and interact with running agent
 executions. They provide access to execution metadata and are used with
 streaming, forking, and result retrieval methods.
 
 Example:AgentInvoke invoke = client.initiate("Hello world");
 // Use invoke with streaming, forking, or result methods
 String result = client.result(invoke);

#### Method Summary

| Method | Description |
|---|---|
| `getAgentInvokeId()` | Gets the unique agent invoke ID for this execution. |
| `getTaskId()` | Gets the task ID for this agent execution. |

#### Method Details

**`longgetTaskId()`**

> Gets the task ID for this agent execution.

- Returns:: the task ID

**`UUIDgetAgentInvokeId()`**

> Gets the unique agent invoke ID for this execution.

- Returns:: the agent invoke ID

---
### AgentManager

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentManager.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentManager.html)

```java
public interfaceAgentManagerextends com.rpl.agentorama.impl.IFetchAgentClient,Closeable
```

Manager for interacting with deployed agents on a Rama cluster.

 The agent manager provides access to agent clients, dataset management,
 and evaluation capabilities for a specific module deployed on a cluster.

 Example:AgentManager manager = AgentManager.create(cluster, "MyModule");
 Set<String> agentNames = manager.getAgentNames();
 AgentClient client = manager.getAgentClient("my-agent");
 String result = client.invoke("Hello world");

#### Method Summary

| Method | Description |
|---|---|
| `addDatasetExample(UUIDdatasetId,Objectinput,AddDatasetExampleOptionsoptions)` | Adds an example to a dataset for testing and evaluation. |
| `addDatasetExampleAsync(UUIDdatasetId,Objectinput,AddDatasetExampleOptionsoptions)` | Asynchronously adds an example to a dataset. |
| `addDatasetExampleTag(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Stringtag)` | Adds a tag to a specific dataset example for categorization. |
| `create(com.rpl.rama.cluster.ClusterManagerBase cluster,StringmoduleName)` | Creates an agent manager for managing and interacting with deployed agents on a Rama cluster. |
| `createCategoricalHumanMetric(Stringname,Stringdescription,Set<String> categories)` | Creates a categorical human metric for collecting human feedback on agent runs. |
| `createDataset(Stringname,Stringdescription,StringinputJsonSchema,StringoutputJsonSchema)` | Creates a new dataset for agent testing and evaluation. |
| `createEvaluator(Stringname,StringbuilderName,Mapparams,Stringdescription,CreateEvaluatorOptionsoptions)` | Creates an evaluator instance from a builder for measuring agent performance in experiments or actions. |
| `createNumericHumanMetric(Stringname,Stringdescription,
 int min,
 int max)` | Creates a numeric human metric for collecting human feedback on agent runs. |
| `destroyDataset(UUIDdatasetId)` | Permanently deletes a dataset and all its examples. |
| `getAgentNames()` | Gets the names of all available agents in the module. |
| `removeDatasetExample(UUIDdatasetId,StringsnapshotName,UUIDexampleId)` | Removes a specific example from a dataset. |
| `removeDatasetExampleTag(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Stringtag)` | Removes a tag from a specific dataset example. |
| `removeDatasetSnapshot(UUIDdatasetId,StringsnapshotName)` | Removes a specific snapshot from a dataset. |
| `removeEvaluator(Stringname)` | Removes an evaluator from the system. |
| `removeHumanMetric(Stringname)` | Removes a human metric from the system. |
| `searchDatasets(StringsearchString,
 int limit)` | Searches for datasets by name or description. |
| `searchEvaluators(StringsearchString)` | Searches for evaluators by name or description. |
| `setDatasetDescription(UUIDdatasetId,Stringdescription)` | Updates the description of an existing dataset. |
| `setDatasetExampleInput(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Objectinput)` | Updates the input data for a specific dataset example. |
| `setDatasetExampleReferenceOutput(UUIDdatasetId,StringsnapshotName,UUIDexampleId,ObjectreferenceOutput)` | Updates the reference output for a specific dataset example. |
| `setDatasetName(UUIDdatasetId,Stringname)` | Updates the name of an existing dataset. |
| `snapshotDataset(UUIDdatasetId,StringfromSnapshotName,StringtoSnapshotName)` | Creates a snapshot of a dataset at its current state. |
| `tryComparativeEvaluator(Stringname,Objectinput,ObjectreferenceOutput,List<Object> outputs)` | Tests a comparative evaluator on multiple outputs. |
| `tryEvaluator(Stringname,Objectinput,ObjectreferenceOutput,Objectoutput)` | Tests an evaluator on a single sample input / reference output / output. |
| `trySummaryEvaluator(Stringname,List<ExampleRun> exampleRuns)` | Tests a summary evaluator on a collection of example runs. |

#### Method Details

**`staticAgentManagercreate(com.rpl.rama.cluster.ClusterManagerBase cluster,StringmoduleName)`**

> Creates an agent manager for managing and interacting with deployed agents on a Rama cluster.

- Parameters:: cluster- the Rama cluster instance (IPC or remote cluster)
- Returns:: interface for managing agents and datasets

**`Set<String>getAgentNames()`**

> Gets the names of all available agents in the module.

- Returns:: set of agent names available in the module

**`UUIDcreateDataset(Stringname,Stringdescription,StringinputJsonSchema,StringoutputJsonSchema)`**

> Creates a new dataset for agent testing and evaluation.

 Datasets are collections of input/output examples used for testing
 agent performance, running experiments, and regression testing.

- Parameters:: name- the name of the dataset
- Returns:: UUID of the created dataset

**`voidsetDatasetName(UUIDdatasetId,Stringname)`**

> Updates the name of an existing dataset.

- Parameters:: datasetId- UUID of the dataset

**`voidsetDatasetDescription(UUIDdatasetId,Stringdescription)`**

> Updates the description of an existing dataset.

- Parameters:: datasetId- UUID of the dataset

**`voiddestroyDataset(UUIDdatasetId)`**

> Permanently deletes a dataset and all its examples.

- Parameters:: datasetId- UUID of the dataset to delete

**`CompletableFuture<Void>addDatasetExampleAsync(UUIDdatasetId,Objectinput,AddDatasetExampleOptionsoptions)`**

> Asynchronously adds an example to a dataset.

- Parameters:: datasetId- UUID of the dataset
- Returns:: future that completes when the example is added

**`UUIDaddDatasetExample(UUIDdatasetId,Objectinput,AddDatasetExampleOptionsoptions)`**

> Adds an example to a dataset for testing and evaluation.

- Parameters:: datasetId- UUID of the dataset
- Returns:: UUID of the added example

**`voidsetDatasetExampleInput(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Objectinput)`**

> Updates the input data for a specific dataset example.

- Parameters:: datasetId- UUID of the dataset

**`voidsetDatasetExampleReferenceOutput(UUIDdatasetId,StringsnapshotName,UUIDexampleId,ObjectreferenceOutput)`**

> Updates the reference output for a specific dataset example.

- Parameters:: datasetId- UUID of the dataset

**`voidremoveDatasetExample(UUIDdatasetId,StringsnapshotName,UUIDexampleId)`**

> Removes a specific example from a dataset.

- Parameters:: datasetId- UUID of the dataset

**`voidaddDatasetExampleTag(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Stringtag)`**

> Adds a tag to a specific dataset example for categorization.

- Parameters:: datasetId- UUID of the dataset

**`voidremoveDatasetExampleTag(UUIDdatasetId,StringsnapshotName,UUIDexampleId,Stringtag)`**

> Removes a tag from a specific dataset example.

- Parameters:: datasetId- UUID of the dataset

**`voidsnapshotDataset(UUIDdatasetId,StringfromSnapshotName,StringtoSnapshotName)`**

> Creates a snapshot of a dataset at its current state.

- Parameters:: datasetId- UUID of the dataset

**`voidremoveDatasetSnapshot(UUIDdatasetId,StringsnapshotName)`**

> Removes a specific snapshot from a dataset.

- Parameters:: datasetId- UUID of the dataset

**`Map<UUID,String>searchDatasets(StringsearchString,
 int limit)`**

> Searches for datasets by name or description.

- Parameters:: searchString- string to search for in names and descriptions
- Returns:: map from dataset UUID to dataset name

**`voidcreateEvaluator(Stringname,StringbuilderName,Mapparams,Stringdescription,CreateEvaluatorOptionsoptions)`**

> Creates an evaluator instance from a builder for measuring agent performance in experiments or actions.

- Parameters:: name- name for the evaluator

**`voidremoveEvaluator(Stringname)`**

> Removes an evaluator from the system.

- Parameters:: name- name of the evaluator to remove

**`voidcreateCategoricalHumanMetric(Stringname,Stringdescription,Set<String> categories)`**

> Creates a categorical human metric for collecting human feedback on agent runs.

 Human metrics allow humans to provide feedback on agent executions through the UI.
 Categorical metrics present a fixed set of category options for humans to choose from.
 The collected feedback is aggregated and displayed in time-series telemetry in the UI.

- Parameters:: name- unique name for the metric

**`voidcreateNumericHumanMetric(Stringname,Stringdescription,
 int min,
 int max)`**

> Creates a numeric human metric for collecting human feedback on agent runs.

 Human metrics allow humans to provide feedback on agent executions through the UI.
 Numeric metrics allow humans to provide a numeric value within a specified range.
 The collected feedback is aggregated and displayed in time-series telemetry in the UI.

- Parameters:: name- unique name for the metric

**`voidremoveHumanMetric(Stringname)`**

> Removes a human metric from the system.

- Parameters:: name- name of the human metric to remove

**`Set<String>searchEvaluators(StringsearchString)`**

> Searches for evaluators by name or description.

- Parameters:: searchString- string to search for in evaluator names
- Returns:: set of matching evaluator names

**`MaptryEvaluator(Stringname,Objectinput,ObjectreferenceOutput,Objectoutput)`**

> Tests an evaluator on a single sample input / reference output / output.

- Parameters:: name- name of the evaluator
- Returns:: result scores from score name to score value

**`MaptryComparativeEvaluator(Stringname,Objectinput,ObjectreferenceOutput,List<Object> outputs)`**

> Tests a comparative evaluator on multiple outputs.

- Parameters:: name- name of the evaluator
- Returns:: comparative evaluation result, a map of score name to score value

**`MaptrySummaryEvaluator(Stringname,List<ExampleRun> exampleRuns)`**

> Tests a summary evaluator on a collection of example runs.

- Parameters:: name- name of the evaluator
- Returns:: summary evaluation result with aggregate metrics, a map from score name to score value

---
### AgentModule

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentModule.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentModule.html)

```java
public abstract classAgentModuleextendsObjectimplements com.rpl.rama.RamaModule
```

Base class for creating agent modules that can be deployed to a Rama cluster.

 Alternatively, a regular RamaModule can be defined with anAgentTopologyexplicitly created to add agents to it.

 Agent modules are deployable units containing agent definitions, stores, and objects.
 They extend RamaModule and provide a simplified interface for defining agents
 and their associated infrastructure.

 The topology provides the configuration context for:Declaring agents withAgentTopology.newAgent(String)Declaring stores:AgentTopology.declareKeyValueStore(String, Class, Class),AgentTopology.declareDocumentStore(String, Class, Object...),AgentTopology.declarePStateStore(String, Class)Declaring agent objects:AgentTopology.declareAgentObject(String, Object),AgentTopology.declareAgentObjectBuilder(String, com.rpl.rama.ops.RamaFunction1)Declaring evaluators:AgentTopology.declareEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1),AgentTopology.declareComparativeEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1),AgentTopology.declareSummaryEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1)Declaring actions:AgentTopology.declareActionBuilder(String, String, com.rpl.rama.ops.RamaFunction1)Example:public class MyAgentModule extends AgentModule {
   @Override
   protected void defineAgents(AgentTopology topology) {
     topology.declareKeyValueStore("$$myStore", String.class, Integer.class);

     topology.newAgent("myAgent")
             .node("start", "process", (AgentNode agentNode, String input) -> {
               KeyValueStore<String, Integer> store = agentNode.getStore("$$myStore");
               store.put("key", 42);
               agentNode.emit("process", "Hello " + input);
             })
             .node("process", null, (AgentNode agentNode, String input) -> {
               agentNode.result("Processing: " + input);
             });
   }
 }

#### Method Summary

| Method | Description |
|---|---|
| `define(com.rpl.rama.RamaModule.Setup setup,
 com.rpl.rama.RamaModule.Topologies topologies)` |  |
| `defineAgents(AgentTopologytopology)` |  |

#### Method Details

**`protected abstractvoiddefineAgents(AgentTopologytopology)`**

**`publicvoiddefine(com.rpl.rama.RamaModule.Setup setup,
 com.rpl.rama.RamaModule.Topologies topologies)`**

- Specified by:: definein interfacecom.rpl.rama.RamaModule

---
### AgentNode

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentNode.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentNode.html)

```java
public interfaceAgentNodeextendsAgentObjectFetcher, com.rpl.agentorama.impl.IFetchAgentClient
```

Interface for agent node functions to interact with the agent execution environment.

 Agent nodes are the computational units within an agent graph. This interface
 provides access to agent objects, stores, streaming, recording trace information, and other execution capabilities.

 Example:topology.newAgent("myAgent")
         .node("start", "process", (AgentNode agentNode, String input) -> {
           ChatModel model = agentNode.getAgentObject("openai-model");
           KeyValueStore<String, Integer> store = agentNode.getStore("$$myStore");
           store.put("key", 42);
           agentNode.emit("process", "Hello " + input);
         })
         .node("process", null, (AgentNode agentNode, String input) -> {
           agentNode.result("Processing: " + input);
         });

#### Method Summary

| Method | Description |
|---|---|
| `emit(Stringnode,Object... args)` | Emits data to another node in the agent graph. |
| `getDepot(Stringname)` | Gets a depot client within a node. |
| `getHumanInput(Stringprompt)` | Requests human input during agent execution. |
| `getMetadata()` | Gets metadata associated with this agent execution. |
| `getMirrorAgentClient(StringmoduleName,StringagentName)` |  |
| `getMirrorDepot(StringmoduleName,Stringname)` | Gets a depot instance from another module. |
| `getMirrorQueryTopologyClient(StringmoduleName,Stringname)` | Gets a query topology client from another module. |
| `getMirrorStore(StringmoduleName,Stringname)` | Gets a store instance from another module. |
| `getQueryTopologyClient(Stringname)` | Gets a query topology client for invoking queries within a node. |
| `getStore(Stringname)` | Gets a store by name for persistent data access. |
| `recordNestedOp(NestedOpTypenestedOpType,
 long startTimeMillis,
 long finishTimeMillis,Map<String,Object> info)` | Records a nested operation for tracing and analytics. |
| `result(Objectarg)` | Sets the final result of the agent execution. |
| `streamChunk(Objectchunk)` | Streams a chunk of data to clients. |

#### Method Details

**`voidemit(Stringnode,Object... args)`**

> Emits data to another node in the agent graph.

 The target node must be declared in the outputNodesSpec when creating the agent.

- Parameters:: node- the name of the target node

**`voidresult(Objectarg)`**

> Sets the final result of the agent execution.

 This is a first-one wins situation: if multiple nodes return results in parallel,
 only the first one will be the agent result and others will be dropped.

- Parameters:: arg- the final result value

**`AgentClientgetMirrorAgentClient(StringmoduleName,StringagentName)`**

**`<T extendsStore>TgetStore(Stringname)`**

> Gets a store by name for persistent data access.

 Store names must start with "$$". Stores are declared in the agent topology using:
 -AgentTopology.declareKeyValueStore(String, Class, Class)for simple key-value storage
 -AgentTopology.declareDocumentStore(String, Class, Object...)for schema-flexible nested data
 -AgentTopology.declarePStateStore(String, Class)for direct Rama PState access

- Parameters:: name- the name of the store (must start with "$$")
- Returns:: the store instance

**`<T extendsStore>TgetMirrorStore(StringmoduleName,Stringname)`**

> Gets a store instance from another module.

 Stores provide distributed, persistent, replicated storage. Mirror stores are read-only.

- Parameters:: moduleName- the module where the store exists
- Returns:: the store instance with API methods (get, put, etc.)

**`com.rpl.rama.DepotgetDepot(Stringname)`**

> Gets a depot client within a node.

 Depots are Rama's append-only logs that can be consumed by any number of topologies.

- Parameters:: name- the name of the depot (declared in the module)
- Returns:: Depot instance for appending data

**`com.rpl.rama.DepotgetMirrorDepot(StringmoduleName,Stringname)`**

> Gets a depot instance from another module.

 Depots are Rama's append-only logs that can be consumed by any number of topologies.

- Parameters:: moduleName- the module where the depot exists
- Returns:: Depot instance for appending data

**`<T>com.rpl.rama.QueryTopologyClient<T>getQueryTopologyClient(Stringname)`**

> Gets a query topology client for invoking queries within a node.

- Parameters:: name- the name of the query topology
- Returns:: QueryTopologyClient instance

**`<T>com.rpl.rama.QueryTopologyClient<T>getMirrorQueryTopologyClient(StringmoduleName,Stringname)`**

> Gets a query topology client from another module.

- Parameters:: moduleName- the module where the query topology exists
- Returns:: QueryTopologyClient instance

**`voidstreamChunk(Objectchunk)`**

> Streams a chunk of data to clients.

- Parameters:: chunk- the data chunk to stream

**`voidrecordNestedOp(NestedOpTypenestedOpType,
 long startTimeMillis,
 long finishTimeMillis,Map<String,Object> info)`**

> Records a nested operation for tracing and analytics.

 Nested operations track internal operations like model calls, database access,
 and tool calls within an agent execution. The info map provides type-specific
 metadata for the operation.

 Special info map usage for certain operation types that gets incorporated into analytics:
 - model call: "inputTokenCount", "outputTokenCount", "totalTokenCount", "failure" (exception string for failures)

**`StringgetHumanInput(Stringprompt)`**

> Requests human input during agent execution.

 This method blocks until human input is provided. The agent execution
 will pause until the input is received. The agent will remain in a waiting state unti
 the human provides a response through the client API or web UI. Since nodes run on virtual threads, this is efficient.

- Parameters:: prompt- the prompt to show to the human
- Returns:: the human's response

**`Map<String,Object>getMetadata()`**

> Gets metadata associated with this agent execution.

- Returns:: map of metadata key-value pairs

---
### AgentObjectFetcher

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectFetcher.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectFetcher.html)

```java
public interfaceAgentObjectFetcher
```

Interface for accessing agent objects by name.

 Agent objects are shared resources (LLMs, APIs, databases, etc.) that are
 accessible by agent nodes during execution. They can be static objects or
 built on-demand with pooling and thread-safety considerations.

 Agent objects are declared in the agent topology using:AgentTopology.declareAgentObject(String, Object)for static objectsAgentTopology.declareAgentObjectBuilder(String, com.rpl.rama.ops.RamaFunction1)for on-demand objectsImplemented by:
 -AgentNode- provides access to agent objects within node functions
 -AgentObjectSetup- provides access to agent objects during object builder setup

#### Method Summary

| Method | Description |
|---|---|
| `getAgentObject(Stringname)` | Gets an agent object by name. |

#### Method Details

**`<T>TgetAgentObject(Stringname)`**

> Gets an agent object by name.

 When a node gets an object, it gets exclusive access to it. A pool of up to
 of size configured by the agent object builder is created on demand. Exception is when builder
 is configured to be thread-safe, in which case one object is created and shared for all usage within
 agents (no pool in this case).

- Type Parameters:: T- the type of the agent object
- Parameters:: name- the name of the agent object
- Returns:: the agent object instance

---
### AgentObjectOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectOptions.html)

```java
public interfaceAgentObjectOptions
```

Configuration options for agent object builders.
 
 AgentObjectOptions provides configuration for how agent objects are created,
 managed, and accessed within agent executions.
 
 Example:AgentObjectOptions options = AgentObjectOptions.create()
   .threadSafe()
   .workerObjectLimit(10)
   .disableAutoTracing();
 
 topology.declareAgentObjectBuilder("myObject", builder, options);

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates a new instance of agent object options. |
| `disableAutoTracing()` | Creates options with auto-tracing disabled. |
| `threadSafe()` | Creates options with thread-safe configuration. |
| `workerObjectLimit(int amt)` | Creates options with a specific worker object limit. |

#### Method Details

**`staticAgentObjectOptions.Implcreate()`**

> Creates a new instance of agent object options.

- Returns:: new options instance

**`staticAgentObjectOptions.ImplthreadSafe()`**

> Creates options with thread-safe configuration.
 
 When this is set, one object is created and shared for all usage
 within agents (no pool in this case).

- Returns:: options with thread safety enabled

**`staticAgentObjectOptions.ImpldisableAutoTracing()`**

> Creates options with auto-tracing disabled. When auto-tracing is enabled, chat models and
 embedding stores from Langchain4j are automatically wrapped to record all calls as nested operations.

- Returns:: options with auto-tracing disabled

**`staticAgentObjectOptions.ImplworkerObjectLimit(int amt)`**

> Creates options with a specific worker object limit.
 
 A pool of up to this many objects is created on demand at runtime in each Rama worker running the agent module.

- Parameters:: amt- the maximum number of objects to create
- Returns:: options with the specified worker object limit

---
### AgentObjectOptions.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectOptions.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectOptions.Impl.html)

```java
public static classAgentObjectOptions.ImplextendsObjectimplementsAgentObjectOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `disableAutoTracing()` | Creates options with auto-tracing disabled. |
| `threadSafe()` | Creates options with thread-safe configuration. |
| `workerObjectLimit(int amt)` | Creates options with a specific worker object limit. |

#### Method Details

**`publicAgentObjectOptions.ImplthreadSafe()`**

> Creates options with thread-safe configuration.
 
 When this is set, one object is created and shared for all usage
 within agents (no pool in this case).

- Returns:: options with thread safety enabled

**`publicAgentObjectOptions.ImpldisableAutoTracing()`**

> Creates options with auto-tracing disabled. When auto-tracing is enabled, chat models and
 embedding stores from Langchain4j are automatically wrapped to record all calls as nested operations.

- Returns:: options with auto-tracing disabled

**`publicAgentObjectOptions.ImplworkerObjectLimit(int amt)`**

> Creates options with a specific worker object limit.
 
 A pool of up to this many objects is created on demand at runtime in each Rama worker running the agent module.

- Parameters:: amt- the maximum number of objects to create
- Returns:: options with the specified worker object limit

---
### AgentObjectSetup

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectSetup.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentObjectSetup.html)

```java
public interfaceAgentObjectSetupextendsAgentObjectFetcher
```

Setup context for agent object builders.
 
 AgentObjectSetup provides access to other agent objects and the current object's
 name during the object building process. This is passed to agent object builder
 functions to enable objects to depend on other objects or access their own name.
 
 This interface is used when declaring agent object builders with:AgentTopology.declareAgentObjectBuilder(String, com.rpl.rama.ops.RamaFunction1)Example:topology.declareAgentObjectBuilder("myService", (AgentObjectSetup setup) -> {
   // Get the name of the object being built
   String objectName = setup.getObjectName();
   
   // Access other agent objects if needed
   DatabaseConnection db = setup.getAgentObject("database");
   Logger logger = setup.getAgentObject("logger");
   
   // Build the service with dependencies
   return new MyService(objectName, db, logger);
 });

#### Method Summary

| Method | Description |
|---|---|
| `getObjectName()` | Gets the name of the agent object being built. |

#### Method Details

**`StringgetObjectName()`**

> Gets the name of the agent object being built.
 
 This is the name that was specified when declaring the agent object builder
 in the topology.

- Returns:: the name of the agent object being built

---
### AgentRef

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentRef.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentRef.html)

```java
public interfaceAgentRef
```

Reference to an agent in a specific module.

#### Method Summary

| Method | Description |
|---|---|
| `getAgentName()` | Gets the name of the agent within the module. |
| `getModuleName()` | Gets the name of the module containing the agent. |

#### Method Details

**`StringgetModuleName()`**

> Gets the name of the module containing the agent.

- Returns:: the module name

**`StringgetAgentName()`**

> Gets the name of the agent within the module.

- Returns:: the agent name

---
### AgentStep

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStep.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStep.html)

```java
public interfaceAgentStep
```

Represents the result of invokingAgentClient.nextStep(AgentInvoke)orAgentClient.nextStepAsync(AgentInvoke).
 
 This is either aHumanInputRequest(indicating the agent requires input from a human)
 or anAgentComplete(indicating the agent has completed its task).

---
### AgentStream

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStream.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStream.html)

```java
public interfaceAgentStreamextendsCloseable
```

Stream for accessing data emitted from a single agent node invoke.
 
 The returned object can be closed to immediately stop streaming. The stream automatically closes when the node invoke completes.
 
 Example:AgentStream stream = client.stream(invoke, "myNode");
 
 // Get current chunks
 List<String> chunks = stream.get();
 
 // Check for resets
 int resets = stream.numResets();

#### Method Summary

| Method | Description |
|---|---|
| `get()` | Gets the current streamed chunks. |
| `numResets()` | Gets the number of times the stream has been reset. |

#### Method Details

**`<T>List<T>get()`**

> Gets the current streamed chunks.

- Type Parameters:: T- the type of chunks being streamed
- Returns:: list of current chunks

**`intnumResets()`**

> Gets the number of times the stream has been reset.
 
 Resets occur when nodes fail and retry, causing the stream to start over.

- Returns:: number of resets

---
### AgentStreamByInvoke

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStreamByInvoke.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentStreamByInvoke.html)

```java
public interfaceAgentStreamByInvokeextendsCloseable
```

Stream for accessing data emitted from all invocations of a specific node.
 
 The returned object can be closed to immediately stop streaming.
 
 Example:AgentStreamByInvoke stream = client.streamAll(invoke, "myNode");
 
 // Get current chunks grouped by invoke ID
 Map<UUID, List<String>> chunksByInvoke = stream.get();
 
 // Check resets per invoke
 Map<UUID, Long> resetsByInvoke = stream.numResetsByInvoke();
 
 stream.close();

#### Method Summary

| Method | Description |
|---|---|
| `get()` | Gets the current streamed chunks grouped by node invoke ID. |
| `numResetsByInvoke()` | Gets the number of resets per node invoke ID. |

#### Method Details

**`<T>Map<UUID,List<T>>get()`**

> Gets the current streamed chunks grouped by node invoke ID.

- Type Parameters:: T- the type of chunks being streamed
- Returns:: map from node invoke ID to list of chunks

**`Map<UUID,Long>numResetsByInvoke()`**

> Gets the number of resets per node invoke ID.
 
 Resets occur when nodes fail and retry, causing the stream to start over.

- Returns:: map from node invoke ID to number of resets

---
### AgentTopology

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentTopology.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/AgentTopology.html)

```java
public interfaceAgentTopology
```

The agent topology provides the configuration context for defining agents,
 stores, objects, evaluators, actions, and other infrastructure components
 within an agent module.

 The topology provides the configuration context for:Declaring agents withnewAgent(String)Declaring stores:declareKeyValueStore(String, Class, Class),declareDocumentStore(String, Class, Object...),declarePStateStore(String, Class)Declaring agent objects:declareAgentObject(String, Object),declareAgentObjectBuilder(String, com.rpl.rama.ops.RamaFunction1)Declaring evaluators:declareEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1),declareComparativeEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1),declareSummaryEvaluatorBuilder(String, String, com.rpl.rama.ops.RamaFunction1)Declaring actions:declareActionBuilder(String, String, com.rpl.rama.ops.RamaFunction1)Example:public class MyAgentModule extends AgentModule {
   @Override
   protected void defineAgents(AgentTopology topology) {
     topology.declareKeyValueStore("$$myStore", String.class, Integer.class);

     topology.declareAgentObject("openai-api-key", "sk-...");
     topology.declareAgentObjectBuilder("openai-model", setup -> {
       String apiKey = setup.getAgentObject("openai-api-key");
       return OpenAiChatModel.builder()
         .apiKey(apiKey)
         .modelName("gpt-4o-mini")
         .build();
     });

     // Create agents using builder pattern
     topology.newAgent("my-agent")
       .node("start", "process", (AgentNode agentNode, String input) -> {
         KeyValueStore<String, Integer> store = agentNode.getStore("$$myStore");
         store.put("key", 42);
         agentNode.emit("process", "Hello " + input);
       })
       .node("process", (AgentNode agentNode, String input) -> {
         OpenAiChatModel model = agentNode.getAgentObject("openai-model");
         agentNode.result(model.chat(input));
       });
   }
 }

#### Method Summary

| Method | Description |
|---|---|
| `create(com.rpl.rama.RamaModule.Setup setup,
 com.rpl.rama.RamaModule.Topologies topologies)` | Creates an agent topology for defining agents and infrastructure. |
| `declareActionBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,List<Input>,Output,RunInfo,Map>> builder)` | Declares an action builder for real-time evaluation on production runs. |
| `declareActionBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,List<Input>,Output,RunInfo,Map>> builder,ActionBuilderOptionsoptions)` | Declares an action builder with configuration options. |
| `declareAgentObject(Stringname,Objecto)` | Declares a static agent object that is shared across all agent executions. |
| `declareAgentObjectBuilder(Stringname,
 com.rpl.rama.ops.RamaFunction1<AgentObjectSetup,Object> builder)` | Declares an agent object builder that creates objects on demand. |
| `declareAgentObjectBuilder(Stringname,
 com.rpl.rama.ops.RamaFunction1<AgentObjectSetup,Object> builder,AgentObjectOptionsoptions)` | Declares an agent object builder with configuration options. |
| `declareComparativeEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,List<Output>,Map>> builder)` | Declares a comparative evaluator builder for comparing multiple outputs. |
| `declareComparativeEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,List<Output>,Map>> builder,EvaluatorBuilderOptionsoptions)` | Declares a comparative evaluator builder with configuration options. |
| `declareDocumentStore(Stringname,ClasskeyClass,Object... keyAndValClasses)` | Declares a document store for schema-flexible persistent storage. |
| `declareEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,Output,Map>> builder)` | Declares an evaluator builder for measuring agent performance. |
| `declareEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,Output,Map>> builder,EvaluatorBuilderOptionsoptions)` | Declares an evaluator builder with configuration options. |
| `declareKeyValueStore(Stringname,ClasskeyClass,ClassvalClass)` | Declares a key-value store for simple typed persistent storage. |
| `declarePStateStore(Stringname,
 com.rpl.rama.PState.Schema schema)` | Declares a PState store for direct access to Rama's built-in PState storage,
 which are stores defined as any combination of data structures of any size. |
| `declarePStateStore(Stringname,Classschema)` | Declares a PState store for direct access to Rama's built-in PState storage, |
| `declareSummaryEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction2<AgentObjectFetcher,List<ExampleRun>,Map>> builder)` | Declares a summary evaluator builder for aggregate metrics in experiments. |
| `declareSummaryEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction2<AgentObjectFetcher,List<ExampleRun>,Map>> builder,EvaluatorBuilderOptionsoptions)` | Declares a summary evaluator builder with configuration options. |
| `define()` | Defines the topology for deployment to a Rama cluster. |
| `getStreamTopology()` | Gets the underlying Rama stream topology. |
| `newAgent(Stringname)` | Creates a new agent with the specified name. |
| `newToolsAgent(Stringname,List<ToolInfo> tools)` | Creates a new tools agent with the specified name and tools. |
| `newToolsAgent(Stringname,List<ToolInfo> tools,ToolsAgentOptionsoptions)` | Creates a new tools agent with the specified name, tools, and options. |

#### Method Details

**`staticAgentTopologycreate(com.rpl.rama.RamaModule.Setup setup,
 com.rpl.rama.RamaModule.Topologies topologies)`**

> Creates an agent topology for defining agents and infrastructure. This is used when adding agents to a regular Rama module.

- Parameters:: setup- the setup object for module configuration
- Returns:: the agent topology instance

**`AgentGraphnewAgent(Stringname)`**

> Creates a new agent with the specified name.

- Parameters:: name- the name of the agent
- Returns:: the agent graph for defining the agent's execution flow

**`AgentGraphnewToolsAgent(Stringname,List<ToolInfo> tools)`**

> Creates a new tools agent with the specified name and tools.

 Tools agents are specialized agents for executing tool calls from AI models.
 They provide a standardized interface for function calling and tool execution.

- Parameters:: name- the name of the agent
- Returns:: the agent graph for defining the agent's execution flow

**`AgentGraphnewToolsAgent(Stringname,List<ToolInfo> tools,ToolsAgentOptionsoptions)`**

> Creates a new tools agent with the specified name, tools, and options.

- Parameters:: name- the name of the agent
- Returns:: the agent graph for defining the agent's execution flow

**`voiddeclareKeyValueStore(Stringname,ClasskeyClass,ClassvalClass)`**

> Declares a key-value store for simple typed persistent storage.

 Store names must start with "$$". Key-value stores provide basic
 persistent storage with type safety for simple data structures.

- Parameters:: name- the name of the store (must start with "$$")

**`voiddeclareDocumentStore(Stringname,ClasskeyClass,Object... keyAndValClasses)`**

> Declares a document store for schema-flexible persistent storage.

 Document stores provide flexible storage for nested data structures
 with schema validation capabilities.

- Parameters:: name- the name of the store (must start with "$$")

**`com.rpl.rama.PState.DeclarationdeclarePStateStore(Stringname,Classschema)`**

> Declares a PState store for direct access to Rama's built-in PState storage,

- Parameters:: name- the name of the store (must start with "$$")
- Returns:: the PState declaration for further configuration

**`com.rpl.rama.PState.DeclarationdeclarePStateStore(Stringname,
 com.rpl.rama.PState.Schema schema)`**

> Declares a PState store for direct access to Rama's built-in PState storage,
 which are stores defined as any combination of data structures of any size.
 PStates are durable, replicated, and scalable

- Parameters:: name- the name of the store (must start with "$$")
- Returns:: the PState declaration for further configuration

**`voiddeclareAgentObject(Stringname,Objecto)`**

> Declares a static agent object that is shared across all agent executions.

 Static objects are created once and reused for all agent executions.
 They are suitable for static information like API keys.

- Parameters:: name- the name of the agent object

**`voiddeclareAgentObjectBuilder(Stringname,
 com.rpl.rama.ops.RamaFunction1<AgentObjectSetup,Object> builder)`**

> Declares an agent object builder that creates objects on demand.

 When a node gets an object, it gets exclusive access to it. A pool of up to
 the configured object limit is created on demand. Exception is when the thread-safe option
 is set, in which case one object is created and shared for all usage within
 agents (no pool in this case).

- Parameters:: name- the name of the agent object

**`voiddeclareAgentObjectBuilder(Stringname,
 com.rpl.rama.ops.RamaFunction1<AgentObjectSetup,Object> builder,AgentObjectOptionsoptions)`**

> Declares an agent object builder with configuration options.

- Parameters:: name- the name of the agent object

**`<Input,RefOutput,Output>voiddeclareEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,Output,Map>> builder)`**

> Declares an evaluator builder for measuring agent performance.

 Evaluator builders return a map of scores, score name (string) to score value
 (string, boolean, or number). The "fetcher" argument can be used to get agent objects.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the evaluator builder

**`<Input,RefOutput,Output>voiddeclareEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,Output,Map>> builder,EvaluatorBuilderOptionsoptions)`**

> Declares an evaluator builder with configuration options.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the evaluator builder

**`<Input,RefOutput,Output>voiddeclareComparativeEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,List<Output>,Map>> builder)`**

> Declares a comparative evaluator builder for comparing multiple outputs.

 If a comparative evaluator returns with an "index" key, that is treated specially
 in the comparative experiment results UI to highlight that output as green as the better result.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the evaluator builder

**`<Input,RefOutput,Output>voiddeclareComparativeEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,Input,RefOutput,List<Output>,Map>> builder,EvaluatorBuilderOptionsoptions)`**

> Declares a comparative evaluator builder with configuration options.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the evaluator builder

**`voiddeclareSummaryEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction2<AgentObjectFetcher,List<ExampleRun>,Map>> builder)`**

> Declares a summary evaluator builder for aggregate metrics in experiments.

- Parameters:: name- the name of the evaluator builder

**`voiddeclareSummaryEvaluatorBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction2<AgentObjectFetcher,List<ExampleRun>,Map>> builder,EvaluatorBuilderOptionsoptions)`**

> Declares a summary evaluator builder with configuration options.

- Parameters:: name- the name of the evaluator builder

**`<Input,Output>voiddeclareActionBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,List<Input>,Output,RunInfo,Map>> builder)`**

> Declares an action builder for real-time evaluation on production runs.

 Actions are user-defined hooks running on live agent executions for
 real-time evaluation, data capture, etc. They can be parameterized and
 have concurrency limits controlled by the global config max.limited.actions.concurrency.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the action builder

**`<Input,Output>voiddeclareActionBuilder(Stringname,Stringdescription,
 com.rpl.rama.ops.RamaFunction1<Map<String,String>,com.rpl.rama.ops.RamaFunction4<AgentObjectFetcher,List<Input>,Output,RunInfo,Map>> builder,ActionBuilderOptionsoptions)`**

> Declares an action builder with configuration options.

- Type Parameters:: Input- the type of input data
- Parameters:: name- the name of the action builder

**`com.rpl.rama.module.StreamTopologygetStreamTopology()`**

> Gets the underlying Rama stream topology.

- Returns:: the stream topology instance

**`voiddefine()`**

> Defines the topology for deployment to a Rama cluster.

 This method must be called after all agents and infrastructure have been
 declared and is only used when adding an AgentTopology to a regular Rama module.

---
### BuiltIn

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/BuiltIn.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/BuiltIn.html)

```java
public classBuiltInextendsObject
```

Built-in aggregators for use with agent aggregation nodes.
 
 This class provides pre-configured aggregators that can be used withAgentTopology.newAgent(String)aggregation nodes. These aggregators
 are wrappers around Rama's built-in aggregation functions, adapted for use
 in agent-o-rama.

---
### CreateEvaluatorOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/CreateEvaluatorOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/CreateEvaluatorOptions.html)

```java
public classCreateEvaluatorOptionsextendsObject
```

---
### EvaluatorBuilderOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/EvaluatorBuilderOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/EvaluatorBuilderOptions.html)

```java
public interfaceEvaluatorBuilderOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates an empty EvaluatorBuilderOptions. |
| `param(Stringname,Stringdescription)` |  |
| `param(Stringname,Stringdescription,StringdefaultValue)` |  |
| `withoutInputPath()` |  |
| `withoutOutputPath()` |  |
| `withoutReferenceOutputPath()` |  |

#### Method Details

**`staticEvaluatorBuilderOptions.Implcreate()`**

> Creates an empty EvaluatorBuilderOptions.EvaluatorBuilderOptions.withoutInputPath()is the
 same asEvaluatorBuilderOptions.create().withoutInputPath()

**`staticEvaluatorBuilderOptions.Implparam(Stringname,Stringdescription)`**

**`staticEvaluatorBuilderOptions.Implparam(Stringname,Stringdescription,StringdefaultValue)`**

**`staticEvaluatorBuilderOptions.ImplwithoutInputPath()`**

**`staticEvaluatorBuilderOptions.ImplwithoutOutputPath()`**

**`staticEvaluatorBuilderOptions.ImplwithoutReferenceOutputPath()`**

---
### EvaluatorBuilderOptions.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/EvaluatorBuilderOptions.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/EvaluatorBuilderOptions.Impl.html)

```java
public static interfaceEvaluatorBuilderOptions.ImplextendsEvaluatorBuilderOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `param(Stringname,Stringdescription)` |  |
| `param(Stringname,Stringdescription,StringdefaultValue)` |  |
| `withoutInputPath()` |  |
| `withoutOutputPath()` |  |
| `withoutReferenceOutputPath()` |  |

#### Method Details

**`EvaluatorBuilderOptions.Implparam(Stringname,Stringdescription)`**

**`EvaluatorBuilderOptions.Implparam(Stringname,Stringdescription,StringdefaultValue)`**

**`EvaluatorBuilderOptions.ImplwithoutInputPath()`**

**`EvaluatorBuilderOptions.ImplwithoutOutputPath()`**

**`EvaluatorBuilderOptions.ImplwithoutReferenceOutputPath()`**

---
### ExampleRun

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ExampleRun.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ExampleRun.html)

```java
public interfaceExampleRun
```

Represents a single example run for summary evaluators..

#### Method Summary

| Method | Description |
|---|---|
| `create(Objectinput,ObjectreferenceOutput,Objectoutput)` | Creates a new example run with the specified input, reference output, and actual output. |
| `getInput()` | Gets the input data for this example run. |
| `getOutput()` | Gets the actual output for this example run. |
| `getReferenceOutput()` | Gets the reference output for this example run. |

#### Method Details

**`staticExampleRuncreate(Objectinput,ObjectreferenceOutput,Objectoutput)`**

> Creates a new example run with the specified input, reference output, and actual output.

- Parameters:: input- the input data for the example
- Returns:: the example run instance

**`<T>TgetInput()`**

> Gets the input data for this example run.

- Type Parameters:: T- the type of the input data
- Returns:: the input data

**`<T>TgetReferenceOutput()`**

> Gets the reference output for this example run.

- Type Parameters:: T- the type of the reference output
- Returns:: the reference output

**`<T>TgetOutput()`**

> Gets the actual output for this example run.

- Type Parameters:: T- the type of the actual output
- Returns:: the actual output

---
### FinishedAgg

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/FinishedAgg.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/FinishedAgg.html)

```java
public classFinishedAggextendsObject
```

Signals that an aggregator should immediately finish with a final value.

 When an aggregator implementation returns a FinishedAgg, the aggregation
 immediately completes with the specified value. All future values sent to
 the aggregator will be ignored.

 This is useful for implementing aggregators that can determine their final
 result early based on certain conditions.

#### Method Summary

| Method | Description |
|---|---|
| `getValue()` | Gets the final value for the aggregation. |

#### Method Details

**`publicObjectgetValue()`**

> Gets the final value for the aggregation.

- Returns:: the final value

---
### HumanInputRequest

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/HumanInputRequest.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/HumanInputRequest.html)

```java
public interfaceHumanInputRequestextendsAgentStep
```

Represents a request for human input during agent execution.
 
 When an agent callsAgentNode.getHumanInput(String)within a node function, it creates
 a human input request that must be responded to before the agent can continue.
 
 Example:AgentStep step = client.nextStep(invoke);
 if (step instanceof HumanInputRequest) {
   HumanInputRequest request = (HumanInputRequest) step;
   client.provideHumanInput(request, "Yes, proceed");
 }

#### Method Summary

| Method | Description |
|---|---|
| `getNode()` | Gets the name of the node that requested human input. |
| `getNodeInvokeId()` | Gets the unique ID of the node invocation that requested human input. |
| `getPrompt()` | Gets the prompt text shown to the human. |

#### Method Details

**`StringgetNode()`**

> Gets the name of the node that requested human input.

- Returns:: the node name

**`UUIDgetNodeInvokeId()`**

> Gets the unique ID of the node invocation that requested human input.

- Returns:: the node invoke ID

**`StringgetPrompt()`**

> Gets the prompt text shown to the human.

- Returns:: the prompt text

---
### IUnderlying

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/IUnderlying.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/IUnderlying.html)

```java
public interfaceIUnderlying
```

#### Method Summary

| Method | Description |
|---|---|
| `getUnderlying()` |  |

#### Method Details

**`<T>TgetUnderlying()`**

---
### MultiAgg

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/MultiAgg.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/MultiAgg.html)

```java
public interfaceMultiAgg
```

Creates an aggregator for use with aggregation nodes that supports multiple dispatch targets.
 
 MultiAgg provides dispatch-based aggregation where different values are processed by
 different aggregation functions based on a dispatch key. The first argument when emitting
 to the aggregation node is the dispatch target, which runs the corresponding handler.
 
 Example usage:MultiAgg.Impl agg = MultiAgg.create()
     .init(() -> Map.of("sum", 0, "texts", new ArrayList<>()))
     .on("add", (acc, value) -> {
         Map<String, Object> newAcc = new HashMap<>(acc);
         newAcc.put("sum", (Integer) newAcc.get("sum") + (Integer) value);
         return newAcc;
     })
     .on("text", (acc, text) -> {
         Map<String, Object> newAcc = new HashMap<>(acc);
         ((List<String>) newAcc.get("texts")).add((String) text);
         return newAcc;
     });

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates a new multi-aggregator instance. |
| `init(com.rpl.rama.ops.RamaFunction0<S> impl)` | Creates a multi-aggregator with an initial value function. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction1<S,Object> impl)` | Creates a multi-aggregator with a dispatch handler for zero arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction2<S,T0,Object> impl)` | Creates a multi-aggregator with a dispatch handler for one argument. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction3<S,T0,T1,Object> impl)` | Creates a multi-aggregator with a dispatch handler for two arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction4<S,T0,T1,T2,Object> impl)` | Creates a multi-aggregator with a dispatch handler for three arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction5<S,T0,T1,T2,T3,Object> impl)` | Creates a multi-aggregator with a dispatch handler for four arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction6<S,T0,T1,T2,T3,T4,Object> impl)` | Creates a multi-aggregator with a dispatch handler for five arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction7<S,T0,T1,T2,T3,T4,T5,Object> impl)` | Creates a multi-aggregator with a dispatch handler for six arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction8<S,T0,T1,T2,T3,T4,T5,T6,Object> impl)` | Creates a multi-aggregator with a dispatch handler for seven arguments. |

#### Method Details

**`staticMultiAgg.Implcreate()`**

> Creates a new multi-aggregator instance.

- Returns:: a new multi-aggregator builder

**`static<S>MultiAgg.Implinit(com.rpl.rama.ops.RamaFunction0<S> impl)`**

> Creates a multi-aggregator with an initial value function.

- Parameters:: impl- function that returns the initial aggregation value
- Returns:: a multi-aggregator builder with the initial value set

**`static<S>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction1<S,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for zero arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction2<S,T0,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for one argument.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction3<S,T0,T1,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for two arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1,T2>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction4<S,T0,T1,T2,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for three arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1,T2,T3>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction5<S,T0,T1,T2,T3,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for four arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1,T2,T3,T4>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction6<S,T0,T1,T2,T3,T4,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for five arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1,T2,T3,T4,T5>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction7<S,T0,T1,T2,T3,T4,T5,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for six arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

**`static<S,T0,T1,T2,T3,T4,T5,T6>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction8<S,T0,T1,T2,T3,T4,T5,T6,Object> impl)`**

> Creates a multi-aggregator with a dispatch handler for seven arguments.

- Parameters:: name- the dispatch target name
- Returns:: a multi-aggregator builder with the handler set

---
### MultiAgg.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/MultiAgg.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/MultiAgg.Impl.html)

```java
public static interfaceMultiAgg.Impl
```

Builder interface for configuring multi-aggregators.

#### Method Summary

| Method | Description |
|---|---|
| `init(com.rpl.rama.ops.RamaFunction0<S> impl)` | Sets the initial value function for the aggregation. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction1<S,Object> impl)` | Adds a dispatch handler for zero arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction2<S,T0,Object> impl)` | Adds a dispatch handler for one argument. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction3<S,T0,T1,Object> impl)` | Adds a dispatch handler for two arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction4<S,T0,T1,T2,Object> impl)` | Adds a dispatch handler for three arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction5<S,T0,T1,T2,T3,Object> impl)` | Adds a dispatch handler for four arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction6<S,T0,T1,T2,T3,T4,Object> impl)` | Adds a dispatch handler for five arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction7<S,T0,T1,T2,T3,T4,T5,Object> impl)` | Adds a dispatch handler for six arguments. |
| `on(Stringname,
 com.rpl.rama.ops.RamaFunction8<S,T0,T1,T2,T3,T4,T5,T6,Object> impl)` | Adds a dispatch handler for seven arguments. |

#### Method Details

**`<S>MultiAgg.Implinit(com.rpl.rama.ops.RamaFunction0<S> impl)`**

> Sets the initial value function for the aggregation.

- Parameters:: impl- function that returns the initial aggregation value
- Returns:: this builder for method chaining

**`<S>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction1<S,Object> impl)`**

> Adds a dispatch handler for zero arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction2<S,T0,Object> impl)`**

> Adds a dispatch handler for one argument.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction3<S,T0,T1,Object> impl)`**

> Adds a dispatch handler for two arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1,T2>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction4<S,T0,T1,T2,Object> impl)`**

> Adds a dispatch handler for three arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1,T2,T3>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction5<S,T0,T1,T2,T3,Object> impl)`**

> Adds a dispatch handler for four arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1,T2,T3,T4>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction6<S,T0,T1,T2,T3,T4,Object> impl)`**

> Adds a dispatch handler for five arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1,T2,T3,T4,T5>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction7<S,T0,T1,T2,T3,T4,T5,Object> impl)`**

> Adds a dispatch handler for six arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

**`<S,T0,T1,T2,T3,T4,T5,T6>MultiAgg.Implon(Stringname,
 com.rpl.rama.ops.RamaFunction8<S,T0,T1,T2,T3,T4,T5,T6,Object> impl)`**

> Adds a dispatch handler for seven arguments.

- Parameters:: name- the dispatch target name
- Returns:: this builder for method chaining

---
### NestedOpType

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/NestedOpType.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/NestedOpType.html)

```java
public enumNestedOpTypeextendsEnum<NestedOpType>
```

Types of nested operations that can be tracked within agent executions.
 
 Nested operations are internal operations like model calls, database access,
 and tool calls that are tracked for tracing and analytics purposes.

#### Method Summary

| Method | Description |
|---|---|
| `valueOf(Stringname)` | Returns the enum constant of this class with the specified name. |
| `values()` | Returns an array containing the constants of this enum class, in
the order they are declared. |

#### Method Details

**`public staticNestedOpType[]values()`**

> Returns an array containing the constants of this enum class, in
the order they are declared.

- Returns:: an array containing the constants of this enum class, in the order they are declared

**`public staticNestedOpTypevalueOf(Stringname)`**

> Returns the enum constant of this class with the specified name.
The string must matchexactlyan identifier used to declare an
enum constant in this class.  (Extraneous whitespace characters are 
not permitted.)

- Parameters:: name- the name of the enum constant to be returned.
- Returns:: the enum constant with the specified name
- Throws:: IllegalArgumentException- if this enum class has no constant with the specified name

---
### NodeInvoke

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/NodeInvoke.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/NodeInvoke.html)

```java
public interfaceNodeInvoke
```

Handle representing a specific execution instance of a node within an agent.
 
 Node invoke handles are used to track and identify individual node executions
 within an agent execution. They provide access to execution metadata and are
 used for streaming, forking, and other node-specific operations.

#### Method Summary

| Method | Description |
|---|---|
| `getNodeInvokeId()` | Gets the unique node invoke ID for this execution. |
| `getTaskId()` | Gets the task ID for this node execution. |

#### Method Details

**`longgetTaskId()`**

> Gets the task ID for this node execution.

- Returns:: the task ID

**`UUIDgetNodeInvokeId()`**

> Gets the unique node invoke ID for this execution.

- Returns:: the node invoke ID

---
### RunInfo

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/RunInfo.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/RunInfo.html)

```java
public interfaceRunInfo
```

#### Method Summary

| Method | Description |
|---|---|
| `getActionName()` | Returns name of the action. |
| `getAgentInvoke()` | Get agent invoke handle for the run |
| `getAgentName()` | Returns agent name this run was from. |
| `getAgentStats()` | Returns stats for agent run. |
| `getFeedback()` | Get all feedback on this run. |
| `getLatencyMillis()` | Return latency of this run. |
| `getNodeInvoke()` | If this is RunInfo for a node, returns node invoke information. |
| `getNodeName()` | If this is RunInfo for a node, returns node name this run was from. |
| `getNodeNestedOps()` | Returns nested op info for node run. |
| `getRuleName()` | Returns name of the rule for the action. |
| `getRunType()` | Returns whether this is a run info for an agent or a node. |
| `getStartTimeMillis()` | Returns the start time of the run. |

#### Method Details

**`StringgetRuleName()`**

> Returns name of the rule for the action.

- Returns:: rule name

**`StringgetActionName()`**

> Returns name of the action.

- Returns:: action name

**`StringgetAgentName()`**

> Returns agent name this run was from.

- Returns:: agent name

**`AgentInvokegetAgentInvoke()`**

> Get agent invoke handle for the run

- Returns:: agent invoke handle

**`StringgetNodeName()`**

> If this is RunInfo for a node, returns node name this run was from. Otherwise, returns null.

- Returns:: node name

**`NodeInvokegetNodeInvoke()`**

> If this is RunInfo for a node, returns node invoke information. Otherwise, returns null.

- Returns:: node invoke

**`RunTypegetRunType()`**

> Returns whether this is a run info for an agent or a node.

- Returns:: run type

**`longgetStartTimeMillis()`**

> Returns the start time of the run.

- Returns:: start time in milliseconds since epoch

**`LonggetLatencyMillis()`**

> Return latency of this run. May be null if the node failed and never completed.

- Returns:: latency in milliseconds

**`List<Feedback>getFeedback()`**

> Get all feedback on this run.

- Returns:: List of feedback in order in which they were given

**`AgentInvokeStatsgetAgentStats()`**

> Returns stats for agent run. This method returns null if this is a RunInfo for a node.

- Returns:: agent invoke stats

**`List<NestedOpInfo>getNodeNestedOps()`**

> Returns nested op info for node run. This method returns null if this is a RunInfo for an agent.

- Returns:: list of nested op infos in order of their execution

---
### RunType

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/RunType.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/RunType.html)

```java
public enumRunTypeextendsEnum<RunType>
```

Types of runs that can be tracked in the system.
 
 RunType distinguishes between agent-level runs and node-level runs
 for tracking and analytics purposes.

#### Method Summary

| Method | Description |
|---|---|
| `valueOf(Stringname)` | Returns the enum constant of this class with the specified name. |
| `values()` | Returns an array containing the constants of this enum class, in
the order they are declared. |

#### Method Details

**`public staticRunType[]values()`**

> Returns an array containing the constants of this enum class, in
the order they are declared.

- Returns:: an array containing the constants of this enum class, in the order they are declared

**`public staticRunTypevalueOf(Stringname)`**

> Returns the enum constant of this class with the specified name.
The string must matchexactlyan identifier used to declare an
enum constant in this class.  (Extraneous whitespace characters are 
not permitted.)

- Parameters:: name- the name of the enum constant to be returned.
- Returns:: the enum constant with the specified name
- Throws:: IllegalArgumentException- if this enum class has no constant with the specified name

---
### StreamingRecorder

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/StreamingRecorder.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/StreamingRecorder.html)

```java
public interfaceStreamingRecorder
```

#### Method Summary

| Method | Description |
|---|---|
| `streamChunk(Objectchunk)` |  |

#### Method Details

**`voidstreamChunk(Objectchunk)`**

---
### ToolInfo

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolInfo.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolInfo.html)

```java
public interfaceToolInfo
```

Combines a tool specification with its implementation function.
 
 ToolInfo is used to define tools that can be called by AI models in tools agents
 created withAgentTopology.newToolsAgent(String, java.util.List). It combines
 a LangChain4j ToolSpecification (which defines the tool's interface) with a function
 that implements the tool's behavior.
 
 Example:ToolSpecification spec = ToolSpecification.builder()
   .name("calculator")
   .description("Performs basic arithmetic operations")
   .parameters(jsonSchema)
   .build();
 
 ToolInfo toolInfo = ToolInfo.create(spec, (Map args) -> {
   int a = (Integer) args.get("a");
   int b = (Integer) args.get("b");
   return String.valueOf(a + b);
 });
 
 // Use with newToolsAgent
 topology.newToolsAgent("my-tools-agent", Arrays.asList(toolInfo));

#### Method Summary

| Method | Description |
|---|---|
| `create(dev.langchain4j.agent.tool.ToolSpecification spec,
 com.rpl.rama.ops.RamaFunction1<Map<String,T1>,String> toolFn)` | Creates a tool info with a simple function implementation. |
| `createWithContext(dev.langchain4j.agent.tool.ToolSpecification spec,
 com.rpl.rama.ops.RamaFunction3<AgentNode,T1,Map<String,T2>,String> toolFn)` | Creates a tool info with a function that has access to the agent node context. |
| `getToolSpecification()` | Gets the tool specification for this tool. |

#### Method Details

**`static<T1>ToolInfocreate(dev.langchain4j.agent.tool.ToolSpecification spec,
 com.rpl.rama.ops.RamaFunction1<Map<String,T1>,String> toolFn)`**

> Creates a tool info with a simple function implementation.

- Type Parameters:: T1- the type of the tool function's input
- Parameters:: spec- the tool specification defining the tool's interface
- Returns:: the tool info instance

**`static<T1,T2>ToolInfocreateWithContext(dev.langchain4j.agent.tool.ToolSpecification spec,
 com.rpl.rama.ops.RamaFunction3<AgentNode,T1,Map<String,T2>,String> toolFn)`**

> Creates a tool info with a function that has access to the agent node context.
 
 This allows the tool function to access agent objects, stores, and other
 agent execution context.

- Type Parameters:: T1- the type of the tool function's first input
- Parameters:: spec- the tool specification defining the tool's interface
- Returns:: the tool info instance

**`dev.langchain4j.agent.tool.ToolSpecificationgetToolSpecification()`**

> Gets the tool specification for this tool.

- Returns:: the tool specification

---
### ToolsAgentOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.html)

```java
public interfaceToolsAgentOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `create()` | Creates an empty ToolsAgentOptions. |
| `errorHandlerByType(ToolsAgentOptions.FunctionHandler... handlers)` |  |
| `errorHandlerDefault()` |  |
| `errorHandlerRethrow()` |  |
| `errorHandlerStaticString(Stringmessage)` |  |
| `errorHandlerStaticStringByType(ToolsAgentOptions.StaticStringHandler... handlers)` |  |

#### Method Details

**`staticToolsAgentOptions.Implcreate()`**

> Creates an empty ToolsAgentOptions.ToolsAgentOptions.errorHandlerRethrow()is the
 same asToolsAgentOptions.create().errorHandlerRethrow()

**`staticToolsAgentOptions.ImplerrorHandlerDefault()`**

**`staticToolsAgentOptions.ImplerrorHandlerStaticString(Stringmessage)`**

**`staticToolsAgentOptions.ImplerrorHandlerRethrow()`**

**`staticToolsAgentOptions.ImplerrorHandlerStaticStringByType(ToolsAgentOptions.StaticStringHandler... handlers)`**

**`staticToolsAgentOptions.ImplerrorHandlerByType(ToolsAgentOptions.FunctionHandler... handlers)`**

---
### ToolsAgentOptions.FunctionHandler

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.FunctionHandler.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.FunctionHandler.html)

```java
public static final classToolsAgentOptions.FunctionHandler<T extendsThrowable>extendsObject
```

#### Method Summary

| Method | Description |
|---|---|
| `create(Class<T> type,
 com.rpl.rama.ops.RamaFunction1<? super T,String> function)` |  |

#### Method Details

**`public static<T extendsThrowable>ToolsAgentOptions.FunctionHandler<T>create(Class<T> type,
 com.rpl.rama.ops.RamaFunction1<? super T,String> function)`**

---
### ToolsAgentOptions.Impl

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.Impl.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.Impl.html)

```java
public static interfaceToolsAgentOptions.ImplextendsToolsAgentOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `errorHandlerByType(ToolsAgentOptions.FunctionHandler... handlers)` |  |
| `errorHandlerDefault()` |  |
| `errorHandlerRethrow()` |  |
| `errorHandlerStaticString(Stringmessage)` |  |
| `errorHandlerStaticStringByType(ToolsAgentOptions.StaticStringHandler... handlers)` |  |

#### Method Details

**`ToolsAgentOptions.ImplerrorHandlerDefault()`**

**`ToolsAgentOptions.ImplerrorHandlerStaticString(Stringmessage)`**

**`ToolsAgentOptions.ImplerrorHandlerRethrow()`**

**`ToolsAgentOptions.ImplerrorHandlerStaticStringByType(ToolsAgentOptions.StaticStringHandler... handlers)`**

**`ToolsAgentOptions.ImplerrorHandlerByType(ToolsAgentOptions.FunctionHandler... handlers)`**

---
### ToolsAgentOptions.StaticStringHandler

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.StaticStringHandler.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ToolsAgentOptions.StaticStringHandler.html)

```java
public static final classToolsAgentOptions.StaticStringHandler<T extendsThrowable>extendsObject
```

#### Method Summary

| Method | Description |
|---|---|
| `create(Class<T> type,Stringmessage)` |  |

#### Method Details

**`public static<T extendsThrowable>ToolsAgentOptions.StaticStringHandler<T>create(Class<T> type,Stringmessage)`**

---
### UI

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UI.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UI.html)

```java
public classUIextendsObject
```

Java API for starting the Agent-o-rama web UI.The UI provides real-time monitoring of agent execution, state visualization, and debugging
 tools for agent development.

#### Method Summary

| Method | Description |
|---|---|
| `start(com.rpl.rama.test.InProcessCluster ipc)` | Start the Agent-o-rama web UI with default settings. |
| `start(com.rpl.rama.test.InProcessCluster ipc,UIOptionsoptions)` | Start the Agent-o-rama web UI with custom options. |

#### Method Details

**`public staticAutoCloseablestart(com.rpl.rama.test.InProcessCluster ipc)`**

> Start the Agent-o-rama web UI with default settings.

- Parameters:: ipc- the InProcessCluster to monitor
- Returns:: an AutoCloseable that can be used to stop the UI

**`public staticAutoCloseablestart(com.rpl.rama.test.InProcessCluster ipc,UIOptionsoptions)`**

> Start the Agent-o-rama web UI with custom options.

- Parameters:: ipc- the InProcessCluster to monitor
- Returns:: an AutoCloseable that can be used to stop the UI

---
### UI.Options

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UI.Options.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UI.Options.html)

```java
public static interfaceUI.Options
```

#### Method Summary

| Method | Description |
|---|---|
| `noInputBeforeClose()` |  |
| `port(int portNumber)` |  |

#### Method Details

**`staticUIOptionsport(int portNumber)`**

**`staticUIOptionsnoInputBeforeClose()`**

---
### UIOptions

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UIOptions.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UIOptions.html)

```java
public interfaceUIOptions
```

#### Method Summary

| Method | Description |
|---|---|
| `noInputBeforeClose()` |  |
| `port(int portNumber)` |  |

#### Method Details

**`UIOptionsport(int portNumber)`**

**`UIOptionsnoInputBeforeClose()`**

---
### UpdateMode

**Package:** `com.rpl.agentorama`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UpdateMode.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/UpdateMode.html)

```java
public enumUpdateModeextendsEnum<UpdateMode>
```

Controls how in-flight agent executions should be handled after a module is updated.
 
 When a module is updated while agent executions are running, this enum determines
 what happens to those executions.

#### Method Summary

| Method | Description |
|---|---|
| `valueOf(Stringname)` | Returns the enum constant of this class with the specified name. |
| `values()` | Returns an array containing the constants of this enum class, in
the order they are declared. |

#### Method Details

**`public staticUpdateMode[]values()`**

> Returns an array containing the constants of this enum class, in
the order they are declared.

- Returns:: an array containing the constants of this enum class, in the order they are declared

**`public staticUpdateModevalueOf(Stringname)`**

> Returns the enum constant of this class with the specified name.
The string must matchexactlyan identifier used to declare an
enum constant in this class.  (Extraneous whitespace characters are 
not permitted.)

- Parameters:: name- the name of the enum constant to be returned.
- Returns:: the enum constant with the specified name
- Throws:: IllegalArgumentException- if this enum class has no constant with the specified name

---
## Package `com.rpl.agentorama.analytics`

### Classes and Interfaces

- [AgentInvokeStats](#agentinvokestats)
- [BasicAgentInvokeStats](#basicagentinvokestats)
- [Feedback](#feedback)
- [NestedOpInfo](#nestedopinfo)
- [OpStats](#opstats)
- [SubagentInvokeStats](#subagentinvokestats)

### AgentInvokeStats

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/AgentInvokeStats.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/AgentInvokeStats.html)

```java
public interfaceAgentInvokeStats
```

Statistics for an agent invocation including subagent calls and basic metrics.
 
 AgentInvokeStats provides comprehensive analytics for a single agent execution,
 including performance metrics for the main agent and all subagent invocations
 that occurred during execution.

#### Method Summary

| Method | Description |
|---|---|
| `getBasicStats()` | Gets basic statistics for the main agent execution. |
| `getSubagentStats()` | Gets statistics for all subagent invocations that occurred during this execution. |

#### Method Details

**`Map<AgentRef,SubagentInvokeStats>getSubagentStats()`**

> Gets statistics for all subagent invocations that occurred during this execution.

- Returns:: map from agent reference to subagent invocation statistics

**`BasicAgentInvokeStatsgetBasicStats()`**

> Gets basic statistics for the main agent execution.

- Returns:: basic agent invocation statistics

---
### BasicAgentInvokeStats

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/BasicAgentInvokeStats.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/BasicAgentInvokeStats.html)

```java
public interfaceBasicAgentInvokeStats
```

Basic statistics for an agent invocation including token counts and operation metrics.
 
 BasicAgentInvokeStats provides fundamental performance and usage metrics for a single
 agent execution, including token consumption, nested operation statistics, and
 per-node performance data.

#### Method Summary

| Method | Description |
|---|---|
| `getInputTokenCount()` | Gets the total number of input tokens consumed during this execution. |
| `getNestedOpStats()` | Gets statistics for nested operations by type. |
| `getNodeStats()` | Gets statistics for each node execution within this agent. |
| `getOutputTokenCount()` | Gets the total number of output tokens generated during this execution. |
| `getTotalTokenCount()` | Gets the total number of tokens for this execution. |

#### Method Details

**`Map<NestedOpType,OpStats>getNestedOpStats()`**

> Gets statistics for nested operations by type.

- Returns:: map from nested operation type to operation statistics

**`intgetInputTokenCount()`**

> Gets the total number of input tokens consumed during this execution.

- Returns:: input token count

**`intgetOutputTokenCount()`**

> Gets the total number of output tokens generated during this execution.

- Returns:: output token count

**`intgetTotalTokenCount()`**

> Gets the total number of tokens for this execution.

- Returns:: total token count

**`Map<String,OpStats>getNodeStats()`**

> Gets statistics for each node execution within this agent.

- Returns:: map from node name to node execution statistics

---
### Feedback

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/Feedback.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/Feedback.html)

```java
public interfaceFeedback
```

Feedback data for agent execution evaluation and scoring.
 
 Feedback represents evaluation results and scores for agent executions,
 including information about the source of the feedback and when it was
 created or last modified.

#### Method Summary

| Method | Description |
|---|---|
| `getCreatedAt()` | Gets the timestamp when this feedback was created. |
| `getModifiedAt()` | Gets the timestamp when this feedback was last modified. |
| `getScores()` | Gets the evaluation scores for this feedback. |
| `getSource()` | Gets the source of this feedback. |

#### Method Details

**`Map<String,Object>getScores()`**

> Gets the evaluation scores for this feedback.

- Returns:: map from score name to score value (String, Boolean, or Number)

**`InfoSourcegetSource()`**

> Gets the source of this feedback.

- Returns:: the information source that generated this feedback

**`longgetCreatedAt()`**

> Gets the timestamp when this feedback was created.

- Returns:: creation timestamp in milliseconds since epoch

**`longgetModifiedAt()`**

> Gets the timestamp when this feedback was last modified.

- Returns:: modification timestamp in milliseconds since epoch

---
### NestedOpInfo

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/NestedOpInfo.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/NestedOpInfo.html)

```java
public interfaceNestedOpInfo
```

Information about a nested operation within an agent execution.
 
 NestedOpInfo represents a single nested operation (such as a model call,
 database access, or tool call) that occurred during agent execution.
 It includes timing information, operation type, and operation-specific metadata.

#### Method Summary

| Method | Description |
|---|---|
| `getFinishTimeMillis()` | Gets the finish time of this operation. |
| `getInfo()` | Gets operation-specific metadata and information. |
| `getStartTimeMillis()` | Gets the start time of this operation. |
| `getType()` | Gets the type of this nested operation. |

#### Method Details

**`longgetStartTimeMillis()`**

> Gets the start time of this operation.

- Returns:: start timestamp in milliseconds since epoch

**`longgetFinishTimeMillis()`**

> Gets the finish time of this operation.

- Returns:: finish timestamp in milliseconds since epoch

**`NestedOpTypegetType()`**

> Gets the type of this nested operation.

- Returns:: the nested operation type

**`Map<String,Object>getInfo()`**

> Gets operation-specific metadata and information.

- Returns:: map from string key to operation-specific value

---
### OpStats

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/OpStats.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/OpStats.html)

```java
public interfaceOpStats
```

Statistics for a specific type of operation.
 
 OpStats provides basic metrics for tracking the frequency and duration
 of operations, such as nested operations or node executions within
 an agent invocation.

#### Method Summary

| Method | Description |
|---|---|
| `getCount()` | Gets the number of times this operation was executed. |
| `getTotalTimeMillis()` | Gets the total time spent on this operation across all executions. |

#### Method Details

**`intgetCount()`**

> Gets the number of times this operation was executed.

- Returns:: operation count

**`longgetTotalTimeMillis()`**

> Gets the total time spent on this operation across all executions.

- Returns:: total time in milliseconds

---
### SubagentInvokeStats

**Package:** `com.rpl.agentorama.analytics`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/SubagentInvokeStats.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/analytics/SubagentInvokeStats.html)

```java
public interfaceSubagentInvokeStats
```

Statistics for subagent invocations within an agent execution.
 
 SubagentInvokeStats provides metrics for tracking how many times a specific
 subagent was invoked and the aggregated statistics across all those invocations.

#### Method Summary

| Method | Description |
|---|---|
| `getBasicStats()` | Gets the aggregated basic statistics across all invocations of this subagent. |
| `getCount()` | Gets the number of times this subagent was invoked. |

#### Method Details

**`intgetCount()`**

> Gets the number of times this subagent was invoked.

- Returns:: invocation count

**`BasicAgentInvokeStatsgetBasicStats()`**

> Gets the aggregated basic statistics across all invocations of this subagent.

- Returns:: aggregated basic agent invocation statistics

---
## Package `com.rpl.agentorama.ops`

### Classes and Interfaces

- [RamaVoidFunction](#ramavoidfunction)
- [RamaVoidFunction0](#ramavoidfunction0)
- [RamaVoidFunction1](#ramavoidfunction1)
- [RamaVoidFunction2](#ramavoidfunction2)
- [RamaVoidFunction3](#ramavoidfunction3)
- [RamaVoidFunction4](#ramavoidfunction4)
- [RamaVoidFunction5](#ramavoidfunction5)
- [RamaVoidFunction6](#ramavoidfunction6)
- [RamaVoidFunction7](#ramavoidfunction7)
- [RamaVoidFunction8](#ramavoidfunction8)

### RamaVoidFunction

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction.html)

```java
public interfaceRamaVoidFunctionextends com.rpl.rama.RamaSerializable
```

Base interface for custom function implementations with no return.

---
### RamaVoidFunction0

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction0.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction0.html)

```java
public interfaceRamaVoidFunction0extendsRamaVoidFunction
```

Interface for custom function implementations of zero arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke()` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke()`**

> Computes result of function from input arguments

---
### RamaVoidFunction1

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction1.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction1.html)

```java
public interfaceRamaVoidFunction1<T0>extendsRamaVoidFunction
```

Interface for custom function implementations of one argument

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0)`**

> Computes result of function from input arguments

---
### RamaVoidFunction2

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction2.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction2.html)

```java
public interfaceRamaVoidFunction2<T0,T1>extendsRamaVoidFunction
```

Interface for custom function implementations of two arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1)`**

> Computes result of function from input arguments

---
### RamaVoidFunction3

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction3.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction3.html)

```java
public interfaceRamaVoidFunction3<T0,T1,T2>extendsRamaVoidFunction
```

Interface for custom function implementations of three arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2)`**

> Computes result of function from input arguments

---
### RamaVoidFunction4

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction4.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction4.html)

```java
public interfaceRamaVoidFunction4<T0,T1,T2,T3>extendsRamaVoidFunction
```

Interface for custom function implementations of four arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2,T3arg3)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2,T3arg3)`**

> Computes result of function from input arguments

---
### RamaVoidFunction5

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction5.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction5.html)

```java
public interfaceRamaVoidFunction5<T0,T1,T2,T3,T4>extendsRamaVoidFunction
```

Interface for custom function implementations of five arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4)`**

> Computes result of function from input arguments

---
### RamaVoidFunction6

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction6.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction6.html)

```java
public interfaceRamaVoidFunction6<T0,T1,T2,T3,T4,T5>extendsRamaVoidFunction
```

Interface for custom function implementations of six arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5)`**

> Computes result of function from input arguments

---
### RamaVoidFunction7

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction7.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction7.html)

```java
public interfaceRamaVoidFunction7<T0,T1,T2,T3,T4,T5,T6>extendsRamaVoidFunction
```

Interface for custom function implementations of seven arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5,T6arg6)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5,T6arg6)`**

> Computes result of function from input arguments

---
### RamaVoidFunction8

**Package:** `com.rpl.agentorama.ops`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction8.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/ops/RamaVoidFunction8.html)

```java
public interfaceRamaVoidFunction8<T0,T1,T2,T3,T4,T5,T6,T7>extendsRamaVoidFunction
```

Interface for custom function implementations of eight arguments

#### Method Summary

| Method | Description |
|---|---|
| `invoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5,T6arg6,T7arg7)` | Computes result of function from input arguments |

#### Method Details

**`voidinvoke(T0arg0,T1arg1,T2arg2,T3arg3,T4arg4,T5arg5,T6arg6,T7arg7)`**

> Computes result of function from input arguments

---
## Package `com.rpl.agentorama.source`

### Classes and Interfaces

- [ActionSource](#actionsource)
- [AgentRunSource](#agentrunsource)
- [AiSource](#aisource)
- [ApiSource](#apisource)
- [BulkUploadSource](#bulkuploadsource)
- [EvalSource](#evalsource)
- [ExperimentSource](#experimentsource)
- [HumanSource](#humansource)
- [InfoSource](#infosource)

### ActionSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ActionSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ActionSource.html)

```java
public interfaceActionSourceextendsInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getRuleName()` |  |

#### Method Details

**`StringgetRuleName()`**

---
### AgentRunSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/AgentRunSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/AgentRunSource.html)

```java
public interfaceAgentRunSourceextendsInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getAgentInvoke()` |  |
| `getAgentName()` |  |
| `getModuleName()` |  |

#### Method Details

**`StringgetModuleName()`**

**`StringgetAgentName()`**

**`AgentInvokegetAgentInvoke()`**

---
### AiSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/AiSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/AiSource.html)

```java
public interfaceAiSourceextendsInfoSource
```

---
### ApiSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ApiSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ApiSource.html)

```java
public interfaceApiSourceextendsInfoSource
```

---
### BulkUploadSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/BulkUploadSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/BulkUploadSource.html)

```java
public interfaceBulkUploadSourceextendsInfoSource
```

---
### EvalSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/EvalSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/EvalSource.html)

```java
public interfaceEvalSourceextendsInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getEvalName()` |  |

#### Method Details

**`StringgetEvalName()`**

---
### ExperimentSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ExperimentSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/ExperimentSource.html)

```java
public interfaceExperimentSourceextendsInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getDatasetId()` |  |
| `getExperimentId()` |  |

#### Method Details

**`UUIDgetDatasetId()`**

**`UUIDgetExperimentId()`**

---
### HumanSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/HumanSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/HumanSource.html)

```java
public interfaceHumanSourceextendsInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getName()` |  |

#### Method Details

**`StringgetName()`**

---
### InfoSource

**Package:** `com.rpl.agentorama.source`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/InfoSource.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/source/InfoSource.html)

```java
public interfaceInfoSource
```

#### Method Summary

| Method | Description |
|---|---|
| `getSourceString()` |  |

#### Method Details

**`StringgetSourceString()`**

---
## Package `com.rpl.agentorama.store`

### Classes and Interfaces

- [DocumentStore](#documentstore)
- [KeyValueStore](#keyvaluestore)
- [PStateStore](#pstatestore)
- [Store](#store)

### DocumentStore

**Package:** `com.rpl.agentorama.store`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/DocumentStore.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/DocumentStore.html)

```java
public interfaceDocumentStore<K>extendsKeyValueStore<K,Map>
```

DocumentStore is like a key-value store where each value is a document (map) that can contain
 nested fields. Stores are distributed, durable, and replicated.
 
 Document stores are created usingAgentTopology.declareDocumentStore(String, Class, Object...).

#### Method Summary

| Method | Description |
|---|---|
| `containsDocumentField(Kkey,ObjectdocKey)` | Checks if a document contains a specific field. |
| `getDocumentField(Kkey,ObjectdocKey)` | Gets a field value from a document. |
| `getDocumentFieldOrDefault(Kkey,ObjectdocKey,ObjectdefaultValue)` | Gets a field value from a document, or returns a default value if not found. |
| `putDocumentField(Kkey,ObjectdocKey,Objectvalue)` | Sets a field value in a document. |
| `updateDocumentField(Kkey,ObjectdocKey,
 com.rpl.rama.ops.RamaFunction1<T,R> updateFunction)` | Updates a field value in a document using the provided function. |

#### Method Details

**`ObjectgetDocumentField(Kkey,ObjectdocKey)`**

> Gets a field value from a document.

- Parameters:: key- the document key
- Returns:: the field value, or null if not found

**`ObjectgetDocumentFieldOrDefault(Kkey,ObjectdocKey,ObjectdefaultValue)`**

> Gets a field value from a document, or returns a default value if not found.

- Parameters:: key- the document key
- Returns:: the field value, or the default value if not found

**`booleancontainsDocumentField(Kkey,ObjectdocKey)`**

> Checks if a document contains a specific field.

- Parameters:: key- the document key
- Returns:: true if the document contains the field, false otherwise

**`voidputDocumentField(Kkey,ObjectdocKey,Objectvalue)`**

> Sets a field value in a document.

- Parameters:: key- the document key

**`<T,R>voidupdateDocumentField(Kkey,ObjectdocKey,
 com.rpl.rama.ops.RamaFunction1<T,R> updateFunction)`**

> Updates a field value in a document using the provided function.

- Parameters:: key- the document key

---
### KeyValueStore

**Package:** `com.rpl.agentorama.store`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/KeyValueStore.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/KeyValueStore.html)

```java
public interfaceKeyValueStore<K,V>extendsPStateStore
```

Simple typed persistent storage for key-value pairs. Stores are distributed, durable, and replicated.

 Key-value stores are created usingAgentTopology.declareKeyValueStore(String, Class, Class).

#### Method Summary

| Method | Description |
|---|---|
| `containsKey(Kkey)` | Checks if the store contains the specified key. |
| `get(Kkey)` | Gets the value associated with the given key. |
| `getOrDefault(Kkey,VdefaultValue)` | Gets the value associated with the given key, or returns a default value if not found. |
| `put(Kkey,Vvalue)` | Associates the specified value with the specified key. |
| `update(Kkey,
 com.rpl.rama.ops.RamaFunction1<T,R> updateFunction)` | Updates the value associated with the given key using the provided function. |

#### Method Details

**`Vget(Kkey)`**

> Gets the value associated with the given key.

- Parameters:: key- the key to look up
- Returns:: the value associated with the key, or null if not found

**`VgetOrDefault(Kkey,VdefaultValue)`**

> Gets the value associated with the given key, or returns a default value if not found.

- Parameters:: key- the key to look up
- Returns:: the value associated with the key, or the default value if not found

**`voidput(Kkey,Vvalue)`**

> Associates the specified value with the specified key.

- Parameters:: key- the key

**`<T extendsV,R>voidupdate(Kkey,
 com.rpl.rama.ops.RamaFunction1<T,R> updateFunction)`**

> Updates the value associated with the given key using the provided function.

- Parameters:: key- the key to update

**`booleancontainsKey(Kkey)`**

> Checks if the store contains the specified key.

- Parameters:: key- the key to check
- Returns:: true if the store contains the key, false otherwise

---
### PStateStore

**Package:** `com.rpl.agentorama.store`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/PStateStore.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/PStateStore.html)

```java
public interfacePStateStoreextendsStore
```

Direct access to Rama's built-in PState storage.

 PStates are stores defined as any combination of data structures of any size.
 They are distributed, durable, and replicated, and read and written to with a flexible "path" API.

#### Method Summary

| Method | Description |
|---|---|
| `select(com.rpl.rama.Path path)` | Selects data using a path expression. |
| `select(ObjectpartitioningKey,
 com.rpl.rama.Path path)` | Selects data using a path expression with a partitioning key. |
| `selectOne(com.rpl.rama.Path path)` | Selects a single value using a path expression. |
| `selectOne(ObjectpartitioningKey,
 com.rpl.rama.Path path)` | Selects a single value using a path expression with a partitioning key. |
| `transform(ObjectpartitioningKey,
 com.rpl.rama.Path path)` | Transforms data using a path expression with a partitioning key. |

#### Method Details

**`<V>List<V>select(com.rpl.rama.Path path)`**

> Selects data using a path expression.

- Type Parameters:: V- the type of data being selected
- Parameters:: path- the path expression for data selection, e.gPath.key("a").mapVals()
- Returns:: list of selected values

**`<V>List<V>select(ObjectpartitioningKey,
 com.rpl.rama.Path path)`**

> Selects data using a path expression with a partitioning key.

- Type Parameters:: V- the type of data being selected
- Parameters:: partitioningKey- the partitioning key for the operation
- Returns:: list of selected values

**`<V>VselectOne(com.rpl.rama.Path path)`**

> Selects a single value using a path expression.

- Type Parameters:: V- the type of data being selected
- Parameters:: path- the path expression for data selection, e.gPath.key("a", "b")
- Returns:: the selected value, or null if not found

**`<V>VselectOne(ObjectpartitioningKey,
 com.rpl.rama.Path path)`**

> Selects a single value using a path expression with a partitioning key.

- Type Parameters:: V- the type of data being selected
- Parameters:: partitioningKey- the partitioning key for the operation
- Returns:: the selected value, or null if not found

**`voidtransform(ObjectpartitioningKey,
 com.rpl.rama.Path path)`**

> Transforms data using a path expression with a partitioning key.

- Parameters:: partitioningKey- the partitioning key for the operation

---
### Store

**Package:** `com.rpl.agentorama.store`
**Javadoc URL:** [https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/Store.html](https://redplanetlabs.com/aor/javadoc/com/rpl/agentorama/store/Store.html)

```java
public interfaceStore
```

Base interface for built-in persistent stores accessible from agent nodes.

 Store names must start with "$$" and are declared in the agent topology.

 Stores are distributed, durable, and replicated.

 Available store types:KeyValueStore- Simple typed key-value storageDocumentStore- Schema-flexible storage for nested dataPStateStore- Direct access to Rama's built-in PState storage

---
# Agent-O-Rama Clojuredoc API Reference

This document contains the Clojure API reference extracted from the Agent-O-Rama Clojuredoc.

**Source:** [https://redplanetlabs.com/aor/clojuredoc/index.html](https://redplanetlabs.com/aor/clojuredoc/index.html)

**Version:** 0.7.0

---

## Namespace `com.rpl.agent-o-rama`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.html)

### Public Variables and Functions

- [`add-dataset-example!`](#add-dataset-example)
- [`add-dataset-example-async!`](#add-dataset-example-async)
- [`add-dataset-example-tag!`](#add-dataset-example-tag)
- [`agent-client`](#agent-client)
- [`agent-fork`](#agent-fork)
- [`agent-fork-async`](#agent-fork-async)
- [`agent-initiate`](#agent-initiate)
- [`agent-initiate-async`](#agent-initiate-async)
- [`agent-initiate-fork`](#agent-initiate-fork)
- [`agent-initiate-fork-async`](#agent-initiate-fork-async)
- [`agent-initiate-with-context`](#agent-initiate-with-context)
- [`agent-initiate-with-context-async`](#agent-initiate-with-context-async)
- [`agent-invoke`](#agent-invoke)
- [`agent-invoke-async`](#agent-invoke-async)
- [`agent-invoke-complete?`](#agent-invoke-complete)
- [`agent-invoke-with-context`](#agent-invoke-with-context)
- [`agent-invoke-with-context-async`](#agent-invoke-with-context-async)
- [`agent-manager`](#agent-manager)
- [`agent-names`](#agent-names)
- [`agent-next-step`](#agent-next-step)
- [`agent-next-step-async`](#agent-next-step-async)
- [`agent-result`](#agent-result)
- [`agent-result-async`](#agent-result-async)
- [`agent-stream`](#agent-stream)
- [`agent-stream-all`](#agent-stream-all)
- [`agent-stream-reset-info`](#agent-stream-reset-info)
- [`agent-stream-specific`](#agent-stream-specific)
- [`agent-topology`](#agent-topology)
- [`agentmodule`](#agentmodule)
- [`agg-node`](#agg-node)
- [`agg-start-node`](#agg-start-node)
- [`create-dataset!`](#create-dataset)
- [`create-evaluator!`](#create-evaluator)
- [`declare-action-builder`](#declare-action-builder)
- [`declare-agent-object`](#declare-agent-object)
- [`declare-agent-object-builder`](#declare-agent-object-builder)
- [`declare-comparative-evaluator-builder`](#declare-comparative-evaluator-builder)
- [`declare-document-store`](#declare-document-store)
- [`declare-evaluator-builder`](#declare-evaluator-builder)
- [`declare-key-value-store`](#declare-key-value-store)
- [`declare-pstate-store`](#declare-pstate-store)
- [`declare-summary-evaluator-builder`](#declare-summary-evaluator-builder)
- [`defagentmodule`](#defagentmodule)
- [`define-agents!`](#define-agents)
- [`destroy-dataset!`](#destroy-dataset)
- [`emit!`](#emit)
- [`get-agent-object`](#get-agent-object)
- [`get-depot`](#get-depot)
- [`get-human-input`](#get-human-input)
- [`get-metadata`](#get-metadata)
- [`get-mirror-depot`](#get-mirror-depot)
- [`get-mirror-query-topology-client`](#get-mirror-query-topology-client)
- [`get-mirror-store`](#get-mirror-store)
- [`get-query-topology-client`](#get-query-topology-client)
- [`get-store`](#get-store)
- [`human-input-request?`](#human-input-request)
- [`mirror-agent-client`](#mirror-agent-client)
- [`mk-example-run`](#mk-example-run)
- [`multi-agg`](#multi-agg)
- [`new-agent`](#new-agent)
- [`node`](#node)
- [`pending-human-inputs`](#pending-human-inputs)
- [`pending-human-inputs-async`](#pending-human-inputs-async)
- [`provide-human-input`](#provide-human-input)
- [`provide-human-input-async`](#provide-human-input-async)
- [`record-nested-op!`](#record-nested-op)
- [`remove-dataset-example!`](#remove-dataset-example)
- [`remove-dataset-example-tag!`](#remove-dataset-example-tag)
- [`remove-dataset-snapshot!`](#remove-dataset-snapshot)
- [`remove-evaluator!`](#remove-evaluator)
- [`remove-metadata!`](#remove-metadata)
- [`result!`](#result)
- [`search-datasets`](#search-datasets)
- [`search-evaluators`](#search-evaluators)
- [`set-dataset-description!`](#set-dataset-description)
- [`set-dataset-example-input!`](#set-dataset-example-input)
- [`set-dataset-example-reference-output!`](#set-dataset-example-reference-output)
- [`set-dataset-name!`](#set-dataset-name)
- [`set-metadata!`](#set-metadata)
- [`set-update-mode`](#set-update-mode)
- [`setup-object-name`](#setup-object-name)
- [`snapshot-dataset!`](#snapshot-dataset)
- [`start-ui`](#start-ui)
- [`stop-ui`](#stop-ui)
- [`stream-chunk!`](#stream-chunk)
- [`try-comparative-evaluator`](#try-comparative-evaluator)
- [`try-evaluator`](#try-evaluator)
- [`try-summary-evaluator`](#try-summary-evaluator)
- [`underlying-stream-topology`](#underlying-stream-topology)

#### `add-dataset-example!`

```clojure
(add-dataset-example! manager dataset-id input)(add-dataset-example! manager dataset-id input options)
```

Adds an example to a dataset for testing and evaluation. Fails and throws exception of input or output violates the dataset’s JSON schemas.Args:manager - agent manager instancedataset-id - UUID of the datasetinput - Input data for the exampleoptions - Optional map with configuration (same as add-dataset-example-async!)Returns:UUID of the added exampleExample:```

(aor/add-dataset-example! agent-manager dataset-id
  {:query "What is AI?" :context "educational"}
  {:reference-output "AI is artificial intelligence..."
   :tags #{"basic" "ai"}})

```

---

#### `add-dataset-example-async!`

```clojure
(add-dataset-example-async! manager dataset-id input)(add-dataset-example-async! manager dataset-id input options)
```

Asynchronously adds an example to a dataset. Fails and throws exception of input or output violates the dataset’s JSON schemas.Args:manager - agent manager instancedataset-id - UUID of the datasetinput - Input data for the exampleoptions - Optional map with configuration::reference-output - Expected output for the example:tags - Set of tags for categorizationReturns:CompletableFuture- Future that completes with the example UUID

---

#### `add-dataset-example-tag!`

```clojure
(add-dataset-example-tag! manager dataset-id example-id tag)(add-dataset-example-tag! manager dataset-id example-id tag options)
```

Adds a tag to a specific dataset example for categorization.Args:manager - agent manager instancedataset-id - UUID of the datasetexample-id - UUID of the exampletag - String tag to add

---

#### `agent-client`

```clojure
(agent-client agent-client-fetcher agent-name)
```

Gets an agent client for interacting with a specific agent either in a client or within an agent node function.Agent clients provide the interface for invoking agents, streaming data, handling human input, and managing agent executions.When called from within an agent node function, this enables subagent execution:Can invoke any other agent in the same module (including the current agent)Enables recursive agent execution patternsEnables mutually recursive agent execution between different agentsSubagent calls are tracked and displayed in the UI traceArgs:agent-client-fetcher - either an agent manager or agent nodeagent-name - String name of the agentReturns:Interface for agent interactionExample:```

;; From client code
(let [client (aor/agent-client manager "my-agent")]
  (aor/agent-invoke client "Hello world"))
;; From within an agent node (subagent execution)
(fn [agent-node input]
  (let [subagent-client (aor/agent-client agent-node "helper-agent")
        result (aor/agent-invoke subagent-client input)]
    (aor/result! agent-node result)))

```

---

#### `agent-fork`

```clojure
(agent-fork agent-client invoke node-invoke-id->new-args)
```

Creates a fork of an agent execution with modified parameters for specific nodes.Forking allows creating execution branches with different inputs for testing variations or exploring alternative execution paths.Args:agent-client - agent clint instanceinvoke - agent invoke handle to fork fromnode-invoke-id->new-args - Map from node invoke ID (UUID) to new arguments. Node invoke IDs can be found in the trace UI.Returns:Result of the forked execution

---

#### `agent-fork-async`

```clojure
(agent-fork-async agent-client invoke node-invoke-id->new-args)
```

Asynchronously creates a fork of an agent execution.Forking allows creating execution branches with different inputs for testing variations or exploring alternative execution paths.Args:agent-client - AgentClient instanceinvoke - AgentInvoke instance to fork fromnode-invoke-id->new-args - Map from node invoke ID (UUID) to new arguments. Node invoke IDs can be found in the trace UI.Returns:CompletableFuture - Future that completes with the result of the forked execution

---

#### `agent-initiate`

```clojure
(agent-initiate agent-client & args)
```

Initiates an agent execution and returns a handle for tracking.This function starts an agent execution but doesn’t wait for completion. Use the returned result handle withagent-result,agent-next-step, or streaming functions to interact with the running agent.Args:agent-client - agent client instanceargs - Arguments to pass to the agentReturns:Agent invoke handle for tracking and interacting with the executionExample:```

(let [invoke (aor/agent-initiate client "Hello world")]
  (aor/agent-result client invoke))

```

---

#### `agent-initiate-async`

```clojure
(agent-initiate-async agent-client & args)
```

Asynchronously initiates an agent execution and returns a CompletableFuture with a handle for tracking.Args:agent-client - agent client instanceargs - Arguments to pass to the agentReturns:CompletableFuture- Future that completes with the handle

---

#### `agent-initiate-fork`

```clojure
(agent-initiate-fork agent-client invoke node-invoke-id->new-args)
```

Initiates a fork of an agent execution without waiting for completion.Args:agent-client - AgentClient instanceinvoke - AgentInvoke instance to fork fromnode-invoke-id->new-args - Map from node invoke ID (UUID) to new arguments. Node invoke IDs can be found in the trace UI.Returns:New agent invoke handle for the forked execution

---

#### `agent-initiate-fork-async`

```clojure
(agent-initiate-fork-async agent-client invoke node-invoke-id->new-args)
```

Asynchronously initiates a fork of an agent execution.Args:agent-client - AgentClient instanceinvoke - AgentInvoke instance to fork fromnode-invoke-id->new-args - Map from node invoke ID (UUID) to new arguments. Node invoke IDs can be found in the trace UI.Returns:Future that completes with the forked agent invoke handle

---

#### `agent-initiate-with-context`

```clojure
(agent-initiate-with-context agent-client context & args)
```

Initiates an agent execution with context metadata.Metadata allows attaching custom key-value data to agent executions. Metadata is an additional optional parameter to agent execution, and its also used for analytics. Metadata can be accessed anywhere inside agents by callingget-metadatawithin node functions.Args:agent-client - agent client instancecontext - Map with single key :metadata containing a map with string keys and values that are strings, numbers, or booleansargs - Arguments to pass to the agentReturns:Agent invoke handle for tracking and interacting with the executionExample:```

(aor/agent-initiate-with-context client
  {:metadata {"model" "openai"}}
  "Hello world")

```

---

#### `agent-initiate-with-context-async`

```clojure
(agent-initiate-with-context-async agent-client context & args)
```

Asynchronously initiates an agent execution with context metadata.Metadata allows attaching custom key-value data to agent executions. Metadata is an additional optional parameter to agent execution, and its also used for analytics. Metadata can be accessed anywhere inside agents by callingget-metadatawithin node functions.Args:agent-client - agent client instancecontext - Map with single key :metadata containing a map with string keys and values that are strings, numbers, or booleansargs - Arguments to pass to the agentReturns:CompletableFuture- Future that completes with the AgentInvoke handleExample:```

(aor/agent-initiate-with-context-async client
  {:metadata {"model" "openai"}}
  "Hello world")

```

---

#### `agent-invoke`

```clojure
(agent-invoke agent-client & args)
```

Synchronously invokes an agent with the provided arguments.This function blocks until the agent execution completes and returns the final result. For long-running agents, consider usingagent-initiatewithagent-resultfor better control.Args:agent-client - agent client instanceargs - Arguments to pass to the agentReturns:The final result from the agent executionExample:```

(aor/agent-invoke client "Hello world")
(aor/agent-invoke client {:query "What is AI?" :context "educational"})

```

---

#### `agent-invoke-async`

```clojure
(agent-invoke-async agent-client & args)
```

Asynchronously invokes an agent with the provided arguments.Returns a CompletableFuture that will complete with the agent’s result. This allows for non-blocking agent execution and better resource utilization.Args:agent-client - agent client instanceargs - Arguments to pass to the agentReturns:CompletableFuture - Future that completes with the agent result

---

#### `agent-invoke-complete?`

```clojure
(agent-invoke-complete? agent-client agent-invoke)
```

Checks if an agent invocation has completed.Args:agent-client - agent client instanceagent-invoke - agent invoke handleReturns:Boolean - True if the invocation has completed

---

#### `agent-invoke-with-context`

```clojure
(agent-invoke-with-context agent-client context & args)
```

Synchronously invokes an agent with context metadata.Metadata allows attaching custom key-value data to agent executions. Metadata is an additional optional parameter to agent execution, and its also used for analytics. Metadata can be accessed anywhere inside agents by callingget-metadatawithin node functions.Args:agent-client - agent client instancecontext - Map with single key :metadata containing a map with string keys and values that are strings, numbers, or booleansargs - Arguments to pass to the agentReturns:The final result from the agent executionExample:```

(aor/agent-invoke-with-context client
  {:metadata {"model" "openai"}}
  "Hello world")

```

---

#### `agent-invoke-with-context-async`

```clojure
(agent-invoke-with-context-async agent-client context & args)
```

Asynchronously invokes an agent with context metadata.Metadata allows attaching custom key-value data to agent executions. Metadata is an additional optional parameter to agent execution, and its also used for analytics. Metadata can be accessed anywhere inside agents by callingget-metadatawithin node functions.Args:agent-client - agent client instancecontext - Map with single key :metadata containing a map with string keys and values that are strings, numbers, or booleansargs - Arguments to pass to the agentReturns:CompletableFuture - Future that completes with the agent resultExample:```

(aor/agent-invoke-with-context-async client
  {:metadata {"model" "openai"}}
  "Hello world")

```

---

#### `agent-manager`

```clojure
(agent-manager cluster module-name)
```

Creates an agent manager for managing and interacting with deployed agents on a Rama cluster.The agent manager provides access to agent clients, dataset management, and evaluation capabilities for a specific module deployed on a cluster.Args:cluster - Rama cluster instance (IPC or remote cluster)module-name - String name of the deployed moduleReturns:Interface for managing agents and datasetsExample:```

(let [manager (aor/agent-manager ipc "MyModule")]
  (aor/agent-client manager "MyAgent"))

```

---

#### `agent-names`

```clojure
(agent-names agent-manager)
```

Gets the names of all available agents in a module.Args:agent-manager - agent manager instanceReturns:Set of agent names available in the moduleExample:```

(aor/agent-names manager) ; => #{"ChatAgent" "ProcessAgent" "ToolsAgent"}

```

---

#### `agent-next-step`

```clojure
(agent-next-step client agent-invoke)
```

Gets the next step in an agent execution for step-by-step control.Returns the next execution step, which is either a human input request or agent result. Check which one by callinghuman-input-request?. If it’s a result, it’s a record with a key`:result`in it. If the agent fails, it will throw an exception.Args:client - agent client instanceagent-invoke - agent invoke handleReturns:Either a human input request or agent result recordExample:```

(let [step (agent-next-step client invoke)]
  (if (human-input-request? step)
    (do-something-with-human-input step)
    (let [result (:result step)]
      (process-result result))))

```

---

#### `agent-next-step-async`

```clojure
(agent-next-step-async client agent-invoke)
```

Asynchronously gets the next step in an agent execution.Returns the next execution step, which is either a human input request or agent result. Check which one by callinghuman-input-request?. If it’s a result, it’s a record with a key`:result`in it. If the agent fails, it will deliver an exception.Args:client - agent client instanceagent-invoke - agent invoke handleReturns:CompletableFuture - Future that completes with either a human input request or agent result record

---

#### `agent-result`

```clojure
(agent-result agent-client agent-invoke)
```

Gets the final result from an agent execution.Blocks until the agent execution completes and returns the final result. For non-blocking access, useagent-result-async.Args:agent-client - agent client instanceagent-invoke - agent invoke handleReturns:The final result from the agent execution

---

#### `agent-result-async`

```clojure
(agent-result-async agent-client agent-invoke)
```

Asynchronously gets the final result from an agent execution.Args:agent-client - agent client instanceagent-invoke - agent invoke handleReturns:CompletableFuture - Future that completes with the agent result

---

#### `agent-stream`

```clojure
(agent-stream agent-client agent-invoke node)(agent-stream agent-client agent-invoke node callback-fn)
```

Creates a streaming subscription to receive data from a specific node.Streams data from the first invocation of the specified node during agent execution. Useful for real-time monitoring and progress tracking.The returned object can be deref’d to get the current streamed chunks (list of chunks).The returned object can have Closeable/close called on it to immediately stop streaming.Args:agent-client - agent client instanceagent-invoke - agent invoke handlenode - String name of the node to stream fromcallback-fn - Optional callback function for handling chunks. Takes 4 arguments: ‘all-chunks new-chunks reset? complete?’ where all-chunks is the complete list of chunks so far, new-chunks are the latest chunks, reset? indicates if the stream was reset because the node failed and retried, and complete? indicates if streaming is finishedReturns:Streaming subscription for the node.Example:```

(aor/agent-stream client invoke "process-node"
  (fn [all-chunks new-chunks reset? complete?]
    (when reset? (println "Stream was reset due to node retry"))
    (doseq [chunk new-chunks]
      (println "New chunk:" chunk))
    (when complete? (println "Streaming finished"))))

```

---

#### `agent-stream-all`

```clojure
(agent-stream-all agent-client agent-invoke node)(agent-stream-all agent-client agent-invoke node callback-fn)
```

Creates a streaming subscription to all invocations of a specific node.Streams data from all invocations of the specified node, with chunksgrouped by invocation ID. Useful for monitoring parallel processing.The returned object can be deref’d to get the current streamed chunks (map from node invoke ID to chunks).The returned object can have Closeable/close called on it to immediately stop streaming.Args:agent-client - agent client instanceagent-invoke - agent invoke handlenode - String name of the node to stream fromcallback-fn - Optional callback function for handling chunks. Takes 4 arguments: ‘all-chunks new-chunks reset-invoke-ids complete?’ where all-chunks is a map from node invoke ID to complete list of chunks, new-chunks are the latest chunks grouped by invoke ID, reset-invoke-ids indicates if any nodes invokes in this iteration failed and retried, and complete? indicates if streaming is finished across all nodes invocations for the full agent execution.Returns:Streaming subscription for all node invocationsExample:```

(aor/agent-stream-all client invoke "process-node"
  (fn [all-chunks new-chunks reset-invoke-ids complete?]
    (when (not (empty? reset-invoke-ids)) (println "Stream was reset for one or more node invokes"))
    (doseq [[invoke-id chunks] new-chunks]
      (println "New chunks for invocation" invoke-id ":" chunks))
    (when complete? (println "Streaming finished"))))

```

---

#### `agent-stream-reset-info`

```clojure
(agent-stream-reset-info stream)
```

Gets reset information from a streaming subscription.Returns reset information based on the stream type: - For streams created withagent-streamoragent-stream-specific: Number of resets - For streams created withagent-stream-all: Map from node invoke ID to reset countResets occur due to nodes failing and retrying.Args:stream - return fromagent-stream,agent-stream-all, oragent-stream-specificReturns:Number or Map - Reset count for single streams, or map of invoke ID to reset count for stream-all

---

#### `agent-stream-specific`

```clojure
(agent-stream-specific agent-client agent-invoke node node-invoke-id)(agent-stream-specific agent-client agent-invoke node node-invoke-id callback-fn)
```

Creates a streaming subscription to a specific node invocation.Streams data from a particular invocation of a node, useful whena node is invoked multiple times and you want to track a specific one.The returned object can be deref’d to get the current streamed chunks (list of chunks).The returned object can have Closeable/close called on it to immediately stop streaming.Args:agent-client - agent client instanceagent-invoke - agent invoke handlenode - String name of the node to stream fromnode-invoke-id - UUID of the specific node invocation to stream from. Node invoke IDs can be found in the trace UI.callback-fn - Optional callback function for handling chunks. Takes 4 arguments: ‘all-chunks new-chunks reset? complete?’ where all-chunks is the complete list of chunks so far, new-chunks are the latest chunks, reset? indicates if the stream was reset because the node failed and retried, and complete? indicates if streaming is finishedReturns:Streaming subscription for the specific node invocation

---

#### `agent-topology`

```clojure
(agent-topology setup topologies)
```

Creates a topology instance for defining agents, stores, and objects within a module. This function is used to add agents to a regular Rama module defined withdefmodule.The topology provides the configuration context for:Declaring agents withnew-agentDeclaring stores:declare-key-value-store,declare-document-store,declare-pstate-storeDeclaring agent objects:declare-agent-object,declare-agent-object-builderDeclaring evaluators:declare-evaluator-builder,declare-comparative-evaluator-builder,declare-summary-evaluator-builderDeclaring actions:declare-action-builderArgs:setup - Rama module setup instance from defmodule parameterstopologies - Rama module topologies instance from defmodule parametersReturns:agent topology instance

---

#### `agentmodule`

```clojure
(agentmodule & args)
```

Creates an anonymous agent module for packaging agents, stores, and objects into a deployable unit.An agent module is the top-level container that defines a complete agent system, encapsulating all resources needed for distributed agent execution. It provides the context for defining agents, stores, and shared objects within a Rama module.The topology provides the configuration context for:Declaring agents withnew-agentDeclaring stores:declare-key-value-store,declare-document-store,declare-pstate-storeDeclaring agent objects:declare-agent-object,declare-agent-object-builderDeclaring evaluators:declare-evaluator-builder,declare-comparative-evaluator-builder,declare-summary-evaluator-builderDeclaring actions:declare-action-builderArgs:options - Optional map with configuration::module-name - String name for the module (defaults to auto-generated)agent-topology-sym - Symbol for the agent topology binding in the bodybody - Forms that define agents, stores, and objects using the topologyReturns:Rama module that can be deployed to a clusterExample:```

(agentmodule
       [topology]
       (-> topology
           (aor/new-agent "my-agent")
           (aor/node "process" nil
             (fn [agent-node input]
               (aor/result! agent-node (str "Processed: " input))))))

```

---

#### `agg-node`

```clojure
(agg-node agent-graph name output-nodes-spec agg node-fn)
```

Adds an aggregation node that collects and combines results from multiple sources.Aggregation nodes gather results from parallel processing nodes and combine them using a specified aggregation function. They receive both the collected results and any metadata from the aggregation start node.Args:agent-graph - agent graph builder instancename - String name for the nodeoutput-nodes-spec - Target node name(s) for emissions, or nil for terminal nodes. Can be a string, vector of strings, or nil. Calls toemit!inside the node function must target one of these declared nodes.agg - Rama aggregator for combining results. Can be any Rama aggregator fromaggs namespaceorcustom aggregatorsnode-fn - Function that processes the aggregated results. Takes (agent-node aggregated-value agg-start-res) where agg-start-res is the return value from the correspondingagg-start-nodeExample:```

(-> topology
    (aor/new-agent "data-processor")
    (aor/agg-start-node "distribute-work" "process-chunk"
      (fn [agent-node {:keys [data chunk-size]}]
        (let [chunks (partition-all chunk-size data)]
          (doseq [chunk chunks]
            (aor/emit! agent-node "process-chunk" chunk)))))
    (aor/node "process-chunk" "agg-results"
      (fn [agent-node chunk]
        (let [processed (mapv #(* % %) chunk)
              chunk-sum (reduce + 0 processed)]
          (aor/emit! agent-node "agg-results" chunk-sum))))
    (aor/agg-node "agg-results" nil aggs/+sum
      (fn [agent-node total _]
        (aor/result! agent-node total))))

```

---

#### `agg-start-node`

```clojure
(agg-start-node agent-graph name output-nodes-spec node-fn)
```

Adds an aggregation start node that scopes aggregation within a subgraph.Aggregation start nodes work like regular nodes but define the beginning of an aggregation subgraph. They must have a correspondingagg-nodedownstream. Within the aggregation subgraph, edges must stay within the subgraph and cannot connect to nodes outside of it.The return value of the node function is passed to the downstreamagg-nodeas its last argument, allowing propagation of non-aggregated information downstream post-aggregation.Args:agent-graph - agent graph builder instancename - String name for the nodeoutput-nodes-spec - Target node name(s) for emissions, or nil for terminal nodes. Can be a string, vector of strings, or nil. Calls toemit!inside the node function must target one of these declared nodes.node-fn - Function that implements the node logic. Takes (agent-node & args) where args come from upstream emissions or agent invocation. Return value is passed to downstreamagg-nodeas last argument.Example:```

(-> topology
    (aor/new-agent "data-processor")
    (aor/agg-start-node "distribute-work" "process-chunk"
      (fn [agent-node {:keys [data chunk-size]}]
        (let [chunks (partition-all chunk-size data)]
          (doseq [chunk chunks]
            (aor/emit! agent-node "process-chunk" chunk)))))
    (aor/node "process-chunk" "collect-results"
      (fn [agent-node chunk]
        (let [processed (mapv #(* % %) chunk)
              chunk-sum (reduce + 0 processed)]
          (aor/emit! agent-node "agg-results" chunk-sum))))
    (aor/agg-node "agg-results" nil aggs/+sum
      (fn [agent-node total _]
        (aor/result! agent-node total))))

```

---

#### `create-dataset!`

```clojure
(create-dataset! manager name)(create-dataset! manager name options)
```

Creates a new dataset for agent testing and evaluation.Datasets are collections of input/output examples used for testing agent performance, running experiments, and regression testing.Args:manager - agent manager instancename - String name for the datasetoptions - Optional map with configuration::description - String description of the dataset:input-json-schema - JSON schema for input validation:output-json-schema - JSON schema for output validationReturns:UUID of the created datasetExample:```

(aor/create-dataset! agent-manager "test-cases"
  {:description "Basic test cases"
   :input-json-schema {"type" "object" "properties" {"query" {"type" "string"}} "required" ["query"]}
   :output-json-schema {"type" "string"}})

```

---

#### `create-evaluator!`

```clojure
(create-evaluator! manager name builder-name params description)(create-evaluator! manager name builder-name params description options)
```

Creates an evaluator instance from a builder for measuring agent performance in experiments or actions.Args:manager - agent manager instancename - String name for the evaluatorbuilder-name - String name of the evaluator builder (declared in topology or built-in)params - Map of parameters for the evaluator. Parameters are a map from parameter name to parameter value, both strings.description - String description of what the evaluator measuresoptions - Optional map with configuration::input-json-path - JSON path to extract input from runs:output-json-path - JSON path to extract output from runs:reference-output-json-path - JSON path to extract reference output from runsExample:```

(aor/create-evaluator! agent-manager "brief-check" "aor/conciseness"
  {"threshold" "150"} "Checks if response is under 150 characters")

```

---

#### `declare-action-builder`

```clojure
(declare-action-builder agent-topology name description builder-fn)(declare-action-builder agent-topology name description builder-fn options)
```

Declares an action builder for creating custom actions that run on agent executions.Actions are hooks that execute on a sampled subset of live agent runs for online evaluation, dataset capture, webhook triggers, or custom logic.Args:agent-topology - agent topology instancename - String name for the action builderdescription - String description of what the action doesbuilder-fn - Function that takes params map and returns an action functionoptions - Optional map with configuration::params - Map of parameter definitions for the action. Each param is a map with::description - String description of the parameter:default - String default value for the parameter:limit-concurrency? - Boolean, whether to limit concurrent executions of the action (e.g. to avoid hitting model rate limits). Concurrency is controlled in the UI by the global action max.limited.actions.concurrency setting (default false)Example:```

(declare-action-builder topology "telemetry-exporter"
  "Exports agent execution metrics to OpenTelemetry"
  (fn [params]  ; params is Map
    (let [service-name (get params "service-name" "agent-o-rama")
          endpoint (get params "otlp-endpoint")]
      (fn [fetcher input output run-info]
        (let [span-data {:service-name service-name
                         :operation-name "agent-execution"
                         :duration-ms (:latency-millis run-info)
                         :input-length (count (str input))
                         :output-length (count (str output))}]
          (send-to-otlp! endpoint span-data)))))
  {:params {"service-name" {:description "OpenTelemetry service name" :default "agent-o-rama"}
            "otlp-endpoint" {:description "OTLP collector endpoint"}}}})

```

---

#### `declare-agent-object`

```clojure
(declare-agent-object agent-topology name val)
```

Declares a static agent object that will be shared across all agent executions.Agent objects are shared resources like AI models, database connections, or API clients that agents can access during execution. Static objects are created once and reused.Args:agent-topology - agent topology instancename - String name for the object (used with get-agent-object)val - The object instance to shareExample:```

(declare-agent-object topology "openai-api-key" (System/getenv "OPENAI_API_KEY"))

```

---

#### `declare-agent-object-builder`

```clojure
(declare-agent-object-builder agent-topology name afn)(declare-agent-object-builder agent-topology name afn options)
```

Declares an agent object builder that creates objects on-demand during agent execution.Builder objects are created lazily when first accessed, allowing for complex initialization logic and dependency injection. When a node gets an object, it gets exclusive access to it. A pool of up to worker-object-limit objects is created on demand, except when thread-safe? is set, in which case one object is created and shared for all usage within agents.Args:agent-topology - agent topology instancename - String name for the object (used withget-agent-object)afn - Function that creates the objectoptions - Optional map with configuration::thread-safe? - Boolean, whether object is thread-safe (default false):auto-tracing? - Boolean, whether to auto-trace object calls (default true):worker-object-limit - Number, max objects per worker (default 1000)Example:```

(declare-agent-object-builder topology "openai-model"
  (fn [setup]
    (-> (OpenAiChatModel/builder)
        (.apiKey (get-agent-object setup "openai-api-key"))
        (.modelName "gpt-4o-mini")
        .build))
  {:thread-safe? true})

```

---

#### `declare-comparative-evaluator-builder`

```clojure
(declare-comparative-evaluator-builder agent-topology name description builder-fn)(declare-comparative-evaluator-builder agent-topology name description builder-fn options)
```

Declares a comparative evaluator builder for comparing multiple agent outputs.Comparative evaluators compare multiple agent outputs against a reference to determine which performs better, useful for A/B testing and model selection.Args:agent-topology - agent topology instancename - String name for the evaluator builderdescription - String description of what the evaluator comparesbuilder-fn - Function that takes params map and returns a comparative evaluator function. The evaluator function takes (fetcher input reference-output outputs) where fetcher can be used withget-agent-objectto access shared resources. Must return a map of scores: score name (string) to score value (string, boolean, or number). If the map contains an “index” key, that output will be highlighted as green in the comparative experiment results UI as the better resultoptions - Optional map with configuration::params - Map of parameter definitions for the builder. Each param is a map with::description - String description of the parameter:default - String default value for the parameter:input-path? - Boolean, whether user must specify JSON path to extract input value (default true):output-path? - Boolean, whether user must specify JSON path to extract output value (default true):reference-output-path? - Boolean, whether user must specify JSON path to extract reference output value (default true)Example:```

(declare-comparative-evaluator-builder topology "quality-ranker"
  "Ranks outputs by quality metric"
  (fn [params]  ; params is Map
    (let [weight (Double/parseDouble (get params "weight" "1.0"))]
      (fn [fetcher input reference-output outputs]
        (let [scored (map-indexed #(vector %1 %2 (+ (count %2) (* weight (if (str/includes? %2 "good") 10 0)))) outputs)
              best (apply max-key last scored)]
          {:best-index (first best)
           :best-output (second best)
           :best-score (last best)}))))
  {:params {"weight" {:description "Quality weight multiplier" :default "1.0"}}})

```

---

#### `declare-document-store`

```clojure
(declare-document-store agent-topology name key-class & key-val-classes)
```

Declares a document store in the agent topology.Document stores provide schema-flexible storage for complex nested data structures. Each document has a primary key and multiple typed fields that can be accessed independently.Args:agent-topology - agent topology instancename - String name for the store that must begin with`$$`(used withget-store)key-class - Class for document primary keys (e.g., String, Long)key-val-classes - Alternating field names (strings) and classes (e.g., “user-id” String “profile” Object “preferences” Map)Example:```

(declare-document-store topology "$$user-docs" String
  :profile UserProfile
  :preferences Map)

```

---

#### `declare-evaluator-builder`

```clojure
(declare-evaluator-builder agent-topology name description builder-fn)(declare-evaluator-builder agent-topology name description builder-fn options)
```

Declares an evaluator builder for creating custom evaluation functions for use in experiments or actions.Evaluators measure agent performance and can use AI models, databases, or custom logic to score agent outputs.Args:agent-topology - agent topology instancename - String name for the evaluator builderdescription - String description of what the evaluator measuresbuilder-fn - Function that takes params map and returns evaluator function. The evaluator function takes (fetcher input reference-output output) where fetcher can be used withget-agent-objectto access shared resources. Must return a map of scores: score name (string) to score value (string, boolean, or number)options - Optional map with configuration::params - Map of parameter definitions for the builder. Each param is a map with::description - String description of the parameter:default - String default value for the parameter:input-path? - Boolean, whether user must specify JSON path to extract input value (default true):output-path? - Boolean, whether user must specify JSON path to extract output value (default true):reference-output-path? - Boolean, whether user must specify JSON path to extract reference output value (default true)Example:```

(declare-evaluator-builder topology "length-checker"
  "Checks if text meets length criteria"
  (fn [params]  ; params is Map
    (let [max-len (Integer/parseInt (get params "maxLength" "100"))]
      (fn [fetcher input ref-output output]
        {"within-limit?" (<= (count output) max-len)
         "actual-length" (count output)})))
  {:params {"maxLength" {:description "Maximum allowed length" :default "100"}}
   :input-path? true
   :output-path? true
   :reference-output-path? false})

```

---

#### `declare-key-value-store`

```clojure
(declare-key-value-store agent-topology name key-class val-class)
```

Declares a key-value store in the agent topology.Key-value stores provide simple typed storage for agent state with automatic partitioning and distributed access.Args:agent-topology - agent topology instancename - String name for the store that must begin with`$$`(used withget-store)key-class - Class for store keys (e.g., String, Long)val-class - Class for store values (e.g., String, Object)Example:```

(declare-key-value-store topology "$$user-cache" String UserProfile)

```

---

#### `declare-pstate-store`

```clojure
(declare-pstate-store agent-topology name schema)
```

Declares a PState store that directly uses Rama’s built-in storage. PState stores are defined as any combination of durable, compound data structures.Args:agent-topology - agent topology instancename - String name for the store that must begin with`$$`(used withget-store)schema - Rama PState schema definitionReturns:The PState declaration for further configurationExample:```

(declare-pstate-store topology "$$user-stats" {String (fixed-keys-schema {:a String :b (set-schema Long)})})

```

---

#### `declare-summary-evaluator-builder`

```clojure
(declare-summary-evaluator-builder agent-topology name description builder-fn)(declare-summary-evaluator-builder agent-topology name description builder-fn options)
```

Declares a summary evaluator builder for evaluating collections of example runs.Summary evaluators analyze multiple example runs to produce aggregate metrics and insights about agent performance across a dataset.Args:agent-topology - agent topology instancename - String name for the evaluator builderdescription - String description of what the evaluator summarizesbuilder-fn - Function that takes params map and returns a summary evaluator function. The evaluator function takes (fetcher example-runs) where fetcher can be used withget-agent-objectto access shared resources. Must return a map of scores: score name (string) to score value (string, boolean, or number)options - Optional map with configuration::params - Map of parameter definitions for the builder. Each param is a map with::description - String description of the parameter:default - String default value for the parameter:input-path? - Boolean, whether user must specify JSON path to extract input value (default true):output-path? - Boolean, whether user must specify JSON path to extract output value (default true):reference-output-path? - Boolean, whether user must specify JSON path to extract reference output value (default true)Example:```

(declare-summary-evaluator-builder topology "accuracy-summary"
  "Calculates accuracy across multiple examples"
  (fn [params]  ; params is Map
    (let [threshold (Double/parseDouble (get params "threshold" "0.8"))]
      (fn [fetcher example-runs]
        (let [total (count example-runs)
              correct (count (filter #(= (:reference-output %) (:output %)) example-runs))
              accuracy (if (pos? total) (/ (double correct) total) 0.0)
              pass-rate (if (pos? total) (/ (count (filter #(>= accuracy threshold) example-runs)) total) 0.0)]
          {:total-examples total
           :correct-predictions correct
           :accuracy accuracy
           :pass-rate pass-rate}))))
  {:params {"threshold" {:description "Minimum accuracy threshold" :default "0.8"}}})

```

---

#### `defagentmodule`

```clojure
(defagentmodule sym & args)
```

Defines a named agent module for packaging agents, stores, and objects into a deployable unit.This is a convenience macro that creates a def binding for an agent module, automatically setting the module name to the symbol name. It’s the primary way to define agent modules in most applications.The topology provides the configuration context for:Declaring agents withnew-agentDeclaring stores:declare-key-value-store,declare-document-store,declare-pstate-storeDeclaring agent objects:declare-agent-object,declare-agent-object-builderDeclaring evaluators:declare-evaluator-builder,declare-comparative-evaluator-builder,declare-summary-evaluator-builderDeclaring actions:declare-action-builderArgs:sym - Symbol name for the module (becomes the module name)options - Optional map with configuration to override the module nameagent-topology-sym - Symbol for the agent topology binding in the bodybody - Forms that define agents, stores, and objects using the topologyReturns:Defines a Rama module that can be deployed to a clusterExample:```

(defagentmodule BasicAgentModule
  [topology]
  (-> topology
      (aor/new-agent "my-agent")
      (aor/node "process" nil
        (fn [agent-node user-name]
          (aor/result! agent-node (str "Welcome, " user-name "!"))))))

```

---

#### `define-agents!`

```clojure
(define-agents! at)
```

Finalizes the agent topology definition and prepares it for deployment. This is used when adding agents to a regular Rama module withagent-topology.This function must be called after all agents, stores, and objects have been declared on the topology. It validates the configuration and prepares the topology for module launch.Args:at - agent topology instance to finalize

---

#### `destroy-dataset!`

```clojure
(destroy-dataset! manager dataset-id)
```

Permanently deletes a dataset and all its examples.Args:manager - agent manager instancedataset-id - UUID of the dataset to delete

---

#### `emit!`

```clojure
(emit! agent-node node & args)
```

Emits data from the current node to the specified target node.This is the primary mechanism for data flow between nodes in agent graphs. Emissions trigger execution of downstream nodes with the provided arguments.Args:agent-node - agent node instance from the current node functionnode - String name of the target nodeargs - Arguments to pass to the target nodeExample:```

(aor/emit! agent-node "process" data)

```

---

#### `get-agent-object`

```clojure
(get-agent-object fetch name)
```

Gets a shared agent object (AI models, database clients, etc.) within a node, evaluator, or action function.Agent objects are shared resources declared in the topology that can be accessed by any node. They support automatic lifecycle management and connection pooling.Args:fetch - object fetcher instance (agent-node or first argument to evaluator or action function)name - String name of the object (declared with declare-agent-object*)Returns:The shared object instanceExample:```

(let [model (get-agent-object agent-node "openai-model")]
  (lc4j/chat model messages))

```

---

#### `get-depot`

```clojure
(get-depot agent-node name)
```

Gets a depot client within a node.Depots are Rama’s append-only logs that can be consumed by any number of topologies.Args:agent-node - agent node instance from the current node functionname - String name of the depot (declared in the module)Returns:Depot instance for appending data using foreign-append! or similar Rama functionsExample:```

(let [depot (get-depot agent-node "*my-depot")]
  (foreign-append! depot {:event "user-action" :data data}))

```

---

#### `get-human-input`

```clojure
(get-human-input agent-node prompt)
```

Requests human input during agent execution, blocking until response is received.This function pauses agent execution and requests input from a human user. The agent will remain in a waiting state until the human provides a response through the client API or web UI. Since nodes run on virtual threads, this is efficient.Args:agent-node - agent node instance from the current node functionprompt - String prompt to display to the human userReturns:String - The human’s responseExample:```

(defn human-yes?
 [agent-node prompt]
 (loop [res (aor/get-human-input agent-node prompt)]
   (cond (= res "yes") true
         (= res "no") false
         :else (recur (aor/get-human-input agent-node "Please answer 'yes' or 'no'.")))))

```

---

#### `get-metadata`

```clojure
(get-metadata client agent-invoke)(get-metadata agent-node)
```

Gets metadata associated with an agent invocation. Can be called from an agent client to get the metadata for that invoke, or can be called from within any agent node function.Metadata allows attaching custom key-value data to agent executions. Metadata is an additional optional parameter to agent execution, and its also used for analytics. Metadata can be accessed anywhere inside agents by callingget-metadatawithin node functions.Args:client - agent client instanceagent-invoke - agent invoke returned byagent-initiateORagent-node - agent node instance (for accessing within agent execution)Returns:Map - The metadata associated with the invocation or nodeExample:```

(get-metadata client agent-invoke)
(get-metadata agent-node)

```

---

#### `get-mirror-depot`

```clojure
(get-mirror-depot agent-node module-name name)
```

Gets a depot instance from another module.Depots are Rama’s append-only logs that can be consumed by any number of topologies.Args:agent-node - agent node instance from the current node functionmodule-name – module where the depot existsname - String name of the depot (declared in the target module)Returns:Depot instance for appending data using foreign-append! or similar Rama functionsExample:```

(let [depot (get-mirror-depot agent-node "com.mycompany/OtherModule" "*events-depot")]
  (foreign-append! depot {:event "cross-module-event" :data data}))

```

---

#### `get-mirror-query-topology-client`

```clojure
(get-mirror-query-topology-client agent-node module-name name)
```

Gets a query topology client from another module.Args:agent-node - agent node instance from the current node functionmodule-name – module where the query topology existsname - String name of the query topologyReturns:QueryTopologyClient instanceExample:```

(let [query-client (get-mirror-query-topology-client agent-node "com.mycompany/OtherModule" "analytics-query")]
  (let [result (foreign-invoke-query query-client "arg1" 100)]
    (process-analytics result)))

```

---

#### `get-mirror-store`

```clojure
(get-mirror-store agent-node module-name name)
```

Gets a store instance from another module.Stores provide distributed, persistent, replicated storage. Mirror stores are read-only.Args:agent-node - agent node instance from the current node functionmodule-name – module where the store existsname - String name of the store (declared with declare-*-store functions in the target module)Returns:Store instance with API methods in the com.rpl.agent-o-rama.store namespace (get, put!, etc.)Example:```

(let [store (get-mirror-store agent-node "com.mycompany/OtherModule" "$$some-kv-store")]
  (store/get store "user-123"))

```

---

#### `get-query-topology-client`

```clojure
(get-query-topology-client agent-node name)
```

Gets a query topology client for invoking queries within a node.Args:agent-node - agent node instance from the current node functionname - String name of the query topologyReturns:QueryTopologyClient instanceExample:```

(let [query-client (get-query-topology-client agent-node "my-query-topology")]
  (let [result (foreign-invoke-query query-client "arg1" 100)]
    (process-results result)))

```

---

#### `get-store`

```clojure
(get-store agent-node name)
```

Gets a store instance for accessing persistent storage within a node.Stores provide distributed, persistent, replicated storage that agents can use to maintain state across executions.Args:agent-node - agent node instance from the current node functionname - String name of the store (declared with declare-*-store functions)Returns:Store instance with API methods in the com.rpl.agent-o-rama.store namespace (get, put!, etc.)Example:```

(let [store (get-store agent-node "$$user-cache")]
  (store/put! store "user-123" user-data)
  (store/get store "user-123"))

```

---

#### `human-input-request?`

```clojure
(human-input-request? obj)
```

Checks if an object returned byagent-next-stepis a human input request.Args:obj - Object to checkReturns:Boolean - True if the object is a human input request

---

#### `mirror-agent-client`

```clojure
(mirror-agent-client agent-node module-name agent-name)
```

Gets and agent client for an agent in another module.Agent clients provide the interface for invoking agents, streaming data, handling human input, and managing agent executions.Example:```

;; From within an agent node (subagent execution)
(fn [agent-node input]
  (let [subagent-client (aor/mirror-agent-client agent-node "com.mycompany/SomeAgentModule ""other-agent")
        result (aor/agent-invoke subagent-client input)]
    (aor/result! agent-node result)))

```

---

#### `mk-example-run`

```clojure
(mk-example-run input reference-output output)
```

Creates an example run for summary evaluation withtry-summary-evaluator.Args:input - Input data for the examplereference-output - Expected outputoutput - Actual outputReturns:Example run instance for summary evaluation

---

#### `multi-agg`

```clojure
(multi-agg & body)
```

Creates an aggregator for use withagg-nodethat supports multiple dispatch targets.The first argument when emitting to the agg node is the dispatch target, which runs the corresponding`on`declaration.Args:body - Forms defining the multi-aggregation:```
(init [bindings] & body)
```- Returns the initial aggregation value```
(on dispatch-target [agg-value & additional-args] & body)
```- Handler for each dispatch target. Takes the current aggregation value plus additional arguments from the emit! callExample:```

(multi-agg
  (init [] {:sum 0 :texts []})
  (on "add" [acc value]
    (update acc :sum + value))
  (on "text" [acc text]
    (update acc :texts conj text)))

```

---

#### `new-agent`

```clojure
(new-agent agent-topology name)
```

Creates a new agent graph builder for defining an agent’s execution flow.Returns an object that can be configured with nodes, edges, and execution logic. Agents are defined as directed graphs where nodes represent computation steps and edges define data flow. Graphs can contain loops for iterative processing.Args:agent-topology - agent topology instancename - String name for the agent (must be unique within the module)Returns:Builder for configuring the agent’s execution graphExample:```

(-> topology
    (aor/new-agent "text-processor")
    (aor/node "start" "process"
      (fn [agent-node input]
        (let [preprocessed (str/trim (str/upper-case input))]
          (aor/emit! agent-node "process" preprocessed))))
    (aor/node "process" nil
      (fn [agent-node text]
        (let [processed (str/replace text #"[^A-Z ]" "")]
          (aor/result! agent-node processed)))))

```

---

#### `node`

```clojure
(node agent-graph name output-nodes-spec node-fn)
```

Adds a node to an agent graph created withnew-agentwith specified execution logic.Nodes are the fundamental computation units in agent graphs. Each node receives data from upstream nodes and can emit data to downstream nodes or return a final result.Args:agent-graph - agent graph builder instancename - String name for the node (must be unique within the agent)output-nodes-spec - Target node name(s) for emissions, or nil for terminal nodes. Can be a string, vector of strings, or nil. Calls toemit!inside the node function must target one of these declared nodes.node-fn - Function that implements the node logic. Takes (agent-node & args) where args come from upstream emissions or agent invocation.Example:```

(node agent-graph "process" "finalize"
  (fn [agent-node data]
    (let [processed (transform data)]
      (emit! agent-node "finalize" processed))))

```

---

#### `pending-human-inputs`

```clojure
(pending-human-inputs client agent-invoke)
```

Gets all pending human input requests for an agent invocation handle.Returns a collection of request objects that are waiting for human responses to continue agent execution.Args:client - agent client instanceagent-invoke - agent invoke handleReturns: - Collection - Pending human input requests. Each request has fields`:node`and`:prompt`to get the node name making the request and the prompt.Example:```

(let [requests (aor/pending-human-inputs client invoke)]
  (doseq [request requests]
    (aor/provide-human-input client request "yes")))

```

---

#### `pending-human-inputs-async`

```clojure
(pending-human-inputs-async client agent-invoke)
```

Asynchronously gets all pending human input requests for an agent invocation.Args:client - agent client instanceagent-invoke - agent invoke handleReturns:CompletableFuture - Future with pending requests. Each request has fields`:node`and`:prompt`to get the node name making the request and the prompt.

---

#### `provide-human-input`

```clojure
(provide-human-input client request response)
```

Provides a human response to a pending human input request.This function sends a response to continue agent execution that was paused waiting for human input.Args:client - agent client instancerequest - request object frompending-human-inputsoragent-next-stepresponse - String response from the humanExample:```

(aor/provide-human-input agent-client request "yes")

```

---

#### `provide-human-input-async`

```clojure
(provide-human-input-async client request response)
```

Asynchronously provides a human response to a pending human input request.Args:client - agent client instancerequest - request object frompending-human-inputsoragent-next-stepresponse - String response from the humanReturns:CompletableFuture - Future that completes when the response is processed

---

#### `record-nested-op!`

```clojure
(record-nested-op! agent-node nested-op-type start-time-millis finish-time-millis info-map)
```

Records a nested operation for tracing and performance monitoring.This function is used by the framework to track operations like AI model calls, database queries, and external API calls, is viewable in the trace in the UI, and is included in aggregated statistics about agent execution.Args:agent-node - agent node instance from the current node functionnested-op-type - Keyword type of the operation. Must be one of::store-read, :store-write, :db-read, :db-write, :model-call,:tool-call, :agent-call, :human-input, :otherstart-time-millis - Long start time of the operationfinish-time-millis - Long finish time of the operationinfo-map - Map from String to value with additional operation metadata. For :model-call, include “inputTokenCount”, “outputTokenCount”, “totalTokenCount” for analytics, or “failure” with exception string for failures.

---

#### `remove-dataset-example!`

```clojure
(remove-dataset-example! manager dataset-id example-id)(remove-dataset-example! manager dataset-id example-id options)
```

Removes a specific example from a dataset.Args:manager - agent manager instancedataset-id - UUID of the datasetexample-id - UUID of the example to remove

---

#### `remove-dataset-example-tag!`

```clojure
(remove-dataset-example-tag! manager dataset-id example-id tag)(remove-dataset-example-tag! manager dataset-id example-id tag options)
```

Removes a tag from a specific dataset example.Args:manager - agent manager instancedataset-id - UUID of the datasetexample-id - UUID of the exampletag - String tag to remove

---

#### `remove-dataset-snapshot!`

```clojure
(remove-dataset-snapshot! manager dataset-id snapshot-name)
```

Removes a specific snapshot from a dataset.Args:manager - agent manager instancedataset-id - UUID of the datasetsnapshot-name - String name of the snapshot to remove

---

#### `remove-evaluator!`

```clojure
(remove-evaluator! manager name)
```

Removes an evaluator from the system.Args:manager - agent manager instancename - String name of the evaluator to remove

---

#### `remove-metadata!`

```clojure
(remove-metadata! client agent-invoke key)
```

Removes metadata from an agent invocation.Note: This only affects metadata visible to external clients and analytics. For agent execution within nodes, only the metadata provided at invocation time viaagent-invoke-with-contextoragent-initiate-with-contextis accessible viaget-metadata.Args:client - agent client instanceagent-invoke - agent invoke handlekey - String key of the metadata to remove

---

#### `result!`

```clojure
(result! agent-node val)
```

Sets the final result for the agent that will be displayed in the UI and returned for calls toagent-resultandagent-invoke.This function signals completion of the agent execution and returns the final result to the client. If multiple nodes call`result!`in parallel, only the first one will be used as the agent result and others will be dropped (first-one-wins behavior). It is mutually exclusive with emit! - a node should either emit to other nodes or return a result, not both.Args:agent-node - agent node instance from the current node functionval - The final result value to return to the clientExample:```

(result! agent-node {:status "success" :data processed-data})

```

---

#### `search-datasets`

```clojure
(search-datasets manager search-string limit)
```

Searches for datasets by name or description.Args:manager - agent manager instancesearch-string - String to search for in names and descriptionslimit - Maximum number of results to returnReturns:Map - Map from dataset UUID to dataset name

---

#### `search-evaluators`

```clojure
(search-evaluators manager search-string)
```

Searches for evaluators by name or description.Args:manager - agent manager instancesearch-string - String to search for in evaluator namesReturns:Set - Set of matching evaluator names

---

#### `set-dataset-description!`

```clojure
(set-dataset-description! manager dataset-id description)
```

Updates the description of an existing dataset.Args:manager - agent manager instancedataset-id - UUID of the datasetdescription - String new description for the dataset

---

#### `set-dataset-example-input!`

```clojure
(set-dataset-example-input! manager dataset-id example-id input)(set-dataset-example-input! manager dataset-id example-id input options)
```

Updates the input data for a specific dataset example.Args:manager - agent manager instancedataset-id - UUID of the datasetexample-id - UUID of the exampleinput - New input data for the example

---

#### `set-dataset-example-reference-output!`

```clojure
(set-dataset-example-reference-output! manager dataset-id example-id reference-output)(set-dataset-example-reference-output! manager dataset-id example-id reference-output options)
```

Updates the reference output for a specific dataset example.Args:manager - agent manager instancedataset-id - UUID of the datasetexample-id - UUID of the examplereference-output - New reference output for the example

---

#### `set-dataset-name!`

```clojure
(set-dataset-name! manager dataset-id name)
```

Updates the name of an existing dataset.Args:manager - agent manager instancedataset-id - UUID of the datasetname - String new name for the dataset

---

#### `set-metadata!`

```clojure
(set-metadata! client agent-invoke key value)
```

Sets metadata on an agent invocation for tracking and debugging.Note: This only affects metadata visible to external clients and analytics. For agent execution within nodes, only the metadata provided at invocation time viaagent-invoke-with-contextoragent-initiate-with-contextis accessible viaget-metadata.Args:client - agent client instanceagent-invoke - agent invoke handlekey - String key for the metadatavalue - Value to store (must be a restricted type: int, long, float, double, boolean, or string)Example:```

(set-metadata! client invoke "user-id" "user-123")

```

---

#### `set-update-mode`

```clojure
(set-update-mode agent-graph mode)
```

Sets the update mode for an agent graph to control how in-flight agent executions are handled after the module is updated.When a module is updated, in-flight agent executions can be handled in three ways::continue - Executions continue where they left off with the new agent definition:restart - Executions restart from the beginning with the new agent definition:drop - In-flight executions are dropped and not processedArgs:agent-graph - agent graph builder instancemode - Update mode keyword: :continue, :restart, or :dropExample:```

(set-update-mode agent-graph :continue)

```

---

#### `setup-object-name`

```clojure
(setup-object-name setup)
```

Gets the name of an agent object from its setup context.Used within agent object builder functions to identify which object is being built.Args:setup - setup instance from builder functionReturns:String - The name of the object being built

---

#### `snapshot-dataset!`

```clojure
(snapshot-dataset! manager dataset-id from-snapshot to-snapshot)
```

Creates a snapshot of a dataset at its current state.Args:manager - agent manager instancedataset-id - UUID of the datasetfrom-snapshot - String name of the source snapshot (or nil for current)to-snapshot - String name for the new snapshot

---

#### `start-ui`

```clojure
(start-ui ipc)(start-ui ipc options)
```

Starts the Agent-o-rama web UI for monitoring and debugging.The UI provides real-time visualization of agent execution, traces, datasets, experiments, and telemetry. Accessible via web browser.Args:ipc - In-Process Cluster instanceoptions - Optional map with configuration::port - Port number for the UI (default 1974):host - Host address to bind to (default “localhost”)Returns:UI instance that should be closed when doneExample:```

(with-open [ui (aor/start-ui ipc {:port 8080})]
  (run-agents))

```

---

#### `stop-ui`

```clojure
(stop-ui)
```

Stops the Agent-o-rama web UI started withstart-ui

---

#### `stream-chunk!`

```clojure
(stream-chunk! agent-node chunk)
```

Manually streams a chunk of data from the current node for real-time consumption from agent clients viaagent-streamoragent-stream-all.Streaming chunks are separate from the agent’s final result and allow for real-time progress updates and incremental data delivery to clients. Chunks are delivered to streaming subscriptions in order.Args:agent-node - agent node instance from the current node functionchunk - The data chunk to stream (any serializable value)Example:```

(aor/stream-chunk! agent-node {:progress (count processed) :item item})

```

---

#### `try-comparative-evaluator`

```clojure
(try-comparative-evaluator manager name input reference-output outputs)
```

Tests a comparative evaluator on multiple outputs.Args:manager - agent manager instancename - String name of the evaluatorinput - Input data for the evaluationreference-output - Reference output for comparisonoutputs - Collection of actual outputs to compareReturns:Map - Comparative evaluation result, a map of score name to score value

---

#### `try-evaluator`

```clojure
(try-evaluator manager name input reference-output output)
```

Tests an evaluator on a single sample input / reference output / output.Args:manager - agent manager instancename - String name of the evaluatorinput - Input data for the evaluationreference-output - Reference output for comparisonoutput - Actual output to evaluateReturns:Map - Result scores from score name to score value

---

#### `try-summary-evaluator`

```clojure
(try-summary-evaluator manager name example-runs)
```

Tests a summary evaluator on a collection of example runs.Args:manager - agent manager instancename - String name of the evaluatorexample-runs - Collection of example runs created withmk-example-runReturns:Map - Summary evaluation result with aggregate metrics, a map from score name to score value

---

#### `underlying-stream-topology`

```clojure
(underlying-stream-topology at)
```

Gets the underlying stream topology from an agent topology.This provides access to the low-level Rama topology for advanced use cases that require direct interaction with Rama’s stream processing capabilities.Args:at - agent topology instanceReturns:the underlying Rama stream topology

---

## Namespace `com.rpl.agent-o-rama.langchain4j`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.langchain4j.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.langchain4j.html)

Convenience functions for working with LangChain4j model APIs in agent nodes.This namespace provides a small subset of common functionality wrapped in Clojure-friendly functions. It focuses on the most frequently usedoperations for chat models, tool calling, and JSON response formatting.For advanced LangChain4j features not covered here, you can directly use the LangChain4j Java API within your agent node functions.

### Public Variables and Functions

- [`basic-chat`](#basic-chat)
- [`chat`](#chat)
- [`chat-request`](#chat-request)
- [`json-response-format`](#json-response-format)

#### `basic-chat`

```clojure
(basic-chat model prompt)
```

Performs a simple chat interaction with a model using a string prompt.This is the simplest way to interact with a chat model. The prompt is automatically converted to a UserMessage and sent to the model.Args:model - ChatModel instance (obtained fromget-agent-object)prompt - String prompt to send to the modelReturns:String - The model’s response textExample:```

(let [model (aor/get-agent-object agent-node "openai-model")]
  (lc4j/basic-chat model "What is the capital of France?"))
;; => "The capital of France is Paris.")

```

---

#### `chat`

```clojure
(chat model request)
```

Performs a chat interaction with a model using a structured request.This function provides more control over the chat interaction, supporting both simple message sequences and full ChatRequest objects with advanced configuration options.Args:model - ChatModel instance (obtained fromget-agent-object)request - Either:Sequential collection of messages (strings or message objects)ChatRequest object with full configurationReturns:ChatResponse - Full response object with metadata and tool callsExample:```

(let [model (aor/get-agent-object agent-node "openai-model")]
  ;; With message sequence
  (lc4j/chat model [(UserMessage. "Hello") (UserMessage. "How are you?")])
  ;; With ChatRequest
  (lc4j/chat model (lc4j/chat-request
                    [(UserMessage. "Calculate 2+2")]
                    {:tools [calculator-tool]
                     :temperature 0.1})))

```

---

#### `chat-request`

```clojure
(chat-request messages)(chat-request messages {:keys [frequency-penalty max-output-tokens model-name presence-penalty response-format stop-sequences temperature tool-choice tools top-k top-p]})
```

Creates a ChatRequest object for advanced model interactions.This function builds a structured request with full control over model parameters, tool usage, and response formatting. String messages areautomatically converted to UserMessage objects.Args:messages - Collection of messages (strings or message objects)options - Optional map with configuration::frequency-penalty - Number, penalty for frequent tokens:max-output-tokens - Integer, maximum tokens to generate:model-name - String, specific model to use:presence-penalty - Number, penalty for presence of tokens:response-format - ResponseFormat object for structured outputs, such as withjson-response-format:stop-sequences - Collection of strings that stop generation:temperature - Number, randomness in generation (0.0-2.0):tool-choice - Keyword, tool usage strategy (:auto or :required):tools - Collection of tool info created withtool-info:top-k - Integer, top-k sampling parameter:top-p - Number, nucleus sampling parameter (0.0-1.0)Returns:ChatRequest - Request object for use withchatExample:```

(lc4j/chat-request
  [(UserMessage. "Calculate the area of a circle with radius 5")]
  {:tools [calculator-tool geometry-tool]
   :temperature 0.1
   :tool-choice :required
   :response-format (lc4j/json-response-format "calculation" math-schema)
   :max-output-tokens 500})

```

---

#### `json-response-format`

```clojure
(json-response-format name schema)
```

Creates a JSON response format configuration for structured model outputs.This function configures the model to return responses in a specific JSON format, useful for structured data extraction and API integrations.Args:name - String name for the JSON schemaschema - JSON schema object defining the expected response structure. The`com.rpl.agent-o-rama.langchain4j.json`namespace provides helpers for creating JSON schema objects.Returns:ResponseFormat - Configuration object for use inchat-requestExample:```

(let [math-schema (lj/object
                    {"result" (lj/number "The calculated result")
                     "steps" (lj/array (lj/string) "Calculation steps")})
      response-format (lc4j/json-response-format "math-calc" math-schema)]
  (lc4j/chat model
    (lc4j/chat-request
      [(UserMessage. "Calculate 15 * 23")]
      {:response-format response-format})))

```

---

## Namespace `com.rpl.agent-o-rama.langchain4j.json`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.langchain4j.json.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.langchain4j.json.html)

JSON schema builders for LangChain4j structured outputs and tool specifications.This namespace provides Clojure-friendly functions for building JSON schemas used throughout LangChain4j for structured outputs, tool parameter definitions, and response formatting. These schemas ensure models return data in predictable formats and enable type-safe tool calling.

### Public Variables and Functions

- [`any-of`](#any-of)
- [`array`](#array)
- [`boolean`](#boolean)
- [`enum`](#enum)
- [`from-json-string`](#from-json-string)
- [`int`](#int)
- [`null`](#null)
- [`number`](#number)
- [`object`](#object)
- [`reference`](#reference)
- [`string`](#string)

#### `any-of`

```clojure
(any-of elems)(any-of description elems)
```

Creates a JSON schema that accepts any of the provided schema types.This is useful for union types where a field can be one of several different types or values.Args:elems - Collection of JsonSchema objects that are valid alternativesdescription - Optional string description of the schemaReturns:JsonAnyOfSchema - Schema that accepts any of the provided typesExample:```

;; Field can be either string or number
(lj/any-of "ID can be string or number"
           [(lj/string "String identifier")
            (lj/number "Numeric identifier")])
;; Field can be any of several enum values
(lj/any-of [(lj/enum "Status" ["active" "inactive"])
            (lj/null)])

```

---

#### `array`

```clojure
(array item-schema)(array description item-schema)
```

Creates a JSON schema for arrays with a specific item type.Args:item-schema - JsonSchema object defining the type of array elementsdescription - Optional string description of the arrayReturns:JsonArraySchema - Schema for arrays with the specified item typeExample:```

;; Array of strings
(lj/array "List of tags" (lc4j/string "A tag"))
;; Array of objects
(lj/array "List of users"
          (lj/object {"name" (lc4j/string)
                     "age" (lc4j/int)}))

```

---

#### `boolean`

```clojure
(boolean)(boolean description)
```

Creates a JSON schema for boolean values.Args:description - Optional string description of the boolean fieldReturns:JsonBooleanSchema - Schema for boolean valuesExample:```

(lj/boolean "Whether the feature is enabled")
(lj/boolean)  ; No description

```

---

#### `enum`

```clojure
(enum vals)(enum description vals)
```

Creates a JSON schema for enumerated values.Args:vals - Collection of valid string valuesdescription - Optional string description of the enumReturns:JsonEnumSchema - Schema that accepts only the specified valuesExample:```

;; Status field with specific values
(lj/enum "User status" ["active" "inactive" "pending"])
;; Priority levels
(lj/enum ["low" "medium" "high" "critical"])

```

---

#### `from-json-string`

```clojure
(from-json-string json-str)
```

Parses a JSON schema string and constructs a JsonObjectSchema.Supports JSON Schema features: - Object types with properties, required fields, and additionalProperties - Primitive types: string, integer, number, boolean - Array types with item schemas - Enum types - References using $ref - anyOf combinator for union types - Descriptions for all schema types - Nested objects and arraysUnsupported features (will throw exceptions): - Constraints: minimum, maximum, minLength, maxLength, pattern, format - Logical combinators: allOf, oneOf, not - Other: const, defaultArgs: json-str - JSON string containing the schema definitionReturns: JsonSchema object (typically JsonObjectSchema)Throws: ExceptionInfo if JSON is invalid or contains unsupported featuresExample:```

(from-json-string
  "{\"type\": \"object\",
    \"properties\": {
      \"name\": {\"type\": \"string\", \"description\": \"User name\"},
      \"age\": {\"type\": \"integer\"}
    },
    \"required\": [\"name\"]}")

```

---

#### `int`

```clojure
(int)(int description)
```

Creates a JSON schema for integer values.Args:description - Optional string description of the integer fieldReturns:JsonIntegerSchema - Schema for integer valuesExample:```

(lj/int "Number of retries")
(lj/int)  ; No description

```

---

#### `null`

```clojure
(null)
```

Creates a JSON schema for null values.Returns:JsonNullSchema - Schema that only accepts nullExample:```

;; Optional field that can be null
(lj/any-of "Optional field"
           [(lj/string "String value")
            (lj/null)])

```

---

#### `number`

```clojure
(number)(number description)
```

Creates a JSON schema for numeric values (integers and floats).Args:description - Optional string description of the number fieldReturns:JsonNumberSchema - Schema for numeric valuesExample:```

(lj/number "Price in dollars")
(lj/number)  ; No description

```

---

#### `object`

```clojure
(object name->schema)(object options name->schema)
```

Creates a JSON schema for objects with defined properties.This is the most commonly used schema type for structured data.Args:name->schema - Map from property names (strings) to their JsonSchema definitionsoptions - Optional configuration map or string description::description - String description of the object:required - Collection of required property names:definitions - Map of reusable schema definitions:additional-properties? - Boolean, whether additional properties are allowedReturns:JsonObjectSchema - Schema for objects with the specified propertiesExample:```

;; Simple object
(lj/object {"name" (lj/string "User name")
            "age" (lj/int "User age")})
;; Complex object with options
(lj/object
  {:description "User profile with required fields"
   :required ["id" "name"]
   :additional-properties? false}
  {"id" (lj/string "Unique identifier")
   "name" (lj/string "Full name")
   "email" (lj/string "Email address")
   "preferences" (lj/object {"theme" (lj/enum ["light" "dark"])
                             "notifications" (lj/boolean)})})
;; With string description
(lj/object "Simple user object"
           {"id" (lj/string)
            "name" (lj/string)})

```

---

#### `reference`

```clojure
(reference ref)
```

Creates a JSON schema reference to a definition.References are used to avoid duplicating schema definitions and enable recursive schemas.Args:ref - String reference path (e.g., “#/$defs/User”)Returns:JsonReferenceSchema - Schema that references another definitionExample:```

;; Reference to a definition
(lj/reference "#/$defs/User")
;; Self-reference for recursive structures
(lj/object {"value" (lj/string)
            "children" (lj/array (lj/reference "#")})

```

---

#### `string`

```clojure
(string)(string description)
```

Creates a JSON schema for string values.Args:description - Optional string description of the string fieldReturns:JsonStringSchema - Schema for string valuesExample:```

(lj/string "User's full name")
(lj/string)  ; No description

```

---

## Namespace `com.rpl.agent-o-rama.store`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.store.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.store.html)

Store API for accessing persistent storage within agent nodes.This namespace provides a unified interface for working with different types of persistent storage in agent-o-rama. Stores are obtained viaget-storewithin agent node functions and provide distributed, persistent, replicated storage that agents can use to maintain state across executions.Store types:Key-value stores: Simple typed storage with automatic partitioningDocument stores: Schema-flexible storage for nested dataPState stores: Direct access to Rama’s built-in storage capabilitiesAll store operations are automatically traced and included in agent execution traces for debugging and monitoring purposes.Example:(fn [agent-node input]
  (let [store (aor/get-store agent-node "$$user-cache")]
    (store/put! store :user-id "12345")
    (store/put! store :last-seen (System/currentTimeMillis))
    (let [user-id (store/get store :user-id)
          last-seen (store/get store :last-seen)]
      (aor/result! agent-node {:user-id user-id :last-seen last-seen}))))

### Public Variables and Functions

- [`contains-document-field?`](#contains-document-field)
- [`contains?`](#contains)
- [`get`](#get)
- [`get-document-field`](#get-document-field)
- [`pstate-select`](#pstate-select)
- [`pstate-select-one`](#pstate-select-one)
- [`pstate-transform!`](#pstate-transform)
- [`put!`](#put)
- [`put-document-field!`](#put-document-field)
- [`update!`](#update)
- [`update-document-field!`](#update-document-field)

#### `contains-document-field?`

```clojure
(contains-document-field? store k doc-key)
```

Checks if a specific field exists in a document.Args:store - Document store instance obtained fromget-storek - Document key (primary key)doc-key - Field name to check for existenceReturns:Boolean - True if the field exists in the documentExample:```

(let [doc-store (aor/get-store agent-node "$$user-docs")]
  (when (store/contains-document-field? doc-store :user-123 :email)
    (store/get-document-field doc-store :user-123 :email)))

```

---

#### `contains?`

```clojure
(contains? store k)
```

Checks if a key exists in a key-value store.Args:store - Store instance obtained fromget-storek - Key to check for existenceReturns:Boolean - True if the key exists in the storeExample:```

(let [store (aor/get-store agent-node "$$cache")]
  (when (store/contains? store :user-id)
    (store/get store :user-id)))

```

---

#### `get`

```clojure
(get store k)(get store k default-value)
```

Gets a value from a key-value store.Retrieves the value associated with the given key from the store. If the key doesn’t exist, returns the default value (or nil if not provided).Args:store - Store instance obtained fromget-storek - Key to look updefault-value - Value to return if key doesn’t exist (optional, defaults to nil)Returns:The value associated with the key, or default-value if key doesn’t existExample:```

(let [store (aor/get-store agent-node "$$cache")]
  (store/get store :user-id "unknown")
  (store/get store :count 0))

```

---

#### `get-document-field`

```clojure
(get-document-field store k doc-key)(get-document-field store k doc-key default-value)
```

Gets a specific field from a document in a document store.Document stores allow accessing individual fields of nested data structures without loading the entire document.Args:store - Document store instance obtained fromget-storek - Document key (primary key)doc-key - Field name within the documentdefault-value - Value to return if field doesn’t exist (optional, defaults to nil)Returns:The value of the specified field, or default-value if field doesn’t existExample:```

(let [doc-store (aor/get-store agent-node "$$user-docs")]
  (store/get-document-field doc-store :user-123 :email "unknown@example.com")
  (store/get-document-field doc-store :user-123 :preferences {}))

```

---

#### `pstate-select`

```clojure
(pstate-select apath store)(pstate-select apath store partitioning-key)
```

Selects data from a PState store using Rama path expressions.PState stores provide direct access to Rama’s built-in storage capabilities with powerful querying using path expressions. This function returns a collection of all matching values.Args:apath - Rama path expressionstore - PState store instance obtained fromget-storepartitioning-key - Optional partitioning key for the query. Mandatory if the path does not begin with key navigation.Returns:Collection of all values matching the path expressionExample:```

(let [pstate (aor/get-store agent-node "$$analytics")]
  (store/pstate-select [:user-id ALL] pstate)
  (store/pstate-select [ALL (selected? :active)] pstate :some-partition-key))

```

---

#### `pstate-select-one`

```clojure
(pstate-select-one apath store)(pstate-select-one apath store partitioning-key)
```

Selects a single value from a PState store using Rama path expressions.Similar topstate-selectbut returns only the first matching value. Useful when you know the path will match exactly one item.Args:apath - Rama path expressionstore - PState store instance obtained fromget-storepartitioning-key - Optional partitioning key for the query. Mandatory if the path does not begin with key navigation.Returns:The first value matching the path expression, or nil if no matchExample:```

(let [pstate (aor/get-store agent-node "$$config")]
  (store/pstate-select-one [:settings :max-retries] pstate))

```

---

#### `pstate-transform!`

```clojure
(pstate-transform! apath store partitioning-key)
```

Transforms data in a PState store using Rama path expressions.Applies a transformation function to data matching the path expression.Args:apath - Rama path expression with transformationstore - PState store instance obtained fromget-storepartitioning-key - Partitioning key for the transformationExample:```

(let [pstate (aor/get-store agent-node "$$analytics")]
  (store/pstate-transform! [:alice :page-views (termval 42)] pstate :alice))

```

---

#### `put!`

```clojure
(put! store k v)
```

Stores a key-value pair in a key-value store.Args:store - Store instance obtained fromget-storek - Key to storev - Value to storeExample:```

(let [store (aor/get-store agent-node "$$cache")]
  (store/put! store :user-id "12345")
  (store/put! store :last-seen (System/currentTimeMillis)))

```

---

#### `put-document-field!`

```clojure
(put-document-field! store k doc-key value)
```

Sets a specific field in a document.Args:store - Document store instance obtained fromget-storek - Document key (primary key)doc-key - Field name to setvalue - Value to store in the fieldExample:```

(let [doc-store (aor/get-store agent-node "$$user-docs")]
  (store/put-document-field! doc-store :user-123 :email "user@example.com")
  (store/put-document-field! doc-store :user-123 :last-login (System/currentTimeMillis)))

```

---

#### `update!`

```clojure
(update! store k afn)
```

Updates a value in a key-value store using a function.Applies the function to the current value (or nil if key doesn’t exist) and stores the result back to the same key.Args:store - Store instance obtained fromget-storek - Key to updateafn - Function that takes the current value and returns the new valueExample:```

(let [store (aor/get-store agent-node "$$counters")]
  (store/update! store :page-views #(inc (or % 0)))
  (store/update! store :total #(+ (or % 0) amount)))

```

---

#### `update-document-field!`

```clojure
(update-document-field! store k doc-key afn)
```

Updates a specific field in a document using a function.Applies the function to the current field value (or nil if field doesn’t exist) and stores the result back to the same field.Args:store - Document store instance obtained fromget-storek - Document key (primary key)doc-key - Field name to updateafn - Function that takes the current field value and returns the new valueExample:```

(let [doc-store (aor/get-store agent-node "$$user-docs")]
  (store/update-document-field! doc-store :user-123 :login-count #(inc (or % 0)))
  (store/update-document-field! doc-store :user-123 :preferences #(assoc % :theme "dark")))

```

---

## Namespace `com.rpl.agent-o-rama.throttled-logging`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.throttled-logging.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.throttled-logging.html)

Throttled logging utilities.This namespace provides rate-limited logging macros that prevent log overflow during high-frequency operations. Throttled logging automatically limits the number of log messages emitted per callsite within a time window.Key features:Rate limiting per callsite to prevent log spamAutomatic throttling based on configurable thresholdsPreserves critical messages while filtering repetitive onesTransparent integration with standard logging levelsExample:(require '[com.rpl.agent-o-rama.throttled-logging :as tl])
;; In an agent node function
(fn [agent-node input]
  (dotimes [i 1000]
    ;; This will be throttled after the rate limit is reached
    (tl/info ::processing-loop (str "Processing item " i)))
  (aor/result! agent-node :completed))

### Public Variables and Functions

- [`debug`](#debug)
- [`error`](#error)
- [`fatal`](#fatal)
- [`info`](#info)
- [`logp`](#logp)
- [`warn`](#warn)

#### `debug`

```clojure
(debug callsite-id & args)
```

Logs a debug message using throttled logging.Debug messages are typically used for detailed diagnostic information that is only of interest when debugging problems. These messages areusually filtered out in production environments.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)message - Debug message stringExample:```

(tl/debug ::data-validation "Validating input parameters")
(tl/debug ::cache-lookup (str "Cache hit for key: " cache-key))

```

---

#### `error`

```clojure
(error callsite-id & args)
```

Logs an error message using throttled logging.Error messages indicate serious problems that prevent the application from performing a function but allow it to continue running. These are typically exceptions or unexpected conditions that require attention.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)throwable - Optional Throwable instancemessage - Error message stringExample:```

(tl/error ::tool-exec-error ex "Tool execution failed")
(tl/error ::data-validation "Invalid input data received")

```

---

#### `fatal`

```clojure
(fatal callsite-id & args)
```

Logs a fatal message using throttled logging.Fatal messages indicate very severe errors that will presumably lead to the application aborting. These are the highest priority log messagesand should be used sparingly for critical system failures.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)throwable - Optional Throwable instancemessage - Fatal error message stringExample:```

(tl/fatal ::system-failure ex "Critical system component failed")
(tl/fatal ::resource-exhaustion "Out of memory, shutting down")

```

---

#### `info`

```clojure
(info callsite-id & args)
```

Logs an informational message using throttled logging.Info messages provide general information about the application’s execution flow. They are typically used to track important events and state changes.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)message - Informational message stringExample:```

(tl/info ::agent-start "Agent execution started")
(tl/info ::data-processing (str "Processed " count " items successfully"))

```

---

#### `logp`

```clojure
(logp callsite-id & args)
```

Logs a message with the specified level using throttled logging.This is the base throttled logging macro that all other level-specific macros delegate to. It provides fine-grained control over log levelswhile maintaining rate limiting per callsite.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)level - Log level keyword (:debug, :info, :warn, :error, :fatal)throwable - Optional Throwable instancemessage - Log message stringExample:```

(tl/logp ::data-processing :info "Processing batch of 1000 items")
(tl/logp ::error-handling :error ex "Failed to process request")

```

---

#### `warn`

```clojure
(warn callsite-id & args)
```

Logs a warning message using throttled logging.Warning messages indicate potentially harmful situations or unusual conditions that don’t prevent the application from continuing butmay indicate problems that should be investigated.Args:callsite-id - Keyword identifying the callsite for rate limiting (recommended: namespaced keyword unique to that callsite)throwable - Optional Throwable instancemessage - Warning message stringExample:```

(tl/warn ::rate-limit ex "API rate limit hit")
(tl/warn ::deprecated-usage "Using deprecated function, consider upgrading")

```

---

## Namespace `com.rpl.agent-o-rama.tools`

**Clojuredoc URL:** [https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.tools.html](https://redplanetlabs.com/aor/clojuredoc/com.rpl.agent-o-rama.tools.html)

Tools integration for AI agents using LangChain4j tool specifications.This namespace provides utilities for creating tool specifications and tool agents that can be used with AI models for function calling. Tools allow AI agents to interact with external systems, perform calculations, and execute custom logic during conversation.Key concepts:Tool specifications define the interface for tools (name, parameters, description)Tool info combines specifications with implementation functionsTool agents execute tool calls and return results to AI modelsError handlers control how tool execution failures are handledExample:(def calculator-tool
  (tool-info
    (tool-specification
      "add"
      (lj/object {"a" (lj/number "first number")
                 "b" (lj/number "second number")})
      "Add two numbers together")
    (fn [args] (+ (get args "a") (get args "b")))))
(new-tools-agent topology "calculator" [calculator-tool])

### Public Variables and Functions

- [`error-handler-by-type`](#error-handler-by-type)
- [`error-handler-default`](#error-handler-default)
- [`error-handler-rethrow`](#error-handler-rethrow)
- [`error-handler-static-string`](#error-handler-static-string)
- [`error-handler-static-string-by-type`](#error-handler-static-string-by-type)
- [`new-tools-agent`](#new-tools-agent)
- [`tool-info`](#tool-info)
- [`tool-specification`](#tool-specification)

#### `error-handler-by-type`

```clojure
(error-handler-by-type tuples)
```

Creates an error handler that handles different exception types differently.This handler matches exceptions by type and applies the corresponding handler function. If no type matches, the exception is re-thrown.Args:tuples - Vector ofexception-type handler-functionpairsReturns:Function - Error handler that dispatches by exception typeExample:```

(new-tools-agent topology "calculator" tools
  {:error-handler (error-handler-by-type
                    [[ArithmeticException (fn [e] "Math error occurred")]
                     [IllegalArgumentException (fn [e] "Invalid input provided")]])})

```

---

#### `error-handler-default`

```clojure
(error-handler-default)
```

Creates the default error handler that formats exceptions as user-friendly messages.This handler converts exceptions to readable error messages with a standard format: “Error:\nPlease fix your mistakes.”Returns:Function - Error handler that formats exceptions as stringsExample:```

(new-tools-agent topology "calculator" tools
  {:error-handler (error-handler-default)})

```

---

#### `error-handler-rethrow`

```clojure
(error-handler-rethrow)
```

Creates an error handler that re-throws exceptions without modification.This is useful when you want tool execution errors to propagate up to the calling agent, allowing it to handle the error in its own logic.Returns:Function - Error handler that re-throws any exceptionExample:```

(new-tools-agent topology "calculator" tools
  {:error-handler (error-handler-rethrow)})

```

---

#### `error-handler-static-string`

```clojure
(error-handler-static-string s)
```

Creates an error handler that always returns a static string for any exception.This is useful for providing user-friendly error messages back to a model when tool execution fails, rather than exposing technical exception details.Args:s - String to return for any tool execution errorReturns:Function - Error handler that takes an exception and returns the stringExample:```

(new-tools-agent topology "calculator" tools
  {:error-handler (error-handler-static-string "Something went wrong. Please try again.")})

```

---

#### `error-handler-static-string-by-type`

```clojure
(error-handler-static-string-by-type tuples)
```

Creates an error handler that returns static strings for different exception types.This is a convenience function that combineserror-handler-by-typewitherror-handler-static-stringto provide simple string responses for different exception types.Args:tuples - Vector ofexception-type stringpairsReturns:Function - Error handler that returns strings based on exception typeExample:```

(new-tools-agent topology "calculator" tools
  {:error-handler (error-handler-static-string-by-type
                    [[ArithmeticException "Math error occurred"]
                     [IllegalArgumentException "Invalid input provided"]
                     [ClassCastException "Type conversion failed"]])})

```

---

#### `new-tools-agent`

```clojure
(new-tools-agent topology name tools)(new-tools-agent topology name tools options)
```

Creates a tools agent that can execute tool calls from AI models.A tools agent is a special type of agent designed to execute tool calls requested by AI models. It processes batches of tool execution requests, executes the corresponding tool functions, and returns results back to the calling agent.The agent uses aggregation to collect results from parallel tool executions and returns them as a vector of ToolExecutionResultMessage objects.Args:topology - agent topology instancename - String name for the tools agenttools - Collection of ToolInfo instances created withtool-infooptions - Optional map with configuration::error-handler - Function that handles tool execution errors (default:error-handler-default)Example:```

(let [calculator-tool
      (tool-info
        (tool-specification
          "add"
          (lj/object {"a" (lj/number "first number")
                     "b" (lj/number "second number")})
          "Add two numbers together")
        (fn [args] (+ (get args "a") (get args "b"))))]
  (new-tools-agent topology "calculator" [calculator-tool]))
;; With custom error handling
(new-tools-agent topology "robust-calculator" tools
  {:error-handler (error-handler-static-string "Calculation failed")})

```

---

#### `tool-info`

```clojure
(tool-info tool-specification tool-fn)(tool-info tool-specification tool-fn options)
```

Creates a tool info that combines a tool specification with its implementation function.Tool info is the complete definition of a tool, including both its interface (specification) and implementation (function). Tools can optionally include context from the agent node for advanced functionality.Args:tool-specification - ToolSpecification instance created withtool-specificationtool-fn - Function that implements the tool logic. Takes either:(args) - Just the parsed arguments map(agent-node caller-data args) - Agent node, caller data, and argumentsoptions - Optional map with configuration::include-context? - Boolean, whether to pass agent-node and caller-data to tool-fn (default false)Returns:ToolInfo - Complete tool definition for use withnew-tools-agentExample:```

(tool-info
  (tool-specification "add" params "Add two numbers")
  (fn [args] (+ (get args "a") (get args "b"))))
;; With context access
(tool-info (tool-specification “context-aware” params “Uses agent context”) (fn agent-node caller-data args (let store (aor/get-store agent-node “$$cache”) (aor/put! store “key” (get args “value”)))) {:include-context? true})

```

---

#### `tool-specification`

```clojure
(tool-specification name parameters-json-schema)(tool-specification name parameters-json-schema description)
```

Creates a tool specification that defines the interface for a tool.Tool specifications describe how AI models should call tools, including the tool name, parameter schema, and description. They are used with LangChain4j to enable function calling in AI conversations.Args:name - String name of the tool (must be unique within a tool agent)parameters-json-schema - JSON schema defining the tool’s parametersdescription - String description of what the tool does (optional)Returns:ToolSpecification - LangChain4j tool specification instanceExample:```

(tool-specification
  "calculate"
  (lj/object {"expression" (lj/string "mathematical expression to evaluate")})
  "Evaluates a mathematical expression")

```

---

