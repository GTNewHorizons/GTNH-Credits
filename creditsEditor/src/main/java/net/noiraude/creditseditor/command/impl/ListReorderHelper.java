package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Reorders a selection of items within a list to a new insertion position, preserving
 * the selection's original relative order.
 *
 * <p>
 * Shared by the move-by-drop commands ({@link MoveCategoriesOrderCommand},
 * {@link MoveRolesOrderCommand}) which would otherwise duplicate this extract-and-reinsert
 * logic verbatim.
 */
final class ListReorderHelper {

    private ListReorderHelper() {}

    /**
     * Removes the items at {@code sortedFromIndices} from {@code list} and reinserts them
     * as a contiguous block at the position equivalent to a {@link javax.swing.DropMode#INSERT}
     * drop at {@code dropIndex} in the pre-removal list. A discontinuous selection becomes a
     * contiguous block; relative order is preserved.
     *
     * @param list              list mutated in place
     * @param sortedFromIndices source positions in ascending order; must reference distinct,
     *                          valid indices in {@code list}
     * @param dropIndex         insertion index measured against the original list (i.e. before
     *                          any items are removed), as reported by Swing's
     *                          {@link javax.swing.DropMode#INSERT}
     */
    static <T> void move(@NotNull List<T> list, int @NotNull [] sortedFromIndices, int dropIndex) {
        List<T> extracted = new ArrayList<>(sortedFromIndices.length);
        for (int i = sortedFromIndices.length - 1; i >= 0; i--) {
            extracted.addFirst(list.remove(sortedFromIndices[i]));
        }
        int below = 0;
        for (int idx : sortedFromIndices) {
            if (idx < dropIndex) below++;
        }
        list.addAll(dropIndex - below, extracted);
    }
}
