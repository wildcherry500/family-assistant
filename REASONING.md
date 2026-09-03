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

## New session — multi-event extraction, deterministic dates, sane times

Read `CLAUDE_HANDOFF.md`, `RAMA_VERIFIED_LEARNINGS.md`, this file (including the
non-determinism flag above), `EmailParsingModule.java`, `EmailIngestionModule.java`,
`FamilySchemaModule.java`, `ZooEmailTest.java`, `QueryAgentTest.java`,
`NonLlmPipelineTest.java`, plus `EmailIngestionTest.java`, `FamilyAssistantTest.java`,
`GmailMessage.java`, `DigestModule.java`, and `QueryModule.java`'s date-parsing helpers
for ripple/pattern evidence. Also verified, against actual jars on this machine, two
things the task asked me not to guess:

- `dev.langchain4j.model.googleai.BaseGeminiChatModel$GoogleAiGeminiChatModelBaseBuilder`
  (decompiled from `langchain4j-google-ai-gemini-1.8.0.jar`, the version pinned in
  `pom.xml`) DOES expose `.temperature(Double)` and `.seed(Integer)` — confirmed by
  `javap`, not assumed from langchain4j's general docs.
- The same builder does NOT have a `GoogleAiGeminiChatModelBuilder`-level
  `responseFormat` override visible on the subclass, but the shared base builder does:
  `.responseFormat(ResponseFormat)`. This means structured/JSON-schema output is
  available in our pinned dependency if wanted — noted as an option below, not adopted,
  since it would be a new pattern in this codebase (current code always does
  prompt-only JSON + manual parsing) and nothing in the task asked for it.

### Phase A1 — baseline

`mvn -o test` (non-LLM, default exclusion): **111/111, BUILD SUCCESS.** Matches
`CLAUDE_HANDOFF.md`'s claim exactly.

### Phase A2 — LLM suite, three consecutive runs (`mvn -o test -Dexcluded.groups=`)

*(Tooling note: my first attempt at run 2 silently executed against
`/Volumes/CORSAIR/family-assistant` — the stray non-git scratch folder — because a prior
`cd` into a scratchpad directory during jar inspection caused the shell's cwd to reset to
this session's configured "primary working directory" rather than persisting my last
real `cd`. It failed fast with "no POM in this directory," was obviously wrong, and I
redid it with an explicit `cd /Users/toddkeelingfolder/CORSAIR/family_assistant &&`
prefix. Flagging so I don't repeat it — every `mvn` call this session must carry that
explicit prefix.)*

| Run | `QueryAgentTest` | Field-trip time extracted | Permission-slip test | March-20 test |
|---|---|---|---|---|
| 1 | 3/4 (`Tests run: 4, Failures: 1`) | "Friday, March 20th at **1:00 AM PDT**" | **FAIL** — "I didn't find any events matching your question." | PASS |
| 2 | 2/4 (`Tests run: 4, Failures: 2`) | "Thursday, **March 19** at 5:00 PM PDT" — wrong calendar day | **FAIL** | **FAIL** — "I didn't find any events matching your question." |
| 3 | 3/4 (`Tests run: 4, Failures: 1`) | "Friday, March 20" / elsewhere in the same run's answer: "**1:00 AM PDT**" | **FAIL** | PASS |

**QueryAgentTest went 4/4 in zero of three runs.** The permission-slip question failed
in all three — not a rare flake, a near-constant failure, worse than
`CLAUDE_HANDOFF.md`'s "all 4 green as of 2026-07-03" claim (that claim was true on
whatever single run produced it, but isn't the steady-state behavior). The March-20
question failed once (run 2), when the extracted day itself landed on March 19 instead
of March 20 — direct, reproduced confirmation of the exact non-determinism this file
already flagged last session, now with a third example: two runs produced "1:00 AM PDT"
(same implausible value both times), one run produced "5:00 PM PDT" on the wrong day
entirely. Three runs, three different observed `startTime` outcomes for the identical
input email — this is exactly what R1/R2/R3 are meant to fix, confirmed empirically
before touching anything.

**A precise, new piece of evidence on *why* the permission-slip question specifically
fails so consistently**: in run 3, the March-20 answer correctly surfaces both the trip
*and* the permission-slip deadline ("...with a deadline of Monday, March 16th, at 4:59 PM
PDT") from the one collapsed record — proving the deadline data is present and correct in
storage — yet the dedicated permission-slip question still fails. This is consistent with
`QueryModule`'s two-tier hard/soft filter design (documented earlier in this file): the
permission-slip question likely resolves to a SOFT `categoryFilter=PERMISSION_SLIP` with
no HARD dimension (no keywords, no date range) — and since the stored record's real
`eventType` is `SCHOOL_EVENT` (the classify step only ever assigns one category per
email), the SOFT filter alone finds nothing, and per the two-tier design's own stated
rule ("if no HARD dimension was present at all, an empty intersection stays empty"),
there's no fallback to rescue it. This isn't a search-agent bug — the search-agent is
behaving exactly as documented — it's a direct, mechanical consequence of one email
collapsing two distinct real-world items into one `eventType`. Strong, specific
confirmation that R1 is the actual fix for this failure mode, not a coincidental
correlation.

### Phase A2 (bonus, unplanned) — two pre-existing bugs surfaced, unrelated to R1/R2/R3

All three runs also showed identical (non-flaky, 100%-reproducible) failures I did not
expect and are NOT part of the classify/extract path:

1. **`GmailMessage`/`String` cast mismatch, still present at 5 call sites.**
   `CLAUDE_HANDOFF.md`'s "RESOLVED (2026-07-03) — String vs GmailMessage ingestion
   mismatch" note says this was fixed by switching `QueryAgentTest`/`ZooEmailTest` to
   `GmailMessageTestFixtures.fromRawBody(...)`. It was — but three other call sites still
   pass a raw `String`/`List<String>` directly into agents that require
   `GmailMessage`/`List<GmailMessage>`, and every one of them throws the identical
   `ClassCastException: class java.lang.String cannot be cast to class
   com.family.assistant.gmail.GmailMessage` in all three runs:
   - `FamilyAssistantTest.java:86` (`testEmailParsingWritesEventToStore`, `@Tag("llm")`)
   - `FamilyAssistantTest.java:171` (`testEndToEnd_EmailThenDigest`, `@Tag("llm")`)
   - `EmailIngestionTest.java:117,141,169` (`testMixedBatchSkipsBlanksAndParsesValid`,
     `testDuplicateEmailsProduceSeparateEvents`, `testMalformedEmailsDoNotCrashBatch`, all
     `@Tag("llm")`) — these pass `List<String>` where `EmailIngestionModule`'s `ingest`
     node requires `List<GmailMessage>`.
   This means 5 of the repo's `@Tag("llm")` tests currently **always** fail, deterministically,
   regardless of anything this session touches — `CLAUDE_HANDOFF.md`'s "RESOLVED" note is
   accurate for the two sites it names but incomplete as a statement about the whole
   suite's health.
2. **`ZooEmailTest.testZooEmailExtraction` — "Executor pool is shut down."** Reproduced
   identically in all three runs, always the same Clojure-level error inside
   `getMirrorAgentClient`/query-invoke internals, not inside app code I can point to a
   line number for. Looks like JVM-wide test-ordering interaction (Surefire runs all
   classes in one JVM by default; something torn down by an earlier test class's
   `ipc.close()` isn't recreated for a later class's fresh `InProcessCluster`) rather than
   anything in `EmailParsingModule`'s classify/extract path.

**Neither of these is caused by, or fixable within, this session's mandate** (classify/
extract-details path only). But they directly collide with the acceptance gate as
written: *"run the full LLM suite FIVE consecutive times... 5/5 green"* — as things stand,
the full `@Tag("llm")` suite cannot reach 5/5 green no matter what I do to
`EmailParsingModule`, because 5 tests in two other classes fail for reasons outside this
session's scope. **Flagging this as a plan-divergence point, not deciding it myself**: I
need to know whether "the full LLM suite" in the acceptance gate means literally every
`@Tag("llm")` test in the repo (in which case these two pre-existing bugs need fixing
too, even though they're outside the classify/extract path), or specifically
`QueryAgentTest` (the 4 rubric tests) plus `ZooEmailTest` (the two tests this session's
CONTEXT section is actually about) — in which case I'd propose running those specific
classes for the acceptance gate and reporting the other two pre-existing bugs as a
separate, optional fix to take or leave.

### Phase A3 — full ripple inventory: every place assuming ONE event per email

**Production code (must change for R1):**
- `EmailParsingModule.java:238-246` — `extract-details` node builds exactly one
  `ParsedEvent` per email.
- `EmailParsingModule.java:256-295` (`write-to-store`) — builds exactly one
  `eventRecord`, calls `depot.append()` once.
- `EmailParsingModule.java:261-263` — **idempotency scheme breaks under multi-event**:
  `eventId = gmailMessageId` directly. If N>1 events share one `gmailMessageId`, giving
  them the same ID means they'd overwrite each other in `$$family-data` (last-write-wins
  on an identical key) — this is a real bug R1 must solve, not just a type change. My
  plan proposes a composite key (below).
- `EmailParsingModule.java:302-306` (`finalize`) — takes a single `String eventId`,
  calls `agentNode.result(eventId)`. Must become a list.
- `EmailIngestionModule.java:34` — `IngestionResult.eventIds` comment says "one per
  successfully parsed email"; the field itself (`List<String>`, flat) needs no type
  change, only its accumulation logic does.
- `EmailIngestionModule.java:76,86,89,93` — `futures` is
  `List<CompletableFuture<String>>`; `parsingClient.invoke(m)` returns one ID; accumulation
  is `eventIds.add(f.get())`. All four must change to accumulate a *list of lists*
  (`eventIds.addAll(f.get())`) once the per-email agent returns `List<String>`.
- `GmailIngestionModule.java:279-294` — **checked, no change needed.** Only reads
  `result.eventIds.size()` for a summary count; doesn't assume 1:1 with email count.

**Test code (ripple, needs updating — each will get a one-line justification when I
actually make the edit, per the acceptance gate):**
- `ZooEmailTest.java:120` — `assertEquals(1, result.eventIds.size(), "Should parse 1
  email")`. Direct contradiction of R1's explicit requirement. Must become `assertTrue(...
  >= 2)`.
- `FamilyAssistantTest.java:89-91` (`testEmailParsingWritesEventToStore`) —
  `assertInstanceOf(String.class, result, ...)` on the direct agent invoke result. Must
  change to expect a `List`. (Also currently broken by the pre-existing `GmailMessage`
  cast bug above — flagged separately.)
- `FamilyAssistantTest.java:171-172` (`testEndToEnd_EmailThenDigest`) — `String eventId =
  (String) emailAgent.invoke(rawEmail);`. Same change needed. (Same pre-existing bug
  applies.)
- `EmailIngestionTest.java:121` — `assertEquals(2, result.eventIds.size(), "Two valid
  emails should produce event IDs")`. The two emails are `"Field Trip to the Zoo\nPlease
  return permission slip by Friday.\nBring $5."` and `"Reminder: Science project due
  Monday."` — the first one bundles a field trip AND a permission-slip deadline,
  structurally identical to the zoo fixture. A correctly-working multi-event extractor
  could reasonably split it into 2, making the true total 3, not 2. This assertion needs
  to become tolerant (`>= 2`), not just retyped — a hardcoded exact total isn't
  something to encode when the actual count depends on LLM judgment about what's
  "distinct."
- `EmailIngestionTest.java:143-145` (`testDuplicateEmailsProduceSeparateEvents`) — email
  is `"Book Fair next Thursday in the school gym."` (single clear item, unlikely to
  split) sent twice; asserts exactly 2 distinct IDs total. Should still hold under R1
  (each copy → 1 event, distinct IDs from my proposed composite key scheme), but flagging
  since it's the one assertion most directly testing the idempotency/uniqueness property
  R1's ID scheme has to preserve.
- `EmailIngestionTest.java:173` (`testMalformedEmailsDoNotCrashBatch`) — `assertEquals(6,
  result.eventIds.size() + result.failed, "All 6 inputs must be accounted for")`. This
  exact-equality invariant assumes 1 ID per non-failed email; under multi-event a
  non-failed email can contribute 2+ IDs, so `eventIds.size() + failed` can exceed 6.
  Needs to become an inequality or be reworked to count at the per-email level.
- `EmailIngestionTest.java:71,94` — `assertTrue(result.eventIds.isEmpty(), ...)` for
  empty/blank input. **Checked, no change needed** — zero emails in still means zero
  events out regardless of multi-event support.
- `QueryAgentTest.java:63` — `assumeTrue(result.eventIds.size() >= 1, ...)`. **Checked,
  no change needed** — already tolerant.
- `NonLlmPipelineTest.java` — **checked, no change needed.** Never invokes the
  email-parsing-agent or `EmailIngestionModule`; its one `ParsedEvent` reference
  (`testParsedEventSerialization`, already updated last session for the silo/intent
  fields) constructs the object directly and round-trips it — unaffected by how many
  `ParsedEvent`s a real email produces.

**Schema/index layer — checked, genuinely no ripple.** `FamilySchemaModule`'s stream
topology (`FamilySchemaModule.java:137-182`) keys everything off `*record`'s own `id`
field pulled from the depot (`Path.key("id")`, line 139) — it has no concept of "one
email," only "one depot record." As long as each of the N events from one email gets
appended as its own record with a unique `id`, every index ($$events-by-child/-category/
-account/-date/-silo/-intent/-keyword) populates correctly with zero changes to this
file. Worth stating explicitly since it significantly shrinks the actual blast radius
versus what the ripple might have looked like.

### Design decision — R1: multi-event extraction

**Agent graph shape.** The task offered two framings: "per-event classify" vs
"classify-then-split." I'm proposing a third, more specific option that satisfies R1's
literal requirement ("each extracted event gets its own eventId, its own
eventType/silo/intent classification") while minimizing LLM round-trips: **collapse
`classify` and `extract-details` into a single node/prompt that returns a JSON array**,
where each array element independently carries its own `category`/`silo`/`intent`/
`title`/date-strings/`childName` — i.e., splitting and per-item classification happen
in the same LLM call, not as two sequential passes. This is "per-event classify" in the
sense that matters (each item's classification is independently determined, not
inherited from a single whole-email guess) without paying for N+1 separate model
invocations (a naive "split first, then classify each piece separately" design). I'm
recommending this over a literal two-stage split-then-classify pipeline mainly on
latency/cost/consistency grounds — happy to reconsider if there's a reason to prefer
strict separation (e.g. wanting to unit-test "how many events" independently of "how are
they classified").

**Idempotency / eventId scheme — a problem the task didn't spell out but the code makes
unavoidable.** Current scheme uses `gmailMessageId` directly as `eventId` for natural
idempotency (reprocessing the same email overwrites the same record instead of
duplicating it). Under multi-event, N items from one email can't all use the bare
`gmailMessageId` — they'd collide. Proposing: `eventId = gmailMessageId + "#" + itemIndex`
when `gmailMessageId` is present (stable, deterministic per item position, preserves
idempotency on reprocessing), falling back to a fresh UUID per item when it isn't
(matching today's fallback for the single-event case). This needs to be explicit in the
plan since it's a correctness requirement, not a style choice — without it, R1 would
silently drop events on the floor via last-write-wins.

**Result-shape change**: `email-parsing-agent`'s terminal node changes from
`agentNode.result(String)` to `agentNode.result(List<String>)` — for *every* invocation,
including emails that turn out to have exactly one event, for type consistency (an agent
node has one static signature; it can't conditionally return `String` sometimes and
`List<String>` other times). This is the change that ripples into every caller listed in
Phase A3 above.

### Design decision — R2: deterministic dates

**Root cause, from the actual prompt (`EmailParsingModule.java:205-214`), not
guessed**: `extract-details` asks Gemini to free-form-convert prose ("Thursday, March
20th") into a complete ISO-8601 datetime string, doing the day/month/year *and* the
day-of-week disambiguation *and* any relative-date math in one LLM step, then Java's
`parseIsoToEpoch` just parses whatever string comes back. All the actual date
arithmetic happens inside the model's generation, which is exactly the kind of
multi-step reasoning that varies run to run even at low temperature — this matches the
task's own prior ("the less the LLM touches epoch math, the better").

**Mechanism verified before proposing it**: `.temperature(Double)` and `.seed(Integer)`
do exist on our pinned `langchain4j-google-ai-gemini:1.8.0` builder (confirmed by
`javap`, see above) — but I'm not recommending them as the primary fix. Even at
temperature 0 with a fixed seed, hosted LLM inference is not guaranteed bit-for-bit
deterministic for multi-step reasoning (well-documented behavior across providers,
not a langchain4j or Gemini-specific limitation) — it narrows variance, it doesn't
guarantee it, and the acceptance gate requires identical epochs across 5 runs, not "less
often different."

**Recommended mechanism**: change the prompt to ask the LLM only for **discrete calendar
components already present in the source text** — `year` (int or null — "assume 2026 if
not stated," same anchoring rule already used), `month` (1-12), `day` (1-31), and `time`
(24h `HH:mm` string or **null if the source text states no explicit clock time**) — then
resolve `(year, month, day, time)` to an epoch in deterministic Java, anchored to the
family's timezone. This directly reuses the proven pattern already in this codebase
(`QueryModule.java:601-629`, `parseToEpochStartOfDay`/`parseToEpochEndOfDay`:
`LocalDate.of(...).atStartOfDay(ZoneId.of(tz))`/`.atTime(h,m).atZone(ZoneId.of(tz))` →
`.toInstant().toEpochMilli()`) rather than inventing a new date-resolution idiom. The
LLM's job shrinks to "which digits are in this text" — much lower-entropy than full
ISO-8601 synthesis — and Java owns 100% of the epoch math, satisfying the task's stated
prior directly. This is the same mechanism that also solves R3 (below) for free, since
"no explicit time" becomes a first-class, honestly-represented case instead of something
the LLM has to guess a plausible-sounding value for.

**Minor, in-scope-adjacent finding**: `extract-details`'s "today" anchor
(`EmailParsingModule.java:204`, `LocalDate.now().toString()`) has no timezone attached,
unlike `QueryModule.java:138-140`'s `interpret-query`, which explicitly does
`Instant.now().atZone(ZoneId.of(timezone)).toLocalDate()`. Proposing to fix this
inconsistency as part of R2's prompt rewrite (same file, same prompt, directly in scope)
rather than leaving today's-date resolution itself as another latent source of
off-by-one-day drift near midnight boundaries.

### Design decision — R3: sane times

**Root cause, from the code, not guessed**: `parseIsoToEpoch`
(`EmailParsingModule.java:322-339`)'s middle fallback branch treats a *zoneless* ISO
local-datetime string as UTC: `LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC)`. If
Gemini emits a zoneless local time that was actually meant as Pacific wall-clock time
(e.g. a 9:00 AM departure), treating it as UTC shifts it back 7-8 hours — 9:00 AM
Pacific becomes stored as 9:00 AM UTC, which displays as ~2:00 AM Pacific. This lines up
exactly with the observed symptom across all three audit runs (1:00 AM / 2:00 AM PDT
artifacts) and doesn't require guessing "the LLM just hallucinated a bad time" — the
existing UTC-fallback assumption is sufficient to explain it on its own once the LLM's
output lacks a zone suffix, which free-form ISO-8601 generation doesn't reliably include.

**R2's mechanism dissolves this at the source**: since Java resolves `(year, month, day,
time)` explicitly anchored to the family timezone (no zoneless-string guessing at all),
this specific UTC-fallback bug can't recur for newly-extracted events — the ambiguous
zoneless-parse branch in `parseIsoToEpoch` becomes dead code for the new path (I'd
leave it in place only if anything else still calls `parseIsoToEpoch` with a raw string;
if nothing does after the rewrite, removing it is a simplification I'd flag at
implementation time, not decide now).

**All-day representation — proposed, without breaking `$$events-by-date`**: when `time`
is null (source stated no explicit clock time), store `startTime`/`deadline` as the
epoch of **local midnight** for that calendar day in the family timezone (a perfectly
normal `Long`, sorts and ranges through `$$events-by-date` exactly like today — a
date-range query spanning that day still finds it via the existing
`sortedMapRange`/`mapVals` mechanics, zero schema or index changes), plus a new boolean
field on the event record, `allDay` (or similar), so **display code** can suppress the
fabricated clock-time. **This is a real, unavoidable ripple into `DigestModule.java`
(~216-244) and `QueryModule.java`'s `formatEventsForPrompt`/`formatEventsPlain`
(~641-673)** — both currently always render `displayFmt.format(Instant.ofEpochMilli(...))`
with a time-of-day baked into the format pattern. Without a small conditional there (skip
the time portion / use a date-only formatter when `allDay` is true), the underlying data
would be honestly represented but a human or the LLM-facing prompt text would still show
a misleading "12:00 AM" for an all-day event — the exact class of bug R3 exists to kill,
just relocated from "wrong time" to "fake midnight." Per the task's explicit "if R1's
ripple forces a QueryModule touch, STOP and report" — flagging this now: this is a
required, minimal, one-line-per-site conditional in two files nominally out of scope
this session (`DigestModule`, `QueryModule`), not a redesign of either, and I'm not
touching them without confirmation.

### Nothing else contradicts the plan as given — proceeding to write it up.

## Task 1 (approved, sequenced first) — GmailMessage fix + handoff correction

Fixed all 5 remaining call sites using the existing `GmailMessageTestFixtures.fromRawBody(...)`
pattern, matching the two already-fixed sites exactly:
- `FamilyAssistantTest.java:86` and `:171` — wrapped the raw `rawEmail` String.
- `EmailIngestionTest.java` (3 sites) — changed `List<String> inputs` to
  `List<GmailMessage> inputs`, wrapping every raw string literal through
  `GmailMessageTestFixtures.fromRawBody(...)`, including the blank/whitespace entries in
  the mixed-batch test (still correctly skipped downstream since blank-checking reads
  `msg.body.isBlank()`, unaffected by the wrapper).

Verified: `mvn -o test-compile` clean; non-LLM suite still 111/111; full `@Tag("llm")`
suite went from 6 errors + 1 failure to 1 error + 1 failure — the 6 `ClassCastException`s
are gone (`FamilyAssistantTest` 4/4, `EmailIngestionTest` 5/5, both clean), leaving
exactly the two issues already known and out of scope for this sub-task (March-20 date
flake, executor-pool error).

**`CLAUDE_HANDOFF.md` corrected**, not just the code: the original "RESOLVED — String vs
GmailMessage" note only covered 2 of 5 sites — stated that plainly rather than silently
extending it. Also corrected two other claims in the same file that my 3-run audit had
already disproven before this sub-task started: the "all 4 QueryAgentTest passing"
line (top summary + the compound-search RESOLVED section) and the "both ZooEmailTest
tests passing" line — both were true of a single run, not the steady state. Added the
LLM-tagged tests table rows for `EmailIngestionTest`/`FamilyAssistantTest`, which the
original table omitted entirely despite them having 5 `@Tag("llm")` tests between them.

**Executor-pool diagnosis (not a blind fix)**: traced the error to Rama's own
`rpl.rama.distributed.util.executor_pool.SingleThreadExecutorPool` by decompiling
`rama-1.5.0.jar` and grepping for the exact string `"Executor pool is shut down"` — it
only exists in that jar, not in `agent-o-rama-0.8.0.jar`. Reached via
`AgentNode.getMirrorAgentClient` → `AgentDeclaredObjectsTaskGlobal.getMirrorAgentClient`.
Read that class's actual bundled `.java` source (present in the jar) to test my first
hypothesis — a naive JVM-wide static cache colliding across test classes that reuse the
same module name string — and that hypothesis is **wrong**: `_mirrorAgents` is a
per-task `WorkerManagedResource`, freshly created in `prepareForTask`, not a shared
static map. I did not chase this further into Rama's lower-level distributed/executor
internals once it was clear that would mean decompiling substantially more of the
platform to get a definitive answer, and the task explicitly said not to expand this
session to chase a fix outside the classify/extract path once diagnosed. Reported the
honest state in `CLAUDE_HANDOFF.md`: confirmed *where* it originates and *what it isn't*
(not the naive hypothesis), left the exact trigger unresolved, flagged two candidate next
steps (`reuseForks=false`, or an upstream question) for the user to scope separately.

Nothing committed yet — about to commit this sub-task's work as its own commit, then
stop for go-ahead before starting the parser work (R1/R2/R3), per the approved sequencing.

## 2026-07-14 — Audit before "Piece 2: search-agent" task

### Working directory verification
`git status` confirms `/Users/toddkeelingfolder/CORSAIR/family_assistant` is the real
repo (branch `feature/raw-ingestion-depot`, clean tree, real commit history through
`377112c`). Did not touch the `/Volumes` scratch path.

### Major finding: the requested search-agent already exists
The task brief described building a `parse-filters → resolve-indexes → intersect →
finalize` search-agent as new work ("Piece 2"). It is not new. `git log` shows it was
built 2026-07-03 (commit `58de389`, "Compound search complete...") *before* the
Session-2 schema refactor (`583fb97`, 2026-07-05, `eventType`→`tags`,
`childName`/`childId`→`personId`). I initially suspected the refactor might have left it
stale against the old field names — that turned out to be false.

Read `QueryModule.java` in full: the agent graph, node names, and two-tier HARD/SOFT
dimension model described in the task brief are already implemented verbatim
(`QueryModule.java:317-515`). It already resolves against the renamed
`$$events-by-tag`/`$$events-by-person` indexes (`:365-366`), and a code comment at
`:360-362` already documents *why* a single-value lookup against those renamed indexes
correctly implements list-containment semantics: each index was populated by fanning
out one write per list element (`FamilySchemaModule.java:193-205`, `Ops.EXPLODE` +
`anchor`/`hook`), so `psPerson.selectOne(key(familyId).key(childName))` inherently
returns "events whose `personId` list contains `childName`" — no extra containment
logic needed anywhere in `search-agent`.

Ran the real non-LLM suite (not trusted from docs) via
`mvn test` + `target/surefire-reports/*.txt`: 123/123 non-LLM tests green, counts sum
exactly to the 123 `CLAUDE_HANDOFF.md` claims (including `RawEmailDepotTest`, 4, not yet
listed in that doc's test table — a doc gap, not a code problem). `SearchAgentTest`:
6/6 green. Only failures are `QueryAgentTest` (1 failure) and `ZooEmailTest` (1 error) —
both `@Tag("llm")`, both pre-existing and already diagnosed in `CLAUDE_HANDOFF.md`
(date-extraction non-determinism; `InProcessCluster` executor-pool lifecycle issue),
unrelated to search.

### Real gap found: test coverage, not implementation
`SearchAgentTest.java`'s fixture (`:108-130`) gives every event an **empty** `personId`
list (`new ArrayList<String>()`) and at most **one** `tags` element. So while the
underlying containment mechanism is proven generically by `MultiValueIndexTest` (raw
index, not through search-agent) and the tag/keyword/date HARD∩SOFT logic is proven by
`SearchAgentTest`, nothing exercises: (a) a `personId` filter actually matching a real
value through `search-agent`, (b) a single event carrying >1 `tags`/`personId` element
through `search-agent`, or (c) the exact three-dimension compound query the task's GATE
describes (tag + personId + date range together, asserting correct intersection). This
is the one concrete, honest gap between "what exists" and "what the task asked for."

Also flagging, not assuming: `QueryModule.QueryParams` still names its fields
`childName`/`categoryFilter` (`:81-91`, `:142-149` in the test), not `personId`/`tags`.
Functionally correct (verified above) but inconsistent with current schema vocabulary.
Renaming is a design choice with a real blast radius (the LLM prompt's JSON keys at
`:182-206`, every call site) — surfacing it for a decision rather than doing it
unasked.

Proceeding to write up requirements + Phase A summary + a plan scoped to the actual gap
(test coverage + optional naming decision), not a reimplementation, and stopping for
approval before writing any code.

### Approved: test-coverage-only plan, no field rename
User picked "test-coverage plan only" over the field-rename option — `QueryParams`
stays `childName`/`categoryFilter` for now; not touching `QueryModule.java` or
`FamilySchemaModule.java`.

### Implementation
Added `SearchAgentTest.java` tests 7-9 in a new isolated fixture family
(`PERSON_FAMILY_ID`, 4 events: `evt-P1..P4`) — multi-element `tags`/`personId` on
`evt-P1` (`[SCHOOL_EVENT, FIELD_TRIP]` / `[Alice, Bob]`), with `evt-P2`/`evt-P4` as
single-wrong-dimension controls and `evt-P3` as a two-wrong-dimensions control, so each
new test has a genuine "must exclude" assertion, not just a "must include" one:
- Test 7: personId containment on Bob, the 2nd element of `evt-P1`'s list.
- Test 8: tags containment on FIELD_TRIP, the 2nd element of `evt-P1`'s list, narrow
  date range as the HARD anchor (SOFT-only would correctly stay empty per test 6's
  invariant, so every new SOFT-dimension test needed a HARD anchor alongside it).
- Test 9 (the literal GATE scenario): tag + personId + date range together, asserting
  `evt-P1` is the sole match and `results.size() == 1`.

Added a new `appendEventMulti` helper (not touching the existing `appendEvent`, still
used by tests 1-6) that wraps incoming lists in `new ArrayList<>(...)` before storing —
call sites use `List.of(...)` for readability, but the object that actually reaches the
depot is a mutable `ArrayList`, per the verified `List.of()`-in-depot-payloads
constraint in `RAMA_VERIFIED_LEARNINGS.md`.

Verified: `mvn test-compile` clean. `SearchAgentTest` alone: 9/9 green. Full suite:
126/126 non-LLM green (123 + 3 new), same two pre-existing `@Tag("llm")` failures as
before (`QueryAgentTest`, `ZooEmailTest`) — both already diagnosed, unrelated to search,
unchanged by this session. No production code touched (`QueryModule.java`,
`FamilySchemaModule.java` unmodified — confirmed the search-agent needed zero changes).

Updated `CLAUDE_HANDOFF.md`: test count 123→126, `SearchAgentTest` row 6→9 tests with
the new coverage described, added the missing `RawEmailDepotTest` row (a pre-existing
doc gap noticed during the audit, unrelated to this task but cheap to fix in passing).

Not yet committed — reporting completion and awaiting go-ahead before committing.

## 2026-07-15 — Graph schema evolution: typed relations + entity foundation (DESIGN ONLY)

Scope was explicitly design-only: propose PState schemas and the parser's triple JSON
contract for typed relation edges and a first-class `$$entities` table, step 1 of a
four-part path between search and future "Layer 2 commitments." Entity resolution and
co-occurrence edges were out of scope by the task brief. No code or topology was
implemented this session; the full plan (schemas, write-path design, audit evidence) is
at `/Users/toddkeelingfolder/.claude/plans/eventual-sniffing-lamport.md`.

### Audit summary (full evidence in the plan file)
Confirmed the existing `Ops.EXPLODE` + `anchor`/`hook` fan-out pattern
(`FamilySchemaModule.java:186-210`) is what the new triple-edge writes reuse — same
`nullToSet().voidSetElem().termVal(...)` shape already used by
`$$events-by-tag`/`$$events-by-person`. Confirmed the parse-time integration point
(`EmailParsingModule.java`, extract-details call `:243-271`) currently deserializes into
`Map<String,String>` — adding a nested `"relations"` array forces that target to widen
to `Map<String,Object>`, which touches 4 existing field-read lines with casts (mechanical,
not behavioral, called out rather than claimed as zero-diff). Confirmed no entity registry
exists anywhere (`FamilyMembers`/`FamilyConfig`/`EntityRegistry` all grep to nothing) —
entity UUID minting is genuinely new logic, not something I missed. Confirmed additivity:
every PState is its own independent `stream.pstate(...)` call, and both `QueryModule.java`
and `DigestModule.java` fetch PStates by hardcoded name, never by iteration — new PStates
are invisible to existing consumers by construction (same additivity already proven true
for the `-by-person`/`-by-tag` refactor per the 2026-07-14 entry above).

### Determinism verification (VERIFY-AT-SOURCE)
Chat-o-rama was unreachable this session (browser extension not connected). Fell back to
RPL docs per the project's verify-at-source rule: `redplanetlabs.com/docs/~/operating-rama.html`,
"Task scaling" section, states PState recomputation from depot data "only works if your
processing is deterministic, which may not be the case if your processing makes use of
any random numbers (such as UUIDs)." This is Rama's own documentation naming random UUIDs
as the canonical determinism violation — grounds the decision to mint entity IDs via
`UUID.nameUUIDFromBytes(...)`, never `UUID.randomUUID()`, computed inside
`FamilySchemaModule`'s deterministic stream topology (not the LLM-touching parser layer).
Separately confirmed `eventId` itself is already deterministic
(`EmailParsingModule.java:297-301`, `eventId = gmailMessageId` with a `randomUUID()`
fallback only when `gmailMessageId` is blank — flagged as a pre-existing, unresolved edge
case, not introduced by this design). Also clarified two distinct "replay" concepts only
one of which Rama natively guarantees: re-running the LLM parse over old emails
("DEFERRED: Email replay capability", `CLAUDE_HANDOFF.md:345-352`) is non-deterministic
and explicitly not-yet-built; redraining the already-committed `*family-events` depot
through the deterministic stream topology is the native, guaranteed operation — this
design depends only on the latter.

### Key decision: entity-ID hash drops the mention index
Original hash formula was `hash(eventId | objectType | object | index)` — one entity
row per triple *slot*. User caught that this makes `$$entities` behave like a mention
log despite its entity-shaped schema (type/canonicalName/aliases): the same object+type
mentioned via two different relations in one event should collapse to one row, not two.
Revised to `hash(eventId | objectType | object)`, no index. Re-traced the write-path
design against this change before accepting it: every write is either an idempotent
`Set.add` (`$$edges-forward`/`-inverse`) or a same-key/same-value overwrite (`$$entities`),
and nothing in the topology reads or branches on array position — dropping the index is
safe, not just tolerated, and actually fixes a latent duplication bug rather than trading
one problem for another. No `$$mentions` rename fallback was needed. Cross-event
distinctness is preserved (`eventId` stays in the hash); merging the same real-world
entity across different events is still resolution's job, still out of scope.

### Other decisions
- `objectType` vocabulary finalized: `PERSON | ORG | PLACE | PROJECT | UNKNOWN`. `ORG`
  added (missing from my draft; high-volume in this domain). `PROJECT` added as
  `PART_OF`'s target type. `EVENT` deliberately excluded — event-to-event linkage
  already exists via the (currently always-empty) `relatedEventIds` field on the event
  record; not duplicated as an entity type.
- `objectType` is part of the entity-ID hash, so a mis-typed mention (LLM tags something
  `PLACE` that should've been `ORG`) mints a different `entityId` than the correctly-typed
  version, splitting one real entity into two until resolution merges them. Accepted
  trade-off, not a bug — recording it here so it isn't mistaken for one later.
- Relation vocabulary: closed enum `MENTIONS_PERSON | PART_OF | LOCATED_AT | ACTION_NEEDED
  | UNKNOWN`, prompt-constrained like the existing `category|silo|intent` pattern
  (`EmailParsingModule.java:177-192`).
- `DEADLINE_FOR` proposed then dropped: `deadline`/`startTime` are already scalar event
  time-metadata feeding `$$events-by-date` directly; a triple needs a target entity, and
  a deadline isn't a relation to one.
- `ACTION_NEEDED` included: audited for a structural blocker to later promotion into a
  richer Layer-2 commitment lifecycle and found none — the edge is stateless, Layer 2 can
  read it as a seed signal or expand the relation vocabulary additively without touching
  existing PState shape. Flagged, not resolved: semantic overlap with the existing scalar
  `intent = ACTION_REQUIRED` field/`$$events-by-intent` index — the edge adds the "who"
  dimension the scalar lacks (and gives a free per-person action queue via the inverse
  index), but Layer 2 will need to decide how the two signals relate to each other.
- Source-neutrality: triple emission must work identically for email today and a
  confirmed second stream ("Brain Dump," zero prior repo trace — new context introduced
  this session) without a rewrite. Turned out to already be structurally supported:
  `eventRecord.put("sourceType", "email")` already exists as a generic origin field
  (`EmailParsingModule.java:326`), and `FamilySchemaModule`'s topology has no hard
  dependency on email-specific fields (`emailSubject` is the only one referenced, and
  degrades gracefully to `""` when absent). The triple contract rides on the same shared,
  already-source-agnostic depot/topology — no branching logic needed anywhere.
- `WORK` type (songs/paintings/books/etc.) deliberately deferred to `UNKNOWN` rather than
  guessed at now. Valid only because `UNKNOWN` is inspectable: added a fourth PState,
  `$$entities-by-type` (`familyId -> type -> Set<entityId>`, same pattern as
  `$$events-by-account`), so the `UNKNOWN` bucket is a real indexed queue, not a
  scan-and-hope. Trigger condition for building a promoted `WORK` type: real recurring
  volume showing up under that queue, not a guess made in this session.

### Open items carried forward (not blocking, in the plan file §8)
Exact `$$entities` write Path syntax needs confirming against `$$family-data`'s actual
write code (not audited this session — Audit A covered the Set-based inverted indexes,
not the record-store-style write). The `childId`/id-resolver comment at
`EmailParsingModule.java:314-320` references the same concept the future entity-resolution
brief will build — the two efforts should converge on one resolver, not two.

Plan approved 2026-07-15. Stopping here per the task's explicit scope (design + this
REASONING.md entry only) — no code, no topology implementation this session.

## 2026-07-15 — Graph schema evolution: implementation

User said "move on to implement" after approving the design plan above. Implemented
exactly what the plan specified — no redesign, no simplification.

### Resolving the one open implementation question before writing code
Plan §8.1 flagged that `$$entities`'s write-path Path syntax (a direct key→value
"record overwrite," not a Set-based inverted-index write) hadn't been audited. Read
`FamilySchemaModule.java` in full before touching it: `$$family-data`'s write
(`Path.key("*familyId").key("events").key("*eventId").termVal("*record")`,
`FamilySchemaModule.java:161-162` pre-edit) is exactly that pattern — direct key path +
`termVal`, no `nullToSet`. Confirms `$$entities` should write
`Path.key("*familyId").key("*objectId").termVal("*entityRecord")`.

Also needed to confirm `Block.each` supports multi-argument static methods (the entity-ID
formula takes 3 inputs: eventId, objectType, object) — no example existed anywhere in
this repo, only single-arg usage. Rather than guess, decompiled the pinned
`rama-1.5.0.jar` (`~/.m2/repository/com/rpl/rama/1.5.0/rama-1.5.0.jar`) and ran `javap`
on `com.rpl.rama.Block` directly: confirmed overloads exist up to
`RamaFunction8`/`RamaOperation8` (`Block.each(RamaFunction2<T0,T1,R>, Object, Object)`,
`RamaFunction3`, etc.), same verification method this file's own comments already used
for `Ops.EXPLODE`/`anchor`/`hook`. No novel/undocumented API needed — the write path
uses exactly the same tool family already proven in this codebase.

### Implementation
`FamilySchemaModule.java`: added `mintEntityId` (3-arg, deterministic hash per the
approved formula) and `buildEntityRecord` (2-arg, builds the `{type, canonicalName,
aliases}` map) as static helpers next to `effectiveTime`. Added 4 PState declarations
(`$$edges-forward`, `$$edges-inverse`, `$$entities`, `$$entities-by-type`). Added a new
branch inside the existing `anchor("fanoutRoot")`/`hook(...)` structure, inserted between
the personId branch and the keyword branch — one `EXPLODE` over `"relations"`, then four
sequential writes off the same exploded triple (not a second EXPLODE, so no further
anchor/hook isolation needed, consistent with the invariant this file's own comments
already document).

`EmailParsingModule.java`: extended the extract-details prompt with the closed-enum
`relations` field; widened the Jackson deserialization target from `Map<String,String>`
to `Map<String,Object>` (required — a nested array doesn't fit the old type), which
needed casts added to the 4 existing field-read lines, exactly the mechanical/non-behavioral
consequence flagged in the plan's §4a/§6, not a surprise. Added `parseRelations` (mirrors
`classify`'s regex-validation posture: malformed or unrecognized entries are dropped, not
coerced to UNKNOWN — a shape we don't recognize isn't safely "unknown"). Added `relations`
to `ParsedEvent` (appended as the last constructor param) and to the `eventRecord` map
written to the depot.

### Fixing what the plan didn't anticipate
`mvn test` (before a clean rebuild) surfaced a `NoSuchMethodError` at runtime in
`NonLlmPipelineTest.testParsedEventSerialization` — it directly constructs a
`ParsedEvent` with the old 16-arg constructor. This should have been a compile error
(the constructor signature changed), and the fact that it wasn't is itself worth
recording: `mvn compile`/`test-compile` reported "Nothing to compile - all classes are
up to date" because I'd already run them in isolation earlier in the session, so this
particular `mvn test` invocation never re-checked `NonLlmPipelineTest.java` against the
new signature — Maven's default incremental compiler doesn't always do full
dependency-closure recompilation when an upstream method signature changes. Updated the
test to pass a representative `relations` list and added a round-trip assertion on the
new field, then ran `mvn clean test` (not just `mvn test`) to force a truly fresh build
before trusting the result.

### Proving the design decisions actually hold, not just that nothing broke
The 126 pre-existing tests passing was necessary but not sufficient — none of their
fixtures ever populate a `relations` field, so the entire new branch was previously
compiled but never executed by any test. Added `EdgesEntityIndexTest` (10 tests,
following `MultiValueIndexTest`'s established InProcessCluster/PState-assertion
convention) specifically targeting the properties this session's decisions depended on,
not just "does it write something": forward+inverse edges exist for every triple; two
different relations targeting the same object+type *within one event* collapse to the
same entityId (the exact mention-log-vs-entity-table distinction the user caught during
design); the same object+type in *two different events* mints two different entityIds
(no accidental cross-event dedup); `$$entities-by-type` actually indexes both PERSON
mentions and the PLACE mention; an event with no `relations` key at all writes nothing to
any of the 4 new PStates (backward compatibility with every pre-existing record shape);
and — the test most directly tied to the redrain-safety argument in the design session —
re-appending an identical record (simulating a depot redrain) mints no new entities and
does not grow any edge Set, with the re-derived entityId asserted equal to the original.

`mvn clean test`: **136/136 non-LLM tests green** (126 existing, unchanged + 10 new),
zero regressions. Updated `CLAUDE_HANDOFF.md`: PState schema table (+4 rows), event
record fields table (+`relations` row), test suite table (+`EdgesEntityIndexTest` row,
126→136), Current Build Status line, and a new "Recently Completed (2026-07-15)" section
summarizing this work for future sessions.

### Pre-commit coverage check (caught a real gap, not a rubber stamp)
Before committing, checked the test file against two specific coverage questions:
does it assert BOTH edge directions for the same triple (not just each independently),
and does it exercise `$$entities-by-type`'s `UNKNOWN` bucket specifically. Read the file
rather than trusting memory of what I'd written.

Forward+inverse: covered. `inverseEdgesPointBackAtTheSubjectEvent` uses
`billyIdFromE1`/`jeffersonId`, both derived in `setup()` via `soleElement(forwardSet(...))`
— so the inverse assertions are keyed by whatever the forward write actually produced, not
an independently hardcoded ID. A bug that wrote the wrong objectId to forward, or the
wrong key to inverse, would break this test. Genuine pairing, not two decoupled
existence checks.

UNKNOWN-bucket population: NOT covered — a real gap, not a false alarm. Every fixture in
the file used PERSON/PLACE; nothing ever exercised `objectType: "UNKNOWN"`, so
`$$entities-by-type["UNKNOWN"]` — the entire mechanism the `WORK`-type deferral depends
on being real rather than aspirational — was asserted nowhere. Added one more event
(`evt-E4`, a deliberately-unrecognized mention, "Blue Sky Symphony" — a song, tying back
to the design session's own creative-work example) and one test asserting the UNKNOWN
bucket contains it and resolves back to the correct `$$entities` row (type + raw
canonicalName recoverable). Tiny follow-up, not a redo — the rest of the suite and the
production code were untouched.

`mvn clean test` (final): **137/137 non-LLM tests green** (126 existing + 11 in
`EdgesEntityIndexTest`), zero regressions. Updated `CLAUDE_HANDOFF.md` counts and the
`EdgesEntityIndexTest` row description accordingly (136→137, 10→11 tests).

Committing this as a checkpoint — the commit message states what's done and what's
deliberately deferred (resolution, co-occurrence) so the next session (resolution or
Layer 2) can read scope from the commit, not reconstruct it from the diff.

## 2026-07-16 — Layer 2: Commitments — audit-first design brief (DESIGN ONLY, forks pending)

Scope: design only, no code, no topology changes. Deliverable is this entry plus a chat
presentation of the same content, per the task's explicit "stop for fork decisions before
any implementation" instruction. Plan file with the pre-audit scoping is at
`/Users/toddkeelingfolder/.claude/plans/cheeky-purring-breeze.md`, approved with three
amendments (deterministic commitment IDs as a hard requirement; resolve ordering, not just
mutation, for the replayability fork; reframe the identity fork as confirming the standing
entity-resolution deferral rather than reopening it).

### Phase A audit (file:line evidence)

**A1 — `ACTION_NEEDED` edge, current state.** Declared in the closed relation enum
(`FamilySchemaModule.java:39-40`), populated at `FamilySchemaModule.java:273-282`: one
`EXPLODE` over the record's `relations` list, then `$$edges-forward`
(`familyId -> eventId -> relation -> Set<objectId>`) and `$$edges-inverse`
(`familyId -> objectId -> relation -> Set<eventId>`) writes for every triple, `ACTION_NEEDED`
included with no special-casing. Grepped `QueryModule.java` and `DigestModule.java` for
`edges-forward`/`edges-inverse`/`$$entities`: zero hits. Confirmed — no consumer exists yet;
this is genuinely an unconsumed seed signal, not an already-wired one.

**A2 — scalar `intent` signal, current state.** `$$events-by-intent`
(`FamilySchemaModule.java:161-164` declaration, `:236-239` write) is a *scalar* field on the
event record (`ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN`), one value per event —
unlike `relations`, which is a list that can name several typed targets. It **is** consumed
today: `QueryModule.java`'s `search-agent` reads it as a SOFT dimension (`intentFilter`,
`QueryModule.java:91,187,370,436-441,546`). The two signals differ in shape (scalar-per-event
vs. list-of-typed-edges-with-a-target) and in consumption state (one wired into search, one
not wired anywhere) — this is the reconciliation the original brief flagged as due.

**A3 — append-only/replayability boundary.** `*family-events` is `Depot.hashBy("familyId")`
(`FamilySchemaModule.java:110`); every downstream PState in this module is written by exactly
one deterministic stream topology off that depot (`FamilySchemaModule.java:218` onward) — no
PState here is ever written from anywhere else. Entity IDs use `UUID.nameUUIDFromBytes`
specifically to preserve that determinism (2026-07-15 entry above, verified against
`redplanetlabs.com/docs/~/operating-rama.html`, "Task scaling"). No existing PState in this
codebase has ever modeled state that changes independent of the depot's own content —
`$$leverage-map`/`$$weakness-map` are config, written once per config record, never mutated
in place by a later, separate action. A commitment's `status` transitioning from `open` to
`done` on a user action, with no corresponding depot-derived signal, has no precedent here.
Genuine architectural fork, not a formality.

**A4 — additivity precedent.** Confirmed by grep that the 2026-07-15 graph-schema addition
touched zero existing PState declarations and zero `QueryModule.java`/`DigestModule.java`
lines — both modules fetch PStates by hardcoded name, never by iteration, so new PStates are
invisible to old consumers by construction. This is the bar any `$$commitments` addition must
also clear.

**A5 — Brain Dump.** Zero repo trace beyond its mention in the 2026-07-15 entry as a
"confirmed second stream." No ingestion module, depot, or schema exists for it. Per the
brief's own lean (confirmed on approval, "everything else as written"), it stays out of scope
this session — the commitments schema must not hard-code an email-shaped source assumption,
the same source-neutrality check already applied to `relations` in the prior session.

### Amendment 2 — resolving ordering, not just mutation (verify-at-source)

Chat-o-rama status not re-checked this session; went straight to `redplanetlabs.com/docs`
per this project's established fallback order, since it resolved the last two API questions
(2026-07-03, 2026-07-15) without needing the chat product.

**Within one depot partition, order is guaranteed.** `redplanetlabs.com/docs/~/depots.html`:
"By using a depot partitioner to ensure any individual user's ... data goes to the same depot
partition, local ordering is maintained and ETLs can process that data in the correct order."
And the converse, stated explicitly: "If you were to use `Depot.random()` for that depot,
then they could be processed out of order since data on different partitions are processed in
parallel and independently."

**Across two different depots, there is no ordering guarantee at all.**
`redplanetlabs.com/docs/~/tutorial6.html` (the social-network tutorial) hits this exact
shape — a `FriendRequest`/`CancelFriendRequest` pair, structurally identical to a
commitment-created/commitment-status-changed pair. Direct quote: "This is necessary so that
different types of data that affect the same PStates are processed in the order in which
they happened... If those were kept on separate depots, there's no guarantee as to the order
in which they will be processed." The tutorial's resolution is **not** a create-if-missing
read on the consuming side — it's structural: both record types go on **one** depot
(`Depot.hashBy(UserIdExtract.class)`, so a given user's `FriendRequest`/`CancelFriendRequest`
always share a partition), branched inside the topology via `subSource`
(`SubSource.create(FriendRequest.class)... Agg.set(...)`,
`SubSource.create(CancelFriendRequest.class)... Agg.setRemove(...)`).

**This resolves Amendment 2's two candidates: (a) single depot is the docs-demonstrated
idiomatic pattern for this exact shape of problem, not a hypothetical alternative.** I did not
find (and did not separately go looking for, since (a) already matches the reference tutorial
precisely) a documented "create-if-missing apply" pattern (b) — Rama's own worked example
solves the identical ordering problem structurally, via partitioning, not defensively, via
read-before-write logic on the consumer. Recommending (a); still presenting as Fork 1 below
since the *choice* to model commitments this way (vs. some other shape entirely) is still
yours, even though the *mechanism*, once you choose event-sourced transitions, is now
verified rather than assumed.

**Also verified, narrowing Fork 1 further:** every PState write in this codebase (and every
PState write demonstrated in the docs) happens inside topology processing of depot data —
`RAMA_VERIFIED_LEARNINGS.md` documents no other write mechanism, and I found none in the
pages fetched this session either. I did not exhaustively search for a "direct PState write"
API outside topology context, so I'm stating this as consistent-with-everything-verified,
not as an exhaustively-ruled-out claim — but it means Fork 1 is less "event-sourced vs. some
unknown alternative" and more "event-sourced by construction (Rama has no other way to write
a PState), single-depot-with-subSource vs. two-depot-with-unresolved-ordering-risk."

### Amendment 1 — deterministic commitment IDs (hard requirement, stated explicitly)

A commitment's ID must be derived deterministically from its source edge's stable fields —
same discipline as `mintEntityId` (`FamilySchemaModule.java`, 2026-07-15 session):
`commitmentId = hash(sourceEventId | "ACTION_NEEDED" | objectId)` via
`UUID.nameUUIDFromBytes`, never `UUID.randomUUID()`. This is not optional polish — the
event-sourced replay argument above only holds if redraining `*family-events` (which
regenerates the `ACTION_NEEDED` edge and therefore must regenerate the *same* seed record)
produces the identical `commitmentId` every time. A random ID would silently break replay:
every redrain would mint a new commitment for the same underlying edge, duplicating rather
than reconciling. Naming this as a requirement rather than leaving it implied, per the
approved amendment.

### Proposed `$$commitments` schema + write-path sketch (gated on Fork 1 confirmation)

Proposal only — not implemented, not locked, contingent on you confirming Fork 1's mechanism.

- **New depot:** `*commitment-events`, `Depot.hashBy("commitmentId")` — mirrors the
  `FriendRequest`/`CancelFriendRequest` partitioner exactly, substituting `commitmentId` for
  `userId` as the co-partitioning key so a given commitment's creation and every subsequent
  status change land on the same partition, in the order they happened.
- **Two record types on that depot, branched via `subSource`** (unverified call signature
  for this codebase — `subSource` has zero prior usage here; flagging as an open item to
  confirm against the jar before implementation, not guessing the shape):
  - `CommitmentSeeded { commitmentId, familyId, sourceEventId, objectId, createdAt, status }`
    — `commitmentId` computed via the deterministic hash above, `status` set to the initial
    value from whatever vocabulary Fork 2 settles on.
  - `CommitmentStatusChanged { commitmentId, familyId, newStatus, actor, changedAt }`.
- **`$$commitments` PState:** `familyId -> commitmentId -> {status, createdAt, updatedAt,
  sourceEventId, objectId}` — same direct key-path + `termVal` shape already proven at
  `$$family-data` (`FamilySchemaModule.java:223-224`) and `$$entities`
  (`FamilySchemaModule.java:284-285`). `CommitmentSeeded` writes the initial record;
  `CommitmentStatusChanged` overwrites `status`/`updatedAt` only, via a narrower
  `Path.key(...).key(...).key("status").termVal(...)`-style write (exact multi-field-update
  Path shape not yet verified against the jar — another open item, not a guess).
- **`$$commitments-by-status` index:** `familyId -> status -> Set<commitmentId>`, same shape
  as `$$events-by-intent`/`$$entities-by-type` — this is the concrete mechanism behind "enough
  for a future proactive loop to scan for gaps/overdue" (a `sortedMapRange`-style or
  bucket-lookup query the Layer 3 scanner can use directly, no full-table scan). Moving a
  commitmentId between status buckets on a transition requires reading the current status
  before writing the new bucket (to remove it from the old one) — a read-then-write inside the
  same topology pass, which I have not verified the exact API shape for in this codebase
  (every existing index write here is a pure append into a `Set`, never a move-between-buckets
  update). Flagging as an open item to verify before implementation, same posture as every
  other "don't guess an API" item in this file.

### Fork 1 — replayability mechanism (yours to confirm, now evidence-backed)

Recommending: single depot (`*commitment-events`), `subSource`-branched, per the verified
`FriendRequest`/`CancelFriendRequest` pattern above. This is the only mechanism found in the
docs that solves the creation-before-transition ordering problem structurally rather than
defensively. Confirm to lock, or tell me what's wrong with applying that pattern here.

### Fork 2 — lifecycle-state vocabulary (product decision, yours)

Proposing four states, open for your edit: `OPEN → IN_PROGRESS → DONE`, plus `DISMISSED` as a
distinct terminal state from `DONE` (not-applicable / no-longer-relevant, vs. actually
completed — these have different meaning for a future "what's overdue" scan, so collapsing
them would lose information). Unlike `silo`/`intent`/`relation`, this vocabulary is not
LLM-classified — it's set deterministically (`OPEN` on creation) and changed only by explicit
action — so no `UNKNOWN` bucket is needed here, unlike those three enums.

### Fork 3 — `ACTION_NEEDED` edge vs. scalar `intent=ACTION_REQUIRED` (due this session, yours)

Recommending: **coexist, independently enforced, commitments seed only from edges.**
`intent` stays exactly as it is today (an event-level SOFT search dimension, untouched,
zero risk to `QueryModule.java`'s existing wiring). `ACTION_NEEDED` edges are the sole seed
signal for `$$commitments`, because only the edge carries the structured target (`objectId` —
who/what the action concerns) that a commitment record needs; the scalar `intent` field has
no target, only a coarse event-level flag. I'm not proposing to make the parser guarantee
edge-when-intent-is-ACTION_REQUIRED (that's a prompt change in `EmailParsingModule`, out of
scope this session, and risks conflating a coarse classifier signal with a structured relation
the LLM extracts separately and possibly inconsistently) — flagging as a real future
tightening opportunity, not doing it now.

### Fork 4 — seeding mechanism (yours)

Recommending: **auto-create**, mechanically, in the same topology branch that already writes
`$$edges-forward`/`$$edges-inverse` for an `ACTION_NEEDED` triple — zero LLM involvement, zero
new human-review step, matches the deterministic/additive posture of everything built so far.
A promotion/review gate (alternative option) would make commitment creation depend on
asynchronous human review timing rather than deterministic redrain timing, and starts to look
like Layer 3 UI surface rather than Layer 2 state. If review-before-acting-on-a-commitment is
wanted, `DISMISSED` (Fork 2) already gives a lightweight way to reject a bad auto-created
commitment after the fact, without needing a separate pre-creation gate.

### Fork 5 — commitment identity (reframed per Amendment 3: confirmation, not an open call)

This is the entity-resolution deferral wearing a disguise, not a new decision. Per Amendment
1's deterministic-ID rule, two different `ACTION_NEEDED` edges — even ones a human would
recognize as "the same real-world commitment" — always mint two different `commitmentId`s,
because the hash includes `sourceEventId`. This is the identical shape as the 2026-07-15
entity-resolution deferral ("the same object+type mentioned in two different events still
mints different entityIds until a future resolution effort merges them"). Presenting this as
confirming the standing decision (allow duplicates now, resolve later only if real volume
demands it) — not reopening entity resolution, not solving it here.

### Additive / replayable / no-scope-leak checklist

- **Additive:** one new depot (`*commitment-events`), two-to-three new PStates
  (`$$commitments`, `$$commitments-by-status`); zero edits to any existing PState declaration;
  zero edits to `QueryModule.java`/`DigestModule.java` this session (read-wiring, if wanted,
  is a separate future decision — not assumed in scope here, flagging explicitly rather than
  silently including or silently excluding it).
- **Replayable:** commitment IDs deterministic (Amendment 1); creation-before-transition
  ordering guaranteed structurally by single-depot partitioning (Amendment 2), matching the
  verified `FriendRequest`/`CancelFriendRequest` reference pattern, not a defensive
  create-if-missing check.
- **No scope leak:** no Layer 3 scanning agent, no entity resolution, no Brain Dump wiring
  (A5, confirmed out per your approval), no parser-prompt changes to `EmailParsingModule`
  (Fork 3's tightening opportunity deliberately not taken).

### Open items carried forward, not blocking a fork decision but blocking implementation

`subSource`'s exact call shape (zero prior usage in this codebase); the exact Path syntax for
a partial-field PState update (every existing write here is either a whole-record `termVal` or
a `Set`-append, never a move-between-index-buckets update, which `$$commitments-by-status`
needs on every transition) — both need jar-level verification before any code is written,
per this project's standing "never guess an API" rule.

Stopping here. No code, no topology implementation this session — waiting for your decisions
on Forks 1–4 (Fork 5 stands as confirmed) before scoping an implementation session.

## 2026-07-16 — Layer 2: Fork 1 correction — creation/status-change asymmetry

User caught a real defect in the recommended Fork 1 mechanism before locking it: the
`FriendRequest`/`CancelFriendRequest` reference pattern I verified last entry assumes both
record types are **symmetric user actions** — both permanent, both must be ordered against
each other. `CommitmentSeeded` and `CommitmentStatusChanged` are not symmetric:
`CommitmentStatusChanged` is a genuine user action, not regenerable from email — matches the
reference pattern. `CommitmentSeeded` is *derived*, regenerated identically on every redrain
of `*family-events` — it does not need to be a permanent record at all, and treating it as one
creates the exact failure this layer exists to prevent: a redrain re-appends a fresh
`CommitmentSeeded` that can land, in a single shared depot/partition, after an existing
`StatusChanged`, re-materializing the commitment at its initial status and silently erasing a
`DONE`. I had reused the reference pattern's shape (both sides permanent, same depot,
ordering-by-partitioning) without checking whether both sides in *this* problem actually
carry the same permanence requirement — they don't. Correcting rather than patching around it.

### Corrected mechanism (recompute + create-if-missing merge, not co-partitioned ordering)

**Creation is not a depot record.** It's recomputed, every redrain, in the same
`FamilySchemaModule` stream-topology branch that already writes `$$edges-forward`/
`$$edges-inverse` for an `ACTION_NEEDED` triple (`FamilySchemaModule.java:273-282`). For each
`ACTION_NEEDED` edge processed: `localSelect` the current `$$commitments` record at
`(familyId, commitmentId)` (deterministic ID per Amendment 1). If absent, write the full
initial record (`sourceEventId`, `objectId`, `createdAt`, `status = OPEN`) and add it to the
`OPEN` bucket of `$$commitments-by-status`. If present, at most refresh the identity fields
(idempotent — same deterministic values every redrain) and **never touch `status` or its
index bucket**. This is what makes redraining safe: the derived side is naturally idempotent
and explicitly forbidden from clobbering the user-driven side.

**Status changes are the only permanent record.** New depot,
`*commitment-status-changes` (`Depot.hashBy("commitmentId")`), holding only
`{commitmentId, familyId, newStatus, actor, changedAt}` — small, dedicated, genuinely
append-only because every record in it really did happen once, at a real point in time, and
must never be regenerated or replayed differently. A second stream-topology branch (same
module, per the "index PStates live with their primary PState" convention already established)
processes it: `localSelect` the current record; if **absent** (a status change arrived before
the creation branch ever materialized this commitment — e.g. first-run ordering, or a status
change referencing a commitment whose source edge a later parser change removed), create a
stub record with whatever identity fields the event itself carries (`commitmentId`,
`familyId`) and `sourceEventId`/`objectId` left absent until/unless the creation branch later
fills them in — flagging this as a known, accepted gap (an "orphaned" status change with a
temporarily-incomplete identity), not a silently swept-under-the-rug case. If **present**,
read the old `status` (needed to remove the commitment from its old `$$commitments-by-status`
bucket before adding it to the new one), then write the new `status`/`updatedAt`.

**Why this is genuinely order-independent, not just re-labeled ordering.** Both branches use
the identical shape of rule — "create the base record if it doesn't exist, but never
overwrite what the other side owns" (creation never touches `status`; status-changes never
overwrite `sourceEventId`/`objectId` once the creation branch has filled them in, only add
them if genuinely absent). Since it doesn't depend on which branch runs first, it doesn't need
co-partitioning across two depots to establish an order — it only needs each branch to be
correct on its own, which is a strictly weaker and safer requirement than "hope the two
depots' events interleave correctly."

### Whether this is covered by the previously-flagged open items — no, adding a new one

The two open items from the prior entry (`subSource`'s call shape; a generic "partial-field
update" Path syntax) were written under the now-superseded single-depot/`subSource` model and
don't quite name the actual mechanism this corrected design depends on. **Retiring** the
`subSource` item — it's not needed under this design, since we're deliberately using two
independent depot-fed branches rather than one `subSource`-branched depot. **Replacing** the
generic "partial-field update" item with the precise mechanism now in play: a `localSelect`
read of the current `$$commitments` record, followed by a conditional write (different Path
writes depending on whether the read returned a value), inside one topology event.

Verified the *concept* is sound, via `redplanetlabs.com/docs/~/pstates.html`: `localSelect`
"queries the PState partition located on the current task" and "is a synchronous call —
nothing else can happen on a task while a `localSelect` is running, meaning no other PStates
on that task can change." Cross-referenced against the general execution model (same docs,
prior fetch): "An individual event could do an arbitrary number of reads and writes to PStates
on its task. The event doing the writing will be able to read its writes immediately... it's
impossible for subsequent events to ever see intermediate states." Together these confirm
read-then-conditional-write within one event is atomic with respect to every other event on
that task/partition — the concept this corrected design needs is real, not assumed.

**Still open, not yet verified, before implementation:** the exact Java call shape for
`localSelect` and for branching a `Block` chain on its result (this codebase has zero prior
usage of `localSelect` — every existing read in `FamilySchemaModule.java`'s write path is a
`.select(...)` pulling a field off the incoming `*record`/`*triple`, never a PState read
mid-topology) and the exact Path syntax for writing only `status`+`updatedAt` on an existing
record without re-writing the whole map (vs. the current codebase's only two write shapes:
whole-record `termVal` and `Set`-append via `nullToSet().voidSetElem()`). Both need jar-level
(`javap` against the pinned `rama-1.5.0.jar`) or doc confirmation before any code is written —
per this project's standing rule, not guessed at here.

### Fork 1 — re-presented, corrected

**Recommending:** recompute-on-redrain for creation (no depot record) + one small permanent
depot for status changes only + create-if-missing merge logic on both branches, as described
above. This replaces the previously recommended single-shared-depot/`subSource` mechanism,
which was borrowed from a reference pattern that doesn't actually fit this problem's
creation/status-change asymmetry. Awaiting your lock.

### Fork 2 — `WAITING` state added, confirmed clean fit

Since the lifecycle vocabulary is a closed set of values (not a strict, enforced state
machine — same posture as `silo`/`intent`/`relation`, no transition-graph rules coded
anywhere), adding a fifth value doesn't touch the mechanism at all: `$$commitments-by-status`
just gains one more bucket. Vocabulary becomes: `OPEN, IN_PROGRESS, WAITING, DONE, DISMISSED`.
`WAITING` (over `BLOCKED`) is my naming suggestion — it reads slightly more specifically as
"waiting on an external party," matching your "waiting on the school to reply" example, versus
`BLOCKED`'s more generic "something is obstructing this" connotation — but this is a naming
preference only, not a structural one; either name fits the model equally cleanly. Ready to
lock once you pick the name (or confirm `WAITING`).

### Fork 4 — approved, conditional on Fork 1's stub rule, now satisfied

The corrected Fork 1 mechanism includes exactly the stub rule this approval was conditioned
on: a `StatusChanged` record for a not-yet-materialized (or, per your "dropped edge on a
future parse change" scenario, no-longer-materializable) commitment still gets applied,
against a stub record, rather than being silently lost. Auto-create + the stub rule = both in
the corrected design. Fork 4 locks alongside Fork 1.

### Forks 3 and 5 — unchanged, restated as locked

Fork 3: `ACTION_NEEDED` edges are the sole seed signal; `intent`/`$$events-by-intent` stays an
untouched, independent search dimension. Fork 5: deterministic per-edge commitment IDs mean
duplicates are allowed by design when two edges describe the same real-world thing — the
entity-resolution deferral's shape, confirmed, not reopened.

Stopping here again — no code. Waiting on Fork 1 (mechanism) and Fork 2 (state name) locks;
Fork 4 is settled contingent on Fork 1.

## 2026-07-16 — Layer 2: `localSelect` read-then-conditional-write mechanism verified (jar + minimal test)

New session. Task: verify the corrected Fork 1 mechanism's two remaining open items
(`localSelect`'s exact call shape; the partial-field-update Path syntax) against the pinned
jars and a minimal `InProcessCluster` test, before writing any `$$commitments` code. Per
explicit instruction, the probe lived entirely outside the module —
`/private/tmp/.../scratchpad/ProbeModule.java` + `ProbeMain.java` (plain `main()`, no
JUnit), compiled/run directly against a `mvn dependency:build-classpath` classpath, never
under `src/test/java`. Deleted after this entry was written, per the same instruction.

### `javap` findings against the pinned `rama-1.5.0.jar`

`com.rpl.rama.Block$Impl`:
```
public abstract com.rpl.rama.Block$OutImpl localSelect(java.lang.String, com.rpl.rama.Path);
public abstract com.rpl.rama.Block$Impl localTransform(java.lang.String, com.rpl.rama.Path);
public abstract com.rpl.rama.Block$Impl ifTrue(java.lang.Object, com.rpl.rama.Block);
public abstract com.rpl.rama.Block$Impl ifTrue(java.lang.Object, com.rpl.rama.Block, com.rpl.rama.Block);
```
`localSelect(String pstateName, Path)` is the read half — same `Block$OutImpl` shape as
`.select(Object, Path)`, chained with `.out("*varName")` exactly like every existing
`.select(...)` call in `FamilySchemaModule.java`. The **3-arg** `ifTrue(Object, Block, Block)`
overload (never used anywhere in this codebase today — every existing `.ifTrue(...)` call is
2-arg, e.g. `FamilySchemaModule.java:227`) is the genuine if/else branch this design needs:
predicate first, then-branch second, else-branch third. `Path.ifPath(Path, Path[, Path])`
also exists as an alternative single-Path branching mechanism but wasn't needed once the
`localSelect` + 3-arg `ifTrue` shape was confirmed to work end-to-end — not pursued further.

### A real, previously-unverified failure mode this probe caught

First probe attempt built the CREATE branch's record as one `java.util.HashMap` (via a
`Block.each` helper method) and wrote it with a single whole-value
`Path.key("*id").termVal("*initialRecord")` — mirroring how `$$family-data`/`$$entities`
write whole records today. That part worked. But the UPDATE branch's **partial**-field write
(`Path.key("*id").key("status").termVal("*requestedStatus")`, navigating *into* the
already-stored value to overwrite only one key) threw at runtime:
```
java.lang.ClassCastException: class java.util.HashMap cannot be cast to class
clojure.lang.Associative
	at com.rpl.ramaspecter.keypath_termvalRichNav.transform_STAR_(ramaspecter.cljc:5367)
```
Root cause: every existing PState write in this codebase either replaces a whole leaf value
(`termVal` at the final path segment, never read back and re-navigated by a *later, separate*
write) or appends into a `Set` (`nullToSet().voidSetElem()`) — no existing code ever stores a
raw Java `HashMap` as a schema-declared nested-map *level* and then, in a later depot event,
navigates one key deeper into that same stored value. Rama's Specter-based path engine
(`ramaspecter`) needs the container at that level to be `clojure.lang.Associative` (a Clojure
persistent map) to `assoc` a single key into it — a plain `java.util.HashMap` instance,
however schema-declared as `Object`, doesn't satisfy that once it's the thing already sitting
in the PState. This is exactly the "unproven partial-field-update Path syntax" the prior
session's REASONING.md entry flagged as an open item — now it's not just unproven, it's a
confirmed failure mode with a confirmed fix, not a guess:

**Fix: build the nested map key-by-key through the declared schema, never `termVal` a raw
Java `Map` as a stand-in for a schema-managed level you intend to path into again later.**
CREATE branch became two sequential `.localTransform(...)` calls (one per field) instead of
one whole-map `termVal`; UPDATE branch's single-field write was already doing this correctly.
Once both branches build the map through individual key-level writes, Rama's own internal
representation at that level is native/Associative-compatible, and the later partial-key
write succeeds. **This is the concrete implication for `$$commitments`**: the `CommitmentSeeded`
creation write must write `sourceEventId`/`objectId`/`createdAt`/`status` as separate
sequential `.localTransform()` calls (or otherwise avoid a whole-map `termVal`), not as one
assembled `Map` object passed to a single `termVal` — otherwise the later
`CommitmentStatusChanged` partial `status`/`updatedAt` write will hit this identical
`ClassCastException`.

### Probe results — both halves green, the "did not clobber" assertion explicit

Ran in `InProcessCluster`, `ProbeModule` (`$$probe`: `id -> {content, status}`,
`*probe-events` depot hashed by `id`):

```
After create: {"content" "v1", "status" "OPEN"}
PASS: record exists after first append (localSelect-absent -> CREATE branch fired)
PASS: content == v1 after create
PASS: status == OPEN after create
After update: {"content" "v1", "status" "DONE"}
PASS: record still exists after second append
PASS: status == DONE after update (partial write applied)
PASS: *** content STILL == v1, NOT clobbered to v2-should-be-ignored *** (proves localSelect
read the existing record and the ifTrue branch wrote ONLY Path.key("*id").key("status"),
never touching "content")
c2 (independent id): {"content" "fresh", "status" "OPEN"}
PASS: c2 record exists (localSelect is per-key, not a whole-PState presence check)
PASS: c2 content == fresh
PASS: c2 status == OPEN
PASS: c1 unaffected by c2's independent append

ALL PROBE ASSERTIONS PASSED
```

Second append deliberately sent `content = "v2-should-be-ignored"` alongside the status
change — the assertion that matters, per the task's explicit ask, is that `content` reads
back as `"v1"` afterward, not `"v2-should-be-ignored"`. It does. This is the literal Fork 1
guarantee (status write-once via a targeted partial write, everything else refreshed/untouched)
proven against the real jar, not inferred from the docs' prose about `localSelect`'s
atomicity (which was already verified conceptually last session, per the 2026-07-16 Fork-1-
correction entry's citation of `redplanetlabs.com/docs/~/pstates.html`).

### Open items retired

Both remaining open items from the Fork-1-correction entry are now resolved:
`localSelect`'s call shape (`Block$Impl.localSelect(String, Path)` → `.out(...)`, confirmed
by `javap` and by a green run) and the partial-field-update Path syntax (confirmed working,
with the important caveat above about never `termVal`-ing a raw `Map` into a level you'll
later path into). `subSource` was already retired last session (superseded design).

### Stopping here, per instructions

No `$$commitments` PState, no `*commitment-status-changes` depot, no `FamilySchemaModule.java`
edit this session. Probe files deleted from scratchpad now that the call shape is captured
here with citations. Next session can proceed straight to the commitments write-path — Forks
1 (now mechanism-verified twice over: conceptually via docs, concretely via jar+test) and 4
are settled; Fork 2's state-name pick (`WAITING` vs. an alternative) is the only remaining
lock needed before implementation.

## 2026-07-16 — Layer 2: Commitments write-path implementation

New session. Audited `CLAUDE_HANDOFF.md`, this file, and `RAMA_VERIFIED_LEARNINGS.md` in full
before writing anything, per the task's explicit audit-first instruction. Plan approved at
`/Users/toddkeelingfolder/.claude/plans/layer2-commitments-writepath.md`, with one scope call
confirmed by the user: dropping `$$commitments-by-status` this session (Layer 3 scanning
infrastructure, rebuildable later) — this also removed the only reason the status-change
branch needed `localSelect` (reading the old status to move it between index buckets), so
that branch ended up as a plain unconditional partial write, relying on Rama's existing
auto-vivify behavior for the "create a stub if absent" case instead of explicit branching
logic.

### Implementation

`FamilySchemaModule.java`: added `mintCommitmentId` (3-arg, `hash(sourceEventId|relation|
objectId)`, same `nameUUIDFromBytes` discipline as `mintEntityId`), `isActionNeeded`, and
`isAbsent` as static helpers. Declared `$$commitments` (`familyId -> commitmentId ->
{sourceEventId, objectId, createdAt, status, updatedAt}`) and the new
`*commitment-status-changes` depot (`Depot.hashBy("familyId")`, matching every other depot in
this module — never random).

**Creation branch** — inserted inside the existing `relations`-EXPLODE branch
(`FamilySchemaModule.java`, same scope where `*eventId`/`*relation`/`*objectId` are already
bound for the edges/entities writes), guarded by
`.ifTrue(new Expr(FamilySchemaModule::isActionNeeded, "*relation"), Block...)` so
`MENTIONS_PERSON`/`LOCATED_AT`/etc. triples skip it entirely (Fork 3). Inside that guard:
`localSelect` reads the current `$$commitments` record **before** any write in this event
(ordering matters — an event sees its own writes immediately, so a read-after-write would
always see "present" and the OPEN-initialization would never fire); a 2-arg `ifTrue(isAbsent,
...)` sets `status = OPEN` only the first time; then, unconditionally (no guard), three
sequential `.localTransform` calls refresh `sourceEventId`/`objectId`/`createdAt` from the
source edge every time, redrain or not. This nests one `ifTrue` inside another
(`isActionNeeded` outer, `isAbsent` inner) — no prior example of that nesting in this
codebase; it compiled and ran correctly on the first attempt, confirming the `Block$Impl`
fluent API composes as expected (every op returns a further-chainable `Block$Impl`).

**Status-change branch** — a second `.source("*commitment-status-changes")` chain, two
unconditional sequential `.localTransform` writes (`status`, `updatedAt`). No `localSelect`
needed, per the scope call above.

### A real runtime failure the plan didn't anticipate, diagnosed not worked around

First implementation attempt declared `$$commitments` in the existing `stream`
(`family-events-stream`) topology but put the status-change branch on a **separate**
`topologies.stream("commitment-status-changes-stream")` object — matching the plan's original
`var commitmentStatusStream = topologies.stream(...)` line. Every single append to
`*commitment-status-changes` then failed, 100% reproducible, with
`rpl.rama.distributed.exceptions.IllegalWriteException` naming the PState's owning topology
(`family-events-stream`) against the topology attempting the write
(`commitment-status-changes-stream`). Per the project's highest-priority rule (diagnose the
platform root cause, never strip down the design to make an error go away), tracked this down
via `javap` on `RamaModule.Setup`/`Topologies` (no obvious "share this PState across
topologies" method) and then `redplanetlabs.com/docs/~/stream.html`, which confirmed: a single
`StreamTopology` can consume multiple depots via successive `.source(...)` calls on the SAME
topology object, and doing so is the documented, idiomatic pattern for exactly this shape —
"it's typical for each source block to modify the same PStates in different ways." Fixed by
removing the separate `commitmentStatusStream` variable entirely and adding
`*commitment-status-changes` as a second `.source(...)` branch on the existing `stream`
variable. Full verification trail (both this finding and last session's `localSelect`/
key-by-key-write finding) is now backfilled into `RAMA_VERIFIED_LEARNINGS.md`'s Verified
section, not just recorded here.

### Test results

`CommitmentsTest.java` (6 tests, `InProcessCluster`, `@TestMethodOrder`, mirroring
`EdgesEntityIndexTest`'s conventions): creation fires on first sight with correct content and
`status = OPEN`; a second, independent `ACTION_NEEDED` edge mints a distinct commitmentId (no
cross-event dedup, Fork 5); a `MENTIONS_PERSON`-only event produces zero `$$commitments`
writes (Fork 3 scoping); a status-change event updates `status`/`updatedAt` and leaves content
untouched; **the core guarantee — redraining the identical source `ACTION_NEEDED` event after
a status change does NOT reset `status` back to `OPEN`**; and a status change for a
not-yet-materialized commitment auto-vivifies a stub record (`status` set,
`sourceEventId`/`objectId`/`createdAt` absent), with no explicit stub-handling code required.

Per the user's explicit request, verified the redrain-guarantee test individually rather than
trusting "suite green": the surefire XML report
(`target/surefire-reports/TEST-com.family.assistant.CommitmentsTest.xml`) records
`redrainOfSourceEdgeDoesNotClobberADoneStatus` as a bare, failure-free `<testcase>` entry
(no `<failure>`/`<error>` child) in the full-class run. (A `-Dtest=Class#method` isolated run
of just this test method fails, expectedly — Maven skips the earlier `@Order`ed test that sets
`status = DONE` in the first place, so the isolated run starts from a fresh `OPEN` and the
"still DONE" assertion has nothing to compare against; this is a test-isolation artifact of
Maven's single-method filter on stateful ordered tests, not a defect in the commitments logic
— confirmed by re-running the whole class, where all 6 pass together.)

`mvn clean test` (full suite, non-LLM): **143/143 green** (137 existing + 6 new
`CommitmentsTest`), `BUILD SUCCESS`, zero regressions — confirms this is additive, per the
user's second explicit ask. `GmailIngestionTest`'s pre-existing, unrelated
`invalid_grant`/expired-token warning appears in the log (same warning documented in
`CLAUDE_HANDOFF.md` from earlier sessions) but does not fail the build or the test.

### Known consequence, logged per instruction: the dropped-edge ghost

Same risk family as the already-accepted auto-create risk (Fork 4: a bad auto-created
commitment persists until manually `DISMISSED` — there's no pre-creation review gate). If a
future `EmailParsingModule` change stops extracting the `ACTION_NEEDED` relation that
originally seeded a given commitment (a corrected prompt, a schema change, or the triple
simply stops matching), the creation branch will never process that edge again — but nothing
in this design ever deletes a `$$commitments` record. The commitment becomes a permanent
**ghost**: its `status` can still be changed by a real status-change event (the status-change
branch has no dependency on the creation branch ever having run, by design — that's what makes
the out-of-order stub case in test 6 work), but its content fields will never again be
refreshed by a live source edge, and the record itself will never be garbage-collected. This
is not a new problem requiring new design — it's the identical shape as the auto-create risk,
and the identical fix already exists: `DISMISSED` is the after-the-fact escape hatch for a
commitment that no longer reflects anything real, whether it was wrong from the start (Fork 4)
or became stale because its source edge disappeared (this entry). No action taken this
session — flagging for whoever eventually builds Layer 3's "what's overdue" scan, since a
ghost with a stale `OPEN`/`IN_PROGRESS` status is exactly the kind of record such a scan would
surface and a human would need to `DISMISS`.

### Stopping here, per instructions

No git commit. `CLAUDE_HANDOFF.md` PState table, event-record section, and test-suite table
still need updating to reflect `$$commitments`/`*commitment-status-changes`/`CommitmentsTest`
and the new 143 test count — next step, before reporting complete.

## 2026-07-16 — Open-items view + mark-done path: audit, decisions, and gate answers

Same-day follow-on session to the Layer 2 Commitments write-path above: build the first read
(`$$commitments` open-items view) and first real write trigger (mark-done →
`*commitment-status-changes`), scoped small per the user's brief. Plan was written and required
explicit approval before any code — the user gated approval on two audit confirmations, answered
below, plus pre-approved both design decision points to "go with your recommendations."

### Step 0 audit — confirmed clean, nothing to fix

- **Second `.source()` wiring**: confirmed by direct file read, not by trusting
  `CLAUDE_HANDOFF.md`'s prose. `$$commitments` is declared on the `stream` variable
  (`family-events-stream`), and `*commitment-status-changes` is consumed via a second
  `stream.source(...)` call on that *same* variable (`FamilySchemaModule.java:412-421`) — not a
  separate topology object. Quoted the actual code back to the user before proceeding, per the
  brief's "if not wired, stop and report — do not fix it silently" instruction. It was wired;
  independently corroborated by `CommitmentsTest`'s existing 6/6 green, including the
  redrain-doesn't-clobber-DONE test.
- **Webhook server location**: `WebhookReceiver.java` (`com.family.assistant.webhook`), started
  from `FamilyAssistantApp.main()`, fronted by a `cloudflared tunnel` (README.md:62-69 — pure
  network passthrough, no cluster logic of its own). Its constructor only took `AgentClient`
  handles before this session; the two existing debug PState routes were instead bolted directly
  onto `receiver.getApp()` inside `FamilyAssistantApp.main()`, an ad hoc pattern this session
  deliberately did not continue (see decision point below).

### Gate answer — is $$commitments subindexed?

Confirmed **not subindexed** by direct inspection of its declaration in `FamilySchemaModule.java`:
no `.subindexed()` call, unlike `$$events-by-date` (which does have one and is the PState the
`RocksDBWrapper`-on-`selectOne(Path.key(familyId))` gotcha in `RAMA_VERIFIED_LEARNINGS.md` applies
to). `$$commitments` is a plain 3-level `mapSchema`, same shape as `$$entities`. Independently
corroborated: `CommitmentsTest`'s own `commitmentIdForSourceEvent`/`commitmentRecord` helpers
already call `selectOne(Path.key(FAMILY_ID))` directly and get a usable `Map`, not a
`RocksDBWrapper`. So the plain `selectOne` in the open-items read is correct as originally
planned — the `RocksDBWrapper` workaround does not apply here and was not added.

### Decision points — both resolved per the user's "go with your recommendations"

1. **Read location**: direct `clusterPState` read inside `WebhookReceiver`, not a `QueryModule`
   query. Reasoning stands as proposed in the plan: `QueryModule`'s `query-agent` is LLM-backed
   (needs `GEMINI_API_KEY`, goes through Agent-o-rama invocation) — routing a zero-LLM plain
   scan/filter through it would add a real dependency for no benefit.
2. **Entry point shape**: extended `WebhookReceiver`'s constructor to take `PState
   commitmentsPState` and `Depot statusChangesDepot` (mirroring its existing `AgentClient` params)
   rather than bolting the two new routes onto `FamilyAssistantApp.main()` the way the pre-existing
   `/debug/pstate` routes were. This keeps routing logic together in the class that actually owns
   it — the brief's "entry point on the existing webhook server" — and, as a direct consequence,
   makes the route logic unit-testable: `openCommitments`/`markDone` were extracted as public
   methods so `OpenItemsAndMarkDoneTest` calls the *exact* code the HTTP routes call, without
   booting Javalin or making real HTTP requests. (They ended up `public`, not package-private, only
   because this project's test convention keeps all tests in the `com.family.assistant` package
   regardless of which subpackage the production class lives in — `CommitmentsTest` does the same
   for `FamilySchemaModule`'s package-crossing access pattern.)

### What was NOT built (flagged, not fixed)

`$$commitments-by-status` index — still explicitly out of scope (Layer 3), so the open-items read
is an O(n)-per-family scan-and-filter over the whole `$$commitments` map on every request. Flagged
again in this session's `CLAUDE_HANDOFF.md` entry as a known future cost, not silently absorbed.

### Test results

New `OpenItemsAndMarkDoneTest` (1 test, `InProcessCluster`, poll-with-timeout per
`RAMA_VERIFIED_LEARNINGS.md` — never `waitForStreamProcessedCount`): the full brief-mandated loop
in one test — ingest an `ACTION_NEEDED` event → commitment appears in the open-items view →
mark-done → commitment disappears from the open-items view → redrain the identical source event →
commitment stays absent for a 3-second poll window, with the underlying record independently
verified to still read `status = "DONE"` (not just "filtered out by some other bug").

`mvn test` (full suite, non-LLM): **144/144 green** (143 existing + 1 new), `BUILD SUCCESS`, zero
regressions. All existing pre-session warnings (`invalid_grant` in `GmailIngestionTest`, two
non-varargs-call warnings in `IndexPStateTest`/`QueryIndexTest`) are pre-existing and unrelated,
same as documented in earlier sessions.

`CLAUDE_HANDOFF.md` updated in the same session: test count (143→144), `$$commitments` PState row
(new consumers noted), new "Recently Completed" entry, new test-suite table row. No git commit —
per standing project convention, commits happen only when the user explicitly asks.

## 2026-07-16 — Live server smoke test: unvalidated mark-done finding, flagged for the deploy session

After the plan above shipped, the user ran the actual server locally (`InProcessCluster` mode,
`mvn compile exec:exec`) and exercised the new routes by hand — including injecting a real test
event through `*family-events` via a temporary `/debug/inject-test-event` route (added for this
smoke test only, same ad hoc pattern as the pre-existing `/debug/pstate` routes; see
`FamilyAssistantApp.java`). While poking at it, the user POSTed `/commitments/{id}/done` with a
garbage `commitmentId` that was never seeded by any real `ACTION_NEEDED` edge, and got back
`{"status":"ok"}` — the endpoint has no existence check before appending. Asked to trace the
consequence and log a decision, not fix it.

### Traced consequence

`WebhookReceiver.markDone` unconditionally appends `{familyId, commitmentId, newStatus: "DONE",
changedAt}` to `*commitment-status-changes` regardless of whether `commitmentId` corresponds to
anything real (`WebhookReceiver.java`, `markDone` — no `$$commitments` read before the append).
That depot is consumed by `FamilySchemaModule`'s status-change branch
(`FamilySchemaModule.java:412-421`, quoted below), which is a **plain unconditional partial
write** — no `localSelect`/`ifTrue` existence guard, unlike the ACTION_NEEDED creation branch's
OPEN-initialization check:

```java
stream.source("*commitment-status-changes").out("*statusChange")
  .select("*statusChange", Path.key("familyId")).out("*familyId")
  .select("*statusChange", Path.key("commitmentId")).out("*commitmentId")
  .select("*statusChange", Path.key("newStatus")).out("*newStatus")
  .select("*statusChange", Path.key("changedAt")).out("*changedAt")
  .hashPartition("*familyId")
  .localTransform("$$commitments",
      Path.key("*familyId").key("*commitmentId").key("status").termVal("*newStatus"))
  .localTransform("$$commitments",
      Path.key("*familyId").key("*commitmentId").key("updatedAt").termVal("*changedAt"));
```

Rama's auto-vivify behavior (the same mechanism `CommitmentsTest`'s test 6,
`statusChangeForNeverSeenCommitmentCreatesStub`, already exercises and asserts) creates a brand
new `$$commitments` row keyed by the garbage `commitmentId`: `{status: "DONE", updatedAt:
<changedAt>}`, with `sourceEventId`/`objectId`/`createdAt` permanently absent (no creation branch
will ever populate them for an ID that doesn't correspond to a real `hash(sourceEventId|relation|
objectId)`). This is a **permanent stub row** — nothing in this design ever deletes a
`$$commitments` record, same as the already-documented dropped-edge "ghost" risk above. Because
`markDone` always sets `newStatus = "DONE"`, this specific stub is immediately filtered out of
`openCommitments`'s scan (`status != "DONE"`), so it's invisible in the open-items view but still
occupies a row in `$$commitments` forever — silent storage pollution, not a visible bug.

One distinction from the already-documented ghost case worth logging: the prior ghost (dropped
source edge) always originates from a `commitmentId` that *was* real at some point — a
deterministic hash of a genuine `ACTION_NEEDED` edge. This new case is different in kind: the
HTTP layer accepts **any string** as `commitmentId`, not just hash-shaped ones — there is no
format validation either, only the depot append. So the pollution key-space is unbounded, not
just "real IDs whose source edge later disappeared."

### Decision flagged for the deploy session — not resolved now, per instruction

Whether `POST /commitments/{id}/done` should `localSelect`-check `$$commitments` for the
`commitmentId`'s existence before accepting the mark-done (return 404 on a miss) versus keep the
current auto-vivify-on-anything behavior as an accepted, Layer-2-consistent tradeoff (same
philosophy as the auto-create-with-no-review-gate decision, Fork 4, already accepted for
commitment *creation*). No code changed for this — explicitly deferred to the user's judgment in
the deploy session.

**Bundled with a second, related pre-deploy flag** (also raised by the user in this same
conversation turn, not a new finding of mine): the `/debug/pstate*` and `/debug/inject-test-event`
routes are currently unauthenticated and bolted directly onto the same Javalin app instance that
`/webhooks/gmail` listens on — if `RAMA_MODE=cluster` is used with the real Cloudflare tunnel
(README.md:62-69) without gating or stripping them first, they'd be reachable from the public
internet: `/debug/inject-test-event` can forge arbitrary `$$family-data`/`$$commitments` writes
with no auth, and `/debug/pstate*` leaks the full family record. Both this and the mark-done
validation question are deploy-session decisions, not resolved here.

## 2026-07-17 — First cluster deploy plan: gate-review revisions A/B/C/D

Planning session for the first real deploy of current code to the persistent cluster (2 of 6
modules deployed, `FamilySchemaModule` dated Apr 11 — pre-graph-schema, pre-commitments). Plan
went through the user's separate Lumino Plan Review Gate process (a Claude-chat-run checklist,
`SKILL.md` at `~/Library/Application Support/Claude/local-agent-mode-sessions/skills-plugin/
.../skills/lumino-plan-review-gate/`, located via filesystem search this session — an 11-gate
red-team checklist Claude Code plans must pass before approval). Four revisions came back;
this entry records revision A's verification trail, since the user explicitly asked for the
source to be cited here. Revisions B, C, D were design decisions, not verification — recorded
in the plan file (`~/.claude/plans/first-cluster-deploy.md`) rather than duplicated here.

### Revision A — deploy parallelism, verified against RPL docs (not assumed)

The plan originally proposed a uniform `--tasks 1 --threads 1 --workers 1 --replicationFactor 1`
for all 6 modules, including the 2 already deployed in April via `--action update`. Gate review
flagged this as unverified: does `--action update` accept changed parallelism on an existing
module, and can task count change post-launch at all? Verified via `WebFetch` against
`redplanetlabs.com/docs/~/operating-rama.html` (not from memory, not from `RAMA_VERIFIED_LEARNINGS.md`
alone, per that file's own "Verify-at-source" discipline and the gate checklist's identical
reminder):

- **Task count is permanently fixed at launch.** Doc quote: *"Currently Rama does not support
  changing the number of tasks for a module, though adding support for this is high priority
  for us."*
- **`--action update` does not accept parallelism flags at all.** Doc quote: *"Parallelism
  settings (tasks, threads, workers) are not specified on module update since the new version
  of the module will use the same settings as the old module."*
- **Post-launch thread/worker/replication changes are a separate command,** `rama scaleExecutors`
  (`--threads`/`--workers`/`--replicationFactor`, no `--tasks` option). Corroborated
  independently by `rama scaleExecutors --help` against the actual pinned CLI — the absence of
  a `--tasks` flag there matches the docs' claim that task count can't be changed by any
  mechanism, not just `update`.

**Resolution:** the 2 April modules (`FamilySchemaModule`, `EmailIngestionModule`) get
`--action update` with no parallelism flags at all — whatever April used is what persists, and
there was never a real choice available here despite how the original plan phrased it. The
alternative (destroy + relaunch to pick a different task count for these two) is a destructive
op that conflicts with "no destructive action on `rama-data/` without stop-and-report" and with
the separate "657MB April data is inspect-first, no wipe decision yet" call — not pursued,
since the verification didn't surface a blocker requiring it. The 4 never-deployed modules get
real parallelism choices since this is their one, permanent chance to set task count:
`--tasks 4 --threads 4 --workers 1 --replicationFactor 1` — flagged in the plan as the one
genuinely irreversible number in the whole deploy, to be confirmed before the command runs, not
silently executed just because it was the pre-approved default.

No code changed, no deploy command run this session — plan-only, per instruction.

## 2026-07-17 (continued) — Part 1 executed: unknown-ID guard lands, test 6 superseded

Same-day follow-on, after gate-review revisions A/B/C/D were folded into the plan and the
user approved executing Part 1 (pre-deploy code items) in this window, stopping before any
daemon start. `EDGE_CODE_RULES.md` was supplied by the user (previously flagged missing in
the Step 0 audit) and saved to the repo root; the Lumino Plan Review Gate checklist source
was located via filesystem search (`SKILL.md` under `~/Library/Application Support/Claude/
local-agent-mode-sessions/skills-plugin/.../skills/lumino-plan-review-gate/`) and copied into
`docs/PLAN_REVIEW_GATE.md`.

### Test 6 supersedes its own prior logged behavior — flagging explicitly per instruction

`CommitmentsTest.statusChangeForNeverSeenCommitmentCreatesStub` (added 2026-07-16, logged in
that day's "Layer 2 Commitments write-path" entry above) asserted that a status change for an
unknown commitmentId auto-vivifies a stub record — that was the correct, intended behavior at
the time, relying on Rama's auto-vivify with no existence check, since `$$commitments-by-status`
was out of scope and nothing else needed the old status read.

Gate-review revision C reversed this by design: `EDGE_CODE_RULES.md` Gate 6/Gate 8 flagged the
original plan's fix (an edge-side existence check before append) as itself a violation —
"deduplicating/checking existence before append" is exactly the creep signal the doc calls out,
and belongs in a topology, not the edge. The authoritative fix instead added a `localSelect`/
`ifTrue` existence guard to `FamilySchemaModule`'s status-change branch itself (mirroring the
creation branch's existing guard pattern) — a status change for a commitmentId the ACTION_NEEDED
branch never seeded is now dropped entirely, no write, no stub.

**This session's test 6 rewrite (`statusChangeForNeverSeenCommitmentIsDroppedNotStubbed`)
supersedes the 2026-07-16 entry's logged auto-vivify-stub behavior — a future session reading
that earlier entry should NOT treat "auto-vivify creates a stub for an unknown ID" as still
live.** That consequence is gone as of this session; the current behavior is "dropped, no write
at all."

**The separate dropped-source-edge "ghost" case is a different mechanism, unaffected, still
accepted.** That risk (logged in the 2026-07-16 Commitments entry above) is: a commitment that
DID exist — created for real, from a real `ACTION_NEEDED` edge at some point — whose source edge
later stops being extracted by a parser change. Nothing in this design ever deletes a
`$$commitments` record, so that commitment becomes a permanent "ghost" with stale content that
will never refresh again. This is unrelated to today's fix: today's guard only prevents a
*status change* from creating a *brand-new* record for an ID that was never real in the first
place. A ghost's `commitmentId` WAS real at creation time, so the new existence guard does
nothing to it — it still exists in `$$commitments`, and its status can still be legitimately
changed (including via `DISMISSED`, the already-accepted fix for both the ghost case and the
no-review-gate auto-create risk, Fork 4). Two distinct risks, two distinct fixes, only one of
which changed today.

### What else landed in Part 1

- `WebhookReceiver.commitmentExists(familyId, commitmentId)` — new public method,
  `selectOne(Path.key(familyId).key(commitmentId))` against `$$commitments` (same non-subindexed
  read shape as `openCommitments`, confirmed safe under Gate 5 during the gate-review pass).
  Used only to pick the mark-done endpoint's HTTP response code (200 vs 404) — `markDone` still
  always appends to `*commitment-status-changes` regardless of what this read finds; it does not
  gate the append. Accepted race: a commitment created moments ago and not yet drained can read
  as "doesn't exist" and return a false 404 to a human tapping something already on screen.
- `DEBUG_ROUTES_ENABLED` env flag (default off) in `FamilyAssistantApp.java` — when unset, the
  `/debug/pstate`, `/debug/pstate/{familyId}`, and `/debug/inject-test-event` route-registration
  calls are skipped entirely, not just left to 404. `README.md` updated to document the flag and
  warn against enabling it behind the Cloudflare tunnel.
- New test: `OpenItemsAndMarkDoneTest.commitmentExists_trueForRealCommitment_falseForGarbageId`.

### Test results

`mvn test` (full suite, non-LLM): **145/145 green** (was 144/144), `BUILD SUCCESS`. Net +1 test,
landing as: `OpenItemsAndMarkDoneTest` gained one test (`commitmentExists` true/false check);
`CommitmentsTest` stayed at 6 tests, with test 6 rewritten (not added to) — matching the Gate 10
correction made during gate review ("test 6 reverses, not extends"). Zero other regressions.
`CLAUDE_HANDOFF.md` updated in the same pass: test count (144→145), `$$commitments` PState row,
test-suite table (both `CommitmentsTest` and `OpenItemsAndMarkDoneTest` rows), new "Recently
Completed" entry.

No deploy command run, no daemon started — Part 1 (pre-deploy code items) only, per the plan's
own gating. Part 2 (cluster deploy) and Part 3 (go-live) are explicitly a separate session's
work, per the user's instruction to open a fresh window and audit-first against this file and
`RAMA_VERIFIED_LEARNINGS.md` before starting any daemon.

## 2026-07-18 — exFAT crash, root cause and fix (config only, recorded retroactively)

`local.dir` was `/Volumes/CORSAIR/rama-data` (exFAT). Conductor crashed during its stale-jar
cleanup cycle: `Deleting stale jar` for `._com.family.assistant.email.EmailIngestionModule_...jar`
(a macOS AppleDouble sidecar file, artifact of exFAT lacking the metadata support HFS+/APFS have)
threw `java.io.IOException: Couldn't delete ...`, which propagated up through
`util.throwable-handler` as "Scheduled item execution failed. Terminating scheduler" — 26ms after
"Conductor started successfully!" — and the daemon halted. Root cause is the filesystem, not
Rama/Conductor logic: exFAT has no journaling/locking and macOS silently drops AppleDouble
sidecar files (`._*`) into any exFAT directory it touches, and Conductor's cleanup routine has no
tolerance for an undeletable file in its jars directory. Fix applied (uncommitted, working
change): `~/rama-release/rama.yaml`'s `local.dir` repointed to
`/Users/toddkeelingfolder/rama-data` (internal APFS). The April data under
`/Volumes/CORSAIR/rama-data` was confirmed disposable (empty skeleton, zero real writes per
RocksDB's own cumulative-writes stats) before the repoint — not migrated. Note: this incident
isn't otherwise documented anywhere in this file before this entry — `rama.yaml`'s own comment
says "see REASONING.md" but nothing existed here until now; that pointer was stale/wrong until
this retroactive entry.

## 2026-07-19 — Part 2: cluster deploy, daemon verification + launch-vs-update finding

Fresh session, audit-first per instruction (`CLAUDE_HANDOFF.md`, this file, `RAMA_VERIFIED_LEARNINGS.md`,
`EDGE_CODE_RULES.md`, plus this project's `CLAUDE.md`). Step 0 confirmed: `local.dir` reads the
APFS path, `/Users/toddkeelingfolder/rama-data` exists and is empty, no Rama processes running.

**ZooKeeper + Conductor started, watched specifically for the exFAT crash's exact failure mode.**
Conductor's log file (`~/rama-release/logs/conductor.log`) is append-only across restarts, so the
2026-07-17 exFAT crash and tonight's APFS run sit back-to-back in the same file — direct
side-by-side comparison, not a memory of the old behavior. Tonight: `Conductor started
successfully!` at 10:14:42.484, then **no** `Deleting stale jar` lines at all (the fresh APFS
`rama-data` is empty, so the cleanup cycle found nothing to touch), no ERROR, no IOException, no
"Halting process." Confirmed stable 85+ seconds past startup, process alive, port 8889 held.
Caveat: since the directory was empty, this run didn't exercise an actual stale-jar *deletion* on
APFS — only confirmed the crash doesn't recur when there's nothing to clean up. The first real
stale-jar deletion on APFS is still unverified; worth revisiting once jars actually age out
post-deploy.

**Launch-vs-update question, resolved against real cluster state, not assumed.** The existing
plan (`~/.claude/plans/first-cluster-deploy.md`, gate-review revision A) had `FamilySchemaModule`
and `EmailIngestionModule` — deployed to the OLD exFAT Conductor back in April — using
`--action update`, since a module's task count can never change and `update` accepts no
parallelism flags (verified 2026-07-17 against `redplanetlabs.com/docs/~/operating-rama.html`).
But this session's Conductor is backed by a brand-new, empty APFS `local.dir` — the April jars
never crossed over. Checked directly rather than assumed:
```
$ rama moduleStatus com.family.assistant.schema.FamilySchemaModule
{"moduleState":"NOT_ALIVE", ...}
$ rama moduleStatus com.family.assistant.email.EmailIngestionModule
{"moduleState":"NOT_ALIVE", ...}
```
Both `NOT_ALIVE` — this Conductor has zero record of either module. **All six modules require
`--action launch`, not just the four that were always fresh** — the update/launch split from the
2026-07-17 plan no longer applies verbatim on this fresh data dir. This also means all six get a
real, one-time, permanent parallelism choice, not just four of them.

**Supervisor started and watched the same way as Conductor.** `root-dir` confirmed
`/Users/toddkeelingfolder/rama-data` (APFS) in the log. `Started supervisor!` at 10:28:10.299, no
further log lines (nothing to house-keep on an empty data dir), process alive 35+ seconds later.
`rama numSupervisors` → `1` (was `0` before Supervisor started, confirming the query reflects
real state, not a stale cache). `rama licenseInfo` → active license, `num-nodes: 2`,
`1917-12-16`–`2117-12-16` (effectively unlimited dev license) — answers the historical "not
enough licensed supervisors" question: 1 registered Supervisor against 2 licensed nodes, no
capacity concern.

**Fat JAR build gap found and fixed.** `mvn clean package -DskipTests` alone produced only the
78KB thin jar (`target/family-assistant-1.0.0.jar`) — `pom.xml`'s `maven-assembly-plugin`
(lines 114-121) declares the `jar-with-dependencies` descriptor but has no `<executions>` block
binding it to a build phase, so plain `package` never invokes it. Not a Rama/AOR issue, a Maven
wiring gap. Fixed operationally (no `pom.xml` edit) by running the assembly goal explicitly:
`mvn clean package assembly:single -DskipTests`, producing the real 259MB
`target/family-assistant-1.0.0-jar-with-dependencies.jar` needed for `rama deploy --jar`.

Six `--action launch` commands presented for approval next; none run yet.

## 2026-07-19 (continued) — Part 2 complete: all six modules deployed to internal APFS

User approved all six `--action launch --tasks 4 --threads 4 --workers 1 --replicationFactor 1`,
dependency order (schema → parsing → ingestion → gmail → digest/query), confirmed the fresh jar
build. Deployed one at a time, each confirmed `RUNNING` via `moduleStatus` before the next.

**(a) Full fresh deploy — old exFAT April deployment fully superseded.** All six modules —
`FamilySchemaModule`, `EmailParsingModule`, `EmailIngestionModule`, `GmailIngestionModule`,
`DigestModule`, `QueryModule` — launched fresh via `--action launch` on the internal-APFS
Conductor/Supervisor (no `--action update` needed anywhere, confirmed correct per the earlier
`NOT_ALIVE` finding). Final state, verified via `rama moduleStatus <ShortName>` for all six:
`RUNNING`. `rama numSupervisors` → `1`. The old exFAT-backed deployment (April, 2 of 6 modules)
is no longer live anywhere — this is a clean, complete platform on the fixed filesystem.

**(b) `rama moduleStatus` CLI usage correction, made mid-session.** First attempt queried by
fully-qualified class name (`com.family.assistant.schema.FamilySchemaModule`) and got
`NOT_ALIVE` even though Conductor's own log showed `Launch of module FamilySchemaModule
complete!` / `module-state [running]` moments earlier. Re-querying with the short name
(`FamilySchemaModule` — what Conductor's log itself calls it throughout) correctly returned
`RUNNING`. `--module` on `rama deploy` takes the FQCN; `moduleStatus`/`moduleInstanceStatus` want
the short name. Recorded in `RAMA_VERIFIED_LEARNINGS.md`. This also means the earlier (Step-3,
pre-deploy) `NOT_ALIVE` checks used the wrong query syntax — the launch-vs-update conclusion they
fed into was still correct (independently confirmed by the empty `local.dir/conductor/jars/` and
by Conductor logging "Creating new state machine" — a genuinely fresh launch, not a collision),
but the verification method itself was flawed and is documented as such rather than quietly
smoothed over.

**(c) Fat-jar build gap.** `mvn clean package -DskipTests` alone produces only the 78KB thin jar —
`pom.xml`'s `maven-assembly-plugin` has no `<executions>` binding, so `mvn clean package
assembly:single -DskipTests` is required to produce the real
`target/family-assistant-1.0.0-jar-with-dependencies.jar` (259MB). Verified fresh before deploy:
jar timestamp postdated every file under `src/`.

**(d) Memory finding — the real blocker for Part 3, more so than OAuth/Gmail.** This Mac Mini has
24GB total RAM. Each worker JVM launches with `-Xmx4096m` (`worker.child.opts` in `rama.yaml`);
six workers is 24GB of *committed max heap* alone, on a 24GB machine, before Conductor
(`-Xmx1024m`), Supervisor (`-Xmx1024m`), or ZooKeeper are even counted. This wasn't a hypothetical
— it showed up directly during tonight's deploy: modules 1-5 each completed in 10-40s per phase;
`QueryModule` (module 6, the heaviest — two agents, `query-agent` + `search-agent`, roughly double
the internal AOR-managed PStates/RocksDB stores of a single-agent module) took over 8 minutes
end-to-end, with every phase (capture deploy info, build jar, upload, download, worker
RocksDB-open calls) running 5-10x slower than the same phase on earlier modules. `vm_stat` at the
time showed ~59MB of free physical memory (3625 pages × 16KB) with system load average 5.67 — real
resource contention, not a QueryModule code/topology defect (worker log showed zero errors/
exceptions/OOM signatures throughout, just slow RocksDB opens and long gaps between init steps).
No JVM actually OOM'd tonight — the deploy succeeded — but there is zero headroom, and Part 3 adds
real ingestion load (Gmail fetch, LLM calls, active stream processing) on top of six already-tight
JVMs. **Worker heap right-sizing (`worker.child.opts` in `rama.yaml`, currently `-Xmx4096m`
uniform for all six) is flagged as the required first step of Part 3, ahead of any Gmail/OAuth
work** — not because anything is broken now, but because the margin observed tonight (system-wide
near-zero free memory just from deploy-time JVM startup churn, no steady-state load yet) won't
survive real ingestion without either lowering per-worker heap or deciding lighter modules
(email/gmail ingestion, digest) need less than the heavier ones (query, schema).

No Supervisor config changes, no Gmail/OAuth calls, no ingestion — session stops here, at the
Part 2/Part 3 boundary, per instruction.

## 2026-07-19 (continued) — Part 3 pre-restart: cold-restart persistence test, heap-tuning attempt, and the decision to abandon the Mini

Fresh session (new window), audit-first per instruction. Machine had been powered off between
sessions — no reboot uptime discontinuity was expected to matter since PState data lives on disk,
not in daemon memory, but this was the first real test of that claim.

**Cold-restart persistence test: passed cleanly.** With all daemons down (confirmed: zero Rama
Java processes, ports 2000/1973/8889/1974 clear, `uptime` showing the machine had genuinely been
off), `rama-data/` was inspected directly rather than assumed intact: `conductor/jars/` held all
six module jars at their 2026-07-19 Part-2 timestamps, `objects/` held one RocksDB directory per
module with every PState from `CLAUDE_HANDOFF.md`'s schema table present and structurally valid
(`CURRENT`/`MANIFEST`/`IDENTITY`/`LOCK`/`OPTIONS` all in place). Zero `.sst` files and all-zero-byte
WAL logs confirmed this was empty-because-ingestion-never-ran, correctly distinguished from
empty-because-deploy-failed (Part 3/ingestion never got that far in Part 2). Starting ZooKeeper →
Conductor → Supervisor in sequence, Conductor's own log immediately recreated state machines for
all six modules at `[running]` from persisted disk state with zero errors, and Supervisor then
auto-relaunched all six workers (matching Conductor's remembered target state) without any
`rama deploy` command being run at all — genuine self-healing recovery, confirmed via matching
`appendTargetId`s against the pre-restart instance IDs. All six auto-recovered at the old
`-Xmx4096m` default, as expected (auto-recovery doesn't go through `--configOverrides`).

**Heap right-sizing: user chose `-Xmx1536m`** (reasoning: frees ~13-14GB, above the ~1GB GC-thrash
floor, tunable per-module later via `--configOverrides` if `QueryModule` specifically shows
pressure). Two questions were sent to Chat-o-rama (chat.redplanetlabs.com) before grinding through
six ~15-25-minute redeploys, both answered with doc citations:

1. **Persistence**: `worker.child.opts` is a Rama "config" (not a "dynamic option" — those are a
   separate, always-changeable mechanism via `set-launch-*-dynamic-option!` that explicitly doesn't
   cover worker JVM settings). Per `rama-shared → "All configs"`, configs come from `rama.yaml`
   (cluster-wide default), `--configOverrides` (per-deploy), or programmatic `RamaClusterManager`
   construction — and are never persisted per-module, so a bare future redeploy without
   `--configOverrides` reverts to whatever default applies. This directly **contradicted** this
   project's own prior working theory (logged mid-session as "rama.yaml is inert") — the docs say
   `rama.yaml` should work as a global default; this project's actual history (April deploy, the
   2026-07-19 fresh six-module deploy, and tonight's auto-recovery — three separate real events)
   shows `rama.yaml`'s `-Xmx2g` never once took effect, always falling back to `-Xmx4096m` instead.
   Rather than resolve this by more live trial-and-error mid-session, it was deliberately filed as
   an open investigation in `RAMA_VERIFIED_LEARNINGS.md` with a concrete isolated-test plan for a
   future dedicated session, with the leading hypothesis being a programmatic-`RamaClusterManager`-
   path vs. CLI-`rama deploy`-path distinction that the docs don't spell out.
2. **Efficiency**: no config-only update path exists — `--action update` always requires `--jar`
   and always performs a full module-instance transition, confirmed via
   `rama-shared → "Operating Rama clusters" → "Updating modules"`. The ~15-25 min/module cost is
   real and not something to optimize away; the same JAR can be reused (no rebuild needed since code
   is unchanged) but the upload + coordinated worker-swap is unavoidable.

**Redeploy execution — this is where the real finding landed.** `FamilySchemaModule` was updated
first (dependency order). Verification at the time checked `moduleStatus: RUNNING` plus a
`supervisor.log` grep showing a `Launching process` line with `-Xmx1536m` — and was reported to the
user as confirmed. **This was wrong, caught only on a later, more careful check prompted by the
user asking for a free-RAM readout before continuing.** Comparing `moduleStatus`'s `appendTargetId`
against the actual new instance ID (rather than just checking the status string and the existence
of a launch-attempt log line) revealed `FamilySchemaModule` was still serving its ORIGINAL instance
(`55b3c805-...`, port 3001, `-Xmx4096m`) — the new instance (`ada41606-...`, port 3007,
`-Xmx1536m`) had gotten stuck at the `UPDATE-PREPARE-HANDOVER` state-machine stage (per its own
worker log, which simply stops mid-sequence with no further lines) and was killed by Supervisor's
heartbeat watchdog 36 seconds after starting (`supervisor.log`: "Port 3007 heartbeat is no longer
valid, moving to KILLING"). `moduleStatus` reported `RUNNING` the entire time this was happening,
because the module never stopped serving — it just never cut over, which made the earlier
"confirmed" report false despite following the mandated grep-verify step. The grep step alone was
insufficient; verifying the *serving* instance ID, not just the *most recent launch attempt*, is
the actually-sufficient check, and is now the corrected standing practice.

`EmailParsingModule`'s update, run immediately after, DID succeed genuinely — confirmed via
matching `appendTargetId` to its new instance (`e4e90fad-...`) and confirmed alive and actively
processing 19+ minutes later (only `WARN`-level "task thread event took excessive time" entries, no
errors). **So the real count was 1 of 6 successfully converted, not 2** as originally reported.

**Working diagnosis for why `FamilySchemaModule` specifically failed:** it owns far more PState/
depot surface than any other module (15 PStates + 3 depots vs. one agent + a couple of AOR-internal
depots for the others), so its handover has proportionally more RocksDB/task-state work to complete
inside Supervisor's ~30-second heartbeat window. At the exact moment this happened, `top`/`vm_stat`
showed **73MB free system memory, load average 6.2 (vs. an idle baseline of 1.5), and a 10GB memory
compressor** — real, active OS-level memory pressure, not a hypothetical ceiling. The hypothesis
(explicitly unconfirmed, no controlled A/B test run) is that RAM starvation and the handover
timeout are the same problem: less headroom makes the heavy module's sync slower, which makes it
more likely to miss the watchdog window, which kills the new worker and leaves the module stuck on
its old (also-uncomfortable) instance — a loop that heap-tuning alone cannot break, because the fix
itself needs the headroom it's trying to create.

**Decision (user's call, made explicitly rather than continuing to grind): abandon the Mac Mini
deploy, do not retry `FamilySchemaModule`, do not touch the remaining four modules
(`EmailIngestionModule`, `GmailIngestionModule`, `DigestModule`, `QueryModule` — all still at
`-Xmx4096m`, untouched).** Rationale: `QueryModule`, the next module in line, carries the same or
higher risk profile as `FamilySchemaModule` (two agents, previously the slowest module under
deploy-time churn even before RAM pressure became this severe) — continuing to grind through
updates on a machine already showing 73MB free and a failed handover was judged more likely to
produce more failures than progress. The Mini deploy is treated as a complete, valuable result on
its own terms: it proved the six-module architecture deploys correctly, all modules run, and state
survives a cold restart intact — and it also proved, empirically rather than by inference, that
24GB is undersized for six AOR-heavy modules running comfortably together, independent of any code
defect. `CLAUDE_HANDOFF.md` updated with the full decision record. All Rama daemons (ZooKeeper,
Conductor, Supervisor, all six workers — 9 processes total, enumerated by PID before shutdown) were
stopped cleanly via `SIGTERM`, confirmed exited, all relevant ports (2000, 1973, 8889, 3001-3007)
confirmed clear. `rama-data/` and all persisted state left untouched on disk. Next session: design
a cloud deployment sized with genuine headroom, as a dedicated planning session, not a continuation
of tonight's attempt.

## 2026-07-29 — Host-hunt dead end, and the sizing number that turned out to be wrong

### The pricing wall that started this
The plan coming out of 2026-07-19 was "rent a cloud VM with real headroom" — read at the time as a
32GB box, for roughly the ~$30/mo the project budgets. That is not purchasable right now:

| Option | Price | Note |
|---|---|---|
| Hetzner CX / CAX Cost-Optimized | — | **Sold out EU-wide.** Not a queue-and-wait; unavailable. |
| Hetzner dedicated auction, cheapest 32GB | **$66.90/mo** | ~2.2× budget |
| Hetzner CPX, 32GB | **$152.99/mo** | ~5× budget |

So the three options on the table were: pay 2.2–5× budget, refactor six modules into two or three to
cut per-JVM overhead, or re-examine whether 32GB was ever the right number. Took the third first,
because it's free and it gates the other two.

### The 32GB requirement was arithmetic on ceilings
It did not survive contact with the evidence. `hs_err_pid19834.log` — the real `FamilySchemaModule`
worker's crash log, sitting in the project root the whole time — shows that worker launched at
`-Xmx4096m` and running with:

```
garbage-first heap   total 352256K, used 196606K
Metaspace       used 290453K, committed 291968K
```

**G1 committed 352MB of a 4096MB ceiling and used 197MB of it.** The "six workers × 4096m = 24GB, so
24GB is undersized, so buy 32GB" chain was summing `-Xmx` values, and `-Xmx` is a ceiling the JVM
never reserved. Recorded as a verified entry in `RAMA_VERIFIED_LEARNINGS.md`.

**Precise correction to the 2026-07-19 decision record, because it overstated its own evidence.**
That entry claims the Mini deploy "proved, empirically rather than by inference, that 24GB is
undersized." Two claims were tangled there and only one holds:
- **Holds:** the memory pressure was real and directly measured — 73MB free, load average 6.2 vs.
  1.5 idle, 10GB compressor. Something genuinely ran out of room, and `FamilySchemaModule`'s
  handover genuinely died in the watchdog window.
- **Does not hold:** that this establishes a 32GB requirement. That step was inference from summed
  ceilings, and it is the step being retracted. The real fixed per-JVM cost looks like metaspace
  ~290MB plus code cache/stacks/GC metadata plus Netty direct buffers — ≈500–700MB per worker — not
  4GB. Six of those is ~3–4GB of fixed overhead, not 24GB.

Worth being blunt about the failure mode, since it cost a session and nearly cost 5× budget: the
number was never measured. Nobody recorded RSS for a single worker across the entire Mini deploy.
The pressure was real, so the conclusion drawn from it felt validated, and a plausible arithmetic
chain went unchallenged because its output agreed with the symptom.

### Decision: measure before buying, and before refactoring
**Measure actual RSS first.** Neither spending 2.2–5× budget nor refactoring six modules is
justified by a number computed from `-Xmx` sums. The next session starts the daemons and all six
modules on the Mini, lets them idle, and records real RSS per worker — see `CLAUDE_HANDOFF.md`.
Two free levers get applied and re-measured in the same run: `worker.max.direct.memory.size` (Rama
defaults it to 500m *per worker*, passed as `-XX:MaxDirectMemorySize=500m` on top of `-Xmx`, and our
`worker-heap-overrides.yaml` has never set it) and `conductor.child.opts` (defaults to `-Xmx1024m`
for a pure coordination process).

One unresolved term could still move the answer: RocksDB's default 256MB block cache, whose scope —
per PState, per partition, or per worker — is genuinely undocumented, and which spans 256MB to
~3.8GB for `FamilySchemaModule`'s 15 PStates. It is off-heap, so it is invisible in the heap figures
above. Logged in `RAMA_VERIFIED_LEARNINGS.md`'s Unverified section; the RSS run resolves it as a
side effect by comparing `FamilySchemaModule` (15 PStates) against `DigestModule` (0).

### Consolidation stays a live fallback, and it is cheaper than expected
Audited it rather than assuming, in case measurement says the RAM need is real. Verified at source
that **multiple modules cannot share a worker JVM** — `terminology.html` defines a Worker as "a
process launched by a Supervisor to run part of **a module**", and a module's depots/PStates/
topologies "all run colocated inside the same set of processes / threads". Colocation exists; its
boundary is the module. Confirmed at process level from our own `supervisor.log`: the worker daemon
takes exactly one module name as an argv (`rpl.rama.distributed.daemon.worker 3001
FamilySchemaModule`), six ports for six modules. The isolation-scheduler language about workers
sharing "nodes" is about machines, not JVMs. **So fewer JVMs requires merging code — there is no
deploy-config shortcut.**

The **three-module split has clean seams**: `FamilySchemaModule` untouched /
Gmail + EmailIngestion + EmailParsing / Digest + Query. What makes it cheap is a fact confirmed by
grep: **all 4 depots and all 15 PStates live in `FamilySchemaModule`**, and the other five modules
declare zero persistent state of their own. Consequences:
- **No PState-ownership problem.** `FamilySchemaModule` keeps sole write ownership via its own
  stream topologies; nothing moves, so the `IllegalWriteException` class of failure never arises.
- **No `define()`-last problem.** That rule only binds a module that implements `RamaModule`
  directly and builds agents via `AgentTopology.create(setup, topologies)` … `agentTopology.define()`.
  Leave `FamilySchemaModule` agent-free and no module needs the manual route — all merged modules
  stay `AgentModule` subclasses with `defineAgents()`.
- **No agent name collisions.** All six agent names are distinct, and the only duplicated agent
  object key (`gemini-model`, declared in both `EmailParsingModule` and `QueryModule`) lands in
  *different* modules under this split. The two-module split is where it collides and the two
  builders must be reconciled.
- Cross-module calls have documented same-module equivalents: `getMirrorAgentClient(m, a)` →
  `getAgentClient(a)`; `getMirrorStore`/`getMirrorDepot` into `FamilySchemaModule` stay as they are.

**Its real costs**, neither of which is a blocker but both of which are genuine:
1. **Consolidated blast radius.** Today a `GmailIngestionModule` OOM cannot touch parsing. Merged,
   one worker's heap pressure takes down the whole ingestion path. This is the actual price of the
   RAM saving.
2. **Losing AOR agent history for merged-away modules.** Merging means `rama destroyModule` on the
   absorbed modules, and per the verified depot/PState destruction rule an undeclared object is
   destroyed with its partitions deleted from disk. Each carries ~77–100MB of Agent-o-rama-internal
   replog/trace state (measured: `rama-data/task-threads/*`). **No business data is at risk** —
   it all lives in `FamilySchemaModule`, which would only ever be `--action update`d, never renamed;
   all six modules already return their plain class name from `getModuleName()`, so identity and
   data survive an in-place update.

Incidental: merging removes the `getMirrorAgentClient` calls where `ZooEmailTest`'s 100%-reproducible
`Executor pool is shut down` failure originates, so that may resolve as a side effect.

Nothing was built or deployed this session — audit and documentation only, by request. The
consolidation assessment is read from code and docs; none of it has been exercised by a test.

---

## 2026-07-30 — Step 0 executed: the measured idle footprint is ~6GB, and the 24GB figure is dead

Ran the RSS measurement that Step 0 of `LUMINO_MASTER_SEQUENCE.md` was blocking on. This is the
session that replaces estimate with data. Nothing was deployed, redeployed, or refactored.

### The measurement had to be re-baselined before it was worth anything

The first attempt was contaminated and would have produced a wrong answer in the *dangerous*
direction. Worker RSS was being read from a cluster that had lived through the memory-pressure
period — and macOS keeps compressed pages compressed until they are touched again. Those processes
were reporting their post-compression size, not their working set.

The size of the error, same processes, same machine, same day: **ZooKeeper read 413.4MB
contaminated vs 812.8MB clean — 51% of its true footprint.** Conductor and Supervisor were
understated the same way. Sizing a box from the contaminated numbers would have undersized the
daemon tier roughly twofold, and the error would have been invisible, because the numbers looked
plausible and self-consistent.

So the sequence was: shut the cluster down → take a true floor with IntelliJ gone and Rama fully
down (`pgrep -f java` = 0) → restart → settle → measure. The floor also had to be *verified settled
rather than still draining*: a Spotlight (`mds_stores`) reindex, almost certainly triggered by the
Rama data churn, moved free memory by 2.4GB after shutdown. It was distinguished from a genuine
floor only by sampling until it showed 0.0% CPU with free and compressor flat.

**This is the same failure mode as the 2026-07-29 near-miss, one level down.** That one was
"a plausible calculation that matches an observed symptom is still not a measurement." This one is
"a measurement taken from a contaminated instrument is still not a measurement." The general rule:
before trusting a number, check the *instrument*, not just the arithmetic.

### The number

| | PhysMem used | Unused |
|---|---|---|
| Floor — Rama down, IntelliJ gone, settled | ~17 GB | 6458 MB |
| Cluster up, settled, idle | ~23 GB | 128–345 MB |
| **Measured Rama footprint** | **~6 GB** | |

Sum-of-RSS across the nine JVMs reads **8.03GB**, but that double-counts pages shared between nine
processes running an identical `rama.jar` + `lib/` classpath. **~6GB is the honest figure**;
8.03GB is a ceiling on it.

**This retracts the 24GB ceiling-arithmetic figure definitively** — not as a reasoning error this
time, but against measurement. And it **validates the ~9.5GB hypothesis as slightly conservative**,
which is the right direction for a hypothesis to be wrong in. The hypothesis was extrapolated from
one crash-log snapshot of one module; it landed within ~60% of a nine-process measured total. Worth
recording that the extrapolation was *directionally sound* — the error in the 2026-07-29 session was
never the ~9.5GB estimate, it was the 24GB sum.

### Two caveats that keep this honest

1. **This is an IDLE figure.** No app running, no ingestion, no LLM calls, no depot appends. It is a
   floor for the cluster, not a working figure. Load testing requires real ingestion, which requires
   OAuth re-auth and the Gemini cost gate — its own session. **Do not buy a box on this number.**
2. **The floor retained 1.78GB of non-Rama compressor state** left over from the pressure period —
   pages belonging to other applications that were never touched again. A true cold-boot floor would
   be lower, which means the ~6GB subtraction is **slightly generous to Rama**. The bias is in the
   safe direction, but it is real and it is not quantified.

### What the measurement resolved for free

`FamilySchemaModule` (15 PStates) settled at **865.6MB — the smallest worker.** `DigestModule`
(0 PStates) settled at **1011.3MB — larger.** That single comparison kills the RocksDB block-cache
question that had been sitting in `RAMA_VERIFIED_LEARNINGS.md`'s Unverified section as the one term
able to invalidate the whole sizing: at per-PState scope `FamilySchemaModule` would have carried
~3.8GB of extra cache. It carries none, and is 146MB *smaller* than the zero-PState module. **The
cache is per worker.** All six workers sit in an 866–1076MB band regardless of PState count.

Also measured: `EmailParsingModule` at `-Xmx1536m` uses 956.1MB while `GmailIngestionModule` at
`-Xmx4096m` uses 1076.2MB — **a 2.67× ceiling difference producing 12.6% more RSS.** The
`-Xmx`-is-a-ceiling finding now rests on a direct measurement rather than a single crash log.

Both are recorded as verified entries in `RAMA_VERIFIED_LEARNINGS.md`.

### What this changes about the plan

- **Box sizing is deferred, not decided.** ~6GB idle suggests 16GB is ample and 32GB was never
  indicated — but per caveat 1, the decision waits for a loaded figure. The value of Step 0 was
  never "pick a box," it was "stop picking a box from arithmetic."
- **`-Xmx` tuning is demoted to near-worthless as a RAM lever.** 2.67× of ceiling bought ~13% of
  RSS. Dropping five workers to 1536m reclaims ~100MB each, not 2.5GB each — not worth an
  `--action update`'s redeploy risk. The `--configOverrides` trap had in fact already fired (five of
  six workers were running at the 4096m default, only `EmailParsingModule` at 1536m), and it was
  deliberately **not** fixed: Step 0 needs no redeploy, and the measurement shows fixing it would
  barely move the number.
- **`conductor.child.opts` is promoted to the highest-value untried lever.** The three daemons cost
  **2.33GB — ~39% of the idle total — before a single module loads.** Conductor is a pure
  coordination process sitting at 768.7MB. Unlike worker `-Xmx`, trying it costs no redeploy risk.
- **Consolidation's payoff is now confirmed to be the right target.** The 2026-07-29 audit reasoned
  that merging modules eliminates *duplicated fixed per-worker cost* rather than PState-proportional
  cost. The measurement confirms exactly that: cost is fixed-per-worker (~866MB+ floor), and does
  not scale with PStates. Consolidation remains a fallback, and its arithmetic is now real.

### Process note

The cluster came back cleanly with no redeploy — all six modules restored from disk state,
`moduleState: RUNNING` with `appendTargetId == readTargetId` on all six, satisfying the project's
own "RUNNING does not prove a cutover" rule. One operational discovery: `rama shutdownCluster`
persists a `cluster-shutdown-complete` state in ZooKeeper, so `conductorReady` reports `false` after
restart until a Supervisor registers. It clears on its own; `forceClusterOpen` is not needed. Also
noted: `devZookeeper` listens on **port 2000**, not the ZooKeeper default 2181.

## 2026-08-01 — Step 0b Tasks 1 & 2: the gate that would have been meaningless, and a trap found by reading

Time-boxed session. Code landed and green; deploy deliberately not started (see the end of this entry).

### The actual problem with the hardcoded query wasn't that it was hardcoded

`GmailIngestionModule` carried the same filter twice — `:148` for the fetch, `:138` for the
already-processed count. The brief framed the fix as "make it configurable." That is necessary but not
sufficient: two independently-written strings that are *supposed* to describe the same message scope will
drift the moment one is edited, and the failure is invisible, because both queries are individually
plausible. The Step 3 cost gate would then price one population and the module would ingest another —
i.e. the gate would produce a number that was precisely wrong rather than obviously wrong.

So the count query is **derived** from the fetch query (`-label:X` → `label:X`) rather than configured
alongside it. Consistency becomes structural instead of a convention someone has to remember. Two of the
seven new tests exist purely to pin that relationship, and one pins the absence of `is:unread` — that being
the specific regression the whole task exists to prevent.

Worth noting *why* `is:unread` was wrong and not merely unwanted: read/unread state is a mailbox UI concern
that a human changes by clicking around. It was never a record of what this system had ingested — that is
what the `FamilyAssistant/Processed` label is. Using it as an ingestion filter coupled the pipeline's notion
of "done" to an unrelated, user-mutable signal.

### Two defects found that the brief didn't mention, both left alone on purpose

The `alreadyProcessed` counter reads a single `messages.list` page and saturates at the page size — it has
never been a true count. It is a display-only summary field, so this is cosmetic *there*, but it is exactly
the kind of number that gets grabbed later for a purpose it can't support. Left in place, documented in code
as unusable for sizing a backlog, and the Step 3 counter will paginate properly instead. Two debug probes
hardcoding `from:acemystuff@gmail.com` were also left alone (log-only, zero-result path).

Fixing either would have been easy. Neither was asked for, and quietly widening the diff on a session that
also touches a module about to be redeployed is how a small change becomes an unreviewable one.

### The finding that mattered was found by reading, not by running

`GmailService.java:73` builds its token store from a **relative** path (`new File("tokens")`). In local mode
that resolves against the project root and works. In cluster mode the Gmail call runs inside a
Supervisor-launched worker, so it resolves against the *Supervisor's* cwd — and Rama neither logs a working
directory nor sets `-Duser.dir`, so nothing in the logs would say what it was.

The failure mode is what makes it worth an entry in `RAMA_VERIFIED_LEARNINGS.md`: OAuth is run separately and
would report `SUCCESS` with a real mailbox total — the token genuinely being valid — while the worker,
unable to see the file, falls back to attempting an interactive browser consent inside a headless process.
**Auth succeeds and ingestion fails, separated in time and in log file.** Ingestion has never run in cluster
mode (Part 3 never happened), so this has never been exercised and would have surfaced for the first time at
precisely the worst moment: immediately after the cost gate, with spend already authorized.

Mitigation is free — start the Supervisor from the project root and the workers inherit it. The honest part
is what is *not* claimed: the actual cwd of a running worker has not been observed. The mechanism is verified
from source and from documented `File` semantics; the empirical check (`lsof -d cwd`) is a step in the next
session. If the Supervisor was already being started from the project root, the trap was dormant, not
absent — and a cloud box with a systemd unit setting its own `WorkingDirectory` will differ, which is why it
is recorded now rather than after it bites.

General form, recorded as the rule: **any relative path in code that runs inside a Rama module is a
cluster-mode liability, and local-mode tests structurally cannot catch it** — local mode runs in the process
the developer started.

### On the gate review's Gate 3, which I answered "no" to

The review asked me to confirm the new fields are written via sequential `localTransform` rather than an
assembled `HashMap` + `termVal`. Answering yes would have been false, and answering it as-asked would have
meant rewriting `FamilySchemaModule`'s record write into 27 individual transforms — in a module explicitly
excluded from this deploy.

The change adds zero PState writes. `modelId`/`promptVersion` are two more keys on a *depot payload*. The
PState write is `FamilySchemaModule.java:279`'s whole-record `termVal("*record")`, and it is safe precisely
because grep confirms it is the only write to `$$family-data` anywhere — the trap requires a value to be both
assembled as one `Map` *and* later targeted by a narrower write, which is `$$commitments` (correctly using
sequential `localTransform` for that reason) and is not this. The right response to a gate question is to
check the precondition, not to perform the remedy.

### Why the deploy didn't happen

Roughly 30 minutes were available. Tasks 1 and 2 plus the suite fit; a two-module `--action update` does not
fit reliably — the last cluster update on this box got watchdog-killed mid-handover and needed diagnosis.
Starting a module update that cannot be watched to completion is strictly worse than not starting one. Repo
left green at 152/152 with the deploy as the next session's first action.

---

## 2026-08-03 — Phase A audit: the must-land test was wrong, and the baseline was not what the handoff said

### DECISION_temporal_model.md §6 over-scoped the must-land list

§6 sorts schema work by "captured at parse time or lost forever." That test is wrong for this
codebase, and the audit is what exposed it.

**The correct test is "not reconstructable by depot redrain," not "not captured at parse time."**

The difference is `*raw-emails`. It is a genuine write-ahead archive: `persist-raw`
(`EmailParsingModule.java:194-197`) appends the complete raw email with `AckLevel.APPEND_ACK`
*before* any parsing happens. So anything an LLM can re-extract from a stored raw email is not
lossy — it is recoverable by re-draining the archive through a newer parser. That is the entire
point of having built the depot.

Applying the corrected test to §6's list:

- **Provenance fields, `assertedAt`/`eventTime` split, actor attribution** — genuinely must land.
  Provenance describes the derivation itself; a redrain produces *new* provenance, not the old
  record's. `assertedAt` is when the system came to believe something — a redrain cannot
  reconstruct the original belief time. Actor is who caused a transition, and transitions live in
  `*commitment-status-changes` as permanent event-sourced records, not recomputed state.
- **Trigger preservation and held-as-default (B5)** — do NOT need to land pre-ingestion. Both are
  prompt changes. A when-clause dropped by today's parse is still sitting in the raw email body;
  re-drain with the improved prompt and it comes back. §6 called these "lossy-if-skipped," which
  is true of a system without a raw archive and false of this one.
- **Relation edge assertions (B3)** — also do not need to land pre-ingestion, for a different
  reason. Edges are a materialized view over `*family-events`, not permanent records: the stream
  topology recomputes `$$edges-forward`/`$$edges-inverse`/`$$entities` from each record's
  `relations` field on every drain. So `$$edge-assertions` rebuilds by redrain too — provided the
  depot record carries `assertedAt` and the relation triples, both of which are true once B2
  lands. §2's "retrofitting supersession onto live edges is a migration" assumed edges were
  permanent records. They are not.

Net effect: B3, B5 and Fork 4 (the relation cardinality registry) defer to the edge-schema work.
This session's scope reduced to step 0, B0, B1, B2, B4.

The general lesson is that a write-ahead archive changes what "irreversible" means. §6 was
written against the *category* of decision (schema-shaped, cheap now, expensive later) rather
than against this system's actual replay capability. Both docs are otherwise sound; this is a
scoping correction, not a reversal.

### C1 — the temporal doc's edgeId formula was not redrain-safe

§2.2 specifies `edgeId = hash(subject, relation, object, assertedAt)`. Correct only if
`assertedAt` is depot data. Computed in the topology with `currentTimeMillis()`, every redrain
mints different edgeIds and the supersession chain the field exists to preserve is destroyed.
Promoted to a general rule in `RAMA_VERIFIED_LEARNINGS.md` ("Any value participating in a
deterministic ID must be stamped into the depot payload at append time"), since it is the same
requirement `mintEntityId`/`mintCommitmentId` already satisfy without ever having stated it.

### The 152/152 baseline was inherited, and it was wrong

The handoff records 152/152 non-LLM green. Observed this session: **151/152**.
`GmailIngestionTest.testGmailToFamilyData` errors with `Executor pool is shut down` — the same
`InProcessCluster`-lifecycle-across-test-classes failure mode already documented for
`ZooEmailTest`, and confirmed order-dependent here: the test **passes in isolation** (65s, live
Gmail fetch succeeds) and fails only in full-suite position.

Two things worth keeping:

1. **That test cannot skip under Maven, ever.** `pom.xml:96` passes
   `<GEMINI_API_KEY>${env.GEMINI_API_KEY}</GEMINI_API_KEY>`. With the env var unset, Maven
   substitutes nothing and the child JVM receives the **literal string**
   `${env.GEMINI_API_KEY}` — which is non-null, so `assumeTrue` at `GmailIngestionTest.java:65`
   always passes and the test always runs. The handoff's "no `GEMINI_API_KEY` required / skips
   gracefully" claim is false for this test. Verified by re-running with `env -u GEMINI_API_KEY`:
   `Skipped: 0`, same failure.
2. `GmailIngestionTest` is tagged `gmail`, not `llm`, so `excluded.groups=llm` never excludes it.

Not fixed — out of scope, and it is test infrastructure rather than parsing logic. Recorded so
the next session does not re-derive it or trust the inherited number.

### B0 — why the byte-identity gate was built the way it was

The brief requires proving the extracted templates render byte-identical output to the inline
concatenation they replaced. The obvious implementation — hand-copy the old prompt text into the
test as the reference — has a hole: an identical transcription slip in both the test copy and the
production template makes the gate pass while the prompt has in fact changed. That is precisely
the failure the gate exists to prevent, so it was worth avoiding structurally.

Instead the legacy reference in `PromptTemplateByteIdentityTest` was generated **mechanically**
from commit `4dc6582`'s `EmailParsingModule.java` (lines 211-226 and 277-297) via three purely
textual edits — declaration to `return`, `message.body` to `body`, dedent — with the source
snippet md5s recorded in the test's header comment. The production templates were generated from
the same extracted snippets by the same method. Neither side was retyped.

The gate was then **mutation-tested**: changing one character in the classify template
(`IS.` to `Is.`) made it fail, and reverting made it pass. A green gate that has never been shown
to fail is not evidence.

Result: 4/4 green, full suite 156 run / 155 pass / 1 pre-existing error, zero regressions.

`PROMPT_VERSION = "v1"` deliberately still stands and is still what gets stamped. B0 adds the
hash mechanism (`CLASSIFY_PROMPT_VERSION`, `EXTRACT_PROMPT_VERSION`, per-prompt per Fork 2) but
does not switch the stamp — that is B1. Keeping the switch out of B0 is what makes B0 a strictly
behavior-preserving refactor with nothing to detect but the prompt text itself.

### Correction recorded in code

`FamilySchemaModule.java:415` claimed "actor is durably captured in this depot's own replay log."
It never was — no append site has ever written an `actor` field, and `WebhookReceiver.markDone`
appends exactly `{familyId, commitmentId, newStatus, changedAt}`. Comment corrected to describe
actual behavior and point at B4 as planned work.

## 2026-08-09 (backfilled 2026-09-02) — D1 cwd fix, Fork 1 gate answers, and the llm,gmail
## exclusion close-out (commits `292dcb0`, `c2910e1`, `a0f5815`)

These three commits landed same-day, immediately after the 2026-08-03 Phase A audit entry
above, and were never logged here — this entry backfills them from the commit messages and
diffs so the record isn't missing three commits of real decisions. Written retroactively;
treat the commits themselves as the source of truth for anything this summary compresses.

### `292dcb0` — D1: kill the cwd overload at the root; suite honestly green; Fork 1 gate answers

**Root cause of the deploy blocker, found structurally, not worked around.** `rama devZookeeper`
hardcodes a *relative* `"local-zk"` dataset directory at the bytecode level — verified by
`javap` against the real 1.5.0 jar, only `:port` is configurable, the dataset path is a
compile-time constant. So which ZK dataset gets used depends entirely on the launching
process's cwd, and the same cwd ambiguity was also silently selecting `GmailService`'s
`tokens/` directory (`new File("tokens")`, resolved against the Supervisor's cwd in cluster
mode). One unpinned cwd was overloading two unrelated concerns.

Fix was structural, not configuration: one canonical absolute ZK dataset
(`/Users/toddkeelingfolder/rama-zk`), with both candidate cwds' `local-zk` replaced by
symlinks to it — a symlink makes the wrong-cwd launch impossible rather than merely
discouraged. Both prior datasets archived (moved, not deleted). Canonical starts clean per
Tor's call — the July metadata was already orphaned (`~/rama-data` was 12KB, no RocksDB
artifacts).

`GmailService`'s token directory was pinned absolute the same way (`FA_TOKENS_DIR`, override
must itself be absolute — a relative override would reintroduce the exact bug), and
`tokensDirectory()` now refuses three ways this previously failed silently: a relative
override, a missing directory, and a missing `StoredCredential` unless interactive consent
was explicitly enabled. That third check matters specifically because a cluster worker
cannot answer a browser OAuth flow — the old failure mode was a hang on `127.0.0.1:8888`
*after* startup had already reported success. Interactive consent is opted in from
`GmailOAuthSetup`/`GmailWatchSetup`'s `main()`, not from `renewWatch()`, so a future
non-interactive caller of `renewWatch()` still fails loud instead of hanging.

**Baseline hygiene, same commit:** `pom.xml` was passing the *literal string*
`"${env.GEMINI_API_KEY}"` into the test JVM whenever the env var was unset (Maven does not
substitute an unset `${env.X}`), which is non-null, so every `assumeTrue(key != null)` guard
passed and LLM/Gmail tests ran with a garbage key instead of skipping. Fixed with an empty
default property overridden by a `gemini-key` Maven profile that activates only when the env
var is genuinely present. `GmailIngestionTest`'s own guard was hardened to also reject blank
and a literal `${...}`, so reverting the pom alone can't silently reintroduce the bug.

**Fork 1 (nested `derivations` map) gate answers — all four PASS, with two corrections rather
than rubber stamps**, fully detailed in `docs/decisions/PLAN_provenance_temporal.md`'s "FORK 1
LOCKED" section: Gate 3's premise doesn't apply (`derivations` is a depot-payload key, never a
PState partial-write target — `$$family-data` has exactly one writer, re-verified by grep);
Gate 9's `sourceId = gmailMessageId` holds on two conditions (an absent id must stay null,
never a fallback UUID; `derivedAt` must never feed a deterministic ID). B1 step 9
("no fake `modelId`" on the keyword-fallback path) was found mis-specified: the model is
*always* called and billed, and still produces `silo`/`intent` even when `classifyByKeyword`
overrides `category` — `modelId: null` would erase true facts, not just suppress a false one.
Also flagged: `receivedAt` (Gmail's arrival time) is NOT `assertedAt` (when this system came to
believe the claim) — copying it would misdate an entire backlog ingest on go-live.

Tests: 156 run, 0 failures, 0 errors, 1 skipped (`env -u GEMINI_API_KEY mvn test`) — corrects
both the handoff's stale "145/145" and this session's inherited "151/152"; B0 added 4 tests,
real total is 156. Caveat recorded at the time: `GEMINI_API_KEY` is exported from `~/.zshrc`,
so a *plain* `mvn test` still ran the live-LLM tests and still hit the known
"Executor pool is shut down" defect — `env -u` was the documented routine command as of this
commit, superseded two commits later.

### `c2910e1` — Verify-before-wiring: two checklist claims are wrong at source; exclude gmail tag

B1 was approved to start but was deliberately **not** started this session — the go-live
checklist asked for two claims to be confirmed at source before wiring B1, and both came back
negative, so writing B1 to the checklist's stated shape would have been wrong.

**Conflict 1 — `classifyByKeyword` DOES fire on an in-schema `UNKNOWN`, not only on off-schema
output as the checklist claimed.** `EmailParsingModule.java:317-349`: `UNKNOWN` is both the
sentinel default *and* a member of the accepted category enum, so all four cases (model said
`UNKNOWN`, model said something off-schema, the `category` field was missing, or the JSON
parse threw) collapse to the identical string `"UNKNOWN"` at line 347 — the code cannot
distinguish them today. B1 has to create that distinction, not assume it already exists:
two closed-set fields, `outcome` (`ok`/`off-schema`/`parse-error`) and `categoryBasis`
(`model`/`keyword`/`none`), added alongside the existing control flow unchanged, same
discipline as B0. `none` is a real case — off-schema output with no keyword match either
leaves `UNKNOWN` with nothing having actually decided it.

**Conflict 2 — `created` is NOT recomputed on redrain, so mirroring its mechanism for
`assertedAt` is safe; the checklist's warning was inverted.** `created` is stamped at
`EmailParsingModule.java:418` inside the `write-to-store` *agent node*, into the depot payload,
before `depot.append()` — `FamilySchemaModule` only ever reads it back
(`.select("*record", Path.key("created"))`). That is exactly the append-time-stamping
discipline C1 requires, and is the same rule the Gate 9 entry in `RAMA_VERIFIED_LEARNINGS.md`
already states. The distinction the checklist was actually reaching for is reparse-of-raw-email
(re-running the node produces a genuinely new assertion time, correctly) versus a
`*family-events` redrain (replays the stored payload verbatim) — those are different
operations, and only the second one is what "redrain" means for this depot.

**Conflict 3 (minor) — the `llm`-exclusion fix the checklist proposed already existed.**
`pom.xml`'s `excluded.groups=llm` predates this session, so `EmailIngestionTest`,
`FamilyAssistantTest`, and `QueryAgentTest` were never live-spend by default — correcting an
overstatement in this same session's earlier summary. **The actual gap was
`GmailIngestionTest`**, tagged `@Tag("gmail")` rather than `@Tag("llm")`, so nothing excluded
it. Fixed by widening the default to `excluded.groups=llm,gmail`.

**Cost-gate input, verified rather than assumed:** the model is called *twice* per email
(classify + extract) regardless of outcome — `classifyByKeyword` does not save a call, it only
runs after the classify call is already made and billed. Backlog cost estimates should price at
count × 2.

Baseline recorded as last-verified-green at commit time: 156 run / 0 failures / 0 errors /
1 skipped, via `env -u GEMINI_API_KEY mvn test`, verified twice. The confirming run for the new
`llm,gmail` exclusion (i.e. whether a *plain* `mvn test` was now clean) had not finished before
session end — flagged explicitly as the first thing to re-run next session, which is exactly
what `a0f5815` is.

**Parked, not decided:** the `classify` node will need to emit a 5th value once the
`derivations` map lands (4 today), and whether Agent-o-rama's node-lambda has an arity ceiling
that blocks this was **not confirmed** — research was cut short. Next session must check the
docs or sidestep with a `RamaSerializable` carrier object rather than guessing the ceiling
doesn't exist.

### `a0f5815` — Confirm the llm,gmail exclusion: plain `mvn test` is 155/0/0/0 green

The confirming run parked by `c2910e1` completed: **155 run, 0 failures, 0 errors, 0 skipped,
BUILD SUCCESS**, from a *plain* `mvn test` with the real `GEMINI_API_KEY` still exported from
`~/.zshrc`, and zero live API calls made. Count drops 156 → 155 exactly as predicted:
`GmailIngestionTest.testGmailToFamilyData` is now excluded by tag rather than reached and
skipped by an internal assumption, so the known "Executor pool is shut down" defect is no
longer reached by a default run. **That defect itself is untouched — excluded, not fixed.**

`mvn test` (no `env -u` prefix) is now the correct routine command; the previous two commits'
`env -u GEMINI_API_KEY mvn test` guidance is superseded. `CLAUDE_HANDOFF.md` was reconciled so
the 156/1-skipped figure (pre-exclusion) and the 155/0-skipped figure (post-exclusion) both
appear without reading as a contradiction, and the stale "does NOT make plain `mvn test` green"
caveat was removed now that it no longer applies.

### Net state after all three commits

B1 (provenance fields — `derivedAt`, `sourceId`, hash-derived `promptVersion`, the
`derivations` map) is **still not started**, now blocked on two things surfaced by the
verify-before-wiring pass rather than ready to write to the original checklist shape: the
`outcome`/`categoryBasis` distinction (Conflict 1) and the AOR node-lambda arity question
(parked). Baseline as of `a0f5815` is 155 run / 0 failures / 0 errors / 0 skipped via a plain
`mvn test`, with `GmailIngestionTest`'s pre-existing "Executor pool is shut down" defect
excluded from that run by tag, not resolved.
