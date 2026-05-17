package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.pack.CreditsLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("SpellCheckingInspection")
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
    public void saveAs_dirToZip_writesPackAndRetargetsSession() throws Exception {
        Path sourceDir = Files.createDirectory(temp.resolve("source"));
        Path destZip = temp.resolve("fork.zip");

        EditorSession session = EditorSession.open(sourceDir.toString());
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        assertTrue(session.isDirty());

        session.saveAs(destZip.toString());

        assertTrue(Files.exists(destZip), "destination zip must be created");
        assertFalse(session.isDirty(), "dirty flag must clear after Save As");
        assertEquals("fork.zip", session.displayPath(), "session must retarget to the new path");

        // Subsequent save() writes to the new destination, not the old directory.
        session.creditsDoc().categories.add(new DocumentCategory("art"));
        assertTrue(session.isDirty());
        session.save();
        assertFalse(session.isDirty());
        session.close();

        // Original directory must remain untouched by the saveAs (no credits.json written there).
        assertFalse(
            Files.exists(sourceDir.resolve("assets/gtnhcredits/credits.json")),
            "Save As must not write to the original directory");

        // Reopen the destination zip and verify both edits are present.
        EditorSession reopened = EditorSession.open(destZip.toString());
        try {
            assertEquals(2, reopened.creditsDoc().categories.size());
        } finally {
            reopened.close();
        }
    }

    @Test
    public void saveAs_zipToDir_writesPackAndRetargetsSession() throws Exception {
        Path sourceZip = temp.resolve("source.zip");
        Path destDir = temp.resolve("fork");

        EditorSession session = EditorSession.open(sourceZip.toString());
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        assertTrue(session.isDirty());

        session.saveAs(destDir.toString());

        assertTrue(
            Files.exists(destDir.resolve("assets/gtnhcredits/credits.json")),
            "destination directory must contain credits.json");
        assertFalse(session.isDirty(), "dirty flag must clear after Save As");
        assertEquals("fork", session.displayPath(), "session must retarget to the new path");

        session.creditsDoc().categories.add(new DocumentCategory("art"));
        session.save();
        session.close();

        EditorSession reopened = EditorSession.open(destDir.toString());
        try {
            assertEquals(2, reopened.creditsDoc().categories.size());
        } finally {
            reopened.close();
        }
    }

    @Test
    public void saveAs_preservesNonEnglishLocale() throws Exception {
        Path sourceDir = Files.createDirectory(temp.resolve("source"));
        Path langDir = sourceDir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("en_US.lang"), "credits.category.team=Team\n", StandardCharsets.UTF_8);
        Files.writeString(langDir.resolve("fr_FR.lang"), "credits.category.team=Equipe\n", StandardCharsets.UTF_8);

        Path destZip = temp.resolve("fork.zip");

        EditorSession session = EditorSession.open(sourceDir.toString());
        session.saveAs(destZip.toString());
        session.close();

        EditorSession reopened = EditorSession.open(destZip.toString());
        try {
            LangDocument fr = reopened.langDoc("fr_FR")
                .orElseThrow();
            assertTrue(
                reopened.availableLocales()
                    .contains("fr_FR"),
                "fr_FR must survive Save As to a fresh zip");
            assertEquals("Equipe", fr.get("credits.category.team"));
        } finally {
            reopened.close();
        }
    }

    @Test
    public void saveAs_overExistingZip_preservesUnrelatedContent() throws Exception {
        Path destZip = temp.resolve("dest.zip");

        // Seed an existing destination zip with content ResourceManager does not manage:
        // a texture and another mod's lang file. Save As must preserve both.
        // noinspection EmptyTryBlock
        try (java.io.OutputStream ignored = java.nio.file.Files.newOutputStream(destZip)) {
            // create empty file then immediately remove so the zip filesystem can create it.
        }
        java.nio.file.Files.delete(destZip);
        java.net.URI uri = java.net.URI.create("jar:" + destZip.toUri());
        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("create", "true");
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(uri, env)) {
            java.nio.file.Path texture = fs.getPath("/assets/gtnhcredits/textures/foo.png");
            java.nio.file.Files.createDirectories(texture.getParent());
            java.nio.file.Files.writeString(texture, "fake-png-bytes", StandardCharsets.UTF_8);
            java.nio.file.Path foreignLang = fs.getPath("/assets/othermod/lang/en_US.lang");
            java.nio.file.Files.createDirectories(foreignLang.getParent());
            java.nio.file.Files.writeString(foreignLang, "othermod.title=Other\n", StandardCharsets.UTF_8);
        }

        // Save a different project over the existing zip.
        Path freshSource = Files.createDirectory(temp.resolve("fresh"));
        EditorSession session = EditorSession.open(freshSource.toString());
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        session.saveAs(destZip.toString());
        session.close();

        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems
            .newFileSystem(uri, java.util.Map.<String, String>of())) {
            java.nio.file.Path texture = fs.getPath("/assets/gtnhcredits/textures/foo.png");
            assertTrue(Files.exists(texture), "Save As must not erase unrelated textures inside the destination zip");
            assertEquals("fake-png-bytes", java.nio.file.Files.readString(texture, StandardCharsets.UTF_8));

            java.nio.file.Path foreignLang = fs.getPath("/assets/othermod/lang/en_US.lang");
            assertTrue(Files.exists(foreignLang), "Save As must not erase another mod's lang files");
        }

        EditorSession reopened = EditorSession.open(destZip.toString());
        try {
            assertEquals(1, reopened.creditsDoc().categories.size(), "Save As must replace credits.json content");
            assertEquals("dev", reopened.creditsDoc().categories.getFirst().id);
        } finally {
            reopened.close();
        }
    }

    @Test
    public void defaultLocale_picksMatchingLangFileForJvmLocale() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        Path langDir = dir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("en_US.lang"), "", StandardCharsets.UTF_8);
        Files.writeString(langDir.resolve("fr_FR.lang"), "", StandardCharsets.UTF_8);

        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            EditorSession session = EditorSession.open(dir.toString());
            try {
                assertEquals("fr_FR", session.defaultLocale());
            } finally {
                session.close();
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void defaultLocale_fallsBackToEnglishWhenJvmLocaleNotPresentOnDisk() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        Path langDir = dir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("en_US.lang"), "", StandardCharsets.UTF_8);

        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            EditorSession session = EditorSession.open(dir.toString());
            try {
                assertEquals("en_US", session.defaultLocale());
            } finally {
                session.close();
            }
        } finally {
            Locale.setDefault(previous);
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
