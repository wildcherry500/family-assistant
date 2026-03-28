package com.family.assistant;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * PersonalAssistantConfig
 *
 * Loads assistant configuration from application.properties (classpath),
 * with environment variable overrides.
 *
 * Env var overrides (take precedence over application.properties):
 *   PA_OWNER_NAME        — owner's display name
 *   PA_GMAIL_ACCOUNTS    — comma-separated Gmail account emails to monitor
 *   PA_EVENT_CATEGORIES  — comma-separated event category names
 *   PA_TIMEZONE          — IANA timezone, e.g. "America/Los_Angeles"
 */
public class PersonalAssistantConfig {

    public final String ownerName;
    public final List<String> gmailAccounts;
    public final List<String> eventCategories;
    public final String timezone;

    private PersonalAssistantConfig(String ownerName, List<String> gmailAccounts,
                                     List<String> eventCategories, String timezone) {
        this.ownerName       = ownerName;
        this.gmailAccounts   = gmailAccounts;
        this.eventCategories = eventCategories;
        this.timezone        = timezone;
    }

    /**
     * Loads configuration from application.properties on the classpath,
     * with environment variable overrides for each field.
     */
    public static PersonalAssistantConfig load() {
        Properties props = new Properties();
        try (InputStream is = PersonalAssistantConfig.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {
            // no file — fall through to env vars / defaults
        }

        String ownerName = coalesce(
            System.getenv("PA_OWNER_NAME"),
            props.getProperty("pa.owner.name"),
            "Owner");

        List<String> gmailAccounts = splitCsv(coalesce(
            System.getenv("PA_GMAIL_ACCOUNTS"),
            props.getProperty("pa.gmail.accounts"),
            ""));

        List<String> eventCategories = splitCsv(coalesce(
            System.getenv("PA_EVENT_CATEGORIES"),
            props.getProperty("pa.event.categories"),
            "SCHOOL_EVENT,DEADLINE,PERMISSION_SLIP,TASK,UNKNOWN"));

        String timezone = coalesce(
            System.getenv("PA_TIMEZONE"),
            props.getProperty("pa.timezone"),
            "America/Los_Angeles");

        return new PersonalAssistantConfig(ownerName, gmailAccounts, eventCategories, timezone);
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
