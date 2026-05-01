package net.noiraude.libcredits.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable representation of a category as held in a {@link CreditsDocument}.
 *
 * <p>
 * Lang-derived display fields (display name, description) are not stored here; callers
 * retrieve them from the lang document using the key derived from {@link #id}.
 */
public final class DocumentCategory {

    /** Category id as stored in {@code credits.json}. */
    public String id;

    /** Semantic class markers ({@code person}, {@code role}, {@code detail}). */
    public Set<String> classes;

    public DocumentCategory(String id) {
        this.id = id;
        this.classes = new LinkedHashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentCategory)) return false;
        DocumentCategory other = (DocumentCategory) o;
        return id.equals(other.id) && classes.equals(other.classes);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + classes.hashCode();
    }
}
