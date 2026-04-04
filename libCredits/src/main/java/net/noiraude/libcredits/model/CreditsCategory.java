package net.noiraude.libcredits.model;

import java.util.Set;

public final class CreditsCategory {

    public final String id;
    /** Semantic class markers, e.g. {@code "person"}, {@code "role"}, {@code "detail"}. */
    public final Set<String> classes;

    public CreditsCategory(String id, Set<String> classes) {
        this.id = id;
        this.classes = classes;
    }
}
