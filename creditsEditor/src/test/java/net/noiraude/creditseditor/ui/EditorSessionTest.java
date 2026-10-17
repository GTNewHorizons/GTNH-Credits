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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("SpellCheckingInspection")
public class EditorSessionTest {

    @TempDir
    Path temp;

    @Test
    public void openUnrecognizedDirectory_doesNotCreateAProject() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        assertThrows(java.io.IOException.class, () -> EditorSession.open(dir.toString()));
        try (var entries = Files.list(dir)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    public void malformedOptionalTranslations_doNotPreventOpeningCredits() throws Exception {
        Path dir = contentWithTeam("instance");
        Path mods = Files.createDirectory(dir.resolve("mods"));
        try (var archive = new java.util.zip.ZipOutputStream(Files.newOutputStream(mods.resolve("example.jar")))) {
            archive.finish();
        }
        Path index = dir.resolve("assets/indexes/1.7.10.json");
        Files.createDirectories(index.getParent());
        Files.writeString(index, "{}");

        EditorSession session = EditorSession.open(dir.toString());
        try {
            assertEquals("team", session.creditsDoc().categories.getFirst().id);
            assertFalse(session.hasTranslationCatalog());
            assertFalse(session.isDirty());
        } finally {
            session.close();
        }
    }

    @Test
    public void editing_marksDirty_andSaveClearsIt() throws Exception {
        Path dir = temp.resolve("root");

        EditorSession session = EditorSession.create(dir.toString(), CreditsLayout.Format.RESOURCE_DIRECTORY);
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
    public void newZip_createsAndLoadsItEmpty() throws Exception {
        Path zip = temp.resolve("fresh.zip");
        assertFalse(Files.exists(zip));

        EditorSession session = EditorSession.create(zip.toString(), CreditsLayout.Format.RESOURCE_PACK);
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
        Path sourceDir = temp.resolve("source");
        Path destZip = temp.resolve("fork.zip");

        EditorSession session = EditorSession.create(sourceDir.toString(), CreditsLayout.Format.RESOURCE_DIRECTORY);
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        assertTrue(session.isDirty());

        session.saveAs(destZip.toString());

        assertTrue(Files.exists(destZip), "destination zip must be created");
        assertFalse(session.isDirty(), "dirty flag must clear after Save As");
        assertEquals(
            destZip.toAbsolutePath() + "!/assets/gtnhcredits/credits.json",
            session.displayPath(),
            "session must retarget to the new path");

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

        EditorSession session = EditorSession.create(sourceZip.toString(), CreditsLayout.Format.RESOURCE_PACK);
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        assertTrue(session.isDirty());

        session.saveAs(destDir.toString());

        assertTrue(
            Files.exists(destDir.resolve("assets/gtnhcredits/credits.json")),
            "destination directory must contain credits.json");
        assertFalse(session.isDirty(), "dirty flag must clear after Save As");
        assertEquals(
            destDir.resolve("assets/gtnhcredits/credits.json")
                .toAbsolutePath()
                .toString(),
            session.displayPath(),
            "session must retarget to the new path");

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
        Path sourceDir = contentWithTeam("source");
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
        java.net.URI uri = java.net.URI.create("jar:" + destZip.toUri());
        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems
            .newFileSystem(uri, java.util.Map.of("create", "true"))) {
            java.nio.file.Path texture = fs.getPath("/assets/gtnhcredits/textures/foo.png");
            java.nio.file.Files.createDirectories(texture.getParent());
            java.nio.file.Files.writeString(texture, "fake-png-bytes", StandardCharsets.UTF_8);
            java.nio.file.Path foreignLang = fs.getPath("/assets/othermod/lang/en_US.lang");
            java.nio.file.Files.createDirectories(foreignLang.getParent());
            java.nio.file.Files.writeString(foreignLang, "othermod.title=Other\n", StandardCharsets.UTF_8);
        }

        // Save a different project over the existing zip.
        Path freshSource = temp.resolve("fresh");
        EditorSession session = EditorSession.create(freshSource.toString(), CreditsLayout.Format.RESOURCE_DIRECTORY);
        session.creditsDoc().categories.add(new DocumentCategory("dev"));
        session.saveAs(destZip.toString());
        session.close();

        try (java.nio.file.FileSystem fs = java.nio.file.FileSystems
            .newFileSystem(uri, java.util.Map.<String, String>of())) {
            java.nio.file.Path texture = fs.getPath("/assets/gtnhcredits/textures/foo.png");
            assertTrue(Files.exists(texture), "Save As must not erase unrelated textures inside the destination zip");
            assertEquals("fake-png-bytes", java.nio.file.Files.readString(texture, StandardCharsets.UTF_8));

            java.nio.file.Path foreignLang = fs.getPath("/assets/othermod/lang/en_US.lang");
            assertEquals("othermod.title=Other\n", Files.readString(foreignLang, StandardCharsets.UTF_8));
        }

        EditorSession reopened = EditorSession.open(destZip.toString());
        try {
            assertEquals(1, reopened.creditsDoc().categories.size(), "Save As must replace credits.json content");
            assertEquals("dev", reopened.creditsDoc().categories.getFirst().id);
        } finally {
            reopened.close();
        }
    }

    @ParameterizedTest
    @CsvSource({ "fr-FR,fr_FR", "de-DE,en_US" })
    public void defaultLocale_matchesJvmLocaleOrFallsBackToEnglish(String languageTag, String expected)
        throws Exception {
        Path dir = contentWithTeam("root");
        Path langDir = dir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("en_US.lang"), "", StandardCharsets.UTF_8);
        Files.writeString(langDir.resolve("fr_FR.lang"), "", StandardCharsets.UTF_8);

        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag(languageTag));
        try {
            EditorSession session = EditorSession.open(dir.toString());
            try {
                assertEquals(expected, session.defaultLocale());
            } finally {
                session.close();
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void saveFailure_leavesDirtyFlagSet() throws Exception {
        Path dir = temp.resolve("root");
        EditorSession session = EditorSession.create(dir.toString(), CreditsLayout.Format.CONTENT_DIRECTORY);
        Files.createDirectory(dir.resolve("credits.json"));
        try {
            session.creditsDoc().categories.add(new DocumentCategory("dev"));
            assertTrue(session.isDirty());

            assertThrows(java.io.IOException.class, session::save, "credits.json is a directory");

            assertTrue(session.isDirty(), "dirty flag must remain set after a failed save");
        } finally {
            session.close();
        }
    }

    private Path contentWithTeam(String name) throws Exception {
        Path path = Files.createDirectory(temp.resolve(name));
        Files.writeString(path.resolve("credits.json"), "{\"version\":2,\"category\":[{\"id\":\"team\"}]}");
        return path;
    }

}
