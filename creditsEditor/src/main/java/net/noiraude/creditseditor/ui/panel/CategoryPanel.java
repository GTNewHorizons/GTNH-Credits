package net.noiraude.creditseditor.ui.panel;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddCategoryCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.MoveCategoriesOrderCommand;
import net.noiraude.creditseditor.command.impl.RemoveCategoryCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.KeyHintLabel;
import net.noiraude.creditseditor.ui.component.dnd.ListReorderTransferHandler;
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
 * Subscribes to the document bus at construction: rebuilds from {@code TOPIC_SESSION} and
 * {@code TOPIC_CATEGORIES}, and repaints cells (without a rebuild) for {@code TOPIC_CATEGORY}
 * and {@code TOPIC_LANG}. Structural edits are issued as commands through the supplied executor.
 */
public final class CategoryPanel extends ListPanel<Object, List<DocumentCategory>> {

    /** Sentinel value that represents the "show all persons" state. */
    private static final @NotNull Object ALL_SENTINEL = new Object() {};

    private final @NotNull DocumentBus bus;
    private boolean enforcingSentinel;

    /**
     * @param bus                event bus to subscribe to and pass to commands
     * @param onCommand          receives each structural command to execute
     * @param onSelectionChanged called with the selected real categories (empty when only
     *                           the sentinel or nothing is selected)
     */
    public CategoryPanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand,
        @NotNull Consumer<List<DocumentCategory>> onSelectionChanged) {
        super(I18n.get("panel.categories.title"), onCommand, onSelectionChanged);
        this.bus = bus;

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new CategoryCellRenderer());
        list.getAccessibleContext()
            .setAccessibleName(I18n.get("panel.categories.list.accessible"));
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(
            new ListReorderTransferHandler<>(list, onCommand, this::createReorderCommand, i -> i > 0, i -> i > 0));
        list.addListSelectionListener(this::enforceSentinelExclusive);

        addButton.setToolTipText(I18n.get("panel.categories.add.tooltip"));
        removeButton.setToolTipText(I18n.get("panel.categories.remove.tooltip"));

        addButton.addActionListener(e -> onAdd());
        removeButton.addActionListener(e -> onRemove());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(addButton);
        toolbar.add(removeButton);
        add(toolbar, BorderLayout.SOUTH);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_CATEGORIES, e -> rebuild());
        bus.addListener(DocumentBus.TOPIC_CATEGORY, e -> list.repaint());
        bus.addListener(DocumentBus.TOPIC_LANG, e -> list.repaint());

        updateButtons();
    }

    private void rebuild() {
        if (!bus.hasSession()) return;
        CreditsDocument creditsDoc = bus.creditsDoc();
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
        if (!bus.hasSession()) return;
        CreditsDocument creditsDoc = bus.creditsDoc();

        JTextField idField = new JTextField(20);
        KeyHintLabel hint = new KeyHintLabel(idField);
        JPanel panel = new JPanel(new BorderLayout(0, gapSmall));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, gapSmall, 0));
        panel.add(new JLabel(I18n.get("panel.categories.add.prompt")), BorderLayout.NORTH);
        panel.add(idField, BorderLayout.CENTER);
        panel.add(hint, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            I18n.get("panel.categories.add.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String id = idField.getText();
        if (id == null || id.isBlank()) return;
        id = id.strip();
        String finalId = id;
        boolean exists = creditsDoc.categories.stream()
            .anyMatch(c -> c.id.equals(finalId));
        if (exists) {
            JOptionPane.showMessageDialog(
                this,
                I18n.get("panel.categories.duplicate.message", finalId),
                I18n.get("panel.categories.duplicate.title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        onCommand.execute(new AddCategoryCommand(bus, new DocumentCategory(finalId)));
    }

    private void onRemove() {
        List<DocumentCategory> selected = getSelectedCategories();
        if (selected.isEmpty() || !bus.hasSession()) return;
        String message = selected.size() == 1
            ? I18n.get("panel.categories.remove.confirm.single", selected.getFirst().id)
            : I18n.get("panel.categories.remove.confirm.multiple", selected.size());
        int confirm = JOptionPane.showConfirmDialog(
            this,
            message,
            I18n.get("panel.categories.remove.confirm.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        if (selected.size() == 1) {
            onCommand.execute(new RemoveCategoryCommand(bus, selected.getFirst()));
        } else {
            CompoundCommand.Builder builder = new CompoundCommand.Builder(
                I18n.get("command.remove.categories", selected.size()));
            for (DocumentCategory cat : selected) {
                builder.add(new RemoveCategoryCommand(bus, cat));
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
        if (!bus.hasSession()) return null;
        CreditsDocument creditsDoc = bus.creditsDoc();
        int[] modelIndices = new int[uiFromIndices.length];
        for (int i = 0; i < uiFromIndices.length; i++) {
            int m = uiFromIndices[i] - 1;
            if (m < 0 || m >= creditsDoc.categories.size()) return null;
            modelIndices[i] = m;
        }
        int modelDrop = Math.clamp(uiDropIndex - 1, 0, creditsDoc.categories.size());
        return new MoveCategoriesOrderCommand(bus, modelIndices, modelDrop);
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
        removeButton.setEnabled(!getSelectedCategories().isEmpty() && bus.hasSession());
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
                label.setText(I18n.get("panel.categories.all"));
                label.setFont(
                    label.getFont()
                        .deriveFont(java.awt.Font.ITALIC));
            } else if (value instanceof DocumentCategory cat) {
                String langKey = "credits.category." + KeySanitizer.sanitize(cat.id);
                String displayName = bus.hasSession() ? bus.langDoc()
                    .get(langKey) : null;
                if (displayName == null || displayName.isEmpty()) {
                    label.setText(I18n.get("panel.categories.cell.untranslated", cat.id));
                } else {
                    label.setText(McText.strip(displayName));
                }
            }
            return label;
        }
    }
}
