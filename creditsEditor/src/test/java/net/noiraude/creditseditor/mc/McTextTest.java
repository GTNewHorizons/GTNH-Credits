package net.noiraude.creditseditor.mc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;

import org.junit.Test;

@SuppressWarnings({ "SpellCheckingInspection" })
public class McTextTest {

    // -----------------------------------------------------------------------
    // parse: edge cases
    // -----------------------------------------------------------------------

    @Test
    public void parse_null_returnsEmptyList() {
        assertTrue(
            McText.parse(null)
                .isEmpty());
    }

    @Test
    public void parse_empty_returnsEmptyList() {
        assertTrue(
            McText.parse("")
                .isEmpty());
    }

    @Test
    public void parse_plainText_oneSingleSegmentNoColor() {
        List<McText.Segment> segments = McText.parse("hello");
        assertEquals(1, segments.size());
        assertEquals("hello", segments.getFirst().text);
        assertNull(McFormatCode.activeColor(segments.getFirst().codes));
        assertFalse(segments.getFirst().codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void parse_sectionSignAtEndOfString_emittedLiterally() {
        // A lone § at the very end (no following character) must be emitted as text
        List<McText.Segment> segments = McText.parse("hello§");
        assertEquals(1, segments.size());
        assertEquals("hello§", segments.getFirst().text);
    }

    @Test
    public void parse_codeOnly_noTextSegments() {
        // §r with no surrounding text produces no segments
        assertTrue(
            McText.parse("§r")
                .isEmpty());
    }

    @Test
    public void parse_adjacentCodes_noEmptySegment() {
        // §l§o should yield no empty segment between them
        List<McText.Segment> segments = McText.parse("§l§otext");
        assertEquals(1, segments.size());
        assertEquals("text", segments.getFirst().text);
        assertTrue(segments.getFirst().codes.contains(McFormatCode.BOLD));
        assertTrue(segments.getFirst().codes.contains(McFormatCode.ITALIC));
    }

    // -----------------------------------------------------------------------
    // parse: colour codes
    // -----------------------------------------------------------------------

    @Test
    public void parse_colorCode_setsCorrectPaletteColor() {
        // §a = GREEN (palette index 10)
        List<McText.Segment> segs = McText.parse("§agreen");
        assertEquals(1, segs.size());
        assertEquals(McFormatCode.GREEN, McFormatCode.activeColor(segs.getFirst().codes));
        assertEquals(
            McFormatCode.PALETTE[10].rgb(),
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes))
                .rgb());
        assertEquals("green", segs.getFirst().text);
    }

    @Test
    public void parse_colorCode0_isBlack() {
        List<McText.Segment> segs = McText.parse("§0x");
        assertEquals(
            0x000000,
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes))
                .rgb());
    }

    @Test
    public void parse_colorCodef_isWhite() {
        List<McText.Segment> segs = McText.parse("§fx");
        assertEquals(
            0xFFFFFF,
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes))
                .rgb());
    }

    @Test
    public void parse_colorCodeUpperCase_treatedAsLowerCase() {
        // §A should behave the same as §a
        List<McText.Segment> segs = McText.parse("§Agreen");
        assertEquals(McFormatCode.GREEN, McFormatCode.activeColor(segs.getFirst().codes));
    }

    @Test
    public void parse_colorCode_resetsFormatModifiers() {
        // §l sets bold; §6 (color) should reset it
        List<McText.Segment> segs = McText.parse("§l§6text");
        assertEquals(1, segs.size());
        assertFalse("colour code must reset bold", segs.getFirst().codes.contains(McFormatCode.BOLD));
        assertEquals(McFormatCode.GOLD, McFormatCode.activeColor(segs.getFirst().codes));
    }

    // -----------------------------------------------------------------------
    // parse: format modifier codes
    // -----------------------------------------------------------------------

    @Test
    public void parse_boldCode_setsBold() {
        List<McText.Segment> segs = McText.parse("§lbold");
        assertTrue(segs.getFirst().codes.contains(McFormatCode.BOLD));
        assertFalse(segs.getFirst().codes.contains(McFormatCode.ITALIC));
    }

    @Test
    public void parse_italicCode_setsItalic() {
        assertTrue(
            McText.parse("§oitalic")
                .getFirst().codes.contains(McFormatCode.ITALIC));
    }

    @Test
    public void parse_underlineCode_setsUnderline() {
        assertTrue(
            McText.parse("§ntext")
                .getFirst().codes.contains(McFormatCode.UNDERLINE));
    }

    @Test
    public void parse_strikethroughCode_setsStrikethrough() {
        assertTrue(
            McText.parse("§mtext")
                .getFirst().codes.contains(McFormatCode.STRIKETHROUGH));
    }

    @Test
    public void parse_obfuscatedCode_setsObfuscated() {
        assertTrue(
            McText.parse("§ktext")
                .getFirst().codes.contains(McFormatCode.OBFUSCATED));
    }

    @Test
    public void parse_formatModifiers_areAdditive() {
        List<McText.Segment> segs = McText.parse("§l§o§ntext");
        McText.Segment seg = segs.getFirst();
        assertTrue(seg.codes.contains(McFormatCode.BOLD));
        assertTrue(seg.codes.contains(McFormatCode.ITALIC));
        assertTrue(seg.codes.contains(McFormatCode.UNDERLINE));
    }

    // -----------------------------------------------------------------------
    // parse: reset code §r
    // -----------------------------------------------------------------------

    @Test
    public void parse_resetCode_clearsAllModifiers() {
        List<McText.Segment> segs = McText.parse("§l§o§rtext");
        McText.Segment seg = segs.getFirst();
        assertFalse(seg.codes.contains(McFormatCode.BOLD));
        assertFalse(seg.codes.contains(McFormatCode.ITALIC));
        assertNull(McFormatCode.activeColor(seg.codes));
    }

    @Test
    public void parse_resetCode_clearsPreviousColor() {
        List<McText.Segment> segs = McText.parse("§a§rtext");
        assertNull(McFormatCode.activeColor(segs.getFirst().codes));
    }

    // -----------------------------------------------------------------------
    // parse: unknown code
    // -----------------------------------------------------------------------

    @Test
    public void parse_unknownCode_emittedAsLiteralText() {
        List<McText.Segment> segs = McText.parse("§ztext");
        assertEquals(1, segs.size());
        assertEquals("§ztext", segs.getFirst().text);
    }

    // -----------------------------------------------------------------------
    // parse: multi-segment strings
    // -----------------------------------------------------------------------

    @Test
    public void parse_goldBoldThenWhiteBold_twoSegments() {
        // §6§lGTNH §f§lCreator: exactly the pattern from the lang file
        List<McText.Segment> segs = McText.parse("§6§lGTNH §f§lCreator");
        assertEquals(2, segs.size());

        McText.Segment first = segs.getFirst();
        assertEquals("GTNH ", first.text);
        assertEquals(McFormatCode.GOLD, McFormatCode.activeColor(first.codes));
        assertTrue(first.codes.contains(McFormatCode.BOLD));

        McText.Segment second = segs.get(1);
        assertEquals("Creator", second.text);
        assertEquals(McFormatCode.WHITE, McFormatCode.activeColor(second.codes));
        assertTrue(second.codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void parse_colorResetColor_threeSegments() {
        List<McText.Segment> segs = McText.parse("§agreen§rdefault§cred");
        assertEquals(3, segs.size());
        assertEquals("green", segs.getFirst().text);
        assertNotNull(McFormatCode.activeColor(segs.getFirst().codes));
        assertEquals("default", segs.get(1).text);
        assertNull(McFormatCode.activeColor(segs.get(1).codes));
        assertEquals("red", segs.get(2).text);
        assertEquals(McFormatCode.RED, McFormatCode.activeColor(segs.get(2).codes));
    }

    // -----------------------------------------------------------------------
    // PALETTE
    // -----------------------------------------------------------------------

    @Test
    public void palette_hasExactly16Entries() {
        assertEquals(16, McFormatCode.PALETTE.length);
    }

    @Test
    public void palette_allEntriesAreColorCodes() {
        for (int i = 0; i < McFormatCode.PALETTE.length; i++) {
            assertTrue("PALETTE[" + i + "] is not a colour code", McFormatCode.PALETTE[i].isColor());
        }
    }

    // -----------------------------------------------------------------------
    // strip
    // -----------------------------------------------------------------------

    @Test
    public void strip_null_returnsNull() {
        assertNull(null);
    }

    @Test
    public void strip_empty_returnsEmpty() {
        assertEquals("", McText.strip(""));
    }

    @Test
    public void strip_plainText_unchanged() {
        assertEquals("hello", McText.strip("hello"));
    }

    @Test
    public void strip_allCodesRemoved() {
        assertEquals("GTNH Creator", McText.strip("§6§lGTNH §f§lCreator"));
    }

    @Test
    public void strip_trailingSection_preserved() {
        // lone § at end has no following char as it is literal content and must survive
        assertEquals("hello§", McText.strip("hello§"));
    }
}
