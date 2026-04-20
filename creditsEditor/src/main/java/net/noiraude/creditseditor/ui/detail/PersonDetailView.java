package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Scrollable;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.creditseditor.command.impl.RemoveMembershipCommand;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Form panel that displays and edits the fields of a single {@link DocumentPerson}.
 *
 * <p>
 * Subscribes to {@link DocumentBus#TOPIC_PERSON} to reload the current person when it is
 * the one that changed. Removal of the current person is detected by the owning
 * {@code DetailPanel}, which calls {@link #clear()} and switches the card away from this
 * view; this view therefore does not listen for {@code TOPIC_PERSONS}. Membership table,
 * toolbar, and selection-suppression logic are delegated to {@link MembershipTablePanel};
 * role editing is delegated to {@link MembershipRolePanel}. Changes are dispatched through
 * the supplied {@link CommandExecutor}; editing the name field goes through a
 * {@link DocumentEditCommand} that relies on this view's own property listener to write
 * the new value back into the model.
 */
public final class PersonDetailView extends DetailView<DocumentPerson> implements Scrollable {

    private final @NotNull DocumentBus bus;

    private final @NotNull MinecraftTextEditor nameEditor = new MinecraftTextEditor();
    private final @NotNull MembershipTablePanel membershipPanel;
    private final @NotNull MembershipRolePanel rolePanel;

    public PersonDetailView(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        super(onCommand);
        this.bus = bus;
        this.rolePanel = new MembershipRolePanel(bus, onCommand);
        this.membershipPanel = new MembershipTablePanel(this::onAddMembership, this::onRemoveMembership);
        buildLayout();
        wireEvents();
    }

    private void buildLayout() {
        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();

        // Row 0: Name
        label.gridy = 0;
        add(new JLabel("Name:"), label);
        field.gridy = 0;
        add(nameEditor, field);

        // Row 1: Memberships table + toolbar. weighty stays at 0 so the panel keeps its
        // preferred height (MembershipTablePanel pins 2 rows + toolbar); the role panel on
        // row 2 absorbs any extra vertical space.
        label.gridy = 1;
        label.anchor = GridBagConstraints.NORTHWEST;
        add(new JLabel("Memberships:"), label);
        field.gridy = 1;
        field.fill = GridBagConstraints.BOTH;
        add(membershipPanel, field);

        // Row 2: Role panel (spans both columns)
        GridBagConstraints roleGbc = new GridBagConstraints();
        roleGbc.gridy = 2;
        roleGbc.gridx = 0;
        roleGbc.gridwidth = 2;
        roleGbc.fill = GridBagConstraints.BOTH;
        roleGbc.weightx = 1.0;
        roleGbc.weighty = 1.0;
        roleGbc.insets = new Insets(scaled(4), scaled(4), scaled(4), scaled(4));
        add(rolePanel, roleGbc);
    }

    private void wireEvents() {
        wireNameEditor();
        membershipPanel.addRowSelectionListener(this::onMembershipSelected);
        // Name edits update the text field directly (via its document), so we only reload
        // the membership table on TOPIC_PERSON to avoid resetting the caret position while
        // the user is typing.
        bus.addListener(
            DocumentBus.TOPIC_PERSON,
            evt -> { if (current != null && evt.getNewValue() == current) reloadMemberships(); });
    }

    private void wireNameEditor() {
        nameEditor.addPropertyChangeListener("text", e -> {
            if (loading || current == null) return;
            DocumentPerson p = current;
            p.name = (String) e.getNewValue();
            bus.firePersonChanged(p);
        });
        nameEditor.addUndoableEditListener(e -> {
            if (!loading && current != null) {
                onCommand.execute(new DocumentEditCommand("Edit person name", e.getEdit()));
            }
        });
    }

    private void onMembershipSelected(int row) {
        if (row >= 0 && current != null) {
            rolePanel.load(current, current.memberships.get(row));
        } else {
            rolePanel.load(current, null);
        }
    }

    private void reloadMemberships() {
        if (current == null) return;
        membershipPanel.setMembershipsPreservingSelection(current.memberships, selectedMembershipCategoryId());
        onMembershipSelected(membershipPanel.getSelectedRow());
    }

    /**
     * Clears the current subject so stale bus events targeted at the previously displayed
     * person are ignored. Called by the owning panel when switching away from this view.
     */
    public void clear() {
        current = null;
    }

    /**
     * Populates all fields from {@code person} without firing any commands.
     * Call after any external model change: initial load, undo, or redo.
     */
    public void load(@NotNull DocumentPerson person) {
        String prevCategoryId = selectedMembershipCategoryId();
        current = person;
        loading = true;
        try {
            nameEditor.setText(person.name);
            membershipPanel.setMembershipsPreservingSelection(person.memberships, prevCategoryId);
        } finally {
            loading = false;
        }
        onMembershipSelected(membershipPanel.getSelectedRow());
    }

    private @Nullable String selectedMembershipCategoryId() {
        int row = membershipPanel.getSelectedRow();
        if (row >= 0 && current != null && row < current.memberships.size()) {
            return current.memberships.get(row).categoryId;
        }
        return null;
    }

    private void onAddMembership() {
        if (current == null || !bus.hasSession()) return;

        List<String> used = current.memberships.stream()
            .map(m -> m.categoryId)
            .toList();
        List<DocumentCategory> available = bus.creditsDoc().categories.stream()
            .filter(c -> !used.contains(c.id))
            .toList();

        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                McText.strip(current.name) + " is already a member of all categories.",
                "No categories available",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] labels = available.stream()
            .map(c -> {
                String displayName = bus.langDoc()
                    .get("credits.category." + KeySanitizer.sanitize(c.id));
                return c.id + (displayName == null || displayName.isEmpty() ? "" : " " + McText.strip(displayName));
            })
            .toArray(String[]::new);
        JComboBox<String> combo = new JComboBox<>(labels);
        int result = JOptionPane
            .showConfirmDialog(this, combo, "Add membership", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        DocumentCategory chosen = available.get(combo.getSelectedIndex());
        onCommand.execute(new AddMembershipCommand(bus, current, new DocumentMembership(chosen.id)));
    }

    private void onRemoveMembership() {
        if (current == null) return;
        int row = membershipPanel.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Select a membership row to remove.",
                "Nothing selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        DocumentMembership membership = current.memberships.get(row);
        onCommand.execute(new RemoveMembershipCommand(bus, current, membership));
    }

    // --- Scrollable: fill the viewport width, scroll vertically when content overflows ---

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return scaled(16);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == javax.swing.SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
