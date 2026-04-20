package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.UndoableEditEvent;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.*;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.RoleIndex;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;
import net.noiraude.creditseditor.ui.component.dnd.ListReorderTransferHandler;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

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
 *
 * <p>
 * Commands targeting roles (add, remove, reorder) require both the person and the
 * membership; the owning person is supplied via {@link #load(DocumentMembership)} by
 * {@link PersonDetailView}.
 */
public final class MembershipRolePanel extends JPanel {

    private static final int MIN_VISIBLE_ROLE_ROWS = 2;

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;

    private final @NotNull JLabel contextLabel = new JLabel();
    private final @NotNull DefaultListModel<String> roleListModel = new DefaultListModel<>();
    private final @NotNull JList<String> roleList = new JList<>(roleListModel);
    private final @NotNull JButton addButton = new JButton("Add");
    private final @NotNull JButton deleteButton = new JButton("Delete");
    private final @NotNull RoleDetailCard roleDetailCard;
    private @NotNull JScrollPane roleListScroll = new JScrollPane();
    private @NotNull JPanel toolbar = new JPanel();

    private @Nullable DocumentPerson currentPerson;
    private @Nullable DocumentMembership currentMembership;

    public MembershipRolePanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        this.bus = bus;
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

    /**
     * Ensures the titled panel keeps the context label, {@value #MIN_VISIBLE_ROLE_ROWS} role-list
     * rows, and the Add/Delete toolbar visible even when the parent layout shrinks. The role
     * detail card on row 3 is the only part allowed to collapse below its preferred height.
     */
    @Override
    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        int contextH = contextLabel.getPreferredSize().height;
        int rowsH = roleList.getFixedCellHeight() > 0 ? roleList.getFixedCellHeight() * MIN_VISIBLE_ROLE_ROWS
            : new JLabel("X").getPreferredSize().height * MIN_VISIBLE_ROLE_ROWS;
        Insets scrollInsets = roleListScroll.getInsets();
        int scrollH = rowsH + scrollInsets.top + scrollInsets.bottom;
        int toolbarH = toolbar.getPreferredSize().height;
        Insets borderInsets = getInsets();
        int rowSpacing = scaled(2) * 2 * 3; // 3 top-bottom insets between rows
        int floorH = borderInsets.top + borderInsets.bottom + rowSpacing + contextH + scrollH + toolbarH;
        return new Dimension(min.width, Math.max(min.height, floorH));
    }

    /**
     * Sets the current person and loads the role list for {@code membership}, or resets to
     * the placeholder state when {@code membership} is {@code null}. If {@code membership} is
     * the same object as the current one, delegates to {@link #refresh()} to preserve the
     * list selection.
     */
    public void load(@Nullable DocumentPerson person, @Nullable DocumentMembership membership) {
        this.currentPerson = person;
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
     * Convenience form for callers that do not track the owning person separately. Used by
     * internal selection changes that stay within a single person.
     */
    public void load(@Nullable DocumentMembership membership) {
        load(currentPerson, membership);
    }

    /** Rebuilds the role list from the current membership, preserving the selection. */
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
        roleList.setTransferHandler(
            new ListReorderTransferHandler<>(
                roleList,
                onCommand,
                (fromIndices, dropIndex) -> (currentMembership == null || currentPerson == null) ? null
                    : new MoveRolesOrderCommand(bus, currentPerson, currentMembership, fromIndices, dropIndex),
                i -> true,
                i -> currentMembership != null));
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
        roleListScroll = new JScrollPane(roleList) {

            @Override
            public Dimension getMinimumSize() {
                int rowH = roleList.getFixedCellHeight() > 0 ? roleList.getFixedCellHeight()
                    : getFontMetrics(roleList.getFont()).getHeight();
                Insets in = getInsets();
                int h = rowH * MIN_VISIBLE_ROLE_ROWS + in.top + in.bottom;
                Dimension d = super.getMinimumSize();
                return new Dimension(d.width, Math.max(d.height, h));
            }
        };
        add(roleListScroll, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        toolbar = buildToolbar();
        add(toolbar, gbc);

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
        String stored = bus.hasSession() ? bus.langDoc()
            .get(langKey) : null;
        roleDetailCard.load(role, langKey, stored);
    }

    // -----------------------------------------------------------------------
    // Add / Delete actions
    // -----------------------------------------------------------------------

    private void onAdd() {
        if (currentMembership == null || currentPerson == null) return;
        CreditsDocument creditsDoc = bus.hasSession() ? bus.creditsDoc() : null;
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
        onCommand.execute(new AddPersonRoleCommand(bus, currentPerson, currentMembership, role));
    }

    private void onDelete() {
        List<String> selected = roleList.getSelectedValuesList();
        if (selected.isEmpty() || currentMembership == null || currentPerson == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            "Remove " + selected.size() + " role(s) from " + currentMembership.categoryId);
        for (String role : selected) {
            builder.add(new RemovePersonRoleCommand(bus, currentPerson, currentMembership, role));
        }
        onCommand.execute(builder.build());
    }

    // -----------------------------------------------------------------------
    // Display-name change callbacks (wired into RoleDetailCard at construction)
    // -----------------------------------------------------------------------

    private void onDisplayNameChanged(@NotNull String value) {
        if (!bus.hasSession()) return;
        List<String> sel = roleList.getSelectedValuesList();
        if (sel.size() != 1) return;
        String langKey = langKeyForRole(sel.getFirst());
        if (value.isEmpty()) bus.langDoc()
            .remove(langKey);
        else bus.langDoc()
            .set(langKey, value);
        bus.fireLangChanged(langKey);
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
        boolean hasMembership = currentMembership != null && currentPerson != null;
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
                String display = bus.hasSession() ? bus.langDoc()
                    .get(langKeyForRole(raw)) : null;
                label.setText((display == null || display.isEmpty()) ? raw : McText.strip(display));
            }
            return label;
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
