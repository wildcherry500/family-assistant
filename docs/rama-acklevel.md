# Rama AckLevel — Depot Append Acknowledgment Control

Verified against official Rama 1.5.0 documentation.

## Overview

`AckLevel` controls how long `depot.append()` blocks before returning. Three values:

| Level | Waits for | Best for |
|---|---|---|
| `AckLevel.NONE` | Nothing — returns immediately | Fire-and-forget, highest throughput |
| `AckLevel.APPEND_ACK` | Data written + replicated to depot partition | Fast appends without PState consistency guarantee |
| `AckLevel.ACK` *(default)* | APPEND_ACK + all colocated stream topologies finish + PState replication | HTTP handlers that must read consistent PState after write |

## Method Signatures

```java
depot.append(Object data);                           // defaults to ACK
depot.append(Object data, AckLevel ackLevel);        // explicit

CompletableFuture<?> depot.appendAsync(Object data);
CompletableFuture<?> depot.appendAsync(Object data, AckLevel ackLevel);
```

## Key Behaviors

**Default is ACK.** Calling `append(data)` without an ack level waits for all colocated stream
topologies to finish processing before returning. This means by the time your Javalin HTTP handler
gets a response from `append()`, the PState is already consistent and safe to read — no sleep or
polling needed for same-module reads.

**ACK does NOT cover cross-module mirror depots.** If `EmailParsingModule` appends to
`*family-events` via `getMirrorDepot(...)`, the ACK from that append only waits for stream
topologies colocated with that depot (inside `FamilySchemaModule`). Stream processing in other
modules is not waited for. In integration tests that span module boundaries, you may need a short
poll loop on the target PState.

**Failure behavior.** By default, if a stream topology fails on first attempt but succeeds on retry,
`ACK` throws an exception to the client. Disable with dynamic option:
`depot.ack.failure.on.any.streaming.failure=false`

**Timeout.** Config key `replication.depot.append.timeout.millis` applies to both `APPEND_ACK` and
`ACK`; exceeding it throws an exception.

## REST API Equivalents

```json
{ "ackLevel": "ack" }        // AckLevel.ACK
{ "ackLevel": "appendAck" }  // AckLevel.APPEND_ACK
{ "ackLevel": "none" }       // AckLevel.NONE
```

Default in REST API: `"ack"`.

## Practical Guidance

- **Javalin HTTP handlers** that append then read: default `append(data)` is fine — ACK ensures consistency.
- **Email/notification fire-and-forget flows**: use `append(data, AckLevel.NONE)` for lower latency.
- **InProcessCluster tests**: `ACK` (default) makes stream topology processing synchronous, enabling
  immediate PState assertions after `append()`.
