package net.noiraude.creditseditor.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable editor representation of a credits' category.
 *
 * <p>
 * {@code displayName} and {@code description} hold raw strings as they appear in the lang
 * file (may contain Minecraft {@code §} formatting codes). They correspond to the lang keys
 * {@code credits.category.{sanitizedId}} and {@code credits.category.{sanitizedId}.detail}
 * respectively.
 */
public final class EditorCategory {

    /** Category id as stored in {@code credits.json}. */
    public String id;

    /**
     * Raw display name (lang value for {@code credits.category.{key}}).
     * Empty string means no lang entry exists yet.
     */
    public String displayName;

    /**
     * Raw description text (lang value for {@code credits.category.{key}.detail}).
     * Paragraphs are separated by {@code \n}. Empty string means no entry exists yet.
     */
    public String description;

    /** Semantic class markers ({@code person}, {@code role}, {@code detail}). */
    public Set<String> classes;

    public EditorCategory(String id) {
        this.id = id;
        this.displayName = "";
        this.description = "";
        this.classes = new LinkedHashSet<>();
    }
}
