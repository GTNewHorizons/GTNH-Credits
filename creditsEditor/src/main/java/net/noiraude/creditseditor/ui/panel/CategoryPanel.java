package net.noiraude.creditseditor.ui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.AddCategoryCommand;
import net.noiraude.creditseditor.command.impl.MoveCategoryOrderCommand;
import net.noiraude.creditseditor.command.impl.RemoveCategoryCommand;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.ui.component.McFormatCode;

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
 * supplied executor. Call {@link #refresh(EditorModel)} after any model change.
 */
public final class CategoryPanel extends JPanel {

    /** Sentinel value that represents the "show all persons" state. */
    private static final Object ALL_SENTINEL = new Object() {};

    private final Consumer<Command> onCommand;
    private final Consumer<EditorCategory> onSelectionChanged;
    private EditorModel model;

    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(listModel);

    private final JButton addButton = new JButton("+");
    private final JButton removeButton = new JButton("−");
    private final JButton upButton = new JButton("▲");
    private final JButton downButton = new JButton("▼");

    /**
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected {@link EditorCategory}, or
     *                           {@code null} when the "All persons" sentinel is selected
     */
    public CategoryPanel(Consumer<Command> onCommand, Consumer<EditorCategory> onSelectionChanged) {
        this.onCommand = onCommand;
        this.onSelectionChanged = onSelectionChanged;
        setLayout(new BorderLayout());

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CategoryCellRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
                onSelectionChanged.accept(getSelectedCategory());
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);

        // Toolbar
        addButton.setToolTipText("Add category");
        removeButton.setToolTipText("Remove category");
        upButton.setToolTipText("Move up");
        downButton.setToolTipText("Move down");

        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());
        upButton.addActionListener(e -> onMove(-1));
        downButton.addActionListener(e -> onMove(+1));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        toolbar.add(upButton);
        toolbar.add(downButton);
        add(toolbar, BorderLayout.SOUTH);

        updateButtons();
    }

    /**
     * Repopulates the list from {@code model}, preserving the selection by category id
     * where possible.
     */
    public void refresh(EditorModel model) {
        this.model = model;

        String prevId = selectedCategoryId();

        listModel.clear();
        listModel.addElement(ALL_SENTINEL);
        for (EditorCategory cat : model.categories) {
            listModel.addElement(cat);
        }

        // Restore previous selection
        boolean restored = false;
        if (prevId != null) {
            for (int i = 1; i < listModel.size(); i++) {
                EditorCategory cat = (EditorCategory) listModel.get(i);
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

        updateButtons();
    }

    /** Returns the underlying list component, for targeted repaint after field edits. */
    public JList<Object> getList() {
        return list;
    }

    /**
     * Returns the currently selected {@link EditorCategory}, or {@code null} when
     * the "All persons" sentinel is selected or nothing is selected.
     */
    public EditorCategory getSelectedCategory() {
        Object sel = list.getSelectedValue();
        return (sel instanceof EditorCategory ec) ? ec : null;
    }

    // -----------------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (model == null) return;
        String id = JOptionPane.showInputDialog(this, "Category ID:", "Add category", JOptionPane.PLAIN_MESSAGE);
        if (id == null || id.isBlank()) return;
        id = id.strip();
        String finalId = id;
        boolean exists = model.categories.stream()
            .anyMatch(c -> c.id.equals(finalId));
        if (exists) {
            JOptionPane.showMessageDialog(
                this,
                "A category with id '" + finalId + "' already exists.",
                "Duplicate id",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        onCommand.accept(new AddCategoryCommand(model, new EditorCategory(finalId)));
    }

    private void onRemove() {
        EditorCategory cat = getSelectedCategory();
        if (cat == null || model == null) return;
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove category '" + cat.id + "'?\nAll memberships in this category will also be removed.",
            "Confirm remove",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            onCommand.accept(new RemoveCategoryCommand(model, cat));
        }
    }

    private void onMove(int delta) {
        if (model == null) return;
        EditorCategory cat = getSelectedCategory();
        if (cat == null) return;
        int idx = model.categories.indexOf(cat);
        int target = idx + delta;
        if (target < 0 || target >= model.categories.size()) return;
        onCommand.accept(new MoveCategoryOrderCommand(model, cat, target));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void updateButtons() {
        EditorCategory cat = getSelectedCategory();
        boolean catSelected = cat != null && model != null;
        removeButton.setEnabled(catSelected);
        int idx = catSelected ? model.categories.indexOf(cat) : -1;
        upButton.setEnabled(catSelected && idx > 0);
        downButton.setEnabled(catSelected && idx < model.categories.size() - 1);
    }

    private String selectedCategoryId() {
        EditorCategory cat = getSelectedCategory();
        return cat != null ? cat.id : null;
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private static final class CategoryCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == ALL_SENTINEL) {
                label.setText("All persons");
                label.setFont(
                    label.getFont()
                        .deriveFont(java.awt.Font.ITALIC));
            } else if (value instanceof EditorCategory cat) {
                String display = cat.displayName.isEmpty() ? cat.id : McFormatCode.strip(cat.displayName);
                if (cat.displayName.isEmpty()) {
                    label.setText("[" + display + "]");
                } else {
                    label.setText(display);
                }
            }
            return label;
        }
    }
}
