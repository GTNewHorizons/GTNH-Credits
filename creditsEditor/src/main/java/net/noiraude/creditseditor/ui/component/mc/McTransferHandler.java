package net.noiraude.creditseditor.ui.component.mc;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CompoundEdit;

import org.jetbrains.annotations.NotNull;

/** Base for clipboard handlers attached to {@link McWysiwygPane}. */
abstract class McTransferHandler extends TransferHandler {

    @Override
    public final int getSourceActions(@NotNull JComponent c) {
        return pane(c).isEditable() ? COPY_OR_MOVE : COPY;
    }

    @Override
    protected final @NotNull Transferable createTransferable(@NotNull JComponent c) {
        return new StringSelection(serializeSelection(pane(c)));
    }

    @Override
    protected final void exportDone(@NotNull JComponent source, @NotNull Transferable data, int action) {
        if (action != MOVE) return;
        McWysiwygPane p = pane(source);
        if (p.isEditable()) p.replaceSelection("");
    }

    @Override
    public final boolean canImport(@NotNull TransferSupport support) {
        if (!(support.getComponent() instanceof McWysiwygPane p)) return false;
        return p.isEditable() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public final boolean importData(@NotNull TransferSupport support) {
        if (!canImport(support)) return false;
        McWysiwygPane p = pane(support.getComponent());
        String text;
        try {
            text = (String) support.getTransferable()
                .getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return false;
        }
        return applyAsSingleEdit(p, () -> insertText(p, text));
    }

    /** Returns the clipboard payload for the current selection in {@code pane}. */
    protected abstract @NotNull String serializeSelection(@NotNull McWysiwygPane pane);

    /** Inserts clipboard {@code text} into {@code pane} at the caret. */
    protected abstract void insertText(@NotNull McWysiwygPane pane, @NotNull String text) throws BadLocationException;

    @FunctionalInterface
    protected interface DocumentEditBody {

        void run() throws BadLocationException;
    }

    /** Performs {@code body} so all UndoableEdits collapse into one event for the document's listeners. */
    private static boolean applyAsSingleEdit(@NotNull McWysiwygPane pane, @NotNull DocumentEditBody body) {
        AbstractDocument doc = (AbstractDocument) pane.getDocument();
        UndoableEditListener[] listeners = doc.getListeners(UndoableEditListener.class);
        for (UndoableEditListener l : listeners) doc.removeUndoableEditListener(l);
        CompoundEdit combined = new CompoundEdit();
        UndoableEditListener collector = e -> combined.addEdit(e.getEdit());
        doc.addUndoableEditListener(collector);
        boolean ok = true;
        try {
            body.run();
        } catch (BadLocationException ex) {
            ok = false;
        } finally {
            doc.removeUndoableEditListener(collector);
            for (UndoableEditListener l : listeners) doc.addUndoableEditListener(l);
        }
        combined.end();
        if (combined.canUndo()) {
            UndoableEditEvent event = new UndoableEditEvent(doc, combined);
            for (UndoableEditListener l : listeners) l.undoableEditHappened(event);
        }
        return ok;
    }

    private static @NotNull McWysiwygPane pane(@NotNull Component c) {
        return (McWysiwygPane) c;
    }
}
