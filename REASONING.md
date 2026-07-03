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
