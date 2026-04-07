package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.List;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

/**
 * Removes a category from the model and strips every person's membership in that category.
 *
 * <p>
 * Undo restores the category at its original position and each stripped membership at its
 * original position within the respective person's membership list.
 */
public final class RemoveCategoryCommand implements Command {

    private final EditorModel model;
    private final EditorCategory category;

    private int savedIndex;
    private final List<AffectedMembership> saved = new ArrayList<>();

    private record AffectedMembership(EditorPerson person, int index, EditorMembership membership) {}

    public RemoveCategoryCommand(EditorModel model, EditorCategory category) {
        this.model = model;
        this.category = category;
    }

    @Override
    public void execute() {
        savedIndex = model.categories.indexOf(category);
        saved.clear();
        for (EditorPerson person : model.persons) {
            for (int i = 0; i < person.memberships.size(); i++) {
                if (person.memberships.get(i).categoryId.equals(category.id)) {
                    saved.add(new AffectedMembership(person, i, person.memberships.get(i)));
                    break; // at most one membership per category per person
                }
            }
        }
        for (AffectedMembership am : saved) {
            am.person().memberships.remove(am.membership());
        }
        model.categories.remove(savedIndex);
    }

    @Override
    public void undo() {
        model.categories.add(savedIndex, category);
        for (AffectedMembership am : saved) {
            int idx = Math.min(am.index(), am.person().memberships.size());
            am.person().memberships.add(idx, am.membership());
        }
    }

    @Override
    public String getDisplayName() {
        return "Remove category " + category.id;
    }
}
