package net.noiraude.creditseditor.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of a Minecraft {@code .lang} file.
 *
 * <p>
 * Preserves the original file structure (blank lines, comment lines, key-value order)
 * for all keys not touched by the editor. Keys that are updated or deleted are modified
 * in place. Keys that are newly added by the editor are appended at the end of the file
 * when writing, in the order they were first set.
 *
 * <p>
 * Consecutive blank lines in the output are collapsed to a single blank line to avoid
 * accumulating whitespace when keys are deleted.
 *
 * <p>
 * Use {@link LangService#load(java.io.InputStream)} to create instances.
 */
public final class LangDocument {

    // -----------------------------------------------------------------------
    // Internal line representation
    // -----------------------------------------------------------------------

    interface Line {

        String toFileString();
    }

    static final class BlankLine implements Line {

        static final BlankLine INSTANCE = new BlankLine();

        @Override
        public String toFileString() {
            return "";
        }
    }

    static final class CommentLine implements Line {

        final String text;

        CommentLine(String text) {
            this.text = text;
        }

        @Override
        public String toFileString() {
            return text;
        }
    }

    static final class KeyValueLine implements Line {

        final String key;
        String value;
        boolean deleted;

        KeyValueLine(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toFileString() {
            return key + "=" + value;
        }
    }

    // -----------------------------------------------------------------------

    /** Original lines in order, with key-value lines mutable. */
    private final List<Line> lines;

    /**
     * Index of existing key-value lines, for fast lookup.
     * Deleted lines remain in this index until writing time.
     */
    private final Map<String, KeyValueLine> index;

    /**
     * New key-value pairs to append, in insertion order.
     * These are keys that were not present in the original file.
     */
    private final Map<String, String> pendingInserts;

    LangDocument(List<Line> lines, Map<String, KeyValueLine> index) {
        this.lines = lines;
        this.index = index;
        this.pendingInserts = new LinkedHashMap<>();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the current value for {@code key}, or {@code null} if the key does not exist
     * or has been deleted.
     */
    public String get(String key) {
        KeyValueLine line = index.get(key);
        if (line != null && !line.deleted) {
            return line.value;
        }
        return pendingInserts.get(key);
    }

    /**
     * Sets the value for {@code key}.
     * If the key already exists in the original file, its line is updated in place.
     * If the key is new, it is queued for appending at the end of the file when writing.
     */
    public void set(String key, String value) {
        KeyValueLine existing = index.get(key);
        if (existing != null) {
            existing.value = value;
            existing.deleted = false;
        } else {
            pendingInserts.put(key, value);
        }
    }

    /**
     * Removes the value for {@code key}.
     * If the key exists in the original file, its line is marked for deletion.
     * If the key is only in the pending-insert queue, it is removed from there.
     */
    public void remove(String key) {
        KeyValueLine existing = index.get(key);
        if (existing != null) {
            existing.deleted = true;
        }
        pendingInserts.remove(key);
    }

    /** Returns {@code true} if {@code key} has a value (original or pending insert). */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Writes the document to {@code out} as UTF-8 text.
     *
     * <p>
     * Foreign lines are written in their original order. Deleted key-value lines are
     * skipped. Pending inserts are appended after all original content, preceded by a
     * blank line if the file does not already end with one. Consecutive blank lines in
     * the output are collapsed to a single blank line.
     *
     * @throws IOException if the stream cannot be written
     */
    public void writeTo(OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean lastWasBlank = false;

        for (Line line : lines) {
            if (line instanceof KeyValueLine && ((KeyValueLine) line).deleted) {
                continue;
            }
            if (line instanceof BlankLine) {
                if (!lastWasBlank) {
                    sb.append('\n');
                    lastWasBlank = true;
                }
            } else {
                sb.append(line.toFileString())
                    .append('\n');
                lastWasBlank = false;
            }
        }

        if (!pendingInserts.isEmpty()) {
            if (!lastWasBlank && !sb.isEmpty()) {
                sb.append('\n');
            }
            for (Map.Entry<String, String> entry : pendingInserts.entrySet()) {
                sb.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append('\n');
            }
        }

        out.write(
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------
    // Package-private helpers used by LangService
    // -----------------------------------------------------------------------

    static LangDocument empty() {
        return new LangDocument(new ArrayList<>(), new LinkedHashMap<>());
    }
}
