package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddPersonRoleCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.MoveRolesOrderCommand;
import net.noiraude.creditseditor.command.impl.RemovePersonRoleCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.dnd.ListReorderTransferHandler;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top half of {@link MembershipRolePanel}: context label, role list, add/delete toolbar, and
 * drag-to-reorder handling.
 *
 * <p>
 * Owns the current person/membership pair so it can issue add/remove/reorder commands. The
 * parent panel observes selection changes via {@link #setSelectionListener(Runnable)} and
 * drives the right-hand detail card based on {@link #getSelectedRoles()}.
 */
public final class RoleListPanel extends JPanel {

    private static final int MIN_VISIBLE_ROLE_ROWS = 2;

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;

    private final @NotNull JLabel contextLabel = new JLabel();
    private final @NotNull DefaultListModel<String> roleListModel = new DefaultListModel<>();
    private final @NotNull JList<String> roleList = new JList<>(roleListModel);
    private final @NotNull JButton addButton = new JButton("Add");
    private final @NotNull JButton deleteButton = new JButton("Delete");
    private @NotNull JScrollPane roleListScroll = new JScrollPane();
    private @NotNull JPanel toolbar = new JPanel();
    private @Nullable Runnable selectionListener;

    private @Nullable DocumentPerson currentPerson;
    private @Nullable DocumentMembership currentMembership;

    public RoleListPanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        this.bus = bus;
        this.onCommand = onCommand;

        setLayout(new GridBagLayout());

        configureList();
        assembleLayout();
        wireEvents();

        load(null, null);
    }

    /**
     * Ensures the panel keeps the context label, {@value #MIN_VISIBLE_ROLE_ROWS} role-list
     * rows, and the Add/Delete toolbar visible even when the parent layout shrinks.
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
        int rowSpacing = scaled(2) * 2 * 2; // 2 inter-row gaps inside this panel
        int floorH = rowSpacing + contextH + scrollH + toolbarH;
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
        updateButtons();
        notifySelectionChanged();
    }

    /** Rebuilds the role list from the current membership, preserving the selection. */
    public void refresh() {
        List<String> previous = roleList.getSelectedValuesList();
        rebuildListModel();
        restoreListSelection(previous);
        updateButtons();
        notifySelectionChanged();
    }

    /** Returns the currently selected roles in display order. */
    public @NotNull List<String> getSelectedRoles() {
        return roleList.getSelectedValuesList();
    }

    /** Sets a callback fired whenever the list selection settles (after value-is-adjusting). */
    public void setSelectionListener(@Nullable Runnable listener) {
        this.selectionListener = listener;
    }

    static @NotNull String langKeyForRole(@NotNull String role) {
        return "credits.person.role." + KeySanitizer.sanitize(role);
    }

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
        gbc.weighty = 1.0;
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
    }

    private @NotNull JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, scaled(4), scaled(2)));
        bar.add(addButton);
        bar.add(deleteButton);
        return bar;
    }

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

    private void onSelectionChanged(@NotNull ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        updateButtons();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) selectionListener.run();
    }

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

    private void updateContextLabel() {
        boolean has = currentMembership != null;
        contextLabel.setText(has ? "Roles in " + currentMembership.categoryId + ":" : "Select a membership above");
        contextLabel.setFont(
            contextLabel.getFont()
                .deriveFont(has ? Font.PLAIN : Font.ITALIC));
        contextLabel.setForeground(UIManager.getColor(has ? "Label.foreground" : "Label.disabledForeground"));
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
}
