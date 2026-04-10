package net.noiraude.creditseditor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and saves {@code lang/en_US.lang} files while preserving their structure.
 *
 * <p>
 * The lang file is treated as a structured document: blank lines, comment lines
 * ({@code #}), and the order of all foreign keys are preserved exactly. Only the
 * editor-owned key prefixes ({@code credits.category.*} and
 * {@code credits.person.role.*}) are updated or removed by the editor; all other content
 * is written back unchanged.
 */
public final class LangService {

    /** Relative path inside a resource pack. */
    static final String LANG_PATH = "assets/gtnhcredits/lang/en_US.lang";

    private LangService() {}

    /**
     * Parses an {@code en_US.lang} input stream into a {@link LangDocument}.
     * The caller is responsible for closing the stream.
     *
     * @throws IOException if the stream cannot be read
     */
    public static LangDocument load(InputStream in) throws IOException {
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
                    // Malformed line: preserve as a comment-like entry
                    lines.add(new LangDocument.CommentLine(raw));
                }
            }
        }

        return new LangDocument(lines, index);
    }

    /**
     * Loads {@code lang/en_US.lang} from the resource manager.
     * Returns an empty {@link LangDocument} if the file does not exist.
     *
     * @throws IOException if the file exists but cannot be read
     */
    public static LangDocument load(net.noiraude.creditseditor.ResourceManager rm) throws IOException {
        if (rm.notExists(LANG_PATH)) {
            return LangDocument.empty();
        }
        try (InputStream in = rm.openRead(LANG_PATH)) {
            return load(in);
        }
    }

    /**
     * Saves the {@link LangDocument} to {@code lang/en_US.lang} via the resource manager.
     *
     * @throws IOException if the file cannot be written
     */
    public static void save(LangDocument doc, net.noiraude.creditseditor.ResourceManager rm) throws IOException {
        try (OutputStream out = rm.openWrite(LANG_PATH)) {
            doc.writeTo(out);
        }
    }
}
