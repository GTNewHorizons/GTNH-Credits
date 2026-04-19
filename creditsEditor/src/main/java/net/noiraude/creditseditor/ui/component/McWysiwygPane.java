package net.noiraude.creditseditor.ui.component;

import java.awt.event.ActionEvent;
import java.util.EnumSet;

import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McFormatCode;
import net.noiraude.creditseditor.mc.McSelectionPresence;
import net.noiraude.creditseditor.mc.McText;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editable {@link JTextPane} that renders and edits Minecraft {@code §x} formatting codes in
 * WYSIWYG style, wrapped in a {@link JScrollPane}.
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
 *
 * <h3>Pending-codes carry</h3>
 *
 * <p>
 * A {@link McFormatCode} carry ({@code pendingCodes}) acts as the "input style" for the next
 * character to be typed. When the caret moves (no selection), it is re-initialized from the
 * element at {@code caretPos - 1} so that typing continues in the style of the preceding
 * character. Toolbar actions modify the carry directly rather than reading back stale element
 * attributes. The carry is flushed to the Swing input-attributes map before each flush so that
 * characters inserted by Swing's key handler inherit it automatically.
 */
public final class McWysiwygPane extends JScrollPane implements McFormatTarget {

    /** Property name fired when the text value changes due to user input. */
    public static final @NotNull String PROP_TEXT = "text";

    private final @NotNull JTextPane pane = new JTextPane() {

        @Contract(pure = true)
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // always fill viewport width so word-wrap matches JTextArea behavior
        }
    };
    private final boolean multiLine;
    private boolean settingText;
    private boolean rawMode;

    /**
     * Carry accumulating the active {@link McFormatCode}s for the next character to be typed.
     *
     * <p>
     * Kept as an {@link EnumSet}{@code <McFormatCode>} rather than delegating to Swing's built-in
     * {@link JTextPane#getInputAttributes()} map for two reasons:
     *
     * <ol>
     * <li><b>Domain match.</b> The toolbar API operates entirely in {@link McFormatCode} values.
     * Using the Swing map would require a {@code McSwingStyle.fromAttributes} round-trip on every
     * toolbar read and a {@code McSwingStyle.applyTo} translation on every toolbar writing. Keeping
     * the carry in the same domain removes all conversion overhead.</li>
     * <li><b>Custom attribute.</b> The obfuscated flag has no
     * {@link javax.swing.text.StyleConstants} equivalent; the Swing map stores it as an opaque
     * key that nothing in Swing understands. Representing it as an enum constant in the carry
     * makes its semantics first-class.</li>
     * </ol>
     */
    private @NotNull EnumSet<McFormatCode> pendingCodes = EnumSet.noneOf(McFormatCode.class);

    public McWysiwygPane(boolean multiLine) {
        this.multiLine = multiLine;

        pane.setBackground(UIManager.getColor("TextArea.background"));
        setViewportView(pane);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        AbstractMcEditor.removeLafUndoManager(pane);

        if (!multiLine) {
            pane.getInputMap()
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "wysiwyg-no-enter");
            pane.getActionMap()
                .put("wysiwyg-no-enter", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {}
                });
        }

        pane.getStyledDocument()
            .addDocumentListener(new AnyChangeListener(this::onChanged));

        // Reinitialize the carry whenever the caret moves with no selection (rendered mode only).
        pane.addCaretListener(e -> {
            if (!settingText && !rawMode && pane.getSelectionStart() == pane.getSelectionEnd()) {
                syncPendingFromCaret();
            }
        });
    }

    private void onChanged() {
        if (!settingText) {
            firePropertyChange(PROP_TEXT, null, getText());
        }
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
        if (this.rawMode == raw) return;
        String text = getText();
        this.rawMode = raw;
        pendingCodes.clear();
        setText(text);
    }

    public void setText(@Nullable String displayText) {
        settingText = true;
        try {
            StyledDocument doc = pane.getStyledDocument();
            doc.remove(0, doc.getLength());
            String content = displayText != null ? displayText : "";
            if (rawMode) {
                doc.insertString(0, content, null);
            } else if (multiLine) {
                String[] paras = content.split("\n", -1);
                for (int i = 0; i < paras.length; i++) {
                    if (i > 0) doc.insertString(doc.getLength(), "\n", null);
                    insertParagraph(doc, paras[i]);
                }
            } else {
                insertParagraph(doc, content);
            }
        } catch (BadLocationException ignored) {} finally {
            settingText = false;
        }
        pane.setCaretPosition(0);
    }

    /**
     * Returns the current value as a {@code §x} string in display format.
     *
     * <p>
     * In multi-line mode, paragraphs are separated by actual {@code '\n'} characters.
     * Backslash characters are literal.
     */
    public @NotNull String getText() {
        if (rawMode) {
            StyledDocument doc = pane.getStyledDocument();
            try {
                String text = doc.getText(0, doc.getLength());
                return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
            } catch (BadLocationException ignored) {
                return "";
            }
        }
        return styledDocToRaw();
    }

    /**
     * Returns the formatting codes at the effective caret position for toolbar state reflection.
     *
     * <p>
     * When there is no selection, returns the pending-codes carry, which tracks the style of the
     * character preceding the caret and accumulates toolbar toggles performed since the last
     * caret movement.
     *
     * <p>
     * When there is a selection (e.g., from double-click or triple-click), returns the codes at
     * the start of the selection: the caret dot sits at the end of the selection after a word or
     * line is selected, while the formatting codes that apply to the selected content are set on
     * the characters at and before the selection starts.
     */
    @Override
    public @NotNull EnumSet<McFormatCode> getCaretStyle() {
        if (rawMode) return EnumSet.noneOf(McFormatCode.class);
        int start = pane.getSelectionStart();
        if (start != pane.getSelectionEnd()) {
            return McSwingStyle.fromAttributes(
                pane.getStyledDocument()
                    .getCharacterElement(start)
                    .getAttributes());
        }
        return EnumSet.copyOf(pendingCodes);
    }

    @Override
    public boolean hasSelection() {
        return pane.getSelectionStart() != pane.getSelectionEnd();
    }

    @Contract(" -> new")
    @Override
    public @NotNull McSelectionPresence computeSelectionPresence() {
        return McSwingStyle.computePresence(pane.getStyledDocument(), pane.getSelectionStart(), pane.getSelectionEnd());
    }

    /**
     * Registers a {@link CaretListener} on the internal {@link JTextPane}. Used by
     * {@link McFormatToolbar} to detect caret and selection changes.
     */
    @Override
    public void addCaretListener(@NotNull CaretListener l) {
        pane.addCaretListener(l);
    }

    /**
     * Registers an {@link UndoableEditListener} that is notified of document edits caused by user
     * input. Events generated by programmatic {@link #setText} calls are suppressed.
     */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        pane.getDocument()
            .addUndoableEditListener(e -> { if (!settingText) l.undoableEditHappened(e); });
    }

    /**
     * Connects a {@link McFormatToolbar} so its state tracks the caret and its buttons apply
     * formatting to this pane.
     */
    public void connectToolbar(@NotNull McFormatToolbar toolbar) {
        toolbar.connectTo(this);
    }

    /** Requests focus on the internal text pane. */
    public void requestPaneFocus() {
        pane.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Apply formatting: called by McFormatToolbar
    // ------------------------------------------------------------------

    /**
     * Applies or removes a formatting code on the current selection (or updates the pending carry
     * when there is no selection).
     *
     * @param mc     the code to apply or remove
     * @param active {@code true} to activate, {@code false} to deactivate
     */
    @Override
    public void applyCode(@NotNull McFormatCode mc, boolean active) {
        if (rawMode) return;
        if (pane.getSelectionStart() != pane.getSelectionEnd()) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (active) McSwingStyle.applyTo(attrs, mc);
            else McSwingStyle.removeFrom(attrs, mc);
            applyAttrsToSelectionRange(attrs, false);
        } else {
            if (active) pendingCodes.add(mc);
            else pendingCodes.remove(mc);
            flushPendingToInputAttributes();
        }
        pane.requestFocusInWindow();
    }

    /** Removes all formatting from the selection (or clears the pending carry). */
    @Override
    public void applyReset() {
        if (rawMode) return;
        if (pane.getSelectionStart() != pane.getSelectionEnd()) {
            applyAttrsToSelectionRange(new SimpleAttributeSet(), true);
        } else {
            pendingCodes.clear();
            flushPendingToInputAttributes();
        }
        pane.requestFocusInWindow();
    }

    @Override
    public void applyColorReset() {
        if (rawMode) return;
        int start = pane.getSelectionStart();
        int end = pane.getSelectionEnd();
        if (start != end) {
            StyledDocument doc = pane.getStyledDocument();
            int offset = start;
            while (offset < end) {
                Element elem = doc.getCharacterElement(offset);
                int runEnd = Math.min(elem.getEndOffset(), end);
                try {
                    String text = doc.getText(offset, runEnd - offset);
                    AttributeSet elemAttrs = elem.getAttributes();
                    if (McText.containsNonNewline(text) && elemAttrs.isDefined(StyleConstants.Foreground)) {
                        SimpleAttributeSet attrs = new SimpleAttributeSet(elemAttrs);
                        attrs.removeAttribute(StyleConstants.Foreground);
                        doc.setCharacterAttributes(offset, runEnd - offset, attrs, true);
                    }
                } catch (BadLocationException ignored) {}
                offset = runEnd;
            }
        } else {
            pendingCodes.removeIf(McFormatCode::isColor);
            flushPendingToInputAttributes();
        }
        pane.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Pending-codes carry
    // ------------------------------------------------------------------

    /**
     * Re-initializes the carry from the document element immediately before the caret. Called by
     * the caret listener whenever the caret moves with no active selection.
     *
     * <p>
     * Reading from {@code caretPos - 1} rather than {@code caretPos} mirrors Swing's own
     * {@code AttributeTracker} behaviour and avoids reading the implicit trailing {@code '\n'}
     * element that {@link JTextPane} maintains at the end of the document, which carries no
     * formatting attributes.
     */
    private void syncPendingFromCaret() {
        int caret = pane.getCaretPosition();
        int pos = Math.max(0, caret - 1);
        pendingCodes = McSwingStyle.fromAttributes(
            pane.getStyledDocument()
                .getCharacterElement(pos)
                .getAttributes());
        flushPendingToInputAttributes();
    }

    /**
     * Writes {@link #pendingCodes} to the Swing input-attributes map so that the next character
     * typed inherits the carry.
     */
    private void flushPendingToInputAttributes() {
        MutableAttributeSet input = pane.getInputAttributes();
        input.removeAttributes(input);
        input.addAttributes(McSwingStyle.toAttributes(pendingCodes));
    }

    // ------------------------------------------------------------------
    // Selection-range attribute application
    // ------------------------------------------------------------------

    private void applyAttrsToSelectionRange(@NotNull SimpleAttributeSet attrs, boolean replace) {
        StyledDocument doc = pane.getStyledDocument();
        int start = pane.getSelectionStart();
        int end = pane.getSelectionEnd();
        int offset = start;
        while (offset < end) {
            Element elem = doc.getCharacterElement(offset);
            int runEnd = Math.min(elem.getEndOffset(), end);
            try {
                String text = doc.getText(offset, runEnd - offset);
                if (McText.containsNonNewline(text)) {
                    doc.setCharacterAttributes(offset, runEnd - offset, attrs, replace);
                }
            } catch (BadLocationException ignored) {}
            offset = runEnd;
        }
    }

    // ------------------------------------------------------------------
    // § <-> StyledDocument conversion
    // ------------------------------------------------------------------

    private static void insertParagraph(@NotNull StyledDocument doc, @NotNull String raw) throws BadLocationException {
        for (McText.Segment seg : McText.parse(raw)) {
            doc.insertString(doc.getLength(), seg.text, McSwingStyle.toAttributes(seg.codes));
        }
    }

    /**
     * Converts the styled document to a minimal raw {@code §x} string.
     *
     * <p>
     * Emits the fewest § codes required: when a modifier is turned off or the color changes,
     * emit a color code (or {@code §r}) then re-emit all currently active modifiers; when only
     * new modifiers are added, emit only those modifier codes. Paragraph separator ({@code '\n'})
     * characters carry no formatting codes of their own; the implicit trailing {@code '\n'} that
     * JTextPane always maintains is stripped from the result.
     */
    private @NotNull String styledDocToRaw() {
        StyledDocument doc = pane.getStyledDocument();
        int len = doc.getLength();
        if (len == 0) return "";

        StringBuilder sb = new StringBuilder(len + 16);
        EnumSet<McFormatCode> prev = EnumSet.noneOf(McFormatCode.class);
        int offset = 0;
        while (offset < len) {
            Element elem = doc.getCharacterElement(offset);
            int runEnd = Math.min(elem.getEndOffset(), len);
            String text;
            try {
                text = doc.getText(offset, runEnd - offset);
            } catch (BadLocationException ignored) {
                offset = runEnd;
                continue;
            }
            if ("\n".equals(text)) {
                // Paragraph separators carry no style: append without changing state
                sb.append('\n');
            } else {
                EnumSet<McFormatCode> curr = McSwingStyle.fromAttributes(elem.getAttributes());
                McText.appendTransitionCodes(sb, prev, curr);
                sb.append(text);
                prev = curr;
            }
            offset = runEnd;
        }

        // Strip the implicit trailing newline that JTextPane always maintains
        String result = sb.toString();
        if (result.endsWith("\n")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
