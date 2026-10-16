package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

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
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.MsgArg;
import net.noiraude.creditseditor.ui.component.dnd.ListReorderTransferHandler;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

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
    private static final int PREFERRED_VISIBLE_ROLE_ROWS = 5;

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;

    private final @NotNull JLabel contextLabel = new JLabel();
    private final @NotNull DefaultListModel<DocumentRole> roleListModel = new DefaultListModel<>();
    private final @NotNull JList<DocumentRole> roleList = new JList<>(roleListModel);
    private final @NotNull JButton addButton = new JButton(I18n.get("button.add"));
    private final @NotNull JButton deleteButton = new JButton(I18n.get("button.delete"));
    private final @NotNull JScrollPane roleListScroll = new JScrollPane(roleList);
    private final @NotNull JPanel toolbar = buildToolbar();
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

    @Override
    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        int cellHeight = roleList.getFixedCellHeight();
        int contextH = contextLabel.getPreferredSize().height;
        int rowsH = cellHeight * MIN_VISIBLE_ROLE_ROWS;
        Insets scrollInsets = roleListScroll.getInsets();
        int scrollH = rowsH + scrollInsets.top + scrollInsets.bottom;
        int toolbarH = toolbar.getPreferredSize().height;
        int rowSpacing = gapTiny * 6;
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
        List<DocumentRole> previous = roleList.getSelectedValuesList();
        rebuildListModel();
        restoreListSelection(previous);
        updateButtons();
        notifySelectionChanged();
    }

    /** Returns the currently selected roles in display order. */
    public @NotNull List<DocumentRole> getSelectedRoles() {
        return roleList.getSelectedValuesList();
    }

    /** Sets a callback fired whenever the list selection settles (after value-is-adjusting). */
    public void setSelectionListener(@Nullable Runnable listener) {
        this.selectionListener = listener;
    }

    static @NotNull String langKeyForRole(@NotNull DocumentRole role) {
        return role.translationKey();
    }

    void replaceSelectedReference(String key) {
        if (currentMembership == null || currentPerson == null || roleList.getSelectedIndices().length != 1) return;
        try {
            DocumentRole replacement = DocumentRole.translation(key);
            int index = roleList.getSelectedIndex();
            if (replacement.equals(currentMembership.roles.get(index))) return;
            if (currentMembership.roles.contains(replacement))
                throw new IllegalArgumentException(I18n.get("role.external.duplicate"));
            onCommand.execute(
                new net.noiraude.creditseditor.command.impl.EditPersonRoleCommand(
                    bus,
                    currentPerson,
                    currentMembership,
                    index,
                    replacement));
            roleList.setSelectedIndex(index);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                I18n.get("dialog.load.error.title"),
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configureList() {
        roleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        roleList.getAccessibleContext()
            .setAccessibleName(I18n.get("panel.roles.list.accessible"));
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
        roleList.setCellRenderer(new RoleListCellRenderer());
        int cellHeight = roleList.getCellRenderer()
            .getListCellRendererComponent(roleList, DocumentRole.local("X"), 0, false, false)
            .getPreferredSize().height;
        roleList.setFixedCellHeight(cellHeight);
        roleList.setVisibleRowCount(PREFERRED_VISIBLE_ROLE_ROWS);
    }

    private void assembleLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(gapTiny, gapSmall, gapTiny, gapSmall);

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(contextLabel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(roleListScroll, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(toolbar, gbc);
    }

    private @NotNull JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, gapSmall, gapTiny));
        bar.add(addButton);
        bar.add(deleteButton);
        return bar;
    }

    private void rebuildListModel() {
        roleListModel.clear();
        if (currentMembership != null) {
            for (DocumentRole role : currentMembership.roles) {
                roleListModel.addElement(role);
            }
        }
    }

    private void restoreListSelection(@NotNull List<DocumentRole> previous) {
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
        DocumentRole role = new AddRolePrompt(currentMembership, creditsDoc, bus).show(this);
        if (role == null) return;
        if (currentMembership.roles.contains(role)) {
            JOptionPane.showMessageDialog(
                this,
                I18n.get("panel.roles.duplicate.message", MsgArg.text(role.value())),
                I18n.get("panel.roles.duplicate.title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        onCommand.execute(new AddPersonRoleCommand(bus, currentPerson, currentMembership, role));
    }

    private void onDelete() {
        List<DocumentRole> selected = roleList.getSelectedValuesList();
        if (selected.isEmpty() || currentMembership == null || currentPerson == null) return;

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            I18n.get("command.remove.roles", MsgArg.count(selected.size()), MsgArg.text(currentMembership.categoryId)));
        for (DocumentRole role : selected) {
            builder.add(new RemovePersonRoleCommand(bus, currentPerson, currentMembership, role));
        }
        onCommand.execute(builder.build());
    }

    private void updateContextLabel() {
        boolean has = currentMembership != null;
        contextLabel.setText(
            has ? I18n.get("panel.roles.context", MsgArg.text(currentMembership.categoryId))
                : I18n.get("panel.roles.no_membership"));
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
        bus.addListener(DocumentBus.TOPIC_TRANSLATIONS, e -> roleList.repaint());
        addButton.addActionListener(e -> onAdd());
        deleteButton.addActionListener(e -> onDelete());
        roleList.addListSelectionListener(this::onSelectionChanged);
    }

    private final class RoleListCellRenderer extends DefaultListCellRenderer {

        @Override
        public @NotNull Component getListCellRendererComponent(@NotNull JList<?> list, @Nullable Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DocumentRole raw) {
                String display = bus.hasSession() ? raw.isExternal()
                    ? bus.externalTranslation(raw.translationKey(), bus.activeLocale())
                        .orElse(null)
                    : bus.langDoc()
                        .get(langKeyForRole(raw))
                    : null;
                label.setText((display == null || display.isEmpty()) ? raw.value() : McText.strip(display));
            }
            return label;
        }
    }
}
