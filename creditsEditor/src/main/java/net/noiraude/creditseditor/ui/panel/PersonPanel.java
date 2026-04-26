package net.noiraude.creditseditor.ui.panel;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddPersonCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.RemovePersonCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.ui.component.AnyChangeListener;
import net.noiraude.creditseditor.ui.dialog.ImportTsvDialog;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.util.PersonSortKey;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Middle panel showing the list of persons, optionally filtered by category.
 *
 * <p>
 * Supports multi-selection. The selection callback receives the full list of selected
 * persons (empty list when nothing is selected). Subscribes to the document bus: rebuilds
 * from {@code TOPIC_SESSION} and {@code TOPIC_PERSONS}, repaints cells (without a rebuild)
 * for {@code TOPIC_PERSON} so light edits don't disturb selection or scroll.
 * {@link #setFilter(List)} restricts the visible set to the union of memberships across
 * the supplied categories; pass an empty list to show all persons.
 */
public final class PersonPanel extends ListPanel<DocumentPerson, List<DocumentPerson>> {

    private final @NotNull DocumentBus bus;
    private @NotNull List<DocumentCategory> filters = List.of();

    private final @NotNull JTextField searchField = new JTextField();

    /**
     * @param bus                event bus to subscribe to and pass to commands
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected persons (empty list when cleared)
     */
    public PersonPanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand,
        @NotNull Consumer<List<DocumentPerson>> onSelectionChanged) {
        super("Persons", onCommand, onSelectionChanged);
        this.bus = bus;
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new PersonCellRenderer());

        // Search bar
        searchField.putClientProperty("JTextField.placeholderText", "Filter by name...");
        searchField.getDocument()
            .addDocumentListener(new AnyChangeListener(this::applyFilter));
        JPanel searchRow = new JPanel(new BorderLayout(gapSmall, 0));
        searchRow.setBorder(BorderFactory.createEmptyBorder(gapTiny, gapTiny, gapTiny, gapTiny));
        searchRow.add(new JLabel("Search:"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        add(searchRow, BorderLayout.NORTH);

        // Toolbar
        addButton.setToolTipText("Add person");
        removeButton.setToolTipText("Remove person");
        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());

        JButton importButton = new JButton("Import");
        importButton.setToolTipText("Import persons from a TSV file");
        importButton.setMinimumSize(importButton.getPreferredSize());
        importButton.addActionListener(e -> onImportTsv());

        JPanel leftButtons = new JPanel(new GridLayout(1, 0, gapSmall, 0));
        leftButtons.add(addButton);
        leftButtons.add(removeButton);

        JPanel toolbar = new JPanel(new BorderLayout(gapSmall, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(gapTiny, gapTiny, gapTiny, gapTiny));
        toolbar.add(leftButtons, BorderLayout.WEST);
        toolbar.add(importButton, BorderLayout.EAST);
        add(toolbar, BorderLayout.SOUTH);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_PERSONS, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_PERSON, e -> list.repaint());
        bus.addListener(DocumentBus.TOPIC_CATEGORIES, e -> onCategoriesChanged());

        updateButtons();
    }

    /**
     * Changes the active category filter and repopulates the list. The visible set becomes
     * the union of persons who are members of the supplied categories. Pass an empty
     * list to show all persons.
     */
    public void setFilter(@NotNull List<DocumentCategory> categories) {
        filters = List.copyOf(categories);
        applyFilter();
    }

    private void rebuild() {
        refreshing = true;
        try {
            applyFilter();
        } finally {
            refreshing = false;
        }
    }

    private void onCategoriesChanged() {
        if (!bus.hasSession() || filters.isEmpty()) return;
        // Drop any filter categories that no longer exist in the document.
        List<DocumentCategory> surviving = filters.stream()
            .filter(
                f -> bus.creditsDoc().categories.stream()
                    .anyMatch(c -> c.id.equals(f.id)))
            .toList();
        if (surviving.size() != filters.size()) {
            filters = List.copyOf(surviving);
            applyFilter();
        }
    }

    @Override
    protected @NotNull List<DocumentPerson> getSelection() {
        return list.getSelectedValuesList();
    }

    // -----------------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (!bus.hasSession()) return;
        String name = JOptionPane.showInputDialog(this, "Person name:", "Add person", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        onCommand.execute(new AddPersonCommand(bus, new DocumentPerson(name.strip())));
    }

    private void onRemove() {
        List<DocumentPerson> selected = list.getSelectedValuesList();
        if (selected.isEmpty() || !bus.hasSession()) return;

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
            onCommand.execute(new RemovePersonCommand(bus, selected.getFirst()));
        } else {
            CompoundCommand.Builder builder = new CompoundCommand.Builder("Remove " + selected.size() + " person(s)");
            for (DocumentPerson person : selected) {
                builder.add(new RemovePersonCommand(bus, person));
            }
            onCommand.execute(builder.build());
        }
    }

    private void onImportTsv() {
        if (!bus.hasSession()) return;
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        String defaultCategoryId = filters.size() == 1 ? filters.getFirst().id : null;
        ImportTsvDialog dialog = new ImportTsvDialog(owner, bus, onCommand, defaultCategoryId);
        dialog.setVisible(true);
    }

    // -----------------------------------------------------------------------
    // Filter / population
    // -----------------------------------------------------------------------

    private void applyFilter() {
        if (!bus.hasSession()) return;

        String prevName = list.getSelectedValue() != null ? list.getSelectedValue().name : null;
        String search = searchField.getText()
            .toLowerCase();

        List<DocumentPerson> visible = bus.creditsDoc().persons.stream()
            .filter(p -> passesFilter(p, search))
            .sorted(Comparator.comparing(p -> PersonSortKey.of(p.name)))
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

    private boolean passesFilter(@NotNull DocumentPerson person, @NotNull String search) {
        if (!filters.isEmpty()) {
            boolean member = person.memberships.stream()
                .anyMatch(
                    m -> filters.stream()
                        .anyMatch(f -> f.id.equals(m.categoryId)));
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
                .isEmpty() && bus.hasSession());
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private static final class PersonCellRenderer extends DefaultListCellRenderer {

        @Override
        public @NotNull Component getListCellRendererComponent(@NotNull JList<?> list, @Nullable Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DocumentPerson person) {
                String display = McText.strip(person.name);
                label.setText(display.isEmpty() ? "(unnamed)" : display);
            }
            return label;
        }
    }
}
