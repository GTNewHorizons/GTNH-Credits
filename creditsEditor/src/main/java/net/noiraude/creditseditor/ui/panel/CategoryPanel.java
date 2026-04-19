package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddCategoryCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.MoveCategoriesOrderCommand;
import net.noiraude.creditseditor.command.impl.RemoveCategoryCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.dnd.ListReorderTransferHandler;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Left-side panel showing the ordered list of categories.
 *
 * <p>
 * The first entry is always a sentinel representing "all persons". Selecting it clears the
 * category filter on the person panel. Selecting real categories both filters the person
 * panel to the union of their memberships and, when exactly one is selected, shows that
 * category's detail form. The sentinel is mutually exclusive with any real-category selection.
 *
 * <p>
 * Structural edits (add, remove, move) are performed via commands issued through the supplied
 * executor. Call {@link #refresh(CreditsDocument, LangDocument)} after any document change.
 */
public final class CategoryPanel extends ListPanel<Object, List<DocumentCategory>> {

    /** Sentinel value that represents the "show all persons" state. */
    private static final @NotNull Object ALL_SENTINEL = new Object() {};

    private @Nullable LangDocument langDoc;
    private boolean enforcingSentinel;

    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected real categories (empty when only
     *                           the sentinel or nothing is selected)
     */
    public CategoryPanel(@NotNull CommandExecutor onCommand,
        @NotNull Consumer<List<DocumentCategory>> onSelectionChanged) {
        super("Categories", onCommand, onSelectionChanged);

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new CategoryCellRenderer());
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(
            new ListReorderTransferHandler<>(list, onCommand, this::createReorderCommand, i -> i > 0, i -> i > 0));
        list.addListSelectionListener(this::enforceSentinelExclusive);

        addButton.setToolTipText("Add category");
        removeButton.setToolTipText("Remove category");

        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Repopulates the list from {@code creditsDoc}, preserving the selection by category id
     * where possible. {@code langDoc} is used by the cell renderer to resolve display names.
     */
    public void refresh(@NotNull CreditsDocument creditsDoc, @NotNull LangDocument langDoc) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
        List<String> prevIds = selectedCategoryIds();

        refreshing = true;
        try {
            listModel.clear();
            listModel.addElement(ALL_SENTINEL);
            for (DocumentCategory cat : creditsDoc.categories) {
                listModel.addElement(cat);
            }

            list.clearSelection();
            boolean restored = false;
            if (!prevIds.isEmpty()) {
                for (int i = 1; i < listModel.size(); i++) {
                    DocumentCategory cat = (DocumentCategory) listModel.get(i);
                    if (prevIds.contains(cat.id)) {
                        list.addSelectionInterval(i, i);
                        restored = true;
                    }
                }
            }
            if (!restored) {
                list.setSelectedIndex(0);
            }
        } finally {
            refreshing = false;
        }

        updateButtons();
    }

    /**
     * Returns the list of currently selected real categories (excluding the sentinel).
     * Empty when only the sentinel or nothing is selected.
     */
    @Contract(pure = true)
    public @NotNull List<DocumentCategory> getSelectedCategories() {
        return getSelection();
    }

    @Override
    protected @NotNull List<DocumentCategory> getSelection() {
        List<Object> raw = list.getSelectedValuesList();
        List<DocumentCategory> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o instanceof DocumentCategory dc) out.add(dc);
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (creditsDoc == null) return;
        String id = JOptionPane.showInputDialog(this, "Category ID:", "Add category", JOptionPane.PLAIN_MESSAGE);
        if (id == null || id.isBlank()) return;
        id = id.strip();
        String finalId = id;
        boolean exists = creditsDoc.categories.stream()
            .anyMatch(c -> c.id.equals(finalId));
        if (exists) {
            JOptionPane.showMessageDialog(
                this,
                "A category with id '" + finalId + "' already exists.",
                "Duplicate id",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        onCommand.execute(new AddCategoryCommand(creditsDoc, new DocumentCategory(finalId)));
    }

    private void onRemove() {
        List<DocumentCategory> selected = getSelectedCategories();
        if (selected.isEmpty() || creditsDoc == null) return;
        String message = selected.size() == 1
            ? "Remove category '" + selected.getFirst().id
                + "'?\nAll memberships in this category will also be removed."
            : "Remove " + selected.size() + " categories?\nAll memberships in these categories will also be removed.";
        int confirm = JOptionPane.showConfirmDialog(
            this,
            message,
            "Confirm remove",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        if (selected.size() == 1) {
            onCommand.execute(new RemoveCategoryCommand(creditsDoc, selected.getFirst()));
        } else {
            CompoundCommand.Builder builder = new CompoundCommand.Builder("Remove " + selected.size() + " categories");
            for (DocumentCategory cat : selected) {
                builder.add(new RemoveCategoryCommand(creditsDoc, cat));
            }
            onCommand.execute(builder.build());
        }
    }

    /**
     * Builds a {@link MoveCategoriesOrderCommand} from the UI drag/drop indices produced by
     * {@link ListReorderTransferHandler}. The UI list carries a sentinel at index 0 for
     * "All persons", so UI indices are shifted by 1 relative to {@code creditsDoc.categories}.
     */
    private @Nullable MoveCategoriesOrderCommand createReorderCommand(int @NotNull [] uiFromIndices, int uiDropIndex) {
        if (creditsDoc == null) return null;
        int[] modelIndices = new int[uiFromIndices.length];
        for (int i = 0; i < uiFromIndices.length; i++) {
            int m = uiFromIndices[i] - 1;
            if (m < 0 || m >= creditsDoc.categories.size()) return null;
            modelIndices[i] = m;
        }
        int modelDrop = Math.clamp(uiDropIndex - 1, 0, creditsDoc.categories.size());
        return new MoveCategoriesOrderCommand(creditsDoc, modelIndices, modelDrop);
    }

    // -----------------------------------------------------------------------
    // Sentinel exclusivity
    // -----------------------------------------------------------------------

    /**
     * Keeps the "All persons" sentinel mutually exclusive with any real-category selection.
     * When both are present, the sentinel is removed, so adding categories to an "all" state
     * transitions cleanly to a filtered view.
     */
    private void enforceSentinelExclusive(@NotNull ListSelectionEvent e) {
        if (e.getValueIsAdjusting() || enforcingSentinel || refreshing) return;
        int[] indices = list.getSelectedIndices();
        if (indices.length <= 1 || indices[0] != 0) return;
        enforcingSentinel = true;
        try {
            list.removeSelectionInterval(0, 0);
        } finally {
            enforcingSentinel = false;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Override
    protected void updateButtons() {
        removeButton.setEnabled(!getSelectedCategories().isEmpty() && creditsDoc != null);
    }

    @Contract(pure = true)
    private @NotNull List<String> selectedCategoryIds() {
        List<DocumentCategory> cats = getSelectedCategories();
        List<String> ids = new ArrayList<>(cats.size());
        for (DocumentCategory c : cats) ids.add(c.id);
        return ids;
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private final class CategoryCellRenderer extends DefaultListCellRenderer {

        @Override
        public @NotNull Component getListCellRendererComponent(@NotNull JList<?> list, @Nullable Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == ALL_SENTINEL) {
                label.setText("All persons");
                label.setFont(
                    label.getFont()
                        .deriveFont(java.awt.Font.ITALIC));
            } else if (value instanceof DocumentCategory cat) {
                String langKey = "credits.category." + KeySanitizer.sanitize(cat.id);
                String displayName = langDoc != null ? langDoc.get(langKey) : null;
                if (displayName == null || displayName.isEmpty()) {
                    label.setText("[" + cat.id + "]");
                } else {
                    label.setText(McText.strip(displayName));
                }
            }
            return label;
        }
    }
}
