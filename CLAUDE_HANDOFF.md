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
- **CURRENT (2026-08-09): 155 run, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**, from a
  plain `mvn test` with a real `GEMINI_API_KEY` still exported in the shell. **No `env -u`
  needed and no live API spend** — the default `excluded.groups` is now `llm,gmail`, which
  excludes `GmailIngestionTest` (the last live-spend test that ran by default) rather than
  skipping it. That is why the count is 155 and not the 156/1-skipped figure measured
  earlier the same day under `env -u GEMINI_API_KEY mvn test`.
  **`mvn test` is now the correct routine command.** To opt the live path back in:
  `mvn test -Dexcluded.groups=llm` (Gmail only) or `-Dexcluded.groups=` (everything).
  **Every count below this line is stale** — retained only for the history of what each
  session added.
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
| `modelId` | String | **Added 2026-08-01.** LLM that produced this parse — `EmailParsingModule.MODEL_ID`, currently `gemini-2.5-flash`. The agent-object builder references the same constant, so the stamp cannot drift from the model actually called. |
| `promptVersion` | String | **Added 2026-08-01.** Generation of the classify + extract-details prompts — `EmailParsingModule.PROMPT_VERSION`, currently `v1`. **Bump on any prompt-text change.** Labels the prompt generation, not the parse date. |
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

## Recently Completed (2026-07-19) — Part 2: cluster deploy, all six modules live on internal APFS

Fixed the exFAT/AppleDouble crash from 2026-07-18 by repointing `rama.yaml`'s `local.dir` to
`/Users/toddkeelingfolder/rama-data` (internal APFS). This session: started ZooKeeper, Conductor,
Supervisor — all confirmed stable, Conductor specifically verified past the exact cleanup-cycle
crash point that killed it on exFAT. Deployed all six modules fresh (`--action launch --tasks 4
--threads 4 --workers 1 --replicationFactor 1` — the fresh APFS `local.dir` meant none of the six
had prior state to `update`, including the two deployed back in April to the now-superseded exFAT
cluster). All six confirmed `RUNNING` via `rama moduleStatus <ShortName>` (short name, not FQCN —
see `RAMA_VERIFIED_LEARNINGS.md`). `rama numSupervisors` → `1`, license active/2-node capacity, no
concern. Full detail and the memory-pressure finding below: `REASONING.md`'s 2026-07-19 entries.

**Part 2 is complete. Part 3 (Gmail/OAuth/ingestion) is next, but its first step is worker-heap
right-sizing, not OAuth.** Six worker JVMs at the current uniform `worker.child.opts: -Xmx4096m`
commit 24GB of max heap on a 24GB machine — deploy-time churn alone (no steady-state load yet) hit
~59MB free system memory and made the heaviest module (`QueryModule`, two agents) take 8+ minutes
instead of the ~30s the other five needed. Nothing crashed, but there's no margin left for the
real load Part 3 adds (Gmail fetch, LLM calls, active stream processing). Right-size
`worker.child.opts` (uniformly lower, or per-module if the lighter ingestion modules don't need
4GB) before doing any Gmail/OAuth/ingestion work.

## DECISION (2026-07-19, Part 3 pre-restart session) — Mac Mini deploy abandoned; moving to cloud

**The heap-right-sizing attempt above didn't just proceed slowly — it hit a hard wall that proved
the Mini cannot host this reliably.** Full narrative in `REASONING.md`'s 2026-07-19 (continued)
entries; summary here for anyone picking this project up next.

After a clean cold-restart test (machine was powered off between sessions; all six modules'
persisted state — jars, RocksDB PState skeletons — survived intact on APFS, and Conductor +
Supervisor auto-recovered all six to `RUNNING` without needing a redeploy), the plan was to
right-size worker heap from `-Xmx4096m` to `-Xmx1536m` per module via `rama deploy --action update
--configOverrides`, one module at a time, verifying the actually-applied heap in `supervisor.log`
after each (not just trusting `moduleStatus: RUNNING`, per this session's own hard-won lesson about
verifying claims against logs, not status codes).

**Result: 1 of 6 succeeded (`EmailParsingModule`, confirmed genuinely serving at `-Xmx1536m`,
verified by instance-ID match, not just log presence). `FamilySchemaModule`'s update — the
foundational module, owning 15 PStates + 3 depots, the largest handover surface of any module —
got stuck mid-handover (`UPDATE-PREPARE-HANDOVER` state, per its worker log) and was killed by
Supervisor's ~30-second heartbeat watchdog. `moduleStatus` still reported `RUNNING` throughout,
because it silently kept serving the OLD `-Xmx4096m` instance — a genuinely misleading signal that
was only caught by comparing `appendTargetId` against the actual new instance ID, not by trusting
the status string.** At the point this was caught, system free RAM had dropped to **73MB**, load
average had climbed to **6.2** (idle baseline: 1.5), and the memory compressor held **10GB** —
i.e., macOS was under real, active memory pressure while this was happening, not just running
close to a hypothetical ceiling.

**Working diagnosis (unconfirmed, logged as a hypothesis in `RAMA_VERIFIED_LEARNINGS.md`):** the
RAM starvation and the handover failure are plausibly the same problem, not two separate ones — a
heavy module's handover needs enough headroom to complete its RocksDB/task-state sync inside the
watchdog window, and a machine already down to double-digit MB free under compressor pressure makes
that sync slower, which makes it more likely to miss the window, which kills the new worker and
leaves the module stuck on its old (also 4096m, also uncomfortable) instance. If true, this is a
**loop that heap-tuning alone cannot escape on this hardware**: the fix for RAM pressure is itself
handicapped by the RAM pressure.

**Decision: stop the Mac Mini deploy here. Do not retry `FamilySchemaModule`, do not touch the
remaining four modules (`EmailIngestionModule`, `GmailIngestionModule`, `DigestModule`,
`QueryModule` — all still at `-Xmx4096m`).** All Rama daemons (ZooKeeper, Conductor, Supervisor,
all six workers) were cleanly stopped (`SIGTERM`, confirmed exited, all ports 2000/1973/8889/3001-
3007 clear) at the end of this session. `rama-data/` and all persisted state remain on disk,
untouched — nothing was deleted. **The Mini deploy is considered a complete and valuable result on
its own terms: it proved the six-module architecture deploys, runs, and survives a cold restart
with data intact. It also proved the hardware itself (24GB RAM, six AOR-heavy modules) is
undersized for comfortable operation, independent of any code defect.** Next step is a cloud VM
sized with real headroom (six modules at a sane heap plus genuine margin for Gmail/LLM/ingestion
load, not squeezed to the ceiling) — planned as a dedicated next session, not a continuation of this
one. `worker-heap-overrides.yaml` (project root, `worker.child.opts: "-Xmx1536m"`) is still valid
and reusable as a starting point on whatever platform hosts this next, though the target value
should be reconsidered once real headroom is available rather than assumed to be depend on rescuing
a 24GB box.

## ▶ NEXT SESSION STARTS HERE (set 2026-08-09, end of session)

**State: deploy track is unblocked through D3. Code track B1 is approved but NOT started.**
Tree is clean and committed on `feature/raw-ingestion-depot`.

### Resume at: B1 — but NOT to the shape the go-live checklist specifies

Two of the checklist's "confirm before wiring" items were checked at source this session and
**came back negative**. Full evidence in `docs/decisions/PLAN_provenance_temporal.md`
("VERIFY-BEFORE-WIRING RESULTS"). Read that before writing any B1 code:

1. **`classifyByKeyword` DOES fire on an in-schema `UNKNOWN`** (the checklist says it does
   not). `UNKNOWN` is both the sentinel default and a valid enum member, so all four paths
   collapse to the same string and `EmailParsingModule.java:347` cannot tell them apart.
   B1 must *create* that distinction via two closed-set fields — `outcome`
   (`ok`/`off-schema`/`parse-error`) and `categoryBasis` (`model`/`keyword`/`none`) —
   added alongside unchanged control flow, B0-style.
2. **`created` is NOT recomputed on redrain** (the checklist warns that it is). It is
   stamped at `EmailParsingModule.java:418` inside the write-to-store *agent node*, into the
   depot payload, before `depot.append`. `assertedAt` should use exactly that mechanism.

### Open question parked for next session (B1 plumbing, not a blocker)

The classify node currently emits 4 values (`message, categoryStr, silo, intent`). Adding the
derivations map needs a 5th, and **the AOR node-lambda arity limit was not confirmed** — the
research was cut short by end of session. Two options: check the arity ceiling in
`docs/Agent_O_Rama_Complete_Documentation.md`, or sidestep it entirely with a small
`RamaSerializable` classification carrier (precedent: `ParsedEvent` already crosses that same
node boundary as a POJO). The carrier avoids the question and is probably the better shape
regardless. **Do not guess the arity** — that is the class of error CLAUDE.md warns about.

### Deploy track — ready to run, nothing blocking

D0 ✅ and D1 ✅ are done. D2 can proceed as written: daemons from any cwd (both now resolve
to the clean canonical `/Users/toddkeelingfolder/rama-zk`), `--action launch` all six,
`--configOverrides worker-heap-overrides.yaml` on every one. **D4 remains a hard stop until
B1 + B2 land.**

Cost-gate note now verified rather than assumed: the model is called **twice per email**
(classify + extract), and `classifyByKeyword` does **not** save a call — it runs *after* the
classify call has already been made and billed. Price the backlog at `count × 2` calls.

---

## OAuth Token Durability — RESULT (2026-08-09)

- Aug 2 re-authed token tested on Aug 9 (exactly 7 days later) via `GmailWatchSetup`.
- Refreshed silently — no browser, no `invalid_grant`. Watch registered:
  **historyId=6420017, expires Sun Aug 16 2026.**
- Conclusion: token survived past the 7-day mark → the recurring `invalid_grant`/
  7-day-expiry cycle is broken for this credential.
- Still open: root cause of the PREVIOUS (pre-Aug-2) token death remains UNKNOWN;
  the ~100-refresh-token rotation limit is the leading candidate. This run does
  not stress that path.

Physical location confirmed by this run: the credential refreshed from
`<project-root>/tokens` (`tokens/StoredCredential`, 1179 bytes, mtime Aug 9 15:56),
with the process started from the project root. That confirmation is what unblocked
pinning the token path absolute (D1 below).

---

## Recently Completed (2026-08-09) — D1 deploy blocker: cwd overload killed at the root; suite honestly green

### The ZK half — `local-zk` is a hardcoded relative literal, so configuration cannot fix it

The 2026-08-02 entry below diagnosed the symptom correctly but assumed the data directory
was configurable. It is not. Verified by disassembling the actual 1.5.0 jar:
`rama devZookeeper` calls
`(mk-inprocess-zookeeper {:port <config ZOOKEEPER-PORT> :local-dir "local-zk"})`, where
**only `:port` comes from config** and `"local-zk"` is a compile-time constant resolved
against the launching process's cwd. No CLI flag, no `rama.yaml` key, no `-D` property.
Full bytecode evidence in `RAMA_VERIFIED_LEARNINGS.md`.

**Fix applied — symlink, not discipline.** One canonical absolute dataset at
`/Users/toddkeelingfolder/rama-zk`, with each candidate cwd's `local-zk` replaced by a
symlink to it. This makes a wrong-cwd launch *impossible* rather than merely discouraged,
which a "always start daemons from the project root" rule never could.

- `family_assistant/local-zk` → symlink to `/Users/toddkeelingfolder/rama-zk` ✓
- `~/rama-release/local-zk` → symlink to `/Users/toddkeelingfolder/rama-zk` ✓
- April dataset archived (moved, not deleted) to
  `family_assistant/local-zk.archive-april-2026-08-09` (460 KB)
- July dataset archived (moved, not deleted) to
  `~/rama-release/local-zk.archive-july-2026-08-09` (4.6 MB)
- `/Users/toddkeelingfolder/rama-zk` is **empty** — a clean dataset, per the decision below

Both cwds now resolve to the same physical dataset, so **which directory the daemons are
started from no longer selects a cluster.** Verified: `readlink` on both returns
`/Users/toddkeelingfolder/rama-zk`.

**Do not confuse `local-zk` with `local.dir`.** They are independent. `local.dir`
(`~/rama-data`, configurable in `rama.yaml`) holds PStates/RocksDB, depot replica logs and
module JARs; `local-zk` holds only cluster metadata. Re-verified 2026-08-09: `~/rama-data`
is **12 KB with zero RocksDB artifacts** (no `.sst`, no `CURRENT`, empty `conductor/jars`).

### The `tokens/` half — pinned absolute, and the silent failure made loud

`GmailService.java` no longer uses `new File("tokens")`. It now resolves an absolute
directory (default `/Users/toddkeelingfolder/CORSAIR/family_assistant/tokens`, overridable
via the `FA_TOKENS_DIR` env var) and **refuses three things that used to look like success**:

1. a non-absolute override — that would reintroduce the cwd bug
2. a missing directory
3. a missing `StoredCredential` **unless** interactive consent is explicitly enabled

Check 3 is the important one: a cluster worker cannot answer a browser OAuth flow, so the
old behavior was a hang on `127.0.0.1:8888` *after* startup had already reported success.
`GmailOAuthSetup` and `GmailWatchSetup` opt in via `GmailService.allowInteractiveConsent()`
in their `main()` — deliberately in `main()`, not inside `renewWatch()`, so a future
non-interactive caller of `renewWatch()` still fails loudly. The resolved token dir and
whether the credential is present are now printed on every authorize.

### Test baseline — corrected, and now honestly green

**Actual current baseline: 155 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**
(verified 2026-08-09 with a plain `mvn test`, after the `llm,gmail` exclusion below).
An intermediate measurement the same day, before that exclusion, read 156 run / 1 skipped
under `env -u GEMINI_API_KEY mvn test` — both are correct for their respective configs.

Two prior numbers in this file were wrong. The "145/145" at the top and the
"151/152" quoted for this session are both stale: B0 added 4 tests, and the real count is
**156**. The `06fa488` commit message's "156 run, 155 pass" was the accurate figure.

`pom.xml`'s `<GEMINI_API_KEY>${env.GEMINI_API_KEY}</GEMINI_API_KEY>` is fixed. Maven does
not substitute an unset `${env.X}` — it passed the **literal string** through, which is
non-null, so every `assumeTrue(key != null)` guard passed and the test ran with a garbage
key instead of skipping. Now an empty default property is overridden by a `gemini-key`
profile that activates only when the env var is genuinely present.
`GmailIngestionTest`'s guard was hardened in the same pass to reject null, blank, **and** a
literal `${...}` — so reverting the pom cannot silently un-fix this.

**Superseded later the same session — `mvn test` is now green on its own.** The pom fix
alone did not achieve that: `GEMINI_API_KEY` is exported from `~/.zshrc` (real key), so in a
normal shell the guard passed legitimately, `GmailIngestionTest.testGmailToFamilyData` ran,
and it still hit the known "Executor pool is shut down" InProcessCluster ordering defect
(passes in isolation, fails in full-suite position). **That defect is untouched and still
pre-existing** — it is now simply not reached by a default run.

The durable fix was widening the default `excluded.groups` from `llm` to `llm,gmail`.
Verified: **plain `mvn test` → 155 run, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**,
with the real key still exported and zero live API calls.

Correcting an overstatement made earlier in this same session: `EmailIngestionTest`,
`FamilyAssistantTest` and `QueryAgentTest` were **never** live spend on a default run —
they are `@Tag("llm")` and `excluded.groups=llm` predates this session. The only test that
was actually spending was `GmailIngestionTest`, tagged `@Tag("gmail")`, which nothing
excluded. See `docs/decisions/PLAN_provenance_temporal.md` ("CONFLICT 3").

### RESOLVED (Tor, 2026-08-09) — canonical ZK starts CLEAN

The go-live checklist's D2 said "start daemons (real July metadata dataset from
`~/rama-release`)" **and** "`--action launch` (NOT update) all six modules." Those two
cannot both hold: if the July dataset were reused, the six modules from Jul 31 are already
registered in it and `--action launch` is rejected for an existing module name.

**Decision: clean dataset + `--action launch`** — consistent with the 2026-08-02 Option 1
lock and with D2's own action bullet. Rationale: the July ZK metadata is orphaned
regardless. It describes six module instances whose JARs and worker state no longer exist
anywhere in `~/rama-data` (12 KB, zero RocksDB artifacts, empty `conductor/jars`), so
launching against it would produce modules with no backing data. Nothing real is lost —
module JARs rebuild from the fat jar and the PStates were empty by definition.

Both datasets remain on disk as archives, so this is reversible.

---

## Next Task (set 2026-08-02) — BLOCKER FOUND: cwd is overloaded (ZK dataset vs `tokens/`). Nothing deployed.

**Read this before the 2026-08-01 section below — it supersedes that section's deploy step.** The
2026-08-01 plan says "start daemons from the project root, then `--action update` the two modules."
That cannot work as written, and the reason is a genuine conflict, not a mistake in the plan.

### Root cause — cwd selects the ZooKeeper dataset

**`rama devZookeeper` resolves its data directory relative to cwd.** There are two `local-zk` datasets
on this machine and cwd decides which one the cluster sees:

| Dataset | Contents | Last written |
|---|---|---|
| `family_assistant/local-zk` | **stale April data** (Apr 11–22) | Aug 2 16:35 — today's project-root run |
| `~/rama-release/local-zk` | **the real July cluster metadata** | Jul 31 17:36 |

Starting the daemons from the project root therefore pointed the cluster at the **April** dataset, which
has never heard of the July deployments. All six modules read `{"moduleState":"NOT_ALIVE",
"appendTargetId":null}` — not because anything broke, but because they were launched against the other
ZooKeeper. Confirmed: all six module names appear by exact class name in `~/rama-release/logs/conductor.log`,
alongside 7 worker logs from Jul 31.

**cwd is overloaded.** `tokens/` is *also* cwd-relative (`GmailService.java:73`) and requires the
project root so workers find the OAuth credential. So one setting controls two unrelated things and
they want opposite values:

- project-root cwd → `tokens/` resolves ✓, wrong ZK dataset ✗
- `~/rama-release` cwd → correct ZK dataset ✓, `tokens/` silently fails after OAuth reports success ✗

### DECISION for next session — Option 1: fresh launch from project root

1. **Archive `family_assistant/local-zk` first** so ZooKeeper starts clean (do not merge or transplant
   datasets — move the April one aside).
2. Start daemons from the project root, `GEMINI_API_KEY` exported before the Supervisor.
3. **`--action launch` all six modules — NOT `update`.** They do not exist in a clean ZK dataset, so
   `update` has nothing to update.
4. **`appendTargetId` / cutover checks do not apply to a first launch.** There is no prior instance to
   cut over from; a fresh launch has no before-value to compare against. Skip that verification — it is
   a redeploy check and reporting it here would be meaningless.
5. `--configOverrides worker-heap-overrides.yaml` on **every** module — never inherited.

**Nothing real is lost by launching fresh.** `local.dir` (`~/rama-data`) holds **no RocksDB artifacts
at all** — no `.sst`, no `CURRENT`, no `MANIFEST`, 15 MB total, `conductor/jars` empty. That matches the
record that nothing was ever ingested: the PStates were empty by definition. What the April dataset
costs us is deployment *metadata*, not data. Module JARs rebuild from the fat jar.

### Durable fix — do this next session

**Pin both cwd-relative paths to absolute locations** so neither depends on where a daemon happens to be
started: the ZooKeeper data dir, and `tokens/` in `GmailService.java:73`. Until that is done, every
daemon start is a chance to silently pick the wrong ZooKeeper dataset, and every worker start is a
chance to silently miss the OAuth credential. Both failures look like success at startup.

### Next session resumes at

`clean ZK` → `launch six` → `backlog count + Gemini pricing` → **cost sign-off (HARD STOP)** → `ingest`

### Also verified this session

- **Daemons log to `~/rama-release/logs/`, not `family_assistant/logs/`.** A `nohup` redirect from the
  project root captures only stdout and yields a 0-byte file; the real conductor/supervisor/worker logs
  are in the release directory. Look there when diagnosing.
- Daemon startup itself is sound: ZK → `conductorReady` → `numSupervisors` all came up clean from the
  project root, and all three daemons held cwd = project root (`lsof -a -p <pid> -d cwd`). Worker cwd
  was **not** verified — no module was ever launched, so no worker process existed to check.
- `local.dir` is `/Users/toddkeelingfolder/rama-data` per `rama.yaml`. The "Components needed" section
  further down still says `/Volumes/CORSAIR/rama-data`; that is stale — `rama.yaml` is authoritative and
  documents the 2026-07-17 move to internal APFS.

## Next Task (set 2026-08-01) — Step 0b: code is DONE and green; resumes at DEPLOY

**Both code blockers are cleared and committed. Nothing was deployed — the session was time-boxed and a
module update that cannot be watched to completion is worse than one not started.** Resume here:

**1. Deploy the two changed modules.** `mvn clean package -DskipTests`, start daemons **from the project
root** (see step 2), then `--action update` **`EmailParsingModule` first** (lighter; its update genuinely
succeeded on 2026-07-19, so it rehearses the procedure) and **`GmailIngestionModule`** second. Both need
`--jar` AND `--configOverrides worker-heap-overrides.yaml` — overrides are never inherited; the trap has
already fired once. **`FamilySchemaModule` is NOT redeployed, so the `UPDATE-PREPARE-HANDOVER` risk does not
apply.** Record `appendTargetId` for both modules BEFORE updating, and verify after: `appendTargetId` must
have CHANGED, match the new instance ID from the launch log, and equal `readTargetId`. `RUNNING` alone proves
nothing.

**2. Start the Supervisor with `cwd` = project root, and export env before starting it, not before deploying.**
New this session — see `RAMA_VERIFIED_LEARNINGS.md` ("Relative file paths resolve against the SUPERVISOR's
cwd"). `GmailService.java:73` uses a relative `tokens/` path, so in cluster mode the worker resolves it
against the Supervisor's cwd. **This would have failed silently AFTER OAuth reported success.** Verify with
`lsof -a -p <worker-pid> -d cwd` — that check is still outstanding.

**3. OAuth consent flow — DONE (2026-08-02).** Consent screen is **"In production"** (confirmed against
the live Audience screen for `family-assistant-dev-490204` on 08-02). The dead Jul 17 credential was
backed up, deleted, and re-authorized via `GmailOAuthSetup`. Verified twice: the runner's real
`getProfile()` call returned `toddkeeling@gmail.com` / 45,875 messages, and a direct `refresh_token`
grant against `https://oauth2.googleapis.com/token` returned **HTTP 200** (`expires_in` 3599, scopes
`gmail.readonly gmail.modify`). `tokens/StoredCredential` is now a working **Aug 2 16:14, 1178-byte**
file with both refresh and access tokens present. The `.dead` backup was shredded after verification.

`mvn -q compile exec:java -Dexec.mainClass="com.family.assistant.gmail.GmailOAuthSetup"` worked fine —
the previously suspected bad interaction with the pom's `exec-maven-plugin` config (configured for
`exec:exec`) **did not materialize**; the plugin-level `executable`/`commandlineArgs` are ignored by the
`exec:java` goal. The fat-jar invocation remains a valid alternative, not a requirement.

**4. Step 3 cost gate — HARD STOP.** Needs a new `GmailBacklogCount` tool: read-only, **paginating**, counting
against `GmailQueryConfig.fetchQuery()` — the same resolver the module calls, which is the entire point of the
query fix. Do **not** use the module's `alreadyProcessed` field for this: it reads one result page and
saturates (known defect, left in place deliberately, documented in code). Price at `count × 2` Gemini calls,
fetching current `gemini-2.5-flash` rates at gate time rather than quoting from memory. **Tor's explicit
sign-off before anything ingests.**

**5. Then Step 4** (ingest + measure RSS under load; `maxResults=10` with no pagination means ingestion is
inherently batched in tens — a useful throttle) **and Step 5** (`/commitments` end to end, plus read back one
record to confirm `modelId`/`promptVersion` actually landed — the real verification of the provenance work).

**Floor numbers below still apply** — re-establish the floor from scratch; the 07-31 cluster-up system totals
are Spotlight-contaminated.

---

## Recently Completed (2026-08-01) — Step 0b Tasks 1 & 2: configurable Gmail query + provenance stamping

**152/152 non-LLM tests green** (was 145/145; +7 new `GmailQueryConfigTest`, `NonLlmPipelineTest` test 33
amended not added). Zero regressions. Code committed; **no deploy** — see Next Task above.

**Task 1 — the query is no longer hardcoded, and the two call sites can no longer drift.** New
`com.family.assistant.gmail.GmailQueryConfig` resolves env `GMAIL_INGEST_QUERY` → `application.properties`
`pa.gmail.query` → `DEFAULT_QUERY`, which is now exactly
`in:INBOX -label:FamilyAssistant/Processed after:2026-07-01` — **date-bounded and deliberately NOT
`is:unread`** (read/unread is a mailbox UI concern; the `FamilyAssistant/Processed` label is the real record
of what has been ingested). `GmailIngestionModule` resolves it once per invocation and **derives** the
already-processed count query from that same value (`-label:X` → `label:X`, appending the term if no
exclusion is present) instead of carrying a second hardcoded string. Both queries are logged.
`FetchRequest` gained a nullable `query` field (2-arg and 3-arg constructors delegate, so both existing call
sites compile unchanged). `PROCESSED_LABEL_NAME` now has one definition instead of two.

**Why derivation rather than a second config value:** the cost gate prices what `GmailQueryConfig.fetchQuery()`
returns; if the count query were configured independently it could silently describe a different message
population. Derivation makes consistency structural rather than a promise.

**Task 2 — provenance.** `MODEL_ID` (`gemini-2.5-flash`) and `PROMPT_VERSION` (`v1`) constants in
`EmailParsingModule`, stamped onto `ParsedEvent` and onto **every** event record. The `gemini-model`
agent-object builder now references `MODEL_ID` instead of repeating the literal, so the stamp cannot drift
from the model actually called. Purely additive: no index reads them, nothing branches on them, no PState
declaration changed, and pre-existing records simply lack the keys.

**Gate-review confirmations recorded, because one of them is a correction worth keeping:** the
assembled-`HashMap`-plus-`termVal` trap does **not** apply here. This change adds **zero PState writes** —
`modelId`/`promptVersion` are two more keys on a *depot payload* appended to `*family-events`. The PState
write is `FamilySchemaModule.java:279`'s whole-record `termVal("*record")`, untouched, and it is safe because
grep confirms it is the **only** write to `$$family-data` anywhere — no later, narrower write ever navigates
into a stored event record. The trap only bites when a value is *both* assembled as one `Map` *and* later
targeted by a partial write (that is `$$commitments`, which correctly uses sequential `localTransform` calls
for exactly that reason). Also confirmed: no `Map.of()`/`List.of()`/`Arrays.asList()` in any new field
construction — the new fields are plain `String` constants.

**Two pre-existing defects found and deliberately left in place** (flagged, not silently fixed): the
`alreadyProcessed` counter reads a single result page and saturates (now documented in code as
display-only — **must not be used for the cost gate**), and two debug probes hardcode
`in:INBOX from:acemystuff@gmail.com` (log-only, fire only on a zero-result fetch).

**`EDGE_CODE_RULES.md` check:** no new creep. The query is a filter, but it is not a *new* filter — the Gmail
API requires a selector. This made an existing hardcoded one configurable and observable, moved zero decisions
into the edge, and left the `*raw-emails` write-ahead depot receiving every fetched message complete, so
replay is unaffected.

---

## Superseded Next Task (set 2026-07-31) — Step 0b resumes at OAuth; two blockers must clear before the cost gate

**Session of 2026-07-31 got through Step 1 only.** Cluster start and module verification are done and
reproducible; **OAuth re-auth was prepared but never executed**, so everything from the cost gate
onward is untouched. Details in "PARTIAL (2026-07-31)" below. Run the next session in this order:

**1. OAuth — DONE (2026-08-02), but the original failure is still unexplained.** The consent screen is
**"In production"** in `family-assistant-dev-490204`, confirmed on the live Audience screen. Re-auth is
complete and verified (see item 3 in the current-session list above).

**The "Testing → 7-day expiry" theory previously recorded here was wrong and has been removed.** Two
reasons it never held up: the client was already in production, and the observed lifetime doesn't fit —
the token was written **2026-07-17** and was dead by **07-31**, which is **14 days, not 7**. The earlier
note claimed this "fits the observed lifetime exactly"; it does not. So the cause of the 07-31
`invalid_grant` remains **unknown** — candidate explanations (unverified): a manual revoke, a Google-side
session/security event, or the 100-refresh-token-per-client-per-account rotation limit.

**Consequence: durability is unproven.** The 08-02 token is confirmed working *today*, but nothing here
predicts how long it lasts. Re-run the grant check around **2026-08-09** — that is the first real
datapoint on whether tokens now survive. If it dies again at ~14 days with the client in production, the
rotation-limit hypothesis is the one to chase first.

**2. Fix the hardcoded Gmail query, then redeploy `GmailIngestionModule` with `--configOverrides`.**
`GmailIngestionModule.java:148` hardcodes:
```java
String gmailQuery = "is:unread in:INBOX -label:" + PROCESSED_LABEL_NAME;
```
`FetchRequest` carries only `userId` and `maxResults` — **no query field**. The planned Step 3 filter
is `in:INBOX -label:FamilyAssistant/Processed after:2026-07-01`: deliberately **NOT** `is:unread`, and
date-bounded. **As deployed, the cost gate would price one set of messages and the module would
ingest a different set** — the gate would be meaningless. Make the query configurable (plumb it
through `FetchRequest`) so the gate prices exactly what runs. Note the count at line 138 uses a
matching hardcoded query and needs the same treatment.

This is the one redeploy that IS justified. **Use `--configOverrides` when you do it** — the trap has
already fired once and left five of six workers on the 4096m default. Fixing that default is *not* a
reason to redeploy on its own (measured: 2.67× ceiling bought 12.6% RSS), but since
`GmailIngestionModule` is being redeployed anyway, set its overrides correctly in the same action.

**3. Then Step 3 (cost gate) onward** — backlog count with the filter above, `count × 2` Gemini calls
at `gemini-2.5-flash` rates with a dollar figure, **HARD STOP for Tor's explicit sign-off** before
anything ingests. Then Step 4 (ingest + measure RSS under load) and Step 5 (verify `/commitments`
end to end).

### Floor numbers — read this before the next measurement

- **The clean floor is now ~19GB used / ~4.2GB unused**, not the ~17GB / 6458MB of the Step 0 run.
  Verified settled on 2026-07-31: compressor flat at **60MB** across 4 samples, swap 0/0,
  `mds_stores` 0.0%, `pgrep -f java` = 0. **Next session's loaded delta must subtract this floor, not
  Step 0's** — using the old one overstates Rama's footprint by ~2GB.
- **The 2026-07-31 cluster-up system-memory numbers are Spotlight-contaminated — do not reuse them.**
  `mds_stores` ran 120–165% for the entire cluster-up window (a reindex triggered by startup churn —
  the exact false-floor signature the Step 0 procedure warns about). Per-process RSS from that window
  is fine (it plateaued); the **system totals are not**.
- **The post-shutdown reading is also not a settled floor.** It read 15–16GB used / ~8GB unused, but
  the compressor was still at **377MB** against a 60MB clean baseline, i.e. ~317MB of compressed
  state had not yet drained, and `mds_stores` only reached 0.0% on the final sample. Re-establish the
  floor from scratch next session rather than trusting this number.

**Everything below from the 2026-07-30 entry still stands** — worker `-Xmx` is not worth tuning for
RAM, `conductor.child.opts` is the highest-value untried lever, consolidation is not needed.

---

## PARTIAL (2026-07-31) — Step 0b: Step 1 complete, Step 2 prepared but NOT executed

**Step 1 — clean start: COMPLETE.** Floor confirmed settled by sampling (not a single read): 4 samples
over 60s, compressor pinned at 60MB, free drifting <30MB, swap 0, `mds_stores` 0.0%, `pgrep -f java`
= 0. Started ZooKeeper → Conductor → Supervisor. `conductorReady` was `false` until the Supervisor
registered, then `true`; `numSupervisors` = 1.

**All six modules RUNNING, all six with `appendTargetId == readTargetId`:**

| Module | Target ID (append == read) |
|---|---|
| FamilySchemaModule | `55b3c805-76dc-6b42-6a41-bea212279e2b` |
| EmailParsingModule | `e4e90fad-5eb4-96d9-abd1-2de58ba3ad79` |
| EmailIngestionModule | `054a26c5-5b46-c061-2807-9c7bbd68d02d` |
| GmailIngestionModule | `2d94891f-aae2-06e4-7f0b-d4d9866470dd` |
| DigestModule | `6da28ce8-99b8-e12c-d33d-484747c0a6b2` |
| QueryModule | `9e10e2a1-ce37-615f-5928-f23404e3f588` |

Cluster-up per-process RSS, plateaued (sum oscillated 7693–7815MB across 5 samples, not climbing —
so this is a plateau, not a ramp). Consistent with the Step 0 idle profile; **not** a loaded figure:

| Process | RSS |
|---|---|
| QueryModule | 1075.9 MB |
| GmailIngestionModule | 973.1 MB |
| DigestModule | 961.2 MB |
| EmailIngestionModule | 957.1 MB |
| EmailParsingModule | 927.7 MB |
| FamilySchemaModule | 905.1 MB |
| Conductor | 713.2 MB |
| ZooKeeper | 695.2 MB |
| Supervisor | 604.2 MB |

**Step 2 — OAuth: NOT DONE as of 07-31.** The command was prepared and handed over, but the flow was
never run — verified afterward: `tokens/StoredCredential` was still the original **Jul 17 13:24, 846
bytes**, and the backup the script writes before deleting it (`StoredCredential.dead`) did not exist.

**Superseded 2026-08-02: the consent flow was executed and the token is now live and verified.** See
the OAuth entry in the current-session list. The store is a working **Aug 2 16:14, 1178-byte**
credential for `toddkeeling@gmail.com`; the `.dead` backup was shredded after the new token passed both
a `getProfile()` call and a direct `refresh_token` grant.

**Steps 3, 4, 5: not started.** No backlog count, no ingestion, no cost gate, no `/commitments`
verification. Nothing was ingested and no Gemini calls were made — **no spend occurred this session.**

**Useful things this session did establish:**

- **The port-8888 conflict is dead, verified at runtime.** `rama.yaml` sets `cluster.ui.port: 8889`;
  Conductor's UI was confirmed listening on **8889** while **8888 had no listener**. The project and
  deployed `~/rama-release/rama.yaml` are byte-identical, so this will not regress. The OAuth
  callback (`GmailService.java:78`, `LocalServerReceiver` on 8888) is clear to bind.
- **`GmailOAuthSetup` now exists** (`src/main/java/com/family/assistant/gmail/GmailOAuthSetup.java`).
  It was referenced in `GmailWatchSetup`'s javadoc as a prerequisite but had **never been written**.
  Auth only — no Pub/Sub watch, no cluster connection — and it makes a real authenticated
  `users().getProfile()` call so success means the token works, not merely that a file was written.
  Compiles clean. Deliberately do **not** re-auth via `GmailWatchSetup`: it registers a Pub/Sub watch
  as a side effect.
- **Re-auth requires deleting the token first.** `rm -f tokens/StoredCredential` — the library will
  not prompt for consent while a token file exists; it just retries the refresh and fails again.
  Authorize as **toddkeeling@gmail.com** (`GmailService.java:80` calls
  `.authorize("toddkeeling@gmail.com")`; a different account writes under the wrong user key).
  Expect the **"Google hasn't verified this app"** interstitial → **Advanced** → **Go to Family
  Assistant (unsafe)** — normal for a Desktop-type client in your own dev project. Scopes requested
  are Gmail **readonly + modify** (modify is required to apply the `FamilyAssistant/Processed` label).
- **ZooKeeper binds port 2000**, not 2181 — `lsof -iTCP:2181` will show nothing and that is correct.
- **Possible follow-up, unverified:** `GmailWatchSetup.TOPIC_NAME` is
  `projects/family-assistant-dev-490204/topics/gmail-notifications`, but this file's GCP section
  records the created topic as **`gmail-push-notifications`**. If those really differ, watch
  registration will fail. Not on the critical path (Step 0b ingestion is poll-driven, not push), and
  not checked against the console this session — confirm before relying on push.

**Shutdown was clean:** `rama shutdownCluster` (workers exited), then SIGTERM supervisor → conductor →
ZooKeeper in that order, `pgrep -f java` verified **0**. Floor left clean for next session, with the
draining caveat noted above.

---

## Next Task (superseded 2026-07-31, retained for context) — Load testing under real ingestion

**Step 0 is DONE. Its number is ~6GB idle — and "idle" is why this session exists.** The measured
figure has no app running, no ingestion, no LLM calls, no depot appends. It is a floor for the
cluster, not a working figure. **Do not provision a box on it.**

**Prerequisites, both of which are real gates, not formalities:**
1. **OAuth re-auth** — browser consent, requires Tor's hands. Cannot be automated from a session.
2. **Gemini cost gate** — backlog count first, then Tor's explicit sign-off on projected spend
   before any real ingestion runs. This gate exists because ingestion volume drives LLM cost
   directly.

**What to measure, once ingestion is actually flowing:** the same fields as Step 0 — per-worker RSS,
per-daemon RSS, and system totals (free, **compressor**, swap, load) — but under load rather than at
idle, and sampled over time rather than once. The delta between the ~6GB idle floor and the loaded
peak is the number that sizes the box.

**Measure from a clean, settled floor.** Step 0 nearly produced a wrong answer because compressed
pages understate RSS by ~2× (ZooKeeper read 413MB contaminated vs 813MB clean). The full procedure —
shut down, verify `pgrep -f java` = 0, confirm the floor is settled and not still draining, restart,
settle 10 min, confirm RSS has plateaued — is recorded in `RAMA_VERIFIED_LEARNINGS.md`
("Measurement contamination"). Follow it; do not shortcut it.

**Read the compressor, not free memory.** Low free is normal macOS behavior. The 2026-07-19 failure
signature was a **10GB compressor** at load 6.2. A healthy settled cluster showed 0.25GB free with
9.63GB reclaimable inactive, 0.00M swap, and a flat 2.12GB compressor — that is not pressure.

**Highest-value lever to try, and it is cheap:** `conductor.child.opts`. The three daemons cost
**2.33GB — ~39% of the idle total — before a single module loads**, with Conductor at 768.7MB
against a 1024m ceiling. Unlike worker `-Xmx`, changing it needs only a Conductor restart, no
redeploy, no `UPDATE-PREPARE-HANDOVER` risk. `worker.max.direct.memory.size` (500m × 6 = 3GB of
unchosen ceiling) is still unset too, but that one does require a redeploy on every module.

**Do NOT bother tuning worker `-Xmx` for RAM.** Measured: 2.67× of ceiling bought 12.6% of RSS.
Dropping five workers from 4096m to 1536m reclaims ~100MB each, not 2.5GB each — not worth a
redeploy. Note the `--configOverrides` trap has already fired: five of six workers currently run at
the 4096m default, only `EmailParsingModule` at 1536m. **This was left unfixed deliberately** and
should stay that way unless a redeploy is happening for another reason.

**Consolidation is not needed on current evidence** and should not be started. ~6GB idle against a
24GB Mini is not a squeeze. The three-module split stays pre-audited and available as a fallback if
loaded numbers say otherwise; its assessment is in `REASONING.md` (2026-07-29).

---

## COMPLETED (2026-07-30) — RSS measurement run: Step 0 executed, ~6GB idle measured

**Outcome, measured on a clean settled floor:**

| | PhysMem used | Unused |
|---|---|---|
| Floor — Rama down, IntelliJ gone, settled | ~17 GB | 6458 MB |
| Cluster up, settled, idle | ~23 GB | 128–345 MB |
| **Measured Rama footprint (idle)** | **~6 GB** | |

Sum-of-RSS reads 8.03GB but double-counts pages shared across nine JVMs on an identical classpath;
**~6GB is the honest figure.** Settled per-process RSS:

| Process | Port | `-Xmx` | PStates | RSS |
|---|---|---|---|---|
| GmailIngestionModule | 3004 | 4096m | — | 1076.2 MB |
| QueryModule | 3006 | 4096m | — | 1034.3 MB |
| DigestModule | 3005 | 4096m | **0** | 1011.3 MB |
| EmailIngestionModule | 3003 | 4096m | — | 1006.8 MB |
| EmailParsingModule | 3007 | **1536m** | — | 956.1 MB |
| FamilySchemaModule | 3001 | 4096m | **15** | **865.6 MB** |
| ZooKeeper | — | — | — | 812.8 MB |
| Conductor | — | 1024m | — | 768.7 MB |
| Supervisor | — | 1024m | — | 753.0 MB |

**Results against what this session set out to decide:**
- **24GB ceiling-arithmetic figure: retracted against measurement.** The ~9.5GB hypothesis was
  slightly conservative — right direction, right order of magnitude.
- **RocksDB block cache: per WORKER, resolved.** `FamilySchemaModule` (15 PStates) is *smaller*
  than `DigestModule` (0 PStates). The ~3.8GB unknown does not exist. Moved to Verified.
- **`-Xmx` is a ceiling: now measured**, not extrapolated. 2.67× ceiling → 12.6% RSS.
- **Measurement contamination discovered and documented** — compressed pages understate RSS ~2×.
- **Box sizing: still deferred**, because ~6GB is idle. See the Next Task above.

All four findings are written up as verified entries in `RAMA_VERIFIED_LEARNINGS.md`; the reasoning
and caveats are in `REASONING.md` (2026-07-30).

**Caveats that must travel with the ~6GB number:** it is idle-only, and the floor retained 1.78GB of
non-Rama compressor state, making the subtraction slightly generous to Rama.

**Operational notes learned:** `rama shutdownCluster` persists a `cluster-shutdown-complete` state in
ZooKeeper, so `conductorReady` reads `false` after restart until a Supervisor registers — it clears
itself, `forceClusterOpen` is not needed. `devZookeeper` listens on **port 2000**, not 2181.
`rama moduleStatus` takes the short module name as a **positional** arg (no `--module` flag). All six
modules restored from disk with no redeploy, `RUNNING` with `appendTargetId == readTargetId`.

<details>
<summary>Original 2026-07-29 task specification (retained for its reasoning)</summary>

**This is a measurement session. No code changes, no refactoring, no consolidation work.** Its
entire purpose is to replace an estimate with data. The 2026-07-19 "24GB is undersized, need 32GB"
conclusion was arithmetic on summed `-Xmx` ceilings and has been retracted — `FamilySchemaModule`'s
worker crash log shows G1 committed 352MB of a 4096MB ceiling and used 197MB of it. See
`RAMA_VERIFIED_LEARNINGS.md` ("`-Xmx` is a ceiling, not a reservation") and `REASONING.md`
(2026-07-29). Nobody has ever recorded RSS for a single worker in this project. Do that first.

**Step 1 — baseline, at current defaults.** Start ZooKeeper + Conductor + Supervisor and all six
modules on the Mini (procedure at the end of this file). Change nothing else — leave heap exactly as
it currently is so the baseline is comparable to the 2026-07-19 deploy. **Let it idle 5–10 minutes**
before recording, so JIT/metaspace/caches settle and the numbers aren't launch-transient.

Record, per worker (all six) and per daemon (Conductor, Supervisor, ZooKeeper):
- **RSS** — `ps -eo pid,rss,command | grep rama`. This is the number that matters. Map each PID to
  its module via the `rpl.rama.distributed.daemon.worker <port> <ModuleName>` argv on its command
  line; note the launched `-Xmx` and `-XX:MaxDirectMemorySize` alongside it.
- **Total system memory** — `vm_stat` plus `top -l 1 -s 0 | head -12`: free pages, compressor size,
  swap used, load average. The 2026-07-19 failure happened at 73MB free / 10GB compressor / load 6.2,
  so capture the same fields to make the two sessions directly comparable.
- Optionally per-worker heap vs. non-heap via `jcmd <pid> GC.heap_info` and `jcmd <pid> VM.native_memory`
  (the latter needs `-XX:NativeMemoryTracking=summary` in `worker.child.opts` to work — only worth
  adding if the RSS split turns out to be the interesting question).

**Watch `FamilySchemaModule` vs. `DigestModule` specifically.** This comparison resolves an open
question for free: `FamilySchemaModule` declares 15 PStates, `DigestModule` declares none, and
RocksDB's default 256MB block cache has undocumented scope (per PState → ~3.8GB for FamilySchema;
per worker → 256MB). If their RSS is comparable, the cache is effectively per-worker. If
FamilySchema is GBs higher, it scales with PState count. Write the answer back into
`RAMA_VERIFIED_LEARNINGS.md` — the entry is already staked out in the Unverified section.

**Step 2 — apply the two free levers, re-measure.** Both are pure config, no code:
1. **`worker.max.direct.memory.size`** — Rama defaults it to `500m` **per worker**, passed as
   `-XX:MaxDirectMemorySize=500m` *in addition to* `-Xmx` (confirmed in the crash log's `jvm_args:`
   line). `worker-heap-overrides.yaml` has never set it. Six workers × 500m = 3GB of ceiling nobody
   chose. Set it below the default and re-measure.
2. **`conductor.child.opts`** — defaults to `-Xmx1024m` for what is a pure coordination process.
   Try `-Xmx512m`.

Lever 1 goes in `worker-heap-overrides.yaml` and must be passed via `--configOverrides` on **every**
module's deploy — config overrides are never inherited between deploys (verified, see
`RAMA_VERIFIED_LEARNINGS.md`). Lever 2 goes in `~/rama-release/rama.yaml` and needs a Conductor
restart. **Verify what actually applied** by grepping `supervisor.log`'s `Launching process` line —
and remember that a `Launching process` line plus `moduleStatus: RUNNING` does NOT prove cutover;
match `appendTargetId` against the new instance ID (both traps are documented in
`RAMA_VERIFIED_LEARNINGS.md` and both have burned this project already).

**Redeploy risk, carried over:** `FamilySchemaModule`'s update is the one that got stuck at
`UPDATE-PREPARE-HANDOVER` and was watchdog-killed on 2026-07-19. If Step 2 needs a
`FamilySchemaModule` redeploy and RSS headroom looks tight at that moment, take the baseline as the
deliverable and stop — a stuck handover costs more than the lever saves. Baseline alone answers the
sizing question.

**What the outcome decides:**
- Measured total comfortably under 16GB → the Mini (24GB) may be viable after all, or a modest cloud
  box is; **consolidation is not needed** and the six-module architecture stands.
- Measured total genuinely needs more → consolidation becomes the cheap alternative to paying 2.2–5×
  budget. The **three-module split** is pre-audited and has clean seams (`FamilySchemaModule`
  untouched / Gmail + EmailIngestion + EmailParsing / Digest + Query): no PState-ownership problem,
  no `define()`-last problem, no agent name collisions, because all 4 depots and 15 PStates live in
  `FamilySchemaModule` and the other five modules declare zero persistent state. Full assessment
  including its real costs (consolidated blast radius; losing AOR agent history for merged-away
  modules) is in `REASONING.md` (2026-07-29). **Do not start that refactor without the measurement.**

Also settled this session so it isn't re-litigated: **multiple modules cannot share a worker JVM.**
Verified at source (`terminology.html`: a Worker runs "part of **a module**") and at process level
(the worker daemon takes exactly one module name as an argv). There is no deploy-config shortcut to
fewer JVMs — merging code is the only route.

</details>

---

## Deferred — Multi-event extraction in `EmailParsingModule`

*(Was "Next Task" until 2026-07-29; deferred behind the RSS measurement run, not dropped. That run
completed 2026-07-30 — this now sits behind the load-testing session that succeeded it.)*

One email currently always yields
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
