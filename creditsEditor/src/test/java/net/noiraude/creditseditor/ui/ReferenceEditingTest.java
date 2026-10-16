package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.impl.EditPersonRoleCommand;
import net.noiraude.libcredits.model.DocumentRole;
import net.noiraude.libcredits.pack.CreditsLayout.Format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceEditingTest {

    @TempDir
    Path temporary;

    @Test
    void saveAsAtTheSameDirectoryHonorsADifferentLayout() throws Exception {
        Path content = temporary.resolve("source");
        EditorSession session = EditorSession.create(content.toString(), Format.CONTENT_DIRECTORY);
        try {
            session.creditsDoc().categories.add(new net.noiraude.libcredits.model.DocumentCategory("dev"));
            session.save();
            session.saveAs(content.toString(), Format.RESOURCE_DIRECTORY);
            assertEquals(content.resolve("assets/gtnhcredits/credits.json"), session.openPath());
            assertTrue(Files.isRegularFile(content.resolve("credits.json")));
            assertTrue(Files.isRegularFile(content.resolve("assets/gtnhcredits/credits.json")));
        } finally {
            session.close();
        }
    }

    @Test
    void referenceEditUndoRedoAndBothSaveAsLayoutsPreserveSourceAndOrder() throws Exception {
        Path content = temporary.resolve("source");
        Files.createDirectories(content.resolve("lang"));
        Files.writeString(
            content.resolve("credits.json"),
            "{\"version\":3,\"category\":[{\"id\":\"dev\"}],\"person\":[{\"name\":\"Alice\",\"category\":{\"dev\":[\"developer\",{\"translationKey\":\"machine.Original\"}]}}]}");
        Files.writeString(
            content.resolve("lang/en_US.lang"),
            "credits.category.dev=Developers\ncredits.person.role.developer=Developer\nforeign.key=Keep exactly\n");
        EditorSession session = EditorSession.open(content.toString());
        try {
            DocumentBus bus = new DocumentBus(() -> Optional.of(session));
            var person = session.creditsDoc().persons.getFirst();
            var membership = person.memberships.getFirst();
            session.execute(
                new EditPersonRoleCommand(bus, person, membership, 1, DocumentRole.translation("machine.Replacement")));
            assertTrue(session.isDirty());
            session.undo();
            assertFalse(session.isDirty());
            assertEquals(
                "machine.Original",
                membership.roles.get(1)
                    .translationKey());
            session.redo();
            session.save();
            assertFalse(session.isDirty());
            session.saveAs(
                temporary.resolve("pack.zip")
                    .toString(),
                Format.RESOURCE_PACK);
            session.saveAs(
                temporary.resolve("export")
                    .toString(),
                Format.CONTENT_DIRECTORY);
        } finally {
            session.close();
        }
        EditorSession reopened = EditorSession.open(
            temporary.resolve("export")
                .toString());
        try {
            var roles = reopened.creditsDoc().persons.getFirst().memberships.getFirst().roles;
            assertEquals(DocumentRole.local("developer"), roles.getFirst());
            assertEquals(DocumentRole.translation("machine.Replacement"), roles.get(1));
            assertFalse(reopened.hasTranslationCatalog());
        } finally {
            reopened.close();
        }
        assertFalse(
            Files.readString(content.resolve("lang/en_US.lang"))
                .contains("machine.Replacement"));
        assertTrue(
            Files.readString(content.resolve("lang/en_US.lang"))
                .contains("foreign.key=Keep exactly"));
    }
}
