package net.noiraude.creditseditor.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/** Enumeration of the JDK's known locale tags, filtered for editor consumption. */
public final class KnownLocales {

    private KnownLocales() {}

    /** Returns the locale choice for a given basename. */
    public static @NotNull LocaleChoice choiceFor(@NotNull String basename) {
        Locale uiLocale = Locale.getDefault();
        int underscore = basename.indexOf('_');
        Locale locale = underscore < 0 ? Locale.of(basename)
            : Locale.of(basename.substring(0, underscore), basename.substring(underscore + 1));
        String name = locale.getDisplayName(uiLocale);
        return new LocaleChoice(basename, name.isEmpty() ? basename : name);
    }

    /** Returns the JDK-known locale choices not present in {@code exclude}. */
    public static @NotNull List<LocaleChoice> jdkBasenamesExcluding(@NotNull Set<String> exclude) {
        Locale uiLocale = Locale.getDefault();
        Collator collator = Collator.getInstance(uiLocale);
        Set<String> seenBasenames = new HashSet<>();
        List<LocaleChoice> choices = new ArrayList<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language.isEmpty() || country.isEmpty()) continue;
            String basename = language + "_" + country;
            if (exclude.contains(basename)) continue;
            if (!seenBasenames.add(basename)) continue;
            choices.add(new LocaleChoice(basename, locale.getDisplayName(uiLocale)));
        }
        choices.sort(Comparator.comparing(LocaleChoice::displayName, collator));
        return choices;
    }
}
