package net.noiraude.creditseditor.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeySanitizerTest {

    @Test
    public void simpleAlphanumeric_lowercased() {
        assertEquals("team", KeySanitizer.sanitize("team"));
        assertEquals("dev", KeySanitizer.sanitize("Dev"));
        assertEquals("contrib", KeySanitizer.sanitize("CONTRIB"));
    }

    @Test
    public void dots_areDeleted() {
        assertEquals("coremod", KeySanitizer.sanitize("Core.Mod"));
        assertEquals("coremod", KeySanitizer.sanitize("core.mod"));
    }

    @Test
    public void hyphens_areDeleted() {
        assertEquals("gtnhcreator", KeySanitizer.sanitize("gtnh-creator"));
        assertEquals("coreteam", KeySanitizer.sanitize("core-team"));
    }

    @Test
    public void spaces_becomeUnderscore() {
        assertEquals("key_test", KeySanitizer.sanitize("Key Test"));
        assertEquals("core_team", KeySanitizer.sanitize("core team"));
    }

    @Test
    public void multipleSpaces_collapseToOneUnderscore() {
        assertEquals("a_b", KeySanitizer.sanitize("a  b"));
        assertEquals("a_b", KeySanitizer.sanitize("a   b"));
    }

    @Test
    public void dotsAndHyphens_bothDeleted() {
        assertEquals("coremodteam", KeySanitizer.sanitize("Core.Mod-Team"));
    }

    @Test
    public void mixedSpecialChars_combined() {
        // "No.Trans Role" -> dots deleted, spaces -> _, lowercase -> "notrans_role"
        assertEquals("notrans_role", KeySanitizer.sanitize("No.Trans Role"));
    }

    @Test
    public void underscores_preserved() {
        assertEquals("my_key", KeySanitizer.sanitize("my_key"));
    }

    @Test
    public void emptyString_returnsEmpty() {
        assertEquals("", KeySanitizer.sanitize(""));
    }

    @Test
    public void singleLetter_lowercased() {
        assertEquals("a", KeySanitizer.sanitize("A"));
    }
}
