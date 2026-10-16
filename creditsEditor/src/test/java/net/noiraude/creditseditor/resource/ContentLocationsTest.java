package net.noiraude.creditseditor.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.noiraude.creditseditor.CreditsEditorApp;
import net.noiraude.libcredits.pack.CreditsLayout;
import net.noiraude.libcredits.pack.CreditsLayout.Format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentLocationsTest {

    @TempDir
    Path temporary;

    private void archive(Path path, String resource, String contents) throws Exception {
        Files.createDirectories(path.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new ZipEntry(resource));
            output.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    static void writeContent(Path directory) throws Exception {
        Files.createDirectories(directory.resolve("lang"));
        Files.writeString(
            directory.resolve("credits.json"),
            "{\"version\":3,\"category\":[{\"id\":\"dev\"}],\"person\":[{\"name\":\"Alice\",\"category\":{\"dev\":{\"translationKey\":\"machine.Name\"}}}]}");
        Files.writeString(
            directory.resolve("lang/en_US.lang"),
            "credits.category.dev=Developers\nforeign.key=Untouched\n");
    }

    @Test
    void resolvesDirectFileContentDirectoryModpackAndResourceRoot() throws Exception {
        Path content = temporary.resolve(CreditsLayout.MODPACK_DIRECTORY);
        writeContent(content);
        for (Path location : List.of(temporary, content, content.resolve("credits.json"))) {
            try (ResourceManager resources = ResourceManager.open(location.toString())) {
                resources.loadDocuments();
                assertEquals(
                    content.resolve("credits.json")
                        .toString(),
                    resources.displayPath());
                assertTrue(
                    resources.getCreditsDoc().persons.getFirst().memberships.getFirst().roles.getFirst()
                        .isExternal());
            }
        }
        Path pack = temporary.resolve("pack");
        writeContent(pack.resolve(CreditsLayout.RESOURCE_DIRECTORY));
        try (ResourceManager resources = ResourceManager.open(pack.toString())) {
            resources.loadDocuments();
            assertEquals(1, resources.getCreditsDoc().categories.size());
        }
        assertFalse(Files.exists(content.resolve("assets")));
    }

    @Test
    void explicitDirectoryFormatDoesNotTurnAZipSuffixIntoAnArchive() throws Exception {
        Path directory = temporary.resolve("content.zip");
        try (ResourceManager resources = ResourceManager
            .create(directory.toString(), "test", Format.CONTENT_DIRECTORY)) {
            resources.loadDocuments();
            resources.getCreditsDoc().categories.add(new net.noiraude.libcredits.model.DocumentCategory("dev"));
            resources.writeCredits();
        }
        assertTrue(Files.isDirectory(directory));
        try (ResourceManager resources = ResourceManager.open(directory.toString())) {
            assertEquals(
                directory.resolve("credits.json")
                    .toString(),
                resources.displayPath());
        }
    }

    @Test
    void ambiguousRootRequiresExplicitSelection() throws Exception {
        writeContent(temporary);
        writeContent(temporary.resolve(CreditsLayout.RESOURCE_DIRECTORY));
        assertThrows(java.io.IOException.class, () -> ResourceManager.open(temporary.toString()));
        try (ResourceManager resources = ResourceManager.open(
            temporary.resolve("credits.json")
                .toString())) {
            resources.loadDocuments();
        }
    }

    @Test
    void headlessValidationReadsWithoutWritingAndReportsItsExternalCheckScope() throws Exception {
        writeContent(temporary);
        byte[] json = Files.readAllBytes(temporary.resolve("credits.json"));
        byte[] english = Files.readAllBytes(temporary.resolve("lang/en_US.lang"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(
            0,
            CreditsEditorApp.validate(temporary.toString(), null, new PrintStream(output), new PrintStream(errors)));
        assertTrue(
            output.toString()
                .contains("not checked"));
        assertArrayEquals(json, Files.readAllBytes(temporary.resolve("credits.json")));
        assertArrayEquals(english, Files.readAllBytes(temporary.resolve("lang/en_US.lang")));
        assertEquals(
            1,
            CreditsEditorApp.validate(
                temporary.resolve("missing")
                    .toString(),
                null,
                new PrintStream(output),
                new PrintStream(errors)));
        assertFalse(Files.exists(temporary.resolve("missing")));
    }

    @Test
    void headlessValidationIncludesVanillaResourcesAndStillRejectsMissingKeys() throws Exception {
        writeContent(temporary);
        Path json = temporary.resolve("credits.json");
        Files.writeString(
            json,
            Files.readString(json)
                .replace("machine.Name", "tile.furnace.name"));
        Path instance = temporary.resolve("instance");
        Files.createDirectories(instance.resolve("mods"));
        archive(
            instance.resolve("versions/1.7.10/1.7.10.jar"),
            "assets/minecraft/lang/en_US.lang",
            "tile.furnace.name=Furnace\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(
            0,
            CreditsEditorApp
                .validate(temporary.toString(), instance.toString(), new PrintStream(output), new PrintStream(errors)));
        assertEquals("", errors.toString());
        assertFalse(
            output.toString()
                .contains("not checked"));
        Files.writeString(
            json,
            Files.readString(json)
                .replace("tile.furnace.name", "missing.machine"));
        assertEquals(
            1,
            CreditsEditorApp
                .validate(temporary.toString(), instance.toString(), new PrintStream(output), new PrintStream(errors)));
        assertTrue(
            errors.toString()
                .contains("missing external translation: missing.machine"));
    }

    @Test
    void headlessValidationReportsUncheckedWhenBaseResourcesAreUnavailable() throws Exception {
        writeContent(temporary);
        Path instance = temporary.resolve("instance");
        Files.createDirectories(instance.resolve("mods"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(
            0,
            CreditsEditorApp
                .validate(temporary.toString(), instance.toString(), new PrintStream(output), new PrintStream(errors)));
        assertEquals("", errors.toString());
        assertTrue(
            output.toString()
                .contains("not checked"));
        assertTrue(
            output.toString()
                .contains("base language resources were not found"));
    }

    @Test
    void contentAndZipExportsKeepForeignLanguageEntries() throws Exception {
        Path source = temporary.resolve("source");
        writeContent(source);
        try (ResourceManager original = ResourceManager.open(source.toString())) {
            original.loadDocuments();
            for (Path destination : List.of(temporary.resolve("content"), temporary.resolve("pack.zip"))) {
                Format format = destination.toString()
                    .endsWith(".zip") ? Format.RESOURCE_PACK : Format.CONTENT_DIRECTORY;
                try (ResourceManager copy = ResourceManager.destination(destination.toString(), "test", format)) {
                    copy.adoptDocuments(original.getCreditsDoc(), original.langDocs());
                    copy.writeCredits();
                    copy.writeAllLang();
                }
                try (ResourceManager reopened = ResourceManager.open(destination.toString())) {
                    reopened.loadDocuments();
                    assertEquals(original.getCreditsDoc().persons, reopened.getCreditsDoc().persons);
                }
            }
        }
        assertEquals(
            "credits.category.dev=Developers\nforeign.key=Untouched\n",
            Files.readString(source.resolve("lang/en_US.lang")));
    }
}
