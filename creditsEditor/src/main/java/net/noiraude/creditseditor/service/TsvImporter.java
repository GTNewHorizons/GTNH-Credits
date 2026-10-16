package net.noiraude.creditseditor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Parses a TSV file and computes the import plan against a {@link CreditsDocument}.
 *
 * <p>
 * File format: one person per line, tab-separated. Role columns are credits-owned ids or explicit translationKey JSON
 * objects; existing external references are preserved. First column is the person name
 * (required). Subsequent columns are roles within the target category. Empty lines
 * and lines starting with {@code #} are ignored.
 */
public final class TsvImporter {

    /** The action that will be taken for a single TSV line during import. */
    public enum Action {
        /** Person does not exist yet; will be created with the category membership and roles. */
        CREATE,
        /** Person exists but is not in the target category; membership will be added. */
        ADD,
        /** Person already in the target category; only missing roles will be added. */
        COMPLETE,
        /** Everything already present; no changes needed. */
        NO_CHANGE
    }

    /** One parsed TSV entry with its computed import action. */
    public static final class ImportLine {

        public final @NotNull String name;
        /** All roles the person will have after import (existing + new, deduplicated). */
        public final @NotNull List<DocumentRole> roles;
        /** Roles that will actually be added by the import (subset of {@link #roles}). */
        public final @NotNull List<DocumentRole> newRoles;
        public final @NotNull Action action;

        @Contract(pure = true)
        ImportLine(@NotNull String name, @NotNull List<DocumentRole> roles, @NotNull List<DocumentRole> newRoles,
            @NotNull Action action) {
            this.name = name;
            this.roles = roles;
            this.newRoles = newRoles;
            this.action = action;
        }
    }

    @Contract(pure = true)
    private TsvImporter() {}

    /**
     * Parses the TSV content and computes the import plan.
     * The supplied reader is closed, including on failure; the document is not mutated.
     *
     * @param reader     source of TSV data
     * @param doc        the current document (for computing actions)
     * @param categoryId the target category id
     * @return list of parsed lines with their actions
     * @throws IOException on read failure or an invalid role value
     */
    @Blocking
    public static @NotNull @UnmodifiableView List<ImportLine> parse(@NotNull Reader reader,
        @NotNull CreditsDocument doc, @NotNull String categoryId) throws IOException {
        // Merge duplicate names: later lines add their roles to the first occurrence.
        Map<String, List<DocumentRole>> merged = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = line.split("\t", -1);
                String name = parts[0].strip();
                if (name.isEmpty()) continue;

                List<DocumentRole> roles = merged.computeIfAbsent(name, k -> new ArrayList<>());
                for (int i = 1; i < parts.length; i++) {
                    String value = parts[i].strip();
                    if (!value.isEmpty()) {
                        DocumentRole role;
                        try {
                            role = value.startsWith("{")
                                ? net.noiraude.libcredits.parser.CreditsParser.parseRoleJson(value)
                                : DocumentRole.local(value);
                        } catch (IllegalArgumentException | net.noiraude.libcredits.parser.CreditsParseException ex) {
                            throw new IOException("Invalid role for " + name + ": " + ex.getMessage(), ex);
                        }
                        if (!roles.contains(role)) roles.add(role);
                    }
                }
            }
        }

        List<ImportLine> lines = new ArrayList<>();
        for (Map.Entry<String, List<DocumentRole>> entry : merged.entrySet()) {
            String name = entry.getKey();
            List<DocumentRole> tsvRoles = entry.getValue();
            lines.add(buildImportLine(doc, categoryId, name, tsvRoles));
        }
        return Collections.unmodifiableList(lines);
    }

    @Contract("_, _, _, _ -> new")
    private static @NotNull ImportLine buildImportLine(@NotNull CreditsDocument doc, @NotNull String categoryId,
        @NotNull String name, @NotNull List<DocumentRole> tsvRoles) {
        DocumentPerson person = doc.persons.stream()
            .filter(p -> p.name.equals(name))
            .findFirst()
            .orElse(null);

        if (person == null) {
            return new ImportLine(
                name,
                Collections.unmodifiableList(tsvRoles),
                Collections.unmodifiableList(tsvRoles),
                Action.CREATE);
        }

        DocumentMembership membership = person.memberships.stream()
            .filter(m -> m.categoryId.equals(categoryId))
            .findFirst()
            .orElse(null);

        if (membership == null) {
            return new ImportLine(
                name,
                Collections.unmodifiableList(tsvRoles),
                Collections.unmodifiableList(tsvRoles),
                Action.ADD);
        }

        // Merge: existing roles first, then any new roles from the TSV
        List<DocumentRole> allRoles = new ArrayList<>(membership.roles);
        List<DocumentRole> newRoles = new ArrayList<>();
        for (DocumentRole role : tsvRoles) {
            if (!allRoles.contains(role)) {
                allRoles.add(role);
                newRoles.add(role);
            }
        }

        Action action = newRoles.isEmpty() ? Action.NO_CHANGE : Action.COMPLETE;
        return new ImportLine(
            name,
            Collections.unmodifiableList(allRoles),
            Collections.unmodifiableList(newRoles),
            action);
    }
}
