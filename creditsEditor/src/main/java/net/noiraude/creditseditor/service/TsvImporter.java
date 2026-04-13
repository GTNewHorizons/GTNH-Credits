package net.noiraude.creditseditor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Parses a TSV file and computes the import plan against a {@link CreditsDocument}.
 *
 * <p>
 * File format: one person per line, tab-separated. First column is the person name
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

    /** One parsed TSV line with its computed import action. */
    public static final class ImportLine {

        public final String name;
        public final List<String> roles;
        public final Action action;

        ImportLine(String name, List<String> roles, Action action) {
            this.name = name;
            this.roles = roles;
            this.action = action;
        }
    }

    private TsvImporter() {}

    /**
     * Parses the TSV content and computes the import plan.
     *
     * @param reader     source of TSV data
     * @param doc        the current document (for computing actions)
     * @param categoryId the target category id
     * @return list of parsed lines with their actions
     * @throws IOException on read failure
     */
    public static List<ImportLine> parse(Reader reader, CreditsDocument doc, String categoryId) throws IOException {
        List<ImportLine> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = line.split("\t", -1);
                String name = parts[0].strip();
                if (name.isEmpty()) continue;

                List<String> roles = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    String role = parts[i].strip();
                    if (!role.isEmpty()) {
                        roles.add(role);
                    }
                }

                Action action = computeAction(doc, categoryId, name, roles);
                lines.add(new ImportLine(name, Collections.unmodifiableList(roles), action));
            }
        }
        return Collections.unmodifiableList(lines);
    }

    private static Action computeAction(CreditsDocument doc, String categoryId, String name, List<String> roles) {
        DocumentPerson person = doc.persons.stream()
            .filter(p -> p.name.equals(name))
            .findFirst()
            .orElse(null);

        if (person == null) return Action.CREATE;

        DocumentMembership membership = person.memberships.stream()
            .filter(m -> m.categoryId.equals(categoryId))
            .findFirst()
            .orElse(null);

        if (membership == null) return Action.ADD;

        boolean hasNewRoles = roles.stream()
            .anyMatch(r -> !membership.roles.contains(r));
        return hasNewRoles ? Action.COMPLETE : Action.NO_CHANGE;
    }
}
