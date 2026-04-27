package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import net.noiraude.creditseditor.service.TsvImporter.Action;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.jupiter.api.Test;

public class TsvImporterTest {

    private static CreditsDocument docWithCategory(String catId) {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory(catId));
        return doc;
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    @Test
    public void parse_singleLine_nameAndRoles() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\tcoder\n"), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Alice", lines.getFirst().name);
        assertEquals(List.of("lead", "coder"), lines.getFirst().roles);
    }

    @Test
    public void parse_nameOnly_emptyRoles() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        List<ImportLine> lines = TsvImporter.parse(new StringReader("Bob\n"), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Bob", lines.getFirst().name);
        assertTrue(lines.getFirst().roles.isEmpty());
    }

    @Test
    public void parse_emptyLinesAndComments_ignored() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        String tsv = "# header comment\n\nAlice\tlead\n\n# another comment\nBob\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(2, lines.size());
        assertEquals("Alice", lines.getFirst().name);
        assertEquals("Bob", lines.get(1).name);
    }

    @Test
    public void parse_emptyNameColumn_ignored() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        List<ImportLine> lines = TsvImporter.parse(new StringReader("\tlead\n"), doc, "dev");
        assertTrue(lines.isEmpty());
    }

    @Test
    public void parse_trailingEmptyTabs_ignored() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\t\t\n"), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(List.of("lead"), lines.getFirst().roles);
    }

    // -----------------------------------------------------------------------
    // Action computation
    // -----------------------------------------------------------------------

    @Test
    public void action_newPerson_create() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(Action.CREATE, lines.getFirst().action);
    }

    @Test
    public void action_existingPersonNotInCategory_add() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        doc.persons.add(new DocumentPerson("Alice"));
        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(Action.ADD, lines.getFirst().action);
    }

    @Test
    public void action_existingPersonInOtherCategory_addDoesNotLeakOtherRoles() throws IOException {
        CreditsDocument doc = docWithCategory("art");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("art", List.of("painter")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(Action.ADD, lines.getFirst().action);
        // Only the TSV roles; the "art" membership roles must stay isolated.
        assertEquals(List.of("lead"), lines.getFirst().roles);
        assertEquals(List.of("lead"), lines.getFirst().newRoles);
    }

    @Test
    public void action_existingPersonInCategoryMissingRoles_complete() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(Action.COMPLETE, lines.getFirst().action);
        // roles = merged existing + new; newRoles = only the additions
        assertEquals(List.of("coder", "lead"), lines.getFirst().roles);
        assertEquals(List.of("lead"), lines.getFirst().newRoles);
    }

    @Test
    public void action_existingPersonInCategoryAllRolesPresent_noChange() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", Arrays.asList("lead", "coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\tcoder\n"), doc, "dev");

        assertEquals(Action.NO_CHANGE, lines.getFirst().action);
        assertEquals(List.of("lead", "coder"), lines.getFirst().roles);
        assertTrue(lines.getFirst().newRoles.isEmpty());
    }

    @Test
    public void action_existingPersonInCategoryNoRolesInTsv_noChange() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev"));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\n"), doc, "dev");

        assertEquals(Action.NO_CHANGE, lines.getFirst().action);
    }

    // -----------------------------------------------------------------------
    // Multiple lines mixed
    // -----------------------------------------------------------------------

    @Test
    public void parse_mixedActions_computedCorrectly() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson existing = new DocumentPerson("Bob");
        existing.memberships.add(new DocumentMembership("dev", List.of("coder")));
        doc.persons.add(existing);

        String tsv = "Alice\tlead\nBob\tcoder\nBob\tnewrole\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(2, lines.size());
        assertEquals("Alice", lines.getFirst().name);
        assertEquals(Action.CREATE, lines.getFirst().action);
        // Bob's two lines are merged: roles = [coder, newrole], and "newrole" is missing
        assertEquals("Bob", lines.get(1).name);
        assertEquals(List.of("coder", "newrole"), lines.get(1).roles);
        assertEquals(Action.COMPLETE, lines.get(1).action);
    }

    @Test
    public void parse_duplicateNames_mergedIntoSingleEntry() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        String tsv = "Greg Ewing\tlead\nGreg Ewing\tcoder\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Greg Ewing", lines.getFirst().name);
        assertEquals(List.of("lead", "coder"), lines.getFirst().roles);
        assertEquals(Action.CREATE, lines.getFirst().action);
    }

    @Test
    public void parse_duplicateNames_duplicateRolesDeduped() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        String tsv = "Alice\tlead\nAlice\tlead\tcoder\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(List.of("lead", "coder"), lines.getFirst().roles);
    }

    @Test
    public void parse_existingPersonWithRoles_mergesWithTsvRoles() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson greg = new DocumentPerson("Greg Ewing");
        greg.memberships.add(new DocumentMembership("dev", List.of("lead")));
        doc.persons.add(greg);

        String tsv = "Greg Ewing\tcoder\nGreg Ewing\ttester\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Greg Ewing", lines.getFirst().name);
        assertEquals(Action.COMPLETE, lines.getFirst().action);
        // Full role set: existing "lead" + new "coder" and "tester"
        assertEquals(List.of("lead", "coder", "tester"), lines.getFirst().roles);
        assertEquals(List.of("coder", "tester"), lines.getFirst().newRoles);
    }

    @Test
    public void parse_existingPersonWithRoles_noNewRoles_noChange() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson greg = new DocumentPerson("Greg Ewing");
        greg.memberships.add(new DocumentMembership("dev", List.of("lead", "coder")));
        doc.persons.add(greg);

        String tsv = "Greg Ewing\tlead\tcoder\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(Action.NO_CHANGE, lines.getFirst().action);
        assertEquals(List.of("lead", "coder"), lines.getFirst().roles);
        assertTrue(lines.getFirst().newRoles.isEmpty());
    }
}
