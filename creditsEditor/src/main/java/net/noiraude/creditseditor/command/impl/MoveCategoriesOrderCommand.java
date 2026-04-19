package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moves one or more categories to a new position in the ordered category list, preserving
 * their relative order.
 *
 * <p>
 * {@code fromIndices} are the source positions in {@code creditsDoc.categories} and
 * {@code dropIndex} is the insertion point as reported by a {@link javax.swing.DropMode#INSERT}
 * drop location. Discontinuous selections are reinserted as a contiguous block at the drop
 * position and keep their original relative order.
 */
public final class MoveCategoriesOrderCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final int @NotNull [] fromIndices;
    private final int dropIndex;
    private @Nullable List<DocumentCategory> originalOrder;

    public MoveCategoriesOrderCommand(@NotNull CreditsDocument creditsDoc, int @NotNull [] fromIndices, int dropIndex) {
        this.creditsDoc = creditsDoc;
        int[] sorted = fromIndices.clone();
        Arrays.sort(sorted);
        this.fromIndices = sorted;
        this.dropIndex = dropIndex;
    }

    @Override
    public void execute() {
        originalOrder = new ArrayList<>(creditsDoc.categories);
        List<DocumentCategory> extracted = new ArrayList<>(fromIndices.length);
        for (int i = fromIndices.length - 1; i >= 0; i--) {
            extracted.addFirst(creditsDoc.categories.remove(fromIndices[i]));
        }
        int below = 0;
        for (int idx : fromIndices) {
            if (idx < dropIndex) below++;
        }
        int insertAt = dropIndex - below;
        creditsDoc.categories.addAll(insertAt, extracted);
    }

    @Override
    public void undo() {
        if (originalOrder == null) return;
        creditsDoc.categories.clear();
        creditsDoc.categories.addAll(originalOrder);
    }

    @Override
    public @NotNull String getDisplayName() {
        return fromIndices.length == 1 ? "Move category" : "Move " + fromIndices.length + " categories";
    }
}
