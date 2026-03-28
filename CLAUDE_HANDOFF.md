# Claude Handoff — Family Assistant Project

## Modules

| Module | Class | Package | Purpose |
|---|---|---|---|
| FamilySchemaModule | `FamilySchemaModule` | `com.family.assistant.schema` | Owns `$$family-data` PState and `*family-events` depot |
| EmailParsingModule | `EmailParsingModule` | `com.family.assistant.email` | Parses raw email → appends to `*family-events` depot |
| DigestModule | `DigestModule` | `com.family.assistant.digest` | Reads `$$family-data` mirror store → returns digest string |

## Current Build Status

Maven project root: `/Users/toddkeelingfolder/CORSAIR/family_assistant/`
- Do NOT compile from `/Volumes/CORSAIR/family-assistant/` — no `pom.xml` there.

## GCP Project Situation (IMPORTANT)

There are TWO GCP projects in play:

| Project | Numeric ID | Role |
|---|---|---|
| `agent-o-rama-build` | (old) | Where OAuth credentials (`credentials.json`) were originally created |
| `family-assistant-dev` | `family-assistant-dev-490204` | New project; where Pub/Sub topic was created |

### What exists in `family-assistant-dev-490204`
- Pub/Sub topic: `gmail-push-notifications`
- Pub/Sub subscription: `gmail-push-sub`
- IAM: `gmail-api-push@system.gserviceaccount.com` granted `roles/pubsub.publisher` on topic

### OAuth credentials
- `src/main/resources/credentials.json` → `project_id: family-assistant-dev-490204` ✓
- Tokens stored in `tokens/` for `toddkeeling@gmail.com`
- `GmailService.authorize()` uses `"toddkeeling@gmail.com"` as login hint

## Current State — FULLY CONFIGURED ✓

- Watch registered: `historyId=6262325`, expires **Mon Mar 23 17:21:13 PDT 2026**
- Pub/Sub topic: `projects/family-assistant-dev-490204/topics/gmail-push-notifications` ✓
- IAM: `gmail-api-push@system.gserviceaccount.com` has `roles/pubsub.publisher` ✓

## Watch Renewal (every ~7 days)

```bash
cd /Users/toddkeelingfolder/CORSAIR/family_assistant
mvn compile exec:java -Dexec.mainClass="com.family.assistant.gmail.GmailWatchSetup"
```

### If it fails with 404 "Resource not found"

Re-grant IAM (single line — no backslash continuations in zsh):
```bash
gcloud pubsub topics add-iam-policy-binding gmail-push-notifications --project=family-assistant-dev-490204 --member="serviceAccount:gmail-api-push@system.gserviceaccount.com" --role="roles/pubsub.publisher"
```

## Checkpoint Behavior (2026-03-28)
- The "checkpoint at 10% context" instruction is **manual only** — no hook is configured
- When user says "checkpoint": update this file, then `git add -A && git commit -m "checkpoint" && git push origin master`
- No automated hook exists in `~/.claude/settings.json` or project `settings.local.json` for this

## Deferred Items

### CRITICAL: PState data is NOT persistent (InProcessCluster in production)
`FamilyAssistantApp` uses `InProcessCluster.create()` which stores all PState data
in JVM-managed temp storage. **Every process restart wipes all family event data.**

`InProcessCluster` has no persistent-directory API — it is test-only by design.
Confirmed by inspecting the Rama 1.5.0 jar: `create()` and `create(List<Class>)` are
the only overloads, neither accepts a data directory.

**Resolution: migrate to local Rama cluster (Phase 2 — in progress)**
See "Phase 2 Migration Plan" section below for the full migration path.

---

### DEFERRED: Email replay capability
Current deduplication uses the `FamilyAssistant/Processed` Gmail label.
This correctly prevents duplicate processing in live operation but
means old emails cannot be reprocessed if parsing logic changes.
Future fix: implement replay by removing Processed labels and
re-draining the depot. Rama's append-only depot supports this
natively. Trigger: first time parsing logic changes and reprocessing
old emails would be valuable.

---

## Phase 2 Migration Plan — Local Rama Cluster (persistent storage)

### What the docs say (Rama 1.5.0, verified from rama-knowledge-base)

There is **no embedded persistent cluster API**. The only path to persistence is the
full cluster stack: ZooKeeper + Conductor + Supervisor. All three run on the same
Mac Mini for single-node production use.

Docs quote: *"Rama is easy to set up on a single node by running Zookeeper, Conductor,
and one Supervisor on the same node. This is a great way to start for low-scale
applications, and it's easy to scale up later by adding more nodes."*

### Components needed
| Component | Command | Notes |
|---|---|---|
| ZooKeeper | `./rama devZookeeper` | Dev-only (no admin routines). Acceptable for Mac Mini personal use. For hardened production, deploy standalone ZK. |
| Conductor | `./rama conductor` | Orchestrates module deployment, stores module JARs |
| Supervisor | `./rama supervisor` | Manages worker processes, stores RocksDB PState data |

Conductor and Supervisor share the same `rama.yaml` — each uses an independent
subdirectory within `local.dir`.

### rama.yaml (single-node, all on localhost)
```yaml
zookeeper.servers:
  - "localhost"
local.dir: "/Users/toddkeelingfolder/rama-data"
conductor.host: "localhost"
supervisor.port.range:
  - 3000
  - 4000
```

### local.dir — what gets backed up
`/Users/toddkeelingfolder/rama-data/` contains:
- Module JARs (Conductor subdirectory)
- RocksDB PState data (`$$family-data`, `$$events-by-child`, etc.)
- Depot replica logs (`*family-events`)

This is the directory to tar and ship to S3 nightly.

### Application code changes required
1. Replace `InProcessCluster.create()` with `RamaClusterManager.open(config)`
2. `RamaClusterManager` connects to the running cluster like a client
3. Modules are deployed via `rama deploy` CLI, not `ipc.launchModule()`
4. `AgentManager.create(ipc, ...)` → `AgentManager.create(clusterManager, ...)`
5. `pom.xml`: rama scope must be `provided` (not `compile`) for cluster deployment

### Steps to execute
1. Download Rama 1.5.0 release from https://nexus.redplanetlabs.com (same release as the jar)
2. Unpack to e.g. `~/rama-release/`
3. Create `rama.yaml` at project root with config above
4. Run: `~/rama-release/rama devZookeeper &`
5. Run: `~/rama-release/rama conductor &`
6. Run: `~/rama-release/rama supervisor &`
7. Deploy modules via: `~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module FamilySchemaModule ...`
8. Update `FamilyAssistantApp.java` to use `RamaClusterManager`
9. **Tests stay on InProcessCluster** — no test changes needed

### S3 backup (unlocked by local.dir)
Once `local.dir` is configured, nightly backup is straightforward:
```bash
tar -czf /tmp/rama-backup-$(date +%Y%m%d).tar.gz ~/rama-data/
aws s3 cp /tmp/rama-backup-$(date +%Y%m%d).tar.gz s3://BUCKET/rama-backups/
```
S3 lifecycle policy: expire objects older than 7 days.
