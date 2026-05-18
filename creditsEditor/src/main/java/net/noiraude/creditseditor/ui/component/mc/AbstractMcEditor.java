package net.noiraude.creditseditor.ui.component.mc;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHair;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.UndoableEditListener;

import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Abstract base for Minecraft-formatted text editors. */
class AbstractMcEditor extends JPanel {

    /** Property name fired on user-driven text changes. */
    public static final @NotNull String PROP_TEXT = "text";

    private final @NotNull McWysiwygPane wysiwygPane;
    private final @NotNull McFormatToolbar toolbar;
    private final @NotNull JPanel topBarTrailing;
    private final @NotNull JButton toggleButton = new JButton("<>");
    private final @NotNull JScrollPane defaultBody;
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
        toggleButton.setToolTipText(I18n.get("toolbar.raw_rendered.toggle.tooltip"));
        toggleButton.addActionListener(e -> setRawMode(!rawMode));

        topBarTrailing = new JPanel();
        topBarTrailing.setLayout(new BoxLayout(topBarTrailing, BoxLayout.X_AXIS));
        topBarTrailing.setOpaque(false);
        topBarTrailing.add(toggleButton);

        JPanel topBar = new JPanel(new BorderLayout(gapTiny, 0));
        topBar.setOpaque(false);
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(topBarTrailing, BorderLayout.EAST);

        defaultBody = new JScrollPane(wysiwygPane);
        defaultBody.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(topBar, BorderLayout.NORTH);
        add(defaultBody, BorderLayout.CENTER);

        wysiwygPane.addPropertyChangeListener(
            McWysiwygPane.PROP_TEXT,
            e -> firePropertyChange(PROP_TEXT, null, McText.encodeLang((String) e.getNewValue())));
    }

    /** Replaces the body shown below the top bar with a custom component. */
    final void setBodyComponent(@NotNull JComponent body) {
        BorderLayout layout = (BorderLayout) getLayout();
        Component current = layout.getLayoutComponent(BorderLayout.CENTER);
        if (current == body) return;
        if (current != null) remove(current);
        add(body, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Restores the body to the editor's own scrolled WYSIWYG pane. */
    final void restoreDefaultBody() {
        setBodyComponent(defaultBody);
    }

    /** Installs the clipboard handler on the underlying text pane. */
    final void setPaneTransferHandler(@NotNull javax.swing.TransferHandler handler) {
        wysiwygPane.setTransferHandler(handler);
    }

    /** Pushes {@code displayText} to {@code pane} in the controller's current content form. */
    protected final void pushContentTo(@NotNull McWysiwygPane pane, @NotNull String displayText) {
        if (rawMode) pane.setPlainText(displayText);
        else pane.setStyledText(displayText);
    }

    /** Pushes {@code displayText} to {@code pane} in the current form with user-input events. */
    protected final void pushContentToAsUserInput(@NotNull McWysiwygPane pane, @NotNull String displayText) {
        if (rawMode) pane.setPlainTextAsUserInput(displayText);
        else pane.setStyledTextAsUserInput(displayText);
    }

    /** Subclass hook for refreshing additional panes owned by the subclass after a state change. */
    protected void refreshOwnedPanes() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Sets the displayed text from a value in Minecraft lang file format. */
    public void setText(@NotNull String langValue) {
        pushContentTo(wysiwygPane, McText.decodeLang(langValue));
    }

    /** Sets the displayed text from a lang-file value and fires user-input events. */
    public void setTextAsUserInput(@NotNull String langValue) {
        pushContentToAsUserInput(wysiwygPane, McText.decodeLang(langValue));
    }

    /** Sets whether the editor accepts user input. */
    public void setEditable(boolean editable) {
        wysiwygPane.setEditable(editable);
        updateToolbarEnabled();
    }

    /** Returns {@code true} when the editor accepts user input. */
    @Contract(pure = true)
    public boolean isEditable() {
        return wysiwygPane.isEditable();
    }

    /** Returns the caret offset within the underlying document. */
    @Contract(pure = true)
    public int getCaretPosition() {
        return wysiwygPane.getCaretPosition();
    }

    /** Sets the caret offset within the underlying document, clamped to its current length. */
    public void setCaretPosition(int position) {
        int len = wysiwygPane.getDocument()
            .getLength();
        wysiwygPane.setCaretPosition(Math.max(0, Math.min(position, len)));
    }

    private static void setEnabledRecursive(@NotNull Container c, boolean enabled) {
        c.setEnabled(enabled);
        for (Component child : c.getComponents()) {
            if (child instanceof Container) setEnabledRecursive((Container) child, enabled);
            else child.setEnabled(enabled);
        }
    }

    /** Returns the current value in Minecraft lang file format. */
    @Contract(pure = true)
    public @NotNull String getText() {
        return McText.encodeLang(wysiwygPane.getText());
    }

    /** Registers an {@link UndoableEditListener} notified of user-driven document edits. */
    public void addUndoableEditListener(@NotNull UndoableEditListener l) {
        wysiwygPane.addUndoableEditListener(l);
    }

    /** Registers a typed text-change listener invoked after every user-driven document mutation. */
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
        if (raw == rawMode) return;
        String canonical = wysiwygPane.getText();
        rawMode = raw;
        pushContentTo(wysiwygPane, canonical);
        updateToolbarEnabled();
        refreshOwnedPanes();
        toggleButton.setText(raw ? "Aa" : "<>");
        wysiwygPane.requestFocusInWindow();
    }

    private void updateToolbarEnabled() {
        setEnabledRecursive(toolbar, !rawMode && wysiwygPane.isEditable());
    }

    /** Inserts {@code c} into the top bar trailing group, before the raw/rendered toggle. */
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

    /** Removes the L&amp;F-installed {@code UndoManager} from the document of {@code c}. */
    static void removeLafUndoManager(javax.swing.text.@NotNull JTextComponent c) {
        if (c.getClientProperty("JTextField.undoManager") instanceof UndoableEditListener listener) {
            c.getDocument()
                .removeUndoableEditListener(listener);
        }
    }
}
