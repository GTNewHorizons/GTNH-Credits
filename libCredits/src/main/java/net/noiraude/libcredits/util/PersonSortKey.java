package net.noiraude.libcredits.util;

/**
 * Computes the canonical sort key for a person name.
 *
 * <p>
 * The algorithm strips Minecraft {@code §x} formatting codes, removes dots and hyphens,
 * collapses runs of spaces into a single underscore, and lowercases the result. This
 * matches the lang-key sanitization used by the GTNH Credits mod at runtime, ensuring
 * that the serialized person order is deterministic and locale-independent.
 */
public final class PersonSortKey {

    private PersonSortKey() {}

    /** Returns the sort key for the given raw person name. */
    public static String of(String name) {
        return sanitize(stripFormatting(name));
    }

    private static String stripFormatting(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == '\u00a7' && i + 1 < len) {
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String sanitize(String value) {
        return value.replace(".", "")
            .replace("-", "")
            .replaceAll(" +", "_")
            .toLowerCase();
    }
}
