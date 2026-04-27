package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import net.noiraude.libcredits.model.DocumentCategory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EditorSessionTest {

    @TempDir
    Path temp;

    @Test
    public void freshOpenDirectory_loadsEmptyAndNotDirty() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));

        EditorSession session = EditorSession.open(dir.toString());
        try {
            assertTrue(session.creditsDoc().categories.isEmpty());
            assertTrue(session.creditsDoc().persons.isEmpty());
            assertFalse(session.isDirty());
        } finally {
            session.close();
        }
    }

    @Test
    public void editing_marksDirty_andSaveClearsIt() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));

        EditorSession session = EditorSession.open(dir.toString());
        try {
            assertFalse(session.isDirty());
            session.creditsDoc().categories.add(new DocumentCategory("dev"));
            assertTrue(session.isDirty());

            session.save();

            assertFalse(session.isDirty());
            assertTrue(Files.exists(dir.resolve("assets/gtnhcredits/credits.json")));
        } finally {
            session.close();
        }
    }

    @Test
    public void openNonExistentZip_createsAndLoadsItEmpty() throws Exception {
        Path zip = temp.resolve("fresh.zip");
        assertFalse(Files.exists(zip));

        EditorSession session = EditorSession.open(zip.toString());
        try {
            assertTrue(Files.exists(zip));
            assertTrue(session.creditsDoc().categories.isEmpty());
            assertTrue(session.creditsDoc().persons.isEmpty());
            assertFalse(session.isDirty());
        } finally {
            session.close();
        }
    }

    @Test
    public void saveFailure_leavesDirtyFlagSet() throws Exception {
        // Place a regular file at the "assets" segment so ResourceManager cannot create
        // the assets/gtnhcredits subtree during save. Load still succeeds because the
        // target credits.json path does not resolve, so notExists() returns true and
        // an empty document is created in memory.
        Path dir = Files.createDirectory(temp.resolve("root"));
        Files.writeString(dir.resolve("assets"), "blocker");

        EditorSession session = EditorSession.open(dir.toString());
        try {
            session.creditsDoc().categories.add(new DocumentCategory("dev"));
            assertTrue(session.isDirty());

            assertThrows(Exception.class, session::save, "save should fail when assets is a regular file");

            assertTrue(session.isDirty(), "dirty flag must remain set after a failed save");
        } finally {
            session.close();
        }
    }
}
