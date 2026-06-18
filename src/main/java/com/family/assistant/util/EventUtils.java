package com.family.assistant.util;

import java.time.Instant;
import java.util.Map;

public final class EventUtils {

    private EventUtils() {}

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
}
