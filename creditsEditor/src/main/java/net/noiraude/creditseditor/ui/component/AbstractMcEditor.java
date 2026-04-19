package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;

import net.noiraude.creditseditor.mc.McText;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for Minecraft-formatted text editors.
 *
 * <p>
 * Wraps a {@link McWysiwygPane} together with a {@link McFormatToolbar} and a toggle button
 * that switches the pane between rendered (WYSIWYG) and raw ({@code §x} literal) mode.
 *
 * <p>
 * Fires a {@code "text"} {@link java.beans.PropertyChangeEvent} whenever the value changes
 * due to user input. The event value is in Minecraft lang file format. Programmatic calls
 * to {@link #setText} do not fire the event.
 */
class AbstractMcEditor extends JPanel {

    /** Property name fired when the text value changes due to user input. */
    public static final @NotNull String PROP_TEXT = "text";

    private final @NotNull McWysiwygPane wysiwygPane;
    private final @NotNull JButton toggleButton = new JButton("<>");
    private boolean rawMode = false;

    AbstractMcEditor(boolean multiLine) {
        setLayout(new BorderLayout(0, 0));

        wysiwygPane = new McWysiwygPane(multiLine);
        McFormatToolbar toolbar = new McFormatToolbar();
        wysiwygPane.connectToolbar(toolbar);

        toggleButton.setMargin(new Insets(scaled(1), scaled(4), scaled(1), scaled(4)));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRawMode(!rawMode));

        JPanel topBar = new JPanel(new BorderLayout(scaled(2), 0));
        topBar.setOpaque(false);
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(toggleButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(wysiwygPane, BorderLayout.CENTER);

        wysiwygPane.addPropertyChangeListener(
            McWysiwygPane.PROP_TEXT,
            e -> firePropertyChange(PROP_TEXT, null, McText.encodeLang((String) e.getNewValue())));
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Sets the displayed text from a value in Minecraft lang file format.
     *
     * <p>
     * Does not fire a {@code "text"} property change event.
     *
     * @param langValue the value in lang file format; {@code null} is treated as empty
     */
    public void setText(@Nullable String langValue) {
        wysiwygPane.setText(McText.decodeLang(langValue != null ? langValue : ""));
    }

    /**
     * Returns the current value in Minecraft lang file format.
     */
    @Contract(pure = true)
    public @NotNull String getText() {
        return McText.encodeLang(wysiwygPane.getText());
    }

    /**
     * Registers an {@link UndoableEditListener} notified of document edits caused by user
     * input. Events from programmatic {@link #setText} calls are suppressed.
     */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        wysiwygPane.addUndoableEditListener(l);
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private void setRawMode(boolean raw) {
        rawMode = raw;
        toggleButton.setText(raw ? "Aa" : "<>");
        wysiwygPane.setRawMode(raw);
        wysiwygPane.requestPaneFocus();
    }

    // ------------------------------------------------------------------
    // Package-private utility shared with McWysiwygPane
    // ------------------------------------------------------------------

    /**
     * Removes the L&F-installed {@code UndoManager} from the document of {@code c} so it does
     * not maintain a parallel undo history that conflicts with the application command stack.
     */
    static void removeLafUndoManager(javax.swing.text.@NotNull JTextComponent c) {
        Object mgr = c.getClientProperty("JTextField.undoManager");
        if (mgr instanceof UndoableEditListener) {
            c.getDocument()
                .removeUndoableEditListener((UndoableEditListener) mgr);
        }
    }
}
