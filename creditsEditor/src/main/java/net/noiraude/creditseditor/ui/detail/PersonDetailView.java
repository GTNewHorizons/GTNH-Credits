package net.noiraude.creditseditor.ui.detail;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.command.impl.RemoveMembershipCommand;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.creditseditor.ui.component.McFormatCode;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;

/**
 * Form panel that displays and edits the fields of a single {@link EditorPerson}.
 *
 * <p>
 * Changes fire {@link Command} objects through the supplied executor rather than mutating
 * the model directly. Call {@link #load(EditorPerson, EditorModel)} whenever the active
 * person changes or the model has been refreshed by an undo/redo.
 */
public final class PersonDetailView extends JPanel {

    private final Consumer<Command> onCommand;
    private EditorPerson current;
    private EditorModel model;
    private boolean loading;

    private final MinecraftTextEditor nameEditor = new MinecraftTextEditor();
    private final MembershipTableModel tableModel = new MembershipTableModel();
    private final JTable membershipTable = new JTable(tableModel);
    private final JButton addMembershipButton = new JButton("Add");
    private final JButton removeMembershipButton = new JButton("Remove");

    public PersonDetailView(Consumer<Command> onCommand) {
        this.onCommand = onCommand;
        setLayout(new GridBagLayout());

        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();

        // Row 0: Name
        label.gridy = 0;
        add(new JLabel("Name:"), label);
        field.gridy = 0;
        add(nameEditor, field);

        // Row 1: Memberships label
        label.gridy = 1;
        label.anchor = GridBagConstraints.NORTHWEST;
        add(new JLabel("Memberships:"), label);

        // Row 1: Memberships table + toolbar
        field.gridy = 1;
        field.fill = GridBagConstraints.BOTH;
        field.weighty = 1.0;
        membershipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        membershipTable.getColumnModel()
            .getColumn(0)
            .setPreferredWidth(120);
        membershipTable.getColumnModel()
            .getColumn(1)
            .setPreferredWidth(280);
        JScrollPane tableScroll = new JScrollPane(membershipTable);
        tableScroll.setBorder(BorderFactory.createEtchedBorder());

        JPanel membershipPanel = new JPanel(new BorderLayout());
        membershipPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel membershipToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        membershipToolbar.add(addMembershipButton);
        membershipToolbar.add(removeMembershipButton);
        membershipPanel.add(membershipToolbar, BorderLayout.SOUTH);
        add(membershipPanel, field);

        // Wire events
        nameEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null) {
                String newVal = (String) e.getNewValue();
                if (!newVal.equals(current.name)) {
                    onCommand.accept(
                        new EditFieldCommand<>("Edit person name", () -> current.name, v -> current.name = v, newVal));
                }
            }
        });

        addMembershipButton.addActionListener(e -> onAddMembership());
        removeMembershipButton.addActionListener(e -> onRemoveMembership());
    }

    /**
     * Populates all fields from {@code person} without firing any commands.
     * Call after any external model change (undo, redo, or initial load).
     */
    public void load(EditorPerson person, EditorModel model) {
        current = person;
        this.model = model;
        loading = true;
        try {
            nameEditor.setText(person.name);
            tableModel.setMemberships(person.memberships);
        } finally {
            loading = false;
        }
    }

    private void onAddMembership() {
        if (current == null || model == null) return;

        List<String> used = current.memberships.stream()
            .map(m -> m.categoryId)
            .collect(Collectors.toList());
        List<EditorCategory> available = model.categories.stream()
            .filter(c -> !used.contains(c.id))
            .collect(Collectors.toList());

        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                McFormatCode.strip(current.name) + " is already a member of all categories.",
                "No categories available",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] labels = available.stream()
            .map(c -> c.id + (c.displayName.isEmpty() ? "" : " " + McFormatCode.strip(c.displayName)))
            .toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(labels);
        int result = JOptionPane
            .showConfirmDialog(this, combo, "Add membership", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        EditorCategory chosen = available.get(combo.getSelectedIndex());
        onCommand.accept(new AddMembershipCommand(current, new EditorMembership(chosen.id)));
    }

    private void onRemoveMembership() {
        if (current == null) return;
        int row = membershipTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Select a membership row to remove.",
                "Nothing selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        EditorMembership membership = current.memberships.get(row);
        onCommand.accept(new RemoveMembershipCommand(current, membership));
    }

    // -----------------------------------------------------------------------
    // Membership table model
    // -----------------------------------------------------------------------

    private static final class MembershipTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = { "Category", "Roles" };
        private List<EditorMembership> memberships = List.of();

        void setMemberships(List<EditorMembership> memberships) {
            this.memberships = memberships;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return memberships.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            EditorMembership m = memberships.get(row);
            return switch (col) {
                case 0 -> m.categoryId;
                case 1 -> String.join(", ", m.roles);
                default -> "";
            };
        }
    }

    // -----------------------------------------------------------------------
    // Layout helpers
    // -----------------------------------------------------------------------

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(4, 6, 4, 4);
        return c;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0;
        c.insets = new Insets(4, 0, 4, 6);
        return c;
    }
}
