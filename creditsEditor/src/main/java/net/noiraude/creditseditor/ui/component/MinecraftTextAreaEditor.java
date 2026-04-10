package net.noiraude.creditseditor.ui.component;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;

/**
 * Multi-line editable area for strings that may contain Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Each line in the editor represents one paragraph. {@link #setText} accepts and
 * {@link #getText} returns values in Minecraft 1.7.10 lang file format: paragraphs separated by
 * the literal two-character sequence {@code \n} (backslash-n), and backslashes escaped as
 * {@code \\}. Internally and visually, the editor uses actual newline characters and unescaped
 * backslashes.
 *
 * <p>
 * A {@link McWysiwygPane} is the default (rendered) mode; it shows {@code §} codes as styled
 * text and is fully editable via a {@link McFormatToolbar}. A {@link JTextArea} raw mode shows
 * and accepts the same display format with literal {@code §} escape sequences. A
 * {@code [<>]} / {@code [Aa]} toggle button at the top-right switches modes.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes due
 * to user input. The event value is in lang file format. Programmatic calls to {@link #setText}
 * do not fire the event.
 */
public final class MinecraftTextAreaEditor extends JPanel {

    /** Property name fired when the text value changes due to user input. */
    public static final String PROP_TEXT = "text";

    private static final String CARD_RENDERED = "rendered";
    private static final String CARD_RAW = "raw";

    private final McWysiwygPane wysiwygPane = new McWysiwygPane(true);
    private final JTextArea rawArea = new JTextArea();
    private final JButton toggleButton = new JButton("<>");
    private final JPanel contentCard = new JPanel(new CardLayout());

    private boolean renderedMode = true;
    private boolean settingText;
    private boolean syncing;

    public MinecraftTextAreaEditor() {
        setLayout(new BorderLayout(0, 0));

        McFormatToolbar toolbar = new McFormatToolbar();
        wysiwygPane.connectToolbar(toolbar);
        removeLafUndoManager(rawArea);

        rawArea.setLineWrap(true);
        rawArea.setWrapStyleWord(true);
        rawArea.setBackground(UIManager.getColor("TextArea.background"));

        JScrollPane rawScroll = new JScrollPane(rawArea);
        rawScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        contentCard.add(wysiwygPane, CARD_RENDERED);
        contentCard.add(rawScroll, CARD_RAW);

        toggleButton.setMargin(new Insets(1, 4, 1, 4));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRenderedMode(!renderedMode));

        JPanel topBar = new JPanel(new BorderLayout(2, 0));
        topBar.setOpaque(false);
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(toggleButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(contentCard, BorderLayout.CENTER);

        // When wysiwyg changes (display format), keep rawArea in sync
        wysiwygPane.addPropertyChangeListener(McWysiwygPane.PROP_TEXT, e -> {
            if (!syncing && !settingText) {
                syncing = true;
                try {
                    rawArea.setText((String) e.getNewValue()); // display format
                } finally {
                    syncing = false;
                }
                firePropertyChange(PROP_TEXT, null, getText());
            }
        });

        // When rawArea changes (display format), keep wysiwygPane in sync
        rawArea.getDocument()
            .addDocumentListener(new AnyChangeListener(this::onRawChanged));

        showCard(CARD_RENDERED);
    }

    private void onRawChanged() {
        if (!syncing) {
            syncing = true;
            try {
                wysiwygPane.setText(rawArea.getText()); // display format
            } finally {
                syncing = false;
            }
        }
        if (!settingText) {
            firePropertyChange(PROP_TEXT, null, getText());
        }
    }

    private void setRenderedMode(boolean rendered) {
        this.renderedMode = rendered;
        toggleButton.setText(rendered ? "<>" : "Aa");
        showCard(rendered ? CARD_RENDERED : CARD_RAW);
        if (rendered) {
            wysiwygPane.requestPaneFocus();
        } else {
            rawArea.requestFocusInWindow();
        }
    }

    private void showCard(String card) {
        ((CardLayout) contentCard.getLayout()).show(contentCard, card);
    }

    /**
     * Sets the displayed text from a value in Minecraft lang file format.
     *
     * <p>
     * Paragraphs must be separated by the two-character sequence {@code \n} (backslash-n).
     * Literal backslashes must be escaped as {@code \\}. Does not fire a {@code "text"} property
     * change event.
     *
     * @param langValue the value in lang file format; {@code null} is treated as empty
     */
    public void setText(String langValue) {
        String value = (langValue != null) ? langValue : "";
        settingText = true;
        try {
            rawArea.setText(decodeLang(value)); // triggers onRawChanged → syncs wysiwyg
            rawArea.setCaretPosition(0);
        } finally {
            settingText = false;
        }
    }

    /**
     * Returns the current value in Minecraft lang file format: paragraphs joined by the
     * two-character sequence {@code \n} (backslash-n), backslashes escaped as {@code \\}.
     */
    public String getText() {
        return encodeLang(rawArea.getText());
    }

    /** Returns {@code true} if the component is currently showing the WYSIWYG rendered view. */
    public boolean isRenderedMode() {
        return renderedMode;
    }

    /**
     * Switches to rendered (WYSIWYG) mode programmatically. Has no effect if already in rendered
     * mode.
     */
    public void showRendered() {
        if (!renderedMode) setRenderedMode(true);
    }

    /** Switches to raw mode and focuses the text area. Has no effect if already in raw mode. */
    public void showRaw() {
        if (renderedMode) setRenderedMode(false);
    }

    /**
     * Removes the L&F-installed {@code UndoManager} from the document of {@code c}.
     */
    private static void removeLafUndoManager(javax.swing.text.JTextComponent c) {
        Object mgr = c.getClientProperty("JTextField.undoManager");
        if (mgr instanceof UndoableEditListener) {
            c.getDocument()
                .removeUndoableEditListener((UndoableEditListener) mgr);
        }
    }

    /**
     * Registers an {@link UndoableEditListener} that is notified of document edits caused by user
     * input. Events from programmatic {@link #setText} calls and cross-component sync are
     * suppressed.
     */
    public void addUndoableEditListener(UndoableEditListener l) {
        wysiwygPane.addUndoableEditListener(e -> { if (!syncing) l.undoableEditHappened(e); });
        rawArea.getDocument()
            .addUndoableEditListener(e -> { if (!syncing && !settingText) l.undoableEditHappened(e); });
    }

    // ------------------------------------------------------------------
    // Lang file escape / unescape
    // ------------------------------------------------------------------

    /**
     * Decodes a Minecraft lang file value to display format.
     *
     * <p>
     * Converts {@code \n} (backslash-n) to actual newline and {@code \\} to a single
     * backslash. Other {@code \x} sequences are passed through with the backslash preserved.
     */
    private static String decodeLang(String langValue) {
        if (!langValue.contains("\\")) return langValue;
        StringBuilder sb = new StringBuilder(langValue.length());
        int i = 0;
        while (i < langValue.length()) {
            char c = langValue.charAt(i);
            if (c == '\\' && i + 1 < langValue.length()) {
                char next = langValue.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i += 2;
                } else if (next == '\\') {
                    sb.append('\\');
                    i += 2;
                } else {
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Encodes a display-format string to Minecraft lang file format.
     *
     * <p>
     * Converts actual newlines to {@code \n} (backslash-n) and backslashes to {@code \\}.
     */
    private static String encodeLang(String displayValue) {
        if (displayValue.isEmpty()) return displayValue;
        StringBuilder sb = new StringBuilder(displayValue.length() + 4);
        for (int i = 0; i < displayValue.length(); i++) {
            char c = displayValue.charAt(i);
            if (c == '\n') sb.append("\\n");
            else if (c == '\\') sb.append("\\\\");
            else sb.append(c);
        }
        return sb.toString();
    }
}
