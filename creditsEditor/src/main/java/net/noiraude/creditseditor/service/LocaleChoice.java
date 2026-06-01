package net.noiraude.creditseditor.service;

import org.jetbrains.annotations.NotNull;

/** Pairs a locale tag with its human-readable display name for UI presentation. */
public record LocaleChoice(@NotNull String basename, @NotNull String displayName) {

    @Override
    public @NotNull String toString() {
        return basename + "  " + displayName;
    }
}
