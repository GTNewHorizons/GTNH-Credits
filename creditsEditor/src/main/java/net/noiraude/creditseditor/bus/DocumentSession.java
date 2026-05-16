package net.noiraude.creditseditor.bus;

import java.util.Map;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;

import org.jetbrains.annotations.NotNull;

/** Live view of an editing session's bundled credits structure and its lang documents. */
public interface DocumentSession {

    @NotNull
    CreditsDocument creditsDoc();

    @NotNull
    Map<String, LangDocument> langDocs();
}
