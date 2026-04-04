package net.noiraude.libcredits.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CreditsPerson {

    public final String name;
    /**
     * Maps each category id this person belongs to, to the roles they hold in that category.
     * The roles list is empty when no roles are defined for that category.
     */
    public final Map<String, List<String>> categoryRoles;

    public CreditsPerson(String name, Map<String, List<String>> categoryRoles) {
        this.name = name;
        this.categoryRoles = Collections.unmodifiableMap(categoryRoles);
    }
}
