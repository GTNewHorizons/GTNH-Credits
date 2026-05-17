package net.noiraude.creditseditor.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.TestDocumentSession;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.LangFieldWriter;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangKey;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;

import org.junit.jupiter.api.Test;

public class EditCategoryDisplayNameCommandTest {

    @Test
    public void execute_undo_redo_targetsCapturedLocaleOnly() {
        LangDocument en = LangParser.empty();
        LangDocument fr = LangParser.empty();
        en.set("credits.category.team", "Team");
        DocumentBus bus = new DocumentBus();
        bus.setSession(TestDocumentSession.of(CreditsDocument.empty(), en));

        LangKey key = new LangKey("credits.category.team");
        LangFieldWriter writer = LangFieldWriter.ofBus(bus, fr, key);

        Command cmd = EditCategoryDisplayNameCommand.create(writer, "", "Equipe");

        cmd.execute();
        assertEquals("Equipe", fr.get("credits.category.team"));
        assertEquals("Team", en.get("credits.category.team"));

        cmd.undo();
        assertNull(fr.get("credits.category.team"));
        assertEquals("Team", en.get("credits.category.team"));

        cmd.execute();
        assertEquals("Equipe", fr.get("credits.category.team"));
        assertEquals("Team", en.get("credits.category.team"));
    }
}
