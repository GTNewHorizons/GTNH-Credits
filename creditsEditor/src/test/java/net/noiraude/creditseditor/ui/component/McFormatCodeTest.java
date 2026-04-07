package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.List;

import org.junit.Test;

public class McFormatCodeTest {

    // -----------------------------------------------------------------------
    // parse: edge cases
    // -----------------------------------------------------------------------

    @Test
    public void parse_null_returnsEmptyList() {
        assertTrue(
            McFormatCode.parse(null)
                .isEmpty());
    }

    @Test
    public void parse_empty_returnsEmptyList() {
        assertTrue(
            McFormatCode.parse("")
                .isEmpty());
    }

    @Test
    public void parse_plainText_oneSingleSegmentNullColor() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("hello");
        assertEquals(1, segs.size());
        assertEquals("hello", segs.getFirst().text);
        assertNull(segs.getFirst().color);
        assertFalse(segs.getFirst().bold);
    }

    @Test
    public void parse_sectionSignAtEndOfString_emittedLiterally() {
        // A lone § at the very end (no following character) must be emitted as text
        List<McFormatCode.Segment> segs = McFormatCode.parse("hello§");
        assertEquals(1, segs.size());
        assertEquals("hello§", segs.getFirst().text);
    }

    @Test
    public void parse_codeOnly_noTextSegments() {
        // §r with no surrounding text produces no segments
        assertTrue(
            McFormatCode.parse("§r")
                .isEmpty());
    }

    @Test
    public void parse_adjacentCodes_noEmptySegment() {
        // §l§o should yield no empty segment between them
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§otext");
        assertEquals(1, segs.size());
        assertEquals("text", segs.getFirst().text);
        assertTrue(segs.getFirst().bold);
        assertTrue(segs.getFirst().italic);
    }

    // -----------------------------------------------------------------------
    // parse: colour codes
    // -----------------------------------------------------------------------

    @Test
    public void parse_colorCode_setsCorrectPaletteColor() {
        // §a is index 10 = green
        List<McFormatCode.Segment> segs = McFormatCode.parse("§agreen");
        assertEquals(1, segs.size());
        assertEquals(McFormatCode.PALETTE[10], segs.getFirst().color);
        assertEquals("green", segs.getFirst().text);
    }

    @Test
    public void parse_colorCode0_isBlack() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§0x");
        assertEquals(new Color(0x000000), segs.getFirst().color);
    }

    @Test
    public void parse_colorCodef_isWhite() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§fx");
        assertEquals(new Color(0xFFFFFF), segs.getFirst().color);
    }

    @Test
    public void parse_colorCodeUpperCase_treatedAsLowerCase() {
        // §A should behave the same as §a
        List<McFormatCode.Segment> segs = McFormatCode.parse("§Agreen");
        assertEquals(McFormatCode.PALETTE[10], segs.getFirst().color);
    }

    @Test
    public void parse_colorCode_resetsFormatModifiers() {
        // §l sets bold; §6 (color) should reset it
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§6text");
        assertEquals(1, segs.size());
        assertFalse("colour code must reset bold", segs.getFirst().bold);
        assertEquals(McFormatCode.PALETTE[6], segs.getFirst().color);
    }

    // -----------------------------------------------------------------------
    // parse: format modifier codes
    // -----------------------------------------------------------------------

    @Test
    public void parse_boldCode_setsBold() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§lbold");
        assertTrue(segs.getFirst().bold);
        assertFalse(segs.getFirst().italic);
    }

    @Test
    public void parse_italicCode_setsItalic() {
        assertTrue(
            McFormatCode.parse("§oitalic")
                .getFirst().italic);
    }

    @Test
    public void parse_underlineCode_setsUnderline() {
        assertTrue(
            McFormatCode.parse("§ntext")
                .getFirst().underline);
    }

    @Test
    public void parse_strikethroughCode_setsStrikethrough() {
        assertTrue(
            McFormatCode.parse("§mtext")
                .getFirst().strikethrough);
    }

    @Test
    public void parse_obfuscatedCode_setsObfuscated() {
        assertTrue(
            McFormatCode.parse("§ktext")
                .getFirst().obfuscated);
    }

    @Test
    public void parse_formatModifiers_areAdditive() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§o§ntext");
        McFormatCode.Segment seg = segs.getFirst();
        assertTrue(seg.bold);
        assertTrue(seg.italic);
        assertTrue(seg.underline);
    }

    // -----------------------------------------------------------------------
    // parse: reset code §r
    // -----------------------------------------------------------------------

    @Test
    public void parse_resetCode_clearsAllModifiers() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§o§rtext");
        McFormatCode.Segment seg = segs.getFirst();
        assertFalse(seg.bold);
        assertFalse(seg.italic);
        assertNull(seg.color);
    }

    @Test
    public void parse_resetCode_clearsPreviousColor() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§a§rtext");
        assertNull(segs.getFirst().color);
    }

    // -----------------------------------------------------------------------
    // parse: unknown code
    // -----------------------------------------------------------------------

    @Test
    public void parse_unknownCode_emittedAsLiteralText() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§ztext");
        assertEquals(1, segs.size());
        assertEquals("§ztext", segs.getFirst().text);
    }

    // -----------------------------------------------------------------------
    // parse: multi-segment strings
    // -----------------------------------------------------------------------

    @Test
    public void parse_goldBoldThenWhiteBold_twoSegments() {
        // §6§lGTNH §f§lCreator — exactly the pattern from the lang file
        List<McFormatCode.Segment> segs = McFormatCode.parse("§6§lGTNH §f§lCreator");
        assertEquals(2, segs.size());

        McFormatCode.Segment first = segs.getFirst();
        assertEquals("GTNH ", first.text);
        assertEquals(McFormatCode.PALETTE[6], first.color); // gold
        assertTrue(first.bold);

        McFormatCode.Segment second = segs.get(1);
        assertEquals("Creator", second.text);
        assertEquals(McFormatCode.PALETTE[15], second.color); // white
        assertTrue(second.bold);
    }

    @Test
    public void parse_colorResetColor_threeSegments() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§agreen§rdefault§cred");
        assertEquals(3, segs.size());
        assertEquals("green", segs.getFirst().text);
        assertNotNull(segs.getFirst().color);
        assertEquals("default", segs.get(1).text);
        assertNull(segs.get(1).color);
        assertEquals("red", segs.get(2).text);
        assertEquals(McFormatCode.PALETTE[12], segs.get(2).color);
    }

    // -----------------------------------------------------------------------
    // PALETTE
    // -----------------------------------------------------------------------

    @Test
    public void palette_hasExactly16Colors() {
        assertEquals(16, McFormatCode.PALETTE.length);
    }

    @Test
    public void palette_allEntriesNonNull() {
        for (int i = 0; i < McFormatCode.PALETTE.length; i++) {
            assertNotNull("PALETTE[" + i + "] is null", McFormatCode.PALETTE[i]);
        }
    }

    // -----------------------------------------------------------------------
    // strip
    // -----------------------------------------------------------------------

    @Test
    public void strip_null_returnsNull() {
        assertNull(McFormatCode.strip(null));
    }

    @Test
    public void strip_empty_returnsEmpty() {
        assertEquals("", McFormatCode.strip(""));
    }

    @Test
    public void strip_plainText_unchanged() {
        assertEquals("hello", McFormatCode.strip("hello"));
    }

    @Test
    public void strip_allCodesRemoved() {
        assertEquals("GTNH Creator", McFormatCode.strip("§6§lGTNH §f§lCreator"));
    }

    @Test
    public void strip_trailingSection_preserved() {
        // lone § at end has no following char — it is literal content and must survive
        assertEquals("hello§", McFormatCode.strip("hello§"));
    }
}
