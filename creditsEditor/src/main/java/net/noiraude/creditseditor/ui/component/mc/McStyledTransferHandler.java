package net.noiraude.creditseditor.ui.component.mc;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McText;

import org.jetbrains.annotations.NotNull;

/** Clipboard handler for the rendered (WYSIWYG) form: serializes attributes to §-codes. */
final class McStyledTransferHandler extends TransferHandler {

    @Override
    public int getSourceActions(@NotNull JComponent c) {
        return pane(c).isEditable() ? COPY_OR_MOVE : COPY;
    }

    @Override
    protected @NotNull Transferable createTransferable(@NotNull JComponent c) {
        return new StringSelection(
            pane(c).serializeSelectionRendered()
                .orElse(""));
    }

    @Override
    protected void exportDone(@NotNull JComponent source, @NotNull Transferable data, int action) {
        if (action != MOVE) return;
        McWysiwygPane p = pane(source);
        if (!p.isEditable()) return;
        int start = p.getSelectionStart();
        int end = p.getSelectionEnd();
        if (start == end) return;
        try {
            p.getDocument()
                .remove(start, end - start);
        } catch (BadLocationException ignored) {}
    }

    @Override
    public boolean canImport(@NotNull TransferSupport support) {
        if (!(support.getComponent() instanceof McWysiwygPane p)) return false;
        return p.isEditable() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(@NotNull TransferSupport support) {
        if (!canImport(support)) return false;
        McWysiwygPane p = pane(support.getComponent());
        String text;
        try {
            text = (String) support.getTransferable()
                .getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return false;
        }

        AttributeSet preserved = copyOf(p.getInputAttributes());
        StyledDocument doc = (StyledDocument) p.getDocument();
        int start = p.getSelectionStart();
        int end = p.getSelectionEnd();
        try {
            if (start != end) doc.remove(start, end - start);
            int offset = start;
            for (McText.Segment seg : McText.parse(text)) {
                doc.insertString(offset, seg.text, McSwingStyle.toAttributes(seg.codes));
                offset += seg.text.length();
            }
            p.setCaretPosition(offset);
        } catch (BadLocationException ex) {
            return false;
        }

        MutableAttributeSet input = p.getInputAttributes();
        input.removeAttributes(input);
        input.addAttributes(preserved);
        return true;
    }

    private static @NotNull McWysiwygPane pane(@NotNull Component c) {
        return (McWysiwygPane) c;
    }

    private static @NotNull AttributeSet copyOf(@NotNull AttributeSet src) {
        return new SimpleAttributeSet(src);
    }
}
