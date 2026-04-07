package net.noiraude.creditseditor.ui.component;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Single-line editable field for strings that may contain Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Contains a {@link JTextField} (always the data source) and a {@link MinecraftTextRenderer}
 * (rendered preview), toggled by a {@code [<>]} / {@code [Aa]} button. Default mode is
 * <em>rendered</em>. Clicking the rendered view, or pressing the toggle button, switches to raw
 * mode and focuses the text field.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes due
 * to user input. Programmatic calls to {@link #setText} do not fire the event.
 */
public final class MinecraftTextEditor extends JPanel {

    /** Property name fired when the text value changes due to user input. */
    public static final String PROP_TEXT = "text";

    private static final String CARD_RENDERED = "rendered";
    private static final String CARD_RAW = "raw";

    private final JTextField rawField = new JTextField();
    private final MinecraftTextRenderer renderer = new MinecraftTextRenderer();
    private final JButton toggleButton = new JButton("<>");
    private final JPanel contentPanel = new JPanel(new CardLayout());

    private boolean renderedMode = true;
    private boolean settingText = false;

    public MinecraftTextEditor() {
        setLayout(new BorderLayout(2, 0));

        contentPanel.add(renderer, CARD_RENDERED);
        contentPanel.add(rawField, CARD_RAW);

        renderer.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        renderer.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (renderedMode) setRenderedMode(false);
            }
        });

        toggleButton.setMargin(new Insets(1, 4, 1, 4));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRenderedMode(!renderedMode));

        rawField.getDocument()
            .addDocumentListener(new AnyChangeListener(this::onTextChanged));

        add(contentPanel, BorderLayout.CENTER);
        add(toggleButton, BorderLayout.EAST);

        showCard(CARD_RENDERED);
    }

    private void onTextChanged() {
        renderer.setText(rawField.getText());
        if (!settingText) {
            firePropertyChange(PROP_TEXT, null, rawField.getText());
        }
    }

    private void setRenderedMode(boolean rendered) {
        this.renderedMode = rendered;
        toggleButton.setText(rendered ? "<>" : "Aa");
        showCard(rendered ? CARD_RENDERED : CARD_RAW);
        if (!rendered) {
            rawField.requestFocusInWindow();
        }
    }

    private void showCard(String card) {
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, card);
    }

    /**
     * Sets the displayed text. Does not fire a {@code "text"} property change event.
     *
     * @param text the raw string to display; {@code null} is treated as empty
     */
    public void setText(String text) {
        String value = (text != null) ? text : "";
        settingText = true;
        try {
            rawField.setText(value);
            // onTextChanged fires synchronously and updates the renderer
        } finally {
            settingText = false;
        }
    }

    /** Returns the current raw text value. */
    public String getText() {
        return rawField.getText();
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

    /** Switches to raw mode and focuses the text field. Has no effect if already in raw mode. */
    public void showRaw() {
        if (renderedMode) setRenderedMode(false);
    }
}
