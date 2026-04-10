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
import javax.swing.text.StyledDocument;

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
 */
public final class McWysiwygPane extends JScrollPane {

    /** Property name fired when the text value changes due to user input. */
    public static final String PROP_TEXT = "text";

    private final JTextPane pane = new JTextPane() {

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // always fill viewport width so word-wrap matches JTextArea behavior
        }
    };
    private final boolean multiLine;
    private boolean settingText;

    public McWysiwygPane(boolean multiLine) {
        this.multiLine = multiLine;

        pane.setBackground(UIManager.getColor("TextArea.background"));
        setViewportView(pane);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        removeLafUndoManager(pane);

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
    }

    /**
     * Removes the L&F-installed {@code UndoManager} from the document of {@code c} so it does
     * not maintain a parallel undo history that conflicts with the application command stack.
     */
    private static void removeLafUndoManager(javax.swing.text.JTextComponent c) {
        Object mgr = c.getClientProperty("JTextField.undoManager");
        if (mgr instanceof UndoableEditListener) {
            c.getDocument()
                .removeUndoableEditListener((UndoableEditListener) mgr);
        }
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
     * Sets the displayed text from a {@code §x} string in display format.
     *
     * <p>
     * In multi-line mode, paragraphs are separated by actual {@code '\n'} characters.
     * Backslash characters are literal. Does not fire a {@code "text"} property change event.
     *
     * @param displayText the value in display format; {@code null} is treated as empty
     */
    public void setText(String displayText) {
        settingText = true;
        try {
            StyledDocument doc = pane.getStyledDocument();
            doc.remove(0, doc.getLength());
            String content = displayText != null ? displayText : "";
            if (multiLine) {
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
    public String getText() {
        return styledDocToRaw();
    }

    /**
     * Returns the character attributes at the current caret position, for use by
     * {@link McFormatToolbar} to reflect the active formatting state.
     */
    public AttributeSet getCaretAttributes() {
        return pane.getCharacterAttributes();
    }

    /**
     * Registers a {@link CaretListener} on the internal {@link JTextPane}. Used by
     * {@link McFormatToolbar} to detect caret and selection changes.
     */
    public void addCaretListener(CaretListener l) {
        pane.addCaretListener(l);
    }

    /**
     * Registers an {@link UndoableEditListener} that is notified of document edits caused by user
     * input. Events generated by programmatic {@link #setText} calls are suppressed.
     */
    public void addUndoableEditListener(UndoableEditListener l) {
        pane.getDocument()
            .addUndoableEditListener(e -> { if (!settingText) l.undoableEditHappened(e); });
    }

    /**
     * Connects a {@link McFormatToolbar} so its state tracks the caret and its buttons apply
     * formatting to this pane.
     */
    public void connectToolbar(McFormatToolbar toolbar) {
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
     * Applies or removes a formatting code on the current selection (or sets the input style when
     * there is no selection).
     *
     * @param mc     the code to apply or remove
     * @param active {@code true} to activate, {@code false} to deactivate
     */
    void applyCode(McFormatCode mc, boolean active) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        if (active) mc.applyTo(attrs);
        else mc.removeFrom(attrs);
        applyToSelection(attrs, false);
    }

    /** Removes all formatting from the selection (equivalent to {@code §r}). */
    void applyReset() {
        applyToSelection(new SimpleAttributeSet(), true);
    }

    private void applyToSelection(SimpleAttributeSet attrs, boolean replace) {
        int start = pane.getSelectionStart();
        int end = pane.getSelectionEnd();
        if (start != end) {
            pane.getStyledDocument()
                .setCharacterAttributes(start, end - start, attrs, replace);
        } else {
            MutableAttributeSet input = pane.getInputAttributes();
            if (replace) {
                McFormatCode.clearAttributes(input);
            } else {
                input.addAttributes(attrs);
            }
        }
        pane.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // § ↔ StyledDocument conversion
    // ------------------------------------------------------------------

    private static void insertParagraph(StyledDocument doc, String raw) throws BadLocationException {
        for (McFormatCode.Segment seg : McFormatCode.parse(raw)) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            for (McFormatCode mc : seg.codes) mc.applyTo(attrs);
            doc.insertString(doc.getLength(), seg.text, attrs);
        }
    }

    /**
     * Converts the styled document to a minimal raw {@code §x} string.
     *
     * <p>
     * Emits the fewest § codes required:
     * <ul>
     * <li>When a modifier is turned off or the color changes: emit a color code (or {@code §r}),
     * then re-emit all currently active modifiers.
     * <li>When only new modifiers are added: emit only those modifier codes.
     * </ul>
     * Paragraph separator ({@code '\n'}) characters carry no formatting codes of their own; the
     * implicit trailing {@code '\n'} that JTextPane always maintains is stripped from the result.
     */
    private String styledDocToRaw() {
        StyledDocument doc = pane.getStyledDocument();
        int len = doc.getLength();
        if (len == 0) return "";

        StringBuilder sb = new StringBuilder(len + 16);
        RunStyle prev = RunStyle.DEFAULT;
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
                RunStyle curr = RunStyle.of(elem.getAttributes());
                appendTransitionCodes(sb, prev, curr);
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

    /**
     * Emits the minimal set of § codes to transition from {@code prev} to {@code curr} style.
     *
     * <p>
     * When a modifier is turned off or the color changes, a reset point (color code or
     * {@code §r}) is emitted followed by all currently active modifier codes. When only new
     * modifiers are added, only those new codes are emitted.
     */
    private static void appendTransitionCodes(StringBuilder sb, RunStyle prev, RunStyle curr) {
        if (McFormatCode.needsResetBefore(prev.codes, curr.codes)) {
            McFormatCode color = McFormatCode.activeColor(curr.codes);
            if (color != null) color.appendTo(sb);
            else sb.append("§r");
            for (McFormatCode mc : curr.codes) {
                if (mc.isModifier()) mc.appendTo(sb);
            }
        } else {
            for (McFormatCode mc : curr.codes) {
                if (mc.isModifier() && !prev.codes.contains(mc)) mc.appendTo(sb);
            }
        }
    }

    // ------------------------------------------------------------------
    // RunStyle value type
    // ------------------------------------------------------------------

    /** Immutable snapshot of the formatting codes active at one styled run. */
    private static final class RunStyle {

        static final RunStyle DEFAULT = new RunStyle(EnumSet.noneOf(McFormatCode.class));

        final EnumSet<McFormatCode> codes;

        private RunStyle(EnumSet<McFormatCode> codes) {
            this.codes = codes;
        }

        static RunStyle of(AttributeSet attrs) {
            return new RunStyle(McFormatCode.fromAttributes(attrs));
        }
    }
}
