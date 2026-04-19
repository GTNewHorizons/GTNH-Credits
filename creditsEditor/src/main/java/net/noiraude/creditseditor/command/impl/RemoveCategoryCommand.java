package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.List;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a category from the document and strips every person's membership in that category.
 *
 * <p>
 * Undo restores the category at its original position and each stripped membership at its
 * original position within the respective person's membership list.
 */
public final class RemoveCategoryCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final @NotNull DocumentCategory category;

    private int savedIndex;
    private final @NotNull List<AffectedMembership> saved = new ArrayList<>();

    private record AffectedMembership(DocumentPerson person, int index, DocumentMembership membership) {}

    public RemoveCategoryCommand(@NotNull CreditsDocument creditsDoc, @NotNull DocumentCategory category) {
        this.creditsDoc = creditsDoc;
        this.category = category;
    }

    @Override
    public void execute() {
        savedIndex = creditsDoc.categories.indexOf(category);
        saved.clear();
        for (DocumentPerson person : creditsDoc.persons) {
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
        creditsDoc.categories.remove(savedIndex);
    }

    @Override
    public void undo() {
        creditsDoc.categories.add(savedIndex, category);
        for (AffectedMembership am : saved) {
            int idx = Math.min(am.index(), am.person().memberships.size());
            am.person().memberships.add(idx, am.membership());
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove category " + category.id;
    }
}
