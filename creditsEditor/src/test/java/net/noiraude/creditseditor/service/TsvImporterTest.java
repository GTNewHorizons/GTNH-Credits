package net.noiraude.creditseditor.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import org.junit.Test;

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
        assertEquals("Alice", lines.get(0).name);
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
    public void action_existingPersonInCategoryMissingRoles_complete() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(Action.COMPLETE, lines.getFirst().action);
    }

    @Test
    public void action_existingPersonInCategoryAllRolesPresent_noChange() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", Arrays.asList("lead", "coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\tcoder\n"), doc, "dev");

        assertEquals(Action.NO_CHANGE, lines.getFirst().action);
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

        assertEquals(3, lines.size());
        assertEquals(Action.CREATE, lines.get(0).action);
        assertEquals(Action.NO_CHANGE, lines.get(1).action);
        assertEquals(Action.COMPLETE, lines.get(2).action);
    }
}
