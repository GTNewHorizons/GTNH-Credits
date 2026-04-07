package net.noiraude.creditseditor.ui.component;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * Multi-line editable area for strings that may contain Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Each line in the editor represents one paragraph. Paragraphs are joined with the two-character
 * sequence {@code \n} (backslash-n) when read via {@link #getText}, matching the separator used in
 * Minecraft 1.7.10 lang files. The value is split on the same sequence when set via
 * {@link #setText}.
 *
 * <p>
 * Raw mode shows a {@link JTextArea} where the user types {@code §} codes directly and presses
 * Enter to start a new paragraph. Rendered mode shows one {@link MinecraftTextRenderer} per
 * paragraph in a scrollable panel. A {@code [<>]} / {@code [Aa]} toggle button at the top-right
 * switches modes. Default mode is <em>rendered</em>.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes due
 * to user input. Programmatic calls to {@link #setText} do not fire the event.
 */
public final class MinecraftTextAreaEditor extends JPanel {

    /** Property name fired when the text value changes due to user input. */
    public static final String PROP_TEXT = "text";

    private static final String CARD_RENDERED = "rendered";
    private static final String CARD_RAW = "raw";

    /**
     * Separator used in {@link #getText} and expected by {@link #setText}: the two-character
     * sequence backslash followed by n, matching Minecraft 1.7.10 lang file conventions.
     */
    private static final String PARA_SEP = "\\n";

    private final JTextArea rawArea = new JTextArea();
    private final JPanel renderedContent = new JPanel();
    private final JPanel contentCard = new JPanel(new CardLayout());
    private final JButton toggleButton = new JButton("<>");

    private boolean renderedMode = true;
    private boolean settingText = false;

    public MinecraftTextAreaEditor() {
        setLayout(new BorderLayout(0, 0));

        rawArea.setLineWrap(true);
        rawArea.setWrapStyleWord(true);

        renderedContent.setLayout(new BoxLayout(renderedContent, BoxLayout.Y_AXIS));
        renderedContent.setBackground(UIManager.getColor("TextArea.background"));
        renderedContent.setOpaque(true);

        contentCard.add(new JScrollPane(renderedContent), CARD_RENDERED);
        contentCard.add(new JScrollPane(rawArea), CARD_RAW);

        toggleButton.setMargin(new Insets(1, 4, 1, 4));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRenderedMode(!renderedMode));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        header.add(toggleButton);

        rawArea.getDocument()
            .addDocumentListener(new AnyChangeListener(this::onTextChanged));

        add(header, BorderLayout.NORTH);
        add(contentCard, BorderLayout.CENTER);

        showCard(CARD_RENDERED);
    }

    private void onTextChanged() {
        rebuildRenderedView();
        if (!settingText) {
            firePropertyChange(PROP_TEXT, null, getText());
        }
    }

    private void rebuildRenderedView() {
        renderedContent.removeAll();
        String[] paragraphs = rawArea.getText()
            .split("\n", -1);
        for (String para : paragraphs) {
            MinecraftTextRenderer r = new MinecraftTextRenderer();
            r.setText(para);
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            renderedContent.add(r);
        }
        renderedContent.revalidate();
        renderedContent.repaint();
    }

    private void setRenderedMode(boolean rendered) {
        this.renderedMode = rendered;
        toggleButton.setText(rendered ? "<>" : "Aa");
        showCard(rendered ? CARD_RENDERED : CARD_RAW);
        if (!rendered) {
            rawArea.requestFocusInWindow();
        }
    }

    private void showCard(String card) {
        ((CardLayout) contentCard.getLayout()).show(contentCard, card);
    }

    /**
     * Sets the displayed text. Paragraphs must be separated by the two-character sequence
     * {@code \n} (backslash-n). Does not fire a {@code "text"} property change event.
     *
     * @param text the raw value; {@code null} is treated as empty
     */
    public void setText(String text) {
        String value = (text != null) ? text : "";
        // Convert the literal two-char \n separator to actual newlines for JTextArea
        String forArea = value.replace(PARA_SEP, "\n");
        settingText = true;
        try {
            rawArea.setText(forArea);
            rebuildRenderedView();
        } finally {
            settingText = false;
        }
    }

    /**
     * Returns the current value with paragraphs separated by the two-character sequence {@code \n}
     * (backslash-n), matching Minecraft 1.7.10 lang file conventions.
     */
    public String getText() {
        // Convert actual JTextArea newlines back to the literal \n separator
        return rawArea.getText()
            .replace("\n", PARA_SEP);
    }

    /** Returns {@code true} if the component is currently showing the rendered view. */
    public boolean isRenderedMode() {
        return renderedMode;
    }

    /**
     * Switches to rendered mode programmatically. Has no effect if already in rendered mode.
     */
    public void showRendered() {
        if (!renderedMode) setRenderedMode(true);
    }

    /** Switches to raw mode and focuses the text area. Has no effect if already in raw mode. */
    public void showRaw() {
        if (renderedMode) setRenderedMode(false);
    }
}
