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
- **145/145 tests passing** (non-LLM suite, no GEMINI_API_KEY required) as of the
  2026-07-17 pre-deploy gate-review session (Part 1 of the first real cluster deploy:
  unknown-ID topology guard + debug-route gating — see "Recently Completed" below).
  Prior to that, 144/144 as of the 2026-07-16 open-items/mark-done session (+1: new
  `OpenItemsAndMarkDoneTest` — see "Recently Completed" below). Prior to that, 143/143 as of the same-day Layer 2
  Commitments write-path session (+6: new `CommitmentsTest` — see "Recently Completed"
  below). Prior to that, 137/137 as of the
  2026-07-15 graph-schema-evolution session (+11: new `EdgesEntityIndexTest` covering
  `$$edges-forward`/`$$edges-inverse`/`$$entities`/`$$entities-by-type` — see "Recently
  Completed" below). Prior to that, 126/126 as of the 2026-07-14 `SearchAgentTest`
  expansion (+3: personId containment on a non-first list element, tags containment on a
  non-first list element, and a 3-way compound tag+personId+date-range intersection — the
  search-agent itself needed no changes, see `REASONING.md`'s "Audit before 'Piece 2:
  search-agent' task"). Prior to that, 123/123 as of the 2026-07-05 schema refactor
  (Session 2: `eventType`→`tags`, `childName`/`childId`→`personId`, + plumbed classifier
  fields). +12 vs the prior 111 = the new `MultiValueIndexTest` (8) plus reworked
  index-test assertions.
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
| `$$events-by-person` | `familyId -> personId` | `Set<eventId>` | `personId` is a `List<String>`; fanned out one entry per element via `Ops.EXPLODE` (empty list → no entry). Renamed from `$$events-by-child` 2026-07-05. |
| `$$events-by-tag` | `familyId -> tag` | `Set<eventId>` | `tags` is a `List<String>`; fanned out one entry per element via `Ops.EXPLODE` (empty list → no entry). Renamed from `$$events-by-category` 2026-07-05. |
| `$$events-by-account` | `familyId -> accountLabel` | `Set<eventId>` | Null/blank accountLabel not indexed |
| `$$events-by-date` | `familyId -> epochMs` (subindexed) | `Set<eventId>` | effectiveTime = startTime ?? deadline; null excluded |
| `$$events-by-silo` | `familyId -> silo` | `Set<eventId>` | VAULT/OFFICE/STUDIO/UNKNOWN; UNKNOWN is indexed (correction-loop) |
| `$$events-by-intent` | `familyId -> intent` | `Set<eventId>` | ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN; UNKNOWN is indexed |
| `$$events-by-keyword` | `familyId -> keyword` | `Set<eventId>` | Tokenized `title`+`description`+`emailSubject` (lowercase, `[^a-z0-9]+` split, 3-char min, ~40-word stopword list). Populated via `Ops.EXPLODE` fan-out. Since 2026-07-05 it shares the topology with the `tags` and `personId` fan-outs; all three are isolated as independent branches via `.anchor("fanoutRoot")`/`.hook(...)` to avoid cartesian write amplification — see `RAMA_VERIFIED_LEARNINGS.md`. Read by `QueryModule`'s `search-agent` as the primary/HARD search dimension. |
| `$$edges-forward` | `familyId -> subjectId(eventId) -> relation` | `Set<objectId>` | Typed relation edges (MENTIONS_PERSON/PART_OF/LOCATED_AT/ACTION_NEEDED/UNKNOWN), forward direction. Added 2026-07-15. Populated from a `relations` List<Map> field on the event record via the same `Ops.EXPLODE`/`anchor`/`hook` branch pattern as tags/personId/keyword. |
| `$$edges-inverse` | `familyId -> objectId -> relation` | `Set<subjectId>(eventId)` | Same edges, inverse direction. Added 2026-07-15. |
| `$$entities` | `familyId -> entityId` | `{type, canonicalName, aliases}` | Entity foundation. Added 2026-07-15. `entityId = hash(eventId\|objectType\|object)` — deterministic (`UUID.nameUUIDFromBytes`, never `randomUUID`), one row per distinct mention within an event (two different relations targeting the same object+type in one event collapse to the same entityId), NOT deduped across events — that's a future entity-resolution effort. `aliases` starts as an empty `Set<String>`. |
| `$$entities-by-type` | `familyId -> entityType` | `Set<entityId>` | PERSON/ORG/PLACE/PROJECT/UNKNOWN. Added 2026-07-15. Makes the `UNKNOWN` bucket an inspectable indexed queue — the trigger for eventually promoting a `WORK` type (creative-work mentions currently fall to UNKNOWN) is real recurring volume showing up here, not a guess. See `REASONING.md`'s 2026-07-15 entry. |
| `$$leverage-map` | `familyId -> entryId` | `{silo, intent, weight}` | Config, not an index. silo/intent null = wildcard. Populated via `*weakness-leverage-config` depot. Read by DigestModule to reorder events (matches float to top, chronological tiebreak). |
| `$$weakness-map` | `familyId -> entryId` | `{silo, intent, tag, note}` | Same depot/config pattern as leverage-map. Read by DigestModule to annotate matched events with a `Note:` line. |
| `$$commitments` | `familyId -> commitmentId` | `{sourceEventId, objectId, createdAt, status, updatedAt}` | Layer 2 commitments. Added 2026-07-16. Seeded ONLY from `ACTION_NEEDED` edges (Fork 3) — `intent`/`$$events-by-intent` stays an untouched, independent search dimension. `commitmentId = hash(sourceEventId\|relation\|objectId)` — deterministic (`nameUUIDFromBytes`, never random), duplicates across events allowed by design (Fork 5, same shape as entity-ID non-dedup). `sourceEventId`/`objectId`/`createdAt` are content — always refreshed by the `family-events-stream` creation branch on every redrain, since they're deterministic from the source edge. `status` is write-once: initialized to `OPEN` only the first time a commitment is seen (guarded by a `localSelect`-then-`ifTrue` read that happens before any write in that event), and thereafter owned exclusively by the `*commitment-status-changes` depot's own branch (a second `.source(...)` on the SAME topology — see `RAMA_VERIFIED_LEARNINGS.md`, a PState can only be written by one topology). **The status-change branch itself also `localSelect`s-then-`ifTrue`-guards on existence (added 2026-07-17, gate-review revision C): a status change for a commitmentId the ACTION_NEEDED branch never seeded is dropped, not auto-vivified into a stub — closes an unbounded-key-space write surface flagged by `EDGE_CODE_RULES.md` Gate 6/Gate 8. This does NOT affect the separate "ghost" case below (a real commitment whose source edge later disappears) — that risk is unrelated and still accepted.** States: `OPEN, IN_PROGRESS, WAITING, DONE, DISMISSED` (Fork 2) — closed value set, not an enforced state machine. Auto-create, no review gate (Fork 4); `DISMISSED` is the after-the-fact undo, including for a "ghost" commitment whose source edge later stops being extracted (see `REASONING.md`'s 2026-07-16 implementation entry). No `$$commitments-by-status` index this session — deliberately deferred as Layer 3 scanning infrastructure. First read/write consumers added 2026-07-16: `WebhookReceiver`'s `GET /commitments/{familyId}` (scan-and-filter open-items view, still no index) and `POST /commitments/{id}/done` (appends to `*commitment-status-changes` only; also does a `commitmentExists` read as of 2026-07-17, transport-level only — picks the HTTP response code, does not gate the append). Not yet read by `QueryModule.java`/`DigestModule.java` — that's a later session. |

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
| `tags` | `List<String>` | Classifier category values (SCHOOL_EVENT, DEADLINE, PERMISSION_SLIP, TASK, UNKNOWN). Replaced single-value `eventType` 2026-07-05; classifier still emits one category, so 0..1 element for now — the List shape lets a later parser attach several. |
| `personId` | `List<String>` | Family members referenced by the email. Replaced `childName`/`childId` 2026-07-05. Currently holds the extracted child **name** (0..1 element) until an id-resolver session; sender-tagging not yet wired. |
| `relations` | `List<Map<String,String>>` | **Added 2026-07-15.** Typed triples `{relation, objectType, object}` — subject is implicit (this record's own `id`). Closed enums: `relation` ∈ MENTIONS_PERSON/PART_OF/LOCATED_AT/ACTION_NEEDED/UNKNOWN, `objectType` ∈ PERSON/ORG/PLACE/PROJECT/UNKNOWN, validated in `EmailParsingModule.parseRelations` before landing here — malformed entries are dropped, not coerced to UNKNOWN. Source-neutral field name (no email-specific naming) — any future parser populating the same shape gets `$$edges-*`/`$$entities` materialization for free. |
| `startTime` | Long | epoch ms |
| `deadline` | Long | epoch ms |
| `documentType` | String | **Schema-only (2026-07-05): plumbed, not yet populated — `null` this session.** |
| `relatedEventIds` | `List<String>` | **Schema-only (2026-07-05): plumbed, not yet populated — empty list this session.** |
| `confidence` | Double | **Schema-only (2026-07-05): classifier score, plumbed, not yet populated — `null` this session. Always `Double`, never `Integer`.** |
| `reason` | String | **Schema-only (2026-07-05): classifier rationale, plumbed, not yet populated — `null` this session.** |
| `urgency` | String | critical, high, medium, low |
| `status` | String | pending, completed |
| `sourceType` | String | email, test |
| `accountLabel` | String | Gmail account label |
| `created` / `updated` | Long | epoch ms |

> **Classifier-output fields** (`confidence`, `reason`, `documentType`, `relatedEventIds`) are plumbed into the record + serialization but the parsing agent does NOT populate them yet — `null`/empty is the correct passing state this session. Wiring the classifier to fill them is a later session. Verified that `null` map values round-trip through Rama serialization (`containsKey` true, `get` null), so no sentinel is needed — see `RAMA_VERIFIED_LEARNINGS.md`.

---

## Test Suite (145 tests, all non-LLM)

| Test class | Tests | What it covers |
|---|---|---|
| `CommitmentsTest` | 6 | `$$commitments` write-path — creation fires on first sight from an `ACTION_NEEDED` edge with correct content + initial `OPEN` status; independent edges mint distinct commitmentIds (no cross-event dedup); a non-`ACTION_NEEDED` relation seeds nothing (Fork 3 scoping); a status-change event updates `status`/`updatedAt` and leaves content untouched; **redraining the identical source edge after a status change does NOT reset `status` back to `OPEN`** (the core Fork 1 guarantee, verified individually via the surefire XML report, not just suite-green); a status change for a not-yet-materialized commitment is **dropped, not stubbed** (test 6, rewritten 2026-07-17 — previously asserted the opposite auto-vivify-stub behavior; see `REASONING.md`'s 2026-07-17 entry for why that's now superseded). Added 2026-07-16, test 6 revised 2026-07-17. |
| `OpenItemsAndMarkDoneTest` | 2 | Full loop over `WebhookReceiver`'s `openCommitments`/`markDone` (no HTTP, direct method calls against `InProcessCluster`-sourced `PState`/`Depot` handles): ingest an `ACTION_NEEDED` event → commitment appears in the open-items view → mark-done appends a status change → commitment disappears from the open-items view → redrain the identical source event → commitment stays absent, status verified to remain `DONE`. Plus (added 2026-07-17): `commitmentExists` returns true for a real, seeded commitmentId and false for an arbitrary garbage one — the transport-level read the mark-done endpoint uses to pick its HTTP response code. Added 2026-07-16, +1 test 2026-07-17. |
| `EdgesEntityIndexTest` | 11 | `$$edges-forward`/`$$edges-inverse`/`$$entities`/`$$entities-by-type` — forward+inverse edge materialization (paired: inverse assertions use the exact objectId extracted from the forward set, a genuine cross-direction consistency check, not two decoupled existence checks), entity-ID collapse within one event (two relations, same object+type → one entity row), cross-event distinctness (no dedup), `$$entities-by-type` inspectability for PERSON/PLACE **and the UNKNOWN bucket specifically** (a genuinely-unrecognized mention resolves back to its raw `canonicalName` via the index — the exact mechanism the `WORK`-type deferral depends on), no-op on an absent `relations` field, and idempotency under a simulated redrain (re-append the identical record, assert no new entities/no set growth, same entityId re-derived). Added 2026-07-15. |
| `MultiValueIndexTest` | 8 | Multi-element `tags`/`personId` fan-out completeness, tag/person branch isolation (no field bleed), keyword-branch coexistence under `anchor`/`hook`, and classifier-field `null` round-trip. Added 2026-07-05. |
| `NonLlmPipelineTest` | 20 | Schema → DigestModule pipeline, time filtering, serialization |
| `CohenFamilyDatasetTest` | 19 | Real 21-day dataset (13 email records), all indexes |
| `IndexPStateTest` | 13 | Person/tag index correctness (single-element lists) |
| `KeywordIndexTest` | 10 | `$$events-by-keyword` population — multi-field tokenization, case folding, stopwords, min-length, dedup, isolation |
| `WeaknessLeverageMapTest` | 9 | `$$leverage-map`/`$$weakness-map` population, digest reordering, weakness annotation, graceful no-op |
| `AccountLabelTest` | 8 | `$$events-by-account` index + DigestModule account filtering |
| `SiloIntentIndexTest` | 8 | `$$events-by-silo`/`$$events-by-intent` population and isolation |
| `QueryIndexTest` | 7 | `$$events-by-person` and `$$events-by-tag` range assertions |
| `SearchAgentTest` | 9 | `search-agent`'s two-tier hard/soft intersection — zero-dimension full scan, wrong-SOFT+right-HARD rescue, wrong-HARD+right-SOFT control (stays empty), HARD∩HARD genuine filtering, SOFT narrowing in the non-fallback path, all-SOFT-no-HARD-anchor stays empty, personId containment on a non-first list element, tags containment on a non-first list element (and that it genuinely excludes), 3-way compound tag+personId+date-range intersection. Added 2026-07-14 (+3 tests): the 6 original tests never populated a non-empty `personId` or a >1-element `tags` list, so containment through `search-agent` itself (as opposed to the raw index, covered by `MultiValueIndexTest`) was unexercised. |
| `DateIndexTest` | 6 | `$$events-by-date` range queries, effectiveTime logic |
| `RawEmailDepotTest` | 4 | `*raw-emails` write-ahead depot → `$$raw-emails` PState drain |
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

## Recently Completed (2026-07-05) — Schema refactor Session 2

`eventType`→`tags` (`List<String>`), `childName`/`childId`→`personId` (`List<String>`),
indexes renamed to `$$events-by-tag`/`$$events-by-person` (EXPLODE fan-out, isolated with
`anchor`/`hook`), and four classifier-output fields (`documentType`, `relatedEventIds`,
`confidence`, `reason`) plumbed schema-only. Clean swap — old fields/indexes removed, no
parallel-emit. 123/123 non-LLM tests green. **Now unblocked (deliberately deferred out of
this session):** (1) wire the classifier to populate `confidence`/`reason`; (2) `personId`
id-resolver + sender-tagging (currently holds child name only); (3) the multi-event
extraction below is now easier because `tags` already accepts multiple values per event.

## Recently Completed (2026-07-14) — search-agent compound-filter test coverage

A task framed as "build a compound search-agent" (`parse-filters → resolve-indexes →
intersect → finalize`) turned out to already exist, unchanged, since the 2026-07-03
session, and to already resolve correctly against the post-2026-07-05-refactor
`tags`/`personId` schema — the inverted indexes are populated via `Ops.EXPLODE` fan-out
(one write per list element), so a single-value lookup against `$$events-by-tag`/
`$$events-by-person` already *is* list-containment matching, no extra logic needed.
Audited with file+line evidence rather than trusting the task brief's "this is new work"
framing; see `REASONING.md`'s "Audit before 'Piece 2: search-agent' task" for the full
trail. Zero production code changed (`QueryModule.java`, `FamilySchemaModule.java`
untouched).

The actual gap was test coverage: `SearchAgentTest`'s original 6 tests never populated a
non-empty `personId` or a >1-element `tags` list, so containment *through search-agent*
(as opposed to the raw index, covered by `MultiValueIndexTest`) was unexercised. Added
tests 7-9: personId containment on a non-first list element, tags containment on a
non-first list element (with genuine wrong-dimension exclusion controls), and a 3-way
compound tag+personId+date-range intersection. **126/126 non-LLM tests green** (was
123/123), zero regressions.

**Deliberately not done this session** (flagged, user declined): `QueryModule.QueryParams`
still names its fields `childName`/`categoryFilter`, not `personId`/`tags` — functionally
correct (verified above) but inconsistent with current schema vocabulary. Renaming would
also touch `interpret-query`'s LLM prompt JSON schema. Revisit if it becomes confusing in
a future session.

## Recently Completed (2026-07-15) — Graph schema evolution: typed relations + entity foundation

Design-then-implement session, step 1 of a four-part path between search and future
"Layer 2 commitments." Entity resolution and co-occurrence edges were explicitly out of
scope (separate future briefs) — see `REASONING.md`'s 2026-07-15 entries (a design-only
entry with the full audit trail, followed by an implementation entry) for the complete
decision record.

Added 4 PStates to `FamilySchemaModule` (`$$edges-forward`, `$$edges-inverse`,
`$$entities`, `$$entities-by-type` — schemas in the table above) and a `relations` field
to the event record, emitted by `EmailParsingModule`'s extract-details LLM call as
closed-enum triples (`{relation, objectType, object}`) and validated before landing on
the record. All 4 new PStates are populated by a new branch in the existing
`anchor("fanoutRoot")`/`hook(...)` fan-out structure — same `Ops.EXPLODE` pattern as
tags/personId/keyword, verified via `javap` against the pinned `rama-1.5.0.jar` that
`Block.each` has the needed multi-argument overloads (`RamaFunction2`/`RamaFunction3`)
before writing the code.

Entity IDs are deterministic (`hash(eventId|objectType|object)`, `UUID.nameUUIDFromBytes`,
never `randomUUID`) so PState redrain reproduces identical IDs — grounded in Rama's own
documented determinism requirement (`redplanetlabs.com/docs/~/operating-rama.html`,
"Task scaling" section, fetched this session since Chat-o-rama was unreachable). The hash
deliberately omits a mention-index: the same object+type reached via two different
relations within one event collapses to one `$$entities` row, not one per relation-slot;
cross-event distinctness is preserved (different events mentioning the same name still
mint different entityIds until a future resolution effort merges them).

Purely additive — zero existing PState declarations or `QueryModule.java`/`DigestModule.java`
code touched. Fully backward compatible — every pre-existing record (none of which has a
`relations` field) produces zero writes to the 4 new PStates, same no-op behavior as an
absent `tags`/`personId` list. **137/137 non-LLM tests green** (was 126/126), zero
regressions — new `EdgesEntityIndexTest` (11 tests) exercises the new branch directly,
since no pre-existing fixture ever populated `relations`.

## Recently Completed (2026-07-16) — Layer 2 Commitments write-path

Design-then-implement, two sessions (localSelect/`ifTrue` mechanism verified via jar + a
throwaway probe first; the write-path itself second). Locked design: commitments seed ONLY
from `ACTION_NEEDED` edges (Fork 3); creation is recomputed every `*family-events` redrain,
not a depot record (content fields always refreshed, deterministic); `status` is write-once,
owned exclusively by a new permanent depot/branch (`*commitment-status-changes`); commitment
IDs deterministic (Fork 5, duplicates allowed by design); five-value closed status vocabulary
(Fork 2); auto-create, no review gate (Fork 4). Full rationale and the corrected (recompute +
create-if-missing, not co-partitioned `subSource`) mechanism are in `REASONING.md`'s
2026-07-16 entries.

Added `$$commitments` and `*commitment-status-changes` (`Depot.hashBy("familyId")`) to
`FamilySchemaModule`. The creation branch lives inside the existing `relations`-EXPLODE
branch, guarded to `ACTION_NEEDED` only; the status-change branch is a **second
`.source(...)` call on the same `family-events-stream` topology object** — not a separate
topology, since a PState can only be written by the one topology that declared it (a real
`IllegalWriteException` hit and fixed this session; see `RAMA_VERIFIED_LEARNINGS.md`).
Deliberately dropped `$$commitments-by-status` this session (Layer 3 scanning
infrastructure, rebuildable later) — simplifies the status-change branch to a plain
unconditional partial write, relying on Rama's auto-vivify behavior for the "arrived before
creation" stub case instead of explicit branching.

**143/143 non-LLM tests green** (was 137/137), zero regressions — new `CommitmentsTest` (6
tests) exercises creation, cross-event distinctness, Fork-3 scoping, status-change
application, and — verified individually via the surefire XML report per explicit request,
not just suite-green — **the core guarantee: redraining the source `ACTION_NEEDED` edge
after a status change does not reset `status` back to `OPEN`**. Not yet read by
`QueryModule.java`/`DigestModule.java` — consuming commitments is a later session. A known,
accepted consequence is logged in `REASONING.md`: a commitment whose source edge is later
removed by a parser change becomes a permanent "ghost" (same risk family as the already-
accepted auto-create risk; `DISMISSED` is the same fix for both).

## Recently Completed (2026-07-16) — Open-items view + mark-done path (first $$commitments consumers)

First read/write consumers of Layer 2's `$$commitments`/`*commitment-status-changes`, built as
two additions to the existing webhook server (`WebhookReceiver.java`, `com.family.assistant.webhook`)
rather than a new module or a `QueryModule` route:

- **`GET /commitments/{familyId}`** — open-items view. Scan-and-filter: `selectOne(Path.key(familyId))`
  on `$$commitments` (confirmed NOT subindexed — no `.subindexed()` on its declaration, unlike
  `$$events-by-date` — so this is a plain `Map` read, no `RocksDBWrapper` gotcha), then filter
  `status != "DONE"` in plain Java. No `$$commitments-by-status` index — still deliberately deferred
  to Layer 3; flagged again here as a known future cost (O(n) scan per family per request).
- **`POST /commitments/{commitmentId}/done`** — appends `{familyId, commitmentId, newStatus: "DONE",
  changedAt}` to `*commitment-status-changes` only, via `WebhookReceiver.markDone(...)`. Never writes
  `$$commitments` directly — that PState stays owned exclusively by `FamilySchemaModule`'s stream
  topology, per `RAMA_VERIFIED_LEARNINGS.md`. `familyId` defaults to `"keeling-family-001"` from an
  optional JSON body field, matching the existing `/query` route's convention.

`WebhookReceiver`'s constructor was extended to take `PState commitmentsPState` and
`Depot statusChangesDepot` (alongside its existing `AgentClient` params), wired in
`FamilyAssistantApp.main()` via `cluster.clusterPState(...)`/`cluster.clusterDepot(...)` — the same
`ClusterManagerBase` handles already used for the pre-existing `/debug/pstate` routes, confirmed via
`javap` against the pinned `rama-1.5.0.jar` to work identically for both `RamaClusterManager` (cluster
mode) and `InProcessCluster` (local mode / tests). The route logic itself was extracted into two public
methods (`openCommitments`, `markDone`) so tests exercise the exact same code the HTTP routes call,
without booting Javalin.

**144/144 non-LLM tests green** (was 143/143), zero regressions — new `OpenItemsAndMarkDoneTest`
(1 test) exercises the full loop: ingest an `ACTION_NEEDED` event → commitment appears in the
open-items view → mark-done → commitment disappears from the open-items view → redrain the identical
source event → commitment stays absent (status verified to remain `DONE`, not reset to `OPEN`).

## Recently Completed (2026-07-17) — Pre-deploy Part 1: unknown-ID topology guard + debug-route gating

Part 1 of the first real cluster deploy plan (`~/.claude/plans/first-cluster-deploy.md`),
landed after a full pass through the user's Lumino Plan Review Gate checklist (now also
copied into this repo at `docs/PLAN_REVIEW_GATE.md` — an 11-gate red-team checklist a
separate Claude-chat session runs against Claude Code plans before approval). Two new project
docs landed this session: `EDGE_CODE_RULES.md` (standing rule — edge/glue code receives →
appends raw → acks, no business decisions; read alongside this file and
`RAMA_VERIFIED_LEARNINGS.md` before writing any code) and `docs/PLAN_REVIEW_GATE.md`.

**Unknown-ID handling reversed, not extended (gate-review revision C):** the authoritative
fix moved from an edge-side check (rejected — violates `EDGE_CODE_RULES.md` Gate 6/Gate 8,
"deduplicating/checking existence before append belongs in a topology, not the edge") into
`FamilySchemaModule`'s status-change branch itself: a new `localSelect`/`ifTrue` guard
(mirroring the creation branch's existing pattern) drops a status change for any
commitmentId the ACTION_NEEDED branch never seeded, instead of auto-vivifying a stub with an
unbounded key-space. `WebhookReceiver` keeps a `commitmentExists` read, but only to pick the
HTTP response code (404 vs 200) — it doesn't gate whether `markDone` appends, which it always
does; that read can race a mid-flight event and return a false 404, an accepted tradeoff.
`CommitmentsTest`'s test 6 previously asserted the OPPOSITE (auto-vivify creates a stub) —
rewritten to assert the drop instead, since that's exactly the behavior this guard removes.
The separate dropped-source-edge "ghost" case (a commitment that DID exist at creation time,
whose ACTION_NEEDED edge later stops being extracted) is unrelated and still on the accepted
list — see `REASONING.md`'s 2026-07-17 entry for the full distinction.

**Debug routes gated (gate-review revision D):** `/debug/pstate`, `/debug/pstate/{familyId}`,
and `/debug/inject-test-event` now only register at all when `DEBUG_ROUTES_ENABLED=true` is
set — default off, and when off the routes don't exist rather than 404ing. `README.md`
updated to document the flag and warn against enabling it behind the Cloudflare tunnel.

**145/145 non-LLM tests green** (was 144/144) — net +1 (`OpenItemsAndMarkDoneTest` gained a
`commitmentExists` test; `CommitmentsTest` stayed at 6, with test 6 rewritten not added to).
Zero other regressions. No deploy command run this session — Part 1 (code) only, per the
plan's own gating; Part 2 (cluster deploy) and Part 3 (go-live) are a separate session's work,
starting from an audit-first read of this file and `RAMA_VERIFIED_LEARNINGS.md` per standing
instruction.

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
