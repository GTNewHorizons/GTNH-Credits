package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.List;

import net.noiraude.creditseditor.bus.DocumentBus;
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
public final class RemoveCategoryCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentCategory category;

    private int savedIndex;
    private final @NotNull List<AffectedMembership> saved = new ArrayList<>();

    private record AffectedMembership(DocumentPerson person, int index, DocumentMembership membership) {}

    public RemoveCategoryCommand(@NotNull DocumentBus bus, @NotNull DocumentCategory category) {
        this.bus = bus;
        this.category = category;
    }

    @Override
    public void execute() {
        CreditsDocument doc = bus.creditsDoc();
        savedIndex = doc.categories.indexOf(category);
        saved.clear();
        for (DocumentPerson person : doc.persons) {
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
        doc.categories.remove(savedIndex);
        bus.fireCategoriesChanged();
        if (!saved.isEmpty()) bus.firePersonsChanged();
    }

    @Override
    public void undo() {
        CreditsDocument doc = bus.creditsDoc();
        doc.categories.add(savedIndex, category);
        for (AffectedMembership am : saved) {
            int idx = Math.min(am.index(), am.person().memberships.size());
            am.person().memberships.add(idx, am.membership());
        }
        bus.fireCategoriesChanged();
        if (!saved.isEmpty()) bus.firePersonsChanged();
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove category " + category.id;
    }
}
