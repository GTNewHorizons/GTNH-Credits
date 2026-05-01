package net.noiraude.libcredits.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable representation of a person's membership in one category, including the roles
 * they hold within that category, as held in a {@link CreditsDocument}.
 */
public final class DocumentMembership {

    /** {@code Id} of the category this membership belongs to. */
    public final String categoryId;

    /** Ordered list of role strings held in this category membership. Maybe empty. */
    public final List<String> roles;

    public DocumentMembership(String categoryId) {
        this.categoryId = categoryId;
        this.roles = new ArrayList<>();
    }

    public DocumentMembership(String categoryId, List<String> roles) {
        this.categoryId = categoryId;
        this.roles = new ArrayList<>(roles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentMembership)) return false;
        DocumentMembership other = (DocumentMembership) o;
        return categoryId.equals(other.categoryId) && roles.equals(other.roles);
    }

    @Override
    public int hashCode() {
        return 31 * categoryId.hashCode() + roles.hashCode();
    }
}
