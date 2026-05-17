package net.noiraude.creditseditor.bus;

import org.jetbrains.annotations.NotNull;

/** Mutator surface for the locale set of an editing session, with snapshot-based restore. */
public interface LocaleEditor {

    void addLocale(@NotNull String locale);

    void removeLocale(@NotNull String locale);

    @NotNull
    LocaleSnapshot snapshotLocale(@NotNull String locale);

    void applyLocaleSnapshot(@NotNull String locale, @NotNull LocaleSnapshot snapshot);
}
