package net.noiraude.libcredits.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses Minecraft {@code .lang} files into {@link LangDocument} instances.
 *
 * <p>
 * This class is the single definition point for the key prefixes that libCredits owns.
 * {@link LangDocument} enforces write access to only those prefixes, ensuring that
 * foreign content (GUI strings, other mods' keys) is never altered.
 */
public final class LangParser {

    /** Prefix for category name and detail keys ({@code credits.category.{key}}). */
    public static final String CATEGORY_PREFIX = "credits.category.";

    /** Prefix for role display-name keys ({@code credits.person.role.{sanitized}}). */
    public static final String ROLE_PREFIX = "credits.person.role.";

    /**
     * The complete set of key prefixes owned by libCredits, in declaration order.
     * Any key under one of these prefixes may be written by the editor; all other
     * keys are treated as read-only foreign content.
     */
    public static final Set<String> OWNED_PREFIXES = Collections
        .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(CATEGORY_PREFIX, ROLE_PREFIX)));

    private LangParser() {}

    /**
     * Parses a {@code .lang} input stream into a {@link LangDocument}.
     * The caller is responsible for closing the stream.
     *
     * <p>
     * Blank lines, comment lines ({@code #}), and the order of all key-value lines are
     * preserved exactly. Malformed lines (no {@code =} sign) are preserved as
     * comment-like entries.
     *
     * @throws IOException if the stream cannot be read
     */
    public static LangDocument parse(InputStream in) throws IOException {
        List<LangDocument.Line> lines = new ArrayList<>();
        Map<String, LangDocument.KeyValueLine> index = new LinkedHashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String raw;
        while ((raw = reader.readLine()) != null) {
            if (raw.isEmpty()) {
                lines.add(LangDocument.BlankLine.INSTANCE);
            } else if (raw.startsWith("#")) {
                lines.add(new LangDocument.CommentLine(raw));
            } else {
                int eq = raw.indexOf('=');
                if (eq > 0) {
                    String key = raw.substring(0, eq);
                    String value = raw.substring(eq + 1);
                    LangDocument.KeyValueLine kvLine = new LangDocument.KeyValueLine(key, value);
                    lines.add(kvLine);
                    index.put(key, kvLine);
                } else {
                    lines.add(new LangDocument.CommentLine(raw));
                }
            }
        }

        return new LangDocument(lines, index, OWNED_PREFIXES);
    }

    /**
     * Returns an empty {@link LangDocument} for new resources that have no lang file yet.
     */
    public static LangDocument empty() {
        return new LangDocument(new ArrayList<>(), new LinkedHashMap<>(), OWNED_PREFIXES);
    }
}
