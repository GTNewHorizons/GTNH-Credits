package net.noiraude.creditseditor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root mutable editor model holding all categories and persons.
 *
 * <p>
 * The order of {@code categories} is significant: it controls the display order in-game.
 * The order of {@code persons} is the editing order and may differ from the order in
 * the saved {@code credits.json} (the parser sorts persons by name on loading).
 */
public final class EditorModel {

    /** Ordered list of categories. */
    public final List<EditorCategory> categories;

    /** List of persons. */
    public final List<EditorPerson> persons;

    public EditorModel() {
        this.categories = new ArrayList<>();
        this.persons = new ArrayList<>();
    }
}
