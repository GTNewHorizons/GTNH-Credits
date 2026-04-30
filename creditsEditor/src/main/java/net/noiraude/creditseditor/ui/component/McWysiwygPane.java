package net.noiraude.creditseditor.ui.component;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.MutableAttributeSet;

import net.noiraude.creditseditor.mc.McFormatCode;
import net.noiraude.creditseditor.mc.McSelectionPresence;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editable {@link JTextPane} that renders and edits Minecraft {@code §x} formatting codes in
 * WYSIWYG style. All document state and {@code §x} conversion live in the underlying
 * {@link McDocumentModel}; this class is the Swing wiring on top.
 *
 * <p>
 * In multi-line mode, paragraphs are separated by the literal two-character sequence
 * {@code \n} (backslash-n) in {@link #getText}/{@link #setText}; internally the document uses
 * actual {@code '\n'} characters. In single-line mode Enter is suppressed.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes due
 * to user input (typed text or toolbar formatting). Programmatic {@link #setText} calls do not
 * fire the event.
 *
 * <p>
 * Connect a {@link McFormatToolbar} via {@link #connectToolbar} to keep toolbar state in sync
 * with the caret and route toolbar actions back to this pane.
 */
public final class McWysiwygPane extends JTextPane implements McFormatTarget {

    /** Property name fired when the text value changes due to user input. */
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

    private void onCaretMoved() {
        if (settingText || model.isRawMode()) return;
        if (getSelectionStart() == getSelectionEnd()) {
            model.syncPendingFromCaret(getCaretPosition());
            flushPendingToInputAttributes();
        }
    }

    private void onChanged() {
        if (!settingText) firePropertyChange(PROP_TEXT, null, getText());
    }

    /**
     * Writes the model's pending-codes carry to the Swing input-attributes map so that the next
     * character typed inherits the carry.
     */
    private void flushPendingToInputAttributes() {
        MutableAttributeSet input = getInputAttributes();
        input.removeAttributes(input);
        input.addAttributes(McSwingStyle.toAttributes(model.getPendingCodes()));
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Switches between rendered (WYSIWYG) and raw (literal {@code §x} codes) display mode.
     *
     * <p>
     * The current text is preserved across the switch. Does not fire a {@code "text"} event.
     */
    public void setRawMode(boolean raw) {
        settingText = true;
        try {
            model.setRawMode(raw);
        } finally {
            settingText = false;
        }
        setCaretPosition(0);
    }

    @Override
    public void setText(@Nullable String displayText) {
        settingText = true;
        try {
            model.setText(displayText);
        } finally {
            settingText = false;
        }
        setCaretPosition(0);
    }

    /**
     * Returns the current value as a {@code §x} string in display format.
     *
     * <p>
     * In multi-line mode, paragraphs are separated by actual {@code '\n'} characters.
     * Backslash characters are literal.
     */
    @Override
    public @NotNull String getText() {
        return model.getText();
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

    /**
     * Registers an {@link UndoableEditListener} that is notified of document edits caused by user
     * input. Events generated by programmatic {@link #setText} calls are suppressed.
     */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        getDocument().addUndoableEditListener(e -> { if (!settingText) l.undoableEditHappened(e); });
    }

    /**
     * Connects a {@link McFormatToolbar} so its state tracks the caret and its buttons apply
     * formatting to this pane.
     */
    public void connectToolbar(@NotNull McFormatToolbar toolbar) {
        toolbar.connectTo(this);
    }

    // ------------------------------------------------------------------
    // Apply formatting: called by McFormatToolbar
    // ------------------------------------------------------------------

    @Override
    public void applyCode(@NotNull McFormatCode mc, boolean active) {
        model.applyCode(mc, active, getSelectionStart(), getSelectionEnd());
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }

    @Override
    public void applyReset() {
        model.applyReset(getSelectionStart(), getSelectionEnd());
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }

    @Override
    public void applyColorReset() {
        model.applyColorReset(getSelectionStart(), getSelectionEnd());
        flushPendingToInputAttributes();
        requestFocusInWindow();
    }
}
