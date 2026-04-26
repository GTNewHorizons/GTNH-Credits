package net.noiraude.creditseditor.ui.component;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
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
 * Headless model behind {@link McWysiwygPane}: owns a {@link StyledDocument} and a
 * {@link McFormatCode} carry, and converts to and from the Minecraft {@code §x} string format.
 *
 * <p>
 * The model carries no Swing UI dependency beyond {@code javax.swing.text}, so it can be unit
 * tested without a graphics environment. The pane wires this model to a {@link javax.swing.JTextPane}
 * that uses {@link #getDocument()} as its document.
 *
 * <h3>Pending-codes carry</h3>
 *
 * <p>
 * The carry ({@link #getPendingCodes()}) is the active style for the next character to be typed.
 * It is re-initialized from the element at {@code caretPos - 1} via {@link #syncPendingFromCaret}
 * whenever the caret moves with no selection, and mutated by {@link #applyCode}, {@link #applyReset}
 * and {@link #applyColorReset} when the operation has no selection.
 *
 * <p>
 * The carry is intentionally separate from {@link #getCaretStyle}: {@code getCaretStyle} reads
 * the document directly so callers (notably {@link McFormatToolbar}) get a value that does not
 * depend on the order in which caret listeners fire.
 */
public final class McDocumentModel {

    private static final Logger LOG = System.getLogger(McDocumentModel.class.getName());

    private final @NotNull StyledDocument doc = new DefaultStyledDocument();
    private final boolean multiLine;
    private boolean rawMode;
    private @NotNull EnumSet<McFormatCode> pendingCodes = EnumSet.noneOf(McFormatCode.class);

    @Contract(pure = true)
    public McDocumentModel(boolean multiLine) {
        this.multiLine = multiLine;
    }

    @Contract(pure = true)
    public @NotNull StyledDocument getDocument() {
        return doc;
    }

    @Contract(pure = true)
    public boolean isRawMode() {
        return rawMode;
    }

    /** Returns a defensive copy of the pending-codes carry. */
    @Contract(" -> new")
    public @NotNull EnumSet<McFormatCode> getPendingCodes() {
        return EnumSet.copyOf(pendingCodes);
    }

    // ------------------------------------------------------------------
    // Text I/O
    // ------------------------------------------------------------------

    /**
     * Replaces the document content from a {@code §x} display string.
     *
     * <p>
     * In multi-line mode, paragraphs separated by {@code '\n'} are inserted with the separator
     * carrying no formatting. In raw mode the string is inserted verbatim.
     */
    public void setText(@Nullable String displayText) {
        try {
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
        } catch (BadLocationException ex) {
            LOG.log(Level.WARNING, "setText: bad location while rebuilding document", ex);
        }
    }

    /**
     * Returns the document content as a {@code §x} string in display format.
     *
     * <p>
     * In multi-line mode, paragraphs are separated by actual {@code '\n'} characters. The
     * implicit trailing {@code '\n'} that {@link DefaultStyledDocument} maintains is stripped.
     */
    public @NotNull String getText() {
        if (rawMode) {
            try {
                String text = doc.getText(0, doc.getLength());
                return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
            } catch (BadLocationException ex) {
                LOG.log(Level.WARNING, "getText: bad location while reading raw document", ex);
                return "";
            }
        }
        return styledDocToRaw();
    }

    /**
     * Switches between rendered (WYSIWYG) and raw (literal {@code §x}) display mode, preserving
     * the current text across the switch and clearing the pending-codes carry.
     */
    public void setRawMode(boolean raw) {
        if (this.rawMode == raw) return;
        String text = getText();
        this.rawMode = raw;
        pendingCodes.clear();
        setText(text);
    }

    // ------------------------------------------------------------------
    // Caret-aware queries
    // ------------------------------------------------------------------

    /**
     * Re-initializes the pending-codes carry from the element immediately before {@code caretPos}.
     *
     * <p>
     * Reading from {@code caretPos - 1} mirrors Swing's {@code AttributeTracker} behaviour and
     * skips the implicit trailing {@code '\n'} element, which carries no formatting.
     */
    public void syncPendingFromCaret(int caretPos) {
        int pos = Math.max(0, caretPos - 1);
        pendingCodes = McSwingStyle.fromAttributes(
            doc.getCharacterElement(pos)
                .getAttributes());
    }

    /**
     * Returns the codes that should be reflected in the toolbar state for the given caret/selection.
     *
     * <p>
     * With a selection, returns the codes at {@code selStart}: the caret dot sits at the end of
     * a word- or line-selection while the styles that govern the selected content are set on the
     * characters at and before the start. Without a selection, returns the codes at the element
     * immediately before {@code selStart}, read directly from the document so the result does
     * not depend on the order in which caret listeners run.
     */
    public @NotNull EnumSet<McFormatCode> getCaretStyle(int selStart, int selEnd) {
        if (rawMode) return EnumSet.noneOf(McFormatCode.class);
        if (selStart != selEnd) {
            return McSwingStyle.fromAttributes(
                doc.getCharacterElement(selStart)
                    .getAttributes());
        }
        int pos = Math.max(0, selStart - 1);
        return McSwingStyle.fromAttributes(
            doc.getCharacterElement(pos)
                .getAttributes());
    }

    /** Computes per-code presence across the characters in {@code [selStart, selEnd)}. */
    @Contract("_, _ -> new")
    public @NotNull McSelectionPresence computeSelectionPresence(int selStart, int selEnd) {
        return McSwingStyle.computePresence(doc, selStart, selEnd);
    }

    // ------------------------------------------------------------------
    // Formatting application
    // ------------------------------------------------------------------

    /**
     * Applies or removes a code on the selection if non-empty, otherwise updates the carry.
     */
    public void applyCode(@NotNull McFormatCode mc, boolean active, int selStart, int selEnd) {
        if (rawMode) return;
        if (selStart != selEnd) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (active) McSwingStyle.applyTo(attrs, mc);
            else McSwingStyle.removeFrom(attrs, mc);
            applyAttrsToSelectionRange(attrs, false, selStart, selEnd);
        } else {
            if (active) pendingCodes.add(mc);
            else pendingCodes.remove(mc);
        }
    }

    /** Removes all formatting from the selection if non-empty, otherwise clears the carry. */
    public void applyReset(int selStart, int selEnd) {
        if (rawMode) return;
        if (selStart != selEnd) {
            applyAttrsToSelectionRange(new SimpleAttributeSet(), true, selStart, selEnd);
        } else {
            pendingCodes.clear();
        }
    }

    /**
     * Removes only the color attribute from the selection if non-empty, otherwise drops every
     * color from the carry while leaving modifiers intact.
     */
    public void applyColorReset(int selStart, int selEnd) {
        if (rawMode) return;
        if (selStart != selEnd) {
            int offset = selStart;
            while (offset < selEnd) {
                Element elem = doc.getCharacterElement(offset);
                int runEnd = Math.min(elem.getEndOffset(), selEnd);
                try {
                    String text = doc.getText(offset, runEnd - offset);
                    AttributeSet elemAttrs = elem.getAttributes();
                    if (McText.containsNonNewline(text) && elemAttrs.isDefined(StyleConstants.Foreground)) {
                        SimpleAttributeSet attrs = new SimpleAttributeSet(elemAttrs);
                        attrs.removeAttribute(StyleConstants.Foreground);
                        doc.setCharacterAttributes(offset, runEnd - offset, attrs, true);
                    }
                } catch (BadLocationException ex) {
                    LOG.log(Level.WARNING, "applyColorReset: bad location while scanning run", ex);
                }
                offset = runEnd;
            }
        } else {
            pendingCodes.removeIf(McFormatCode::isColor);
        }
    }

    private void applyAttrsToSelectionRange(@NotNull SimpleAttributeSet attrs, boolean replace, int start, int end) {
        int offset = start;
        while (offset < end) {
            Element elem = doc.getCharacterElement(offset);
            int runEnd = Math.min(elem.getEndOffset(), end);
            try {
                String text = doc.getText(offset, runEnd - offset);
                if (McText.containsNonNewline(text)) {
                    doc.setCharacterAttributes(offset, runEnd - offset, attrs, replace);
                }
            } catch (BadLocationException ex) {
                LOG.log(Level.WARNING, "applyAttrsToSelectionRange: bad location while scanning run", ex);
            }
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
     * Converts the styled document to a minimal raw {@code §x} string, emitting only the
     * transition codes required between adjacent runs and stripping the implicit trailing
     * {@code '\n'} maintained by {@link DefaultStyledDocument}.
     */
    private @NotNull String styledDocToRaw() {
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
            } catch (BadLocationException ex) {
                LOG.log(Level.WARNING, "styledDocToRaw: bad location while reading run", ex);
                offset = runEnd;
                continue;
            }
            if ("\n".equals(text)) {
                sb.append('\n');
            } else {
                EnumSet<McFormatCode> curr = McSwingStyle.fromAttributes(elem.getAttributes());
                McText.appendTransitionCodes(sb, prev, curr);
                sb.append(text);
                prev = curr;
            }
            offset = runEnd;
        }

        String result = sb.toString();
        if (result.endsWith("\n")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
