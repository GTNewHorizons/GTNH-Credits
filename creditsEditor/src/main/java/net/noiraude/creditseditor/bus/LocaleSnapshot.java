package net.noiraude.creditseditor.bus;

import java.util.Optional;

import net.noiraude.libcredits.lang.LangDocument;

import org.jetbrains.annotations.NotNull;

/** Captured state of one locale, sufficient to restore it verbatim on undo. */
public record LocaleSnapshot(@NotNull Optional<LangDocument> doc, boolean pendingRemoval) {}
