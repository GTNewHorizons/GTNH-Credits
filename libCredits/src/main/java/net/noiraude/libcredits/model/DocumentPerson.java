package net.noiraude.libcredits.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable representation of a person as held in a {@link CreditsDocument}.
 *
 * <p>
 * {@code name} holds the raw string as stored in {@code credits.json} and may contain
 * Minecraft {@code §} formatting codes.
 */
public final class DocumentPerson {

    /**
     * Raw person name as stored in {@code credits.json}.
     * May contain Minecraft {@code §} formatting codes.
     */
    public String name;

    /** Ordered list of category memberships. */
    public final List<DocumentMembership> memberships;

    public DocumentPerson(String name) {
        this.name = name;
        this.memberships = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentPerson)) return false;
        DocumentPerson other = (DocumentPerson) o;
        return name.equals(other.name) && memberships.equals(other.memberships);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + memberships.hashCode();
    }
}
