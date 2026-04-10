package net.noiraude.creditseditor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

/**
 * Derived, read-only view of all roles present in an {@link EditorModel}.
 *
 * <p>
 * Built on demand via {@link #build(EditorModel)}. Not updated automatically when the model
 * changes; rebuild after each mutation that may affect roles.
 */
public final class RoleIndex {

    /** A single role aggregated across all persons and category memberships. */
    public static final class Entry {

        /** The role string as stored in the JSON. */
        public final String raw;

        /** The lang key derived from the raw value: {@code credits.person.role.{sanitized}}. */
        public final String langKey;

        /** Number of distinct persons carrying this role (in any category). */
        public final int count;

        /** Ids of the categories where this role appears (in encounter order). */
        public final List<String> categoryIds;

        Entry(String raw, String langKey, int count, List<String> categoryIds) {
            this.raw = raw;
            this.langKey = langKey;
            this.count = count;
            this.categoryIds = categoryIds;
        }
    }

    private final List<Entry> entries;

    private RoleIndex(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Builds a {@link RoleIndex} by scanning the entire model.
     *
     * <p>
     * The resulting entry list is sorted alphabetically by {@link Entry#raw}.
     */
    public static RoleIndex build(EditorModel model) {
        Map<String, Set<String>> roleToCats = new LinkedHashMap<>();
        Map<String, Set<EditorPerson>> roleToPersons = new LinkedHashMap<>();

        for (EditorPerson person : model.persons) {
            for (EditorMembership membership : person.memberships) {
                for (String role : membership.roles) {
                    roleToCats.computeIfAbsent(role, k -> new LinkedHashSet<>())
                        .add(membership.categoryId);
                    roleToPersons.computeIfAbsent(role, k -> new HashSet<>())
                        .add(person);
                }
            }
        }

        List<Entry> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : roleToCats.entrySet()) {
            String role = e.getKey();
            String langKey = "credits.person.role." + KeySanitizer.sanitize(role);
            int count = roleToPersons.get(role)
                .size();
            result.add(new Entry(role, langKey, count, List.copyOf(e.getValue())));
        }
        result.sort(Comparator.comparing(e -> e.raw));

        return new RoleIndex(Collections.unmodifiableList(result));
    }

    /** Returns an immutable alphabetically sorted list of all role entries. */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Returns all distinct role strings that appear in any membership of {@code catId}.
     *
     * @return immutable set in encounter order
     */
    public Set<String> rolesForCategory(String catId) {
        Set<String> result = new LinkedHashSet<>();
        for (Entry e : entries) {
            if (e.categoryIds.contains(catId)) {
                result.add(e.raw);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns {@code true} if the index contains at least one occurrence of {@code role}. */
    public boolean contains(String role) {
        for (Entry e : entries) {
            if (e.raw.equals(role)) return true;
        }
        return false;
    }
}
