package net.noiraude.creditseditor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable representation of a person's membership in one category, including the roles
 * they hold within that category.
 */
public final class EditorMembership {

    /** Id of the category this membership belongs to. */
    public String categoryId;

    /** Ordered list of role strings held in this category membership. May be empty. */
    public List<String> roles;

    public EditorMembership(String categoryId) {
        this.categoryId = categoryId;
        this.roles = new ArrayList<>();
    }

    public EditorMembership(String categoryId, List<String> roles) {
        this.categoryId = categoryId;
        this.roles = new ArrayList<>(roles);
    }
}
