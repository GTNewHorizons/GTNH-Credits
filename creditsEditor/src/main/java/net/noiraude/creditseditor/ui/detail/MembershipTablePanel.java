package net.noiraude.creditseditor.ui.detail;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Memberships table with Add and Remove toolbar buttons.
 *
 * <p>
 * Owns the table, its data model, and the selection-suppression flag used when reloading
 * the table contents so a programmatic selection change does not fire listener callbacks.
 * Add and Remove actions are relayed to the owner through the callbacks supplied at
 * construction.
 */
public final class MembershipTablePanel extends JPanel {

    private static final @NotNull String @NotNull [] COLUMNS = { I18n.get("table.membership.column.category"),
        I18n.get("table.membership.column.roles") };

    private static final int MIN_VISIBLE_ROWS = 2;

    private final @NotNull MembershipTableModel model = new MembershipTableModel();
    private final @NotNull JTable table = new JTable(model);
    private final @NotNull JScrollPane scroll;
    private final @NotNull JPanel toolbar;
    private final @NotNull List<IntConsumer> rowSelectionListeners = new ArrayList<>();
    private boolean suppressSelectionEvents = false;

    public MembershipTablePanel(@NotNull Runnable onAdd, @NotNull Runnable onRemove) {
        super(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel()
            .getColumn(0)
            .setPreferredWidth(120);
        table.getColumnModel()
            .getColumn(1)
            .setPreferredWidth(280);
        // Pin the scrollable viewport so the table reports a preferred height large enough
        // for MIN_VISIBLE_ROWS rows even when the model is empty; getMinimumSize() below
        // turns that same height into an enforced floor.
        table.setPreferredScrollableViewportSize(new Dimension(400, table.getRowHeight() * MIN_VISIBLE_ROWS));
        scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);

        toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        JButton addButton = new JButton(I18n.get("button.add"));
        JButton removeButton = new JButton(I18n.get("button.remove"));
        addButton.addActionListener(e -> onAdd.run());
        removeButton.addActionListener(e -> onRemove.run());
        toolbar.add(addButton);
        toolbar.add(removeButton);
        add(toolbar, BorderLayout.SOUTH);

        table.getSelectionModel()
            .addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || suppressSelectionEvents) return;
                int row = table.getSelectedRow();
                for (IntConsumer l : rowSelectionListeners) l.accept(row);
            });
    }

    @Contract(pure = true)
    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    /**
     * Ensures the table always has room for at least {@value #MIN_VISIBLE_ROWS} rows plus its
     * header and that the Add/Remove toolbar stays fully visible when this panel is placed in
     * a shrinking layout.
     */
    @Override
    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        JTableHeader header = table.getTableHeader();
        int headerHeight = header != null ? header.getPreferredSize().height : 0;
        int rowsHeight = table.getRowHeight() * MIN_VISIBLE_ROWS;
        Insets scrollInsets = scroll.getInsets();
        int scrollMin = headerHeight + rowsHeight + scrollInsets.top + scrollInsets.bottom;
        int toolbarMin = toolbar.getPreferredSize().height;
        return new Dimension(min.width, Math.max(min.height, scrollMin + toolbarMin));
    }

    public void addRowSelectionListener(@NotNull IntConsumer listener) {
        rowSelectionListeners.add(listener);
    }

    /**
     * Replaces the table contents and restores selection to the row whose membership has
     * {@code previousCategoryId}, suppressing selection events throughout. If no row matches,
     * the selection is cleared.
     */
    public void setMembershipsPreservingSelection(@NotNull List<DocumentMembership> memberships,
        @Nullable String previousCategoryId) {
        suppressSelectionEvents = true;
        try {
            model.setMemberships(memberships);
            if (previousCategoryId == null) {
                table.clearSelection();
                return;
            }
            for (int i = 0; i < memberships.size(); i++) {
                if (memberships.get(i).categoryId.equals(previousCategoryId)) {
                    table.setRowSelectionInterval(i, i);
                    return;
                }
            }
            table.clearSelection();
        } finally {
            suppressSelectionEvents = false;
        }
    }

    private static final class MembershipTableModel extends AbstractTableModel {

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
