package net.noiraude.creditseditor.ui.component.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.TransferHandler;

import net.noiraude.creditseditor.service.LocaleChoice;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Multi-item move handler between two disjoint {@link JList}s of {@link LocaleChoice}. */
public final class LocaleSetMoveTransferHandler extends TransferHandler {

    /** Receives the locale codes that were dropped onto the host list. */
    @FunctionalInterface
    public interface OnImport {

        void accept(@NotNull List<String> basenames);
    }

    private final @NotNull JList<LocaleChoice> list;
    private final @NotNull OnImport onImport;

    public LocaleSetMoveTransferHandler(@NotNull JList<LocaleChoice> list, @NotNull OnImport onImport) {
        this.list = list;
        this.onImport = onImport;
    }

    @Contract(pure = true)
    @Override
    public int getSourceActions(@NotNull JComponent c) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(@NotNull JComponent c) {
        List<LocaleChoice> values = list.getSelectedValuesList();
        if (values.isEmpty()) return null;
        String payload = values.stream()
            .map(LocaleChoice::basename)
            .collect(Collectors.joining("\n"));
        return new StringSelection(payload);
    }

    @Override
    public boolean canImport(@NotNull TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(@NotNull TransferSupport support) {
        if (!canImport(support)) return false;
        List<String> codes = readCodes(support);
        if (codes.isEmpty()) return false;
        Set<String> present = collectPresent(list.getModel());
        List<String> toMove = codes.stream()
            .filter(c -> !present.contains(c))
            .toList();
        if (toMove.isEmpty()) return false;
        onImport.accept(toMove);
        return true;
    }

    private static @NotNull List<String> readCodes(@NotNull TransferSupport support) {
        try {
            Object data = support.getTransferable()
                .getTransferData(DataFlavor.stringFlavor);
            String text = data.toString();
            if (text.isEmpty()) return List.of();
            return List.of(text.split("\n"));
        } catch (UnsupportedFlavorException | IOException ex) {
            return List.of();
        }
    }

    private static @NotNull Set<String> collectPresent(@NotNull ListModel<LocaleChoice> model) {
        Set<String> out = new HashSet<>();
        for (int i = 0; i < model.getSize(); i++) {
            out.add(
                model.getElementAt(i)
                    .basename());
        }
        return out;
    }
}
