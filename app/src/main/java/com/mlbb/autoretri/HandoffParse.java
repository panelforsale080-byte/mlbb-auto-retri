package com.mlbb.autoretri;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tolerant key=value parsing for handoff files: handles glued text like
 * {@code my_level=15tap_x=1575.0} and multiple pasted blocks (last key wins).
 */
public final class HandoffParse {
    /** Keys sorted longest-first so "tap_x" beats "tap" if both existed. */
    private static final String[] ALL_KEYS = {
        "best_dist",
        "retri_dmg",
        "best_hp",
        "best_id",
        "my_level",
        "trigger",
        "turtle",
        "ready",
        "lord",
        "blue",
        "crab",
        "lito",
        "red",
        "seq",
        "ver",
        "tap_x",
        "tap_y",
        "ts",
    };

    private static final Pattern KEY_EQUALS;

    static {
        StringBuilder alt = new StringBuilder();
        for (int i = 0; i < ALL_KEYS.length; i++) {
            if (i > 0) alt.append('|');
            alt.append(Pattern.quote(ALL_KEYS[i]));
        }
        KEY_EQUALS = Pattern.compile("(" + alt + ")=");
    }

    private HandoffParse() {}

    public static String readFileText(File f, int maxBytes) throws IOException {
        if (!f.isFile()) return null;
        long len = f.length();
        if (len > maxBytes) len = maxBytes;
        byte[] buf = new byte[(int) len];
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            int n = in.read(buf);
            if (n <= 0) return "";
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        }
    }

    /** Parse file; only keys in {@code allow} are stored (last occurrence wins). */
    public static Map<String, String> parse(File f, Set<String> allow, int maxBytes) throws IOException {
        String text = readFileText(f, maxBytes);
        if (text == null) return Map.of();
        return parseString(text, allow);
    }

    public static Map<String, String> parseString(String text, Set<String> allow) {
        Map<String, String> out = new HashMap<>();
        if (text == null) return out;
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        Matcher m = KEY_EQUALS.matcher(normalized);
        List<Integer> starts = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
            keys.add(m.group(1));
        }
        for (int i = 0; i < starts.size(); i++) {
            String k = keys.get(i);
            if (!allow.contains(k)) continue;
            int eqAfter = starts.get(i) + k.length() + 1;
            int valEnd = (i + 1 < starts.size()) ? starts.get(i + 1) : normalized.length();
            String val = normalized.substring(eqAfter, valEnd).trim();
            int nl = val.indexOf('\n');
            if (nl >= 0) val = val.substring(0, nl).trim();
            out.put(k, val);
        }
        return out;
    }
}
