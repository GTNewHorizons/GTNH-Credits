package net.noiraude.creditseditor.service;

import java.util.List;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.AddPersonCommand;
import net.noiraude.creditseditor.command.impl.AddPersonRoleCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the undo/redo command corresponding to a TSV import plan.
 */
public final class TsvImportCommandFactory {

    @Contract(pure = true)
    private TsvImportCommandFactory() {}

    /**
     * Builds the command for the given import plan.
     *
     * @return the command to execute, or {@code null} when every line is a no-op
     */
    public static @Nullable Command build(@NotNull DocumentBus bus, @NotNull String categoryId,
        @NotNull List<ImportLine> lines) {
        CompoundCommand.Builder builder = new CompoundCommand.Builder("Import TSV (" + lines.size() + " persons)");
        for (ImportLine line : lines) {
            appendLine(builder, bus, categoryId, line);
        }
        return builder.isEmpty() ? null : builder.build();
    }

    private static void appendLine(@NotNull CompoundCommand.Builder builder, @NotNull DocumentBus bus,
        @NotNull String categoryId, @NotNull ImportLine line) {
        switch (line.action) {
            case CREATE -> {
                DocumentPerson person = new DocumentPerson(line.name);
                builder.add(new AddPersonCommand(bus, person));
                DocumentMembership membership = new DocumentMembership(categoryId);
                builder.add(new AddMembershipCommand(bus, person, membership));
                addRoles(builder, bus, person, membership, line.newRoles);
            }
            case ADD -> {
                DocumentPerson person = findPerson(bus, line.name);
                DocumentMembership membership = new DocumentMembership(categoryId);
                builder.add(new AddMembershipCommand(bus, person, membership));
                addRoles(builder, bus, person, membership, line.newRoles);
            }
            case COMPLETE -> {
                DocumentPerson person = findPerson(bus, line.name);
                DocumentMembership membership = person.memberships.stream()
                    .filter(m -> m.categoryId.equals(categoryId))
                    .findFirst()
                    .orElseThrow();
                addRoles(builder, bus, person, membership, line.newRoles);
            }
            case NO_CHANGE -> {
                // nothing to do
            }
        }
    }

    private static @NotNull DocumentPerson findPerson(@NotNull DocumentBus bus, @NotNull String name) {
        return bus.creditsDoc().persons.stream()
            .filter(p -> p.name.equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static void addRoles(@NotNull CompoundCommand.Builder builder, @NotNull DocumentBus bus,
        @NotNull DocumentPerson person, @NotNull DocumentMembership membership, @NotNull List<String> roles) {
        for (String role : roles) {
            builder.add(new AddPersonRoleCommand(bus, person, membership, role));
        }
    }
}
