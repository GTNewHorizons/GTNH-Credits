package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.creditseditor.command.impl.RemoveMembershipCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Form panel that displays and edits the fields of a single {@link DocumentPerson}.
 *
 * <p>
 * Changes are dispatched through the supplied {@link CommandExecutor} rather than mutating
 * the document directly. Call {@link #setContext(CreditsDocument, LangDocument)} once after
 * a session loads, then {@link #load(DocumentPerson)} whenever the active person changes or
 * the document has been refreshed by an undo/redo.
 */
public final class PersonDetailView extends DetailView<DocumentPerson> {

    private @Nullable CreditsDocument creditsDoc;
    private @Nullable LangDocument langDoc;
    private boolean restoringMembershipSelection = false;

    private final @NotNull MinecraftTextEditor nameEditor = new MinecraftTextEditor();
    private final @NotNull MembershipTableModel tableModel = new MembershipTableModel();
    private final @NotNull JTable membershipTable = new JTable(tableModel);
    private final @NotNull MembershipRolePanel rolePanel;

    public PersonDetailView(@NotNull CommandExecutor onCommand) {
        super(onCommand);
        rolePanel = new MembershipRolePanel(onCommand);

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
                onCommand.execute(new DocumentEditCommand("Edit person name", e.getEdit()));
            }
        });

        addMembershipButton.addActionListener(e -> onAddMembership());
        removeMembershipButton.addActionListener(e -> onRemoveMembership());

        membershipTable.getSelectionModel()
            .addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || restoringMembershipSelection) return;
                int row = membershipTable.getSelectedRow();
                if (row >= 0 && current != null) {
                    rolePanel.load(current.memberships.get(row));
                } else {
                    rolePanel.load(null);
                }
            });

        // Row 2: Role panel (spans both columns)
        GridBagConstraints roleGbc = new GridBagConstraints();
        roleGbc.gridy = 2;
        roleGbc.gridx = 0;
        roleGbc.gridwidth = 2;
        roleGbc.fill = GridBagConstraints.BOTH;
        roleGbc.weightx = 1.0;
        roleGbc.weighty = 1.0;
        roleGbc.insets = new Insets(scaled(4), scaled(4), scaled(4), scaled(4));
        add(rolePanel, roleGbc);
    }

    /**
     * Sets the document context used for category lookups and display name resolution.
     * Call once after a session is loaded.
     */
    public void setContext(@NotNull CreditsDocument creditsDoc, @NotNull LangDocument langDoc) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
        rolePanel.setContext(creditsDoc, langDoc);
    }

    /**
     * Populates all fields from {@code person} without firing any commands.
     * Call after any external model change: initial load, undo, or redo.
     */
    public void load(@NotNull DocumentPerson person) {
        String prevCategoryId = selectedMembershipCategoryId();
        current = person;
        loading = true;
        restoringMembershipSelection = true;
        try {
            nameEditor.setText(person.name);
            tableModel.setMemberships(person.memberships);
            restoreMembershipSelection(person, prevCategoryId);
        } finally {
            loading = false;
            restoringMembershipSelection = false;
        }
        // Trigger role panel manually now that the flag is cleared
        int row = membershipTable.getSelectedRow();
        if (row >= 0) {
            rolePanel.load(current.memberships.get(row));
        } else {
            rolePanel.load(null);
        }
    }

    private @Nullable String selectedMembershipCategoryId() {
        int row = membershipTable.getSelectedRow();
        if (row >= 0 && current != null && row < current.memberships.size()) {
            return current.memberships.get(row).categoryId;
        }
        return null;
    }

    private void restoreMembershipSelection(@NotNull DocumentPerson person, @Nullable String categoryId) {
        if (categoryId == null) {
            membershipTable.clearSelection();
            return;
        }
        for (int i = 0; i < person.memberships.size(); i++) {
            if (person.memberships.get(i).categoryId.equals(categoryId)) {
                membershipTable.setRowSelectionInterval(i, i);
                return;
            }
        }
        membershipTable.clearSelection();
    }

    private void onAddMembership() {
        if (current == null || creditsDoc == null) return;

        List<String> used = current.memberships.stream()
            .map(m -> m.categoryId)
            .toList();
        List<DocumentCategory> available = creditsDoc.categories.stream()
            .filter(c -> !used.contains(c.id))
            .toList();

        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                McText.strip(current.name) + " is already a member of all categories.",
                "No categories available",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] labels = available.stream()
            .map(c -> {
                String displayName = langDoc != null ? langDoc.get("credits.category." + KeySanitizer.sanitize(c.id))
                    : null;
                return c.id + (displayName == null || displayName.isEmpty() ? "" : " " + McText.strip(displayName));
            })
            .toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(labels);
        int result = JOptionPane
            .showConfirmDialog(this, combo, "Add membership", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        DocumentCategory chosen = available.get(combo.getSelectedIndex());
        onCommand.execute(new AddMembershipCommand(current, new DocumentMembership(chosen.id)));
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
        DocumentMembership membership = current.memberships.get(row);
        onCommand.execute(new RemoveMembershipCommand(current, membership));
    }

    // -----------------------------------------------------------------------
    // Membership table model
    // -----------------------------------------------------------------------

    private static final class MembershipTableModel extends AbstractTableModel {

        private static final @NotNull String @NotNull [] COLUMNS = { "Category", "Roles" };
        private @NotNull List<DocumentMembership> memberships = List.of();

        void setMemberships(@NotNull List<DocumentMembership> memberships) {
            this.memberships = memberships;
            fireTableDataChanged();
        }

        @Contract(pure = true)
        @Override
        public int getRowCount() {
            return memberships.size();
        }

        @Contract(pure = true)
        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public @NotNull Object getValueAt(int row, int col) {
            DocumentMembership m = memberships.get(row);
            return switch (col) {
                case 0 -> m.categoryId;
                case 1 -> String.join(", ", m.roles);
                default -> "";
            };
        }
    }
}
