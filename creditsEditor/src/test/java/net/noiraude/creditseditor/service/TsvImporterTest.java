package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.TestDocumentSession;
import net.noiraude.creditseditor.service.TsvImporter.Action;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("SpellCheckingInspection")
public class TsvImporterTest {

    private static @NotNull CreditsDocument docWithCategory(String catId) {
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
        assertEquals(localRoles("lead", "coder"), lines.getFirst().roles);
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
        assertEquals(localRoles("lead"), lines.getFirst().roles);
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
        alice.memberships.add(new DocumentMembership("art", localRoles("painter")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(Action.ADD, lines.getFirst().action);
        // Only the TSV roles; the "art" membership roles must stay isolated.
        assertEquals(localRoles("lead"), lines.getFirst().roles);
        assertEquals(localRoles("lead"), lines.getFirst().newRoles);
    }

    @Test
    public void action_existingPersonInCategoryMissingRoles_complete() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", localRoles("coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\n"), doc, "dev");

        assertEquals(Action.COMPLETE, lines.getFirst().action);
        // roles = merged existing + new; newRoles = only the additions
        assertEquals(localRoles("coder", "lead"), lines.getFirst().roles);
        assertEquals(localRoles("lead"), lines.getFirst().newRoles);
    }

    @Test
    public void action_existingPersonInCategoryAllRolesPresent_noChange() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", localRoles("lead", "coder")));
        doc.persons.add(alice);

        List<ImportLine> lines = TsvImporter.parse(new StringReader("Alice\tlead\tcoder\n"), doc, "dev");

        assertEquals(Action.NO_CHANGE, lines.getFirst().action);
        assertEquals(localRoles("lead", "coder"), lines.getFirst().roles);
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
        existing.memberships.add(new DocumentMembership("dev", localRoles("coder")));
        doc.persons.add(existing);

        String tsv = "Alice\tlead\nBob\tcoder\nBob\tnewrole\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(2, lines.size());
        assertEquals("Alice", lines.getFirst().name);
        assertEquals(Action.CREATE, lines.getFirst().action);
        // Bob's two lines are merged: roles = [coder, newrole], and "newrole" is missing
        assertEquals("Bob", lines.get(1).name);
        assertEquals(localRoles("coder", "newrole"), lines.get(1).roles);
        assertEquals(Action.COMPLETE, lines.get(1).action);
    }

    @Test
    public void parse_duplicateNames_mergedIntoSingleEntry() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        String tsv = "Greg Ewing\tlead\nGreg Ewing\tcoder\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Greg Ewing", lines.getFirst().name);
        assertEquals(localRoles("lead", "coder"), lines.getFirst().roles);
        assertEquals(Action.CREATE, lines.getFirst().action);
    }

    @Test
    public void parse_duplicateNames_duplicateRolesDeduped() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        String tsv = "Alice\tlead\nAlice\tlead\tcoder\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals(localRoles("lead", "coder"), lines.getFirst().roles);
    }

    @Test
    public void parse_existingPersonWithRoles_mergesWithTsvRoles() throws IOException {
        CreditsDocument doc = docWithCategory("dev");
        DocumentPerson greg = new DocumentPerson("Greg Ewing");
        greg.memberships.add(new DocumentMembership("dev", localRoles("lead")));
        doc.persons.add(greg);

        String tsv = "Greg Ewing\tcoder\nGreg Ewing\ttester\n";
        List<ImportLine> lines = TsvImporter.parse(new StringReader(tsv), doc, "dev");

        assertEquals(1, lines.size());
        assertEquals("Greg Ewing", lines.getFirst().name);
        assertEquals(Action.COMPLETE, lines.getFirst().action);
        // Full role set: existing "lead" + new "coder" and "tester"
        assertEquals(localRoles("lead", "coder", "tester"), lines.getFirst().roles);
        assertEquals(localRoles("coder", "tester"), lines.getFirst().newRoles);
    }

    @Test
    public void referenceImportMergesBySourceAndSupportsUndo() throws Exception {
        CreditsDocument document = docWithCategory("dev");
        DocumentRole existing = DocumentRole.translation("machine.Original");
        DocumentRole external = DocumentRole.translation("Exact.Key");
        DocumentRole local = DocumentRole.local("Exact.Key");
        DocumentPerson person = new DocumentPerson("Alice");
        DocumentMembership membership = new DocumentMembership("dev", List.of(existing));
        person.memberships.add(membership);
        document.persons.add(person);
        DocumentBus bus = new DocumentBus(() -> Optional.of(TestDocumentSession.of(document)));

        var lines = TsvImporter.parse(
            new StringReader(
                "Alice\t{\"translationKey\":\"Exact.Key\"}\tExact.Key\nAlice\t{\"translationKey\":\"Exact.Key\"}\n"),
            document,
            "dev");
        assertEquals(List.of(existing, external, local), lines.getFirst().roles);
        assertEquals(List.of(external, local), lines.getFirst().newRoles);
        assertEquals(List.of(existing), membership.roles, "planning must not mutate the document");

        var command = Objects.requireNonNull(TsvImportCommandFactory.build(bus, "dev", lines));
        command.execute();
        assertEquals(List.of(existing, external, local), membership.roles);
        command.undo();
        assertEquals(List.of(existing), membership.roles);
        command.execute();
        assertEquals(List.of(existing, external, local), membership.roles);
    }

    @Test
    public void invalidReferenceFailsBeforeImportCanMutateTheDocument() {
        CreditsDocument document = docWithCategory("dev");
        assertThrows(
            IOException.class,
            () -> TsvImporter.parse(
                new StringReader("Alice\tdeveloper\nBob\t{\"translationKey\":\"has spaces\"}\n"),
                document,
                "dev"));
        assertTrue(document.persons.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void collidingLocalRolesFailBeforeImportCanMutateTheDocument(boolean collisionWithExistingRole) {
        CreditsDocument document = docWithCategory("dev");
        document.categories.add(new DocumentCategory("art"));
        if (collisionWithExistingRole) {
            DocumentPerson alice = new DocumentPerson("Alice");
            alice.memberships.add(new DocumentMembership("art", localRoles("Core.Dev")));
            document.persons.add(alice);
        }
        document.markClean();
        String tsv = collisionWithExistingRole ? "Bob\tCoreDev\n" : "Alice\tCore.Dev\nBob\tCoreDev\n";

        IOException failure = assertThrows(
            IOException.class,
            () -> TsvImporter.parse(new StringReader(tsv), document, "dev"));

        assertTrue(
            failure.getMessage()
                .contains("translation-key collision"));
        assertTrue(
            failure.getMessage()
                .contains("Core.Dev"));
        assertTrue(
            failure.getMessage()
                .contains("CoreDev"));
        assertFalse(document.isDirty(), "a rejected import must leave the document unchanged");
    }

    @Test
    public void localRoleReuseAndExternalKeysWithSimilarSpellingRemainAllowed() throws IOException {
        CreditsDocument document = docWithCategory("dev");
        document.categories.add(new DocumentCategory("art"));
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("art", localRoles("CoreDev")));
        document.persons.add(alice);
        document.markClean();

        List<ImportLine> lines = TsvImporter.parse(
            new StringReader("Bob\tCoreDev\t{\"translationKey\":\"Core.Dev\"}\nCarol\tCoreDev\n"),
            document,
            "dev");

        assertEquals(2, lines.size());
        assertEquals(
            List.of(DocumentRole.local("CoreDev"), DocumentRole.translation("Core.Dev")),
            lines.getFirst().newRoles);
        assertEquals(localRoles("CoreDev"), lines.get(1).newRoles);
        assertFalse(document.isDirty(), "planning must not mutate the document");
    }

    private static java.util.List<DocumentRole> localRoles(String... values) {
        return java.util.Arrays.stream(values)
            .map(DocumentRole::local)
            .collect(java.util.stream.Collectors.toList());
    }

}
