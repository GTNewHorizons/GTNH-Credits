package net.noiraude.creditseditor.bus;

import java.util.Map;
import java.util.Optional;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;

import org.jetbrains.annotations.NotNull;

/** Live view of an editing session's credits content and its owned lang documents. */
public interface DocumentSession {

    /** Returns the optional catalog's translation with English fallback, or empty when unavailable. */
    default Optional<String> externalTranslation(String key, String locale) {
        return Optional.empty();
    }

    /** Returns the catalog's English keys for reference search, or an empty list when unavailable. */
    default java.util.List<String> externalTranslationKeys() {
        return java.util.List.of();
    }

    default boolean hasTranslationCatalog() {
        return false;
    }

    @NotNull
    CreditsDocument creditsDoc();

    @NotNull
    Map<String, LangDocument> langDocs();

    /** Returns the lang document registered for {@code locale}, if loaded. */
    @NotNull
    Optional<LangDocument> langDoc(@NotNull String locale);
}
