package com.family.assistant.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventUtils {

    private EventUtils() {}

    private static final Set<String> STOPWORDS = Set.of(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of",
        "with", "is", "are", "was", "were", "be", "been", "this", "that", "these",
        "those", "it", "its", "by", "as", "from", "your", "you", "we", "our", "i",
        "me", "my", "if", "no", "not", "do", "does", "did"
    );

    private static final int MIN_TOKEN_LENGTH = 3;

    /**
     * Lowercase, split on non-alphanumeric runs, drop tokens shorter than
     * MIN_TOKEN_LENGTH, drop STOPWORDS, dedupe. Shared by index-time
     * (FamilySchemaModule) and query-time (QueryModule's search-agent) so
     * both sides can never tokenize the same text two different ways.
     */
    public static List<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return new ArrayList<>(tokens);
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() >= MIN_TOKEN_LENGTH && !STOPWORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return new ArrayList<>(tokens);
    }

    /** Tokenizes title + description + emailSubject from a stored event record. */
    public static List<String> tokenizeEvent(Map<String, Object> record) {
        String title        = str(record.get("title"), "");
        String description  = str(record.get("description"), "");
        String emailSubject = str(record.get("emailSubject"), "");
        return tokenize(title + " " + description + " " + emailSubject);
    }

    public static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            try {
                String normalized = s.length() == 19 ? s + "Z" : s;
                return Instant.parse(normalized).toEpochMilli();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static long effectiveTime(Map<String, Object> event) {
        Long s = toLong(event.get("startTime"));
        Long d = toLong(event.get("deadline"));
        if (s != null) return s;
        if (d != null) return d;
        return Long.MAX_VALUE;
    }

    public static String str(Object val, String fallback) {
        if (val == null) return fallback;
        String s = val.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    /**
     * Renders a record's multi-valued tags field for display, joined with ", ".
     * Returns {@code fallback} when the value is absent, not a list, or empty.
     */
    public static String tagsDisplay(Object rawTags, String fallback) {
        if (!(rawTags instanceof List)) return fallback;
        List<?> tags = (List<?>) rawTags;
        if (tags.isEmpty()) return fallback;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(str(tags.get(i), fallback));
        }
        return sb.toString();
    }
}
