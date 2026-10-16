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

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Derived, read-only view of all roles present in a {@link CreditsDocument}.
 *
 * <p>
 * Built on demand via {@link #build(CreditsDocument)}. Not updated automatically when the
 * document changes; rebuild after each mutation that may affect roles.
 */
public final class RoleIndex {

    /** A single role aggregated across all persons and category memberships. */
    public static final class Entry {

        /** Role identity, including whether its value is credits-owned or external. */
        public final @NotNull DocumentRole raw;

        /** Credits-owned lang key or the exact external translation key. */
        public final @NotNull String langKey;

        /** Number of distinct persons carrying this role (in any category). */
        public final int count;

        /** Ids of the categories where this role appears (in encounter order). */
        public final @NotNull List<String> categoryIds;

        @Contract(pure = true)
        Entry(@NotNull DocumentRole raw, @NotNull String langKey, int count, @NotNull List<String> categoryIds) {
            this.raw = raw;
            this.langKey = langKey;
            this.count = count;
            this.categoryIds = categoryIds;
        }
    }

    private final @NotNull List<Entry> entries;

    @Contract(pure = true)
    private RoleIndex(@NotNull List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Builds a {@link RoleIndex} by scanning the entire document.
     *
     * <p>
     * Entries are sorted by raw value, with local roles before external references when
     * the values match. These remain separate entries because their ownership differs.
     */
    @Contract("_ -> new")
    public static @NotNull RoleIndex build(@NotNull CreditsDocument creditsDoc) {
        Map<DocumentRole, Set<String>> roleToCats = new LinkedHashMap<>();
        Map<DocumentRole, Set<DocumentPerson>> roleToPersons = new LinkedHashMap<>();

        for (DocumentPerson person : creditsDoc.persons) {
            for (DocumentMembership membership : person.memberships) {
                for (DocumentRole role : membership.roles) {
                    roleToCats.computeIfAbsent(role, k -> new LinkedHashSet<>())
                        .add(membership.categoryId);
                    roleToPersons.computeIfAbsent(role, k -> new HashSet<>())
                        .add(person);
                }
            }
        }

        List<Entry> result = new ArrayList<>();
        for (Map.Entry<DocumentRole, Set<String>> e : roleToCats.entrySet()) {
            DocumentRole role = e.getKey();
            String langKey = role.translationKey();
            int count = roleToPersons.get(role)
                .size();
            result.add(new Entry(role, langKey, count, List.copyOf(e.getValue())));
        }
        result.sort(
            Comparator.comparing((Entry e) -> e.raw.value())
                .thenComparing(e -> e.raw.isExternal()));

        return new RoleIndex(Collections.unmodifiableList(result));
    }

    /** Returns an immutable alphabetically sorted list of all role entries. */
    @Contract(pure = true)
    public @NotNull @UnmodifiableView List<Entry> entries() {
        return entries;
    }

    /**
     * Returns distinct role identities that appear in any membership of {@code catId}.
     *
     * @return immutable set in the index's sorted order
     */
    public @NotNull @UnmodifiableView Set<DocumentRole> rolesForCategory(@NotNull String catId) {
        Set<DocumentRole> result = new LinkedHashSet<>();
        for (Entry e : entries) {
            if (e.categoryIds.contains(catId)) {
                result.add(e.raw);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** Returns {@code true} if the index contains at least one occurrence of {@code role}. */
    @Contract(pure = true)
    public boolean contains(@NotNull DocumentRole role) {
        for (Entry e : entries) {
            if (e.raw.equals(role)) return true;
        }
        return false;
    }
}
