package com.family.assistant.query;

import static com.family.assistant.util.EventUtils.*;

import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.store.PStateStore;
import com.rpl.rama.Path;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * QueryModule
 *
 * Answers natural language questions about family events and tasks.
 *
 * Agents:
 *   query-agent  — interpret-query → fetch-data → generate-answer → finalize.
 *                  interpret-query is the only LLM call in this module; it
 *                  extracts a compound filter (keywords + optional
 *                  child/category/silo/intent/date-range dimensions), not a
 *                  single guessed category. fetch-data delegates to
 *                  search-agent (below) for the actual PState work.
 *   search-agent — parse-filters → resolve-indexes → intersect → finalize.
 *                  No LLM. Two-tier hard/soft dimension intersection — see
 *                  the agent's own doc comment and RAMA_VERIFIED_LEARNINGS.md.
 *                  Callable cross-module via
 *                  getMirrorAgentClient("QueryModule", "search-agent").
 *
 * Input:  QueryRequest (familyId, question, requesterTimezone)
 * Output: String natural language answer
 *
 * Reads from: $family-data, and every $$events-by-* index
 * (via getMirrorStore from FamilySchemaModule)
 *
 * Agent objects required:
 *   "gemini-model" — ChatModel
 */
public class QueryModule extends AgentModule implements java.io.Serializable {

    @Override
    public String getModuleName() {
        return "QueryModule";
    }

    // -----------------------------------------------------------------------
    // Input type
    // -----------------------------------------------------------------------
    public static class QueryRequest implements com.rpl.rama.RamaSerializable {
        public final String familyId;
        public final String question;
        public final String requesterTimezone; // e.g. "America/Los_Angeles", "America/New_York"
        public final String accountLabel;      // null = no filter; non-null = restrict to this account

        public QueryRequest(String familyId, String question, String requesterTimezone) {
            this(familyId, question, requesterTimezone, null);
        }

        public QueryRequest(String familyId, String question, String requesterTimezone, String accountLabel) {
            this.familyId           = familyId;
            this.question           = question;
            this.requesterTimezone  = requesterTimezone != null
                                      ? requesterTimezone : "America/Los_Angeles";
            this.accountLabel       = accountLabel;
        }
    }

    // -----------------------------------------------------------------------
    // Internal data carrier — interpret-query → fetch-data
    // -----------------------------------------------------------------------
    public static class QueryParams implements com.rpl.rama.RamaSerializable {
        public final String queryType;      // UPCOMING_EVENTS, DEADLINES, TASKS, SPECIFIC_DATE, GENERAL
        public final String childName;      // SOFT dimension; null if not specified
        public final String dateFrom;       // HARD dimension (with dateTo); ISO-8601 or null
        public final String dateTo;         // HARD dimension (with dateFrom); ISO-8601 or null
        public final String categoryFilter; // SOFT dimension; SCHOOL_EVENT, PERMISSION_SLIP, TASK, etc. or null
        public final String originalQuestion;
        public final String familyId;
        public final String requesterTimezone;
        public final String accountLabel;   // null = no filter; non-null = restrict to this account
        public final List<String> keywords; // HARD dimension; never null, empty = no keyword filter
        public final String siloFilter;     // SOFT dimension; VAULT/OFFICE/STUDIO/UNKNOWN or null
        public final String intentFilter;   // SOFT dimension; ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN or null

        public QueryParams(String queryType, String childName,
                           String dateFrom, String dateTo, String categoryFilter,
                           String originalQuestion, String familyId,
                           String requesterTimezone) {
            this(queryType, childName, dateFrom, dateTo, categoryFilter,
                 originalQuestion, familyId, requesterTimezone, null,
                 new ArrayList<>(), null, null);
        }

        public QueryParams(String queryType, String childName,
                           String dateFrom, String dateTo, String categoryFilter,
                           String originalQuestion, String familyId,
                           String requesterTimezone, String accountLabel) {
            this(queryType, childName, dateFrom, dateTo, categoryFilter,
                 originalQuestion, familyId, requesterTimezone, accountLabel,
                 new ArrayList<>(), null, null);
        }

        public QueryParams(String queryType, String childName,
                           String dateFrom, String dateTo, String categoryFilter,
                           String originalQuestion, String familyId,
                           String requesterTimezone, String accountLabel,
                           List<String> keywords, String siloFilter, String intentFilter) {
            this.queryType         = queryType;
            this.childName         = childName;
            this.dateFrom          = dateFrom;
            this.dateTo            = dateTo;
            this.categoryFilter    = categoryFilter;
            this.originalQuestion  = originalQuestion;
            this.familyId          = familyId;
            this.requesterTimezone = requesterTimezone;
            this.accountLabel      = accountLabel;
            this.keywords          = keywords != null ? keywords : new ArrayList<>();
            this.siloFilter        = siloFilter;
            this.intentFilter      = intentFilter;
        }
    }

    // -----------------------------------------------------------------------
    // defineAgents
    // -----------------------------------------------------------------------
    @Override
    public void defineAgents(AgentTopology topology) {

        // Declare the Gemini model
        topology.declareAgentObjectBuilder("gemini-model", setup -> {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return null;
            return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .build();
        });

        topology.newAgent("query-agent")

            // ----------------------------------------------------------------
            // Node 1: interpret-query
            // Input:  QueryRequest
            // Output: QueryParams → fetch-data
            // ----------------------------------------------------------------
            .node("interpret-query", "fetch-data",
                (AgentNode agentNode, QueryRequest request) -> {

                    ChatModel model =
                        (ChatModel) agentNode.getAgentObject("gemini-model");

                    QueryParams params;

                    if (model == null) {
                        // Stub fallback — no API key
                        params = new QueryParams(
                            "UPCOMING_EVENTS", null, null, null, null,
                            request.question, request.familyId,
                            request.requesterTimezone, request.accountLabel,
                            new ArrayList<>(), null, null);
                    } else {
                        String today = Instant.now()
                            .atZone(ZoneId.of(request.requesterTimezone))
                            .toLocalDate().toString();

                        String prompt =
                            "You are a family assistant query parser. Today is " + today + ".\n\n" +
                            "Parse this question into structured JSON. Respond ONLY with JSON, no markdown.\n\n" +
                            "Question: \"" + request.question + "\"\n\n" +
                            "Return this exact JSON structure:\n" +
                            "{\n" +
                            "  \"queryType\": \"UPCOMING_EVENTS|DEADLINES|TASKS|SPECIFIC_DATE|GENERAL\",\n" +
                            "  \"keywords\": [\"word1\", \"word2\"],\n" +
                            "  \"childName\": \"name or null\",\n" +
                            "  \"dateFrom\": \"YYYY-MM-DD or null\",\n" +
                            "  \"dateTo\": \"YYYY-MM-DD or null\",\n" +
                            "  \"categoryFilter\": \"SCHOOL_EVENT|PERMISSION_SLIP|TASK|DEADLINE|null\",\n" +
                            "  \"siloFilter\": \"VAULT|OFFICE|STUDIO|null\",\n" +
                            "  \"intentFilter\": \"ACTION_REQUIRED|DECISION_NEEDED|FYI|SCHEDULING|null\"\n" +
                            "}\n\n" +
                            "Rules:\n" +
                            "- Today's date is " + today + ". All dates should be in 2026 unless " +
                            "explicitly stated otherwise.\n" +
                            "- 'this week' = " + today + " to 7 days from now\n" +
                            "- 'today' = " + today + "\n" +
                            "- 'next week' = 7 to 14 days from now\n" +
                            "- If no date range implied, use null for both dates\n" +
                            "- keywords is the PRIMARY signal. Extract ONLY content words that are " +
                            "ACTUALLY PRESENT in the question text itself (e.g. \"permission\", \"slip\", " +
                            "\"zoo\" — words the user typed). If the question has no such content words " +
                            "(e.g. a pure date/time question like \"what is happening on March 20th\"), " +
                            "return an EMPTY array — do NOT invent generic words like \"activity\", " +
                            "\"happening\", or \"event\" to fill the list. An empty keyword list combined " +
                            "with a date range is a valid, expected filter.\n" +
                            "- childName/categoryFilter/siloFilter/intentFilter are SECONDARY, " +
                            "confirmation-only signals. Only set one when the question makes it " +
                            "unambiguous which value applies — use null whenever you are not confident. " +
                            "Do not guess a category just because the question resembles one.";

                        String response = model.chat(prompt);
                        params = parseQueryParams(
                            response, request.question,
                            request.familyId, request.requesterTimezone,
                            request.accountLabel);
                    }

                    agentNode.emit("fetch-data", params);
                })

            // ----------------------------------------------------------------
            // Node 2: fetch-data
            // Input:  QueryParams
            // Output: (QueryParams, List<Map> matchingEvents) → generate-answer
            //
            // Delegates the actual search to search-agent (defined below, same
            // module) via same-module agent invocation — no PState/index logic
            // lives here anymore. See RAMA_VERIFIED_LEARNINGS.md for
            // agentNode.getAgentClient(String) verification.
            // ----------------------------------------------------------------
            .node("fetch-data", "generate-answer",
                (AgentNode agentNode, QueryParams params) -> {

                    AgentClient searchAgent = agentNode.getAgentClient("search-agent");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> matched =
                        (List<Map<String, Object>>) searchAgent.invoke(params);

                    agentNode.emit("generate-answer", params, matched);
                })

            // ----------------------------------------------------------------
            // Node 3: generate-answer
            // Input:  QueryParams, List<Map> matchingEvents
            // Output: String answer → finalize
            // ----------------------------------------------------------------
            .node("generate-answer", "finalize",
                (AgentNode agentNode, QueryParams params,
                 List<Map<String, Object>> events) -> {

                    ChatModel model =
                        (ChatModel) agentNode.getAgentObject("gemini-model");

                    String answer;

                    if (events.isEmpty()) {
                        answer = "I didn't find any events matching your question"
                            + (params.childName != null ? " for " + params.childName : "")
                            + ".";
                    } else if (model == null) {
                        // Stub fallback
                        answer = formatEventsPlain(events, params.requesterTimezone);
                    } else {
                        String eventSummary = formatEventsForPrompt(
                            events, params.requesterTimezone);

                        String prompt =
                            "You are a helpful family assistant. Answer the parent's question " +
                            "naturally and concisely based on the family event data below.\n\n" +
                            "Question: \"" + params.originalQuestion + "\"\n\n" +
                            "Family event data:\n" + eventSummary + "\n\n" +
                            "Rules:\n" +
                            "- Be warm and helpful, like a knowledgeable friend\n" +
                            "- Mention specific dates, times, and requirements\n" +
                            "- If pickup or dropoff is needed, highlight it\n" +
                            "- Keep the answer concise — 2-4 sentences max unless detail is needed\n" +
                            "- Times should be in " + params.requesterTimezone + " timezone";

                        answer = model.chat(prompt);
                    }

                    agentNode.emit("finalize", answer);
                })

            // ----------------------------------------------------------------
            // Node 4: finalize (terminal)
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, String answer) -> {
                    agentNode.result(answer);
                });

        // ------------------------------------------------------------------
        // Agent: search-agent
        //
        // Compound search over ALL index dimensions with two-tier set
        // intersection — replaces the single-categoryFilter guess that used
        // to zero out results whenever that one guess was wrong. No LLM call
        // anywhere in this agent; query-agent's interpret-query node is the
        // only place natural language gets parsed. Callable cross-module via
        // getMirrorAgentClient("QueryModule", "search-agent") for future
        // reuse (e.g. DigestModule), though nothing else calls it this
        // session.
        //
        // Graph: parse-filters -> resolve-indexes -> intersect -> finalize
        //
        // HARD dimensions (keywords, dateRange, accountLabel): always
        // applied, never dropped. accountLabel is grouped with HARD because
        // it's a caller-supplied scope restriction, not an LLM guess from
        // free text — dropping it as if it were an uncertain classification
        // would leak across accounts.
        //
        // SOFT dimensions (childName, categoryFilter, siloFilter,
        // intentFilter): applied normally when they agree with the hard
        // dimensions. If the full intersection across every provided
        // dimension is empty AND at least one hard dimension was provided,
        // soft dimensions are dropped and the hard dimensions alone are
        // re-intersected — a deterministic fallback, not a retry.
        // ------------------------------------------------------------------
        topology.newAgent("search-agent")

            // ----------------------------------------------------------------
            // Node 1: parse-filters
            // Input:  QueryParams (raw — keywords are un-normalized phrases/words)
            // Output: (QueryParams, List<String> normalizedTokens, boolean
            //          hasDateRange, long fromMs, long toMs) -> resolve-indexes
            // ----------------------------------------------------------------
            .node("parse-filters", "resolve-indexes",
                (AgentNode agentNode, QueryParams params) -> {

                    Set<String> normalizedTokens = new HashSet<>();
                    for (String rawKeyword : params.keywords) {
                        normalizedTokens.addAll(tokenize(rawKeyword));
                    }

                    boolean hasDateRange = params.dateFrom != null || params.dateTo != null;
                    long fromMs = parseToEpochStartOfDay(params.dateFrom, params.requesterTimezone, Long.MIN_VALUE);
                    long toMs   = parseToEpochEndOfDay(params.dateTo,     params.requesterTimezone, Long.MAX_VALUE);

                    agentNode.emit("resolve-indexes", params,
                        new ArrayList<>(normalizedTokens), hasDateRange, fromMs, toMs);
                })

            // ----------------------------------------------------------------
            // Node 2: resolve-indexes
            // Input:  QueryParams, List<String> normalizedTokens, boolean
            //         hasDateRange, long fromMs, long toMs
            // Output: (QueryParams, Map<String, Set<String>> byDimension) -> intersect
            //
            // One PState lookup per dimension actually present. Dimensions
            // not provided are never added to byDimension — this is the
            // literal mechanism behind "empty dimensions are skipped, not
            // treated as match-nothing."
            // ----------------------------------------------------------------
            .node("resolve-indexes", "intersect",
                (AgentNode agentNode, QueryParams params, List<String> normalizedTokens,
                 Boolean hasDateRange, Long fromMs, Long toMs) -> {

                    PStateStore psKeyword = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-keyword");
                    PStateStore psDate = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-date");
                    // childName/categoryFilter are legacy query-param names; they now resolve
                    // against the renamed multi-valued person/tag indexes. A single-value lookup
                    // still works because each list element is an index key.
                    PStateStore psPerson = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-person");
                    PStateStore psTag = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-tag");
                    PStateStore psSilo = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-silo");
                    PStateStore psIntent = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-intent");
                    PStateStore psAccount = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-account");

                    Map<String, Set<String>> byDimension = new HashMap<>();

                    // HARD: keywords — union across tokens (any token match counts;
                    // only the overall compound filter is a strict AND, not this).
                    if (!normalizedTokens.isEmpty()) {
                        Set<String> union = new HashSet<>();
                        for (String token : normalizedTokens) {
                            @SuppressWarnings("unchecked")
                            Set<String> ids = (Set<String>)
                                psKeyword.selectOne(Path.key(params.familyId).key(token));
                            if (ids != null) union.addAll(ids);
                        }
                        byDimension.put("keywords", union);
                    }

                    // HARD: dateRange
                    if (hasDateRange) {
                        @SuppressWarnings("unchecked")
                        List<String> dateIds = (List<String>) (List<?>) psDate.select(
                            Path.key(params.familyId)
                                .sortedMapRange(fromMs, toMs)
                                .mapVals().all());
                        byDimension.put("dateRange",
                            dateIds != null ? new HashSet<>(dateIds) : new HashSet<>());
                    }

                    // HARD: accountLabel (caller-supplied scope, not an LLM guess)
                    if (params.accountLabel != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>)
                            psAccount.selectOne(Path.key(params.familyId).key(params.accountLabel));
                        byDimension.put("accountLabel",
                            ids != null ? new HashSet<>(ids) : new HashSet<>());
                    }

                    // SOFT: childName
                    if (params.childName != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>)
                            psPerson.selectOne(Path.key(params.familyId).key(params.childName));
                        byDimension.put("childName",
                            ids != null ? new HashSet<>(ids) : new HashSet<>());
                    }

                    // SOFT: categoryFilter
                    if (params.categoryFilter != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>)
                            psTag.selectOne(Path.key(params.familyId).key(params.categoryFilter));
                        byDimension.put("categoryFilter",
                            ids != null ? new HashSet<>(ids) : new HashSet<>());
                    }

                    // SOFT: siloFilter
                    if (params.siloFilter != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>)
                            psSilo.selectOne(Path.key(params.familyId).key(params.siloFilter));
                        byDimension.put("siloFilter",
                            ids != null ? new HashSet<>(ids) : new HashSet<>());
                    }

                    // SOFT: intentFilter
                    if (params.intentFilter != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> ids = (Set<String>)
                            psIntent.selectOne(Path.key(params.familyId).key(params.intentFilter));
                        byDimension.put("intentFilter",
                            ids != null ? new HashSet<>(ids) : new HashSet<>());
                    }

                    agentNode.emit("intersect", params, byDimension);
                })

            // ----------------------------------------------------------------
            // Node 3: intersect
            // Input:  QueryParams, Map<String, Set<String>> byDimension
            // Output: List<Map<String,Object>> matchedEvents -> finalize
            //
            // Two-tier intersection: try every provided dimension first; if
            // that's empty and a HARD dimension was present, drop SOFT
            // dimensions and re-intersect on HARD dimensions only.
            // ----------------------------------------------------------------
            .node("intersect", "finalize",
                (AgentNode agentNode, QueryParams params, Map<String, Set<String>> byDimension) -> {

                    PStateStore psMain = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$family-data");

                    List<Map<String, Object>> matched = new ArrayList<>();

                    if (byDimension.isEmpty()) {
                        // Zero dimensions extracted at all — full scan (unchanged
                        // fallback behavior for a fully generic question).
                        @SuppressWarnings("unchecked")
                        Map<String, Object> allEvents = (Map<String, Object>)
                            psMain.selectOne(Path.key(params.familyId).key("events"));
                        if (allEvents != null) {
                            for (Object val : allEvents.values()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> event = (Map<String, Object>) val;
                                matched.add(event);
                            }
                        }
                    } else {
                        Set<String> resultIds = intersectAll(byDimension.values());

                        if (resultIds.isEmpty()) {
                            Map<String, Set<String>> hardOnly = new HashMap<>();
                            for (Map.Entry<String, Set<String>> entry : byDimension.entrySet()) {
                                if (HARD_DIMENSIONS.contains(entry.getKey())) {
                                    hardOnly.put(entry.getKey(), entry.getValue());
                                }
                            }
                            if (!hardOnly.isEmpty()) {
                                resultIds = intersectAll(hardOnly.values());
                            }
                            // else: no HARD dimension was present at all — stays empty.
                        }

                        for (String eventId : resultIds) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> event = (Map<String, Object>)
                                psMain.selectOne(Path.key(params.familyId).key("events").key(eventId));
                            if (event != null) {
                                matched.add(event);
                            }
                        }
                    }

                    matched.sort((a, b) -> Long.compare(effectiveTime(a), effectiveTime(b)));

                    agentNode.emit("finalize", matched);
                })

            // ----------------------------------------------------------------
            // Node 4: finalize (terminal)
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, List<Map<String, Object>> matched) -> {
                    agentNode.result(matched);
                });
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static final Set<String> HARD_DIMENSIONS =
        Set.of("keywords", "dateRange", "accountLabel");

    private static Set<String> intersectAll(Collection<Set<String>> sets) {
        Iterator<Set<String>> it = sets.iterator();
        if (!it.hasNext()) return new HashSet<>();
        Set<String> result = new HashSet<>(it.next());
        while (it.hasNext()) {
            result.retainAll(it.next());
        }
        return result;
    }

    private QueryParams parseQueryParams(String json, String originalQuestion,
                                          String familyId, String timezone, String accountLabel) {
        try {
            String clean = json.replaceAll("```json", "").replaceAll("```", "").trim();
            // Simple field extraction without Jackson dependency
            String queryType      = extractJsonString(clean, "queryType", "GENERAL");
            String childName      = extractJsonString(clean, "childName", null);
            String dateFrom       = extractJsonString(clean, "dateFrom", null);
            String dateTo         = extractJsonString(clean, "dateTo", null);
            String categoryFilter = extractJsonString(clean, "categoryFilter", null);
            String siloFilter     = extractJsonString(clean, "siloFilter", null);
            String intentFilter   = extractJsonString(clean, "intentFilter", null);
            List<String> keywords = extractJsonStringArray(clean, "keywords");

            if ("null".equalsIgnoreCase(childName))      childName = null;
            if ("null".equalsIgnoreCase(dateFrom))       dateFrom = null;
            if ("null".equalsIgnoreCase(dateTo))         dateTo = null;
            if ("null".equalsIgnoreCase(categoryFilter)) categoryFilter = null;
            if ("null".equalsIgnoreCase(siloFilter))     siloFilter = null;
            if ("null".equalsIgnoreCase(intentFilter))   intentFilter = null;

            return new QueryParams(queryType, childName, dateFrom, dateTo,
                categoryFilter, originalQuestion, familyId, timezone, accountLabel,
                keywords, siloFilter, intentFilter);
        } catch (Exception e) {
            return new QueryParams("GENERAL", null, null, null, null,
                originalQuestion, familyId, timezone, accountLabel,
                new ArrayList<>(), null, null);
        }
    }

    private String extractJsonString(String json, String key, String defaultVal) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return defaultVal;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return defaultVal;
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf("\"", 1);
            return end > 0 ? rest.substring(1, end) : defaultVal;
        }
        if (rest.startsWith("null")) return null;
        return defaultVal;
    }

    /** Extracts a JSON string array like "keywords": ["a", "b"]. Missing/malformed => empty list. */
    private List<String> extractJsonStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return result;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return result;
        int open = json.indexOf("[", colon);
        int close = json.indexOf("]", colon);
        if (open < 0 || close < 0 || close < open) return result;
        String body = json.substring(open + 1, close);
        for (String part : body.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
                String value = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!value.isEmpty()) result.add(value);
            }
        }
        return result;
    }

    /** Start of dateStr's day in the given timezone. Falls back to a bare Instant parse for full timestamps. */
    private long parseToEpochStartOfDay(String dateStr, String timezone, long fallback) {
        if (dateStr == null) return fallback;
        try {
            return LocalDate.parse(dateStr).atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                return Instant.parse(dateStr).toEpochMilli();
            } catch (Exception e2) {
                return fallback;
            }
        }
    }

    /** End of dateStr's day (23:59:59.999) in the given timezone — makes a single-day range inclusive. */
    private long parseToEpochEndOfDay(String dateStr, String timezone, long fallback) {
        if (dateStr == null) return fallback;
        try {
            return LocalDate.parse(dateStr)
                .atTime(23, 59, 59, 999_000_000)
                .atZone(ZoneId.of(timezone))
                .toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                return Instant.parse(dateStr).toEpochMilli();
            } catch (Exception e2) {
                return fallback;
            }
        }
    }

    private boolean personMatches(Map<String, Object> event, String filter) {
        String lower = filter.toLowerCase();
        Object raw = event.get("personId");
        if (!(raw instanceof List)) return false;
        for (Object p : (List<?>) raw) {
            if (p != null && p.toString().toLowerCase().contains(lower)) return true;
        }
        return false;
    }

    private String formatEventsForPrompt(List<Map<String, Object>> events,
                                          String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("EEEE MMMM d 'at' h:mm a z").withZone(zone);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> e : events) {
            sb.append("- ").append(str(e.get("title"), "Untitled")).append("\n");
            sb.append("  Type: ").append(tagsDisplay(e.get("tags"), "unknown")).append("\n");
            Long st = toLong(e.get("startTime"));
            Long dl = toLong(e.get("deadline"));
            if (st != null) sb.append("  Date: ")
                .append(fmt.format(Instant.ofEpochMilli(st))).append("\n");
            if (dl != null) sb.append("  Deadline: ")
                .append(fmt.format(Instant.ofEpochMilli(dl))).append("\n");
            Object desc = e.get("description");
            if (desc != null) sb.append("  Notes: ")
                .append(desc.toString(), 0,
                    Math.min(desc.toString().length(), 200)).append("\n");
        }
        return sb.toString();
    }

    private String formatEventsPlain(List<Map<String, Object>> events, String timezone) {
        StringBuilder sb = new StringBuilder("Here's what I found:\n\n");
        ZoneId zone = ZoneId.of(timezone);
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("EEE MMM d 'at' h:mm a").withZone(zone);
        for (Map<String, Object> e : events) {
            sb.append("• ").append(str(e.get("title"), "Untitled")).append("\n");
            Long st = toLong(e.get("startTime"));
            Long dl = toLong(e.get("deadline"));
            if (st != null) sb.append("  When: ")
                .append(fmt.format(Instant.ofEpochMilli(st))).append("\n");
            if (dl != null) sb.append("  Due: ")
                .append(fmt.format(Instant.ofEpochMilli(dl))).append("\n");
        }
        return sb.toString();
    }
}
