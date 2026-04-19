package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moves one or more roles to a new position within a membership's role list, preserving
 * their relative order.
 *
 * <p>
 * {@code fromIndices} are the source positions and {@code dropIndex} is the insertion point
 * as reported by a {@link javax.swing.DropMode#INSERT} drop location (i.e., the index in the
 * original list before any drag item is removed). When the selection is discontinuous, the
 * dragged items are reinserted as a contiguous block at the computed drop position and keep
 * their original relative order.
 */
public final class MoveRolesOrderCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentMembership membership;
    private final int @NotNull [] fromIndices;
    private final int dropIndex;
    private @Nullable List<String> originalOrder;

    public MoveRolesOrderCommand(@NotNull DocumentMembership membership, int @NotNull [] fromIndices, int dropIndex) {
        this.membership = membership;
        int[] sorted = fromIndices.clone();
        Arrays.sort(sorted);
        this.fromIndices = sorted;
        this.dropIndex = dropIndex;
    }

    @Override
    public void execute() {
        originalOrder = new ArrayList<>(membership.roles);
        List<String> extracted = new ArrayList<>(fromIndices.length);
        for (int i = fromIndices.length - 1; i >= 0; i--) {
            extracted.addFirst(membership.roles.remove(fromIndices[i]));
        }
        int below = 0;
        for (int idx : fromIndices) {
            if (idx < dropIndex) below++;
        }
        int insertAt = dropIndex - below;
        membership.roles.addAll(insertAt, extracted);
    }

    @Override
    public void undo() {
        if (originalOrder == null) return;
        membership.roles.clear();
        membership.roles.addAll(originalOrder);
    }

    @Override
    public @NotNull String getDisplayName() {
        return fromIndices.length == 1 ? "Move role in " + membership.categoryId
            : "Move " + fromIndices.length + " roles in " + membership.categoryId;
    }
}
