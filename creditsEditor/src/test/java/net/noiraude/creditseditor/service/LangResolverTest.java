package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;

import org.junit.jupiter.api.Test;

public class LangResolverTest {

    private static LangDocument doc(String text) throws Exception {
        return LangParser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, LangDocument> locales(LangDocument en, LangDocument fr) {
        Map<String, LangDocument> map = new LinkedHashMap<>();
        if (en != null) map.put("en_US", en);
        if (fr != null) map.put("fr_FR", fr);
        return map;
    }

    @Test
    public void activeLocaleHit_returnsActiveValue() throws Exception {
        LangResolver resolver = new LangResolver(
            locales(doc("credits.category.team=Team\n"), doc("credits.category.team=Equipe\n")));

        assertEquals(Optional.of("Equipe"), resolver.resolve("credits.category.team", "fr_FR"));
    }

    @Test
    public void activeLocaleEmptyString_fallsThroughToDefault() throws Exception {
        LangResolver resolver = new LangResolver(
            locales(doc("credits.category.team=Team\n"), doc("credits.category.team=\n")));

        assertEquals(Optional.of("Team"), resolver.resolve("credits.category.team", "fr_FR"));
    }

    @Test
    public void activeLocaleMissingKey_fallsThroughToDefault() throws Exception {
        LangResolver resolver = new LangResolver(locales(doc("credits.category.team=Team\n"), doc("")));

        assertEquals(Optional.of("Team"), resolver.resolve("credits.category.team", "fr_FR"));
    }

    @Test
    public void defaultLocaleEmptyString_returnsEmptyOptional() throws Exception {
        LangResolver resolver = new LangResolver(
            locales(doc("credits.category.team=\n"), doc("credits.category.team=\n")));

        assertFalse(
            resolver.resolve("credits.category.team", "fr_FR")
                .isPresent());
    }

    @Test
    public void bothTiersMissingKey_returnsEmptyOptional() throws Exception {
        LangResolver resolver = new LangResolver(locales(doc(""), doc("")));

        assertFalse(
            resolver.resolve("credits.category.team", "fr_FR")
                .isPresent());
    }

    @Test
    public void activeIsDefaultLocale_doesNotDoubleLookup() throws Exception {
        LangResolver resolver = new LangResolver(locales(doc("credits.category.team=Team\n"), null));

        assertEquals(Optional.of("Team"), resolver.resolve("credits.category.team", "en_US"));
    }

    @Test
    public void activeIsDefaultLocale_missingKey_returnsEmptyOptional() throws Exception {
        LangResolver resolver = new LangResolver(locales(doc(""), null));

        assertFalse(
            resolver.resolve("credits.category.team", "en_US")
                .isPresent());
    }

    @Test
    public void activeLocaleNotInMap_fallsThroughToDefault() throws Exception {
        LangResolver resolver = new LangResolver(locales(doc("credits.category.team=Team\n"), null));

        assertEquals(Optional.of("Team"), resolver.resolve("credits.category.team", "de_DE"));
    }

    @Test
    public void defaultLocaleNotInMap_returnsEmptyOptional() throws Exception {
        LangResolver resolver = new LangResolver(locales(null, doc("credits.category.team=Equipe\n")));

        assertEquals(Optional.of("Equipe"), resolver.resolve("credits.category.team", "fr_FR"));
        assertFalse(
            resolver.resolve("credits.category.support", "fr_FR")
                .isPresent());
    }

    @Test
    public void emptyMap_returnsEmptyOptional() {
        LangResolver resolver = new LangResolver(new LinkedHashMap<>());

        assertFalse(
            resolver.resolve("credits.category.team", "fr_FR")
                .isPresent());
        assertFalse(
            resolver.resolve("credits.category.team", "en_US")
                .isPresent());
    }

    @Test
    public void liveMapReference_observesUpdates() throws Exception {
        Map<String, LangDocument> map = locales(doc(""), null);
        LangResolver resolver = new LangResolver(map);

        assertFalse(
            resolver.resolve("credits.category.team", "en_US")
                .isPresent());

        map.get("en_US")
            .set("credits.category.team", "Team");

        assertTrue(
            resolver.resolve("credits.category.team", "en_US")
                .isPresent());
        assertEquals(Optional.of("Team"), resolver.resolve("credits.category.team", "en_US"));
    }
}
