package net.noiraude.libcredits.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Mutable, owning document for credits data.
 *
 * <p>
 * Holds the live {@link DocumentCategory} and {@link DocumentPerson} lists that callers
 * mutate directly. Tracks whether those lists have diverged from the on-disk baseline so
 * that callers can query {@link #isDirty()} without supplying any external snapshot.
 *
 * <p>
 * Comparison is structural: two states are considered equal when their category lists are
 * identical in order, their person lists are identical in order, and each person's category
 * membership entries are in the same order with the same roles. Category class sets are
 * compared without regard to ordering.
 *
 * <p>
 * Use {@link #empty()} to create an empty document, then populate and call {@link #markClean()}
 * to establish the baseline. {@link net.noiraude.libcredits.parser.CreditsParser} does this
 * automatically when parsing from a stream.
 */
public final class CreditsDocument {

    /** Live, mutable list of categories. Callers may add, remove, or reorder entries. */
    public final List<DocumentCategory> categories;

    /** Live, mutable list of persons. Callers may add, remove, or reorder entries. */
    public final List<DocumentPerson> persons;

    private List<DocumentCategory> baselineCategories;
    private List<DocumentPerson> baselinePersons;

    private CreditsDocument() {
        this.categories = new ArrayList<>();
        this.persons = new ArrayList<>();
        this.baselineCategories = new ArrayList<>();
        this.baselinePersons = new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // Factory
    // ------------------------------------------------------------------

    /**
     * Creates an empty document. {@link #isDirty()} returns {@code false} as long as
     * both lists remain empty.
     */
    public static CreditsDocument empty() {
        return new CreditsDocument();
    }

    // ------------------------------------------------------------------
    // Dirty tracking
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if the current lists differ structurally from the baseline.
     */
    public boolean isDirty() {
        return !categories.equals(baselineCategories) || !persons.equals(baselinePersons);
    }

    /**
     * Resets the baseline to the current list state.
     * Call immediately after a successful save, or after the initial population from a parser.
     */
    public void markClean() {
        this.baselineCategories = deepCopyCategories(categories);
        this.baselinePersons = deepCopyPersons(persons);
    }

    // ------------------------------------------------------------------

    private static List<DocumentCategory> deepCopyCategories(List<DocumentCategory> src) {
        List<DocumentCategory> copy = new ArrayList<>(src.size());
        for (DocumentCategory dc : src) {
            DocumentCategory c = new DocumentCategory(dc.id);
            c.classes = new LinkedHashSet<>(dc.classes);
            copy.add(c);
        }
        return copy;
    }

    private static List<DocumentPerson> deepCopyPersons(List<DocumentPerson> src) {
        List<DocumentPerson> copy = new ArrayList<>(src.size());
        for (DocumentPerson dp : src) {
            DocumentPerson p = new DocumentPerson(dp.name);
            for (DocumentMembership dm : dp.memberships) {
                p.memberships.add(new DocumentMembership(dm.categoryId, dm.roles));
            }
            copy.add(p);
        }
        return copy;
    }
}
