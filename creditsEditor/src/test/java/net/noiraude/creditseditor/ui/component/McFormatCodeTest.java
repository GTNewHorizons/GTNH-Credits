package net.noiraude.creditseditor.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
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
    public void parse_plainText_oneSingleSegmentNoColor() {
        List<McFormatCode.Segment> segments = McFormatCode.parse("hello");
        assertEquals(1, segments.size());
        assertEquals("hello", segments.getFirst().text);
        assertNull(McFormatCode.activeColor(segments.getFirst().codes));
        assertFalse(segments.getFirst().codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void parse_sectionSignAtEndOfString_emittedLiterally() {
        // A lone § at the very end (no following character) must be emitted as text
        List<McFormatCode.Segment> segments = McFormatCode.parse("hello§");
        assertEquals(1, segments.size());
        assertEquals("hello§", segments.getFirst().text);
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
        List<McFormatCode.Segment> segments = McFormatCode.parse("§l§otext");
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
        // §a is index 10 = green
        List<McFormatCode.Segment> segs = McFormatCode.parse("§agreen");
        assertEquals(1, segs.size());
        assertEquals(
            McFormatCode.COLORS.get(10).color,
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes)).color);
        assertEquals("green", segs.getFirst().text);
    }

    @Test
    public void parse_colorCode0_isBlack() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§0x");
        assertEquals(
            new Color(0x000000),
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes)).color);
    }

    @Test
    public void parse_colorCodef_isWhite() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§fx");
        assertEquals(
            new Color(0xFFFFFF),
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes)).color);
    }

    @Test
    public void parse_colorCodeUpperCase_treatedAsLowerCase() {
        // §A should behave the same as §a
        List<McFormatCode.Segment> segs = McFormatCode.parse("§Agreen");
        assertEquals(
            McFormatCode.COLORS.get(10).color,
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes)).color);
    }

    @Test
    public void parse_colorCode_resetsFormatModifiers() {
        // §l sets bold; §6 (color) should reset it
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§6text");
        assertEquals(1, segs.size());
        assertFalse("colour code must reset bold", segs.getFirst().codes.contains(McFormatCode.BOLD));
        assertEquals(
            McFormatCode.COLORS.get(6).color,
            Objects.requireNonNull(McFormatCode.activeColor(segs.getFirst().codes)).color);
    }

    // -----------------------------------------------------------------------
    // parse: format modifier codes
    // -----------------------------------------------------------------------

    @Test
    public void parse_boldCode_setsBold() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§lbold");
        assertTrue(segs.getFirst().codes.contains(McFormatCode.BOLD));
        assertFalse(segs.getFirst().codes.contains(McFormatCode.ITALIC));
    }

    @Test
    public void parse_italicCode_setsItalic() {
        assertTrue(
            McFormatCode.parse("§oitalic")
                .getFirst().codes.contains(McFormatCode.ITALIC));
    }

    @Test
    public void parse_underlineCode_setsUnderline() {
        assertTrue(
            McFormatCode.parse("§ntext")
                .getFirst().codes.contains(McFormatCode.UNDERLINE));
    }

    @Test
    public void parse_strikethroughCode_setsStrikethrough() {
        assertTrue(
            McFormatCode.parse("§mtext")
                .getFirst().codes.contains(McFormatCode.STRIKETHROUGH));
    }

    @Test
    public void parse_obfuscatedCode_setsObfuscated() {
        assertTrue(
            McFormatCode.parse("§ktext")
                .getFirst().codes.contains(McFormatCode.OBFUSCATED));
    }

    @Test
    public void parse_formatModifiers_areAdditive() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§o§ntext");
        McFormatCode.Segment seg = segs.getFirst();
        assertTrue(seg.codes.contains(McFormatCode.BOLD));
        assertTrue(seg.codes.contains(McFormatCode.ITALIC));
        assertTrue(seg.codes.contains(McFormatCode.UNDERLINE));
    }

    // -----------------------------------------------------------------------
    // parse: reset code §r
    // -----------------------------------------------------------------------

    @Test
    public void parse_resetCode_clearsAllModifiers() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§l§o§rtext");
        McFormatCode.Segment seg = segs.getFirst();
        assertFalse(seg.codes.contains(McFormatCode.BOLD));
        assertFalse(seg.codes.contains(McFormatCode.ITALIC));
        assertNull(McFormatCode.activeColor(seg.codes));
    }

    @Test
    public void parse_resetCode_clearsPreviousColor() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§a§rtext");
        assertNull(McFormatCode.activeColor(segs.getFirst().codes));
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
        // §6§lGTNH §f§lCreator: exactly the pattern from the lang file
        List<McFormatCode.Segment> segs = McFormatCode.parse("§6§lGTNH §f§lCreator");
        assertEquals(2, segs.size());

        McFormatCode.Segment first = segs.getFirst();
        assertEquals("GTNH ", first.text);
        assertEquals(
            McFormatCode.COLORS.get(6).color,
            Objects.requireNonNull(McFormatCode.activeColor(first.codes)).color); // gold
        assertTrue(first.codes.contains(McFormatCode.BOLD));

        McFormatCode.Segment second = segs.get(1);
        assertEquals("Creator", second.text);
        assertEquals(
            McFormatCode.COLORS.get(15).color,
            Objects.requireNonNull(McFormatCode.activeColor(second.codes)).color); // white
        assertTrue(second.codes.contains(McFormatCode.BOLD));
    }

    @Test
    public void parse_colorResetColor_threeSegments() {
        List<McFormatCode.Segment> segs = McFormatCode.parse("§agreen§rdefault§cred");
        assertEquals(3, segs.size());
        assertEquals("green", segs.getFirst().text);
        assertNotNull(McFormatCode.activeColor(segs.getFirst().codes));
        assertEquals("default", segs.get(1).text);
        assertNull(McFormatCode.activeColor(segs.get(1).codes));
        assertEquals("red", segs.get(2).text);
        assertEquals(
            McFormatCode.COLORS.get(12).color,
            Objects.requireNonNull(McFormatCode.activeColor(segs.get(2).codes)).color);
    }

    // -----------------------------------------------------------------------
    // COLORS
    // -----------------------------------------------------------------------

    @Test
    public void colors_hasExactly16Entries() {
        assertEquals(16, McFormatCode.COLORS.size());
    }

    @Test
    public void colors_allEntriesNonNull() {
        for (int i = 0; i < McFormatCode.COLORS.size(); i++) {
            assertNotNull("COLORS[" + i + "] is null", McFormatCode.COLORS.get(i).color);
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
        // lone § at end has no following char as it is literal content and must survive
        assertEquals("hello§", McFormatCode.strip("hello§"));
    }
}
