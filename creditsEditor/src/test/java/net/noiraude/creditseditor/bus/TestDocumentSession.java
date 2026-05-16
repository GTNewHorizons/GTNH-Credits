package net.noiraude.creditseditor.bus;

import java.util.Collections;
import java.util.Map;

import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;

import org.jetbrains.annotations.NotNull;

/** Test-only {@link DocumentSession} record carrying explicit credits and lang documents. */
public record TestDocumentSession(@NotNull CreditsDocument creditsDoc, @NotNull Map<String, LangDocument> langDocs)
    implements DocumentSession {

    public static @NotNull DocumentSession of(@NotNull CreditsDocument creditsDoc) {
        return of(creditsDoc, LangParser.empty());
    }

    public static @NotNull DocumentSession of(@NotNull CreditsDocument creditsDoc, @NotNull LangDocument lang) {
        return new TestDocumentSession(creditsDoc, Collections.singletonMap(LangResolver.DEFAULT_LOCALE, lang));
    }
}
