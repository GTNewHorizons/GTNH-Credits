package net.noiraude.creditseditor.ui.component.mc;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McText;

import org.jetbrains.annotations.NotNull;

/** Clipboard handler for the rendered (WYSIWYG) form: serializes attributes to §-codes. */
final class McStyledTransferHandler extends McTransferHandler {

    @Override
    protected @NotNull String serializeSelection(@NotNull McWysiwygPane pane) {
        return pane.serializeSelectionRendered()
            .orElse("");
    }

    @Override
    protected void insertText(@NotNull McWysiwygPane pane, @NotNull String text) throws BadLocationException {
        AttributeSet preserved = new SimpleAttributeSet(pane.getInputAttributes());
        StyledDocument doc = (StyledDocument) pane.getDocument();
        int start = pane.getSelectionStart();
        int end = pane.getSelectionEnd();
        if (start != end) doc.remove(start, end - start);
        int offset = start;
        for (McText.Segment seg : McText.parse(text)) {
            doc.insertString(offset, seg.text, McSwingStyle.toAttributes(seg.codes));
            offset += seg.text.length();
        }
        pane.setCaretPosition(offset);
        MutableAttributeSet input = pane.getInputAttributes();
        input.removeAttributes(input);
        input.addAttributes(preserved);
    }
}
