package net.noiraude.creditseditor.ui.component;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;

/**
 * Single-line editable field for strings that may contain Minecraft {@code §x} formatting codes.
 *
 * <p>
 * {@link #setText} accepts and {@link #getText} returns values in Minecraft 1.7.10 lang file
 * format: backslashes escaped as {@code \\}. Internally and visually, the editor uses unescaped
 * backslashes.
 *
 * <p>
 * Contains a {@link McWysiwygPane} (WYSIWYG rendered mode, the default) and a
 * {@link JTextField} (raw mode, showing literal {@code §} codes with unescaped backslashes).
 * A {@code [<>]} / {@code [Aa]} toggle button and a {@link McFormatToolbar} are shown in a top
 * bar.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes due
 * to user input. The event value is in lang file format. Programmatic calls to {@link #setText}
 * do not fire the event.
 */
public final class MinecraftTextEditor extends JPanel {

    /** Property name fired when the text value changes due to user input. */
    public static final String PROP_TEXT = "text";

    private static final String CARD_RENDERED = "rendered";
    private static final String CARD_RAW = "raw";

    private final McWysiwygPane wysiwygPane = new McWysiwygPane(false);
    private final JTextField rawField = new JTextField();
    private final JButton toggleButton = new JButton("<>");
    private final JPanel contentPanel = new JPanel(new CardLayout());

    private boolean renderedMode = true;
    private boolean settingText;
    private boolean syncing;

    public MinecraftTextEditor() {
        setLayout(new BorderLayout(0, 0));

        McFormatToolbar toolbar = new McFormatToolbar();
        wysiwygPane.connectToolbar(toolbar);
        removeLafUndoManager(rawField);

        contentPanel.add(wysiwygPane, CARD_RENDERED);
        contentPanel.add(rawField, CARD_RAW);

        toggleButton.setMargin(new Insets(1, 4, 1, 4));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRenderedMode(!renderedMode));

        JPanel topBar = new JPanel(new BorderLayout(2, 0));
        topBar.setOpaque(false);
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(toggleButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // When wysiwyg changes (display format), keep rawField in sync
        wysiwygPane.addPropertyChangeListener(McWysiwygPane.PROP_TEXT, e -> {
            if (!syncing && !settingText) {
                syncing = true;
                try {
                    rawField.setText((String) e.getNewValue()); // display format
                } finally {
                    syncing = false;
                }
                // PROP_TEXT is fired from onRawChanged() triggered by rawField.setText above
            }
        });

        // When rawField changes (display format), keep wysiwygPane in sync
        rawField.getDocument()
            .addDocumentListener(new AnyChangeListener(this::onRawChanged));

        showCard(CARD_RENDERED);
    }

    private void onRawChanged() {
        if (!syncing) {
            syncing = true;
            try {
                wysiwygPane.setText(rawField.getText()); // display format
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
            rawField.requestFocusInWindow();
        }
    }

    private void showCard(String card) {
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, card);
    }

    /**
     * Sets the displayed text from a value in Minecraft lang file format.
     *
     * <p>
     * Literal backslashes must be escaped as {@code \\}. Does not fire a {@code "text"}
     * property change event.
     *
     * @param langValue the value in lang file format; {@code null} is treated as empty
     */
    public void setText(String langValue) {
        String value = (langValue != null) ? langValue : "";
        settingText = true;
        try {
            rawField.setText(decodeLang(value)); // triggers onRawChanged which syncs wysiwygPane
        } finally {
            settingText = false;
        }
    }

    /**
     * Returns the current raw text value in Minecraft lang file format (backslashes escaped as
     * {@code \\}).
     */
    public String getText() {
        return encodeLang(rawField.getText());
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
        rawField.getDocument()
            .addUndoableEditListener(e -> { if (!syncing && !settingText) l.undoableEditHappened(e); });
    }

    // ------------------------------------------------------------------
    // Lang file escape / unescape
    // ------------------------------------------------------------------

    /**
     * Decodes a Minecraft lang file value to display format.
     *
     * <p>
     * Converts {@code \\} to a single backslash and {@code \n} to actual newline. Other
     * {@code \x} sequences are passed through with the backslash preserved.
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
     * Converts backslashes to {@code \\} and actual newlines to {@code \n} (backslash-n).
     */
    private static String encodeLang(String displayValue) {
        if (displayValue.isEmpty() || (!displayValue.contains("\\") && !displayValue.contains("\n"))) {
            return displayValue;
        }
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
