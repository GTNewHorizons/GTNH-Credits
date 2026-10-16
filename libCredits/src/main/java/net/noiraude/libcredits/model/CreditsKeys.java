package net.noiraude.libcredits.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Identifier and language-key rules shared by the reader, editor and validator. */
public final class CreditsKeys {

    private static final Pattern ID = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");

    private CreditsKeys() {}

    public static boolean isValidId(String value) {
        return value != null && value.length() <= 32
            && ID.matcher(value)
                .matches();
    }

    public static boolean isValidTranslationKey(String value) {
        return value != null && !value.isEmpty()
            && value.codePointCount(0, value.length()) <= 512
            && value.chars()
                .noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c));
    }

    /** Normalizes credits-owned ids only; external translation keys must never pass through this method. */
    public static String sanitize(String value) {
        return value.replace(".", "")
            .replace("-", "")
            .replaceAll(" +", "_")
            .toLowerCase(Locale.ROOT);
    }
}
