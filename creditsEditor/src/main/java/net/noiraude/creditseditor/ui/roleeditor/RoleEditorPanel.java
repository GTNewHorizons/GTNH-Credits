package net.noiraude.creditseditor.ui.roleeditor;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.RenameRoleCommand;
import net.noiraude.creditseditor.service.RoleIndex;
import net.noiraude.creditseditor.ui.component.McText;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;

/**
 * Panel displaying all roles in a sortable table with inline display-name editing and
 * a role-rename refactoring action.
 *
 * <p>
 * Columns: raw role value (read-only), lang key (read-only), display name (editable),
 * categories (read-only), person count (read-only).
 */
public final class RoleEditorPanel extends JPanel {

    private final CommandExecutor onCommand;
    private CreditsDocument creditsDoc;
    private LangDocument langDoc;

    private final RoleTableModel tableModel = new RoleTableModel();
    private final JTable table = new JTable(tableModel);
    private final JButton renameButton = new JButton("Rename...");
    private final JButton closeButton = new JButton("Close");

    private Runnable onClose;

    public RoleEditorPanel(CommandExecutor onCommand) {
        this.onCommand = onCommand;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Role Editor"));

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel()
            .addListSelectionListener(e -> updateButtons());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, scaled(4), scaled(4)));
        renameButton.setToolTipText("Rename the selected role across all persons");
        renameButton.addActionListener(e -> onRename());
        toolbar.add(renameButton);
        toolbar.add(closeButton);
        closeButton.addActionListener(e -> { if (onClose != null) onClose.run(); });
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Sets the callback invoked when the user clicks "Close".
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Sets the document context. Call once after a session is loaded.
     */
    public void setContext(CreditsDocument creditsDoc, LangDocument langDoc) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
    }

    /**
     * Rebuilds the role index and refreshes the table. Call after any document change.
     */
    public void refresh() {
        if (creditsDoc == null) return;
        RoleIndex index = RoleIndex.build(creditsDoc);
        tableModel.setEntries(index.entries());
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private void onRename() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || creditsDoc == null) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        RoleIndex.Entry entry = tableModel.getEntry(modelRow);

        String target = (String) JOptionPane.showInputDialog(
            this,
            "Rename '" + entry.raw + "' to:",
            "Rename role",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            entry.raw);
        if (
            target == null || target.isBlank()
                || target.strip()
                    .equals(entry.raw)
        ) return;
        target = target.strip();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            entry.count + " person(s) will be updated. Continue?",
            "Confirm rename",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        onCommand.execute(new RenameRoleCommand(creditsDoc, langDoc, entry.raw, target));
    }

    private void updateButtons() {
        renameButton.setEnabled(table.getSelectedRow() >= 0);
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private final class RoleTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = { "Role (raw)", "Lang key", "Display name", "Categories", "Persons" };
        private List<RoleIndex.Entry> entries = List.of();

        void setEntries(List<RoleIndex.Entry> entries) {
            this.entries = entries;
            fireTableDataChanged();
        }

        RoleIndex.Entry getEntry(int row) {
            return entries.get(row);
        }

        @Override
        public int getRowCount() {
            return entries.size();
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
        public Class<?> getColumnClass(int col) {
            if (col == 4) return Integer.class;
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 2;
        }

        @Override
        public Object getValueAt(int row, int col) {
            RoleIndex.Entry e = entries.get(row);
            return switch (col) {
                case 0 -> e.raw;
                case 1 -> e.langKey;
                case 2 -> resolveDisplayName(e);
                case 3 -> String.join(", ", e.categoryIds);
                case 4 -> e.count;
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != 2 || langDoc == null) return;
            RoleIndex.Entry e = entries.get(row);
            String newValue = ((String) value).strip();
            String key = e.langKey;

            if (newValue.isEmpty()) {
                langDoc.remove(key);
            } else {
                langDoc.set(key, newValue);
            }
            fireTableCellUpdated(row, col);
        }

        private String resolveDisplayName(RoleIndex.Entry e) {
            if (langDoc == null) return e.raw;
            String display = langDoc.get(e.langKey);
            if (display == null || display.isEmpty()) return e.raw;
            return McText.strip(display);
        }
    }
}
