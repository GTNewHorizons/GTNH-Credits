package net.noiraude.creditseditor.ui.detail;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
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
public final class PersonDetailView extends DetailView<EditorPerson> {

    private EditorModel model;

    private final MinecraftTextEditor nameEditor = new MinecraftTextEditor();
    private final MembershipTableModel tableModel = new MembershipTableModel();
    private final JTable membershipTable = new JTable(tableModel);

    public PersonDetailView(Consumer<Command> onCommand) {
        super(onCommand);

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
        JButton addMembershipButton = new JButton("Add");
        membershipToolbar.add(addMembershipButton);
        JButton removeMembershipButton = new JButton("Remove");
        membershipToolbar.add(removeMembershipButton);
        membershipPanel.add(membershipToolbar, BorderLayout.SOUTH);
        add(membershipPanel, field);

        // Wire events
        nameEditor.addPropertyChangeListener("text", e -> {
            if (!loading && current != null) {
                current.name = (String) e.getNewValue();
            }
        });
        nameEditor.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.accept(new DocumentEditCommand("Edit person name", e.getEdit()));
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
            .toList();
        List<EditorCategory> available = model.categories.stream()
            .filter(c -> !used.contains(c.id))
            .toList();

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
}
