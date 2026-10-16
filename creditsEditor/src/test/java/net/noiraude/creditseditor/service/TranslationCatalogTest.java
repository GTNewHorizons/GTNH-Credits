package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TranslationCatalogTest {

    @TempDir
    Path instance;

    private void archive(Path path, Map<String, String> files) throws Exception {
        Files.createDirectories(path.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : files.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(
                    entry.getValue()
                        .getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    @Test
    void readsModsSelectedPacksAndTxLoaderWithPerKeyEnglishFallback() throws Exception {
        archive(
            instance.resolve("mods/example.jar"),
            Map.of(
                "assets/example/lang/en_US.lang",
                "machine.Name=English name\nonly.english=English fallback\n",
                "assets/example/lang/fr_FR.lang",
                "machine.Name=Nom français\n"));
        archive(
            instance.resolve("resourcepacks/enabled.zip"),
            Map.of("assets/example/lang/en_US.lang", "machine.Name=Selected pack\n"));
        archive(
            instance.resolve("resourcepacks/disabled.zip"),
            Map.of("assets/example/lang/en_US.lang", "machine.Name=Must not load\n"));
        Files.writeString(instance.resolve("options.txt"), "resourcePacks:[\"enabled.zip\"]\n");
        Path force = instance.resolve("config/txloader/forceload/example/lang/en_US.lang");
        Files.createDirectories(force.getParent());
        Files.writeString(force, "machine.Name=Forced pack\n");
        Files.writeString(
            instance.resolve("GregTech.lang"),
            "languagefile {\n    S:machine.Name=Generated default\n    S:only.english=Stale default\n}\n");
        byte[] jar = Files.readAllBytes(instance.resolve("mods/example.jar"));
        TranslationCatalog catalogue = TranslationCatalog.load(instance);
        assertTrue(catalogue.isAvailable());
        assertEquals(
            "Forced pack",
            catalogue.resolve("machine.Name", "en_US")
                .orElseThrow());
        assertEquals(
            "Nom français",
            catalogue.resolve("machine.Name", "fr_FR")
                .orElseThrow());
        assertEquals(
            "English fallback",
            catalogue.resolve("only.english", "fr_FR")
                .orElseThrow());
        assertTrue(
            catalogue.resolve("machine.name", "en_US")
                .isEmpty());
        assertTrue(
            catalogue.keys()
                .contains("machine.Name"));
        assertArrayEquals(jar, Files.readAllBytes(instance.resolve("mods/example.jar")));
    }

    @Test
    void readsGeneratedGregTechMachineNamesWithoutChangingTheFile() throws Exception {
        Files.createDirectory(instance.resolve("mods"));
        Path generated = instance.resolve("GregTech.lang");
        Files.writeString(
            generated,
            """
                # Configuration file
                languagefile {
                    S:gt.blockmachines.multimachine.entitycrusher.name=Extreme Entity Crusher
                    S:gt.blockmachines.multimachine.extremeapiary.name=Industrial Apicultural Acclimatiser and Drone Domestication Station
                    S:"gt.blockmachines.multimachine.nanochipmodule.smdprocessor.name"=Part Preparation Apparatus
                    B:not.a.translation=true
                }
                unrelated {
                    S:outside.languagefile=Must not load
                }
                """);
        byte[] original = Files.readAllBytes(generated);
        TranslationCatalog catalogue = TranslationCatalog.load(instance);
        Map<String, String> names = Map.of(
            "gt.blockmachines.multimachine.entitycrusher.name",
            "Extreme Entity Crusher",
            "gt.blockmachines.multimachine.extremeapiary.name",
            "Industrial Apicultural Acclimatiser and Drone Domestication Station",
            "gt.blockmachines.multimachine.nanochipmodule.smdprocessor.name",
            "Part Preparation Apparatus");
        names.forEach((key, name) -> {
            assertEquals(
                name,
                catalogue.resolve(key, "en_US")
                    .orElseThrow());
            assertEquals(
                name,
                catalogue.resolve(key, "fr_FR")
                    .orElseThrow());
            assertTrue(
                catalogue.keys()
                    .contains(key));
        });
        assertTrue(
            catalogue.resolve("not.a.translation", "en_US")
                .isEmpty());
        assertTrue(
            catalogue.resolve("outside.languagefile", "en_US")
                .isEmpty());
        assertTrue(
            catalogue.resolve("missing.machine.name", "en_US")
                .isEmpty());
        assertArrayEquals(original, Files.readAllBytes(generated));
    }

    @Test
    void unavailableCatalogueIsDifferentFromConfirmedMissingStaticKey() throws Exception {
        assertFalse(
            TranslationCatalog.empty()
                .isAvailable());
        Files.createDirectory(instance.resolve("mods"));
        TranslationCatalog catalogue = TranslationCatalog.load(instance);
        assertTrue(catalogue.isAvailable());
        assertTrue(
            catalogue.resolve("missing", "en_US")
                .isEmpty());
        assertThrows(java.io.IOException.class, () -> TranslationCatalog.load(instance.resolve("missing")));
    }
}
