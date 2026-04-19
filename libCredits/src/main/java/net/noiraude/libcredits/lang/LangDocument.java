package net.noiraude.libcredits.lang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory representation of a Minecraft {@code .lang} file.
 *
 * <p>
 * Preserves the original file structure (blank lines, comment lines, key-value order)
 * for all keys not owned by libCredits. Owned keys that are updated or deleted are
 * modified in place; newly added owned keys are appended at the end when writing.
 *
 * <p>
 * Write access ({@link #set} and {@link #remove}) is restricted to keys whose prefix
 * appears in the set supplied at construction time (see {@link LangParser#OWNED_PREFIXES}).
 * Any attempt to write a foreign key throws {@link IllegalArgumentException}, enforcing
 * that this library never alters content it does not own.
 *
 * <p>
 * Consecutive blank lines in the output are collapsed to a single blank line.
 *
 * <p>
 * Use {@link LangParser#parse(java.io.InputStream)} to create instances.
 */
public final class LangDocument {

    // ------------------------------------------------------------------
    // Internal line representation (package-private for LangParser)
    // ------------------------------------------------------------------

    interface Line {

        String toFileString();
    }

    static final class BlankLine implements Line {

        static final BlankLine INSTANCE = new BlankLine();

        @Contract(pure = true)
        @Override
        public @NotNull String toFileString() {
            return "";
        }
    }

    static final class CommentLine implements Line {

        final String text;

        @Contract(pure = true)
        CommentLine(String text) {
            this.text = text;
        }

        @Contract(pure = true)
        @Override
        public String toFileString() {
            return text;
        }
    }

    static final class KeyValueLine implements Line {

        final String key;
        String value;
        boolean deleted;

        @Contract(pure = true)
        KeyValueLine(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String toFileString() {
            return key + "=" + value;
        }
    }

    // ------------------------------------------------------------------

    /** Original lines in order; key-value lines are mutable in place. */
    private final List<Line> lines;

    /** Index of existing key-value lines for fast lookup. */
    private final Map<String, KeyValueLine> index;

    /** New key-value pairs not present in the original file, in insertion order. */
    private final Map<String, String> pendingInserts;

    /** Key prefixes this document is allowed to write. */
    private final Set<String> ownedPrefixes;

    /** Snapshot of key to value at construction or the last {@link #markClean()} call. */
    private final Map<String, String> loadedValues;

    // Package-private accessors for LangSerializer
    @Contract(pure = true)
    List<Line> lines() {
        return lines;
    }

    @Contract(pure = true)
    Map<String, String> pendingInserts() {
        return pendingInserts;
    }

    LangDocument(List<Line> lines, @NotNull Map<String, KeyValueLine> index, Set<String> ownedPrefixes) {
        this.lines = lines;
        this.index = index;
        this.ownedPrefixes = ownedPrefixes;
        this.pendingInserts = new LinkedHashMap<>();
        this.loadedValues = new LinkedHashMap<>();
        for (KeyValueLine line : index.values()) {
            loadedValues.put(line.key, line.value);
        }
    }

    // ------------------------------------------------------------------
    // Public API -- reads
    // ------------------------------------------------------------------

    /**
     * Returns the current value for {@code key}, or {@code null} if the key does not
     * exist or has been deleted. Reading is unrestricted to all keys.
     */
    public String get(String key) {
        KeyValueLine line = index.get(key);
        if (line != null && !line.deleted) return line.value;
        return pendingInserts.get(key);
    }

    /** Returns {@code true} if {@code key} has a value (original or pending insert). */
    public boolean contains(String key) {
        return get(key) != null;
    }

    // ------------------------------------------------------------------
    // Public API -- writes (restricted to owned prefixes)
    // ------------------------------------------------------------------

    /**
     * Sets the value for {@code key}.
     *
     * <p>
     * If the key already exists in the original file its line is updated in place.
     * If the key is new it is queued for appending when the document is written.
     *
     * @throws IllegalArgumentException if {@code key} does not start with an owned prefix
     */
    public void set(String key, String value) {
        requireOwnedKey(key);
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
     *
     * <p>
     * If the key exists in the original file its line is marked for deletion.
     * If it is only in the pending-insert queue it is removed from there.
     *
     * @throws IllegalArgumentException if {@code key} does not start with an owned prefix
     */
    public void remove(String key) {
        requireOwnedKey(key);
        KeyValueLine existing = index.get(key);
        if (existing != null) existing.deleted = true;
        pendingInserts.remove(key);
    }

    // ------------------------------------------------------------------
    // Public API -- dirty tracking
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if the document's effective content differs from the state
     * it had at construction time or at the last {@link #markClean()} call.
     *
     * <p>
     * A key set back to its original value is treated as clean, so this comparison
     * is content-based rather than operation-based.
     */
    public boolean isDirty() {
        for (KeyValueLine line : index.values()) {
            if (line.deleted) return true;
            if (!line.value.equals(loadedValues.get(line.key))) return true;
        }
        return !pendingInserts.isEmpty();
    }

    /**
     * Resets dirty tracking so that the current state is treated as clean.
     * Call immediately after a successful save.
     *
     * <p>
     * Three structural changes are made so that subsequent dirty checks are accurate:
     * <ol>
     * <li>Deleted lines are removed from the line list and the index entirely,
     * because they have been written out as absent.
     * <li>Pending-insert entries are promoted into the indexed line structure,
     * because they have been written out and now belong to the file.
     * <li>The clean-state snapshot is rebuilt from the resulting index.
     * </ol>
     */
    public void markClean() {
        // Remove deleted lines completely - they no longer exist in the file.
        lines.removeIf(line -> line instanceof KeyValueLine && ((KeyValueLine) line).deleted);
        index.entrySet()
            .removeIf(e -> e.getValue().deleted);

        // Promote pending inserts to real indexed lines so future dirty checks
        // compare against them like any other on-disk entry.
        for (Map.Entry<String, String> entry : pendingInserts.entrySet()) {
            KeyValueLine kvLine = new KeyValueLine(entry.getKey(), entry.getValue());
            lines.add(kvLine);
            index.put(entry.getKey(), kvLine);
        }
        pendingInserts.clear();

        // Rebuild the clean-state snapshot from the now-stable index.
        loadedValues.clear();
        for (KeyValueLine line : index.values()) {
            loadedValues.put(line.key, line.value);
        }
    }

    // ------------------------------------------------------------------

    private void requireOwnedKey(String key) {
        for (String prefix : ownedPrefixes) {
            if (key.startsWith(prefix)) return;
        }
        throw new IllegalArgumentException(
            "Key \"" + key + "\" is not under an owned prefix; write access is restricted to libCredits-owned keys.");
    }
}
