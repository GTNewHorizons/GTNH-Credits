package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.noiraude.creditseditor.bus.DocumentBus;
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
public final class MoveCategoriesOrderCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final int @NotNull [] fromIndices;
    private final int dropIndex;
    private @Nullable List<DocumentCategory> originalOrder;

    public MoveCategoriesOrderCommand(@NotNull DocumentBus bus, int @NotNull [] fromIndices, int dropIndex) {
        this.bus = bus;
        int[] sorted = fromIndices.clone();
        Arrays.sort(sorted);
        this.fromIndices = sorted;
        this.dropIndex = dropIndex;
    }

    @Override
    public void execute() {
        CreditsDocument doc = bus.creditsDoc();
        originalOrder = new ArrayList<>(doc.categories);
        ListReorderHelper.move(doc.categories, fromIndices, dropIndex);
        bus.fireCategoriesChanged();
    }

    @Override
    public void undo() {
        if (originalOrder == null) return;
        CreditsDocument doc = bus.creditsDoc();
        doc.categories.clear();
        doc.categories.addAll(originalOrder);
        bus.fireCategoriesChanged();
    }

    @Override
    public @NotNull String getDisplayName() {
        return fromIndices.length == 1 ? "Move category" : "Move " + fromIndices.length + " categories";
    }
}
