package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddCategoryCommand;
import net.noiraude.creditseditor.command.impl.MoveCategoryOrderCommand;
import net.noiraude.creditseditor.command.impl.RemoveCategoryCommand;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.McText;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

/**
 * Left-side panel showing the ordered list of categories.
 *
 * <p>
 * The first entry is always a sentinel representing "all persons". Selecting it clears
 * the category filter on the person panel. Selecting a real category both filters the
 * person panel and shows that category's detail form.
 *
 * <p>
 * Structural edits (add, remove, move) are performed via commands issued through the
 * supplied executor. Call {@link #refresh(CreditsDocument, LangDocument)} after any
 * document change.
 */
public final class CategoryPanel extends ListPanel<Object, DocumentCategory> {

    /** Sentinel value that represents the "show all persons" state. */
    private static final Object ALL_SENTINEL = new Object() {};

    private LangDocument langDoc;

    private final JButton upButton = new JButton("▲");
    private final JButton downButton = new JButton("▼");
    private final JButton rolesButton = new JButton("Roles...");

    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected {@link DocumentCategory}, or
     *                           {@code null} when the "All persons" sentinel is selected
     */
    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected {@link DocumentCategory}, or
     *                           {@code null} when the "All persons" sentinel is selected
     * @param onRolesRequested   called when the user clicks the "Roles..." button
     */
    public CategoryPanel(CommandExecutor onCommand, Consumer<DocumentCategory> onSelectionChanged,
        Runnable onRolesRequested) {
        super("Categories", onCommand, onSelectionChanged);

        list.setCellRenderer(new CategoryCellRenderer());

        // Toolbar
        addButton.setToolTipText("Add category");
        removeButton.setToolTipText("Remove category");
        upButton.setToolTipText("Move up");
        downButton.setToolTipText("Move down");
        rolesButton.setToolTipText("Open role editor");

        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());
        upButton.addActionListener(e -> onMove(-1));
        downButton.addActionListener(e -> onMove(+1));
        rolesButton.addActionListener(e -> onRolesRequested.run());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        toolbar.add(upButton);
        toolbar.add(downButton);
        toolbar.add(rolesButton);
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Repopulates the list from {@code creditsDoc}, preserving the selection by category id
     * where possible. {@code langDoc} is used by the cell renderer to resolve display names.
     */
    public void refresh(CreditsDocument creditsDoc, LangDocument langDoc) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
        String prevId = selectedCategoryId();

        refreshing = true;
        try {
            listModel.clear();
            listModel.addElement(ALL_SENTINEL);
            for (DocumentCategory cat : creditsDoc.categories) {
                listModel.addElement(cat);
            }

            // Restore previous selection
            boolean restored = false;
            if (prevId != null) {
                for (int i = 1; i < listModel.size(); i++) {
                    DocumentCategory cat = (DocumentCategory) listModel.get(i);
                    if (cat.id.equals(prevId)) {
                        list.setSelectedIndex(i);
                        restored = true;
                        break;
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
     * Returns the currently selected {@link DocumentCategory}, or {@code null} when
     * the "All persons" sentinel is selected or nothing is selected.
     */
    public DocumentCategory getSelectedCategory() {
        return getSelection();
    }

    @Override
    protected DocumentCategory getSelection() {
        Object sel = list.getSelectedValue();
        return (sel instanceof DocumentCategory dc) ? dc : null;
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
        DocumentCategory cat = getSelectedCategory();
        if (cat == null || creditsDoc == null) return;
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove category '" + cat.id + "'?\nAll memberships in this category will also be removed.",
            "Confirm remove",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            onCommand.execute(new RemoveCategoryCommand(creditsDoc, cat));
        }
    }

    private void onMove(int delta) {
        if (creditsDoc == null) return;
        DocumentCategory cat = getSelectedCategory();
        if (cat == null) return;
        int idx = creditsDoc.categories.indexOf(cat);
        int target = idx + delta;
        if (target < 0 || target >= creditsDoc.categories.size()) return;
        onCommand.execute(new MoveCategoryOrderCommand(creditsDoc, cat, target));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Override
    protected void updateButtons() {
        DocumentCategory cat = getSelectedCategory();
        boolean catSelected = cat != null && creditsDoc != null;
        removeButton.setEnabled(catSelected);
        int idx = catSelected ? creditsDoc.categories.indexOf(cat) : -1;
        upButton.setEnabled(catSelected && idx > 0);
        downButton.setEnabled(catSelected && idx < creditsDoc.categories.size() - 1);
    }

    private String selectedCategoryId() {
        DocumentCategory cat = getSelectedCategory();
        return cat != null ? cat.id : null;
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private final class CategoryCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
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
