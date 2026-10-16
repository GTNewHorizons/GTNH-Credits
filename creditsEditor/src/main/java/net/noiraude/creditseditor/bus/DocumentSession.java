package net.noiraude.creditseditor.bus;

import java.util.Map;
import java.util.Optional;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;

import org.jetbrains.annotations.NotNull;

/** Live view of an editing session's credits content and its owned lang documents. */
public interface DocumentSession {

    /** Returns a known translation with English fallback, including from partially loaded resources. */
    default Optional<String> externalTranslation(String key, String locale) {
        return Optional.empty();
    }

    /** Returns known English keys for reference search, including from partially loaded resources. */
    default java.util.List<String> externalTranslationKeys() {
        return java.util.List.of();
    }

    /** Whether base-game resources are available for checking absent external references. */
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
