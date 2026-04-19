package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.UndoableEditEvent;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.*;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.RoleIndex;
import net.noiraude.creditseditor.ui.component.McText;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sub-panel shown in {@link PersonDetailView} below the membership table.
 *
 * <p>
 * Displays the roles held by one person within a specific category membership. Supports
 * multi-selection, add, delete, and drag-to-reorder. Selecting exactly one role reveals
 * a lang-key / display-name form identical in structure to the category detail view.
 */
public final class MembershipRolePanel extends JPanel {

    private final @NotNull CommandExecutor onCommand;

    private final @NotNull JLabel contextLabel = new JLabel();
    private final @NotNull DefaultListModel<String> roleListModel = new DefaultListModel<>();
    private final @NotNull JList<String> roleList = new JList<>(roleListModel);
    private final @NotNull JButton addButton = new JButton("Add");
    private final @NotNull JButton deleteButton = new JButton("Delete");
    private final @NotNull RoleDetailCard roleDetailCard;

    private @Nullable DocumentMembership currentMembership;
    private @Nullable CreditsDocument creditsDoc;
    private @Nullable LangDocument langDoc;

    public MembershipRolePanel(@NotNull CommandExecutor onCommand) {
        this.onCommand = onCommand;
        roleDetailCard = new RoleDetailCard(
            this::onDisplayNameChanged,
            e -> onCommand.execute(new DocumentEditCommand("Edit role display name", e.getEdit())));

        setBorder(BorderFactory.createTitledBorder("Roles"));
        setLayout(new GridBagLayout());

        configureList();
        configureContextLabel();
        assembleLayout();
        wireEvents();

        load(null);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Sets the document context used for role suggestions and display-name resolution.
     * Call once after a session is loaded.
     */
    @Contract(mutates = "this")
    public void setContext(@NotNull CreditsDocument creditsDoc, @NotNull LangDocument langDoc) {
        this.creditsDoc = creditsDoc;
        this.langDoc = langDoc;
    }

    /**
     * Loads the role list for {@code membership}. Passing {@code null} resets to the
     * placeholder state. If {@code membership} is the same object as the current one,
     * delegates to {@link #refresh()} to preserve the list selection.
     */
    public void load(@Nullable DocumentMembership membership) {
        if (membership != null && membership == currentMembership) {
            refresh();
            return;
        }
        currentMembership = membership;
        updateContextLabel();
        rebuildListModel();
        roleDetailCard.clear();
        updateButtons();
    }

    /**
     * Rebuilds the role list from the current membership, preserving the selection where
     * possible. Call after an undo or redo that may have changed the role list.
     */
    public void refresh() {
        List<String> previous = roleList.getSelectedValuesList();
        rebuildListModel();
        restoreListSelection(previous);
        refreshDetailForm();
        updateButtons();
    }

    // -----------------------------------------------------------------------
    // List configuration
    // -----------------------------------------------------------------------

    private void configureList() {
        roleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        roleList.setDragEnabled(true);
        roleList.setDropMode(DropMode.INSERT);
        roleList.setTransferHandler(new RoleReorderTransferHandler());
        roleList.setVisibleRowCount(5);
        roleList.setCellRenderer(new RoleListCellRenderer());
    }

    // -----------------------------------------------------------------------
    // Layout construction
    // -----------------------------------------------------------------------

    private void configureContextLabel() {
        contextLabel.setFont(
            contextLabel.getFont()
                .deriveFont(Font.ITALIC));
        contextLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    }

    private void assembleLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(scaled(2), scaled(4), scaled(2), scaled(4));

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(contextLabel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JScrollPane(roleList), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(buildToolbar(), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        add(roleDetailCard.panel, gbc);
    }

    private @NotNull JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, scaled(4), scaled(2)));
        toolbar.add(addButton);
        toolbar.add(deleteButton);
        return toolbar;
    }

    // -----------------------------------------------------------------------
    // List model
    // -----------------------------------------------------------------------

    private void rebuildListModel() {
        roleListModel.clear();
        if (currentMembership != null) {
            for (String role : currentMembership.roles) {
                roleListModel.addElement(role);
            }
        }
    }

    private void restoreListSelection(@NotNull List<String> previous) {
        if (previous.isEmpty()) return;
        for (int i = 0; i < roleListModel.size(); i++) {
            if (previous.contains(roleListModel.get(i))) {
                roleList.addSelectionInterval(i, i);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Selection and detail form
    // -----------------------------------------------------------------------

    private void onSelectionChanged(@NotNull ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        refreshDetailForm();
        updateButtons();
    }

    private void refreshDetailForm() {
        List<String> selected = roleList.getSelectedValuesList();
        if (selected.size() == 1) {
            loadDetailForm(selected.getFirst());
        } else {
            roleDetailCard.clear();
        }
    }

    private void loadDetailForm(@NotNull String role) {
        String langKey = langKeyForRole(role);
        String stored = langDoc != null ? langDoc.get(langKey) : null;
        roleDetailCard.load(role, langKey, stored);
    }

    // -----------------------------------------------------------------------
    // Add / Delete actions
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (currentMembership == null) return;
        String role = new AddRolePrompt(currentMembership, creditsDoc).show(this);
        if (role == null) return;
        if (currentMembership.roles.contains(role)) {
            JOptionPane.showMessageDialog(
                this,
                "Role '" + role + "' is already assigned in this category.",
                "Duplicate role",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        onCommand.execute(new AddPersonRoleCommand(currentMembership, role));
    }

    private void onDelete() {
        List<String> selected = roleList.getSelectedValuesList();
        if (selected.isEmpty() || currentMembership == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            "Remove " + selected.size() + " role(s) from " + currentMembership.categoryId);
        for (String role : selected) {
            builder.add(new RemovePersonRoleCommand(currentMembership, role));
        }
        onCommand.execute(builder.build());
    }

    // -----------------------------------------------------------------------
    // Display-name change callbacks (wired into RoleDetailCard at construction)
    // -----------------------------------------------------------------------

    private void onDisplayNameChanged(@NotNull String value) {
        if (langDoc == null) return;
        List<String> sel = roleList.getSelectedValuesList();
        if (sel.size() != 1) return;
        String langKey = langKeyForRole(sel.getFirst());
        if (value.isEmpty()) langDoc.remove(langKey);
        else langDoc.set(langKey, value);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void updateContextLabel() {
        if (currentMembership != null) {
            contextLabel.setText("Roles in " + currentMembership.categoryId + ":");
            contextLabel.setFont(
                contextLabel.getFont()
                    .deriveFont(Font.PLAIN));
            contextLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            contextLabel.setText("Select a membership above");
            contextLabel.setFont(
                contextLabel.getFont()
                    .deriveFont(Font.ITALIC));
            contextLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    private void updateButtons() {
        boolean hasMembership = currentMembership != null;
        addButton.setEnabled(hasMembership);
        deleteButton.setEnabled(hasMembership && !roleList.isSelectionEmpty());
    }

    private void wireEvents() {
        addButton.addActionListener(e -> onAdd());
        deleteButton.addActionListener(e -> onDelete());
        roleList.addListSelectionListener(this::onSelectionChanged);
    }

    @Contract(pure = true)
    private static @NotNull String langKeyForRole(@NotNull String role) {
        return "credits.person.role." + KeySanitizer.sanitize(role);
    }

    // -----------------------------------------------------------------------
    // Cell renderer
    // -----------------------------------------------------------------------

    private final class RoleListCellRenderer extends DefaultListCellRenderer {

        @Override
        public @NotNull Component getListCellRendererComponent(@NotNull JList<?> list, @Nullable Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String raw) {
                String display = langDoc != null ? langDoc.get(langKeyForRole(raw)) : null;
                label.setText((display == null || display.isEmpty()) ? raw : McText.strip(display));
            }
            return label;
        }
    }

    // -----------------------------------------------------------------------
    // Drag-and-drop: reorder within list
    // -----------------------------------------------------------------------

    private final class RoleReorderTransferHandler extends TransferHandler {

        private int dragFromIndex = -1;

        @Contract(pure = true)
        @Override
        public int getSourceActions(@NotNull JComponent c) {
            return MOVE;
        }

        @Override
        protected @Nullable Transferable createTransferable(@NotNull JComponent c) {
            int[] indices = roleList.getSelectedIndices();
            if (indices.length != 1) return null;
            dragFromIndex = indices[0];
            return new StringSelection(roleListModel.get(dragFromIndex));
        }

        @Override
        public boolean canImport(@NotNull TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor)
                && currentMembership != null;
        }

        @Override
        public boolean importData(@NotNull TransferSupport support) {
            if (!canImport(support) || dragFromIndex < 0 || currentMembership == null) return false;
            int dropIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
            if (dropIndex == dragFromIndex || dropIndex == dragFromIndex + 1) return false;
            onCommand.execute(new MoveRoleOrderCommand(currentMembership, dragFromIndex, dropIndex));
            return true;
        }

        @Override
        protected void exportDone(@NotNull JComponent source, @Nullable Transferable data, int action) {
            dragFromIndex = -1;
        }
    }

    // -----------------------------------------------------------------------
    // Role detail card
    // -----------------------------------------------------------------------

    private static final class RoleDetailCard {

        private static final @NotNull String CARD_EMPTY = "empty";
        private static final @NotNull String CARD_DETAIL = "detail";

        private final @NotNull CardLayout cards = new CardLayout();
        final @NotNull JPanel panel = new JPanel(cards);
        private final @NotNull JTextField roleValueField = new JTextField();
        private final @NotNull JLabel langKeyLabel = new JLabel();
        private final @NotNull MinecraftTextEditor displayNameEditor = new MinecraftTextEditor();
        private boolean loading;

        RoleDetailCard(@NotNull Consumer<String> onDisplayNameChanged,
            @NotNull Consumer<UndoableEditEvent> onUndoableEdit) {
            roleValueField.setEditable(false);
            roleValueField.setBackground(UIManager.getColor("Panel.background"));
            langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            buildEmptyCard();
            buildDetailCard();
            clear();

            displayNameEditor.addPropertyChangeListener(
                MinecraftTextEditor.PROP_TEXT,
                e -> { if (!loading) onDisplayNameChanged.accept((String) e.getNewValue()); });
            displayNameEditor.addUndoableEditListener(e -> { if (!loading) onUndoableEdit.accept(e); });
        }

        private void buildEmptyCard() {
            JLabel hint = new JLabel("Select a single role to edit its display name");
            hint.setFont(
                hint.getFont()
                    .deriveFont(Font.ITALIC));
            hint.setForeground(UIManager.getColor("Label.disabledForeground"));
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(hint, CARD_EMPTY);
        }

        private void buildDetailCard() {
            JPanel detail = new JPanel(new GridBagLayout());
            GridBagConstraints lbl = labelConstraints();
            GridBagConstraints fld = fieldConstraints();

            lbl.gridy = 0;
            detail.add(new JLabel("Role:"), lbl);
            fld.gridy = 0;
            detail.add(roleValueField, fld);

            lbl.gridy = 1;
            detail.add(new JLabel("Lang key:"), lbl);
            fld.gridy = 1;
            detail.add(langKeyLabel, fld);

            lbl.gridy = 2;
            lbl.anchor = GridBagConstraints.NORTHWEST;
            detail.add(new JLabel("Display name:"), lbl);
            fld.gridy = 2;
            detail.add(displayNameEditor, fld);

            GridBagConstraints spacer = new GridBagConstraints();
            spacer.gridy = 3;
            spacer.gridx = 0;
            spacer.gridwidth = 2;
            spacer.weighty = 1.0;
            spacer.fill = GridBagConstraints.VERTICAL;
            detail.add(Box.createVerticalGlue(), spacer);

            panel.add(detail, CARD_DETAIL);
        }

        void load(@NotNull String role, @NotNull String langKey, @Nullable String displayName) {
            loading = true;
            try {
                roleValueField.setText(role);
                langKeyLabel.setText(langKey);
                displayNameEditor.setText(displayName != null ? displayName : "");
            } finally {
                loading = false;
            }
            cards.show(panel, CARD_DETAIL);
        }

        void clear() {
            cards.show(panel, CARD_EMPTY);
        }

        @Contract(value = " -> new", pure = true)
        private static @NotNull GridBagConstraints labelConstraints() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 0;
            c.weighty = 0;
            c.insets = new Insets(scaled(4), scaled(6), scaled(4), scaled(4));
            return c;
        }

        @Contract(value = " -> new", pure = true)
        private static @NotNull GridBagConstraints fieldConstraints() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            c.weighty = 0;
            c.insets = new Insets(scaled(4), 0, scaled(4), scaled(6));
            return c;
        }
    }

    // -----------------------------------------------------------------------
    // Add role prompt dialog
    // -----------------------------------------------------------------------

    private static final class AddRolePrompt {

        private final @NotNull DocumentMembership membership;
        private final @Nullable RoleIndex index;
        private final @NotNull JComboBox<String> combo = new JComboBox<>();
        private final @NotNull JCheckBox showAll = new JCheckBox("Show all roles");
        final @NotNull JPanel content;

        AddRolePrompt(@NotNull DocumentMembership membership, @Nullable CreditsDocument creditsDoc) {
            this.membership = membership;
            this.index = creditsDoc != null ? RoleIndex.build(creditsDoc) : null;
            combo.setEditable(true);
            content = new JPanel(new BorderLayout(0, scaled(4)));
            content.add(combo, BorderLayout.CENTER);
            content.add(showAll, BorderLayout.SOUTH);
            populate();
            showAll.addActionListener(e -> populate());
        }

        private void populate() {
            String typed = combo.getEditor()
                .getItem() instanceof String s ? s : "";
            combo.removeAllItems();
            if (index != null) {
                for (RoleIndex.Entry entry : index.entries()) {
                    if (
                        !membership.roles.contains(entry.raw)
                            && (showAll.isSelected() || entry.categoryIds.contains(membership.categoryId))
                    ) {
                        combo.addItem(entry.raw);
                    }
                }
            }
            combo.getEditor()
                .setItem(typed);
        }

        /**
         * Shows the dialog. Returns the chosen role string, or {@code null} if the user
         * canceled or left the field empty.
         */
        @Nullable
        String show(@NotNull Component parent) {
            int result = JOptionPane.showConfirmDialog(
                parent,
                content,
                "Add role to " + membership.categoryId,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return null;
            String role = ((String) combo.getEditor()
                .getItem()).strip();
            return role.isEmpty() ? null : role;
        }
    }
}
