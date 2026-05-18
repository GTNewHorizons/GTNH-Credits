package net.noiraude.creditseditor.ui.component.mc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;

import net.noiraude.creditseditor.command.EditAbortedException;
import net.noiraude.creditseditor.mc.McFormatCode;
import net.noiraude.creditseditor.mc.McSelectionPresence;
import net.noiraude.creditseditor.ui.component.AnyChangeListener;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Editable {@link JTextPane} for Minecraft {@code §x}-formatted text. */
public final class McWysiwygPane extends JTextPane implements McFormatTarget {

    /** Property name fired on user-driven text changes. */
    public static final @NotNull String PROP_TEXT = "text";

    private final @NotNull McDocumentModel model;
    private boolean settingText;

    public McWysiwygPane(boolean multiLine) {
        this.model = new McDocumentModel(multiLine);
        setStyledDocument(model.getDocument());
        setBackground(UIManager.getColor("TextArea.background"));
        AbstractMcEditor.removeLafUndoManager(this);

        if (!multiLine) {
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "wysiwyg-no-enter");
            getActionMap().put("wysiwyg-no-enter", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {}
            });
        }

        getStyledDocument().addDocumentListener(new AnyChangeListener(this::onChanged));
        addCaretListener(e -> onCaretMoved());
    }

    // Always fill viewport width so word-wrap matches JTextArea behavior when wrapped in a JScrollPane.
    @Contract(pure = true)
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public void addCaretListener(@NotNull CaretListener listener) {
        super.addCaretListener(listener);
    }

    private void onCaretMoved() {
        if (settingText) return;
        if (getSelectionStart() == getSelectionEnd()) {
            model.syncPendingFromCaret(getCaretPosition());
            flushPendingToInputAttributes();
        }
    }

    private void onChanged() {
        if (!settingText) firePropertyChange(PROP_TEXT, null, getText());
    }

    private void flushPendingToInputAttributes() {
        MutableAttributeSet input = getInputAttributes();
        input.removeAttributes(input);
        input.addAttributes(McSwingStyle.toAttributes(model.getPendingCodes()));
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Replaces the displayed text by parsing {@code §x} codes into styled runs. */
    public void setStyledText(@NotNull String displayText) {
        settingText = true;
        try {
            model.setStyledText(displayText);
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot replace document text", ex);
        } finally {
            settingText = false;
        }
        setCaretPosition(0);
    }

    /** Replaces the displayed text with verbatim plain characters and no attributes. */
    public void setPlainText(@NotNull String text) {
        settingText = true;
        try {
            model.setPlainText(text);
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot replace document text", ex);
        } finally {
            settingText = false;
        }
        setCaretPosition(0);
    }

    /** Replaces the displayed styled text and fires user-input events. */
    public void setStyledTextAsUserInput(@NotNull String displayText) {
        try {
            model.setStyledText(displayText);
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot replace document text", ex);
        }
        setCaretPosition(0);
    }

    /** Replaces the displayed plain text and fires user-input events. */
    public void setPlainTextAsUserInput(@NotNull String text) {
        try {
            model.setPlainText(text);
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot replace document text", ex);
        }
        setCaretPosition(0);
    }

    /** Returns the current value as a {@code §x} string in display format. */
    @Override
    public @NotNull String getText() {
        try {
            return model.getText();
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot read document text", ex);
        }
    }

    @Override
    public @NotNull EnumSet<McFormatCode> getCaretStyle() {
        return model.getCaretStyle(getSelectionStart(), getSelectionEnd());
    }

    @Override
    public boolean hasSelection() {
        return getSelectionStart() != getSelectionEnd();
    }

    @Contract(" -> new")
    @Override
    public @NotNull McSelectionPresence computeSelectionPresence() {
        return model.computeSelectionPresence(getSelectionStart(), getSelectionEnd());
    }

    /** Registers an {@link UndoableEditListener} notified of user-driven document edits. */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        getDocument().addUndoableEditListener(e -> { if (!settingText) l.undoableEditHappened(e); });
    }

    /** Registers a typed text-change listener invoked after every user-driven document mutation. */
    public void addTextChangeListener(@NotNull Consumer<@NotNull String> listener) {
        getStyledDocument()
            .addDocumentListener(new AnyChangeListener(() -> { if (!settingText) listener.accept(getText()); }));
    }

    /** Connects a {@link McFormatToolbar} that drives this pane's formatting. */
    public void connectToolbar(@NotNull McFormatToolbar toolbar) {
        toolbar.connectTo(this);
    }

    // ------------------------------------------------------------------
    // Apply formatting: called by McFormatToolbar
    // ------------------------------------------------------------------

    @Override
    public void applyCode(@NotNull McFormatCode mc, boolean active) {
        try {
            model.applyCode(mc, active, getSelectionStart(), getSelectionEnd());
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot apply formatting code", ex);
        }
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }

    @Override
    public void applyReset() {
        try {
            model.applyReset(getSelectionStart(), getSelectionEnd());
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot reset selection formatting", ex);
        }
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }

    @Override
    public void applyColorReset() {
        try {
            model.applyColorReset(getSelectionStart(), getSelectionEnd());
        } catch (BadLocationException ex) {
            throw new EditAbortedException("Cannot reset selection color", ex);
        }
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }
}
