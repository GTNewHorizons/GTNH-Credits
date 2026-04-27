package net.noiraude.creditseditor.ui.component.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.function.IntPredicate;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.CommandExecutor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic multi-item reorder handler for a {@link JList} using {@link DropMode#INSERT}.
 *
 * <p>
 * Captures all selected UI indices, delegates drag/drop admissibility to two predicates,
 * and builds a single {@link Command} through the supplied factory when a drop is accepted.
 * The factory receives the indices sorted ascending so the dragged items preserve their
 * original relative order when reinserted at the drop location. Commands are dispatched
 * through the shared {@link CommandExecutor} so reorders participate in undo/redo.
 *
 * @param <E> list element type
 */
public final class ListReorderTransferHandler<E> extends TransferHandler {

    /** Factory that builds the reorder {@link Command} for a given multi-drag / drop pair. */
    @FunctionalInterface
    public interface CommandFactory {

        /**
         * @param fromIndices UI list indices of the dragged elements, sorted ascending
         * @param dropIndex   UI insertion index reported by {@link JList.DropLocation}
         *                    ({@code 0..size}, in terms of the list model <em>before</em> removal)
         * @return the command to execute, or {@code null} to cancel the drop
         */
        @Nullable
        Command create(int @NotNull [] fromIndices, int dropIndex);
    }

    private final @NotNull JList<E> list;
    private final @NotNull CommandExecutor onCommand;
    private final @NotNull CommandFactory commandFactory;
    private final @NotNull IntPredicate canDragFrom;
    private final @NotNull IntPredicate canDropAt;

    private int @NotNull [] dragFromIndices = new int[0];

    /**
     * @param list           the list the handler is attached to
     * @param onCommand      executor that dispatches the reorder command
     * @param commandFactory builds the reorder command from UI drag/drop indices
     * @param canDragFrom    index filter; the drag is refused if any selected index fails
     * @param canDropAt      index filter; return {@code false} to refuse a drop at that insert index
     */
    public ListReorderTransferHandler(@NotNull JList<E> list, @NotNull CommandExecutor onCommand,
        @NotNull CommandFactory commandFactory, @NotNull IntPredicate canDragFrom, @NotNull IntPredicate canDropAt) {
        this.list = list;
        this.onCommand = onCommand;
        this.commandFactory = commandFactory;
        this.canDragFrom = canDragFrom;
        this.canDropAt = canDropAt;
    }

    @Contract(pure = true)
    @Override
    public int getSourceActions(@NotNull JComponent c) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(@NotNull JComponent c) {
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0) return null;
        for (int i : indices) {
            if (!canDragFrom.test(i)) return null;
        }
        int[] sorted = indices.clone();
        Arrays.sort(sorted);
        dragFromIndices = sorted;
        return new StringSelection(Arrays.toString(sorted));
    }

    @Override
    public boolean canImport(@NotNull TransferSupport support) {
        if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
        JList.DropLocation loc = (JList.DropLocation) support.getDropLocation();
        return canDropAt.test(loc.getIndex());
    }

    @Override
    public boolean importData(@NotNull TransferSupport support) {
        if (!canImport(support) || dragFromIndices.length == 0) return false;
        int dropIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
        if (isNoOp(dragFromIndices, dropIndex)) return false;
        Command cmd = commandFactory.create(dragFromIndices, dropIndex);
        if (cmd == null) return false;
        onCommand.execute(cmd);
        return true;
    }

    @Override
    protected void exportDone(@NotNull JComponent source, @Nullable Transferable data, int action) {
        dragFromIndices = new int[0];
    }

    /**
     * A move is a no-op when the selection forms a contiguous block and the drop point lies
     * inside or immediately after that block. Discontinuous selections always change at
     * least one element's neighbor, so they are never no-ops.
     */
    @Contract(pure = true)
    private static boolean isNoOp(int @NotNull [] sortedIndices, int dropIndex) {
        int first = sortedIndices[0];
        int last = sortedIndices[sortedIndices.length - 1];
        boolean contiguous = last - first + 1 == sortedIndices.length;
        return contiguous && dropIndex >= first && dropIndex <= last + 1;
    }
}
