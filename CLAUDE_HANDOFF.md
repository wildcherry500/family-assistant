# Claude Handoff — Family Assistant Project

## Modules

| Module | Class | Package | Purpose |
|---|---|---|---|
| FamilySchemaModule | `FamilySchemaModule` | `com.family.assistant.schema` | Owns `$$family-data` PState, `*family-events` depot, and all indexes |
| EmailParsingModule | `EmailParsingModule` | `com.family.assistant.email` | Parses raw email → appends to `*family-events` depot |
| EmailIngestionModule | `EmailIngestionModule` | `com.family.assistant.email` | Batch fan-out to EmailParsingModule |
| GmailIngestionModule | `GmailIngestionModule` | `com.family.assistant.gmail` | Fetches unread Gmail, deduplicates, routes to EmailIngestionModule |
| DigestModule | `DigestModule` | `com.family.assistant.digest` | Reads `$$family-data` mirror store → returns digest string |
| QueryModule | `QueryModule` | `com.family.assistant.query` | LLM-powered natural language query over PState indexes |

## Current Build Status

Maven project root: `/Users/toddkeelingfolder/CORSAIR/family_assistant/`
- Do NOT compile from `/Volumes/CORSAIR/family-assistant/` (hyphen) — that is an old scratch folder with one stub file and no git repo.
- **78/78 tests passing** (non-LLM suite, no GEMINI_API_KEY required)

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

## PState Schema (FamilySchemaModule)

| PState | Key path | Value | Notes |
|---|---|---|---|
| `$$family-data` | `familyId -> "events" -> eventId` | `Map<String, Object>` record | Primary store |
| `$$events-by-child` | `familyId -> childName` | `Set<eventId>` | Null/blank childName not indexed |
| `$$events-by-category` | `familyId -> eventType` | `Set<eventId>` | Null/blank eventType not indexed |
| `$$events-by-account` | `familyId -> accountLabel` | `Set<eventId>` | Null/blank accountLabel not indexed |
| `$$events-by-date` | `familyId -> epochMs` (subindexed) | `Set<eventId>` | effectiveTime = startTime ?? deadline; null excluded |

### $$events-by-date — Rama 1.5.0 API notes (verified by testing)

**Range query (returns `List<String>` of individual eventIds):**
```java
List<String> ids = (List<String>)(List<?>) eventsByDate.select(
    Path.key(familyId).sortedMapRange(startMs, endMs).mapVals().all());
```

**Single-bucket lookup:**
```java
Set<String> bucket = (Set<String>) eventsByDate.selectOne(Path.key(familyId).key(epochMs));
```

**DO NOT use** `selectOne(Path.key(familyId))` on a subindexed PState — returns raw `RocksDBWrapper` (not serializable). Use a wide range query `(0L, Long.MAX_VALUE)` for full-index assertions.

**Write path in stream topology:**
```java
Path.key("*familyId", "*epochMs").nullToSet().voidSetElem().termVal("*eventId")
```

### Event record fields (stored in $$family-data)

| Field | Type | Source |
|---|---|---|
| `id` | String | gmailMessageId or generated UUID |
| `familyId` | String | partition key |
| `title` | String | email subject / LLM-extracted |
| `description` | String | email body |
| `eventType` | String | SCHOOL_EVENT, DEADLINE, PERMISSION_SLIP, TASK, UNKNOWN |
| `childName` | String | LLM-extracted or mapped from JSON |
| `childId` | String | childName.toLowerCase() |
| `startTime` | Long | epoch ms |
| `deadline` | Long | epoch ms |
| `urgency` | String | critical, high, medium, low |
| `status` | String | pending, completed |
| `sourceType` | String | email, test |
| `accountLabel` | String | Gmail account label |
| `created` / `updated` | Long | epoch ms |

---

## Test Suite (78 tests, all non-LLM)

| Test class | Tests | What it covers |
|---|---|---|
| `NonLlmPipelineTest` | 20 | Schema → DigestModule pipeline, time filtering, serialization |
| `CohenFamilyDatasetTest` | 19 | Real 21-day dataset (13 email records), all indexes |
| `IndexPStateTest` | 13 | Child/category index correctness |
| `AccountLabelTest` | 8 | `$$events-by-account` index + DigestModule account filtering |
| `QueryIndexTest` | 7 | `$$events-by-child` and `$$events-by-category` range assertions |
| `DateIndexTest` | 6 | `$$events-by-date` range queries, effectiveTime logic |
| `EmailIngestionTest` | 2 | Batch fan-out, blank/null filtering |
| `FamilyAssistantTest` | 2 | Schema module + depot smoke test |
| `GmailIngestionTest` | 1 | Live Gmail fetch (skips gracefully if no unread) |

### Test resources
- `src/test/resources/cohen_family_test_dataset_complete.json` — Cohen family 21-day dataset (18 messages: Feb 10–Mar 2, 2026)

---

## Checkpoint Behavior (2026-03-28)
- The "checkpoint at 10% context" instruction is **manual only** — no hook is configured
- When user says "checkpoint": update this file, then `git add -A && git commit -m "checkpoint" && git push origin master`
- No automated hook exists in `~/.claude/settings.json` or project `settings.local.json` for this

## Next Task

**SearchIndexModule** — full-text / keyword search index over event titles and descriptions, backed by a new PState in FamilySchemaModule. Prompt and design details to be provided at session start.

---

## Deferred Items

### ~~CRITICAL: PState data is NOT persistent (InProcessCluster in production)~~ RESOLVED
`FamilyAssistantApp` now uses `RamaClusterManager.open()` to connect to a local
Rama cluster (ZooKeeper + Conductor + Supervisor). PState data is persisted in
RocksDB under `local.dir` configured in `rama.yaml`.

**Resolved: Phase 2 migration complete (2026-04-11).** See below for details.

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

## Phase 2 Migration — Local Rama Cluster (persistent storage) ✓ COMPLETE

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
Live file: `rama.yaml` at project root. Key customizations vs defaults:
- `local.dir: /Volumes/CORSAIR/rama-data` (NVMe external drive)
- `cluster.ui.port: 8889` (avoids conflict with OAuth callback on 8888)
- `worker.child.opts: -Xmx2g`

### local.dir — what gets backed up
`/Volumes/CORSAIR/rama-data/` contains:
- Module JARs (Conductor subdirectory)
- RocksDB PState data (`$$family-data`, `$$events-by-child`, etc.)
- Depot replica logs (`*family-events`)

This is the directory to tar and ship to S3 nightly.

### Application code changes (DONE 2026-04-11)
1. ✅ `FamilyAssistantApp.java`: replaced `InProcessCluster.create()` with `RamaClusterManager.open()`
2. ✅ Removed all five `ipc.launchModule()` calls — modules are now deployed via `rama deploy` CLI
3. ✅ `AgentManager.create(ipc, ...)` → `AgentManager.create(cluster, ...)`
4. ✅ `ipc.clusterPState(...)` → `cluster.clusterPState(...)`
5. ✅ `pom.xml`: Rama scope stays as `compile` (NOT `provided`) — the fat JAR needs Rama on classpath for `RamaClusterManager.open()`
6. ✅ Tests remain on `InProcessCluster` — no test changes needed

### Remaining manual steps (one-time setup)
1. Download Rama 1.5.0 release from https://nexus.redplanetlabs.com
2. Unpack to `~/rama-release/`
3. Copy `rama.yaml` from project root to `~/rama-release/rama.yaml`
4. Start cluster daemons:
   ```bash
   ~/rama-release/rama devZookeeper &
   ~/rama-release/rama conductor &
   ~/rama-release/rama supervisor &
   ```
5. Build and deploy modules:
   ```bash
   mvn clean package -DskipTests
   ~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module FamilySchemaModule
   ~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module EmailParsingModule
   ~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module EmailIngestionModule
   ~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module GmailIngestionModule
   ~/rama-release/rama deploy --jar target/family-assistant-1.0.0-jar-with-dependencies.jar --module DigestModule
   ```
6. Start the app: `mvn compile exec:exec`

### S3 backup (unlocked by local.dir)
Once `local.dir` is configured, nightly backup is straightforward:
```bash
tar -czf /tmp/rama-backup-$(date +%Y%m%d).tar.gz ~/rama-data/
aws s3 cp /tmp/rama-backup-$(date +%Y%m%d).tar.gz s3://BUCKET/rama-backups/
```
S3 lifecycle policy: expire objects older than 7 days.
