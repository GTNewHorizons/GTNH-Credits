package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.AddPersonCommand;
import net.noiraude.creditseditor.command.impl.RemovePersonCommand;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.creditseditor.ui.component.AnyChangeListener;
import net.noiraude.creditseditor.ui.component.McFormatCode;

/**
 * Middle panel showing the list of persons, optionally filtered by category.
 *
 * <p>
 * Call {@link #refresh(EditorModel)} after any model change. Call
 * {@link #setFilter(EditorCategory)} to restrict the visible set to members of a specific
 * category; pass {@code null} to show all persons.
 */
public final class PersonPanel extends ListPanel<EditorPerson, EditorPerson> {

    private EditorCategory filter;

    private final JTextField searchField = new JTextField();

    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected {@link EditorPerson}, or
     *                           {@code null} when the selection is cleared
     */
    public PersonPanel(Consumer<Command> onCommand, Consumer<EditorPerson> onSelectionChanged) {
        super("Persons", onCommand, onSelectionChanged);
        list.setCellRenderer(new PersonCellRenderer());

        // Search bar
        searchField.putClientProperty("JTextField.placeholderText", "Filter by name…");
        searchField.getDocument()
            .addDocumentListener(new AnyChangeListener(this::applyFilter));
        JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        searchRow.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        searchRow.add(new JLabel("Search:"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        add(searchRow, BorderLayout.NORTH);

        // Toolbar
        addButton.setToolTipText("Add person");
        removeButton.setToolTipText("Remove person");
        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Changes the active category filter and repopulates the list.
     * Pass {@code null} to show all persons.
     */
    public void setFilter(EditorCategory category) {
        filter = category;
        applyFilter();
    }

    /**
     * Repopulates the list from the model, respecting the current filter and search term.
     * Preserves the selection by name where possible.
     */
    public void refresh(EditorModel model) {
        this.model = model;
        refreshing = true;
        try {
            applyFilter();
        } finally {
            refreshing = false;
        }
    }

    @Override
    protected EditorPerson getSelection() {
        return list.getSelectedValue();
    }

    /**
     * Returns the currently selected {@link EditorPerson}, or {@code null} if nothing is
     * selected.
     */
    public EditorPerson getSelectedPerson() {
        return list.getSelectedValue();
    }

    // -----------------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (model == null) return;
        String name = JOptionPane.showInputDialog(this, "Person name:", "Add person", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        onCommand.accept(new AddPersonCommand(model, new EditorPerson(name.strip())));
    }

    private void onRemove() {
        EditorPerson person = list.getSelectedValue();
        if (person == null || model == null) return;
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove '" + McFormatCode.strip(person.name) + "'?",
            "Confirm remove",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            onCommand.accept(new RemovePersonCommand(model, person));
        }
    }

    // -----------------------------------------------------------------------
    // Filter / population
    // -----------------------------------------------------------------------

    private void applyFilter() {
        if (model == null) return;

        String prevName = list.getSelectedValue() != null ? list.getSelectedValue().name : null;
        String search = searchField.getText()
            .toLowerCase();

        List<EditorPerson> visible = model.persons.stream()
            .filter(p -> passesFilter(p, search))
            .toList();

        listModel.clear();
        for (EditorPerson p : visible) {
            listModel.addElement(p);
        }

        // Restore selection by name
        boolean restored = false;
        if (prevName != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).name.equals(prevName)) {
                    list.setSelectedIndex(i);
                    restored = true;
                    break;
                }
            }
        }
        if (!restored) {
            list.clearSelection();
        }

        updateButtons();
    }

    private boolean passesFilter(EditorPerson person, String search) {
        if (filter != null) {
            boolean member = person.memberships.stream()
                .anyMatch(m -> m.categoryId.equals(filter.id));
            if (!member) return false;
        }
        if (!search.isEmpty()) {
            return McFormatCode.strip(person.name)
                .toLowerCase()
                .contains(search);
        }
        return true;
    }

    @Override
    protected void updateButtons() {
        removeButton.setEnabled(list.getSelectedValue() != null && model != null);
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private static final class PersonCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof EditorPerson person) {
                String display = McFormatCode.strip(person.name);
                label.setText(display.isEmpty() ? "(unnamed)" : display);
            }
            return label;
        }
    }
}
