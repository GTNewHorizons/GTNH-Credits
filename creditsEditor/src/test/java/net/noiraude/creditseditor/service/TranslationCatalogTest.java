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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            instance.resolve("versions/1.7.10/1.7.10.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Furnace\n"));
        archive(
            instance.resolve("mods/example.jar"),
            Map.of(
                "assets/example/lang/en_US.lang",
                "machine.Name=English name\nonly.english=English fallback\n",
                "assets/example/lang/fr_FR.lang",
                "machine.Name=Nom français\n"));
        Path load = instance.resolve("config/txloader/load/example/lang/en_US.lang");
        Files.createDirectories(load.getParent());
        Files.writeString(load, "machine.Name=TX Loader\n");
        assertEquals(
            "TX Loader",
            TranslationCatalog.load(instance)
                .resolve("machine.Name", "en_US")
                .orElseThrow());
        archive(
            instance.resolve("resourcepacks/enabled.zip"),
            Map.of("assets/example/lang/en_US.lang", "machine.Name=Selected pack\n"));
        archive(
            instance.resolve("resourcepacks/disabled.zip"),
            Map.of("assets/example/lang/en_US.lang", "machine.Name=Must not load\n"));
        Files.writeString(instance.resolve("options.txt"), "resourcePacks:[\"enabled.zip\"]\n");
        assertEquals(
            "Selected pack",
            TranslationCatalog.load(instance)
                .resolve("machine.Name", "en_US")
                .orElseThrow());
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
    void readsPrismBaseGameAndIndexedLanguagesFromTheSelectedVersion() throws Exception {
        Path game = instance.resolve("launcher/instances/pack/.minecraft");
        Files.createDirectories(game.resolve("mods"));
        Files.writeString(
            game.getParent()
                .resolve("mmc-pack.json"),
            "{\"components\":[{\"uid\":\"net.minecraft\",\"version\":\"1.7.10\"}]}");
        Path launcher = instance.resolve("launcher");
        archive(
            launcher.resolve("libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Furnace\n"));
        archive(
            launcher.resolve("libraries/com/mojang/minecraft/1.12.2/minecraft-1.12.2-client.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Wrong version\n"));
        Path assets = launcher.resolve("assets");
        String hash = "1234567890abcdef1234567890abcdef12345678";
        Path french = assets.resolve("objects/12/" + hash);
        Files.createDirectories(french.getParent());
        Files.writeString(french, "tile.furnace.name=Four\n");
        Files.createDirectories(assets.resolve("indexes"));
        Files.writeString(
            assets.resolve("indexes/1.7.10.json"),
            "{\"objects\":{\"minecraft/lang/fr_FR.lang\":{\"hash\":\"" + hash + "\"}}}");
        TranslationCatalog catalogue = TranslationCatalog.load(game);
        assertTrue(catalogue.isAvailable());
        assertEquals(
            "Furnace",
            catalogue.resolve("tile.furnace.name", "en_US")
                .orElseThrow());
        assertEquals(
            "Four",
            catalogue.resolve("tile.furnace.name", "fr_FR")
                .orElseThrow());
        assertEquals(
            "Furnace",
            catalogue.resolve("tile.furnace.name", "de_DE")
                .orElseThrow());
    }

    @Test
    void ignoresInactiveArchivesAndReadsOnlyTopLevelAndActiveVersionMods() throws Exception {
        archive(instance.resolve("mods/example.jar"), Map.of("assets/example/lang/en_US.lang", "active.root=Root\n"));
        archive(
            instance.resolve("mods/1.7.10/version.jar"),
            Map.of("assets/example/lang/en_US.lang", "active.version=Version\n"));
        archive(instance.resolve("mods/archive.zip"), Map.of("assets/example/lang/en_US.lang", "active.zip=ZIP\n"));
        for (String path : new String[] { "1.12.2/inactive.jar", "backup/inactive.jar", "1.7.10/backup/inactive.jar",
            "disabled.jar.old" }) {
            archive(
                instance.resolve("mods")
                    .resolve(path),
                Map.of("assets/example/lang/en_US.lang", "inactive.machine=Inactive\n"));
        }
        Files.createDirectory(instance.resolve("mods/directory.jar"));
        TranslationCatalog catalogue = TranslationCatalog.load(instance);
        assertEquals(
            "Root",
            catalogue.resolve("active.root", "en_US")
                .orElseThrow());
        assertEquals(
            "Version",
            catalogue.resolve("active.version", "en_US")
                .orElseThrow());
        assertEquals(
            "ZIP",
            catalogue.resolve("active.zip", "en_US")
                .orElseThrow());
        assertTrue(
            catalogue.resolve("inactive.machine", "en_US")
                .isEmpty());
        assertFalse(
            catalogue.keys()
                .contains("inactive.machine"));
    }

    @Test
    void doesNotUseCachedBaseResourcesForAnUnsupportedSelectedVersion() throws Exception {
        Path game = instance.resolve("launcher/instances/pack/.minecraft");
        Files.createDirectories(game.resolve("mods"));
        Files.writeString(
            game.getParent()
                .resolve("mmc-pack.json"),
            "{\"components\":[{\"uid\":\"net.minecraft\",\"version\":\"1.12.2\"}]}");
        archive(
            instance.resolve("launcher/libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Wrong version\n"));
        TranslationCatalog catalogue = TranslationCatalog.load(game);
        assertFalse(catalogue.isAvailable());
        assertTrue(
            catalogue.resolve("tile.furnace.name", "en_US")
                .isEmpty());
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

    @ParameterizedTest
    @ValueSource(strings = { "Stale saved name", "" })
    void usesGregTechDefaultsOnlyForMissingEnglishTranslations(String savedName) throws Exception {
        Files.createDirectory(instance.resolve("mods"));
        archive(
            instance.resolve("versions/1.7.10/1.7.10.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Furnace\n"));
        Files.writeString(
            instance.resolve("GregTech.lang"),
            "languagefile {\n    S:tile.furnace.name=" + savedName + "\n    S:saved.only=Saved default\n}\n");

        TranslationCatalog catalogue = TranslationCatalog.load(instance);

        assertTrue(catalogue.isAvailable());
        assertEquals(
            "Furnace",
            catalogue.resolve("tile.furnace.name", "en_US")
                .orElseThrow());
        assertEquals(
            "Furnace",
            catalogue.resolve("tile.furnace.name", "fr_FR")
                .orElseThrow());
        assertEquals(
            "Saved default",
            catalogue.resolve("saved.only", "en_US")
                .orElseThrow());
    }

    @Test
    void unavailableCatalogueIsDifferentFromConfirmedMissingStaticKey() throws Exception {
        assertFalse(
            TranslationCatalog.empty()
                .isAvailable());
        archive(
            instance.resolve("mods/example.jar"),
            Map.of("assets/example/lang/en_US.lang", "mod.machine=Machine\n"));
        TranslationCatalog catalogue = TranslationCatalog.load(instance);
        assertFalse(catalogue.isAvailable());
        assertEquals(
            "Machine",
            catalogue.resolve("mod.machine", "en_US")
                .orElseThrow());
        assertTrue(
            catalogue.keys()
                .contains("mod.machine"));
        archive(
            instance.resolve("versions/1.7.10/1.7.10.jar"),
            Map.of("assets/minecraft/lang/en_US.lang", "tile.furnace.name=Furnace\n"));
        catalogue = TranslationCatalog.load(instance);
        assertTrue(catalogue.isAvailable());
        assertTrue(
            catalogue.resolve("missing", "en_US")
                .isEmpty());
        assertThrows(java.io.IOException.class, () -> TranslationCatalog.load(instance.resolve("missing")));
    }
}
