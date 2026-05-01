package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class I18nTest {

    @Test
    public void missingKey_returnsKeyItself() {
        String key = "this.key.deliberately.does.not.exist";
        assertEquals(key, I18n.get(key));
    }

    @Test
    public void missingKey_withArgs_stillReturnsKey() {
        String key = "another.absent.key";
        assertEquals(key, I18n.get(key, "ignored"));
    }

    @Test
    public void presentKey_isFormattedWithArgs() {
        assertEquals("About Credits Editor", I18n.get("menu.help.about", "Credits Editor"));
    }
}
