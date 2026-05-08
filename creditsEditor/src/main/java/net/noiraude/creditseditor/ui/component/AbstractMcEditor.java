package net.noiraude.creditseditor.ui.component;

import net.noiraude.creditseditor.mc.McText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.UndoableEditListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.function.Consumer;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

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
    private final @NotNull McFormatToolbar toolbar;
    private final @NotNull JPanel topBarTrailing;
    private final @NotNull JButton toggleButton = new JButton("<>");
    private final boolean multiLine;
    private boolean rawMode = false;

    AbstractMcEditor(boolean multiLine) {
        this.multiLine = multiLine;
        setLayout(new BorderLayout(0, 0));

        wysiwygPane = new McWysiwygPane(multiLine);
        toolbar = new McFormatToolbar();
        wysiwygPane.connectToolbar(toolbar);

        toggleButton.setMargin(new Insets(gapHair, gapSmall, gapHair, gapSmall));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Toggle raw / rendered mode");
        toggleButton.addActionListener(e -> setRawMode(!rawMode));

        topBarTrailing = new JPanel();
        topBarTrailing.setLayout(new BoxLayout(topBarTrailing, BoxLayout.X_AXIS));
        topBarTrailing.setOpaque(false);
        topBarTrailing.add(toggleButton);

        JPanel topBar = new JPanel(new BorderLayout(gapTiny, 0));
        topBar.setOpaque(false);
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(topBarTrailing, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(wysiwygPane);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(topBar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

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
     * @param langValue the value in lang file format; pass an empty string for empty
     */
    public void setText(@NotNull String langValue) {
        wysiwygPane.setText(McText.decodeLang(langValue));
    }

    /**
     * Replaces the displayed text the same way {@link #setText} does, but lets the underlying
     * document fire its {@code "text"} property change event and undoable edit events so the
     * change is observed by listeners as if the user had typed it.
     *
     * @param langValue the value in lang file format; pass an empty string for empty
     */
    public void setTextAsUserInput(@NotNull String langValue) {
        wysiwygPane.setTextAsUserInput(McText.decodeLang(langValue));
    }

    /**
     * Sets whether the editor accepts user input. When {@code false}, the underlying text pane
     * is non-editable and the formatting toolbar is disabled.
     */
    public void setEditable(boolean editable) {
        wysiwygPane.setEditable(editable);
        setEnabledRecursive(toolbar, editable);
    }

    /** Returns {@code true} when the editor accepts user input. */
    @Contract(pure = true)
    public boolean isEditable() {
        return wysiwygPane.isEditable();
    }

    private static void setEnabledRecursive(@NotNull Container c, boolean enabled) {
        c.setEnabled(enabled);
        for (Component child : c.getComponents()) {
            if (child instanceof Container) setEnabledRecursive((Container) child, enabled);
            else child.setEnabled(enabled);
        }
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

    /**
     * Registers a typed text-change listener invoked with the current value (in lang file
     * format) after every user-driven document mutation. Programmatic {@link #setText} calls
     * are suppressed; {@link #setTextAsUserInput} is observed.
     */
    public void addTextChangeListener(@NotNull Consumer<@NotNull String> listener) {
        wysiwygPane.addTextChangeListener(displayText -> listener.accept(McText.encodeLang(displayText)));
    }

    // In single-line mode, pin the height to the preferred height so parent layouts
    // (notably GridBagLayout with sibling weighty > 0 rows) cannot shrink the editor
    // below one line or stretch it vertically.

    /** {@inheritDoc} */
    @Override
    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        if (multiLine) return min;
        return new Dimension(min.width, getPreferredSize().height);
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getMaximumSize() {
        Dimension max = super.getMaximumSize();
        if (multiLine) return max;
        return new Dimension(max.width, getPreferredSize().height);
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private void setRawMode(boolean raw) {
        rawMode = raw;
        toggleButton.setText(raw ? "Aa" : "<>");
        wysiwygPane.setRawMode(raw);
        wysiwygPane.requestFocusInWindow();
    }

    /**
     * Inserts {@code c} into the top bar's trailing group, immediately to the left of the
     * raw/rendered toggle. Each call adds a small gap before the previous leading component, so
     * later insertions stack rightward, and the toggle stays at the far edge.
     */
    final void addTopBarLeadingComponent(@NotNull Component c) {
        int toggleIdx = topBarTrailing.getComponentCount() - 1;
        topBarTrailing.add(c, toggleIdx);
        topBarTrailing.add(Box.createHorizontalStrut(gapTiny), toggleIdx + 1);
        topBarTrailing.revalidate();
        topBarTrailing.repaint();
    }

    // ------------------------------------------------------------------
    // Package-private utility shared with McWysiwygPane
    // ------------------------------------------------------------------

    /**
     * Removes the L&F-installed {@code UndoManager} from the document of {@code c} so it does
     * not maintain a parallel undo history that conflicts with the application command stack.
     */
    static void removeLafUndoManager(javax.swing.text.@NotNull JTextComponent c) {
        if (c.getClientProperty("JTextField.undoManager") instanceof UndoableEditListener listener) {
            c.getDocument()
                .removeUndoableEditListener(listener);
        }
    }
}
