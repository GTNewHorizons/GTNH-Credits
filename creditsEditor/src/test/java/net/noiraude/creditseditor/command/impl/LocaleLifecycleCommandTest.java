package net.noiraude.creditseditor.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleSnapshot;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.resource.ResourceManager;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.pack.CreditsLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LocaleLifecycleCommandTest {

    @TempDir
    Path temp;

    @Test
    public void addLocaleCommand_executesAndUndoes() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        try (ResourceManager rm = ResourceManager.open(dir.toString())) {
            rm.loadDocuments();
            DocumentBus bus = new DocumentBus();
            TestSession session = new TestSession(rm);
            bus.setSession(session);

            Command cmd = AddLocaleCommand.create(session, bus, "fr_FR");
            cmd.execute();

            assertEquals(Set.of("en_US", "fr_FR"), rm.availableLocales());
            assertEquals("fr_FR", bus.activeLocale());

            cmd.undo();

            assertEquals(Set.of("en_US"), rm.availableLocales());
            assertEquals(LangResolver.DEFAULT_LOCALE, bus.activeLocale());
        }
    }

    @Test
    public void removeLocaleCommand_restoresLangDocumentVerbatimOnUndo() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        Path langDir = dir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(
            langDir.resolve("fr_FR.lang"),
            "credits.category.team=Equipe\ncredits.person.role.dev=Developpeur\n",
            StandardCharsets.UTF_8);

        try (ResourceManager rm = ResourceManager.open(dir.toString())) {
            rm.loadDocuments();
            DocumentBus bus = new DocumentBus();
            TestSession session = new TestSession(rm);
            bus.setSession(session);
            bus.setActiveLocale("fr_FR");

            LangDocument frBefore = rm.langDoc("fr_FR")
                .orElseThrow();

            Command cmd = RemoveLocaleCommand.create(session, bus, "fr_FR");
            cmd.execute();

            assertEquals(Set.of("en_US"), rm.availableLocales());
            assertEquals(LangResolver.DEFAULT_LOCALE, bus.activeLocale());
            assertTrue(rm.isDirty(), "pending removal must mark dirty");

            cmd.undo();

            assertEquals(Set.of("en_US", "fr_FR"), rm.availableLocales());
            assertEquals("fr_FR", bus.activeLocale());
            LangDocument frRestored = rm.langDoc("fr_FR")
                .orElseThrow();
            assertSame(frBefore, frRestored, "undo must restore the exact same LangDocument instance");
            assertEquals("Equipe", frRestored.get("credits.category.team"));
            assertEquals("Developpeur", frRestored.get("credits.person.role.dev"));
        }
    }

    @Test
    public void addLocaleCommand_snapshotPreservesPriorRemovedState() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("root"));
        Path langDir = dir.resolve(CreditsLayout.LANG_DIR.get());
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("de_DE.lang"), "credits.category.team=Team\n", StandardCharsets.UTF_8);

        try (ResourceManager rm = ResourceManager.open(dir.toString())) {
            rm.loadDocuments();
            rm.removeLocale("de_DE");
            assertTrue(rm.isDirty());

            DocumentBus bus = new DocumentBus();
            TestSession session = new TestSession(rm);
            bus.setSession(session);
            LocaleSnapshot before = rm.snapshotLocale("de_DE");
            assertTrue(
                before.doc()
                    .isEmpty());
            assertTrue(before.pendingRemoval());

            Command cmd = AddLocaleCommand.create(session, bus, "de_DE");
            cmd.execute();
            assertEquals(Set.of("en_US", "de_DE"), rm.availableLocales());

            cmd.undo();
            assertFalse(
                rm.availableLocales()
                    .contains("de_DE"),
                "de_DE must remain dropped after undo");
            LocaleSnapshot after = rm.snapshotLocale("de_DE");
            assertTrue(after.pendingRemoval(), "prior removed-set membership must be restored");
            assertNotSame(before, after);
        }
    }

    /** Minimal DocumentSession + LocaleEditor pair backed by a ResourceManager. */
    private record TestSession(ResourceManager rm)
        implements net.noiraude.creditseditor.bus.DocumentSession, net.noiraude.creditseditor.bus.LocaleEditor {

        @Override
        public net.noiraude.libcredits.model.CreditsDocument creditsDoc() {
            return rm.getCreditsDoc();
        }

        @Override
        public java.util.Map<String, LangDocument> langDocs() {
            return rm.langDocs();
        }

        @Override
        public java.util.Optional<LangDocument> langDoc(String locale) {
            return rm.langDoc(locale);
        }

        @Override
        public void addLocale(String locale) {
            rm.addLocale(locale);
        }

        @Override
        public void removeLocale(String locale) {
            rm.removeLocale(locale);
        }

        @Override
        public LocaleSnapshot snapshotLocale(String locale) {
            return rm.snapshotLocale(locale);
        }

        @Override
        public void applyLocaleSnapshot(String locale, LocaleSnapshot snapshot) {
            rm.applyLocaleSnapshot(locale, snapshot);
        }
    }
}
