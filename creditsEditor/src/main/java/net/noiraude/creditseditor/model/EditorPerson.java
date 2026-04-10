package net.noiraude.creditseditor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable editor representation of a person in the credits.
 *
 * <p>
 * {@code name} holds the raw string as stored in {@code credits.json} and may contain
 * Minecraft {@code §} formatting codes. The ordered list of {@link EditorMembership}
 * entries reflects the person's category associations and their roles within each.
 */
public final class EditorPerson {

    /**
     * Raw person name as stored in {@code credits.json}.
     * May contain Minecraft {@code §} formatting codes.
     */
    public String name;

    /** Ordered list of category memberships. */
    public final List<EditorMembership> memberships;

    public EditorPerson(String name) {
        this.name = name;
        this.memberships = new ArrayList<>();
    }
}
