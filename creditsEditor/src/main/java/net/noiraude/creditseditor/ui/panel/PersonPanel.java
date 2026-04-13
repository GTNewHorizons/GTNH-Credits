package net.noiraude.creditseditor.ui.panel;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddPersonCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.RemovePersonCommand;
import net.noiraude.creditseditor.ui.component.AnyChangeListener;
import net.noiraude.creditseditor.ui.component.McText;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Middle panel showing the list of persons, optionally filtered by category.
 *
 * <p>
 * Supports multi-selection. The selection callback receives the full list of selected
 * persons (empty list when nothing is selected). Call {@link #refresh(CreditsDocument)}
 * after any document change. Call {@link #setFilter(DocumentCategory)} to restrict the
 * visible set to members of a specific category; pass {@code null} to show all persons.
 */
public final class PersonPanel extends ListPanel<DocumentPerson, List<DocumentPerson>> {

    private DocumentCategory filter;
    private final Consumer<List<DocumentPerson>> selectionCallback;

    private final JTextField searchField = new JTextField();

    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected persons (empty list when cleared)
     */
    public PersonPanel(CommandExecutor onCommand, Consumer<List<DocumentPerson>> onSelectionChanged) {
        super("Persons", onCommand, onSelectionChanged);
        this.selectionCallback = onSelectionChanged;
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new PersonCellRenderer());

        // Search bar
        searchField.putClientProperty("JTextField.placeholderText", "Filter by name...");
        searchField.getDocument()
            .addDocumentListener(new AnyChangeListener(this::applyFilter));
        JPanel searchRow = new JPanel(new BorderLayout(scaled(4), 0));
        searchRow.setBorder(BorderFactory.createEmptyBorder(scaled(2), scaled(2), scaled(2), scaled(2)));
        searchRow.add(new JLabel("Search:"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        add(searchRow, BorderLayout.NORTH);

        // Toolbar
        addButton.setToolTipText("Add person");
        removeButton.setToolTipText("Remove person");
        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, scaled(4), scaled(2)));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Changes the active category filter and repopulates the list.
     * Pass {@code null} to show all persons.
     */
    public void setFilter(DocumentCategory category) {
        filter = category;
        applyFilter();
    }

    /**
     * Repopulates the list from the document, respecting the current filter and search term.
     * Preserves the selection by name where possible.
     */
    public void refresh(CreditsDocument creditsDoc) {
        this.creditsDoc = creditsDoc;
        refreshing = true;
        try {
            applyFilter();
        } finally {
            refreshing = false;
        }
    }

    @Override
    protected List<DocumentPerson> getSelection() {
        return list.getSelectedValuesList();
    }

    // -----------------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (creditsDoc == null) return;
        String name = JOptionPane.showInputDialog(this, "Person name:", "Add person", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        onCommand.execute(new AddPersonCommand(creditsDoc, new DocumentPerson(name.strip())));
    }

    private void onRemove() {
        List<DocumentPerson> selected = list.getSelectedValuesList();
        if (selected.isEmpty() || creditsDoc == null) return;

        String message = selected.size() == 1 ? "Remove '" + McText.strip(selected.getFirst().name) + "'?"
            : "Remove " + selected.size() + " person(s)?";
        int confirm = JOptionPane.showConfirmDialog(
            this,
            message,
            "Confirm remove",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        if (selected.size() == 1) {
            onCommand.execute(new RemovePersonCommand(creditsDoc, selected.getFirst()));
        } else {
            CompoundCommand.Builder builder = new CompoundCommand.Builder("Remove " + selected.size() + " person(s)");
            for (DocumentPerson person : selected) {
                builder.add(new RemovePersonCommand(creditsDoc, person));
            }
            onCommand.execute(builder.build());
        }
    }

    // -----------------------------------------------------------------------
    // Filter / population
    // -----------------------------------------------------------------------

    private void applyFilter() {
        if (creditsDoc == null) return;

        String prevName = list.getSelectedValue() != null ? list.getSelectedValue().name : null;
        String search = searchField.getText()
            .toLowerCase();

        List<DocumentPerson> visible = creditsDoc.persons.stream()
            .filter(p -> passesFilter(p, search))
            .toList();

        listModel.clear();
        for (DocumentPerson p : visible) {
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

    private boolean passesFilter(DocumentPerson person, String search) {
        if (filter != null) {
            boolean member = person.memberships.stream()
                .anyMatch(m -> m.categoryId.equals(filter.id));
            if (!member) return false;
        }
        if (!search.isEmpty()) {
            return McText.strip(person.name)
                .toLowerCase()
                .contains(search);
        }
        return true;
    }

    @Override
    protected void updateButtons() {
        removeButton.setEnabled(
            !list.getSelectedValuesList()
                .isEmpty() && creditsDoc != null);
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private static final class PersonCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DocumentPerson person) {
                String display = McText.strip(person.name);
                label.setText(display.isEmpty() ? "(unnamed)" : display);
            }
            return label;
        }
    }
}
