package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

/** Replaces one label without changing its order; undo restores the original source and value. */
public final class EditPersonRoleCommand extends AbstractCommand {

    private final DocumentBus bus;
    private final DocumentPerson person;
    private final DocumentMembership membership;
    private final int index;
    private final DocumentRole replacement;
    private DocumentRole original;

    public EditPersonRoleCommand(DocumentBus bus, DocumentPerson person, DocumentMembership membership, int index,
        DocumentRole replacement) {
        this.bus = bus;
        this.person = person;
        this.membership = membership;
        this.index = index;
        this.replacement = replacement;
    }

    @Override
    public void execute() {
        original = membership.roles.set(index, replacement);
        bus.firePersonChanged(person);
    }

    @Override
    public void undo() {
        membership.roles.set(index, original);
        bus.firePersonChanged(person);
    }

    @Override
    public String getDisplayName() {
        return "Change role in " + membership.categoryId;
    }
}
