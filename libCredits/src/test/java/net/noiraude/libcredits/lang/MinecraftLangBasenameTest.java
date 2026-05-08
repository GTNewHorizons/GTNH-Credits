package net.noiraude.libcredits.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

public class MinecraftLangBasenameTest {

    @Test
    public void exactMatch_wins() {
        Set<String> available = orderedSet("en_US", "fr_FR", "fr_CA");
        assertEquals(Optional.of("fr_FR"), MinecraftLangBasename.forJavaLocale(Locale.FRANCE, available));
    }

    @Test
    public void prefixMatch_whenCountryMissingFromAvailable() {
        Set<String> available = orderedSet("en_US", "fr_CA");
        assertEquals(Optional.of("fr_CA"), MinecraftLangBasename.forJavaLocale(Locale.FRANCE, available));
    }

    @Test
    public void languageOnlyJvmLocale_takesPrefixBranch() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertEquals(Optional.of("fr_FR"), MinecraftLangBasename.forJavaLocale(new Locale("fr"), available));
    }

    @Test
    public void noMatch_returnsEmpty() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertFalse(
            MinecraftLangBasename.forJavaLocale(Locale.GERMANY, available)
                .isPresent());
    }

    @Test
    public void emptyAvailable_returnsEmpty() {
        assertFalse(
            MinecraftLangBasename.forJavaLocale(Locale.FRANCE, Collections.<String>emptySet())
                .isPresent());
    }

    @Test
    public void blankLanguage_returnsEmpty() {
        Set<String> available = orderedSet("en_US", "fr_FR");
        assertFalse(
            MinecraftLangBasename.forJavaLocale(new Locale(""), available)
                .isPresent());
    }

    private static Set<String> orderedSet(String... entries) {
        Set<String> set = new LinkedHashSet<String>();
        Collections.addAll(set, entries);
        return set;
    }
}
