package net.noiraude.creditseditor.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class LangServiceTest {

    private static LangDocument load(String content) throws IOException {
        return LangService.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String write(LangDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    @Test
    public void load_keyValueLine_readable() throws IOException {
        LangDocument doc = load("credits.category.team=Core Team\n");
        assertEquals("Core Team", doc.get("credits.category.team"));
    }

    @Test
    public void load_commentLine_doesNotAppearAsKey() throws IOException {
        LangDocument doc = load("# This is a comment\ncredits.category.team=Team\n");
        assertNull(doc.get("# This is a comment"));
        assertEquals("Team", doc.get("credits.category.team"));
    }

    @Test
    public void load_emptyValue_allowed() throws IOException {
        LangDocument doc = load("key=\n");
        assertEquals("", doc.get("key"));
    }

    @Test
    public void load_valueWithEquals_preservesRemainder() throws IOException {
        LangDocument doc = load("key=a=b\n");
        assertEquals("a=b", doc.get("key"));
    }

    @Test
    public void load_missingNewlineAtEnd_stillParsed() throws IOException {
        LangDocument doc = load("key=value");
        assertEquals("value", doc.get("key"));
    }

    // -----------------------------------------------------------------------
    // get / set / remove
    // -----------------------------------------------------------------------

    @Test
    public void set_existingKey_updatesInPlace() throws IOException {
        LangDocument doc = load("credits.category.team=Old Name\n");
        doc.set("credits.category.team", "New Name");
        assertEquals("New Name", doc.get("credits.category.team"));
    }

    @Test
    public void set_newKey_availableViaGet() throws IOException {
        LangDocument doc = load("gui.button=OK\n");
        doc.set("credits.category.team", "Core Team");
        assertEquals("Core Team", doc.get("credits.category.team"));
    }

    @Test
    public void remove_existingKey_getReturnsNull() throws IOException {
        LangDocument doc = load("credits.category.team=Core Team\n");
        doc.remove("credits.category.team");
        assertNull(doc.get("credits.category.team"));
    }

    @Test
    public void remove_pendingInsert_getReturnsNull() throws IOException {
        LangDocument doc = load("");
        doc.set("credits.category.team", "Core Team");
        doc.remove("credits.category.team");
        assertNull(doc.get("credits.category.team"));
    }

    @Test
    public void contains_returnsTrueForExistingKey() throws IOException {
        LangDocument doc = load("key=value\n");
        assertTrue(doc.contains("key"));
    }

    @Test
    public void contains_returnsFalseAfterRemoval() throws IOException {
        LangDocument doc = load("key=value\n");
        doc.remove("key");
        assertFalse(doc.contains("key"));
    }

    @Test
    public void set_afterRemove_restoresKey() throws IOException {
        LangDocument doc = load("credits.category.team=Old\n");
        doc.remove("credits.category.team");
        doc.set("credits.category.team", "Restored");
        assertEquals("Restored", doc.get("credits.category.team"));
    }

    // -----------------------------------------------------------------------
    // writeTo: foreign content preserved
    // -----------------------------------------------------------------------

    @Test
    public void write_foreignKeyPreservedExactly() throws IOException {
        String input = "gui.credits.title=Credits\n";
        LangDocument doc = load(input);
        assertEquals(input, write(doc));
    }

    @Test
    public void write_commentLinePreserved() throws IOException {
        String input = "# section header\ngui.button=OK\n";
        LangDocument doc = load(input);
        assertEquals(input, write(doc));
    }

    @Test
    public void write_blankLinePreserved() throws IOException {
        String input = "gui.a=A\n\ngui.b=B\n";
        LangDocument doc = load(input);
        assertEquals(input, write(doc));
    }

    @Test
    public void write_foreignKeyOrderPreserved() throws IOException {
        String input = "z=last\na=first\nm=middle\n";
        LangDocument doc = load(input);
        assertEquals(input, write(doc));
    }

    // -----------------------------------------------------------------------
    // writeTo: editor-owned key updates
    // -----------------------------------------------------------------------

    @Test
    public void write_updatedKeyInPlace() throws IOException {
        String input = "gui.button=OK\ncredits.category.team=Old\ngui.other=X\n";
        LangDocument doc = load(input);
        doc.set("credits.category.team", "New Name");
        String out = write(doc);
        assertTrue(out.contains("credits.category.team=New Name"));
        // foreign lines still present
        assertTrue(out.contains("gui.button=OK"));
        assertTrue(out.contains("gui.other=X"));
        // old value gone
        assertFalse(out.contains("=Old"));
    }

    @Test
    public void write_deletedKeyRemoved() throws IOException {
        String input = "gui.button=OK\ncredits.category.team=Core Team\n";
        LangDocument doc = load(input);
        doc.remove("credits.category.team");
        String out = write(doc);
        assertFalse(out.contains("credits.category.team"));
        assertTrue(out.contains("gui.button=OK"));
    }

    @Test
    public void write_newKeyAppendedAtEnd() throws IOException {
        String input = "gui.button=OK\n";
        LangDocument doc = load(input);
        doc.set("credits.category.team", "Core Team");
        String out = write(doc);
        int guiPos = out.indexOf("gui.button");
        int creditsPos = out.indexOf("credits.category.team");
        assertTrue("new key should appear after foreign content", creditsPos > guiPos);
        assertTrue(out.contains("credits.category.team=Core Team"));
    }

    @Test
    public void write_newKeysAppendedInInsertionOrder() throws IOException {
        LangDocument doc = load("gui.x=X\n");
        doc.set("credits.category.dev", "Developers");
        doc.set("credits.category.team", "Core Team");
        String out = write(doc);
        int devPos = out.indexOf("credits.category.dev");
        int teamPos = out.indexOf("credits.category.team");
        assertTrue("dev was inserted first, should appear first", devPos < teamPos);
    }

    @Test
    public void write_newKeyPrecededByBlankLine() throws IOException {
        String input = "gui.button=OK\n";
        LangDocument doc = load(input);
        doc.set("credits.category.team", "Core Team");
        String out = write(doc);
        // There should be a blank line between the last foreign line and the new section
        assertTrue(out.contains("OK\n\ncredits.category.team"));
    }

    // -----------------------------------------------------------------------
    // writeTo: blank-line collapsing
    // -----------------------------------------------------------------------

    @Test
    public void write_deletingAllKeysInSectionCollapsesBlankLines() throws IOException {
        String input = "gui.a=A\n\ncredits.category.team=Team\n\ngui.b=B\n";
        LangDocument doc = load(input);
        doc.remove("credits.category.team");
        String out = write(doc);
        // Should not have two consecutive blank lines
        assertFalse("consecutive blank lines should be collapsed", out.contains("\n\n\n"));
        assertTrue(out.contains("gui.a=A"));
        assertTrue(out.contains("gui.b=B"));
    }

    @Test
    public void write_emptyDocument_producesEmptyOutput() throws IOException {
        LangDocument doc = load("");
        assertEquals("", write(doc));
    }

    // -----------------------------------------------------------------------
    // Formatting codes in values
    // -----------------------------------------------------------------------

    @Test
    public void write_formattingCodesInValuePreserved() throws IOException {
        LangDocument doc = load("credits.person.role.creator=§6§lGTNH Creator\n");
        doc.set("credits.person.role.creator", "§6§lGTNH Creator");
        String out = write(doc);
        assertTrue(out.contains("credits.person.role.creator=§6§lGTNH Creator"));
    }
}
