package net.noiraude.libcredits.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Logical access path for a detail entry whose content spans a plain
 * {@code <prefix>.detail} key plus any number of {@code <prefix>.detail.<suffix>}
 * indexed paragraph siblings.
 *
 * <p>
 * Reads return the plain value joined with every indexed sibling's value, sorted in
 * natural order of the suffix and separated by the literal two-character sequence
 * {@code \n}. Writes and removes erase every sibling so the on-disk form is normalized
 * to a single plain entry that the lang file's literal {@code \n} convention already
 * represents as paragraph breaks.
 */
public final class DetailLangKey extends LangKey {

    private static final @NotNull String DETAIL_SUFFIX = ".detail";
    /** Literal two-character {@code \n}, the lang-file paragraph separator. */
    private static final @NotNull String PARAGRAPH_SEPARATOR = "\\n";

    private final @NotNull String indexedPrefix;

    /**
     * @param prefix the lang key the detail entry hangs off; the full physical key is
     *               {@code <prefix>.detail} and indexed siblings are
     *               {@code <prefix>.detail.<suffix>}.
     */
    @Contract(pure = true)
    public DetailLangKey(@NotNull String prefix) {
        super(prefix + DETAIL_SUFFIX);
        this.indexedPrefix = key + ".";
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Joins the plain key's value with every indexed paragraph sibling's value,
     *           sorted in natural order of the suffix, separated by the literal {@code \n}
     *           sequence used as a paragraph break in lang files.
     */
    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> read(@NotNull LangDocument doc) {
        Optional<String> plain = super.read(doc);
        List<IndexedParagraph> indexed = collectIndexedSiblings(doc);
        if (!plain.isPresent() && indexed.isEmpty()) return Optional.empty();
        Collections.sort(indexed);
        return Optional.of(joinParagraphs(plain, indexed));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Writes the plain key and erases every indexed paragraph sibling so the
     *           on-disk form is normalized to a single plain entry.
     */
    @Override
    public void write(@NotNull LangDocument doc, @NotNull String value) {
        super.write(doc, value);
        clearIndexedSiblings(doc);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Removes the plain key and every indexed paragraph sibling.
     */
    @Override
    public void remove(@NotNull LangDocument doc) {
        super.remove(doc);
        clearIndexedSiblings(doc);
    }

    private @NotNull List<IndexedParagraph> collectIndexedSiblings(@NotNull LangDocument doc) {
        List<IndexedParagraph> out = new ArrayList<>();
        for (String siblingKey : doc.keysStartingWith(indexedPrefix)) {
            if (siblingKey.length() <= indexedPrefix.length()) continue;
            String suffix = siblingKey.substring(indexedPrefix.length());
            String value = Objects.requireNonNull(doc.get(siblingKey), "active key without value: " + siblingKey);
            out.add(new IndexedParagraph(suffix, value));
        }
        return out;
    }

    private void clearIndexedSiblings(@NotNull LangDocument doc) {
        for (String siblingKey : doc.keysStartingWith(indexedPrefix)) {
            if (siblingKey.length() > indexedPrefix.length()) doc.remove(siblingKey);
        }
    }

    @Contract(pure = true)
    private static @NotNull String joinParagraphs(@NotNull Optional<String> plain,
        @NotNull List<IndexedParagraph> indexed) {
        StringBuilder out = new StringBuilder();
        plain.ifPresent(out::append);
        boolean prependSeparator = plain.isPresent();
        for (IndexedParagraph p : indexed) {
            if (prependSeparator) out.append(PARAGRAPH_SEPARATOR);
            out.append(p.value);
            prependSeparator = true;
        }
        return out.toString();
    }

    /** Indexed paragraph entry sortable in natural order so {@code "1" < "2" < "10"}. */
    private static final class IndexedParagraph implements Comparable<IndexedParagraph> {

        final @NotNull String suffix;
        final @NotNull String value;

        @Contract(pure = true)
        IndexedParagraph(@NotNull String suffix, @NotNull String value) {
            this.suffix = suffix;
            this.value = value;
        }

        @Override
        public int compareTo(@NotNull IndexedParagraph other) {
            int ai = 0;
            int bi = 0;
            int an = suffix.length();
            int bn = other.suffix.length();
            while (ai < an && bi < bn) {
                boolean ad = Character.isDigit(suffix.charAt(ai));
                boolean bd = Character.isDigit(other.suffix.charAt(bi));
                int aj = ai + 1;
                while (aj < an && Character.isDigit(suffix.charAt(aj)) == ad) aj++;
                int bj = bi + 1;
                while (bj < bn && Character.isDigit(other.suffix.charAt(bj)) == bd) bj++;
                String at = suffix.substring(ai, aj);
                String bt = other.suffix.substring(bi, bj);
                int c = (ad && bd && at.length() != bt.length()) ? at.length() - bt.length() : at.compareTo(bt);
                if (c != 0) return c;
                ai = aj;
                bi = bj;
            }
            return (an - ai) - (bn - bi);
        }
    }
}
