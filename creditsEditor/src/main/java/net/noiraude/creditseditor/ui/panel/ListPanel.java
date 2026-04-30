package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    protected final @NotNull CommandExecutor onCommand;

    protected final @NotNull DefaultListModel<E> listModel = new DefaultListModel<>();
    protected final @NotNull JList<E> list = new JList<>(listModel);
    protected boolean refreshing;

    protected final @NotNull JButton addButton = new JButton("+");
    protected final @NotNull JButton removeButton = new JButton("-");

    /**
     * @param title              border title for the panel
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selection value (or {@code null}) on change
     */
    protected ListPanel(@NotNull String title, @NotNull CommandExecutor onCommand,
        @NotNull Consumer<S> onSelectionChanged) {
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
        list.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(@NotNull FocusEvent e) {
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
    protected abstract @Nullable S getSelection();

    /** Updates toolbar button enabled states to reflect the current selection. */
    protected abstract void updateButtons();
}
