package net.noiraude.creditseditor.ui.component.mc;

import org.jetbrains.annotations.NotNull;

/** Clipboard handler for the raw form: verbatim plain-text copy and paste. */
final class McPlainTransferHandler extends McTransferHandler {

    @Override
    protected @NotNull String serializeSelection(@NotNull McWysiwygPane pane) {
        String sel = pane.getSelectedText();
        return sel == null ? "" : sel;
    }

    @Override
    protected void insertText(@NotNull McWysiwygPane pane, @NotNull String text) {
        pane.replaceSelection(text);
    }
}
