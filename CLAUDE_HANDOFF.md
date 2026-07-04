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
- **111/111 tests passing** (non-LLM suite, no GEMINI_API_KEY required)
- LLM-tagged suite: `EmailIngestionTest`/`FamilyAssistantTest` (5 tests) green as of the
  2026-07-03 `GmailMessage`/`String` call-site fix. `QueryAgentTest` (4 tests) is flaky —
  a 3-consecutive-run audit the same day showed 3/4, 2/4, 3/4, never 4/4 — root cause is
  `EmailParsingModule` collapsing multiple real-world items per email plus non-deterministic
  date extraction, being addressed next. `ZooEmailTest.testZooEmailExtraction` fails 100%
  of the time for an unrelated, diagnosed-not-fixed test-infrastructure reason. See the
  "LLM-tagged tests" table below for the full, corrected picture — the version of this
  line that claimed "all 4 passing" was based on a single run and was wrong.

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
| `$$events-by-silo` | `familyId -> silo` | `Set<eventId>` | VAULT/OFFICE/STUDIO/UNKNOWN; UNKNOWN is indexed (correction-loop) |
| `$$events-by-intent` | `familyId -> intent` | `Set<eventId>` | ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN; UNKNOWN is indexed |
| `$$events-by-keyword` | `familyId -> keyword` | `Set<eventId>` | Tokenized `title`+`description`+`emailSubject` (lowercase, `[^a-z0-9]+` split, 3-char min, ~40-word stopword list). Populated via `Ops.EXPLODE` fan-out — see `RAMA_VERIFIED_LEARNINGS.md`. Read by `QueryModule`'s `search-agent` as the primary/HARD search dimension. |
| `$$leverage-map` | `familyId -> entryId` | `{silo, intent, weight}` | Config, not an index. silo/intent null = wildcard. Populated via `*weakness-leverage-config` depot. Read by DigestModule to reorder events (matches float to top, chronological tiebreak). |
| `$$weakness-map` | `familyId -> entryId` | `{silo, intent, tag, note}` | Same depot/config pattern as leverage-map. Read by DigestModule to annotate matched events with a `Note:` line. |

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

## Test Suite (111 tests, all non-LLM)

| Test class | Tests | What it covers |
|---|---|---|
| `NonLlmPipelineTest` | 20 | Schema → DigestModule pipeline, time filtering, serialization |
| `CohenFamilyDatasetTest` | 19 | Real 21-day dataset (13 email records), all indexes |
| `IndexPStateTest` | 13 | Child/category index correctness |
| `KeywordIndexTest` | 10 | `$$events-by-keyword` population — multi-field tokenization, case folding, stopwords, min-length, dedup, isolation |
| `WeaknessLeverageMapTest` | 9 | `$$leverage-map`/`$$weakness-map` population, digest reordering, weakness annotation, graceful no-op |
| `AccountLabelTest` | 8 | `$$events-by-account` index + DigestModule account filtering |
| `SiloIntentIndexTest` | 8 | `$$events-by-silo`/`$$events-by-intent` population and isolation |
| `QueryIndexTest` | 7 | `$$events-by-child` and `$$events-by-category` range assertions |
| `SearchAgentTest` | 6 | `search-agent`'s two-tier hard/soft intersection — zero-dimension full scan, wrong-SOFT+right-HARD rescue, wrong-HARD+right-SOFT control (stays empty), HARD∩HARD genuine filtering, SOFT narrowing in the non-fallback path, all-SOFT-no-HARD-anchor stays empty |
| `DateIndexTest` | 6 | `$$events-by-date` range queries, effectiveTime logic |
| `EmailIngestionTest` | 2 | Batch fan-out, blank/null filtering |
| `FamilyAssistantTest` | 2 | Schema module + depot smoke test |
| `GmailIngestionTest` | 1 | Live Gmail fetch (skips gracefully if no unread) |

### LLM-tagged tests (`@Tag("llm")`, excluded by default — need `GEMINI_API_KEY`)

**Correction (2026-07-03):** the row below for `QueryAgentTest`/`ZooEmailTest`
previously said "GREEN, all passing" based on a single run. A 3-consecutive-run audit
the same day showed `QueryAgentTest` never actually goes 4/4 (3/4, 2/4, 3/4 across the
three runs) and `ZooEmailTest.testZooEmailExtraction` fails 100% of the time for a
reason unrelated to search or extraction (see below). This table now also lists
`EmailIngestionTest`/`FamilyAssistantTest`'s `@Tag("llm")` tests, which the original
table omitted entirely.

| Test class | Tests | What it covers | Status |
|---|---|---|---|
| `QueryAgentTest` | 4 | Rubric-style `assertResponseContains` assertions on natural-language answers (zoo/field-trip fact, permission-slip date fact, March-20 fact) | **Flaky, not green.** 3-run audit: 3/4, 2/4, 3/4. The permission-slip question failed in all 3 runs; the March-20 question failed in 1 of 3. Root cause: one email collapses two distinct real-world items (field trip + permission-slip deadline) into one `SCHOOL_EVENT` record, and `EmailParsingModule`'s date extraction is non-deterministic on which calendar day an event lands on. Not a search-agent defect — being addressed by the multi-event/date-determinism parser work (see "Next Task"). |
| `ZooEmailTest` | 2 | Prints Gemini's extraction + digest output for the real zoo email fixture | `testZooEmailExtraction` fails 100% of the time (3/3 audit runs + 1 verification run) with `ExceptionInfo: Executor pool is shut down`, originating deep in Rama's `SingleThreadExecutorPool` / `AgentDeclaredObjectsTaskGlobal.getMirrorAgentClient` machinery — reproduced at the identical position in test-class execution order every time. Confirmed NOT a JVM-wide static-cache bug (read `AgentDeclaredObjectsTaskGlobal`'s actual source — its mirror-agent-client cache is per-task, not a naive global singleton), but something about `InProcessCluster` lifecycle across test classes in one Surefire JVM fork still trips it. Diagnosed, not fixed — root cause not fully pinned down, and unrelated to `EmailParsingModule`'s classify/extract logic. `testDigestAfterZooEmail` reports as passing but may be doing so vacuously if test 1's ingestion never completed. |
| `EmailIngestionTest` | 3 | Mixed blank+valid batch, duplicate-email idempotency, malformed-batch resilience | **Green** as of the 2026-07-03 `GmailMessage`/`String` call-site fix (see above) — previously 100%-failing with `ClassCastException`, now 3/3 clean in the post-fix verification run. |
| `FamilyAssistantTest` | 2 | `EmailParsingModule` writes to `$$family-data`; end-to-end email→digest | **Green** as of the same fix — previously 100%-failing with the same `ClassCastException`, now 2/2 clean. |

### Test resources
- `src/test/resources/cohen_family_test_dataset_complete.json` — Cohen family 21-day dataset (18 messages: Feb 10–Mar 2, 2026)

---

## Checkpoint Behavior (2026-03-28)
- The "checkpoint at 10% context" instruction is **manual only** — no hook is configured
- When user says "checkpoint": update this file, then `git add -A && git commit -m "checkpoint" && git push origin master`
- No automated hook exists in `~/.claude/settings.json` or project `settings.local.json` for this

## RESOLVED (2026-07-03, corrected 2026-07-03) — String vs GmailMessage ingestion mismatch

`EmailIngestionModule`'s `ingest` node took `List<GmailMessage>`, but several tests still
passed raw `String`/`List<String>` — fixed by adding
`GmailMessageTestFixtures.fromRawBody(String)` (shared test helper) and switching call
sites to it. Production code untouched.

**Correction**: the note as originally written only covered 2 of 5 actual call sites
(`QueryAgentTest`, `ZooEmailTest`). Three more were found still broken during a later
audit — `FamilyAssistantTest.java:86,171` and `EmailIngestionTest.java:117,141,169` — all
throwing the identical `ClassCastException: String cannot be cast to GmailMessage`,
100% reproducible, not a flake. **All 5 call sites are now fixed** using the same
`GmailMessageTestFixtures.fromRawBody(...)` pattern. Verified: non-LLM suite still
111/111; full `@Tag("llm")` suite shows zero `ClassCastException`s where there were 6
before.

## RESOLVED (2026-07-03) — Compound search: keyword index + two-tier search-agent

`QueryModule` used to pick a single `categoryFilter` guess and query one index bucket —
when the guess was wrong (e.g. `PERMISSION_SLIP` for an event stored as `SCHOOL_EVENT`),
it found nothing even though the event existed in `$$family-data`. Replaced with compound
search over every index dimension:

- **`$$events-by-keyword`** (new PState, `FamilySchemaModule`) — tokenized
  `title`+`description`+`emailSubject`, populated via `Ops.EXPLODE` fan-out (one write per
  token per event). See `RAMA_VERIFIED_LEARNINGS.md` for the jar-verified mechanism.
- **`search-agent`** (new agent, inside `QueryModule` — not a new module):
  `parse-filters → resolve-indexes → intersect → finalize`, entirely LLM-free. Callable
  cross-module via `getMirrorAgentClient("QueryModule", "search-agent")` for future reuse
  (not wired into `DigestModule` yet).
- **Two-tier dimension model** in `intersect`: HARD (`keywords`, `dateRange`,
  `accountLabel`) always applied, never dropped. SOFT (`childName`, `categoryFilter`,
  `siloFilter`, `intentFilter`) applied normally when the full intersection is non-empty;
  if it's empty AND at least one HARD dimension was present, SOFT dimensions are dropped
  and only the HARD ones are re-intersected — a deterministic fallback, not a retry. If no
  HARD dimension was present at all, an empty intersection stays empty (nothing to fall
  back to). See `REASONING.md`'s "Plan correction — two-tier hard/soft filter" section for
  the full design rationale and why naive "AND across everything" doesn't work.
- `interpret-query`'s prompt now extracts a compound filter (keywords always populated —
  empty array for pure date/time questions, never padded with generic words like
  "activity"/"event") instead of one categoryFilter guess, and includes the same
  year-anchoring rule as `EmailParsingModule` ("assume 2026 unless stated otherwise").
- Date-range parsing (`parseToEpochStartOfDay`/`parseToEpochEndOfDay`, in `search-agent`)
  computes a full calendar day *in the asker's timezone*, not a UTC zero-width instant —
  the previous single `parseToEpoch` used the same UTC midnight instant for both
  `dateFrom` and `dateTo`, making single-day range queries nearly always miss.

**Result at the time: all 4 `QueryAgentTest` rubric assertions passed on that session's
final run** (previously 2 of 4 failed red-by-design). **Correction (2026-07-03, later
audit):** that single green run was not representative — a 3-consecutive-run audit the
same day showed 3/4, 2/4, 3/4, never 4/4. The search-agent logic itself is not at fault
(confirmed by direct evidence: in one failing run, the March-20 answer correctly surfaced
the permission-slip deadline from the same record, proving the data and the date-range
matching both worked — only the dedicated permission-slip question, which resolves to a
SOFT-only filter with no HARD anchor, came up empty). See "Known Issue: date extraction
non-determinism" below for why.

## RESOLVED (2026-07-03) — ZooEmailTest diagnostic cast bug

`ZooEmailTest.testDigestAfterZooEmail` cast `startTime`/`deadline` to `String` and
re-parsed as ISO-8601, but they're stored as `Long` epoch millis. Fixed — reads the `Long`
values directly, no re-parsing.

## Known Issue (2026-07-03) — ZooEmailTest "Executor pool is shut down" (diagnosed, not fixed)

`ZooEmailTest.testZooEmailExtraction` fails 100% of the time (confirmed across 3 audit
runs plus 1 later verification run — not a flake, deterministic given test execution
order) with `clojure.lang.ExceptionInfo: Executor pool is shut down`, reached via
`agentNode.getMirrorAgentClient(...)` → AOR's `AgentDeclaredObjectsTaskGlobal` →
Rama's own `rpl.rama.distributed.util.executor_pool.SingleThreadExecutorPool` (confirmed
by decompiling both jars — the string literal only appears in the pinned `rama-1.5.0.jar`,
not agent-o-rama's). Ruled out the simplest hypothesis by reading
`AgentDeclaredObjectsTaskGlobal`'s actual bundled source: its mirror-agent-client cache
(`_mirrorAgents`) is a per-task `WorkerManagedResource`, not a naive JVM-wide static
singleton, so it isn't simply "any two test classes that reuse a module name collide."
The actual trigger is deeper in Rama's distributed-client/executor lifecycle and wasn't
fully pinned down — reproduces at the same position in test-class execution order
(right after `SearchAgentTest`, right before `DateIndexTest`) every run, suggesting
something tied to `InProcessCluster` teardown from an earlier test class in the same
Surefire JVM fork isn't cleanly isolated from a later class's fresh cluster. Not caused
by, and not fixable within, `EmailParsingModule`'s classify/extract logic — flagged for
separate scoping (candidate angles: `<reuseForks>false</reuseForks>` in the surefire
plugin config to give each test class its own JVM, or an upstream Rama/AOR question).

## Known Issue (2026-07-03) — date extraction non-determinism (real, demonstrated, not fixed)

`EmailParsingModule`'s date extraction is measurably non-deterministic on *which calendar
day* it lands an event on, not just the exact time-of-day. Across several ingestion runs
of the identical `ZOO_EMAIL` fixture (which explicitly says "Thursday, March 20th"), the
stored `startTime` landed on **March 19** in Pacific time on some runs and **March 20** on
others — confirmed directly via debug output and the generated answer text ("Thursday,
March 19 at 5:00 PM PDT" vs "Friday, March 20th at 2:00 AM PDT") across otherwise-identical
runs with no code changes in between. `search-agent`'s date-range logic is correct given
whatever epoch value it receives — this is upstream, in `EmailParsingModule`'s own
extraction (out of scope this session; touching the classify/extract prompt was
explicitly forbidden). This is a wider-blast-radius version of the already-known "one
email = one event" parser-granularity issue — it affects any question or feature whose
correctness depends on which side of a day boundary an event's `startTime` lands,
including future digest-window features, not just `QueryModule`.

## Next Task

**Multi-event extraction in `EmailParsingModule`** — one email currently always yields
exactly one event, even when it describes several distinct things (e.g. the zoo trip
fixture bundles a permission slip deadline, a field trip, a picture day, and a pickup-time
change into a single `SCHOOL_EVENT` record). This is the deferred half of the
permission-slip root cause the previous session flagged, and it's also where the
date-extraction non-determinism above should be investigated and fixed, since both live
in the same `extract-details`/classify path. Do not touch this without a design session —
splitting one email into N events changes the depot/PState write shape and needs its own
plan.

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
