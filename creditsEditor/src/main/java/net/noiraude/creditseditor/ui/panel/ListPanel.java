package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.libcredits.model.CreditsDocument;

/**
 * Abstract base panel for list-based editor panels (categories, persons).
 *
 * <p>
 * Provides the shared list infrastructure: a titled border, a single-selection
 * {@link JList} in a scroll pane, and {@code +} / {@code -} toolbar buttons.
 * Subclasses supply the concrete element type {@code E}, the selection-callback
 * value type {@code S} (which may differ from {@code E} when a sentinel element
 * is present), and implement {@link #getSelection()} and {@link #updateButtons()}.
 *
 * @param <E> list element type
 * @param <S> type passed to the selection-changed callback
 */
abstract class ListPanel<E, S> extends JPanel {

    protected final CommandExecutor onCommand;
    protected CreditsDocument creditsDoc;

    protected final DefaultListModel<E> listModel = new DefaultListModel<>();
    protected final JList<E> list = new JList<>(listModel);
    protected boolean refreshing;

    protected final JButton addButton = new JButton("+");
    protected final JButton removeButton = new JButton("-");

    /**
     * @param title              border title for the panel
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selection value (or {@code null}) on change
     */
    protected ListPanel(String title, CommandExecutor onCommand, Consumer<S> onSelectionChanged) {
        this.onCommand = onCommand;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
                if (!refreshing) onSelectionChanged.accept(getSelection());
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Returns the value to pass to the selection-changed callback for the current
     * list selection. Returns {@code null} when nothing meaningful is selected.
     */
    protected abstract S getSelection();

    /** Updates toolbar button enabled states to reflect the current selection. */
    protected abstract void updateButtons();

    /** Returns the underlying list component, for targeted repaint after field edits. */
    public JList<E> getList() {
        return list;
    }
}
