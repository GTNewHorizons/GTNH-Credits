package net.noiraude.creditseditor.ui.component.mc;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.jetbrains.annotations.NotNull;

/** Clipboard handler for the raw form: verbatim plain-text copy and paste. */
final class McPlainTransferHandler extends TransferHandler {

    @Override
    public int getSourceActions(@NotNull JComponent c) {
        return pane(c).isEditable() ? COPY_OR_MOVE : COPY;
    }

    @Override
    protected @NotNull Transferable createTransferable(@NotNull JComponent c) {
        String sel = pane(c).getSelectedText();
        return new StringSelection(sel == null ? "" : sel);
    }

    @Override
    protected void exportDone(@NotNull JComponent source, @NotNull Transferable data, int action) {
        if (action != MOVE) return;
        McWysiwygPane p = pane(source);
        if (p.isEditable()) p.replaceSelection("");
    }

    @Override
    public boolean canImport(@NotNull TransferSupport support) {
        if (!(support.getComponent() instanceof McWysiwygPane p)) return false;
        return p.isEditable() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(@NotNull TransferSupport support) {
        if (!canImport(support)) return false;
        try {
            String text = (String) support.getTransferable()
                .getTransferData(DataFlavor.stringFlavor);
            pane(support.getComponent()).replaceSelection(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static @NotNull McWysiwygPane pane(@NotNull Component c) {
        return (McWysiwygPane) c;
    }
}
