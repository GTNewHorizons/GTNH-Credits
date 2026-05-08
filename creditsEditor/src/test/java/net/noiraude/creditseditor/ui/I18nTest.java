package net.noiraude.creditseditor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
        assertEquals(key, I18n.get(key, MsgArg.text("ignored")));
    }

    @Test
    public void presentKey_isFormattedWithArgs() {
        assertEquals("About Credits Editor", I18n.get("menu.help.about", MsgArg.text("Credits Editor")));
    }

    @Test
    public void resolveLangBasename_exactMatch_wins() {
        Set<String> available = orderedSet("en_US", "fr_FR", "fr_CA");
        assertEquals(Optional.of("fr_FR"), I18n.resolveLangBasename(Locale.FRANCE, available));
    }

    @Test
    public void resolveLangBasename_prefixMatch_whenCountryMissingFromAvailable() {
        Set<String> available = orderedSet("en_US", "fr_CA");
        assertEquals(Optional.of("fr_CA"), I18n.resolveLangBasename(Locale.FRANCE, available));
    }

    @Test
    public void resolveLangBasename_languageOnlyJvmLocale_takesPrefixBranch() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertEquals(Optional.of("fr_FR"), I18n.resolveLangBasename(Locale.of("fr"), available));
    }

    @Test
    public void resolveLangBasename_noMatch_returnsEmpty() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertFalse(
            I18n.resolveLangBasename(Locale.GERMANY, available)
                .isPresent());
    }

    @Test
    public void resolveLangBasename_emptyAvailable_returnsEmpty() {
        assertFalse(
            I18n.resolveLangBasename(Locale.FRANCE, Collections.emptySet())
                .isPresent());
    }

    @Test
    public void resolveLangBasename_blankLanguage_returnsEmpty() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertFalse(
            I18n.resolveLangBasename(Locale.of(""), available)
                .isPresent());
    }

    private static Set<String> orderedSet(String... entries) {
        Set<String> set = new LinkedHashSet<>();
        Collections.addAll(set, entries);
        return set;
    }
}
