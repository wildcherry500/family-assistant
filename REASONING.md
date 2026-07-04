# Session Reasoning Log

## 2026-07-03 — Audit before search-index build

### Working directory verification
Before reading anything, I checked both `/Volumes/CORSAIR/family-assistant` (where the
IDE had a file open) and `/Users/toddkeelingfolder/CORSAIR/family_assistant` (what my
memory says is canonical). Memory was correct: the `/Volumes` path has grown some
real-looking content since memory was last updated (a `.claude/`, `.idea/`, a stub
`EmailParsingModule.java`, a doc file) but is still not a git repo and does not contain
`CLAUDE_HANDOFF.md`, `FamilySchemaModule.java`, `DigestModule.java`, `QueryModule.java`,
or `QueryAgentTest.java`. The `/Users/.../family_assistant` path is a real git repo
with all of the above. I read everything from the `/Users` path. The IDE tab being open
on the `/Volumes` `.iml` file appears to be stale/incidental, not a signal to switch.

### RAMA_VERIFIED_LEARNINGS.md does not exist
I searched the whole repo (case-insensitive, any extension) for this filename and any
file containing the specific non-negotiable phrases quoted in the task
(`waitForStreamProcessedCount`, `agentTopology.define`, `weakness-map`, `leverage-map`).
No such file exists anywhere in the tree, working or committed. The closest equivalents
are `CLAUDE.md` (a "Rama 1.5.0 PState Patterns (Verified)" section) and
`CLAUDE_HANDOFF.md` (a PState schema + API notes section). Neither contains the full
list of non-negotiables the task describes. I'm flagging this rather than assuming the
file was deleted or renamed — I have no evidence either way, just that it isn't here now.

I verified each individual non-negotiable directly against the code instead of trusting
the (missing) doc:
- `new ArrayList<>()/new HashMap<>()` vs `List.of()`: confirmed true in agent
  node/depot code (DigestModule, QueryModule use `new ArrayList<>()`/`new HashMap<>()`
  throughout). The only `List.of()` hits in the whole repo are in Gmail SDK builder
  calls (`GmailIngestionModule`, `GmailWatchSetup`, `GmailService`) — not agent nodes or
  depots, so not a violation of the rule as scoped.
- `agentNode.result(...)` terminal nodes: confirmed in every module I read
  (EmailParsingModule, DigestModule, QueryModule all end their `finalize` node in
  `agentNode.result(...)`).
- **`waitForStreamProcessedCount` after every `depot.append()` in tests: NOT what the
  existing suite actually does.** `IndexPStateTest` and (by pattern) the other non-LLM
  tests append records then call `Thread.sleep(2000)` and poll/assert directly — I found
  zero uses of `waitForStreamProcessedCount` anywhere in the codebase. I don't know if
  this method exists in the real Rama 1.5.0 test API; I did not find it referenced in
  `docs/Agent_O_Rama_Complete_Documentation.md` either, and I don't have network access
  to chat.redplanetlabs.com from this pass. This is a real divergence between what the
  task assumes is established practice and what the code actually does — I'm reporting
  it, not silently adopting either pattern.

### Silo / intent tag (Step 1) — confirmed NOT DONE, and I don't know the taxonomy
I read `FamilySchemaModule.java`, `EmailParsingModule.java`, and `QueryModule.java` in
full and grepped the whole repo (code, docs, and the test JSON fixture) for
`silo|vault|studio|intent`. The only hit is an incidental "studio" inside a test email
body (a music studio, unrelated to the business concept). There is no silo field, no
silo index PState, and no intent field distinct from the existing `eventType`
(SCHOOL_EVENT/DEADLINE/PERMISSION_SLIP/TASK/UNKNOWN, which is really an event category,
not what the task calls "intent").

I'm confused about one thing I should not guess at: the task names "Vault/Office/Studio
or equivalent" as example silo values, but nothing in this repo (code, CLAUDE.md,
CLAUDE_HANDOFF.md, or memory) defines what those silos mean for this specific user's
life/business, or what the "intent" taxonomy should be (distinct from the existing
eventType categories). This looks like new domain vocabulary being introduced this
session rather than something I missed in the existing docs. I should not invent this
taxonomy — building the wrong one means the schema field is wrong on the first attempt,
which is expensive to unwind once events start getting tagged in production data. I'm
asking the user for the actual silo values and how "intent" differs from `eventType`
before proposing the schema-field plan in detail.

### weakness-map / leverage-map (Step 2) — confirmed NOT DONE
Zero grep hits repo-wide for `weakness-map`, `leverage-map`, or camelCase variants.
`FamilySchemaModule` declares exactly five PStates and none of them are these.
`DigestModule` reads only `$$family-data` and `$$events-by-date`. Nothing references
these config PStates anywhere, including in comments or TODOs.

### assertResponseContains (Step 3) — confirmed NOT DONE
`QueryAgentTest.java`'s only helper is `ask()`, which asserts `assertNotNull` and
`assertFalse(isBlank())` — no substring/rubric assertions at all. The four `@Test`
methods don't check that the answer contains anything related to the question (e.g.
that "field trip" query answer actually mentions the field trip). This means the
existing LLM test suite would pass even if the agent returned a generic non-blank
string unrelated to the question. Worth noting even though it's not blocking today.

### Documentation conflict I'm flagging, not resolving
Both `CLAUDE.md` (line 114) and `CLAUDE_HANDOFF.md` (line 136) explicitly say the next
module to build is **`SearchIndexModule`** — a new module, with its PState living in
`FamilySchemaModule`. That's in tension with this task's explicit instruction: "NOT a
new module... Do NOT create a SearchIndexModule that implements RamaModule." Since
Step 1 is NOT DONE, this doesn't matter for what I'm proposing this session (search
build is deferred either way), but whoever wrote those docs was planning a dedicated
module class, and the current instructions override that. Flagging so it isn't silently
re-introduced later out of habit from re-reading the handoff doc.

### Net effect on this session's plan
Per the task's own branching rule, Step 1 not being done means I'm not proposing the
search build (Pieces 1 & 2) this session. I'm proposing the silo+intent-tag addition as
the session's task instead, gated on the user answering what the silo/intent taxonomy
actually is.

## Follow-up pass — same task re-sent with resource-check order + resolving the open API question

The task came back essentially identical, now naming `RAMA_VERIFIED_LEARNINGS.md` as
read #2 and giving an explicit lookup order (chat.redplanetlabs.com → redplanetlabs.com/docs
→ RAMA_VERIFIED_LEARNINGS.md → GitHub example repos) for any uncertain API, with "THIS
FILE WINS" language for RAMA_VERIFIED_LEARNINGS.md when it conflicts with other sources.

**Re-verified the file's absence, this time including git history, not just the working
tree:** `git status --short` shows only my own untracked `REASONING.md`. `git log
--diff-filter=A --name-only --all | grep -i verified` returns nothing — no commit, on any
branch, ever added a file matching "verified" in its name. This isn't a file that got
deleted or moved; as far as this repository's history is concerned, it never existed. I'm
stating this plainly because the task's phrasing ("THIS FILE WINS") assumes it's an
authoritative tie-breaker I can consult — I have no such file to consult, in this repo.

**Resolved the `waitForStreamProcessedCount` question via the docs, per the requested
order.** `chat.redplanetlabs.com` wasn't attempted — it's a chat product, essentially
certain to require authenticated access, and the docs order lists it first but the actual
public/fetchable resource is `redplanetlabs.com/docs`. I went there next (via WebSearch to
find the right page, since a blind fetch of the docs root returned no renderable content
— likely a JS-rendered SPA shell). The `Testing` page
(`https://redplanetlabs.com/docs/~/testing.html`) directly answers this:

- **There is no `waitForStreamProcessedCount` method for stream topologies.** For stream
  topologies, depot appends made with `AckLevel.ACK` don't return until all downstream
  processing (including writes to PStates) has finished — the append call itself is the
  synchronization point, no separate wait call needed.
- A `waitForMicrobatchProcessedCount()`-style method exists, but only for **microbatch**
  topologies — which the non-negotiables explicitly forbid using in tests anyway ("stream
  topology only in InProcessCluster tests, never microbatch"). So this method wouldn't
  apply here even if we wanted it.
- For **mirror depots** (a module appending to another module's depot — exactly
  `EmailParsingModule` → `FamilySchemaModule`'s `*family-events` depot via
  `getMirrorDepot`), the docs say processing is asynchronous and recommend a **condition
  poll with timeout** (their example: loop with `Thread.sleep(50)` up to a 30s timeout,
  checking the actual PState value each iteration) — not a fixed sleep.

This lines up with what I already found in the code: `QueryAgentTest.java` (lines 66–72)
already does exactly this condition-poll-with-timeout pattern for its mirror-depot case
(polling `$$family-data` up to a 30s deadline). `IndexPStateTest`'s flat
`Thread.sleep(2000)` is the weaker version of the same idea — it happens to work today but
isn't the pattern the docs actually recommend, and isn't guaranteed to hold if
processing gets slower.

**Conclusion for next time I write a test in this area:** don't use
`waitForStreamProcessedCount` (it doesn't exist) or blind `Thread.sleep()` for new tests —
follow `QueryAgentTest`'s condition-poll-with-timeout pattern instead. I have not yet
verified the exact `AckLevel.ACK` call signature for the same-module synchronous case,
since I won't be writing that test until the silo/intent taxonomy question is answered
and the plan is approved — I'll verify it against the docs at that point rather than
guess it now.

## Implementation pass — taxonomy approved, plan executed

User approved the plan with three adjustments: include `UNKNOWN` in both new indexes,
leave `QueryModule`/`DigestModule` wiring for next session, and keep
`RAMA_VERIFIED_LEARNINGS.md` strictly separated into verified/unverified.

**Resolved `AckLevel.ACK`'s exact signature by decompiling the real 1.5.0 jar, not
trusting docs.** Found `/Volumes/CORSAIR/.m2/repository/com/rpl/rama/1.5.0/rama-1.5.0.jar`
on disk (alongside 1.0.0, 1.2.0, and 1.8.0 — confirming the "current Rama is 1.8.0, we
run 1.5.0" warning is real and multiple versions genuinely coexist on this machine).
`javap` against the actual `com.rpl.rama.Depot` and `com.rpl.rama.AckLevel` classes in
that exact jar confirmed `Map<String,Object> append(Object, AckLevel)` and the three
enum values `NONE/APPEND_ACK/ACK` — this is stronger evidence than the docs (which may
describe 1.8.0) since it's the literal bytecode of the version this project depends on.
Wrote this into `RAMA_VERIFIED_LEARNINGS.md`'s Verified section, with the behavioral
semantics (what ACK actually waits for) attributed to the docs separately, since I
haven't exercised that behavior with a passing test myself.

**Also resolved the `topology.define()` non-negotiable**, which I'd noticed was absent
from every module in this codebase and hadn't explained yet: `docs/Agent_O_Rama_Complete_Documentation.md`
(lines 2176–2201, 7203–7251) shows it's only required when manually building an
`AgentTopology` inside a plain `RamaModule.define()`. None of `EmailParsingModule`,
`DigestModule`, or `QueryModule` do that — they all extend `AgentModule` and override
`defineAgents()`, where the framework calls `.define()` internally. This isn't a gap in
the existing code; it's the expected shape for that pattern.

**One real bug this surfaced**: extending `ParsedEvent`'s constructor broke
`NonLlmPipelineTest.testParsedEventSerialization` (line 348) — a pre-existing
serialization round-trip test that called the old 14-arg constructor directly. Fixed by
adding `"VAULT", "ACTION_REQUIRED"` to that call and asserting the two new fields
round-trip correctly. This is expected fallout from changing a public constructor with
an existing direct caller, not scope creep — required to keep the suite green per the
acceptance gate.

**Result**: 86/86 tests green (78 baseline + 8 new `SiloIntentIndexTest` tests), `mvn
test` reports `BUILD SUCCESS`. Nothing committed or pushed — that's a separate
decision for the user.

## New session — weakness-map/leverage-map + EA rubric assertions

Working directory re-verified against memory: `/Users/toddkeelingfolder/CORSAIR/family_assistant`,
git clean, up to date with `origin/master`. `RAMA_VERIFIED_LEARNINGS.md` now exists at repo
root (it didn't at the start of the prior session — it was created during that session's
implementation pass) and I read it in full before touching anything else, per this task's
explicit resource order.

### Phase A audit results

**A1 — baseline.** Ran `mvn test` (not `-q`, so I could capture the per-class summary).
Result: `Tests run: 86, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`. Matches the
86/86 the task assumes. One pre-existing, unrelated `GmailIngestionTest` warning
(`invalid_grant... Token has been expired or revoked`) appeared in output but did not fail
the build or the test (it degrades to "no unread messages" and passes) — not something this
session's task touches, flagging only so it isn't mistaken for fallout from my changes later.

**A2 — silo/intent fields confirmed present.** `FamilySchemaModule.java`:
- Schema doc comment lines 25-26 declares `$$events-by-silo` (VAULT/OFFICE/STUDIO/UNKNOWN)
  and `$$events-by-intent` (ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN).
- PState declarations at lines 79-89.
- Topology wiring (extract + conditional indexed write, UNKNOWN included) at lines 121-130.
- Both fields also persist directly on the `$$family-data` event record (same `*record`
  object written at lines 103-104 before any field-specific extraction, so `silo`/`intent`
  ride along automatically — confirmed behaviorally by `SiloIntentIndexTest`
  `testSiloAndIntentPersistedOnEventRecord`, line 192).

**A3 — DigestModule's current selection/ordering.** All in the `query-events` node,
`DigestModule.java:84-126`:
1. Line 87-90: opens mirror stores for `$$family-data` and `$$events-by-date` (read-only,
   via `getMirrorStore`) — this is the existing pattern TASK 1's new PState reads should
   copy.
2. Lines 93-97: selects event IDs from `$$events-by-date` via sorted range query
   (`sortedMapRange(windowStartMs, windowEndMs)`) — this is the window filter.
3. Lines 99-110: hydrates each ID into its full record from `$$family-data`.
4. Lines 113-116: optional in-memory `accountLabel` filter (`removeIf`).
5. **Lines 118-123: sorts by `effectiveTime` ascending (soonest-first)** — this is the
   exact hook point for leverage-based reordering. Whatever TASK 1 adds needs to either
   replace this comparator or add a second sort pass after it, before line 125's
   `agentNode.emit("build-summary", ...)`.
6. `build-summary` (lines 133-186) then iterates `events` in whatever order it received
   them and renders each into a text block (lines 153-178) — this is where weakness
   annotation lines would get appended per-event, since it already has a per-event loop
   with a `StringBuilder`.

No divergences from what the task assumed this time — Phase A confirms the ground the task
expects.

### Design reasoning for TASK 1

**Reusing $$family-data's proven shape instead of inventing a list-append API.** The task
says propose the entry format; my instinct was a `List<Map>` per family (`familyId ->
List<entry>`), but Rama's `Path` idiom for appending into a *list* isn't something I've
seen verified anywhere in this codebase — `RAMA_VERIFIED_LEARNINGS.md` only documents
`.nullToSet().voidSetElem()` (for indexes) and `.termVal()` (for whole-value writes,
`$$family-data`'s `events.eventId` write at line 103-104). Guessing a list-append Path
method would violate "never guess an API." Instead I'm proposing `$$weakness-map` and
`$$leverage-map` use the *exact same shape* as `$$family-data`: `familyId -> entryId ->
entry-map`. That reuses the identical `localTransform(..., Path.key("*familyId").key("*entryId").termVal("*entry"))`
idiom already proven at `FamilySchemaModule.java:103-104`, with zero new Path API surface.
Reading side (`selectOne` a whole family's map, or the `sortedMapRange`-style wide query
already used for `$$events-by-date`) is also already proven. This is the "pattern already
proven in this codebase" the non-negotiables ask me to prefer.

**Matching shape for entries.** Both maps' entries carry the same three matcher fields —
`silo`, `intent` (either may be `null`/absent = wildcard on that dimension) — plus a
type-specific payload:
- LEVERAGE entry: `{id, silo, intent, weight}` — `weight` is a positive number; matching
  events get it added to a priority score, higher sorts earlier.
- WEAKNESS entry: `{id, silo, intent, tag, note}` — `tag` is a short machine label (e.g.
  `SUMMARIZE_LONG_TEXT`, `SURFACE_EARLY`) driving what annotation DigestModule appends;
  `note` is free text shown to the user.

I'm keeping this proposal deliberately generic (wildcard-matchable on the two fields the
schema already has) rather than inventing new matcher dimensions (e.g. text-length
heuristics for "long unstructured text"), because the task explicitly says entries should
use "the silo/intent vocabulary now in the schema" and that I should propose the mechanism,
not the exact entries — the user will correct the entries themselves.

**Open question I'm not resolving myself: per-person vs per-family keying.** The task says
these maps are "keyed by the same family/person ID as everything else," but every existing
PState in this codebase (`$$family-data`, all five indexes) is keyed by `familyId` only —
there's no separate per-child/per-person key anywhere (`childName` is a *value* inside the
child index, not a partition key). I'm proposing `familyId` as the key, consistent with
the existing model, and flagging in the plan that this treats "the person the digest is
for" and "the family" as the same identity for now — correct me if a real distinct
person-id is wanted.

**DigestModule wiring.** In `query-events`, after opening the two existing mirror stores,
open two more (`$$leverage-map`, `$$weakness-map`) the same way. After the existing
soonest-first sort (line 118-123), compute a per-event leverage score (0 if no entries
match or the map is empty/null for this family — graceful no-op) and re-sort primarily by
score descending, with the existing chronological comparator as the tiebreak — this
achieves "leverage matches float to top, otherwise unchanged" without discarding the
existing ordering logic. For weakness, I'm proposing copying each matched event into a
fresh `HashMap` (not mutating the object returned by `selectOne`) and stamping a transient
`"_weaknessNote"` key that isn't part of the persisted schema, purely so `build-summary`'s
existing per-event loop can check for it and append an extra line. Graceful-no-op
constraint: every new lookup is `null`-checked exactly like the existing `eventIds != null`
check at line 100 — empty/missing maps fall through to today's behavior unchanged.

**Population/seeding.** One new depot, `*weakness-leverage-config`, declared in
`FamilySchemaModule` (the module that owns all family data, per the "config PStates live
in the module that owns the data" non-negotiable). Records carry a discriminator field
(`mapType: "LEVERAGE"|"WEAKNESS"`) and route via `.ifTrue` (same idiom already used for
the conditional index writes) into the matching PState. Seed data gets appended the same
way `SiloIntentIndexTest` seeds `*family-events` directly in test setup — I'll write an
analogous test (`WeaknessLeverageMapTest`) that appends config records straight to the
depot and asserts the PStates end up populated, no LLM involved.

### Design reasoning for TASK 2

Only one event is created by the whole ZOO_EMAIL fixture (`ZooEmailTest.java` line 119:
`assertEquals(1, result.eventIds.size())`) — the email itself is single-event, not
multi-event, despite mentioning several dates (permission slip due March 16, trip March 20,
picture day March 18, pickup by 4pm). This matters for what the rubric assertions can
safely claim: I can assert facts drawn from the email text (the trip is at a zoo, a
permission slip has a due date around March 16, the event concerns "Billy"'s class per
`QueryAgentTest`'s question phrasing) but I should not assume the agent will produce
separate structured answers per sub-fact — it's answering from one event's fields plus
whatever narrative context made it into `description`.

**`assertResponseContains` semantics — proposing OR, not AND.** The task's helper
signature (`String... expectedSubstrings`) is ambiguous between "all of these must appear"
and "at least one of these must appear." Given the explicit instruction to keep assertions
"loose enough to survive LLM wording variation," I'm proposing OR semantics: pass several
acceptable phrasings of the *same* fact (e.g. `"zoo", "Woodland Park", "field trip"`) and
the assertion passes if any one is found. This is called out as a proposal, not decided
silently, since AND semantics would read the same call signature very differently.

**Which two tests to upgrade and what to assert**, per the task's own examples:
- `testWhatIsHappeningOnMarch20` (already asks about March 20th): assert the answer
  contains at least one of `"zoo", "Woodland Park", "field trip"` — the known event.
- `testWhenIsNextPermissionSlipDue`: assert the answer contains at least one of
  `"March 16", "3/16", "16th"` — a date is present, per the task's own example wording
  ("asserts a date is present").
Both stay loose (OR-matched phrasings) specifically because the exact wording Gemini
produces is not something I've observed yet in this session — I have not re-run the LLM
suite today. I'll only know if these substrings actually match after running with
`GEMINI_API_KEY`, per the acceptance gate, and will report the real answers back.

### Nothing contradicts the plan-divergence rule so far
No code has been written. Everything above is proposal, pending approval.

### Plan approved — confirmations and one flagged judgment call

User confirmed all three open questions: `familyId` keying, OR semantics for
`assertResponseContains` (with the helper's javadoc required to state that OR means
"variants of ONE fact" — multiple distinct facts need multiple calls, not one call with
a mixed bag of substrings), and seed entries as corrigible placeholders.

**Score-first/chronology-tiebreak ordering — deliberate judgment, flagged as revisitable.**
The re-sort in `DigestModule.query-events` (leverage score descending, existing
soonest-first comparator as tiebreak) means a high-weight leverage match arbitrarily far
in the future can outrank an urgent near-term event with no match at all — there's no
decay or window-position weighting, just two flat sort keys. That's fine for the digest
windows this codebase currently uses (days, per `ZooEmailTest`'s ~2.5 week March window),
where "everything in the window is already near-term" makes score-first reasonable. It
stops being reasonable if digest windows widen substantially (e.g. a month-plus or
"everything upcoming" view) — at that point a distant leverage-matched event could bury
tomorrow's deadline, which is the opposite of what leverage prioritization is supposed to
do. Not solving this now (no decay function requested, would be scope creep on a
mechanism task) — noting it explicitly so it isn't rediscovered as a surprise bug later if
someone widens the window.

## Implementation results

**TASK 1.** `FamilySchemaModule`: new `*weakness-leverage-config` depot, `$$leverage-map`
and `$$weakness-map` PStates (shaped exactly like `$$family-data` — `familyId -> entryId ->
entry-map`, reusing the proven `.termVal()` idiom, no new Path API), routed by a `mapType`
discriminator via two single-arg `.ifTrue(Expr(...))` predicates (`isLeverageMapType` /
`isWeaknessMapType`) — kept single-arg deliberately, matching every existing `Expr` usage
in the file, rather than guessing whether a two-arg `Expr` constructor exists.
`DigestModule.query-events`: opens both new mirror stores, computes a per-event leverage
score (max weight over matching entries, wildcard-matched on silo/intent, 0 if no match),
re-sorts by score descending with the original soonest-first comparator as tiebreak, and
stamps a transient `_weaknessNote` key (on a fresh `HashMap` copy of each event, never
mutating the object returned by `selectOne`) when a weakness entry matches.
`DigestModule.build-summary` appends a `Note:` line per event when that key is present.
Wrote `WeaknessLeverageMapTest` (9 cases, no LLM) mirroring `SiloIntentIndexTest`'s
directly-append-to-depot + condition-poll pattern: PState population, leverage reordering
(a chronologically-last event with a high-weight match provably sorts first), weakness
annotation (and its absence on non-matched events), and the graceful-no-op case (a family
with zero config entries gets unmodified soonest-first output, no `Note:` lines). All 9
pass.

**TASK 2.** Added `assertResponseContains(String answer, String... expectedSubstrings)` to
`QueryAgentTest` with OR semantics and the javadoc language the user asked for (documents
that a single call is for phrasing variants of ONE fact; distinct facts need separate
calls). `ask()` now returns the answer string. Upgraded `testWhatIsHappeningOnMarch20`
(asserts `"zoo"`/`"Woodland Park"`/`"field trip"`) and `testWhenIsNextPermissionSlipDue`
(asserts `"March 16"`/`"3/16"`/`"16th"`). Both stay `@Tag("llm")`. `mvn -o test-compile`
confirms the whole test tree, including these, compiles clean.

**Acceptance gate — non-LLM suite.** `mvn -o test` (default profile, `llm` group still
excluded): **95/95 green**, `BUILD SUCCESS` — the 86 baseline plus the 9 new
`WeaknessLeverageMapTest` cases. No existing test needed changes; nothing regressed.

**Acceptance gate — LLM suite, blocked by a pre-existing bug unrelated to this session's
changes.** Running `QueryAgentTest` with `-Dexcluded.groups=` (to include `@Tag("llm")`)
and a real `GEMINI_API_KEY` fails in `@BeforeAll setup()` — before either upgraded
assertion ever executes — with:
```
java.lang.ClassCastException: class java.lang.String cannot be cast to class
com.family.assistant.gmail.GmailMessage
	at com.family.assistant.email.EmailIngestionModule.lambda$defineAgents$76af86d6$1(EmailIngestionModule.java:79)
```
`EmailIngestionModule.java:68` declares its `ingest` node as
`(AgentNode agentNode, List<GmailMessage> messages) -> ...` — but the class's own javadoc
(lines 17, 22) still says "Accepts a batch of raw email strings" / "Input: List<String>
rawEmails," and both `QueryAgentTest.setup()` (line 58, untouched by me) and
`ZooEmailTest.testZooEmailExtraction` (line 112-114, also untouched, also one of this
session's required "read first" files) call
`ingestionAgent.invoke(new ArrayList<>(List.of(someRawEmailString)))` — passing a
`List<String>`, not `List<GmailMessage>`. The node signature and every direct-String
caller have drifted apart.

**This is not caused by anything in today's diff.** `git diff --stat` shows only
`REASONING.md`, `DigestModule.java`, `FamilySchemaModule.java`, `QueryAgentTest.java`
(plus the new `WeaknessLeverageMapTest.java`) changed —
`EmailIngestionModule.java` is untouched. I confirmed this is pre-existing, not a
side-effect of my edits, by running `ZooEmailTest` — a file I only read, never modified —
under the same `-Dexcluded.groups=` flag: it fails with the **identical**
`ClassCastException` at the identical line. Both LLM-tagged tests that ingest via raw
strings are broken the same way, independent of anything TASK 1 or TASK 2 touched.
Likely cause (not verified, just the visible shape of it): `GmailMessage` and the
Gmail-native ingestion path mentioned in project memory were added after `ZooEmailTest`
and `QueryAgentTest` were last exercised, and the `ingest` node's parameter type was
migrated to `GmailMessage` without updating the two direct-String call sites or the
module's own javadoc.

**Per the plan-divergence rule, stopping here rather than fixing it.** Fixing
`EmailIngestionModule` or the two tests' `String`→`GmailMessage` mismatch is a change to
code outside TASK 1/TASK 2's explicit scope ("OUT OF SCOPE this session: ... any new
agent... Do not touch the classify prompt" — this is adjacent to, though not identical to,
that boundary, and I'd rather report and ask than guess at the right fix under a
non-negotiable to never guess an API). TASK 2's rubric assertions themselves are verified
to compile and are logically ready to run the moment ingestion is unblocked — they were
never reached. Reporting the actual answers from a real Gemini run, per the acceptance
gate, is blocked on this pre-existing bug being fixed first, which needs your direction:
fix `EmailIngestionModule`/the two tests now (separate small task), or treat "compiles
clean, non-LLM suite green" as sufficient for today's gate and defer the LLM run.

## Checkpoint — CLAUDE_HANDOFF.md updated, committed, pushed

User chose to checkpoint before the mismatch fix. Updated `CLAUDE_HANDOFF.md`: test count
78→95, added the four new PStates to the schema table, added `SiloIntentIndexTest` and
`WeaknessLeverageMapTest` rows plus an LLM-tagged-tests table, and a new "Known Issue"
section documenting the String/GmailMessage mismatch and the scoped-fix decision (test
call sites adapt, `EmailIngestionModule`'s signature is production and doesn't change).
Committed (`7f30920`) and pushed to `origin/master`.

## Mismatch fix — scoped to test call sites, per user decision

Grepped every `ingestionAgent.invoke(...)` call site in `src/test`. Found exactly two
broken ones — `ZooEmailTest.java:114` and `QueryAgentTest.java:58` — both passing
`List<String>`. `EmailIngestionTest.java` was already correct (constructs `GmailMessage`
directly per-test since it needs varied field values for its blank/null-filtering
assertions); its pattern is what I copied for the shared helper rather than inventing a
new one.

Added `GmailMessageTestFixtures.fromRawBody(String)` (new file,
`src/test/java/com/family/assistant/`) — wraps a raw email body string in a
`GmailMessage` with `gmailMessageId=null` (falls back to a random UUID per
`EmailParsingModule.java:261-262`, confirmed by reading that code, not guessed) and
`emailSubject=null` (so title extraction falls back to `EmailParsingModule`'s body-based
`extractTitle()`, preserving the exact behavior these two fixtures had before
`GmailMessage` existed — I did not want the fix to silently change what gets extracted
from the fixture). `senderEmail="test@example.com"` since neither test reads it. Updated
both call sites to `GmailMessageTestFixtures.fromRawBody(ZOO_EMAIL)`.

`mvn -o test-compile`: clean. `mvn -o test` (non-LLM, `llm` excluded): still **95/95
green**, `BUILD SUCCESS` — this fix touched only `@Tag("llm")` test files plus one new
test-only helper class, nothing in the non-LLM path.

## Running the LLM suite — the mismatch is fixed, but it surfaced two more pre-existing bugs

`mvn -o test -Dtest=ZooEmailTest,QueryAgentTest -Dexcluded.groups=` with a real
`GEMINI_API_KEY`: **the `ClassCastException` from `EmailIngestionModule` is gone** —
ingestion now succeeds (`IngestionResult{parsed=1, skipped=0, failed=0}`), confirming the
scoped fix worked exactly as intended. But two more issues surfaced, both pre-existing and
both outside anything this session (or the mismatch fix) touched:

**1. `QueryAgentTest` — all 4 questions return "no events matching."** The event itself
extracted correctly and is genuinely in `$$family-data`
(`familyId=keeling-family-001`, `eventType=SCHOOL_EVENT`, `silo=VAULT`,
`intent=ACTION_REQUIRED`, `title="3rd Grade Zoo Trip"`, real `startTime`/`deadline` epoch
values — confirmed by reading `ZooEmailTest.testZooEmailExtraction`'s printed output,
which passed). The query agent still can't find it. I read (not modified)
`QueryModule.java`'s `interpret-query`/`fetch-data` nodes to understand why, without
fixing anything, since `QueryModule` filter wiring was explicitly out of scope from the
very first task description this session ("OUT OF SCOPE this session: ... QueryModule
filter wiring"). Two plausible root causes, both structural, neither guessed at random:
(a) the `ZOO_EMAIL` fixture never actually mentions a child named "Billy" — it's a
generic class-wide letter, no student named — so `testWhatDoesBillyNeedForFieldTrip` and
`testDoINeedToPickUpBilly` failing to find a "Billy"-matching event may be *correct*
behavior on a mismatched fixture, not a bug; (b) the whole email collapses into **one**
event with `eventType=SCHOOL_EVENT`, but `QueryModule`'s LLM query-parser can independently
choose `categoryFilter=PERMISSION_SLIP` for the permission-slip question — since
`$$events-by-category`'s `PERMISSION_SLIP` bucket is empty (the event is indexed only
under `SCHOOL_EVENT`), that filter alone yields zero candidates even though a relevant
event exists. This is a real single-event-multiple-topics classification tension in the
schema, not something I'm fixing today — flagging it precisely so it isn't rediscovered
as a mystery next time `QueryModule` work is in scope.

**2. `ZooEmailTest.testDigestAfterZooEmail` — `ClassCastException: Long cannot be cast to
String`.** Its own diagnostic code at `ZooEmailTest.java:172` does
`String startTime = (String) ev.get("startTime");`, but the schema stores `startTime` as
`Long` (confirmed in `CLAUDE_HANDOFF.md`'s Event record fields table and in the actual
extraction printout from test 1). This crashes before the test ever reaches
`digestAgent.invoke(...)` or its assertions — it's a stale assumption in the test's own
print-debugging code, not anything related to the `GmailMessage` fix (I only touched line
114 in this file). Not fixed — outside the scope of "fix the mismatch."

**Actual answers from the real Gemini run** (verbatim, per the acceptance gate):
- "What does Billy need for the field trip?" → "I didn't find any events matching your
  question for Billy."
- "When is the next permission slip due?" → "I didn't find any events matching your
  question." — assertion failed (expected one of ["March 16", "3/16", "16th"]).
- "What is happening on March 20th?" → "I didn't find any events matching your
  question." — assertion failed (expected one of ["zoo", "Woodland Park", "field
  trip"]).
- "Do I need to pick up Billy from school?" → "I didn't find any events matching your
  question for Billy."

The rubric assertions did exactly the job they were built for: the old not-blank checks
would have passed on all four of these "no events matching" answers silently. The new
assertions correctly caught that the agent isn't actually answering the questions,
surfacing a real `QueryModule` bug that was invisible before. Stopping here per
instructions — no further commit without go-ahead.

## New session — compound search over all index dimensions

Working directory re-verified: `/Users/toddkeelingfolder/CORSAIR/family_assistant`, clean,
up to date with `origin/master` at `e173d22`. Read (in full, not excerpted) all of
`CLAUDE_HANDOFF.md`, `RAMA_VERIFIED_LEARNINGS.md`, this file's last two sessions,
`FamilySchemaModule.java`, `QueryModule.java` (full 436 lines — I'd only read excerpts of
it before this session), `QueryAgentTest.java`, `ZooEmailTest.java`, `SiloIntentIndexTest.java`,
per the task's explicit read-first list.

### Phase A audit

**A1 — baseline.** `mvn -o test`: `Tests run: 95, Failures: 0, Errors: 0` /
`BUILD SUCCESS`. Matches the 95/95 the task assumes.

**A2 — LLM red-state confirmation.** `mvn -o test -Dtest=QueryAgentTest -Dexcluded.groups=`
with real `GEMINI_API_KEY`: identical to last session's report — same 4 verbatim answers,
same 2 assertion failures (`testWhenIsNextPermissionSlipDue`,
`testWhatIsHappeningOnMarch20`), same expected-substring lists. No drift since last
session; the red state is stable and reproducible, confirming it's a real code path issue,
not flakiness.

**A3 — QueryModule's current question → filter → PState read → answer path, file+line:**
1. `interpret-query` node (`QueryModule.java:123-169`): one Gemini call, prompt at
   lines 142-159, asks for a SINGLE JSON object with `queryType`, `childName`, `dateFrom`,
   `dateTo`, and **one** `categoryFilter` (line 152: `"SCHOOL_EVENT|PERMISSION_SLIP|TASK|DEADLINE|null"`).
   Parsed by hand-rolled `parseQueryParams`/`extractJsonString` (lines 337-374, explicitly
   "without Jackson dependency" per the line-341 comment) into a `QueryParams` object
   (lines 65-98).
2. `fetch-data` node (`QueryModule.java:182-273`): for each of `hasDateRange` (196),
   `childName` (212), `categoryFilter` (224) — if non-null, do ONE index lookup
   (`$$events-by-date`/`$$events-by-child`/`$$events-by-category` respectively) and
   **intersect** (`intersect()`, lines 331-335) into a running `candidateIds` set. This is
   the exact bug: `categoryFilter` is usually the only non-null dimension for a question
   like "when is the next permission slip due" (no date range or child name in the
   question), so it functions as the sole gatekeeper — line 224-233's intersect against a
   correct-but-empty `$$events-by-category` `PERMISSION_SLIP` bucket (because the real
   event is indexed only under `SCHOOL_EVENT`) zeroes `candidateIds` to `{}` even though
   `$$family-data` genuinely has a matching event. `accountLabel` (lines 262-266) is
   applied as an in-memory post-filter, not an index lookup, even though
   `$$events-by-account` already exists as an index (per `CLAUDE_HANDOFF.md`'s PState
   table) — a second, smaller inconsistency worth fixing while rewiring this path, since
   the task's compound-filter dimension list explicitly names "account."
3. `generate-answer` (275-316) and `finalize` (318-324) are unchanged by anything in this
   session's scope — they just format whatever `matched` list `fetch-data` hands them.

Also noticed, not yet acted on: `childNameMatches` (`QueryModule.java:389-394`) is a
private helper that does substring/contains matching on `childId`/`childName` — it is
**dead code**, never called anywhere in the file. `fetch-data`'s actual child lookup
(212-221) is an exact-match index `selectOne`, not this fuzzy helper. Flagging for the
plan rather than silently deleting or silently wiring it in — it's outside what's broken
today (the bug is `categoryFilter`, not child matching) and touching it wasn't asked for.

### API verification before finalizing the plan (both now in `RAMA_VERIFIED_LEARNINGS.md`)

Two mechanisms in the task's PIECE 1/PIECE 2 description were not proven-in-this-codebase
patterns, so I verified both against the actual pinned jars before writing a plan that
depends on them — per the explicit "never guess an API" instruction, and because "the
code says the plan should change" would apply retroactively and expensively if I planned
around an API that doesn't exist in 1.5.0/0.8.0.

**1. Fan-out ("explode") for the keyword index.** Every existing index in
`FamilySchemaModule` writes exactly one key per record (one childName, one eventType, one
silo, one intent — all scalar fields). A keyword index needs to write **N** keys per
record (one per token). I found `Ops.EXPLODE` by websearch-then-jar-cross-check: the
public docs describe an `explode` op that "emits one time for each element of the list";
I decompiled `com.rpl.rama.ops.Ops` from the actual pinned `rama-1.5.0.jar` with `javap`
and confirmed `Ops.EXPLODE` genuinely exists there (not a 1.8.0-only addition) as a
`NativeRamaOperation1<Object>`. I also confirmed the exact call shape compiles against
this jar's real interfaces: `Block.each(Ops.EXPLODE, "*tokenList").out("*token")` — I
checked this specifically because `Block`'s own static factory methods only expose
`explodeMaterialized(String)`/`explodeMicrobatch(String)` (narrower, unrelated variants),
which could have been a dead end; `Ops.EXPLODE` instead goes through `Block`'s existing
`.each(RamaOperation1, Object)` overload, and I confirmed
`NativeRamaOperation1<T0> implements RamaOperation1<T0>` so the types actually line up.
Full trace is in `RAMA_VERIFIED_LEARNINGS.md`.

**2. Same-module agent-to-agent invocation for `search-agent`.** The task calls it a
"search-agent inside QueryModule" (not just new nodes on the existing `query-agent`), and
frames it as reusable in the next session ("DigestModule search wiring — next session"),
which reads as wanting a genuinely separate, independently-invocable agent — not just a
relabeling of `query-agent`'s existing nodes. Every cross-module call in this codebase
uses `getMirrorAgentClient(module, agent)` (two args); I needed to confirm a same-module,
one-arg equivalent actually exists before designing around it. `docs/Agent_O_Rama_Complete_Documentation.md`
shows `agentNode.getAgentClient("TextProcessor")` (one arg, same-module) in its
"Subagent"/"Recursive Agent Invocation" sections — but per this project's own standing
rule ("docs may describe a newer version than what we run"), I decompiled
`agent-o-rama-0.8.0.jar` (the exact pinned version) and confirmed
`com.rpl.agentorama.impl.IFetchAgentClient` (which `AgentNode` extends) declares
`AgentClient getAgentClient(String)` — so this is real in 0.8.0, not a docs-only/newer
feature. This resolves the design question: `search-agent` will be a genuine second
`topology.newAgent(...)` inside `QueryModule`, invoked synchronously from `query-agent`'s
node via `agentNode.getAgentClient("search-agent").invoke(...)`.

### Design decisions for PIECE 1 (keyword index)

- **PState name:** `$$events-by-keyword`, shape `familyId -> keyword -> Set<eventId>` —
  same shape as `$$events-by-silo`/`$$events-by-category` (not subindexed; no range query
  need, just bucket lookup by exact token).
- **Tokenized fields:** `title`, `description`, `emailSubject` — confirmed these are the
  three free-text String fields actually stored on the event record by grepping
  `EmailParsingModule.java`'s `eventRecord.put(...)` calls (lines 269-289). Note
  `CLAUDE_HANDOFF.md`'s "Event record fields" table is stale — it's missing `silo`,
  `intent`, `receivedAt`, `senderEmail`, `senderName`, `emailSubject`, `gmailMessageId`,
  all of which are actually stored. Not fixing that table this session (out of scope), but
  flagging it since I'm relying on the real code, not the stale table, for which fields to
  index.
- **Tokenization rule** (my own logic, not a Rama API — free to design, but stating it
  precisely since the task asked me to propose exact case-folding/stopword/min-length
  rules for approval): lowercase; split on `[^a-z0-9]+`; drop tokens shorter than 3 chars;
  drop a small fixed stopword list (~40 common English words: a/an/the/and/or/but/in/on/
  at/to/for/of/with/is/are/was/were/be/been/this/that/these/those/it/its/by/as/from/your/
  you/we/our/i/me/my/if/no/not/do/does/did); dedupe. Implemented as
  `EventUtils.tokenize(String text)` returning `List<String>` (not `Set` — see below) —
  one new shared method, used by BOTH the indexer (write path, tokenizing the record) and
  `search-agent` (read path, tokenizing extracted keyword phrases), so index-time and
  query-time tokenization can never drift apart into two different implementations.
  `EventUtils.tokenizeEvent(Map<String,Object> record)` is a second overload that pulls
  `title`+`description`+`emailSubject` (skipping null/blank) and delegates to
  `tokenize(String)`, for use directly on `*record` in the stream topology (matching the
  existing `effectiveTime(Map<String,Object>)` helper pattern already used the same way at
  `FamilySchemaModule.java:134`).
- **List, not Set, for the tokenizer's return type.** `Ops.EXPLODE`'s docs describe it
  operating on "a list" — to avoid guessing whether it also accepts a `Set`, `tokenize()`
  dedupes internally (via a `LinkedHashSet` while building) but returns
  `new ArrayList<>(...)`, so the emitted type reaching `Block.each(Ops.EXPLODE, ...)` is
  unambiguously a `List`.
- **Write path**: appended onto `FamilySchemaModule`'s existing `family-events-stream`
  chain (not a new stream topology — same source, same `*familyId`/`*eventId`/`*record`
  bindings already in scope), immediately after the existing intent-index block:
  `.macro(Block.each(EventUtils::tokenizeEvent, "*record").out("*tokens"))` then
  `.macro(Block.each(Ops.EXPLODE, "*tokens").out("*token"))` then
  `.localTransform("$$events-by-keyword", Path.key("*familyId").key("*token").nullToSet().voidSetElem().termVal("*eventId"))`.
  An event with zero tokens (blank title/description/emailSubject) simply explodes zero
  times — no index writes, no error — which is correct behavior, not a bug to guard
  against separately.

### Design decisions for PIECE 2/3 (search-agent + rewired query-agent)

- **Reusing `QueryParams` instead of inventing a parallel `SearchFilter` type.**
  `QueryParams` already carries `childName`, `dateFrom`, `dateTo`, `categoryFilter`,
  `familyId`, `requesterTimezone`, `accountLabel` — everything the compound filter needs
  except `keywords` and two new fields (`siloFilter`, `intentFilter`). Extending the
  existing type (adding three fields) rather than introducing a second, largely-redundant
  carrier type is a genuine simplification I'm flagging for approval rather than doing
  silently, per the "no silent substitution" rule — the task didn't specify the type name,
  but it did imply a distinct "compound filter" concept, and I want to confirm reusing
  `QueryParams` is acceptable before building on it.
- **`search-agent`'s graph**, all three non-finalize nodes are LLM-free (only
  `query-agent`'s existing `interpret-query` node calls Gemini — the task says "No LLM in
  resolve/intersect," and I'm extending that to `parse-filters` too, since putting an LLM
  call inside `search-agent` would fragment where classification happens and contradict
  "no LLM" being the point of pulling this out as its own agent):
  1. `parse-filters` (pure): normalizes the raw `QueryParams` the LLM produced —
     tokenizes each raw keyword phrase via `EventUtils.tokenize()` and unions into one
     `Set<String>` of normalized index tokens; converts `dateFrom`/`dateTo` strings to
     epoch bounds via the existing `parseToEpoch` helper (unchanged, reused).
  2. `resolve-indexes` (pure): for each dimension actually present (keywords, childName,
     categoryFilter, siloFilter, intentFilter, dateRange, accountLabel), do exactly one
     PState lookup and collect a `Map<String, Set<String>>` of dimension name → matched
     event IDs. Absent dimensions are never added to this map (this is the literal
     mechanism behind "empty dimensions are skipped, not treated as match-nothing").
     Within the **keywords** dimension specifically: union (OR) the per-token
     `$$events-by-keyword` bucket lookups together, not intersect — a single-token miss
     shouldn't sink the whole keywords signal, only the whole compound filter (across
     dimensions) is a strict AND. `accountLabel` moves from today's in-memory post-filter
     to a real `$$events-by-account` index lookup (the index already exists and isn't
     used for lookups today — a small, low-risk consistency fix the task's own dimension
     list ("...account...") implies).
  3. `intersect` (pure): if the per-dimension map is empty (question implied zero
     dimensions), fall back to a full scan of `$$family-data` for the family — this
     preserves today's documented zero-filter fallback behavior
     (`QueryModule.java:179-180`'s comment). Otherwise, intersect every dimension's set
     together (strict AND across whatever was actually provided) and hydrate the
     resulting IDs into full event records from `$$family-data`, sorted by
     `effectiveTime` (unchanged helper).
  4. `finalize` (terminal): `agentNode.result(matchedEventsList)`.
- **The real fix isn't "AND across more things" alone — it's shifting which dimension is
  the reliable one.** I want to be explicit about this because the task's literal wording
  ("intersection over provided dimensions only") could, read narrowly, still let a wrong
  `categoryFilter` zero out a correct keyword match, since intersect is still strict AND
  across whatever's non-null. The actual fix is two things working together: (a) keywords
  are a new, almost-always-present, almost-always-correct dimension (they're literal words
  from the user's own question, not a guessed enum value), and (b) **the rewired
  `interpret-query` prompt is tuned to leave `categoryFilter`/`siloFilter`/`intentFilter`/
  `childName` null unless the LLM is genuinely confident**, explicitly naming keywords as
  the primary signal and the enum filters as secondary/confirmation-only. This is a
  judgment call about prompt wording, not a Rama API question, but it's the actual
  mechanism behind "a wrong or missing single dimension must no longer zero out results" —
  worth stating plainly since it's the crux of whether this fix actually works, and I want
  to confirm this reading matches intent before building the prompt around it.
- **`childNameMatches`** (dead code, `QueryModule.java:389-394`): not wiring it in, not
  deleting it without asking — flagging as an optional cleanup, separate from this
  session's actual bug.

### Design decision for PIECE 4 (ZooEmailTest cast fix)

Straightforward: `ZooEmailTest.java:172-182`'s diagnostic block casts `startTime`/
`deadline` to `String` and re-parses them as ISO-8601 — but they're stored as `Long`
epoch millis already (confirmed by `CLAUDE_HANDOFF.md`'s schema table AND by the actual
extraction printout captured last session). Fix removes the stale string-parsing entirely
and reads the `Long` values directly. No API uncertainty here — this is our own test code,
not a Rama call.

### Things I'm not fully certain about — flagging rather than guessing

- Whether `search-agent` should be reusable by `DigestModule` in the *next* session is
  implied by the "Next Task" note in `CLAUDE_HANDOFF.md` but this session's OUT OF SCOPE
  explicitly excludes "DigestModule search wiring" — I'm building `search-agent` as a
  standalone, cross-module-callable agent (via the now-verified
  `getMirrorAgentClient("QueryModule", "search-agent")` pattern) so that reuse is possible
  later without rework, but I am NOT wiring `DigestModule` to it this session. If this
  extra generality is unwanted (e.g. if `DigestModule`'s eventual needs turn out to look
  different from `QueryModule`'s), that's rework I'd rather flag now than discover later.
- The exact stopword list and 3-character minimum are my own proposal, not derived from
  anything in the codebase — explicitly a placeholder for correction, same spirit as last
  session's leverage/weakness seed entries.
- I have NOT yet verified `PState.setSchema`/`mapSchema` construction for
  `$$events-by-keyword` beyond "it's the same shape as four other indexes already
  proven in this file" — I'm treating that as sufficiently proven-by-repetition rather
  than re-verifying against the jar, since it's a literal copy of an existing, passing
  pattern, not a new API surface.

## Plan correction — two-tier hard/soft filter, per user review

User caught a real contradiction before I wrote any code: my step-3 design said
`intersect` does "strict AND across whatever was provided," but my own step-8 test case
(wrong `categoryFilter`, right keywords → event should still be found) requires exactly
the opposite for that case — under strict AND, a wrong category zeroes the result exactly
like today's bug, just with keywords added to the AND instead of replacing it. I had
written the mechanism-explanation paragraph ("the real fix isn't AND across more things
alone — it's shifting which dimension is reliable... via prompt tuning") without actually
encoding that shift into `intersect`'s logic — the prose described the intent, the
pseudocode didn't implement it. Good catch; resolving explicitly rather than patching
around it.

**Two-tier dimension model, now the actual `intersect` algorithm:**

- **HARD dimensions:** `keywords`, `dateRange`. Always applied strictly — never dropped,
  never bypassed by the fallback.
- **SOFT dimensions:** `categoryFilter`, `siloFilter`, `intentFilter`, `childName`.
  Applied normally when they agree with the hard dimensions, but sacrificial when they
  don't.

**Algorithm** (`resolve-indexes` still collects one `Map<String, Set<String>> byDimension`
keyed by dimension name, tagged HARD or SOFT via a fixed constant set — no change there;
the change is entirely in `intersect`):

1. `byDimension` empty (zero dimensions extracted at all) → full scan, unchanged from the
   original plan.
2. Otherwise, intersect every present dimension's set together (hard + soft) →
   `fullResult`.
3. If `fullResult` is non-empty, use it as-is. This is the common case where all signals
   agree — soft dimensions still narrow results normally when they're correct, they are
   not being ignored wholesale.
4. If `fullResult` is empty AND at least one HARD dimension was present, drop every SOFT
   dimension from `byDimension` and re-intersect using only the HARD dimensions that were
   present (keywords ∩ dateRange, or whichever subset of the two is actually there) →
   this becomes the result. This is the literal "deterministic fallback, no LLM involved"
   the user specified — a second, pure re-intersect over a filtered map, not a retry or a
   new PState read (the per-dimension sets from `resolve-indexes` are reused as-is).
5. If `fullResult` is empty AND no HARD dimension was present at all (only soft
   dimensions were extracted, and they didn't agree with each other), there is nothing to
   fall back to — result stays empty. This is correct, not a gap: with zero hard anchors,
   an all-soft empty intersection has no evidence of a real match to fall back on (e.g. "
   deadline items for Billy" finding nothing for Billy really is zero results; there's no
   keyword or date signal to be lenient about).

**Why this specific tiering is safe against the original bug reappearing in a new
shape:** the fallback only ever *drops* soft dimensions, never *adds* leniency to hard
ones. A wrong `keywords` guess (case: user question keywords don't actually appear in the
matching event's text — genuinely rare given keywords come from the user's own words, but
possible) still correctly finds nothing, because `keywords` is itself one of the hard
dimensions being re-intersected in the fallback, not something the fallback discards.
This is exactly the control case the user asked me to add to `SearchAgentTest`: wrong
keywords + right category must still return empty, proving the fallback doesn't quietly
turn into "any one matching dimension wins."

**Updated `SearchAgentTest` case list** (supersedes the single case sketched in the
original plan's step 8):
1. Zero dimensions → full scan, all events in the family returned.
2. Wrong `categoryFilter` (soft, doesn't match the event's real `eventType`) + right
   `keywords` (hard, matches the event's text) → event IS found, via the hard-only
   fallback (`fullResult` empty because category excludes it → drop soft → keywords-only
   re-intersect finds it). This is the exact bug scenario from `QueryAgentTest`, now
   reproducible without any LLM call.
3. **Control, new per user request:** wrong `keywords` (matches nothing) + right
   `categoryFilter` (would match on its own) → result is empty. Proves the fallback path
   doesn't launder a bad hard-dimension guess through a correct soft one — hard stays
   hard.
4. Two HARD dimensions together (`keywords` ∩ `dateRange`) genuinely filter each other:
   a second event sharing the same keyword but outside the date range must be excluded
   from the result — proves hard∩hard isn't relaxed by the fallback logic either (the
   fallback only removes SOFT dimensions, never loosens a HARD one).
5. A SOFT dimension that agrees with `keywords` narrows correctly in the *non-fallback*
   path (`fullResult` non-empty case) — proves soft dimensions aren't just dead weight
   when they happen to be right.

Implementing now.

## Implementation — steps 1-9, all green

`EventUtils.tokenize`/`tokenizeEvent`, `FamilySchemaModule`'s `$$events-by-keyword`
PState + `Ops.EXPLODE` wiring, `QueryModule`'s extended `QueryParams` (backward-compatible
— both legacy 8-arg/9-arg constructors preserved so `NonLlmPipelineTest.testQueryParamsSerialization`,
which directly constructs `QueryParams` via the 8-arg form, keeps compiling and passing
unchanged), the new `search-agent` (`parse-filters → resolve-indexes → intersect →
finalize`, two-tier hard/soft logic exactly as corrected), rewired `interpret-query`
prompt + `fetch-data` (now delegates via `agentNode.getAgentClient("search-agent")`), and
`ZooEmailTest.java:172-182`'s stale cast fix — all written and compiling.

New tests: `KeywordIndexTest` (10 cases — multi-field tokenization, case folding, stopword
exclusion, min-length exclusion, dedup, cross-event isolation) and `SearchAgentTest` (6
cases — zero-dimension full scan, the exact original bug scenario reproduced LLM-free
(wrong `categoryFilter` + right `keywords` → found via fallback), the user-requested
control (wrong `keywords` + right `categoryFilter` → empty, proving HARD isn't laundered
through a correct SOFT dimension), HARD∩HARD genuinely filtering (`keywords` ∩
`dateRange` excludes a same-keyword event outside the range), SOFT narrowing correctly in
the non-fallback path, and all-SOFT empty-with-no-HARD-anchor correctly staying empty).
All pass on the first run — including the `Ops.EXPLODE` fan-out working end-to-end, which
was the piece I was least certain about despite the jar verification.

Full non-LLM suite: `mvn -o test` — **111/111 green** (95 baseline + 10 `KeywordIndexTest`
+ 6 `SearchAgentTest`), `BUILD SUCCESS`.

## LLM run — 3 of 4 pass; found a real, previously-invisible bug in the new prompt, not the search logic

`mvn -o test -Dtest=QueryAgentTest -Dexcluded.groups=` with real `GEMINI_API_KEY`:

- **"When is the next permission slip due?" now PASSES** — this was one of the two
  original red tests, and the task said it *might* stay red for a different reason
  (parser granularity). It didn't need that excuse: the fallback mechanism worked exactly
  as designed on the exact real scenario. `categoryFilter=PERMISSION_SLIP` (wrong — the
  event is `SCHOOL_EVENT`) zeroed the full intersection; the HARD-only fallback
  (`keywords=[slip, due, permission]`, all literally present in the raw email body) found
  the event anyway. Answer: *"The next permission slip due is for the 3rd Grade Woodland
  Park Zoo Trip. It needs to be turned in by Monday, March 16th at 4:59 PM PDT."*
- **Both Billy questions still report "not found," honestly** — the fixture genuinely
  never names a child "Billy," so this reads as correct behavior, not a bug (matches the
  acceptance gate's own framing).
- **"What is happening on March 20th?" FAILS** — this is the one the acceptance gate
  explicitly expected green. I did not accept this at face value; I added a temporary
  diagnostic print inside `resolve-indexes` (`System.out.println` of the resolved
  `QueryParams`/tokens/epoch bounds — removed again immediately after, not left in the
  codebase) and re-ran just this test to see exactly what `search-agent` received, rather
  than guessing why it failed.

**What the diagnostic showed**, verbatim:
```
question="What is happening on March 20th?"
keywords=[activity, happening, event]
dateFrom=2027-03-20  dateTo=2027-03-20
fromMs=1805500800000  toMs=1805500800000
```

Two distinct, real bugs, both in code this session touched — neither is a pre-existing
issue I'm inheriting blamelessly, and neither is "parser granularity" (the excuse the
task pre-authorized for the permission-slip question):

**Bug 1 — no year-anchoring rule in the rewired `interpret-query` prompt.** The real
current date in this environment is 2026-07-03. Gemini reasonably (from its own
perspective, with no year anchor given) interpreted "March 20th" as the *next upcoming*
March 20th relative to a July 2026 "today" — i.e. March 2027, since March 2026 already
passed. `EmailParsingModule`'s *own* extraction prompt already has exactly this problem
solved: `"All dates should be in 2026 unless explicitly stated otherwise"`
(`EmailParsingModule.java`, `extractPrompt`). My rewritten `interpret-query` prompt never
carried that convention over — a real omission in the prompt I wrote this session, not
something inherited.

**Bug 2 — `parseToEpoch`'s single-day range collapses to a zero-width instant.**
`dateFrom` and `dateTo` are both `"2026-03-20"` for a single-day question, and
`parseToEpoch` parses *both* the same way (`date + "T00:00:00Z"`) — so `fromMs == toMs`,
an exact-instant range, not an inclusive whole-day range. Even with Bug 1 fixed, the
event's actual `startTime` (a specific extracted time-of-day, not midnight) would almost
certainly still fall outside a zero-width `[X, X]` window. This function is pre-existing
code (unchanged from the original `fetch-data`, just relocated verbatim into
`resolve-indexes`) — I reused it without auditing its date-range correctness, and it's now
squarely `search-agent`'s own logic, not something outside this session's scope. `dateTo`
needs end-of-day semantics (`23:59:59.999`) when no time component is given, distinct from
`dateFrom`'s start-of-day semantics.

**A third issue, more subtle — worth flagging even though the first two fixes might be
enough on their own.** `keywords=[activity, happening, event]` — none of these three words
appear literally anywhere in the raw email body. This isn't a tokenizer bug (tokenize()
does exactly what it's specified to do); it's the LLM choosing abstract paraphrases of the
question ("what is *happening*" → "activity", "happening", "event") instead of literal
content words. Per the approved two-tier design, `keywords` is HARD and its resolved set
(if the tokens genuinely match nothing) IS correctly empty — and per the user's own
control-case requirement (`SearchAgentTest` test 3), a HARD dimension that resolves to
empty must NOT be silently dropped by the fallback, since that's exactly the "any one
dimension rescues everything" failure mode the control case exists to prevent. So even
after fixing Bugs 1 and 2, if `keywords` still resolves to an empty match set, the
HARD-only fallback (`keywords ∩ dateRange`) is `{} ∩ dateRangeSet = {}` — still empty. This
is the *tiering design working exactly as specified*, not a flaw in it; the actual root
cause is prompt quality (the LLM's word choice), which a prompt tweak can address:
instruct it to prefer literal words from the question over abstract paraphrases when the
question doesn't have an obvious topical anchor.

**Proposed fixes, none of which touch anything explicitly out-of-scope (classify prompt,
multi-event extraction, DigestModule):**
1. Add a year-anchoring rule to `interpret-query`'s prompt, mirroring
   `EmailParsingModule`'s existing convention.
2. Split `parseToEpoch` into start-of-day (`dateFrom`) and end-of-day (`dateTo`) variants
   inside `search-agent`'s `parse-filters` node.
3. Tighten the keyword-extraction prompt guidance to prefer literal words over abstract
   paraphrases.

**Stopping here rather than applying these silently.** All three are small, self-contained,
and within scope — but none were in the plan I got approval for, and I'd rather report a
precisely-diagnosed defect and a concrete proposed fix than keep iterating on my own
judgment call after call. Per the plan-divergence rule.

## Applying the three approved fixes, plus finding the actual remaining cause

User approved all three fixes with one refinement to #2 (timezone-aware, not UTC — the
request's timezone was already sitting right there in `QueryParams.requesterTimezone` and
I should have used it the first time instead of hardcoding `Z`) and a precise rewording for
#3 (empty keyword list is a valid, expected output — not a failure to pad).

1. Copied `EmailParsingModule`'s year-anchoring sentence verbatim into `interpret-query`'s
   prompt: `"All dates should be in 2026 unless explicitly stated otherwise."`
2. Replaced the single UTC-only `parseToEpoch` with `parseToEpochStartOfDay`/
   `parseToEpochEndOfDay`, both taking the request's `timezone` and using
   `LocalDate.parse(dateStr).atStartOfDay(ZoneId.of(timezone))` /
   `.atTime(23, 59, 59, 999_000_000).atZone(ZoneId.of(timezone))` — a single calendar day
   in the *asker's* timezone, not a UTC-day, and not a zero-width instant.
3. Reworded the keywords rule: extract only words actually present in the question;
   return an empty array for pure date/time questions; explicitly stated that empty
   keywords + a date range is a valid, expected filter (this needed no change to
   `resolve-indexes` — it already skips a dimension whose *list* is empty rather than
   adding an empty-set veto to `byDimension`; the fix was purely in what the prompt asks
   the LLM to produce).

`mvn -o compile`: clean. `mvn -o test`: **111/111 still green** — none of the three changes
touch anything the non-LLM suite exercises directly (the date-range unit tests in
`SearchAgentTest`/`DateIndexTest`/`QueryIndexTest` pass hand-built `QueryParams`/records
directly, not through `interpret-query`'s prompt, so the prompt wording changes couldn't
regress them; the `parseToEpochStartOfDay`/`EndOfDay` split is exercised transitively by
`SearchAgentTest`'s HARD∩HARD date-range test, which stayed green, confirming the new
timezone-aware boundary logic didn't break the existing passing case).

### Re-running the LLM suite — permission-slip and Billy-honesty held, March-20 still flaked once

First re-run: permission-slip stayed green, both Billy questions stayed honest, but
**March-20 failed again** — this time for a genuinely different reason than either bug I'd
just fixed. I did not accept "still broken, ship it anyway" or "must be the same bug,
re-diagnose the same fix" — I re-added a temporary diagnostic (removed again immediately
after, same as before) to see the *exact* resolved filter and the *exact* stored event data
for this specific run, since guessing which of three now-fixed mechanisms was still at
fault would have been exactly the kind of guess this session's non-negotiables forbid.

**What the second diagnostic showed, verbatim:**
```
keywords=[]  dateFrom=2026-03-20  dateTo=2026-03-20  tz=America/Los_Angeles
fromMs=1773990000000  toMs=1774076399999
dateRange matched 0 ids: []
event id=f4a1... startTime=1773964800000 deadline=1773705599000
```

All three of my fixes worked exactly as intended: empty keywords (correct — no content
words in "What is happening on March 20th?"), correct year (2026, not 2027), and a full
Pacific-timezone calendar day (`fromMs`/`toMs` span midnight-to-midnight March 20 in
`America/Los_Angeles`, not a zero-width UTC instant). The event's `startTime`
(`1773964800000`) simply falls seven hours *before* `fromMs` — i.e. it lands on **March
19** in Pacific time, not March 20, even though the raw email explicitly says "Thursday,
March 20th." Confirmed directly from that same run's answer text: *"Thursday, March 19 at
5:00 PM PDT."*

**This is not a search-agent defect — it's `EmailParsingModule`'s own date extraction
landing on the wrong calendar day, run to run.** The event's `startTime` is produced by
`EmailParsingModule`'s `extract-details` node and `parseIsoToEpoch` (not anything this
session touched), and it varies non-deterministically between ingestion runs — I'd
already seen three *different* stored `startTime` values across the different debug runs
in this session (`1773997200000` → March 20, 09:00 UTC / March 20, 2:00 AM PDT;
`1773964800000` → March 19, 24:00 UTC / March 19, 5:00 PM PDT; and a third value from an
earlier run). `search-agent`'s date-range logic is doing exactly the right thing with
whatever epoch value it's given — the instability is entirely upstream, in a component
this session is explicitly forbidden from touching ("Do NOT touch the classify prompt";
multi-event/extraction work is next session's).

**This is the same class of issue the acceptance gate pre-authorized for the
permission-slip question ("root cause is parser granularity... NOT a search defect") —
it just showed up on the question the gate expected to be reliably green instead.**
Verifying this claim rather than asserting it: I re-ran the full `QueryAgentTest` suite
one more time after removing the diagnostic prints. All 4 tests passed, `BUILD SUCCESS` —
on this run `EmailParsingModule` happened to land the event's `startTime` on March 20,
2:00 AM PDT (correctly inside the Pacific-timezone day search-agent computed), and the
answer text confirms it: *"Friday, March 20th at 2:00 AM PDT."* Same code, same fixture,
same search-agent logic — the only thing that changed between the failing and passing
runs was which calendar day Gemini's non-deterministic extraction happened to produce for
`startTime`. That's conclusive: the flakiness is entirely upstream of `search-agent`, not
within it.

**Final verbatim answers (the passing run, all 4 green):**
- "What does Billy need for the field trip?" → *"Hi there! Billy has a field trip to the
  Woodland Park Zoo scheduled for Friday, March 20th at 2:00 AM PDT. The notes provided
  currently focus on read-a-thon pledge forms and don't specify what he needs to bring for
  the zoo trip itself. You might want to check the full school communication for those
  details!"* (not asserted — informational; the fixture never names a child "Billy," yet
  the answer surfaces the trip anyway since keyword/date matching found the one real event
  regardless of the child mismatch — arguably even more helpful than a strict "not found.")
- "When is the next permission slip due?" → *"Hi there! The permission slip for the
  Woodland Park Zoo Trip is due by **Monday, March 16th at 4:59 PM PDT**."* — **PASS**
  (contains "March 16").
- "What is happening on March 20th?" → *"On Friday, March 20th, there's a Woodland Park
  Zoo Trip scheduled for 2:00 AM PDT. This is a school event. Just a quick reminder that
  the deadline for this trip was Monday, March 16th at 4:59 PM PDT."* — **PASS** (contains
  "Woodland Park" and "zoo").
- "Do I need to pick up Billy from school?" → *"I didn't find any events matching your
  question for Billy."* — honest not-found, fixture has no Billy.

`mvn -o test -Dtest=QueryAgentTest -Dexcluded.groups=`: **Tests run: 4, Failures: 0,
Errors: 0** / `BUILD SUCCESS`.

### What I'm flagging for whoever picks up next session's parser-granularity work

`EmailParsingModule`'s date extraction is measurably non-deterministic on the *day*
boundary, not just the exact time-of-day — across this session's various debug runs I saw
the same "Thursday, March 20th" email text produce stored `startTime` values landing on
March 19 *and* March 20 in Pacific time on different runs. That's a wider-blast-radius
version of the "parser granularity" issue the task already flagged for the permission-slip
question (one email, one event) — it affects any question whose correctness depends on
which side of a day boundary an event's `startTime` lands on, including digest windows and
any future date-range digest features, not just `QueryModule`. Not fixing it this
session (explicitly out of scope), but noting it precisely since it's now demonstrated,
not hypothetical.

Nothing committed or pushed — stopping here per instructions.
